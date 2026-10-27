# Performance of the deterministic math route: census, candidates, recommended stack

Batch `2026-09-27-jdk-determinism`, owner decision D4 ("the performance hit must be mitigated; slightly less accuracy of
the math functions is acceptable as long as results stay bitwise identical on every JVM and platform"). Written
2026-09-27 by Claude (Opus 5.5) on branch `claude/jdk-determinism-perf-wip` (worktree `jdk-determinism-perf`), Windows 11,
AMD Ryzen 7 9700X, Oracle JDK 21.0.11 unless stated. Tools and logs: `tools/jdk-determinism/perf/` (README section
"perf/" of `tools/jdk-determinism/README.md`).

## 1. Answer in short

- **Where the cost was.** In the fluid path, `pow` was the cost (4.77 M calls per mixed-gas 5 s run at 22 ns in the
  fdlibm port against 7 ns for the HotSpot intrinsic), but 56 % of those calls had constant integer exponents; of the
  9.95 M `log` calls, 5.1 M were loop-invariant recomputations of one value (one of them always `ln 1`) and 2.35 M fed a
  dead update. In the
  column, one site, `V3PropertyComponent` `delta^5`, made 33 M of 34 M `pow` calls per test class.
- **Recommended stack = B + A + C**, implemented as three WIP commits: B hoists loop-invariant calls (bitwise identical
  to the prototype), A writes integer and half-integer powers as products and `sqrt`, C replaces the remaining `pow` by a
  table-driven deterministic kernel (`DeterministicPow`, at most 1 ulp from `StrictMath.pow`, 0.505 ulp from the exact
  value, 11 ns). `log`/`exp`/`cos` stay fdlibm (candidate D measured, not worth it).
- **Cost against the base without deterministic math** (two alternated series of 3 runs, medians): fluid suites **34.0
  / 35.3 s vs 38.1 s base (-11 / -7 %)**, `MIXED_GAS_COST` **195 / 203 ms vs 251 / 274 ms (-22 / -26 %)**; the prototype
  was 44.2 / 43.9 s and 392 / 363 ms. Column solves **3 to 10 % faster than base**; the unmitigated whole-science routing
  was 12 to 26 % slower. Sections 7 and 8.
- **Solver effect**: every Newton solve count unchanged (286 in the mixed-gas cost case, chain-100 3 accepted / 0
  rejected, 4 solves, 29 iterations), ledgers below 6e-15; 24 of 33 junction lines and chain-100 (5.8e-15 relative moles)
  move at the ulp level against the prototype reference, to be re-recorded once (D5). The column fails the same 4
  JDK-sensitive tests with the same values under every candidate (section 7).
- **Determinism evidence**: the kernel's outputs hash identically on HotSpot 21.0.11 (JIT, `-Xint`, C1 only), Zulu 25.0.4
  (JIT, `-Xint`) and Temurin 17.0.20 on 16 input classes x 10^6; the 33 junction lines and chain-100 are bit-identical
  on HotSpot 21.0.11 and Zulu 25.0.4 for the prototype and for B (before the fix JDK 25 was a different result family).

## 2. Method

- **Census** (timing-independent): `perf/census/census.sh` copies the tree, rewrites every `DeterministicMath.f(` into
  `DeterministicMath.f("File.java:line[#k]", ...)` and drops in a counting facade (`perf/census/DeterministicMath.java`:
  calls, argument range, bitwise repeats of the site's previous argument, `pow` exponent values up to 16 distinct, same
  results as the real facade). Scenarios: (i) `MixedGasJunctionTransientTest` 5 s method (the `MIXED_GAS_COST` case, 12
  cases, 286 solves); (ii) chain-100 (`FluidSolverRegressionTest`, exact mode); (iii) column: one literature CDU solve
  (`V3LiteraturePresetTest#theLiteraturePresetPublishesWithATrayOneWaterDewPointWarning`) and the 7-test class
  `V3TrayHydraulicsColumnTest`, with `full-science-routing.patch` applied. Raw: `perf/census/results/*.txt` (prototype)
  and `after-*.txt` (recommended stack).
- **Build and run without Gradle**: `perf/wbuild.sh` (javac + `MiniLauncher`, a 50-line JUnit launcher on the Gradle
  cache jars; the console jar is not cached and nothing was downloaded), harness stand-ins plus one overlay stand-in
  for the literature preset. `perf/check.sh`: the 33 junction lines against a reference and chain-100 exact.
  `perf/src/ColumnProbe.java`: three column solves (literature prescribed-drop, literature with tray hydraulics,
  methane), digest or timing.
- **Timing**: `perf/timing.sh` alternates builds, waits before every run while another Gradle build, Gradle test JVM or
  dev client is alive (none overlapped; checked by process start times), 3 repetitions, medians. Builds: **base** =
  `a0c09c9f` (phase-ports head, `Math` intrinsics), **proto** = `27f7b53c` (fluid routing), **routed** = `0f3a94b6`
  (whole science routed, D3), then B, B+A, B+A+C, B+A+C+D. Indicative, not JMH; the machine was otherwise idle.

## 3. Census

### 3.1 Calls per function

| scenario | log | exp | pow | log10 | cbrt | cos | acos | expm1 |
|---|---|---|---|---|---|---|---|---|
| mixed-gas 5 s, prototype | 9,946,350 | 2,857,495 | 4,765,744 | 1,045,280 | 508,374 | 66,420 | 22,140 | 127,501 |
| mixed-gas 5 s, B+A+C | 1,337,477 | 502,948 | 1,045,288 | 1,045,279 | 508,372 | 66,420 | 22,140 | 127,501 |
| chain-100, prototype | 2,406,763 | 742,623 | 460,325 | 75,884 | 106,838 | 103 | 1 | 46,601 |
| chain-100, B+A+C | 847,747 | 742,549 | 95,105 | 75,884 | 106,838 | 103 | 1 | 46,601 |
| column literature solve, routed | 8,508,902 | 5,246,016 | 8,576,684 | 0 | 34,702 | 1,322,667 | 440,889 | 10,086 |
| column literature solve, B+A+C | 6,712,143 | 5,246,016 | 1,841 | 0 | 34,702 | 1,322,667 | 440,889 | 10,086 |
| `V3TrayHydraulicsColumnTest` (7 tests), routed | 35,475,693 | 22,307,608 | 33,588,053 | 0 | 171,376 | 4,966,929 | 1,655,643 | 64,244 |
| same, B+A+C | 27,604,164 | 22,307,608 | 15,326 | 0 | 172,081 | 4,966,929 | 1,655,643 | 64,244 |

### 3.2 `pow` sites and exponent classes (calls: mixed-gas / chain-100 / tray-hydraulics class)

| site | exponent | class | calls | treatment |
|---|---|---|---|---|
| `PipeResistance:35` `d^4` | 4 | constant integer (base constant per pipe, 99.9999 % repeats) | 1,075,577 / 80,784 / 0 | A: `(d d)(d d)` |
| `PipeResistance:40` `log10(arg)^3` | 3 | constant integer | 1,045,280 / 75,884 / 0 | A: products |
| `PipeResistance:40` `re^1.9` | 1.9 | other constant, = 0.9 + 1 | 1,045,280 / 75,884 / 0 | A: `re^0.9 re` (reuses the next line's pow) |
| `PipeResistance:38` `re^0.9` | 0.9 | other constant, base variable 2.0e3..5.4e6 | 1,045,280 / 75,884 / 0 | C |
| `TranslatedPengRobinson:318/319` `(v-b)^3`, `den^3` | 3 | constant integer | 276,307 x2 / 53,400 x2 / 0 | A: products |
| `V3PropertyComponent:57` `delta^5` (ideal-gas enthalpy) | 5 | constant integer (97 % repeats) | 0 / 0 / 33,024,798 | A: `(d d)(d d) d` |
| `V3PropertyComponent:48` `delta^4` (ideal-gas Cp) | 4 | constant integer | 0 / 0 / 532,452 | A |
| `V3WaterProperties:46-48, 68-70` | 1.5, 3.5, 7.5 | half-integer | 568 / 8,620 / 0 (5.4 M over the whole gates) | A: `theta^n sqrt(theta)` |
| `V3WaterProperties:72/73` (d ln Psat/dT) | 2.5, 6.5 | half-integer | not reached here | A |
| `V3WaterProperties:140` Watson | 0.38 | other constant (water data) | 1 / 1 / 8,158 | C |
| `ViscosityCorrelation:101` power sum | -40, -19.6, -7.7, -1.9 | per-component constants | 8 / 19,228 / 8 | C |
| `SlurryTransport:29` | -1.55 | other constant | 0 (69,455 over the gates) | C |
| `V3TrayHydraulics:209-252` (7 sites) | 2/7, 0.91, 2/3, 0.755, 0.842, 0.2, variable | other constants, one variable | 895 each | C (too rare for A) |
| `V3ColumnCalculator:2640` `10^e` | variable | variable | 0 | C |

No variable-exponent site is hot. Negative bases are not reached by any executed site (all bases are positive by
construction: diameters, Reynolds numbers, `1 - T/Tc` clamped, reduced temperatures, `1 - phi/phi_max` > 0); zero bases
only through `V3WaterProperties` at the critical point (`theta = 0`, where `sqrt(0) = 0` keeps the value). Infinite and NaN
arguments are rejected upstream by the solvers' finiteness checks.

### 3.3 Hot `log`/`exp` sites

| site | calls (mixed-gas / chain / tray class) | argument | treatment |
|---|---|---|---|
| `FluidThermodynamics:508` `ln(P_L/P_V)` in the flash's component loop | 2,550,000 / 132,000 / 0 | always 1.0 in the mixed-gas cases; loop-invariant | B: hoisted out of both loops |
| `GlobalLiquidResponse:53/66` `ln(Pref/P)` in the component loop | 2,550,000 / 532,000 / 0 | 99.95 % repeats; loop-invariant | B: hoisted |
| `FluidThermodynamics:513` `exp(0.5 ln K + ...)` for components with zero amount | 2,354,550 x2 / 0 / 0 | dead: nothing reads that K | B: skipped when the amount is exactly 0 |
| `FluidThermodynamics:515` `ln K` computed twice | 195,450 / 132,000 / 0 | same argument | B: once |
| `PipeResistance:40` `ln 10` | 1,045,280 / 75,884 / 0 | constant | B: static constant (same double) |
| `PhaseLayout:317` `ln(nV/nL)`, `ln(P/Pc)` in the component loop | 0 / 296,500 x2 / 0 | 98 % repeats; loop-invariant | B: hoisted |
| `ViscosityCorrelation:97` `ln c_low` twice | 45,353 / 160,153 / 0 | same argument | B: once |
| `V3MeshResidualEvaluator:470` water dilution term, per component row | 0 / 0 / 8,508,905 | 92 % repeats, one value per node | B: once per node and evaluation |
| `V3DryMeshCoordinateMap:140` `exp` decode | 0 / 0 / 21,925,182 | unique | kept (D would change every value) |
| `V3MeshResidualEvaluator:324/326` `ln x`, `ln y` | 0 / 0 / 11,166,025 x2 | unique | kept |
| `PengRobinsonKernel:501-504` `acos` + 3 `cos` | 22,139 / 0 / 1,655,643 x4 | Cardano three-root branch | kept (E below) |

## 4. Candidates

Kernel costs (ns/call, `perf/src/FastDetCheck.java bench`, warmed loop, best of 25 rounds, JDK 21.0.11;
`logs/20-kernel-bench-jdk21.txt`):

| function | `Math` (intrinsic) | `StrictMath` | facade before | candidate |
|---|---|---|---|---|
| `pow(x, y)` random | 7.3 | 51.9 (allocates 96 B) | 22.0 (`FdlibmPow`) | C: 11.2 |
| `Re^0.9` | 7.3 | - | 21.5 | C: 11.2 |
| `x^5` | - | - | 24.5 | A: 0.36 (products) |
| `x^1.5` | - | - | 22.1 | A: 0.37 (`x sqrt x`) |
| `log` | 3.5 | 3.7 | 3.7 | D: 4.7 (slower) |
| `exp` | 2.4 | 4.1 | 4.1 | D: 2.3 |
| `cos` (args -4.2..1.1) | 4.4 | 10.7 | 10.7 | - |
| `acos`, `cbrt`, `log10`, `expm1` | 4.7, 6.1, 4.0, - | 5.1, 6.6, 7.7, 7.6 | same | - |

| candidate | what | accuracy | determinism argument | solver effect | cost effect |
|---|---|---|---|---|---|
| **B** hoisting (commit `55348ff0`) | 6 sites of 3.3 computed once instead of per component/row/call; one dead update skipped | bitwise identical to the prototype | same operations on the same doubles | none: 33 junction lines identical, chain-100 0.000e+00, column digests identical | `MIXED_GAS_COST` 392 -> 320 ms; mixed-gas `log` 9.95 M -> 2.4 M |
| **A** strength reduction (`903e6004`) | the integer and half-integer exponents of 3.2 as products and `Math.sqrt` | products: at most ~2 ulp from the exact value (3-4 roundings), fdlibm pow < 1 ulp; so 1-2 ulp from before | `+ - *` and `Math.sqrt` are correctly rounded by the JLS | 12 of 33 junction lines (every `key=0.05`) and chain-100 (4.9e-15 relative moles) move; all solve counts equal | `MIXED_GAS_COST` 320 -> 228 ms; column solves -13 to -18 % (`delta^5`) |
| **C** `DeterministicPow` (`0bea3493`) | Tang log (128-entry table, exact residual of `u`, degree-8 series, double-double) x y (exact Dekker product) -> exp (128-entry `2^(j/128)` table, degree-6 series); fdlibm outside the fast domain | vs `StrictMath.pow` on 10^7 inputs per class: max 1 ulp, 8-10 % of inputs differ; exact error max 0.505 ulp (`StrictMath` 0.826) | `+ - * /`, comparisons, `Math.abs/rint`, raw bit conversions, table lookups, no fma; equal hashes on 3 JDKs x modes (section 6) | 24 of 33 junction lines and chain-100 (5.8e-15) move against the prototype; all solve counts equal | `MIXED_GAS_COST` 228 -> 195 ms; column -2 to -5 % |
| D fast `log`/`exp` | same kernels as C for `log`/`exp` | log max 1 ulp vs StrictMath (7.8 % differ on [0.5, 2]), exp max 1 ulp (9.7 %); exact error 0.500 / 0.506 ulp | as C | 27 of 33 junction lines move; counts equal | none measurable: `log` 4.7 ns is slower than fdlibm's 3.7 on this CPU, `exp` saves 1.8 ns; `MIXED_GAS_COST` 244 ms single run (C: 243) | 
| E other | `cos(a - 2 pi/3)`, `cos(a - 4 pi/3)` from `cos a`, `sin a`; caching `exp`/`expm1` of the repeated `GlobalLiquidResponse` argument; `ln` of `exp`-decoded flows | would change the Cardano roots (column root selection is the fragile part) | - | not measured | at most 5 M `cos` x 6 ns = 30 ms per 9.4 s tray-hydraulics class (0.3 %); the cache is not thread-safe on a shared model without a per-call workspace: not worth it |

Special cases of C: every input outside the fast domain returns `FdlibmPow` (`StrictMath.pow`) bit for bit, which the
gate test asserts on a 39 x 39 grid (zeros, negative bases with integer and half-integer exponents, subnormals,
infinities, NaN, overflow and underflow edges); results that would be subnormal or overflow (|y ln x| > 708) also fall
back. Within the fast domain the fast path returns a positive finite normal number. Exact cases stay exact (checked:
`2^10`, `4^0.5`, `10^3`, `3^4`, `0.5^-3`, `9^0.5`, `2^-1`, `7.5^2`). Reachable from the solver: only positive normal bases with
finite exponents (section 3.2), i.e. the fast path; the fallback is for the facade's contract.

## 5. Recommended stack and its code

Commits on `claude/jdk-determinism-perf-wip` (not pushed, not merged, no version bump):

| commit | content |
|---|---|
| `0f3a94b6` | whole-science routing (`full-science-routing.patch` unchanged, decision D3), so the column is measured as it will ship |
| `55348ff0` | B: hoisting in `FluidThermodynamics`, `GlobalLiquidResponse`, `PhaseLayout`, `PipeResistance`, `ViscosityCorrelation`, `V3MeshResidualEvaluator` |
| `903e6004` | A: `PipeResistance`, `TranslatedPengRobinson`, `V3WaterProperties`, `V3PropertyComponent` |
| `0bea3493` | C: `science/math/DeterministicPow` (tables as exact hex literals), `DeterministicMath.pow` -> it, `DeterministicMath.strictPow` = the bitwise fdlibm port; `DeterministicMathTest` extended |
| (tools commit) | `tools/jdk-determinism/perf/`, README section, logs, this review, INDEX rows, CHANGELOG line, decision log D6-D7 |

`DeterministicMathTest` (in `fluidScienceTest`) now checks: `strictPow` bitwise `StrictMath.pow` (special grid, 10^6
sampled inputs); `pow` bitwise `StrictMath.pow` outside the kernel domain and within 1 ulp inside it (grid and 1.2 x
10^6 samples, including `Re^0.9`); 14 pinned bit patterns (census inputs, one overflow fallback, and 4 inputs where
the kernel is 1 ulp from fdlibm, so any JVM that computes the kernel differently fails); the 640 table entries and 5
constants equal their regeneration from BigDecimal series; no allocation by `pow` or `strictPow`.
`DeterministicMathSourceTest` still passes (the kernel calls no banned function).

## 6. Determinism evidence for the new kernel

`perf/kernel-sweep.sh` compiles the facade, `FdlibmPow`, `DeterministicPow` and the check with `--release 17` and runs
`FastDetCheck accuracy 1000000` (16 input classes; SplitMix64 inputs, identical everywhere) on each runtime; the output
hash of every class is the same on all six (`logs/30-kernel-sweep-1e6.txt`):

| runtime | modes |
|---|---|
| Oracle HotSpot 21.0.11 (Windows) | default JIT, `-Xint`, `-XX:TieredStopAtLevel=1` |
| Azul Zulu 25.0.4 (Windows) | default JIT, `-Xint` |
| Eclipse Temurin 17.0.20 (Windows) | default JIT |

The production class and the prototype (`FastDetMath`, tables built at class initialisation from BigDecimal) agree on
10^7 inputs (differ 0). Argument for runtimes not tried (OpenJ9, GraalVM, aarch64): the kernel is ordinary Java
arithmetic, which JLS 15.4 (strict since JDK 17, JEP 306) fixes to IEEE 754 round-to-nearest for every operation, and
it has no call a JIT may intrinsify with a different result (`Math.rint`, `Math.abs`, raw bit conversions are exact).
A fused multiply-add contraction by a JIT would violate the JLS; the sweep's `-Xint` vs JIT equality is the check.
Not run here: the 36-runtime Linux sweep (`sweep.sh`) on the stack; it needs the container JDKs.

## 7. Column findings

| build | 4 JDK-sensitive tests (review 7.4) | failure values | column/thermo/material suite (harness, one run) |
|---|---|---|---|
| base `a0c09c9f` (`Math`) | pass | - | 565/566, 132.9 s (final series) |
| routed (`0f3a94b6`) | fail | `V3ExactWarmStartSweepTest.physicallyAcceptedLiquidOnlyState...`: MAX_ITERATIONS, 128 iterations, maximumScaledResidual 1.0341034026697182E-5; `...numericalTwoPhaseMatrixRemainsRejected...`: LINEAR_SINGULAR, 26 iterations, residual 6.661338147750939E-15; `V3SideDrawCalculatorTest.legalNearFeedDraw...`: iteration budget exhausted; `V3LearnedRecoveryTest.anAcceptedLearnedCorrection...`: Failure instead of Success | 561/566, 159.7 s |
| + B | same 4 | identical values | 561/566, 157.7 s |
| + B + A | same 4 | identical values | 561/566, 127.8 s |
| + B + A + C | same 4 | identical values | 561/566, 130.3 s |

The fifth failure in every row is `V3MethaneColumnTest.freshPresetAndSavedInputRetainTheMethaneAxis`, a harness gap (it
reflects on the real block entity's NBT `writeInput`; passes in Gradle); `ColumnInputPresetTest` is excluded as in the
earlier study. No candidate adds or removes a column failure, and no column test was re-tuned: the 4 remain for the
column batch of D3 (old values: pass on HotSpot 21-24 `Math`; new values: the failures above).
Column cost per solve (`ColumnProbe`, median of 3 alternated rounds of 5 solves, ms):

| case | base (`Math`) | routed | B | B+A | B+A+C |
|---|---|---|---|---|---|
| literature, prescribed drop | 1141 | 1341 (+18 %) | 1315 | 1112 (-3 %) | 1051 (-8 %) |
| literature, tray hydraulics | 1346 | 1698 (+26 %) | 1592 | 1358 (+1 %) | 1303 (-3 %) |
| methane preset | 1686 | 1998 (+19 %) | 1952 | 1607 (-5 %) | 1579 (-6 %) |

The routed column's extra cost was the `delta^5` of the ideal-gas enthalpy (22-25 ns per fdlibm call against 7 ns
intrinsic, 33 M calls per test class); after A the remaining fdlibm `log`/`exp`/`cos` calls cost about what the
intrinsics cost on this CPU.

## 8. Fluid cost

Fluid suites (`science.fluid` + `runtime.fluid` + `runtime.Fluid*Test`, one JVM, `MiniLauncher` wall), 3 alternated
rounds (`logs/21-timing-fluid-suite.txt`):

| build | wall s (3 runs) | median | `MIXED_GAS_COST` ms | median | tests |
|---|---|---|---|---|---|
| base `a0c09c9f` | 38.1, 35.5, 40.8 | 38.1 | 251, 235, 300 | 251 | 472/472 |
| prototype `27f7b53c` | 44.2, 39.2, 46.2 | 44.2 (+16 %) | 392, 327, 414 | 392 (+56 %) | 477/477 |
| B | 39.4, 39.7, 39.8 | 39.7 (+4 %) | 317, 321, 320 | 320 (+27 %) | 477/477 |
| B+A | 34.0, 33.2, 35.3 | 34.0 (-11 %) | 224, 228, 237 | 228 (-9 %) | 477/477 |
| B+A+C (prototype kernel) | 32.6, 36.2, 34.0 | 34.0 (-11 %) | 194, 208, 195 | 195 (-22 %) | 475/477 (the 2 bitwise-pow tests of the old `DeterministicMathTest`, expected) |

Final confirmation on the committed stack (`0bea3493`), a second alternated series after the other agent's dev client
had exited (`logs/40-*`, `41-*`, `42-*`):

| build | fluid wall s | median | `MIXED_GAS_COST` ms | median | tests | column probe medians ms (prescribed / trays / methane) | column suite (one run) |
|---|---|---|---|---|---|---|---|
| base | 38.1, 37.4, 39.9 | 38.1 | 266, 274, 283 | 274 | 472/472 | 1140 / 1342 / 1653 | 565/566, 132.9 s |
| prototype | 43.9, 46.6, 40.6 | 43.9 (+15 %) | 363, 405, 338 | 363 (+32 %) | 477/477 | - | - |
| routed (column) | - | - | - | - | - | 1277 / 1574 / 1918 (+12 to +17 %) | 561/566, 146.7 s |
| **B+A+C** | 33.5, 35.3, 36.9 | **35.3 (-7 %)** | 197, 220, 203 | **203 (-26 %)** | 480/480 | **1028 / 1275 / 1566 (-10 / -5 / -5 %)** | 561/566, 130.6 s |

Both series agree: the recommended stack is faster than the base without deterministic math, in the fluid suites
(-7 to -11 % wall), in the product's 5 s mixed-gas cycle (-22 to -26 %) and in column solves (-3 to -10 %).

Windows-lane numbers of the other agent on the same machine (Gradle, one run each, review section 15.4): base 37 s /
288 ms, prototype 47 s / 383 ms, prototype + the 10-line strength reduction 41 s / 286 ms; consistent with the above.
Allocation per mixed-gas cost case is unchanged (139 MB, as the prototype).

## 9. Re-baseline this implies (to be recorded once, D5)

Against the prototype's reference (`tools/cloud-science-harness/reference/junction-lines.txt`, `chain-100.json`
`fa2b9e26`): 24 of 33 junction lines move (all 12 `interval=0.1` `MIXED_GAS_TRANSIENT` lines, 11 of the 12
`interval=5` ones, and `LIQUID_JUNCTION interval=0.1 tanks:0.02`, the roundoff ratio line of review 4.2, now
`Q(1s)=3.070e-12` with `mismatch(end)=NaN` as on HotSpot base); every `newtonSolves` count and `MIXED_GAS_COST
newtonSolves=286` unchanged; worst ledger 5.8e-15 (gate 1e-12). chain-100: max 5.779e-15 relative moles (node 82), 1.7e-13
K, phase fraction 3.3e-16, flow 1.1e-11 relative (1.6e-14 kg/s against an allowance of 3.2e-8 kg/s); 3/0 steps, 4
solves, 29 iterations, 33 residual evaluations. Column: printed values move at the ulp level (digests of the three
`ColumnProbe` solves change; iteration counts and pass/fail do not). No assertion or tolerance changes. Procedure as in
HANDOFF section 5 (Gradle `-PfluidRegressionCapture=true`, harness reference), on this stack.

## 10. Risks

- C is a new numerical kernel (130 lines + tables) the project owns: its correctness rests on the 10^7-input
  comparisons, the exact-error sample and the pinned-bits test; a future edit must keep the test green on two JDKs.
- The dead-update skip in `FluidThermodynamics` (B) relies on components with exactly zero amount never reading K; true
  today (x, y, split and allocation are exact zeros); a future change that reads K of absent components must restore it.
- A and C move values by an ulp or two; any solver cliff near a threshold could flip (none seen in 480 fluid tests, the
  33 GameTests were not run, the column fails the same 4 tests).
- Timing is indicative (not JMH), on one CPU (Zen 5, where fdlibm `log` costs about what the intrinsic does); on CPUs
  with slower scalar division or where HotSpot's intrinsics are relatively faster, B and A still remove the calls.

## 11. Not measured

Gradle gates on the stack (fluid suites, `fluidSolverRegression` re-capture and exact, adjacent, full `test`,
GameTests), the Linux 36-runtime sweep, OpenJ9 and GraalVM for the new kernel, macOS/aarch64, JMH-grade timings.
