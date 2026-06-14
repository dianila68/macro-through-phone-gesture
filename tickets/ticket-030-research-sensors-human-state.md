# ticket-030: Deep research — mobile sensors & everyday human-state inference

- **Milestone:** M4
- **Priority:** P2
- **Status:** Backlog (research spike)
- **Dependencies:** —

## Description

Foundational research spike for the sensor-driven macro expansion (tickets 031–034).
Survey **what is achievable with commodity phone sensors** and **how everyday human
"change of state" is inferred** from them, so the utility functions (031), use cases (032),
and the composition model (033) are grounded in prior art rather than guessed.

Produce a written findings document under `docs/research/` (cited), not code.

## Scope to cover

- [ ] **Sensor inventory & what each yields:** accelerometer, gyroscope, magnetometer, proximity, ambient light, barometer/pressure, step counter/detector, significant-motion, rotation-vector, (where available) heart-rate / temperature; plus Android's high-level **Activity Recognition** (still/walking/running/cycling/in-vehicle) and **sensor fusion** (game/rotation vectors).
- [ ] **Human "change of state" inference:** activity transitions, gait/step cadence, picked-up / put-down, pocket vs hand vs table, entering darkness/light, altitude/floor change (stairs), heading changes, sleep/stationary detection — with the typical signal-processing technique each uses (thresholds, peak detection, windowed variance, FFT/cadence, low/high-pass filtering, state machines).
- [ ] **Prior art / how others did it:** Google Activity Recognition API, Motorola gesture actions (chop/twist), Tasker/Automate/MacroDroid sensor triggers, academic HAR (human activity recognition) approaches — what's reliable vs flaky on real devices.
- [ ] **Battery / sampling-rate / Doze constraints** for each (ties to NFR-1 and ticket-013's demand-driven subscription) — which signals are cheap (hardware step counter, significant-motion) vs expensive (continuous high-rate accel/gyro).
- [ ] **Privacy/threat notes:** which inferences are sensitive (location-via-sensors, behavioral profiling) — feed the threat model.

## Acceptance criteria

- [ ] `docs/research/sensor-human-state.md` with cited findings, a per-sensor capability table, and a recommended shortlist of *reliably-detectable* states/events to implement first.
- [ ] A short "implications" section mapping findings → concrete recommendations for tickets 031–033.

## Technical notes

- Run via the `deep-research` skill (fan-out web search + adversarial verification + cited
  synthesis). This ticket is the *brief*; the deliverable is the cited document.
