package io.github.dianila68.gesturemacro.core.sensors

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Counts acceleration peaks (deviation of |a| from gravity) and fires when enough
 * distinct peaks land inside a sliding window.
 */
class ShakeDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.SHAKE

    private val threshold = lerp(LOOSE_THRESHOLD, TIGHT_THRESHOLD, sensitivity)
    private val peaks = ArrayDeque<Long>()
    private var lastPeakAt = Long.MIN_VALUE

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.ACCELEROMETER || sample.v.size < 3) return null
        val magnitude = sqrt(sample.v[0] * sample.v[0] + sample.v[1] * sample.v[1] + sample.v[2] * sample.v[2])
        val deviation = abs(magnitude - EARTH_GRAVITY)
        if (deviation < threshold) return null
        if (lastPeakAt != Long.MIN_VALUE && sample.t - lastPeakAt < MIN_PEAK_SPACING_MS) return null

        lastPeakAt = sample.t
        peaks.addLast(sample.t)
        while (peaks.isNotEmpty() && sample.t - peaks.first() > WINDOW_MS) {
            peaks.removeFirst()
        }
        if (peaks.size >= REQUIRED_PEAKS) {
            val confidence = min(1f, 0.5f + deviation / (threshold * 4f))
            reset()
            return GestureEvent(pattern, sample.t, confidence)
        }
        return null
    }

    override fun reset() {
        peaks.clear()
        lastPeakAt = Long.MIN_VALUE
    }

    companion object {
        const val LOOSE_THRESHOLD = 12f
        const val TIGHT_THRESHOLD = 4f
        const val MIN_PEAK_SPACING_MS = 100L
        const val WINDOW_MS = 1_500L
        const val REQUIRED_PEAKS = 3
    }
}
