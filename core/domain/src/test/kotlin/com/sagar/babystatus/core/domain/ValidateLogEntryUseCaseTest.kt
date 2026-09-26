package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.SolidsAmount
import com.sagar.babystatus.core.model.SolidsFeed
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.MutableClock
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ValidateLogEntryUseCaseTest {
    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val validate = ValidateLogEntryUseCase(clock)
    private val today = LocalDate.of(2026, 9, 24)
    private val birth = LocalDate.of(2026, 6, 12)

    private fun bottleAt(offset: Duration) = clock.now.plus(offset).let {
        BottleFeed("b", it, it.atZone(clock.zone).toLocalDate(), null, Volume.ofOz(3.0))
    }

    @Test
    fun `a valid entry has no errors`() {
        assertEquals(emptySet(), validate(bottleAt(Duration.ofMinutes(-10)), birth))
    }

    @Test
    fun `timed entries can't be in the future`() {
        assertEquals(setOf(LogEntryError.IN_FUTURE), validate(bottleAt(Duration.ofMinutes(5)), birth))
    }

    @Test
    fun `entries can't predate the birth`() {
        val entry = bottleAt(Duration.ofDays(-200))
        assertEquals(setOf(LogEntryError.BEFORE_BIRTH), validate(entry, birth))
    }

    @Test
    fun `solids need a food`() {
        val entry = SolidsFeed("s", clock.now, today, null, food = "  ", amount = SolidsAmount.SOME, reaction = null)
        assertEquals(setOf(LogEntryError.MISSING_FOOD), validate(entry, birth))
    }

    @Test
    fun `growth needs a measurement and a date that has happened`() {
        val empty = GrowthMeasurement("g", today.plusDays(1), null, weight = null, length = null)
        assertEquals(setOf(LogEntryError.IN_FUTURE, LogEntryError.MISSING_MEASUREMENT), validate(empty, birth))

        val ok = GrowthMeasurement("g", today, null, weight = Mass.ofLb(13.2), length = null)
        assertEquals(emptySet(), validate(ok, birth))
    }
}
