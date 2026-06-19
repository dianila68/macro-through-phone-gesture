package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import kotlin.math.sqrt
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordedGestureDetectorTest {

    /** Build a simple envelope: magnitudes ramp up then down over 30 slices, mean ~6, std ~0.5. */
    private fun buildTestEnvelope(
        meanPeak: Float = 6f,
        stdWidth: Float = 0.5f,
    ): GestureEnvelope {
        val slices = 30
        val mean = FloatArray(slices) { i ->
            val t = i.toFloat() / (slices - 1)
            meanPeak * 4f * t * (1f - t) // bell
        }
        val std = FloatArray(slices) { stdWidth }
        return GestureEnvelope(
            sliceCount = slices,
            magnitudeMean = mean,
            magnitudeStd = std,
            durationMeanMs = 800f,
            durationStdMs = 100f,
            sampleCount = 5,
            confidence = 0.85f,
        )
    }

    private fun feedBellTrace(
        detector: RecordedGestureDetector,
        peakMag: Float = 6f,
        durationMs: Long = 800L,
        frameCount: Int = 40,
    ) {
        var t = 0L
        val step = durationMs / frameCount
        repeat(frameCount) { i ->
            val rise = i.toFloat() / frameCount
            val mag = peakMag * 4f * rise * (1f - rise)
            val norm = sqrt(1f / 3f) // distribute evenly across 3 axes
            detector.feed(
                SensorSample(
                    sensor = SensorType.ACCELEROMETER,
                    t = t,
                    v = floatArrayOf(mag * norm, mag * norm, mag * norm),
                ),
            )
            t += step
        }
    }

    @Test
    fun `replay of average trace matches`() {
        val envelope = buildTestEnvelope()
        val detector = RecordedGestureDetector("test", envelope, sensitivity = 0.5f)
        var result = detector.feed(
            SensorSample(SensorType.ACCELEROMETER, 0L, floatArrayOf(0f, 0f, 9.8f)),
        )
        // Feed a bell-shaped trace matching the envelope
        feedBellTrace(detector)
        // Check that some result was emitted during the feed
        var lastResult = result
        val frameCount = 40
        val durationMs = 800L
        val step = durationMs / frameCount
        var t = 1L
        repeat(frameCount) { i ->
            val rise = i.toFloat() / frameCount
            val mag = 6f * 4f * rise * (1f - rise)
            val norm = sqrt(1f / 3f)
            val r = detector.feed(
                SensorSample(
                    sensor = SensorType.ACCELEROMETER,
                    t = t,
                    v = floatArrayOf(mag * norm, mag * norm, mag * norm),
                ),
            )
            if (r != null) lastResult = r
            t += step
        }
        // With sensitivity 0.5 (medium), the bell trace should fire
        assertNotNull(lastResult, "Expected a match event but got none")
        assertTrue(lastResult!!.confidence >= 0.5f, "Confidence should be >= 0.5")
    }

    @Test
    fun `partial trace under min duration does not fire`() {
        val envelope = buildTestEnvelope()
        val detector = RecordedGestureDetector("test", envelope, sensitivity = 0.5f)
        // Feed only 100ms (much less than minDataMs ~700ms)
        var result: Any? = null
        repeat(5) { i ->
            result = detector.feed(
                SensorSample(SensorType.ACCELEROMETER, i * 20L, floatArrayOf(3f, 0f, 0f)),
            )
        }
        assertNull(result, "Should not fire on insufficient data")
    }

    @Test
    fun `wrong sensor type is ignored`() {
        val envelope = buildTestEnvelope()
        val detector = RecordedGestureDetector("test", envelope, sensitivity = 0.5f)
        val result = detector.feed(
            SensorSample(SensorType.GYROSCOPE, 100L, floatArrayOf(5f, 5f, 5f)),
        )
        assertNull(result, "Non-accelerometer samples should be ignored")
    }

    @Test
    fun `reset clears buffer`() {
        val envelope = buildTestEnvelope()
        val detector = RecordedGestureDetector("test", envelope, sensitivity = 0.5f)
        feedBellTrace(detector)
        detector.reset()
        // After reset, a single frame should not fire (buffer cleared)
        val result = detector.feed(
            SensorSample(SensorType.ACCELEROMETER, 10_000L, floatArrayOf(6f, 0f, 0f)),
        )
        assertNull(result, "After reset, single frame should not match")
    }

    @Test
    fun `sensitivity multiplier covers expected range`() {
        val low = RecordedGestureDetector.sensitivityToMultiplier(0f)
        val high = RecordedGestureDetector.sensitivityToMultiplier(1f)
        assertTrue(low < high, "higher sensitivity should give larger multiplier")
        assertTrue(low >= 1.4f && low <= 1.6f, "low k should be ~1.5")
        assertTrue(high >= 2.9f && high <= 3.1f, "high k should be ~3.0")
    }
}
