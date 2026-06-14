package io.github.dianila68.gesturemacro.core.sensors

/**
 * Fires when two distinct shakes land within [DOUBLE_WINDOW_MS] of each other.
 *
 * Implemented by composition over [ShakeDetector]: the inner detector owns all
 * peak/burst logic and resets itself after each shake, so a double shake needs
 * two full shakes (six peaks) rather than one long one. Because a vigorous
 * single shake can also satisfy the inner detector twice in a row, a SHAKE macro
 * and a DOUBLE_SHAKE macro may both match the same energetic gesture — the two
 * patterns are offered separately so the user picks the intent.
 */
class DoubleShakeDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.DOUBLE_SHAKE
    override val sensor = SensorType.ACCELEROMETER

    private val inner = ShakeDetector(sensitivity)
    private var firstShakeAt = Long.MIN_VALUE

    override fun feed(sample: SensorSample): GestureEvent? {
        val shake = inner.feed(sample) ?: return null
        val t = shake.t
        return when {
            firstShakeAt == Long.MIN_VALUE -> {
                firstShakeAt = t
                null
            }

            t - firstShakeAt <= DOUBLE_WINDOW_MS -> {
                reset()
                GestureEvent(pattern, t, shake.confidence)
            }

            // Too slow to be a pair: treat this shake as the first of a new attempt.
            else -> {
                firstShakeAt = t
                null
            }
        }
    }

    override fun reset() {
        inner.reset()
        firstShakeAt = Long.MIN_VALUE
    }

    companion object {
        const val DOUBLE_WINDOW_MS = 1_200L
    }
}
