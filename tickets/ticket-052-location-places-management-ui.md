# ticket-052: Saved places management UI

- **Milestone:** M5
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-049

## Description

`SavedLocation` data model and Room storage land in ticket-049. This ticket adds the user-facing
UI to manage the saved-places library: a list screen and an add/edit screen.

## Acceptance criteria

- [ ] **Places list screen** reachable from Settings or from the macro editor's location
  constraint picker. Shows each place as a card: name, address/coordinates, radius chip.
  Swipe-to-delete with undo snackbar.
- [ ] **Add/edit place screen:**
  - Name field (required, ≤ 32 chars).
  - Coordinate entry: "Use current location" button (requires `ACCESS_FINE_LOCATION`) or
    manual lat/lon text fields with validation.
  - Radius slider: 50 m → 2000 m with labelled stops (50 m, 100 m, 250 m, 500 m, 1 km, 2 km).
  - Preview: shows approximate accuracy ("within ~100 m of Home").
- [ ] Address reverse-geocoding (optional enhancement): `Geocoder.getFromLocation()` to show
  a human-readable street/city label next to the coordinates.
- [ ] Places used by at least one macro show a "used by N macros" tag; deleting them shows a
  confirmation dialog listing the affected macros (constraint will be removed).
- [ ] Empty state: friendly illustration + "Add your first place" CTA.

## Technical notes

- No map widget required — OSM or Google Maps would add a large dependency. Coordinates +
  radius slider is sufficient for the use case.
- `Geocoder` is best-effort and blocking; call on `Dispatchers.IO` via `LaunchedEffect`.
- `FusedLocationProviderClient` or `LocationManager.getCurrentLocation()` (API 30+) for
  current-location capture. Fall back to `getLastKnownLocation()` if current not available.
