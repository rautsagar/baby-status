package com.sagar.babystatus.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme

/** White rounded card that groups rows (recent activity, a History day, settings). */
@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BabyStatusTheme.colors.card),
        content = content,
    )
}

/** Upper-case caption ("AGE", "AMOUNT", "UNITS"). */
@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = BabyStatusTheme.colors.stone) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Title row of a bottom sheet with its close button. */
@Composable
fun SheetHeader(title: String, closeLabel: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = BabyStatusTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        CircleIconButton(
            icon = BabyIcons.Close,
            contentDescription = closeLabel,
            onClick = onClose,
            containerColor = BabyStatusTheme.colors.closeButton,
        )
    }
}

/** Forest top bar with rounded bottom corners and a back arrow (History, Settings). */
@Composable
fun ForestTopBar(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = BabyStatusTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(colors.forest)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
    ) {
        CircleIconButton(
            icon = BabyIcons.Back,
            contentDescription = backLabel,
            onClick = onBack,
            contentColor = colors.ivory,
            size = 44.dp,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.ivory,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        actions()
    }
}
