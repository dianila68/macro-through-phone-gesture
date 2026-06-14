package io.github.dianila68.gesturemacro.core.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Kotlin mirror of schema/gesture-macro-v1.json — the app's public macro contract.
 * Invariants enforced in init blocks so no invalid macro can exist in memory.
 */
@Serializable
data class GestureMacro(
    val version: Int,
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val trigger: Trigger,
    val constraints: Constraints = Constraints(),
    val actions: List<MacroAction>,
) {
    init {
        require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH) {
            "name must be 1..$MAX_NAME_LENGTH characters"
        }
        require(name.none { it.isISOControl() }) { "name must not contain control characters" }
        require(actions.isNotEmpty()) { "actions must not be empty" }
        require(id.isNotBlank()) { "id must not be blank" }
        require(id.none { it.isISOControl() }) { "id must not contain control characters" }
    }

    companion object {
        const val MAX_NAME_LENGTH = 64
    }
}

@Serializable
data class Trigger(
    val sensor: SensorKind,
    val pattern: PatternKind,
    val sensitivity: Float = 0.5f,
    @SerialName("cooldown_ms") val cooldownMs: Long = 2_000,
    @SerialName("custom_thresholds") val customThresholds: Map<String, Float> = emptyMap(),
    @SerialName("source_device") val sourceDevice: String? = null,
) {
    init {
        require(sensitivity in 0f..1f) { "trigger.sensitivity must be within 0.0..1.0" }
        require(cooldownMs >= 0) { "trigger.cooldown_ms must be >= 0" }
        require(customThresholds.isEmpty() || pattern == PatternKind.CUSTOM) {
            "trigger.custom_thresholds is only valid when pattern == custom"
        }
        require(sourceDevice == null || sensor == SensorKind.EXTERNAL) {
            "trigger.source_device is only valid when sensor == external"
        }
    }
}

@Serializable
enum class SensorKind {
    @SerialName("accelerometer")
    ACCELEROMETER,

    @SerialName("gyroscope")
    GYROSCOPE,

    @SerialName("proximity")
    PROXIMITY,

    @SerialName("external")
    EXTERNAL,
}

@Serializable
enum class PatternKind {
    @SerialName("shake")
    SHAKE,

    @SerialName("double_shake")
    DOUBLE_SHAKE,

    @SerialName("flip_face_down")
    FLIP_FACE_DOWN,

    @SerialName("flip_face_up")
    FLIP_FACE_UP,

    @SerialName("twist")
    TWIST,

    @SerialName("proximity_wave")
    PROXIMITY_WAVE,

    @SerialName("fall")
    FALL,

    @SerialName("custom")
    CUSTOM,
}

@Serializable
data class Constraints(
    @SerialName("screen_state") val screenState: ScreenState = ScreenState.ANY,
    @SerialName("time_window") val timeWindow: TimeWindow? = null,
)

@Serializable
enum class ScreenState {
    @SerialName("any")
    ANY,

    @SerialName("on")
    ON,

    @SerialName("off")
    OFF,
}

@Serializable
data class TimeWindow(val start: String, val end: String) {
    init {
        require(PATTERN.matches(start)) { "time_window.start must be HH:MM" }
        require(PATTERN.matches(end)) { "time_window.end must be HH:MM" }
    }

    companion object {
        private val PATTERN = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")
    }
}

@Serializable
sealed class MacroAction {
    abstract val delayAfterMs: Long
}

@Serializable
@SerialName("system_toggle")
data class SystemToggleAction(
    val target: String,
    @SerialName("delay_after_ms") override val delayAfterMs: Long = 0,
) : MacroAction()

@Serializable
@SerialName("media_control")
data class MediaControlAction(
    val command: String,
    val target: String? = null,
    @SerialName("delay_after_ms") override val delayAfterMs: Long = 0,
) : MacroAction()

@Serializable
@SerialName("intent")
data class IntentAction(
    val target: String,
    val command: String,
    val extras: Map<String, String> = emptyMap(),
    @SerialName("delay_after_ms") override val delayAfterMs: Long = 0,
) : MacroAction()

@Serializable
@SerialName("accessibility")
data class AccessibilityAction(
    val target: String,
    val command: String,
    @SerialName("delay_after_ms") override val delayAfterMs: Long = 0,
) : MacroAction()
