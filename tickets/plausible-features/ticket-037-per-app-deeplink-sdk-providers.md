# ticket-037: Per-app deep-link / official-SDK action providers (Spotify first)

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-017, ticket-036

## Description

The answer to "open Spotify and play a **specific** song": ADR-0004 **Tier 1**. A pluggable
provider model where a specific app contributes high-fidelity actions (exact playback, deep links)
via its own deep links or official SDK. Spotify is the first provider; the model must generalise
so other apps can be added without touching the engine.

## Acceptance criteria

- [ ] An `AppActionProvider` SPI (registered through the catalog, ADR-0003): given a target + intent ("play track/playlist/search"), build the concrete mechanism (deep link or SDK call).
- [ ] Spotify provider: `spotify:` deep links (`track`/`album`/`playlist`/`search`) for open/navigate; document that **exact auto-play needs the App Remote SDK** (`app-remote-control` scope) **+ Premium**, and scope whether we integrate the SDK or stop at deep links for v1.
- [ ] The catalog advertises only what each provider can actually deliver (e.g. "Spotify: play specific track" appears only if the App Remote path is implemented; otherwise "open in Spotify").
- [ ] Clear handling when the app is not installed / account not Premium / SDK auth declined.

## Technical notes

- Per-app, opt-in, **does not scale to every app** — that's expected (ADR-0004). Only curate
  providers worth the maintenance.
- App Remote/Web API carry OAuth + registration + Premium constraints; capture them as provider
  prerequisites, not engine concerns.
