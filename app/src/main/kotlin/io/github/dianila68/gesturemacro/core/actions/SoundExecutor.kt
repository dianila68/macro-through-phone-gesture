@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.actions

/**
 * ticket-036: SoundExecutor moved to `io.github.dianila68.gesturemacro.android.actions`.
 * This typealias keeps existing callers compiling; migrate to the new package (ticket-038).
 */
@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.actions.SoundExecutor",
    replaceWith = ReplaceWith(
        "SoundExecutor",
        "io.github.dianila68.gesturemacro.android.actions.SoundExecutor",
    ),
)
typealias SoundExecutor = io.github.dianila68.gesturemacro.android.actions.SoundExecutor
