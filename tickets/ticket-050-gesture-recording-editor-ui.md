# ticket-050: Gesture recording sub-editor UI

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-045, ticket-047, ticket-010

## Description

A **Compose sub-editor** accessible from the macro trigger picker. When the user
selects "Record a gesture", this sub-editor takes over the screen and guides them
through the multi-repetition recording flow. It is a self-contained route that
returns either a `GestureEnvelope` (on success) or `null` (on cancel).

## Screens / steps

### 1. Pre-recording briefing (RecordingBriefingScreen)

Shown before any recording starts.

- Plain-language explanation: "You'll perform the same movement **N times**. Hold
  the phone however you plan to hold it when using this gesture."
- Configure-before-you-start options:
  - Repetition count slider (3–8; default 5).
  - Sensor selection toggle: Accelerometer only / Accelerometer + Gyroscope.
- **"Start recording"** primary button. **"Cancel"** secondary.

### 2. Countdown overlay (RecordingCountdownScreen)

Full-screen overlay, large countdown timer (3… 2… 1… Go!). No input; auto-advances
to the recording screen.

### 3. Active recording screen (ActiveRecordingScreen)

Shows:
- **"Repetition N of M"** heading.
- Live waveform visualisation: a scrolling line chart of the accelerometer magnitude,
  updating at ~10 fps. Implemented with a Canvas-based Compose component; no third-party
  charting library.
- **Stillness indicator**: icon that changes when the engine signals the window is about
  to close (ticket-046 stillness detection).
- Progress bar for the current window (max duration).
- Quality badge from the previous window (ticket-047): ✓ Good / ⚠ Weak / not shown for
  first repetition.

After each window closes, briefly shows **"Rest… (next in X s)"** during
`INTER_SAMPLE_PAUSE`, then the countdown resets for the next repetition automatically.

### 4. Analysis screen (AnalysingScreen)

Shown during the `ANALYSING` state. Indeterminate progress indicator.
Duration < 1 s on device; skip animation if state resolves faster than 300 ms.

### 5. Review / confirmation screen (RecordingReviewScreen)

Shows after `READY`:
- Summary: "N repetitions recorded (M used). Confidence: High / Medium / Low."
- Envelope visualisation: a shaded band chart (mean ± 1 std) for the accelerometer
  magnitude across the 30 normalised time slices (static, not live). Canvas-based.
- **"Test it"** button → temporarily enables the live detector for 10 seconds and shows
  a match indicator (ticket-052).
- **"Save"** button → returns the envelope to the macro editor.
- **"Record again"** button → resets session to IDLE and goes back to step 1.

### 6. Failure / insufficient data screen

Shown on `INSUFFICIENT_DATA`:
- Explains the issue (e.g. "Too few clear repetitions. Try moving more decisively.").
- **"Try again"** → reset. **"Cancel"** → return null.

## Navigation

The sub-editor is a modal bottom-sheet-like route in the existing Compose nav graph
(no new `Activity`). Use `NavHost` nested navigation so back-press from the briefing
screen cancels and dismisses the sub-editor.

## Acceptance criteria

- [ ] All 6 screens implemented in Compose; each is a separate `@Composable` function
  in `ui/recording/`.
- [ ] The sub-editor is reachable from `MacroEditor` trigger picker when the user
  selects "Custom recorded gesture".
- [ ] Live waveform updates at ~10 fps without causing recompositions outside the
  Canvas scope.
- [ ] Confidence displayed as "High" (≥ 0.75), "Medium" (0.5–0.75), "Low" (< 0.5).
- [ ] On rotation / process-death, the sub-editor resets to IDLE (recording state is
  ephemeral; do not attempt to survive configuration changes mid-recording).
- [ ] No third-party charting or animation libraries introduced.
- [ ] Manual test script documented in a comment at the top of the `RecordingReviewScreen`
  composable (what to verify, what the waveform should look like for a strong gesture).
