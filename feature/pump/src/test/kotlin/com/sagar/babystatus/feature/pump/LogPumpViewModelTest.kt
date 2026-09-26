package com.sagar.babystatus.feature.pump

import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.domain.PumpSessionError
import com.sagar.babystatus.core.domain.ValidatePumpSessionUseCase
import com.sagar.babystatus.core.model.BreastSide
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakePumpRepository
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
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogPumpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val today = LocalDate.of(2026, 9, 24)

    private fun TestScope.viewModel(
        key: LogPumpKey = LogPumpKey(),
        pump: FakePumpRepository = FakePumpRepository(),
        unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    ) = LogPumpViewModel(
        key = key,
        pumpRepository = pump,
        userPreferencesRepository = FakeUserPreferencesRepository(unitSystem),
        validate = ValidatePumpSessionUseCase(clock),
        deleteEntry = DeleteEntryUseCase(FakeTimelineRepository(pump = pump)),
        idGenerator = SequentialIdGenerator(),
        clock = clock,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() } }

    @Test
    fun `a quick save is 3 oz now, with no side or duration`() = runTest {
        val pump = FakePumpRepository()
        val vm = viewModel(pump = pump)
        vm.save()

        val saved = pump.all.single()
        assertEquals(3.0, saved.volume.oz, 0.001)
        assertEquals(today, saved.date)
        assertNull(saved.side)
        assertNull(saved.duration)
        assertTrue(vm.uiState.value.isDone)
    }

    @Test
    fun `side toggles off, duration takes whole minutes, metric amounts snap to 10 ml`() = runTest {
        val pump = FakePumpRepository()
        val vm = viewModel(pump = pump, unitSystem = UnitSystem.METRIC)

        vm.onSideToggled(BreastSide.LEFT)
        vm.onSideToggled(BreastSide.LEFT)
        assertNull(vm.uiState.value.side)
        vm.onSideToggled(BreastSide.BOTH)

        vm.onDurationChanged("1a5")
        assertEquals("15", vm.uiState.value.durationText, "non-digits dropped")
        vm.onVolumeChanged(143.0)
        assertEquals(140.0, vm.uiState.value.volumeValue)
        vm.onNoteChanged(" Froze 2 oz ")
        vm.save()

        assertEquals(
            PumpSession("id-1", clock.now, today, "Froze 2 oz", Volume(140.0), BreastSide.BOTH, Duration.ofMinutes(15)),
            pump.all.single().copy(occurredAt = clock.now),
        )
    }

    @Test
    fun `an out-of-range duration blocks saving`() = runTest {
        val vm = viewModel()
        vm.onDurationChanged("200")
        assertTrue(vm.uiState.value.durationInvalid)
        assertFalse(vm.uiState.value.canSave)
        vm.onDurationChanged("")
        assertTrue(vm.uiState.value.canSave, "blank means not recorded")
    }

    @Test
    fun `a future time is rejected`() = runTest {
        val pump = FakePumpRepository()
        val vm = viewModel(pump = pump)
        vm.onOccurredAtChanged(clock.now.plus(Duration.ofHours(2)))
        vm.save()
        assertEquals(setOf(PumpSessionError.IN_FUTURE), vm.uiState.value.errors)
        assertTrue(pump.all.isEmpty())
    }

    @Test
    fun `editing loads the session and can delete it`() = runTest {
        val existing = PumpSession("p1", clock.now.minusSeconds(3600), today, "left only", Volume.ofOz(5.0), BreastSide.LEFT, Duration.ofMinutes(20))
        val pump = FakePumpRepository(listOf(existing))
        val vm = viewModel(key = LogPumpKey("p1"), pump = pump)

        val state = vm.uiState.value
        assertTrue(state.isEdit)
        assertEquals(5.0, state.volumeValue)
        assertEquals(BreastSide.LEFT, state.side)
        assertEquals("20", state.durationText)
        assertEquals("left only", state.note)

        vm.delete()
        assertEquals(setOf("p1"), pump.deletedIds)
        assertTrue(vm.uiState.value.isDone)
    }
}
