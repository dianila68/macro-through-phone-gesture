package io.github.dianila68.gesturemacro.core.sensors

import kotlin.math.abs
import kotlin.math.atan2

/**
 * ticket-031: Compass heading-change detector from the raw magnetic field vector.
 *
 * Fires [GesturePattern.HEADING_CHANGED] when the phone's azimuth (bearing in the XY
 * plane) has rotated by at least [thresholdDeg] degrees since the baseline was last set.
 * Sensitivity maps 0→90° change required (loose) to 15° (tight).
 *
 * Note: for best accuracy the magnetometer should be calibrated and the accelerometer
 * used to compute the inclination matrix. This implementation uses the raw XY field
 * to derive azimuth, which is sufficient for coarse heading-change detection
 * (e.g. "turned around", "rotated ~90°") without requiring the accelerometer feed.
 */
class HeadingChangedDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val sensor: SensorType = SensorType.MAGNETOMETER

    private val thresholdDeg = lerp(LOOSE_DEG, TIGHT_DEG, sensitivity)
    private var baselineDeg: Float? = null
    private var lastEventT: Long = 0L

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.MAGNETOMETER) return null
        val x = sample.v.getOrElse(0) { 0f }
        val y = sample.v.getOrElse(1) { 0f }
        val azimuth = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()

        val baseline = baselineDeg
        if (baseline == null) {
            baselineDeg = azimuth
            return null
        }

        val delta = angularDelta(baseline, azimuth)
        if (delta >= thresholdDeg && sample.t - lastEventT > MIN_REFIRE_MS) {
            baselineDeg = azimuth
            lastEventT = sample.t
            return GestureEvent(GesturePattern.HEADING_CHANGED, sample.t, confidence = delta / 180f)
        }
        return null
    }

    /** Minimum signed angular difference in [0, 180]. */
    private fun angularDelta(a: Float, b: Float): Float {
        val diff = abs(a - b) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    companion object {
        private const val LOOSE_DEG = 90f
        private const val TIGHT_DEG = 15f
        private const val MIN_REFIRE_MS = 500L
    }
}
