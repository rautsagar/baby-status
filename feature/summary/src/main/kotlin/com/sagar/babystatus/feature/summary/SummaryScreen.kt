package com.sagar.babystatus.feature.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.CapsLabel
import com.sagar.babystatus.core.designsystem.component.ForestTopBar
import com.sagar.babystatus.core.designsystem.component.IconBadge
import com.sagar.babystatus.core.designsystem.component.MomBadge
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.domain.Summary
import com.sagar.babystatus.core.model.DaySummary
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.ui.format.TimeFormatter
import com.sagar.babystatus.core.ui.format.UnitFormatter
import java.time.LocalDate

@Composable
internal fun SummaryRoute(
    onBack: () -> Unit,
    onOpenHistory: (focusEpochDay: Long?) -> Unit,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SummaryScreen(uiState = uiState, onBack = onBack, onOpenHistory = onOpenHistory)
}

@Composable
internal fun SummaryScreen(
    uiState: SummaryUiState,
    onBack: () -> Unit,
    onOpenHistory: (focusEpochDay: Long?) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(Modifier.fillMaxSize().background(colors.sage)) {
        ForestTopBar(
            title = stringResource(R.string.feature_summary_title),
            backLabel = stringResource(R.string.feature_summary_back),
            onBack = onBack,
        )
        if (uiState is SummaryUiState.Ready) {
            Column(
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                TodaySection(uiState.summary.today, uiState.unitSystem, uiState.today)
                PreviousDaysSection(uiState.summary, uiState.unitSystem, uiState.today, onOpenHistory)
            }
        }
    }
}

// --- Today ---

@Composable
private fun TodaySection(today: DaySummary, unitSystem: UnitSystem, date: LocalDate) {
    val colors = BabyStatusTheme.colors
    val fed = UnitFormatter.volumeParts(today.fed, unitSystem)
    val pumped = UnitFormatter.volumeParts(today.pumped, unitSystem)
    val fedDetail = stringResource(
        R.string.feature_summary_fed_detail,
        pluralStringResource(R.plurals.feature_summary_bottles, today.bottleCount, today.bottleCount),
        today.solidsCount,
    )
    val pumpedDetail = pluralStringResource(R.plurals.feature_summary_sessions, today.pumpSessions, today.pumpSessions)
    val diapersCount = pluralStringResource(R.plurals.feature_summary_diapers_count, today.diapers, today.diapers)
    val diapersDetail = stringResource(R.string.feature_summary_pee_potty, today.pee, today.potty)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                stringResource(R.string.feature_summary_today),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.feature_summary_so_far, TimeFormatter.shortDate(date)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.stone,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = colors.forest, spotColor = colors.forest)
                .background(colors.card),
        ) {
            TotalRow(
                icon = BabyIcons.Bottle,
                badgeColor = colors.feed.bg,
                iconColor = colors.feedIcon,
                title = stringResource(R.string.feature_summary_fed),
                detail = fedDetail,
                number = fed.number,
                unit = fed.unit,
                description = stringResource(R.string.feature_summary_today_fed_description, "${fed.number} ${fed.unit}", fedDetail),
            )
            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 18.dp))
            TotalRow(
                icon = BabyIcons.Pump,
                badgeColor = colors.mom.bg,
                iconColor = colors.momIcon,
                title = stringResource(R.string.feature_summary_pumped),
                titleTag = { MomBadge(stringResource(R.string.feature_summary_mom)) },
                detail = pumpedDetail,
                number = pumped.number,
                unit = pumped.unit,
                description = stringResource(R.string.feature_summary_today_pumped_description, "${pumped.number} ${pumped.unit}", pumpedDetail),
            )
            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 18.dp))
            TotalRow(
                icon = BabyIcons.Drop,
                badgeColor = colors.diaper.bg,
                iconColor = colors.diaperIcon,
                title = stringResource(R.string.feature_summary_diapers),
                detail = diapersDetail,
                number = today.diapers.toString(),
                unit = null,
                description = stringResource(R.string.feature_summary_today_diapers_description, diapersCount, diapersDetail),
            )
        }
    }
}

