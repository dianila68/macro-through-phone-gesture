# ticket-007: Room Persistence for the Macro Store

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-005

## Description

Replace the in-memory `MacroStore` backing with Room so macros, enable/disable state, and the execution audit log (threat T4, FR-9) survive process death. The `MacroStore` API stays.

## Acceptance criteria

- [ ] Room entities/DAO for macros (serialized via `MacroCodec` or columns) and `ExecutionLog`.
- [ ] KSP wired in the version catalog for the Room compiler.
- [ ] `ActionDispatcher` results recorded as `ExecutionLog` rows (macro id, action type, result, timestamp).
- [ ] `allowBackup=false` confirmed still set; integrity HMAC column per threat T5 (key in Keystore).
- [ ] Migration story documented (schema export enabled, `Migration` test harness).
- [ ] Built-in shake-to-flashlight macro seeded on first run only.
