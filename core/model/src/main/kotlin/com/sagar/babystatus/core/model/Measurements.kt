package com.sagar.babystatus.core.model

/** Everything is stored metric; conversion happens only for display and input. */
@JvmInline
value class Volume(val ml: Double) {
    val oz: Double get() = ml / ML_PER_OZ

    companion object {
        const val ML_PER_OZ = 29.5735295625
        fun ofOz(oz: Double) = Volume(oz * ML_PER_OZ)
    }
}

@JvmInline
value class Mass(val grams: Int) {
    val kg: Double get() = grams / 1000.0
    val lb: Double get() = grams / GRAMS_PER_LB

    companion object {
        const val GRAMS_PER_LB = 453.59237
        fun ofKg(kg: Double) = Mass(Math.round(kg * 1000).toInt())
        fun ofLb(lb: Double) = Mass(Math.round(lb * GRAMS_PER_LB).toInt())
    }
}

@JvmInline
value class Length(val mm: Int) {
    val cm: Double get() = mm / 10.0
    val inches: Double get() = mm / MM_PER_INCH

    companion object {
        const val MM_PER_INCH = 25.4
        fun ofCm(cm: Double) = Length(Math.round(cm * 10).toInt())
        fun ofInches(inches: Double) = Length(Math.round(inches * MM_PER_INCH).toInt())
    }
}

enum class UnitSystem { IMPERIAL, METRIC }

/**
 * The weight as the app shows it, in tenths of the display unit (13.2 lb → 132). Comparing
 * these compares what the user sees, so a gain can't read "+0.0 lb".
 */
fun Mass.displayTenths(unitSystem: UnitSystem): Long =
    Math.round((if (unitSystem == UnitSystem.IMPERIAL) lb else kg) * 10)

/** The length as the app shows it, in tenths of the display unit (23.5 in → 235). */
fun Length.displayTenths(unitSystem: UnitSystem): Long =
    Math.round((if (unitSystem == UnitSystem.IMPERIAL) inches else cm) * 10)
