package io.github.dianila68.gesturemacro.core.recording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pure-Kotlin lifecycle controller for a single gesture-recording session (ticket-045).
 *
 * State machine:
 *   IDLE → start() → COUNTDOWN(remaining) → CAPTURING → stop()/timeout → REVIEWING → confirm() → DONE
 *   Any state → reset() → IDLE
 *
 * The session does not capture samples itself; that is delegated to [SampleBuffer] (ticket-046)
 * which is fed by the platform sensor layer. The session only manages *when* to capture.
 */
class GestureRecordingSession(
    private val scope: CoroutineScope,
    val countdownMs: Long = DEFAULT_COUNTDOWN_MS,
    val maxCaptureDurationMs: Long = MAX_CAPTURE_DURATION_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var captureJob: Job? = null

    fun start() {
        val current = _state.value
        if (current != RecordingState.Idle) return
        _state.value = RecordingState.Countdown(countdownMs)
        countdownJob = scope.launch {
            val tickMs = 100L
            var remaining = countdownMs
            while (remaining > 0) {
                delay(tickMs)
                remaining -= tickMs
                if (_state.value is RecordingState.Countdown) {
                    _state.value = RecordingState.Countdown(maxOf(0L, remaining))
                }
            }
            if (_state.value is RecordingState.Countdown) {
                beginCapture()
            }
        }
    }

    fun stop() {
        if (_state.value !is RecordingState.Capturing) return
        captureJob?.cancel()
        captureJob = null
        _state.value = RecordingState.Reviewing
    }

    fun confirm() {
        if (_state.value != RecordingState.Reviewing) return
        _state.value = RecordingState.Done
    }

    fun reset() {
        countdownJob?.cancel()
        captureJob?.cancel()
        countdownJob = null
        captureJob = null
        _state.value = RecordingState.Idle
    }

    private fun beginCapture() {
        _state.value = RecordingState.Capturing(startedAt = clock())
        captureJob = scope.launch {
            delay(maxCaptureDurationMs)
            if (_state.value is RecordingState.Capturing) stop()
        }
    }

    companion object {
        const val DEFAULT_COUNTDOWN_MS = 3_000L
        const val MAX_CAPTURE_DURATION_MS = 5_000L
    }
}

sealed class RecordingState {
    object Idle : RecordingState()
    data class Countdown(val remainingMs: Long) : RecordingState()
    data class Capturing(val startedAt: Long) : RecordingState()
    object Reviewing : RecordingState()
    object Done : RecordingState()
}
