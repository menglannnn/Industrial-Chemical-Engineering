# Integration rehearsal: deterministic math + phase-ports (`db3ac1b9`)

Batch `2026-09-27-jdk-determinism`. Written 2026-09-28 by Claude (Opus 5.5) on branch `claude/determinism-rehearsal`
(worktree `.claude/worktrees/agent-a83a489f9c1edd578`), Windows 11, Oracle JDK 21.0.11 for Gradle. A rehearsal: it
measures and reports, records no reference and is not to be merged as it stands. Tools and logs:
`tools/jdk-determinism/rehearsal/` (section 11). Sections 13 (OpenJ9) added 2026-09-28; 14 (GraalVM) and 15 (Windows
sweep of five runtimes, `tools/jdk-determinism/winsweep/`) added 2026-10-12 by Claude (Opus 5.5) on the same branch.

## 1. Conclusion

- **The merge is mechanical.** Two textual conflicts (`CHANGELOG.md`, `PhaseLayout.java`), both resolved by keeping both
  sides. Phase-ports added **no** non-correctly-rounded math call under `science/` since the merge base `a0c09c9f` (its
  only new elementary calls are three `Math.sqrt`, exempt), so **0 call sites needed routing**; the one routed site in a
  conflict hunk (`PhaseLayout.temperature`, `DeterministicMath.exp`) was kept routed. The guard
  `DeterministicMathSourceTest` exists on the determinism branch and passes on the merged tree.
- **`column-retune.patch` applies cleanly**; the 4 libm-sensitive column tests pass, as do the 2 that failed under the
  fluid-only boundary.
- **Every reference moves at rounding level only**: 28 of 33 junction lines (ledger residuals of 1e-16 to 6e-15 and
  four ratios of stagnation residues, the families the review already classified), chain-100 max 6.3e-15 relative moles,
  1.7e-13 K, flow 5.8e-15 kg/s against an allowance of 3.2e-8 kg/s, the GameTest printed values at most 1.8e-15
  relative. All Newton solve, step and iteration counts are unchanged. The junction lines are **byte-identical to the
  perf branch's B+A+C lines** (`perf/logs/50-candC-junction-lines.txt`) and chain-100 deviates from the prototype record
  by exactly the values of `POW_PERFORMANCE_REVIEW.md` section 9: the phase-ports commits since `a0c09c9f` moved none of
  them.
- **One finding: `PhasePortPriorityTest.aBottomPortDrainsItsWaterToZeroThenVentsTheGasAtFiveSecondSlices` fails**
  (fluid suites 510/511, full `test` 1204/1205). Its assertion "the water drains to exactly zero" sits on a phase
  boundary: after the water is drained the headspace is humid nitrogen at its dew point, so whether the flash leaves
  exactly no free water or a condensate of 1e-17 to 1e-14 of the vessel volume is decided by the last bits. The same
  test (and its 0.1 s twin) **already fails on the phase-ports base under HotSpot 25** (Zulu 25.0.4), so this is a
  pre-existing JDK sensitivity that the deterministic math now pins, identically on every runtime, to the failing side
  for the 5 s variant. Needs an owner decision (section 10).
- **Fuzz sensitivity** (seeds 1-1000, both distributions, 4 workers, JDK 21, control = the phase-ports base at the same
  commit): the clean count is unchanged in random (856 / 856) and moves by one in defaults (829 -> 830). Excluding
  budget (class F) holds, the math change flips 4 of 2000 seed runs between clean and held (2 each way, all in
  defaults) and changes the class of the first hold in 14 held seeds.
