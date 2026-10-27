# unified-thermo-port-quick-fixes

Purpose: the fixes of the regressions the quick checks after the range work (WP1-WP4) found on branch
`claude/unified-thermo-port`: bundled class X (seeds 145, 157, 164: the kernel's "no physical VAPOR root" escaping the
network), cold E9 seed 85 ("Invalid pipe transport properties") and cold E9 seed 183 ("Fluid TP equilibrium did not
converge"), and the lead's follow-up decisions 1 (the numerical box bounds a vapour's partial pressure) and 2 (a vessel
holding no fluid is a dry end). Batch: `documentation/2026-09-24-coolprop-low-temperature/` (report
`QUICK_CHECK_FIXES.md`). Commits 8220d4bc (X), fd0dc531 (seed 85), 87c106f6 (seed 183) on 31e25f6f, then db6b1e6f
(box) and 95e4325d (dry end), the behaviour-free cleanup 04f84aa5 (phase-hold names) and cc2f8c37 (test-only kij,
the fallback counter), option (a) of the fallback regression, e4769fa2, the fixed-volume freeze step, 703d69b3, and
pure water's triple point, 7bd5fafc, 2026-10-17.

Nothing here was removed from a tracked path: every probe and script was written outside the worktree and applies to
an exported copy of a commit (`/tmp/forms-<name>`). The gate tests stay in the tracked tests. The lane is the
Gradle-free index lane of `tools/unified-thermo-port-forms/` (main checkout copy; `export.sh`, `index-compile.sh`,
`index-run.sh`, the hold-fuzz harness); `WT` (default the coolprop-multiphase-thermo-37f6b0 worktree) gives the
repository and its `build/moddev` classpath files. `FORMS` below is that tool's folder.

## Contents and how to run

- `scripts/run-seed.sh`: one hold-fuzz seed (one worker, 120 slices, counters) on a compiled export. `KIND=cold` (default,
  pilot cryogenic package) or `KIND=bundled`. Waits while a dev client runs. Logs to `$LOGS` (default `/tmp/qf-logs`).
  `NAME=qf-x bash $FORMS/scripts/export.sh <commit>; bash $FORMS/scripts/index-compile.sh qf-x;`
  `KIND=bundled bash scripts/run-seed.sh qf-x 157 b157`
- `scripts/compile-all-tests.sh <name>`: compiles all of `src/test/java` of a compiled export (index-compile builds only
  the harness's dependencies) into its `test-all`: the check that a commit compiles its tests.
- `scripts/run-touching-tests.sh <name>`: the 41 science and 17 runtime classes touching the fixed code (runtime ones with
  `-Dcreatecheme.fluid.scheduler.verify=true`). First `cp -r /tmp/forms-<name>/test-all/. /tmp/forms-<name>/test/`.
- `rule-off/run.sh [tree] [rules]`: the rule-off proofs. For each rule it exports the tree, switches that rule off with
  `rule-off/apply.pl` (its header describes each), compiles and runs that one gate test, which must fail. Rules: `root`,
  `kept`, `crystal` (the code of 31e25f6f), `box` (the partial pressure's floor alone), `dry` (`dryEnd` of a vessel
  holding nothing only), `standin` (no ambient viscosity or infinite acoustic bound for a state with no fluid), `layout`
  (the dry layout of a vessel holding nothing only). `bash rule-off/run.sh 95e4325d`: all seven fail as intended.
  Option (a): `skip` (the refresh the rest test reads visits every node) and `melting` (water competes at any
  temperature); gate `PrecipitatedSolidsTest#aWarmWaterVesselHasNothingToConvertAndAColdOneFreezes`; both fail on e4769fa2.
- `probe/apply-root-trace.pl <tree>`: X trace. The kernel's exception carries A, B, the discriminant and the roots; the
  network phase read that met it prints a `QFROOT` line (T, P, root, amounts).
- `probe/apply-transport-trace.pl <tree>`: seed 85 trace. `QFPIPE` lines (refused pipe rows) and `QFTRANS` lines (node
  transport rows whose density or viscosity is not finite and positive).
- `probe/seed85/*.pl <file>`: seed 85's follow-up traces, one file each (`QFDONOR`, `QFREFRESH`, `QFRECON`, `QFCAND`,
  `QFRES`); each header names its file. They match the code of 31e25f6f.
- `probe/apply-nofluid-trace.pl <tree>`: decision 2's trace on 95e4325d: `QFNOFLUID` (a vessel with solids judged to hold
  no fluid), `QFDRYBED` (solids, no moles), `QFSTANDIN` (an ambient viscosity read). Cold seed 85 prints only QFNOFLUID.
