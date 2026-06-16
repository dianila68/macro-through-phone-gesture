# ticket-060: [FUTURE v2.0] IoT device orchestration

- **Milestone:** FUTURE — do not schedule before v2.0
- **Priority:** P4
- **Status:** Deferred

## Description

Scope gate: this ticket captures everything that would turn GestureMacro into
an IoT orchestrator rather than a macro/gesture trigger layer. It is intentionally
deferred so the v1.x line stays focused.

**Excluded from v1.x by design:**

- Device state management (on/off/dim/color per device)
- Scene / group management ("goodnight scene")
- Two-way device control (read state back from actuators)
- Native integrations with Zigbee, Z-Wave, Thread/Matter
- Push notifications FROM devices back to the app
- Device-to-device automation rules (light turns on when motion sensor fires)
- Built-in broker (MQTT or otherwise)

**v1.x boundary:** the app fires a webhook or MQTT publish (ticket-056) when a
gesture macro triggers. The user's existing Home Assistant / Node-RED / IFTTT
handles actuation. The app is the **trigger layer only**.

## What would unlock this ticket

- User research confirming ≥ 30 % of users want in-app device control
- Dedicated maintainer for the IoT surface (separate from gesture/sensor work)
- Decision to publish a companion "GestureMacro Hub" backend service

## Notes

- BLE sensor INPUT (tickets 054–055, 057) is NOT IoT orchestration — it stays
  in v1.x because it uses the existing SensorStream seam with zero engine changes.
- This ticket is a deliberate scope fence, not a technical blocker.
