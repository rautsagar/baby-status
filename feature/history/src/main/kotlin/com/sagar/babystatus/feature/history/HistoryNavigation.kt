package com.sagar.babystatus.feature.history

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sagar.babystatus.core.model.TimelineEntry
import kotlinx.serialization.Serializable

@Serializable
data object HistoryKey : NavKey

fun EntryProviderScope<NavKey>.historyEntry(onBack: () -> Unit, onEditEntry: (TimelineEntry) -> Unit) {
    entry<HistoryKey> {
        HistoryRoute(onBack = onBack, onEditEntry = onEditEntry)
    }
}
