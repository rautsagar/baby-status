package com.sagar.babystatus.core.data.model

import com.sagar.babystatus.core.database.model.TimelineRow
import com.sagar.babystatus.core.model.BreastSide
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.Volume
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PumpingMappersTest {
    private val at = Instant.parse("2026-09-24T17:10:00Z")
    private val day = LocalDate.of(2026, 9, 24)

    private fun row(source: String, type: String, occurredAt: Long? = at.toEpochMilli()) = TimelineRow(
        source = source, id = "x", type = type, occurredAt = occurredAt, localEpochDay = day.toEpochDay(),
        createdAt = 1, updatedAt = 1, note = null, volumeMl = null, food = null, solidsAmount = null,
        reaction = null, diaperKind = null, weightGrams = null, lengthMm = null, side = null, durationMin = null,
    )

    @Test
    fun `pump sessions survive a round trip`() {
        listOf(
            PumpSession("p1", at, day, "Froze 2 oz", Volume.ofOz(4.0), BreastSide.BOTH, Duration.ofMinutes(15)),
            PumpSession("p2", at, day, null, Volume(150.0), side = null, duration = null),
        ).forEach { assertEquals(it, it.toEntity("mother-1", createdAt = 1, updatedAt = 2).toModel()) }
    }

    @Test
    fun `timeline rows map to the entry of their source`() {
        val pump = row(TimelineRow.SOURCE_PUMP, "PUMP").copy(volumeMl = 120.0, side = "LEFT", durationMin = 20)
        assertEquals(PumpSession("x", at, day, null, Volume(120.0), BreastSide.LEFT, Duration.ofMinutes(20)), pump.toModelOrNull())

        val diaper = row(TimelineRow.SOURCE_LOG, "DIAPER").copy(diaperKind = "PEE")
        assertEquals(DiaperChange("x", at, day, null, DiaperKind.PEE), diaper.toModelOrNull())

        val growth = row(TimelineRow.SOURCE_LOG, "GROWTH", occurredAt = null).copy(weightGrams = 6000)
        assertEquals(GrowthMeasurement("x", day, null, Mass(6000), null), growth.toModelOrNull())
    }

    @Test
    fun `malformed timeline rows map to null`() {
        assertNull(row(TimelineRow.SOURCE_PUMP, "PUMP").toModelOrNull(), "pump without a volume")
        assertNull(row(TimelineRow.SOURCE_PUMP, "PUMP", occurredAt = null).copy(volumeMl = 90.0).toModelOrNull(), "pump without a time")
        assertNull(row("NAP", "NAP").toModelOrNull(), "unknown source")
    }
}
