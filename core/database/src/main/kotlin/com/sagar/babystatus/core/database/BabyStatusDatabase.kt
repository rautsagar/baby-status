package com.sagar.babystatus.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sagar.babystatus.core.database.dao.BabyDao
import com.sagar.babystatus.core.database.dao.CaregiverDao
import com.sagar.babystatus.core.database.dao.LogEntryDao
import com.sagar.babystatus.core.database.dao.PumpSessionDao
import com.sagar.babystatus.core.database.dao.TimelineDao
import com.sagar.babystatus.core.database.model.BabyEntity
import com.sagar.babystatus.core.database.model.CaregiverEntity
import com.sagar.babystatus.core.database.model.LogEntryEntity
import com.sagar.babystatus.core.database.model.PumpSessionEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [BabyEntity::class, LogEntryEntity::class, CaregiverEntity::class, PumpSessionEntity::class],
    version = 2,
    exportSchema = true,
    // v2 only adds tables (caregiver, pump_session), which Room can migrate from the
    // exported schemas. The Mom row is created lazily, so no data migration is needed.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class BabyStatusDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun caregiverDao(): CaregiverDao
    abstract fun pumpSessionDao(): PumpSessionDao
    abstract fun timelineDao(): TimelineDao

    companion object {
        /** Also referenced by the Auto Backup rules in :app. */
        const val NAME = "baby-status.db"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): BabyStatusDatabase =
        Room.databaseBuilder(context, BabyStatusDatabase::class.java, BabyStatusDatabase.NAME).build()

    @Provides
    fun providesBabyDao(database: BabyStatusDatabase): BabyDao = database.babyDao()

    @Provides
    fun providesLogEntryDao(database: BabyStatusDatabase): LogEntryDao = database.logEntryDao()

    @Provides
    fun providesCaregiverDao(database: BabyStatusDatabase): CaregiverDao = database.caregiverDao()

    @Provides
    fun providesPumpSessionDao(database: BabyStatusDatabase): PumpSessionDao = database.pumpSessionDao()

    @Provides
    fun providesTimelineDao(database: BabyStatusDatabase): TimelineDao = database.timelineDao()
}
