@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.actions

/**
 * ticket-023/039: AccessibilityExecutor moved to `io.github.dianila68.gesturemacro.android.actions`.
 * Now accepts an [AccessibilityServiceGate] instead of accessing the service singleton directly.
 * Remaining call sites should migrate to the new package (ticket-038).
 */
@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.actions.AccessibilityExecutor",
    replaceWith = ReplaceWith(
        "AccessibilityExecutor",
        "io.github.dianila68.gesturemacro.android.actions.AccessibilityExecutor",
    ),
)
typealias AccessibilityExecutor = io.github.dianila68.gesturemacro.android.actions.AccessibilityExecutor
