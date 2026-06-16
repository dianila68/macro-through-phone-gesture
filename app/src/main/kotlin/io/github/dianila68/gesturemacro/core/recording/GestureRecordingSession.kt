package io.github.dianila68.gesturemacro.core.recording

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ticket-045/047: Orchestrates the multi-repetition gesture recording lifecycle.
 * Pure JVM — no android.* imports. Sensor wiring lives in the Android layer.
 */
interface GestureRecordingSession {
    val state: StateFlow<RecordingState>
    val coverageUpdates: SharedFlow<CoverageUpdate>
    val buffer: SampleBuffer
    fun start(config: RecordingConfig, scope: CoroutineScope)
    fun appendFrame(frame: SensorFrame)
    fun cancel()
}

class DefaultGestureRecordingSession : GestureRecordingSession {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _coverageUpdates = MutableSharedFlow<CoverageUpdate>(extraBufferCapacity = 16)
    override val coverageUpdates: SharedFlow<CoverageUpdate> = _coverageUpdates.asSharedFlow()

    override val buffer = SampleBuffer()

    private var sessionJob: Job? = null
    private val coverageTracker = CoverageTracker()
    private lateinit var config: RecordingConfig

    override fun start(config: RecordingConfig, scope: CoroutineScope) {
        this.config = config
        buffer.clear()
        sessionJob?.cancel()
        sessionJob = scope.launch {
            runSession()
        }
    }

    override fun appendFrame(frame: SensorFrame) {
        if (_state.value is RecordingState.Recording) {
            buffer.appendFrame(frame)
        }
    }

    override fun cancel() {
        sessionJob?.cancel()
        _state.value = RecordingState.Cancelled
    }

    private suspend fun CoroutineScope.runSession() {
        val countdownEnd = System.currentTimeMillis() + config.countdownMs
        while (isActive && System.currentTimeMillis() < countdownEnd) {
            _state.value = RecordingState.Countdown(countdownEnd - System.currentTimeMillis())
            delay(50)
        }
        if (!isActive) return

        var sampleIndex = 0
        while (isActive && sampleIndex < config.requiredSamples) {
            _state.value = RecordingState.Recording(sampleIndex, 0L)
            buffer.openWindow(sampleIndex)
            val windowStart = System.currentTimeMillis()
            val windowEnd = windowStart + config.maxWindowMs
            while (isActive && System.currentTimeMillis() < windowEnd) {
                _state.value = RecordingState.Recording(sampleIndex, System.currentTimeMillis() - windowStart)
                delay(50)
            }
            buffer.closeWindow()

            if (!isActive) return

            val window = buffer.windows.lastOrNull()
            if (window != null) {
                val quality = RepetitionQualityScorer.score(window, config.maxWindowMs)
                window.qualityScore = quality.score
                window.qualityRating = quality.rating
                val coverage = coverageTracker.compute(buffer.windows, config.requiredSamples)
                _coverageUpdates.emit(CoverageUpdate(sampleIndex, quality.score, coverage.coverageScore))

                if (coverage.isEarlyAbortRecommended) {
                    _state.value = RecordingState.InsufficientData
                    return
                }
            }

            sampleIndex++
            if (sampleIndex < config.requiredSamples) {
                val pauseEnd = System.currentTimeMillis() + config.interSamplePauseMs
                while (isActive && System.currentTimeMillis() < pauseEnd) {
                    _state.value = RecordingState.InterSamplePause(sampleIndex, pauseEnd - System.currentTimeMillis())
                    delay(50)
                }
            }
        }
        if (!isActive) return

        _state.value = RecordingState.Analysing
        val goodWindows = buffer.windows.filter { it.qualityRating != QualityRating.LOW_QUALITY }
        val usableWindows = if (goodWindows.size >= config.minSamples) goodWindows else buffer.windows
        if (usableWindows.size < config.minSamples) {
            _state.value = RecordingState.InsufficientData
            return
        }
        val envelope = GestureEnvelopeBuilder.build(usableWindows, config)
        _state.value = RecordingState.Ready(envelope)
    }
}
