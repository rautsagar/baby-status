package com.sagar.babystatus.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalBabyStatusColors = staticCompositionLocalOf { BabyStatusColors() }

/** Brand theme only: no dynamic color and no dark variant (the brand is already dark green). */
@Composable
fun BabyStatusTheme(content: @Composable () -> Unit) {
    val colors = BabyStatusColors()
    val colorScheme = lightColorScheme(
        primary = colors.forest,
        onPrimary = colors.goldLight,
        primaryContainer = colors.moss,
        onPrimaryContainer = colors.ivory,
        secondary = colors.gold,
        onSecondary = colors.ink,
        secondaryContainer = colors.feed.bg,
        onSecondaryContainer = colors.feed.fg,
        background = colors.sage,
        onBackground = colors.ink,
        surface = colors.cream,
        onSurface = colors.ink,
        surfaceVariant = colors.sage,
        onSurfaceVariant = colors.stone,
        surfaceContainerLowest = colors.card,
        surfaceContainerLow = colors.cream,
        surfaceContainer = colors.cream,
        surfaceContainerHigh = colors.cream,
        surfaceContainerHighest = colors.card,
        outline = colors.border,
        outlineVariant = colors.divider,
        error = colors.danger,
        inverseSurface = colors.ink,
        inverseOnSurface = colors.cream,
        inversePrimary = colors.goldLight,
        scrim = colors.forestDeep,
    )
    CompositionLocalProvider(LocalBabyStatusColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, typography = BabyStatusTypography, content = content)
    }
}

object BabyStatusTheme {
    val colors: BabyStatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBabyStatusColors.current
}
