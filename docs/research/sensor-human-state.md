# Research: Mobile Sensors & Everyday Human-State Inference

**ticket-030 deliverable** — foundation for tickets 031–033 (feature extractors, use cases, composed conditions).

---

## 1. Sensor Inventory

| Sensor | Android type constant | What it yields | Typical sampling rate | Battery cost |
|---|---|---|---|---|
| Accelerometer | `TYPE_ACCELEROMETER` | 3-axis linear acceleration (m/s²); includes gravity | 50–200 Hz in continuous mode | Medium–high |
| Gyroscope | `TYPE_GYROSCOPE` | 3-axis angular velocity (rad/s) | 50–200 Hz | Medium–high |
| Magnetometer | `TYPE_MAGNETIC_FIELD` | 3-axis ambient magnetic field (µT) | 5–50 Hz | Low |
| Proximity | `TYPE_PROXIMITY` | Distance to nearest object (cm); most phone sensors are binary (near/far) | Event-driven | Very low |
| Ambient light | `TYPE_LIGHT` | Illuminance (lux) | Event-driven; 1–10 Hz | Very low |
| Barometer / Pressure | `TYPE_PRESSURE` | Atmospheric pressure (hPa) | 1–10 Hz | Very low |
| Step counter | `TYPE_STEP_COUNTER` | Cumulative step count since last reboot; hardware-fused | Event-driven on step | Very low (hardware) |
| Step detector | `TYPE_STEP_DETECTOR` | Event per step (one-shot); hardware-fused | Per step | Very low (hardware) |
| Significant motion | `TYPE_SIGNIFICANT_MOTION` | One-shot trigger: device moved significantly | One-shot | Negligible |
| Rotation vector | `TYPE_ROTATION_VECTOR` | Device orientation (quaternion); sensor fusion of accel + gyro + mag | 50 Hz | Medium |
| Game rotation vector | `TYPE_GAME_ROTATION_VECTOR` | Same but without mag (no magnetic north) | 50 Hz | Medium |
| Heart rate | `TYPE_HEART_RATE` | BPM; present on Wear OS and select Samsung Galaxy | ~1 Hz | Low |
| Activity Recognition | `ActivityRecognitionClient` (API) | Software classification: still / walking / running / on_bicycle / in_vehicle | ~30 s update | Very low (batched) |

### Android sensor fusion notes
- **Rotation vector** fuses accel + gyro + mag for stable orientation that doesn't drift.
- **Game rotation vector** skips mag — no heading, but unaffected by magnetic interference; best for gesture detection.
- The hardware step counter/detector is the single cheapest motion signal: wake-up capable, processed in a dedicated low-power DSP on most devices since 2015.

---

## 2. Human-State Inference Techniques

### 2.1 Activity / gait
| State / event | Primary sensor | Technique | Reliability |
|---|---|---|---|
| Walking | Step counter / accel | Cadence (steps/min, ~100–130) + magnitude peaks | High on most hardware (hardware fusion) |
| Running | Accel | Step cadence > 160/min; higher magnitude peaks | Good |
| Cycling | Accel + Activity API | Low cadence, smoother vibration pattern | Moderate without Activity API |
| Stationary / still | Step counter | No step increment for N seconds; also low accel variance | High |
| Step detected (one-shot event) | Step detector / counter delta | Delta of counter ≥ 1, or TYPE_STEP_DETECTOR event | High |
| Picked up | Accel | Deviation of magnitude from 1 g lasting > ~200 ms (gravity band departure) | Medium — false positives from car vibration |
| Put down / set on surface | Accel | Return to 1 g + low variance for > 1 s | High |
| In pocket vs hand | Proximity + accel + light | Pocket: proximity = near AND light low AND periodic micro-motion | Medium — varies by pocket depth |
| In vehicle | Accel + Activity API | Low variance micro-vibration pattern; Activity API is strongly preferred | High (API), Low (raw accel alone) |

