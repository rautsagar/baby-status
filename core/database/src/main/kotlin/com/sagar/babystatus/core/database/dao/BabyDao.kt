package com.sagar.babystatus.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sagar.babystatus.core.database.model.BabyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyDao {
    /** The app shows one baby: the first one created that isn't deleted. */
    @Query("SELECT * FROM baby WHERE deleted_at IS NULL ORDER BY created_at LIMIT 1")
    fun observeBaby(): Flow<BabyEntity?>

    @Query("SELECT * FROM baby WHERE deleted_at IS NULL ORDER BY created_at LIMIT 1")
    suspend fun getBaby(): BabyEntity?

    @Upsert
    suspend fun upsert(baby: BabyEntity)
}
