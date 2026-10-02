package com.sagar.babystatus.core.ui.format

import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import java.util.Locale
import kotlin.math.roundToInt

/** A formatted volume split into its [number] and [unit]. */
data class VolumeParts(val number: String, val unit: String)

/** Metric storage -> display strings. English abbreviations; numbers use the device locale. */
object UnitFormatter {
    fun volume(volume: Volume, unitSystem: UnitSystem): String =
        volumeParts(volume, unitSystem).let { "${it.number} ${it.unit}" }

    /** The number and the unit apart, for layouts that style them differently ("18", "oz"). */
    fun volumeParts(volume: Volume, unitSystem: UnitSystem): VolumeParts = when (unitSystem) {
        UnitSystem.IMPERIAL -> VolumeParts(ounces(volume.oz), "oz")
        UnitSystem.METRIC -> VolumeParts(((volume.ml / 5).roundToInt() * 5).toString(), "ml")
    }

    fun mass(mass: Mass, unitSystem: UnitSystem): String = when (unitSystem) {
        UnitSystem.IMPERIAL -> "${oneDecimal(mass.lb)} lb"
        UnitSystem.METRIC -> "${oneDecimal(mass.kg)} kg"
    }

    fun length(length: Length, unitSystem: UnitSystem): String = when (unitSystem) {
        UnitSystem.IMPERIAL -> "${oneDecimal(length.inches)} in"
        UnitSystem.METRIC -> "${oneDecimal(length.cm)} cm"
    }

    fun massUnit(unitSystem: UnitSystem) = if (unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"

    fun lengthUnit(unitSystem: UnitSystem) = if (unitSystem == UnitSystem.IMPERIAL) "in" else "cm"

    /** The value a growth input field shows for [mass], without the unit. */
    fun massInput(mass: Mass, unitSystem: UnitSystem): String =
        oneDecimal(if (unitSystem == UnitSystem.IMPERIAL) mass.lb else mass.kg)

    fun lengthInput(length: Length, unitSystem: UnitSystem): String =
        oneDecimal(if (unitSystem == UnitSystem.IMPERIAL) length.inches else length.cm)

    /** Nearest half ounce, without a trailing ".0": 3, 3.5, 17.5. */
    fun ounces(oz: Double): String {
        val halves = (oz * 2).roundToInt()
        return if (halves % 2 == 0) (halves / 2).toString() else String.format(Locale.getDefault(), "%.1f", halves / 2.0)
    }

    private fun oneDecimal(value: Double) = String.format(Locale.getDefault(), "%.1f", value)
}

/** Parses a typed decimal, accepting either `.` or `,` as the separator. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
