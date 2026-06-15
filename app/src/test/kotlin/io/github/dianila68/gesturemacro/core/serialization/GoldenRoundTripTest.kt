package io.github.dianila68.gesturemacro.core.serialization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * ticket-022: golden format fixtures guard.
 *
 * Each golden file under test/resources/golden/ is the canonical JSON
 * representation of a specific [GestureMacro]. Tests assert:
 *   1. The file decodes without error (format contract is honored).
 *   2. The decoded model matches the expected Kotlin value (no silent drift).
 *   3. Encoding the decoded model and re-decoding produces the same model
 *      (round-trip identity; format version is stable across codec ↔ model).
 *
 * When the schema bumps to v2 these tests will fail intentionally — the failure
 * is the signal that golden files need to be updated alongside the version bump.
 */
class GoldenRoundTripTest {

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("golden/$name")) {
            "Golden fixture not found: golden/$name"
        }.bufferedReader().readText()

    // ── shake-flashlight ──────────────────────────────────────────────────────

    private val shakeFlashlight = GestureMacro(
        version = 1,
        id = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        name = "Shake to toggle flashlight",
        enabled = true,
        trigger = Trigger(
            sensor = SensorKind.ACCELEROMETER,
            pattern = PatternKind.SHAKE,
            sensitivity = 0.5f,
            cooldownMs = 2_000,
        ),
        actions = listOf(SystemToggleAction(target = "flashlight")),
    )

    @Test
    fun `shake-flashlight golden decodes correctly`() {
        val macro = MacroCodec.decode(resource("shake-flashlight-v1.json")).getOrThrow()
        assertEquals(shakeFlashlight, macro)
    }

    @Test
    fun `shake-flashlight round-trips`() {
        val decoded = MacroCodec.decode(resource("shake-flashlight-v1.json")).getOrThrow()
        val reDecoded = MacroCodec.decode(MacroCodec.encode(decoded)).getOrThrow()
        assertEquals(decoded, reDecoded)
    }

    // ── fall-location-alert ───────────────────────────────────────────────────

    private val fallLocationAlert = GestureMacro(
        version = 1,
        id = "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        name = "Fall → location alert",
        enabled = true,
        trigger = Trigger(
            sensor = SensorKind.ACCELEROMETER,
            pattern = PatternKind.FALL,
            sensitivity = 0.5f,
            cooldownMs = 30_000,
        ),
        actions = listOf(
            PlaySoundAction(mode = SoundMode.BUNDLED, bundledSound = "alert"),
            LocationAlertAction(
                contactName = "Alice",
                contactPhone = "+15555550100",
                message = "I may have fallen",
                countdownSec = 15,
            ),
        ),
    )

    @Test
    fun `fall-location-alert golden decodes correctly`() {
        val macro = MacroCodec.decode(resource("fall-location-alert-v1.json")).getOrThrow()
        assertEquals(fallLocationAlert, macro)
    }

    @Test
    fun `fall-location-alert round-trips`() {
        val decoded = MacroCodec.decode(resource("fall-location-alert-v1.json")).getOrThrow()
        val reDecoded = MacroCodec.decode(MacroCodec.encode(decoded)).getOrThrow()
        assertEquals(decoded, reDecoded)
    }

    // ── proximity-wave-tts ────────────────────────────────────────────────────

    private val proximityWaveTts = GestureMacro(
        version = 1,
        id = "c3d4e5f6-a7b8-9012-cdef-012345678902",
        name = "Wave → speak time window only",
        enabled = true,
        trigger = Trigger(
            sensor = SensorKind.PROXIMITY,
            pattern = PatternKind.PROXIMITY_WAVE,
            sensitivity = 0.6f,
            cooldownMs = 2_000,
        ),
        constraints = Constraints(
            screenState = ScreenState.ON,
            timeWindow = TimeWindow("08:00", "22:00"),
        ),
        actions = listOf(
            PlaySoundAction(mode = SoundMode.TTS, ttsText = "Good morning", delayAfterMs = 500),
        ),
    )

    @Test
    fun `proximity-wave-tts golden decodes correctly`() {
        val macro = MacroCodec.decode(resource("proximity-wave-tts-v1.json")).getOrThrow()
        assertEquals(proximityWaveTts, macro)
    }

    @Test
    fun `proximity-wave-tts round-trips`() {
        val decoded = MacroCodec.decode(resource("proximity-wave-tts-v1.json")).getOrThrow()
        val reDecoded = MacroCodec.decode(MacroCodec.encode(decoded)).getOrThrow()
        assertEquals(decoded, reDecoded)
    }

    // ── version field is present and correct ──────────────────────────────────

    @Test
    fun `all golden fixtures carry version 1`() {
        listOf("shake-flashlight-v1.json", "fall-location-alert-v1.json", "proximity-wave-tts-v1.json")
            .forEach { name ->
                val macro = MacroCodec.decode(resource(name)).getOrThrow()
                assertEquals("$name must carry version 1", 1, macro.version)
            }
    }

    // ── format version bump sentinel ──────────────────────────────────────────
    // If SUPPORTED_VERSION increases, this test fails intentionally — update goldens.

    @Test
    fun `codec supported version is 1 (bump policy sentinel)`() {
        assertEquals(
            "Format version bumped to ${MacroCodec.SUPPORTED_VERSION}; " +
                "update golden fixtures in test/resources/golden/ and this sentinel.",
            1,
            MacroCodec.SUPPORTED_VERSION,
        )
    }

    @Test
    fun `golden fixture files are non-empty and parseable`() {
        listOf("shake-flashlight-v1.json", "fall-location-alert-v1.json", "proximity-wave-tts-v1.json")
            .forEach { name ->
                val text = resource(name)
                assertNotNull("$name must not be null", text)
                assert(text.isNotBlank()) { "$name must not be blank" }
                MacroCodec.decode(text).getOrThrow()
            }
    }
}
