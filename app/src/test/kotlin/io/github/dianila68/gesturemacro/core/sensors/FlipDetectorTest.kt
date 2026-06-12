package io.github.dianila68.gesturemacro.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipDetectorTest {

    @Test
    fun `stable face-up to face-down emits one flip down`() {
        val detector = FlipDetector(GesturePattern.FLIP_FACE_DOWN)
        val events = TraceReplay.run(detector, TraceReplay.load("/traces/flip_down_positive.json"))
        assertEquals(1, events.size)
        assertEquals(GesturePattern.FLIP_FACE_DOWN, events[0].pattern)
        assertEquals(1500L, events[0].t)
    }

    @Test
    fun `stable face-down to face-up emits one flip up`() {
        val detector = FlipDetector(GesturePattern.FLIP_FACE_UP)
        val events = TraceReplay.run(detector, TraceReplay.load("/traces/flip_up_positive.json"))
        assertEquals(1, events.size)
        assertEquals(GesturePattern.FLIP_FACE_UP, events[0].pattern)
    }

    @Test
    fun `brief face-down dip does not fire`() {
        val detector = FlipDetector(GesturePattern.FLIP_FACE_DOWN)
        val events = TraceReplay.run(detector, TraceReplay.load("/traces/flip_too_brief.json"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `flip down trace does not trigger the flip up detector`() {
        val detector = FlipDetector(GesturePattern.FLIP_FACE_UP)
        val events = TraceReplay.run(detector, TraceReplay.load("/traces/flip_down_positive.json"))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `non flip patterns are rejected at construction`() {
        assertThrows(IllegalArgumentException::class.java) {
            FlipDetector(GesturePattern.SHAKE)
        }
    }
}
