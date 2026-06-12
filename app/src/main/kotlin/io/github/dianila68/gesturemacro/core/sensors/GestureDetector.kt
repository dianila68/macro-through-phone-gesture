package io.github.dianila68.gesturemacro.core.sensors

/**
 * Stateful pattern detector over a sample stream. Pure Kotlin by contract so detectors
 * are testable by trace replay on the JVM (DESIGN.md, NFR-2). Single-threaded feeding.
 */
interface GestureDetector {
    val pattern: GesturePattern

    /** Returns an event when this sample completes the pattern, null otherwise. */
    fun feed(sample: SensorSample): GestureEvent?

    fun reset()
}
