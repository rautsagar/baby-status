package com.sagar.babystatus.feature.pump

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sagar.babystatus.core.ui.navigation.BottomSheetSceneStrategy
import kotlinx.serialization.Serializable

/** The Log pumping sheet: a new session, or edits [sessionId]. */
@Serializable
data class LogPumpKey(val sessionId: String? = null) : NavKey

fun EntryProviderScope<NavKey>.logPumpEntry(onDone: (LogPumpKey) -> Unit) {
    entry<LogPumpKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        LogPumpRoute(
            onDone = { onDone(key) },
            viewModel = hiltViewModel<LogPumpViewModel, LogPumpViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) },
            ),
        )
    }
}
