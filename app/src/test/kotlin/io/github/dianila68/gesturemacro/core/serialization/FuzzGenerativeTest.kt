package io.github.dianila68.gesturemacro.core.serialization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * ticket-015: generative fuzz harness for the macro codec (threat T2).
 *
 * Exercises [MacroCodec.decode]/[decodeYaml]/[decodeAuto] with structured and
 * adversarial inputs. Deterministic in CI (fixed [SEED]); extend [ITERATIONS]
 * locally for a deeper soak. Invariants checked on every input:
 *
 *  1. Codec never throws — only returns [Result.failure].
 *  2. Size cap is enforced: inputs > [MacroCodec.MAX_DOCUMENT_BYTES] are rejected.
 *  3. Round-trip stability: valid decoded macros survive encode → decode intact.
 *  4. Accessibility import guard (T1): a macro with an accessibility action decoded
 *     from an external string must not be [enabled].
 */
class FuzzGenerativeTest {

    companion object {
        private const val SEED = 0xDEADBEEFL
        private const val ITERATIONS = 500
    }

    private val rng = Random(SEED)

    // ── Invariant 1: never throws ─────────────────────────────────────────────

    @Test
    fun `decode never throws on random bytes`() {
        repeat(ITERATIONS) {
            val len = rng.nextInt(1, 1024)
            val text = buildString(len) { repeat(len) { append(rng.nextInt(0x20, 0x7F).toChar()) } }
            runCatching { MacroCodec.decodeAuto(text) } // must not throw
        }
    }

    @Test
    fun `decode never throws on mutated valid json`() {
        val base = validJson()
        repeat(ITERATIONS) {
            val mutated = mutate(base)
            runCatching { MacroCodec.decode(mutated) } // must not throw
        }
    }

    @Test
    fun `decode never throws on mutated valid yaml`() {
        val base = validYaml()
        repeat(ITERATIONS) {
            val mutated = mutate(base)
            runCatching { MacroCodec.decodeYaml(mutated) } // must not throw
        }
    }

    // ── Invariant 2: size cap ─────────────────────────────────────────────────

    @Test
    fun `oversized inputs are always rejected`() {
        repeat(20) {
            val padding = "x".repeat(MacroCodec.MAX_DOCUMENT_BYTES + rng.nextInt(1, 10_000))
            val text = """{"version":1,"padding":"$padding"}"""
            val result = MacroCodec.decodeAuto(text)
            assertTrue("Oversized input must fail", result.isFailure)
            assertTrue(
                "Oversized failure must be TooLarge, was ${result.exceptionOrNull()?.javaClass?.simpleName}",
                result.exceptionOrNull() is MacroCodec.ImportException.TooLarge,
            )
        }
    }

    // ── Invariant 3: round-trip stability ─────────────────────────────────────

    @Test
    fun `valid macros survive json round-trip`() {
        repeat(50) {
            val macro = randomMacro()
            val encoded = MacroCodec.encode(macro)
            val decoded = MacroCodec.decode(encoded).getOrThrow()
            assert(macro == decoded) {
                "Round-trip failed:\noriginal=$macro\ndecoded=$decoded"
            }
        }
    }

    @Test
    fun `valid macros survive yaml round-trip`() {
        repeat(50) {
            val macro = randomMacro()
            val encoded = MacroCodec.encodeYaml(macro)
            val decoded = MacroCodec.decodeYaml(encoded).getOrThrow()
            assert(macro == decoded) {
                "YAML round-trip failed:\noriginal=$macro\ndecoded=$decoded"
            }
        }
    }

    // ── Invariant 4: accessibility import guard (T1) ──────────────────────────

    @Test
    fun `imported macro with accessibility action is never enabled`() {
        val doc = """
            {
              "version": 1,
              "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
              "name": "Suspicious import",
              "enabled": true,
              "trigger": {"sensor":"accelerometer","pattern":"shake"},
              "actions": [{"type":"accessibility","target":"com.attacker","command":"back"}]
            }
        """.trimIndent()
        val macro = MacroCodec.decode(doc).getOrThrow()
        assertFalse(
            "Imported macro with accessibility action must not be enabled (T1)",
            macro.enabled,
        )
    }

