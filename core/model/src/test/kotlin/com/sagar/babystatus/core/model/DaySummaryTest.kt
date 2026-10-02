package com.sagar.babystatus.core.model

import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DaySummaryTest {
    private val day = LocalDate.of(2026, 9, 24)
    private val at = Instant.parse("2026-09-24T18:05:00Z")

    private fun bottle(id: String, oz: Double) = BottleFeed(id, at, day, null, Volume.ofOz(oz))
    private fun solids(id: String) = SolidsFeed(id, at, day, null, "Banana", SolidsAmount.SOME, null)
    private fun diaper(id: String, kind: DiaperKind) = DiaperChange(id, at, day, null, kind)
    private fun pump(id: String, oz: Double) = PumpSession(id, at, day, null, Volume.ofOz(oz), null, null)
    private val growth = GrowthMeasurement("g", day, null, Mass(6000), null)

    @Test
    fun `totals and counts, as the Summary card shows them`() {
        val summary = summarize(
            day,
            listOf(
                bottle("b1", 5.0), bottle("b2", 4.0), bottle("b3", 4.5), bottle("b4", 4.5), solids("s1"),
                pump("p1", 4.0), pump("p2", 5.0), pump("p3", 5.0),
                diaper("d1", DiaperKind.PEE), diaper("d2", DiaperKind.PEE), diaper("d3", DiaperKind.PEE),
                diaper("d4", DiaperKind.PEE), diaper("d5", DiaperKind.POTTY), diaper("d6", DiaperKind.POTTY),
            ),
        )
        assertEquals(18.0, summary.fed.oz, 0.001)
        assertEquals(4, summary.bottleCount)
        assertEquals(1, summary.solidsCount)
        assertEquals(14.0, summary.pumped.oz, 0.001)
        assertEquals(3, summary.pumpSessions)
        assertEquals(4, summary.pee)
        assertEquals(2, summary.potty)
        assertEquals(6, summary.diapers)
        assertEquals(14, summary.entryCount)
    }

    @Test
    fun `solids add a count but no volume`() {
        val summary = summarize(day, listOf(solids("s1"), solids("s2")))
        assertEquals(0.0, summary.fed.ml)
        assertEquals(2, summary.solidsCount)
    }

    @Test
    fun `a growth entry makes the day exist without counting toward any total`() {
        val summary = summarize(day, listOf(growth))
        assertTrue(summary.hasEntries)
        assertEquals(0, summary.diapers + summary.bottleCount + summary.solidsCount + summary.pumpSessions)
        assertEquals(0.0, summary.fed.ml + summary.pumped.ml)
    }

    @Test
    fun `an empty day has no entries`() {
        val summary = summarize(day, emptyList())
        assertFalse(summary.hasEntries)
        assertEquals(0, summary.entryCount)
        assertEquals(day, summary.date)
    }

    @Test
    fun `a timeline day carries its own summary`() {
        val timelineDay = TimelineDay(day, listOf(bottle("b1", 3.0), diaper("d1", DiaperKind.POTTY)))
        assertEquals(summarize(day, timelineDay.entries), timelineDay.summary)
    }
}
