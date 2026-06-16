package io.github.dianila68.gesturemacro.core.sensors

import kotlin.math.acos

/**
 * ticket-051: Detects significant device rotation using TYPE_ROTATION_VECTOR.
 *
 * The rotation vector sensor provides a quaternion (x, y, z, w) representing
 * device orientation. This detector fires ROTATION_CHANGED when the rotation
 * delta from the last stable orientation exceeds the sensitivity threshold.
 *
 * sensitivity=0 → loose (45° delta); sensitivity=1 → tight (5° delta).
 */
class RotationVectorDetector(sensitivity: Float = 0.5f) : GestureDetector {
    override val pattern: GesturePattern = GesturePattern.ROTATION_CHANGED
    override val sensor: SensorType = SensorType.ROTATION_VECTOR

    private val thresholdDeg = lerp(LOOSE_DEG, TIGHT_DEG, sensitivity)
    private var baselineQuat: FloatArray? = null
    private var lastFiredMs = 0L

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.ROTATION_VECTOR || sample.v.size < 4) return null
        val q = sample.v
        val baseline = baselineQuat
        if (baseline == null) {
            baselineQuat = q.copyOf()
            return null
        }
        val dot = (q[0] * baseline[0] + q[1] * baseline[1] + q[2] * baseline[2] + q[3] * baseline[3])
            .coerceIn(-1f, 1f)
        val angleDeg = Math.toDegrees(2.0 * acos(dot.toDouble())).toFloat()
        if (angleDeg >= thresholdDeg && sample.t - lastFiredMs >= MIN_REFIRE_MS) {
            lastFiredMs = sample.t
            baselineQuat = q.copyOf()
            return GestureEvent(
                pattern = GesturePattern.ROTATION_CHANGED,
                t = sample.t,
                confidence = (angleDeg / 180f).coerceAtMost(1f),
            )
        }
        return null
    }

    override fun reset() {
        baselineQuat = null
        lastFiredMs = 0L
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    companion object {
        private const val LOOSE_DEG = 45f
        private const val TIGHT_DEG = 5f
        private const val MIN_REFIRE_MS = 500L
    }
}
