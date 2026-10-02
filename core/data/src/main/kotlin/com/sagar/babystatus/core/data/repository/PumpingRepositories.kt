package com.sagar.babystatus.core.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.sagar.babystatus.core.common.IdGenerator
import com.sagar.babystatus.core.data.model.DEFAULT_MOTHER_NAME
import com.sagar.babystatus.core.data.model.toEntity
import com.sagar.babystatus.core.data.model.toModel
import com.sagar.babystatus.core.data.model.toModelOrNull
import com.sagar.babystatus.core.database.dao.BabyDao
import com.sagar.babystatus.core.database.dao.CaregiverDao
import com.sagar.babystatus.core.database.dao.LogEntryDao
import com.sagar.babystatus.core.database.dao.PumpSessionDao
import com.sagar.babystatus.core.database.dao.TimelineDao
import com.sagar.babystatus.core.database.model.CaregiverEntity
import com.sagar.babystatus.core.database.model.TimelineRow
import com.sagar.babystatus.core.model.Caregiver
import com.sagar.babystatus.core.model.CaregiverRole
import com.sagar.babystatus.core.model.DaySummary
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.PumpSummary
import com.sagar.babystatus.core.model.TimelineDay
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.summarize
import com.sagar.babystatus.core.model.Volume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "TimelineRepository"
private const val DAYS_PER_PAGE = 20

/** Get-or-create the one mother row; the first pump save or rename creates it. */
internal suspend fun CaregiverDao.mother(clock: Clock, idGenerator: IdGenerator): CaregiverEntity =
    getOrCreateMother {
        val now = clock.millis()
        CaregiverEntity(
            id = idGenerator.newId(),
            role = CaregiverEntity.ROLE_MOTHER,
            name = DEFAULT_MOTHER_NAME,
            createdAt = now,
            updatedAt = now,
        )
    }

internal class DefaultCaregiverRepository @Inject constructor(
    private val caregiverDao: CaregiverDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : CaregiverRepository {

    override val mother: Flow<Caregiver> = caregiverDao.observeMother().map { entity ->
        entity?.toModel() ?: Caregiver(id = "", role = CaregiverRole.MOTHER, name = DEFAULT_MOTHER_NAME)
    }

    override suspend fun renameMother(name: String) {
        val mother = caregiverDao.mother(clock, idGenerator)
        caregiverDao.rename(mother.id, name.trim(), clock.millis())
    }
}

internal class DefaultPumpRepository @Inject constructor(
    private val pumpSessionDao: PumpSessionDao,
    private val caregiverDao: CaregiverDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : PumpRepository {

    override fun observeSummary(today: LocalDate): Flow<PumpSummary> = combine(
        pumpSessionDao.observeTotalMlOn(today.toEpochDay()),
        pumpSessionDao.observeLatest(),
    ) { totalMl, latest -> PumpSummary(pumpedToday = Volume(totalMl), last = latest?.toModel()) }

    override suspend fun get(id: String): PumpSession? = pumpSessionDao.getById(id)?.toModel()

    override suspend fun save(session: PumpSession) {
        val mother = caregiverDao.mother(clock, idGenerator)
        val now = clock.millis()
        val createdAt = pumpSessionDao.getById(session.id)?.createdAt ?: now
        pumpSessionDao.upsert(session.toEntity(mother.id, createdAt = createdAt, updatedAt = now))
    }
}

internal class DefaultTimelineRepository @Inject constructor(
    private val babyDao: BabyDao,
    private val timelineDao: TimelineDao,
    private val logEntryDao: LogEntryDao,
    private val pumpSessionDao: PumpSessionDao,
    private val clock: Clock,
) : TimelineRepository {

    override fun historyDays(): Flow<PagingData<TimelineDay>> =
        babyDao.forBaby(PagingData.empty()) { babyId ->
            Pager(PagingConfig(pageSize = DAYS_PER_PAGE)) { timelineDao.pagingSourceDays(babyId) }
                .flow
                .map { page ->
                    page
                        .map { row -> TimelineDay(LocalDate.ofEpochDay(row.day), entriesOn(babyId, row.day)) }
                        // A day emptied between the two queries; the invalidation that follows drops it.
                        .filter { it.entries.isNotEmpty() }
                }
        }

    override suspend fun dayIndex(date: LocalDate): Int {
        val babyId = babyDao.getBaby()?.id ?: return 0
        return timelineDao.countDaysAfter(babyId, date.toEpochDay())
    }

    override fun observeSummaries(from: LocalDate): Flow<Map<LocalDate, DaySummary>> =
        babyDao.forBaby(emptyMap()) { babyId ->
            timelineDao.observeEntriesSince(babyId, from.toEpochDay()).map { rows ->
                rows.mapNotNull { it.toModelLogged() }
                    .groupBy { it.date }
                    .mapValues { (date, entries) -> summarize(date, entries) }
            }
        }

    private suspend fun entriesOn(babyId: String, day: Long): List<TimelineEntry> =
        timelineDao.entriesOnDay(babyId, day).mapNotNull { it.toModelLogged() }

    private fun TimelineRow.toModelLogged(): TimelineEntry? =
        toModelOrNull().also { if (it == null) Log.w(TAG, "Skipping malformed $source/$type row $id") }

    /** Ids are UUIDs, so at most one table has a row with this id. */
    override suspend fun delete(id: String) {
        val now = clock.millis()
        if (logEntryDao.softDelete(id, now) == 0) pumpSessionDao.softDelete(id, now)
    }

    override suspend fun undoDelete(id: String) {
        val now = clock.millis()
        if (logEntryDao.restore(id, now) == 0) pumpSessionDao.restore(id, now)
    }
}
