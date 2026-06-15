# ticket-052: Gesture replay validation — live test mode in the sub-editor

- **Milestone:** M3
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-049, ticket-050, ticket-051

## Description

The **"Test it"** button in the review screen (ticket-050) opens a 10-second live
validation window: the just-recorded envelope is loaded into a temporary
`RecordedGestureDetector` instance and the user performs the gesture once. The UI
shows whether the detector fired, how confidently, and annotates the match on the
waveform.

This lets users verify the gesture works before saving and greatly reduces the
frustration of saving a gesture that never fires.

## Validation mode lifecycle

```
[RecordingReviewScreen] → user taps "Test it"
    → ValidationSession starts (10 s countdown)
    → User performs gesture
    → Detector fires (or countdown expires)
    → ValidationResult displayed
    → User taps "Test again" or "Save"
```

The `ValidationSession` is lightweight: it runs `RecordedGestureDetector.onFrame()`
against live sensor frames for up to 10 s, then stops. No macro is created or
enabled during this step.

## UI in RecordingReviewScreen during validation

- Countdown ring (10 s) in the top corner.
- Same scrolling waveform Canvas as the recording screen (reuse the composable).
- On match: waveform background flashes green; a "Match! Confidence: XX %" badge
  appears; countdown stops; "Test again" and "Save" buttons appear.
- On timeout (no match): red badge "No match — try moving more like you did during
  recording." with "Adjust sensitivity" link (navigates to the sensitivity slider,
  rebuilds detector with new multiplier, re-enables "Test it").
- Partial match (confidence 0.4–0.75): amber badge with advice.

## Sensitivity adjustment from review

The review screen exposes a **sensitivity slider** (Low / Medium / High) that:
1. Re-instantiates `RecordedGestureDetector` with the new `sensitivityMultiplier`.
2. Updates the macro's default sensitivity when "Save" is pressed.

This slider is the same control that will appear in the macro editor; surface it here
early so the user can calibrate before saving.

## Acceptance criteria

- [ ] `ValidationSession` class in `core/recording/`; pure JVM; takes a
  `GestureEnvelope` and a `Flow<SensorFrame>`, emits `ValidationEvent`
  (`Matched(confidence)`, `Partial(confidence)`, `TimedOut`, `Cancelled`).
- [ ] `RecordingReviewScreen` (ticket-050) integrates the validation UI as described.
- [ ] Sensitivity slider updates the detector live (no screen navigation required).
- [ ] On configuration change during validation, the session resets to READY state
  (not mid-validation) — same as the recording session.
- [ ] Unit test: synthetic replay of the averaged envelope trace through
  `ValidationSession` produces `Matched` with confidence ≥ 0.85.
- [ ] No macros are written to Room during the validation step; persistence happens
  only on "Save".

## Out of scope

- Logging validation attempts to audit log (may add in M4).
- Allowing validation against a *different* gesture than the one just recorded.
