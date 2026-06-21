# SDLC State — persistent "what to do next"

> **Purpose:** any person or agent session can resume the project from this file alone.
> **Rule:** every PR that advances a stage updates this file in the same commit.
> Last updated: 2026-06-21 (session: M4 analytics dashboard + widget + notification arm/disarm; FallDetector/SampleBuffer/RepetitionScorer/EngineMetrics unit tests; INarrativePipeline interface; new tickets 059/060)

## The lifecycle graph

States `S` and transitions `T` (Monitoring feeds back into Requirements/Architecture/Implementation):

```
Requirements → ThreatModeling → Architecture → Design → Implementation
Implementation → { StaticAnalysis, SecurityAnalysis, FormalVerification, UnitTesting }
StaticAnalysis | SecurityAnalysis | FormalVerification → UnitTesting
UnitTesting → IntegrationTesting → { PerformanceTesting, FuzzTesting } → CodeReview
CodeReview → Deployment → Monitoring
Monitoring → { Requirements, Architecture, Implementation }   (feedback loop)
```

## Stage status

| Stage | Status | Artifact / evidence |
|---|---|---|
| Requirements | ✅ Done (v1) | [REQUIREMENTS.md](REQUIREMENTS.md) — FR-1..10, NFR-1..8 |
| ThreatModeling | ✅ Done (v1) | [THREAT_MODEL.md](THREAT_MODEL.md) — STRIDE T1–T11, gating rules |
| Architecture | ✅ Done (v1) | [ARCHITECTURE.md](ARCHITECTURE.md), ADRs 0001/0002, [`schema/gesture-macro-v1.json`](../schema/gesture-macro-v1.json); ticket-006 executed |
| Design | ✅ Done (v1) | [DESIGN.md](DESIGN.md) — contracts for engine/sensors/actions/data/serialization |
| Implementation | 🟡 **In progress (M3 done, M4 active)** | **M1 done:** 001 ✅, 003 ✅, 004 ✅, 005 ✅, 007 ✅, 008 ✅, 010 ✅, all 6 triggers live, multi-sensor pipeline, demand-driven subscription (013). **M2 done:** `FallDetector` multi-phase state machine ✅; `SoundExecutor` MediaPlayer/TTS ✅; `LocationAlertExecutor` 5 s countdown + FusedLocation + SMS ✅; proximity threshold calibration (020) ✅; app-launch manifest fix (019) ✅. **M3 done:** `GestureRecordingSession` IDLE→COUNTDOWN→CAPTURING→REVIEWING→DONE ✅; `SampleBuffer` ring buffer ✅; `RepetitionScorer` ✅; `EnvelopeBuilder` DTW ✅; `RecordedGestureDetector` live DTW match ✅; `RecordingSubEditor` Composable ✅; Room DB v4 + `Migration_3_4` ✅; `ReplayValidator` ✅; `GestureLibrarySection` UI ✅. **Sensing/conditions:** `SensorCondition` sealed class (030–033) ✅; `SensorUtils` ✅; `ConditionEvaluator` wired into `MacroEngine` ✅. **M4 active:** `EngineMetrics`/`EngineMetricsCollector` rolling p50/p95 wired into `GestureCaptureService` ✅; `AnalyticsDashboardViewModel` + `AnalyticsDashboardScreen` Composable (ticket-056) ✅; `MacroToggleWidget` home-screen arm/disarm widget (ticket-057) ✅; `GestureCaptureService` ARM/DISARM notification action buttons (ticket-058) ✅. Schema v1+v2+v3 JSON committed. |
| StaticAnalysis | 🟡 Improved | ktlint plugin wired; Android Lint in CI; detekt configured (`config/detekt/detekt.yml`) with coroutines rules, ComplexCondition, expanded MagicNumber ignore list, sensor-threshold tuning. Detekt baseline clean. |
| SecurityAnalysis | 🟡 Partial | T1 (accessibility disabled on import), T2 (size cap, strict decode, version dispatch), T3 (system-only binding, minimal scope), T5 (Keystore HMAC seal, fail-closed on load), NFR-7 (executor refuses when disconnected) implemented + tested |
| FormalVerification | ⬜ Not started | Scope decision pending — realistic target: model-check the engine state machine |
| UnitTesting | 🟢 **Active** | JVM tests green in CI: detector trace-replay (shake, double-shake, flip, fall, altitude, light, step, twist, proximity-wave); `SampleBuffer` (ring buffer, overflow, snapshotWindow); `RepetitionScorer` (duration/amplitude/peak scoring); `EngineMetricsCollector` (counters, p50/p95, window eviction, StateFlow); `GestureEnvelopeBuilder`; `RecordedGestureDetector`; JSON+YAML codec (incl. anchor rejection); engine cooldown/constraints + sensor condition gates; integrity seal/fail-closed; trigger-library catalog/detector coverage |
| IntegrationTesting | 🟡 **In progress** | Emulator CI job added (`connectedDebugAndroidTest`, API 29); `MacroMigrationTest` + `MacroDaoIntegrationTest` written and merged; `Migration_3_4` for `recorded_gestures` table added |
| PerformanceTesting | ⬜ Not started | NFR-1 battery duty-cycle measurements; macrobenchmark at M3; `EngineMetrics` p50/p95 latency now available as foundation |
| FuzzTesting | 🟡 Seeded | 12-seed corpus + fail-closed regression test in CI (`FuzzCorpusTest`); continuous fuzzing harness still TODO (ticket-015) |
| CodeReview | ⬜ Recurring gate | Every PR; security-sensitive areas rule in CONTRIBUTING |
| Deployment | ⬜ Not started | Internal track / GitHub releases; needs signing setup (out of VCS) |
| Monitoring | 🟡 Foundation | `EngineMetrics` (p50/p95 latency, missed gesture count, executor failure rate) exposed as `StateFlow<EngineMetrics>` from `GestureCaptureService`. `AnalyticsDashboardScreen` Composable renders live metrics (ticket-056). No remote telemetry (NFR-4). |

