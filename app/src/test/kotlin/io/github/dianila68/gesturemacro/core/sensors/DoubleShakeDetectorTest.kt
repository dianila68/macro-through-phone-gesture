package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleShakeDetectorTest {

    @Test
    fun `two shakes within the window emit exactly one double shake`() {
        val events = TraceReplay.run(
            DoubleShakeDetector(),
            TraceReplay.load("/traces/double_shake_positive.json"),
        )
        assertEquals(1, events.size)
        assertEquals(GesturePattern.DOUBLE_SHAKE, events[0].pattern)
        // Second burst completes on the peak at t=1300.
        assertEquals(1300L, events[0].t)
    }

    @Test
    fun `a single shake is not a double shake`() {
        val events = TraceReplay.run(
            DoubleShakeDetector(),
            TraceReplay.load("/traces/shake_positive.json"),
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a second shake beyond the window does not pair`() {
        val detector = DoubleShakeDetector()
        val first = TraceReplay.load("/traces/shake_positive.json")
        // Shift the second shake far past DOUBLE_WINDOW_MS so it starts a fresh attempt.
        val farLater = first.map { SensorSample(it.sensor, it.t + 5_000, it.v) }
        val events = TraceReplay.run(detector, first + farLater)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `detector resets after firing and can fire again`() {
        val detector = DoubleShakeDetector()
        val trace = TraceReplay.load("/traces/double_shake_positive.json")
        val first = TraceReplay.run(detector, trace)
        val second = TraceReplay.run(detector, trace.map { SensorSample(it.sensor, it.t + 10_000, it.v) })
        assertEquals(1, first.size)
        assertEquals(1, second.size)
    }
}
