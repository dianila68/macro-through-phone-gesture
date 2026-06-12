# ticket-008: YAML Import/Export at the Boundary

- **Milestone:** M3
- **Priority:** P2
- **Status:** In Progress (kaml wired, decodeYaml/encodeYaml + 7 tests landed; fuzz corpus seeds pending)
- **Dependencies:** ticket-005

## Description

Complete the ADR-0002 contract: accept YAML at import and offer it at export as an alternate serialization of the identical Kotlin models (JSON stays canonical).

## Acceptance criteria

- [x] `kaml` (or equivalent) added via the version catalog; anchors/aliases disabled, strict mode on (threat T2).
- [x] Explicit-format routing via `MacroCodec.decodeYaml`; same size cap and import policy as JSON (accessibility ⇒ disabled).
- [x] Round-trip property tests: model → YAML → model identity; YAML(export) → JSON(import) equivalence.
- [ ] Malformed-YAML fuzz corpus seeds checked in (FuzzTesting stage feed).
