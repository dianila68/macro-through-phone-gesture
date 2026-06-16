package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.engine.ExecutorRegistrySpi
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
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
 * ticket-038: Routes actions through [ExecutorRegistrySpi] map lookup instead of a
 * sealed when-expression. Adding a new action type requires only a new [MacroAction]
 * subclass, a new [ActionExecutor], and a registry entry — no changes here.
 *
 * Runs a macro's actions sequentially, honoring delay_after_ms, stopping on
 * fatal failures, and reporting every result for the audit log (threat T4).
 */
class ActionDispatcher(private val registry: ExecutorRegistrySpi) {
    suspend fun run(macro: GestureMacro): List<ExecResult> {
        val executors = registry.executors()
        val results = mutableListOf<ExecResult>()
        for (action in macro.actions) {
            val executor = executors[action.actionType]
                ?: run {
                    results += ExecResult.Failure("No executor registered for action type '${action.actionType}'", fatal = true)
                    break
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
