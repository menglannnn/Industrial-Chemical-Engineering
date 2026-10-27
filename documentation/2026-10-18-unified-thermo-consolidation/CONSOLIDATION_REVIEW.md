# Consolidation of the unified thermo port: review (t35, 2026-10-18)

Branch `claude/unified-thermo-consolidation`, base `c9aea46e` (= port `3db5e5d3`), 14 code commits `8ca973e6`..`74785ddb` plus the changelog commit. Proof between commits: the javac lane (`tools/unified-thermo-consolidation/lane-check.sh`, 16 groups) against the `c0` baseline at the base: 973 tests (975 less the two removed), every printed line bitwise except the one row named below; `PackagedSparseLuTest` fails at the base too; `science/column` is not on the lane. Plan: `CONSOLIDATION_PLAN.md` (owner decision 2026-10-18: O1, O2, O4, O5 yes; O3 no).

## Before and after (Java files, lines)

| Package | main files | main lines | test files | test lines |
|---|---|---|---|---|
| science/thermo | 30 → 28 | 10 698 → 10 085 (-613) | 51 | 12 051 → 11 934 (-117) |
| science/fluid | 39 | 13 716 → 12 632 (-1 084) | 66 | 9 802 → 9 783 (-19) |
| science/material | 19 | 3 452 → 3 442 (-10) | 21 | 2 536 |
| runtime, runtime/fluid | 27 | 7 145 → 7 039 (-106) | 74 | 11 684 → 11 512 (-172) |
| science/column | 82 | 18 979 → 18 845 (-134) | 81 | 15 386 |
| other (client, network, registry, world, math, root) | 33 | 6 113 → 6 109 (-4) | 19 | 2 207 → 2 204 (-3) |
| total | 230 → 228 | 60 103 → 58 152 (-1 951, 3.2 %) | 312 | 53 666 → 53 355 (-311) |

Themes (commit, lines): dead declarations `8ca973e6` -280 and `d88ec0ed` -85; interface folds `4bc62e7c` -140; same-argument parameters `3a1e2f9e` -45; comment narratives `b0ef7e44` -611 (PassiveStepSolver), `5e4b4722` -270, `5c5b51b6` -208, `6afbfa6d` -223, `e309dbaa` -164 (HoldClassWorldsTest); duplicates `74785ddb` -62 (kernel sparse-pair branches, step-solver run walks as `PassiveStepSolver.Runs`); owner decisions O4 `265cf745` -77, O2 `919cbf82` -17, O1 `7b77fb25` -35, O5 `517a6278` -76.

## Removed, by name

- Classes: `EquilibriumService`, `PhaseEvaluator` (folded into `FluidTpEquilibrium` and `CubicPhaseEvaluator`); nested: `PhaseDerivatives.Capability`, `EquilibriumResult.UnsupportedReason.SPECIES_NOT_IN_PACKAGE`, `EquilibriumResult.Property.ENTROPY`, `TranslatedPengRobinson.Phase` views.
- Tests: `LiquidFullBubblePointTest.theHeldOutUvIsTheFluidAnswerOfTheCrystalCompetitionsMaterial` (O4); `CubicPhaseEvaluatorTest.derivativeBlocksAFamilyDidNotProvideAreRefused` (O5; its commit message misnames the sibling test, which is `derivativesMatchCentralDifferences`).
- Printed reference line: `G3F9DataRangeBoundaryTest.answeredContinuouslyJustOutsideEveryDataBoundary` row `hydrates (D8)` (O1); every other printed line is bitwise.
- Assertions inside kept tests: O1 the hydrates request and the contract `assertThrows` in `FluidTpEquilibriumTest.contractRefusalsAreTypedAndCarryNoPhases`, the hydrates request in `FluidTpEquilibriumPhUvTest.refusalsAreTyped`; O2 the ammonia, absent-species and sub-basis refusals in the same two tests (13 sub-basis requests now run over the full basis through `PhaseTestSupport.over`); O5 the `supported()` assertion in `CubicPhaseEvaluatorTest.derivativesMatchCentralDifferences`, the `capabilities()` and composition-derivative `assertThrows` in `GasSolidEquilibriumTest.theCrystalsDerivativesAndItsAnchoredView`; O4 `G5F6ConsistencyTest.theGridsThreePhaseStatesAreAnswered` asks `tp(..., FLUID_ONLY)` instead of the held-out answer (same printed grid).
- Main API that went (all unreferenced or test-only): the 8-argument `FluidThermodynamics.state`, `FluidTpEquilibrium.depositionTemperature(double, ...)`, `freezingTemperature(double, List, double[])`, `depositionPressure(double, List, double[])`, `species(int)`, `tpCrystalsHeldOut`, `uvCrystalsHeldOut`; `IslandCoordinator.resolveDeliveryFence`, `certificates()`; `CancellationToken.deadlineNanos`, `isDeadlineExceeded`, `isCancellationRequested`; `IdealGasFunction.revision`; `SolidPhaseEvaluator` defaults; `PassiveStepSolver.velocityCap`; `V3GeneralNeuralFeatures.decode/targets/flow/componentScale/outputWidth`; test-only constructors of `InletPhase.Supply`, `PassiveNetwork.Reservoir`, `FluidView.State`, `TangentPlaneStability.Result`, `TranslatedPengRobinson`; the 15-argument `FluidTpEquilibrium.Settings` (now 12 components and five public constants).

