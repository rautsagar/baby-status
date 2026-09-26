package com.sagar.babystatus.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme

private val FieldShape = RoundedCornerShape(14.dp)

/** A label above its control, as every form field in the design has. */
@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = BabyStatusTheme.colors.stone)
        content()
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = BabyStatusTheme.colors.danger)
        }
    }
}

/** Single-line text input styled like the design's white, bordered fields. */
@Composable
fun BabyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    suffix: String? = null,
    error: String? = null,
    height: Dp = 52.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = BabyStatusTheme.colors
    LabeledField(label = label, error = error, modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.forest),
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(FieldShape)
                        .background(colors.card)
                        .border(1.5.dp, if (error != null) colors.danger else colors.border, FieldShape)
                        .padding(horizontal = 14.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = textStyle, color = colors.stone.copy(alpha = 0.7f))
                        }
                        innerTextField()
                    }
                    if (suffix != null) {
                        Text(suffix, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = colors.stone)
                    }
                }
            },
        )
    }
}

/** Looks like a text field but opens a picker when tapped ("When", "Date", "Birth date"). */
@Composable
fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
) {
    val colors = BabyStatusTheme.colors
    LabeledField(label = label, error = error, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(FieldShape)
                .background(colors.card)
                .border(1.5.dp, if (error != null) colors.danger else colors.border, FieldShape)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                .padding(horizontal = 14.dp),
        ) {
            val showPlaceholder = value.isEmpty() && placeholder != null
            Text(
                if (showPlaceholder) placeholder.orEmpty() else value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (showPlaceholder) colors.stone.copy(alpha = 0.7f) else colors.ink,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Gold info box: "Enter weight, length, or both…". */
@Composable
fun HintBox(text: String, modifier: Modifier = Modifier) {
    val colors = BabyStatusTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(colors.hintBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(BabyIcons.Info, contentDescription = null, tint = colors.hintIcon, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp), color = colors.hintFg)
    }
}
