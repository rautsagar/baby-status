package com.sagar.babystatus.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene

private const val SLIDE_MILLIS = 300

/** The screen underneath moves a third as far as the one on top, so it trails behind it. */
private const val UNDERLAY_DIVISOR = 3

private val slideSpec = tween<IntOffset>(SLIDE_MILLIS, easing = FastOutSlowInEasing)

/**
 * Navigation 3's default transition is a fade, so screens set their own: opening a screen
 * slides it in from the right over the current one, going back slides it out to the right.
 *
 * The z-index is the screen's depth in the back stack. Without it the incoming screen
 * always draws on top, so on Back the returning screen would cover the one sliding away.
 */
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.slideForward(): ContentTransform =
    ContentTransform(
        targetContentEnter = slideInHorizontally(slideSpec) { fullWidth -> fullWidth },
        initialContentExit = slideOutHorizontally(slideSpec) { fullWidth -> -fullWidth / UNDERLAY_DIVISOR },
        targetContentZIndex = targetState.previousEntries.size.toFloat(),
    )

internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.slideBack(): ContentTransform =
    ContentTransform(
        targetContentEnter = slideInHorizontally(slideSpec) { fullWidth -> -fullWidth / UNDERLAY_DIVISOR },
        initialContentExit = slideOutHorizontally(slideSpec) { fullWidth -> fullWidth },
        targetContentZIndex = targetState.previousEntries.size.toFloat(),
    )
