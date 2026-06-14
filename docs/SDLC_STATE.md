# SDLC State — persistent "what to do next"

> **Purpose:** any person or agent session can resume the project from this file alone.
> **Rule:** every PR that advances a stage updates this file in the same commit.
> Last updated: 2026-06-13 (session: twist trigger, multi-sensor pipeline, demand-driven sensor subscription; tickets 011–015)

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
| Implementation | 🟡 **In progress** | 001 ✅, 003 ✅, 004 ✅, 005 ✅ (JSON+YAML), 002 code-complete (device pass → 009), 007 ✅ (HMAC sealing + migration), 008 ✅, 010 ✅ (full editor + trigger library). Pipeline + Room persistence + audit log live. `TriggerLibrary` is the single source of truth for triggers (editor offers them, service builds detectors + subscribes to sensors from them); 5 of 6 live (shake, double-shake, flip up/down, twist — proximity-wave still planned, ticket-012). All 6 triggers live (shake, double-shake, flip up/down, twist, proximity-wave). Multi-sensor pipeline merges accelerometer + gyroscope + proximity, demand-driven: only sensors used by enabled macros are registered (ticket-013). All CI-green. Schema v1+v2 JSON committed |
| StaticAnalysis | 🟡 Partial | ktlint plugin wired (runs in `gradlew build` via `check`); Android Lint step in CI. TODO: detekt |
| SecurityAnalysis | 🟡 Partial | T1 (accessibility disabled on import), T2 (size cap, strict decode, version dispatch), T3 (system-only binding, minimal scope), T5 (Keystore HMAC seal, fail-closed on load), NFR-7 (executor refuses when disconnected) implemented + tested |
| FormalVerification | ⬜ Not started | Scope decision pending — realistic target: model-check the engine state machine (cooldown/constraint logic) or exhaustive property tests; decide at M1 review |
| UnitTesting | 🟢 **Active** | JVM tests green in CI: detector trace-replay (shake, double-shake, flip), JSON+YAML codec (incl. anchor rejection), engine cooldown/constraints, integrity seal/fail-closed, trigger-library catalog/detector coverage |
| IntegrationTesting | 🟡 **In progress** | Emulator CI job added (`connectedDebugAndroidTest`, API 29, `reactivecircus/android-emulator-runner@v2`); `MacroMigrationTest` (MigrationTestHelper v1→v2, PRAGMA column check) + `MacroDaoIntegrationTest` (round-trip, seal preservation, tamper fail-closed, execution-log) written; schema JSONs v1+v2 committed |
| PerformanceTesting | ⬜ Not started | NFR-1 battery duty-cycle measurements; macrobenchmark at M3 |
| FuzzTesting | 🟡 Seeded | 12-seed corpus + fail-closed regression test in CI (FuzzCorpusTest); continuous fuzzing harness still TODO |
| CodeReview | ⬜ Recurring gate | Every PR; security-sensitive areas rule in CONTRIBUTING |
| Deployment | ⬜ Not started | Internal track / GitHub releases; needs signing setup (out of VCS) |
| Monitoring | ⬜ Not started | In-app diagnostics (FR-9) is the v1 "monitoring"; no remote telemetry (NFR-4) |

## ▶ NEXT ACTIONS (in order)

> **Product thesis (read first):** [ADR-0005](adr/0005-product-direction.md) — private on-device
> sensing → safe local reactions, fall-alert flagship. The third-party app-control track is **parked**
> in [`tickets/plausible-features/`](../tickets/plausible-features/).
> **Full dependency-ordered backlog (tracks, recommended order):** [BACKLOG.md](BACKLOG.md).
> **Architecture:** [ADR-0003](adr/0003-core-app-separation.md) core/app split; [ADR-0004](adr/0004-third-party-app-control-strategy.md) app-control (parked).
> New tickets: **042** fall detector, **043** location-alert action, **044** sound/voice action.


1. **ticket-009** — manual on-device M1 verification pass (screen-off latency, 24 h soak, Doze, restart, shake→flashlight E2E). Closes ticket-002 and M1. **Requires a human with a device.**
2. **ticket-010** ✅ — full macro editor shipped (`ui/MacroEditor.kt`): trigger picker + sensitivity + cooldown, screen/time constraints, ordered multi-action builder, prefilled edit. Backed by `core/triggers/TriggerLibrary`. Follow-up: lift editor state into a ViewModel at the M2 module split; per-field validation.
3. **IntegrationTesting** ✅ — emulator CI job wired (`reactivecircus/android-emulator-runner@v2`, API 29); `MacroMigrationTest` + `MacroDaoIntegrationTest` merged. Next: add `MigrationTestHelper` second pass once schema hash is confirmed green by CI; add DAO flow-assertion tests for multi-macro scenarios.
4. **ticket-013** ✅ — demand-driven sensor subscription: the pipeline now runs only the detectors for *enabled* macro patterns and registers only their sensors (gyroscope off unless a twist macro is on).
5. **ticket-012** ✅ — proximity-wave trigger shipped; **all 6 triggers now live**. Gesture vocabulary complete.
6. **ticket-014** (detekt) / **ticket-015** (continuous fuzzing) — finish StaticAnalysis and FuzzTesting stages.
7. At M1 closure: re-plan checkpoint (REFACTORING_PLAN Phase 3), module split decision (M2 trigger), FormalVerification scope decision (engine state machine).

## Environment notes for future sessions (important)

- **`dl.google.com` is blocked (HTTP 403) in the remote execution sandbox** → AGP/AndroidX cannot be resolved locally; **local `gradlew build` is impossible**. Maven Central is reachable. Verification happens in **GitHub Actions** (runners have full network + Android SDK). Push to a `claude/**` or convention branch to trigger CI.
- No Android SDK locally (`ANDROID_HOME` unset); Gradle 8.14 + JDK 21 available at `/opt/gradle` / `/usr/bin/java` (wrapper pins 8.11.1, JDK 17 in CI).
- Remote `origin` redirects: canonical URL is now `https://github.com/dianila68/macro-through-phone-gesture.git` (repo renamed, trailing hyphen dropped).
- **ktlint can be verified locally**: fat-jar CLI from Maven Central (`com.pinterest.ktlint:ktlint-cli:1.2.1`, classifier `all`) → `java -jar ktlint.jar "app/src/**/*.kt"`. Style pinned to `intellij_idea` in `.editorconfig` — the `ktlint_official` default caused CI failure run #6.
- Compilation still CANNOT be verified locally (dl.google.com blocked) — push and watch CI; GitHub MCP `actions_list` output overflows, parse the saved JSON file with python instead.
- Process docs: tickets in [`../tickets/`](../tickets/), workflow rules in [`../CONTRIBUTING.md`](../CONTRIBUTING.md), threat gates in [THREAT_MODEL.md](THREAT_MODEL.md).
