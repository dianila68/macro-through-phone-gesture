package io.github.dianila68.gesturemacro.core.recording

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingSessionTest {

    private fun makeSession() = DefaultGestureRecordingSession()

    @Test
    fun `cancel from idle leaves state as Idle`() = runTest {
        val session = makeSession()
        session.cancel()
        assertTrue("Expected Idle state", session.state.value is RecordingState.Idle)
    }

    @Test
    fun `cancel during countdown transitions to Cancelled`() = runTest {
        val session = makeSession()
        val config = RecordingConfig(countdownMs = 3_000, requiredSamples = 3, minSamples = 1)
        session.start(config, this)
        advanceTimeBy(500)
        session.cancel()
        advanceTimeBy(100)
        assertTrue("Expected Cancelled state", session.state.value is RecordingState.Cancelled)
    }

    @Test
    fun `session moves past Idle after start`() = runTest {
        val session = makeSession()
        val config = RecordingConfig(
            requiredSamples = 1,
            minSamples = 1,
            countdownMs = 100,
            maxWindowMs = 200,
            interSamplePauseMs = 0,
        )
        session.start(config, this)
        advanceTimeBy(1_500)
        assertFalse("Should not still be Idle", session.state.value is RecordingState.Idle)
        assertFalse("Should not be Cancelled", session.state.value is RecordingState.Cancelled)
    }

    @Test
    fun `SampleBuffer accumulates frames correctly`() {
        val buf = SampleBuffer()
        buf.openWindow(0)
        val frame = SensorFrame(1_000_000L, RecordingChannel.ACCELEROMETER, floatArrayOf(1f, 2f, 3f))
        buf.appendFrame(frame)
        buf.closeWindow()

        val windows = buf.windows
        assert(windows.size == 1) { "Expected 1 window" }
        assert(windows[0].frames.size == 1) { "Expected 1 frame" }
        assert(windows[0].frames[0].values.contentEquals(floatArrayOf(1f, 2f, 3f)))
    }

    @Test
    fun `SampleBuffer clear removes all windows`() {
        val buf = SampleBuffer()
        buf.openWindow(0)
        buf.closeWindow()
        buf.clear()
        assert(buf.windows.isEmpty()) { "Buffer should be empty after clear" }
    }
}
