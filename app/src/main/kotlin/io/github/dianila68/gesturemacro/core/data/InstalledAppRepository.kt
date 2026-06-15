@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.data

/**
 * ticket-037: InstalledAppRepository moved to `io.github.dianila68.gesturemacro.android.data`.
 * This typealias keeps existing callers compiling; migrate to the new package (ticket-038).
 */
@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.data.InstalledAppRepository",
    replaceWith = ReplaceWith(
        "InstalledAppRepository",
        "io.github.dianila68.gesturemacro.android.data.InstalledAppRepository",
    ),
)
typealias InstalledAppRepository = io.github.dianila68.gesturemacro.android.data.InstalledAppRepository
