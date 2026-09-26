package com.sagar.babystatus.feature.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.common.IdGenerator
import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.LogRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.domain.LogEntryError
import com.sagar.babystatus.core.domain.ValidateLogEntryUseCase
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.Reaction
import com.sagar.babystatus.core.model.SolidsAmount
import com.sagar.babystatus.core.model.SolidsFeed
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class FeedKind { BOTTLE, SOLIDS }

/** What the user has entered so far. Units-free: the bottle is kept as a [Volume]. */
internal data class LogActivityForm(
    val type: ActivityType,
    val feedKind: FeedKind = FeedKind.BOTTLE,
    /** Null until the user moves the slider: then the unit's default (3 oz / 90 ml) applies. */
    val bottle: Volume? = null,
    val food: String = "",
    val solidsAmount: SolidsAmount? = null,
    val reaction: Reaction? = null,
    val diaperKind: DiaperKind = DiaperKind.PEE,
    val occurredAt: Instant,
    val note: String = "",
)

data class LogActivityUiState(
    val isLoading: Boolean,
    val isEdit: Boolean,
    val type: ActivityType,
    val feedKind: FeedKind,
    val bottleScale: VolumeScale,
    /** Slider position in [bottleScale]'s unit. */
    val bottleValue: Double,
    val food: String,
    val foodSuggestions: List<String>,
    val solidsAmount: SolidsAmount?,
    val reaction: Reaction?,
    val diaperKind: DiaperKind,
    val occurredAt: Instant,
    val note: String,
    val errors: Set<LogEntryError>,
    val today: LocalDate,
    val zone: ZoneId,
    val birthDate: LocalDate?,
    /** Set once saved or deleted; the screen then closes. */
    val isDone: Boolean,
) {
    val canSave: Boolean
        get() = !isLoading && when {
            type == ActivityType.DIAPER -> true
            feedKind == FeedKind.BOTTLE -> true
            else -> food.isNotBlank() && solidsAmount != null
        }
}

