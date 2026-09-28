#!/usr/bin/env python3
"""One-time import of a Baby Tracker (nighp.com) CSV export into an Artha tracker database.

    python babytracker_csv.py export.zip --adb                # dry run against the attached device
    python babytracker_csv.py export.zip --adb --write        # really import
    python babytracker_csv.py export/ --db baby-status.db     # a local database file instead

The device needs a *debug* build of the app (adb's run-as only opens debuggable apps).
Nothing is written without --write, and --write first saves the untouched database files
next to the script's working directory, so the backup folder is the undo.

Mapping (decided):
    nursing rows        -> skipped (the app has no breastfeeding)
    formula rows        -> BOTTLE
    diaper Wet          -> PEE;  Dirty and Mixed -> POTTY;  Dry -> skipped
    growth rows         -> GROWTH, weight only, date only (lb -> g)
    pump rows           -> pump_session; rows with the same time merge into one session

Re-running is safe: every row gets a deterministic id and is inserted with INSERT OR IGNORE.
"""
from __future__ import annotations

import argparse
import collections
import csv
import datetime as dt
import io
import os
import re
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import time
import uuid
import zipfile
from dataclasses import dataclass, field
from zoneinfo import ZoneInfo

PACKAGE = "com.sagar.babystatus"
DB_NAME = "baby-status.db"
ML_PER_OZ = 29.5735295625
GRAMS_PER_LB = 453.59237
NAMESPACE = uuid.UUID("6f0d3c1e-5b8a-4e0e-9a55-3f1f5f0a7b21")  # fixed: ids must not change between runs
EPOCH = dt.date(1970, 1, 1)
TIME_FORMAT = "%m/%d/%y %I:%M %p"  # 8/15/26 10:50 AM
MAX_PUMP_OZ = 20.0  # the app's slider stops at 10; anything above 20 is a typo
GROWTH_KG_RANGE = (0.4, 30.0)  # same plausibility window the app's growth sheet uses


# ---------------------------------------------------------------- report

@dataclass
class Report:
    parsed: collections.Counter = field(default_factory=collections.Counter)
    inserted: collections.Counter = field(default_factory=collections.Counter)
    already: collections.Counter = field(default_factory=collections.Counter)
    skipped: list = field(default_factory=list)  # (kind, reason, detail)
    warnings: list = field(default_factory=list)

    def skip(self, kind: str, reason: str, detail: str) -> None:
        self.skipped.append((kind, reason, detail))

    def print(self, dry_run: bool) -> None:
        kinds = ["BOTTLE", "DIAPER", "GROWTH", "PUMP", "NURSING"]
        print(f"\n{'kind':<9}{'found':>8}{'to add' if dry_run else 'added':>8}{'already':>9}{'skipped':>9}")
        for kind in kinds:
            skipped = sum(1 for k, _, _ in self.skipped if k == kind)
            print(f"{kind:<9}{self.parsed[kind]:>8}{self.inserted[kind]:>8}{self.already[kind]:>9}{skipped:>9}")
        by_reason = collections.defaultdict(list)
        for kind, reason, detail in self.skipped:
            by_reason[(kind, reason)].append(detail)
        if by_reason:
            print("\nSkipped:")
            for (kind, reason), details in sorted(by_reason.items()):
                print(f"  {kind}: {reason} ({len(details)})")
                if kind == "NURSING":
                    continue  # expected and hundreds long: the count is enough
                for d in details[:8]:
                    print(f"      {d}")
                if len(details) > 8:
                    print(f"      ... and {len(details) - 8} more")
        print("\n(pump 'found' counts sessions: rows at the same time are merged)")
        for w in self.warnings:
            print(f"warning: {w}")


# ---------------------------------------------------------------- reading the export

def read_export(path: str) -> dict[str, list[dict]]:
    """Reads a folder or zip of CSVs and files them by what their header says, not their name."""
    texts: dict[str, str] = {}
    if zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as z:
            for name in z.namelist():
                if name.lower().endswith(".csv"):
                    texts[name] = z.read(name).decode("utf-8-sig")
    else:
        for name in sorted(os.listdir(path)):
            if name.lower().endswith(".csv"):
                with open(os.path.join(path, name), encoding="utf-8-sig", newline="") as f:
                    texts[name] = f.read()
    out: dict[str, list[dict]] = {}
    for name, text in texts.items():
        rows = list(csv.DictReader(io.StringIO(text)))
        header = set(rows[0].keys()) if rows else set(csv.reader(io.StringIO(text)).__next__())
        if "Status" in header:
            kind = "DIAPER"
        elif "Weight (lbs.)" in header:
            kind = "GROWTH"
        elif "Amount (oz.)" in header:
            kind = "BOTTLE"
        elif "Total amount (oz.)" in header:
            kind = "PUMP"
        elif "Total (min)" in header and "Start side" in header:
            kind = "NURSING"
        else:
            print(f"warning: {name}: unrecognised columns, ignored", file=sys.stderr)
            continue
        out.setdefault(kind, []).extend(dict(r, _src=f"{name}:{i}") for i, r in enumerate(rows, 2))
    return out


