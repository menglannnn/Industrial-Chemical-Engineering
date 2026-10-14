# Column tests under the deterministic math: diagnosis and re-tune patch (unapplied)

Batch `2026-09-27-jdk-determinism`, owner decision D3 ("column needs to be fixed as well"). Written 2026-09-27 by
Claude (Opus 5.5) on `claude/jdk-determinism-perf-wip` at `570fbf9c` (whole `science/` tree routed through
`DeterministicMath`, mitigation stack B+A+C). Four column tests pass on HotSpot 21-24 `Math` intrinsics and fail on
the deterministic math (and on OpenJ9 / HotSpot 25 with the unmodified code, review `JDK_DETERMINISM_REVIEW.md` 7.4).
This review explains each and proposes test-only re-tunes: `tools/jdk-determinism/column-retune.patch`, **not applied**.
No product code, acceptance gate, residual tolerance, iteration budget or audit threshold changes.

## 1. Answer in short

- **Common cause: every one of the four fixtures has `ReboilerDuty(0)`.** With no boil-up the stripping section is
  dead: the solved state has bottom liquid of ~1e-8 mol/s, zero vapour and pinched temperatures, and the MESH
  Jacobian at (or near) the root is numerically singular (LU pivots 1e-18 to 7e-10 against `PIVOT_TOLERANCE` 1e-13
  and the 1e-12 pivot-ratio guard). Whether a Newton path converges, and whether the final-Newton certificate can be
  produced there, is decided by the last bits of `log/exp/pow`. HotSpot 21's intrinsics happened to land on the
  passing side; fdlibm, OpenJ9 and HotSpot 25 do not (or not for all four).
- The paths are chaotic, the outcomes are not: switching only the libm (same code, same seeds) changes iteration
  counts of `V3ColdStartSweepTest` cases from 3 to 41 (nominal) or 86 to 0 (`hot_low_reflux`), but all 19
  solvable cases stay SUCCESS on every runtime. Only degenerate fixtures flip outcome.
- **Recommended change: move each fixture to a live reboiler** (50 kW; 100 kW for test 3). Measured on the patched
  test classes: 19/19 pass on the deterministic math (JDK 21.0.11 and Zulu 25.0.4, bitwise the same path), on
  HotSpot 21 with the facade pointed at `Math`, on Zulu 25 with `Math`, and in Gradle. Full column/thermo/material
  suite with the patch on the deterministic math: 566 found, 565 passed, the only failure the known runner gap.
- Three solver weaknesses (W1-W3, section 7) are presented for the owner; none is implemented.

## 2. Method

