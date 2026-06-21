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
import kotlinx.coroutines.launch
import kotlin.math.sqrt

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Countdown(val remainingMs: Long) : RecordingState
    data class Recording(val sampleIndex: Int, val elapsedMs: Long) : RecordingState
    data class InterSamplePause(val nextSampleIndex: Int, val remainingMs: Long) : RecordingState
    data object Analysing : RecordingState
    data class Ready(val envelope: GestureEnvelope) : RecordingState
    data object InsufficientData : RecordingState
    data object Cancelled : RecordingState
    data object TimedOut : RecordingState
}

data class CoverageUpdate(
    val windowIndex: Int,
    val qualityScore: Float,
    val coverageScore: Float,
)

interface GestureRecordingSession {
    val state: StateFlow<RecordingState>
    val coverageUpdates: SharedFlow<CoverageUpdate>
    val buffer: SampleBuffer

    fun start(config: RecordingConfig, scope: CoroutineScope)
    fun cancel()

    /** Called by the sensor collector when stillness is detected; triggers early window close. */
    fun signalStillness()
}

/** Pure-JVM implementation — no android.* imports. Sensor wiring injected externally. */
class DefaultGestureRecordingSession : GestureRecordingSession {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _coverageUpdates = MutableSharedFlow<CoverageUpdate>(extraBufferCapacity = COVERAGE_UPDATES_BUFFER)
    override val coverageUpdates: SharedFlow<CoverageUpdate> = _coverageUpdates.asSharedFlow()

    override val buffer = SampleBuffer()

    private var sessionJob: Job? = null
    private var stillnessSignalled = false
    private val scorer = RepetitionQualityScorer()
    private val coverageTracker = CoverageTracker()

    override fun start(config: RecordingConfig, scope: CoroutineScope) {
        sessionJob?.cancel()
        buffer.clear()
        stillnessSignalled = false

        sessionJob = scope.launch {
            // Countdown
            var remaining = config.countdownMs
            while (remaining > 0) {
                _state.value = RecordingState.Countdown(remaining)
                delay(COUNTDOWN_STEP_MS)
                remaining -= COUNTDOWN_STEP_MS
            }

            // Repetitions
            var collectedCount = 0
            for (i in 0 until config.requiredSamples) {
                if (_state.value == RecordingState.Cancelled) return@launch

                // Active recording window
                stillnessSignalled = false
                buffer.openWindow(i)
                val windowStart = System.currentTimeMillis()
                var elapsed = 0L

                while (elapsed < config.maxWindowMs && !stillnessSignalled) {
                    if (_state.value == RecordingState.Cancelled) {
                        buffer.closeWindow()
                        return@launch
                    }
                    _state.value = RecordingState.Recording(i, elapsed)
                    delay(RECORDING_POLL_MS)
                    elapsed = System.currentTimeMillis() - windowStart
                }
                buffer.closeWindow()
                collectedCount++

                // Score and emit coverage update
                val windows = buffer.windows
                val lastWindow = windows.lastOrNull()
                val quality = if (lastWindow != null) scorer.score(lastWindow, config) else 0f
                val report = coverageTracker.update(windows)
                _coverageUpdates.emit(CoverageUpdate(i, quality, report.coverageScore))

                if (report.isEarlyAbortRecommended) break

                // Inter-sample pause (skip after last)
                if (i < config.requiredSamples - 1) {
                    var pauseRemaining = config.interSamplePauseMs
                    while (pauseRemaining > 0) {
                        if (_state.value == RecordingState.Cancelled) return@launch
                        _state.value = RecordingState.InterSamplePause(i + 1, pauseRemaining)
                        delay(PAUSE_STEP_MS)
                        pauseRemaining -= PAUSE_STEP_MS
                    }
                }
            }

            if (collectedCount < config.minSamples) {
                _state.value = RecordingState.InsufficientData
                return@launch
            }

            // Build envelope
            _state.value = RecordingState.Analysing
            val envelope = GestureEnvelopeBuilder.build(buffer.windows, config)
            if (envelope != null) {
                _state.value = RecordingState.Ready(envelope)
            } else {
                _state.value = RecordingState.InsufficientData
            }
        }
    }

    override fun cancel() {
        val cancellable = _state.value !is RecordingState.Ready &&
            _state.value !is RecordingState.Cancelled &&
            _state.value !is RecordingState.InsufficientData &&
            _state.value !is RecordingState.TimedOut
        if (cancellable) {
            _state.value = RecordingState.Cancelled
            sessionJob?.cancel()
        }
    }

    override fun signalStillness() {
        stillnessSignalled = true
    }

    companion object {
        private const val COUNTDOWN_STEP_MS = 250L
        private const val RECORDING_POLL_MS = 50L
        private const val PAUSE_STEP_MS = 100L
        private const val COVERAGE_UPDATES_BUFFER = 16
    }
}

