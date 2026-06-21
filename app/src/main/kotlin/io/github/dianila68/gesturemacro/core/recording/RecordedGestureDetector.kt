package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import kotlin.math.sqrt

/**
 * Live detector that matches incoming sensor frames against a [GestureEnvelope].
 *
 * Sensitivity multipliers: low=1.5, medium=2.0, high=3.0 applied to per-slice std.
 * Match threshold: 75% of slices must be within the band.
 */
class RecordedGestureDetector(
    val envelopeId: String,
    private val envelope: GestureEnvelope,
    sensitivity: Float = 0.5f,
) : GestureDetector {

    override val pattern: GesturePattern = GesturePattern.SHAKE // placeholder; keyed by envelopeId
    override val sensor: SensorType = SensorType.ACCELEROMETER

    private val sensitivityMultiplier = lerp(LOW_K, HIGH_K, sensitivity)
    private val matchThreshold = lerp(HIGH_THRESHOLD, LOW_THRESHOLD, sensitivity)

    // Sliding buffer of recent accel magnitudes with timestamps (ms)
    private val buffer: ArrayDeque<Pair<Long, Float>> = ArrayDeque()

    // Buffer upper bound in ms
    private val bufferWindowMs = (envelope.durationMeanMs + 2f * envelope.durationStdMs)
        .coerceAtLeast(500f).toLong()

    // Minimum data before attempting a match
    private val minDataMs = (envelope.durationMeanMs - envelope.durationStdMs)
        .coerceAtLeast(MIN_DATA_COERCE_MS).toLong()

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.ACCELEROMETER || sample.v.size < MIN_RECENT_FRAMES) return null

        val mag = sqrt(
            sample.v[0] * sample.v[0] +
                sample.v[1] * sample.v[1] +
                sample.v[2] * sample.v[2],
        )
        buffer.addLast(sample.t to mag)

        // Prune old entries
        while (buffer.isNotEmpty() && sample.t - buffer.first().first > bufferWindowMs) {
            buffer.removeFirst()
        }

        // Need minimum data before matching
        if (buffer.isEmpty()) return null
        val span = buffer.last().first - buffer.first().first
        if (span < minDataMs) return null

        return tryMatch(sample.t)
    }

    private fun tryMatch(nowMs: Long): GestureEvent? {
        val recentMs = envelope.durationMeanMs.toLong().coerceAtLeast(MIN_RECENT_MS)
        val recent = buffer.filter { (t, _) -> nowMs - t <= recentMs }
        if (recent.size < MIN_RECENT_FRAMES) return null

        // Time-normalise to envelope slice count
        val magnitudes = recent.map { it.second }
        val normalised = GestureEnvelopeBuilder.timeNormalise(magnitudes, envelope.sliceCount)

        // Per-slice band check
        var passCount = 0
        for (i in 0 until envelope.sliceCount) {
            val lo = envelope.magnitudeMean[i] - sensitivityMultiplier * envelope.magnitudeStd[i]
            val hi = envelope.magnitudeMean[i] + sensitivityMultiplier * envelope.magnitudeStd[i]
            if (normalised[i] in lo..hi) passCount++
        }

        val matchFraction = passCount.toFloat() / envelope.sliceCount
        if (matchFraction < matchThreshold) return null

        reset()
        return GestureEvent(pattern, nowMs, matchFraction)
    }

    override fun reset() {
        buffer.clear()
    }

    companion object {
        // Sensitivity multiplier range
        private const val LOW_K = 1.5f
        private const val HIGH_K = 3.0f

        // Match fraction threshold range (low sensitivity = stricter)
        private const val HIGH_THRESHOLD = 0.85f
        private const val LOW_THRESHOLD = 0.65f

        private const val MIN_DATA_COERCE_MS = 100f
        private const val MIN_RECENT_MS = 100L
        private const val MIN_RECENT_FRAMES = 3

        /** Utility sensitivity slider value → multiplier. */
        fun sensitivityToMultiplier(sensitivity: Float): Float = lerp(LOW_K, HIGH_K, sensitivity)

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
    }
}

/** Registry ID for a recorded gesture trigger. */
data class RecordedTriggerId(val envelopeId: String)
