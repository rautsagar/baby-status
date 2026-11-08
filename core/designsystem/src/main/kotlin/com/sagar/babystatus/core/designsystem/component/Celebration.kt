package com.sagar.babystatus.core.designsystem.component

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import kotlinx.coroutines.flow.first
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Timing shared by everything that celebrates a growth gain. */
object Celebration {
    /** Lets the closing Log growth sheet get out of the way before anything moves. */
    const val START_DELAY_MILLIS = 200L
}

/** False when animations are switched off on the device (Accessibility → Remove animations). */
fun Context.animationsEnabled(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

/**
 * Confetti that rains from the top for about three seconds, each time [burst] changes to a new
 * non-null value. Put it above everything else: it has no touch handling or semantics, so the
 * screen underneath keeps working and TalkBack never lands on it.
 */
@Composable
fun ConfettiRain(burst: Any?, modifier: Modifier = Modifier) {
    val colors = BabyStatusTheme.colors
    val palette = remember(colors) {
        listOf(colors.goldLight, colors.gold, colors.ivory, colors.mist, colors.diaper.bg, colors.mom.bg, colors.feed.bg)
    }
    val density = LocalDensity.current.density
    var area by remember { mutableStateOf(IntSize.Zero) }
    var confetti by remember { mutableStateOf<Confetti?>(null) }
    var frameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(burst) {
        if (burst == null) return@LaunchedEffect
        val size = snapshotFlow { area }.first { it.width > 0 && it.height > 0 }
        val run = Confetti.rain(size.width.toFloat(), size.height.toFloat(), density, palette.size, Random.Default)
        confetti = run
        try {
            var last = withFrameNanos { it }
            while (!run.isFinished) {
                withFrameNanos { now ->
                    run.step(((now - last) / 1_000_000_000f).coerceAtMost(MAX_STEP_SECONDS))
                    last = now
                    frameTime = now
                }
            }
        } finally {
            confetti = null
        }
    }

    Canvas(modifier.fillMaxSize().onSizeChanged { area = it }) {
        frameTime // Read so the canvas redraws on every frame of the run.
        confetti?.pieces?.forEach { drawPiece(it, palette[it.colorIndex]) }
    }
}

private fun DrawScope.drawPiece(piece: ConfettiPiece, color: Color) {
    if (!piece.started) return
    val alpha = piece.alpha
    if (alpha <= 0f) return
    withTransform({
        translate(piece.x, piece.y)
        rotate(piece.rotation, pivot = Offset.Zero)
        // Squashing one axis with cos(tilt) makes the piece look like it flips over in 3D.
        scale(1f, cos(piece.tilt), pivot = Offset.Zero)
    }) {
        if (piece.round) {
            drawCircle(color, radius = piece.width / 2, center = Offset.Zero, alpha = alpha)
        } else {
            drawRect(color, Offset(-piece.width / 2, -piece.height / 2), Size(piece.width, piece.height), alpha = alpha)
        }
    }
}

/** A small gold "▲ 0.7 lb" pill: how much a measure went up. */
@Composable
fun GainChip(text: String, modifier: Modifier = Modifier) {
    val colors = BabyStatusTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(CircleShape)
            .background(colors.goldLight.copy(alpha = 0.16f))
            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
    ) {
        Canvas(Modifier.size(8.dp)) {
            val up = Path().apply {
                moveTo(size.width / 2, size.height * 0.1f)
                lineTo(size.width, size.height * 0.9f)
                lineTo(0f, size.height * 0.9f)
                close()
            }
            drawPath(up, colors.goldLight)
        }
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
            color = colors.goldLight,
            maxLines = 1,
        )
    }
}

private const val MAX_STEP_SECONDS = 1 / 30f

