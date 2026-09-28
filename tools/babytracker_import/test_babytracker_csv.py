"""Tests for babytracker_csv.py. Run:  python -m unittest tools/babytracker_import/test_babytracker_csv.py

The test database is built from the app's own exported Room schema, so it has exactly the
tables and columns the app has.
"""
import collections
import datetime as dt
import glob
import json
import os
import sqlite3
import sys
import tempfile
import unittest
import uuid
from zoneinfo import ZoneInfo

sys.path.insert(0, os.path.dirname(__file__))
import babytracker_csv as bt  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
NY = ZoneInfo("America/New_York")
BIRTH = (dt.date(2026, 7, 9) - bt.EPOCH).days


def schema_db() -> sqlite3.Connection:
    latest = sorted(glob.glob(os.path.join(ROOT, "core", "database", "schemas", "*", "*.json")),
                    key=lambda p: int(os.path.basename(p).split(".")[0]))[-1]
    con = sqlite3.connect(":memory:")
    with open(latest, encoding="utf-8") as f:
        entities = json.load(f)["database"]["entities"]
    for entity in entities:
        con.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        for index in entity.get("indices", []):
            con.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
    con.execute("PRAGMA foreign_keys = ON")
    con.execute("INSERT INTO baby VALUES ('baby-1', 'Ava', ?, NULL, 1, 1, NULL)", (BIRTH,))
    con.commit()  # an open transaction would make sqlite's backup() wait forever
    return con


PUMP_HEADER = ["Time", "Start side", "Left duration (min)", "Right Duration (min)", "Total duration (min)",
               "Left Amount (oz.)", "Right Amount (oz.)", "Total amount (oz.)", "Note"]


def pump(time, ld="", rd="", td="", la="", ra="", ta="", note=""):
    return dict(zip(PUMP_HEADER, [time, "", ld, rd, td, la, ra, ta, note]), _src="pump.csv:x")


def run(export, fixes=None, con=None, write=True, overlap=5):
    con = con or schema_db()
    report = bt.Report()
    rows = bt.convert(export, NY, fixes or {}, report)
    bt.import_into(con, rows, report, write, overlap)
    return con, report, rows


def count(con, table="log_entry", where="1=1"):
    return con.execute(f"SELECT COUNT(*) FROM {table} WHERE {where}").fetchone()[0]


class Diapers(unittest.TestCase):
    def rows(self, *items):
        return {"DIAPER": [{"Baby": "Baby Pooh", "Time": t, "Status": s, "Note": "", "_src": f"d:{i}"} for i, (t, s) in enumerate(items)]}

    def test_status_mapping_and_dry_skipped(self):
        con, report, _ = run(self.rows(("7/15/26 8:27 AM", "Wet"), ("7/15/26 9:00 AM", "Dirty"),
                                       ("7/15/26 10:00 AM", "Mixed"), ("7/15/26 11:00 AM", "Dry")))
        kinds = dict(con.execute("SELECT diaper_kind, COUNT(*) FROM log_entry GROUP BY 1"))
        self.assertEqual(kinds, {"PEE": 1, "POTTY": 2})
        self.assertEqual([(k, r) for k, r, _ in report.skipped], [("DIAPER", "dry diaper (no equivalent)")])

    def test_identical_rows_at_the_same_minute_stay_two_entries(self):
        con, _, _ = run(self.rows(("7/15/26 7:44 PM", "Dirty"), ("7/15/26 7:44 PM", "Dirty"),
                                  ("7/15/26 5:46 PM", "Dirty"), ("7/15/26 5:46 PM", "Wet")))
        self.assertEqual(count(con), 4)

    def test_times_are_converted_with_the_zone(self):
        con, _, _ = run(self.rows(("7/15/26 8:27 AM", "Wet")))
        ms, day = con.execute("SELECT occurred_at, local_epoch_day FROM log_entry").fetchone()
        self.assertEqual(dt.datetime.fromtimestamp(ms / 1000, dt.timezone.utc), dt.datetime(2026, 7, 15, 12, 27, tzinfo=dt.timezone.utc))
        self.assertEqual(bt.EPOCH + dt.timedelta(days=day), dt.date(2026, 7, 15))


