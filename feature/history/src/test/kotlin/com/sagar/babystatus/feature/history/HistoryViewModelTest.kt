package com.sagar.babystatus.feature.history

import androidx.paging.testing.asSnapshot
import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakePumpRepository
import com.sagar.babystatus.core.testing.FakeTimelineRepository
import com.sagar.babystatus.core.testing.FakeUserPreferencesRepository
import com.sagar.babystatus.core.testing.MainDispatcherRule
import com.sagar.babystatus.core.testing.MutableClock
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val today = LocalDate.of(2026, 9, 24)

    private fun at(date: LocalDate, hour: Int = 12) = Instant.parse("${date}T${hour.toString().padStart(2, '0')}:00:00Z")
    private fun bottle(id: String, date: LocalDate, oz: Double = 4.0) = BottleFeed(id, at(date), date, null, Volume.ofOz(oz))
    private fun diaper(id: String, date: LocalDate, kind: DiaperKind = DiaperKind.PEE) =
        DiaperChange(id, at(date, 13), date, null, kind)
    private fun pump(id: String, date: LocalDate, oz: Double) = PumpSession(id, at(date, 6), date, null, Volume.ofOz(oz), null, null)

    private val log = FakeLogRepository(
        listOf(
            bottle("b0", today),
            diaper("d0", today),
            bottle("b2", today.minusDays(2)),
            bottle("b3", today.minusDays(3)),
            diaper("d3", today.minusDays(3), DiaperKind.POTTY),
        ),
    )
    private val pumping = FakePumpRepository(listOf(pump("p2", today.minusDays(2), 5.0)))
    private val timeline = FakeTimelineRepository(log, pumping)

    private fun viewModel(key: HistoryKey = HistoryKey()) = HistoryViewModel(
        key = key,
        timelineRepository = timeline,
        userPreferencesRepository = FakeUserPreferencesRepository(UnitSystem.METRIC),
        deleteEntry = DeleteEntryUseCase(timeline),
        clock = clock,
    )

    @Test
    fun `one item per day with entries, newest first, Mom's pumping alongside the baby's entries`() = runTest {
        val days = viewModel().days.asSnapshot()

        assertEquals(listOf(today, today.minusDays(2), today.minusDays(3)), days.map { it.date }, "yesterday has nothing, so no item")
        assertEquals(listOf("b0", "d0").sorted(), days[0].entries.map { it.id }.sorted())
        assertEquals(listOf("b2", "p2"), days[1].entries.map { it.id }.sorted())
    }

    @Test
    fun `a day's totals come from its own entries`() = runTest {
        val twoDaysAgo = viewModel().days.asSnapshot()[1].summary

        assertEquals(4.0, twoDaysAgo.fed.oz, 0.001)
        assertEquals(5.0, twoDaysAgo.pumped.oz, 0.001)
        assertEquals(1, twoDaysAgo.pumpSessions)
    }

    @Test
    fun `no scroll target unless History was opened for a day`() = runTest {
        assertNull(viewModel().initialScrollIndex.value)
    }

    @Test
    fun `opened for a day, the target is that day's position in the list`() = runTest {
        assertEquals(1, viewModel(HistoryKey(today.minusDays(2).toEpochDay())).initialScrollIndex.value)
        assertEquals(2, viewModel(HistoryKey(today.minusDays(3).toEpochDay())).initialScrollIndex.value)
    }

    @Test
    fun `a day with no entries lands on the next older day that has some`() = runTest {
        // Yesterday has nothing: it sits between today (index 0) and two days ago (index 1).
        assertEquals(1, viewModel(HistoryKey(today.minusDays(1).toEpochDay())).initialScrollIndex.value)
    }

    @Test
    fun `the scroll target is cleared once the list has scrolled`() = runTest {
        val vm = viewModel(HistoryKey(today.minusDays(2).toEpochDay()))
        vm.onScrolled()
        assertNull(vm.initialScrollIndex.value)
    }

    @Test
    fun `deleting an entry removes it from its day`() = runTest {
        val deleteEntry = DeleteEntryUseCase(timeline)
        val vm = HistoryViewModel(HistoryKey(), timeline, FakeUserPreferencesRepository(), deleteEntry, clock)

        vm.delete(bottle("b3", today.minusDays(3)))

        assertEquals(listOf(today, today.minusDays(2), today.minusDays(3)), vm.days.asSnapshot().map { it.date })
        assertEquals(listOf("d3"), vm.days.asSnapshot().last().entries.map { it.id })
    }
}
