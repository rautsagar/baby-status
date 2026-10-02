package com.sagar.babystatus.core.model

/** Most recent weight and length. They can come from different measurements. */
data class LatestGrowth(
    val weight: Mass?,
    val length: Length?,
)

data class UserPreferences(
    val unitSystem: UnitSystem,
)

/** Build facts the UI needs; provided by :app from its BuildConfig. */
data class AppInfo(
    val versionName: String,
    val feedbackEmail: String,
)
