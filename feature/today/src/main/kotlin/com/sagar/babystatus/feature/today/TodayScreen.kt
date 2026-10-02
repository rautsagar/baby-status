package com.sagar.babystatus.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.CapsLabel
import com.sagar.babystatus.core.designsystem.component.CircleIconButton
import com.sagar.babystatus.core.designsystem.component.IconBadge
import com.sagar.babystatus.core.designsystem.component.LogFab
import com.sagar.babystatus.core.designsystem.component.MomBadge
import com.sagar.babystatus.core.designsystem.component.WhiteCard
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.domain.BabyAge
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.BottleFeed
import com.sagar.babystatus.core.model.DiaperChange
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.Length
import com.sagar.babystatus.core.model.LogEntry
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.PumpSummary
import com.sagar.babystatus.core.model.TimedEntry
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.ui.BabyPhoto
import com.sagar.babystatus.core.ui.PhotoSource
import com.sagar.babystatus.core.ui.entrySubtitle
import com.sagar.babystatus.core.ui.entryTitle
import com.sagar.babystatus.core.ui.entryVisual
import com.sagar.babystatus.core.ui.format.TimeFormatter
import com.sagar.babystatus.core.ui.format.UnitFormatter
import com.sagar.babystatus.core.ui.rememberIs24Hour
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun TodayRoute(
    onOpenSummary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onLogActivity: (ActivityType?) -> Unit,
    onLogGrowth: () -> Unit,
    onLogPump: () -> Unit,
    onEditEntry: (LogEntry) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        uiState = uiState,
        onOpenSummary = onOpenSummary,
        onOpenSettings = onOpenSettings,
        onOpenHistory = onOpenHistory,
        onLogActivity = onLogActivity,
        onLogGrowth = onLogGrowth,
        onLogPump = onLogPump,
        onEditEntry = onEditEntry,
    )
}

@Composable
internal fun TodayScreen(
    uiState: TodayUiState,
    onOpenSummary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onLogActivity: (ActivityType?) -> Unit,
    onLogGrowth: () -> Unit,
    onLogPump: () -> Unit,
    onEditEntry: (LogEntry) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Box(Modifier.fillMaxSize().background(colors.forest)) {
        if (uiState is TodayUiState.Ready) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    // Room for the FAB below the last card.
                    .padding(bottom = 96.dp),
            ) {
                Header(uiState.babyName, uiState.today, onOpenSummary, onOpenSettings)
                StatusCircle(uiState, onLogGrowth)
                ActivitySection(uiState, onOpenHistory, onLogActivity, onLogPump, onEditEntry)
            }
            LogFab(
                text = stringResource(R.string.feature_today_log_activity),
                onClick = { onLogActivity(null) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Header(name: String, today: LocalDate, onOpenSummary: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = BabyStatusTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(TimeFormatter.dateWithDay(today, today), style = MaterialTheme.typography.bodySmall, color = colors.mist)
        }
        CircleIconButton(
            icon = BabyIcons.BarChart,
            contentDescription = stringResource(R.string.feature_today_summary),
            onClick = onOpenSummary,
            containerColor = colors.goldLight,
            contentColor = colors.forest,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        CircleIconButton(
            icon = BabyIcons.Settings,
            contentDescription = stringResource(R.string.feature_today_settings),
            onClick = onOpenSettings,
            containerColor = colors.goldLight,
            contentColor = colors.forest,
        )
    }
}

@Composable
private fun StatusCircle(state: TodayUiState.Ready, onLogGrowth: () -> Unit) {
    val colors = BabyStatusTheme.colors
    val none = stringResource(R.string.feature_today_no_measurement)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
    ) {
        Box(Modifier.widthIn(max = 320.dp).fillMaxWidth().height(284.dp)) {
            Stat(
                label = stringResource(R.string.feature_today_age),
                value = formatAge(state.age),
                modifier = Modifier.align(Alignment.TopCenter),
            )
            Stat(
                label = stringResource(R.string.feature_today_weight),
                value = state.weight?.let { UnitFormatter.mass(it, state.unitSystem) } ?: none,
                onClick = onLogGrowth,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 6.dp),
            )
            Stat(
                label = stringResource(R.string.feature_today_height),
                value = state.length?.let { UnitFormatter.length(it, state.unitSystem) } ?: none,
                onClick = onLogGrowth,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 6.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(162.dp)
                    .shadow(20.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.goldLight),
            ) {
                BabyPhoto(
                    source = state.photoPath?.let(PhotoSource::Stored),
                    contentDescription = stringResource(R.string.feature_today_photo, state.babyName),
                    size = 150.dp,
                    iconSize = 60.dp,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = BabyStatusTheme.colors
    val logGrowth = stringResource(R.string.feature_today_log_growth)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClickLabel = logGrowth, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clearAndSetSemantics {
                contentDescription = "$label $value"
                if (onClick != null) {
                    role = Role.Button
                    onClick(label = logGrowth) { onClick(); true }
                }
            },
    ) {
        CapsLabel(label, color = colors.mist)
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.ivory, maxLines = 1)
    }
}

@Composable
private fun ActivitySection(
    state: TodayUiState.Ready,
    onOpenHistory: () -> Unit,
    onLogActivity: (ActivityType?) -> Unit,
    onLogPump: () -> Unit,
    onEditEntry: (LogEntry) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.feature_today_activity),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ivory,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = onOpenHistory) {
                Text(
                    stringResource(R.string.feature_today_see_all),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.goldLight,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActivityTile(
                label = stringResource(R.string.feature_today_feed),
                icon = BabyIcons.Bottle,
                iconBg = colors.feed.bg,
                iconTint = colors.feedIcon,
                last = state.lastFeed,
                now = state.now,
                onClick = { onLogActivity(ActivityType.FEED) },
                modifier = Modifier.weight(1f),
            )
            ActivityTile(
                label = stringResource(R.string.feature_today_diaper),
                icon = BabyIcons.Drop,
                iconBg = colors.diaper.bg,
                iconTint = colors.diaperIcon,
                last = state.lastDiaper,
                now = state.now,
                onClick = { onLogActivity(ActivityType.DIAPER) },
                modifier = Modifier.weight(1f),
            )
        }
        PumpingCard(state.pumping, state.now, state.unitSystem, onLogPump)
        RecentCard(state, onEditEntry)
    }
}