def parse_time(text: str) -> dt.datetime:
    return dt.datetime.strptime(text.strip(), TIME_FORMAT)


def num(text: str | None) -> float:
    text = (text or "").strip()
    return float(text) if text else 0.0


# ---------------------------------------------------------------- converting

@dataclass
class NewRow:
    table: str  # log_entry | pump_session
    kind: str  # BOTTLE | DIAPER | GROWTH | PUMP
    id: str
    local: dt.datetime
    occurred_ms: int | None
    day: int  # local epoch day
    cols: dict
    label: str  # for reports


def make_id(seen: collections.Counter, kind: str, local: dt.datetime, values: str) -> str:
    """Deterministic id. Two identical rows at the same minute (e.g. two Dirty diapers at
    7:44 PM) are distinct entries, so the occurrence number is part of the key."""
    base = f"babytracker|{kind}|{local.isoformat()}|{values}"
    n = seen[base]
    seen[base] += 1
    return str(uuid.uuid5(NAMESPACE, f"{base}|{n}"))


def to_ms(local: dt.datetime, zone: ZoneInfo) -> int:
    return int(local.replace(tzinfo=zone).timestamp() * 1000)


def epoch_day(local: dt.datetime) -> int:
    return (local.date() - EPOCH).days


def convert(export: dict[str, list[dict]], zone: ZoneInfo, pump_fixes: dict[str, float], report: Report) -> list[NewRow]:
    rows: list[NewRow] = []
    seen: collections.Counter = collections.Counter()

    def timed(kind, r):
        try:
            return parse_time(r["Time"])
        except ValueError:
            report.skip(kind, "unreadable time", f"{r['_src']}: {r['Time']!r}")
            return None

    for r in export.get("NURSING", []):
        report.parsed["NURSING"] += 1
        report.skip("NURSING", "no breastfeeding in the app (by decision)", r["_src"])

    for r in export.get("BOTTLE", []):
        report.parsed["BOTTLE"] += 1
        local = timed("BOTTLE", r)
        oz = num(r["Amount (oz.)"])
        if local is None:
            continue
        if not 0 < oz <= MAX_PUMP_OZ:
            report.skip("BOTTLE", "implausible amount", f"{r['_src']}: {r['Time']} {r['Amount (oz.)']!r} oz")
            continue
        ml = round(oz * ML_PER_OZ, 3)
        rows.append(NewRow("log_entry", "BOTTLE", make_id(seen, "BOTTLE", local, f"{ml}"), local, to_ms(local, zone),
                           epoch_day(local), {"volume_ml": ml, "note": r["Note"].strip() or None},
                           f"{r['Time']} formula {oz:g} oz"))

    status_map = {"wet": "PEE", "dirty": "POTTY", "mixed": "POTTY"}
    for r in export.get("DIAPER", []):
        report.parsed["DIAPER"] += 1
        local = timed("DIAPER", r)
        if local is None:
            continue
        status = r["Status"].strip().lower()
        if status == "dry":
            report.skip("DIAPER", "dry diaper (no equivalent)", f"{r['_src']}: {r['Time']}")
            continue
        if status not in status_map:
            report.skip("DIAPER", "unknown status", f"{r['_src']}: {r['Time']} {r['Status']!r}")
            continue
        kind = status_map[status]
        rows.append(NewRow("log_entry", "DIAPER", make_id(seen, "DIAPER", local, kind), local, to_ms(local, zone),
                           epoch_day(local), {"diaper_kind": kind, "note": r["Note"].strip() or None},
                           f"{r['Time']} {r['Status']} -> {kind}"))

    for r in export.get("GROWTH", []):
        report.parsed["GROWTH"] += 1
        local = timed("GROWTH", r)
        if local is None:
            continue
        lb, inches = num(r["Weight (lbs.)"]), num(r["Length (inches)"])
        weight_g = round(lb * GRAMS_PER_LB) if lb else None
        length_mm = round(inches * 25.4) if inches else None
        if weight_g is None and length_mm is None:
            report.skip("GROWTH", "no weight or length", f"{r['_src']}: {r['Time']}")
            continue
        if weight_g is not None and not GROWTH_KG_RANGE[0] <= weight_g / 1000 <= GROWTH_KG_RANGE[1]:
            report.skip("GROWTH", "implausible weight", f"{r['_src']}: {r['Time']} {lb} lb")
            continue
        if num(r.get("Head Size (inches)")):
            report.warnings.append(f"{r['_src']}: head size is not stored by the app")
        rows.append(NewRow("log_entry", "GROWTH", make_id(seen, "GROWTH", local, f"{weight_g}|{length_mm}"), local, None,
                           epoch_day(local), {"weight_g": weight_g, "length_mm": length_mm, "note": r["Note"].strip() or None},
                           f"{r['Time']} {lb} lb"))

    rows.extend(convert_pumping(export.get("PUMP", []), zone, pump_fixes, report, seen))
    return rows


