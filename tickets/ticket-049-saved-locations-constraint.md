# ticket-049: Saved-location constraint (macro fires only at named place)

- **Milestone:** M5
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-007, ticket-033

## Description

Users want macros that only trigger in specific contexts — "shake to flashlight" should only work
at home at night, not everywhere. This ticket adds a **named-location constraint** to macros:
the user saves named places (Home, Gym, Office…) with a center coordinate + radius, and a macro
can require "must be at [place]" or "must NOT be at [place]" as a condition.

Default: no location constraint → macro fires everywhere (v1 backward-compatible).

## Acceptance criteria

- [ ] **Data model:** `SavedLocation(id, name, lat, lon, radiusMeters)` stored in Room
  (`saved_locations` table). CRUD operations via `SavedLocationDao` and `SavedLocationStore`.
- [ ] **Macro constraint:** `GestureMacro.constraints` extended with
  `locationConstraint: LocationConstraint? = null` where
  `LocationConstraint(placeId, mode: REQUIRE | EXCLUDE)`. Null = everywhere.
- [ ] **Runtime check:** `LocationConstraintChecker` in `android.sensors` queries
  `LocationManager.getLastKnownLocation()` (GPS_PROVIDER + NETWORK_PROVIDER, best of two)
  and checks distance against saved location radius. Called in `GestureCaptureService.onGesture()`
  before dispatching. Fails open (no location permission → constraint is skipped with a logcat
  warning, NOT a fatal failure).
- [ ] **Location library in MacroEditor:** "Location" section in macro constraints shows a list
  of saved places; user picks one and sets REQUIRE / EXCLUDE; "Everywhere" is the default.
  Separate "Manage places" screen for add/edit/delete named locations with a map picker or
  manual lat/lon entry.
- [ ] **Permissions:** `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION` (API 29+)
  requested on first use of a location constraint, with rationale dialog. No permission = feature
  hidden, not crashed.
- [ ] No geofencing API used — passive `getLastKnownLocation()` check at gesture time is
  sufficient (location accuracy within 100 m is acceptable for home/gym/office scale).
- [ ] Serialization: `locationConstraint` field added to `GestureMacro` JSON (format v2 or v3).
- [ ] Unit tests: `LocationConstraintChecker` with mock location vs. saved place at various
  distances + radii.

## Technical notes

- `getLastKnownLocation()` does not wake GPS; it returns the OS cache. This is intentional —
  the app must not drain battery starting fresh location fixes. If the cache is stale (> 5 min),
  the constraint is treated as unknown → fail open.
- Radius suggestions: home/office ≈ 100 m; neighbourhood ≈ 500 m. Offer a slider 50–2000 m.
- `ACCESS_BACKGROUND_LOCATION` is needed on API 29+ for location checks while the foreground
  service runs with the screen off. On API 28 and below, `ACCESS_FINE_LOCATION` suffices.
- On API 30+ (Android 11+), "Allow all the time" is removed from the in-app dialog; send users
  to Settings via `ACTION_APPLICATION_DETAILS_SETTINGS` with a clear rationale dialog first.
- Always request `ACCESS_FINE_LOCATION` before `ACCESS_BACKGROUND_LOCATION` (two separate
  `requestPermissions()` calls — the system requires foreground grant first).
- Declare `foregroundServiceType="location"` on `GestureCaptureService` for API 29+.
- Do NOT use `GeofencingClient` for the constraint check — it requires Play Services and a
  PendingIntent round-trip. Direct distance check is simpler and more reliable.
- `Location.distanceBetween()` gives accurate great-circle distance in metres.
