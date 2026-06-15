# ticket-049: Recorded gesture trigger — live envelope matcher

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-048, ticket-011

## Description

Expose the recorded gesture as a first-class trigger in the `TriggerLibrary` so it
can be assigned to a macro like any built-in gesture. The live detector loads the
`GestureEnvelope` (ticket-048) at startup and continuously matches the incoming
sensor stream against the tolerance band.

## GesturePattern extension

Add `GesturePattern.RECORDED(id: String)` (or a typed subclass) to the pattern
enum/sealed class. The `id` maps to a stored `RecordedGestureEnvelope` row in Room
(ticket-051). A macro's trigger field can reference any stored id.

## RecordedGestureDetector

```kotlin
class RecordedGestureDetector(
    private val envelope: GestureEnvelope,
    private val sensitivityMultiplier: Float,   // derived from macro sensitivity slider
) : GestureDetector {
    override val requiredSensors: Set<SensorChannel>
    override fun onFrame(frame: SensorFrame): DetectionResult?
}
```

### Matching algorithm

The detector maintains a **sliding buffer** of the last `durationMeanMs + 2*durationStdMs`
milliseconds of sensor frames (upper bound; keeps memory bounded). When the buffer
holds at least `durationMeanMs − durationStdMs` ms of data:

1. Extract the most recent `durationMeanMs` ms of frames (or the full buffer if shorter).
2. Time-normalise to `envelope.sliceCount` points (same resampling as the builder).
3. Per slice, check: `|magnitudeSample[i] − mean[i]| ≤ k × std[i]` where `k = sensitivityMultiplier`.
4. **Match fraction** = fraction of slices that pass. Emit `DetectionResult` (with
   confidence = match fraction) when match fraction ≥ 0.75 (threshold is
   sensitivity-adjusted: low=0.85, medium=0.75, high=0.65).
5. Apply the normal cooldown from `MacroEngine` after a match.

## Acceptance criteria

- [ ] `RecordedGestureDetector` in `core/triggers/`; pure JVM.
- [ ] `TriggerLibrary` can instantiate a `RecordedGestureDetector` given an envelope
  loaded from storage; demand-driven subscription unchanged (ticket-013 contract respected).
- [ ] Sensitivity multiplier derived from the macro sensitivity setting (1–5 slider maps
  to k ∈ [1.2, 3.0] linearly).
- [ ] Unit tests (trace-replay style):
  - Positive: replay of the averaged recorded trace matches (confidence ≥ 0.9).
  - Negative: a shake trace does not match a twist envelope (confidence < 0.4).
  - Partial trace (50 % of expected duration): does not fire.
  - Sensitivity boundary: same trace, low vs. high sensitivity → different thresholds.
- [ ] Sliding buffer is bounded: assert memory stays < 200 KB for a 5-second recording
  at 50 Hz with 3 channels × 3 floats × 4 bytes ≈ 540 bytes/frame → ~27 000 bytes for
  5 s; well within budget even at the generous upper bound.
