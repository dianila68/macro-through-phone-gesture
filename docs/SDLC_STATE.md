# SDLC State — persistent "what to do next"

> **Purpose:** any person or agent session can resume the project from this file alone.
> **Rule:** every PR that advances a stage updates this file in the same commit.
> Last updated: 2026-06-11 (session: SDLC walk, stages Requirements → Implementation)

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
| Implementation | 🟡 **In progress** | ticket-001 **Done** — scaffolding verified by CI run #4 (green: `gradlew build` + ktlint + Android Lint). Tickets 002–005 not started |
| StaticAnalysis | 🟡 Partial | ktlint plugin wired (runs in `gradlew build` via `check`); Android Lint step in CI. TODO: detekt |
| SecurityAnalysis | ⬜ Not started | Gate: NFR-3; first concrete task arrives with ticket-005 import path |
| FormalVerification | ⬜ Not started | Scope decision pending — realistic target: model-check the engine state machine (cooldown/constraint logic) or exhaustive property tests; decide at M1 review |
| UnitTesting | ⬜ Not started | First real tests: detector trace-replay (ticket-003), import rules (ticket-005) |
| IntegrationTesting | ⬜ Not started | Needs instrumented tests / emulator job in CI (add `gradlew connectedCheck` matrix later) |
| PerformanceTesting | ⬜ Not started | NFR-1 battery duty-cycle measurements; macrobenchmark at M3 |
| FuzzTesting | ⬜ Not started | Targets per ADR-0002: JSON + YAML import (threat T2) |
| CodeReview | ⬜ Recurring gate | Every PR; security-sensitive areas rule in CONTRIBUTING |
| Deployment | ⬜ Not started | Internal track / GitHub releases; needs signing setup (out of VCS) |
| Monitoring | ⬜ Not started | In-app diagnostics (FR-9) is the v1 "monitoring"; no remote telemetry (NFR-4) |

## ▶ NEXT ACTIONS (in order)

1. **Open a PR from `claude/repo-criticality-flaws-review-jxpodr` to `main`** with the SDLC docs + verified scaffolding (CI already green on the branch: run #4) and merge it.
2. **ticket-002** (`feat/ticket-002-foreground-service`): GestureCaptureService per DESIGN.md (WakeLockGuard with hard timeout, heartbeat, FGS `specialUse` + pre-34 fallback).
3. **ticket-003** (`feat/ticket-003-sensor-listener-module`): SensorStream + shake/flip detectors + trace-replay unit tests → this activates the **UnitTesting** stage. M1 exit: flip detected screen-off < 500 ms.
4. **ticket-004**, then **ticket-005** (ticket-005 activates **SecurityAnalysis** — import rules T1/T2 — and defines **FuzzTesting** targets).
5. At M1 completion: re-plan checkpoint (REFACTORING_PLAN Phase 3), file M2 executor tickets, decide FormalVerification scope.

## Environment notes for future sessions (important)

- **`dl.google.com` is blocked (HTTP 403) in the remote execution sandbox** → AGP/AndroidX cannot be resolved locally; **local `gradlew build` is impossible**. Maven Central is reachable. Verification happens in **GitHub Actions** (runners have full network + Android SDK). Push to a `claude/**` or convention branch to trigger CI.
- No Android SDK locally (`ANDROID_HOME` unset); Gradle 8.14 + JDK 21 available at `/opt/gradle` / `/usr/bin/java` (wrapper pins 8.11.1, JDK 17 in CI).
- Remote `origin` redirects: canonical URL is now `https://github.com/dianila68/macro-through-phone-gesture.git` (repo renamed, trailing hyphen dropped).
- Process docs: tickets in [`../tickets/`](../tickets/), workflow rules in [`../CONTRIBUTING.md`](../CONTRIBUTING.md), threat gates in [THREAT_MODEL.md](THREAT_MODEL.md).
