package io.github.dianila68.gesturemacro.android.data

import android.content.Context
import io.github.dianila68.gesturemacro.core.recording.GestureEnvelope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * ticket-051: Repository mediating between RecordedGestureDao and the recording layer.
 * Serialises GestureEnvelope to/from JSON for Room storage.
 * Cascade-disables a gesture when the macro referencing it is deleted.
 */
class RecordedGestureStore(context: Context) {

    private val dao = MacroDatabase.get(context).recordedGestureDao()
    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<RecordedGestureEntity>> = dao.observeAll()

    fun observeEnabled(): Flow<List<Pair<RecordedGestureEntity, GestureEnvelope>>> =
        dao.observeAll().map { list ->
            list.filter { it.enabled }.mapNotNull { entity ->
                runCatching { json.decodeFromString<GestureEnvelope>(entity.envelopeJson) }
                    .getOrNull()
                    ?.let { envelope -> entity to envelope }
            }
        }

    suspend fun upsert(
        name: String,
        envelope: GestureEnvelope,
        id: String = UUID.randomUUID().toString(),
    ): String {
        val entity = RecordedGestureEntity(
            id = id,
            name = name,
            createdAt = System.currentTimeMillis(),
            envelopeJson = json.encodeToString(envelope),
            confidence = envelope.confidence,
            sampleCount = envelope.sampleCount,
        )
        dao.upsert(entity)
        return id
    }

    suspend fun delete(id: String) {
        val entity = dao.getById(id) ?: return
        dao.delete(entity)
    }

    suspend fun disable(id: String) = dao.disable(id)

    suspend fun getEnvelope(id: String): GestureEnvelope? {
        val entity = dao.getById(id) ?: return null
        return runCatching { json.decodeFromString<GestureEnvelope>(entity.envelopeJson) }.getOrNull()
    }

    suspend fun getAllEnabled(): List<Pair<RecordedGestureEntity, GestureEnvelope>> =
        dao.getAllEnabled().mapNotNull { entity ->
            runCatching { json.decodeFromString<GestureEnvelope>(entity.envelopeJson) }
                .getOrNull()
                ?.let { entity to it }
        }
}