## Final test (Opus)

t36, 2026-10-18: `lane.sh` m11 on `688f4cd7`, all four steps, against m10 (`3db5e5d3` = `c9aea46e`); tools `compare-junit-xml.pl`, `compare-campaign.sh`; logs `tools/unified-thermo-consolidation/logs/m11-logs.tar.gz`. Full test 1474 = 1476 - 2 (exactly the O4 and O5 tests), 0 failures; per class, `<system-out>`/`<system-err>` bitwise apart from timing fields, stack-trace line numbers and the `hydrates (D8)` row (254 same, 16 timing-only, 1 physics line). The column suite, the only check of the kernel fold `74785ddb`, is in that result: every `science/column` class is bitwise beyond timing. G2 chain-100 deviation 0; GameTests 31/31.
Campaign: every seed's classes, the `.classes.txt` files and clean counts equal m10 (cold 911, bundled random 986, defaults 1000); defaults COUNTERS identical. **Not bitwise:** COUNTERS of cold random (implicitSolves 231843 → 233463, newtonIterations 2698653 → 2714846, phaseSeeds 16660 → 17219, …) and bundled random (124352 → 124482, …); cold seeds 29, 473, 555, 988 hold at the same tick, island and committed tick but with `wall deadline` in m10 and the solver's own limit in m11 (these seeds run 18-35 s). Bundled random's SEED blocks are identical; its F (budget) seeds 47, 241, 430 hold on the wall deadline in both. Likely cause: wall-clock budget cut-offs, not the code; not proven. Sent back to the orchestrator as instructed.
Decisive check (alone, THREADS=1, `07-alone-688-*` and `07-alone-c9a-*`; the owner's geosim ran on 5 of 16 cores): cold seeds 29, 473, 555, 988 are bitwise between `688f4cd7` and `c9aea46e` (every SEED and HELD line, COUNTERS identical), so the cold difference is timing. Bundled seeds: 430 is bitwise; 241 holds at the same tick on the 2 s wall budget (`wallBudgetMilliseconds`) in both runs, and its newtonSolves still differ (3912 → 3929); 47 gets an extra `HELD: wall deadline` (tick 1826, island 42) at `688f4cd7` only, newtonSolves 1886 → 1921, seed 18.4 s against 17.3 s. Every difference that remains alone sits behind a wall-deadline hold. By the rule this is a code difference at bundled seed 47; its mechanism is the wall budget, not a computed result. A replay with the wall budget off would separate the two; it was not run.

## Proposals for the owner (not behaviour-free; none implemented)

- P1 `V3SimultaneousColumnSolver`: seven `solve*` overloads with 82 duplicate parameter lines; one entry with a parameter record. P2 `PhaseLayout.decode/residual` carry an `offset` every caller passes as 0 (42 mentions). P3 `ConservativeTransport.reconstruct` 8-argument form (test-only) and `PipeTransfer.Accumulator.add` weight (always 1). P4 O1 was implemented as the removal of `hydrates` only; the two-value collapse would rewrite about 200 test requests and every printed competition name.

## Open behaviour-free items

Test narratives beyond HoldClassWorldsTest (FilterBlockLineIslandTest 113, ExtremeTopologyIslandTest 102, PrecipitatedSolidsTest 88, FluidTpEquilibriumP3Test 82 lines in blocks); column comments (V3ColumnCalculator 513 comment lines); exact test duplicates (267 lines, mostly column fixtures); merged helpers of the three `FluidTpEquilibrium*Test` classes. `DeadCode` reports `Cdu17TestCatalog.withProduction` dead; it is live through an `@ExtendWith` extension.

## Code guide delta (for the port's merge refresh; the main checkout's guide predates the port)

- thermo-materials: `FluidTpEquilibrium.tp/ph/uv(request)` and `CubicPhaseEvaluator` are the types (no `EquilibriumService`, `PhaseEvaluator`); `PhaseDerivatives` has no capability set; `PhaseCompetition(crystals, packageCrystals)`; `Settings` 12 components; a request names the contract basis or throws; no held-out fluid answers; `TangentPlaneStability.cholesky` is shared.
- fluid-science: `PassiveStepSolver.Runs` (interior, node edges, walk) behind `reopenable`, `initialMassFlows`, `closeDeadHeads`; `FluidThermodynamics.state` has one 7-argument form. fluid-runtime: `IslandCoordinator` without `resolveDeliveryFence`/`certificates()`; `CancellationToken` trimmed. column-v3: `V3GeneralNeuralFeatures` without `decode`/`targets`. verification: `tools/unified-thermo-consolidation/` (lane-check, DeadCode, condensation specs). client-gui: no change.
