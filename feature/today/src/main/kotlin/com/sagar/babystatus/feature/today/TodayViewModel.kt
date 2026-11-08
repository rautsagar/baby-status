package com.sagar.babystatus.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.common.minuteTicks
import com.sagar.babystatus.core.data.repository.PumpRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.designsystem.component.Celebration
import com.sagar.babystatus.core.domain.BabyAge
import com.sagar.babystatus.core.domain.GetTodayStatusUseCase
import com.sagar.babystatus.core.domain.GrowthGain
import com.sagar.babystatus.core.domain.SaveGrowthUseCase
import com.sagar.babystatus.core.model.LatestGrowth
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.PumpSummary
import com.sagar.babystatus.core.model.TimedEntry
import com.sagar.babystatus.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        /** What just grew, while Today shows its ▲ chip; null otherwise. */
        val gain: GrowthGain? = null,
    ) : TodayUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayStatus: GetTodayStatusUseCase,
    pumpRepository: PumpRepository,
    userPreferencesRepository: UserPreferencesRepository,
    saveGrowth: SaveGrowthUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val gain = MutableStateFlow<GrowthGain?>(null)

    init {
        // A gain is announced wherever the growth sheet was opened from. Today shows it for a
        // few seconds, starting with the confetti once the sheet is out of the way.
        viewModelScope.launch {
            saveGrowth.gains.collectLatest {
                delay(Celebration.START_DELAY_MILLIS)
                gain.value = it
                delay(GAIN_CHIP_MILLIS)
                gain.value = null
            }
        }
    }

    /** Ticks at each minute boundary so "2h 25m ago" and the day stay current. */
    private val now: Flow<Instant> = clock.minuteTicks()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val today: Flow<LocalDate> = now.map { it.atZone(clock.zone).toLocalDate() }.distinctUntilChanged()

    val uiState: StateFlow<TodayUiState> = combine(
        now,
        today.flatMapLatest { today -> getTodayStatus(today) },
        today.flatMapLatest { today -> pumpRepository.observeSummary(today) },
        userPreferencesRepository.preferences,
        gain,
    ) { now, status, pumping, preferences, gain ->
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
                gain = gain?.shownOn(status.latestGrowth),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    companion object {
        /** How long the ▲ chip stays under a stat that grew. */
        const val GAIN_CHIP_MILLIS = 4_000L
    }
}

/**
 * Only the increases Today is showing: a backdated measurement can grow on the one before it
 * without being the latest, and a chip under a newer number would be wrong.
 */
private fun GrowthGain.shownOn(latest: LatestGrowth): GrowthGain? {
    val weight = weight?.takeIf { it.to == latest.weight }
    val length = length?.takeIf { it.to == latest.length }
    return if (weight == null && length == null) null else GrowthGain(weight, length)
}
