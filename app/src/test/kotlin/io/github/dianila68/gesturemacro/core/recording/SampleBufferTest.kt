package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleBufferTest {

    private fun accel(t: Long) = SensorSample(SensorType.ACCELEROMETER, t, floatArrayOf(0f, 0f, 9.81f))

    @Test fun `empty buffer reports zero size and isEmpty`() {
        val buf = SampleBuffer(10)
        assertEquals(0, buf.size())
        assertTrue(buf.isEmpty())
        assertEquals(emptyList<SensorSample>(), buf.snapshot())
    }

    @Test fun `add then snapshot returns sample in order`() {
        val buf = SampleBuffer(5)
        val s1 = accel(100); val s2 = accel(200); val s3 = accel(300)
        buf.add(s1); buf.add(s2); buf.add(s3)
        val snap = buf.snapshot()
        assertEquals(3, snap.size)
        assertEquals(100L, snap[0].t)
        assertEquals(200L, snap[1].t)
        assertEquals(300L, snap[2].t)
    }

    @Test fun `overflow evicts oldest sample`() {
        val buf = SampleBuffer(3)
        buf.add(accel(1)); buf.add(accel(2)); buf.add(accel(3))
        buf.add(accel(4)) // evicts t=1
        val snap = buf.snapshot()
        assertEquals(3, snap.size)
        assertEquals(2L, snap[0].t)
        assertEquals(4L, snap[2].t)
    }

    @Test fun `clear empties buffer`() {
        val buf = SampleBuffer(5)
        buf.add(accel(1)); buf.add(accel(2))
        buf.clear()
        assertEquals(0, buf.size())
        assertTrue(buf.isEmpty())
    }

    @Test fun `size tracks count up to capacity`() {
        val buf = SampleBuffer(4)
        assertEquals(0, buf.size())
        repeat(4) { buf.add(accel(it.toLong())) }
        assertEquals(4, buf.size())
        buf.add(accel(99)) // overflow — still 4
        assertEquals(4, buf.size())
    }

    @Test fun `snapshotWindow returns only samples in range`() {
        val buf = SampleBuffer(10)
        for (t in listOf(100L, 200L, 300L, 400L, 500L)) buf.add(accel(t))
        val window = buf.snapshotWindow(200L, 400L)
        assertEquals(2, window.size)
        assertEquals(200L, window[0].t)
        assertEquals(300L, window[1].t)
    }

    @Test fun `snapshotWindow with empty range returns empty`() {
        val buf = SampleBuffer(10)
        buf.add(accel(500)); buf.add(accel(600))
        assertTrue(buf.snapshotWindow(100L, 200L).isEmpty())
    }

    @Test fun `snapshot is immutable copy (add after does not affect it)`() {
        val buf = SampleBuffer(10)
        buf.add(accel(1))
        val snap = buf.snapshot()
        buf.add(accel(2))
        assertEquals(1, snap.size) // snapshot was captured before second add
    }

    @Test fun `DEFAULT_CAPACITY is 500`() {
        assertEquals(500, SampleBuffer.DEFAULT_CAPACITY)
    }

    @Test fun `buffer at default capacity holds 500 samples`() {
        val buf = SampleBuffer()
        repeat(501) { buf.add(accel(it.toLong())) }
        assertEquals(500, buf.size())
        assertEquals(1L, buf.snapshot().first().t) // t=0 was evicted
    }

    @Test fun `add then clear then add starts fresh`() {
        val buf = SampleBuffer(5)
        repeat(5) { buf.add(accel(it.toLong())) }
        buf.clear()
        buf.add(accel(99))
        assertEquals(1, buf.size())
        assertEquals(99L, buf.snapshot()[0].t)
    }
}