/** Mom's pumping at a glance; the whole card opens the Log pumping sheet. */
@Composable
private fun PumpingCard(
    pumping: PumpSummary,
    now: Instant,
    unitSystem: UnitSystem,
    onLogPump: () -> Unit,
) {
    val colors = BabyStatusTheme.colors
    val last = pumping.last
    val summary = when {
        last == null -> stringResource(R.string.feature_today_not_yet)
        pumping.pumpedToday.ml > 0 -> stringResource(
            R.string.feature_today_pumped_today,
            UnitFormatter.volume(pumping.pumpedToday, unitSystem),
            TimeFormatter.timeAgo(last.occurredAt, now).lowercase(),
        )
        else -> stringResource(R.string.feature_today_pumped_none_today, TimeFormatter.timeAgo(last.occurredAt, now).lowercase())
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .clickable(
                onClickLabel = stringResource(R.string.feature_today_log_pumping),
                role = Role.Button,
                onClick = onLogPump,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        IconBadge(icon = BabyIcons.Pump, containerColor = colors.mom.bg, contentColor = colors.momIcon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.feature_today_pumping),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                )
                MomBadge(stringResource(R.string.feature_today_mom))
            }
            Text(summary, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = colors.stone)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.mom.border)
                .padding(horizontal = 14.dp),
        ) {
            Icon(BabyIcons.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.feature_today_log),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ActivityTile(
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    last: TimedEntry?,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BabyStatusTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
    ) {
        IconBadge(icon = icon, containerColor = iconBg, contentColor = iconTint)
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colors.ink)
        Text(
            last?.let { TimeFormatter.timeAgo(it.occurredAt, now) } ?: stringResource(R.string.feature_today_not_yet),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = colors.stone,
        )
    }
}

@Composable
private fun RecentCard(state: TodayUiState.Ready, onEditEntry: (LogEntry) -> Unit) {
    val colors = BabyStatusTheme.colors
    val is24Hour = rememberIs24Hour()
    WhiteCard {
        if (state.recent.isEmpty()) {
            Text(
                stringResource(R.string.feature_today_nothing_today),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.stone,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            )
        }
        state.recent.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
            RecentRow(entry, state.unitSystem, state.zone, is24Hour, onClick = { onEditEntry(entry) })
        }
    }
}

@Composable
private fun RecentRow(entry: TimedEntry, unitSystem: UnitSystem, zone: ZoneId, is24Hour: Boolean, onClick: () -> Unit) {
    val colors = BabyStatusTheme.colors
    val visual = entryVisual(entry, colors)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            TimeFormatter.clockTime(entry.occurredAt, zone, is24Hour),
            style = MaterialTheme.typography.labelMedium,
            color = colors.stone,
            modifier = Modifier.width(64.dp),
        )
        Box(Modifier.size(10.dp).clip(CircleShape).background(visual.accent.border))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entryTitle(entry), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            entrySubtitle(entry, unitSystem)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.stone)
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TodayScreenPreview() {
    val zone = ZoneId.of("America/New_York")
    val now = LocalDate.of(2026, 9, 24).atTime(15, 5).atZone(zone).toInstant()
    val today = LocalDate.of(2026, 9, 24)
    BabyStatusTheme {
        TodayScreen(
            uiState = TodayUiState.Ready(
                babyName = "Ava",
                photoPath = null,
                age = BabyAge.Months(3, 1),
                weight = Mass.ofLb(13.2),
                length = Length.ofInches(23.5),
                lastFeed = BottleFeed("1", now.minusSeconds(8700), today, null, Volume.ofOz(5.0)),
                lastDiaper = DiaperChange("2", now.minusSeconds(3600), today, null, DiaperKind.PEE),
                recent = listOf(
                    DiaperChange("2", now.minusSeconds(3600), today, null, DiaperKind.PEE),
                    BottleFeed("1", now.minusSeconds(8700), today, null, Volume.ofOz(5.0)),
                ),
                pumping = PumpSummary(pumpedToday = Volume.ofOz(14.0), last = null),
                unitSystem = UnitSystem.IMPERIAL,
                now = now,
                today = today,
                zone = zone,
            ),
            onOpenSummary = {},
            onOpenSettings = {},
            onOpenHistory = {},
            onLogActivity = {},
            onLogGrowth = {},
            onLogPump = {},
            onEditEntry = {},
        )
    }
}
