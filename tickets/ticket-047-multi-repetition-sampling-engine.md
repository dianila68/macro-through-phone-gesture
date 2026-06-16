# ticket-047: Multi-repetition sampling engine — repetition quality scoring

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-046

## Description

After each recording window closes, the engine must give the user immediate
feedback on whether that repetition was **usable** before asking for the next one.
A repetition might be unusable if the window was too short (user barely moved), too
long (user held the phone still throughout), or if the motion magnitude is below
noise floor.

This ticket implements per-window **quality scoring** and a running **coverage
report** shown in the UI between repetitions.

## Quality score per window

Score is in [0.0, 1.0]. Factors (weighted equally at 0.33 each):

| Factor | Poor (→ 0) | Good (→ 1) |
|---|---|---|
| **Duration ratio** | `durationMs / maxWindowMs` < 0.1 (too short) or > 0.9 (likely no stillness close) | 0.2 – 0.7 |
| **Peak magnitude** | accelerometer peak < 1.5 m/s² (noise floor) | > 4 m/s² |
| **Spectral spread** | dominant frequency bin > 80 % of total power (very stereotyped, may be noise) | < 50 % |

A window with score < 0.4 is marked **LOW_QUALITY**; the engine still keeps it
(it won't be discarded immediately) but warns the user and increments a retry
budget. If the running low-quality count exceeds `ceil(requiredSamples / 2)` the
session transitions to `INSUFFICIENT_DATA` early.

## Running coverage report

After each window the engine computes how much of the "gesture space" has been
explored so far, as a rough proxy for confidence:

- Compute pairwise DTW distance (simplified: Euclidean distance on 20-point
  downsampled magnitude traces) between all collected windows.
- Coverage score = 1 − (stdDev of pairwise distances / mean pairwise distance).
  High variance = diverse samples = better coverage of natural variation.
- Emit `CoverageUpdate(windowIndex, qualityScore, coverageScore)` on a `SharedFlow`
  that the UI (ticket-050) subscribes to.

## Acceptance criteria

- [ ] `RepetitionQualityScorer` in `core/recording/`; pure JVM; takes a `SampleWindow`,
  returns a `WindowQuality(score: Float, rating: QualityRating, factors: Map<String,Float>)`.
- [ ] `CoverageTracker` takes the list of scored windows so far and returns
  `CoverageReport(coverageScore: Float, lowQualityCount: Int, isEarlyAbortRecommended: Boolean)`.
- [ ] `GestureRecordingSession` (045) integrates: after each window close, score it,
  update coverage, emit `CoverageUpdate`, decide whether to abort early.
- [ ] Unit tests: synthetic traces for each quality bucket (too short, noise-floor,
  good motion); coverage increases monotonically with diverse samples (use three
  distinct synthetic traces).
- [ ] DTW distance computation is O(n²) on the downsampled (20-point) trace only —
  do not run DTW on raw 50 Hz frames.
