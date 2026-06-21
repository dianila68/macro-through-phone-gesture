package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepetitionScorerTest {

    private fun accel(t: Long, x: Float, y: Float = 0f, z: Float = 0f) =
        SensorSample(SensorType.ACCELEROMETER, t, floatArrayOf(x, y, z))

    /** Builds a realistic shake-like gesture of [durationMs] at 100 Hz with given amplitude. */
    private fun shakeGesture(
        startMs: Long = 0L,
        durationMs: Long = 800L,
        peakMag: Float = 20f,
    ): List<SensorSample> {
        val samples = mutableListOf<SensorSample>()
        var t = startMs
        val step = 10L
        val periods = ((durationMs / step)).toInt()
        for (i in 0 until periods) {
            // Simple sine-like alternation to create peaks
            val x = peakMag * kotlin.math.sin(i * 0.4).toFloat()
            samples += accel(t, x)
            t += step
        }
        return samples
    }

    @Test fun `empty list scores zero with too-short feedback`() {
        val result = RepetitionScorer.score(emptyList())
        assertEquals(0f, result.overall, 0.001f)
        assertTrue(result.feedback.contains("short", ignoreCase = true)
            || result.feedback.contains("long", ignoreCase = true)
            || result.feedback.contains("short", ignoreCase = true))
    }

    @Test fun `fewer than 10 samples scores zero`() {
        val tiny = (0 until 5).map { accel(it * 10L, 10f) }
        val result = RepetitionScorer.score(tiny)
        assertEquals(0f, result.overall, 0.001f)
    }

    @Test fun `overall is clamped to 0-1`() {
        val samples = shakeGesture(peakMag = 50f)
        val result = RepetitionScorer.score(samples)
        assertTrue("overall=${result.overall} out of [0,1]", result.overall in 0f..1f)
    }

    @Test fun `score data class exposes duration, amplitude, peak sub-scores`() {
        val samples = shakeGesture()
        val result = RepetitionScorer.score(samples)
        assertTrue(result.durationScore in 0f..1f)
        assertTrue(result.amplitudeScore in 0f..1f)
        assertTrue(result.peakScore in 0f..1f)
    }

    @Test fun `too-short gesture (100 ms) gives low duration score`() {
        // 100 ms < MIN_DURATION_MS (150 ms)
        val samples = (0 until 15).map { accel(it * 6L, 20f) }
        val result = RepetitionScorer.score(samples)
        assertEquals(0f, result.durationScore, 0.001f)
        assertTrue(result.overall < 0.5f)
    }

    @Test fun `too-long gesture (4000 ms) gets partial duration score`() {
        // 4000 ms > MAX_DURATION_MS (3000 ms) → durationScore = 0.4
        val samples = (0 until 50).map { accel(it * 80L, 20f) }
        val result = RepetitionScorer.score(samples)
        assertEquals(0.4f, result.durationScore, 0.001f)
    }

    @Test fun `gesture within valid duration window scores 1 on duration`() {
        // 800 ms is within [150, 3000]
        val samples = shakeGesture(durationMs = 800L)
        val result = RepetitionScorer.score(samples)
        assertEquals(1f, result.durationScore, 0.001f)
    }

    @Test fun `low amplitude gesture gives low amplitude score`() {
        // magnitude ≈ 1 m/s² — well below TARGET_RMS_G = 15
        val samples = (0 until 30).map { accel(it * 10L, 0.5f, 0.5f, 0.5f) }
        val result = RepetitionScorer.score(samples)
        assertTrue("amplitudeScore should be low, got ${result.amplitudeScore}",
            result.amplitudeScore < 0.5f)
    }

    @Test fun `high amplitude gesture caps amplitude score at 1`() {
        val samples = shakeGesture(peakMag = 40f, durationMs = 800L)
        val result = RepetitionScorer.score(samples)
        assertTrue("amplitudeScore should be <=1, got ${result.amplitudeScore}",
            result.amplitudeScore <= 1f)
    }

    @Test fun `flat signal with no peaks gives low peak score`() {
        // Constant signal has no local maxima above threshold
        val samples = (0 until 30).map { accel(it * 10L, 5f) } // mag=5 < PEAK_THRESHOLD_G=12
        val result = RepetitionScorer.score(samples)
        assertEquals(0f, result.peakScore, 0.001f)
    }

    @Test fun `good shake gesture scores overall >= 0_5`() {
        val samples = shakeGesture(peakMag = 20f, durationMs = 600L)
        val result = RepetitionScorer.score(samples)
        assertTrue("good shake should score >=0.5, got ${result.overall}", result.overall >= 0.5f)
    }

    @Test fun `feedback string is non-empty for any input`() {
        listOf(
            emptyList(),
            shakeGesture(durationMs = 50L, peakMag = 1f),
            shakeGesture(durationMs = 800L, peakMag = 20f),
        ).forEach {
            assertTrue(RepetitionScorer.score(it).feedback.isNotBlank())
        }
    }
}
