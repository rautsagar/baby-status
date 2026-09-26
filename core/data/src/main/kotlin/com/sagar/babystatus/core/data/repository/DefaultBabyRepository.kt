package com.sagar.babystatus.core.data.repository

import com.sagar.babystatus.core.common.IdGenerator
import com.sagar.babystatus.core.data.model.toModel
import com.sagar.babystatus.core.database.dao.BabyDao
import com.sagar.babystatus.core.database.model.BabyEntity
import com.sagar.babystatus.core.model.Baby
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

internal class DefaultBabyRepository @Inject constructor(
    private val babyDao: BabyDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : BabyRepository {

    override val baby: Flow<Baby?> = babyDao.observeBaby().map { it?.toModel() }

    override suspend fun getBaby(): Baby? = babyDao.getBaby()?.toModel()

    override suspend fun create(name: String, birthDate: LocalDate, photoPath: String?): Baby {
        val now = clock.millis()
        val entity = BabyEntity(
            id = idGenerator.newId(),
            name = name.trim(),
            birthEpochDay = birthDate.toEpochDay(),
            photoPath = photoPath,
            createdAt = now,
            updatedAt = now,
        )
        babyDao.upsert(entity)
        return entity.toModel()
    }

    override suspend fun update(baby: Baby) {
        val existing = checkNotNull(babyDao.getBaby()) { "No baby to update" }
        babyDao.upsert(
            existing.copy(
                name = baby.name.trim(),
                birthEpochDay = baby.birthDate.toEpochDay(),
                photoPath = baby.photoPath,
                updatedAt = clock.millis(),
            ),
        )
    }
}
