package com.sagar.babystatus.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * A grown-up in the household. For now only the mother exists, as the owner of pumping
 * sessions; it isn't tied to a baby (with twins, one mother pumps for both).
 */
data class Caregiver(
    val id: String,
    val role: CaregiverRole,
    val name: String,
)

enum class CaregiverRole { MOTHER }

/** Mom's pumping. On the timeline, but not a [LogEntry]: it isn't about the baby. */
data class PumpSession(
    override val id: String,
    val occurredAt: Instant,
    override val date: LocalDate,
    override val note: String?,
    val volume: Volume,
    val side: BreastSide?,
    val duration: Duration?,
) : TimelineEntry

enum class BreastSide { LEFT, RIGHT, BOTH }

/** What the Today card shows: "14 oz today · last 3h ago". */
data class PumpSummary(
    val pumpedToday: Volume,
    val last: PumpSession?,
)

/** When it happened, for everything with a time of day; null for date-only growth. */
val TimelineEntry.occurredAtOrNull: Instant?
    get() = when (this) {
        is TimedEntry -> occurredAt
        is PumpSession -> occurredAt
        is GrowthMeasurement -> null
    }
