package com.sagar.babystatus.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sagar.babystatus.core.designsystem.R

// Both families are bundled variable fonts (OFL); each weight is an instance of the axis.
private fun fraunces(weight: Int) = Font(
    resId = R.font.fraunces,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        // A mid optical size keeps the display serif crisp at heading sizes.
        FontVariation.Setting("opsz", 36f),
    ),
)

private fun figtree(weight: Int) = Font(
    resId = R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Fraunces = FontFamily(fraunces(500), fraunces(600))
val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700))

private fun display(size: Int, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = Fraunces, fontWeight = weight, fontSize = size.sp)

private fun body(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Figtree, fontWeight = weight, fontSize = size.sp)

internal val BabyStatusTypography = Typography(
    /** Bottle amount. */
    displayLarge = display(48, FontWeight.Medium),
    /** Summary totals. */
    displayMedium = display(40, FontWeight.Medium),
    /** Sheet and onboarding titles. */
    headlineMedium = display(26),
    /** Baby name on Today. */
    headlineSmall = display(24),
    /** Stat values, top bar titles. */
    titleLarge = display(22),
    /** Section headers. */
    titleMedium = display(20),
    /** History day headers, profile name. */
    titleSmall = display(17),
    bodyLarge = body(16),
    bodyMedium = body(15),
    bodySmall = body(13),
    /** Primary buttons. */
    labelLarge = body(17, FontWeight.Bold),
    /** Field labels and small emphasis. */
    labelMedium = body(13, FontWeight.SemiBold),
    /** Upper-case caption labels ("AGE", "AMOUNT"). */
    labelSmall = body(12, FontWeight.SemiBold).copy(letterSpacing = 0.72.sp),
)
