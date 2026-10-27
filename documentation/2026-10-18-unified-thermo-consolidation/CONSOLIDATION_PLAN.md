# Consolidation of the unified thermo port: plan (t35, 2026-10-18)

Branch `claude/unified-thermo-consolidation` from `c9aea46e`. Goal: smaller and simpler code, every number bitwise the same. Proof: javac lane (16 groups, 975 tests, baseline `c0` at c9aea46e in `/tmp/t35-logs`) between commits; the final lane (`lane.sh` m11 against `build/m10-logs`) is an Opus task after this thread (owner, 2026-10-18). Tools in `tools/unified-thermo-consolidation/` (`DeadCode.java` javac-attributed liveness, `comments.pl`, `dupes.pl`, `outline.pl`).

## Inventory (lines; main 230 files / 60 103, test 312 / 53 666)

| Package (main) | lines | comment | in blocks >= 8 | dead | test-only | notes |
|---|---|---|---|---|---|---|
| science/thermo/phase | 8 424 | 2 153 | 1 055 | 44 | 141 | FluidTpEquilibrium 5 993 (190 methods, 170-line header); 3 single-implementation interfaces |
| science/thermo | 2 274 | 454 | 251 | 1 | 17 | Cholesky duplicated in TangentPlaneStability and FluidTpEquilibrium (2 x 30) |
| science/fluid/thermo | 3 075 | 1 145 | 593 | 14 | 74 | FluidThermodynamics 1 499 (44 % comment) |
| science/fluid/network | 8 247 | 3 408 | 2 072 | 21 | 31 | PassiveStepSolver 4 624: 2 006 comment lines, 81 blocks >= 8 (1 315 lines) |
| science/fluid/solver, state, linalg, transport | 1 800 | 450 | 230 | 4 | 40 | PhaseLayout decode/residual offset always 0 |
| science/material | 3 453 | 581 | 264 | 25 | 96 | dead accessors; Eppr78Groups table style |
| runtime, runtime/fluid | 7 146 | 1 452 | 445 | 34 | 126 | IslandCoordinator.Settings.adaptive always false; FluidIslandCommand slice always 5 |
| science/column (unchanged by the port) | 18 979 | 2 733 | 1 504 | 138 | 198 | V3GeneralNeuralFeatures.decode 46 lines dead; 4 x 10-line duplicate blocks in V3SimultaneousColumnSolver |
| tests (all) | 53 666 | 7 090 | 3 307 | 155 | - | exact duplicate runs 267 lines; HoldClassWorldsTest 471 comment lines |

Exact duplicate code in main is small (181 lines); the bulk is comment narrative and solver code itself.

## Behaviour-free candidates, by lines saved (estimates)

1. Comment condensation, port packages first (PassiveStepSolver, FluidTpEquilibrium, FluidThermodynamics, PhaseLayout, ConservativeTransport, NetworkReduction, VesselCrystals, EquilibriumResult, kernel, stability, IslandCoordinator, certificates): decision histories and traced numbers become the rule they state; measured reasons for a term stay. Main -2 500 to -3 000; tests (HoldClassWorldsTest, line-island tests, P3Test) -700.
2. Dead and test-only main code: 294 dead lines (column decode/targets, dead accessors, `velocityCap`, dead overloads); test-only factories and helpers deleted with their only test or moved to the test tree where they are fixtures. -500.
3. Interface folds and nesting: `EquilibriumService` and `PhaseEvaluator` (one implementation each) fold into `FluidTpEquilibrium` and `CubicPhaseEvaluator`; `Capability` nests in `PhaseDerivatives`; dead `PhaseKind.fluid`, `PhaseDomain.packageId/open`, `EquilibriumResult.entropy/molar*`. -150.
4. Same-argument parameters and one-value switches: `FluidTpEquilibrium.Settings` (5 constant components), `IslandCoordinator.Settings.adaptive`, `FluidIslandCommand` slice, `PhaseLayout.decode/residual` offset, `TranslatedPengRobinson.evaluate` root, `InletPhase.Supply`, `Reservoir`, `ConservativeTransport.reconstruct`, `Deposition` trace, `Accumulator.add`, `TangentPlaneStability.Result`. -100, simpler signatures.
5. Duplicates: Cholesky (`factorShifted/solveCholesky`) shared; `V3SimultaneousColumnSolver` attempt blocks; `CubicPhaseEvaluator` 55-78; kernel 320-350; `PassiveStepSolver` 198/2379 and 1688/2386; `ColumnV3Network`/block entity 765/656. -150.
6. Tests: dead helpers (-155), exact duplicates into shared fixtures (-250), merged helpers of `FluidTpEquilibrium{,P3,PhUv}Test` and the G3/G5 support (-300), defensive checks that cannot fire (-100).

Expected: main about -4 000 (7 %), test about -1 500. Big classes shrink in place; no split into files (a split deletes nothing). Commits: one theme each, javac lane on the touched groups, bitwise printed output.

## Proposals for the owner (not behaviour-free; numbers at the network's states)

- O1 `PhaseCompetition` to two values. Production requests only `FLUID_ONLY` or `FLUID_AND_CRYSTALS` (NetworkPhaseEngine 90-105); named crystals, `hydrates` and the contract's qualified-competition sets are test-only. Deletes about 150 main lines plus the refusal tests (`PHASE_COMPETITION_NOT_QUALIFIED`, hydrates). Risk: a package admitting crystals in separate sets loses per-set qualification.
- O2 Positional requests. If production always requests over the contract basis, the species-name mapping and `SPECIES_NOT_IN_PACKAGE` (about 100 lines) go. Risk: its tests go; a wrong basis becomes an exception.
- O3 Onset searches. Production calls only `depositionTemperature(crystal, 1 atm, pure species)` (NetworkCrystals 81). `frostTemperature`, `freezingTemperature`, `depositionPressure`, the pressure search and `DepositionOnset.approximation` (about 300 lines) serve the G4/G5 holdout tests only. Deleting them deletes those references.
- O4 `tpCrystalsHeldOut` / `uvCrystalsHeldOut` (43 lines): fluid-only comparisons for tests.
- O5 `PhaseDerivatives` capability gating (about 40 lines): one fluid family provides every block; asserted by CubicPhaseEvaluatorTest and GasSolidEquilibriumTest.
- O6 Test implementations of `SolidPhaseEvaluator` and `FreeWaterModel` keep those interfaces; folding them moves tests onto the production models and changes their printed numbers.

Owner decision 2026-10-18: O1, O2, O4, O5 yes; O3 no; O6 stays as planned (interfaces kept). O1 implemented as the removal of `hydrates` only: the two-value collapse would rewrite about 200 test requests and every printed competition name.
