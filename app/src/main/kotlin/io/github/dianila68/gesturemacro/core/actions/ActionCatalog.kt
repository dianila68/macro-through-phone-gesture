package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.LocationAlertAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import io.github.dianila68.gesturemacro.core.serialization.PlaySoundAction
import io.github.dianila68.gesturemacro.core.serialization.SoundMode
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction

enum class ActionCategory(val label: String) {
    MEDIA_CONTROL("Media control"),
    SYSTEM_TOGGLE("System toggle"),
    APP_LAUNCH("App launch"),
    SOUND("Sound / Voice"),
    LOCATION_ALERT("Location alert"),
    ACCESSIBILITY("Accessibility"),
}

/**
 * One action the app understands, described once for every consumer.
 *
 * [available] is true exactly when [buildDirect] can produce a runnable
 * [MacroAction] today — the [init] invariant ties that flag to the presence of
 * a builder so the catalog can never claim an action is available without a
 * working executor behind it. [requiresPackage] marks template entries (APP_LAUNCH)
 * whose concrete target is supplied at selection time via [ActionAssembly].
 *
 * Consumers read only the display fields ([category], [displayName], [description]);
 * the raw command/target detail stays internal to the builder.
 */
data class ActionSpec(
    /** Stable identifier — saved/referenced by picker selections. Never changes after release. */
    val id: String,
    val category: ActionCategory,
    val displayName: String,
    val description: String,
    val available: Boolean,
    /** True when a package name must be provided via [ActionAssembly.assembleWithPackage]. */
    val requiresPackage: Boolean = false,
    private val builder: (() -> MacroAction)?,
) {
    init {
        require(available == (builder != null)) {
            "available must match the presence of a builder for '$id'"
        }
    }

    /** Builds the action directly; null for template entries ([requiresPackage] == true). */
    internal fun buildDirect(): MacroAction? = if (!requiresPackage) builder?.invoke() else null

    /**
     * Builds the action substituting [packageName]; only valid for [requiresPackage] entries.
     * Exposed internally for [ActionAssembly].
     */
    internal fun buildWithPackage(packageName: String): MacroAction? =
        if (requiresPackage) builder?.invoke()?.let {
            when (it) {
                is IntentAction -> it.copy(target = packageName)
                else -> it
            }
        } else null
}

/**
 * Single source of truth for macro actions, mirroring [TriggerLibrary].
 * The editor picker reads [available]; [byCategory] groups them for display.
 * Each entry hides serialized command detail behind a friendly display surface.
 */
object ActionCatalog {
    val all: List<ActionSpec> = listOf(
        // ── Media control ────────────────────────────────────────────────────
        ActionSpec(
            id = "media.play_pause",
            category = ActionCategory.MEDIA_CONTROL,
            displayName = "Play / Pause",
            description = "Toggle play/pause on the active media player.",
            available = true,
            builder = { MediaControlAction(command = "play_pause") },
        ),
        ActionSpec(
            id = "media.next",
            category = ActionCategory.MEDIA_CONTROL,
            displayName = "Next track",
            description = "Skip to the next track.",
            available = true,
            builder = { MediaControlAction(command = "next") },
        ),
        ActionSpec(
            id = "media.previous",
            category = ActionCategory.MEDIA_CONTROL,
            displayName = "Previous track",
            description = "Go back to the previous track.",
            available = true,
            builder = { MediaControlAction(command = "previous") },
        ),
        ActionSpec(
            id = "media.stop",
            category = ActionCategory.MEDIA_CONTROL,
            displayName = "Stop playback",
            description = "Stop the active media player.",
            available = true,
            builder = { MediaControlAction(command = "stop") },
        ),

        // ── System toggle ────────────────────────────────────────────────────
        ActionSpec(
            id = "system.flashlight",
            category = ActionCategory.SYSTEM_TOGGLE,
            displayName = "Toggle flashlight",
            description = "Toggle the camera flash unit on or off.",
            available = true,
            builder = { SystemToggleAction(target = "flashlight") },
        ),

        // ── App launch ───────────────────────────────────────────────────────
        ActionSpec(
            id = "app.launch",
            category = ActionCategory.APP_LAUNCH,
            displayName = "Launch app",
            description = "Launch any installed app by package name. " +
                "The target package is selected when you add this action.",
            available = true,
            requiresPackage = true,
            builder = { IntentAction(target = "com.example.placeholder", command = "launch") },
        ),

        // ── Sound / Voice ────────────────────────────────────────────────────
        ActionSpec(
            id = "sound.alert",
            category = ActionCategory.SOUND,
            displayName = "Play alert sound",
            description = "Play the system alert/notification sound.",
            available = true,
            builder = { PlaySoundAction(mode = SoundMode.BUNDLED, bundledSound = "alert") },
        ),
        ActionSpec(
            id = "sound.chime",
            category = ActionCategory.SOUND,
            displayName = "Play chime",
            description = "Play a brief chime sound.",
            available = true,
            builder = { PlaySoundAction(mode = SoundMode.BUNDLED, bundledSound = "chime") },
        ),
        ActionSpec(
            id = "sound.tts_hello",
            category = ActionCategory.SOUND,
            displayName = "Speak phrase",
            description = "Speak a custom phrase via text-to-speech. " +
                "Customize the text in the action details.",
            available = true,
            builder = { PlaySoundAction(mode = SoundMode.TTS, ttsText = "Hello") },
        ),

        // ── Location alert ───────────────────────────────────────────────────
        ActionSpec(
            id = "location.alert",
            category = ActionCategory.LOCATION_ALERT,
            displayName = "Send location alert",
            description = "Acquire current location and SMS it to a pre-set contact " +
                "after a countdown. Tap Cancel on the notification to abort. " +
                "Not a replacement for emergency services.",
            available = true,
            builder = {
                LocationAlertAction(
                    contactName = "Emergency contact",
                    contactPhone = "+10000000000",
                    countdownSec = 15,
                )
            },
        ),

        // ── Accessibility ────────────────────────────────────────────────────
        ActionSpec(
            id = "accessibility.back",
            category = ActionCategory.ACCESSIBILITY,
            displayName = "Global back",
            description = "Perform the global Back gesture.",
            available = true,
            builder = { AccessibilityAction(target = "", command = "back") },
        ),
        ActionSpec(
            id = "accessibility.notifications",
            category = ActionCategory.ACCESSIBILITY,
            displayName = "Open notifications",
            description = "Pull down the notification shade.",
            available = true,
            builder = { AccessibilityAction(target = "", command = "notifications") },
        ),
    )

    /** Entries that can fire today — the picker's source. */
    val available: List<ActionSpec> = all.filter { it.available }

    /** Groups [available] entries by category for a sectioned picker. */
    fun byCategory(): Map<ActionCategory, List<ActionSpec>> =
        available.groupBy { it.category }

    fun forCategory(category: ActionCategory): List<ActionSpec> =
        available.filter { it.category == category }

    fun forId(id: String): ActionSpec? = all.firstOrNull { it.id == id }
}
