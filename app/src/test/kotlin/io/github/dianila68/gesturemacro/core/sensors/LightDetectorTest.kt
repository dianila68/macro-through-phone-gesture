package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LightDetectorTest {

    private fun lux(t: Long, value: Float) =
        SensorSample(SensorType.LIGHT, t, floatArrayOf(value))

    @Test
    fun `going dark fires when lux drops below threshold`() {
        val d = GoingDarkDetector(sensitivity = 0f) // dark threshold = 5 lux
        d.onSample(lux(0, 200f))
        d.onSample(lux(500, 2f))
        val evt = d.onSample(lux(2_600, 1f)) // 2.1s in the dark → fires
        assertNotNull(evt)
    }

    @Test
    fun `going dark does not fire if light recovers quickly`() {
        val d = GoingDarkDetector(sensitivity = 0f)
        d.onSample(lux(0, 200f))
        d.onSample(lux(500, 2f))
        d.onSample(lux(800, 300f)) // light came back
        assertNull(d.onSample(lux(3_000, 2f))) // restarting; 3s dark but reset at 800
    }

    @Test
    fun `going bright fires when lux rises above threshold`() {
        val d = GoingBrightDetector(sensitivity = 0f) // bright threshold = 100 lux
        d.onSample(lux(0, 5f))
        d.onSample(lux(500, 200f))
        val evt = d.onSample(lux(2_600, 200f))
        assertNotNull(evt)
    }

    @Test
    fun `non-light samples are ignored`() {
        val d = GoingDarkDetector()
        val accel = SensorSample(SensorType.ACCELEROMETER, 0, floatArrayOf(0f, 0f, 9.81f))
        assertNull(d.onSample(accel))
    }
}
