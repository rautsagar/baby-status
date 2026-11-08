package com.sagar.babystatus.core.ui.format

import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.model.displayTenths
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

    // Weights and lengths round through displayTenths, the same rounding that decides whether
    // a measurement grew, so a gain chip always matches the numbers on screen.
    fun mass(mass: Mass, unitSystem: UnitSystem): String =
        "${tenths(mass.displayTenths(unitSystem))} ${massUnit(unitSystem)}"

    fun length(length: Length, unitSystem: UnitSystem): String =
        "${tenths(length.displayTenths(unitSystem))} ${lengthUnit(unitSystem)}"

    /** How much a weight went up, as shown: 13.3 lb → 13.9 lb is "0.6 lb". */
    fun massIncrease(from: Mass, to: Mass, unitSystem: UnitSystem): String =
        "${tenths(to.displayTenths(unitSystem) - from.displayTenths(unitSystem))} ${massUnit(unitSystem)}"

    /** How much a length went up, as shown: "0.5 in". */
    fun lengthIncrease(from: Length, to: Length, unitSystem: UnitSystem): String =
        "${tenths(to.displayTenths(unitSystem) - from.displayTenths(unitSystem))} ${lengthUnit(unitSystem)}"

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

    private fun tenths(value: Long) = oneDecimal(value / 10.0)
}

/** Parses a typed decimal, accepting either `.` or `,` as the separator. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
