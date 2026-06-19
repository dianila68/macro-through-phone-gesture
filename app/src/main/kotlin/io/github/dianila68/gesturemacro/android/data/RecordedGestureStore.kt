package io.github.dianila68.gesturemacro.android.data

import io.github.dianila68.gesturemacro.core.recording.GestureEnvelope
import io.github.dianila68.gesturemacro.core.security.IntegritySealer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists [GestureEnvelope] objects to the `recorded_gesture` Room table.
 * Applies the same HMAC-seal pattern as [MacroStore] (threat T12).
 * Fail-closed: a broken seal disables the gesture rather than crashing.
 */
class RecordedGestureStore(
    private val dao: RecordedGestureDao,
    private val sealer: IntegritySealer?,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Live list of all stored gestures, with seal verified. */
    fun observeAll(): Flow<List<StoredGesture>> = dao.observeAll().map { entities ->
        entities.mapNotNull { entity ->
            val envelope = decodeEnvelope(entity.envelopeJson) ?: return@mapNotNull null
            val trusted = if (sealer != null) {
                sealer.verify(entity.envelopeJson, entity.integritySeal)
            } else {
                true
            }
            StoredGesture(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                envelope = envelope,
                sealValid = trusted,
            )
        }
    }

    suspend fun getById(id: String): StoredGesture? {
        val entity = dao.getById(id) ?: return null
        val envelope = decodeEnvelope(entity.envelopeJson) ?: return null
        val trusted = sealer?.verify(entity.envelopeJson, entity.integritySeal) ?: true
        return StoredGesture(id, entity.name, entity.createdAt, envelope, trusted)
    }

    suspend fun save(id: String, name: String, envelope: GestureEnvelope): StoredGesture {
        val envelopeJson = json.encodeToString(envelope)
        val seal = sealer?.seal(envelopeJson)
        val entity = RecordedGestureEntity(
            id = id,
            name = name.take(40).ifBlank { "Gesture" },
            createdAt = System.currentTimeMillis(),
            envelopeJson = envelopeJson,
            confidence = envelope.confidence,
            sampleCount = envelope.sampleCount,
            integritySeal = seal,
        )
        dao.upsert(entity)
        return StoredGesture(id, entity.name, entity.createdAt, envelope, sealValid = true)
    }

    suspend fun rename(id: String, newName: String) {
        val entity = dao.getById(id) ?: return
        dao.upsert(entity.copy(name = newName.take(40).ifBlank { "Gesture" }))
    }

    /** Deletes the gesture. Callers must cascade-disable macros referencing [id]. */
    suspend fun delete(id: String) {
        val entity = dao.getById(id) ?: return
        dao.delete(entity)
    }

    suspend fun macrosUsing(id: String): Int = dao.countMacrosUsing(id)

    private fun decodeEnvelope(json: String): GestureEnvelope? = try {
        this.json.decodeFromString<GestureEnvelope>(json)
    } catch (_: Exception) {
        null
    }
}

data class StoredGesture(
    val id: String,
    val name: String,
    val createdAt: Long,
    val envelope: GestureEnvelope,
    /** False if the HMAC seal is broken — gesture should be treated as untrusted. */
    val sealValid: Boolean,
)
