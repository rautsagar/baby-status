package com.sagar.babystatus.feature.profile

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

enum class ProfileMode {
    /** Onboarding: the start destination while there is no baby. */
    CREATE,

    /** Opened from Settings. */
    EDIT,
}

@Serializable
data class ProfileKey(val mode: ProfileMode) : NavKey

/** [onDone] is called after a successful save; [onBack] only exists in edit mode. */
fun EntryProviderScope<NavKey>.profileEntry(onDone: (ProfileMode) -> Unit, onBack: () -> Unit) {
    entry<ProfileKey> { key ->
        ProfileRoute(
            onDone = { onDone(key.mode) },
            onBack = onBack,
            viewModel = hiltViewModel<ProfileViewModel, ProfileViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) },
            ),
        )
    }
}
