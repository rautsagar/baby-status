package com.sagar.babystatus.core.data.repository

import com.sagar.babystatus.core.datastore.UserPreferencesDataSource
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultUserPreferencesRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = dataSource.preferences

    override suspend fun setUnitSystem(unitSystem: UnitSystem) = dataSource.setUnitSystem(unitSystem)
}
