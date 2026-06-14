# ticket-048: Gesture envelope builder — tolerance interval computation

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-047

## Description

After all repetitions are collected and quality-scored, the engine computes a
**GestureEnvelope**: the parameterised tolerance band that describes what the gesture
looks like across its natural variation. The envelope is what the live detector
(ticket-049) matches against at runtime.

This is the core mathematical piece of the feature. All computation is pure JVM.

## Envelope representation

```kotlin
data class GestureEnvelope(
    val version: Int = 1,
    /** Downsampled to a fixed number of time slices (default 30). */
    val sliceCount: Int,
    /** Per-slice mean accelerometer magnitude (m/s²). */
    val magnitudeMean: FloatArray,
    /** Per-slice standard deviation — the tolerance band half-width. */
    val magnitudeStd: FloatArray,
    /** Per-slice mean gyroscope magnitude (rad/s). Null if gyro not recorded. */
    val gyroMean: FloatArray?,
    val gyroStd: FloatArray?,
    /** Envelope duration stats: mean and std of window durations (ms). */
    val durationMeanMs: Float,
    val durationStdMs: Float,
    /** Source samples used (after filtering low-quality windows below threshold). */
    val sampleCount: Int,
    /** Confidence score derived from CoverageTracker. */
    val confidence: Float,
)
```

## Algorithm

1. **Filter:** drop windows with `QualityRating.LOW_QUALITY` if `sampleCount − lowCount ≥ minSamples`; otherwise keep all.
2. **Time-normalise:** resample each window's magnitude trace to `sliceCount` points using
   linear interpolation (DTW alignment is optional and not required for v1).
3. **Per-slice stats:** compute mean and std across the N resampled traces per slice.
4. **Duration stats:** compute mean and std of `SampleWindow.durationMs` across kept windows.
5. **Tolerance multiplier:** the live detector uses `mean ± k * std` where `k` is the
   user-controlled sensitivity (mapped: low = 1.5, medium = 2.0, high = 3.0). Store
   the raw mean/std; the multiplier is applied at match time.

## Acceptance criteria

- [ ] `GestureEnvelopeBuilder` in `core/recording/`; pure JVM.
- [ ] `build(windows: List<SampleWindow>, config: RecordingConfig): GestureEnvelope`
- [ ] Time-normalisation with `sliceCount` configurable (default 30; must be between 10–60).
- [ ] Unit tests:
  - Identical synthetic traces → std ≈ 0.0 per slice; confidence ≈ 1.0.
  - Three diverse-but-similar traces → non-zero std; sampleCount = 3.
  - `build()` with one LOW_QUALITY window and `minSamples = 2` using 3 total →
    keeps 2, reports sampleCount = 2.
- [ ] `GestureEnvelope` is serialisable via `kotlinx.serialization` (add `@Serializable`).
  This is needed for Room storage (ticket-051) and export.
