# 2026-09-27-jdk-determinism

Purpose: Make the fluid solver's results bitwise identical on every JDK: sweep of 36 Linux x64 runtimes (HotSpot 21-27 of 11 vendors, GraalVM 21/25, OpenJ9 21/25), diagnosis (the libm: HotSpot's `Math.log/exp/pow/cos/log10` intrinsics, JDK 25's `cbrt` intrinsic, OpenJ9 = fdlibm), fix = `science/math/DeterministicMath` facade (StrictMath results, allocation-free fdlibm `pow`) with a source-scan guard

Status: Implemented 2026-10-14 (0.7.0, inside the phase-ports merge). Index: [../INDEX.md](../INDEX.md).

## Documents

- [HANDOFF.md](HANDOFF.md)
- [JDK_DETERMINISM_REVIEW.md](JDK_DETERMINISM_REVIEW.md)
- [DECISION_LOG.md](DECISION_LOG.md)
- [POW_PERFORMANCE_REVIEW.md](POW_PERFORMANCE_REVIEW.md)
- [COLUMN_TESTS_REVIEW.md](COLUMN_TESTS_REVIEW.md)
- [INTEGRATION_REHEARSAL.md](INTEGRATION_REHEARSAL.md)

## Code areas

`science/math/DeterministicMath`, `FdlibmPow`, `science/fluid/**`, `science/thermo/**`, `science/material/ViscosityCorrelation`, `V3WaterProperties`, `DeterministicMathSourceTest`; tools/jdk-determinism/, tools/cloud-science-harness/; `fluidScienceTest`, `fluidSolverRegression`, `test`

## History

- In progress since 2026-09-27 (study commit `0354863` on `claude/phase-ports-compressor`, pushed
- the prototype commit `4956d2e` of the local branch `claude/jdk-determinism-wip` is preserved as `tools/jdk-determinism/prototype/0001-*.patch`
- HANDOFF.md written 2026-09-27
- three result families before, one after
- cost +17 % wall / +46 % `MIXED_GAS_COST`, optional `pow` strength reduction back to base
- re-baseline 28 junction lines and chain-100
- fluid suites 477/477, exact regression 0, adjacent 45/45, full `test` 1169/1171 with 2 column failures from the shared `V3WaterProperties`
- owner decisions D1-D5 taken 2026-09-27 (adopt, whole science, mitigate cost, re-baseline once)
- pow performance study 2026-09-27 on `claude/jdk-determinism-perf-wip` (not pushed): whole-science routing `0f3a94b6`, mitigation B hoisting `55348ff0`, A strength reduction `903e6004`, C table-driven `DeterministicPow` `0bea3493`: fluid suites -7 to -11 % and `MIXED_GAS_COST` -22 to -26 % against the base without deterministic math, column solves 3-10 % faster than base, solve counts unchanged, re-baseline and Gradle gates still to run
- the 4 JDK-sensitive column tests diagnosed 2026-09-27: all four fixtures have a zero-duty reboiler (near-singular roots), test-only re-tune `tools/jdk-determinism/column-retune.patch` NOT applied, awaiting the owner
- re-tune approved by the owner and applied in the integration rehearsal 2026-09-28 on `claude/determinism-rehearsal` (perf branch + phase-ports `db3ac1b9`, not for merging): 0 new call sites to route, guard passes, every reference moves at rounding level only, one pre-existing dew-point knife-edge test (`PhasePortPriorityTest` drain, fails on the phase-ports base under HotSpot 25 too) for the owner, fuzz clean counts unchanged within 1 seed, HotSpot 21 and 25 identical
- OpenJ9 21 identical 2026-09-28
- owner decision D8: GraalVM on Windows, Linux sweep later as separate work
- GraalVM 25 (Graal JIT) identical 2026-10-12, its own compiled `Math.pow` found tier-dependent (not called by the merged code)
- Windows sweep tool `tools/jdk-determinism/winsweep/`, first run: five runtimes identical, budget holds excepted).
- Integrated 2026-10-14 into the phase-ports batch as C4 of its close (D9): merge `a8e49dae`, the column re-tune applied, A414's `exp` routed (`a68e8e1d`), rejection reasons in insertion order (`ce4809e9`)
- gates green, references moved at rounding level, full suite 1258 / 0 after the phase-ports fixes (`INTEGRATION_REHEARSAL.md` 16)
- awaiting the `winsweep` after the phase-ports C1 and the merge to `main`

## Notes

- 2026-10-26: status corrected from "In progress": the code reached `main` in 0.7.0 with the phase-ports squash `1dba69b3` (CHANGELOG 0.7.0). Open: the `winsweep` run after that merge.
