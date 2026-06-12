# GestureMacro — Hardware-Gesture System Automation for Android

GestureMacro is a native Android application that executes **system macros** and controls **third-party apps** (e.g. Spotify, flashlight) through **hardware gestures captured in the background** — shake patterns, device flips, proximity waves, and other sensor-derived triggers — without requiring the screen to be on or the app to be in the foreground.

> **Status: pre-alpha (planning & scaffolding).** No application code has landed yet. See the [roadmap](docs/ARCHITECTURE.md#milestone-roadmap) and the [`tickets/`](tickets/) backlog for what is being built first.

## Why native Kotlin (the architectural pivot)

This project deliberately targets **100% native Kotlin** rather than a cross-platform stack. The app's entire value lives in OS-level integration that cross-platform frameworks cannot reach reliably:

- **Background survivability.** Continuous gesture capture requires a persistent **Foreground Service** that cooperates with Doze mode, App Standby, and OEM battery managers. This demands direct, fine-grained control of Android service lifecycles and WakeLocks.
- **Sensor latency.** Gesture detection is a real-time signal-processing problem. Direct `SensorManager` access with native sampling rates and sensor batching avoids the bridge overhead and jitter of cross-platform sensor plugins.
- **Accessibility-level control.** Driving third-party apps requires the **Accessibility API** (node introspection, action dispatch, global actions) — a deeply platform-specific surface with no faithful cross-platform abstraction.

## Core capabilities (planned)

| Capability | Mechanism |
|---|---|
| Background gesture capture | Foreground Service + `SensorManager` (accelerometer, gyroscope, proximity) |
| Macro engine | Trigger → constraint check → ordered action execution |
| System actions | Camera/flashlight (`CameraManager`), media control (`MediaSession` / key events), Intents |
| Third-party app control | `AccessibilityService` action dispatch |
| Macro portability | JSON (canonical) / YAML import & export with versioned schema |

## Tech stack

- **Language:** Kotlin (Coroutines + Flow)
- **UI:** Jetpack Compose (Material 3)
- **Persistence:** Room
- **Serialization:** kotlinx.serialization (JSON canonical format)
- **Background:** Foreground Service, strategic WakeLocks
- **Execution:** Accessibility API, system Intents

Full details, including the Gesture-Macro JSON Schema and the milestone roadmap, are in [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

| Path | Purpose |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Tech stack, background-execution strategy, data format, roadmap |
| [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) | Functional & non-functional requirements (FR-x / NFR-x) |
| [`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md) | STRIDE threat model (T1–T11) gating security-sensitive work |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records |
| [`docs/SDLC_STATE.md`](docs/SDLC_STATE.md) | Lifecycle state machine — current stage & next actions |
| [`schema/`](schema/) | Versioned Gesture-Macro JSON Schemas (normative format contract) |
| [`SECURITY.md`](SECURITY.md) | Vulnerability reporting & hard security rules |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Branch naming, ticket workflow, PR rules |
| [`tickets/`](tickets/) | File-based ticket backlog (source of truth for planned work) |
| `.github/workflows/` | CI (Gradle build + lint) |

## Contributing

All work is tracked through the file-based ticket system in [`tickets/`](tickets/) and branch names are bound to ticket IDs (e.g. `feat/ticket-003-sensor-listener-module`). Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

## License

[MIT](LICENSE) © 2026 dianila68
