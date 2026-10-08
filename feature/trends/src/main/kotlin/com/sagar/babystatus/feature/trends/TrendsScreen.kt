package com.sagar.babystatus.feature.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.ForestTopBar
import com.sagar.babystatus.core.designsystem.component.SegmentedToggle
import com.sagar.babystatus.core.designsystem.component.WhiteCard
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.domain.TrendCategory
import com.sagar.babystatus.core.domain.Trends
import com.sagar.babystatus.core.model.DaySummary
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.ui.format.TimeFormatter
import com.sagar.babystatus.core.ui.format.UnitFormatter
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
internal fun TrendsRoute(
    onBack: () -> Unit,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrendsScreen(uiState = uiState, onBack = onBack, onSelectCategory = viewModel::onSelectCategory)
}

@Composable
internal fun TrendsScreen(
    uiState: TrendsUiState,
    onBack: () -> Unit,
    onSelectCategory: (TrendCategory) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(Modifier.fillMaxSize().background(colors.sage)) {
        ForestTopBar(
            title = stringResource(R.string.feature_trends_title),
            backLabel = stringResource(R.string.feature_trends_back),
            onBack = onBack,
        )
        if (uiState is TrendsUiState.Ready) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                SegmentedToggle(
                    options = TrendCategory.entries.map { stringResource(it.label) },
                    selectedIndex = uiState.category.ordinal,
                    onSelect = { onSelectCategory(TrendCategory.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                    segmentPadding = 4.dp,
                )
                TrendCard(uiState.trends, uiState.category, uiState.unitSystem)
            }
        }
    }
}

/** The selected category: title, the one figure, and the chart over the window. */
@Composable
private fun TrendCard(trends: Trends, category: TrendCategory, unitSystem: UnitSystem) {
    val colors = BabyStatusTheme.colors
    val points = category.points(trends.days)
    val values = points.map { it.value }
    val format = { value: Double -> category.format(value, unitSystem) }
    val title = stringResource(category.title)
    // Growth shows the newest weight; an average weight over a month means nothing.
    val figure = if (category == TrendCategory.GROWTH) values.lastOrNull() else values.average().takeIf { values.isNotEmpty() }
    val subtitle = figure?.let {
        if (category == TrendCategory.GROWTH) {
            stringResource(R.string.feature_trends_now, format(it))
        } else {
            stringResource(R.string.feature_trends_average, format(it))
        }
    }

    WhiteCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.stone)
            }
            if (points.size < 2 || figure == null) {
                Text(
                    stringResource(R.string.feature_trends_not_enough),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.stone,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            } else {
                // Volumes and counts start at zero, so a quiet day reads as low; weight never does.
                val yMin = if (category == TrendCategory.GROWTH) values.min() else 0.0
                val yMax = values.max()
                val spokenFigure = when (category) {
                    TrendCategory.GROWTH -> stringResource(R.string.feature_trends_now, format(figure))
                    TrendCategory.DIAPERS -> stringResource(
                        R.string.feature_trends_average_spoken,
                        pluralStringResource(R.plurals.feature_trends_diapers_count, figure.roundToInt(), figure.roundToInt()),
                    )
                    TrendCategory.FEEDING, TrendCategory.PUMPING -> stringResource(R.string.feature_trends_average_spoken, format(figure))
                }
                val description = stringResource(
                    R.string.feature_trends_chart_description,
                    title,
                    pluralStringResource(R.plurals.feature_trends_last_days, trends.days.size, trends.days.size),
                    format(values.min()),
                    format(yMax),
                    spokenFigure,
                )
                val start = trends.days.first().date
                val end = trends.days.last().date
                LineChart(
                    points = points,
                    start = start,
                    end = end,
                    yMin = yMin,
                    yMax = yMax,
                    minLabel = format(yMin),
                    maxLabel = format(yMax),
                    startLabel = TimeFormatter.shortDate(start),
                    endLabel = TimeFormatter.shortDate(end),
                    color = category.color(),
                    description = description,
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 16.dp),
                )
            }
        }
    }
}

private val TrendCategory.label: Int
    get() = when (this) {
        TrendCategory.GROWTH -> R.string.feature_trends_growth
        TrendCategory.FEEDING -> R.string.feature_trends_feeding
        TrendCategory.DIAPERS -> R.string.feature_trends_diapers
        TrendCategory.PUMPING -> R.string.feature_trends_pumping
    }

private val TrendCategory.title: Int
    get() = when (this) {
        TrendCategory.GROWTH -> R.string.feature_trends_weight
        TrendCategory.FEEDING -> R.string.feature_trends_fed_per_day
        TrendCategory.DIAPERS -> R.string.feature_trends_diapers_per_day
        TrendCategory.PUMPING -> R.string.feature_trends_pumped_per_day
    }

/** Growth: a point per day with a weight. The others: a point for every day in the window. */
private fun TrendCategory.points(days: List<DaySummary>): List<ChartPoint> = when (this) {
    TrendCategory.GROWTH -> days.mapNotNull { day -> day.weight?.let { ChartPoint(day.date, it.grams.toDouble()) } }
    TrendCategory.FEEDING -> days.map { ChartPoint(it.date, it.fed.ml) }
    TrendCategory.DIAPERS -> days.map { ChartPoint(it.date, it.diapers.toDouble()) }
    TrendCategory.PUMPING -> days.map { ChartPoint(it.date, it.pumped.ml) }
}

/** A plotted value (grams, ml or a count) in the current units: "13.2 lb", "24 oz", "6". */
private fun TrendCategory.format(value: Double, unitSystem: UnitSystem): String = when (this) {
    TrendCategory.GROWTH -> UnitFormatter.mass(Mass(value.roundToInt()), unitSystem)
    TrendCategory.FEEDING, TrendCategory.PUMPING -> UnitFormatter.volume(Volume(value), unitSystem)
    TrendCategory.DIAPERS -> value.roundToInt().toString()
}

@Composable
private fun TrendCategory.color(): Color {
    val colors = BabyStatusTheme.colors
    return when (this) {
        TrendCategory.GROWTH -> colors.forest
        TrendCategory.FEEDING -> colors.feedIcon
        TrendCategory.DIAPERS -> colors.diaperIcon
        TrendCategory.PUMPING -> colors.momIcon
    }
}

private class TrendCategoryProvider : PreviewParameterProvider<TrendCategory> {
    override val values = TrendCategory.entries.asSequence()
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun TrendsScreenPreview(@PreviewParameter(TrendCategoryProvider::class) category: TrendCategory) {
    val today = LocalDate.of(2026, 9, 24)
    val days = (29L downTo 0L).map { back ->
        val date = today.minusDays(back)
        DaySummary(
            date = date,
            fed = Volume.ofOz(20.0 + (back * 7 % 9)),
            bottleCount = 6,
            solidsCount = 0,
            pumped = Volume.ofOz(if (back % 6 == 0L) 0.0 else 12.0 + (back * 5 % 8)),
            pumpSessions = 3,
            pee = 4 + (back % 3).toInt(),
            potty = 1 + (back % 2).toInt(),
            entryCount = 14,
            // Weighed about once a week, gaining as babies do.
            weight = if (back % 7 == 2L) Mass(5900 - back.toInt() * 25) else null,
            length = null,
        )
    }
    BabyStatusTheme {
        TrendsScreen(
            uiState = TrendsUiState.Ready(Trends(days), category, UnitSystem.IMPERIAL),
            onBack = {},
            onSelectCategory = {},
        )
    }
}
