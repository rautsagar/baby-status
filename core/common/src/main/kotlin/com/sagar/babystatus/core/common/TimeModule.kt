package com.sagar.babystatus.core.common

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * The system clock, but re-reading the default zone on every call so a time zone
 * change while the app is running is picked up. Tests inject a fixed clock instead.
 */
private class SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
}

fun interface IdGenerator {
    fun newId(): String
}

class UuidIdGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun providesClock(): Clock = SystemZoneClock()

    @Provides
    fun providesIdGenerator(generator: UuidIdGenerator): IdGenerator = generator
}
