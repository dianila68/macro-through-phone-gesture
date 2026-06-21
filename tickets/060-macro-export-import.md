# ticket-060: Macro export / import via share sheet

**Track:** M4 — Portability  
**Priority:** Medium  
**Estimate:** 3 h

## Problem

Macros are stored only in Room on-device. Users cannot back them up, share them with others, or restore after re-install.

## Acceptance criteria

1. **Export:** `MacroExporter.export(macros: List<GestureMacro>): Uri` serializes the list to signed JSON (HMAC-SHA256 with a random key, key stored in Android Keystore under alias `macro_export_key`), writes to a `FileProvider`-exposed cache file, and returns its `content://` URI ready for `Intent.ACTION_SEND`.
2. **Import:** `MacroImporter.import(uri: Uri): ImportResult` reads the JSON, verifies HMAC, parses, and returns `ImportResult.Success(macros)` or `ImportResult.Failure(reason)`. On success, macros are upserted into Room (IDs preserved; duplicates update rather than insert).
3. Export button in `GestureLibrarySection` triggers share sheet.
4. Import handled via `FileProvider` intent filter in `AndroidManifest.xml`.
5. Unit tests: round-trip (export → import restores original list); tampered HMAC → `Failure`; empty list → handled gracefully.

## Affected files

- `core/data/MacroExporter.kt` (new)
- `core/data/MacroImporter.kt` (new)
- `AndroidManifest.xml` (import intent filter)
- `res/xml/file_paths.xml` (FileProvider path)
- `ui/recording/GestureLibrarySection.kt` (export button)
