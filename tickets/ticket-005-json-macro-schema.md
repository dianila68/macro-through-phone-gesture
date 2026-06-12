# ticket-005: Gesture-Macro JSON Schema & Serialization Models

- **Milestone:** M3
- **Priority:** P1
- **Status:** In Progress (JSON models, strict codec, import policy + tests landed; YAML and schema-file sync pending)
- **Dependencies:** ticket-001

## Description

Finalize the Gesture-Macro v1 schema (draft embedded in [ARCHITECTURE.md](../docs/ARCHITECTURE.md#gesture-macro-data-structure)) and implement the kotlinx.serialization model layer with strict validation, import/export, and a versioned migration path. The schema is the app's public contract — macros must survive export → import across devices and app versions.

## Acceptance criteria

- [x] Schema v1 frozen as `schema/gesture-macro-v1.json` in the repo (extracted from ARCHITECTURE.md, which then references the file).
- [x] `@Serializable` Kotlin models in `core/serialization` mirroring the schema (sealed `Action` hierarchy by `type`; sealed/validated trigger patterns).
- [x] **Strict import:** unknown fields rejected, schema violations produce field-level error messages, no partially-applied imports (all-or-nothing per document).
- [x] `version` dispatch: importer reads `version` first and routes to the right decoder; importing an unsupported version fails with a clear message.
- [ ] YAML accepted at import and offered at export as an alternate serialization of the identical model (single source of truth: the Kotlin models).
- [ ] Security rule enforced in the import path: macros containing `accessibility` actions are persisted **disabled** regardless of the document's `enabled` flag.
- [ ] Round-trip property tests: model → JSON → model and model → YAML → model are identity; the ARCHITECTURE.md example macro is a test fixture.

## Technical notes

- kotlinx.serialization for JSON; pick the YAML library at implementation time (e.g. `kaml`) and record the decision in this ticket.
- Schema validation can be structural-via-decoding (strict mode + init-block invariants) rather than a runtime JSON-Schema engine — but error messages must still name the offending field/path.
