package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.MutableClock
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ValidatePumpSessionUseCaseTest {
    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val validate = ValidatePumpSessionUseCase(clock)

    private fun session(offset: Duration = Duration.ofMinutes(-10), ml: Double = 120.0, duration: Duration? = null) =
        PumpSession("p", clock.now.plus(offset), LocalDate.of(2026, 9, 24), null, Volume(ml), null, duration)

    @Test
    fun `a valid session has no errors`() {
        assertEquals(emptySet(), validate(session(duration = Duration.ofMinutes(15))))
    }

    @Test
    fun `no birth-date bound, but no future either`() {
        assertEquals(emptySet(), validate(session(offset = Duration.ofDays(-400))), "pumping before the birth is fine")
        assertEquals(setOf(PumpSessionError.IN_FUTURE), validate(session(offset = Duration.ofMinutes(5))))
    }

    @Test
    fun `amount and duration limits`() {
        assertEquals(setOf(PumpSessionError.NO_AMOUNT), validate(session(ml = 0.0)))
        assertEquals(setOf(PumpSessionError.DURATION_OUT_OF_RANGE), validate(session(duration = Duration.ofMinutes(181))))
        assertEquals(setOf(PumpSessionError.DURATION_OUT_OF_RANGE), validate(session(duration = Duration.ZERO)))
    }
}
