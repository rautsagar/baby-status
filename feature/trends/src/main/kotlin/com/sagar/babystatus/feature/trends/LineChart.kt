package com.sagar.babystatus.feature.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sagar.babystatus.core.designsystem.component.WhiteCard
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.model.Mass
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.model.Volume
import com.sagar.babystatus.core.ui.format.TimeFormatter
import com.sagar.babystatus.core.ui.format.UnitFormatter
import java.time.LocalDate
import kotlin.math.roundToInt

/** One plotted value: metric units (ml, g) or a count, never display units. */
data class ChartPoint(val date: LocalDate, val value: Double)

/**
 * A single line through [points], oldest first, spaced by date across [start]..[end] so a sparse
 * series isn't stretched to look evenly sampled. [yMin]..[yMax] is the plotted range: a faint
 * gridline marks each end, labelled [minLabel] and [maxLabel]; [startLabel] and [endLabel] sit
 * under the plot's ends. Screen readers get [description] as one node instead of the drawing.
 */
@Composable
internal fun LineChart(
    points: List<ChartPoint>,
    start: LocalDate,
    end: LocalDate,
    yMin: Double,
    yMax: Double,
    minLabel: String,
    maxLabel: String,
    startLabel: String,
    endLabel: String,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = BabyStatusTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.stone)
    Canvas(modifier.clearAndSetSemantics { contentDescription = description }) {
        val dotRadius = 3.dp.toPx()
        val top = textMeasurer.measure(maxLabel, labelStyle)
        val bottom = textMeasurer.measure(minLabel, labelStyle)
        val first = textMeasurer.measure(startLabel, labelStyle)
        val last = textMeasurer.measure(endLabel, labelStyle)

        // The y labels take a gutter on the left; the dates take a strip along the bottom.
        val gutter = maxOf(top.size.width, bottom.size.width).toFloat()
        val plotLeft = gutter + 10.dp.toPx() + dotRadius
        val plotRight = size.width - dotRadius
        val plotTop = maxOf(dotRadius, top.size.height / 2f)
        val plotBottom = size.height - first.size.height - 8.dp.toPx() - maxOf(dotRadius, bottom.size.height / 2f)

        val days = (end.toEpochDay() - start.toEpochDay()).coerceAtLeast(1)
        val span = yMax - yMin
        fun x(date: LocalDate) = plotLeft + (plotRight - plotLeft) * (date.toEpochDay() - start.toEpochDay()) / days
        // A flat range (every value equal) sits on the bottom line.
        fun y(value: Double) = if (span > 0) plotBottom - (plotBottom - plotTop) * ((value - yMin) / span).toFloat() else plotBottom

        val grid = 1.dp.toPx()
        drawLine(colors.divider, Offset(plotLeft - dotRadius, plotBottom), Offset(size.width, plotBottom), grid)
        drawLabel(bottom, gutter - bottom.size.width, plotBottom)
        if (span > 0) {
            drawLine(colors.divider, Offset(plotLeft - dotRadius, plotTop), Offset(size.width, plotTop), grid)
            drawLabel(top, gutter - top.size.width, plotTop)
        }

        val line = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) line.moveTo(x(point.date), y(point.value)) else line.lineTo(x(point.date), y(point.value))
        }
        drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEach { point -> drawCircle(color, dotRadius, Offset(x(point.date), y(point.value))) }

        val datesTop = size.height - first.size.height
        drawText(first, topLeft = Offset(plotLeft - dotRadius, datesTop))
        drawText(last, topLeft = Offset(size.width - last.size.width, datesTop))
    }
}

/** Draws [label] vertically centred on [centerY]. */
private fun DrawScope.drawLabel(label: TextLayoutResult, left: Float, centerY: Float) {
    drawText(label, topLeft = Offset(left, centerY - label.size.height / 2f))
}

@Preview(widthDp = 390)
@Composable
private fun LineChartPreview() {
    val end = LocalDate.of(2026, 9, 24)
    val start = end.minusDays(29)
    // A daily series from zero (Feeding) and a sparse one that doesn't start at zero (Growth).
    val fed = (0L..29L).map { ChartPoint(start.plusDays(it), Volume.ofOz(18.0 + (it * 7 % 11)).ml) }
    val weight = listOf(2L, 9L, 17L, 26L).mapIndexed { index, day -> ChartPoint(start.plusDays(day), 5200.0 + index * 260) }
    BabyStatusTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WhiteCard {
                LineChart(
                    points = fed,
                    start = start,
                    end = end,
                    yMin = 0.0,
                    yMax = fed.maxOf { it.value },
                    minLabel = UnitFormatter.volume(Volume(0.0), UnitSystem.IMPERIAL),
                    maxLabel = UnitFormatter.volume(Volume(fed.maxOf { it.value }), UnitSystem.IMPERIAL),
                    startLabel = TimeFormatter.shortDate(start),
                    endLabel = TimeFormatter.shortDate(end),
                    color = BabyStatusTheme.colors.feedIcon,
                    description = "",
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                )
            }
            WhiteCard {
                LineChart(
                    points = weight,
                    start = start,
                    end = end,
                    yMin = weight.minOf { it.value },
                    yMax = weight.maxOf { it.value },
                    minLabel = UnitFormatter.mass(Mass(weight.minOf { it.value }.roundToInt()), UnitSystem.IMPERIAL),
                    maxLabel = UnitFormatter.mass(Mass(weight.maxOf { it.value }.roundToInt()), UnitSystem.IMPERIAL),
                    startLabel = TimeFormatter.shortDate(start),
                    endLabel = TimeFormatter.shortDate(end),
                    color = BabyStatusTheme.colors.forest,
                    description = "",
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                )
            }
        }
    }
}
