package com.sagar.babystatus.core.model

import java.time.LocalDate

/**
 * One day's totals, as History's day header and the Summary screen show them. Always built
 * from the day's entries by [summarize], so every screen agrees.
 */
data class DaySummary(
    val date: LocalDate,
    /** Bottles only: solids have no volume. */
    val fed: Volume,
    val bottleCount: Int,
    val solidsCount: Int,
    val pumped: Volume,
    val pumpSessions: Int,
    val pee: Int,
    val potty: Int,
    /** Every entry that day, including growth, which none of the totals above count. */
    val entryCount: Int,
) {
    val diapers: Int get() = pee + potty

    /** Whether History has a card for this day. */
    val hasEntries: Boolean get() = entryCount > 0
}

/** Everything logged on one local day, in timeline order (newest first). */
data class TimelineDay(
    val date: LocalDate,
    val entries: List<TimelineEntry>,
) {
    val summary: DaySummary = summarize(date, entries)
}

fun summarize(date: LocalDate, entries: List<TimelineEntry>): DaySummary {
    var fedMl = 0.0
    var bottles = 0
    var solids = 0
    var pumpedMl = 0.0
    var sessions = 0
    var pee = 0
    var potty = 0
    for (entry in entries) {
        when (entry) {
            is BottleFeed -> {
                fedMl += entry.volume.ml
                bottles++
            }
            is SolidsFeed -> solids++
            is DiaperChange -> if (entry.kind == DiaperKind.PEE) pee++ else potty++
            is PumpSession -> {
                pumpedMl += entry.volume.ml
                sessions++
            }
            is GrowthMeasurement -> Unit
        }
    }
    return DaySummary(
        date = date,
        fed = Volume(fedMl),
        bottleCount = bottles,
        solidsCount = solids,
        pumped = Volume(pumpedMl),
        pumpSessions = sessions,
        pee = pee,
        potty = potty,
        entryCount = entries.size,
    )
}
