package com.sagar.babystatus.feature.log

import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import com.sagar.babystatus.core.domain.LogEntryError
import com.sagar.babystatus.core.domain.ValidateLogEntryUseCase
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.Reaction
import com.sagar.babystatus.core.model.SolidsAmount
import com.sagar.babystatus.core.model.SolidsFeed
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakeLogRepository
import com.sagar.babystatus.core.testing.FakeTimelineRepository
import com.sagar.babystatus.core.testing.FakeUserPreferencesRepository
import com.sagar.babystatus.core.testing.MainDispatcherRule
import com.sagar.babystatus.core.testing.MutableClock
import com.sagar.babystatus.core.testing.SequentialIdGenerator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LogActivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val today = LocalDate.of(2026, 9, 24)
    private val babyRepository = FakeBabyRepository(Baby("baby-1", "Ava", LocalDate.of(2026, 6, 12), null))

    private fun TestScope.viewModel(
        key: LogActivityKey = LogActivityKey(),
        logRepository: FakeLogRepository = FakeLogRepository(),
        unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    ) = LogActivityViewModel(
        key = key,
        logRepository = logRepository,
        babyRepository = babyRepository,
        userPreferencesRepository = FakeUserPreferencesRepository(unitSystem),
        validate = ValidateLogEntryUseCase(clock),
        deleteEntry = DeleteEntryUseCase(FakeTimelineRepository(logRepository)),
        idGenerator = SequentialIdGenerator(),
        clock = clock,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() } }

    @Test
    fun `a new bottle defaults to 3 oz and saves`() = runTest {
        val log = FakeLogRepository()
        val vm = viewModel(logRepository = log)

        assertEquals(3.0, vm.uiState.value.bottleValue)
        vm.save()

        val saved = assertIs<BottleFeed>(log.all.single())
        assertEquals(3.0, saved.volume.oz, 0.001)
        assertEquals(today, saved.date)
        assertTrue(vm.uiState.value.isDone)
    }

    @Test
    fun `metric bottles default to 90 ml and move in 10 ml steps`() = runTest {
        val log = FakeLogRepository()
        val vm = viewModel(logRepository = log, unitSystem = UnitSystem.METRIC)

        assertEquals(90.0, vm.uiState.value.bottleValue)
        vm.onBottleChanged(123.0)
        assertEquals(120.0, vm.uiState.value.bottleValue)
        vm.save()

        assertEquals(120.0, assertIs<BottleFeed>(log.all.single()).volume.ml, 0.001)
    }

    @Test
    fun `the tapped tile preselects the type`() = runTest {
        val vm = viewModel(key = LogActivityKey(initialType = ActivityType.DIAPER))
        assertEquals(ActivityType.DIAPER, vm.uiState.value.type)
    }

    @Test
    fun `solids need a food and an amount before they can be saved`() = runTest {
        val log = FakeLogRepository()
        val vm = viewModel(logRepository = log)
        vm.onFeedKindSelected(FeedKind.SOLIDS)
        assertFalse(vm.uiState.value.canSave)

        vm.onFoodChanged("Sweet potato")
        assertFalse(vm.uiState.value.canSave, "amount still missing")

        vm.onSolidsAmountSelected(SolidsAmount.SOME)
        vm.onReactionToggled(Reaction.LIKED)
        assertTrue(vm.uiState.value.canSave)
        vm.save()

        val saved = assertIs<SolidsFeed>(log.all.single())
        assertEquals("Sweet potato", saved.food)
        assertEquals(Reaction.LIKED, saved.reaction)
    }

    @Test
    fun `tapping the selected reaction clears it`() = runTest {
        val vm = viewModel()
        vm.onReactionToggled(Reaction.ALLERGIC)
        vm.onReactionToggled(Reaction.ALLERGIC)
        assertEquals(null, vm.uiState.value.reaction)
    }

    @Test
    fun `a time in the future is rejected`() = runTest {
        val log = FakeLogRepository()
        val vm = viewModel(logRepository = log)
        vm.onOccurredAtChanged(clock.now.plus(Duration.ofHours(1)))
        vm.save()

        assertEquals(setOf(LogEntryError.IN_FUTURE), vm.uiState.value.errors)
        assertTrue(log.all.isEmpty())
        assertFalse(vm.uiState.value.isDone)
    }

    @Test
    fun `editing loads the entry, locks its type and can delete it`() = runTest {
        val existing: LogEntry = DiaperChange("d1", clock.now.minusSeconds(600), today, "leaky", DiaperKind.POTTY)
        val log = FakeLogRepository(listOf(existing))
        val vm = viewModel(key = LogActivityKey(entryId = "d1"), logRepository = log)

        val state = vm.uiState.value
        assertTrue(state.isEdit)
        assertEquals(ActivityType.DIAPER, state.type)
        assertEquals(DiaperKind.POTTY, state.diaperKind)
        assertEquals("leaky", state.note)

        vm.onTypeSelected(ActivityType.FEED)
        assertEquals(ActivityType.DIAPER, vm.uiState.value.type, "type is locked while editing")

        vm.delete()
        assertEquals(setOf("d1"), log.deletedIds)
        assertTrue(vm.uiState.value.isDone)
    }

    @Test
    fun `editing keeps the id so the save updates in place`() = runTest {
        val existing = BottleFeed("b1", clock.now.minusSeconds(600), today, null, Volume.ofOz(4.0))
        val log = FakeLogRepository(listOf(existing))
        val vm = viewModel(key = LogActivityKey(entryId = "b1"), logRepository = log)

        assertEquals(4.0, vm.uiState.value.bottleValue)
        vm.onBottleChanged(6.0)
        vm.save()

        val saved = assertIs<BottleFeed>(log.all.single())
        assertEquals("b1", saved.id)
        assertEquals(6.0, saved.volume.oz, 0.001)
    }

    @Test
    fun `food suggestions put past foods first and skip the exact text`() {
        val past = listOf("Mango", "banana")
        assertEquals(
            listOf("Mango", "banana", "Sweet potato", "Avocado", "Rice cereal", "Peas"),
            LogActivityViewModel.suggestions("", past),
        )
        assertEquals(listOf("Sweet potato"), LogActivityViewModel.suggestions("pot", past))
        assertEquals(emptyList(), LogActivityViewModel.suggestions("Banana", past))
    }
}