- `probe/apply-crystal-trace.pl <tree>`: seed 183 trace. A `QFXTAL` line where the crystal solve refused its answer
  (T, P, feed, last f, iterations, each fluid phase's (mu_i - mu_s)/RT).
- `probe/java/.../Seed183CrystalProbe.java`: replays seed 183's crystal request as a fresh TP flash. Copy it into an
  export's `src/test/java`, compile with `EXTRA_TESTS=<its path>`, run
  `index-run.sh <name> com.wormzjl.createcheme.science.thermo.phase.Seed183CrystalProbe`.

- `scripts/run-cleanup-tests.sh <name>`: the cleanup's 15 science classes (DeterministicMathSourceTest from the export root) and
  18 runtime classes. `scripts/bisect-fallback-test.sh <commit>`: FluidFallbackQualificationTest#hardTimeout... at one
  commit (it fails from ead6d6f2 on; 1674e3db and earlier pass).
- `edit/apply-edits.pl <root> <edits>`: exact block replacements, all or nothing (each block names its file and count).
  `edit/phase-hold-rename.edits` and `edit/test-only-cleanup.edits` made 04f84aa5 and cc2f8c37 on the worktree;
  `edit/phase-hold-tools.edits` (root: the main checkout's `tools/`) updated the hold-fuzz harnesses of
  `unified-thermo-port-forms/` (copied to the worktree's `tools/` too: literal prefixes, no fallback counter), class C
  and E3 of `phase-ports-probes/hold-fuzz/classify.sh` and `jdk-determinism/rehearsal/fuzzcmp.js` (both prefixes).

Run a trace by exporting the commit, applying the script, compiling, and running the seed with `run-seed.sh`; grep the
seed's stdout log for the line prefix.

## Option (a) of the fallback regression (e4769fa2)

- `scripts/run-option-a-tests.sh <name>`: the union of the touching and cleanup sets, each class once (46 science, 31
  runtime). e4769fa2 (tree 23e2049b): 233 and 177 tests, all successful.
- `scripts/compare-test-output.pl <new log> <baseline log>...`: the lines each test prints, keyed by its M3RUN line,
  object hashes blanked; prints the tests whose lines differ. Baselines: `qf2-*` (95e4325d, the touching set) and
  `qf3f-*` (cc2f8c37, the cleanup set); all logs in `logs/option-a-test-logs.tar.gz`.
- The literal placement (the skip in every refresh, `qf4lit` logs): export the tree, replace
  `{return refresh(graph,model,checkpoint,node->true);}` in `InventoryEquilibrium.java` with
  `{return refresh(graph,model,checkpoint,node->mayConvert(node,model));}`, compile and run the set: 3 science tests
  fail (DirectLiquidContinuityTest, G3F8, the new gate) and six tests' physical numbers move (LevelHeadTest's
  three drains, the thawed outlet, VesselStateFollowsContentsTest, an IslandCertificateTest revalidation).
- `probe/java/.../science/fluid/network/CoexistenceProbe.java`: nitrogen over water a little below the melting line,
  taut and slack, refreshed and solved one 5 s interval. Copy it into an export's `src/test/java` (same package), run
  `compile-all-tests.sh`, copy `test-all` over `test`, run `index-run.sh <name> <its class>` and grep `COEX`: a taut
  vessel whose freeze ends in ice and water at T_m fails ("a step of U(T)"), a slack one coexists at 273.1525 K.

## The fixed-volume freeze step (703d69b3, tree 353c6bc0)

WP4's option 7, decided by the lead: `FluidTpEquilibrium.freezingAtVolume` answers a rigid vessel's freeze on the
melting line (T = T_m(P), the ice from the energy, P from the volume by a secant in P). Exports: `qf5e` is 703d69b3's
tree, `qf4c` is e4769fa2 (tree 23e2049b).

- `scripts/run-uv-freeze-tests.sh <name>`: `run-option-a-tests.sh`'s classes plus the 13 other science classes that call
  the engine's PH or UV or belong to the crystal and solids suites (59 science, 31 runtime classes). `ONLY_EXTRA=1` runs
  the 13 alone, as their baseline. 703d69b3: 291 and 177 tests, all successful. Against e4769fa2 (`qf4c-*` plus
  `qf4c-extra`) only gate (b)'s TP count moved (85 -> 15, same answer), besides wall-clock lines.
- `rule-off/run.sh 703d69b3 uvfreeze`: `freezingAtVolume` answers nothing (`if (true) return null;`). Its three gates
  fail: `FreeWaterIceTest#aRigidVesselOfNitrogenOverWaterFreezesOnTheMeltingLine` and
  `PrecipitatedSolidsTest#aTautVesselOfNitrogenOverWaterCooledThroughItsFreezeFreezesOnTheMeltingLine` with "a step of
  U(T)", `FreeWaterIceTest#phAndUvAcrossTheFreezeLandOnTheMeltingLine` with 85 TP calls. A rule may now list several
  gates (space-separated selectors in `run.sh`).
- `probe/apply-freeze-trace.pl <tree>`: `DBG` lines for each try (its p0), each pressure on the line (T_m, beta,
  V_spec/V - 1, ln(V_spec/V), slope, step), a warm side without liquid water, the beta exit and the caller (`site
  newton` or `site nested`); `-Ddbg.tp=true` adds every counted TP call. Recompile the one file into the export's
  `main` with javac (the header has the line).
- `probe/java/.../science/thermo/phase/FreezeUvProbe.java`: pure-water UV targets, grep `FREEZEUV`. (a) Twice the
  liquid's volume with 2 kJ/mol out: the triple point, which no rule answers (NOT_CONVERGED, "no convergence of the UV
  pressure search at 273.159 K", 2778 TP calls at e4769fa2, 2798 now: two line tries that halve back toward 611.657 Pa).
  (c-e) Liquid-full with 2, 5.5 and 6.5 kJ/mol out: 268.168 K 67.3 MPa ice 0.280; 250.350 K 307.5 MPa 0.720; 242.661 K
  411.2 MPa 0.842, 17 to 21 TP calls (at e4769fa2 refused by IF97 region 1 at 226 K and 630 MPa after 69 to 91).
  `CoexistenceProbe` (grep `COEX`): the three taut freezes that refused at e4769fa2 now coexist at T_m; the rest repeat.
- Measured and dropped: a crossing predicted in the Newton before its trial's TP (`crossesMelting(current, tt, pt)`).
  Cold vessels whose Newton steps from liquid water cross the line far above an all-ice target tried it: gate (c),
  `WaterIntoCryogenicMethaneTest`, 15 tries ending at once (beta 2.8 to 38), and the extra TP call moved its trajectory
  from interval 2 at rounding and from interval 5 visibly (164.355 -> 164.347 K, 970536.7 -> 970520.2 Pa, ice 8.901 ->
  8.907 kg; `PrecipitatedSolidsTest`'s cold vessel 120 -> 126 checkpoints). The trial's TP and the energy between the
  step's ends decide now, as a PH bracket does: bitwise. The nested search keeps the prediction (it saves the far
  volume solve: 14 TP calls in gate (b)); a false one there costs one TP call and ends the search's tries.
- An earlier form solved in ln P from the nested bracket's ice end (320 MPa): the residual is exponential in ln P for a
  liquid, so it crawled 0.7 per step and gate (b) took 109 TP calls; the secant in P takes 4 from the liquid end.
- Logs: `logs/uv-freeze-test-logs.tar.gz` (`qf5e-*` science, runtime, probes and rule-off; `qf4c-extra`, `qf4c-probes`).

## Pure water's triple point (7bd5fafc, tree 38d80269)

The lead's direction, the limit of WP4's freeze step: `FluidTpEquilibrium.waterTriplePoint` answers pure water with
only its own steam inside its triple point's triangle at 273.16 K and 611.657 Pa, before the UV search, the amounts from
the mole, energy and volume balances, as `pureTriplePoint` does a crystal's species. `insideTriangle`, shared by both,
tests the specification (energy to 1e-6 N R T, ln V to 1e-6) after setting a negative amount to 0, the others keeping
the moles. Export `qf6c` is tree 8956b70d, the commit's code before two comment edits.

- `scripts/run-uv-freeze-tests.sh qf6c`: 293 science and 177 runtime tests, all successful. Against 703d69b3 (`qf5e-*`)
  no printed number moves, the CO2 triple points of NearPureCrystalStepTest, G5F1 and G5F6 included; besides the two
  new gates only wall-clock lines differ (SolidChainTransportTest, the checkpoint timings, PhysicalRegistryTest).
- `rule-off/run.sh <tree> watertriple triangle`. `watertriple`: `waterTriplePoint` answers nothing; gate
  `FreeWaterIceTest#pureWaterWithItsOwnSteamInARigidVesselStandsAtTheTriplePoint` fails (ice 0.1: "a step of U(T)").
  `triangle`: the 1e-6 N mole tolerance again; gate
  `FreeWaterIceTest#pureWaterJustOutsideItsTriplePointIsAnsweredOnTheNeighbouringLine` fails, its no-steam target taken
  at the triple point (volume 1.88345e-5 against 1.88341e-5 m3). That gate passes with `watertriple` off, with the same
  numbers: it pins that the rule leaves the paths outside the triangle as they were.
- `probe/java/.../science/thermo/phase/TriplePointProbe.java` (grep `TRIPLE`): the triangle's corners; four targets
  inside; targets 1e-3, 1e-5, 1e-7 and 1e-10 mol beyond an edge; -1e-12 mol of steam, within the specification
  (answered at the triple point, residual 2e-7); the gate's vessel. Without the rule the inside targets are
  NOT_CONVERGED, "a step of U(T)" after 97 to 489 TP calls or no convergence of the pressure search in 1379 to 2798.
  With the mole tolerance the -1e-7 and -1e-10 mol steam targets were CONVERGED at the triple point with residuals 2e-2
  and 2e-5, and `FreezeUvProbe`'s liquid-full vessels (c) and (d) at the triple point instead of 67 and 307 MPa on the
  melting line. `FreezeUvProbe` (a) now stands at the triple point, ice 0.333, 0 TP calls.
- Logs: `logs/triple-point-test-logs.tar.gz` (`qf6c-*`: science, runtime, probes, rule-off).

## Detail moved from QUICK_CHECK_FIXES.md (2026-10-17)

- X: the kernel's `NoPhysicalRoot` escaped the network; 8220d4bc makes it `RootAbsent` (IAE), db6b1e6f bounds the
  partial pressure in `FluidThermodynamics.state` (`NumericalBox.requirePartialPressure`, 1e-6 Pa to 1 GPa). Gates in
  `HydrocarbonModelTest` fail with the rule off. Seeds 145, 164 clean; 157 keeps its 15 pre-existing E5 holds.
- Cold 85: `PassiveStepSolver.holdsNoFluid` replaces `holdsNothing` (dry end, D54 dry layout, junction seeds, rest
  test); ends no flow passes read the ambient's viscosity. Gates
  `ConservativeTransportTest#aVesselNothingReachesKeepsWhatItHoldsItsSolidsIncluded`,
  `EmptyNetworkTest#aVesselHoldingOnlySolidsIsADryEndAndGivesNothing`; the seed ends with 13 E7 holds as F4.
- Cold 183: the liquid's residual was 1.0046e-9 against the 1e-9 tolerance; gate in `GasSolidEquilibriumTest`.
- Cleanup: "HELD (phases): ", `status=PHASE_HOLD`, "phase-hold: "; the harness copies, `classify.sh` and `fuzzcmp.js`
  take both prefixes.
- Option (a): `InventoryEquilibrium.conversions` is read by `PassiveIntervalSolver.solve` before its rest test. Failure
  branch: warm water holding no ice keeps its closure failure at once; held ice keeps the crystal test. The skip placed
  in `refresh(node)` for both callers failed DirectLiquidContinuityTest and G3F8 and moved six tests' physical numbers.
  Counters that fell: CrystalFlushBudgetTest 3 to 5 checkpoints per interval. The fallback gate fails with `skip` off.
- Fixed-volume freeze (703d69b3): before, U(T) at a fixed volume rose by the latent heat within a microkelvin and UV
  refused "a step of U(T)" (taut N2 over water from 263 and 272 K); pure water took 85 TP calls; a liquid-full vessel
  left IF97 (226 K, 630 MPa) and failed. The secant in P on V_spec/V - 1 (linear for an ideal gas and, to first order,
  a liquid) closes to the outer tolerance or the volume's rounding (liquid-full: P to 1e-5 Pa); beta beyond [0, 1] on
  the root's side ends the search's tries. Tried by a UV Newton step whose ends straddle the freeze with the energy
  between them, and by a nested-search step across the line. Gates: from 272 K, 273.152494 K, 101809.090 Pa, ice
  0.0225; gate (b)'s UV 273.152629 K, 1e5 Pa, ice 0.4 (11 of its 15 TP calls the search's volume solves at 273.16 K);
  the network vessel's interval 2 ends at 273.136156 K, 322098.7 Pa, 0.369 kg of ice. Its open point was pure water's
  triple point (2778 TP calls at e4769fa2, 2798 at 703d69b3): 7bd5fafc.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Traces and probes of the quick-check regressions after the range work (`8220d4bc` EOS root lost at absurd trial partial pressures, `fd0dc531` roundoff solids in an empty tank, `87c106f6` crystal-solve stop test): seed replays and failing-iterate dumps (index lane and single-seed Gradle runs, nothing tracked)

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-17. Index: [../INDEX.md](../INDEX.md).

### History

- Done 2026-10-17 (report `QUICK_CHECK_FIXES.md`)
