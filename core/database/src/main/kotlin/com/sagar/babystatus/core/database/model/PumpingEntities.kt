package com.sagar.babystatus.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A grown-up in the household. Not tied to a baby. Same sync-ready conventions as [BabyEntity]. */
@Entity(tableName = "caregiver")
data class CaregiverEntity(
    @PrimaryKey val id: String,
    /** `MOTHER` (the only role so far). */
    val role: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
) {
    companion object {
        const val ROLE_MOTHER = "MOTHER"
    }
}

@Entity(
    tableName = "pump_session",
    foreignKeys = [
        ForeignKey(
            entity = CaregiverEntity::class,
            parentColumns = ["id"],
            childColumns = ["caregiver_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["caregiver_id", "deleted_at", "local_epoch_day", "occurred_at"])],
)
data class PumpSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "caregiver_id") val caregiverId: String,
    /** Epoch millis UTC. Pumping is always timed. */
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "local_epoch_day") val localEpochDay: Long,
    @ColumnInfo(name = "volume_ml") val volumeMl: Double,
    /** `LEFT` / `RIGHT` / `BOTH`, or null when not recorded. */
    val side: String?,
    @ColumnInfo(name = "duration_min") val durationMin: Int?,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/**
 * One row of the merged History timeline: the columns of both [LogEntryEntity] and
 * [PumpSessionEntity], plus [source] saying which table it came from.
 */
data class TimelineRow(
    val source: String,
    val id: String,
    val type: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long?,
    @ColumnInfo(name = "local_epoch_day") val localEpochDay: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val note: String?,
    @ColumnInfo(name = "volume_ml") val volumeMl: Double?,
    val food: String?,
    @ColumnInfo(name = "solids_amount") val solidsAmount: String?,
    val reaction: String?,
    @ColumnInfo(name = "diaper_kind") val diaperKind: String?,
    @ColumnInfo(name = "weight_g") val weightGrams: Int?,
    @ColumnInfo(name = "length_mm") val lengthMm: Int?,
    val side: String?,
    @ColumnInfo(name = "duration_min") val durationMin: Int?,
) {
    companion object {
        const val SOURCE_LOG = "LOG"
        const val SOURCE_PUMP = "PUMP"
    }
}
