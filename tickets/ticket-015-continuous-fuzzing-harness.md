# ticket-015: Continuous Fuzzing Harness for the Macro Codec

- **Milestone:** M2
- **Priority:** P2
- **Status:** Backlog
- **Dependencies:** ticket-005, ticket-008

## Description

FuzzTesting is currently seeded: a 12-entry corpus + a fail-closed regression
test (`FuzzCorpusTest`) guard known-bad inputs. The gap is a *generative* harness
that explores new inputs against the JSON/YAML decode boundary (threat T2) and
fails closed (never crashes, never enables an accessibility macro on import).

## Acceptance criteria

- [ ] Property/fuzz harness (e.g. jqwik or kotlinx property testing, or a JUnit
      loop over a structured generator) exercising `MacroCodec.decode`/`decodeYaml`/
      `decodeAuto` with malformed, oversized, deeply nested, and adversarial inputs.
- [ ] Invariants asserted: never throws an uncaught exception; respects the size cap;
      any decoded macro with an accessibility action is import-disabled (T1);
      round-trip stability for valid inputs.
- [ ] New crashers are minimized and added to the `app/src/test/resources/fuzz/`
      corpus so `FuzzCorpusTest` regression-guards them forever.
- [ ] Runs in CI within a bounded time budget (e.g. fixed iteration count / seed).

## Technical notes

- Keep it deterministic in CI (fixed seed) but allow a longer local/nightly soak.
- The decoder is pure JVM, so the harness needs no Android — fits the existing
  `app/src/test` setup.
