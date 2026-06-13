# ticket-012: Proximity-Wave Trigger

- **Milestone:** M1
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-011

## Description

Add the last planned trigger: a hand wave just over the top of the screen,
detected on the proximity sensor. Promotes the `PROXIMITY_WAVE` library entry
from planned to available.

## Acceptance criteria

- [ ] `ProximityWaveDetector` (pure JVM, `sensor = SensorType.PROXIMITY`): detect a
      near→far (or far→near→far) transition within a short window. Most proximity
      sensors are coarse (binary near/far, ~0 cm vs max range) — detect on the
      reported distance crossing a threshold, not on absolute value.
- [ ] Handle single-value sensor events (`event.values.size == 1`); `SensorSample.v`
      may carry one element. Confirm detectors that assume `size >= 3` are unaffected.
- [ ] Promote the library entry (`available = true`, detector factory) so the editor
      offers it and the service subscribes to the proximity sensor automatically.
- [ ] JVM trace-replay tests: positive wave, a hand resting near (no false fire),
      and accelerometer/gyroscope samples ignored.

## Technical notes

- Proximity is event-driven (fires on change), so sampling-period batching is
  largely moot; verify the merged-stream pipeline tolerates a low-rate source.
- Consider a debounce so pocket near/far flapping does not fire repeatedly (the
  engine cooldown also mitigates this).
