package com.sagar.babystatus.core.data.model

import com.sagar.babystatus.core.database.model.BabyEntity
import com.sagar.babystatus.core.database.model.LogEntryEntity
import com.sagar.babystatus.core.database.model.LogEntryEntity.Companion.TYPE_BOTTLE
import com.sagar.babystatus.core.database.model.LogEntryEntity.Companion.TYPE_DIAPER
import com.sagar.babystatus.core.database.model.LogEntryEntity.Companion.TYPE_GROWTH
import com.sagar.babystatus.core.database.model.LogEntryEntity.Companion.TYPE_SOLIDS
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.Reaction
import com.sagar.babystatus.core.model.SolidsAmount
import com.sagar.babystatus.core.model.SolidsFeed
import com.sagar.babystatus.core.model.TimedEntry
import com.sagar.babystatus.core.model.Volume
import java.time.Instant
import java.time.LocalDate

internal fun BabyEntity.toModel() = Baby(
    id = id,
    name = name,
    birthDate = LocalDate.ofEpochDay(birthEpochDay),
    photoPath = photoPath,
)

internal val ActivityType.entityTypes: List<String>
    get() = when (this) {
        ActivityType.FEED -> listOf(TYPE_BOTTLE, TYPE_SOLIDS)
        ActivityType.DIAPER -> listOf(TYPE_DIAPER)
    }

/**
 * Maps a row to its model, or null if the row is malformed (e.g. a BOTTLE without a
 * volume). Bad rows are skipped rather than crashing the screen that shows them.
 */
internal fun LogEntryEntity.toModelOrNull(): LogEntry? {
    val date = LocalDate.ofEpochDay(localEpochDay)
    val time = occurredAt?.let(Instant::ofEpochMilli)
    return when (type) {
        TYPE_BOTTLE -> BottleFeed(
            id = id,
            occurredAt = time ?: return null,
            date = date,
            note = note,
            volume = Volume(volumeMl ?: return null),
        )
        TYPE_SOLIDS -> SolidsFeed(
            id = id,
            occurredAt = time ?: return null,
            date = date,
            note = note,
            food = food ?: return null,
            amount = solidsAmount.toEnumOrNull<SolidsAmount>() ?: return null,
            reaction = reaction.toEnumOrNull<Reaction>(),
        )
        TYPE_DIAPER -> DiaperChange(
            id = id,
            occurredAt = time ?: return null,
            date = date,
            note = note,
            kind = diaperKind.toEnumOrNull<DiaperKind>() ?: return null,
        )
        TYPE_GROWTH -> {
            if (weightGrams == null && lengthMm == null) return null
            GrowthMeasurement(
                id = id,
                date = date,
                note = note,
                weight = weightGrams?.let(::Mass),
                length = lengthMm?.let(::Length),
            )
        }
        else -> null
    }
}

internal fun LogEntry.toEntity(babyId: String, createdAt: Long, updatedAt: Long): LogEntryEntity {
    val base = LogEntryEntity(
        id = id,
        babyId = babyId,
        type = "",
        occurredAt = (this as? TimedEntry)?.occurredAt?.toEpochMilli(),
        localEpochDay = date.toEpochDay(),
        note = note?.takeIf { it.isNotBlank() },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
    return when (this) {
        is BottleFeed -> base.copy(type = TYPE_BOTTLE, volumeMl = volume.ml)
        is SolidsFeed -> base.copy(
            type = TYPE_SOLIDS,
            food = food.trim(),
            solidsAmount = amount.name,
            reaction = reaction?.name,
        )
        is DiaperChange -> base.copy(type = TYPE_DIAPER, diaperKind = kind.name)
        is GrowthMeasurement -> base.copy(
            type = TYPE_GROWTH,
            weightGrams = weight?.grams,
            lengthMm = length?.mm,
        )
    }
}

internal inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
