package io.github.dianila68.gesturemacro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.android.data.SavedLocationsRepository
import io.github.dianila68.gesturemacro.core.serialization.SavedLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ticket-052: ViewModel for saved places management screen.
 */
class SavedPlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SavedLocationsRepository(application)

    private val _places = MutableStateFlow<List<SavedLocation>>(emptyList())
    val places: StateFlow<List<SavedLocation>> = _places.asStateFlow()

    init { load() }

    private fun load() {
        _places.value = repo.getAll()
    }

    fun add(name: String, lat: Double, lon: Double, radiusMeters: Float = 100f) {
        viewModelScope.launch {
            val place = SavedLocation(
                id = UUID.randomUUID().toString(),
                name = name,
                latitudeDeg = lat,
                longitudeDeg = lon,
                radiusMeters = radiusMeters,
            )
            repo.save(place)
            load()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id)
            load()
        }
    }
}
