# ticket-041: Persist Heartbeat writes to Room for process-death diagnostics

- **Milestone:** M3
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-007

## Description

`Heartbeat.kt` currently tracks engine liveness in-memory only. When the process dies
unexpectedly the last-beat timestamp is lost, making the `diedUnexpectedly()` check in
`MainActivity.kt` (lines 82–86) unreliable across process restarts. Persist each heartbeat
beat to Room so the UI can surface a "engine stopped at HH:MM" message after an unexpected
death.

## Acceptance criteria

- [ ] `HeartbeatEntity` added to `MacroDatabase` with at minimum an `id` and `lastBeatMs` column.
- [ ] `HeartbeatDao` with `upsert` and `lastBeat` query methods.
- [ ] `Heartbeat` class writes to Room on each beat and on clean stop.
- [ ] `MainActivity` reads the persisted last-beat timestamp and shows an "engine stopped at HH:MM" message when `diedUnexpectedly()` is true.
- [ ] A unit test verifies the persistence path (write beat → kill in-memory state → read last beat from DB).
- [ ] All tests green; no behaviour change to engine liveness logic.

## Technical notes

- `Heartbeat.kt` exists but is in-memory only; extend it rather than replacing it.
- The `diedUnexpectedly()` check in `MainActivity.kt` at lines 82–86 is already present and
  just needs the persisted timestamp wired in.
