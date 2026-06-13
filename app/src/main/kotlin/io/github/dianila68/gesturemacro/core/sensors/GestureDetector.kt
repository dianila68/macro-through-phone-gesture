package io.github.dianila68.gesturemacro.core.sensors

/**
 * Stateful pattern detector over a sample stream. Pure Kotlin by contract so detectors
 * are testable by trace replay on the JVM (DESIGN.md, NFR-2). Single-threaded feeding.
 */
interface GestureDetector {
    val pattern: GesturePattern

    /** The sensor whose samples this detector consumes; lets the pipeline subscribe minimally. */
    val sensor: SensorType

    /** Returns an event when this sample completes the pattern, null otherwise. */
    fun feed(sample: SensorSample): GestureEvent?

    fun reset()
}
