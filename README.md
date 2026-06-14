# GestureMacro — Hardware-Gesture Automation for Android

GestureMacro is a **native Android** app that turns **hardware gestures captured in the background** — shake, double-shake, device flips, wrist twist, proximity wave — into **safe, local, instant reactions** (play a sound, toggle the flashlight, control media, launch an app), without the screen on or the app in the foreground.

> **Status: alpha — in active development.** The background engine and the full gesture vocabulary are implemented and CI-verified; the app builds and runs on a device. Work is now oriented around the product thesis below. See the [milestone roadmap](docs/ARCHITECTURE.md#milestone-roadmap) and the dependency-ordered [BACKLOG.md](docs/BACKLOG.md).

## Product direction ([ADR-0005](docs/adr/0005-product-direction.md))

**Private, on-device sensing → safe, local, reversible reactions — with personal-safety (fall → send my location) as the flagship,** and quick delight macros (sounds, flashlight, media) as the everyday surface. Gesture detection is probabilistic, so the app deliberately pairs it with low-consequence actions and keeps everything **local and private** (no telemetry). The ambitious "control arbitrary third-party apps" direction is researched but **parked** in [`tickets/plausible-features/`](tickets/plausible-features/).

## Why native Kotlin

- **Background survivability.** Continuous capture runs in a persistent **Foreground Service** that cooperates with Doze, App Standby, and OEM battery managers — demanding fine-grained control of service lifecycles and WakeLocks.
- **Sensor latency.** Gesture detection is real-time signal processing; direct `SensorManager` access (native sampling rates, batching) avoids cross-platform bridge jitter.
- **Privacy.** Local-only, fail-closed, no telemetry — which is exactly what a safety/location feature needs.

## Capabilities

| Capability | Status |
|---|---|
| Background gesture capture (Foreground Service + `SensorManager`) | ✅ implemented |
| 6 gesture triggers: shake, double-shake, flip up/down, twist, proximity-wave | ✅ implemented |
| Macro engine (trigger → constraints → cooldown → ordered actions) | ✅ implemented |
| Demand-driven sensor subscription (only sensors used by enabled macros) | ✅ implemented |
| Actions: flashlight, media keys, app launch¹, accessibility (basic) | ✅ implemented |
| Compose macro editor; Room persistence + Keystore integrity sealing | ✅ implemented |
| JSON (canonical) / YAML macro import & export, versioned schema | ✅ implemented |
| Fall detection → send-my-location flagship; "play sound/voice" action | 🚧 planned (tickets 042–044) |
| Deeper sensing (activity/state inference, composed conditions) | 🚧 planned (tickets 030–033) |

¹ App-launch currently needs the `<queries>` fix (ticket-019); proximity-wave needs a sensor-relative threshold (ticket-020) — see the backlog.

## Build & run

Requires the **Android SDK** (platform 35 + build-tools) and **JDK 17**; min Android 8.0 (API 26).

- **Easiest — Android Studio:** open the project, let it sync (it installs the SDK + writes `local.properties`), plug in a device with USB debugging on, hit **Run**.
- **Command line:**
  ```sh
  ./gradlew assembleDebug                                  # use the wrapper (Gradle 8.11.1), not system gradle
  adb install app/build/outputs/apk/debug/app-debug.apk
  ```
  Point the build at your SDK via `ANDROID_HOME`, or a `local.properties` with `sdk.dir=/path/to/Android/Sdk`.

On first launch: grant the notification permission, tap **Start engine**, optionally allow the battery-optimization exemption (reliable screen-off capture). A shake-to-flashlight macro is seeded by default.

## Tech stack

Kotlin (Coroutines + Flow) · Jetpack Compose (Material 3) · Room · kotlinx.serialization (+ kaml for YAML) · Foreground Service + strategic WakeLocks. Full detail and the Gesture-Macro JSON Schema: [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Repository layout

| Path | Purpose |
|---|---|
| [`app/`](app/) | The Android app (Kotlin, Compose) |
| [`docs/SDLC_STATE.md`](docs/SDLC_STATE.md) | Current state & next actions — **start here** |
| [`docs/BACKLOG.md`](docs/BACKLOG.md) | Every open ticket in dependency order + parallel tracks |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Tech stack, background strategy, data format, milestone roadmap |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records (0001–0005) |
| [`docs/research/`](docs/research/) | Cited research (e.g. third-party app control) |
| [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) · [`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md) | Requirements (FR/NFR) · STRIDE threat model |
| [`schema/`](schema/) | Versioned Gesture-Macro JSON Schemas (normative format) |
| [`tickets/`](tickets/) | File-based ticket backlog; [`plausible-features/`](tickets/plausible-features/) = parked ideas |
| [`log/`](log/) | Session logs |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) · [`SECURITY.md`](SECURITY.md) | Workflow rules · security policy |
| `.github/workflows/` | CI (Gradle build + lint + API-29 emulator tests) |

## Contributing

Work is tracked through the file-based ticket system in [`tickets/`](tickets/); branch names are bound to ticket IDs. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

## License

[MIT](LICENSE) © 2026 dianila68
