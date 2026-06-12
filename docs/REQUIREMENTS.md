# Requirements

> SDLC stage: **Requirements** · Status: Baseline v1 (2026-06-11)
> Downstream consumers: [THREAT_MODEL.md](THREAT_MODEL.md), [ARCHITECTURE.md](ARCHITECTURE.md), ticket backlog.

## Problem statement

Users want hands-free, screen-free control of their phone: trigger system actions (flashlight, media) and third-party app behavior through physical gestures (shake, flip, twist, proximity wave) — reliably, even with the screen off, and without rooting the device.

## Actors

- **User** — owns the device, authors/imports macros, grants permissions.
- **Macro author (remote)** — anyone who shares a macro file; **untrusted** by default.
- **Android OS** — adversarial resource manager (Doze, OEM killers); enforces permission model.
- **Third-party apps** — targets of actions; not controllable, only drivable via public surfaces.

## Functional requirements

| ID | Requirement | Milestone |
|---|---|---|
| FR-1 | Detect configured hardware gestures (shake, double-shake, flip up/down, twist, proximity wave) while app is backgrounded, screen on or off | M1 |
| FR-2 | Gesture detection latency ≤ 500 ms from gesture completion to engine event | M1 |
| FR-3 | Capture engine runs continuously; survives screen-off, Doze, and process restart (auto-recovers) | M1 |
| FR-4 | Execute ordered action lists per macro: system toggles (flashlight), media control (play/pause/next/prev), arbitrary Intents, Accessibility-driven actions in third-party apps | M2 |
| FR-5 | Per-macro constraints: screen state, time window; per-trigger sensitivity and cooldown | M2 |
| FR-6 | User can create, edit, enable/disable, delete macros in a Compose UI | M3 |
| FR-7 | Export macros to JSON (canonical) or YAML; import with strict validation against the versioned schema | M3 |
| FR-8 | User can pause/resume the whole engine from the FGS notification | M1 |
| FR-9 | Engine surfaces diagnostics: last heartbeat, missed/killed intervals, per-macro execution log | M2 |
| FR-10 | Bridged external devices (watch/secondary phone) act as gesture trigger sources over BLE/LAN | M4 |

## Non-functional requirements

| ID | Requirement | Verified by |
|---|---|---|
| NFR-1 (Battery) | Idle monitoring drain ≤ ~2%/24h class; wakelocks only during open gesture windows, hard-timeout bounded | PerformanceTesting stage; duty-cycle measurements per detector PR |
| NFR-2 (Reliability) | ≥ 99% detection rate for calibrated gestures at default sensitivity; engine uptime ≥ 24 h unattended | trace-replay tests + soak test |
| NFR-3 (Security) | All threats in THREAT_MODEL.md rated ≥ Medium have implemented mitigations before the affected feature ships | SecurityAnalysis stage |
| NFR-4 (Privacy) | Sensor data never leaves the device (until M4, and then only to user-paired, mutually-authenticated devices); no analytics SDKs | CodeReview gate |
| NFR-5 (Portability) | Macro files round-trip across devices and app versions (schema `version` migrations) | round-trip property tests |
| NFR-6 (Compatibility) | minSdk 26, target latest stable; FGS type rules honored on API 34+ | CI matrix / lint |
| NFR-7 (Safety) | Imported macros with `accessibility` actions are disabled until explicit user re-enable; no action may run while its required service is disconnected | unit tests on import path + engine guards |
| NFR-8 (UX honesty) | Every dangerous permission (Accessibility, battery exemption) is requested with an in-app explanation of exactly what it enables | manual review checklist |

## Explicit non-requirements (v1)

- No root features, no private-API use.
- No cloud account, sync, or macro marketplace.
- No gesture *recording/learning* (custom thresholds are manual in v1).
- No control of other devices (host executes; M4 only makes *other* devices into *sensors*).

## Acceptance traceability

Milestone exit criteria in [ARCHITECTURE.md](ARCHITECTURE.md#milestone-roadmap) operationalize FR-1/2/3 (M1), FR-4/5 (M2), FR-6/7 (M3), FR-10 (M4).
