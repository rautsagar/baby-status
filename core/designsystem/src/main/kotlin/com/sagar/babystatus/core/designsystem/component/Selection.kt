package com.sagar.babystatus.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sagar.babystatus.core.designsystem.theme.Accent
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme

/** Big activity-type tile (Feed / Diaper) in the log sheet. Use inside a [selectableGroup]. */
@Composable
fun SelectableTile(
    label: String,
    icon: ImageVector,
    accent: Accent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = BabyStatusTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(88.dp)
            // A locked choice (e.g. the other type while editing) reads as unavailable.
            .alpha(if (enabled || selected) 1f else 0.45f)
            .clip(shape)
            .background(if (selected) accent.bg else colors.card)
            .border(2.dp, if (selected) accent.border else Color.Transparent, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    ) {
        IconBadge(
            icon = icon,
            size = 40.dp,
            iconSize = 22.dp,
            containerColor = if (selected) accent.border else accent.bg,
            contentColor = if (selected) Color.White else accent.fg,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) accent.fg else colors.ink,
        )
    }
}

/** A choice in a grid of options (Pee / Potty, Taste / Some / Lots, reactions). */
@Composable
fun OptionButton(
    label: String,
    accent: Accent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val colors = BabyStatusTheme.colors
    val shape = RoundedCornerShape(if (height > 48.dp) 16.dp else 14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (selected) accent.bg else colors.card)
            .border(
                if (selected) BorderStroke(2.dp, accent.border) else BorderStroke(1.5.dp, colors.border),
                shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) accent.fg else colors.ink,
            textAlign = TextAlign.Center,
        )
    }
}

/** Visual style of a [SegmentedToggle]'s selected segment. */
enum class SegmentStyle { Raised, Gold }

/** Two or more segments on a sage track: Bottle | Solids, Imperial | Metric. */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: SegmentStyle = SegmentStyle.Raised,
    segmentHeight: Dp = 44.dp,
    fillWidth: Boolean = true,
    // Narrower than the default when four or more labels share one row (Trends).
    segmentPadding: Dp = 16.dp,
) {
    val colors = BabyStatusTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(if (style == SegmentStyle.Raised) 16.dp else 14.dp))
            .background(colors.sage)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val shape = RoundedCornerShape(if (style == SegmentStyle.Raised) 12.dp else 10.dp)
            val (container, content) = when {
                !selected -> Color.Transparent to if (style == SegmentStyle.Raised) colors.segmentUnselected else colors.stone
                style == SegmentStyle.Raised -> colors.card to colors.ink
                else -> colors.gold to colors.forest
            }
            Surface(
                color = container,
                contentColor = content,
                shape = shape,
                modifier = Modifier
                    .then(if (fillWidth) Modifier.weight(1f) else Modifier)
                    .height(segmentHeight)
                    .then(if (selected && style == SegmentStyle.Raised) Modifier.shadow(1.dp, shape) else Modifier)
                    .clip(shape)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) }),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = segmentPadding)) {
                    Text(
                        option,
                        style = if (style == SegmentStyle.Raised) {
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            )
                        },
                    )
                }
            }
        }
    }
}

/** An icon centered in a filled circle. */
@Composable
fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(CircleShape).background(containerColor),
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
    }
}

/** Arranges option buttons in equal columns, as the design's grids do. */
@Composable
fun OptionGrid(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().selectableGroup(),
        content = content,
    )
}
