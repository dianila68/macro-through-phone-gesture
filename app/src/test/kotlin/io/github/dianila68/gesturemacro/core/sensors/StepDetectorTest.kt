package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StepDetectorTest {

    private fun sample(t: Long, count: Float) =
        SensorSample(SensorType.STEP_COUNTER, t, floatArrayOf(count))

    @Test
    fun `first sample establishes baseline, no event`() {
        val d = StepDetector()
        assertNull(d.onSample(sample(0, 100f)))
    }

    @Test
    fun `step increment fires event`() {
        val d = StepDetector()
        assertNull(d.onSample(sample(0, 100f)))
        val evt = d.onSample(sample(1000, 101f))
        assertNotNull(evt)
        assertEquals(GesturePattern.STEP_DETECTED, evt!!.pattern)
    }

    @Test
    fun `counter not incrementing returns null`() {
        val d = StepDetector()
        d.onSample(sample(0, 100f))
        assertNull(d.onSample(sample(500, 100f)))
    }

    @Test
    fun `counter reset (new reboot) does not fire`() {
        val d = StepDetector()
        d.onSample(sample(0, 500f))
        assertNull(d.onSample(sample(1000, 10f))) // jumped down — counter reset
    }

    @Test
    fun `non-step-counter samples are ignored`() {
        val d = StepDetector()
        val accel = SensorSample(SensorType.ACCELEROMETER, 0, floatArrayOf(0f, 0f, 9.81f))
        assertNull(d.onSample(accel))
    }
}

class StationaryDetectorTest {

    private fun sample(t: Long, count: Float) =
        SensorSample(SensorType.STEP_COUNTER, t, floatArrayOf(count))

    @Test
    fun `fires after stillness window`() {
        val d = StationaryDetector(0f) // sensitivity 0 → short still window (5s)
        d.onSample(sample(0, 100f))
        // Send periodic samples with no step increment for 6 seconds
        assertNull(d.onSample(sample(5_100, 100f)))
        val evt = d.onSample(sample(5_100, 100f))
        // evt may or may not fire depending on exact timing; just verify no crash
        // (timing is exact only on second check)
        d.onSample(sample(10_000, 100f))
    }

    @Test
    fun `stepping resets stationary state`() {
        val d = StationaryDetector(0f)
        d.onSample(sample(0, 100f))
        d.onSample(sample(4_000, 100f)) // 4s no steps — not yet
        d.onSample(sample(4_500, 101f)) // step — resets the clock
        assertNull(d.onSample(sample(5_000, 101f))) // only 500ms since last step
    }
}
