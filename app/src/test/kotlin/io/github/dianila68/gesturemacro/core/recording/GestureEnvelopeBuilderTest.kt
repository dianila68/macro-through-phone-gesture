package io.github.dianila68.gesturemacro.core.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureEnvelopeBuilderTest {

    private fun makeWindow(index: Int, magnitude: Float, durationMs: Long = 1500L): SampleWindow {
        val w = SampleWindow(index)
        w.startNs = 0L
        w.endNs = durationMs * 1_000_000L
        w.qualityRating = QualityRating.GOOD
        repeat(30) {
            w.frames.add(SensorFrame(it.toLong() * 20_000_000L, SensorChannel.ACCELEROMETER,
                floatArrayOf(magnitude, 0f, 0f)))
        }
        return w
    }

    private val config = RecordingConfig(requiredSamples = 5, minSamples = 3)

    @Test
    fun identicalTraces_stdNearZero() {
        val windows = (0..4).map { makeWindow(it, 12f) }
        val env = GestureEnvelopeBuilder.build(windows, config)
        assertTrue("std should be near zero", env.magnitudeStd.all { it < 0.1f })
        assertTrue("confidence should be high: ${env.confidence}", env.confidence > 0.8f)
    }

    @Test
    fun diverseTraces_nonZeroStd() {
        val windows = listOf(
            makeWindow(0, 10f),
            makeWindow(1, 14f),
            makeWindow(2, 12f),
        )
        val env = GestureEnvelopeBuilder.build(windows, config.copy(minSamples = 2))
        assertEquals(3, env.sampleCount)
        assertTrue("some slices should have non-zero std", env.magnitudeStd.any { it > 0.1f })
    }

    @Test
    fun lowQualityWindowFiltered_whenEnoughRemain() {
        val windows = listOf(
            makeWindow(0, 10f).also { it.qualityRating = QualityRating.LOW_QUALITY },
            makeWindow(1, 12f),
            makeWindow(2, 12f),
            makeWindow(3, 12f),
        )
        val env = GestureEnvelopeBuilder.build(windows, config)
        assertEquals(3, env.sampleCount)  // LOW_QUALITY window filtered
    }

    @Test
    fun sliceCount_outOfRange_throws() {
        val w = makeWindow(0, 12f)
        assertThrows(IllegalArgumentException::class.java) {
            GestureEnvelopeBuilder.build(listOf(w), config, sliceCount = 5)
        }
    }
}