/** Scores a single SampleWindow for quality. Returns 0..1. */
class RepetitionQualityScorer {

    fun score(window: SampleWindow, config: RecordingConfig): Float {
        val accelFrames = window.frames.filter { it.channel == RecordingChannel.ACCELEROMETER }
        if (accelFrames.isEmpty()) return 0f

        val durationRatio = window.durationMs.toFloat() / config.maxWindowMs.toFloat()
        val durationScore = when {
            durationRatio < DURATION_TOO_SHORT -> 0f
            durationRatio > DURATION_TOO_LONG -> DURATION_LONG_PENALTY
            durationRatio in DURATION_IDEAL_MIN..DURATION_IDEAL_MAX -> 1f
            else -> DURATION_EDGE_SCORE
        }

        val magnitudes = accelFrames.map { f ->
            sqrt(f.values[0] * f.values[0] + f.values[1] * f.values[1] + f.values[2] * f.values[2])
        }
        val peakMag = magnitudes.maxOrNull() ?: 0f
        val peakScore = when {
            peakMag < PEAK_MAG_LOW -> 0f
            peakMag >= PEAK_MAG_HIGH -> 1f
            else -> (peakMag - PEAK_MAG_LOW) / PEAK_MAG_RANGE
        }

        val mean = magnitudes.average().toFloat()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
        val spreadScore = when {
            variance < VARIANCE_LOW -> 0f
            variance > VARIANCE_HIGH -> 1f
            else -> variance / VARIANCE_HIGH
        }

        return (durationScore + peakScore + spreadScore) / SCORE_PARTS
    }

    companion object {
        const val LOW_QUALITY_THRESHOLD = 0.4f
        private const val DURATION_TOO_SHORT = 0.1f
        private const val DURATION_TOO_LONG = 0.9f
        private const val DURATION_IDEAL_MIN = 0.2f
        private const val DURATION_IDEAL_MAX = 0.7f
        private const val DURATION_LONG_PENALTY = 0.3f
        private const val DURATION_EDGE_SCORE = 0.6f
        private const val PEAK_MAG_LOW = 1.5f
        private const val PEAK_MAG_HIGH = 4f
        private const val PEAK_MAG_RANGE = 2.5f
        private const val VARIANCE_LOW = 0.01f
        private const val VARIANCE_HIGH = 2f
        private const val SCORE_PARTS = 3f
    }
}

data class CoverageReport(
    val coverageScore: Float,
    val lowQualityCount: Int,
    val isEarlyAbortRecommended: Boolean,
)

/** Tracks how well the collected repetitions span the gesture's natural variation. */
class CoverageTracker {

    private val scorer = RepetitionQualityScorer()

    fun update(windows: List<SampleWindow>, config: RecordingConfig = RecordingConfig()): CoverageReport {
        val lowQuality = windows.count { scorer.score(it, config) < RepetitionQualityScorer.LOW_QUALITY_THRESHOLD }

        val coverageScore = if (windows.size < 2) {
            0f
        } else {
            val traces = windows.map { downsample(it, DOWNSAMPLE_POINTS) }
            val distances = mutableListOf<Float>()
            for (i in traces.indices) {
                for (j in i + 1 until traces.size) {
                    distances.add(euclideanDistance(traces[i], traces[j]))
                }
            }
            val mean = distances.average().toFloat()
            val std = if (distances.size > 1) {
                val variance = distances.map { (it - mean) * (it - mean) }.average().toFloat()
                kotlin.math.sqrt(variance)
            } else {
                0f
            }
            if (mean < MIN_COVERAGE_MEAN) 0f else (1f - (std / mean.coerceAtLeast(MIN_COVERAGE_MEAN))).coerceIn(0f, 1f)
        }

        val earlyAbort = lowQuality > (windows.size / 2) && windows.size >= 2

        return CoverageReport(coverageScore, lowQuality, earlyAbort)
    }

    private fun downsample(window: SampleWindow, points: Int): FloatArray {
        val accelFrames = window.frames.filter { it.channel == RecordingChannel.ACCELEROMETER }
        if (accelFrames.isEmpty()) return FloatArray(points)
        val magnitudes = accelFrames.map { f ->
            sqrt(f.values[0] * f.values[0] + f.values[1] * f.values[1] + f.values[2] * f.values[2])
        }
        return FloatArray(points) { i ->
            val idx = (i.toFloat() / (points - 1) * (magnitudes.size - 1)).toInt()
                .coerceIn(0, magnitudes.size - 1)
            magnitudes[idx]
        }
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return sqrt(sum)
    }

    companion object {
        private const val DOWNSAMPLE_POINTS = 20
        private const val MIN_COVERAGE_MEAN = 0.001f
    }
}
