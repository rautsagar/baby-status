package com.sagar.babystatus.feature.growth

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sagar.babystatus.core.ui.navigation.BottomSheetSceneStrategy
import kotlinx.serialization.Serializable

/** The Log growth sheet: a new measurement, or edits [entryId]. */
@Serializable
data class LogGrowthKey(val entryId: String? = null) : NavKey

fun EntryProviderScope<NavKey>.logGrowthEntry(onDone: (LogGrowthKey) -> Unit) {
    entry<LogGrowthKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        LogGrowthRoute(
            onDone = { onDone(key) },
            viewModel = hiltViewModel<LogGrowthViewModel, LogGrowthViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) },
            ),
        )
    }
}