def convert_pumping(pump_rows: list[dict], zone: ZoneInfo, fixes: dict[str, float], report: Report,
                    seen: collections.Counter) -> list[NewRow]:
    """Baby Tracker splits one session over several rows (an amount row and a minutes row at the
    same time). Rows with the same timestamp merge into one session:
      volume   = sum of the rows' amounts (left + right when given, else the total)
      side     = LEFT/RIGHT when only that breast has an amount or minutes, BOTH when both do
      duration = the longer of the two breasts' minutes (their 'total' adds the sides together,
                 which double-counts a double pump)"""
    groups: dict[str, list[dict]] = collections.OrderedDict()
    for r in pump_rows:
        groups.setdefault(r["Time"].strip(), []).append(r)
    out = []
    for time_text, group in groups.items():
        report.parsed["PUMP"] += 1
        src = ", ".join(r["_src"] for r in group)
        try:
            local = parse_time(time_text)
        except ValueError:
            report.skip("PUMP", "unreadable time", f"{src}: {time_text!r}")
            continue
        oz = 0.0
        left = right = False
        duration = 0
        notes = []
        for r in group:
            la, ra, ta = num(r["Left Amount (oz.)"]), num(r["Right Amount (oz.)"]), num(r["Total amount (oz.)"])
            ld, rd = int(num(r["Left duration (min)"])), int(num(r["Right Duration (min)"]))
            if la or ra:
                oz += la + ra
                if ta and abs(ta - (la + ra)) > 0.01:
                    report.warnings.append(f"{src}: {time_text} total {ta:g} oz != left+right {la + ra:g} oz; used left+right")
            else:
                oz += ta
            left |= bool(la or ld)
            right |= bool(ra or rd)
            duration = max(duration, ld, rd)
            if r["Note"].strip():
                notes.append(r["Note"].strip())
        if time_text in fixes:
            oz = fixes[time_text]
        if oz <= 0:
            report.skip("PUMP", "no amount (minutes only)", f"{src}: {time_text}")
            continue
        if oz > MAX_PUMP_OZ:
            report.skip("PUMP", f"implausible amount (> {MAX_PUMP_OZ:g} oz); fix with --pump-fix", f"{src}: {time_text} {oz:g} oz")
            continue
        if duration and not 1 <= duration <= 180:
            report.warnings.append(f"{src}: {time_text} duration {duration} min is outside 1-180; stored without duration")
            duration = 0
        side = "BOTH" if left and right else "LEFT" if left else "RIGHT" if right else None
        ml = round(oz * ML_PER_OZ, 3)
        out.append(NewRow("pump_session", "PUMP", make_id(seen, "PUMP", local, f"{ml}"), local, to_ms(local, zone),
                          epoch_day(local),
                          {"volume_ml": ml, "side": side, "duration_min": duration or None, "note": "; ".join(notes) or None},
                          f"{time_text} pump {oz:g} oz {side or ''} {duration or ''}".rstrip()))
    return out


# ---------------------------------------------------------------- writing to the database

