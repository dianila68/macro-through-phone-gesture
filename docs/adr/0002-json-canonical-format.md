# ADR-0002: JSON as canonical macro format, YAML at the boundary

- **Status:** Accepted (2026-06-11)
- **Context:** Macros must be exportable/importable (FR-7, NFR-5) and are the app's only untrusted input (threat T1/T2). Candidates: JSON, YAML, protobuf, custom DSL.
- **Decision:** JSON is the canonical, normative format, specified by [`schema/gesture-macro-v1.json`](../../schema/gesture-macro-v1.json) (JSON Schema 2020-12). YAML is accepted at import and offered at export purely as an alternate serialization of the identical model. The Kotlin `@Serializable` models are the single in-code source of truth; every document carries a `version` for migrations.
- **Rationale:**
  1. kotlinx.serialization gives strict, reflection-free JSON decoding (unknown-field rejection) — the security posture T2 requires.
  2. JSON Schema lets tooling/tests validate files independently of the app.
  3. YAML is human-friendlier for hand-editing/sharing, but its parser complexity (anchors, aliases, implicit typing) makes it a poor *canonical* choice; it is confined to the boundary with anchors/aliases disabled and size caps (threat T2 mitigations).
  4. protobuf rejected (not hand-editable, hostile to a community sharing format); custom DSL rejected (parser = new attack surface).
- **Consequences:** Two parsers at the boundary (JSON + YAML) must be fuzz-tested (FuzzTesting stage); schema evolution is governed — breaking changes require `gesture-macro-v2.json` plus a migration path.
