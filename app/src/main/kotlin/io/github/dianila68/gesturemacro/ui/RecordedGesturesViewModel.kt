package io.github.dianila68.gesturemacro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.android.data.RecordedGestureEntity
import io.github.dianila68.gesturemacro.android.data.RecordedGestureStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ticket-053: Manages the list of saved recorded gestures.
 */
class RecordedGesturesViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RecordedGestureStore(application)

    val gestures: StateFlow<List<RecordedGestureEntity>> = store.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    fun toggleEnabled(entity: RecordedGestureEntity) {
        viewModelScope.launch {
            if (entity.enabled) {
                store.disable(entity.id)
            } else {
                store.upsert(
                    name = entity.name,
                    envelope = store.getEnvelope(entity.id) ?: return@launch,
                    id = entity.id,
                )
            }
        }
    }
}
