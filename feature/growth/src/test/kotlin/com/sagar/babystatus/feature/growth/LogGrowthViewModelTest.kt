package com.sagar.babystatus.feature.growth

import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.domain.ValidateLogEntryUseCase
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakeTimelineRepository
import com.sagar.babystatus.core.testing.FakeUserPreferencesRepository
import com.sagar.babystatus.core.testing.MainDispatcherRule
import com.sagar.babystatus.core.testing.MutableClock
import com.sagar.babystatus.core.testing.SequentialIdGenerator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogGrowthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val today = LocalDate.of(2026, 9, 24)

    @Before
    fun useUsLocale() = Locale.setDefault(Locale.US)

    private fun TestScope.viewModel(
        key: LogGrowthKey = LogGrowthKey(),
        log: FakeLogRepository = FakeLogRepository(),
        unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    ) = LogGrowthViewModel(
        key = key,
        logRepository = log,
        babyRepository = FakeBabyRepository(Baby("baby-1", "Ava", LocalDate.of(2026, 6, 12), null)),
        userPreferencesRepository = FakeUserPreferencesRepository(unitSystem),
        validate = ValidateLogEntryUseCase(clock),
        deleteEntry = DeleteEntryUseCase(FakeTimelineRepository(log)),
        idGenerator = SequentialIdGenerator(),
        clock = clock,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() } }

    @Test
    fun `nothing to save until a measurement is typed`() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canSave)
        vm.onLengthChanged("23.5")
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `weight only is saved in grams, dated today`() = runTest {
        val log = FakeLogRepository()
        val vm = viewModel(log = log)
        vm.onWeightChanged("13.2")
        vm.save()

        val saved = assertIs<GrowthMeasurement>(log.all.single())
        assertEquals(Mass.ofLb(13.2), saved.weight)
        assertNull(saved.length)
        assertEquals(today, saved.date)
        assertTrue(vm.uiState.value.isDone)
    }

    @Test
    fun `implausible or unreadable numbers block saving`() = runTest {
        val vm = viewModel()
        vm.onWeightChanged("abc")
        assertTrue(vm.uiState.value.weightInvalid)
        vm.onWeightChanged("250") // lb
        assertTrue(vm.uiState.value.weightInvalid)
        assertFalse(vm.uiState.value.canSave)
        vm.onWeightChanged("13,2")
        assertFalse(vm.uiState.value.weightInvalid, "a comma decimal is fine")
    }

    @Test
    fun `editing shows stored values in the current unit`() = runTest {
        val entry = GrowthMeasurement("g1", today.minusDays(3), "checkup", Mass(6000), Length(600))
        val log = FakeLogRepository(listOf(entry))
        val vm = viewModel(key = LogGrowthKey("g1"), log = log, unitSystem = UnitSystem.METRIC)

        val state = vm.uiState.value
        assertTrue(state.isEdit)
        assertEquals("6.0", state.weightText)
        assertEquals("60.0", state.lengthText)
        assertEquals(today.minusDays(3), state.date)

        vm.delete()
        assertEquals(setOf("g1"), log.deletedIds)
    }
}
