package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.SolidsFeed
import com.sagar.babystatus.core.model.TimedEntry
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class LogEntryError {
    IN_FUTURE,
    BEFORE_BIRTH,
    MISSING_FOOD,
    MISSING_MEASUREMENT,
}

/**
 * Rules every entry must pass before it's saved. Form-level gaps (like no amount picked
 * yet for solids) are the form's job; this checks a fully built entry.
 */
class ValidateLogEntryUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(entry: LogEntry, birthDate: LocalDate): Set<LogEntryError> = buildSet {
        val today = LocalDate.now(clock)
        when (entry) {
            is TimedEntry -> if (entry.occurredAt.isAfter(clock.instant())) add(LogEntryError.IN_FUTURE)
            is GrowthMeasurement -> if (entry.date.isAfter(today)) add(LogEntryError.IN_FUTURE)
        }
        if (entry.date.isBefore(birthDate)) add(LogEntryError.BEFORE_BIRTH)
        when (entry) {
            is SolidsFeed -> if (entry.food.isBlank()) add(LogEntryError.MISSING_FOOD)
            is GrowthMeasurement -> if (entry.weight == null && entry.length == null) {
                add(LogEntryError.MISSING_MEASUREMENT)
            }
            is BottleFeed, is DiaperChange -> Unit
        }
    }
}
