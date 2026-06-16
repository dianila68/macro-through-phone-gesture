package io.github.dianila68.gesturemacro.android.data

import android.content.Context
import android.content.SharedPreferences
import io.github.dianila68.gesturemacro.core.serialization.SavedLocation
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * ticket-049 / ticket-052: Persists user-named locations to SharedPreferences.
 * Uses JSON serialization via kotlinx.serialization.
 */
class SavedLocationsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("saved_locations", Context.MODE_PRIVATE)

    fun getAll(): List<SavedLocation> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            Json.decodeFromString(ListSerializer(SavedLocation.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    fun save(location: SavedLocation) {
        val current = getAll().toMutableList()
        current.removeAll { it.id == location.id }
        current.add(location)
        prefs.edit().putString(KEY, Json.encodeToString(ListSerializer(SavedLocation.serializer()), current)).apply()
    }

    fun delete(locationId: String) {
        val current = getAll().filter { it.id != locationId }
        prefs.edit().putString(KEY, Json.encodeToString(ListSerializer(SavedLocation.serializer()), current)).apply()
    }

    fun asMap(): Map<String, SavedLocation> = getAll().associateBy { it.id }

    companion object {
        private const val KEY = "locations_json"
    }
}
