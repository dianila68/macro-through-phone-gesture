# ticket-053: Recorded gesture management UI — library screen

- **Milestone:** M3
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-051, ticket-050

## Description

Users may accumulate several recorded gestures over time (e.g. one for each hand
they use, or different gestures for different contexts). This ticket adds a
**Recorded Gestures** section to the existing `MacroManager` screen, letting users
rename, re-record, and delete saved gesture envelopes.

## UI additions

### In MacroManager

Add a "Recorded Gestures" collapsible section below the macro list (or as a bottom
nav tab if the layout warrants it — keep it a section for now to minimise nav changes).

Each row shows:
- Gesture name (editable inline on long-press).
- Confidence indicator (High / Medium / Low dot).
- Sample count (e.g. "5 samples").
- How many macros reference this gesture (e.g. "Used by 2 macros").
- **Re-record** icon button → opens the recording sub-editor pre-seeded with the
  same `RecordingConfig` that was used originally (loads from entity; falls back to
  defaults if not stored).
- **Delete** icon button → confirmation dialog warns if macros reference it ("2 macros
  will be disabled").

### Rename inline edit

Tapping the name enters an inline `TextField`. Confirm with IME Done or by tapping
elsewhere; cancel with back. Name must be non-empty and ≤ 40 characters.

## Acceptance criteria

- [ ] "Recorded Gestures" section visible in `MacroManager` (or the designated screen),
  driven by `RecordedGestureDao.observeAll()`.
- [ ] Empty state: "No recorded gestures yet. Record one from the trigger picker."
- [ ] Re-record opens `RecordingBriefingScreen` (ticket-050) with pre-filled config;
  on save, overwrites the existing `RecordedGestureEntity` (same id, updated envelope).
- [ ] Delete shows a confirmation dialog listing affected macro count; on confirm,
  calls `RecordedGestureStore.delete()` (cascade disable is enforced in ticket-051).
- [ ] Inline rename calls `RecordedGestureStore.upsert()` on confirm; rejects empty /
  > 40 char names with a snackbar.
- [ ] No new bottom nav tabs or Activities introduced.
