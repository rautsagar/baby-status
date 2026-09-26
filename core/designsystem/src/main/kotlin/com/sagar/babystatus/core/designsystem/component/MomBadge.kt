package com.sagar.babystatus.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme

/** Small rust "MOM" tag marking Mom's entries (the pumping card, sheet and History rows). */
@Composable
fun MomBadge(text: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val colors = BabyStatusTheme.colors
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = if (large) 11.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (large) 0.9.sp else 0.6.sp,
        ),
        color = colors.mom.fg,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.mom.bg)
            .padding(horizontal = if (large) 8.dp else 6.dp, vertical = if (large) 3.dp else 2.dp),
    )
}
