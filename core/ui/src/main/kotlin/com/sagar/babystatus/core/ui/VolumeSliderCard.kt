package com.sagar.babystatus.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagar.babystatus.core.designsystem.component.CapsLabel
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.ui.format.VolumeScale

/**
 * The big-number amount card with a stepped slider: the bottle ("Amount", gold) and
 * pumping ("Amount pumped", rust) use the same card in their own accent.
 */
@Composable
fun VolumeSliderCard(
    label: String,
    sliderDescription: String,
    scale: VolumeScale,
    value: Double,
    onValueChange: (Double) -> Unit,
    accent: Color,
    track: Color,
    modifier: Modifier = Modifier,
) {
    val colors = BabyStatusTheme.colors
    val rangeStyle = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp, fontWeight = FontWeight.Normal)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .padding(20.dp),
    ) {
        CapsLabel(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(scale.label(value), style = MaterialTheme.typography.displayLarge, color = colors.ink)
            Text(
                scale.unitLabel,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                color = colors.stone,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = scale.min.toFloat()..scale.max.toFloat(),
            steps = scale.sliderSteps,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = track,
                activeTickColor = accent,
                inactiveTickColor = accent.copy(alpha = 0.5f),
            ),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = sliderDescription },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${scale.label(scale.min)} ${scale.unitLabel}", style = rangeStyle, color = colors.stone)
            Text("${scale.label(scale.max)} ${scale.unitLabel}", style = rangeStyle, color = colors.stone)
        }
    }
}

/** "ounces" / "milliliters", for the slider's spoken description. */
fun VolumeScale.unitName(): String =
    if (unitSystem == UnitSystem.IMPERIAL) "ounces" else "milliliters"
