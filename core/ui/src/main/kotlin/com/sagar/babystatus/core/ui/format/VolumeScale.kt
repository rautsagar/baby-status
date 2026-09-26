package com.sagar.babystatus.core.ui.format

import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import kotlin.math.roundToInt

/**
 * The bottle and pumping sliders in the user's unit. Imperial: 1–10 oz in 0.5 steps, default 3.
 * Metric: 30–300 ml in 10 ml steps, default 90.
 */
data class VolumeScale(
    val unitSystem: UnitSystem,
    val min: Double,
    val max: Double,
    val step: Double,
    val default: Double,
    val unitLabel: String,
) {
    /** Number of stops between [min] and [max], as `Slider(steps = …)` expects. */
    val sliderSteps: Int get() = ((max - min) / step).roundToInt() - 1

    fun toVolume(value: Double): Volume = when (unitSystem) {
        UnitSystem.IMPERIAL -> Volume.ofOz(value)
        UnitSystem.METRIC -> Volume(value)
    }

    /** A stored volume shown on this scale, snapped to the nearest step and clamped. */
    fun fromVolume(volume: Volume): Double {
        val raw = if (unitSystem == UnitSystem.IMPERIAL) volume.oz else volume.ml
        return snap(raw)
    }

    fun snap(value: Double): Double = ((value - min) / step).roundToInt().times(step).plus(min).coerceIn(min, max)

    /** The number as the big amount label shows it: "3", "3.5", "90". */
    fun label(value: Double): String = when (unitSystem) {
        UnitSystem.IMPERIAL -> UnitFormatter.ounces(value)
        UnitSystem.METRIC -> value.roundToInt().toString()
    }

    companion object {
        fun forUnits(unitSystem: UnitSystem) = when (unitSystem) {
            UnitSystem.IMPERIAL -> VolumeScale(unitSystem, min = 1.0, max = 10.0, step = 0.5, default = 3.0, unitLabel = "oz")
            UnitSystem.METRIC -> VolumeScale(unitSystem, min = 30.0, max = 300.0, step = 10.0, default = 90.0, unitLabel = "ml")
        }
    }
}
