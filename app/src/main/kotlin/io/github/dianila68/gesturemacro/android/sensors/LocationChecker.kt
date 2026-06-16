package io.github.dianila68.gesturemacro.android.sensors

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import io.github.dianila68.gesturemacro.core.serialization.LocationConstraint
import io.github.dianila68.gesturemacro.core.serialization.SavedLocation

/**
 * ticket-049: Checks whether the current device location satisfies a
 * LocationConstraint. Uses getLastKnownLocation — no geofencing, no wake.
 *
 * Requires ACCESS_FINE_LOCATION. Background use requires ACCESS_BACKGROUND_LOCATION (API 29+).
 */
class LocationChecker(private val context: Context) {

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun isSatisfied(
        constraint: LocationConstraint,
        savedLocations: Map<String, SavedLocation>,
    ): Boolean {
        if (constraint.allowedLocationIds.isEmpty()) return true
        val location = getLastLocation() ?: return true  // fail-open
        val allowed = constraint.allowedLocationIds.mapNotNull { savedLocations[it] }
        return allowed.any { saved ->
            val result = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                saved.latitudeDeg, saved.longitudeDeg,
                result,
            )
            result[0] <= saved.radiusMeters
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getLastLocation(): android.location.Location? {
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            manager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }
}
