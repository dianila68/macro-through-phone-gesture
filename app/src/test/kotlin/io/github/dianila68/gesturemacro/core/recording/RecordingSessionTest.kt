package io.github.dianila68.gesturemacro.core.recording

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingSessionTest {

    @Test
    fun `initial state is Idle`() = runTest {
        val session = GestureRecordingSession(this)
        assertEquals(RecordingState.Idle, session.state.value)
    }

    @Test
    fun `start transitions to Countdown`() = runTest {
        val session = GestureRecordingSession(this)
        session.start()
        assertTrue(session.state.value is RecordingState.Countdown)
    }

    @Test
    fun `countdown transitions to Capturing after delay`() = runTest {
        val session = GestureRecordingSession(
            this,
            countdownMs = 1_000L,
        )
        session.start()
        advanceTimeBy(1_100L)
        assertTrue(session.state.value is RecordingState.Capturing)
    }

    @Test
    fun `stop moves to Reviewing`() = runTest {
        val session = GestureRecordingSession(this, countdownMs = 100L)
        session.start()
        advanceTimeBy(200L)
        session.stop()
        assertEquals(RecordingState.Reviewing, session.state.value)
    }

    @Test
    fun `confirm moves to Done`() = runTest {
        val session = GestureRecordingSession(this, countdownMs = 100L)
        session.start()
        advanceTimeBy(200L)
        session.stop()
        session.confirm()
        assertEquals(RecordingState.Done, session.state.value)
    }

    @Test
    fun `reset returns to Idle from any state`() = runTest {
        val session = GestureRecordingSession(this, countdownMs = 100L)
        session.start()
        advanceTimeBy(200L)
        session.reset()
        assertEquals(RecordingState.Idle, session.state.value)
    }

    @Test
    fun `capture auto-stops after maxCaptureDurationMs`() = runTest {
        val session = GestureRecordingSession(
            this,
            countdownMs = 100L,
            maxCaptureDurationMs = 500L,
        )
        session.start()
        advanceTimeBy(700L)
        assertEquals(RecordingState.Reviewing, session.state.value)
    }
}
