package com.sagar.babystatus.core.model

import java.time.LocalDate

data class Baby(
    val id: String,
    val name: String,
    val birthDate: LocalDate,
    /** Relative to the app's files dir, e.g. `photos/<uuid>.jpg`. */
    val photoPath: String?,
)
