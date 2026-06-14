# ticket-036: Targeted media control via MediaController (Notification Listener)

- **Milestone:** M3
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-017

## Description

Upgrade media control from the global, untargetable `dispatchMediaKeyEvent` to **targeted** control
of a chosen app's media session (ADR-0004 Tier 3). Lets a macro say "next track **in Spotify**"
rather than "next track in whatever is playing", and exposes `playFromSearch` as a best-effort path.

## Acceptance criteria

- [ ] Optional `MediaSessionManager.getActiveSessions(...)` integration gated on the user enabling **Notification Listener** access (a `NotificationListenerService` + onboarding/consent screen).
- [ ] Resolve a `MediaController` for a chosen target package; issue `TransportControls` (play/pause/next/previous/seek) and `playFromSearch(query)` where supported.
- [ ] Graceful fallback to the existing global `dispatchMediaKeyEvent` when Notification Listener is off or the target has no active session.
- [ ] Surface clearly that `playFromSearch` is best-match and unreliable on some apps (e.g. Spotify) — exact playback is ticket-037.

## Technical notes

- Notification Listener is a sensitive permission with its own Play disclosure expectations — fold
  into the ADR-0004 compliance posture (ticket-039).
- Keep the executor behind the `ActionCatalog`/`ActionExecutor` SPI (ADR-0003).
