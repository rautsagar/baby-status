package com.sagar.babystatus.feature.trends

import com.sagar.babystatus.core.domain.GetTrendsUseCase
import com.sagar.babystatus.core.domain.TrendCategory
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakeTimelineRepository
import com.sagar.babystatus.core.testing.FakeUserPreferencesRepository
import com.sagar.babystatus.core.testing.MainDispatcherRule
import com.sagar.babystatus.core.testing.MutableClock
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TrendsViewModelTest {
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
        preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
    ) = TrendsViewModel(
        getTrends = GetTrendsUseCase(babies, FakeTimelineRepository(log)),
        userPreferencesRepository = preferences,
        clock = clock,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() } }

    @Test
    fun `loading until there is a baby`() = runTest {
        val babies = FakeBabyRepository()
        val vm = viewModel(babies = babies)
        assertEquals(TrendsUiState.Loading, vm.uiState.value)

        babies.create("Ava", baby.birthDate, null)

        assertIs<TrendsUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `feeding is the default category, and the window ends today`() = runTest {
        val state = assertIs<TrendsUiState.Ready>(viewModel().uiState.value)

        assertEquals(TrendCategory.FEEDING, state.category)
        assertEquals(30, state.trends.days.size)
        assertEquals(today, state.trends.days.last().date)
    }

    @Test
    fun `selecting a category changes it`() = runTest {
        val vm = viewModel()

        vm.onSelectCategory(TrendCategory.GROWTH)
        assertEquals(TrendCategory.GROWTH, assertIs<TrendsUiState.Ready>(vm.uiState.value).category)

        vm.onSelectCategory(TrendCategory.PUMPING)
        assertEquals(TrendCategory.PUMPING, assertIs<TrendsUiState.Ready>(vm.uiState.value).category)
    }

    @Test
    fun `switching units changes the unit system the screen formats with`() = runTest {
        val preferences = FakeUserPreferencesRepository(UnitSystem.IMPERIAL)
        val vm = viewModel(preferences = preferences)
        assertEquals(UnitSystem.IMPERIAL, assertIs<TrendsUiState.Ready>(vm.uiState.value).unitSystem)

        preferences.setUnitSystem(UnitSystem.METRIC)

        assertEquals(UnitSystem.METRIC, assertIs<TrendsUiState.Ready>(vm.uiState.value).unitSystem)
    }

    @Test
    fun `logging something updates the window without reopening the screen`() = runTest {
        val log = FakeLogRepository()
        val vm = viewModel(log = log)
        vm.onSelectCategory(TrendCategory.DIAPERS)
        assertEquals(0.0, assertIs<TrendsUiState.Ready>(vm.uiState.value).trends.days.last().fed.ml)

        log.save(bottle("b0", today, 3.0))
        log.save(bottle("b1", today.minusDays(3), 5.0))

        val state = assertIs<TrendsUiState.Ready>(vm.uiState.value)
        assertEquals(3.0, state.trends.days.last().fed.oz, 0.001)
        assertEquals(5.0, state.trends.days.single { it.date == today.minusDays(3) }.fed.oz, 0.001)
        assertEquals(TrendCategory.DIAPERS, state.category, "the selection survives new data")
    }
}
