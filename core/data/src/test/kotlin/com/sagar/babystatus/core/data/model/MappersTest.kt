package com.sagar.babystatus.core.data.model

import com.sagar.babystatus.core.database.model.LogEntryEntity
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.Reaction
import com.sagar.babystatus.core.model.SolidsAmount
import com.sagar.babystatus.core.model.SolidsFeed
import com.sagar.babystatus.core.model.Volume
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MappersTest {
    private val at = Instant.parse("2026-09-24T18:05:00Z")
    private val day = LocalDate.of(2026, 9, 24)

    private fun roundTrip(entry: LogEntry) = entry.toEntity("baby", createdAt = 1, updatedAt = 2).toModelOrNull()

    @Test
    fun `every entry type survives a round trip`() {
        listOf(
            BottleFeed("1", at, day, "fussy", Volume.ofOz(4.5)),
            SolidsFeed("2", at, day, null, "Sweet potato", SolidsAmount.SOME, Reaction.LIKED),
            SolidsFeed("3", at, day, null, "Peas", SolidsAmount.TASTE, reaction = null),
            DiaperChange("4", at, day, null, DiaperKind.POTTY),
            GrowthMeasurement("5", day, "Checkup", Mass(5987), Length(597)),
            GrowthMeasurement("6", day, null, weight = null, length = Length(600)),
        ).forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `growth is stored without a time`() {
        val entity = GrowthMeasurement("g", day, null, Mass(6000), null).toEntity("baby", 1, 1)
        assertNull(entity.occurredAt)
        assertEquals(day.toEpochDay(), entity.localEpochDay)
    }

    @Test
    fun `blank notes are stored as null and food is trimmed`() {
        val entity = SolidsFeed("s", at, day, "   ", " Banana ", SolidsAmount.LOTS, null).toEntity("baby", 1, 1)
        assertNull(entity.note)
        assertEquals("Banana", entity.food)
    }

    @Test
    fun `malformed rows map to null instead of crashing`() {
        val base = LogEntryEntity(
            id = "x", babyId = "baby", type = LogEntryEntity.TYPE_BOTTLE, occurredAt = at.toEpochMilli(),
            localEpochDay = day.toEpochDay(), note = null, createdAt = 1, updatedAt = 1,
        )
        assertNull(base.toModelOrNull(), "bottle without volume")
        assertNull(base.copy(type = LogEntryEntity.TYPE_DIAPER, diaperKind = "BOTH").toModelOrNull(), "unknown diaper kind")
        assertNull(base.copy(type = LogEntryEntity.TYPE_GROWTH).toModelOrNull(), "growth without measurements")
        assertNull(base.copy(type = "NAP").toModelOrNull(), "unknown type")
        assertNull(base.copy(volumeMl = 90.0, occurredAt = null).toModelOrNull(), "timed entry without a time")
    }
}
