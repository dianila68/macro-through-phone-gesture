# Threat Model

> SDLC stage: **ThreatModeling** · Status: Baseline v1 (2026-06-11) · Method: STRIDE per asset
> Inputs: [REQUIREMENTS.md](REQUIREMENTS.md) · Outputs gate: NFR-3 (all ≥ Medium threats mitigated before the affected feature ships)

## Assets & trust boundaries

| Asset | Why it matters |
|---|---|
| A1 — **MacroAccessibilityService** | Can act inside any app on the device; the single most powerful capability we hold |
| A2 — **Macro import parser** | Only place untrusted external data (shared macro files) enters the app |
| A3 — **GestureCaptureService + sensor stream** | Continuous motion data; availability target of OS/OEM killers |
| A4 — **Macro store (Room DB)** | Tampering here = persistent arbitrary action execution |
| A5 — **M4 BLE/LAN bridge** | Network-reachable trigger source; remote attack surface |

Trust boundaries: (B1) macro file → parser; (B2) app ↔ Accessibility framework; (B3) external device ↔ host over BLE/LAN; (B4) other apps ↔ our exported components.

## Threats (STRIDE) and mitigations

| ID | Asset | STRIDE | Threat | Risk | Mitigation | Enforced at |
|---|---|---|---|---|---|---|
| T1 | A2 | Tampering/EoP | Malicious shared macro contains `accessibility` actions that drive banking/settings apps when imported | **High** | Imported macros with `accessibility` actions persist **disabled** regardless of file's `enabled` flag; explicit re-enable UI shows exactly what the macro can do | ticket-005 import path; NFR-7 |
| T2 | A2 | Tampering | Crafted JSON/YAML exploits parser (billion-laughs, anchors, huge payloads, type confusion) | **High** | Strict decoding (unknown fields rejected), document size cap, YAML anchors/aliases disabled, no polymorphic deserialization outside sealed types; fuzz target in FuzzTesting stage | ticket-005 |
| T3 | A1 | EoP | Another app spoofs/binds our AccessibilityService or injects events | Medium | Service protected by `BIND_ACCESSIBILITY_SERVICE` (system-only); no exported custom actions; no IPC surface on the service | ticket-004 |
| T4 | A1 | Repudiation | Accessibility actions execute with no record → user can't audit what a macro did | Medium | Per-execution log (macro id, action, target, timestamp) in Room, surfaced in diagnostics UI (FR-9) | M2 engine |
| T5 | A4 | Tampering | On rooted/backup-extracted devices, DB edited to enable/insert macros | Medium | `allowBackup=false`; integrity column (HMAC over action list, key in Android Keystore) checked before execution of `accessibility` macros | M2/M3 |
| T6 | A3 | DoS | OEM killers / Doze stop capture; user believes protection/automation is active when it isn't | Medium | Heartbeat + visible "engine down since HH:MM" diagnostics; FGS restart (`START_STICKY`); onboarding for battery exemption | ticket-002 |
| T7 | A3 | Info disclosure | Continuous motion data could fingerprint user activity if exfiltrated | Medium | No network permission until M4; sensor data never persisted raw (only derived gesture events); NFR-4 no-analytics rule | CodeReview gate |
| T8 | B4 | EoP | Exported components (deep links, receivers) triggered by other apps to fire macros | Medium | Default `exported=false`; any exported surface requires signature-level permission + threat-model update in the PR | CodeReview gate |
| T9 | A5 | Spoofing | Unpaired device injects gesture events over BLE/LAN → remote macro execution | **High** (M4) | Mutual authentication at pairing (out-of-band code), per-session keys, replay protection (monotonic counter + MAC); unauthenticated frames dropped before parsing | M4 design doc (required before M4 code) |
| T10 | A5 | DoS | Flooding the bridge port drains battery / wedges engine | Medium (M4) | Rate limiting, backpressure, bridge isolated in its own process | M4 |
| T11 | A1 | Spoofing (social) | A "helpful macro pack" socially engineers users into enabling Accessibility for abuse | Medium | Onboarding states plainly what the service can do; import warns when a file requests accessibility capability; no silent capability escalation | ticket-004 onboarding copy |

## Residual / accepted risks

- A device attacker with root can defeat T5's integrity check (Keystore extraction caveats apply) — accepted; root is out of scope (see REQUIREMENTS non-goals).
- Accessibility misuse by the *user against themselves* (automating ToS-violating interactions in other apps) is a policy matter, not a technical control.

## Process rules

1. Every PR touching A1–A5 must reference the threat IDs it affects (CONTRIBUTING security-sensitive rule).
2. New exported component, permission, or network usage ⇒ this document updated in the same PR.
3. M4 work may not start until T9/T10 have a reviewed protocol design.
