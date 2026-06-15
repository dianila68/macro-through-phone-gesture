@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.actions

/**
 * ticket-023: Executors moved to `io.github.dianila68.gesturemacro.android.actions`.
 * These typealiases keep existing callers compiling; migrate to the new package (ticket-024).
 */

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.actions.FlashlightExecutor",
    replaceWith = ReplaceWith(
        "FlashlightExecutor",
        "io.github.dianila68.gesturemacro.android.actions.FlashlightExecutor",
    ),
)
typealias FlashlightExecutor = io.github.dianila68.gesturemacro.android.actions.FlashlightExecutor

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.actions.MediaControlExecutor",
    replaceWith = ReplaceWith(
        "MediaControlExecutor",
        "io.github.dianila68.gesturemacro.android.actions.MediaControlExecutor",
    ),
)
typealias MediaControlExecutor = io.github.dianila68.gesturemacro.android.actions.MediaControlExecutor

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.actions.IntentExecutor",
    replaceWith = ReplaceWith(
        "IntentExecutor",
        "io.github.dianila68.gesturemacro.android.actions.IntentExecutor",
    ),
)
typealias IntentExecutor = io.github.dianila68.gesturemacro.android.actions.IntentExecutor
