package com.sagar.babystatus.core.domain

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class CalculateAgeUseCaseTest {
    private val calculateAge = CalculateAgeUseCase()
    private val birth = LocalDate.of(2026, 6, 12)

    @Test
    fun `born today is zero days`() {
        assertEquals(BabyAge.Days(0), calculateAge(birth, birth))
    }

    @Test
    fun `a date before the birth date is treated as newborn`() {
        assertEquals(BabyAge.Days(0), calculateAge(birth, birth.minusDays(3)))
    }

    @Test
    fun `under two weeks counts days`() {
        assertEquals(BabyAge.Days(13), calculateAge(birth, birth.plusDays(13)))
    }

    @Test
    fun `under a month counts weeks and days`() {
        assertEquals(BabyAge.Weeks(weeks = 2, days = 0), calculateAge(birth, birth.plusDays(14)))
        assertEquals(BabyAge.Weeks(weeks = 4, days = 1), calculateAge(birth, birth.plusDays(29)))
    }

    @Test
    fun `months and weeks, as the design shows`() {
        // Jun 12 -> Sep 24: 3 months, 12 days -> "3 mo, 1 wk"
        assertEquals(BabyAge.Months(months = 3, weeks = 1), calculateAge(birth, LocalDate.of(2026, 9, 24)))
    }

    @Test
    fun `month end birthdays don't skip a month`() {
        val jan31 = LocalDate.of(2026, 1, 31)
        // Feb 28 is not yet a full month.
        assertEquals(BabyAge.Weeks(weeks = 4, days = 0), calculateAge(jan31, LocalDate.of(2026, 2, 28)))
        assertEquals(BabyAge.Months(months = 1, weeks = 0), calculateAge(jan31, LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `leap day birthday turns one on March 1 in common years`() {
        val leap = LocalDate.of(2028, 2, 29)
        assertEquals(BabyAge.Months(months = 11, weeks = 4), calculateAge(leap, LocalDate.of(2029, 2, 28)))
        assertEquals(BabyAge.Months(months = 12, weeks = 0), calculateAge(leap, LocalDate.of(2029, 3, 1)))
    }

    @Test
    fun `two years and up counts years and months`() {
        assertEquals(BabyAge.Years(years = 2, months = 3), calculateAge(birth, LocalDate.of(2028, 9, 20)))
    }
}
