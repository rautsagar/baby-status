package com.sagar.babystatus.core.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme

/**
 * Shows the top entry in a [ModalBottomSheet] over the entries below it when that entry
 * was declared with [bottomSheet] metadata. Swipe-down / scrim tap pop the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        if (!entry.metadata.contains(BottomSheetKey)) return null
        val below = entries.dropLast(1)
        return BottomSheetScene(
            key = entry.contentKey,
            entry = entry,
            previousEntries = below,
            overlaidEntries = below,
            onBack = onBack,
        )
    }

    companion object {
        /** Marks an entry to be shown as a bottom sheet. */
        fun bottomSheet(): Map<String, Any> = metadata { put(BottomSheetKey, true) }
    }
}

private object BottomSheetKey : NavMetadataKey<Boolean>

@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    /** Held so a programmatic pop (e.g. after Save) can slide the sheet away first. */
    private var sheetState: SheetState? = null

    override val content: @Composable () -> Unit = {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        sheetState = state
        val colors = BabyStatusTheme.colors
        ModalBottomSheet(
            onDismissRequest = onBack,
            sheetState = state,
            containerColor = colors.cream,
            contentColor = colors.ink,
            scrimColor = colors.forestDeep.copy(alpha = 0.55f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { SheetDragHandle() },
            // A tall sheet stops below the status bar instead of sliding under it.
            modifier = Modifier.statusBarsPadding(),
            // Light status bar icons over the forest scrim; dark nav bar icons over the cream sheet.
            properties = ModalBottomSheetProperties(
                isAppearanceLightStatusBars = false,
                isAppearanceLightNavigationBars = true,
            ),
        ) {
            entry.Content()
        }
    }

    override suspend fun onRemove() {
        sheetState?.hide()
    }

    // NavDisplay compares scenes to decide whether to animate; equal content means the same scene.
    override fun equals(other: Any?): Boolean = other is BottomSheetScene<*> &&
        key == other.key &&
        entry == other.entry &&
        previousEntries == other.previousEntries &&
        overlaidEntries == other.overlaidEntries

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + entry.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        return result
    }
}

@Composable
private fun SheetDragHandle() {
    Surface(
        color = BabyStatusTheme.colors.handle,
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
    ) {
        Box(Modifier.size(width = 36.dp, height = 4.dp))
    }
}
