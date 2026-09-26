package com.sagar.babystatus.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The design's stroke icons, drawn on a 24-unit grid. They are drawn in black and take
 * their color from `Icon`'s tint.
 */
object BabyIcons {
    val Settings by lazy {
        strokeIcon(
            "Settings",
            2f,
            circle(12f, 12f, 3f),
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 " +
                "1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06" +
                "a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09" +
                "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 " +
                "9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33" +
                "l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4" +
                "h-.09a1.65 1.65 0 0 0-1.51 1z",
        )
    }
    val Bottle by lazy {
        strokeIcon("Bottle", 2f, "M9 2h6", "M10 2v3L8 8v12a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V8l-2-3V2", "M8 12h8")
    }
    val Drop by lazy { strokeIcon("Drop", 2f, "M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z") }
    val Bowl by lazy { strokeIcon("Bowl", 2f, "M2 12a8 8 0 0 0 16 0", "M2 12h20") }
    val Pump by lazy { strokeIcon("Pump", 2f, "M5 4h14l-4.5 7v7a2.5 2.5 0 0 1-5 0v-7z", "M9.5 15h5") }
    val Growth by lazy { strokeIcon("Growth", 2f, "M3 17l6-6 4 4 8-8", "M17 7h4v4") }
    val Plus by lazy { strokeIcon("Plus", 2.4f, "M12 5v14M5 12h14") }
    val Close by lazy { strokeIcon("Close", 2.2f, "M6 6l12 12M18 6L6 18") }
    val Back by lazy { strokeIcon("Back", 2.2f, "M15 18l-6-6 6-6") }
    val Chevron by lazy { strokeIcon("Chevron", 2f, "M9 18l6-6-6-6") }
    val Trash by lazy {
        strokeIcon(
            "Trash",
            2f,
            "M3 6h18",
            "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
            "M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6",
        )
    }
    val Camera by lazy {
        strokeIcon(
            "Camera",
            1.8f,
            "M4 8a2 2 0 0 1 2-2h1.5l1-1.5h7l1 1.5H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z",
            circle(12f, 13f, 3.5f),
        )
    }
    val Person by lazy { strokeIcon("Person", 1.6f, circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0") }
    val Info by lazy { strokeIcon("Info", 2f, circle(12f, 12f, 9f), "M12 8v5", "M12 16.5v.01") }
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0"

private fun strokeIcon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { path ->
            addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
