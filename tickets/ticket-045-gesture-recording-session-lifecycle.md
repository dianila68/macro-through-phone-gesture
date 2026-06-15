# ticket-045: Gesture recording session — lifecycle & entry point

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-003, ticket-011, ticket-013

## Description

Introduce a **GestureRecordingSession** component that manages the lifecycle of a
user-driven gesture-recording flow. The session is the backbone all other recording
tickets (046–052) plug into. It owns state transitions, cancellation, timeout, and
the contract between the UI and the sampling engine.

This is a pure-JVM component (no Android imports) so it can be unit-tested without a
device. The Android sensor wiring lives in ticket-046.

## State machine

```
IDLE → COUNTDOWN → RECORDING → INTER_SAMPLE_PAUSE → RECORDING (repeat)
                                          ↓ (N samples collected)
                               ANALYSING → READY | INSUFFICIENT_DATA
                               (any state) → CANCELLED
                               (any state) → TIMED_OUT
```

- **COUNTDOWN:** 3-second visual countdown before the first movement capture window
  opens (gives the user time to raise the phone to the starting position).
- **RECORDING:** one active capture window; maximum duration is configurable
  (`maxWindowMs`, default 3 000 ms). Window closes early if a stillness tail is
  detected (the user has stopped moving).
- **INTER_SAMPLE_PAUSE:** brief rest between repetitions (default 1 500 ms); engine
  shows which repetition is next.
- **ANALYSING:** CPU-bound envelope computation (ticket-048) runs off the main thread.
- **READY:** envelope produced; preview data available for the review UI (ticket-050).
- **INSUFFICIENT_DATA:** fewer than `minSamples` valid windows were collected — user
  must try again or lower the required repetitions.

## Core types

```kotlin
data class RecordingConfig(
    val requiredSamples: Int = 5,   // how many repetitions to ask for
    val minSamples: Int = 3,        // accept result with at least this many
    val maxWindowMs: Long = 3_000,
    val interSamplePauseMs: Long = 1_500,
    val countdownMs: Long = 3_000,
    val sensors: Set<SensorChannel> = setOf(SensorChannel.ACCELEROMETER, SensorChannel.GYROSCOPE),
)

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Countdown(val remainingMs: Long) : RecordingState
    data class Recording(val sampleIndex: Int, val elapsedMs: Long) : RecordingState
    data class InterSamplePause(val nextSampleIndex: Int, val remainingMs: Long) : RecordingState
    data object Analysing : RecordingState
    data class Ready(val envelope: GestureEnvelope) : RecordingState
    data object InsufficientData : RecordingState
    data object Cancelled : RecordingState
    data object TimedOut : RecordingState
}

interface GestureRecordingSession {
    val state: StateFlow<RecordingState>
    fun start(config: RecordingConfig)
    fun cancel()
}
```

`SensorChannel` is an enum already reachable from the sensor module; add
`GYROSCOPE`, `ROTATION_VECTOR`, `GRAVITY` entries if not present.

## Acceptance criteria

- [ ] `GestureRecordingSession` interface + `DefaultGestureRecordingSession` implementation in
  `core/recording/`.
- [ ] State transitions driven by a `CoroutineScope` + `delay`; no Android imports.
- [ ] `cancel()` is idempotent and transitions to `CANCELLED` from any non-terminal state.
- [ ] Timeout: if `RECORDING` window exceeds `maxWindowMs` the window closes and is
  counted as a valid (possibly low-quality) sample, then continues to `INTER_SAMPLE_PAUSE`
  or `ANALYSING`.
- [ ] Unit tests: full happy path (5 samples → READY), early cancel, timeout, insufficient
  data path (< minSamples windows produce usable data).
- [ ] No raw sensor data is stored in this component — it only orchestrates; raw samples
  live in the `SampleBuffer` (ticket-046).
