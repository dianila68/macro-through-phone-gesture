package io.github.dianila68.gesturemacro.service

import android.content.Context

/**
 * Persists a liveness timestamp so unexpected engine deaths are detectable across
 * process restarts and surfaced in diagnostics (threat T6, FR-9).
 */
class Heartbeat(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun beat(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_BEAT, now).apply()
    }

    fun lastBeat(): Long = prefs.getLong(KEY_LAST_BEAT, 0L)

    fun recordStop(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_CLEAN_STOP, now).apply()
    }

    /** True when the last shutdown was not a user-requested stop (engine was killed). */
    fun diedUnexpectedly(): Boolean {
        val last = lastBeat()
        return last != 0L && prefs.getLong(KEY_CLEAN_STOP, 0L) < last
    }

    companion object {
        private const val PREFS = "engine_heartbeat"
        private const val KEY_LAST_BEAT = "last_beat"
        private const val KEY_CLEAN_STOP = "clean_stop"
    }
}
