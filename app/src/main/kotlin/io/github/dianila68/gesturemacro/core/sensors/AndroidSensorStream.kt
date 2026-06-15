@file:Suppress("DEPRECATION")

package io.github.dianila68.gesturemacro.core.sensors

/**
 * ticket-023: [AndroidSensorStream] has moved to
 * `io.github.dianila68.gesturemacro.android.sensors.AndroidSensorStream`.
 *
 * This typealias keeps existing callers (GestureCaptureService) compiling
 * without changes in this PR; a follow-up (ticket-024) will migrate all
 * remaining call sites to the new package and delete this shim.
 */
@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.sensors.AndroidSensorStream",
    replaceWith = ReplaceWith(
        "AndroidSensorStream",
        "io.github.dianila68.gesturemacro.android.sensors.AndroidSensorStream",
    ),
)
typealias AndroidSensorStream = io.github.dianila68.gesturemacro.android.sensors.AndroidSensorStream
