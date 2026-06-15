@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.actions

/**
 * ticket-036: LocationAlertExecutor moved to `io.github.dianila68.gesturemacro.android.actions`.
 * This typealias keeps existing callers compiling; migrate to the new package (ticket-038).
 */
@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.actions.LocationAlertExecutor",
    replaceWith = ReplaceWith(
        "LocationAlertExecutor",
        "io.github.dianila68.gesturemacro.android.actions.LocationAlertExecutor",
    ),
)
typealias LocationAlertExecutor = io.github.dianila68.gesturemacro.android.actions.LocationAlertExecutor
