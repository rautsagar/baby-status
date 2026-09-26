package com.sagar.babystatus.core.model

import java.time.LocalDate

/** Most recent weight and length. They can come from different measurements. */
data class LatestGrowth(
    val weight: Mass?,
    val length: Length?,
)

/** What a History day header shows: "18 oz fed · 14 oz pumped · 4 pee · 2 potty". */
data class DailyTotals(
    val date: LocalDate,
    val bottle: Volume,
    val pumped: Volume,
    val peeCount: Int,
    val pottyCount: Int,
)

data class UserPreferences(
    val unitSystem: UnitSystem,
)

/** Build facts the UI needs; provided by :app from its BuildConfig. */
data class AppInfo(
    val versionName: String,
    val feedbackEmail: String,
)
