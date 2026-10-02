package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.TimelineRepository
import com.sagar.babystatus.core.model.DaySummary
import com.sagar.babystatus.core.model.summarize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** The Summary screen: today's totals, and the days before it. */
data class Summary(
    val today: DaySummary,
    /** The previous calendar days, newest first. A day with nothing logged is empty, not missing. */
    val previous: List<DaySummary>,
)

class GetSummaryUseCase @Inject constructor(
    private val babyRepository: BabyRepository,
    private val timelineRepository: TimelineRepository,
) {
    /** Emits null while there is no baby (before onboarding). */
    operator fun invoke(today: LocalDate): Flow<Summary?> = combine(
        babyRepository.baby,
        timelineRepository.observeSummaries(from = today.minusDays(PREVIOUS_DAYS.toLong())),
    ) { baby, byDate ->
        baby?.let {
            Summary(
                today = byDate[today] ?: summarize(today, emptyList()),
                previous = (1..PREVIOUS_DAYS).map { today.minusDays(it.toLong()) }
                    // Nothing can be logged before the baby was born.
                    .filter { day -> !day.isBefore(baby.birthDate) }
                    .map { day -> byDate[day] ?: summarize(day, emptyList()) },
            )
        }
    }

    companion object {
        const val PREVIOUS_DAYS = 5
    }
}
