# Component Design

> SDLC stage: **Design** · Status: v1 (2026-06-11)
> Inputs: [REQUIREMENTS.md](REQUIREMENTS.md), [THREAT_MODEL.md](THREAT_MODEL.md), [ARCHITECTURE.md](ARCHITECTURE.md)
> Scope: interface-level design for M1–M3 components. Implementation tickets must conform or amend this doc in the same PR.

## Data flow

```
SensorManager ──samples──▶ SensorStream ──windows──▶ GestureDetector ──GestureEvent──▶ MacroEngine
                                                                                          │
                                                  ┌── constraints OK? ◀───────────────────┤
                                                  ▼                                       │
                                            ActionDispatcher ──▶ ActionExecutor(s) ──▶ result log (Room)
                                                  │
                              (accessibility) ────▶ MacroAccessibilityService (if connected)
```

## Core contracts (package `core/`)

```kotlin
// core/sensors — JVM-pure except SensorStream impl
interface SensorStream {                       // impl: AndroidSensorStream(SensorManager)
    fun samples(spec: SamplingSpec): Flow<SensorSample>   // cold; cancels listener on collect end
}
data class SensorSample(val sensor: SensorType, val t: Long, val v: FloatArray)

interface GestureDetector {                    // pure: trace-replay testable (NFR-2)
    val pattern: GesturePattern
    fun feed(sample: SensorSample): GestureEvent?         // stateful, single-threaded by contract
    fun reset()
}
data class GestureEvent(val pattern: GesturePattern, val t: Long, val confidence: Float)

// core/engine
class MacroEngine(
    private val repo: MacroRepository,
    private val dispatcher: ActionDispatcher,
    private val clock: Clock,                  // injected for testability
) {
    fun onGesture(e: GestureEvent)             // match → constraints → cooldown → dispatch
    val state: StateFlow<EngineState>          // Running / Paused / Degraded(reason)
}

// core/actions
interface ActionExecutor {
    val type: ActionType                       // SYSTEM_TOGGLE, MEDIA_CONTROL, INTENT, ACCESSIBILITY
    suspend fun execute(action: MacroAction): ExecResult   // never throws; returns typed failure
}
class ActionDispatcher(executors: Map<ActionType, ActionExecutor>) {
    suspend fun run(macro: Macro): List<ExecResult>        // sequential, honors delay_after_ms,
}                                                          // stops on Fatal, logs every result (T4)

// core/data
interface MacroRepository {                    // impl: Room
    fun enabledMacros(): Flow<List<Macro>>
    suspend fun recordExecution(log: ExecutionLog)         // threat T4 audit trail
    suspend fun upsert(macro: Macro)                       // enforces T1: accessibility ⇒ disabled on import
}

// core/serialization
sealed interface MacroDocument { val version: Int }
object MacroCodec {
    fun decode(bytes: ByteArray, format: Format): Result<Macro>   // strict; size-capped (T2)
    fun encode(macro: Macro, format: Format): ByteArray
}
```

## Services (package `service/`)

- **`GestureCaptureService : Service`** — owns a `CoroutineScope`; wires `SensorStream → detectors → MacroEngine`. Holds `WakeLockGuard` (acquire with hard timeout, auto-release on window close — API makes indefinite hold impossible). Persists heartbeat every 60 s.
- **`MacroAccessibilityService : AccessibilityService`** — exposes `connectionState: StateFlow<Boolean>` via a process-local singleton; `AccessibilityExecutor` checks it before dispatch (NFR-7). No other IPC surface (T3).

## UI (package `app/`, M3)

Single-activity Compose. Screens: MacroList → MacroEditor (TriggerPicker, ActionListBuilder, ConstraintsSection), Diagnostics (heartbeat gaps, execution log), Onboarding (notification → battery exemption → accessibility, each with plain-language disclosure per NFR-8/T11). State: `ViewModel` + `StateFlow`; no logic in composables.

## Error-handling rules

1. Executors return `ExecResult.Failure(reason, fatal)` — never throw across the dispatcher boundary.
2. Detector exceptions reset the detector, log a diagnostic, never kill the service.
3. Import errors carry JSON-pointer-style paths (`/actions/2/type`) for field-level UI messages.

## Threading model

- One single-threaded dispatcher for detector feeding (ordering guarantee).
- Engine + executors on `Dispatchers.Default`; Room on its own executor; `Main` only in UI.

## Design ↔ ticket map

| Component | Ticket |
|---|---|
| Gradle skeleton + packages | ticket-001 |
| GestureCaptureService, WakeLockGuard, heartbeat | ticket-002 |
| SensorStream, detectors, trace-replay tests | ticket-003 |
| MacroAccessibilityService + onboarding | ticket-004 |
| MacroCodec, models, import rules | ticket-005 |
