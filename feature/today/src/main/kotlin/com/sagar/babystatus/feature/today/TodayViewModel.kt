package com.sagar.babystatus.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.data.repository.PumpRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.BabyAge
import com.sagar.babystatus.core.domain.GetTodayStatusUseCase
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.PumpSummary
import com.sagar.babystatus.core.model.TimedEntry
import com.sagar.babystatus.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Ready(
        val babyName: String,
        val photoPath: String?,
        val age: BabyAge,
        val weight: Mass?,
        val length: Length?,
        val lastFeed: TimedEntry?,
        val lastDiaper: TimedEntry?,
        /** Today's timed entries, newest first (at most five). */
        val recent: List<TimedEntry>,
        /** Mom's pumping: today's total and the last session. Not part of the baby's status. */
        val pumping: PumpSummary,
        val unitSystem: UnitSystem,
        val now: Instant,
        val today: LocalDate,
        val zone: ZoneId,
    ) : TodayUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayStatus: GetTodayStatusUseCase,
    pumpRepository: PumpRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock,
) : ViewModel() {

    /** Ticks at each minute boundary so "2h 25m ago" and the day stay current. */
    private val now: Flow<Instant> = flow {
        while (true) {
            val instant = clock.instant()
            emit(instant)
            delay(MILLIS_PER_MINUTE - instant.toEpochMilli() % MILLIS_PER_MINUTE)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val today: Flow<LocalDate> = now.map { it.atZone(clock.zone).toLocalDate() }.distinctUntilChanged()

    val uiState: StateFlow<TodayUiState> = combine(
        now,
        today.flatMapLatest { today -> getTodayStatus(today) },
        today.flatMapLatest { today -> pumpRepository.observeSummary(today) },
        userPreferencesRepository.preferences,
    ) { now, status, pumping, preferences ->
        if (status == null) {
            TodayUiState.Loading
        } else {
            TodayUiState.Ready(
                babyName = status.baby.name,
                photoPath = status.baby.photoPath,
                age = status.age,
                weight = status.latestGrowth.weight,
                length = status.latestGrowth.length,
                lastFeed = status.lastFeed,
                lastDiaper = status.lastDiaper,
                recent = status.recent,
                pumping = pumping,
                unitSystem = preferences.unitSystem,
                now = now,
                today = now.atZone(clock.zone).toLocalDate(),
                zone = clock.zone,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
