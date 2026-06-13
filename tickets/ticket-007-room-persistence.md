# ticket-007: Room Persistence for the Macro Store

- **Milestone:** M3
- **Priority:** P1
- **Status:** Done (2026-06-13 — HMAC integrity sealing + v1→v2 migration; emulator MigrationTestHelper deferred to integration pass, see note)
- **Dependencies:** ticket-005

## Description

Replace the in-memory `MacroStore` backing with Room so macros, enable/disable state, and the execution audit log (threat T4, FR-9) survive process death. The `MacroStore` API stays.

## Acceptance criteria

- [x] Room entities/DAO for macros (serialized via `MacroCodec` or columns) and `ExecutionLog`.
- [x] KSP wired in the version catalog for the Room compiler.
- [x] `ActionDispatcher` results recorded as `ExecutionLog` rows (macro id, action type, result, timestamp).
- [x] `allowBackup=false` confirmed still set; integrity HMAC column per threat T5 (key in Keystore).
- [x] Migration story documented (schema export enabled, `Migration` object); see note on the harness.
- [x] Built-in shake-to-flashlight macro seeded on first run only.

## Integrity sealing (threat T5)

`MacroIntegrity` (pure JVM, unit-tested) seals only macros that can drive other apps
(accessibility actions) with a Keystore-backed `HmacSHA256` over the stored JSON document.
On load it fails closed: a sealed macro whose document doesn't match its seal — or whose
seal is missing (a row inserted out-of-band into an extracted DB) — is force-disabled and
can never silently fire. Plain macros are unsealed and unaffected. With no Keystore key
available, accessibility macros also fail closed.

## Migration story

- `MacroDatabase` is `version = 2`, `exportSchema = true` → schemas written to `app/schemas/`
  via the `room.schemaLocation` KSP arg (build-verified in CI).
- `MIGRATION_1_2` adds the nullable `integritySeal` column; existing rows get `NULL`, so any
  pre-existing accessibility macro fails closed on first load after upgrade (intended).
- **Note:** the `MigrationTestHelper` instrumented test needs an emulator, which CI does not
  run; it belongs to the integration/on-device pass (tracked with ticket-009). The migration
  object and exported schema are CI-verified at build time.
