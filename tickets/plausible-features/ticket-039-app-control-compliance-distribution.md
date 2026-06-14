# ticket-039: App-control compliance & distribution posture

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-004

## Description

Cross-cutting policy ticket making the [ADR-0004](../docs/adr/0004-third-party-app-control-strategy.md)
compliance posture concrete, so any app-control feature (036/037/038) ships within Google Play
policy and with honest user disclosures. Mostly product/legal/UX, not algorithm work.

## Acceptance criteria

- [ ] **Accessibility:** confirm we do **not** set `isAccessibilityTool="true"`; add a prominent in-app **disclosure + explicit consent** screen before the accessibility service is offered; document the use in the (future) store listing copy.
- [ ] **Deterministic-only guarantee:** product/marketing copy and the feature set make no claim of autonomous/agentic operation or "works on every app" (banned by Play policy Oct 30 2025).
- [ ] **Per-app ToS disclaimer:** a clear in-app notice that automating a given app may breach that app's terms, that the app cannot detect this, and the user is responsible; avoid bundling presets that automate high-risk targets (banking, LinkedIn, anti-bot platforms).
- [ ] **Notification Listener** (ticket-036) gets its own disclosure/consent.
- [ ] **Distribution hedge:** document a sideload / F-Droid path alongside Play, given accessibility-review risk; ensure the build supports it (signing aside, ties to the Deployment stage).

## Technical notes

- This gates ticket-038 (accessibility fallback) and informs ticket-036 (Notification Listener).
- Sources & rationale: [docs/research/third-party-app-control.md](../docs/research/third-party-app-control.md).
