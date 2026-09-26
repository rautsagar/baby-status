package com.sagar.babystatus.core.data.model

import com.sagar.babystatus.core.database.model.CaregiverEntity
import com.sagar.babystatus.core.database.model.LogEntryEntity
import com.sagar.babystatus.core.database.model.PumpSessionEntity
import com.sagar.babystatus.core.database.model.TimelineRow
import com.sagar.babystatus.core.model.BreastSide
import com.sagar.babystatus.core.model.Caregiver
import com.sagar.babystatus.core.model.CaregiverRole
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.Volume
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Shown until the mother row exists, and used as the name of the row when it's created. */
internal const val DEFAULT_MOTHER_NAME = "Mom"

internal fun CaregiverEntity.toModel() = Caregiver(
    id = id,
    role = role.toEnumOrNull<CaregiverRole>() ?: CaregiverRole.MOTHER,
    name = name,
)

internal fun PumpSessionEntity.toModel() = PumpSession(
    id = id,
    occurredAt = Instant.ofEpochMilli(occurredAt),
    date = LocalDate.ofEpochDay(localEpochDay),
    note = note,
    volume = Volume(volumeMl),
    side = side.toEnumOrNull<BreastSide>(),
    duration = durationMin?.let { Duration.ofMinutes(it.toLong()) },
)

internal fun PumpSession.toEntity(caregiverId: String, createdAt: Long, updatedAt: Long) = PumpSessionEntity(
    id = id,
    caregiverId = caregiverId,
    occurredAt = occurredAt.toEpochMilli(),
    localEpochDay = date.toEpochDay(),
    volumeMl = volume.ml,
    side = side?.name,
    durationMin = duration?.toMinutes()?.toInt(),
    note = note?.takeIf { it.isNotBlank() },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** Maps a merged History row to the entry it came from, or null if it's malformed. */
internal fun TimelineRow.toModelOrNull(): TimelineEntry? = when (source) {
    TimelineRow.SOURCE_PUMP -> PumpSession(
        id = id,
        occurredAt = Instant.ofEpochMilli(occurredAt ?: return null),
        date = LocalDate.ofEpochDay(localEpochDay),
        note = note,
        volume = Volume(volumeMl ?: return null),
        side = side.toEnumOrNull<BreastSide>(),
        duration = durationMin?.let { Duration.ofMinutes(it.toLong()) },
    )
    TimelineRow.SOURCE_LOG -> LogEntryEntity(
        id = id,
        babyId = "",
        type = type,
        occurredAt = occurredAt,
        localEpochDay = localEpochDay,
        note = note,
        volumeMl = volumeMl,
        food = food,
        solidsAmount = solidsAmount,
        reaction = reaction,
        diaperKind = diaperKind,
        weightGrams = weightGrams,
        lengthMm = lengthMm,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ).toModelOrNull()
    else -> null
}
