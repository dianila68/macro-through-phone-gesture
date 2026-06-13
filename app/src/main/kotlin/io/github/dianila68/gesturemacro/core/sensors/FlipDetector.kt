package io.github.dianila68.gesturemacro.core.sensors

/**
 * Detects a stable orientation transition on the accelerometer z-axis:
 * face-up → face-down (FLIP_FACE_DOWN) or the reverse. Both ends of the
 * transition must be held stable to reject tosses and pocket noise.
 */
class FlipDetector(
    override val pattern: GesturePattern,
    sensitivity: Float = 0.5f,
) : GestureDetector {

    override val sensor = SensorType.ACCELEROMETER

    init {
        require(pattern == GesturePattern.FLIP_FACE_DOWN || pattern == GesturePattern.FLIP_FACE_UP) {
            "FlipDetector supports flip patterns only"
        }
    }

    private val stableThreshold = lerp(LOOSE_STABLE_Z, TIGHT_STABLE_Z, sensitivity)
    private val requiredStableMs = lerp(LOOSE_STABLE_MS, TIGHT_STABLE_MS, sensitivity).toLong()
    private val sourceOrientation = if (pattern == GesturePattern.FLIP_FACE_DOWN) 1 else -1

    private var orientation = 0
    private var orientationSince = 0L
    private var armed = false

    override fun feed(sample: SensorSample): GestureEvent? {
        if (sample.sensor != SensorType.ACCELEROMETER || sample.v.size < 3) return null
        val z = sample.v[2]
        val observed = when {
            z > stableThreshold -> 1
            z < -stableThreshold -> -1
            else -> return null
        }
        if (observed != orientation) {
            orientation = observed
            orientationSince = sample.t
            return null
        }
        if (sample.t - orientationSince < requiredStableMs) return null

        if (observed == sourceOrientation) {
            armed = true
            return null
        }
        if (armed) {
            armed = false
            return GestureEvent(pattern, sample.t, 1f)
        }
        return null
    }

    override fun reset() {
        orientation = 0
        orientationSince = 0L
        armed = false
    }

    companion object {
        const val LOOSE_STABLE_Z = 9f
        const val TIGHT_STABLE_Z = 6f
        const val LOOSE_STABLE_MS = 900f
        const val TIGHT_STABLE_MS = 300f
    }
}
