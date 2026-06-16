# ticket-046: Recording sensor capture — SampleBuffer & Android wiring

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-045, ticket-003

## Description

Wire the Android `SensorManager` into the recording session so that real sensor
events are collected into a **SampleBuffer** during each RECORDING window. This is
the Android-layer counterpart of the pure-JVM session lifecycle (ticket-045).

The recording system must not interfere with the live gesture-detection pipeline. Both
can coexist: the `SensorStream` fan-outs to both the `TriggerLibrary` subscribers and
the `SampleBuffer` during a recording session.

## SampleBuffer design

```kotlin
data class SensorFrame(
    val timestampNs: Long,
    val channel: SensorChannel,
    val values: FloatArray,  // length depends on sensor (3 for accel/gyro, 4 for quat, etc.)
)

/** Accumulates frames for a single recording window (one repetition). */
class SampleWindow(val index: Int) {
    val frames: MutableList<SensorFrame> = mutableListOf()
    var startNs: Long = 0L
    var endNs: Long = 0L
    val durationMs: Long get() = (endNs - startNs) / 1_000_000L
}

class SampleBuffer {
    val windows: List<SampleWindow>
    fun openWindow(index: Int)
    fun appendFrame(frame: SensorFrame)
    fun closeWindow()
    fun clear()
}
```

Windows map 1-to-1 to repetitions. A window is open for exactly the RECORDING
state duration; frames arrive from `SensorStream` via a `Flow.collect` launched
in the recording `CoroutineScope`.

## Stillness detection (early window close)

After at least 300 ms of data, compute a rolling variance over the last 200 ms of
accelerometer magnitude. If variance < `stillnessVarianceThreshold` (configurable,
default 0.02 m²/s⁴) for 150 ms continuously, signal the session to close the window
early. This lets short, snappy gestures finish without waiting for `maxWindowMs`.

## Acceptance criteria

- [ ] `SampleBuffer`, `SampleWindow`, `SensorFrame` data types in `core/recording/`.
- [ ] `RecordingSensorCollector` (Android module) bridges `SensorStream` → `SampleBuffer`.
  Registers only for channels listed in `RecordingConfig.sensors`; unregisters on cancel/done.
- [ ] Stillness detection emits an event the session lifecycle (045) can subscribe to.
- [ ] Existing live detection pipeline is unaffected: instrument with a test that asserts
  `TriggerLibrary` still fires shake while a recording session is active in parallel.
- [ ] Unit test (JVM): `SampleBuffer` window open/close/append; stillness detector triggers
  on synthetic low-variance trace.
- [ ] Frame rate: request `SensorManager.SENSOR_DELAY_GAME` (≈ 50 Hz) for all recording
  channels. Document the Hz in a comment; don't hard-code numbers elsewhere.
