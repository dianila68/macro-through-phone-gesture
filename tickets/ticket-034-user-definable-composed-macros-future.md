# ticket-034: User-definable composed-sensor macros — FUTURE, PLAN CAREFULLY

- **Milestone:** M4+
- **Priority:** P3
- **Status:** Backlog (future — do not start yet)
- **Dependencies:** ticket-033 (and 010/018 editor work)

## ⚠️ This is a placeholder. Plan carefully before starting.

Eventually users must be able to **define their own composed-sensor macros** (the condition
trees from ticket-033 — multiple modular conditions, each with its own sensitivity multiplier)
through a **simple editor**. Do **not** begin implementation until the condition model (033) has
settled and the core/app split (ADR-0003) is in place.

Things to be careful about when this is planned for real:
- **UX vs power:** exposing a boolean condition tree + per-condition sensitivity to end users is a
  hard UX problem — keep it simple (presets first, advanced tree later).
- **Safety:** user-authored multi-sensor macros that drive other apps multiply the T1/T11 surface;
  apply the same import/accessibility fail-closed rules.
- **Validation:** the editor must build only valid, serializable condition trees (lean on the
  model invariants + format-version migration).
- **Battery:** arbitrary user conditions can subscribe many sensors at once — respect the
  demand-driven pipeline (ticket-013) and warn on expensive combinations.

No acceptance criteria yet — this ticket exists to reserve the scope and the warning.
