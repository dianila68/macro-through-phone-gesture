package io.github.dianila68.gesturemacro.service

import android.content.Context
import android.os.PowerManager

/**
 * Partial wakelocks scoped to bounded gesture windows only. There is deliberately no
 * unbounded acquire: an indefinite hold is impossible by construction (NFR-1).
 */
class WakeLockGuard(context: Context, private val tag: String) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var lock: PowerManager.WakeLock? = null

    fun openWindow(timeoutMs: Long) {
        val bounded = timeoutMs.coerceIn(1L, MAX_WINDOW_MS)
        synchronized(this) {
            releaseLocked()
            lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag).also {
                it.setReferenceCounted(false)
                it.acquire(bounded)
            }
        }
    }

    fun closeWindow() {
        synchronized(this) { releaseLocked() }
    }

    private fun releaseLocked() {
        lock?.takeIf { it.isHeld }?.release()
        lock = null
    }

    companion object {
        const val MAX_WINDOW_MS = 10_000L
    }
}
