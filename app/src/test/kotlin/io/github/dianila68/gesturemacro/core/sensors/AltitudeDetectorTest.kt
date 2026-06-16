package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AltitudeDetectorTest {

    private fun pressure(t: Long, hpa: Float) =
        SensorSample(SensorType.PRESSURE, t, floatArrayOf(hpa))

    /** Feed 30s of baseline then one changed sample. */
    private fun feedBaseline(detector: GestureDetector, baselineHpa: Float, count: Int = 61) {
        repeat(count) { i ->
            detector.onSample(pressure((i * 500).toLong(), baselineHpa))
        }
    }

    @Test
    fun `altitude rise fires when pressure drops`() {
        val d = AltitudeRiseDetector(sensitivity = 0f)
        feedBaseline(d, 1013.25f) // sea-level baseline
        // After baseline established, send readings with clearly lower pressure (rising)
        val evt = d.onSample(pressure(35_000, 1010f)) // ~3 hPa drop, threshold at sens=0 is 0.5
        assertNotNull("Should fire on significant pressure drop", evt)
    }

    @Test
    fun `altitude fall fires when pressure rises`() {
        val d = AltitudeFallDetector(sensitivity = 0f)
        feedBaseline(d, 1010f)
        val evt = d.onSample(pressure(35_000, 1013f))
        assertNotNull("Should fire on significant pressure rise", evt)
    }

    @Test
    fun `no event during baseline collection period`() {
        val d = AltitudeRiseDetector(sensitivity = 0f)
        // Only 10s of baseline — not enough
        repeat(20) { i ->
            assertNull(d.onSample(pressure((i * 500).toLong(), 1013.25f)))
        }
    }

    @Test
    fun `stable pressure produces no event`() {
        val d = AltitudeRiseDetector(sensitivity = 0f)
        feedBaseline(d, 1013.25f)
        // Pressure unchanged — no event
        assertNull(d.onSample(pressure(35_000, 1013.2f)))
    }

    @Test
    fun `non-pressure samples are ignored`() {
        val d = AltitudeRiseDetector()
        val accel = SensorSample(SensorType.ACCELEROMETER, 0, floatArrayOf(0f, 0f, 9.81f))
        assertNull(d.onSample(accel))
    }
}
