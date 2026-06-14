package io.github.dianila68.gesturemacro.core.triggers

import io.github.dianila68.gesturemacro.core.sensors.DoubleShakeDetector
import io.github.dianila68.gesturemacro.core.sensors.FallDetector
import io.github.dianila68.gesturemacro.core.sensors.FlipDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.ProximityWaveDetector
import io.github.dianila68.gesturemacro.core.sensors.ShakeDetector
import io.github.dianila68.gesturemacro.core.sensors.TwistDetector
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.SensorKind

/**
 * One trigger the app understands, described once for every consumer.
 *
 * [available] is true exactly when a detector is wired and the trigger can fire
 * today; the [init] invariant ties that flag to the presence of a detector
 * factory so the catalog can never claim a trigger is usable while no detector
 * backs it (or hide one that is). Unavailable entries are still listed so the
 * UI can show what is planned without offering it as a choice.
 */
data class TriggerSpec(
    val pattern: PatternKind,
    val sensor: SensorKind,
    val displayName: String,
    val description: String,
    val available: Boolean,
    val defaultCooldownMs: Long,
    /** Plain-language note on what raising sensitivity does for this trigger. */
    val sensitivityHint: String,
    private val detectorFactory: ((Float) -> GestureDetector)?,
) {
    init {
        require(available == (detectorFactory != null)) {
            "available must match the presence of a detector factory for $pattern"
        }
    }

    fun buildDetector(sensitivity: Float = DEFAULT_SENSITIVITY): GestureDetector? = detectorFactory?.invoke(sensitivity)

    companion object {
        const val DEFAULT_SENSITIVITY = 0.5f
    }
}

/**
 * Single source of truth for gesture triggers. The editor lists [available]
 * entries to offer the user; the capture service builds its live detector set
 * from [detectors]. Both read this one catalog, so the UI and the runtime can
 * never drift out of sync. `CUSTOM` is deliberately absent: it is an import-only
 * escape hatch, never a user-selectable trigger.
 */
object TriggerLibrary {
    val all: List<TriggerSpec> = listOf(
        TriggerSpec(
            pattern = PatternKind.SHAKE,
            sensor = SensorKind.ACCELEROMETER,
            displayName = "Shake",
            description = "Shake the phone briskly a few times.",
            available = true,
            defaultCooldownMs = 2_000,
            sensitivityHint = "Higher = a gentler shake triggers it (and more false positives).",
            detectorFactory = { s -> ShakeDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.FLIP_FACE_DOWN,
            sensor = SensorKind.ACCELEROMETER,
            displayName = "Flip face down",
            description = "Turn the phone from face-up to face-down on a surface.",
            available = true,
            defaultCooldownMs = 2_000,
            sensitivityHint = "Higher = a shorter, looser hold at each end counts as a flip.",
            detectorFactory = { s -> FlipDetector(GesturePattern.FLIP_FACE_DOWN, s) },
        ),
        TriggerSpec(
            pattern = PatternKind.FLIP_FACE_UP,
            sensor = SensorKind.ACCELEROMETER,
            displayName = "Flip face up",
            description = "Turn the phone from face-down back to face-up.",
            available = true,
            defaultCooldownMs = 2_000,
            sensitivityHint = "Higher = a shorter, looser hold at each end counts as a flip.",
            detectorFactory = { s -> FlipDetector(GesturePattern.FLIP_FACE_UP, s) },
        ),
        TriggerSpec(
            pattern = PatternKind.DOUBLE_SHAKE,
            sensor = SensorKind.ACCELEROMETER,
            displayName = "Double shake",
            description = "Two distinct shake bursts in quick succession.",
            available = true,
            defaultCooldownMs = 2_000,
            sensitivityHint = "Higher = gentler shakes count (may also trip a single-shake macro).",
            detectorFactory = { s -> DoubleShakeDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.TWIST,
            sensor = SensorKind.GYROSCOPE,
            displayName = "Twist",
            description = "Twist your wrist: rotate the phone one way then sharply back.",
            available = true,
            defaultCooldownMs = 2_000,
            sensitivityHint = "Higher = a gentler twist triggers it.",
            detectorFactory = { s -> TwistDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.PROXIMITY_WAVE,
            sensor = SensorKind.PROXIMITY,
            displayName = "Proximity wave",
            description = "Wave a hand just over the top of the screen.",
            available = true,
            defaultCooldownMs = 2_000,
            sensitivityHint = "Higher = a less complete cover still counts as a wave.",
            // maximumRange is injected by GestureCaptureService from Sensor.getMaximumRange();
            // this factory uses the safe default (for any caller without Android context).
            detectorFactory = { s -> ProximityWaveDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.FALL,
            sensor = SensorKind.ACCELEROMETER,
            displayName = "Fall detection",
            description = "Detects a likely fall: free-fall then impact then stillness. " +
                "Not medical-grade — pair with a confirm-countdown action. " +
                "Phone must be on the body; accuracy improves with a wrist sensor.",
            available = true,
            defaultCooldownMs = 30_000,
            sensitivityHint = "Higher = easier to trigger (fewer false negatives, more false positives).",
            detectorFactory = { s -> FallDetector(s) },
        ),
    )

    /** Triggers that can actually fire today — what the editor should offer. */
    val available: List<TriggerSpec> = all.filter { it.available }

    fun forPattern(pattern: PatternKind): TriggerSpec? = all.firstOrNull { it.pattern == pattern }

    /** Live detectors for every available trigger, built at [sensitivity]. */
    fun detectors(sensitivity: Float = TriggerSpec.DEFAULT_SENSITIVITY): List<GestureDetector> =
        available.mapNotNull { it.buildDetector(sensitivity) }
}
