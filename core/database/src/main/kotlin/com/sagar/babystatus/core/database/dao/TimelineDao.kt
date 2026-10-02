package com.sagar.babystatus.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.sagar.babystatus.core.database.model.TimelineDayRow
import com.sagar.babystatus.core.database.model.TimelineRow
import kotlinx.coroutines.flow.Flow

/**
 * Every row of the timeline: the baby's `log_entry` rows and Mom's `pump_session` rows read
 * as one with UNION ALL. Room observes both tables, so a write to either refreshes the
 * queries below.
 */
private const val TIMELINE_ROWS = """
    SELECT 'LOG' AS source, id, type, occurred_at, local_epoch_day, created_at, updated_at, note,
        volume_ml, food, solids_amount, reaction, diaper_kind, weight_g, length_mm,
        NULL AS side, NULL AS duration_min
    FROM log_entry WHERE baby_id = :babyId AND deleted_at IS NULL
    UNION ALL
    SELECT 'PUMP', id, 'PUMP', occurred_at, local_epoch_day, created_at, updated_at, note,
        volume_ml, NULL, NULL, NULL, NULL, NULL, NULL,
        side, duration_min
    FROM pump_session WHERE deleted_at IS NULL
"""

/**
 * Timeline order: newest day first, date-only growth rows at the top of their day, then
 * newest time first. (The union is wrapped in a subquery because a compound SELECT can
 * only be ordered by plain result columns.)
 */
private const val TIMELINE_ORDER =
    "ORDER BY local_epoch_day DESC, occurred_at IS NULL DESC, occurred_at DESC, created_at DESC"

@Dao
interface TimelineDao {
    /**
     * The days that have any entries, newest first: History's list has one item per day.
     * Room counts the rows, so the paged list has placeholders and a day's position is known
     * before its page loads (see [countDaysAfter]).
     */
    @Query("SELECT DISTINCT local_epoch_day FROM ($TIMELINE_ROWS) ORDER BY local_epoch_day DESC")
    fun pagingSourceDays(babyId: String): PagingSource<Int, TimelineDayRow>

    @Query("SELECT * FROM ($TIMELINE_ROWS) WHERE local_epoch_day = :day $TIMELINE_ORDER")
    suspend fun entriesOnDay(babyId: String, day: Long): List<TimelineRow>

    /** How many days with entries are newer than [day]: exactly its index in [pagingSourceDays]. */
    @Query("SELECT COUNT(DISTINCT local_epoch_day) FROM ($TIMELINE_ROWS) WHERE local_epoch_day > :day")
    suspend fun countDaysAfter(babyId: String, day: Long): Int

    /** Every entry from [fromDay] on, for the Summary screen (about a week of rows). */
    @Query("SELECT * FROM ($TIMELINE_ROWS) WHERE local_epoch_day >= :fromDay $TIMELINE_ORDER")
    fun observeEntriesSince(babyId: String, fromDay: Long): Flow<List<TimelineRow>>
}
