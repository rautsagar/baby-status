package com.sagar.babystatus.feature.today

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.LogEntry
import kotlinx.serialization.Serializable

@Serializable
data object TodayKey : NavKey

fun EntryProviderScope<NavKey>.todayEntry(
    onOpenSummary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onLogActivity: (ActivityType?) -> Unit,
    onLogGrowth: () -> Unit,
    onLogPump: () -> Unit,
    onEditEntry: (LogEntry) -> Unit,
) {
    entry<TodayKey> {
        TodayRoute(
            onOpenSummary = onOpenSummary,
            onOpenSettings = onOpenSettings,
            onOpenHistory = onOpenHistory,
            onLogActivity = onLogActivity,
            onLogGrowth = onLogGrowth,
            onLogPump = onLogPump,
            onEditEntry = onEditEntry,
        )
    }
}
