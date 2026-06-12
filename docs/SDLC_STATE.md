# SDLC State — persistent "what to do next"

> **Purpose:** any person or agent session can resume the project from this file alone.
> **Rule:** every PR that advances a stage updates this file in the same commit.
> Last updated: 2026-06-12 (session: feature implementation, tickets 002–005 + onboarding)

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
| Implementation | 🟡 **In progress** | 001 ✅, 003 ✅, 004 ✅, 002 code-complete (device pass → ticket-009), 005 partial (YAML → ticket-008). Full pipeline live: sensors → detectors → engine → executors; onboarding UI done. All CI-green |
| StaticAnalysis | 🟡 Partial | ktlint plugin wired (runs in `gradlew build` via `check`); Android Lint step in CI. TODO: detekt |
| SecurityAnalysis | 🟡 Partial | T1 (accessibility disabled on import), T2 (size cap, strict decode, version dispatch), T3 (system-only binding, minimal scope), NFR-7 (executor refuses when disconnected) implemented + tested |
| FormalVerification | ⬜ Not started | Scope decision pending — realistic target: model-check the engine state machine (cooldown/constraint logic) or exhaustive property tests; decide at M1 review |
| UnitTesting | 🟢 **Active** | 30 JVM tests green in CI: detector trace-replay (JSON fixtures), codec strict-import/T1 policy, engine cooldown/constraints |
| IntegrationTesting | ⬜ Not started | Needs instrumented tests / emulator job in CI (add `gradlew connectedCheck` matrix later) |
| PerformanceTesting | ⬜ Not started | NFR-1 battery duty-cycle measurements; macrobenchmark at M3 |
| FuzzTesting | ⬜ Not started | Targets per ADR-0002: JSON + YAML import (threat T2) |
| CodeReview | ⬜ Recurring gate | Every PR; security-sensitive areas rule in CONTRIBUTING |
| Deployment | ⬜ Not started | Internal track / GitHub releases; needs signing setup (out of VCS) |
| Monitoring | ⬜ Not started | In-app diagnostics (FR-9) is the v1 "monitoring"; no remote telemetry (NFR-4) |

## ▶ NEXT ACTIONS (in order)

1. **Merge the feature PR to `main`** (branch CI-green through commit 663fa40).
2. **ticket-009** — manual on-device M1 verification pass (screen-off latency, 24 h soak, Doze, restart). Closes ticket-002 and M1.
3. **ticket-007** — Room persistence + execution audit log (T4/FR-9).
4. **ticket-008** — YAML at the import/export boundary (completes ticket-005/ADR-0002); seeds FuzzTesting corpus.
5. **M3 UI**: macro list/editor screens over MacroStore (FR-6) + import/export pickers (FR-7).
6. At M1 closure: re-plan checkpoint (REFACTORING_PLAN Phase 3), module split decision (M2 trigger), FormalVerification scope decision.

## Environment notes for future sessions (important)

- **`dl.google.com` is blocked (HTTP 403) in the remote execution sandbox** → AGP/AndroidX cannot be resolved locally; **local `gradlew build` is impossible**. Maven Central is reachable. Verification happens in **GitHub Actions** (runners have full network + Android SDK). Push to a `claude/**` or convention branch to trigger CI.
- No Android SDK locally (`ANDROID_HOME` unset); Gradle 8.14 + JDK 21 available at `/opt/gradle` / `/usr/bin/java` (wrapper pins 8.11.1, JDK 17 in CI).
- Remote `origin` redirects: canonical URL is now `https://github.com/dianila68/macro-through-phone-gesture.git` (repo renamed, trailing hyphen dropped).
- **ktlint can be verified locally**: fat-jar CLI from Maven Central (`com.pinterest.ktlint:ktlint-cli:1.2.1`, classifier `all`) → `java -jar ktlint.jar "app/src/**/*.kt"`. Style pinned to `intellij_idea` in `.editorconfig` — the `ktlint_official` default caused CI failure run #6.
- Compilation still CANNOT be verified locally (dl.google.com blocked) — push and watch CI; GitHub MCP `actions_list` output overflows, parse the saved JSON file with python instead.
- Process docs: tickets in [`../tickets/`](../tickets/), workflow rules in [`../CONTRIBUTING.md`](../CONTRIBUTING.md), threat gates in [THREAT_MODEL.md](THREAT_MODEL.md).
