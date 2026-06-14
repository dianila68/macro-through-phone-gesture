# ticket-038: Accessibility UI-automation fallback (last resort) — PLAN CAREFULLY

- **Milestone:** M4+
- **Priority:** P3
- **Status:** Backlog (advanced / high-risk — gate behind compliance ticket-039)
- **Dependencies:** ticket-004, ticket-039

## Description

ADR-0004 **Tier 4**: drive an app's UI through the `AccessibilityService` when no API/intent/deep
link exists — the "scrape the node tree and inject input" path. This is the universal-but-fragile
last resort. Scope it deliberately small and **never promise it works on every app**.

## Acceptance criteria

- [ ] Action type to target a UI element by stable-ish selector (viewId / text / content-description) and `performAction(ACTION_CLICK)` / `dispatchGesture`, building on the existing `MacroAccessibilityService` (ticket-004) and `AccessibilityAction`.
- [ ] Per-flow definition (a small, user-authored or curated sequence), explicitly best-effort; degrade gracefully (node not found → fail closed + user-visible reason, not a crash).
- [ ] **Compliance is mandatory** (see ticket-039): no `isAccessibilityTool`; prominent disclosure + consent; strictly deterministic (no autonomous/agentic behaviour); user enables the service.
- [ ] Documented fragility: breaks on app updates, localization, WebView/Compose; not maintained per-app at scale.

## Technical notes

- Reuses the T1/T11 fail-closed posture for accessibility macros (import-disabled, integrity-sealed).
- Strong candidate to stay an **advanced/opt-in** feature, possibly sideload-only — see ticket-039.
- Do NOT attempt a generic "record any app" promise; curate a few high-value flows if anything.
