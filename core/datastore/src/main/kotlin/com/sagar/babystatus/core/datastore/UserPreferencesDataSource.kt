package com.sagar.babystatus.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            unitSystem = prefs[UNIT_SYSTEM]
                ?.let { stored -> UnitSystem.entries.firstOrNull { it.name == stored } }
                ?: defaultUnitSystem(),
        )
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { it[UNIT_SYSTEM] = unitSystem.name }
    }

    private companion object {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")

        /** Until the user picks, follow the countries that still use imperial units. */
        fun defaultUnitSystem(): UnitSystem =
            if (Locale.getDefault().country in setOf("US", "LR", "MM")) UnitSystem.IMPERIAL else UnitSystem.METRIC
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    /** Also referenced by the Auto Backup rules in :app. */
    private const val FILE_NAME = "user_preferences"

    @Provides
    @Singleton
    fun providesPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) }
}
