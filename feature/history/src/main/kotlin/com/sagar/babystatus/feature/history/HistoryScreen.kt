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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    HistoryScreen(
        uiState = uiState,
        items = items,
        onBack = onBack,
        onEditEntry = onEditEntry,
        onDelete = viewModel::delete,
    )
}

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    items: LazyPagingItems<HistoryItem>,
    onBack: () -> Unit,
    onEditEntry: (TimelineEntry) -> Unit,
    onDelete: (TimelineEntry) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(Modifier.fillMaxSize().background(colors.sage)) {
        ForestTopBar(
            title = stringResource(R.string.feature_history_title),
            backLabel = stringResource(R.string.feature_history_back),
            onBack = onBack,
        )
        val isEmpty = items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading
        if (isEmpty) {
            Text(
                stringResource(R.string.feature_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.stone,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        ) {
            items(
                count = items.itemCount,
                key = items.itemKey { item ->
                    when (item) {
                        is HistoryItem.DayHeader -> "day-${item.date}"
                        is HistoryItem.Row -> item.entry.id
                    }
                },
                contentType = items.itemContentType { it::class },
            ) { index ->
                when (val item = items[index]) {
                    is HistoryItem.DayHeader -> DayHeader(item, uiState)
                    is HistoryItem.Row -> {
                        // A day's rows share one white card: round only its first and last row.
                        val isFirst = index == 0 || items.peek(index - 1) is HistoryItem.DayHeader
                        val isLast = index == items.itemCount - 1 || items.peek(index + 1) is HistoryItem.DayHeader
                        SwipeableRow(
                            entry = item.entry,
                            uiState = uiState,
                            isFirst = isFirst,
                            isLast = isLast,
                            onClick = { onEditEntry(item.entry) },
                            onDelete = { onDelete(item.entry) },
                        )
                    }
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun DayHeader(header: HistoryItem.DayHeader, uiState: HistoryUiState) {
    val colors = BabyStatusTheme.colors
    val totals = uiState.totals[header.date]
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    ) {
        Text(
            TimeFormatter.dayLabel(header.date, uiState.today),
            style = MaterialTheme.typography.titleSmall,
            color = colors.forest,
            modifier = Modifier.semantics { heading() },
        )
        if (totals != null) {
            val fed = UnitFormatter.volume(totals.bottle, uiState.unitSystem)
            Text(
                if (totals.pumped.ml > 0) {
                    stringResource(
                        R.string.feature_history_totals_with_pumping,
                        fed,
                        UnitFormatter.volume(totals.pumped, uiState.unitSystem),
                        totals.peeCount,
                        totals.pottyCount,
                    )
                } else {
                    stringResource(R.string.feature_history_totals, fed, totals.peeCount, totals.pottyCount)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = colors.stone,
            )
        }
    }
}

@Composable
private fun SwipeableRow(
    entry: TimelineEntry,
    uiState: HistoryUiState,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = BabyStatusTheme.colors
    val corner = 20.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) corner else 0.dp,
        topEnd = if (isFirst) corner else 0.dp,
        bottomStart = if (isLast) corner else 0.dp,
        bottomEnd = if (isLast) corner else 0.dp,
    )
    val deleteLabel = stringResource(R.string.feature_history_delete)
    // Plain remember, not the saveable default: LazyColumn restores saved item state by key,
    // so an undone row would come back already dismissed and be deleted again.
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold = { it * 0.4f }) }
    Box(Modifier.clip(shape).background(colors.card)) {
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
                if (!isFirst) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
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
