package com.sagar.babystatus.core.domain

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** A baby's age at the precision parents actually talk about. Formatted by the UI. */
sealed interface BabyAge {
    /** Under two weeks: "12 days". */
    data class Days(val days: Int) : BabyAge

    /** Under a month: "3 wk, 2 d". */
    data class Weeks(val weeks: Int, val days: Int) : BabyAge

    /** Under two years: "3 mo, 1 wk". */
    data class Months(val months: Int, val weeks: Int) : BabyAge

    /** "2 yr, 3 mo". */
    data class Years(val years: Int, val months: Int) : BabyAge
}

class CalculateAgeUseCase @Inject constructor() {
    operator fun invoke(birthDate: LocalDate, today: LocalDate): BabyAge {
        if (!today.isAfter(birthDate)) return BabyAge.Days(0)
        val totalDays = ChronoUnit.DAYS.between(birthDate, today).toInt()
        // Period handles month ends: Jan 31 -> Feb 28 is 0 months, 28 days.
        val period = Period.between(birthDate, today)
        val totalMonths = period.toTotalMonths().toInt()
        return when {
            totalDays < 14 -> BabyAge.Days(totalDays)
            totalMonths == 0 -> BabyAge.Weeks(weeks = totalDays / 7, days = totalDays % 7)
            totalMonths < 24 -> BabyAge.Months(months = totalMonths, weeks = period.days / 7)
            else -> BabyAge.Years(years = period.years, months = period.months)
        }
    }
}
