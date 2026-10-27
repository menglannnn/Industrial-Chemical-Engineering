# phase-ports-probes/holds

Diagnosis of the in-game island holds F1 (a pump on a tank's bottom face) and F2 (a tank fed liquid-full with its outlets open) of the batch `documentation/2026-09-26-phase-ports-and-compressor/`, 2026-09-27, on `claude/phase-ports-compressor` at `82f2d2f0`. The review text is `REVIEW_SECTION.md` (section "Island holds F1 and F2 (2026-09-27)" for `PHASE_PORTS_REVIEW.md`). Nothing here is run by a Gradle task.

What stays in the code (for the orchestrator to commit): the fix in `PhaseLayout`, `PassiveStepSolver` and `FluidThermodynamics`, and the gate tests `src/test/java/com/wormzjl/createcheme/runtime/fluid/PumpBottomPortDrainTest.java` and `LiquidFullTankOutletsTest.java`. Everything below is detached material.

## src/

- `instr-holds-dump.patch` (against the fixed tree; never commit): on every Newton failure of a pass, prints the failing iterate row by row (node kind, phase state, row label, residual, a rounding floor measured by random 1-2 ulp perturbations of every unknown, the unknown), the edges with their modes, D11 segments and pressure scales, and re-solves the pass from its start on a fresh workspace with `SparseNewton.TRACE` printing each iteration (norm, worst row, step, watched unknowns). Dumps at most `HOLDS_DUMP` (6) failures, one per step-size decade. Apply with `git apply`, run the test, `git checkout` the two files.
- `option-t1-trace-flush.patch` (against the fixed tree): option T1 of the review, the prototype that removes a component below 1e-24 of its node at each accepted step's commit and re-flashes the node at its T and P (removed amount not booked). Measured in `logs/18`, `19`.
- `option-t2-trace-phase-weight.patch` (against the intermediate tree of `logs/04`-`10`, whose difference step had a 1e-290 floor; applies to the fixed tree with offsets): option T2, a trace vapour's equilibrium rows weighted by its share below 1e-12. Measured in `logs/09`; rejected.
- `TraceVapourProbeTest.java`: the properties of a vanishing nitrogen vapour over 55288 mol of water from 1e-20 to 1e-315 of it (`logs/05`).
- `LiquidFullTankLifetimeProbeTest.java`: F2 run until the tank's nitrogen is exactly gone (500 slices at most); `FedPumpLifetimeProbeTest.java`: F1's pump with a 130 kPa generator keeping the tank fed, the tank as the island's first boundary, 400 slices (`-Dholds.target` is not forwarded by Gradle; edit the default to change the target). Both need `PumpBottomPortDrainTest` beside them. To run: copy into `src/test/java/com/wormzjl/createcheme/runtime/fluid/`, `./gradlew --no-configuration-cache test --tests '*LifetimeProbeTest'`, read the XML's system-out, delete the copies.
- `StdoutCompare.java`: `java StdoutCompare.java a.txt b.txt` compares two concatenations of a suite's JUnit system-out (timings masked, map entries sorted, the new tests' lines dropped); how G1' of the review was checked.

## logs/

| file | what |
|---|---|
| `00-repro-first-run.gradle.txt` | the first run of the reproductions on `82f2d2f0` (F2 then asserted exact zero vapour; see `15`) |
| `01-dump-f1-default.*`, `02-trace-f1-default.*` | F1 (0.01 m3/s) on `82f2d2f0` + instrumentation: dumps, then dumps with the traced re-solves |
| `03-trace-f2.*` | F2 likewise |
| `04-fix-try1*` | the difference-step repair alone (with a 1e-290 floor): F1 green, F2 held at slice 195, vapour share 6.9e-160 (the `acoustic` underflow) |
| `05-trace-vapour-probe.*` | `TraceVapourProbeTest` |
| `06-fix-try2-f2.*` | plus the `acoustic` repair: F2 run on to slice 354, vapour share 1.4e-303, `Matrix values must be finite` |
| `07-*`, `08-*` | `FedPumpLifetimeProbeTest` with the generator as first boundary (400 slices clean, the junction never holds a trace) and with the tank first (held at slice 173) |
| `09-t2-*` | option T2 |
| `10-t1-*` | option T1 on the intermediate tree |
| `11-*` | the final tree: the two new test classes, green |
| `12-*` | gate G1, fluid suites: 475/475, junction lines identical, MIXED_GAS_COST 286 (`12-summary.txt`, `12-junction-lines*.txt`, `12-test-names.txt`) |
| `13-gradle-regression-exact.log` | gate G2, 0.000e+00 |
| `14-*` | gate G3, adjacent 45/45 |
| `15-*` | the fluid suites on `82f2d2f0` (the baseline of G1'), with the new tests failing (`15-baseline-new-tests-fail.txt`) |
| `16-fixed-fluid-suite-rerun.log` | the fluid suites on the final tree again, the other side of G1' |
| `17-*` | the lifetime probes on the final tree |
| `18-*`, `19-*` | option T1 on the final tree: fluid suites with the lifetime probes (477/477, junction lines identical), exact regression |

`logs/` is matched by the repository's `logs/` ignore rule: force-add it (`git add -f`), as the other phase-ports probe folders do.
