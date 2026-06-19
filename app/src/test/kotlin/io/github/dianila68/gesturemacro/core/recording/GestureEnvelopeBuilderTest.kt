package io.github.dianila68.gesturemacro.core.recording

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureEnvelopeBuilderTest {

    private fun makeWindow(index: Int, durationMs: Long = 800L, peakMag: Float = 8f): SampleWindow {
        val window = SampleWindow(index)
        window.startNs = 0L
        window.endNs = durationMs * 1_000_000L
        val frameCount = 40
        repeat(frameCount) { i ->
            val t = i * (durationMs / frameCount)
            val rise = i.toFloat() / frameCount
            val mag = peakMag * rise * (1f - rise) * 4f // bell curve
            window.frames.add(
                SensorFrame(
                    timestampNs = t * 1_000_000L,
                    channel = RecordingChannel.ACCELEROMETER,
                    values = floatArrayOf(mag, 0f, 0f),
                ),
            )
        }
        return window
    }

    @Test
    fun `identical traces produce near-zero std`() {
        val windows = List(5) { makeWindow(it) }
        val config = RecordingConfig(requiredSamples = 5, minSamples = 3)
        val envelope = GestureEnvelopeBuilder.build(windows, config)
        assertNotNull(envelope)
        assertTrue(envelope.magnitudeStd.all { abs(it) < 0.5f }, "std should be near zero for identical traces")
        assertEquals(5, envelope.sampleCount)
    }

    @Test
    fun `diverse traces produce non-zero std`() {
        val windows = listOf(
            makeWindow(0, 500L, peakMag = 5f),
            makeWindow(1, 700L, peakMag = 9f),
            makeWindow(2, 900L, peakMag = 12f),
        )
        val config = RecordingConfig(requiredSamples = 3, minSamples = 2)
        val envelope = GestureEnvelopeBuilder.build(windows, config)
        assertNotNull(envelope)
        assertTrue(envelope.magnitudeStd.any { it > 0.1f }, "std should be non-zero for diverse traces")
        assertEquals(3, envelope.sampleCount)
    }

    @Test
    fun `one low-quality window dropped when enough remain`() {
        val goodWindows = List(3) { makeWindow(it, 700L, peakMag = 8f) }
        // Add a near-zero motion window (low quality)
        val badWindow = SampleWindow(3).apply {
            startNs = 0L
            endNs = 200_000_000L
            frames.add(SensorFrame(0L, RecordingChannel.ACCELEROMETER, floatArrayOf(0.1f, 0f, 0f)))
        }
        val windows = goodWindows + badWindow
        val config = RecordingConfig(requiredSamples = 4, minSamples = 2)
        val envelope = GestureEnvelopeBuilder.build(windows, config)
        assertNotNull(envelope)
        // Bad window should be dropped; good windows kept
        assertTrue(envelope.sampleCount <= 3)
    }

    @Test
    fun `build returns null for empty windows`() {
        val envelope = GestureEnvelopeBuilder.build(emptyList(), RecordingConfig())
        assertNull(envelope)
    }

    @Test
    fun `time normalise resamples correctly`() {
        val src = listOf(0f, 1f, 2f, 3f, 4f)
        val result = GestureEnvelopeBuilder.timeNormalise(src, 3)
        assertEquals(3, result.size)
        assertEquals(0f, result[0], 0.01f)
        assertEquals(2f, result[1], 0.01f)
        assertEquals(4f, result[2], 0.01f)
    }

    @Test
    fun `slice count boundary values are accepted`() {
        val windows = List(3) { makeWindow(it) }
        val config = RecordingConfig(requiredSamples = 3, minSamples = 2)
        assertNotNull(GestureEnvelopeBuilder.build(windows, config, sliceCount = 10))
        assertNotNull(GestureEnvelopeBuilder.build(windows, config, sliceCount = 60))
    }
}
