package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ThresholdCrossingDetectorTest {

    private fun makeSample(v: Float, t: Long) =
        SensorSample(SensorType.AMBIENT_TEMPERATURE, t, floatArrayOf(v))

    @Test
    fun firesHighAfterDebounceSamples() {
        val det = ThresholdCrossingDetector(
            sensor = SensorType.AMBIENT_TEMPERATURE,
            patternHigh = GesturePattern.TEMPERATURE_HIGH,
            patternLow = GesturePattern.TEMPERATURE_LOW,
            thresholdLow = 18f,
            thresholdHigh = 26f,
            debounceSamples = 3,
        )
        assertNull(det.feed(makeSample(27f, 1)))
        assertNull(det.feed(makeSample(27f, 2)))
        val event = det.feed(makeSample(27f, 3))
        assertNotNull(event)
        assertEquals(GesturePattern.TEMPERATURE_HIGH, event!!.pattern)
    }

    @Test
    fun doesNotRefireUntilLowCrossing() {
        val det = ThresholdCrossingDetector(
            sensor = SensorType.AMBIENT_TEMPERATURE,
            patternHigh = GesturePattern.TEMPERATURE_HIGH,
            patternLow = GesturePattern.TEMPERATURE_LOW,
            thresholdLow = 18f,
            thresholdHigh = 26f,
            debounceSamples = 1,
        )
        assertNotNull(det.feed(makeSample(27f, 1)))
        assertNull(det.feed(makeSample(27f, 2)))  // already above, no re-fire
    }
}
