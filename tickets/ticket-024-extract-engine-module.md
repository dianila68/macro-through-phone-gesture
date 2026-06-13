# ticket-024: Extract the pure-JVM `:engine` module

- **Milestone:** M2
- **Priority:** P1
- **Status:** Backlog
- **Dependencies:** ticket-021, ticket-022, ticket-023

## Description

Create the `:engine` Gradle module as `kotlin("jvm")` (no Android Gradle plugin) and move the
pure core into it (ADR-0003 step 4). This is the future open-source carve target; `:app`
depends on it.

## Acceptance criteria

- [ ] New `:engine` module, `kotlin("jvm")` only; deps limited to kotlinx-serialization, kaml, **kotlinx-coroutines-core** (never `-android`).
- [ ] Move: `core.engine`, `core.serialization`, pure `core.sensors` (6 detectors + `GestureDetector`/`SensorStream` interfaces + models), `core.triggers`, pure `core.actions` (`ActionExecutor`, `ActionDispatcher`, `ExecResult`), `IntegritySealer` **interface**, and the SPI interfaces from ticket-021.
- [ ] Relocate off-device unit tests (engine/detector/codec/trigger/integrity-interface) to `engine/src/test`; they now run on a plain JVM.
- [ ] `:app` depends on `:engine`; project builds; all moved tests green in CI.
- [ ] `:engine` has **zero** project dependencies and zero `android.*` imports.

## Technical notes

- Audit `internal` symbols the seam needs across the new module boundary; keep seam types
  `public`, the rest `internal`. Do **not** freeze a 1.0 API.
- Land the CI guard (ticket-025) immediately after, so Android-freeness is enforced before more moves.
