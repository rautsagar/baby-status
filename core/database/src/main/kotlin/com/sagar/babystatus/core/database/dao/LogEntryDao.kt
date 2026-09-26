package com.sagar.babystatus.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sagar.babystatus.core.database.model.LogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Query(
        """
        SELECT * FROM log_entry
        WHERE baby_id = :babyId AND deleted_at IS NULL AND local_epoch_day = :epochDay
            AND occurred_at IS NOT NULL
        ORDER BY occurred_at DESC
        LIMIT :limit
        """,
    )
    fun observeTimedEntriesOn(babyId: String, epochDay: Long, limit: Int): Flow<List<LogEntryEntity>>

    @Query(
        """
        SELECT * FROM log_entry
        WHERE baby_id = :babyId AND deleted_at IS NULL AND type IN (:types)
        ORDER BY occurred_at DESC
        LIMIT 1
        """,
    )
    fun observeLatestOfTypes(babyId: String, types: List<String>): Flow<LogEntryEntity?>

    @Query(
        """
        SELECT weight_g FROM log_entry
        WHERE baby_id = :babyId AND deleted_at IS NULL AND type = 'GROWTH' AND weight_g IS NOT NULL
        ORDER BY local_epoch_day DESC, updated_at DESC
        LIMIT 1
        """,
    )
    fun observeLatestWeightGrams(babyId: String): Flow<Int?>

    @Query(
        """
        SELECT length_mm FROM log_entry
        WHERE baby_id = :babyId AND deleted_at IS NULL AND type = 'GROWTH' AND length_mm IS NOT NULL
        ORDER BY local_epoch_day DESC, updated_at DESC
        LIMIT 1
        """,
    )
    fun observeLatestLengthMm(babyId: String): Flow<Int?>

    /** Distinct foods, most recently eaten first. */
    @Query(
        """
        SELECT food FROM log_entry
        WHERE baby_id = :babyId AND deleted_at IS NULL AND type = 'SOLIDS' AND food IS NOT NULL
        GROUP BY food COLLATE NOCASE
        ORDER BY MAX(occurred_at) DESC
        """,
    )
    fun observeFoods(babyId: String): Flow<List<String>>

    @Query("SELECT * FROM log_entry WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): LogEntryEntity?

    @Upsert
    suspend fun upsert(entry: LogEntryEntity)

    @Query("UPDATE log_entry SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long): Int

    @Query("UPDATE log_entry SET deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long): Int
}
