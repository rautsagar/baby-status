package com.sagar.babystatus.feature.settings

import com.sagar.babystatus.core.model.AppInfo
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakeCaregiverRepository
import com.sagar.babystatus.core.testing.FakeUserPreferencesRepository
import com.sagar.babystatus.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `shows the baby and switches units`() = runTest {
        val baby = Baby("baby-1", "Ava", LocalDate.of(2026, 6, 12), null)
        val preferences = FakeUserPreferencesRepository(UnitSystem.IMPERIAL)
        val vm = SettingsViewModel(FakeBabyRepository(baby), preferences, FakeCaregiverRepository(), AppInfo("1.0.0", ""))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        assertEquals(baby, vm.uiState.value.baby)
        assertEquals("1.0.0", vm.uiState.value.appInfo.versionName)

        vm.setUnitSystem(UnitSystem.METRIC)
        assertEquals(UnitSystem.METRIC, vm.uiState.value.unitSystem)
    }

    @Test
    fun `renames mom, ignoring blank names`() = runTest {
        val vm = SettingsViewModel(
            FakeBabyRepository(),
            FakeUserPreferencesRepository(),
            FakeCaregiverRepository(),
            AppInfo("1.0.0", ""),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertEquals("Mom", vm.uiState.value.motherName)

        vm.renameMother("  Priya ")
        assertEquals("Priya", vm.uiState.value.motherName)

        vm.renameMother("   ")
        assertEquals("Priya", vm.uiState.value.motherName)
    }
}