def import_into(con: sqlite3.Connection, rows: list[NewRow], report: Report, write: bool, overlap_min: int) -> list[NewRow]:
    """Returns the rows that are (or, on a dry run, would be) added."""
    baby = con.execute("SELECT id, name, birth_epoch_day FROM baby WHERE deleted_at IS NULL ORDER BY created_at LIMIT 1").fetchone()
    if baby is None:
        sys.exit("error: the app has no baby yet. Finish onboarding in the app first.")
    baby_id, baby_name, birth_day = baby
    print(f"Baby in the app: {baby_name} (born {EPOCH + dt.timedelta(days=birth_day)})")

    ours = {r.id for r in rows}
    existing_ids = {i for (i,) in con.execute("SELECT id FROM log_entry UNION SELECT id FROM pump_session")}
    window = overlap_min * 60 * 1000

    # Entries you logged yourself (not from an earlier run of this script).
    mine_log = [(t, dk, ms, d) for i, t, dk, ms, d in con.execute(
        "SELECT id, type, diaper_kind, occurred_at, local_epoch_day FROM log_entry WHERE baby_id = ? AND deleted_at IS NULL",
        (baby_id,)) if i not in ours]
    mine_pump = [ms for i, ms in con.execute("SELECT id, occurred_at FROM pump_session WHERE deleted_at IS NULL") if i not in ours]

    def overlaps(r: NewRow) -> str | None:
        if r.kind == "PUMP":
            hit = any(abs(ms - r.occurred_ms) <= window for ms in mine_pump)
        elif r.kind == "GROWTH":
            hit = any(t == "GROWTH" and d == r.day for t, _, _, d in mine_log)
        else:
            want_diaper = r.cols.get("diaper_kind")
            hit = any(t == r.kind and dk == want_diaper and ms is not None and abs(ms - r.occurred_ms) <= window
                      for t, dk, ms, _ in mine_log)
        return "overlaps an entry already in the app" if hit else None

    todo = []
    for r in rows:
        if r.id in existing_ids:
            report.already[r.kind] += 1
        elif r.table == "log_entry" and r.day < birth_day:
            report.skip(r.kind, "before the baby's birth date in the app", r.label)
        elif (why := overlaps(r)):
            report.skip(r.kind, why, r.label)
        else:
            todo.append(r)
            report.inserted[r.kind] += 1

    if not write:
        return todo

    now = int(time.time() * 1000)
    mother = con.execute("SELECT id FROM caregiver WHERE role = 'MOTHER' AND deleted_at IS NULL ORDER BY created_at LIMIT 1").fetchone()
    if mother is None and any(r.table == "pump_session" for r in todo):
        mother_id = str(uuid.uuid5(NAMESPACE, "babytracker|mother"))
        con.execute("INSERT INTO caregiver (id, role, name, created_at, updated_at) VALUES (?, 'MOTHER', 'Mom', ?, ?)",
                    (mother_id, now, now))
        print("Created the Mom row.")
    else:
        mother_id = mother[0] if mother else None
    with con:
        for r in todo:
            if r.table == "log_entry":
                c = r.cols
                con.execute(
                    "INSERT OR IGNORE INTO log_entry (id, baby_id, type, occurred_at, local_epoch_day, note, volume_ml,"
                    " diaper_kind, weight_g, length_mm, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    (r.id, baby_id, r.kind, r.occurred_ms, r.day, c.get("note"), c.get("volume_ml"), c.get("diaper_kind"),
                     c.get("weight_g"), c.get("length_mm"), now, now))
            else:
                c = r.cols
                con.execute(
                    "INSERT OR IGNORE INTO pump_session (id, caregiver_id, occurred_at, local_epoch_day, volume_ml, side,"
                    " duration_min, note, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    (r.id, mother_id, r.occurred_ms, r.day, c["volume_ml"], c["side"], c["duration_min"], c["note"], now, now))
    return todo


def write_preview(path: str, rows: list[NewRow], baby_name: str) -> None:
    """A readable picture of what the app will hold: one line per day, then every entry."""
    from decimal import ROUND_HALF_UP, Decimal

    def dec(value: float, places: str) -> str:
        text = format(Decimal(str(value)).quantize(Decimal(places), rounding=ROUND_HALF_UP), "f")
        return text.rstrip("0").rstrip(".") if "." in text else text

    def oz(ml: float) -> float:
        return round(ml / ML_PER_OZ, 2)

    days: dict[dt.date, dict] = collections.defaultdict(
        lambda: {"fed": 0.0, "pumped": 0.0, "sessions": 0, "pee": 0, "potty": 0, "growth": ""})
    entries = []
    for r in sorted(rows, key=lambda r: (r.local, r.kind)):
        d, c = days[r.local.date()], r.cols
        when = r.local.strftime("%I:%M %p").lstrip("0") if r.kind != "GROWTH" else "(date only)"
        if r.kind == "BOTTLE":
            d["fed"] += oz(c["volume_ml"])
            what, detail = "Feed · Bottle", f"{dec(oz(c['volume_ml']), '0.01')} oz"
        elif r.kind == "DIAPER":
            d["pee" if c["diaper_kind"] == "PEE" else "potty"] += 1
            what, detail = f"Diaper · {c['diaper_kind'].title()}", ""
        elif r.kind == "GROWTH":
            lb = dec(c["weight_g"] / GRAMS_PER_LB, "0.1")
            d["growth"] = f"{lb} lb"
            what, detail = "Growth", f"{lb} lb"
        else:
            d["pumped"] += oz(c["volume_ml"])
            d["sessions"] += 1
            what = "Pump · MOM"
            detail = " · ".join(x for x in [f"{dec(oz(c['volume_ml']), '0.01')} oz", (c["side"] or "").title(),
                                            f"{c['duration_min']} min" if c["duration_min"] else ""] if x)
        entries.append((r.local.date(), when, what, detail))

    lines = [f"# Baby Tracker import preview ({len(rows)} entries into {baby_name})", "",
             "## By day", "",
             "| Date | Fed (oz) | Pumped (oz) | Pump sessions | Pee | Potty | Weight |",
             "|---|---:|---:|---:|---:|---:|---|"]
    for day in sorted(days):
        d = days[day]
        lines.append(f"| {day.strftime('%a, %b')} {day.day} | {dec(d['fed'], '0.1') if d['fed'] else ''} | "
                     f"{dec(d['pumped'], '0.1') if d['pumped'] else ''} | {d['sessions'] or ''} | {d['pee'] or ''} | "
                     f"{d['potty'] or ''} | {d['growth']} |")
    lines += ["", "## Every entry", "", "| Date | Time | Entry | Details |", "|---|---|---|---|"]
    lines += [f"| {day.strftime('%b')} {day.day} | {when} | {what} | {detail} |" for day, when, what, detail in entries]
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"Wrote the preview to {path}")


