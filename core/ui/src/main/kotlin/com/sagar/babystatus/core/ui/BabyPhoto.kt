package com.sagar.babystatus.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import java.io.File

/** Where a photo comes from: a stored file, or a just-picked content uri not saved yet. */
sealed interface PhotoSource {
    data class Stored(val path: String) : PhotoSource
    data class Picked(val uri: String) : PhotoSource
}

/**
 * Circular baby photo over the design's moss circle and person silhouette, which stays
 * visible when there's no photo (or while it loads).
 */
@Composable
fun BabyPhoto(
    source: PhotoSource?,
    contentDescription: String?,
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = BabyStatusTheme.colors
    val context = LocalContext.current
    val model: Any? = remember(source) {
        when (source) {
            is PhotoSource.Stored -> File(context.filesDir, source.path)
            is PhotoSource.Picked -> source.uri
            null -> null
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(CircleShape).background(colors.moss),
    ) {
        Icon(BabyIcons.Person, contentDescription = null, tint = colors.goldLight, modifier = Modifier.size(iconSize))
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
