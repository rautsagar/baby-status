package com.sagar.babystatus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.Accent
import com.sagar.babystatus.core.designsystem.theme.BabyStatusColors
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.BreastSide
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.GrowthMeasurement
import com.sagar.babystatus.core.model.PumpSession
import com.sagar.babystatus.core.model.Reaction
import com.sagar.babystatus.core.model.SolidsAmount
import com.sagar.babystatus.core.model.SolidsFeed
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.ui.format.UnitFormatter

/** "Feed · Bottle", "Solids · Sweet potato", "Diaper · Pee", "Growth", "Pump". */
@Composable
@ReadOnlyComposable
fun entryTitle(entry: TimelineEntry): String = when (entry) {
    is BottleFeed -> stringResource(R.string.core_ui_entry_bottle)
    is SolidsFeed -> stringResource(R.string.core_ui_entry_solids, entry.food)
    is DiaperChange -> when (entry.kind) {
        DiaperKind.PEE -> stringResource(R.string.core_ui_entry_diaper_pee)
        DiaperKind.POTTY -> stringResource(R.string.core_ui_entry_diaper_potty)
    }
    is GrowthMeasurement -> stringResource(R.string.core_ui_entry_growth)
    is PumpSession -> stringResource(R.string.core_ui_entry_pump)
}

/** "5 oz", "Some · Liked it", "13.2 lb · 23.5 in", "4 oz · Both · 15 min"; null when there's nothing to add. */
@Composable
@ReadOnlyComposable
fun entrySubtitle(entry: TimelineEntry, unitSystem: UnitSystem): String? = when (entry) {
    is BottleFeed -> UnitFormatter.volume(entry.volume, unitSystem)
    is SolidsFeed -> {
        val amount = solidsAmountLabel(entry.amount)
        when (val reaction = entry.reaction) {
            null -> amount
            else -> stringResource(R.string.core_ui_separator, amount, reactionSentence(reaction))
        }
    }
    is DiaperChange -> null
    is GrowthMeasurement -> listOfNotNull(
        entry.weight?.let { UnitFormatter.mass(it, unitSystem) },
        entry.length?.let { UnitFormatter.length(it, unitSystem) },
    ).joinToString(" · ")
    is PumpSession -> listOfNotNull(
        UnitFormatter.volume(entry.volume, unitSystem),
        entry.side?.let { breastSideLabel(it) },
        entry.duration?.let { stringResource(R.string.core_ui_minutes, it.toMinutes()) },
    ).joinToString(" · ")
}

@Composable
@ReadOnlyComposable
fun breastSideLabel(side: BreastSide): String = stringResource(
    when (side) {
        BreastSide.LEFT -> R.string.core_ui_side_left
        BreastSide.RIGHT -> R.string.core_ui_side_right
        BreastSide.BOTH -> R.string.core_ui_side_both
    },
)

/** Entries that belong to Mom rather than the baby get the MOM badge. */
val TimelineEntry.isMoms: Boolean get() = this is PumpSession

@Composable
@ReadOnlyComposable
fun solidsAmountLabel(amount: SolidsAmount): String = stringResource(
    when (amount) {
        SolidsAmount.TASTE -> R.string.core_ui_solids_taste
        SolidsAmount.SOME -> R.string.core_ui_solids_some
        SolidsAmount.LOTS -> R.string.core_ui_solids_lots
    },
)

/** Chip label: "Liked". */
@Composable
@ReadOnlyComposable
fun reactionLabel(reaction: Reaction): String = stringResource(
    when (reaction) {
        Reaction.LIKED -> R.string.core_ui_reaction_liked
        Reaction.DISLIKED -> R.string.core_ui_reaction_disliked
        Reaction.ALLERGIC -> R.string.core_ui_reaction_allergic
    },
)

@Composable
@ReadOnlyComposable
private fun reactionSentence(reaction: Reaction): String = stringResource(
    when (reaction) {
        Reaction.LIKED -> R.string.core_ui_reaction_liked_it
        Reaction.DISLIKED -> R.string.core_ui_reaction_disliked_it
        Reaction.ALLERGIC -> R.string.core_ui_reaction_allergic_it
    },
)

@Composable
@ReadOnlyComposable
fun diaperKindLabel(kind: DiaperKind): String = stringResource(
    when (kind) {
        DiaperKind.PEE -> R.string.core_ui_diaper_pee
        DiaperKind.POTTY -> R.string.core_ui_diaper_potty
    },
)

/** Icon and tint an entry is drawn with in lists. */
data class EntryVisual(val icon: ImageVector, val accent: Accent, val iconTint: Color)

fun entryVisual(entry: TimelineEntry, colors: BabyStatusColors): EntryVisual = when (entry) {
    is BottleFeed -> EntryVisual(BabyIcons.Bottle, colors.feed, colors.feedIcon)
    is SolidsFeed -> EntryVisual(BabyIcons.Bowl, colors.solids, colors.solids.fg)
    is DiaperChange -> EntryVisual(BabyIcons.Drop, colors.diaper, colors.diaperIcon)
    is GrowthMeasurement -> EntryVisual(BabyIcons.Growth, colors.growth, colors.growth.fg)
    is PumpSession -> EntryVisual(BabyIcons.Pump, colors.mom, colors.momIcon)
}
