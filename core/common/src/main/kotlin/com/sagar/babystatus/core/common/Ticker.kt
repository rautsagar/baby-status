package com.sagar.babystatus.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

private const val MILLIS_PER_MINUTE = 60_000L

/** The current instant, now and at every minute boundary, so "2h 25m ago" and the day stay current. */
fun Clock.minuteTicks(): Flow<Instant> = flow {
    while (true) {
        val now = instant()
        emit(now)
        delay(MILLIS_PER_MINUTE - now.toEpochMilli() % MILLIS_PER_MINUTE)
    }
}

/** The local date, emitted when collection starts and again whenever midnight passes (or the zone changes). */
fun Clock.todayDates(): Flow<LocalDate> = minuteTicks().map { it.atZone(zone).toLocalDate() }.distinctUntilChanged()
