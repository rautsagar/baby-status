package com.sagar.babystatus.core.ui.format

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates and times as the design writes them. English day words, device-locale month names. */
object TimeFormatter {
    // Built per call so a language change while the app runs is picked up.
    private val monthDay get() = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val weekdayMonthDay get() = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val weekdayMonthDayYear get() = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
    private val monthDayYear get() = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val clock12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val clock24 get() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    /** "2:05 PM" or "14:05". */
    fun clockTime(instant: Instant, zone: ZoneId, is24Hour: Boolean): String =
        (if (is24Hour) clock24 else clock12).format(instant.atZone(zone))

    /** History's narrow column: "2:05p" or "14:05". */
    fun compactClockTime(instant: Instant, zone: ZoneId, is24Hour: Boolean): String {
        if (is24Hour) return clock24.format(instant.atZone(zone))
        val time = clock12.format(instant.atZone(zone))
        return time.replace(" AM", "a").replace(" PM", "p")
    }

    /** History day headers: "Today", "Yesterday", "Mon, Sep 22" (with the year if not this year). */
    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (date.year == today.year) weekdayMonthDay.format(date) else weekdayMonthDayYear.format(date)
    }

    /** Today's header and the growth date field: "Today, Sep 24" / "Yesterday, Sep 23" / "Mon, Sep 22". */
    fun dateWithDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today, ${monthDay.format(date)}"
        today.minusDays(1) -> "Yesterday, ${monthDay.format(date)}"
        else -> dayLabel(date, today)
    }

    /** The log sheet's "When": "Today, 3:05 PM". */
    fun dateTime(instant: Instant, zone: ZoneId, today: LocalDate, is24Hour: Boolean): String =
        "${dayLabel(instant.atZone(zone).toLocalDate(), today)}, ${clockTime(instant, zone, is24Hour)}"

    /** "Sep 24". */
    fun shortDate(date: LocalDate): String = monthDay.format(date)

    /** "Jun 12, 2026". */
    fun fullDate(date: LocalDate): String = monthDayYear.format(date)

    /** Time since an event: "Just now", "25m ago", "2h 25m ago", "3d ago". */
    fun timeAgo(then: Instant, now: Instant): String {
        val elapsed = Duration.between(then, now)
        if (elapsed.isNegative || elapsed.toMinutes() < 1) return "Just now"
        val days = elapsed.toDays()
        val hours = elapsed.toHours() % 24
        val minutes = elapsed.toMinutes() % 60
        return when {
            days >= 2 -> "${days}d ago"
            days == 1L -> if (hours == 0L) "1d ago" else "1d ${hours}h ago"
            elapsed.toHours() >= 1 -> if (minutes == 0L) "${hours}h ago" else "${hours}h ${minutes}m ago"
            else -> "${minutes}m ago"
        }
    }
}
