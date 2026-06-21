# Gesture Macro — Codebase Guide

This file is the entry point for any agent or developer resuming work on this repo.

## Quick orientation

| Concern | Location |
|---------|----------|
| Full persistent state | `docs/SDLC_STATE.md` |
| Dependency-ordered backlog | `docs/BACKLOG.md` |
| Architecture + ADRs | `docs/ARCHITECTURE.md`, `docs/adr/` |
| Active tickets | `tickets/` |
| Threat model | `docs/THREAT_MODEL.md` |
| Schema JSON | `schema/gesture-macro-v1.json`, `schema/gesture-macro-v2.json` |

## Environment constraints (important for agent sessions)

- **`dl.google.com` is blocked (HTTP 403)** in the remote sandbox. AGP/AndroidX cannot
  be resolved locally. Do **not** run `./gradlew build` locally — push and let CI run it.
- Maven Central is reachable; ktlint CLI can be verified locally:
  ```
  java -jar ktlint.jar "app/src/**/*.kt"
  ```
- No Android SDK locally (`ANDROID_HOME` unset). JDK 17 is available.
- Development branch: `claude/engine-implementation-maintenance-rj1uh7`.
- Target branch for PRs: `main`.

## Package layout

```
app/src/main/kotlin/io/github/dianila68/gesturemacro/
├── android/          Android-specific wrappers (Room DB, SharedPreferences, BroadcastReceiver)
├── core/
│   ├── actions/       Action executors (FlashlightExecutor, SoundExecutor, LocationAlertExecutor, …)
│   ├── engine/        MacroEngine, SensorCondition + ConditionEventWindow, EngineMetrics
│   ├── recording/     GestureRecordingSession, SampleBuffer, EnvelopeBuilder, RecordedGestureDetector
│   └── sensors/       GestureDetector implementations, SensorUtils, FallDetector, ProximityWaveDetector
├── service/          GestureCaptureService (foreground service, sensor pipeline entry point)
└── ui/               Compose screens: MacroEditor, RecordingSubEditor, GestureLibrarySection
```

## How to add a new gesture trigger

1. Add a `GesturePattern` enum value in `core/sensors/Model.kt`.
2. Create a new `*Detector.kt` implementing `GestureDetector`:
   - `feed(sample: SensorSample): GestureEvent?`
   - `reset()`
3. Register the sensor type(s) the detector needs in `TriggerLibrary` (e.g. `SensorType.ACCELEROMETER`).
4. Add a `MacroTrigger` enum value and wire it in `TriggerLibrary.detectorFor(trigger)` and `SensorPipeline`.
5. Write trace-replay JVM tests in `app/src/test/kotlin/.../core/sensors/`.

## How to add a new action executor

1. Create `core/actions/MyActionExecutor.kt` implementing `ActionExecutor`:
   ```kotlin
   class MyActionExecutor : ActionExecutor {
       override suspend fun execute(action: MacroAction): ExecResult { … }
   }
   ```
2. Add it to `ActionDispatcher` in `core/engine/BuiltinSpiImpls.kt`.
3. Add a matching `MacroActionType` to the schema and `MacroCodec`.
4. Add a ticket entry in `tickets/` and a BACKLOG.md reference.

## How to add a new `SensorCondition`

1. Add a variant to the `SensorCondition` sealed class in `core/engine/SensorCondition.kt`.
2. Add the matching `GesturePattern` and a detector that emits it.
3. Add a `check` branch in `ConditionEventWindow.check()`.
4. Add a `SensorConditionSpec` JSON key and update `MacroCodec`.
5. Add tests in `ConditionEventWindowTest.kt`.

## Testing strategy

- **JVM unit tests** (`app/src/test/`): gesture detectors (trace replay), codec (JSON/YAML round-trip),
  engine cooldown/constraints, condition evaluation, sensor utilities.
  Run locally: `./gradlew :app:test` (requires JDK; no Android SDK needed).
- **Android integration tests** (`app/src/androidTest/`): Room migration, DAO round-trips.
  Run in CI via `connectedDebugAndroidTest` on API 29 emulator.
- **Static analysis**: ktlint (check in `./gradlew build`) + detekt (`config/detekt/detekt.yml`).
- **Fuzz corpus**: `app/src/test/kotlin/.../FuzzCorpusTest.kt` — 12 seeds, fail-closed.

## Milestone status summary

| Milestone | Status |
|-----------|--------|
| M1 — Core gesture engine (6 triggers) | ✅ Code-complete; pending on-device pass (ticket-009) |
| M2 — Fall-alert flagship (042–044) | ✅ Done |
| M3 — Gesture recording (045–053) | ✅ Done |
| M4 — Platform expansion (054–058) | 🕒 Planned; tickets in `tickets/` |

Full milestone detail: `docs/SDLC_STATE.md`.
