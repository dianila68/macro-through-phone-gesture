package io.github.dianila68.gesturemacro.core.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepetitionQualityScorerTest {

    private fun makeWindow(magnitudes: List<Float>, durationMs: Long = 1500L): SampleWindow {
        val w = SampleWindow(0)
        w.startNs = 0L
        w.endNs = durationMs * 1_000_000L
        magnitudes.forEach { mag ->
            val v = floatArrayOf(mag, 0f, 0f)
            w.frames.add(SensorFrame(0L, SensorChannel.ACCELEROMETER, v))
        }
        return w
    }

    @Test
    fun tooShortWindow_lowQuality() {
        val w = makeWindow(listOf(9.81f, 9.82f), durationMs = 100)  // < 10% of 3000ms
        val q = RepetitionQualityScorer.score(w, 3_000L)
        assertEquals(QualityRating.LOW_QUALITY, q.rating)
    }

    @Test
    fun belowNoiseFoor_lowQuality() {
        // Magnitude just above gravity (net < 1.5 m/s²)
        val w = makeWindow(List(20) { 10.0f }, durationMs = 1500L)
        val q = RepetitionQualityScorer.score(w, 3_000L)
        assertTrue("score should be low: ${q.score}", q.score < 0.4f)
    }

    @Test
    fun goodMotion_acceptableOrGood() {
        // Peak well above noise floor, good duration
        val w = makeWindow(List(30) { 14f }, durationMs = 1200L)
        val q = RepetitionQualityScorer.score(w, 3_000L)
        assertTrue("score should be acceptable+: ${q.score}", q.score >= 0.4f)
    }

    @Test
    fun emptyWindow_lowQuality() {
        val w = SampleWindow(0)
        val q = RepetitionQualityScorer.score(w, 3_000L)
        assertEquals(QualityRating.LOW_QUALITY, q.rating)
    }
}
