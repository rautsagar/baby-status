package com.sagar.babystatus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sagar.babystatus.R
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.ui.navigation.BottomSheetSceneStrategy
import com.sagar.babystatus.feature.growth.LogGrowthKey
import com.sagar.babystatus.feature.growth.logGrowthEntry
import com.sagar.babystatus.feature.history.HistoryKey
import com.sagar.babystatus.feature.history.historyEntry
import com.sagar.babystatus.feature.log.LogActivityKey
import com.sagar.babystatus.feature.log.logActivityEntry
import com.sagar.babystatus.feature.profile.ProfileKey
import com.sagar.babystatus.feature.profile.ProfileMode
import com.sagar.babystatus.feature.profile.profileEntry
import com.sagar.babystatus.feature.pump.LogPumpKey
import com.sagar.babystatus.feature.pump.logPumpEntry
import com.sagar.babystatus.feature.settings.PrivacyPolicyKey
import com.sagar.babystatus.feature.settings.SettingsKey
import com.sagar.babystatus.feature.settings.settingsEntries
import com.sagar.babystatus.feature.today.TodayKey
import com.sagar.babystatus.feature.today.todayEntry
import kotlinx.coroutines.flow.Flow

/**
 * The app shell: one back stack, every feature's entries, sheets as an overlay scene, and
 * the app-wide "Entry deleted · UNDO" snackbar. Features never navigate themselves; they
 * call the lambdas wired here.
 */
@Composable
fun BabyStatusApp(
    hasBaby: Boolean,
    deletedIds: Flow<String>,
    onUndoDelete: (String) -> Unit,
) {
    // Restored after process death; the start key only matters on first composition.
    val backStack = rememberNavBackStack(if (hasBaby) TodayKey else ProfileKey(ProfileMode.CREATE))
    val snackbarHostState = remember { SnackbarHostState() }
    val entryDeleted = stringResource(R.string.entry_deleted)
    val undo = stringResource(R.string.undo)
    val currentOnUndoDelete by rememberUpdatedState(onUndoDelete)

    LaunchedEffect(deletedIds) {
        deletedIds.collect { id ->
            val result = snackbarHostState.showSnackbar(entryDeleted, actionLabel = undo, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) currentOnUndoDelete(id)
        }
    }

    val editEntry: (TimelineEntry) -> Unit = { entry ->
        backStack.add(
            when (entry) {
                is GrowthMeasurement -> LogGrowthKey(entry.id)
                is PumpSession -> LogPumpKey(entry.id)
                is LogEntry -> LogActivityKey(entry.id)
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            sceneStrategies = listOf(BottomSheetSceneStrategy()),
            entryProvider = entryProvider {
                profileEntry(
                    onDone = { mode ->
                        if (mode == ProfileMode.CREATE) {
                            // Onboarding is finished for good: Today becomes the root.
                            backStack.clear()
                            backStack.add(TodayKey)
                        } else {
                            backStack.goBack()
                        }
                    },
                    onBack = { backStack.goBack() },
                )
                todayEntry(
                    onOpenSettings = { backStack.add(SettingsKey) },
                    onOpenHistory = { backStack.add(HistoryKey) },
                    onLogActivity = { type -> backStack.add(LogActivityKey(initialType = type)) },
                    onLogGrowth = { backStack.add(LogGrowthKey()) },
                    onLogPump = { backStack.add(LogPumpKey()) },
                    onEditEntry = editEntry,
                )
                historyEntry(onBack = { backStack.goBack() }, onEditEntry = editEntry)
                logActivityEntry(onDone = { key -> backStack.removeIfOnTop(key) })
                logGrowthEntry(onDone = { key -> backStack.removeIfOnTop(key) })
                logPumpEntry(onDone = { key -> backStack.removeIfOnTop(key) })
                settingsEntries(
                    onBack = { backStack.goBack() },
                    onEditProfile = { backStack.add(ProfileKey(ProfileMode.EDIT)) },
                    onOpenPrivacyPolicy = { backStack.add(PrivacyPolicyKey) },
                )
            },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) { data ->
            val colors = BabyStatusTheme.colors
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(14.dp),
                containerColor = colors.ink,
                contentColor = colors.cream,
                actionColor = colors.goldLight,
            )
        }
    }
}

/** Pops one screen, never the root (a double tap on Back mustn't empty the stack). */
private fun NavBackStack<NavKey>.goBack() {
    if (size > 1) removeAt(lastIndex)
}

/** Closes a sheet only if it's still the top entry (Save and Close can both fire). */
private fun NavBackStack<NavKey>.removeIfOnTop(key: NavKey) {
    if (lastOrNull() == key && size > 1) removeAt(lastIndex)
}
