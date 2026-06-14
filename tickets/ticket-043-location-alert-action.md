# ticket-043: "Send my location to a contact" action (flagship)

- **Milestone:** M2 (safe actions) — flagship per [ADR-0005](../docs/adr/0005-product-direction.md)
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-016

## Description

The action half of the fall-alert flagship (pairs with ticket-042). On trigger: acquire the
device location and send it, with an optional message, to a **user-chosen contact** — locally,
privately, no cloud. Also usable standalone as a discreet "panic gesture" → alert.

## Acceptance criteria

- [ ] New `MacroAction` + executor: acquire location (last-known fast-path, then a current fix if available — FusedLocation or `LocationManager`), build a message with coordinates + a maps link (+ optional "I may have fallen / need help").
- [ ] Deliver to a **pre-chosen contact**: SMS (`SmsManager`) and/or the system share sheet / a high-priority notification — pick the simplest reliable path; SMS works without the recipient having the app.
- [ ] **Confirm-countdown** before sending (e.g. 15 s "Cancel") so a false trigger (esp. from fall detection) doesn't fire a false alarm; auto-send if not cancelled.
- [ ] Permission flows for location (and SMS if used) with clear, prominent disclosure; contact chosen by the user in-app.
- [ ] Works **screen-off** in the background; the dispatch is audit-logged (FR-9).

## Notes

- **Privacy is the feature:** local-only, recipient is the user's chosen contact, no telemetry — lean
  on the existing posture (NFR-4). This is sensitive data; handle accordingly.
- **Safety honesty:** best-effort, **not a replacement for emergency services**; say so in copy.
- Location + SMS are sensitive permissions — keep the disclosure honest and the scope minimal.
