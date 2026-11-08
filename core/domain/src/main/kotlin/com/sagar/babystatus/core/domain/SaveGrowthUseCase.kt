package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.data.repository.LogRepository
import com.sagar.babystatus.core.data.repository.UserPreferencesRepository
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.displayTenths
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** A measure that went up: the measurement it's compared with, and the new one. */
data class Increase<T>(val from: T, val to: T)

/** What grew when a measurement was saved. At least one of the two is set. */
data class GrowthGain(val weight: Increase<Mass>?, val length: Increase<Length>?)

/**
 * Saves a growth measurement and, when it shows growth, announces it so the app can
 * celebrate: confetti over whatever screen is open, and a ▲ chip on Today.
 *
 * Growth means a weight or length higher than the newest one measured before this
 * measurement's date, compared as the user sees them (tenths of the display unit). A value an
 * edit didn't change doesn't count, and the first measurement ever has nothing to beat.
 * Imports write the database directly, so they never celebrate.
 */
@Singleton
class SaveGrowthUseCase @Inject constructor(
    private val logRepository: LogRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val _gains = MutableSharedFlow<GrowthGain>(extraBufferCapacity = 4)
    val gains: SharedFlow<GrowthGain> = _gains.asSharedFlow()

    suspend operator fun invoke(entry: GrowthMeasurement) {
        val stored = logRepository.get(entry.id) as? GrowthMeasurement
        val before = logRepository.growthBefore(entry.date, excludeId = entry.id)
        val unitSystem = userPreferencesRepository.preferences.first().unitSystem
        logRepository.save(entry)

        val weight = increase(before.weight, entry.weight, stored?.weight) { it.displayTenths(unitSystem) }
        val length = increase(before.length, entry.length, stored?.length) { it.displayTenths(unitSystem) }
        if (weight != null || length != null) _gains.emit(GrowthGain(weight, length))
    }

    private fun <T> increase(previous: T?, new: T?, stored: T?, shown: (T) -> Long): Increase<T>? = when {
        previous == null || new == null -> null
        stored != null && shown(stored) == shown(new) -> null
        shown(new) > shown(previous) -> Increase(previous, new)
        else -> null
    }
}
