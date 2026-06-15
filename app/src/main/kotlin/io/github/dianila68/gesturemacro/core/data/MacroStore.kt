@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.data

/**
 * ticket-037: MacroStore moved to `io.github.dianila68.gesturemacro.android.data.MacroStore`.
 * The typealias makes `core.data.MacroStore` an alias for the same singleton object, so all
 * existing callers continue to access the one and only MacroStore without changes (ticket-038).
 */
@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.data.MacroStore",
    replaceWith = ReplaceWith("MacroStore", "io.github.dianila68.gesturemacro.android.data.MacroStore"),
)
typealias MacroStore = io.github.dianila68.gesturemacro.android.data.MacroStore
