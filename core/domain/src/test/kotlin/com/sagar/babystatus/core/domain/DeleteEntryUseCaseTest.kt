package com.sagar.babystatus.core.domain

import app.cash.turbine.test
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakePumpRepository
import com.sagar.babystatus.core.testing.FakeTimelineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteEntryUseCaseTest {
    private val entry = DiaperChange("d1", Instant.parse("2026-09-24T18:05:00Z"), LocalDate.of(2026, 9, 24), null, DiaperKind.PEE)
    private val repository = FakeLogRepository(listOf(entry))
    private val deleteEntry = DeleteEntryUseCase(FakeTimelineRepository(repository))

    @Test
    fun `deleting announces the id and undo restores the entry`() = runTest {
        deleteEntry.deletedIds.test {
            deleteEntry("d1")
            assertEquals("d1", awaitItem())
        }
        assertTrue(repository.all.isEmpty())

        deleteEntry.undo("d1")
        assertEquals(listOf(entry), repository.all)
    }

    @Test
    fun `pumping sessions are deleted and restored through the same path`() = runTest {
        val session = PumpSession("p1", Instant.parse("2026-09-24T17:00:00Z"), LocalDate.of(2026, 9, 24), null, Volume.ofOz(4.0), null, null)
        val pump = FakePumpRepository(listOf(session))
        val deleteEntry = DeleteEntryUseCase(FakeTimelineRepository(repository, pump))

        deleteEntry("p1")
        assertEquals(setOf("p1"), pump.deletedIds)
        assertEquals(listOf(entry), repository.all, "the baby's log is untouched")

        deleteEntry.undo("p1")
        assertEquals(listOf(session), pump.all)
    }
}
