package com.sagar.babystatus.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.sagar.babystatus.core.data.repository.TimelineRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.model.DailyTotals
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface HistoryItem {
    data class DayHeader(val date: LocalDate) : HistoryItem
    data class Row(val entry: TimelineEntry) : HistoryItem
}

data class HistoryUiState(
    val totals: Map<LocalDate, DailyTotals> = emptyMap(),
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    val today: LocalDate,
    val zone: ZoneId,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    timelineRepository: TimelineRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val deleteEntry: DeleteEntryUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** The baby's entries and Mom's pumping in timeline order, with a header before each new day. */
    val items: Flow<PagingData<HistoryItem>> = timelineRepository.history()
        .map { page ->
            page.map { HistoryItem.Row(it) }
                .insertSeparators { before: HistoryItem.Row?, after: HistoryItem.Row? ->
                    when {
                        after == null -> null
                        before == null || before.entry.date != after.entry.date -> HistoryItem.DayHeader(after.entry.date)
                        else -> null
                    }
                }
        }
        .cachedIn(viewModelScope)

    /** Day totals are observed separately so a change doesn't rebuild the paged list. */
    val uiState: StateFlow<HistoryUiState> = combine(
        timelineRepository.observeDailyTotals(),
        userPreferencesRepository.preferences,
    ) { totals, preferences ->
        HistoryUiState(totals, preferences.unitSystem, LocalDate.now(clock), clock.zone)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistoryUiState(today = LocalDate.now(clock), zone = clock.zone),
    )

    fun delete(entry: TimelineEntry) {
        viewModelScope.launch { deleteEntry(entry.id) }
    }
}
