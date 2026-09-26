package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.LogRepository
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.LatestGrowth
import com.sagar.babystatus.core.model.TimedEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Everything the Today screen shows for a given day. */
data class TodayStatus(
    val baby: Baby,
    val age: BabyAge,
    val latestGrowth: LatestGrowth,
    val lastFeed: TimedEntry?,
    val lastDiaper: TimedEntry?,
    val recent: List<TimedEntry>,
)

class GetTodayStatusUseCase @Inject constructor(
    private val babyRepository: BabyRepository,
    private val logRepository: LogRepository,
    private val calculateAge: CalculateAgeUseCase,
) {
    /** Emits null while there is no baby (before onboarding). */
    operator fun invoke(today: LocalDate): Flow<TodayStatus?> = combine(
        babyRepository.baby,
        logRepository.observeLatestGrowth(),
        logRepository.observeLast(ActivityType.FEED),
        logRepository.observeLast(ActivityType.DIAPER),
        logRepository.observeTimedEntriesOn(today, limit = RECENT_LIMIT),
    ) { baby, growth, lastFeed, lastDiaper, recent ->
        baby?.let {
            TodayStatus(
                baby = it,
                age = calculateAge(it.birthDate, today),
                latestGrowth = growth,
                lastFeed = lastFeed,
                lastDiaper = lastDiaper,
                recent = recent,
            )
        }
    }

    companion object {
        const val RECENT_LIMIT = 5
    }
}