**Signal-processing techniques for accel:**
- **Windowed variance / RMS**: fast to compute; low variance → still, high → active.
- **Peak detection over a 1 s window**: counts steps (peaks at body's natural gait frequency, ~1–2 Hz).
- **Low-pass filter** (α ≈ 0.8): isolates gravity component; difference from raw gives linear acceleration.
- **High-pass filter**: isolates motion component; magnitude → activity level.

### 2.2 Orientation / heading
| State / event | Sensor | Technique |
|---|---|---|
| Face-down / face-up | Accel | Z-axis sign: Z ≪ 0 → face down, Z ≫ 0 → face up (for Δt ≥ 500 ms) |
| Twist (wrist rotate) | Gyro | High angular velocity on Z-axis, then reversal |
| Heading change | Magnetometer | Azimuth derived from rotation vector; delta > threshold |
| Device rotated 90° | Rotation vector | Quaternion delta; also screen-orientation API |

### 2.3 Environmental / context
| State / event | Sensor | Technique |
|---|---|---|
| Entering darkness | Ambient light | Lux drops below threshold (e.g. < 10 lux) for > 500 ms |
| Entering bright environment | Ambient light | Lux rises above threshold (e.g. > 200 lux) for > 500 ms |
| Altitude rise (stairs up, elevator) | Barometer | Pressure decreases; delta over 30 s baseline > 0.5 hPa (≈ 4 m) |
| Altitude fall | Barometer | Pressure increases over baseline |
| Proximity wave | Proximity | Near → far → near transition in < 1 s |
| In call / face against screen | Proximity | Sustained near reading |

---

## 3. Prior Art

### Google Activity Recognition API
- Available since API 21 (2014); runs in Google Play Services, extremely battery-efficient (batched sensor processing off the critical path).
- Classifies: `STILL`, `WALKING`, `RUNNING`, `ON_BICYCLE`, `IN_VEHICLE`, `TILTING`, `UNKNOWN`.
- Confidence % per class. Update interval configurable (30 s is common).
- **Verdict for GestureMacro**: ideal for "is walking" state guards in composed conditions (ticket-033). Non-free if the device lacks Play Services (AOSP). Tie into `SensorStream` SPI as an optional provider.

### Motorola Chop/Twist Gestures (Moto Actions)
- Chop (karate-chop motion) → toggle flashlight: detects a sharp lateral swing (Y-axis accel spike > ~3 g, reversal within 300 ms). Repeated twice for reliability.
- Twist → open camera: rotation-vector or gyro Z-axis crossing a threshold twice.
- Both run in a low-power "sensor hub" coprocessor when the screen is off.
- **Lesson**: high-g threshold + short reversal window is more reliable than raw speed alone; false positives from everyday motion are the main failure mode.

### Tasker / Automate / MacroDroid (Android automation apps)
- Sensor triggers supported: shake, step count threshold, proximity, light level, orientation, significant-motion.
- Common UX pattern: "trigger if sensor value crosses threshold AND stays there for X seconds" — exactly the state-guard pattern in GestureMacro's Condition model.
- **Lesson**: cooldowns (2–5 s) and "stays for N seconds" debounce are essential; otherwise every fidget triggers macros.

### Academic HAR (Human Activity Recognition)
- Standard pipeline: raw accel/gyro → windowed features (mean, variance, peak count, FFT energy) → classifier (SVM, k-NN, or shallow CNN).
- Window size 1–5 s, 50% overlap; 6-axis (accel + gyro) is near-universal.
- Best results: CNN on raw signal; good results: hand-crafted features + SVM.
- **Limitation for GestureMacro**: classifier models don't transfer well to new users/devices without retraining; the fixed-threshold detectors in our codebase are more portable, just less accurate.
- **Key reference**: Shoaib et al. (2015), "A Survey of Online Activity Recognition Using Mobile Phones"; dataset accuracy 90–98 % in lab, 75–85 % in the wild.

---

## 4. Battery / Sampling-Rate / Doze Constraints

| Signal | Strategy | Wake-up capable | Doze impact |
|---|---|---|---|
| Step counter | Hardware DSP; only fire when step occurs | Yes | None (hardware event) |
| Significant motion | One-shot; re-arms after firing | Yes | None (hardware event) |
| Proximity / Light | Event-driven; low-power sensor hub on most devices | Yes | Low |
| Barometer | Low rate (1 Hz); hardware may batch | Yes | Low |
| Accel / Gyro at high rate | 50–200 Hz; keeps CPU awake via WakeLock | No (without WakeLock) | High — kills battery fast |
| Activity Recognition API | 30 s batch; uses Google Play Services coprocessor | Yes | None |

**Doze modes (Android 6+):** sensors that are not wake-up capable stop delivering events in Doze idle mode unless the app holds a WakeLock. GestureMacro's ForegroundService + WakeLockGuard keeps it awake, but at battery cost. The demand-driven subscription model (ticket-013) is critical: don't register high-rate sensors unless a macro actually uses them.

**Battery budget rules of thumb:**
- Step counter only: < 1 mAh/h.
- Proximity + light: < 2 mAh/h.
- Accel at 50 Hz continuous: ~5–15 mAh/h (device-dependent).
- Accel + gyro at 100 Hz: ~20–30 mAh/h. **Avoid unless necessary; use batching.**
- Activity Recognition API: < 1 mAh/h.

---

## 5. Privacy / Threat Notes

- **Location via sensors (sensor-based positioning):** Continuous high-rate accel + gyro can dead-reckon indoor location over minutes (academic attacks require calibration data and are impractical for casual exploitation, but remain a theoretical concern). Not relevant for GestureMacro's gesture vocabulary.
- **Behavioral profiling:** Step cadence + activity patterns + time-of-day reveal commute, sleep, and work schedules. GestureMacro runs entirely on-device; no telemetry exits the device (ADR-0005).
- **Ambient light → screen content inference:** Academic work (MIRAGE) shows lux values can weakly correlate with displayed content on some devices. Not a realistic threat in GestureMacro's model.
- **Barometer → building / floor inference:** Requires calibrated maps. Not actionable.
- **Threat model note:** The highest-risk sensing expansion is Step counter used for behavioral scheduling + LocationAlertAction's GPS — but both are scoped to the user's own data and never leave the device.

---

## 6. Recommendations for Tickets 031–033

### Reliably-detectable states and events (implement first)

| Priority | Signal | Detector approach | Notes |
|---|---|---|---|
| ✅ P0 | Step detected | Step counter delta or TYPE_STEP_DETECTOR | Already implemented |
| ✅ P0 | Stationary | Step counter no-increment for N s | Already implemented |
| ✅ P0 | Picked up | Accel magnitude departs 1 g for > 200 ms | Already implemented |
| ✅ P0 | Going dark / bright | Lux threshold crossing, sustained | Already implemented |
| ✅ P0 | Altitude rise / fall | Pressure delta over 30 s baseline | Already implemented |
| P1 | Is walking (state) | Activity Recognition API OR step cadence > 80/min | Use API where available; cadence fallback |
| P1 | In vehicle (state) | Activity Recognition API | Don't attempt raw-sensor alone |
| P2 | Heading change | Rotation vector azimuth delta | Useful for navigation-aware macros |
| P2 | Put down / set on surface | Accel magnitude returns to 1 g + low variance | Complements "picked up" |
| P3 | Pocket vs hand | Proximity + light combined | Moderate reliability; needs per-device tuning |

### Key implementation decisions
1. **Activity Recognition API** should be a `SensorStream`-style optional provider in `:engine-android`. When present, it replaces cadence-based walking/running detection. When absent (AOSP), fall back to cadence.
2. **"Is walking" as a continuous state guard** (not an event) is the most-requested composition operand for ticket-033. Implement it before heading / vehicle / pocket.
3. **High-rate sensors** (accel 100 Hz, gyro 100 Hz) already run for shake/twist/fall. No new high-rate registration is needed for step/light/pressure — those are cheap.
4. **Cooldowns of 30–60 s** are appropriate for altitude and stationary triggers; 1–5 s for step, light, picked-up.
5. **Composed condition motivating example** ("dark while walking → flashlight") can be implemented entirely with existing signals: GOING_DARK (light sensor) + IS_WALKING state (Activity Recognition or step cadence). The condition evaluator (ticket-033 `ConditionEvaluator`) is already in place; the missing piece is the IS_WALKING state guard backed by the Activity Recognition API.