class Growth(unittest.TestCase):
    def test_pounds_become_grams_date_only(self):
        con, _, _ = run({"GROWTH": [{"Baby": "Baby Pooh", "Time": "7/22/26 9:11 PM", "Length (inches)": "", "Weight (lbs.)": "7.25",
                                     "Head Size (inches)": "", "Note": "", "_src": "g:2"}]})
        weight, length, occurred = con.execute("SELECT weight_g, length_mm, occurred_at FROM log_entry").fetchone()
        self.assertEqual((weight, length, occurred), (3289, None, None))

    def test_implausible_weight_is_skipped(self):
        _, report, _ = run({"GROWTH": [{"Baby": "Baby Pooh", "Time": "7/22/26 9:11 PM", "Length (inches)": "", "Weight (lbs.)": "700",
                                        "Head Size (inches)": "", "Note": "", "_src": "g:2"}]})
        self.assertEqual(report.skipped[0][1], "implausible weight")


class Formula(unittest.TestCase):
    def test_formula_becomes_a_bottle(self):
        con, _, _ = run({"BOTTLE": [{"Baby": "Baby Pooh", "Time": "7/26/26 9:35 PM", "Amount (oz.)": "2", "Note": "", "_src": "f:2"}]})
        ml = con.execute("SELECT volume_ml FROM log_entry WHERE type = 'BOTTLE'").fetchone()[0]
        self.assertAlmostEqual(ml, 2 * bt.ML_PER_OZ, places=2)


class Pumping(unittest.TestCase):
    def one(self, *rows, fixes=None):
        con, report, _ = run({"PUMP": list(rows)}, fixes)
        return con, report

    def session(self, con):
        return con.execute("SELECT volume_ml, side, duration_min FROM pump_session").fetchall()

    def test_amount_row_and_minutes_row_at_the_same_time_merge(self):
        con, _ = self.one(pump("7/15/26 10:50 AM", la="1.5", ra="1.5", ta="3"),
                          pump("7/15/26 10:50 AM", ld="28", rd="28", td="56"))
        (ml, side, minutes), = self.session(con)
        self.assertAlmostEqual(ml, 3 * bt.ML_PER_OZ, places=2)
        self.assertEqual((side, minutes), ("BOTH", 28), "longer side, not the 56 minute sum")

    def test_side_comes_from_the_breast_that_has_data(self):
        con, _ = self.one(pump("7/15/26 9:48 PM", ra="1", ta="1"), pump("7/19/26 11:30 AM", la="2", ta="2"),
                          pump("7/20/26 1:08 PM", ld="1", rd="1", td="2", ta="3.5"), pump("7/16/26 6:00 PM", ta="3"))
        sides = dict(con.execute("SELECT volume_ml / ?, side FROM pump_session", (bt.ML_PER_OZ,)).fetchall() and
                     [(round(v, 1), s) for v, s in con.execute("SELECT volume_ml / ?, side FROM pump_session", (bt.ML_PER_OZ,))])
        self.assertEqual(sides, {1.0: "RIGHT", 2.0: "LEFT", 3.5: "BOTH", 3.0: None})

    def test_left_plus_right_wins_over_a_blank_total(self):
        con, _ = self.one(pump("7/15/26 9:36 PM", ld="1", rd="1", td="2", la="0.25"))
        (ml, side, _), = self.session(con)
        self.assertAlmostEqual(ml, 0.25 * bt.ML_PER_OZ, places=2)
        self.assertEqual(side, "BOTH")

    def test_minutes_only_session_is_skipped(self):
        con, report = self.one(pump("7/15/26 5:30 PM", ld="1", rd="1", td="2"))
        self.assertEqual(count(con, "pump_session"), 0)
        self.assertEqual(report.skipped[0][1], "no amount (minutes only)")

    def test_typo_amount_is_skipped_unless_fixed(self):
        con, report = self.one(pump("8/15/26 12:26 AM", ta="53"))
        self.assertEqual(count(con, "pump_session"), 0)
        self.assertIn("implausible", report.skipped[0][1])
        con, _ = self.one(pump("8/15/26 12:26 AM", ta="53"), fixes={"8/15/26 12:26 AM": 5.3})
        self.assertAlmostEqual(self.session(con)[0][0], 5.3 * bt.ML_PER_OZ, places=2)

    def test_mother_row_is_created_once(self):
        con, _ = self.one(pump("7/15/26 10:50 AM", ta="3"), pump("7/16/26 1:01 AM", ta="2.5"))
        self.assertEqual(count(con, "caregiver", "role = 'MOTHER'"), 1)


