package io.github.dianila68.gesturemacro.core.sensors

/**
 * ticket-047: Fires SIGNIFICANT_MOTION when the Android TYPE_SIGNIFICANT_MOTION
 * one-shot trigger fires.
 *
 * IMPORTANT: TYPE_SIGNIFICANT_MOTION uses TriggerEventListener (one-shot), NOT
 * SensorEventListener. The Android SensorManager layer (AndroidSensorStream) must
 * call requestTriggerSensor() and re-arm after each event.
 *
 * This detector is a marker / config holder — the actual one-shot re-arm logic
 * lives in AndroidSensorStream. It receives synthetic SensorSamples from there.
 */
class SignificantMotionDetector : GestureDetector {
    override val pattern: GesturePattern = GesturePattern.SIGNIFICANT_MOTION
    override val sensor: SensorType = SensorType.SIGNIFICANT_MOTION

    override fun feed(sample: SensorSample): GestureEvent? {
        // v[0] == 1.0 means the trigger fired (synthetic sample from AndroidSensorStream)
        if (sample.sensor != SensorType.SIGNIFICANT_MOTION) return null
        return GestureEvent(
            pattern = GesturePattern.SIGNIFICANT_MOTION,
            t = sample.t,
            confidence = 1f,
        )
    }

    override fun reset() { /* stateless */ }
}
