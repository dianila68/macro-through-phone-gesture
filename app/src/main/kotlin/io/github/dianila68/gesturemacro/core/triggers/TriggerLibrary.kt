package io.github.dianila68.gesturemacro.core.triggers

import io.github.dianila68.gesturemacro.core.sensors.AltitudeFallDetector
import io.github.dianila68.gesturemacro.core.sensors.AltitudeRiseDetector
import io.github.dianila68.gesturemacro.core.sensors.DoubleShakeDetector
import io.github.dianila68.gesturemacro.core.sensors.FallDetector
import io.github.dianila68.gesturemacro.core.sensors.FlipDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.GoingBrightDetector
import io.github.dianila68.gesturemacro.core.sensors.GoingDarkDetector
import io.github.dianila68.gesturemacro.core.sensors.PickedUpDetector
import io.github.dianila68.gesturemacro.core.sensors.ProximityWaveDetector
import io.github.dianila68.gesturemacro.core.sensors.ShakeDetector
import io.github.dianila68.gesturemacro.core.sensors.StationaryDetector
import io.github.dianila68.gesturemacro.core.sensors.StepDetector
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

        // ── M4: Single-sensor use cases (ticket-032) ──────────────────────────
        TriggerSpec(
            pattern = PatternKind.STEP_DETECTED,
            sensor = SensorKind.STEP_COUNTER,
            displayName = "Step detected",
            description = "Fires on each step registered by the hardware step counter.",
            available = true,
            defaultCooldownMs = 1_000,
            sensitivityHint = "Sensitivity has no effect — step events come from hardware.",
            detectorFactory = { s -> StepDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.IS_STATIONARY,
            sensor = SensorKind.STEP_COUNTER,
            displayName = "Becomes stationary",
            description = "Fires when the device has not moved (no steps) for a sustained period.",
            available = true,
            defaultCooldownMs = 60_000,
            sensitivityHint = "Higher = fires after a shorter stillness period.",
            detectorFactory = { s -> StationaryDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.PICKED_UP,
            sensor = SensorKind.ACCELEROMETER,
            displayName = "Picked up",
            description = "Fires when the device is lifted from a resting surface.",
            available = true,
            defaultCooldownMs = 5_000,
            sensitivityHint = "Higher = a smaller motion counts as a pick-up.",
            detectorFactory = { s -> PickedUpDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.GOING_DARK,
            sensor = SensorKind.LIGHT,
            displayName = "Going dark",
            description = "Fires when ambient light drops below a threshold (e.g. entering a pocket or dark room).",
            available = true,
            defaultCooldownMs = 5_000,
            sensitivityHint = "Higher = a smaller drop in light triggers it.",
            detectorFactory = { s -> GoingDarkDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.GOING_BRIGHT,
            sensor = SensorKind.LIGHT,
            displayName = "Going bright",
            description = "Fires when ambient light rises above a threshold (e.g. leaving a pocket).",
            available = true,
            defaultCooldownMs = 5_000,
            sensitivityHint = "Higher = a smaller rise in light triggers it.",
            detectorFactory = { s -> GoingBrightDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.ALTITUDE_RISE,
            sensor = SensorKind.PRESSURE,
            displayName = "Going up (altitude rise)",
            description = "Fires when barometric pressure drops, indicating the device is moving upward (stairs, elevator).",
            available = true,
            defaultCooldownMs = 10_000,
            sensitivityHint = "Higher = a smaller altitude change triggers it.",
            detectorFactory = { s -> AltitudeRiseDetector(s) },
        ),
        TriggerSpec(
            pattern = PatternKind.ALTITUDE_FALL,
            sensor = SensorKind.PRESSURE,
            displayName = "Going down (altitude fall)",
            description = "Fires when barometric pressure rises, indicating the device is moving downward.",
            available = true,
            defaultCooldownMs = 10_000,
            sensitivityHint = "Higher = a smaller altitude change triggers it.",
            detectorFactory = { s -> AltitudeFallDetector(s) },
        ),
    )

    /** Triggers that can actually fire today — what the editor should offer. */
    val available: List<TriggerSpec> = all.filter { it.available }

    fun forPattern(pattern: PatternKind): TriggerSpec? = all.firstOrNull { it.pattern == pattern }

    /** Live detectors for every available trigger, built at [sensitivity]. */
    fun detectors(sensitivity: Float = TriggerSpec.DEFAULT_SENSITIVITY): List<GestureDetector> =
        available.mapNotNull { it.buildDetector(sensitivity) }
}
