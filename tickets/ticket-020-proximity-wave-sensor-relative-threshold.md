# ticket-020: Proximity-Wave Trigger — Relative (maximumRange) Threshold + Discoverability

- **Milestone:** M1
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-012, ticket-013, ticket-003

## Description

Field report: *"the wave macro doesn't work, or i don't understand how to play
it."* Two distinct problems are folded into that one sentence: the detector
mis-classifies on many real devices, and the trigger is hard to discover.

### Confirmed root cause — fixed cm threshold vs `maximumRange`

`ProximityWaveDetector` in
`app/src/main/kotlin/io/github/dianila68/gesturemacro/core/sensors/ProximityWaveDetector.kt`
classifies "near" against a **fixed centimetre** threshold:

```kotlin
private val nearThreshold = lerp(NEAR_STRICT_CM, NEAR_LENIENT_CM, sensitivity)
// ...
val near = sample.v[0] < nearThreshold
```

with `NEAR_STRICT_CM = 3.0f`, `NEAR_LENIENT_CM = 8.0f`, so at the default
sensitivity (0.5) `nearThreshold ≈ 5.5 cm`.

Real proximity sensors are coarse and binary: many report "far" as the sensor's
`Sensor.getMaximumRange()`, which on a large fraction of devices is **exactly
5.0 cm**. Because `5.0 < 5.5`, the detector classifies the sensor's *far*
reading as **near**. `coveredSince` is then set on the first sample and never
clears (the far branch is never taken), so the far → near → far cycle never
completes and the wave **never fires** (or fires wrongly). The defect is purely a
consequence of comparing a device-dependent range value against an absolute
constant — any fixed cm threshold is wrong for some real device.

### Fix — classify relative to the sensor's maximum range

Binary proximity sensors report `0` for near and `maximumRange` for far.
Classification must be **relative**: e.g. `near = value < maxRange` or, with a
margin, `value < maxRange * 0.5`. The detector is **pure JVM** and cannot read
the `Sensor` object, so `maximumRange` must be **injected at construction**. The
`Sensor` is available where the stream is built (`AndroidSensorStream` /
`GestureCaptureService` / `TriggerLibrary` detector factory); thread the
`maximumRange` value through to the `ProximityWaveDetector` constructor. JVM
trace-replay testability must be preserved — the existing
`ProximityWaveDetectorTest.kt` traces must still drive the detector with no
Android dependency (i.e. pass `maximumRange` as a plain `Float`, defaulted for
tests).

### Discoverability sub-task

- Quick-add (`app/src/main/kotlin/io/github/dianila68/gesturemacro/ui/MacroCreator.kt`)
  offers only `PatternKind.SHAKE`, `FLIP_FACE_DOWN`, `FLIP_FACE_UP` (lines
  43–45), so a proximity-wave macro can be created **only** via the full editor —
  most users never find it.
- After ticket-013, the proximity sensor is only subscribed when an **enabled**
  proximity macro exists, so a wave macro that was never created means the sensor
  never runs — reinforcing the "it does nothing" impression.

## Acceptance criteria

- [ ] `ProximityWaveDetector` takes `maximumRange: Float` (injected) and
      classifies near/far **relative** to it (e.g. `value < maxRange * 0.5`),
      removing the absolute `NEAR_STRICT_CM` / `NEAR_LENIENT_CM` comparison as the
      near/far decision (sensitivity may still scale the relative fraction).
- [ ] The `Sensor.getMaximumRange()` value is read where the `Sensor` is
      available (`AndroidSensorStream` / service / `TriggerLibrary` detector
      factory) and passed into the detector at construction; the detector itself
      remains free of any Android sensor dependency.
- [ ] A device that reports far as `maximumRange == 5.0` no longer latches "near":
      a JVM trace where every far sample equals `maxRange` produces a clean
      far → near → far fire, and a far-only trace fires nothing.
- [ ] JVM trace-replay testability preserved: `ProximityWaveDetectorTest.kt`
      continues to construct the detector with a plain `Float` `maximumRange` (no
      Android), and existing positive/long-cover/single-value cases still pass,
      updated for the relative threshold.
- [ ] **Discoverability:** `MacroCreator.kt` offers `PROXIMITY_WAVE` as a
      quick-add pattern, and/or a usage hint explains the gesture ("wave your hand
      just over the top of the screen"). When the user picks it, the resulting
      macro produces a live proximity subscription (consistent with ticket-013).

## Notes / known limitations

- Relates to **ticket-013** (demand-driven sensor subscription): the wave sensor
  only runs when an enabled proximity macro exists, so the discoverability fix and
  the detector fix together are what make the feature actually usable.
- Relates to **ticket-003** (sensor module) and builds directly on **ticket-012**,
  which introduced `ProximityWaveDetector` with the fixed-cm threshold.
- Sensitivity semantics change slightly (now scales a fraction of `maxRange`
  rather than an absolute cm band); document the new meaning in the detector
  KDoc so the UI sensitivity hint stays accurate.
- Some sensors report intermediate (non-binary) distances; a relative fraction
  threshold handles both binary and graded sensors, which the fixed cm value did
  not.
