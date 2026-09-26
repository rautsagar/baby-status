package com.sagar.babystatus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sagar.babystatus.core.database.model.CaregiverEntity
import com.sagar.babystatus.core.database.model.PumpSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaregiverDao {
    @Query("SELECT * FROM caregiver WHERE role = 'MOTHER' AND deleted_at IS NULL ORDER BY created_at LIMIT 1")
    fun observeMother(): Flow<CaregiverEntity?>

    @Query("SELECT * FROM caregiver WHERE role = 'MOTHER' AND deleted_at IS NULL ORDER BY created_at LIMIT 1")
    suspend fun getMother(): CaregiverEntity?

    @Insert
    suspend fun insert(caregiver: CaregiverEntity)

    @Query("UPDATE caregiver SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long)

    /** The one mother row, created on first use (fresh installs, upgrades and restores alike). */
    @Transaction
    suspend fun getOrCreateMother(newMother: () -> CaregiverEntity): CaregiverEntity =
        getMother() ?: newMother().also { insert(it) }
}

@Dao
interface PumpSessionDao {
    @Query("SELECT * FROM pump_session WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): PumpSessionEntity?

    @Query("SELECT TOTAL(volume_ml) FROM pump_session WHERE deleted_at IS NULL AND local_epoch_day = :epochDay")
    fun observeTotalMlOn(epochDay: Long): Flow<Double>

    @Query("SELECT * FROM pump_session WHERE deleted_at IS NULL ORDER BY occurred_at DESC LIMIT 1")
    fun observeLatest(): Flow<PumpSessionEntity?>

    @Upsert
    suspend fun upsert(session: PumpSessionEntity)

    @Query("UPDATE pump_session SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long): Int

    @Query("UPDATE pump_session SET deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long): Int
}
