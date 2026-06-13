# ticket-012: Proximity-Wave Trigger

- **Milestone:** M1
- **Priority:** P2
- **Status:** Done (2026-06-13)
- **Dependencies:** ticket-011

## Description

Add the last planned trigger: a hand wave just over the top of the screen,
detected on the proximity sensor. Promotes the `PROXIMITY_WAVE` library entry
from planned to available.

## Acceptance criteria

- [x] `ProximityWaveDetector` (pure JVM, `sensor = SensorType.PROXIMITY`): far→near→far
      where the cover lasts `MIN_COVER_MS..MAX_COVER_MS`; below is flicker, above is
      pocket/face-down. Threshold (`v[0] < nearThreshold`) widens with sensitivity.
- [x] Handles single-value sensor events: `feed` guards on `v.isEmpty()` and reads
      `v[0]`. The merged pipeline feeds proximity samples to accel/gyro detectors too,
      but they short-circuit on `sample.sensor != …` before touching `v`, so the
      `size >= 3` assumption is never reached for them.
- [x] Promoted the library entry (`available = true`, detector factory); ticket-013's
      demand-driven pipeline subscribes to the proximity sensor only when an enabled
      macro uses this trigger.
- [x] JVM trace-replay tests: positive wave, long-cover pocket (no fire), single-value
      reading, and accelerometer samples ignored. All six triggers are now live.

## Technical notes

- Proximity is event-driven (fires on change), so sampling-period batching is
  largely moot; verify the merged-stream pipeline tolerates a low-rate source.
- Consider a debounce so pocket near/far flapping does not fire repeatedly (the
  engine cooldown also mitigates this).
