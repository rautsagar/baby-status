package com.sagar.babystatus.core.model

import java.time.Instant
import java.time.LocalDate

/** Anything that shows on the History timeline: the baby's entries and Mom's pumping. */
sealed interface TimelineEntry {
    val id: String

    /** The local day the entry belongs to; used for grouping and daily totals. */
    val date: LocalDate
    val note: String?
}

/**
 * Anything logged for the baby. Entries belong to the single baby; the data layer
 * attaches the baby id, so the UI never passes it around.
 */
sealed interface LogEntry : TimelineEntry

/** An entry logged at a point in time (everything except growth). */
sealed interface TimedEntry : LogEntry {
    val occurredAt: Instant
}

data class BottleFeed(
    override val id: String,
    override val occurredAt: Instant,
    override val date: LocalDate,
    override val note: String?,
    val volume: Volume,
) : TimedEntry

data class SolidsFeed(
    override val id: String,
    override val occurredAt: Instant,
    override val date: LocalDate,
    override val note: String?,
    val food: String,
    val amount: SolidsAmount,
    val reaction: Reaction?,
) : TimedEntry

data class DiaperChange(
    override val id: String,
    override val occurredAt: Instant,
    override val date: LocalDate,
    override val note: String?,
    val kind: DiaperKind,
) : TimedEntry

/** Date only: measurements have no time of day. At least one of [weight] / [length] is set. */
data class GrowthMeasurement(
    override val id: String,
    override val date: LocalDate,
    override val note: String?,
    val weight: Mass?,
    val length: Length?,
) : LogEntry

/** The two things the Today tiles and the log sheet choose between. */
enum class ActivityType { FEED, DIAPER }

enum class SolidsAmount { TASTE, SOME, LOTS }

enum class Reaction { LIKED, DISLIKED, ALLERGIC }

enum class DiaperKind { PEE, POTTY }

val TimedEntry.activityType: ActivityType
    get() = when (this) {
        is BottleFeed, is SolidsFeed -> ActivityType.FEED
        is DiaperChange -> ActivityType.DIAPER
    }
