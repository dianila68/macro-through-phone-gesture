package io.github.dianila68.gesturemacro.core.sensors

/**
 * Detects a hand wave just over the screen: a far → near → far transition where
 * the "near" phase is brief.
 *
 * Proximity sensors are coarse — most report a binary near/far reading: 0 for
 * near and `Sensor.getMaximumRange()` for far. Near/far is classified relative to
 * [maximumRange] so the detector works on all real devices, including common binary
 * sensors that report far as exactly their max range (often 5 cm). [maximumRange]
 * is injected at construction from `Sensor.getMaximumRange()`; the detector itself
 * has no Android dependency.
 *
 * It fires only when the cover lasts between [MIN_COVER_MS] and [MAX_COVER_MS]:
 * shorter is sensor flicker, longer is the phone in a pocket — neither is a wave.
 *
 * Sensitivity scales the "near" fraction: higher sensitivity widens the near band
 * so a less complete cover still counts.
 */
class ProximityWaveDetector(
    sensitivity: Float = 0.5f,
    /** Sensor.getMaximumRange() for the device's proximity sensor; defaulted for JVM tests. */
    maximumRange: Float = DEFAULT_MAX_RANGE,
) : GestureDetector {
    override val pattern = GesturePattern.PROXIMITY_WAVE
    override val sensor = SensorType.PROXIMITY

    private val nearThreshold = maximumRange * lerp(NEAR_STRICT_FRACTION, NEAR_LENIENT_FRACTION, sensitivity)
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
        /** Fraction of maximumRange below which the sensor is "near" (strict sensitivity). */
        const val NEAR_STRICT_FRACTION = 0.3f

        /** Fraction of maximumRange below which the sensor is "near" (lenient sensitivity). */
        const val NEAR_LENIENT_FRACTION = 0.7f

        /** Fallback maximumRange used in JVM tests where no Android Sensor is available. */
        const val DEFAULT_MAX_RANGE = 10f

        /** A deliberate wave covers the sensor briefly; outside this band it is not a wave. */
        const val MIN_COVER_MS = 60L
        const val MAX_COVER_MS = 1_000L
    }
}