/** One piece of confetti. Positions, sizes and speeds are in pixels. */
internal class ConfettiPiece(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    /** Seconds before it appears, so the rain starts as a flurry rather than a sheet. */
    var delay: Float,
    val life: Float,
    val width: Float,
    val height: Float,
    val round: Boolean,
    val colorIndex: Int,
    /** Degrees, and degrees per second. */
    var rotation: Float,
    val spin: Float,
    /** The 3D flip: radians, and radians per second. */
    var tilt: Float,
    val tiltSpeed: Float,
    /** The sideways flutter: a phase in radians and a speed in pixels per second. */
    val swayPhase: Float,
    val sway: Float,
) {
    var age = 0f
    val started: Boolean get() = delay <= 0f

    /** Fully opaque until the last [Confetti.FADE_SECONDS] of its life. */
    val alpha: Float get() = ((life - age) / Confetti.FADE_SECONDS).coerceIn(0f, 1f)
}

/**
 * Falling confetti: gravity, air drag, a sideways flutter and a 3D tumble. Plain Kotlin, so it
 * runs in unit tests; [ConfettiRain] steps and draws it.
 */
internal class Confetti(
    val pieces: List<ConfettiPiece>,
    private val bottom: Float,
    private val density: Float,
    /** Pixels per second; pieces ease toward it instead of accelerating forever: paper floats. */
    private val fallSpeed: Float,
) {

    val isFinished: Boolean get() = pieces.none { it.isAlive() }

    fun step(seconds: Float) {
        val gravity = GRAVITY_DP * density
        for (p in pieces) {
            if (!p.started) {
                p.delay -= seconds
                continue
            }
            p.age += seconds
            p.vy += gravity * seconds
            if (p.vy > fallSpeed) p.vy += (fallSpeed - p.vy) * min(1f, 4f * seconds)
            p.vx -= p.vx * DRAG * seconds
            p.x += (p.vx + sin(p.swayPhase + p.age * SWAY_RATE) * p.sway) * seconds
            p.y += p.vy * seconds
            p.rotation += p.spin * seconds
            p.tilt += p.tiltSpeed * seconds
        }
    }

    private fun ConfettiPiece.isAlive() = !started || (age < life && y < bottom + OFF_SCREEN_DP * density)

    companion object {
        const val PIECES = 90
        const val FADE_SECONDS = 0.6f
        private const val GRAVITY_DP = 950f
        private const val MIN_FALL_SPEED_DP = 300f

        /** On a tall screen pieces fall faster, so the rain still sweeps the whole screen in time. */
        private const val SCREEN_CROSSING_SECONDS = 2.6f
        private const val DRAG = 1.6f
        private const val SWAY_RATE = 4f
        private const val OFF_SCREEN_DP = 40f

        /** [PIECES] pieces just above a [width] × [height] area, spread across its width. */
        fun rain(width: Float, height: Float, density: Float, colorCount: Int, random: Random): Confetti {
            fun between(from: Float, to: Float) = from + random.nextFloat() * (to - from)
            val pieces = List(PIECES) {
                val kind = random.nextFloat()
                val round = kind >= 0.82f
                val ribbon = kind in 0.55f..0.82f
                val w = when {
                    round -> between(5f, 7f)
                    ribbon -> between(3f, 4f)
                    else -> between(7f, 10f)
                }
                val h = when {
                    round -> w
                    ribbon -> between(12f, 16f)
                    else -> between(4f, 6f)
                }
                ConfettiPiece(
                    x = between(0f, width),
                    y = between(-60f, -10f) * density,
                    vx = between(-40f, 40f) * density,
                    vy = between(60f, 200f) * density,
                    delay = between(0f, 0.45f),
                    life = between(2.5f, 2.9f),
                    width = w * density,
                    height = h * density,
                    round = round,
                    colorIndex = random.nextInt(colorCount),
                    rotation = between(0f, 360f),
                    spin = between(-340f, 340f),
                    tilt = between(0f, 6.28f),
                    tiltSpeed = between(5f, 11f),
                    swayPhase = between(0f, 6.28f),
                    sway = between(18f, 42f) * density,
                )
            }
            val fallSpeed = maxOf(MIN_FALL_SPEED_DP * density, height / SCREEN_CROSSING_SECONDS)
            return Confetti(pieces, bottom = height, density = density, fallSpeed = fallSpeed)
        }
    }
}
