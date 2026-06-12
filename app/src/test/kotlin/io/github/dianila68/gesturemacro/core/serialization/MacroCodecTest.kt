package io.github.dianila68.gesturemacro.core.serialization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroCodecTest {

    private val exampleMacro = GestureMacro(
        version = 1,
        id = "3f1a2b6c-9d4e-4f0a-8b7c-1e2d3c4b5a69",
        name = "Flip to pause Spotify",
        enabled = true,
        trigger = Trigger(
            sensor = SensorKind.ACCELEROMETER,
            pattern = PatternKind.FLIP_FACE_DOWN,
            sensitivity = 0.6f,
            cooldownMs = 3_000,
        ),
        actions = listOf(MediaControlAction(command = "play_pause", target = "com.spotify.music")),
    )

    @Test
    fun `json round trip is identity`() {
        val decoded = MacroCodec.decode(MacroCodec.encode(exampleMacro))
        assertEquals(exampleMacro, decoded.getOrThrow())
    }

    @Test
    fun `architecture example document decodes`() {
        // Mirrors the example macro in docs/ARCHITECTURE.md
        val doc = """
            {
              "version": 1,
              "id": "3f1a2b6c-9d4e-4f0a-8b7c-1e2d3c4b5a69",
              "name": "Flip to pause Spotify",
              "enabled": true,
              "trigger": { "sensor": "accelerometer", "pattern": "flip_face_down", "sensitivity": 0.6, "cooldown_ms": 3000 },
              "constraints": { "screen_state": "any" },
              "actions": [
                { "type": "media_control", "target": "com.spotify.music", "command": "play_pause" }
              ]
            }
        """.trimIndent()
        val macro = MacroCodec.decode(doc).getOrThrow()
        assertEquals(exampleMacro, macro)
    }

    @Test
    fun `unknown fields are rejected`() {
        val doc = MacroCodec.encode(exampleMacro)
            .replaceFirst("\"version\": 1,", "\"version\": 1, \"evil_extra\": true,")
        val result = MacroCodec.decode(doc)
        assertTrue(result.exceptionOrNull() is MacroCodec.ImportException.Invalid)
    }

    @Test
    fun `unsupported version is refused before full decode`() {
        val doc = MacroCodec.encode(exampleMacro).replaceFirst("\"version\": 1", "\"version\": 99")
        val error = MacroCodec.decode(doc).exceptionOrNull()
        assertTrue(error is MacroCodec.ImportException.UnsupportedVersion)
    }

    @Test
    fun `missing version is refused`() {
        val error = MacroCodec.decode("""{"id":"x","name":"y"}""").exceptionOrNull()
        assertTrue(error is MacroCodec.ImportException.UnsupportedVersion)
    }

    @Test
    fun `garbage input fails with invalid, not crash`() {
        assertTrue(MacroCodec.decode("not json at all {").isFailure)
    }

    @Test
    fun `oversized document is rejected`() {
        val padding = "x".repeat(MacroCodec.MAX_DOCUMENT_BYTES + 1)
        val error = MacroCodec.decode("{\"version\": 1, \"name\": \"$padding\"}").exceptionOrNull()
        assertTrue(error is MacroCodec.ImportException.TooLarge)
    }

    @Test
    fun `imported accessibility macros arrive disabled regardless of enabled flag`() {
        val sneaky = exampleMacro.copy(
            enabled = true,
            actions = listOf(AccessibilityAction(target = "com.bank.app", command = "click_confirm")),
        )
        val imported = MacroCodec.decode(MacroCodec.encode(sneaky)).getOrThrow()
        assertFalse(imported.enabled)
    }

    @Test
    fun `validation errors carry the offending field`() {
        val doc = MacroCodec.encode(exampleMacro).replaceFirst("\"sensitivity\": 0.6", "\"sensitivity\": 7.5")
        val error = MacroCodec.decode(doc).exceptionOrNull()
        assertTrue(error is MacroCodec.ImportException.Invalid)
        assertTrue(error!!.message!!.contains("sensitivity"))
    }

    @Test
    fun `invalid time window is rejected`() {
        val bad = runCatching { TimeWindow(start = "25:99", end = "06:00") }
        assertTrue(bad.isFailure)
    }
}
