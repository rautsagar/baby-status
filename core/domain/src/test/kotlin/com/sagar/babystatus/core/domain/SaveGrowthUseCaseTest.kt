package com.sagar.babystatus.core.domain

import app.cash.turbine.test
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SaveGrowthUseCaseTest {
    private val today = LocalDate.of(2026, 11, 7)

    private fun weight(id: String, daysAgo: Long, lb: Double) =
        GrowthMeasurement(id, today.minusDays(daysAgo), null, Mass.ofLb(lb), length = null)

    private fun measurement(id: String, daysAgo: Long, lb: Double?, inches: Double?) =
        GrowthMeasurement(id, today.minusDays(daysAgo), null, lb?.let(Mass::ofLb), inches?.let(Length::ofInches))

    private fun useCase(log: FakeLogRepository, unitSystem: UnitSystem = UnitSystem.IMPERIAL) =
        SaveGrowthUseCase(log, FakeUserPreferencesRepository(unitSystem))

    @Test
    fun `a heavier weight than the last measurement is a gain`() = runTest {
        val log = FakeLogRepository(listOf(weight("g0", daysAgo = 14, lb = 13.2)))
        val saveGrowth = useCase(log)
        saveGrowth.gains.test {
            saveGrowth(weight("g1", daysAgo = 0, lb = 13.9))
            assertEquals(GrowthGain(Increase(Mass.ofLb(13.2), Mass.ofLb(13.9)), length = null), awaitItem())
        }
        assertEquals(2, log.all.size, "it's saved either way")
    }

    @Test
    fun `the same weight, a dip, or a first measurement is no gain`() = runTest {
        val log = FakeLogRepository(listOf(weight("g0", daysAgo = 14, lb = 13.2)))
        val saveGrowth = useCase(log)
        saveGrowth.gains.test {
            saveGrowth(weight("g1", daysAgo = 0, lb = 13.2))
            saveGrowth(weight("g2", daysAgo = 0, lb = 13.1))
            useCase(FakeLogRepository()).invoke(weight("g3", daysAgo = 0, lb = 9.0))
            expectNoEvents()
        }
    }

    @Test
    fun `values are compared as shown, so a gain never reads plus zero`() = runTest {
        // 13.24 lb and 13.20 lb both show as 13.2 lb.
        val log = FakeLogRepository(listOf(weight("g0", daysAgo = 14, lb = 13.2)))
        val saveGrowth = useCase(log)
        saveGrowth.gains.test {
            saveGrowth(weight("g1", daysAgo = 0, lb = 13.24))
            expectNoEvents()
        }

        // 6.02 kg and 6.04 kg both show as 6.0 kg.
        val metric = FakeLogRepository(listOf(GrowthMeasurement("m0", today.minusDays(7), null, Mass.ofKg(6.02), null)))
        val saveMetric = useCase(metric, UnitSystem.METRIC)
        saveMetric.gains.test {
            saveMetric(GrowthMeasurement("m1", today, null, Mass.ofKg(6.04), null))
            expectNoEvents()
        }
    }

    @Test
    fun `either measure growing is enough, and only the ones that grew are reported`() = runTest {
        val log = FakeLogRepository(listOf(measurement("g0", daysAgo = 14, lb = 13.2, inches = 23.5)))
        val saveGrowth = useCase(log)
        saveGrowth.gains.test {
            saveGrowth(measurement("g1", daysAgo = 0, lb = 13.0, inches = 24.0))
            assertEquals(GrowthGain(weight = null, length = Increase(Length.ofInches(23.5), Length.ofInches(24.0))), awaitItem())

            saveGrowth(measurement("g2", daysAgo = 0, lb = 13.9, inches = 24.5))
            assertEquals(
                GrowthGain(Increase(Mass.ofLb(13.2), Mass.ofLb(13.9)), Increase(Length.ofInches(23.5), Length.ofInches(24.5))),
                awaitItem(),
                "g1, saved the same day, isn't 'before' g2",
            )
        }
    }

    @Test
    fun `each measure is compared with the newest one that has it`() = runTest {
        // The last measurement had only a length; the weight to beat is the one before.
        val log = FakeLogRepository(
            listOf(measurement("g0", daysAgo = 30, lb = 12.0, inches = null), measurement("g1", daysAgo = 10, lb = null, inches = 23.0)),
        )
        val saveGrowth = useCase(log)
        saveGrowth.gains.test {
            saveGrowth(weight("g2", daysAgo = 0, lb = 12.5))
            assertEquals(Increase(Mass.ofLb(12.0), Mass.ofLb(12.5)), awaitItem().weight)
        }
    }

    @Test
    fun `a backdated measurement is compared with the one before its own date`() = runTest {
        val log = FakeLogRepository(listOf(weight("g0", daysAgo = 30, lb = 12.0), weight("g2", daysAgo = 2, lb = 14.0)))
        val saveGrowth = useCase(log)
        saveGrowth.gains.test {
            saveGrowth(weight("g1", daysAgo = 15, lb = 13.0))
            assertEquals(Increase(Mass.ofLb(12.0), Mass.ofLb(13.0)), awaitItem().weight, "13.0 beats 12.0, though not 14.0")
        }
    }

    @Test
    fun `an edit counts only when it changes the value`() = runTest {
        val log = FakeLogRepository(listOf(weight("g0", daysAgo = 14, lb = 13.2), weight("g1", daysAgo = 0, lb = 13.9)))
        val saveGrowth = useCase(log)
        saveGrowth.gains.test {
            saveGrowth(weight("g1", daysAgo = 0, lb = 13.9).copy(note = "after a feed"))
            expectNoEvents()

            saveGrowth(weight("g1", daysAgo = 0, lb = 14.1))
            assertEquals(Increase(Mass.ofLb(13.2), Mass.ofLb(14.1)), awaitItem().weight, "compared with g0, not its old self")
        }
    }
}
