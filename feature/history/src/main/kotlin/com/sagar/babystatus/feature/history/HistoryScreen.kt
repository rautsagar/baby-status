package com.sagar.babystatus.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sagar.babystatus.core.designsystem.component.ForestTopBar
import com.sagar.babystatus.core.designsystem.component.IconBadge
import com.sagar.babystatus.core.designsystem.component.MomBadge
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.model.TimelineDay
import com.sagar.babystatus.core.model.TimelineEntry
import com.sagar.babystatus.core.model.occurredAtOrNull
import com.sagar.babystatus.core.ui.entrySubtitle
import com.sagar.babystatus.core.ui.entryTitle
import com.sagar.babystatus.core.ui.entryVisual
import com.sagar.babystatus.core.ui.isMoms
import com.sagar.babystatus.core.ui.format.TimeFormatter
import com.sagar.babystatus.core.ui.format.UnitFormatter
import com.sagar.babystatus.core.ui.rememberIs24Hour

@Composable
internal fun HistoryRoute(
    onBack: () -> Unit,
    onEditEntry: (TimelineEntry) -> Unit,
    viewModel: HistoryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val days = viewModel.days.collectAsLazyPagingItems()
    val initialScrollIndex by viewModel.initialScrollIndex.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        days = days,
        initialScrollIndex = initialScrollIndex,
        onScrolled = viewModel::onScrolled,
        onBack = onBack,
        onEditEntry = onEditEntry,
        onDelete = viewModel::delete,
    )
}

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    days: LazyPagingItems<TimelineDay>,
    initialScrollIndex: Int?,
    onScrolled: () -> Unit,
    onBack: () -> Unit,
    onEditEntry: (TimelineEntry) -> Unit,
    onDelete: (TimelineEntry) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    val listState = rememberLazyListState()

    // Opened for a particular day (from Summary): jump there once. The list has a placeholder
    // for every day, so the index is valid before its page loads.
    LaunchedEffect(initialScrollIndex, days.itemCount) {
        if (initialScrollIndex != null && days.itemCount > initialScrollIndex) {
            listState.scrollToItem(initialScrollIndex)
            onScrolled()
        }
    }

    Column(Modifier.fillMaxSize().background(colors.sage)) {
        ForestTopBar(
            title = stringResource(R.string.feature_history_title),
            backLabel = stringResource(R.string.feature_history_back),
            onBack = onBack,
        )
        val isEmpty = days.itemCount == 0 && days.loadState.refresh is LoadState.NotLoading
        if (isEmpty) {
            Text(
                stringResource(R.string.feature_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.stone,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        ) {
            items(
                count = days.itemCount,
                key = days.itemKey { it.date.toEpochDay() },
                contentType = days.itemContentType { "day" },
            ) { index ->
                days[index]?.let { day -> DaySection(day, uiState, onEditEntry, onDelete) }
            }
        }
    }
}

/** One day: its title and totals line, then a white card with all of its entries. */
@Composable
private fun DaySection(
    day: TimelineDay,
    uiState: HistoryUiState,
    onEditEntry: (TimelineEntry) -> Unit,
    onDelete: (TimelineEntry) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        DayHeader(day, uiState)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.card)) {
            day.entries.forEachIndexed { index, entry ->
                // Keyed by entry: the swipe state must follow the entry, not its position, or the
                // row after a deleted one would inherit "dismissed".
                key(entry.id) {
                    SwipeableRow(
                        entry = entry,
                        uiState = uiState,
                        showDivider = index > 0,
                        onClick = { onEditEntry(entry) },
                        onDelete = { onDelete(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: TimelineDay, uiState: HistoryUiState) {
    val colors = BabyStatusTheme.colors
    val summary = day.summary
    val fed = UnitFormatter.volume(summary.fed, uiState.unitSystem)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    ) {
        Text(
            TimeFormatter.dayLabel(day.date, uiState.today),
            style = MaterialTheme.typography.titleSmall,
            color = colors.forest,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            if (summary.pumped.ml > 0) {
                stringResource(
                    R.string.feature_history_totals_with_pumping,
                    fed,
                    UnitFormatter.volume(summary.pumped, uiState.unitSystem),
                    summary.pee,
                    summary.potty,
                )
            } else {
                stringResource(R.string.feature_history_totals, fed, summary.pee, summary.potty)
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = colors.stone,
        )
    }
}

@Composable
private fun SwipeableRow(
    entry: TimelineEntry,
    uiState: HistoryUiState,
    showDivider: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = BabyStatusTheme.colors
    val deleteLabel = stringResource(R.string.feature_history_delete)
    // Plain remember, not the saveable default: LazyColumn restores saved item state by key,
    // so an undone row would come back already dismissed and be deleted again.
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold = { it * 0.4f }) }
    Box(Modifier.background(colors.card)) {
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
            backgroundContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().background(colors.danger).padding(end = 20.dp),
                ) {
                    Icon(BabyIcons.Trash, contentDescription = null, tint = colors.cream, modifier = Modifier.size(18.dp))
                    Text(
                        deleteLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = colors.cream,
                    )
                }
            },
            modifier = Modifier.semantics {
                // Swiping isn't available to every user: offer delete as an action too.
                customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
            },
        ) {
            Column(Modifier.background(colors.card)) {
                if (showDivider) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
                EntryRow(entry, uiState, onClick)
            }
        }
    }
}

@Composable
private fun EntryRow(entry: TimelineEntry, uiState: HistoryUiState, onClick: () -> Unit) {
    val colors = BabyStatusTheme.colors
    val visual = entryVisual(entry, colors)
    val is24Hour = rememberIs24Hour()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            entry.occurredAtOrNull?.let { TimeFormatter.compactClockTime(it, uiState.zone, is24Hour) }.orEmpty(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = colors.stone,
            modifier = Modifier.width(52.dp),
        )
        IconBadge(
            icon = visual.icon,
            containerColor = visual.accent.bg,
            contentColor = visual.iconTint,
            size = 34.dp,
            iconSize = 17.dp,
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entryTitle(entry), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                if (entry.isMoms) MomBadge(stringResource(R.string.feature_history_mom))
            }
            entrySubtitle(entry, uiState.unitSystem)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = colors.stone)
            }
        }
    }
}
