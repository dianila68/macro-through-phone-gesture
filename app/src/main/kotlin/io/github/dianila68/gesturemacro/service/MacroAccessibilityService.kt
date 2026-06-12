package io.github.dianila68.gesturemacro.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Action-execution arm (ticket-004, threat T3): a thin dispatcher with no IPC
 * surface beyond the system binding. The engine treats accessibility actions as
 * inert while this service is disconnected (NFR-7).
 */
class MacroAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceState.value = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instanceState.value = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instanceState.value = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private val instanceState = MutableStateFlow<MacroAccessibilityService?>(null)

        val instance: StateFlow<MacroAccessibilityService?> = instanceState

        val isConnected: Boolean get() = instanceState.value != null
    }
}
