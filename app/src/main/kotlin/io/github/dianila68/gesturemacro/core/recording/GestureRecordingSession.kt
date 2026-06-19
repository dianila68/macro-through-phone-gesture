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

    private val _coverageUpdates = MutableSharedFlow<CoverageUpdate>(extraBufferCapacity = 16)
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
        scorer.reset()
        coverageTracker.reset()

        sessionJob = scope.launch {
            // Countdown
            val countdownStep = 250L
            var remaining = config.countdownMs
            while (remaining > 0) {
                _state.value = RecordingState.Countdown(remaining)
                delay(countdownStep)
                remaining -= countdownStep
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
                    delay(50)
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
                        delay(100)
                        pauseRemaining -= 100
                    }
                }
            }

            // Check sufficiency
            val usableCount = buffer.windows.count { w ->
                scorer.score(w, config) >= RepetitionQualityScorer.LOW_QUALITY_THRESHOLD
            }.coerceAtLeast(
                if (collectedCount >= config.minSamples) collectedCount else 0,
            )

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
        if (_state.value !is RecordingState.Ready &&
            _state.value !is RecordingState.Cancelled &&
            _state.value !is RecordingState.InsufficientData &&
            _state.value !is RecordingState.TimedOut
        ) {
            _state.value = RecordingState.Cancelled
            sessionJob?.cancel()
        }
    }

    override fun signalStillness() {
        stillnessSignalled = true
    }
}

/** Scores a single SampleWindow for quality. Returns 0..1. */
class RepetitionQualityScorer {

    fun reset() {} // stateless; reset is no-op

    fun score(window: SampleWindow, config: RecordingConfig): Float {
        val accelFrames = window.frames.filter { it.channel == RecordingChannel.ACCELEROMETER }
        if (accelFrames.isEmpty()) return 0f

        val duration = window.durationMs.toFloat()
        val maxDuration = config.maxWindowMs.toFloat()

        // Duration ratio factor (ideal: 0.2–0.7 of max window)
        val durationRatio = duration / maxDuration
        val durationScore = when {
            durationRatio < 0.1f -> 0f
            durationRatio > 0.9f -> 0.3f
            durationRatio in 0.2f..0.7f -> 1f
            else -> 0.6f
        }

        // Peak magnitude factor
        val magnitudes = accelFrames.map { f ->
            sqrt(f.values[0] * f.values[0] + f.values[1] * f.values[1] + f.values[2] * f.values[2])
        }
        val peakMag = magnitudes.maxOrNull() ?: 0f
        val peakScore = when {
            peakMag < 1.5f -> 0f
            peakMag >= 4f -> 1f
            else -> (peakMag - 1.5f) / 2.5f
        }

        // Spectral spread (rough: variance of magnitudes as proxy for spread)
        val mean = magnitudes.average().toFloat()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
        val spreadScore = when {
            variance < 0.01f -> 0f
            variance > 2f -> 1f
            else -> variance / 2f
        }

        return (durationScore + peakScore + spreadScore) / 3f
    }

    companion object {
        const val LOW_QUALITY_THRESHOLD = 0.4f
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

    fun reset() {}

    fun update(windows: List<SampleWindow>, config: RecordingConfig = RecordingConfig()): CoverageReport {
        val lowQuality = windows.count { scorer.score(it, config) < RepetitionQualityScorer.LOW_QUALITY_THRESHOLD }

        val coverageScore = if (windows.size < 2) {
            0f
        } else {
            // Pairwise distances on 20-point downsampled magnitude traces
            val traces = windows.map { downsample(it, 20) }
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
            // High variation among samples = better coverage
            if (mean < 0.001f) 0f else (1f - (std / mean.coerceAtLeast(0.001f))).coerceIn(0f, 1f)
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
}
