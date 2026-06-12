# ticket-002: Persistent Foreground Service (GestureCaptureService)

- **Milestone:** M1
- **Priority:** P0
- **Status:** In Progress (service backbone landed; battery-exemption onboarding and Doze measurements pending)
- **Dependencies:** ticket-001

## Description

Implement the long-lived Foreground Service that hosts gesture capture, designed to survive screen-off, Doze, and OEM task killers. This is the backbone of the entire app — without reliable background residency nothing else matters.

## Acceptance criteria

- [ ] `GestureCaptureService` declared with `foregroundServiceType="specialUse"` and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification string (API 34+ path) plus correct pre-34 fallback.
- [ ] `START_STICKY` restart behavior; service restarts after process kill (verified via `adb shell am kill`).
- [ ] Mandatory FGS notification: low-priority channel, shows engine state, includes a pause/resume macro-engine action.
- [ ] `POST_NOTIFICATIONS` runtime permission flow (API 33+).
- [ ] Battery-optimization exemption onboarding: explains why, then fires `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; app remains functional (degraded) if the user declines.
- [ ] Heartbeat persisted to Room (or DataStore until Room lands) so unexpected kills are detectable and surfaced in UI diagnostics.
- [ ] Wakelock helper utility: acquire `PARTIAL_WAKE_LOCK` with a hard timeout, scoped to gesture windows only — no indefinite locks (enforced by API design).

## Technical notes

- Doze behavior: document measured restart latencies on at least one OEM-skinned device (e.g. MIUI/One UI) in the PR.
- Keep the service free of detection logic — it only hosts the sensor module (ticket-003) and engine lifecycle.

## Security / policy

- FGS subtype justification and battery-exemption prompt are Play policy disclosure points; copy must be reviewed.
