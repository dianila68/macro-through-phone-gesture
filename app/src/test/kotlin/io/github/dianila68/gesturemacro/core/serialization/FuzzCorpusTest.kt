package io.github.dianila68.gesturemacro.core.serialization

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Threat T2 regression corpus (ticket-008): every seed must produce a clean
 * Result.failure — never an uncaught exception, hang, or partial import.
 */
class FuzzCorpusTest {

    private val seeds = listOf(
        "deep_nesting.json",
        "huge_numbers.json",
        "type_confusion.json",
        "duplicate_keys.json",
        "control_chars.json",
        "truncated.json",
        "empty.json",
        "unknown_action_type.json",
        "anchor_bomb.yaml",
        "billion_laughs_lite.yaml",
        "tag_injection.yaml",
        "negative_values.json",
    )

    @Test
    fun `every corpus seed fails closed without throwing`() {
        for (seed in seeds) {
            val text = checkNotNull(javaClass.getResourceAsStream("/fuzz/$seed")) {
                "Missing corpus seed $seed"
            }.bufferedReader().use { it.readText() }
            val result = MacroCodec.decodeAuto(text)
            assertTrue("Seed $seed unexpectedly imported", result.isFailure)
        }
    }
}
