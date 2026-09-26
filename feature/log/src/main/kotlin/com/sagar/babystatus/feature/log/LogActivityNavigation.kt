package com.sagar.babystatus.feature.log

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.ui.navigation.BottomSheetSceneStrategy
import kotlinx.serialization.Serializable

/**
 * The Log activity sheet. [entryId] null logs a new entry, preselecting [initialType]
 * (the Feed or Diaper tile that was tapped); otherwise it edits that entry.
 */
@Serializable
data class LogActivityKey(
    val entryId: String? = null,
    val initialType: ActivityType? = null,
) : NavKey

fun EntryProviderScope<NavKey>.logActivityEntry(onDone: (LogActivityKey) -> Unit) {
    entry<LogActivityKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        LogActivityRoute(
            onDone = { onDone(key) },
            viewModel = hiltViewModel<LogActivityViewModel, LogActivityViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) },
            ),
        )
    }
}
