package com.sagar.babystatus.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SettingsKey : NavKey

@Serializable
data object PrivacyPolicyKey : NavKey

fun EntryProviderScope<NavKey>.settingsEntries(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    entry<SettingsKey> {
        SettingsRoute(onBack = onBack, onEditProfile = onEditProfile, onOpenPrivacyPolicy = onOpenPrivacyPolicy)
    }
    entry<PrivacyPolicyKey> {
        PrivacyPolicyScreen(onBack = onBack)
    }
}