class Safety(unittest.TestCase):
    export = {"DIAPER": [{"Baby": "Baby Pooh", "Time": "7/15/26 8:27 AM", "Status": "Wet", "Note": "", "_src": "d:2"}],
              "PUMP": [pump("7/15/26 10:50 AM", ta="3")], "NURSING": [{"Baby": "Baby Pooh", "Time": "7/9/26 3:06 PM", "_src": "n:2"}]}

    def test_rerun_adds_nothing(self):
        con, _, _ = run(self.export)
        before = (count(con), count(con, "pump_session"))
        _, report, _ = run(self.export, con=con)
        self.assertEqual((count(con), count(con, "pump_session")), before)
        self.assertEqual(report.already["DIAPER"], 1)
        self.assertEqual(report.inserted["DIAPER"], 0)

    def test_dry_run_writes_nothing(self):
        con, report, _ = run(self.export, write=False)
        self.assertEqual((count(con), count(con, "pump_session")), (0, 0))
        self.assertEqual(report.inserted["DIAPER"], 1)

    def test_nursing_is_skipped(self):
        con, report, _ = run(self.export)
        self.assertEqual(report.parsed["NURSING"], 1)
        self.assertTrue(any(k == "NURSING" for k, _, _ in report.skipped))

    def test_entry_you_already_logged_is_not_doubled(self):
        con = schema_db()
        ms = bt.to_ms(dt.datetime(2026, 7, 15, 8, 29), NY)
        day = (dt.date(2026, 7, 15) - bt.EPOCH).days
        con.execute("INSERT INTO log_entry (id, baby_id, type, occurred_at, local_epoch_day, diaper_kind, created_at, updated_at)"
                    " VALUES ('mine', 'baby-1', 'DIAPER', ?, ?, 'PEE', 1, 1)", (ms, day))
        _, report, _ = run(self.export, con=con)
        self.assertEqual(count(con, where="type = 'DIAPER'"), 1)
        self.assertEqual([r for k, r, _ in report.skipped if k == "DIAPER"], ["overlaps an entry already in the app"])

    def test_a_deleted_entry_does_not_block_and_a_different_kind_does_not_overlap(self):
        con = schema_db()
        ms = bt.to_ms(dt.datetime(2026, 7, 15, 8, 29), NY)
        day = (dt.date(2026, 7, 15) - bt.EPOCH).days
        con.execute("INSERT INTO log_entry (id, baby_id, type, occurred_at, local_epoch_day, diaper_kind, created_at, updated_at, deleted_at)"
                    " VALUES ('gone', 'baby-1', 'DIAPER', ?, ?, 'PEE', 1, 1, 5)", (ms, day))
        con.execute("INSERT INTO log_entry (id, baby_id, type, occurred_at, local_epoch_day, diaper_kind, created_at, updated_at)"
                    " VALUES ('other', 'baby-1', 'DIAPER', ?, ?, 'POTTY', 1, 1)", (ms, day))
        run(self.export, con=con)
        self.assertEqual(count(con, where="type = 'DIAPER' AND deleted_at IS NULL"), 2)

    def test_entries_before_the_birth_date_are_skipped_but_pumping_is_not(self):
        con = schema_db()
        con.execute("UPDATE baby SET birth_epoch_day = ?", ((dt.date(2026, 8, 1) - bt.EPOCH).days,))
        _, report, _ = run(self.export, con=con)
        self.assertEqual(count(con), 0)
        self.assertEqual(count(con, "pump_session"), 1)
        self.assertEqual([r for k, r, _ in report.skipped if k == "DIAPER"], ["before the baby's birth date in the app"])

    def test_no_baby_is_a_clear_error(self):
        con = schema_db()
        con.execute("DELETE FROM baby")
        with self.assertRaises(SystemExit):
            run(self.export, con=con)


class Reading(unittest.TestCase):
    def test_files_are_recognised_by_header_not_name(self):
        with tempfile.TemporaryDirectory() as d:
            for name, text in {"a.csv": "Baby,Time,Status,Note\n", "b.csv": "Baby,Time,Amount (oz.),Note\n",
                               "c.csv": "Time,Start side,Total amount (oz.)\n",
                               "d.csv": "Baby,Time,Start side,Total (min)\n"}.items():
                with open(os.path.join(d, name), "w", encoding="utf-8") as f:
                    f.write(text + '"Baby Pooh","7/9/26 3:06 PM"' + "," * 6 + "\n")
            found = bt.read_export(d)
        self.assertEqual(set(found), {"DIAPER", "BOTTLE", "PUMP", "NURSING"})


if __name__ == "__main__":
    unittest.main()
