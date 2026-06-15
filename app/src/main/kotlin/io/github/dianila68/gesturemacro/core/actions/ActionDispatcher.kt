package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.IntentAction
import io.github.dianila68.gesturemacro.core.serialization.LocationAlertAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.MediaControlAction
import io.github.dianila68.gesturemacro.core.serialization.PlaySoundAction
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import kotlinx.coroutines.delay

sealed class ExecResult {
    data object Success : ExecResult()

    data class Failure(val reason: String, val fatal: Boolean = false) : ExecResult()
}

fun interface ActionExecutor {
    /** Must not throw across this boundary; failures are returned, not raised (DESIGN.md). */
    suspend fun execute(action: MacroAction): ExecResult
}

/**
 * Runs a macro's actions sequentially, honoring delay_after_ms, stopping on
 * fatal failures, and reporting every result for the audit log (threat T4).
 */
class ActionDispatcher(
    private val systemToggle: ActionExecutor,
    private val mediaControl: ActionExecutor,
    private val intent: ActionExecutor,
    private val accessibility: ActionExecutor,
    private val soundExecutor: ActionExecutor,
    private val locationAlert: ActionExecutor,
) {
    suspend fun run(macro: GestureMacro): List<ExecResult> {
        val results = mutableListOf<ExecResult>()
        for (action in macro.actions) {
            val executor = when (action) {
                is SystemToggleAction -> systemToggle
                is MediaControlAction -> mediaControl
                is IntentAction -> intent
                is AccessibilityAction -> accessibility
                is PlaySoundAction -> soundExecutor
                is LocationAlertAction -> locationAlert
            }
            val result = try {
                executor.execute(action)
            } catch (e: Exception) {
                ExecResult.Failure(e.message ?: "Unexpected executor failure", fatal = true)
            }
            results += result
            if (result is ExecResult.Failure && result.fatal) break
            if (action.delayAfterMs > 0) delay(action.delayAfterMs)
        }
        return results
    }
}
