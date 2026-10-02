package com.sagar.babystatus.core.ui.format

import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.test.assertEquals

class FormattersTest {
    @Before
    fun useUsLocale() = Locale.setDefault(Locale.US)

    @Test
    fun `volumes round to half ounces or 5 ml`() {
        assertEquals("3 oz", UnitFormatter.volume(Volume.ofOz(3.0), UnitSystem.IMPERIAL))
        assertEquals("17.5 oz", UnitFormatter.volume(Volume.ofOz(17.4), UnitSystem.IMPERIAL))
        assertEquals("90 ml", UnitFormatter.volume(Volume.ofOz(3.0), UnitSystem.METRIC))
    }

    @Test
    fun `volume parts split the number from the unit and agree with the full string`() {
        assertEquals(VolumeParts("18", "oz"), UnitFormatter.volumeParts(Volume.ofOz(18.0), UnitSystem.IMPERIAL))
        assertEquals(VolumeParts("17.5", "oz"), UnitFormatter.volumeParts(Volume.ofOz(17.4), UnitSystem.IMPERIAL))
        assertEquals(VolumeParts("0", "oz"), UnitFormatter.volumeParts(Volume.ofOz(0.0), UnitSystem.IMPERIAL))
        assertEquals(VolumeParts("90", "ml"), UnitFormatter.volumeParts(Volume.ofOz(3.0), UnitSystem.METRIC))
    }

    @Test
    fun `mass and length keep one decimal`() {
        assertEquals("13.2 lb", UnitFormatter.mass(Mass.ofLb(13.2), UnitSystem.IMPERIAL))
        assertEquals("6.0 kg", UnitFormatter.mass(Mass(5987), UnitSystem.METRIC))
        assertEquals("23.5 in", UnitFormatter.length(Length.ofInches(23.5), UnitSystem.IMPERIAL))
        assertEquals("59.7 cm", UnitFormatter.length(Length.ofInches(23.5), UnitSystem.METRIC))
    }

    @Test
    fun `decimals parse with a dot or a comma`() {
        assertEquals(13.2, parseDecimal("13,2"))
        assertEquals(13.2, parseDecimal(" 13.2 "))
        assertEquals(null, parseDecimal("abc"))
    }

    @Test
    fun `bottle scale per unit`() {
        val oz = VolumeScale.forUnits(UnitSystem.IMPERIAL)
        assertEquals(3.0, oz.default)
        assertEquals(17, oz.sliderSteps) // 1, 1.5, … 10 = 19 stops, 17 between the ends
        assertEquals(3.0, oz.fromVolume(oz.toVolume(3.0)), "round trip snaps back")
        assertEquals(10.0, oz.fromVolume(Volume.ofOz(14.0)), "clamped to the max")

        val ml = VolumeScale.forUnits(UnitSystem.METRIC)
        assertEquals(90.0, ml.default)
        assertEquals(90.0, ml.fromVolume(Volume.ofOz(3.0)), "3 oz is 88.7 ml, shown as 90")
        assertEquals("3.5", oz.label(3.5))
        assertEquals("90", ml.label(90.0))
    }

    @Test
    fun `time ago`() {
        val now = Instant.parse("2026-09-24T19:05:00Z")
        assertEquals("Just now", TimeFormatter.timeAgo(now.minusSeconds(30), now))
        assertEquals("Just now", TimeFormatter.timeAgo(now.plusSeconds(90), now))
        assertEquals("25m ago", TimeFormatter.timeAgo(now.minus(Duration.ofMinutes(25)), now))
        assertEquals("2h 25m ago", TimeFormatter.timeAgo(now.minus(Duration.ofMinutes(145)), now))
        assertEquals("3h ago", TimeFormatter.timeAgo(now.minus(Duration.ofHours(3)), now))
        assertEquals("1d 2h ago", TimeFormatter.timeAgo(now.minus(Duration.ofHours(26)), now))
        assertEquals("3d ago", TimeFormatter.timeAgo(now.minus(Duration.ofDays(3)), now))
    }

    @Test
    fun `day labels`() {
        val today = LocalDate.of(2026, 9, 24)
        assertEquals("Today", TimeFormatter.dayLabel(today, today))
        assertEquals("Yesterday", TimeFormatter.dayLabel(today.minusDays(1), today))
        assertEquals("Tue, Sep 22", TimeFormatter.dayLabel(today.minusDays(2), today))
        assertEquals("Wed, Dec 31, 2025", TimeFormatter.dayLabel(LocalDate.of(2025, 12, 31), today))
        assertEquals("Today, Sep 24", TimeFormatter.dateWithDay(today, today))
    }

    @Test
    fun `clock times`() {
        val zone = ZoneId.of("America/New_York")
        val at = LocalDate.of(2026, 9, 24).atTime(14, 5).atZone(zone).toInstant()
        assertEquals("2:05 PM", TimeFormatter.clockTime(at, zone, is24Hour = false))
        assertEquals("2:05p", TimeFormatter.compactClockTime(at, zone, is24Hour = false))
        assertEquals("14:05", TimeFormatter.compactClockTime(at, zone, is24Hour = true))
    }
}
