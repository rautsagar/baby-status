package com.sagar.babystatus.feature.today

import com.sagar.babystatus.core.domain.BabyAge
import com.sagar.babystatus.core.domain.CalculateAgeUseCase
import com.sagar.babystatus.core.domain.GetTodayStatusUseCase
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.BreastSide
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakePumpRepository
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

class TodayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val today = LocalDate.of(2026, 9, 24)
    private val baby = Baby("baby-1", "Ava", LocalDate.of(2026, 6, 12), null)

    private fun TestScope.viewModel(
        babies: FakeBabyRepository,
        log: FakeLogRepository,
        pump: FakePumpRepository = FakePumpRepository(),
    ) = TodayViewModel(
        getTodayStatus = GetTodayStatusUseCase(babies, log, CalculateAgeUseCase()),
        pumpRepository = pump,
        userPreferencesRepository = FakeUserPreferencesRepository(),
        clock = clock,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() } }

    @Test
    fun `loading until there is a baby`() = runTest {
        val vm = viewModel(FakeBabyRepository(), FakeLogRepository())
        assertEquals(TodayUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `shows age, latest growth, last feed and diaper, and only today's entries`() = runTest {
        val yesterdayFeed = BottleFeed("f0", clock.now.minusSeconds(86_400), today.minusDays(1), null, Volume.ofOz(6.0))
        val feed = BottleFeed("f1", clock.now.minusSeconds(8_700), today, null, Volume.ofOz(5.0))
        val diaper = DiaperChange("d1", clock.now.minusSeconds(3_600), today, null, DiaperKind.PEE)
        val oldGrowth = GrowthMeasurement("g0", today.minusDays(30), null, Mass(5000), Length(580))
        val newWeight = GrowthMeasurement("g1", today.minusDays(2), null, Mass(6000), length = null)
        val log = FakeLogRepository(listOf(yesterdayFeed, feed, diaper, oldGrowth, newWeight))

        val state = assertIs<TodayUiState.Ready>(viewModel(FakeBabyRepository(baby), log).uiState.value)

        assertEquals("Ava", state.babyName)
        assertEquals(BabyAge.Months(3, 1), state.age)
        assertEquals(Mass(6000), state.weight, "newest weight")
        assertEquals(Length(580), state.length, "newest length, even from an older measurement")
        assertEquals(feed, state.lastFeed)
        assertEquals(diaper, state.lastDiaper)
        assertEquals(listOf(diaper, feed), state.recent, "today only, newest first")
        assertEquals(today, state.today)
    }

    @Test
    fun `pumping shows today's total and the last session, and stays out of recent`() = runTest {
        val yesterday = PumpSession("p0", clock.now.minusSeconds(86_400), today.minusDays(1), null, Volume.ofOz(6.0), null, null)
        val morning = PumpSession("p1", clock.now.minusSeconds(21_600), today, null, Volume.ofOz(4.0), BreastSide.BOTH, null)
        val noon = PumpSession("p2", clock.now.minusSeconds(10_800), today, null, Volume.ofOz(5.0), null, null)
        val vm = viewModel(FakeBabyRepository(baby), FakeLogRepository(), FakePumpRepository(listOf(yesterday, morning, noon)))

        val state = assertIs<TodayUiState.Ready>(vm.uiState.value)
        assertEquals(9.0, state.pumping.pumpedToday.oz, 0.001)
        assertEquals(noon, state.pumping.last)
        assertEquals(emptyList(), state.recent)
    }

    @Test
    fun `age formatting`() {
        assertEquals("Newborn", formatAge(BabyAge.Days(0)))
        assertEquals("1 day", formatAge(BabyAge.Days(1)))
        assertEquals("12 days", formatAge(BabyAge.Days(12)))
        assertEquals("3 wk", formatAge(BabyAge.Weeks(3, 0)))
        assertEquals("3 wk, 2 d", formatAge(BabyAge.Weeks(3, 2)))
        assertEquals("3 mo, 1 wk", formatAge(BabyAge.Months(3, 1)))
        assertEquals("2 yr, 3 mo", formatAge(BabyAge.Years(2, 3)))
    }
}
