package com.sagar.babystatus.core.data.di

import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.CaregiverRepository
import com.sagar.babystatus.core.data.repository.DefaultBabyRepository
import com.sagar.babystatus.core.data.repository.DefaultCaregiverRepository
import com.sagar.babystatus.core.data.repository.DefaultLogRepository
import com.sagar.babystatus.core.data.repository.DefaultPhotoStore
import com.sagar.babystatus.core.data.repository.DefaultPumpRepository
import com.sagar.babystatus.core.data.repository.DefaultTimelineRepository
import com.sagar.babystatus.core.data.repository.DefaultUserPreferencesRepository
import com.sagar.babystatus.core.data.repository.LogRepository
import com.sagar.babystatus.core.data.repository.PhotoStore
import com.sagar.babystatus.core.data.repository.PumpRepository
import com.sagar.babystatus.core.data.repository.TimelineRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindsBabyRepository(repository: DefaultBabyRepository): BabyRepository

    @Binds
    abstract fun bindsLogRepository(repository: DefaultLogRepository): LogRepository

    @Binds
    abstract fun bindsUserPreferencesRepository(
        repository: DefaultUserPreferencesRepository,
    ): UserPreferencesRepository

    @Binds
    abstract fun bindsPhotoStore(store: DefaultPhotoStore): PhotoStore

    @Binds
    abstract fun bindsPumpRepository(repository: DefaultPumpRepository): PumpRepository

    @Binds
    abstract fun bindsCaregiverRepository(repository: DefaultCaregiverRepository): CaregiverRepository

    @Binds
    abstract fun bindsTimelineRepository(repository: DefaultTimelineRepository): TimelineRepository
}