- Runner `tools/jdk-determinism/perf/wbuild.sh` (javac + MiniLauncher, JDK 21.0.11 pinned via `PERF_JAVA_HOME`),
  column build of the branch; "`Math` intrinsics" = the same build with a `DeterministicMath` whose methods call
  `Math.*` (`Math.pow` for `pow`) prepended with `EXTRA_CP`, so the only difference is the libm. Zulu 25.0.4
  (the machine's `JAVA_HOME`) via `JAVA=`.
- Diagnosis probes: the test bodies replicated in a package-local probe compiled in memory (jshell + javac API, no
  files) against the branch classes, with a `V3NewtonTrace` per iteration, the LU pivots of the fresh
  finite-difference Jacobian (an instrumented copy of `V3BandedPivotedSolver` that only records the failing pivot)
  and neighbourhood grids of each candidate fixture on three runtimes. The replicas reproduce the failures
  bit for bit (same codes, iterations and residuals as the tests).
- The patched test classes were then run for real (section 8). Logs: `tools/jdk-determinism/perf/logs/column-retune/`.

## 3. `V3ExactWarmStartSweepTest.physicallyAcceptedLiquidOnlyStatePassesFreshExactInputHotStart`

a. **Intent** (`6bf00a3b` 2026-08-26, split into a positive case in `76075d7d` 2026-08-31): an *accepted*
liquid-only state, handed with its convergence certificate to a fresh exact-input hot solve (fresh problem,
evaluator, workspaces, audit), closes in 0 iterations and is still accepted. The cold solve only has to supply that
state; its helper `coldAttempt` copies a production-like cascade (continuation local blocks from a sequential
material/VLE seed, then bubble-point recovery, coarse differences, material-closed seed) that was extended every
time the solver changed (`3ad65e44` added the bubble-point step, `b42d85a0` the material-closed step).

b. **Deterministic math**: all four cold strategies stop at 128 iterations: residuals 1.03e-5, 6.5e-6, 8.5e-7,
2.07e-5 (the test message quotes the first). Trace of the material-closed attempt: from the same seed, the fdlibm
and intrinsic paths differ from the first iterations (residual 1.83 vs 0.73 at iteration 8). HotSpot 21 then lands in
41 iterations on a root with tray-1 liquid 77.65 mol/s, tray-2 and reboiler liquid 8.84e-9 mol/s and reboiler vapour
5e-100 mol/s (all feed leaves overhead). fdlibm instead heads along a zero-flow valley: tray-1 liquid = tray-2 vapour
= 0.022 mol/s shrinking ~0.75 % per iteration, residual 2.17e-5 -> 2.07e-5 over the last 8 iterations, dominant
row ENERGY_BALANCE node 2, the LU alternating between SINGULAR (pivots 1.7e-18 to 1.3e-16) and ok, so damped
Gauss-Newton crawls to the budget. On HotSpot 21 the test passed only through the 4th fallback; the first three crawl
there as well (2.7e-5, 1.2e-5, 3.2). Zulu 25 with `Math` fails too (MAX_ITERATIONS, 3.10e-5).

**Cause class**: knife edge created by a degenerate specification (dead stripping section, singular root), not a
chaotic test of a healthy case and not a solver defect exposed by fdlibm specifically.

c. **Options**: (i) re-tune: reboiler duty 0 -> 50 kW (the `reboiler_mw_0.05` point of `V3ColdStartSweepTest`);
the first cold attempt then converges directly (55 / 42 / 27 iterations on deterministic / HotSpot 21 / Zulu 25),
cold and hot audits accepted, hot solve 0 iterations. (ii) not applicable (the assertion is the intent). (iii) W1.
**Recommended: (i).**

d. **Robustness** (replica grid): 40 kW and 60 kW also pass on all three runtimes; 551 K and 548 K at zero duty fail
on the deterministic math, confirming the zero-duty family, not 550 K, is the knife edge.

## 4. `V3ExactWarmStartSweepTest.numericalTwoPhaseMatrixRemainsRejectedAfterFreshExactInputHotStarts`

a. **Intent** (`76075d7d`): negative coverage. The two-phase branch of this 300 K condenser converges to a numerical
root that the independent phase audit rejects (CONDENSER_PHASE); neither the cold certificate nor the hot restart
from it may override the rejection. Matrix = nominal plus one-factor edits (551 K, zero pressure drop, 20 kW, 50 kW).

b. **Deterministic math**: fails at the zero-drop member (550 K / 0 Pa / 0 W): the plain solve reaches residual 3.5e-9
at iteration 16 and 6.7e-15 from iteration 19, but at that root the LU pivot is 1.5e-15 to 4e-15 (< 1e-13):
`verifyFinalNewtonCorrection` cannot certify, the damped cascade finds no descent, LINEAR_SINGULAR at iteration 26
(the test message). The root is pinched: nodes 1-3 all at 453.6256 K, 55.7 mol/s liquid each, vapour 2.8e-7 and
1.4e-7 mol/s. The bubble-point retry ends LINE_SEARCH_EXHAUSTED at 8.0e-15, the coarse retry does not converge.
On HotSpot 21 the plain solve stalls instead (MAX_ITERATIONS, 2.0e-5) and the bubble-point retry certifies a root in
4 iterations. On Zulu 25 with `Math` the zero-drop member and the nominal member 550/750/0 both fail (the nominal one
is the HotSpot 25 failure of review 7.4).

**Cause class**: knife edge on the pivot threshold at a degenerate (zero-duty) root.

c. **Options**: (i) re-tune the matrix base to a 50 kW reboiler, keeping one-factor edits:
{550 K/750 Pa/0 W, 551/750/0, 550/0/0, 550/750/20 kW, 550/750/50 kW} -> {550/750/50 kW, 551/750/50 kW,
550/0/50 kW, 550/750/20 kW, 550/750/100 kW}. Passes on all three runtimes. Minimal variant: replace only 550/0/0 by
550/0/50 kW; passes on deterministic and HotSpot 21 but Zulu 25 `Math` still fails the kept nominal 550/750/0, so the
minimal variant keeps two members on the knife edge. (ii) **rejected**: asserting only "not accepted" instead of
"cold converged and certified, audit rejected" would also be satisfied by a solve that never produced a
certificate, i.e. exactly the failing case; the test would no longer check that a certificate cannot override the
phase gate. (iii) W1. **Recommended: (i), full matrix.**

d. **Robustness**: 549/750/50 kW, 550/0/40 kW, 550/0/60 kW pass on all three runtimes. The first cold attempt
converges directly on the deterministic math for all five new members (70, 28, 46, 30, 20 iterations).

## 5. `V3SideDrawCalculatorTest.legalNearFeedDrawNeverBecomesInvalidInputWhenTheDrawBlindSeedIsAvailable`

a. **Intent** (`321fd8ab` 2026-09-01, screen added in `7d08ef73` 2026-09-13): a 99-of-100 draw on tray 1 is legal
geometry, so it must never be answered INVALID_INPUT; the request-only screen types it INFEASIBLE_SPECIFICATION
(ratio 0.9706 > 0.30), and with the screen disabled a failure must name the authored tray (the typed-failure
contract: requested geometry attempted, no continuation-grid excuse).

b. **Deterministic math**: the screen part passes. The raw solve returns NONCONVERGENCE "iteration budget exhausted"
with path `cold/material-closed-fallback/material-vle-recovery/fine-fd/...`: the stage continuation fails, the
material-closed fallback runs on the draw-free surrogate, which is exactly the test-1 column, and crawls (residual
1.25e-5, ENERGY_BALANCE node 2). The failure is reported on the surrogate problem, `sideDrawDiagnostic` finds no
draw on it, so the summary has no "authored tray 2". On HotSpot 21 the surrogate converges, the draw ramp runs to
1.0 and fails with LINEAR_SOLVE_FAILURE "... side draw on authored tray 2 ...".

**Cause class**: the degenerate surrogate of test 1, plus a diagnostics gap (W2): a surrogate failure never names
the authored draw.

c. **Options**: (i) re-tune: reboiler duty 0 -> 100 kW (screen ratio unchanged 0.9706; the stage continuation now
reaches the authored geometry and its failure names tray 2). (ii) dropping the "authored tray" assertion would drop
half the contract; not recommended. (iii) W2 would make the assertion hold on every path. **Recommended: (i), and
W2 for the owner.**

d. **Robustness**: duty scan 10-200 kW on three runtimes: every point passes except 40 kW on the deterministic
math (surrogate crawl again); 70, 80, 100 and 150 kW pass on all three, hence 100 kW rather than 50 kW.

## 6. `V3LearnedRecoveryTest.anAcceptedLearnedCorrectionIsIdenticalUnderEitherRecoveryRule`

a. **Intent** (`fb0efdea` 2026-09-14): the recovery rule is reached only after the learned candidate loop has given
up, so an accepted learned correction must publish the same digest, streams, Newton evidence and path under NONE
and RAMP_HANDOFF. Premise: the column's own accepted profile, returned as the model's seed, closes by itself.

b. **Deterministic math**: the classical solve succeeds (residual 1.42e-14), but the LNN_ONLY correction from its
exported profile ends INITIALIZATION_FAILURE: 7 iterations at 1.4e-14 -> 5.8e-15, "no admissible Armijo-reducing
Newton or descent step". At the re-seeded state the direct Newton correction moves node-3 temperature by 1.2e-3 K
and the pinched bottom nodes 6-9 (536.080 K) by 8.5e-4 K (LU minimum pivot 6.9e-10), while the certificate allows
1e-6 + 1e-9 T = 1.5e-6 K; merit at 1e-28 cannot be reduced by Armijo. HotSpot 21 certifies at iteration 0.

**Cause class**: certificate knife edge on a soft (nearly singular) direction of a pinched zero-duty stripping
section; the premise, not the recovery rule, fails. Also a weakness (W3): an accepted, certified state is not always
re-certifiable once exported.

c. **Options**: (i) new fixture for this test only: `boiledDrawInput()` = the same 8-stage draw column with a 50 kW
reboiler (`column(...)` gains a duty overload; `drawInput()` and the other nine tests are unchanged). (ii) no
weaker assertion keeps the identity claim. (iii) W3. **Recommended: (i).**

d. **Robustness**: 30, 40, 45 and 55 kW pass on all three runtimes (0 or 1 correction iterations); 60 and 70 kW
fail on HotSpot 21 `Math` only (at 60 kW the classical solve itself fails there). At zero duty 550 K and 552 K fail
on the deterministic math, 549 K and 551 K pass: the premise holds at most points but sporadically fails (W3).

## 7. Solver weaknesses (for the owner; not implemented, no-optimisation rule respected)

- **W1: zero-duty (dead) stripping sections give near-singular roots.** Convergence of the cold cascade and the
  final-Newton certificate there depend on last-bit arithmetic (sections 3, 4, 6). Options: (a) keep tests and
  presets off this region and record it as a known ill-conditioned specification (what the patch does);
  (b) a request-only screen that types a zero-duty, zero-boil-up stripping section before any solve, like the
  liquid-supply screen; (c) a certificate path for rank-deficient roots (for example a minimum-norm final correction
  that leaves the undetermined direction alone), which keeps every gate but changes which step is measured.
  (b) and (c) are product decisions.
- **W2: a failure of the draw-free surrogate does not name the authored draw.** `sideDrawDiagnostic` reads the
  selected problem, which is then the surrogate. Option: when the input has draws and the selected problem has none,
  append the authored draw and the surrogate's liquid on that tray (the surrogate has the same tray numbering when
  the stage counts match). Diagnostics text only, no gate; makes test 3's contract path-independent.
