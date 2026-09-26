package com.sagar.babystatus.feature.growth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.common.IdGenerator
import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.LogRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.domain.LogEntryError
import com.sagar.babystatus.core.domain.ValidateLogEntryUseCase
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.ui.format.UnitFormatter
import com.sagar.babystatus.core.ui.format.parseDecimal
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

internal data class GrowthForm(
    val date: LocalDate,
    val weightText: String = "",
    val lengthText: String = "",
    val note: String = "",
)

data class LogGrowthUiState(
    val isEdit: Boolean,
    val isLoading: Boolean,
    val date: LocalDate,
    val weightText: String,
    val lengthText: String,
    val note: String,
    val unitSystem: UnitSystem,
    /** Typed but not a plausible baby weight / length in the current unit. */
    val weightInvalid: Boolean,
    val lengthInvalid: Boolean,
    val errors: Set<LogEntryError>,
    val today: LocalDate,
    val birthDate: LocalDate?,
    val isDone: Boolean,
) {
    val canSave: Boolean
        get() = !isLoading && (weightText.isNotBlank() || lengthText.isNotBlank()) && !weightInvalid && !lengthInvalid
}

@HiltViewModel(assistedFactory = LogGrowthViewModel.Factory::class)
class LogGrowthViewModel @AssistedInject constructor(
    @Assisted private val key: LogGrowthKey,
    private val logRepository: LogRepository,
    private val babyRepository: BabyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val validate: ValidateLogEntryUseCase,
    private val deleteEntry: DeleteEntryUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: LogGrowthKey): LogGrowthViewModel
    }

    private val form = MutableStateFlow(GrowthForm(date = LocalDate.now(clock)))
    private val isLoading = MutableStateFlow(key.entryId != null)
    private val errors = MutableStateFlow<Set<LogEntryError>>(emptySet())
    private val isDone = MutableStateFlow(false)

    val uiState: StateFlow<LogGrowthUiState> = combine(
        form,
        isLoading,
        errors,
        isDone,
        combine(userPreferencesRepository.preferences, babyRepository.baby) { prefs, baby -> prefs.unitSystem to baby?.birthDate },
    ) { form, loading, errors, done, (unitSystem, birthDate) ->
        toUiState(form, loading, errors, done, unitSystem, birthDate)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        toUiState(form.value, isLoading.value, emptySet(), false, UnitSystem.IMPERIAL, null),
    )

    init {
        key.entryId?.let { id ->
            viewModelScope.launch {
                val entry = logRepository.get(id) as? GrowthMeasurement
                if (entry == null) {
                    isDone.value = true
                    return@launch
                }
                val unitSystem = userPreferencesRepository.preferences.first().unitSystem
                form.value = GrowthForm(
                    date = entry.date,
                    weightText = entry.weight?.let { UnitFormatter.massInput(it, unitSystem) }.orEmpty(),
                    lengthText = entry.length?.let { UnitFormatter.lengthInput(it, unitSystem) }.orEmpty(),
                    note = entry.note.orEmpty(),
                )
                isLoading.value = false
            }
        }
    }

    fun onDateChanged(date: LocalDate) {
        form.update { it.copy(date = date) }
        errors.value = emptySet()
    }

    fun onWeightChanged(text: String) = form.update { it.copy(weightText = text) }

    fun onLengthChanged(text: String) = form.update { it.copy(lengthText = text) }

    fun onNoteChanged(note: String) = form.update { it.copy(note = note) }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val baby = babyRepository.getBaby() ?: return@launch
            val entry = GrowthMeasurement(
                id = key.entryId ?: idGenerator.newId(),
                date = state.date,
                note = state.note.trim().ifEmpty { null },
                weight = parseMass(state.weightText, state.unitSystem),
                length = parseLength(state.lengthText, state.unitSystem),
            )
            val problems = validate(entry, baby.birthDate)
            if (problems.isNotEmpty()) {
                errors.value = problems
                return@launch
            }
            logRepository.save(entry)
            isDone.value = true
        }
    }

    fun delete() {
        val id = key.entryId ?: return
        viewModelScope.launch {
            deleteEntry(id)
            isDone.value = true
        }
    }

    private fun toUiState(
        form: GrowthForm,
        loading: Boolean,
        errors: Set<LogEntryError>,
        done: Boolean,
        unitSystem: UnitSystem,
        birthDate: LocalDate?,
    ) = LogGrowthUiState(
        isEdit = key.entryId != null,
        isLoading = loading,
        date = form.date,
        weightText = form.weightText,
        lengthText = form.lengthText,
        note = form.note,
        unitSystem = unitSystem,
        weightInvalid = form.weightText.isNotBlank() && parseMass(form.weightText, unitSystem) == null,
        lengthInvalid = form.lengthText.isNotBlank() && parseLength(form.lengthText, unitSystem) == null,
        errors = errors,
        today = LocalDate.now(clock),
        birthDate = birthDate,
        isDone = done,
    )

    internal companion object {
        /** Plausible ranges for a baby/toddler, stored units: grams and millimetres. */
        private val GRAMS_RANGE = 400..30_000
        private val MM_RANGE = 200..1_300

        fun parseMass(text: String, unitSystem: UnitSystem): Mass? {
            val value = parseDecimal(text) ?: return null
            val mass = if (unitSystem == UnitSystem.IMPERIAL) Mass.ofLb(value) else Mass.ofKg(value)
            return mass.takeIf { it.grams in GRAMS_RANGE }
        }

        fun parseLength(text: String, unitSystem: UnitSystem): Length? {
            val value = parseDecimal(text) ?: return null
            val length = if (unitSystem == UnitSystem.IMPERIAL) Length.ofInches(value) else Length.ofCm(value)
            return length.takeIf { it.mm in MM_RANGE }
        }
    }
}
