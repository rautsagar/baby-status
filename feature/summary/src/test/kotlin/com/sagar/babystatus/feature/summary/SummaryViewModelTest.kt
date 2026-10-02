package com.sagar.babystatus.feature.summary

import com.sagar.babystatus.core.domain.GetSummaryUseCase
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakePumpRepository
import com.sagar.babystatus.core.testing.FakeTimelineRepository
import com.sagar.babystatus.core.testing.FakeUserPreferencesRepository
import com.sagar.babystatus.core.testing.MainDispatcherRule
import com.sagar.babystatus.core.testing.MutableClock
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class SummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 24)
    private val baby = Baby("baby-1", "Ava", LocalDate.of(2026, 6, 12), null)
    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))

    private fun bottle(id: String, date: LocalDate, oz: Double) =
        BottleFeed(id, date.atTime(12, 0).atZone(clock.zone).toInstant(), date, null, Volume.ofOz(oz))

    private fun TestScope.viewModel(
        babies: FakeBabyRepository = FakeBabyRepository(baby),
        log: FakeLogRepository = FakeLogRepository(),
        pump: FakePumpRepository = FakePumpRepository(),
        preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
    ) = SummaryViewModel(
        getSummary = GetSummaryUseCase(babies, FakeTimelineRepository(log, pump)),
        userPreferencesRepository = preferences,
        clock = clock,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() } }

    @Test
    fun `loading until there is a baby`() = runTest {
        assertEquals(SummaryUiState.Loading, viewModel(babies = FakeBabyRepository()).uiState.value)
    }

    @Test
    fun `today's totals and the previous days`() = runTest {
        val log = FakeLogRepository(
            listOf(
                bottle("b0", today, 5.0),
                DiaperChange("d0", clock.now, today, null, DiaperKind.PEE),
                bottle("b1", today.minusDays(1), 6.0),
            ),
        )
        val pump = FakePumpRepository(listOf(PumpSession("p0", clock.now, today, null, Volume.ofOz(4.0), null, null)))

        val state = assertIs<SummaryUiState.Ready>(viewModel(log = log, pump = pump).uiState.value)

        assertEquals(today, state.today)
        assertEquals(5.0, state.summary.today.fed.oz, 0.001)
        assertEquals(4.0, state.summary.today.pumped.oz, 0.001)
        assertEquals(1, state.summary.today.diapers)
        assertEquals((1..5).map { today.minusDays(it.toLong()) }, state.summary.previous.map { it.date })
        assertEquals(6.0, state.summary.previous.first().fed.oz, 0.001)
    }

    @Test
    fun `logging something updates the totals without reopening the screen`() = runTest {
        val log = FakeLogRepository()
        val vm = viewModel(log = log)
        assertEquals(0.0, assertIs<SummaryUiState.Ready>(vm.uiState.value).summary.today.fed.ml)

        log.save(bottle("b0", today, 3.0))

        assertEquals(3.0, assertIs<SummaryUiState.Ready>(vm.uiState.value).summary.today.fed.oz, 0.001)
    }

    @Test
    fun `switching units changes the unit system the screen formats with`() = runTest {
        val preferences = FakeUserPreferencesRepository(UnitSystem.IMPERIAL)
        val vm = viewModel(preferences = preferences)
        assertEquals(UnitSystem.IMPERIAL, assertIs<SummaryUiState.Ready>(vm.uiState.value).unitSystem)

        preferences.setUnitSystem(UnitSystem.METRIC)

        assertEquals(UnitSystem.METRIC, assertIs<SummaryUiState.Ready>(vm.uiState.value).unitSystem)
    }

    @Test
    fun `at midnight today becomes yesterday and the new day starts empty`() = runTest {
        clock.now = LocalDateTime.of(2026, 9, 24, 23, 59, 30).atZone(clock.zone).toInstant()
        val vm = viewModel(log = FakeLogRepository(listOf(bottle("b0", today, 5.0))))
        assertEquals(5.0, assertIs<SummaryUiState.Ready>(vm.uiState.value).summary.today.fed.oz, 0.001)

        clock.advanceBy(Duration.ofSeconds(60))
        advanceTimeBy(31.seconds)

        val state = assertIs<SummaryUiState.Ready>(vm.uiState.value)
        assertEquals(today.plusDays(1), state.today)
        assertEquals(0.0, state.summary.today.fed.ml)
        assertEquals(today, state.summary.previous.first().date)
        assertEquals(5.0, state.summary.previous.first().fed.oz, 0.001)
    }
}
