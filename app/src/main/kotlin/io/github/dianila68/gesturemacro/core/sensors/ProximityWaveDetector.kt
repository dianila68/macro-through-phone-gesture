package io.github.dianila68.gesturemacro.core.sensors

/**
 * Detects a hand wave just over the screen: a far → near → far transition where
 * the "near" phase is brief.
 *
 * Proximity sensors are coarse — most report a binary near/far distance (≈0 cm
 * vs the sensor's max range) in a single value. This detector reads [v]`[0]` and
 * treats anything under [nearThreshold] as covered. It fires only when the cover
 * lasts between [MIN_COVER_MS] and [MAX_COVER_MS]: shorter is sensor flicker,
 * longer is the phone in a pocket or face-down on a table, neither of which is a
 * deliberate wave.
 */
class ProximityWaveDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern = GesturePattern.PROXIMITY_WAVE
    override val sensor = SensorType.PROXIMITY

    // Higher sensitivity widens the "near" zone, so a less complete cover still counts.
    private val nearThreshold = lerp(NEAR_STRICT_CM, NEAR_LENIENT_CM, sensitivity)
    private var coveredSince = Long.MIN_VALUE

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.PROXIMITY || sample.v.isEmpty()) return null
        val near = sample.v[0] < nearThreshold
        if (near) {
            if (coveredSince == Long.MIN_VALUE) coveredSince = sample.t
            return null
        }
        // Uncovered (far).
        if (coveredSince == Long.MIN_VALUE) return null
        val coveredMs = sample.t - coveredSince
        coveredSince = Long.MIN_VALUE
        return if (coveredMs in MIN_COVER_MS..MAX_COVER_MS) {
            GestureEvent(pattern, sample.t, 1f)
        } else {
            null
        }
    }

    override fun reset() {
        coveredSince = Long.MIN_VALUE
    }

    companion object {
        /** Distance (cm) under which the sensor is "covered"; widened by sensitivity. */
        const val NEAR_STRICT_CM = 3.0f
        const val NEAR_LENIENT_CM = 8.0f

        /** A deliberate wave covers the sensor briefly; outside this band it is not a wave. */
        const val MIN_COVER_MS = 60L
        const val MAX_COVER_MS = 1_000L
    }
}
