package io.github.dianila68.gesturemacro.android.actions

import android.accessibilityservice.AccessibilityService

/**
 * ticket-039: Seam between [AccessibilityExecutor] and [MacroAccessibilityService].
 * Injecting this interface instead of accessing the service singleton directly makes
 * the executor testable without a live service instance.
 */
fun interface AccessibilityServiceGate {
    /** Returns the live [AccessibilityService] instance, or null if not connected. */
    fun service(): AccessibilityService?
}
