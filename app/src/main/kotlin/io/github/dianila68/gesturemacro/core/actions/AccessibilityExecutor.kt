@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.actions

/**
 * ticket-023: AccessibilityExecutor moved to `io.github.dianila68.gesturemacro.android.actions`.
 * This typealias keeps existing callers compiling; migrate to the new package (ticket-024).
 */

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.actions.AccessibilityExecutor",
    replaceWith = ReplaceWith(
        "AccessibilityExecutor",
        "io.github.dianila68.gesturemacro.android.actions.AccessibilityExecutor",
    ),
)
typealias AccessibilityExecutor = io.github.dianila68.gesturemacro.android.actions.AccessibilityExecutor