- **Cross-runtime**: on the merged build, HotSpot 21 (Oracle 21.0.11, family A) and HotSpot 25 (Zulu 25.0.4 and JBR
  25.0.3, family B) give identical fluid-suite output (5,907 printed lines; the only differences are a timing line and
  the per-JVM-start random order of one printed map, section 8.2), identical chain-100 and identical per-seed first holds
  in a 200-seed fuzz in both distributions, 2 budget holds excepted. The base code under the same two runtimes differs
  (3 test failures and 8.0e-15 chain-100 deviation on HotSpot 25). **Extended the same day to OpenJ9** (IBM Semeru
  21.0.12.15, Eclipse OpenJ9 0.61.0, installed with the owner's permission; section 13): merged printed lines and
  chain-100 identical to HotSpot 21, fuzz first holds identical except budget holds; the base under OpenJ9 is a third
  result. **Extended on 2026-10-12 to GraalVM** (CE 25.4.4.1.1, Graal JIT, owner decision D8; section 14): identical
  in the same checks, with Graal compiling the science code and the facade, and also with every science method
  Graal-compiled before its first call. The base under GraalVM gives the HotSpot 25 result; GraalVM's own compiled
  `Math.pow` differs from HotSpot's on rare inputs and is tier-dependent, which the merged code never calls. **A
  Windows sweep tool** (`tools/jdk-determinism/winsweep/`) now runs the whole check under any list of JDK homes; its
  first run over the five runtimes of this machine finds them identical, budget holds excepted (section 15).

## 2. Setup

| item | value |
|---|---|
| determinism head | `claude/jdk-determinism-perf-wip` = `08022481` (as expected) |
| phase-ports commit merged | `db3ac1b9` (exactly this sha) |
| merge base | `a0c09c9f` |
| rehearsal commits | `94a45b2e` merge, `83343366` column re-tune, then the tools/report commit |

## 3. Conflicts and their resolution

| file | conflict | resolution |
|---|---|---|
| `CHANGELOG.md` | both sides edited the `[Unreleased]` "Changed" list: determinism side carried the older phase-ports line plus its own JDK-determinism line; phase-ports carried its extended phase-ports line | phase-ports line taken verbatim from `db3ac1b9` (the newer, complete text), followed by the determinism line from `08022481` unchanged |
| `science/fluid/solver/PhaseLayout.java` | one hunk: determinism side had `temperature(...)` routed to `DeterministicMath.exp`; phase-ports side added `TRACE_DIFFERENCE_GUARD`, `seedDifferenceScale`, `differenceStepScale`, `traceDifferenceScale` (hold F1/F2 trace difference step) around the same lines, with `temperature` still on `Math.exp` | phase-ports block taken whole, `temperature` kept on `DeterministicMath.exp` |
| auto-merged, checked by hand | `FluidThermodynamics.java` (phase-ports' subnormal-safe `acoustic` bound, `Math.sqrt` only, and the determinism side's hoisting/skip both present), `PhaseLayout.java` rest (the Jacobian column now calls `differenceStepScale`; the hoisted `ln` of the determinism side intact), `documentation/INDEX.md`, `tools/INDEX.md` | no change needed |

## 4. Routed call sites

Rules of the batch (review 8.1, `POW_PERFORMANCE_REVIEW.md` 3-5): every `log, log10, log1p, exp, expm1, pow, cbrt, sin,
cos, tan, asin, acos, atan, atan2, sinh, cosh, tanh, hypot` in `science/` and `runtime/` goes through
`DeterministicMath`; `+ - * /`, `Math.sqrt`, `Math.fma` and the exact functions stay; integer and half-integer powers are
written as products and `sqrt`; loop-invariant calls are hoisted.

- Phase-ports diff `a0c09c9f..db3ac1b9` under `src/main`: no added line calls any of the functions above (search of the
  added lines for `Math`/`StrictMath` calls and for any bare call of those names); the only elementary calls it adds are
  three `Math.sqrt` in `FluidThermodynamics.acoustic` (exempt, correctly rounded). No new `pow`, so no integer power to
  write as a product; no new call inside a component or row loop, so nothing to hoist.
- **Routed: 0 new sites.** Kept routed through the conflict: `PhaseLayout.temperature` (`DeterministicMath.exp`).
- Merged tree scan (the guard's pattern, comments stripped by the guard): 0 raw calls in `science/`, `runtime/` and
  `src/test/.../fluid/benchmark`. Outside the scanned roots only two reporting calls remain, in the GameTest source set
  (`FluidContentionProbe` busy loop, `FluidServerBenchmark` printed time constant); neither is on a solver path.

## 5. Guard

The determinism branch has one: `src/test/java/com/wormzjl/createcheme/science/fluid/DeterministicMathSourceTest.java`
(scans `science/` whole, the column exemption lifted by `0f3a94b6`, `runtime/` and the exact-regression fixtures; fails on
any call, method reference or static import of a banned `Math`/`StrictMath` function outside the facade; asserts at least
200 files scanned), plus `DeterministicMathTest` for the kernel. Both ran in G1 and in the full `test`: **pass** (8 tests).
No new guard was needed. Not covered by any guard: iteration order of JDK immutable collections (section 8.2).

## 6. Gates

| gate | result | log (`tools/jdk-determinism/rehearsal/logs/`) |
|---|---|---|
| G1 fluid suites (`science.fluid.*`, `runtime.fluid.*`) | 511 tests in 121 classes, **510 pass**; fails `PhasePortPriorityTest.aBottomPortDrainsItsWaterToZeroThenVentsTheGasAtFiveSecondSlices` (section 9); `MIXED_GAS_COST newtonSolves=286` (unchanged); 33 junction lines, 28 differ from the phase-ports reference (section 7) | `g01-G1-*` |
| G2 exact regression, in-tree reference (the prototype's record `27f7b53c`) | fails as expected (exact): moles 5.779e-15 rel. (node 82), T 1.705e-13 K, phase fraction 3.331e-16, flow 1.111e-11 rel. (1.6e-14 kg/s, allowance 3.15e-8 kg/s); 3 accepted / 0 rejected, 4 solves, 29 iterations, 33 residual evaluations | `g01-G2-regression-exact.gradle.txt` |
| G2b exact regression against the phase-ports record (`db3ac1b9` = `a0c09c9f` `chain-100.json`, via `-PfluidRegressionReferences` on a scratch copy, nothing recorded) | moles 6.313e-15 rel. (node 23), T 1.705e-13 K, phase fraction 3.331e-16, flow 1.386e-11 rel. (5.8e-15 kg/s); same counts | `g01-G2b-*` |
| G3 adjacent selection | 45/45 | `g01-G3-*` |
| full `test` | 1205 tests, **1204 pass**, the same single failure; the 4 re-tuned column tests and `V3LearnedRecoveryTest.theRampHandoff...`, `V3NearbyInputRecoveryTest.nearby551Kelvin...` pass | `g02-*` |
| G5 fluid GameTests, run id `det-rehearsal-83343366-a`, fresh world | **33/33** | `g03-G5-gametests.gradle.txt` |

## 7. Moved references (old = phase-ports reference, new = merged tree)

Nothing was re-recorded. Relative difference = |new - old| / max(|old|, |new|).

| reference | old | new | relative difference | classification |
|---|---|---|---|---|
| 24 `MIXED_GAS_TRANSIENT` lines (`holds/12-junction-lines.txt`) | worstMoles 1.6e-16 .. 5.1e-15, worstEnergy 1.8e-16 .. 5.7e-15 | worstMoles 1.5e-16 .. 5.2e-15, worstEnergy 1.5e-16 .. 5.8e-15 (table below) | the ledgers themselves are rounding residues (gate 1e-12) | rounding; newtonSolves identical in all 24 |
| `LIQUID_JUNCTION interval=5 tanks:0.05` | mismatch(end) -0.472, perSlice [-0.356,-0.472,-0.472,-0.472] | 0.00295, [-0.356,0.00295,0.00295,0.00295] | ratio of stagnation residues | review 4.2: the mismatch freezes once the two tanks have equalised; OpenJ9 printed the new value on the base code; newtonSolves 17 = 17 |
| `LIQUID_JUNCTION interval=5 tanks:0.02` | -0.0806 | -0.894 | same | same |
| `LIQUID_JUNCTION interval=0.1 tanks:0.05` | Q(1s) 2.740e-11 kg/s, m_J/Q 3.567e+11 s, mismatch -0.427 | 1.562e-11 kg/s, 6.258e+11 s, 0.00294 | roundoff inflow of a stagnant junction (1e-9 of the 0.015 kg/s scale) | review 4.2 |
| `LIQUID_JUNCTION interval=0.1 tanks:0.02` | Q(1s) 3.103e-12 kg/s, m_J/Q 5.040e+11 s, tauFit 1.003e+12 s | 3.070e-12, 5.094e+11, 2.208e+12 (mismatch(end) NaN both) | same | same |
| `MIXED_GAS_COST` | newtonSolves=286 | 286 | 0 | - |
| `chain-100.json` (phase-ports record) | e.g. node 23 moles[6] 4.220702240868129; pipe 4 flow -4.217556210219395e-4 kg/s | 4.2207022408681025; -4.217556210160935e-4 | max 6.313e-15 (moles), 1.705e-13 K, 3.331e-16 phase fraction, 1.386e-11 flow | rounding (flow difference 5.8e-15 kg/s against a controller allowance of 3.2e-8 kg/s) |
| `chain-100.json` (in-tree, prototype record) | node 82 moles[8] 21.516435245734193 | 21.516435245734318 | 5.779e-15 | = `POW_PERFORMANCE_REVIEW.md` 9 exactly |
| cloud harness `reference/junction-lines.txt` (prototype) | - | 24 of 33 lines differ | - | = the perf branch's B+A+C move (byte-identical to `perf/logs/50-candC-junction-lines.txt`) |
| `PHASE_PORT_GAMETEST` (G5, against `w07-G5-gametests-a3d3a789`) | tankP 123722.65120112627, tankMass 172.07098493664458, liquid outlet 29.205667309729417 kg/s, head 1676.1803947157175, compressor ratio 2.9999859075564457 | 123722.65120112618, 172.07098493664483, 29.20566730972938, 1676.1803947157205, 2.9999859075564412 | 7.3e-16, 1.5e-15, 1.3e-15, 1.8e-15, 1.5e-15 | rounding (ticks, positions and pipe ids differ between any two GameTest runs) |
| test assertions with pinned values | - | all pass except the drain assertion of section 9 | - | - |

Mixed-gas ledger lines (old -> new):

| interval, case | worstMoles | worstEnergy | newtonSolves |
|---|---|---|---|
| 0.1 0.05:4:false | 5.101e-15 -> 5.170e-15 | 5.440e-15 -> 5.761e-15 | 0 |
| 0.1 0.05:4:true | 1.829e-15 -> 1.738e-15 | 4.607e-15 -> 1.890e-15 | 0 |
| 0.1 0.05:5:false | 3.066e-15 -> 1.896e-15 | 5.732e-15 -> 4.153e-15 | 0 |
| 0.1 0.05:5:true | 1.284e-15 -> 1.056e-15 | 1.831e-15 -> 2.274e-15 | 0 |
| 0.1 0.05:6:false | 1.363e-15 -> 1.517e-15 | 7.604e-16 -> 1.111e-15 | 0 |
| 0.1 0.05:6:true | 7.836e-16 -> 1.056e-15 | 2.865e-15 -> 2.097e-15 | 0 |
| 0.1 0.02:4:false | 5.515e-16 -> 5.496e-16 | 1.279e-15 -> 9.298e-16 | 0 |
| 0.1 0.02:4:true | 1.201e-15 -> 1.030e-15 | 2.916e-15 -> 2.507e-15 | 0 |
| 0.1 0.02:5:false | 8.617e-16 -> 1.271e-15 | 2.528e-15 -> 1.511e-15 | 0 |
| 0.1 0.02:5:true | 7.557e-16 -> 7.557e-16 | 2.886e-15 -> 2.682e-15 | 0 |
| 0.1 0.02:6:false | 9.618e-16 -> 7.583e-16 | 8.717e-16 -> 1.075e-15 | 0 |
| 0.1 0.02:6:true | 8.915e-16 -> 1.202e-15 | 2.770e-15 -> 2.274e-15 | 0 |
| 5 0.05:4:false | 2.413e-16 -> 6.204e-16 | 3.217e-16 -> 4.679e-16 | 28 |
| 5 0.05:4:true | 5.968e-16 -> 4.953e-16 | 5.612e-16 -> 4.726e-16 | 26 |
| 5 0.05:5:false | 2.726e-16 -> 3.102e-16 | 2.924e-16 -> 3.802e-16 | 33 |
| 5 0.05:5:true | 5.781e-16 -> 4.770e-16 | 1.772e-16 -> 2.658e-16 | 27 |
| 5 0.05:6:false | 2.385e-16 -> 2.044e-16 | 2.340e-16 -> 4.679e-16 | 35 |
| 5 0.05:6:true | 1.635e-16 -> 6.814e-16 | 5.612e-16 -> 1.772e-16 | 29 |
| 5 0.02:4:false | 2.061e-16 -> 2.061e-16 | 2.325e-16 -> 4.940e-16 | 16 |
| 5 0.02:4:true | 3.263e-16 -> 3.778e-16 | 7.289e-16 -> 2.624e-16 | 16 |
| 5 0.02:5:false | 4.825e-16 -> 3.102e-16 | 6.393e-16 -> 2.906e-16 | 20 |
| 5 0.02:5:true | 4.122e-16 -> 3.435e-16 | 4.082e-16 -> 3.790e-16 | 17 |
| 5 0.02:6:false | 4.136e-16 -> 2.748e-16 | 4.359e-16 -> 1.453e-16 | 20 |
| 5 0.02:6:true | 2.748e-16 -> 5.839e-16 | 3.207e-16 -> 6.123e-16 | 19 |

(The large relative numbers of a line-diff on these files are relative differences between two rounding residues; the
quantities that describe the physics, flows, pressures, amounts and every count, agree to 1e-14 or better.)

## 8. Fuzz sensitivity

### 8.1 Campaigns

The phase-ports campaign nearest before `db3ac1b9` in `tools/phase-ports-probes/hold-classes/logs/` is **`b18`**
(`b18-campaign-rule3-*`, on `89af686c`, D19, Gradle, 8 workers). It predates `983cc915` (D20, only pipes connect), which
changes which blocks connect and so the fuzz layouts, and `db3ac1b9`; a comparison with it mixes D20 with the math. So
a control campaign was run on `db3ac1b9` itself with the same runner, JDK and worker count as the merged one (Gradle-free
fuzz runner of `claude/fuzz-runner`, JDK 21.0.11, 4 workers, 120 slices). Class F (wall deadline / soft budget) depends
on machine load (another agent's 8-worker campaigns ran at the same time), so budget-involved changes are listed apart.

| distribution | clean: b18 (89af686c, 8 w) | clean: control `db3ac1b9` | clean: merged |
|---|---|---|---|
| random | 845 | 856 | 856 |
| defaults | 822 | 829 | 830 |

### 8.2 Per seed, control vs merged (same commit, only the math differs)

Key per seed = clean, or the class and full status text of its first hold (the fuzz-runner equivalence key).

| | random | defaults |
|---|---|---|
| identical first outcome | 907 / 1000 | 879 / 1000 |
| clean -> held | 0 | 3: 101 (E5), 400 (E8), 459 (F) |
| held -> clean | 0 | 4: 194 (E5), 589 (E4), 285 (F), 821 (F) |
| held, first-hold class changed, no F involved | 7: 145 E5>E8, 450 E6>C, 736 E5>E6, 872 E6>E7, 967 E5>C, 992 C>E6, 1000 E5>E6 | 7: 10 E2>E5, 116 E6>E5, 161 C>E5, 200 E5>E2, 401 E6>E8, 547 C>E6, 773 C>E2 |
| held, class changed with F involved | 5: 246 F>C, 441 F>E10, 839 F>E7, 976 F>C, 978 F>E8 | 6: 26 E8>F, 145 F>E6, 492 F>E4, 705 E6>F, 768 F>E8, 772 F>E6 |
| held, same class, other first status | 81 (75 differ only in numbers; 76 at the same tick) | 101 (86 only numbers; 97 same tick) |
| class counts control -> merged | C 50>53, E10 1>2, E5 13>9, E6 30>31, E7 1>3, E8 5>7, F 15>10, others equal | C 6>3, E2 6>7, E5 19>21, E8 7>9, F 20>17, others equal |

Reading: the hold **counts** are insensitive to rounding (random unchanged, defaults +1 clean); per seed, rounding
decides clean vs held in 4 of 2000 runs (0.2 %, net 0) and the class of the first hold in 14 of about 320 held seeds
(4 %), mostly between the step-failure classes E5/E6/E8 and the typed thermo-domain wait C, i.e. which of two nearby
failures of a hard layout comes first. A hold-count claim of the phase-ports batch that rests on a difference of a few
seeds is within this noise; claims on classes of tens of seeds are not.

Against `b18` (for completeness): random 897/1000 identical, 11 held -> clean, 0 clean -> held; defaults 871/1000, 11
held -> clean, 3 clean -> held; D20 and the class X test commit are in that difference.

## 9. Finding: the drain test's "exactly zero" water is a dew-point knife edge

**Physics.** `drainToGas`: 0.1 m3 of water under nitrogen at 120 kPa, drained through a bottom (LIQUID) port to a void at
1 atm. After the gas breakthrough (slice 26) the vessel holds humid nitrogen that was in equilibrium with liquid water,
so its vapour sits at its dew point; the adiabatic expansion cools it and condenses a little water, which the port also
draws (the test's own description). The test then asserts (line 159) that some slice ends with a water volume share of
exactly 0. At the dew point the free-water phase is the difference between the water in the node and the saturated
vapour's capacity for it: its sign and size at the end are decided by the last bits of the temperature and pressure.

**Observed.**

| build / runtime | slice 30 (P from 101325.000034 Pa to the void) | end of run |
|---|---|---|
| base `db3ac1b9`, HotSpot 21 | 3 accepted, port closes, water share 0.0 | dry at slice 30: pass |
| base `db3ac1b9`, HotSpot 25 (Zulu) | 3 accepted, water share 4.3e-17; slice 31 (port closed, no flow) condenses to 1.7e-14 | never exactly 0: **fail** (0.1 s variant fails too) |
| merged, HotSpot 21 and 25 (identical) | "Phase/device active-set cycle" x3, 6 accepted, water share 7.2e-15 frozen once closed | never exactly 0: **fail** (0.1 s variant passes: dry at slice 1406 on both builds) |

The ledgers close at 5e-16 in every case; the states agree to about 1e-10 relative (T 295.57760144 vs 295.57760146 K,
from the different step path at the closure). **Mechanism**: the pass loop's cycle key contains the phase codes of the
seeds and the phase-port segments; with the tank at the dew point and 3.4e-5 Pa above the void, the free-water phase and
the port's drawn segment appear and disappear between passes, the key repeats, the step is refused and substepped, and
the substeps close the port before the last condensate is drawn. Nothing here is larger than rounding in the state; the
outcome of the exact-zero assertion is.

This test was not libm-safe before the merge: it passes on the base only with HotSpot 21-24's intrinsics.

## 10. Owner decisions (recommendations)

1. **The drain assertion** (section 9). Options: (a) restate the test to the physics it can guarantee: after the
   breakthrough the free water is at most the condensate of a saturated headspace (a bound relative to the node, stated
   with its derivation), the port draws it while open, and the port closes at the void; old value "some slice with water
   share exactly 0", new value the bound; a test-only change like `column-retune.patch`; (b) a model rule that a free-water
   phase below a trace level of its node at the dew point is not a phase (an extension of the trace flush), which is the
   trace decision the phase-ports review already left open ("That end needs a decision on traces") and would change
   product behaviour; (c) keep the assertion and accept the failure. **Recommended: (a) now, (b) as part of the open trace
   decision.** Not implemented here (fix-assumption rule).
2. **Printed rejection-reason order.** `PassiveIntervalSolver.Result` stores `Map.copyOf(rejectionReasons)`; the JDK
   randomises the iteration order of its immutable maps at every JVM start, so every printed `reasons={...}` of a
   `Result` (236 test output lines here) differs from run to run on one JDK. Saves are not affected (the checkpoint codec
   sorts) and held-island status texts are not (they print the insertion-ordered map). **Recommended**: keep an
   insertion-ordered (or sorted) unmodifiable copy in `Result`, so every printed form is reproducible; a one-line
   structural change for the final integration, bitwise neutral for the solver.
3. **Re-baseline scope at the final integration** (D5): the 28 junction lines in `holds/12-junction-lines.txt` and
   24 in the harness reference, `chain-100.json`; values as in section 7. **Recommended**: approve once on the final code.
4. **GraalVM check**: done on Windows by the owner's decision D8 ("do GraalVM on win, rewrite tools if needed. Linux
   testing will be a separate work."): OpenJ9 in section 13, GraalVM in section 14, all five runtimes of this machine in
   one `winsweep` run in section 15. **Recommended**: run `tools/jdk-determinism/winsweep/winsweep.sh` again on the final
   code; the Linux sweep (`sweep.sh` in a container) is the separate, later piece of work the owner named.

## 11. What the final integration must repeat, and what it can reuse

Repeat on the final code (the phase-ports branch keeps moving): the merge and conflict check (expect the same two files
unless either side touches them again); the raw-call search of the phase-ports diff (the guard catches direct calls, the
integer-power and hoisting rules need a look at new `DeterministicMath.pow` calls and new calls in loops); the gates G1,
G2 (then the re-capture), G3, full `test`, G5 with a fresh run id; the cross-runtime check on two families; the fuzz
control/merged pair if a hold-count claim depends on it.

Reuse: the conflict resolutions (section 3); `column-retune.patch` as is; the classification of the moving lines
(ledger residues and stagnation ratios); the scripts in `tools/jdk-determinism/rehearsal/`:

- `xrt.sh`, `matrix.sh`, `xsuite.sh`: Gradle-free fluid suites and chain-100 exact of two javac builds (merged tree,
  `git archive db3ac1b9`) under HotSpot 21, Zulu 25 and JBR 25 (perf `wbuild.sh`);
- `printeddiff.js`: printed lines across runtimes, reason-map order normalised; `linediff.js`, `linetable.js`: junction
  lines against a reference, per token;
- `fuzzxrt.sh`: the fuzz runner under another runtime (points this worktree's `build/fuzz-runner/java.txt` at it for the
  run); `fuzzcmp.js`: per-seed first-hold comparison of two reports with the classes of `hold-fuzz/classify.sh`;
- logs: `g01-*` gates, `g02-*` full test, `g03-*` GameTests, `fuzz/f01` merged, `f02` control, `f03` Zulu 25, `xrt/`
  cross-runtime (large files gzipped);
- `tools/jdk-determinism/winsweep/` (section 15): the whole cross-runtime check of the merged tree (suites, chain-100
  tiered and forced-compiled, fuzz, MathSweep, JIT evidence) under any list of JDK homes in one command, one table,
  exit 1 on a real difference. This replaces running `xsuite.sh`, `fuzzxrt.sh`, `printeddiff.js` and `fuzzcmp.js` by
  hand. It runs the Gradle-built test classes: 528 tests against the javac build's 510, since it includes
  `FluidPacketCodecTest` and `FluidDeviceSpecDomainTest` (which need Minecraft classes) and logs through the mod's real
  Log4j logger instead of the harness stand-in.

## 12. Risks

- The fuzz shows that per-seed hold outcomes of hard layouts are rounding-sensitive (section 8.2); a future single-ulp
  change (a kernel edit, a reordered sum) will move a handful of seeds. Compare campaigns only on the same code and JDK,
  and treat single-seed changes as noise unless reproduced.
- Knife-edge assertions on exact zeros or exact phase presence (section 9) exist beyond this test only if they are
  found; the base run under Zulu 25 found two more JDK-sensitive fluid tests, `PhasePortPriorityTest` 0.1 s and
  `TraceTruncationLayoutTest.theOmittedPhaseIsExactlyZeroAndTheComponentTotalIsExact` (a pinned double, 0.1911313604590611
  vs 0.19113136045906112), both of which pass on the merged tree on every runtime tried.
- Class F budget holds are timing-dependent by design and are not covered by determinism.
- The cross-runtime claim rests on HotSpot 21, HotSpot 25, OpenJ9 21 and GraalVM 25 (Graal JIT) on Windows x64 only.
  Linux, other CPUs (aarch64) and OpenJ9 25 are not covered here; the Linux sweep is a later, separate piece of work
  (D8).

## 13. OpenJ9 extension (2026-09-28)

Runtime: IBM Semeru Runtime Open Edition 21.0.12.15 (build 21.0.12.1+1-LTS, Eclipse OpenJ9 0.61.0), at
`D:/Minecraft/Modding/jdks/jdk-21.0.12.15`, not on PATH, no system setting changed. Same javac builds, runners and
exclusions as the HotSpot runs (`xrt.sh`/`fuzzxrt.sh` got an `openj9` entry). Logs: `logs/xrt/s1-merged-openj9-*`,
`s1-base-openj9-*`, `logs/fuzz/f04-merged-openj9-*`.

| item | OpenJ9 result | compared with | verdict |
|---|---|---|---|
| merged fluid suites, printed lines | 510 tests, 509 pass (the drain test of section 9, same values); 5,907 lines: 5,677 identical, 229 differ only in the `reasons={...}` order, 1 timing line | HotSpot 21 merged | **identical** |
| merged chain-100 exact | moles 5.779e-15 (node 82 21.516435245734193 -> 21.516435245734318), T 1.705e-13 K, phase fraction 3.331e-16, flow 1.111e-11; 3 / 0 steps, 4 solves, 29 iterations, 33 residual evaluations | HotSpot 21 merged (the same deviation report, line for line) | **identical** |
| merged fuzz, seeds 1-200, random | 164 clean; first holds identical to HotSpot 21 in 197/200; the 3 others are budget holds on OpenJ9 (seed 145 F vs E8, 198 F vs C, 71 F vs F with another text) | HotSpot 21 (`f01`) and Zulu 25 (`f03`: 198/200, both differences F) | **identical, budget holds excepted** |
| merged fuzz, seeds 1-200, defaults | 158 clean; 198/200 identical to HotSpot 21; seeds 123, 156 F on OpenJ9 vs E8 (Zulu 25: 197/200, 108, 123, 156 all F) | same | **identical, budget holds excepted** |
| base `db3ac1b9` chain-100 exact vs the phase-ports record (HotSpot 21) | moles 5.657e-15 (node 72 moles[16] 5.181434663998052 -> 5.181434663998082), T 3.411e-13 K (node 23 350.04130561159695 -> 350.0413056115973), phase fraction 3.331e-16, flow 1.841e-11 rel. (pipe 13 -0.0014639460202492145 -> -0.001463946020222265, 2.7e-14 kg/s); same counts | HotSpot 21 base: 0; Zulu 25 base: 8.012e-15 | **differs**: a third result family, as review 4.2 (family C) |
| base `db3ac1b9` chain-100 under OpenJ9 vs the prototype record (`27f7b53c`, scratch copy via `-Dfluid.regression.references`) | 0.000e+00 in every quantity | - | the base on OpenJ9 equals the fdlibm prototype bitwise (review: OpenJ9's `Math` is fdlibm) |

The base's three results trace to the libm calls of review 7.1: HotSpot 21's `Math.log/exp/pow/cos/log10` intrinsics,
HotSpot 25's added `cbrt` intrinsic (Peng-Robinson cubic roots), and OpenJ9's fdlibm `Math`. The merged code routes all
of them through `DeterministicMath`, whose `pow` is the table-driven kernel (`DeterministicPow`, not bitwise fdlibm);
that is why the merged chain-100 is one result on all four runtimes but still 5.779e-15 from the prototype record.

OpenJ9 ran every command unchanged: the test task's JVM arguments (`-Xmx8g`, `-ea`, `-D...`, file encoding, locale)
are accepted, no CDS option is passed (the `-Xshare:off` of the scripts only reaches the Gradle launcher), and the
MiniLauncher and the fuzz runner's JUnit launcher work. No workaround was needed. OpenJ9 was slower (fuzz defaults
wall 124 s vs 99 s on Zulu 25), which is why it has more budget holds.

## 14. GraalVM extension (2026-10-12)

**Conclusion: the merged code gives the same results under GraalVM as under HotSpot 21, with the Graal compiler
compiling the science code, including every `DeterministicMath` function.** Fluid-suite printed lines, chain-100 and
the per-seed fuzz first holds match, budget holds excepted. The base `db3ac1b9` under GraalVM gives exactly the HotSpot 25
chain-100 result. GraalVM's compiled `Math.pow` is its own kernel and differs from HotSpot's on rare inputs, so the same
`Math.pow` call can give two results in one run (below). The merged code never calls it.

Runtime: GraalVM Community 25.4.4.1.1+1.1 (build `25.0.4.1.1+1-jvmci-25.4-b23`) at
`D:/Minecraft/Modding/jdks/graalvm-community-25.4.4.1.1+1.1`, installed by the owner (D8), not on PATH, no system
setting changed. Same javac builds, runners and exclusions as the other runtimes (`xrt.sh`/`fuzzxrt.sh` runtime
`graal25`); no workaround was needed. Logs: `logs/xrt/s2-*`, `logs/fuzz/f05-merged-graal25-*`.

**The Graal compiler is the JIT in use.** `-XX:+PrintFlagsFinal` (`logs/xrt/s2-graal25-identity.txt`):
`EnableJVMCI = true`, `UseJVMCICompiler = true`, `UseJVMCINativeLibrary = true` (libgraal), `TieredCompilation = true`.
C1 compiles tiers 1 to 3 and Graal tier 4. Evidence that the hot code ran Graal-compiled:

| run | evidence |
|---|---|
| merged fluid suites, tiered (`-Djdk.graal.PrintCompilation`, `-Xlog:jit+compilation`) | Graal's log: 4,006 compilations, 1,199 of `science/` methods; the JVM's JIT log: 4,011 tier-4 compilations (the same set), 1,164 of `science/`. `DeterministicMath.log` and `exp` at tier 4 from 1.4 s of JVM uptime, `cbrt` from 2.2 s, `DeterministicPow.pow` from 3.4 s, `DeterministicMath.pow` from 9.1 s, of a 30.9 s run; 914 of the 1,164 science tier-4 compilations happen in the first 10 s |
| chain-100, forced (`-Xcomp -XX:-TieredCompilation`, compile only `com.wormzjl.createcheme.science.*`) | every science method is compiled by Graal before its first call, so no science code runs interpreted or in C1: 1,141 Graal compilations on the merged build, 1,126 on the base, all of `science/` |

Results (old = HotSpot 21 on the same build, new = GraalVM):

| item | GraalVM result | compared with | verdict |
|---|---|---|---|
| merged fluid suites, printed lines | 510 tests, 509 pass (the drain test of section 9); 5,907 lines: 5,670 identical, 236 differ only in the `reasons={...}` order, 1 timing line (`one placement, median ms`) | HotSpot 21 merged (`s1-merged-jdk21-printed`) | **identical** |
| merged chain-100 exact, tiered | moles 5.779e-15 (node 82 moles[8] 21.516435245734193 -> 21.516435245734318), T 1.705e-13 K, phase fraction 3.331e-16, flow 1.111e-11; 3 / 0 steps, 4 solves, 29 iterations, 33 residual evaluations; the same 40 listed differences | HotSpot 21 merged | **identical** (line, list and counts) |
| merged chain-100 exact, forced Graal | the same | HotSpot 21 merged | **identical** |
| merged fuzz, seeds 1-200, random | 164 clean; first holds identical to HotSpot 21 (`f01`) in 200/200, budget holds included (Zulu 25: 199/200, seed 145 F) | `f01`, `f03` | **identical** |
| merged fuzz, seeds 1-200, defaults | 158 clean; 198/200 identical to HotSpot 21; seeds 26 and 108 are budget holds (F) on HotSpot 21 and E8 on GraalVM | `f01`, `f03` (199/200, seed 26 F on Zulu) | **identical, budget holds excepted** |
| base `db3ac1b9` chain-100 exact vs the phase-ports record, tiered | moles 8.012e-15 (node 27 moles[1] 0.20785966498276476 -> 0.20785966498276642), T 2.842e-13 K (node 62 349.99029792769363 -> 349.99029792769335), phase fraction 3.886e-16, flow 6.804e-11 rel. (pipe 4, 2.9e-14 kg/s); same counts | HotSpot 21 base: 0; Zulu 25 base: the same line | **differs from HotSpot 21, equals HotSpot 25** |
| base chain-100, forced Graal | the same as tiered | Zulu 25 base | equals HotSpot 25 |

The base result is the one of review 4.2's family B (HotSpot 25: the `cbrt` intrinsic added to the HotSpot 21 set),
even with every science method Graal-compiled.

**GraalVM's own `Math.pow` (found by MathSweep, section 15).** GraalVM's `Math` results equal Zulu 25's for 10 of the
11 functions sampled. `pow` is the exception: Graal-compiled code uses Graal's own `pow` kernel, while the interpreter
and C1 use HotSpot's stub. In MathSweep's tier check (`logs/xrt/s2-mathsweep-tier-graal25.txt`, each function cold, then
after five warm passes), one sampled input of 1,048,576 changes result between the two:

| call | cold (HotSpot stub) | Graal-compiled | `StrictMath.pow` |
|---|---|---|---|
| `Math.pow(2.6359105165595977, 1.3992191771419380)` (hex `0x1.5165840be729p1`, `0x1.66333a5dc2ba4p0`) | 3.8812735768899782 (`0x1.f0cd9293c9239p1`) | 3.8812735768899778 (`...238p1`) | 3.8812735768899778 |

Zulu 25 shows no such input (`s2-mathsweep-tier-zulu25.txt`: 0 for every function). So on GraalVM, code that calls
`Math.pow` can give two results in one run, depending on whether the call site has been compiled yet, which depends on
load. Base chain-100 reached no such input (tiered and forced runs agree). The merged code never calls `Math.pow`
(`DeterministicMath.pow` is the table-driven `DeterministicPow`), and its results are the same tiered and forced. This
is one more libm behaviour the facade removes, not a difference in the merged code.

## 15. Windows sweep of the five runtimes (2026-10-12)

**Conclusion: all five runtimes of this machine give identical results on the merged tree, budget holds excepted
(`winsweep` exit 0).** This covers HotSpot 21, HotSpot 25 from two vendors, OpenJ9 21 and GraalVM 25 with the Graal JIT.
Identical means the fluid-suite printed lines, the failing test, chain-100 exact (tiered, and with every science method
compiled before its first call) and the per-seed fuzz first holds in both distributions. No real difference was found.

Tool: `tools/jdk-determinism/winsweep/` (owner decision D8; README there). Command, on `446fddf0` (the fuzz-runner
build of 2026-09-28 was current, so no Gradle invocation), seeds 1-200, 4 fuzz workers:

```
bash tools/jdk-determinism/winsweep/winsweep.sh build/winsweep/w01 "C:/Program Files/Java/jdk-21.0.11" \
  "C:/Program Files/Zulu/zulu-25" "C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.1/jbr" \
  "D:/Minecraft/Modding/jdks/jdk-21.0.12.15" "D:/Minecraft/Modding/jdks/graalvm-community-25.4.4.1.1+1.1"
```

Logs: `tools/jdk-determinism/rehearsal/logs/winsweep/w01/` (per runtime `r1`-`r5`; files above 300 kB gzipped).

| runtime | family | fluid suites: tests; printed lines vs r1 | chain-100 exact vs in-tree record | fuzz random: first holds vs r1 | fuzz defaults: first holds vs r1 | verdict |
|---|---|---|---|---|---|---|
| r1: Oracle 21.0.11+9-LTS-211 | HotSpot 21 | 528 tests, 1 failed; 6032 lines: reference; 3 with times masked, 8 JDK frames left out | moles 5.779e-15, T 1.705e-13 K, flow 1.111e-11; reference | reference; clean 163 | reference; clean 158 | reference |
| r2: Zulu25.36+15-CA 25.0.4+7-LTS | HotSpot 25 | 528 tests, 1 failed (same tests); 6032 lines: identical, 236 reason-order only; 3 with times masked, 8 JDK frames and 4 JVM warning lines left out | identical to r1 | 196/200 identical; budget-only 4 (23 71 77 145); real 0; clean 164 | 198/200 identical; budget-only 2 (23 26); real 0; clean 158 | identical (budget holds excepted) |
| r3: JBR-25.0.3+9-508.16 | HotSpot 25 | 528 tests, 1 failed (same tests); 6032 lines: identical, 15 reason-order only; 3 with times masked, 8 JDK frames and 4 JVM warning lines left out | identical to r1 | 196/200 identical; budget-only 4 (23 71 77 145); real 0; clean 164 | 198/200 identical; budget-only 2 (23 26); real 0; clean 158 | identical (budget holds excepted) |
| r4: Semeru 21.0.12.15 (OpenJ9 0.61.0) | OpenJ9 21 | 528 tests, 1 failed (same tests); 6032 lines: identical, 18 reason-order only; 3 with times masked, 8 JDK frames left out | identical to r1 | 198/200 identical; budget-only 2 (23 77); real 0; clean 164 | 199/200 identical; budget-only 1 (108); real 0; clean 158 | identical (budget holds excepted) |
| r5: GraalVM CE 25.4.4.1.1+1.1 | GraalVM 25 (Graal JIT) | 528 tests, 1 failed (same tests); 6032 lines: identical, 15 reason-order only; 3 with times masked, 8 JDK frames and 4 JVM warning lines left out | identical to r1 | 196/200 identical; budget-only 4 (23 71 77 145); real 0; clean 164 | 198/200 identical; budget-only 2 (23 26); real 0; clean 158 | identical (budget holds excepted) |

Evidence (the second table `winsweep` prints):

| runtime | JIT during the suite run | Math != StrictMath on this runtime | Math results vs r1 (MathSweep hashes) | chain-100 with science code compiled before first call |
|---|---|---|---|---|
| r1 | tier 4 = C2: 4219 compilations, 1082 of science/; facade at tier 4 after (share of the run): DMath.log 3%, DMath.exp 4%, DMath.cbrt 18%, DMath.pow 14%, DPow.pow 5% | log, exp, pow, cos, log10 | reference | identical to r1 tiered |
| r2 | tier 4 = C2: 4278 compilations, 1109 of science/; facade at tier 4 after: DMath.log 2%, DMath.exp 3%, DMath.cbrt 26%, DMath.pow 14%, DPow.pow 3% | log, exp, pow, cbrt, cos, log10 | differ: cbrt | identical to r1 tiered |
| r3 | tier 4 = C2: 4311 compilations, 1126 of science/; facade at tier 4 after: DMath.log 3%, DMath.exp 3%, DMath.cbrt 32%, DMath.pow 31%, DPow.pow 4% | log, exp, pow, cbrt, cos, log10 | differ: cbrt | identical to r1 tiered |
| r4 | OpenJ9 JIT (no log; forced run: `-Xjit:count=0`) | none | differ: log, exp, pow, cos, log10 | identical to r1 tiered |
| r5 | tier 4 = Graal: 4353 compilations, 1212 of science/; Graal log: 4395 compilations, 1254 of science/; facade at tier 4 after: DMath.log 3%, DMath.exp 3%, DMath.cbrt 5%, DMath.pow 14%, DPow.pow 4% | log, exp, pow, cbrt, cos, log10 (tier-dependent: pow) | differ: pow, cbrt | identical to r1 tiered |

r1 chain-100 line: moles 5.779e-15 (node 82 moles[8] 21.516435245734193 -> 21.516435245734318), T 1.705e-13 K (node 23),
phase fraction 3.331e-16 (node 16), flow 1.111e-11 rel. (pipe 13, 1.6e-14 kg/s against an allowance of 3.2e-8 kg/s);
3 accepted, 0 rejected, 4 solves, 29 iterations, 33 residual evaluations. Every other runtime printed the same line,
the same list of 40 differences and the same counts, tiered and forced.

Reading:

- **Printed lines.** The Gradle-built suites print 6,032 compared lines, more than the javac build's 5,907 (section 13),
  because they include the two Minecraft-dependent test classes and the mod's real Log4j logger. The reason-order count
  varies per JVM start, as decision 2 explains.
- **Two environment artefacts, found by this run and now normalised.** The first comparison pass exited 1, with 158
  differing lines on the three JDK 25 runtimes (`w01.console.txt`). JDK 24+ prints a 4-line warning when a library
  (here Netty) calls a terminally deprecated `sun.misc.Unsafe` method, which shifted every later line. Log4j marks a
  stack frame's jar `~[...]` when it guesses the class location, and on JDK 25 it resolves it exactly (`[...]`).
  Neither is product output. `compare.js` now leaves out the JVM's `WARNING:` lines (the reference prints none) and
  the `~`; the second pass on the same files is `compare.console.txt`.
- **Budget holds.** Every budget-only seed (random 23, 71, 77, 145; defaults 23, 26, 108) is a budget hold (wall
  deadline or soft budget) on HotSpot 21 or OpenJ9, where the other runtimes show a solver hold, another budget text
  (random 71) or clean (random 23). Zulu, JBR and GraalVM agree with each other on all of them. Class F depends on load (another thread's campaign
  ran at the same time), not on the math.
- **The libm under the facade.** MathSweep reproduces the three families of review 7.1: HotSpot 21 intrinsics differ
  from `StrictMath` in `log, exp, pow, cos, log10`; HotSpot 25 adds `cbrt`; OpenJ9's `Math` is `StrictMath`. GraalVM
  matches HotSpot 25 except `pow`, where its compiled kernel differs from HotSpot's stub and is tier-dependent (section
  14). With `Math` differing three or four ways, the merged tree's results are one.
- **Compiled code.** Each suite ran with the facade at the top tier after 2-5% (`log`, `exp`, `DeterministicPow.pow`)
  and 5-32% (`cbrt`, `pow`) of the run. The forced chain-100 runs compile every science method before its first call
  (C2, Graal or the OpenJ9 JIT), so the warm-up question does not arise there.

## 16. Integration into the phase-ports batch (2026-10-14, C4)

Written 2026-10-14 by Claude (Opus 5.5) on `claude/phase-ports-compressor`, for the orchestrator. Item C4 of the
phase-ports close, under that batch's owner decisions D62 and D63; decision D9 here, A427 in the phase-ports
`DECISION_LOG.md`, review section "The close: C4, the full suite and the kink (2026-10-14)" there.

**Conclusion: the integration did what sections 10 and 11 recommended. The references moved at the size section 7
predicted. The full suite then failed five phase-ports tests that predate the integration; they were fixed on the
phase-ports side (A423-A426 there), and the full suite is green (`cl15`: 1258 tests, 0 failures, the guard included).**

| Rehearsal (sections 10, 11) | Integration |
|---|---|
| Repeat the merge; expect conflicts in `CHANGELOG.md` and `PhaseLayout.java` | `a8e49dae` merges `289c6f2d` into the batch; conflicts `CHANGELOG.md` and `chain-100.json` (the determinism side's record kept); `PhaseLayout.java`'s conflict stays resolved by the rehearsal's own merge `94a45b2e` |
| `column-retune.patch` as is | Applied: it came with the branch (`83343366`); the column tests pass in the full suite |
| Raw-call search of the phase-ports diff | One new site: A414's `Math.exp` in `PassiveStepSolver`'s chord test, routed through `DeterministicMath.exp` (`a68e8e1d`); the guard `DeterministicMathSourceTest` green in `cl02` and `cl15` |
| Recommendation 1: restate the drain assertion | Done on the phase-ports side before C4: after the breakthrough the free water is at most a condensate share of 1e-6 (thread t16, `f2b6a987`, 2026-10-13); passes |
| Recommendation 2: reasons in insertion order | `ce4809e9`: `PassiveIntervalSolver.Result` keeps an insertion-ordered copy; bitwise neutral for the solver |
| Recommendation 3: re-baseline once, on the final code | Phase-ports C1, after that batch's thread t32 |
| Recommendation 4: `winsweep` again on the final code | After C1 (below) |
| Gates G1, G2, G3, full `test`, G5 with a fresh run id | `cl01` (alone) green; full `test` `cl02` 1257 tests, 5 failed, all in `com.wormzjl.createcheme.runtime`, failing since earlier phase-ports merges (`cl03`, `cl05`) |
| Fuzz control and merged pair | Not run: no hold-count claim rests on the math alone; the phase-ports campaign `cl16` measures the merged code with A423 |

References moved against `fx37` (the phase-ports gates just before the merge) in `cl01`, nothing re-recorded:

| Reference | Moved |
|---|---|
| Junction lines | 28 of 33 (the rehearsal also found 28): ledger residues at 1e-16; junction shares at 1e-11 or less, or roundoff shares of 1e-15; every Newton solve count unchanged |
| `MIXED_GAS_COST` | 347 -> 347 |
| chain-100 exact | 2.695e-10 relative against the record, the same maximum as `fx37` (interval 0 node 27, now `moles[11]`); temperature 2.572e-9 K, phase fraction 1.206e-11 |
| Printed lines (`printed-maxrel.js`) | 6179 pairs: identical 625, at or below 1e-14 859, 1e-12 3345, 1e-9 539, 1e-6 722, 1e-3 6, above 1e-3 83 (ledgers and residues of 1e-14 to 1e-17, flows of 1e-26, the derivative check's residues, one timing line, and one count: `LEVEL_HEAD_RUN drain` Newton solves 746 -> 756) |
| GameTests | 33 / 33; compressor ratio 2.999995023765548 -> 2.999995023765545; the outlets print 124429.41583189326 -> 124429.54068113302 Pa (1.0e-6, the value family recorded before t28's change; `cl14` reads 124429.41583189282 again; not traced) |

The one count that moved contradicts section 1's "all Newton solve, step and iteration counts are unchanged" for the
code of 2026-10-14; it is a tenth-second drain run, at the Newton tolerance.

Cross-runtime check on the final code:

`w02` on `0041d224` (the close's C1 code: W1, W2, the re-captured chain-100 and the junction references; the phase-ports batch's A441; table `tools/phase-ports-probes/hold-classes/logs/cl20-winsweep-table.md`), 2026-10-14 23:20-23:25, seeds 1-200, 8 workers:

| runtime | suites | printed lines vs r1 | chain-100 exact (tiered; science compiled first) | fuzz random / defaults, first holds vs r1 | Math != StrictMath; vs r1 |
|---|---|---|---|---|---|
| r1 HotSpot 21.0.11+9-LTS-211 | 583 / 0 | reference (6154) | 0.000e+00; 0.000e+00 | reference (clean 195 / 200) | log, exp, pow, cos, log10; reference |
| r2 Zulu 25.0.4+7 | 583 / 0 | identical | 0.000e+00; 0.000e+00 | 200 / 200 identical, real 0 | + cbrt; differ: cbrt |
| r3 JBR 25.0.3+9 | 583 / 0 | identical | 0.000e+00; 0.000e+00 | 200 / 200 identical, real 0 | + cbrt; differ: cbrt |
| r4 OpenJ9 21.0.12.1 | 583 / 0 | identical | 0.000e+00; 0.000e+00 | 200 / 200 identical, real 0 | none; differ: log, exp, pow, cos, log10 |
| r5 GraalVM CE 25.4.4 (Graal JIT) | 583 / 0 | identical | 0.000e+00; 0.000e+00 | 200 / 200 identical, real 0 | + cbrt, pow tier-dependent; differ: pow, cbrt |

Verdict: all runtimes identical, budget holds excepted (`compare.js` rc 0), as in the rehearsal. The sweep's build needs the E6 probe exclusion that `hold-classes.init.gradle` has: `fuzz-runner-e6-exclude-on-c2d124d6.patch` and a README line in `tools/jdk-determinism/winsweep/`, to apply to `claude/fuzz-runner` before the next sweep.
