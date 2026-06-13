package io.github.dianila68.gesturemacro.core.sensors

import kotlin.math.abs
import kotlin.math.min

/**
 * Detects a wrist twist: a sharp rotation around the phone's long axis in one
 * direction, a brief reversal, then rotation in the opposite direction — the
 * "double twist" flick used to, e.g., launch a camera.
 *
 * Reads the gyroscope's angular velocity around the y-axis (index [TWIST_AXIS],
 * the axis running bottom-to-top of the device). It fires only when two
 * above-threshold rotations of opposite sign are separated by a genuine dip
 * below threshold (the reversal) and fall inside [WINDOW_MS]; a single
 * continuous rotation never fires, because it never reverses.
 */
class TwistDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.TWIST
    override val sensor = SensorType.GYROSCOPE

    private val threshold = lerp(LOOSE_RATE, TIGHT_RATE, sensitivity)
    private var firstPeakSign = 0
    private var firstPeakAt = Long.MIN_VALUE
    private var dippedSinceFirst = false

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.GYROSCOPE || sample.v.size < 3) return null
        val rate = sample.v[TWIST_AXIS]
        if (abs(rate) < threshold) {
            // A quiet sample after the first flick marks the reversal gap.
            if (firstPeakAt != Long.MIN_VALUE) dippedSinceFirst = true
            return null
        }

        val sign = if (rate > 0f) 1 else -1
        if (firstPeakAt == Long.MIN_VALUE) {
            startFirstPeak(sign, sample.t)
            return null
        }
        if (sample.t - firstPeakAt > WINDOW_MS) {
            // The first flick is stale: this rotation becomes a fresh attempt.
            startFirstPeak(sign, sample.t)
            return null
        }
        if (sign != firstPeakSign && dippedSinceFirst) {
            val confidence = min(1f, abs(rate) / (threshold * 2f))
            reset()
            return GestureEvent(pattern, sample.t, confidence)
        }
        if (sign == firstPeakSign) {
            // Same direction still ongoing: keep the flick window anchored to it.
            firstPeakAt = sample.t
        }
        return null
    }

    private fun startFirstPeak(sign: Int, t: Long) {
        firstPeakSign = sign
        firstPeakAt = t
        dippedSinceFirst = false
    }

    override fun reset() {
        firstPeakSign = 0
        firstPeakAt = Long.MIN_VALUE
        dippedSinceFirst = false
    }

    companion object {
        /** Angular velocity (rad/s) for the loose (hard) and tight (easy) ends of sensitivity. */
        const val LOOSE_RATE = 5.0f
        const val TIGHT_RATE = 2.0f
        const val WINDOW_MS = 1_000L

        /** Gyroscope y-axis: rotation around the device's long axis. */
        const val TWIST_AXIS = 1
    }
}
