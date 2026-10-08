package com.sagar.babystatus.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.common.todayDates
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.GetTrendsUseCase
import com.sagar.babystatus.core.domain.TrendCategory
import com.sagar.babystatus.core.domain.Trends
import com.sagar.babystatus.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

sealed interface TrendsUiState {
    data object Loading : TrendsUiState

    data class Ready(val trends: Trends, val category: TrendCategory, val unitSystem: UnitSystem) : TrendsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrendsViewModel @Inject constructor(
    getTrends: GetTrendsUseCase,
    userPreferencesRepository: UserPreferencesRepository,
    clock: Clock,
) : ViewModel() {

    // Held here, so the chosen chart survives a rotation.
    private val category = MutableStateFlow(TrendCategory.FEEDING)

    /** The window updates the moment something is logged, and rolls over at local midnight. */
    val uiState: StateFlow<TrendsUiState> = combine(
        clock.todayDates().flatMapLatest { today -> getTrends(today) },
        category,
        userPreferencesRepository.preferences,
    ) { trends, category, preferences ->
        if (trends == null) TrendsUiState.Loading else TrendsUiState.Ready(trends, category, preferences.unitSystem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsUiState.Loading)

    fun onSelectCategory(category: TrendCategory) {
        this.category.value = category
    }
}
