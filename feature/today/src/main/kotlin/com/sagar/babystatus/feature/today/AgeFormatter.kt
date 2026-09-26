package com.sagar.babystatus.feature.today

import com.sagar.babystatus.core.domain.BabyAge

/** "Newborn", "12 days", "3 wk, 2 d", "3 mo, 1 wk", "2 yr, 3 mo". */
internal fun formatAge(age: BabyAge): String = when (age) {
    is BabyAge.Days -> when (age.days) {
        0 -> "Newborn"
        1 -> "1 day"
        else -> "${age.days} days"
    }
    is BabyAge.Weeks -> withRemainder("${age.weeks} wk", age.days, "d")
    is BabyAge.Months -> withRemainder("${age.months} mo", age.weeks, "wk")
    is BabyAge.Years -> withRemainder("${age.years} yr", age.months, "mo")
}

private fun withRemainder(main: String, remainder: Int, unit: String) =
    if (remainder == 0) main else "$main, $remainder $unit"
