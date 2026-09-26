package com.sagar.babystatus.feature.pump

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.common.IdGenerator
import com.sagar.babystatus.core.data.repository.PumpRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.domain.PumpSessionError
import com.sagar.babystatus.core.domain.ValidatePumpSessionUseCase
import com.sagar.babystatus.core.model.BreastSide
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.ui.format.VolumeScale
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** What the user has entered so far. The amount is kept as a [Volume], so units can change. */
internal data class PumpForm(
    /** Null until the slider moves: then the unit's default (3 oz / 90 ml) applies. */
    val volume: Volume? = null,
    val side: BreastSide? = null,
    val durationText: String = "",
    val occurredAt: Instant,
    val note: String = "",
)

data class LogPumpUiState(
    val isEdit: Boolean,
    val isLoading: Boolean,
    val scale: VolumeScale,
    /** Slider position in [scale]'s unit. */
    val volumeValue: Double,
    val side: BreastSide?,
    val durationText: String,
    /** Typed, but not a whole number of minutes from 1 to 180. */
    val durationInvalid: Boolean,
    val occurredAt: Instant,
    val note: String,
    val errors: Set<PumpSessionError>,
    val today: LocalDate,
    val zone: ZoneId,
    /** Set once saved or deleted; the sheet then closes. */
    val isDone: Boolean,
) {
    val canSave: Boolean get() = !isLoading && !durationInvalid
}

@HiltViewModel(assistedFactory = LogPumpViewModel.Factory::class)
class LogPumpViewModel @AssistedInject constructor(
    @Assisted private val key: LogPumpKey,
    private val pumpRepository: PumpRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val validate: ValidatePumpSessionUseCase,
    private val deleteEntry: DeleteEntryUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: LogPumpKey): LogPumpViewModel
    }

    private val form = MutableStateFlow(PumpForm(occurredAt = clock.instant().truncatedTo(ChronoUnit.MINUTES)))
    private val isLoading = MutableStateFlow(key.sessionId != null)
    private val errors = MutableStateFlow<Set<PumpSessionError>>(emptySet())
    private val isDone = MutableStateFlow(false)

    val uiState: StateFlow<LogPumpUiState> = combine(
        form,
        isLoading,
        errors,
        isDone,
        userPreferencesRepository.preferences,
    ) { form, loading, errors, done, preferences ->
        toUiState(form, loading, errors, done, preferences.unitSystem)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        toUiState(form.value, isLoading.value, emptySet(), false, UnitSystem.IMPERIAL),
    )

    init {
        key.sessionId?.let { id ->
            viewModelScope.launch {
                val session = pumpRepository.get(id)
                if (session == null) {
                    isDone.value = true // Deleted meanwhile: nothing to edit.
                    return@launch
                }
                form.value = PumpForm(
                    volume = session.volume,
                    side = session.side,
                    durationText = session.duration?.toMinutes()?.toString().orEmpty(),
                    occurredAt = session.occurredAt,
                    note = session.note.orEmpty(),
                )
                isLoading.value = false
            }
        }
    }

    fun onVolumeChanged(value: Double) {
        val scale = uiState.value.scale
        form.update { it.copy(volume = scale.toVolume(scale.snap(value))) }
    }

    /** Tapping the selected side clears it: the side is optional. */
    fun onSideToggled(side: BreastSide) = form.update { it.copy(side = if (it.side == side) null else side) }

    fun onDurationChanged(text: String) {
        // Digits only, and nobody pumps for more than three digits of minutes.
        form.update { it.copy(durationText = text.filter(Char::isDigit).take(3)) }
    }

    fun onOccurredAtChanged(instant: Instant) {
        form.update { it.copy(occurredAt = instant) }
        errors.update { it - PumpSessionError.IN_FUTURE }
    }

    fun onNoteChanged(note: String) = form.update { it.copy(note = note) }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        val session = PumpSession(
            id = key.sessionId ?: idGenerator.newId(),
            occurredAt = state.occurredAt,
            date = state.occurredAt.atZone(clock.zone).toLocalDate(),
            note = state.note.trim().ifEmpty { null },
            volume = form.value.volume ?: state.scale.toVolume(state.scale.default),
            side = state.side,
            duration = parseDuration(state.durationText),
        )
        val problems = validate(session)
        if (problems.isNotEmpty()) {
            errors.value = problems
            return
        }
        viewModelScope.launch {
            pumpRepository.save(session)
            isDone.value = true
        }
    }

    fun delete() {
        val id = key.sessionId ?: return
        viewModelScope.launch {
            deleteEntry(id)
            isDone.value = true
        }
    }

    private fun toUiState(
        form: PumpForm,
        loading: Boolean,
        errors: Set<PumpSessionError>,
        done: Boolean,
        unitSystem: UnitSystem,
    ): LogPumpUiState {
        val scale = VolumeScale.forUnits(unitSystem)
        return LogPumpUiState(
            isEdit = key.sessionId != null,
            isLoading = loading,
            scale = scale,
            volumeValue = form.volume?.let(scale::fromVolume) ?: scale.default,
            side = form.side,
            durationText = form.durationText,
            durationInvalid = form.durationText.isNotEmpty() && parseDuration(form.durationText) == null,
            occurredAt = form.occurredAt,
            note = form.note,
            errors = errors,
            today = LocalDate.now(clock),
            zone = clock.zone,
            isDone = done,
        )
    }

    internal companion object {
        /** Whole minutes within the allowed range, or null (blank means "not recorded"). */
        fun parseDuration(text: String): Duration? = text.toLongOrNull()
            ?.let(Duration::ofMinutes)
            ?.takeIf { it in ValidatePumpSessionUseCase.MIN_DURATION..ValidatePumpSessionUseCase.MAX_DURATION }
    }
}
