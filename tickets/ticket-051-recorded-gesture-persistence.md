# ticket-051: Recorded gesture persistence — Room storage & migration

- **Milestone:** M3
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-048, ticket-007

## Description

Store `GestureEnvelope` objects (and optionally the raw `SampleWindow` data) in Room
so that a recorded gesture survives app restart and can be referenced by macros via
`GesturePattern.RECORDED(id)`.

## Schema additions (Room DB version 3)

```sql
CREATE TABLE recorded_gesture (
    id          TEXT PRIMARY KEY,   -- UUID, stable across renames
    name        TEXT NOT NULL,      -- user-visible label, editable
    created_at  INTEGER NOT NULL,   -- Unix epoch ms
    envelope_json TEXT NOT NULL,    -- JSON-serialised GestureEnvelope (kotlinx.serialization)
    confidence  REAL NOT NULL,
    sample_count INTEGER NOT NULL
);
```

Raw `SampleWindow` frames are **not** persisted by default (large, not needed at
runtime). An optional debug export (ticket-052) can dump them transiently.

### HMAC seal

Apply the same HMAC-seal pattern as `MacroIntegrity` to the `recorded_gesture` table.
Tampering with the envelope JSON must fail closed at load time: a `RecordedGestureStore`
that detects a broken seal disables the affected gesture and logs an audit event rather
than crashing.

### Migration v2 → v3

Provide a `Migration(2, 3)` that creates the `recorded_gesture` table. Existing macros
are unaffected (they have no `RECORDED` triggers yet).

## DAO

```kotlin
@Dao
interface RecordedGestureDao {
    @Query("SELECT * FROM recorded_gesture ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecordedGestureEntity>>

    @Query("SELECT * FROM recorded_gesture WHERE id = :id")
    suspend fun getById(id: String): RecordedGestureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecordedGestureEntity)

    @Delete
    suspend fun delete(entity: RecordedGestureEntity)
}
```

## Cascade delete

When a recorded gesture is deleted, any macros referencing it via
`GesturePattern.RECORDED(id)` must be disabled (not deleted). Implement as a
Room-level trigger or enforced in `RecordedGestureStore.delete()`.

## Acceptance criteria

- [ ] `RecordedGestureEntity`, `RecordedGestureDao`, `RecordedGestureStore` in
  `core/data/`.
- [ ] Room DB bumped to version 3 with `Migration(2, 3)` and a `fallbackToDestructiveMigration`
  guard removed (never use destructive migration in production).
- [ ] HMAC seal applied; fail-closed load verified in a unit test (tamper one byte → gesture
  is disabled, audit event logged).
- [ ] Emulator integration test: round-trip upsert → get → delete; verify cascade disable.
- [ ] `GestureEnvelope` JSON round-trip: serialise → deserialise → field equality.
- [ ] CI emulator matrix updated to include the v2→v3 migration test.
