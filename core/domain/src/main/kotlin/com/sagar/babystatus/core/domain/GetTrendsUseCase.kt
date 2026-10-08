package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.TimelineRepository
import com.sagar.babystatus.core.model.DaySummary
import com.sagar.babystatus.core.model.summarize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** The lines the Trends screen can draw. */
enum class TrendCategory { GROWTH, FEEDING, DIAPERS, PUMPING }

/** The Trends screen: the window's days, oldest first, with nothing missing. */
data class Trends(val days: List<DaySummary>)

class GetTrendsUseCase @Inject constructor(
    private val babyRepository: BabyRepository,
    private val timelineRepository: TimelineRepository,
) {
    /** Emits null while there is no baby (before onboarding). */
    operator fun invoke(today: LocalDate): Flow<Trends?> = combine(
        babyRepository.baby,
        timelineRepository.observeSummaries(from = today.minusDays(TREND_DAYS - 1L)),
    ) { baby, byDate ->
        baby?.let {
            Trends(
                days = (TREND_DAYS - 1L downTo 0L).map { back -> today.minusDays(back) }
                    // Nothing can be logged before the baby was born.
                    .filter { day -> !day.isBefore(baby.birthDate) }
                    .map { day -> byDate[day] ?: summarize(day, emptyList()) },
            )
        }
    }

    companion object {
        const val TREND_DAYS = 30
    }
}
