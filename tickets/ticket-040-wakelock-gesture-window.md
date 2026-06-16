# ticket-040: Implement WakeLockGuard gesture-window wake lock

- **Milestone:** M3
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-002

## Description

The device CPU can sleep mid-gesture, dropping sensor events and causing missed or corrupted
recognitions. `WakeLockGuard.kt` exists as a stub but is not implemented or wired.
Implement it to hold a `PARTIAL_WAKE_LOCK` for the duration of a potential gesture window and
wire it into `GestureCaptureService`.

## Acceptance criteria

- [ ] `WakeLockGuard` acquires a `PARTIAL_WAKE_LOCK` when a detector signals the start of a potential gesture window.
- [ ] The lock is released when the gesture fires or a configurable timeout (default 2 s) expires, whichever comes first.
- [ ] `GestureCaptureService` uses `WakeLockGuard` for all detectors.
- [ ] `AndroidManifest.xml` includes the `WAKE_LOCK` permission.
- [ ] A unit test verifies the acquire/release lifecycle (acquire on start, release on fire, release on timeout).
- [ ] All tests green; no behaviour change outside wake-lock acquisition.

## Technical notes

- `WakeLockGuard.kt` stub already exists; it needs a full implementation and wiring in
  `GestureCaptureService`.
- Use `PowerManager.PARTIAL_WAKE_LOCK` (not `FULL_WAKE_LOCK`) — only the CPU needs to stay
  awake, not the screen.