## M2 Milestone: Fall-Alert Flagship — ✅ Complete

| Ticket | Name | Status |
|--------|------|--------|
| 019 | App-launch `<queries>` manifest fix | ✅ Done |
| 020 | Proximity sensor-relative threshold | ✅ Done |
| 042 | `FallDetector` multi-phase state machine | ✅ Done |
| 043 | `LocationAlertExecutor` with confirm-countdown + SMS | ✅ Done |
| 044 | `SoundExecutor` (MediaPlayer + TTS) | ✅ Done |

## M3 Milestone: Gesture Recording Sub-Editor — ✅ Complete

| Ticket | Name | Status |
|--------|------|--------|
| 045 | `GestureRecordingSession` lifecycle | ✅ Done |
| 046 | `SampleBuffer` + sensor capture | ✅ Done |
| 047 | `RepetitionScorer` per-rep quality | ✅ Done |
| 048 | `EnvelopeBuilder` DTW | ✅ Done |
| 049 | `RecordedGestureDetector` live match | ✅ Done |
| 050 | Recording sub-editor UI | ✅ Done |
| 051 | Room DB v4 + Migration_3_4 | ✅ Done |
| 052 | `ReplayValidator` ("Test it") | ✅ Done |
| 053 | Gesture library management UI | ✅ Done |

## M4 Milestone: Observability, Sensing Expansion, Widget — In Progress

| Ticket | Name | Status |
|--------|------|--------|
| 054 | Condition UI editor (visual threshold sliders) | ⬜ Planned |
| 055 | Cross-device BLE bridge (stub + interface) | ⬜ Planned |
| 056 | Analytics dashboard (`EngineMetrics` screen) | ✅ Done |
| 057 | Widget quick-toggle (arm/disarm home-screen widget) | ✅ Done |
| 058 | Notification action arm (inline arm/disarm button) | ✅ Done |
| 059 | Battery-aware sampling rate adaptation | ⬜ Planned |
| 060 | Macro export/import (JSON share sheet) | ⬜ Planned |

## ▶ NEXT ACTIONS (in order)

> **Product thesis (read first):** [ADR-0005](adr/0005-product-direction.md) — private on-device
> sensing → safe local reactions, fall-alert flagship. The third-party app-control track is **parked**
> in [`tickets/plausible-features/`](../tickets/plausible-features/).
> **Full dependency-ordered backlog (tracks, recommended order):** [BACKLOG.md](BACKLOG.md).

1. **ticket-009** — manual on-device M1/M2/M3 verification pass (screen-off latency, 24 h soak, Doze, restart, shake→flashlight, fall→SMS E2E). **Requires a human with a device.**
2. **ticket-054** — Condition UI editor: visual threshold sliders for `SensorCondition` rules inside the macro editor.
3. **ticket-055** — BLE bridge stub: `BleSessionManager` interface + no-op impl; real BLE ranging in M5.
4. **ticket-059** — Battery-aware sampling: `BatteryMonitor` observes `BatteryManager` broadcasts; dynamically adjusts `SensorManager` sampling rate between `SENSOR_DELAY_NORMAL` and `SENSOR_DELAY_FASTEST` based on battery level; exposed as `SamplingProfile` enum (BALANCED, PERFORMANCE, POWER_SAVE).
5. **ticket-060** — Macro export/import: serialize `GestureMacro` list to signed JSON; share via `Intent.ACTION_SEND`; import via `FileProvider` intent handler; HMAC-seal on export, verify on import.
6. **ticket-015** — Continuous fuzzing harness (AFL++ or `@FuzzTest`).

## Environment notes for future sessions (important)

- **`dl.google.com` is blocked (HTTP 403) in the remote execution sandbox** → AGP/AndroidX cannot be resolved locally; **local `gradlew build` is impossible**. Maven Central is reachable. Verification happens in **GitHub Actions** (runners have full network + Android SDK). Push to a `claude/**` or convention branch to trigger CI.
- No Android SDK locally (`ANDROID_HOME` unset); Gradle 8.14 + JDK 21 available at `/opt/gradle` / `/usr/bin/java` (wrapper pins 8.11.1, JDK 17 in CI).
- Remote `origin` redirects: canonical URL is `https://github.com/dianila68/macro-through-phone-gesture.git`.
- **ktlint can be verified locally**: fat-jar CLI from Maven Central (`com.pinterest.ktlint:ktlint-cli:1.2.1`, classifier `all`) → `java -jar ktlint.jar "app/src/**/*.kt"`. Style pinned to `intellij_idea` in `.editorconfig`.
- Compilation CANNOT be verified locally (dl.google.com blocked) — push and watch CI.
- Process docs: tickets in [`../tickets/`](../tickets/), workflow rules in [`../CONTRIBUTING.md`](../CONTRIBUTING.md), threat gates in [THREAT_MODEL.md](THREAT_MODEL.md).
