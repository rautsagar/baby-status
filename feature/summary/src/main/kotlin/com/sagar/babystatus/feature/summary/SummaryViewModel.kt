package com.sagar.babystatus.feature.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.common.todayDates
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.GetSummaryUseCase
import com.sagar.babystatus.core.domain.Summary
import com.sagar.babystatus.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface SummaryUiState {
    data object Loading : SummaryUiState

    data class Ready(val summary: Summary, val unitSystem: UnitSystem) : SummaryUiState {
        val today: LocalDate get() = summary.today.date
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    getSummary: GetSummaryUseCase,
    userPreferencesRepository: UserPreferencesRepository,
    clock: Clock,
) : ViewModel() {

    /** Totals update the moment something is logged, and "today" rolls over at local midnight. */
    val uiState: StateFlow<SummaryUiState> = combine(
        clock.todayDates().flatMapLatest { today -> getSummary(today) },
        userPreferencesRepository.preferences,
    ) { summary, preferences ->
        if (summary == null) SummaryUiState.Loading else SummaryUiState.Ready(summary, preferences.unitSystem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState.Loading)
}
