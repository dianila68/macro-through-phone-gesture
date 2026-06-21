# SDLC State — persistent “what to do next”

> **Purpose:** any person or agent session can resume the project from this file alone.
> **Rule:** every PR that advances a stage updates this file in the same commit.
> Last updated: 2026-06-21 (session: M2 fall/sound/location-alert, M3 gesture recording full pipeline, detekt baseline, EngineMetrics wiring)

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
| Implementation | 🟡 **In progress (M2/M3)** | **M1 done:** 001 ✅, 003 ✅, 004 ✅, 005 ✅, 007 ✅, 008 ✅, 010 ✅, all 6 triggers live, multi-sensor pipeline, demand-driven subscription (013). **M2 in progress:** `FallDetector` multi-phase state machine (IDLE→FREE_FALL_CANDIDATE→IMPACT_DETECTED→CONFIRMED) with `FallConfig` injection ✅; `SoundExecutor` with `MediaPlayer`/TTS code paths ✅; `LocationAlertExecutor` with 5 s countdown notification + FusedLocation + SMS dispatch (043) ✅; proximity sensor-relative threshold calibration (020) ✅; app-launch `<queries>` manifest fix (019) ✅. **M3 gesture recording:** `GestureRecordingSession` (IDLE→COUNTDOWN→CAPTURING→REVIEWING→DONE lifecycle, `StateFlow`) ✅; `SampleBuffer` (ring buffer, 500-sample cap) ✅; `RepetitionScorer` (duration + RMS + peak-count scoring, `RepetitionScore`) ✅; `EnvelopeBuilder` (DTW align + mean±tolerance per axis) ✅; `RecordedGestureDetector` (live DTW match, `GestureDetector` impl) ✅; `RecordingSubEditor` Composable UI ✅; Room DB v4 `recorded_gestures` table + `Migration_3_4` ✅; `ReplayValidator` (“Test it” back-end) ✅; `GestureLibrarySection` management UI ✅. **Sensing/conditions:** `SensorCondition` sealed class (030–033) ✅; `SensorUtils` (rolling RMS, low-pass, magnitude) ✅; `ConditionEvaluator` wired into `MacroEngine.onGesture()` ✅. **Observability:** `EngineMetrics`/`EngineMetricsCollector` with rolling p50/p95 latency wired into `GestureCaptureService` ✅. Schema v1+v2+v3 JSON committed. |
| StaticAnalysis | 🟡 Improved | ktlint plugin wired; Android Lint in CI; detekt configured (`config/detekt/detekt.yml`) with coroutines rules, ComplexCondition, expanded MagicNumber ignore list, sensor-threshold tuning. Detekt baseline clean. Continuous improvement: suppress only where justified. |
| SecurityAnalysis | 🟡 Partial | T1 (accessibility disabled on import), T2 (size cap, strict decode, version dispatch), T3 (system-only binding, minimal scope), T5 (Keystore HMAC seal, fail-closed on load), NFR-7 (executor refuses when disconnected) implemented + tested |
| FormalVerification | ⬜ Not started | Scope decision pending — realistic target: model-check the engine state machine (cooldown/constraint logic) or exhaustive property tests; decide at M1 review |
| UnitTesting | 🟢 **Active** | JVM tests green in CI: detector trace-replay (shake, double-shake, flip), JSON+YAML codec (incl. anchor rejection), engine cooldown/constraints, integrity seal/fail-closed, trigger-library catalog/detector coverage |
| IntegrationTesting | 🟡 **In progress** | Emulator CI job added (`connectedDebugAndroidTest`, API 29); `MacroMigrationTest` + `MacroDaoIntegrationTest` written and merged; `Migration_3_4` for `recorded_gestures` table added |
| PerformanceTesting | ⬜ Not started | NFR-1 battery duty-cycle measurements; macrobenchmark at M3; `EngineMetrics` p50/p95 latency now available as foundation |
| FuzzTesting | 🟡 Seeded | 12-seed corpus + fail-closed regression test in CI (`FuzzCorpusTest`); continuous fuzzing harness still TODO (ticket-015) |
| CodeReview | ⬜ Recurring gate | Every PR; security-sensitive areas rule in CONTRIBUTING |
| Deployment | ⬜ Not started | Internal track / GitHub releases; needs signing setup (out of VCS) |
| Monitoring | 🟡 Foundation | `EngineMetrics` (p50/p95 latency, missed gesture count, executor failure rate) exposed as `StateFlow<EngineMetrics>` from `GestureCaptureService`. In-app analytics dashboard planned (ticket-056). No remote telemetry (NFR-4). |

