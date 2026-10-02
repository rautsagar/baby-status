package com.sagar.babystatus.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sync-ready conventions for every table: client-generated UUID ids, `created_at` /
 * `updated_at` stamps, and soft deletes via `deleted_at` (Undo now, tombstones later).
 */
@Entity(tableName = "baby")
data class BabyEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "birth_epoch_day") val birthEpochDay: Long,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

/**
 * One table for every kind of entry, discriminated by [type]. Type-specific columns are
 * nullable; the data layer maps each row to the matching model class.
 */
@Entity(
    tableName = "log_entry",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["baby_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["baby_id", "deleted_at", "local_epoch_day", "occurred_at"]),
        Index(value = ["baby_id", "type", "occurred_at"]),
    ],
)
data class LogEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "baby_id") val babyId: String,
    val type: String,
    /** Epoch millis UTC; null for GROWTH, which is date-only. */
    @ColumnInfo(name = "occurred_at") val occurredAt: Long?,
    /** Local day at write time, so grouping needs no time zone math in SQL. */
    @ColumnInfo(name = "local_epoch_day") val localEpochDay: Long,
    val note: String?,
    @ColumnInfo(name = "volume_ml") val volumeMl: Double? = null,
    val food: String? = null,
    @ColumnInfo(name = "solids_amount") val solidsAmount: String? = null,
    val reaction: String? = null,
    @ColumnInfo(name = "diaper_kind") val diaperKind: String? = null,
    @ColumnInfo(name = "weight_g") val weightGrams: Int? = null,
    @ColumnInfo(name = "length_mm") val lengthMm: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
) {
    companion object {
        const val TYPE_BOTTLE = "BOTTLE"
        const val TYPE_SOLIDS = "SOLIDS"
        const val TYPE_DIAPER = "DIAPER"
        const val TYPE_GROWTH = "GROWTH"
    }
}

