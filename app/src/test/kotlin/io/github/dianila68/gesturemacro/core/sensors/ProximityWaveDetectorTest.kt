package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityWaveDetectorTest {

    private fun proximity(resource: String) = TraceReplay.load(resource, SensorType.PROXIMITY)

    @Test
    fun `a brief cover then uncover emits one wave`() {
        val events = TraceReplay.run(ProximityWaveDetector(), proximity("/traces/proximity_wave_positive.json"))
        assertEquals(1, events.size)
        assertEquals(GesturePattern.PROXIMITY_WAVE, events[0].pattern)
        // Fires when the sensor goes far again, at t=320.
        assertEquals(320L, events[0].t)
    }

    @Test
    fun `a long cover (pocket) does not fire`() {
        val events = TraceReplay.run(ProximityWaveDetector(), proximity("/traces/proximity_wave_pocket.json"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `handles single-value proximity samples`() {
        val detector = ProximityWaveDetector()
        // Near then far, as a real one-element proximity reading would arrive.
        assertEquals(null, detector.feed(SensorSample(SensorType.PROXIMITY, 100L, floatArrayOf(0f))))
        val event = detector.feed(SensorSample(SensorType.PROXIMITY, 300L, floatArrayOf(8f)))
        assertEquals(GesturePattern.PROXIMITY_WAVE, event?.pattern)
    }

    @Test
    fun `accelerometer samples are ignored`() {
        val detector = ProximityWaveDetector()
        assertEquals(null, detector.feed(SensorSample(SensorType.ACCELEROMETER, 0L, floatArrayOf(0f, 9f, 0f))))
    }
}
