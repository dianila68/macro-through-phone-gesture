package io.github.dianila68.gesturemacro.core.serialization

import kotlinx.serialization.Serializable

/** ticket-049: A named geographic location used as a macro constraint. */
@Serializable
data class SavedLocation(
    val id: String,
    val name: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val radiusMeters: Float = 100f,
)

/**
 * Location constraint: macro is eligible only when device is within
 * radiusMeters of one of the allowedLocationIds. Empty = everywhere (default).
 */
@Serializable
data class LocationConstraint(
    val allowedLocationIds: List<String> = emptyList(),
)
