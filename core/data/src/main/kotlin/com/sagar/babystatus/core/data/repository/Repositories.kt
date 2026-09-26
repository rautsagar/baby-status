package com.sagar.babystatus.core.data.repository

import androidx.paging.PagingData
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.Caregiver
import com.sagar.babystatus.core.model.DailyTotals
import com.sagar.babystatus.core.model.LatestGrowth
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.PumpSummary
import com.sagar.babystatus.core.model.TimedEntry
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BabyRepository {
    /** The single baby, or null before onboarding. */
    val baby: Flow<Baby?>

    suspend fun getBaby(): Baby?

    suspend fun create(name: String, birthDate: LocalDate, photoPath: String?): Baby

    suspend fun update(baby: Baby)
}

/** Every query is scoped to the single baby; with no baby they emit empty results. */
interface LogRepository {
    /** Entries with a time of day logged on [date], newest first. */
    fun observeTimedEntriesOn(date: LocalDate, limit: Int): Flow<List<TimedEntry>>

    fun observeLast(type: ActivityType): Flow<TimedEntry?>

    fun observeLatestGrowth(): Flow<LatestGrowth>

    /** Distinct foods logged as solids, most recently eaten first. */
    fun observeFoods(): Flow<List<String>>

    suspend fun get(id: String): LogEntry?

    /** Inserts or updates, stamping `updated_at`. */
    suspend fun save(entry: LogEntry)
}

/** Mom's pumping sessions. The data layer attaches the mother, as it does the baby. */
interface PumpRepository {
    fun observeSummary(today: LocalDate): Flow<PumpSummary>

    suspend fun get(id: String): PumpSession?

    /** Inserts or updates; creates the mother row on the first-ever save. */
    suspend fun save(session: PumpSession)
}

interface CaregiverRepository {
    /** The mother, or a default "Mom" until her row exists (it's created on first use). */
    val mother: Flow<Caregiver>

    suspend fun renameMother(name: String)
}

/** History and delete/undo, across the baby's entries and Mom's pumping. */
interface TimelineRepository {
    /** Timeline order: newest day first, growth at the top of its day, newest time first. */
    fun history(): Flow<PagingData<TimelineEntry>>

    fun observeDailyTotals(): Flow<Map<LocalDate, DailyTotals>>

    /** Soft delete of whichever kind of entry has this id; [undoDelete] restores it. */
    suspend fun delete(id: String)

    suspend fun undoDelete(id: String)
}

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setUnitSystem(unitSystem: UnitSystem)
}

/** Keeps the baby's photo in app-private storage. Paths are relative to the files dir. */
interface PhotoStore {
    /** Copies a picked image (content uri) into app storage, downscaled; returns its path. */
    suspend fun importPhoto(uri: String): String

    suspend fun delete(path: String)
}
