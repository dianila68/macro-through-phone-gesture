# ADR-0003: Core/App separation with an open-source-able engine seam

- **Status:** Accepted (2026-06-13)
- **Context:** The product is to be monetized as a closed app first, then have its **core macro engine** open-sourced later — without losing revenue or letting anyone copy-paste the whole product. Today everything lives in a single `:app` module. Much of `core/*` is already pure-JVM and off-device tested, and every Android touch-point already sits behind a pure interface (`SensorStream`, `ActionExecutor`, `IntegritySealer`, `GestureDetector`). This decision records the agreed target architecture (debated by two architects to convergence).

## Decision

Split into **four Gradle modules** with a one-way dependency graph, and make the engine's Android-freeness a *compile-enforced build invariant* rather than a convention.

| Module | Plugin | Open/Closed | Contents |
|---|---|---|---|
| `:engine` | `kotlin("jvm")` (no AGP) | **OPEN** (eventually, Apache-2.0) | Pure-JVM engine + SPI contracts: `core.engine` (MacroEngine), `core.serialization` (MacroModels, MacroCodec — the format SPEC), pure `core.sensors` (the 6 detectors + `GestureDetector`/`SensorStream` interfaces + models), `core.triggers` (TriggerLibrary), pure `core.actions` (`ActionExecutor`, `ActionDispatcher`, `ExecResult`), the `IntegritySealer` **interface**, and the new SPI interfaces (`ActionCatalog`, `TriggerCatalog`, `SealerProvider`, `EngineConfig`). |
| `:engine-android` | `com.android.library` | **CLOSED** | Android bindings = thin impls of `:engine` interfaces: `AndroidSensorStream`, `Executors` (Flashlight/Media/Intent), `AccessibilityExecutor`, the **T5** `HmacSealer`/`KeystoreSealerFactory`, and Room (`MacroDatabase`/`MacroStore`/`MacroIntegrity`). |
| `:app` (hosts `catalog` package) | `com.android.application` | **CLOSED** | `ui/*` (Compose), `service/*`, app wiring, and the proprietary `…gesturemacro.catalog` package: `CuratedActionCatalog`, curated triggers, and precompiled macros (`app/src/main/assets/macros/*`). |

Allowed dependency directions (enforced by the build graph):
```
:app            ──▶ :engine,  :engine-android
:engine-android ──▶ :engine
:engine         ──▶ (pure-JVM libs only; no internal deps, no android.*)
```

### The API seam (what `:engine` exposes/consumes)
- `SensorStream` — engine consumes sensor frames; `AndroidSensorStream` (closed) supplies them.
- `ActionExecutor` + `ActionDispatcher` — engine dispatches; closed executors implement.
- `MacroCodec`/`MacroModels` — the **published, versioned format spec**, locked with golden round-trip fixtures.
- `TriggerCatalog` SPI — `:engine` ships a built-in catalog from `TriggerLibrary`; extensible.
- `ActionCatalog` SPI — **no default in `:engine`**; the closed `catalog` package provides `CuratedActionCatalog` (the moat).
- `SealerProvider` → `IntegritySealer` — engine declares the interface; the Keystore T5 impl is closed.
- An `EngineConfig` builder in `:engine` takes `sensorStream`, `actionCatalog`, `triggerCatalog`, `sealerProvider`; `GestureMacroApp` (closed) assembles it at startup.

### Licensing & repo strategy
- License `:engine` **Apache-2.0** (client app, no server → AGPL's network clause never fires; AGPL would only add adopter/contributor friction). MPL-2.0 is an acceptable file-copyleft alternative.
- **Internal module split now; no Maven publish and no public mirror until monetization is real.** The later carve is `git subtree split --prefix=engine` into a public repo — a non-event because `:engine` already depends on nothing internal and contains zero Android/closed code.
- This prevents whole-app copy-paste: the open artifact is *an engine that does nothing useful alone* — no curated `ActionCatalog`, no precompiled macros, no working `IntegritySealer`, no UI, no brand. The published **format spec** enables third-party interop without cloning.

### What stays CLOSED forever vs eventually OPEN
- **Closed forever:** T5 sealing impl, curated action catalog, precompiled macros, UI, services, billing/brand.
- **Eventually open:** `:engine` (primary artifact) + the macro format spec; *possibly* `:engine-android` bindings later, but never the Keystore sealer.

## Rationale
- Only a **Gradle module boundary** enforces "engine is Android-free"; a package convention rots the first time someone reaches for `Context`. `:engine` as a `kotlin("jvm")` module makes `import android.*` fail to compile, and gives off-device tests for free.
- The **moat is the curated catalog + precompiled macros + UX + brand, not the engine** — so the engine is the part we *give away*; the split exists to keep the closed tier cleanly closed, not to defend the engine.
- SPI keeps the dependency arrow one-way (engine ← catalog), making the eventual open-source carve a non-event.

## Consequences
- One extra module's build/wiring cost, accepted for compile-enforced Android-freeness and a zero-untangle future carve.
- A CI guard (`verifyEngineAndroidFree` + the jvm-only plugin) must fail the build on any `android.*`/`androidx.*`/`kotlinx-coroutines-android`/Keystore import under `engine/`.
- `:engine` must use `kotlinx-coroutines-core`, never `-android`.
- Migration is sequenced as tickets 021–029 (see `tickets/`), each keeping CI green; the public carve (029) is deferred to a monetization milestone.
