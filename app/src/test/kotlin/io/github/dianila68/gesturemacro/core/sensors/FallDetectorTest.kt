package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FallDetectorTest {

    private fun accel(t: Long, x: Float, y: Float, z: Float) =
        SensorSample(SensorType.ACCELEROMETER, t, floatArrayOf(x, y, z))

    /** Feed a sequence of identical samples spanning [startMs]..[startMs + durationMs) at 20 ms steps. */
    private fun FallDetector.feedWindow(
        startMs: Long,
        durationMs: Long,
        x: Float,
        y: Float,
        z: Float,
    ): GestureEvent? {
        var result: GestureEvent? = null
        var t = startMs
        while (t < startMs + durationMs) {
            result = feed(accel(t, x, y, z)) ?: result
            t += 20L
        }
        return result
    }

    @Test
    fun `positive fall free-fall then impact then stillness fires`() {
        val detector = FallDetector()

        // 1. Resting (normal gravity).
        detector.feedWindow(0, 100, 0f, 0f, 9.81f)

        // 2. Free-fall: magnitude ≈ 0.3 * 9.81 ≈ 2.9 m/s² (below freefallMaxMag at default sensitivity).
        // freefallMaxMag = lerp(0.3, 0.6, 0.5) * 9.81 = 0.45 * 9.81 ≈ 4.41 m/s²
        // Use mag ≈ 0.8 m/s² to stay well below threshold.
        detector.feedWindow(100, 100, 0f, 0f, 0.5f) // mag ≈ 0.5

        // 3. Impact spike: mag ≈ 30 m/s² — well above impactMinMag.
        // impactMinMag = lerp(3.0, 2.0, 0.5) * 9.81 = 2.5 * 9.81 ≈ 24.5 m/s²
        detector.feed(accel(200L, 0f, 5f, 30f)) // mag ≈ 30.4

        // 4. Post-impact stillness for STILLNESS_DURATION_MS + a bit extra.
        val result = detector.feedWindow(
            220,
            FallDetector.STILLNESS_DURATION_MS + 100,
            0f, 0f, 9.81f,
        )
        assertNotNull("fall should fire after free-fall + impact + stillness", result)
        assertEquals(GesturePattern.FALL, result?.pattern)
        assertTrue("confidence should be > 0", (result?.confidence ?: 0f) > 0f)
    }

    @Test
    fun `set-down-hard without free-fall does not fire`() {
        val detector = FallDetector()
        // Resting, then a hard thud — no free-fall phase.
        detector.feedWindow(0, 100, 0f, 0f, 9.81f)
        detector.feed(accel(100L, 0f, 5f, 35f)) // big spike
        val result = detector.feedWindow(120, FallDetector.STILLNESS_DURATION_MS + 100, 0f, 0f, 9.81f)
        assertNull("no free-fall → should not fire", result)
    }

    @Test
    fun `free-fall too brief is rejected`() {
        val detector = FallDetector()
        // Free-fall for only 40 ms (< MIN_FREEFALL_MS = 60).
        detector.feed(accel(0L, 0f, 0f, 0.5f))
        detector.feed(accel(20L, 0f, 0f, 0.5f))
        // Back to normal gravity (no free-fall duration met).
        detector.feed(accel(40L, 0f, 0f, 9.81f))
        // Even a big impact now should not fire.
        detector.feed(accel(60L, 0f, 5f, 35f))
        val result = detector.feedWindow(80, FallDetector.STILLNESS_DURATION_MS + 100, 0f, 0f, 9.81f)
        assertNull("brief free-fall should be rejected", result)
    }

    @Test
    fun `drop on couch with motion after impact does not fire`() {
        val detector = FallDetector()
        // Free-fall.
        detector.feedWindow(0, 100, 0f, 0f, 0.5f)
        // Impact.
        detector.feed(accel(100L, 0f, 5f, 30f))
        // Post-impact motion (bouncing on couch) — magnitude above abort threshold.
        // motionAbortThreshold = impactMinMag * 0.5 ≈ 24.5 * 0.5 ≈ 12.25 m/s²
        detector.feed(accel(120L, 0f, 5f, 20f)) // mag ≈ 20.6 → aborts stillness watch
        val result = detector.feedWindow(140, FallDetector.STILLNESS_DURATION_MS + 100, 0f, 0f, 9.81f)
        assertNull("motion after impact should abort fall detection", result)
    }

    @Test
    fun `no impact after free-fall times out without firing`() {
        val detector = FallDetector()
        // Free-fall.
        detector.feedWindow(0, 100, 0f, 0f, 0.5f)
        // No impact — gentle landing or caught.
        val result = detector.feedWindow(100, FallDetector.IMPACT_WINDOW_MS + 200, 0f, 0f, 9.81f)
        assertNull("no impact should not fire", result)
    }

    @Test
    fun `walking pattern does not trigger free-fall`() {
        val detector = FallDetector()
        // Walking: rhythmic variation around 1g, never dropping to free-fall range.
        val mags = listOf(9.81f, 11f, 13f, 11f, 9.81f, 8f, 9.81f, 11f, 13f)
        val result = mags.flatMapIndexed { i, m ->
            val g = m / sqrt(2f) // distribute across x and z for a realistic walking vector
            listOf(detector.feed(accel((i * 80).toLong(), g, 0f, g)))
        }.lastOrNull { it != null }
        assertNull("walking should not look like a fall", result)
    }

    @Test
    fun `reset clears state`() {
        val detector = FallDetector()
        // Start a free-fall, then reset mid-way.
        detector.feedWindow(0, 100, 0f, 0f, 0.5f)
        detector.reset()
        // Impact that would have completed the pattern does nothing.
        detector.feed(accel(100L, 0f, 5f, 30f))
        val result = detector.feedWindow(120, FallDetector.STILLNESS_DURATION_MS + 100, 0f, 0f, 9.81f)
        assertNull("after reset, partial state should be cleared", result)
    }

    @Test
    fun `non-accelerometer samples are ignored`() {
        val detector = FallDetector()
        val result = detector.feed(SensorSample(SensorType.PROXIMITY, 0L, floatArrayOf(0f, 0f, 0f)))
        assertNull(result)
    }
}
