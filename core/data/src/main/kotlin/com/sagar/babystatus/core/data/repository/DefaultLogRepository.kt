package com.sagar.babystatus.core.data.repository

import android.util.Log
import com.sagar.babystatus.core.data.model.entityTypes
import com.sagar.babystatus.core.data.model.toEntity
import com.sagar.babystatus.core.data.model.toModelOrNull
import com.sagar.babystatus.core.database.dao.BabyDao
import com.sagar.babystatus.core.database.dao.LogEntryDao
import com.sagar.babystatus.core.database.model.LogEntryEntity
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.LatestGrowth
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.TimedEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "LogRepository"

internal class DefaultLogRepository @Inject constructor(
    private val babyDao: BabyDao,
    private val logEntryDao: LogEntryDao,
    private val clock: Clock,
) : LogRepository {

    private fun <T> forBaby(empty: T, query: (String) -> Flow<T>): Flow<T> = babyDao.forBaby(empty, query)

    override fun observeTimedEntriesOn(date: LocalDate, limit: Int): Flow<List<TimedEntry>> =
        forBaby(emptyList()) { id ->
            logEntryDao.observeTimedEntriesOn(id, date.toEpochDay(), limit)
                .map { rows -> rows.mapNotNull { it.toModelLogged() as? TimedEntry } }
        }

    override fun observeLast(type: ActivityType): Flow<TimedEntry?> =
        forBaby(null) { id ->
            logEntryDao.observeLatestOfTypes(id, type.entityTypes).map { it?.toModelLogged() as? TimedEntry }
        }

    override fun observeLatestGrowth(): Flow<LatestGrowth> =
        forBaby(LatestGrowth(weight = null, length = null)) { id ->
            combine(
                logEntryDao.observeLatestWeightGrams(id),
                logEntryDao.observeLatestLengthMm(id),
            ) { grams, mm -> LatestGrowth(weight = grams?.let(::Mass), length = mm?.let(::Length)) }
        }

    override fun observeFoods(): Flow<List<String>> = forBaby(emptyList(), logEntryDao::observeFoods)

    override suspend fun get(id: String): LogEntry? = logEntryDao.getById(id)?.toModelLogged()

    override suspend fun save(entry: LogEntry) {
        val babyId = checkNotNull(babyDao.getBaby()?.id) { "Can't log an entry before the baby exists" }
        val now = clock.millis()
        val createdAt = logEntryDao.getById(entry.id)?.createdAt ?: now
        logEntryDao.upsert(entry.toEntity(babyId, createdAt = createdAt, updatedAt = now))
    }

    private fun LogEntryEntity.toModelLogged(): LogEntry? =
        toModelOrNull().also { if (it == null) Log.w(TAG, "Skipping malformed $type row $id") }
}
