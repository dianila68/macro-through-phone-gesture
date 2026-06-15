# ticket-050: Condition builder UI (AND / OR / NOT combinator tree in MacroEditor)

- **Milestone:** M5
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-033, ticket-046, ticket-045

## Description

The `ConditionEvaluator` and `Condition` model are wired into the engine (ticket-033) and
serializable (ticket-046), but there is no UI to build conditions. This ticket adds a
**condition tree builder** to the macro editor, letting users express logic like:

> "Heading changed, BUT ONLY while walking AND in the dark"
> → `And(Pattern(HEADING_CHANGED), Pattern(IS_WALKING, isState=true), Pattern(GOING_DARK, isState=true))`

## Acceptance criteria

- [ ] **Condition section** in `MacroEditor` below the trigger picker, collapsed by default
  ("No extra conditions — fires anywhere, anytime" when empty).
- [ ] **Flat chip list (default view):** for simple cases (0–3 state guards), show inline chips
  like `[+ AND condition]`. Each chip is one `Pattern` leaf; chips are implicitly AND-combined.
  This covers 90 % of user scenarios without exposing tree semantics.
- [ ] **Advanced tree view:** an "Advanced" toggle reveals the full AND/OR/NOT tree editor.
  Nodes are rendered as indented cards. Add-child buttons appear per node. NOT is a unary
  wrapper that the user wraps around an existing node.
- [ ] **Pattern picker:** selecting a leaf opens a bottom sheet listing all `GesturePattern`
  values with friendly names and an "is a state guard" toggle. State guards are patterns that
  stay active (IS_WALKING, IS_STATIONARY, GOING_DARK) vs. point events (STEP_DETECTED, SHAKE).
- [ ] **Depth limit:** max 3 nesting levels enforced in the UI (the engine has no limit, but
  deep trees are confusing on a phone screen).
- [ ] **Preview text:** below the tree, show a human-readable sentence summarising the condition
  (e.g. "fires when: walking AND dark").
- [ ] `ConditionEvaluatorTest` updated to cover the state guards exposed via UI.

## Technical notes

- State guard patterns to expose: `IS_STATIONARY`, `IS_WALKING`, `IS_RUNNING`, `GOING_DARK`,
  `GOING_BRIGHT`, `ALTITUDE_RISE`, `ALTITUDE_FALL`, `HEADING_CHANGED`, `IS_IN_VEHICLE`.
- Event patterns for AND gates: `STEP_DETECTED`, `FALL`, `SHAKE`, `PICKED_UP`, `PROXIMITY_WAVE`.
- The `Condition` model is a sealed class; the tree editor maps directly onto it. Persist via
  `MacroEditorViewModel` state (ticket-045); commit to `GestureMacro.condition` on save.
- UX reference: MacroDroid uses a flat chip list; Tasker uses a tree. Our hybrid (chip list +
  advanced mode) minimises learning curve while supporting power users.
- Use `AnimatedVisibility` for the tree expansion; `LazyColumn` for deep trees.
