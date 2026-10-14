# Connection rules survey: what relies on equipment contacts and two-way boundaries (2026-09-28)

- Author: Claude (Opus 5.5, subagent of the orchestrator). Read-only survey; no build, test or client was run.
- Base: code claims are read at `da8fb533` and cited `path:line` at that commit, unless marked HEAD. While this survey ran, P1 landed on the branch as `983cc915` (code and rebuilt fixtures) and `e4f0cd7a` (gate test `OnlyPipesConnectTest`). This document compares every finding with that commit.
- Rules surveyed (owner, 2026-09-28): **P1 = D20** only pipes connect; **P2 = D21** a generator only supplies, a void only receives.
- "Unverified" marks a claim that was not checked line by line.

## 1. Summary

Conclusion: P1 breaks a small, known set of fixtures, and `983cc915` already rebuilt all but one. P2 is mostly the rule the solver already enforces; what is left is a question of how exact and how early.

| Count | Value |
|---|---|
| Fluid unit test classes at `da8fb533` (`science/fluid`, `runtime`, `fluid/benchmark`) | 125 classes, 524 test methods |
| Classes that build block layouts (Device, compile, registry, `EventPathWorld`) | 36 classes, 181 methods |
| Graph-only classes (build `PassiveNetwork` directly; P1 cannot touch them) | 89 classes, 343 methods |
| Unit test methods whose layout has an equipment contact that P1 removes | 19 methods in 11 classes (section 4.1) |
| ... of which rebuilt by `983cc915` | 18. **Not rebuilt: `ActuatedIslandGeneratorClosureTest`** (it now passes but no longer tests an actuated island) |
| Tests asserting a non-contact that already holds today | 2 classes (stacked tanks; extreme-topology node counts). They keep passing |
| GameTests (all `src/fluidGameTest`) | 37 methods in 16 holder classes; **3 affected by P1**, all rebuilt by `983cc915`; none depends on a generator receiving or a void supplying |
| In-game scenario rigs with an equipment contact | 3 of 8 rigs (scenarios 2 and 2b in `tools/phase-ports-probes/in-game/`), plus the unit probe `holds/src/FedPumpLifetimeProbeTest.java`; not rebuilt |
| Tests where the assertion is about a generator or void direction (P2) | about 25 methods (section 5); about 24 more use a dead-headed fixture without asserting the direction |
| Bitwise references at risk from P1 | none (chain-100 is graph-only; the 33 junction lines have boundaries only on one pipe's faces) |
| Bitwise references at risk from P2 | low: no generator in the junction fixtures receives, so the 33 lines move only if P2 changes a path every generator island takes; chain-100 has no generator or void |
| Fuzz worlds with at least one contact P1 removes | 346 of 1000 (471 contacts), counted by applying the `da8fb533` rule to the logged layouts of the 1000-seed reports, not by a rerun; about 35 to 40 % by an analytic estimate |
| Player-facing text telling a player to put equipment against equipment | none found |

The five fixtures that matter most:

1. `ActuatedIslandGeneratorClosureTest.aLowCrudeGeneratorInAValveIslandDoesNotHoldTheSteps`: a valve against the crude generator. Not rebuilt. Under P1 the valve branch is a dead end and pruned (D18), so the E2 world has no actuator left and the test is vacuous.
2. `FluidPumpedFillLineTest` (6 methods, layouts `GUPR...`, `RCPR`): generator against pump, tank against compressor. Rebuilt; the gas transfer needed a 0.1 m suction block to stay inside its 3 Pa assertion.
3. `ElevatedBlockLineIslandTest.elevatedLinesIntegrateFortyIntervalsLikeTheirFlatControl`: pump standing on the generator. Rebuilt; the pump's suction is now two blocks up, so the expected tank pressure formula changed and the printed pumped-line values moved.
4. `PumpBottomPortDrainTest` (2 methods) and `TraceFlushFedPumpTest`: pump on a tank's bottom face. Rebuilt with one pipe between; the LIQUID port is kept.
5. `TopologyEventFenceTest.aWalkGoesOnThroughAFilterInADormantIslandOfItsOwn`: filter against filter and tank against filter (the class X world). Rebuilt; whether it still reproduces the original fence defect on the pre-fix tree was not re-checked.

## 2. How connections form today (at `da8fb533`)

### 2.1 The rule

- Every registered block is a `PhysicalFluidTopology.Device` with a kind, a position and a `facing` (`runtime/fluid/PhysicalFluidTopology.java:19-30`).
- Boundaries are tank (`RESERVOIR`), `GENERATOR` and `VOID` (`:27`). Actuators are pump, compressor and valve (`:28`).
- `connects(direction)` (`:29`): a pipe, tank, generator or void connects on all six faces. A pump, compressor, valve or filter connects only along its facing axis, at both ends of the axis.
- Two adjacent blocks are linked when both connect towards each other and they are **not both boundaries** (`:67-70`).

| Pair side by side | Connected at `da8fb533` | Under P1 |
|---|---|---|
| pipe and anything (equipment on its axis) | yes | yes |
| tank, generator or void against pump, compressor, valve or filter (on its axis) | yes, a direct link (`:71-74`) | no |
| pump, compressor, valve or filter against another of them (on axis) | yes, through a virtual middle node (`:75-80`) | no |
| tank-tank, tank-generator, tank-void, generator-generator, generator-void, void-void | no (`:70`) | no |
| actuator or filter touched off its axis | no | no |

### 2.2 What each kind of link compiles to

- **Boundary against anything else** (`:71-74`): one link from the boundary to the other block, with half of the other block's geometry. If the boundary is a tank, the link records the face (`tankFaces`, direction from the tank to its neighbour).
- **Two non-boundaries** (`:75-80`): a virtual PIPE node at the midpoint and two half-links, each with half of its own block's geometry. This is how pipe-pipe, pipe-pump and pump-pump contacts compile alike.
- **Tank faces to ports** (`:173-176`): the face of a run's tank end decides its port: `UP` = `VAPOR` (top, gas first), `DOWN` = `LIQUID` (bottom, heavier liquid first), a side = `BULK`. A generator, void, junction, actuator or filter end is always `BULK`. So a pump standing directly on a tank's bottom face drew from the tank's LIQUID port through its own direct link.
- **In-line equipment** (`:119`, `:189-192`): an actuator is a node; the run on its facing side is driven from it (`outlet`), the run on the other side is its suction. A filter is a node split into two junctions joined by its own filter edge (`:142-149`); which runs go to its positive junction is decided by `positiveSide` (`:188`). Both read the neighbour's point, which may be a boundary when equipment touches equipment.
- **Runs and islands** (`science/fluid/topology/TopologyCompiler.java:29-74`): chains of pipe blocks with two connections become one run; every other block is a node. Dead-end PIPE leaves were pruned (`:37-43`). From HEAD (`73624241`, D18) every non-destination leaf is pruned, pumps and filters included; from `28f8c16f` (D17) islands split at generators and voids.

### 2.3 Every place that applies the rule

| Place | Line at `da8fb533` | Role | Changed by `983cc915` |
|---|---|---|---|
| `PhysicalFluidTopology.compile` | `:67-81` | the links of a compile | yes (`Device.joins`) |
| `PhysicalRegistry.submit` walk | `:157-158` | which devices an event touches (fences) | yes |
| `PhysicalRegistry.apply` component walk | `:256-258` | what a batch recompiles | yes |
| `PhysicalRegistry.reached` walk | `:321-323` | refencing of later events | yes |
| `FluidDeviceBlock.connects` | `world/level/block/FluidDeviceBlock.java:54-60` | the block-state connection flags; only the pipe model draws arms (`blockstates/fluid_pipe.json`) | no. Visually consistent anyway: a pipe still connects to every neighbour |
| `TopologyCompiler.invalidPumps` | `:82-83` | skips the bypass test when a pump touches a boundary directly | no. Becomes dead code under P1 (a pump's neighbours are pipe nodes); harmless |
| `TopologyCompilerTest.parallelPipesAndBranchesSurviveButDeadLegsDoNotCarryFlow` | `src/test/.../topology/TopologyCompilerTest.java:18-22` | a direct tank-tank link in an abstract Node/Link graph | not a block layout; unaffected unless P1 is also made a compiler invariant |

## 3. Direction of generators and voids today

The rule already exists: `PassiveStepSolver.boundaryAllowed` (`science/fluid/network/PassiveStepSolver.java:2034-2038`) forbids flow into a generator or out of a void on every connection, movers included. What P2 can still change is where that rule is applied late or loosely.

| Mechanism | Where | What it does |
|---|---|---|
| Static rule | `PassiveStepSolver.java:2034-2038` | no flow into a GENERATOR, none out of a VOID, none in a blocked direction |
| Pass loop | `:492-525` | after a converged pass, a connection with an illegal direction and \|q\| > 1e-10 kg/s is closed; devices are decided first (`:507-520`), so a boundary closure can wait a pass |
| Acceptance | `:629`, approximate path `:797` | a result with an illegal direction above the threshold is refused |
| Start closures | `closeIllegalStarts` `:1605-1621` | closes illegal passive directions at pass 0, but only after a solve of the same structure was accepted |
| D12 cold closure | `closeColdReceivingGenerators` `:1780-1824` | on the cold rate seed only, closes a passive generator run onto a junction of degree 3+ that the one-way estimate shows receiving. Skipped for islands with solids, runs through an actuator (A69), runs ending at a vessel, or a failed estimate. **There is no void counterpart** |
| Dead heads, reopening | `:195-215`, `closeDeadHeads` | read `boundaryAllowed` for both directions of a run |
| Phase-port capacity | `PassiveStepSolver.java:2116-2130` (A21) | an end facing a generator takes no share of a vessel's phase capacity |
| Inlet check | `InletPhase.java:114` | counts only ends allowed to deliver |
| Rest and feasibility (HEAD) | `NetworkReduction.directions` / `atRest` (`89af686c`, D19) | uses the same rule to decide an island at rest |
| Dormant filter island | `PhysicalFluidTopology.java:160-161`, `SolidEventIntegrator.java:272` | an internal GENERATOR/VOID pair with the filter edge blocked both ways; never a world object |
| Rate-solve copies | `PassiveIntervalSolver.java:355`, `SolidEventIntegrator.java:70` | tanks become two-way `PORT` nodes in a rate solve; generators and voids keep their kind |

So today a generator can receive, or a void supply:

1. inside pass 0 of a cold solve (not in an accepted result);
2. by at most 1e-10 kg/s in an accepted result; this residual is booked as a real negative boundary transfer and replayed by a certificate (per `PHASE_PORTS_REVIEW.md` section 9b; the booking line in `ConservativeTransport` was not re-read here);
3. in the cold rate seed of islands the D12 closure skips (solids, actuated runs, vessel ends, failed estimate), until the pass loop closes it.

## 4. Affected by P1

### 4.1 Unit tests (`src/test/java`)

| File | Test | Touching pair at `da8fb533` | What it asserts | Smallest rebuild | Status at HEAD |
|---|---|---|---|---|---|
| `runtime/fluid/PhysicalFluidTopologyTest.java` | `aPumpOnATanksBottomFaceTakesTheBottomPortInEitherOrientationAndIdentityOrder` (`:166`) | pump directly under the tank's DOWN face (facing DOWN and UP) | tank end LIQUID, pump end BULK, the mover's connection starts at the pump junction | tank to y=2, pipe at y=1; runs 1 m longer, ports kept, no solve | rebuilt |
| same | `aFilterOnATanksTopFaceKeepsTheTopPortInBothOrientations` (`:193`) | filter directly on the tank's UP face | VAPOR at the tank, a BULK filter edge, a nonzero flow | pipe at y=1, filter y=2, pipe y=3, void y=4 | rebuilt. The tank side is now a pipe run, not the filter's boundary link, so the "filter touching a tank" branch of the compile is no longer exercised |
| `runtime/fluid/ElevatedBlockLineIslandTest.java` | `elevatedLinesIntegrateFortyIntervalsLikeTheirFlatControl` (`:211`, fixture `risingPumpLine` `:101`) | pump (facing UP) standing on the generator | the pumped line's tank pressure against a formula (1e-4 relative) | pipe at B+1, pump at B+2, one pipe at B+3; the formula uses a two-block suction head | rebuilt. The comment baselines 852452.8285 / 861063.0384 Pa (`:248-249`) and the printed pumped-line values are stale |
| `runtime/fluid/FilterBlockLineIslandTest.java` | `aFilterBlockCostsAPumpedLineNoIntervalOfItsOwn` (`:359`, fixtures `pumpLine` `:99` and `pumpControlLine` `:125`) | generator against the pump inlet, in both fixtures | the filter costs no extra interval against the control | a pipe between generator and pump in both fixtures | rebuilt |
| `runtime/fluid/SolidRuntimeTest.java` | `physicalPumpFilterAndNitrogenReceiverStartTogether` (`:83`) | generator against the pump inlet | filter clogs, first flow 0 (1e-9), tank without solids | G P M P F P R, tank id 7 | rebuilt |
| `runtime/fluid/PumpBottomPortDrainTest.java` | `aPumpOnTheBottomFaceDrainsTheTankAtTheDefaultTarget`, `...AtATenthOfTheDefaultTarget` (`:99-100`) | pump directly under the tank's DOWN face | the tank drains through its LIQUID port at the target | one pipe between tank and pump, pump and below one block lower | rebuilt |
| `runtime/fluid/TraceFlushFedPumpTest.java` | `aPumpedWaterLineRunsThroughTheWholeLifetimeOfItsJunctionsGasTrace` (`:13`) | pump directly under a tank (same fixture shape) | the junction's gas trace lifetime | one pipe between | rebuilt |
| `runtime/FluidPumpedFillLineTest.java` | all 6 methods (`:101`, `:116`, `:126`, `:139`, `:156`, `:172`) | generator against pump (`GUPR...`); suction tank against compressor (`RCPR`) | commits, no hold, fill to shutoff, through-flow, six-tank start, gas transfer closes at the scaled limit (3 Pa) | `GPUPR...`; `RWCPR` with a 0.1 m suction block (a 0.05 m block closes 4.2 Pa low) | rebuilt |
| `runtime/FluidThermoDomainHoldTest.java` | `aReproducibleDomainFailureHoldsWithTheDedicatedStatusAndWaitsForAnInputChange` (`:52`) | tank against compressor (`RCPR`) | a domain hold names the device ("compressor at 1, 64, 0") | `RPCPR`; the named position becomes x=2 | rebuilt |
| `runtime/fluid/TopologyEventFenceTest.java` | `aWalkGoesOnThroughAFilterInADormantIslandOfItsOwn` (`:28`) | filter against filter (both on axis), tank against filter | keeps up (fence regression of the class X world) | F(2) P(1) F(0) P(-1) T(-2) at the same ticks | rebuilt; reproduction of the original defect not re-checked |
| `runtime/fluid/ActuatedIslandGeneratorClosureTest.java` | `aLowCrudeGeneratorInAValveIslandDoesNotHoldTheSteps` (`:21`) | 200 kPa valve at (0,0,0) facing EAST, crude generator at (-1,0,0) on its inlet | keeps up (hold class E2: an island with an actuator) | pipe at (0,0,0), valve at (1,0,0) EAST, pipes at (2,0,0) and (3,0,0), void at (4,0,0) | **not rebuilt**. Under P1 plus D18 the valve branch is pruned, the second valve is already a dead end, and the island has no actuator: the test passes vacuously. Re-check the rebuild reproduces E2 on the pre-fix tree |
| `runtime/fluid/DeadEndPruningTest.java` (HEAD) | `aFilterBranchOnATanksJunctionIsPrunedKeepsItsCakeAndRejoinsWithADestination` | void placed directly on the filter's axis end | the filter rejoins the tank's island with its cake | pipe then void | rebuilt |

That is 19 methods in 11 classes (FilterBlockLineIslandTest counted once).

Tests that rely on a non-contact that already holds (keep passing, no change):

- `PhysicalFluidTopologyTest.aStackOfTwoTanksJoinsTheUpperTanksBottomToTheLowerTanksTop` (`:146`, the stacked-tank check at `:156-158`).
- `ExtremeTopologyIslandTest` layouts (`:141-157`): generators in a row, and generators alternating with tanks in a row, joined only through the pipe row above. Node and connection counts (`:374-375`) rely on boundary-boundary non-contact.

Checked and unaffected (every equipment block separated by a pipe, or touching only off-axis): `PhysicalRegistryTest`, `InlineFilterLineHoldTest` (the in-game line and all 12 letter layouts), `DeadHeadedLineIslandTest`, `IslandCertificateTest`, `FullTankSolidsEventTest`, the checkpoint store/format/codec tests, `FedTankOpenOutletsTest`, `TankOutletsTest`, `LiquidFullTankOutletsTest`, `TraceFlushLiquidFullTankTest`, `PipePresentationTest`, `PresentationBucketTest`, `MixedGasJunctionStaticTest`, `LiquidJunctionTransientTest`, `MixedGasJunctionTransientTest`, `FilterIslandGeneratorClosureTest`, `JunctionPhaseAppearanceTest`, and at HEAD `FixedBoundarySplitTest`, `RestIslandTest`, `QueuedRemovalWalkTest`. Not opened line by line: `McpGameplayRegressionTest`, `FluidDeviceSpecDomainTest`, `FluidPacketCodecTest`, `WorldTopologyLedgerTest`, `AmbientCrudePresetTest` (unverified, but none appears in the P1 commit and none places a pump, compressor, valve or filter next to a boundary in a grep of their `Kind` uses).

### 4.2 GameTests (`src/fluidGameTest`)

Structures are `empty.nbt`; every block is placed in code.

| Class | Test | Touching pair | Rebuild | Status at HEAD |
|---|---|---|---|---|
| `FluidPumpedFillGameTests` | `aPlacedPumpedThreeTankChainFills` | generator against pump (`GUPRPRPR`) | `GPUPRPRPR`, tank indices shift by one | rebuilt |
| `FluidPhasePortGameTests` | `aPumpOnAGasTankIsRefusedWithItsReasonOnItsStatusLine` | pump directly on the tank's east side | a suction pipe between | rebuilt |
| `SolidPhaseGameTests` | `filterCaptureRecoveryPersistenceAndBreakDropConserveOwnedSolids` | generator against pump | a suction pipe between (and removed at the end) | rebuilt |

Watch in the refused-pump test: its assertion names the supply ("from reservoir at x,y,z", read from `InletPhase.java:180`); with the pipe between it must still name the tank.

The other 34 GameTests have pipes between equipment or only lone blocks: `FluidPumpStartupGameTests` (generator, inlet pipe, pump, outlet pipe, tank), `FluidPhasePortGameTests` outlets and compressor tests, `FluidHarnessGameTests`, `FluidTopologyGameTests`, `FluidPresentationGameTests`, `FluidPlacementGameTests` (RPPPR lines), `FluidServerBenchmark` (generator, pipe, tank and void ladders separated by pipes), the checkpoint, packet, property-reload, module, budget and column tests. `src/mcpCompat` places no blocks. In the 12 GameTests with a generator or void in a network, every generator feeds and every void receives; nothing asserts the direction.

### 4.3 In-game scenario rigs (`tools/phase-ports-probes/`, git-ignored)

| Rig | Placement | Touching pair | Rebuild |
|---|---|---|---|
| `in-game/NOTES.md` scenario 2 | `fluid_pump [facing=east]` at (1,-56,10) | pump against tank T2's east side | pipe at (1,-56,10), pump one block east |
| `in-game/NOTES.md` scenario 2 | `fluid_pump [facing=down]` at (0,-57,10) | pump under T2's bottom face | pipe at (0,-57,10), pump, pipe and void one block lower |
| `in-game/NOTES.md` scenario 2b | `fluid_pump [facing=down]` at (0,-57,30) | pump under T3's bottom face | same as above |
| `in-game/cmds/20-s2-side-pump.txt:1-3`, `21-s2-bottom-pump.txt:7-8`, `23-s2b-pump.txt:3-5` | the command files of those placements | as above | update with the notes; the cleanup in `21-...:3-5` then also needs x=4 |
| `holds/src/FedPumpLifetimeProbeTest.java:15-18` | a unit probe with a pump on a tank's bottom face | pump against tank DOWN face | one pipe between |
| `gui-layout/line.txt` | G P P V, pump, P, filter, P, valve, P, void | none | none |
| scenario 3 (compressor) | tank, pipe, compressor, pipe, tank | none | none |

The screenshots `06-pump-side-refused` and `07-pump-bottom-running` in `in-game/` show layouts that no longer connect. They stay valid as a record of the old rule. Finding F1 (`in-game/REVIEW_SECTION.md:38-46`) is defined on a pump standing on a tank's bottom face, so it has to be reproduced again with a pipe between. None of the rigs has a generator receiving or a void supplying (`NOTES.md:94` records that voids do not feed back; in F2 the tank reached 129.9 kPa against the 130 kPa generator without reversing). The rigs in `gui-fixes/` and the other probe folders place no blocks by command.

### 4.4 Examples

`examples/` holds property and reference generators (Python) and a material override. None builds or describes a fluid block layout.

## 5. Affected by P2

No test asserts that a generator receives or a void supplies. The tests below either assert the one-way behaviour itself or run at a state where a generator would receive if allowed. They keep passing if P2 only formalises today's rule; they are where a changed implementation shows first.

| File | Test | What is asserted | What P2 could change |
|---|---|---|---|
| `DeadHeadedLineIslandTest` | `aTankChargedAboveItsGeneratorRestsInsteadOfHolding` (`:188`), `aDeadHeadedElevatedLineRestsInsteadOfHolding` (`:204`), `aDeadHeadedFilterLineRestsInsteadOfHolding` (`:219`), `raisingTheGeneratorReopensADeadHeadedLineAndFillsTheTank` (`:238`) | every edge CLOSED, flow exactly 0, tank inventory to 1e-12; reopen at 400 kPa | a structural one-way end (a new mode instead of the closure) changes the CLOSED label and must still give exact zero |
| `IslandCertificateTest` | `deadHeadedLinesCertifyRestAndThenCostNothing` (`:155`), `aPropertyHoldFreezesACertifiedIslandAndResumeRequalifiesOverTheConfirmCount` (`:392`), `aModuleDriveWakesACertifiedIslandAtItsExactTick` (`:460`) | drift exactly 0.0; certification at exact tick counts | exact ticks depend on the first intervals being exactly zero |
| same | `grossFlowWithAZeroAverageIsNotRest` (`:546`) | certificate arithmetic on a synthetic tank-to-void reverse transfer | the transfer is excluded physically under P2; keep or relabel |
| `ExtremeTopologyIslandTest` | the fixture tests (`:229`, `:269-288`, `:327`) | a generator below its junction receives at most 1e-10 kg/s x slice and supplies nothing | exact-zero booking would tighten the tolerance; a pass-0 change moves the path |
| `ElevatedBlockLineIslandTest` | `aDeadHeadedElevatedLineBehavesLikeItsFlatEquivalent` (`:290`), `aLineChargedToItsOwnHydrostaticBalanceIntegratesAtZeroFlow` (`:262`) | flat and rising agree; balance to 1e-6 at zero flow | none expected |
| `ActuatedIslandGeneratorClosureTest` | `aLowCrudeGeneratorInAValveIslandDoesNotHoldTheSteps` | keeps up with a 261 kPa generator below a 472 kPa side (D12 cold closure, actuated runs excluded) | the main pass-0 case; and it first needs the P1 rebuild |
| `FilterIslandGeneratorClosureTest` | `aWaterGeneratorBelowAFilterIslandsJunctionDoesNotHoldTheRatePass` (`:19`) | keeps up | its filter is pruned since D18 (A75), so it no longer exercises the closure it is named after |
| `InlineFilterLineHoldTest` | `aFilterPocketFedThroughASmallPipeNeverHolds` (`:196`) | never holds; a 215 kPa generator faces a pocket minted at 406 kPa | the generator would receive at the start; exercises the closure path |
| `RestIslandTest` (HEAD) | `generatorsThatCannotPushIntoTheirVoidsAreAtRestAndNeverSolved`, `aRunBetweenTwoVoidsIsAtRest` | at rest, every flow exactly 0.0, "STEADY: no flow" | rest is decided by `NetworkReduction.directions`, which uses the same rule; unchanged if the rule is unchanged |
| `PhysicalFluidTopologyTest` | `aTankOverAVoidDrainsOnItsLevelHeadAloneAndRests` (`:250`) | rest flow 0 (1e-9) with a void held above the column | none expected |
| `PipePresentationTest` | `aStraightPipeAutomaticallyShowsReverseFlowAndPreservesBothDirectionsOfAReversal` (`:62`) | presentation of a synthetic void-to-generator reverse transfer | excluded physically under P2; a presentation test, keep |
| `MixedGasJunctionStaticTest` | `runCase` (`:48-77`) | `:72` outlets = count - 2, so no generator receives; `:71` allows the lower generator in the unequal cases to be closed | none expected |
| `RestIslandTest` (HEAD) | world at `:26-31` | a generator at (0,0,3) sits under a pump facing NORTH; off the pump's axis, so no contact today or under P1 | none |
| `JunctionPhaseAppearanceTest`, `MixedGasJunctionTransientTest` | star layouts | incidental: both generators far above the void; near rest a void above the junction could be closed (not asserted, not run) | none expected |
| `McpGameplayRegressionTest` | archived graphs with a generator whose pressure is raised | per review section 9b a generator below what it faces; which asserted state has it closed is unverified | unverified |
| Graph-only science tests | `BoundaryAndPhaseTest.sourceToVoidHasSteadyThroughputAndBothBoundariesRejectReverseFlow`, `LevelHeadTest.aGeneratorBelowTheBottomPressureStaysDeadHeadedWithoutReopens`, `LevelHeadTest.aDrainTheHeadLiftsAboveTheVoidInsideAStepIsReopenedAndNeverBottled`, `BackwardEulerStepTest.sourceAndVoidKeepSeparateNonzeroLedgersDespiteZeroNetTransfer` | the static rule itself; dead-headed generator; void reopening | the rule's own tests; any change to it shows here first |

About 24 more methods only use a dead-headed or supply-only fixture as a REST case without asserting the direction (checkpoint store and format tests, `PresentationBucketTest`, `FullTankSolidsEventTest`, `FedTankOpenOutletsTest`). GameTests with a generator filling a tank to rest (`FluidHarnessGameTests`, `FluidTopologyGameTests`, `FluidPresentationGameTests`) end with the generator dead-headed at zero flow; unverified whether any asserts the closure.

## 6. Bitwise references at risk

| Reference | Built from | P1 risk | P2 risk |
|---|---|---|---|
| `src/test/resources/fluid/regression/chain-100.json` (exact mode of `fluidSolverRegression`) | `SolverRegressionHarness.cosineChain`: 100 tanks joined by pipes, a graph, no blocks | none | none: no generator or void |
| `quiet-11312`, `quiet-11324`, `cold-11312` | saved islands of a stress-world snapshot (`core.dat`), usually skipped | none (graphs) | unverified; skipped without the snapshot |
| 33 junction lines (`LIQUID_JUNCTION` x8, `MIXED_GAS_TRANSIENT` x24, `MIXED_GAS_COST` x1; reference `tools/phase-ports-probes/holds/12-junction-lines.txt`) | `LiquidJunctionTransientTest`, `MixedGasJunctionTransientTest`: one junction pipe at the origin with tanks, generators and voids on its faces | none: every boundary touches only the pipe; boundaries on different faces are diagonal | low. The liquid case's generators (150 and 130 kPa) both supply: the junction estimates near 120 kPa and the printed Q(1s) = 54.00 kg/s agrees. The mixed-gas case has tanks and voids only. The lines move only if P2 changes a path every generator or void island takes (for example a new pass-0 step run unconditionally); `MIXED_GAS_COST` (286, a Newton count) is the first to show it |
| `F8_LAYOUT` / `F8_END` lines (`InlineFilterLineHoldTest`) | letter layouts with pipes between | none | low (only the dead-end layouts, already pruned) |
| `ElevatedBlockLineIslandTest` printed tank pressures | rebuilt fixture | moved by the rebuild (expected, not a regression) | none expected |
| Fuzz campaign baselines (clean seeds of 1000: random 845, defaults 822 on `89af686c`) | `HoldFuzzTest` worlds | **not comparable after P1**: 346 of 1000 worlds change connectivity | changes class counts wherever a generator is closed |
| GameTests 33/33 (fluid run ids) | code-built worlds | 3 rebuilt | none known |

## 7. Fuzz

`tools/phase-ports-probes/hold-fuzz/src/HoldFuzzTest.java`:

- A world is 2 to 12 items (`:114`). Item kinds (`:108-111`): tank, generator, void 2/16 each; pipe 3/16; pump 2/16; compressor 1/16; filter 2/16; valve 2/16. So 13/16 of items are equipment.
- The first item sits at the origin. Each later item picks a random existing block as anchor and a random face it connects on (`:119-120`).
- A pipe item places `length+1` pipes, `length` = 1 to 4. **An equipment item places `length` pipes and then the device, `length = r.nextInt(3)`** (`:121`, `:125-129`). With `length = 0` (probability 1/3) the device goes directly against its anchor.
- Axial devices face along the placement axis (`:128`), so a direct contact is always on axis: it connects today unless both blocks are boundaries.
- Side contacts with blocks other than the anchor also occur; they were not counted.

Analytic estimate:

- Second item: its anchor is the first block, equipment 13/16 of the time: 13/16 x 1/3 x 13/16 x (1 - (6/13)^2) = about 0.17.
- Later items: the anchor is equipment about 0.28 of the time once the anchor must connect on the drawn face: 13/16 x 1/3 x 0.28 x 0.67 = about 0.05 per item.
- Over 1 to 11 later items: P(no contact) = about 0.83 x 0.78 = 0.65, so about 35 % of worlds, before side contacts.

Counted from the logged layouts (not a rerun): the `layout` lines of `hold-fuzz/logs/08-campaign-closed-start-1-1000-report.txt` (random) and `11-campaign-defaults-1-1000-report.txt` (defaults), with the `da8fb533` rule applied to each world:

- **346 of 1000 worlds** have at least one contact that connects today, 471 contacts in all (826 equipment adjacencies, the rest already non-connecting).
- Most frequent pairs: generator-valve 52, filter-tank 35, pump-tank 34, filter-generator 33, filter-void 33, generator-pump 31, valve-void 30, pump-void 29, pump-valve 26, tank-valve 23.
- Of the 13 full-seed repro tests in `hold-fuzz/src/repro/`, seeds 118, 4, 48, 33 and 28 have contacts; 333, 6, 30, 168, 325, 613, 39 and 236 do not.
- Both distributions (`random`, and `holdFuzz.defaults`, which makes the same draws) have the same layouts, so both are affected alike.
- The generator's own anchor test (`connects`, `:71`) is the old rule, so it keeps producing such contacts.

Reduced fuzz worlds kept as gate tests: `ActuatedIslandGeneratorClosureTest` (valve against generator, not rebuilt), `TopologyEventFenceTest` (rebuilt), `DeadEndPruningTest` (rebuilt). `FixedBoundarySplitTest`, `RestIslandTest`, `FilterIslandGeneratorClosureTest`, `JunctionPhaseAppearanceTest`, `QueuedRemovalWalkTest` and the `InlineFilterLineHoldTest` worlds have no contact.

## 8. Player-facing text

- `src/main/resources/assets/createcheme/lang/en_us.json`: no string tells a player to place equipment against equipment, or that a generator can receive or a void supply. The closest is `gui.createcheme.fluid.connections.hint` (`:387`): "Readings for every connected pipe...", which fits P1.
- GUI classes (`FluidDeviceScreen`, `ColumnCalculatorV3Screen`): no such text.
- `CHANGELOG.md:20` (phase ports) describes tank faces as ports; it speaks of pipe ends, which fits P1.
- `documentation/`: design and review documents describe fixtures with a pump on a tank or a generator against a pump (for example `in-game/NOTES.md` scenario 2, `SOLID_PHASE_FLUID_SYSTEM_IMPLEMENTATION_REVIEW.md:444` "generator -> pump -> pipes -> tank"). These are records, not player instructions.
- `CHANGELOG.md:20` also speaks of "receiving generator runs" (the D12 closure); true today, reads oddly once P2 is the stated rule.
- The `minecraft-mcp` skill exists only on branch `claude/minecraft-mcp-skill-c3f0f0` (`03952fd3`). Its `SKILL.md:137-138` and `references/pitfalls.md:61` say a void cannot sit against a reservoir because "two boundaries never connect", which implies other pairs do; its build recipe (`:128-132`) already fits P1. This is agent guidance, not player text, but should be updated when that branch merges.
- Missing text: nothing tells a player that a pipe is now required. A pump against a tank shows "ERROR: pump needs two ports without a zero-storage bypass" or, after D18, belongs to no island; a filter or valve shows a dead-end diagnostic.

## 9. Open questions for the owner

1. **`ActuatedIslandGeneratorClosureTest` is vacuous under P1.** Recommendation: rebuild it with one pipe between the crude generator and the valve (layout in 4.1) and confirm on the pre-fix tree that the rebuilt world still reproduces the E2 hold. If it does not, keep it as a keep-up test and say so in its comment.
2. **Fuzz generator.** It still places equipment directly against its anchor a third of the time; under P1 those contacts connect nothing. Recommendation: change `length` for equipment items to `1 + r.nextInt(2)` so every device is piped, and re-baseline both distributions with a fresh 1000-seed campaign (8 workers). Every seed's layout changes, so the old per-seed classes stay as history only.
3. **In-game rigs.** Scenarios 2 and 2b stand pumps on tank faces, and finding F1 is defined on that layout. Recommendation: update `in-game/NOTES.md`, the three command files and `FedPumpLifetimeProbeTest` with a pipe block before the next in-game check, and re-reproduce F1 on the new layout; keep the old screenshots as the record of the old rule.4. **Player message for a lost contact.** Recommendation: a specific diagnostic on equipment whose only neighbours are equipment ("NO FLOW: needs a pipe between equipment") instead of the pump-port error or the dead-end text. Wording is the owner's call.
5. **What P2 changes, given `boundaryAllowed` already forbids the direction.** Options: (a) formalise only (no code change beyond a named rule), bitwise everywhere; (b) apply the one-way rule from pass 0 of every solve, not only the cold rate seed of passive, solid-free islands; (c) book a residual reverse flow at a generator or void (up to 1e-10 kg/s today) as exactly zero; (d) add the void counterpart of the D12 cold closure. Recommendation: (a) now, then (b) and (d) only where a fuzz class shows the defect; (c) rests on a numerical assumption and should be presented with a measurement first (fix-assumptions rule).
6. **Synthetic reverse transfers in presentation and certificate tests** (`PipePresentationTest:62`, `IslandCertificateTest:546`). Recommendation: keep them; they test arithmetic, not physics. Add a comment that P2 excludes the case physically.
7. **`FilterIslandGeneratorClosureTest` no longer exercises the closure it is named after** (its filter is pruned since D18). Recommendation: extend its world with a destination past the filter so the island's rate pass runs again, or rename it.
8. **Stale numbers.** `ElevatedBlockLineIslandTest:248-249` comment baselines and the printed pumped-line values changed with the rebuild. Recommendation: re-record them from the next gate run.
9. **Agent guidance on another branch.** The `minecraft-mcp` skill (branch `claude/minecraft-mcp-skill-c3f0f0`) describes only the boundary-pair rule. Recommendation: state "only pipes connect" there when that branch is next touched.