/** One hero row: a badge, a title with a subtitle, and the big number. Read as a single sentence. */
@Composable
private fun TotalRow(
    icon: ImageVector,
    badgeColor: Color,
    iconColor: Color,
    title: String,
    detail: String,
    number: String,
    unit: String?,
    description: String,
    titleTag: (@Composable () -> Unit)? = null,
) {
    val colors = BabyStatusTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        IconBadge(icon, containerColor = badgeColor, contentColor = iconColor, size = 48.dp, iconSize = 22.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                titleTag?.invoke()
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.stone)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(number, style = MaterialTheme.typography.displayMedium, color = colors.ink, modifier = Modifier.alignByBaseline())
            if (unit != null) {
                Text(unit, style = MaterialTheme.typography.bodyMedium, color = colors.stone, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

// --- Previous days ---

@Composable
private fun PreviousDaysSection(
    summary: Summary,
    unitSystem: UnitSystem,
    today: LocalDate,
    onOpenHistory: (focusEpochDay: Long?) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
            CapsLabel(
                stringResource(R.string.feature_summary_previous_days),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = { onOpenHistory(null) }) {
                Text(
                    stringResource(R.string.feature_summary_full_history),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = colors.moss,
                )
            }
        }
        if (summary.previous.isEmpty()) {
            Text(
                stringResource(R.string.feature_summary_earlier_days),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.stone,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        } else {
            DaysTableHeader()
            summary.previous.forEach { day ->
                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                DayRow(day, unitSystem, today, onOpenHistory)
            }
        }
    }
}

private const val DAY_WEIGHT = 1.45f

@Composable
private fun DaysTableHeader() {
    val colors = BabyStatusTheme.colors
    Row(
        // The header row repeats what each cell below it says, so a screen reader skips it.
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(top = 4.dp, bottom = 6.dp).clearAndSetSemantics { },
    ) {
        HeaderCell(stringResource(R.string.feature_summary_column_day), Modifier.weight(DAY_WEIGHT), TextAlign.Start)
        HeaderCell(stringResource(R.string.feature_summary_column_fed), Modifier.weight(1f), TextAlign.End)
        HeaderCell(stringResource(R.string.feature_summary_column_pumped), Modifier.weight(1f), TextAlign.End)
        HeaderCell(stringResource(R.string.feature_summary_column_diapers), Modifier.weight(1f), TextAlign.End)
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = BabyStatusTheme.colors.chevron,
        textAlign = align,
        maxLines = 1,
        modifier = modifier,
    )
}

/** A previous day. A day with entries opens History at that day; an empty one is just dashes. */
@Composable
private fun DayRow(day: DaySummary, unitSystem: UnitSystem, today: LocalDate, onOpenHistory: (focusEpochDay: Long?) -> Unit) {
    val colors = BabyStatusTheme.colors
    val dash = stringResource(R.string.feature_summary_none)
    val label = TimeFormatter.dayLabel(day.date, today)
    val fed = amount(day.fed, unitSystem, dash)
    val pumped = amount(day.pumped, unitSystem, dash)
    val diapers = if (day.diapers > 0) day.diapers.toString() else dash
    val description = if (day.hasEntries) {
        stringResource(
            R.string.feature_summary_row_description,
            label,
            UnitFormatter.volume(day.fed, unitSystem),
            UnitFormatter.volume(day.pumped, unitSystem),
            pluralStringResource(R.plurals.feature_summary_diapers_count, day.diapers, day.diapers),
        )
    } else {
        stringResource(R.string.feature_summary_row_empty_description, label)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (day.hasEntries) {
                    Modifier.clickable(role = Role.Button) { onOpenHistory(day.date.toEpochDay()) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 4.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (day.hasEntries) colors.ink else colors.stone,
            modifier = Modifier.weight(DAY_WEIGHT),
        )
        ValueCell(fed, Modifier.weight(1f))
        ValueCell(pumped, Modifier.weight(1f))
        ValueCell(diapers, Modifier.weight(1f))
    }
}

/** Muted, right-aligned figures: the hero card is the loud part of this screen. */
@Composable
private fun ValueCell(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = BabyStatusTheme.colors.segmentUnselected,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = modifier,
    )
}

/** "27 oz", or a dash when there is nothing to add up. */
private fun amount(volume: Volume, unitSystem: UnitSystem, dash: String): String =
    if (volume.ml > 0) UnitFormatter.volume(volume, unitSystem) else dash

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SummaryScreenPreview() {
    val today = LocalDate.of(2026, 9, 24)
    fun day(offset: Long, fedOz: Double, pumpedOz: Double, pee: Int, potty: Int, bottles: Int = 4) = DaySummary(
        date = today.minusDays(offset),
        fed = Volume.ofOz(fedOz),
        bottleCount = bottles,
        solidsCount = 0,
        pumped = Volume.ofOz(pumpedOz),
        pumpSessions = if (pumpedOz > 0) 3 else 0,
        pee = pee,
        potty = potty,
        entryCount = bottles + pee + potty,
    )
    BabyStatusTheme {
        SummaryScreen(
            uiState = SummaryUiState.Ready(
                summary = Summary(
                    today = day(0, 18.0, 14.0, 4, 2).copy(solidsCount = 1),
                    previous = listOf(
                        day(1, 27.0, 16.0, 6, 2),
                        day(2, 26.5, 15.0, 5, 3),
                        day(3, 0.0, 0.0, 0, 0, bottles = 0).copy(entryCount = 0),
                        day(4, 25.0, 0.0, 6, 1),
                        day(5, 24.0, 12.0, 5, 2),
                    ),
                ),
                unitSystem = UnitSystem.IMPERIAL,
            ),
            onBack = {},
            onOpenHistory = {},
        )
    }
}
