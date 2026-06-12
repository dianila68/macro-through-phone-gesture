package io.github.dianila68.gesturemacro.core.serialization

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Strict import/export boundary for macro documents (threat T2, ADR-0002):
 * size-capped, unknown fields rejected, version dispatched before full decode,
 * all-or-nothing per document.
 */
object MacroCodec {
    const val SUPPORTED_VERSION = 1
    const val MAX_DOCUMENT_BYTES = 262_144

    sealed class ImportException(message: String) : Exception(message) {
        class TooLarge(size: Int) :
            ImportException("Document is $size bytes; limit is $MAX_DOCUMENT_BYTES")

        class UnsupportedVersion(version: Int?) :
            ImportException("Unsupported macro format version: ${version ?: "missing"} (supported: $SUPPORTED_VERSION)")

        class Invalid(message: String) : ImportException(message)
    }

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    fun decode(text: String): Result<GestureMacro> {
        val size = text.toByteArray(Charsets.UTF_8).size
        if (size > MAX_DOCUMENT_BYTES) return Result.failure(ImportException.TooLarge(size))

        val version = try {
            Json.parseToJsonElement(text).jsonObject["version"]?.jsonPrimitive?.intOrNull
        } catch (e: SerializationException) {
            return Result.failure(ImportException.Invalid("Not a valid JSON document: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return Result.failure(ImportException.Invalid("Not a JSON object: ${e.message}"))
        }
        if (version != SUPPORTED_VERSION) {
            return Result.failure(ImportException.UnsupportedVersion(version))
        }

        return try {
            Result.success(applyImportPolicy(json.decodeFromString<GestureMacro>(text)))
        } catch (e: SerializationException) {
            Result.failure(ImportException.Invalid(e.message ?: "Invalid macro document"))
        } catch (e: IllegalArgumentException) {
            Result.failure(ImportException.Invalid(e.message ?: "Invalid macro values"))
        }
    }

    fun encode(macro: GestureMacro): String = json.encodeToString(macro)

    /**
     * Threat T1: a shared macro that can drive other apps must arrive disabled,
     * regardless of the document's enabled flag. Re-enabling is an explicit user act.
     */
    private fun applyImportPolicy(macro: GestureMacro): GestureMacro =
        if (macro.actions.any { it is AccessibilityAction }) macro.copy(enabled = false) else macro
}
