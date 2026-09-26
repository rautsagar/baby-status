package com.sagar.babystatus.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme

private val ButtonShape = RoundedCornerShape(18.dp)

/** Full-width forest button with gold text: "Save feed", "Save growth". */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = BabyStatusTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.forest,
            contentColor = colors.goldLight,
            disabledContainerColor = colors.forest.copy(alpha = 0.4f),
            disabledContentColor = colors.goldLight.copy(alpha = 0.7f),
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Full-width gold button with forest text: onboarding "Continue". */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = BabyStatusTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.gold,
            contentColor = colors.forest,
            disabledContainerColor = colors.gold.copy(alpha = 0.4f),
            disabledContentColor = colors.forest.copy(alpha = 0.6f),
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Red text button with a trash icon: "Delete entry". */
@Composable
fun DangerTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = BabyStatusTheme.colors.danger),
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(BabyIcons.Trash, contentDescription = null, modifier = Modifier.size(17.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

/** Round icon-only button. [contentDescription] is required: it's the only label. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = BabyStatusTheme.colors.ink,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = containerColor, contentColor = contentColor),
        shape = CircleShape,
        modifier = modifier.size(size),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

/** The gold "Log activity" extended FAB. */
@Composable
fun LogFab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BabyStatusTheme.colors
    ExtendedFloatingActionButton(
        onClick = onClick,
        shape = ButtonShape,
        containerColor = colors.gold,
        contentColor = colors.ink,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = modifier.height(56.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingValues(start = 0.dp, end = 4.dp)),
        ) {
            Icon(BabyIcons.Plus, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize))
        }
    }
}
