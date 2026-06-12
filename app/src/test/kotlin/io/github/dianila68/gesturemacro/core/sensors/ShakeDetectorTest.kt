package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {

    @Test
    fun `three peaks inside window emit exactly one shake`() {
        val events = TraceReplay.run(ShakeDetector(), TraceReplay.load("/traces/shake_positive.json"))
        assertEquals(1, events.size)
        assertEquals(GesturePattern.SHAKE, events[0].pattern)
        assertEquals(600L, events[0].t)
        assertTrue(events[0].confidence > 0.5f)
    }

    @Test
    fun `two peaks are a near miss`() {
        val events = TraceReplay.run(ShakeDetector(), TraceReplay.load("/traces/shake_near_miss.json"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `noisy walk produces no false positive`() {
        val events = TraceReplay.run(ShakeDetector(), TraceReplay.load("/traces/shake_noise.json"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `detector resets after firing and can fire again`() {
        val detector = ShakeDetector()
        val trace = TraceReplay.load("/traces/shake_positive.json")
        val first = TraceReplay.run(detector, trace)
        val second = TraceReplay.run(detector, trace.map { SensorSample(it.sensor, it.t + 5_000, it.v) })
        assertEquals(1, first.size)
        assertEquals(1, second.size)
    }

    @Test
    fun `non accelerometer samples are ignored`() {
        val detector = ShakeDetector()
        val event = detector.feed(SensorSample(SensorType.PROXIMITY, 0L, floatArrayOf(0f, 0f, 30f)))
        assertEquals(null, event)
    }
}
