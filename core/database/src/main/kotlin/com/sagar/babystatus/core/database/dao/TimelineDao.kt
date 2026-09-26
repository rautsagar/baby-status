package com.sagar.babystatus.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.sagar.babystatus.core.database.model.DailyTotalsRow
import com.sagar.babystatus.core.database.model.TimelineRow
import kotlinx.coroutines.flow.Flow

/**
 * History spans two tables: the baby's `log_entry` and Mom's `pump_session`. Both queries
 * read them as one with UNION ALL; Room observes both tables, so a write to either
 * refreshes History.
 */
@Dao
interface TimelineDao {
    /**
     * Timeline order: newest day first, date-only growth rows at the top of their day,
     * then newest time first. (The union is wrapped because a compound SELECT can only be
     * ordered by plain result columns.)
     */
    @Query(
        """
        SELECT * FROM (
        SELECT 'LOG' AS source, id, type, occurred_at, local_epoch_day, created_at, updated_at, note,
            volume_ml, food, solids_amount, reaction, diaper_kind, weight_g, length_mm,
            NULL AS side, NULL AS duration_min
        FROM log_entry WHERE baby_id = :babyId AND deleted_at IS NULL
        UNION ALL
        SELECT 'PUMP', id, 'PUMP', occurred_at, local_epoch_day, created_at, updated_at, note,
            volume_ml, NULL, NULL, NULL, NULL, NULL, NULL,
            side, duration_min
        FROM pump_session WHERE deleted_at IS NULL
        )
        ORDER BY local_epoch_day DESC, occurred_at IS NULL DESC, occurred_at DESC, created_at DESC
        """,
    )
    fun pagingSource(babyId: String): PagingSource<Int, TimelineRow>

    @Query(
        """
        SELECT local_epoch_day,
            TOTAL(bottle_ml) AS bottle_ml,
            TOTAL(pumped_ml) AS pumped_ml,
            SUM(pee) AS pee_count,
            SUM(potty) AS potty_count
        FROM (
            SELECT local_epoch_day,
                CASE WHEN type = 'BOTTLE' THEN volume_ml ELSE 0 END AS bottle_ml,
                0 AS pumped_ml,
                CASE WHEN type = 'DIAPER' AND diaper_kind = 'PEE' THEN 1 ELSE 0 END AS pee,
                CASE WHEN type = 'DIAPER' AND diaper_kind = 'POTTY' THEN 1 ELSE 0 END AS potty
            FROM log_entry WHERE baby_id = :babyId AND deleted_at IS NULL
            UNION ALL
            SELECT local_epoch_day, 0, volume_ml, 0, 0
            FROM pump_session WHERE deleted_at IS NULL
        )
        GROUP BY local_epoch_day
        """,
    )
    fun observeDailyTotals(babyId: String): Flow<List<DailyTotalsRow>>
}
