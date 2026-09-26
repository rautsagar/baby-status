package com.sagar.babystatus.core.testing

import com.sagar.babystatus.core.common.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Replaces Dispatchers.Main so viewModelScope runs on the test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** A clock tests can set and move forward. */
class MutableClock(
    var now: Instant,
    private val zoneId: ZoneId = ZoneId.of("America/New_York"),
) : Clock() {
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now

    fun advanceBy(duration: Duration) {
        now = now.plus(duration)
    }

    companion object {
        /** A clock at the given local date-time in New York. */
        fun at(localDateTime: LocalDateTime, zone: ZoneId = ZoneId.of("America/New_York")) =
            MutableClock(localDateTime.atZone(zone).toInstant(), zone)
    }
}

/** Ids "id-1", "id-2", … so tests can predict them. */
class SequentialIdGenerator : IdGenerator {
    private var next = 1
    override fun newId(): String = "id-${next++}"
}
