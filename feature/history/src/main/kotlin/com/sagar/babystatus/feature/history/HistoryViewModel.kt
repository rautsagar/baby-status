package com.sagar.babystatus.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sagar.babystatus.core.data.repository.TimelineRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.model.TimelineDay
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.UnitSystem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

data class HistoryUiState(
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val today: LocalDate,
    val zone: ZoneId,
)

@HiltViewModel(assistedFactory = HistoryViewModel.Factory::class)
class HistoryViewModel @AssistedInject constructor(
    @Assisted private val key: HistoryKey,
    private val timelineRepository: TimelineRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val deleteEntry: DeleteEntryUseCase,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: HistoryKey): HistoryViewModel
    }

    /** One item per day that has entries, newest first: the baby's entries and Mom's pumping together. */
    val days: Flow<PagingData<TimelineDay>> = timelineRepository.historyDays().cachedIn(viewModelScope)

    val uiState: StateFlow<HistoryUiState> = userPreferencesRepository.preferences
        .map { HistoryUiState(it.unitSystem, LocalDate.now(clock), clock.zone) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HistoryUiState(today = LocalDate.now(clock), zone = clock.zone),
        )

    private val _initialScrollIndex = MutableStateFlow<Int?>(null)

    /**
     * The list position to jump to when History was opened for a particular day, until
     * [onScrolled] says it's done (so a rotation doesn't jump again).
     */
    val initialScrollIndex: StateFlow<Int?> = _initialScrollIndex.asStateFlow()

    init {
        key.focusEpochDay?.let { epochDay ->
            viewModelScope.launch {
                _initialScrollIndex.value = timelineRepository.dayIndex(LocalDate.ofEpochDay(epochDay))
            }
        }
    }

    fun onScrolled() {
        _initialScrollIndex.value = null
    }

    fun delete(entry: TimelineEntry) {
        viewModelScope.launch { deleteEntry(entry.id) }
    }
}
