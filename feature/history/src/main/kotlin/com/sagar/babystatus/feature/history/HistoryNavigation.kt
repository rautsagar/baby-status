package com.sagar.babystatus.feature.history

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sagar.babystatus.core.model.TimelineEntry
import kotlinx.serialization.Serializable

/** History, optionally scrolled to the day whose epoch day is [focusEpochDay]. */
@Serializable
data class HistoryKey(val focusEpochDay: Long? = null) : NavKey

fun EntryProviderScope<NavKey>.historyEntry(onBack: () -> Unit, onEditEntry: (TimelineEntry) -> Unit) {
    entry<HistoryKey> { key ->
        HistoryRoute(
            onBack = onBack,
            onEditEntry = onEditEntry,
            viewModel = hiltViewModel<HistoryViewModel, HistoryViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) },
            ),
        )
    }
}
