# ticket-008: YAML Import/Export at the Boundary

- **Milestone:** M3
- **Priority:** P2
- **Status:** Done (2026-06-13 — 12-seed fuzz corpus + fail-closed regression test; control-char hardening added to the model)
- **Dependencies:** ticket-005

## Description

Complete the ADR-0002 contract: accept YAML at import and offer it at export as an alternate serialization of the identical Kotlin models (JSON stays canonical).

## Acceptance criteria

- [x] `kaml` (or equivalent) added via the version catalog; anchors/aliases disabled, strict mode on (threat T2).
- [x] Explicit-format routing via `MacroCodec.decodeYaml`; same size cap and import policy as JSON (accessibility ⇒ disabled).
- [x] Round-trip property tests: model → YAML → model identity; YAML(export) → JSON(import) equivalence.
- [x] Malformed-YAML fuzz corpus seeds checked in (FuzzTesting stage feed).