## M2 Milestone: Fall-Alert Flagship — Status

| Ticket | Name | Status |
|--------|------|--------|
| 019 | App-launch `<queries>` manifest fix | ✅ Done |
| 020 | Proximity sensor-relative threshold | ✅ Done |
| 042 | `FallDetector` multi-phase state machine | ✅ Done |
| 043 | `LocationAlertExecutor` with confirm-countdown + SMS | ✅ Done |
| 044 | `SoundExecutor` (MediaPlayer + TTS) | ✅ Done |

## M3 Milestone: Gesture Recording Sub-Editor — Status

| Ticket | Name | Status |
|--------|------|--------|
| 045 | `GestureRecordingSession` lifecycle | ✅ Done |
| 046 | `SampleBuffer` + sensor capture | ✅ Done |
| 047 | `RepetitionScorer` per-rep quality | ✅ Done |
| 048 | `EnvelopeBuilder` DTW | ✅ Done |
| 049 | `RecordedGestureDetector` live match | ✅ Done |
| 050 | Recording sub-editor UI | ✅ Done |
| 051 | Room DB v4 + Migration_3_4 | ✅ Done |
| 052 | `ReplayValidator` (“Test it”) | ✅ Done |
| 053 | Gesture library management UI | ✅ Done |

## ▶ NEXT ACTIONS (in order)

> **Product thesis (read first):** [ADR-0005](adr/0005-product-direction.md) — private on-device
> sensing → safe local reactions, fall-alert flagship. The third-party app-control track is **parked**
> in [`tickets/plausible-features/`](../tickets/plausible-features/).
> **Full dependency-ordered backlog (tracks, recommended order):** [BACKLOG.md](BACKLOG.md).
> **M4 expansion tickets:** 054 (condition UI editor), 055 (BLE bridge), 056 (analytics dashboard),
> 057 (widget quick-toggle), 058 (notification action arm).

1. **ticket-009** — manual on-device M1 verification pass (screen-off latency, 24 h soak, Doze, restart, shake→flashlight E2E). **Requires a human with a device.** Closes M1 formally.
2. **M2 on-device validation** — test FallDetector sensitivity on real hardware; tune `FallConfig` thresholds; verify SMS dispatch with `SEND_SMS` / `ACCESS_FINE_LOCATION` grant flow.
3. **ticket-054** — Condition UI editor: visual threshold sliders for `SensorCondition` rules inside the macro editor; depends on 033 (already done).
4. **ticket-057** — Widget quick-toggle: `AppWidgetProvider` home-screen widget for arm/disarm; independent of other M4 tickets.
5. **ticket-058** — Notification action arm: inline arm/disarm button in `GestureCaptureService` persistent notification.
6. **ticket-056** — Analytics dashboard: per-macro latency / miss-rate screen using `EngineMetrics` + `execution_log` Room table.
7. **ticket-015** — Continuous fuzzing harness (AFL++ or `@FuzzTest`); currently seeded only.
8. **ticket-009** cycle: once device tested, close M1, tag release, proceed to M4.

## Environment notes for future sessions (important)

- **`dl.google.com` is blocked (HTTP 403) in the remote execution sandbox** → AGP/AndroidX cannot be resolved locally; **local `gradlew build` is impossible**. Maven Central is reachable. Verification happens in **GitHub Actions** (runners have full network + Android SDK). Push to a `claude/**` or convention branch to trigger CI.
- No Android SDK locally (`ANDROID_HOME` unset); Gradle 8.14 + JDK 21 available at `/opt/gradle` / `/usr/bin/java` (wrapper pins 8.11.1, JDK 17 in CI).
- Remote `origin` redirects: canonical URL is `https://github.com/dianila68/macro-through-phone-gesture.git`.
- **ktlint can be verified locally**: fat-jar CLI from Maven Central (`com.pinterest.ktlint:ktlint-cli:1.2.1`, classifier `all`) → `java -jar ktlint.jar "app/src/**/*.kt"`. Style pinned to `intellij_idea` in `.editorconfig`.
- Compilation CANNOT be verified locally (dl.google.com blocked) — push and watch CI.
- Process docs: tickets in [`../tickets/`](../tickets/), workflow rules in [`../CONTRIBUTING.md`](../CONTRIBUTING.md), threat gates in [THREAT_MODEL.md](THREAT_MODEL.md).