def verify(con: sqlite3.Connection) -> None:
    ok = con.execute("PRAGMA integrity_check").fetchone()[0]
    fk = con.execute("PRAGMA foreign_key_check").fetchall()
    print(f"\nintegrity_check: {ok}; foreign key problems: {len(fk)}")
    if ok != "ok" or fk:
        sys.exit("error: the database failed its checks; NOT pushing it back.")
    print("Rows now in the database:")
    for label, sql in [
        ("log_entry by type", "SELECT type, COUNT(*) FROM log_entry WHERE deleted_at IS NULL GROUP BY type"),
        ("pump_session", "SELECT 'PUMP', COUNT(*) FROM pump_session WHERE deleted_at IS NULL"),
    ]:
        print("  ", ", ".join(f"{k} {n}" for k, n in con.execute(sql)) or "(none)")


# ---------------------------------------------------------------- adb plumbing

def find_adb() -> str:
    for candidate in [shutil.which("adb"),
                      os.path.join(os.environ.get("ANDROID_HOME", ""), "platform-tools", "adb.exe"),
                      os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe")]:
        if candidate and os.path.exists(candidate):
            return candidate
    sys.exit("error: adb not found (put it on PATH or set ANDROID_HOME)")


class Device:
    def __init__(self, serial: str | None):
        self.adb = [find_adb()] + (["-s", serial] if serial else [])

    def run(self, *args: str, binary: bool = False, check: bool = True):
        p = subprocess.run(self.adb + list(args), capture_output=True)
        if check and p.returncode != 0:
            sys.exit(f"error: adb {' '.join(args)}: {p.stderr.decode(errors='replace').strip() or p.stdout.decode(errors='replace').strip()}")
        return p.stdout if binary else p.stdout.decode(errors="replace").strip()

    def timezone(self) -> str:
        return self.run("shell", "getprop", "persist.sys.timezone")

    def check_debuggable(self) -> None:
        out = subprocess.run(self.adb + ["shell", "run-as", PACKAGE, "ls", "databases"], capture_output=True)
        if out.returncode != 0 or DB_NAME.encode() not in out.stdout:
            sys.exit(f"error: can't open {PACKAGE}'s databases. Is a DEBUG build installed and opened once?\n"
                     f"       {(out.stderr or out.stdout).decode(errors='replace').strip()}")

    def pull_db(self, dest_dir: str) -> str:
        files = self.run("shell", "run-as", PACKAGE, "ls", "databases").split()
        for name in (DB_NAME, DB_NAME + "-wal", DB_NAME + "-shm"):
            if name in files:
                data = self.run("exec-out", "run-as", PACKAGE, "cat", f"databases/{name}", binary=True)
                with open(os.path.join(dest_dir, name), "wb") as f:
                    f.write(data)
        return os.path.join(dest_dir, DB_NAME)

    def push_db(self, local: str) -> None:
        tmp = "/data/local/tmp/bs-import.db"
        self.run("push", local, tmp)
        self.run("shell", "run-as", PACKAGE, "cp", tmp, f"databases/{DB_NAME}")
        # Stale write-ahead files would replay old pages over the new file.
        self.run("shell", "run-as", PACKAGE, "rm", "-f", f"databases/{DB_NAME}-wal", f"databases/{DB_NAME}-shm")
        self.run("shell", "rm", "-f", tmp)


# ---------------------------------------------------------------- main

def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("export", help="the Baby Tracker CSV zip, or a folder of the CSVs")
    where = ap.add_mutually_exclusive_group(required=True)
    where.add_argument("--adb", action="store_true", help="import into the app on the attached device (debug build)")
    where.add_argument("--db", help="import into a local copy of baby-status.db")
    ap.add_argument("--serial", help="adb device serial, if more than one is attached")
    ap.add_argument("--zone", help="IANA time zone the Baby Tracker times were recorded in (default: the device's)")
    ap.add_argument("--write", action="store_true", help="really import (the default is a dry run)")
    ap.add_argument("--overlap-min", type=int, default=5, help="skip a row if you already logged the same kind within N minutes")
    ap.add_argument("--pump-fix", action="append", default=[], metavar='"8/15/26 10:50 AM=3"',
                    help="override a pump session's amount in oz (for typos in the export); repeatable")
    ap.add_argument("--preview", metavar="FILE.md", help="also write a readable table of the entries that would be added")
    ap.add_argument("--backup-dir", help="where --write saves the untouched database (default: ./bs-backup-<timestamp>)")
    args = ap.parse_args()

    device = Device(args.serial) if args.adb else None
    zone_name = args.zone or (device.timezone() if device else "")
    if not zone_name:
        sys.exit("error: pass --zone (e.g. America/Los_Angeles); the CSV times have no time zone")
    zone = ZoneInfo(zone_name)
    print(f"Time zone for the Baby Tracker times: {zone_name}" + ("  (taken from the device)" if not args.zone else ""))

    fixes = {}
    for item in args.pump_fix:
        key, _, oz = item.rpartition("=")
        fixes[key.strip()] = float(oz)

    export = read_export(args.export)
    print("Found:", ", ".join(f"{k} {len(v)}" for k, v in sorted(export.items())) or "nothing")
    names = {r["Baby"] for rows in export.values() for r in rows if r.get("Baby")}
    report = Report()
    rows = convert(export, zone, fixes, report)

    work = tempfile.mkdtemp(prefix="bs-import-")
    try:
        if args.adb:
            device.check_debuggable()
            if args.write:
                device.run("shell", "am", "force-stop", PACKAGE)
            db_path = device.pull_db(work)
        else:
            db_path = args.db

        if args.write:
            backup = args.backup_dir or f"bs-backup-{time.strftime('%Y%m%d-%H%M%S')}"
            os.makedirs(backup, exist_ok=True)
            for suffix in ("", "-wal", "-shm"):
                if os.path.exists(db_path + suffix):
                    shutil.copy2(db_path + suffix, os.path.join(backup, DB_NAME + suffix))
            print(f"Backed up the untouched database to {os.path.abspath(backup)}")

        con = sqlite3.connect(db_path)
        con.execute("PRAGMA foreign_keys = ON")
        if names:
            print(f"Baby in the export: {', '.join(sorted(names))} (rows go to the app's baby)")
        added = import_into(con, rows, report, args.write, args.overlap_min)
        report.print(dry_run=not args.write)
        if args.preview:
            baby_name = con.execute("SELECT name FROM baby WHERE deleted_at IS NULL ORDER BY created_at LIMIT 1").fetchone()[0]
            write_preview(args.preview, added, baby_name)

        if args.write:
            con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
            verify(con)
            con.close()
            if args.adb:
                device.push_db(db_path)
                print("\nPushed to the device. Open the app to see the data.")
            else:
                print(f"\nWrote {db_path}")
        else:
            con.close()
            print("\nDry run: nothing was written. Add --write to import.")
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    main()
