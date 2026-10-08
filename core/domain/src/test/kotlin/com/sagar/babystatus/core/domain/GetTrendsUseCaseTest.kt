package com.sagar.babystatus.core.domain

import app.cash.turbine.test
import com.sagar.babystatus.core.domain.GetTrendsUseCase.Companion.TREND_DAYS
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakePumpRepository
import com.sagar.babystatus.core.testing.FakeTimelineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetTrendsUseCaseTest {
    private val today = LocalDate.of(2026, 9, 24)
    private val baby = Baby("baby-1", "Ava", LocalDate.of(2026, 6, 12), null)

    private fun at(date: LocalDate) = Instant.parse("${date}T18:00:00Z")
    private fun bottle(id: String, date: LocalDate, oz: Double) = BottleFeed(id, at(date), date, null, Volume.ofOz(oz))
    private fun pump(id: String, date: LocalDate, oz: Double) = PumpSession(id, at(date), date, null, Volume.ofOz(oz), null, null)

    private fun useCase(babies: FakeBabyRepository, log: FakeLogRepository = FakeLogRepository(), pump: FakePumpRepository = FakePumpRepository()) =
        GetTrendsUseCase(babies, FakeTimelineRepository(log, pump))

    @Test
    fun `nothing until there is a baby`() = runTest {
        useCase(FakeBabyRepository()).invoke(today).test { assertNull(awaitItem()) }
    }

    @Test
    fun `thirty calendar days, oldest first, ending today`() = runTest {
        useCase(FakeBabyRepository(baby)).invoke(today).test {
            val days = awaitItem()!!.days
            assertEquals(TREND_DAYS, days.size)
            assertEquals(today.minusDays(29), days.first().date)
            assertEquals(today, days.last().date)
            assertEquals((29L downTo 0L).map { today.minusDays(it) }, days.map { it.date })
        }
    }

    @Test
    fun `days with nothing logged are present and zero`() = runTest {
        val log = FakeLogRepository(listOf(bottle("b0", today, 3.0)))
        useCase(FakeBabyRepository(baby), log).invoke(today).test {
            val empty = awaitItem()!!.days.dropLast(1)
            assertEquals(TREND_DAYS - 1, empty.size)
            assertTrue(empty.none { it.hasEntries })
            assertTrue(empty.all { it.fed.ml == 0.0 && it.pumped.ml == 0.0 && it.diapers == 0 && it.weight == null })
        }
    }

    @Test
    fun `the window is clipped at the birth date`() = runTest {
        val newborn = baby.copy(birthDate = today.minusDays(4))
        useCase(FakeBabyRepository(newborn)).invoke(today).test {
            assertEquals((4L downTo 0L).map { today.minusDays(it) }, awaitItem()!!.days.map { it.date })
        }
    }

    @Test
    fun `the day totals match what was logged`() = runTest {
        val tenDaysAgo = today.minusDays(10)
        val log = FakeLogRepository(
            listOf(
                bottle("b0", today, 3.0),
                bottle("b1", today, 4.5),
                bottle("b10", tenDaysAgo, 6.0),
                DiaperChange("d10", at(tenDaysAgo), tenDaysAgo, null, DiaperKind.PEE),
                DiaperChange("d11", at(tenDaysAgo), tenDaysAgo, null, DiaperKind.POTTY),
                GrowthMeasurement("g10", tenDaysAgo, null, Mass(6100), null),
                bottle("old", today.minusDays(30), 9.0), // outside the window
            ),
        )
        val pump = FakePumpRepository(listOf(pump("p0", today, 4.0)))
        useCase(FakeBabyRepository(baby), log, pump).invoke(today).test {
            val days = awaitItem()!!.days.associateBy { it.date }
            assertEquals(7.5, days.getValue(today).fed.oz, 0.001)
            assertEquals(4.0, days.getValue(today).pumped.oz, 0.001)
            assertEquals(6.0, days.getValue(tenDaysAgo).fed.oz, 0.001)
            assertEquals(2, days.getValue(tenDaysAgo).diapers)
            assertEquals(Mass(6100), days.getValue(tenDaysAgo).weight)
            assertEquals(0.0, days.getValue(today.minusDays(29)).fed.ml, "the 30-day-old bottle isn't in the window")
        }
    }
}
