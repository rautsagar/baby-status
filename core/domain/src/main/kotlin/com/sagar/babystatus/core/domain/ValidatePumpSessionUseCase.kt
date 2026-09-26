package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.model.PumpSession
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

enum class PumpSessionError {
    IN_FUTURE,
    NO_AMOUNT,
    DURATION_OUT_OF_RANGE,
}

/**
 * Rules for Mom's pumping. Unlike the baby's entries there's no birth-date lower bound:
 * some mothers pump before the birth.
 */
class ValidatePumpSessionUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(session: PumpSession): Set<PumpSessionError> = buildSet {
        if (session.occurredAt.isAfter(clock.instant())) add(PumpSessionError.IN_FUTURE)
        if (session.volume.ml <= 0) add(PumpSessionError.NO_AMOUNT)
        session.duration?.let { if (it !in MIN_DURATION..MAX_DURATION) add(PumpSessionError.DURATION_OUT_OF_RANGE) }
    }

    companion object {
        val MIN_DURATION: Duration = Duration.ofMinutes(1)
        val MAX_DURATION: Duration = Duration.ofMinutes(180)
    }
}
