package io.github.dianila68.gesturemacro.core.recording

import io.github.dianila68.gesturemacro.core.sensors.SensorSample

data class ValidationResult(
    val passed: Boolean,
    val matchScore: Float,
    val diagnostics: List<String>
)

class ReplayValidator(envelope: GestureEnvelope, threshold: Float = 0.70f) {

    private val detector = RecordedGestureDetector(envelope, matchThreshold = threshold)

    fun validate(samples: List<SensorSample>): ValidationResult {
        detector.reset()
        val diagnostics = mutableListOf<String>()

        for (sample in samples) {
            val event = detector.feed(sample) ?: continue
            return ValidationResult(
                passed = true,
                matchScore = event.confidence,
                diagnostics = diagnostics +
                    "Matched at t=${sample.t}ms — score ${"%.0f".format(event.confidence * 100)}%"
            )
        }

        diagnostics += "No match across ${samples.size} samples"
        return ValidationResult(passed = false, matchScore = 0f, diagnostics = diagnostics)
    }
}
