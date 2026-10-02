package com.sagar.babystatus.core.testing

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.CaregiverRepository
import com.sagar.babystatus.core.data.repository.LogRepository
import com.sagar.babystatus.core.data.repository.PhotoStore
import com.sagar.babystatus.core.data.repository.PumpRepository
import com.sagar.babystatus.core.data.repository.TimelineRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.Caregiver
import com.sagar.babystatus.core.model.CaregiverRole
import com.sagar.babystatus.core.model.DaySummary
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.LatestGrowth
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.PumpSummary
import com.sagar.babystatus.core.model.SolidsFeed
import com.sagar.babystatus.core.model.TimedEntry
import com.sagar.babystatus.core.model.TimelineDay
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.UserPreferences
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.model.activityType
import com.sagar.babystatus.core.model.occurredAtOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeBabyRepository(initial: Baby? = null) : BabyRepository {
    private val state = MutableStateFlow(initial)
    override val baby: Flow<Baby?> = state
    val current: Baby? get() = state.value

    override suspend fun getBaby(): Baby? = state.value

    override suspend fun create(name: String, birthDate: LocalDate, photoPath: String?): Baby =
        Baby(id = "baby-1", name = name.trim(), birthDate = birthDate, photoPath = photoPath).also { state.value = it }

    override suspend fun update(baby: Baby) {
        state.value = baby
    }
}

/** In-memory rows with soft delete, shared by the log and pump fakes. */
class FakeTable<T : TimelineEntry>(initial: List<T>) {
    private val rows = MutableStateFlow(initial.associateBy { it.id })
    private val deleted = MutableStateFlow<Map<String, T>>(emptyMap())

    val flow: Flow<List<T>> = rows.map { it.values.sortedWith(TIMELINE_ORDER) }
    val visible: List<T> get() = rows.value.values.toList()
    val deletedIds: Set<String> get() = deleted.value.keys

    operator fun get(id: String): T? = rows.value[id]

    fun put(row: T) = rows.update { it + (row.id to row) }

    /** Returns whether a row with this id was here to delete. */
    fun delete(id: String): Boolean {
        val row = rows.value[id] ?: return false
        rows.update { it - id }
        deleted.update { it + (id to row) }
        return true
    }

    fun restore(id: String): Boolean {
        val row = deleted.value[id] ?: return false
        deleted.update { it - id }
        rows.update { it + (id to row) }
        return true
    }
}

/** Same order as the Room timeline: newest day, date-only growth first, then newest time. */
val TIMELINE_ORDER: Comparator<TimelineEntry> = compareByDescending<TimelineEntry> { it.date }
    .thenByDescending { it is GrowthMeasurement }
    .thenByDescending { it.occurredAtOrNull }

/** In-memory baby log with the same query semantics as the Room implementation. */
class FakeLogRepository(initial: List<LogEntry> = emptyList()) : LogRepository {
    val table = FakeTable(initial)

    /** Visible (not deleted) entries. */
    val all: List<LogEntry> get() = table.visible
    val deletedIds: Set<String> get() = table.deletedIds

    override fun observeTimedEntriesOn(date: LocalDate, limit: Int): Flow<List<TimedEntry>> =
        table.flow.map { list -> list.filterIsInstance<TimedEntry>().filter { it.date == date }.take(limit) }

    override fun observeLast(type: ActivityType): Flow<TimedEntry?> =
        table.flow.map { list ->
            list.filterIsInstance<TimedEntry>().filter { it.activityType == type }.maxByOrNull { it.occurredAt }
        }

    override fun observeLatestGrowth(): Flow<LatestGrowth> = table.flow.map { list ->
        val growth = list.filterIsInstance<GrowthMeasurement>()
        LatestGrowth(
            weight = growth.firstOrNull { it.weight != null }?.weight,
            length = growth.firstOrNull { it.length != null }?.length,
        )
    }

    override fun observeFoods(): Flow<List<String>> = table.flow.map { list ->
        list.filterIsInstance<SolidsFeed>().sortedByDescending { it.occurredAt }.map { it.food }
            .distinctBy { it.lowercase() }
    }

    override suspend fun get(id: String): LogEntry? = table[id]

    override suspend fun save(entry: LogEntry) = table.put(entry)
}

class FakePumpRepository(initial: List<PumpSession> = emptyList()) : PumpRepository {
    val table = FakeTable(initial)
    val all: List<PumpSession> get() = table.visible
    val deletedIds: Set<String> get() = table.deletedIds

    override fun observeSummary(today: LocalDate): Flow<PumpSummary> = table.flow.map { list ->
        PumpSummary(
            pumpedToday = Volume(list.filter { it.date == today }.sumOf { it.volume.ml }),
            last = list.maxByOrNull { it.occurredAt },
        )
    }

    override suspend fun get(id: String): PumpSession? = table[id]

    override suspend fun save(session: PumpSession) = table.put(session)
}

class FakeCaregiverRepository(name: String = "Mom") : CaregiverRepository {
    private val state = MutableStateFlow(Caregiver("mother-1", CaregiverRole.MOTHER, name))
    override val mother: Flow<Caregiver> = state

    override suspend fun renameMother(name: String) {
        state.update { it.copy(name = name.trim()) }
    }
}

/** History over both fakes, routing delete/undo like the Room implementation. */
class FakeTimelineRepository(
    private val log: FakeLogRepository = FakeLogRepository(),
    private val pump: FakePumpRepository = FakePumpRepository(),
) : TimelineRepository {
    private val merged: Flow<List<TimelineEntry>> =
        combine(log.table.flow, pump.table.flow) { entries, sessions -> (entries + sessions).sortedWith(TIMELINE_ORDER) }

    /** One item per day that has entries, newest first, like the Room implementation. */
    private val days: Flow<List<TimelineDay>> = merged.map { list ->
        list.groupBy { it.date }.entries.sortedByDescending { it.key }.map { (date, entries) -> TimelineDay(date, entries) }
    }

    override fun historyDays(): Flow<PagingData<TimelineDay>> = days.map { PagingData.from(it, SETTLED) }

    override suspend fun dayIndex(date: LocalDate): Int = days.first().count { it.date.isAfter(date) }

    override fun observeSummaries(from: LocalDate): Flow<Map<LocalDate, DaySummary>> = days.map { list ->
        list.filter { !it.date.isBefore(from) }.associate { it.date to it.summary }
    }

    override suspend fun delete(id: String) {
        if (!log.table.delete(id)) pump.table.delete(id)
    }

    override suspend fun undoDelete(id: String) {
        if (!log.table.restore(id)) pump.table.restore(id)
    }
}

class FakeUserPreferencesRepository(
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
) : UserPreferencesRepository {
    private val state = MutableStateFlow(UserPreferences(unitSystem))
    override val preferences: Flow<UserPreferences> = state

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        state.update { it.copy(unitSystem = unitSystem) }
    }
}

class FakePhotoStore : PhotoStore {
    val imported = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    override suspend fun importPhoto(uri: String): String {
        imported += uri
        return "photos/imported-${imported.size}.jpg"
    }

    override suspend fun delete(path: String) {
        deletedPaths += path
    }
}

/** A static list that says it has finished loading, so Paging's test tools don't wait for more. */
private val SETTLED = LoadStates(LoadState.NotLoading(true), LoadState.NotLoading(true), LoadState.NotLoading(true))
