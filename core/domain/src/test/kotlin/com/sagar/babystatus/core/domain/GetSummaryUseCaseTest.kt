package com.sagar.babystatus.core.domain

import app.cash.turbine.test
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetSummaryUseCaseTest {
    private val today = LocalDate.of(2026, 9, 24)
    private val baby = Baby("baby-1", "Ava", LocalDate.of(2026, 6, 12), null)

    private fun at(date: LocalDate) = Instant.parse("${date}T18:00:00Z")
    private fun bottle(id: String, date: LocalDate, oz: Double) = BottleFeed(id, at(date), date, null, Volume.ofOz(oz))
    private fun pump(id: String, date: LocalDate, oz: Double) = PumpSession(id, at(date), date, null, Volume.ofOz(oz), null, null)

    private fun useCase(babies: FakeBabyRepository, log: FakeLogRepository = FakeLogRepository(), pump: FakePumpRepository = FakePumpRepository()) =
        GetSummaryUseCase(babies, FakeTimelineRepository(log, pump))

    @Test
    fun `nothing until there is a baby`() = runTest {
        useCase(FakeBabyRepository()).invoke(today).test { assertNull(awaitItem()) }
    }

    @Test
    fun `today and the five calendar days before it, empty days included`() = runTest {
        val log = FakeLogRepository(
            listOf(
                bottle("b0", today, 3.0),
                bottle("b1", today.minusDays(1), 6.0),
                bottle("b3", today.minusDays(3), 5.0),
                DiaperChange("d3", at(today.minusDays(3)), today.minusDays(3), null, DiaperKind.PEE),
                bottle("old", today.minusDays(6), 9.0), // outside the window
            ),
        )
        val pump = FakePumpRepository(listOf(pump("p0", today, 4.0)))
        useCase(FakeBabyRepository(baby), log, pump).invoke(today).test {
            val summary = awaitItem()!!
            assertEquals(3.0, summary.today.fed.oz, 0.001)
            assertEquals(4.0, summary.today.pumped.oz, 0.001)

            assertEquals((1..5).map { today.minusDays(it.toLong()) }, summary.previous.map { it.date })
            assertEquals(6.0, summary.previous[0].fed.oz, 0.001)
            assertFalse(summary.previous[1].hasEntries, "nothing logged two days ago")
            assertEquals(1, summary.previous[2].diapers)
            assertTrue(summary.previous.drop(3).none { it.hasEntries }, "the 6-day-old bottle isn't shown")
        }
    }

    @Test
    fun `a day with nothing logged today still has a today card`() = runTest {
        useCase(FakeBabyRepository(baby)).invoke(today).test {
            val summary = awaitItem()!!
            assertFalse(summary.today.hasEntries)
            assertEquals(0.0, summary.today.fed.ml)
        }
    }

    @Test
    fun `days before the birth date are left out`() = runTest {
        val newborn = baby.copy(birthDate = today.minusDays(2))
        useCase(FakeBabyRepository(newborn)).invoke(today).test {
            assertEquals(listOf(today.minusDays(1), today.minusDays(2)), awaitItem()!!.previous.map { it.date })
        }
    }

    @Test
    fun `a baby born today has no previous days`() = runTest {
        useCase(FakeBabyRepository(baby.copy(birthDate = today))).invoke(today).test {
            assertEquals(emptyList(), awaitItem()!!.previous)
        }
    }
}
