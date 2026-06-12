# ticket-009: On-Device M1 Verification Pass (manual)

- **Milestone:** M1
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-002, ticket-003

## Description

The M1 exit criteria that CI cannot prove require a physical device. This is a manual
verification ticket; results are recorded here and in `docs/SDLC_STATE.md`.

## Checklist

- [ ] Flip gesture detected with screen off within 500 ms (FR-2) — log timestamps from `GestureCapture` tag.
- [ ] Engine survives 24 h in background with battery exemption granted (NFR-2); heartbeat gaps reviewed.
- [ ] Service restarts after `adb shell am kill io.github.dianila68.gesturemacro` (START_STICKY).
- [ ] Built-in shake-to-flashlight macro works end-to-end with screen off.
- [ ] Doze behavior measured on at least one OEM-skinned device (restart latency documented here).
- [ ] Wakelock duty cycle observed via `adb shell dumpsys power` during a 10-minute session (NFR-1).
