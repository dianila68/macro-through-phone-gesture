package io.github.dianila68.gesturemacro.android.actions

import android.accessibilityservice.AccessibilityService
import io.github.dianila68.gesturemacro.core.actions.ActionExecutor
import io.github.dianila68.gesturemacro.core.actions.ExecResult
import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction

/**
 * ticket-023/039: Quarantined to `.android.actions`; decoupled from
 * [MacroAccessibilityService] singleton via [AccessibilityServiceGate].
 *
 * v1 accessibility surface: global actions only (ticket-004 smoke scope).
 * Refuses to act while the service is disconnected — never queues (NFR-7).
 */
class AccessibilityExecutor(private val gate: AccessibilityServiceGate) : ActionExecutor {
    override suspend fun execute(action: MacroAction): ExecResult {
        val spec = action as? AccessibilityAction
            ?: return ExecResult.Failure("AccessibilityExecutor got ${action::class.simpleName}")
        val service = gate.service()
            ?: return ExecResult.Failure("Accessibility service is not enabled", fatal = true)
        val globalAction = when (spec.command) {
            "back" -> AccessibilityService.GLOBAL_ACTION_BACK
            "home" -> AccessibilityService.GLOBAL_ACTION_HOME
            "recents" -> AccessibilityService.GLOBAL_ACTION_RECENTS
            "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            else -> return ExecResult.Failure("Unknown accessibility command: ${spec.command}")
        }
        return if (service.performGlobalAction(globalAction)) {
            ExecResult.Success
        } else {
            ExecResult.Failure("Global action ${spec.command} was not dispatched")
        }
    }
}
