package io.github.dianila68.gesturemacro.android.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.view.KeyEvent
import io.github.dianila68.gesturemacro.core.actions.ActionExecutor
import io.github.dianila68.gesturemacro.core.actions.ExecResult
import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction

/**
 * ticket-023: Android-coupled executors quarantined to `.android.actions`.
 * The [ActionExecutor] SPI lives in `core.actions` (no android.* imports).
 */

/** Toggles the camera flash unit. setTorchMode needs no CAMERA permission. */
class FlashlightExecutor(context: Context) : ActionExecutor {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var torchOn = false

    override suspend fun execute(action: MacroAction): ExecResult {
        val toggle = action as? SystemToggleAction
            ?: return ExecResult.Failure("FlashlightExecutor got ${action::class.simpleName}")
        if (toggle.target != TARGET_FLASHLIGHT) {
            return ExecResult.Failure("Unknown system toggle target: ${toggle.target}")
        }
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ExecResult.Failure("No flash unit on this device")
            torchOn = !torchOn
            cameraManager.setTorchMode(cameraId, torchOn)
            ExecResult.Success
        } catch (e: Exception) {
            ExecResult.Failure("Torch toggle failed: ${e.message}")
        }
    }

    companion object {
        const val TARGET_FLASHLIGHT = "flashlight"
    }
}

/** Dispatches media key events; the active MediaSession (e.g. Spotify) receives them. */
class MediaControlExecutor(context: Context) : ActionExecutor {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override suspend fun execute(action: MacroAction): ExecResult {
        val media = action as? MediaControlAction
            ?: return ExecResult.Failure("MediaControlExecutor got ${action::class.simpleName}")
        val keyCode = when (media.command) {
            "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return ExecResult.Failure("Unknown media command: ${media.command}")
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ExecResult.Success
    }
}

/** v1 intent surface: launch an app by package name (command == "launch"). */
class IntentExecutor(private val context: Context) : ActionExecutor {
    override suspend fun execute(action: MacroAction): ExecResult {
        val spec = action as? IntentAction
            ?: return ExecResult.Failure("IntentExecutor got ${action::class.simpleName}")
        if (spec.command != "launch") {
            return ExecResult.Failure("Unknown intent command: ${spec.command}")
        }
        val launch = context.packageManager.getLaunchIntentForPackage(spec.target)
            ?: return ExecResult.Failure("No launchable app for package ${spec.target}")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launch)
            ExecResult.Success
        } catch (e: Exception) {
            ExecResult.Failure("Launch failed: ${e.message}")
        }
    }
}
