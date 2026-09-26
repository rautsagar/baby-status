package com.sagar.babystatus.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** A tinted pairing used for selectable things: text/icon on [bg], [border] when selected. */
@Immutable
data class Accent(val fg: Color, val bg: Color, val border: Color)

/** Brand palette from the Baby Tracker design. Names describe the color, not a component. */
@Immutable
data class BabyStatusColors(
    val forest: Color = Color(0xFF1B4332),
    val forestDeep: Color = Color(0xFF14301F),
    val moss: Color = Color(0xFF2D6A4F),
    val gold: Color = Color(0xFFC9A227),
    val goldLight: Color = Color(0xFFF0D77B),
    /** Primary text on forest. */
    val ivory: Color = Color(0xFFFDFBF3),
    /** Secondary text on forest. */
    val mist: Color = Color(0xFFA9C9B4),
    /** Light screen background and segmented-control track. */
    val sage: Color = Color(0xFFEEF5EC),
    /** Bottom sheet background. */
    val cream: Color = Color(0xFFFBFAF3),
    val card: Color = Color(0xFFFFFFFF),
    val ink: Color = Color(0xFF16281D),
    val stone: Color = Color(0xFF6B635A),
    val divider: Color = Color(0xFFEFEAE2),
    val border: Color = Color(0xFFD8CFC3),
    val handle: Color = Color(0xFFC9C0B4),
    val closeButton: Color = Color(0xFFE3ECE1),
    val danger: Color = Color(0xFF8B3A2E),
    val chevron: Color = Color(0xFF8A8378),
    val segmentUnselected: Color = Color(0xFF5E574F),
    val hintBg: Color = Color(0xFFF7E9B8),
    val hintFg: Color = Color(0xFF6B5B22),
    val hintIcon: Color = Color(0xFF8A6D1B),
    val feed: Accent = Accent(fg = Color(0xFF7A5D0E), bg = Color(0xFFF7E9B8), border = Color(0xFFC9A227)),
    val feedIcon: Color = Color(0xFF8A6D1B),
    val diaper: Accent = Accent(fg = Color(0xFF1B4332), bg = Color(0xFFD9ECD9), border = Color(0xFF2D6A4F)),
    val diaperIcon: Color = Color(0xFF2D6A4F),
    val solids: Accent = Accent(fg = Color(0xFF7A6519), bg = Color(0xFFF0E4B8), border = Color(0xFFC9A227)),
    val growth: Accent = Accent(fg = Color(0xFFF0D77B), bg = Color(0xFF1B4332), border = Color(0xFF1B4332)),
    val liked: Accent = Accent(fg = Color(0xFF1B4332), bg = Color(0xFFD9ECD9), border = Color(0xFF2D6A4F)),
    val disliked: Accent = Accent(fg = Color(0xFF4A433C), bg = Color(0xFFEFEAE2), border = Color(0xFF8A8378)),
    val allergic: Accent = Accent(fg = Color(0xFF7A2C1F), bg = Color(0xFFF4DCD3), border = Color(0xFF8B3A2E)),
    /** Mom's pumping: the MOM badge, pump icon, side chips and slider. */
    val mom: Accent = Accent(fg = Color(0xFF7A2F22), bg = Color(0xFFF6E1DA), border = Color(0xFF8E4436)),
    val momIcon: Color = Color(0xFF9A4535),
)
