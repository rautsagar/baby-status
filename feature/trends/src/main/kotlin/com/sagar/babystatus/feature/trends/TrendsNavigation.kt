package com.sagar.babystatus.feature.trends

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object TrendsKey : NavKey

/** Trends: a line chart per category over the last 30 days. */
fun EntryProviderScope<NavKey>.trendsEntry(onBack: () -> Unit) {
    entry<TrendsKey> {
        TrendsRoute(onBack = onBack)
    }
}
