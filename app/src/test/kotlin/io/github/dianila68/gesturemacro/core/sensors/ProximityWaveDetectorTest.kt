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
        // Near (0) then far (8), with maximumRange=10 so nearThreshold=5; 8 >= 5 → far.
        val detector = ProximityWaveDetector(maximumRange = 10f)
        assertEquals(null, detector.feed(SensorSample(SensorType.PROXIMITY, 100L, floatArrayOf(0f))))
        val event = detector.feed(SensorSample(SensorType.PROXIMITY, 300L, floatArrayOf(8f)))
        assertEquals(GesturePattern.PROXIMITY_WAVE, event?.pattern)
    }

    @Test
    fun `accelerometer samples are ignored`() {
        val detector = ProximityWaveDetector()
        assertEquals(null, detector.feed(SensorSample(SensorType.ACCELEROMETER, 0L, floatArrayOf(0f, 9f, 0f))))
    }

    @Test
    fun `binary sensor reporting far as maximumRange 5 does not latch near`() {
        // Bug regression: with maximumRange=5 the far reading IS 5; nearThreshold = 5*0.5 = 2.5.
        // A far sample of 5.0 must NOT be classified as near (5.0 >= 2.5 → far).
        val detector = ProximityWaveDetector(maximumRange = 5f)
        // Far → near → far should fire once.
        assertEquals(null, detector.feed(SensorSample(SensorType.PROXIMITY, 0L, floatArrayOf(5f))))   // far
        assertEquals(null, detector.feed(SensorSample(SensorType.PROXIMITY, 100L, floatArrayOf(0f)))) // near
        val event = detector.feed(SensorSample(SensorType.PROXIMITY, 300L, floatArrayOf(5f)))          // far
        assertEquals(GesturePattern.PROXIMITY_WAVE, event?.pattern)
    }

    @Test
    fun `far-only trace with maximumRange 5 fires nothing`() {
        // All samples report the sensor's max range (far); no wave should ever fire.
        val detector = ProximityWaveDetector(maximumRange = 5f)
        val events = (0..10).mapNotNull { i ->
            detector.feed(SensorSample(SensorType.PROXIMITY, (i * 20).toLong(), floatArrayOf(5f)))
        }
        assertTrue(events.isEmpty())
    }
}
