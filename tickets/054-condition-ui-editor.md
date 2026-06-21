# ticket-054: Condition UI editor

**Track:** M4 — Platform expansion  
**Depends on:** ticket-033 (composed multi-sensor conditions in engine, done)

## Context

ticket-033 added `SensorCondition` sealed variants (`AccelerationAbove`, `LightBelow`,
`StepRateAbove`) that gate macro dispatch. They are JSON-configurable but have no
editor UI yet. Users cannot set them without hand-editing macro JSON.

## Goal

A `ConditionEditorSheet` bottom-sheet Composable inside the macro editor that lets
users add / remove / edit `SensorCondition` rules visually.

## Acceptance criteria

- [ ] Opens from the macro editor “Conditions” section (add a row / expand button).
- [ ] Lists existing conditions; each row shows type, threshold, and a live sensor value.
- [ ] “Add condition” FAB offers the three current types in a picker.
- [ ] Each condition type shows a themed slider: `AccelerationAbove` (0–40 m/s²), `LightBelow` (0–20 000 lux), `StepRateAbove` (0–200 steps/min).
- [ ] Live sensor readout beneath each slider updates at 2 Hz from `SensorUtils` so users can see current values while adjusting.
- [ ] Changes persist to the `MacroEntity` JSON schema v3 on “Save”.
- [ ] Unit test: `ConditionEditorViewModelTest` — add/remove/edit round-trips the spec list.

## Implementation notes

- New file: `ui/editor/ConditionEditorSheet.kt` (Compose `ModalBottomSheet`).
- New file: `ui/editor/ConditionEditorViewModel.kt` (manages `List<SensorConditionSpec>`).
- Reuse `SensorUtils.rollingRms` for live readout subscription.
- The existing `MacroCodec` already parses `SensorConditionSpec`; no codec change needed.
