package io.github.dianila68.gesturemacro.core.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleBufferTest {

    @Test
    fun openCloseWindow_tracksIndex() {
        val buf = SampleBuffer()
        buf.openWindow(0)
        buf.appendFrame(SensorFrame(1000L, SensorChannel.ACCELEROMETER, floatArrayOf(1f, 0f, 0f)))
        buf.closeWindow()
        assertEquals(1, buf.windows.size)
        assertEquals(1, buf.windows[0].frames.size)
    }

    @Test
    fun clear_removesAllWindows() {
        val buf = SampleBuffer()
        buf.openWindow(0)
        buf.closeWindow()
        buf.clear()
        assertTrue(buf.windows.isEmpty())
    }
}