- **W3: an exported accepted profile is not always re-certifiable.** LNN_ONLY from the exact accepted state fails at
  residual 1e-14 at scattered points on every runtime (section 6 d). Option: export the certificate with the profile
  so the learned path can verify it as the exact-input hot start does (test 1); changes learned-path semantics.

## 8. Measured results of the patched tests

Patched = `column-retune.patch` applied on `570fbf9c`. Test classes = `V3ExactWarmStartSweepTest` (2),
`V3SideDrawCalculatorTest` (7), `V3LearnedRecoveryTest` (10).

| run | runtime / math | result | log |
|---|---|---|---|
| unpatched, 6 tests | deterministic, JDK 21.0.11 | 2/6 (the 4 fail as quoted in the brief) | `00-unpatched-six-tests-det-jdk21.txt` |
| unpatched, 6 tests | `Math`, JDK 21.0.11 | 6/6 | `00-unpatched-six-tests-math-jdk21.txt` |
| patched, 3 classes | deterministic, JDK 21.0.11 | 19/19 | `01-patched-classes-det-jdk21.txt` |
| patched, 3 classes | deterministic, Zulu 25.0.4 | 19/19 | `01-patched-classes-det-zulu25.txt` |
| patched, 3 classes | `Math`, HotSpot 21.0.11 | 19/19 | `01-patched-classes-math-jdk21.txt` |
| patched, 3 classes | `Math`, Zulu 25.0.4 | 19/19 | `01-patched-classes-math-zulu25.txt` |
| patched, 3 classes | Gradle `test --rerun --tests` (toolchain 21, JAVA_HOME 21.0.11) | 19/19 (2 + 7 + 10) | `02-gradle-patched-classes*.txt` |
| patched, column/thermo/material suite | deterministic, JDK 21.0.11, runner | 566 found, 565 passed; failed only `V3MethaneColumnTest.freshPresetAndSavedInputRetainTheMethaneAxis` (runner gap, passes in Gradle) | `03-column-suite-det-jdk21.txt` |

