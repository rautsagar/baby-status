package com.sagar.babystatus.feature.summary

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SummaryKey : NavKey

/**
 * Summary. [onOpenHistory] is given the epoch day to scroll History to, or null to open it
 * at the top ("Full history").
 */
fun EntryProviderScope<NavKey>.summaryEntry(onBack: () -> Unit, onOpenHistory: (focusEpochDay: Long?) -> Unit) {
    entry<SummaryKey> {
        SummaryRoute(onBack = onBack, onOpenHistory = onOpenHistory)
    }
}
