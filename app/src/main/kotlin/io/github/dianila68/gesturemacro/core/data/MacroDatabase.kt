@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.data

/**
 * ticket-037: Room classes moved to `io.github.dianila68.gesturemacro.android.data`.
 * These typealiases keep existing callers compiling; migrate imports to the new package (ticket-038).
 */

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.data.MacroEntity",
    replaceWith = ReplaceWith("MacroEntity", "io.github.dianila68.gesturemacro.android.data.MacroEntity"),
)
typealias MacroEntity = io.github.dianila68.gesturemacro.android.data.MacroEntity

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.data.ExecutionLogEntity",
    replaceWith = ReplaceWith("ExecutionLogEntity", "io.github.dianila68.gesturemacro.android.data.ExecutionLogEntity"),
)
typealias ExecutionLogEntity = io.github.dianila68.gesturemacro.android.data.ExecutionLogEntity

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.data.MacroDao",
    replaceWith = ReplaceWith("MacroDao", "io.github.dianila68.gesturemacro.android.data.MacroDao"),
)
typealias MacroDao = io.github.dianila68.gesturemacro.android.data.MacroDao

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.data.MacroDatabase",
    replaceWith = ReplaceWith("MacroDatabase", "io.github.dianila68.gesturemacro.android.data.MacroDatabase"),
)
typealias MacroDatabase = io.github.dianila68.gesturemacro.android.data.MacroDatabase