Unpatched suite for comparison (same runner, this branch): 566 found, 561 passed (the 4 + the runner gap). The two
tests that failed under the fluid-only boundary (`theRampHandoffRecoversACappedCorrectionOnAnAuthoredDrawColumn`,
`nearby551KelvinColdInputPassesTheSameFreshAcceptanceGate`) pass on the whole-tree routing, patched or not.

## 9. Summary table

| test | intent | cause class | recommended change | old -> new | deterministic / HotSpot 21 `Math` / Zulu 25 |
|---|---|---|---|---|---|
| `physicallyAcceptedLiquidOnlyState...` | accepted liquid-only state survives a fresh exact-input hot start (0 iterations, accepted) | knife edge, degenerate zero-duty spec | fixture | reboiler duty 0 W -> 50 kW | pass / pass / pass (det and `Math`) |
| `numericalTwoPhaseMatrixRemainsRejected...` | a certificate cannot override the phase rejection | knife edge on the pivot threshold at a zero-duty root | fixture (matrix base) | {550/750/0, 551/750/0, 550/0/0, 550/750/20k, 550/750/50k} -> {550/750/50k, 551/750/50k, 550/0/50k, 550/750/20k, 550/750/100k} (K / Pa / W) | pass / pass / pass |
| `legalNearFeedDrawNeverBecomesInvalidInput...` | never INVALID_INPUT; a failure names the authored tray | degenerate surrogate (test-1 column) + W2 | fixture | reboiler duty 0 W -> 100 kW (screen ratio 0.9706 unchanged) | pass / pass / pass |
| `anAcceptedLearnedCorrectionIsIdentical...` | NONE and RAMP_HANDOFF identical for an accepted correction | certificate knife edge at a pinched section + W3 | new fixture for this test only | `drawInput()` (0 W) -> `boiledDrawInput()` (50 kW) | pass / pass / pass |

Replica predictions and the real runs agree on every point.

## 10. Files

- `tools/jdk-determinism/column-retune.patch`: the three test files (`git apply --check` clean on `570fbf9c`).
- `tools/jdk-determinism/perf/logs/column-retune/`: logs of section 8.
- Scratch (git-ignored, not kept): `build/perf/colfix/` (column build, `Math`-backed facade, instrumented LU copy).
