# ticket-044: "Play sound / voice" action (everyday delight)

- **Milestone:** M2 (safe actions) per [ADR-0005](../docs/adr/0005-product-direction.md)
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-016

## Description

The original delight use case — "push the phone forward → play a 'no' sound". On trigger, play a
**bundled sound**, a **user-chosen audio clip**, or a **spoken phrase (TTS)**. The archetypal safe
local reaction: instant, reversible, affects only the user.

## Acceptance criteria

- [ ] New `MacroAction` + executor that can: play a bundled sound effect; play a user-picked audio file (via SAF, no storage permission); or speak a user-entered phrase via `TextToSpeech`.
- [ ] A few bundled sounds shipped; the editor lets the user pick a bundled sound, choose a file, or type TTS text.
- [ ] Correct audio handling: request transient **audio focus**, respect the chosen stream/volume, work with the screen off; don't fight active media unnecessarily.
- [ ] Trace/JVM-test the executor selection logic where feasible; the Android audio call is the thin device-bound part.

## Notes

- Pure safe-local action — no permissions beyond audio; ideal first "wow" feature to pair with the
  shake/flip/push gestures.
- Slots into the action catalog (ticket-016) as a first-class, friendly entry.