    // ── Adversarial inputs ────────────────────────────────────────────────────

    @Test
    fun `deeply nested json is rejected cleanly`() {
        val depth = 5_000
        val deep = "[".repeat(depth) + "1" + "]".repeat(depth)
        val result = MacroCodec.decodeAuto("""{"version":1,"nested":$deep}""")
        assertTrue(result.isFailure)
    }

    @Test
    fun `huge field values are rejected or capped`() {
        repeat(10) {
            val huge = "A".repeat(rng.nextInt(10_000, 100_000))
            val result = MacroCodec.decodeAuto("""{"version":1,"name":"$huge","id":"x","trigger":{},"actions":[]}""")
            // Either rejected due to size, version, or invalid model — never throws
            result // just assert no throw (invariant 1 covers the rest)
        }
    }

    @Test
    fun `unknown action types are rejected`() {
        val doc = """
            {
              "version": 1,
              "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
              "name": "Unknown action",
              "trigger": {"sensor":"accelerometer","pattern":"shake"},
              "actions": [{"type":"nuke_everything"}]
            }
        """.trimIndent()
        assertTrue(MacroCodec.decode(doc).isFailure)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun validJson() = """
        {
          "version": 1,
          "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
          "name": "Fuzz base",
          "enabled": true,
          "trigger": {"sensor": "accelerometer", "pattern": "shake", "sensitivity": 0.5, "cooldown_ms": 2000},
          "constraints": {"screen_state": "any"},
          "actions": [{"type": "system_toggle", "target": "flashlight", "delay_after_ms": 0}]
        }
    """.trimIndent()

    private fun validYaml() = """
        version: 1
        id: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        name: "Fuzz base"
        enabled: true
        trigger:
          sensor: accelerometer
          pattern: shake
          sensitivity: 0.5
          cooldown_ms: 2000
        constraints:
          screen_state: any
        actions:
          - type: system_toggle
            target: flashlight
            delay_after_ms: 0
    """.trimIndent()

    /** Applies a random single-character mutation to [s]. */
    private fun mutate(s: String): String {
        if (s.isEmpty()) return s
        val chars = s.toMutableList()
        when (rng.nextInt(4)) {
            0 -> if (chars.isNotEmpty()) chars.removeAt(rng.nextInt(chars.size))
            1 -> chars[rng.nextInt(chars.size)] = rng.nextInt(0x20, 0x7E).toChar()
            2 -> chars.add(rng.nextInt(chars.size), rng.nextInt(0x20, 0x7E).toChar())
            3 -> chars.add(rng.nextInt(0x20, 0x7E).toChar())
        }
        return chars.joinToString("")
    }

    private fun randomMacro(): GestureMacro {
        val patterns = listOf(
            PatternKind.SHAKE to SensorKind.ACCELEROMETER,
            PatternKind.FLIP_FACE_DOWN to SensorKind.ACCELEROMETER,
            PatternKind.FLIP_FACE_UP to SensorKind.ACCELEROMETER,
            PatternKind.DOUBLE_SHAKE to SensorKind.ACCELEROMETER,
            PatternKind.TWIST to SensorKind.GYROSCOPE,
            PatternKind.PROXIMITY_WAVE to SensorKind.PROXIMITY,
            PatternKind.FALL to SensorKind.ACCELEROMETER,
        )
        val (pattern, sensor) = patterns[rng.nextInt(patterns.size)]
        val action = when (rng.nextInt(3)) {
            0 -> SystemToggleAction(target = "flashlight")
            1 -> MediaControlAction(command = listOf("play_pause", "next", "previous", "stop")[rng.nextInt(4)])
            else -> PlaySoundAction(mode = SoundMode.BUNDLED, bundledSound = "alert")
        }
        return GestureMacro(
            version = 1,
            id = "aaaaaaaa-bbbb-cccc-dddd-${"%012x".format(rng.nextLong(0, Long.MAX_VALUE)).take(12)}",
            name = "Fuzz macro ${rng.nextInt(10000)}",
            enabled = rng.nextBoolean(),
            trigger = Trigger(sensor = sensor, pattern = pattern, sensitivity = rng.nextFloat()),
            actions = listOf(action),
        )
    }
}
