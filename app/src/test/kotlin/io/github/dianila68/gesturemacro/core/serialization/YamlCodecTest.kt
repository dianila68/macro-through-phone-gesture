package io.github.dianila68.gesturemacro.core.serialization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YamlCodecTest {

    private val macro = GestureMacro(
        version = 1,
        id = "yaml-test-macro",
        name = "Flip to pause",
        trigger = Trigger(sensor = SensorKind.ACCELEROMETER, pattern = PatternKind.FLIP_FACE_DOWN),
        actions = listOf(MediaControlAction(command = "play_pause", target = "com.spotify.music")),
    )

    @Test
    fun `yaml round trip is identity`() {
        val decoded = MacroCodec.decodeYaml(MacroCodec.encodeYaml(macro))
        assertEquals(macro, decoded.getOrThrow())
    }

    @Test
    fun `yaml and json decode to the same model`() {
        val fromYaml = MacroCodec.decodeYaml(MacroCodec.encodeYaml(macro)).getOrThrow()
        val fromJson = MacroCodec.decode(MacroCodec.encode(macro)).getOrThrow()
        assertEquals(fromJson, fromYaml)
    }

    @Test
    fun `handwritten yaml document decodes`() {
        val doc = """
            version: 1
            id: "yaml-test-macro"
            name: "Flip to pause"
            enabled: true
            trigger:
              sensor: "accelerometer"
              pattern: "flip_face_down"
            actions:
              - type: "media_control"
                command: "play_pause"
                target: "com.spotify.music"
        """.trimIndent()
        assertEquals(macro, MacroCodec.decodeYaml(doc).getOrThrow())
    }

    @Test
    fun `imported yaml accessibility macros arrive disabled`() {
        val sneaky = macro.copy(
            actions = listOf(AccessibilityAction(target = "com.bank.app", command = "back")),
        )
        val imported = MacroCodec.decodeYaml(MacroCodec.encodeYaml(sneaky)).getOrThrow()
        assertFalse(imported.enabled)
    }

    @Test
    fun `yaml anchors and aliases are rejected`() {
        val bomb = """
            version: 1
            id: &a "x"
            name: *a
            trigger:
              sensor: "accelerometer"
              pattern: "shake"
            actions:
              - type: "system_toggle"
                target: "flashlight"
        """.trimIndent()
        assertTrue(MacroCodec.decodeYaml(bomb).isFailure)
    }

    @Test
    fun `malformed yaml fails without crashing`() {
        assertTrue(MacroCodec.decodeYaml("::: not yaml {{{").isFailure)
    }

    @Test
    fun `unsupported yaml version is refused`() {
        val doc = MacroCodec.encodeYaml(macro).replaceFirst("version: 1", "version: 99")
        val error = MacroCodec.decodeYaml(doc).exceptionOrNull()
        assertTrue(error is MacroCodec.ImportException.UnsupportedVersion)
    }
}
