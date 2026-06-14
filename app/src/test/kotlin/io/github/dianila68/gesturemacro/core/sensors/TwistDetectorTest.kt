package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwistDetectorTest {

    private fun gyro(resource: String) = TraceReplay.load(resource, SensorType.GYROSCOPE)

    @Test
    fun `a reversing twist emits exactly one event`() {
        val events = TraceReplay.run(TwistDetector(), gyro("/traces/twist_positive.json"))
        assertEquals(1, events.size)
        assertEquals(GesturePattern.TWIST, events[0].pattern)
        // The opposite-direction rotation lands at t=360.
        assertEquals(360L, events[0].t)
        assertTrue(events[0].confidence > 0f)
    }

    @Test
    fun `a one-way rotation never fires`() {
        val events = TraceReplay.run(TwistDetector(), gyro("/traces/twist_one_way.json"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `accelerometer samples are ignored`() {
        val detector = TwistDetector()
        val event = detector.feed(SensorSample(SensorType.ACCELEROMETER, 0L, floatArrayOf(0f, 9f, 0f)))
        assertEquals(null, event)
    }

    @Test
    fun `detector resets after firing and can fire again`() {
        val detector = TwistDetector()
        val trace = gyro("/traces/twist_positive.json")
        val first = TraceReplay.run(detector, trace)
        val second = TraceReplay.run(detector, trace.map { SensorSample(it.sensor, it.t + 5_000, it.v) })
        assertEquals(1, first.size)
        assertEquals(1, second.size)
    }
}
