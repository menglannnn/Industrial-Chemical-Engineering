# phase-ports-probes/holds2

Diagnosis of island hold F7 (a nitrogen tank fed by a 101.3 kPa water generator with four open outlets held its island with `Interval substep limit ... Backward-Euler state change #=511`, 14 times, in the first client run of the GUI-fixes check, `../gui-fixes/NOTES.md`) of the batch `documentation/2026-09-26-phase-ports-and-compressor/`, 2026-09-27, on `claude/phase-ports-compressor` at `fdadcd45`. Review section: `PHASE_PORTS_REVIEW.md` "Island hold F7: state-change rejections on a fed tank (2026-09-27)". Nothing here is run by a Gradle task, and no code was changed: the fix is an owner decision (options O1-O3 below).

## src/

- `FedTankOpenOutletsTest.java`: the reproduction and the acceptance test of whichever option the owner picks. The rig built from blocks in the client log's order and at its online ticks, run as the world runs an island (each placement a topology event starting the slice ladder at one tick, slices cut at the next event, a hold halving the slice on a fresh `RetainedSolver`); fails on the first hold; at the end asserts ten steady slices and feed = drain. Two tests: the in-game order (generator at tick 1007: the client log's 14 held lines to the last digit) and the generator connected before the outlets (on `fdadcd45` every 5 s slice holds, to the end of the run). Fails on `fdadcd45` (`logs/05`). To run: copy into `src/test/java/com/wormzjl/createcheme/runtime/fluid/`, `./gradlew --no-configuration-cache test --tests '*FedTankOpenOutletsTest'`; to adopt an option, commit it there with the option.
- `FedTankOpenOutletsProbeTest.java`: the variant scan (`variants`: generator tick 1005-1028, generator first, every block at once, 0.01 m outlets, a 130 kPa generator; hold count, most attempts in a slice, final pool) and the step tracer (`traceInGame`, needs `instr-f7-step-trace.patch`; without it delete the two `F7_TRACE` lines).
- `instr-f7-step-trace.patch` (against `fdadcd45`; never commit): `PassiveIntervalSolver` prints every converged step's size, acceptance, the vessel's pressure and mass before and after, the relative changes, its condensed mass and the flows (`F7_STEP` lines) while `PassiveIntervalSolver.F7_TRACE` is set.
- `option-o1-capacity-time-floor.patch`: D11 capacity `m / (N max(dt, tau0))`, `tau0` = 0.05 s as written; `logs/07` used `tau0` = 5 s (edit the constant).
- `option-o2-mass-floor-liquid-capacity.patch`: the state-change measure takes a vessel's mass change over `max(m0, rho_ref V)`, `rho_ref` = 1000 kg/m3 as written; `logs/14` used 100.
- `option-o3-error-scaled-reduction.patch`: a state-change refusal scales the step by `clamp(0.9 sqrt(cap/change), 0.1, 0.9)` instead of halving, and the accepted step after it does not grow.

All patches apply to `fdadcd45` with `git apply`; revert with `git checkout src/main/java`.

## logs/

| file | what |
|---|---|
| `01-probe-variants.gradle.txt` | the first probe run (the in-game order with the generator at 1008, and every block at once at the 100-tick cadence); XML not kept |
| `02-trace-staged.*` | step trace of the generator-at-1008 run: the first full slices and the steady slice at tick 3035 (lock at 31.25 ms) |
| `03-variants.*` | baseline variant scan (`F7_END`, `F7_HELD`, `F7_SUMMARY` lines) |
| `04-trace-ingame.*` | step trace of the in-game run from the generator's connection through the first held slice (1134, 1024 attempts) and its 2.5 s retry |
| `05-baseline-new-test.*` | `FedTankOpenOutletsTest` on `fdadcd45`: both tests HELD (1134, 1097) |
| `06-o1-tau005-*` | O1 with `tau0` = 0.05 s: both tests still HELD (5534, 5497, "Phase/device active-set cycle") |
| `07-o1-tau5-*` | O1 with `tau0` = 5 s: F7 passes, 7 D11 fixtures fail (`PhasePortPriorityTest` 5, `PumpBottomPortDrainTest` 2) |
| `08-o2-*` | O2 (1000 kg/m3) on the new test plus `PumpBottomPortDrainTest`, `LiquidFullTankOutletsTest`, `PhasePortPriorityTest`, `LevelHeadTest`: 23/23 |
| `09-o3-*` | O3 on the same selection: 23/23, but still locked (443 attempts per slice) |
| `10-o2-*` | O2, fluid suites (475/478; `10-o2-summary.txt`, the failing classes' XML, the 33 junction lines and their diff against `../holds/12-junction-lines.txt`: identical, MIXED_GAS_COST 286) |
| `11-o3-*` | O3, fluid suites (476/478, one new nonconvergence; 11 junction lines moved, MIXED_GAS_COST 312) |
| `12-o2-regression-exact.log` | O2, exact regression: 0.000e+00 |
| `13-o2-variants.*` | O2, the variant scan: 0 holds in all 14 variants |
| `14-o2-floor100-*` | O2 with 100 kg/m3 on the new test and the three classes O2 moves: only `InletPhaseTest` still differs |

`logs/` is matched by the repository's `logs/` ignore rule: force-add it (`git add -f`), as the other phase-ports probe folders do.