@HiltViewModel(assistedFactory = LogActivityViewModel.Factory::class)
class LogActivityViewModel @AssistedInject constructor(
    @Assisted private val key: LogActivityKey,
    private val logRepository: LogRepository,
    private val babyRepository: BabyRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val validate: ValidateLogEntryUseCase,
    private val deleteEntry: DeleteEntryUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: LogActivityKey): LogActivityViewModel
    }

    private val form = MutableStateFlow(
        LogActivityForm(
            type = key.initialType ?: ActivityType.FEED,
            occurredAt = clock.instant().truncatedTo(ChronoUnit.MINUTES),
        ),
    )
    private val isLoading = MutableStateFlow(key.entryId != null)
    private val errors = MutableStateFlow<Set<LogEntryError>>(emptySet())
    private val isDone = MutableStateFlow(false)

    val uiState: StateFlow<LogActivityUiState> = combine(
        combine(form, isLoading, errors, isDone, ::Quad),
        userPreferencesRepository.preferences,
        logRepository.observeFoods(),
        babyRepository.baby,
    ) { (form, loading, errors, done), preferences, foods, baby ->
        toUiState(form, loading, errors, done, preferences.unitSystem, foods, baby?.birthDate)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        toUiState(form.value, isLoading.value, emptySet(), false, UnitSystem.IMPERIAL, emptyList(), null),
    )

    init {
        key.entryId?.let { id ->
            viewModelScope.launch {
                val entry = logRepository.get(id)
                if (entry == null) {
                    isDone.value = true // Deleted meanwhile: nothing to edit.
                } else {
                    form.value = entry.toForm() ?: form.value
                    isLoading.value = false
                }
            }
        }
    }

    fun onTypeSelected(type: ActivityType) = editIfNew { it.copy(type = type) }

    fun onFeedKindSelected(kind: FeedKind) = editIfNew { it.copy(feedKind = kind) }

    fun onBottleChanged(value: Double) {
        val scale = uiState.value.bottleScale
        form.update { it.copy(bottle = scale.toVolume(scale.snap(value))) }
    }

    fun onFoodChanged(food: String) {
        form.update { it.copy(food = food) }
        errors.update { it - LogEntryError.MISSING_FOOD }
    }

    fun onSolidsAmountSelected(amount: SolidsAmount) = form.update { it.copy(solidsAmount = amount) }

    /** Tapping the selected reaction clears it: the reaction is optional. */
    fun onReactionToggled(reaction: Reaction) =
        form.update { it.copy(reaction = if (it.reaction == reaction) null else reaction) }

    fun onDiaperKindSelected(kind: DiaperKind) = form.update { it.copy(diaperKind = kind) }

    fun onOccurredAtChanged(instant: Instant) {
        form.update { it.copy(occurredAt = instant) }
        errors.update { it - LogEntryError.IN_FUTURE - LogEntryError.BEFORE_BIRTH }
    }

    fun onNoteChanged(note: String) = form.update { it.copy(note = note) }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val baby = babyRepository.getBaby() ?: return@launch
            val entry = buildEntry(form.value, state.bottleScale)
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

    /** Type and feed kind are fixed when editing: an entry doesn't change what it is. */
    private fun editIfNew(change: (LogActivityForm) -> LogActivityForm) {
        if (key.entryId == null) form.update(change)
    }

    private fun buildEntry(form: LogActivityForm, scale: VolumeScale): LogEntry {
        val id = key.entryId ?: idGenerator.newId()
        val date = form.occurredAt.atZone(clock.zone).toLocalDate()
        val note = form.note.trim().ifEmpty { null }
        return when (form.type) {
            ActivityType.DIAPER -> DiaperChange(id, form.occurredAt, date, note, form.diaperKind)
            ActivityType.FEED -> when (form.feedKind) {
                FeedKind.BOTTLE -> BottleFeed(id, form.occurredAt, date, note, form.bottle ?: scale.toVolume(scale.default))
                FeedKind.SOLIDS -> SolidsFeed(
                    id = id,
                    occurredAt = form.occurredAt,
                    date = date,
                    note = note,
                    food = form.food.trim(),
                    amount = checkNotNull(form.solidsAmount),
                    reaction = form.reaction,
                )
            }
        }
    }

    private fun toUiState(
        form: LogActivityForm,
        loading: Boolean,
        errors: Set<LogEntryError>,
        done: Boolean,
        unitSystem: UnitSystem,
        foods: List<String>,
        birthDate: LocalDate?,
    ): LogActivityUiState {
        val scale = VolumeScale.forUnits(unitSystem)
        return LogActivityUiState(
            isLoading = loading,
            isEdit = key.entryId != null,
            type = form.type,
            feedKind = form.feedKind,
            bottleScale = scale,
            bottleValue = form.bottle?.let(scale::fromVolume) ?: scale.default,
            food = form.food,
            foodSuggestions = suggestions(form.food, foods),
            solidsAmount = form.solidsAmount,
            reaction = form.reaction,
            diaperKind = form.diaperKind,
            occurredAt = form.occurredAt,
            note = form.note,
            errors = errors,
            today = LocalDate.now(clock),
            zone = clock.zone,
            birthDate = birthDate,
            isDone = done,
        )
    }

    private data class Quad(
        val form: LogActivityForm,
        val loading: Boolean,
        val errors: Set<LogEntryError>,
        val done: Boolean,
    )

    companion object {
        /** Common first foods, suggested before any solids are logged. */
        val STARTER_FOODS = listOf("Banana", "Sweet potato", "Avocado", "Rice cereal", "Peas", "Applesauce")
        private const val MAX_SUGGESTIONS = 6

        /** Past foods first, then starter foods, matching what's typed; never the exact text. */
        internal fun suggestions(typed: String, pastFoods: List<String>): List<String> {
            val query = typed.trim()
            return (pastFoods + STARTER_FOODS)
                .distinctBy { it.lowercase() }
                .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
                .take(MAX_SUGGESTIONS)
        }
    }
}

private fun LogEntry.toForm(): LogActivityForm? = when (this) {
    is BottleFeed -> LogActivityForm(ActivityType.FEED, FeedKind.BOTTLE, bottle = volume, occurredAt = occurredAt, note = note.orEmpty())
    is SolidsFeed -> LogActivityForm(
        type = ActivityType.FEED,
        feedKind = FeedKind.SOLIDS,
        food = food,
        solidsAmount = amount,
        reaction = reaction,
        occurredAt = occurredAt,
        note = note.orEmpty(),
    )
    is DiaperChange -> LogActivityForm(ActivityType.DIAPER, diaperKind = kind, occurredAt = occurredAt, note = note.orEmpty())
    is GrowthMeasurement -> null
}
