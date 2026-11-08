package com.sagar.babystatus.core.designsystem.component

import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfettiTest {
    // A 412 × 915 dp phone at 2.625 px per dp.
    private val density = 2.625f
    private val width = 412 * density
    private val height = 915 * density

    private fun rain(seed: Int = 7) = Confetti.rain(width, height, density, colorCount = 7, random = Random(seed))

    /** Steps at 60 fps for [seconds]. */
    private fun Confetti.run(seconds: Float) = repeat((seconds * 60).toInt()) { step(1 / 60f) }

    @Test
    fun `pieces start above the screen, spread across it`() {
        val confetti = rain()
        assertEquals(Confetti.PIECES, confetti.pieces.size)
        assertTrue(confetti.pieces.all { it.y < 0f && it.x in 0f..width })
        assertTrue(confetti.pieces.map { it.colorIndex }.toSet().size > 3, "a mix of colors")
    }

    @Test
    fun `a second in, the confetti is falling through the screen`() {
        val confetti = rain()
        confetti.run(1f)
        val onScreen = confetti.pieces.count { it.started && it.y in 0f..height && it.alpha > 0f }
        assertTrue(onScreen > Confetti.PIECES / 2, "$onScreen pieces on screen")
        assertFalse(confetti.isFinished)
    }

    @Test
    fun `it's over in about three seconds`() {
        repeat(5) { seed ->
            val confetti = rain(seed)
            confetti.run(2f)
            assertFalse(confetti.isFinished, "still falling at 2 s")
            confetti.run(1.5f)
            assertTrue(confetti.isFinished, "gone by 3.5 s")
        }
    }

    @Test
    fun `on a tall phone the rain still reaches the bottom third before it fades`() {
        val confetti = rain()
        var lowestVisible = 0f
        repeat(4 * 60) {
            confetti.step(1 / 60f)
            confetti.pieces.filter { it.started && it.alpha > 0.5f }.forEach { lowestVisible = maxOf(lowestVisible, it.y) }
        }
        assertTrue(lowestVisible > height * 2 / 3, "lowest visible piece at ${(lowestVisible / height * 100).toInt()}% of the screen")
    }

    @Test
    fun `a long frame can't fling pieces into nonsense`() {
        val confetti = rain()
        confetti.step(1 / 30f)
        repeat(10) { confetti.step(0.5f) }
        assertTrue(confetti.pieces.none { it.x.isNaN() || it.y.isNaN() })
        assertTrue(confetti.isFinished)
    }
}
