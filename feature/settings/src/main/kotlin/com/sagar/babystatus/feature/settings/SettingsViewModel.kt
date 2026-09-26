package com.sagar.babystatus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.CaregiverRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.model.AppInfo
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baby: Baby? = null,
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    /** Mom's name; the MOM badge elsewhere stays the fixed word. */
    val motherName: String = "",
    val appInfo: AppInfo,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    babyRepository: BabyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val caregiverRepository: CaregiverRepository,
    appInfo: AppInfo,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        babyRepository.baby,
        userPreferencesRepository.preferences,
        caregiverRepository.mother,
    ) { baby, preferences, mother ->
        SettingsUiState(baby = baby, unitSystem = preferences.unitSystem, motherName = mother.name, appInfo = appInfo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(appInfo = appInfo))

    fun renameMother(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { caregiverRepository.renameMother(name.take(MAX_NAME_LENGTH)) }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch { userPreferencesRepository.setUnitSystem(unitSystem) }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
