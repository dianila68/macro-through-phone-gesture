package io.github.dianila68.gesturemacro.android.actions

import io.github.dianila68.gesturemacro.core.actions.ActionExecutor
import io.github.dianila68.gesturemacro.core.actions.ExecResult
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import io.github.dianila68.gesturemacro.core.serialization.WebhookAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * ticket-056: Fire-and-forget HTTP webhook executor.
 * Uses java.net.HttpURLConnection (no extra deps). Timeout 10s connect / 15s read. No retry.
 */
class WebhookExecutor : ActionExecutor {
    override suspend fun execute(action: MacroAction): ExecResult {
        if (action !is WebhookAction) return ExecResult.Failure("Wrong action type", fatal = false)
        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(action.url).openConnection() as HttpURLConnection).apply {
                    requestMethod = action.method
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    doOutput = action.method != "GET"
                    action.headers.forEach { (k, v) -> setRequestProperty(k, v) }
                    if (doOutput && action.bodyTemplate.isNotBlank()) {
                        outputStream.use { it.write(action.bodyTemplate.toByteArray()) }
                    }
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..299) ExecResult.Success
                else ExecResult.Failure("HTTP $code from ${action.url}", fatal = false)
            }.getOrElse { e -> ExecResult.Failure("Webhook error: ${e.message}", fatal = false) }
        }
    }
}
