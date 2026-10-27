# Test review, part `runtime` (src/test/java/com/wormzjl/icheme/runtime/)

Tree: main at 138f4993 (0.11.0). Criteria: CRITERIA.md. Seconds: blank; `build/test-review/xml/` does not exist in this worktree.
Tests = `@Test` methods (ExtremeTopologyIslandTest: 1 `@Test` + 3 `@ParameterizedTest` x 3 geometries = 10 runs).
External helpers named in the last column are outside this scope: FTS = `fluid.support.FluidTestSupport`, MTB = `science.material.MaterialTestBasis`, PCTC = `science.material.PilotCryogenicTestCatalog`.

| Class | Tests | Seconds | Verdict | Code | Reason (max 20 words; for A2 name the kept test) | Helpers/resources used |
|---|---|---|---|---|---|---|
| runtime/BoundedCpuSolveServiceTest | 8 | | KEEP | K6 | Worker pool admission, cancellation, deadline, shutdown; a lost or late completion would drop an island interval. | none |
| runtime/FluidDataRangeIslandTest | 1 | | KEEP | K3 | Owner rule 2026-10-16: leaving a data range never holds; notice on the status line. Code guide pins it. | FTS, bundled catalog |
| runtime/FluidIslandCommandTest | 3 | | KEEP | K3 | D37 S2: job past old soft budget is the full solve; wall-deadline hold keeps snapshot; cancellation. | FTS |
| runtime/FluidPumpedFillLineTest | 6 | | KEEP | K3 | WP5 in-game held fills: pump shutoff 601325 Pa, compressor ratio limit (D2, D7, D54), rig RWCWR (OD4). | FTS, MTB |
| runtime/PresentationMailboxTest | 3 | | KEEP | K3 | Only test of PresentationMailbox (column GUI): inputs at online deadline, closed sessions expire, engine epoch. | none |
| runtime/ProcessSolveServicesRequestTest | 2 | | KEEP | K6 | Command captures the catalog at admission, not worker start; request ids unique and monotonic. | V3TestInputs |
| runtime/V3ColumnCommandTest | 6 | | KEEP | K6 | Admitted V3 column command is immutable, revalidates cutoff and closure, cancels before scientific work. | V3TestInputs, Cdu17FixtureExtension |
| fluid/CadenceTrajectoryQualificationTest | 1 | | KEEP | K6 | Fixed and adaptive cadences stay within 2 % of the refined trajectory and conserve mass and energy. | FTS |
| fluid/CarriedGasTest | 1 | | KEEP | K3 | D41 gate (hold class E6-T card world, seed 844): bubble carried through a liquid-full junction. | EventPathWorld, MTB |
| fluid/CompletionWakeupTest | 2 | | KEEP | K6 | No lost wakeup during a pump; closed wakeup suppresses late callbacks; else islands stall silently. | none |
| fluid/CrystalIslandRuntimeTest | 3 | | KEEP | K3 | Crystal reaches views on the engine bucket only; typed phase hold parks; certified crystal replays bit-exact. | CertifyingRig, FTS |
| fluid/DeadEndPruningTest | 2 | | KEEP | K3 | D18 gate (hold class E1 worlds): dead-end branch pruned, dormant filter keeps its cake. | EventPathWorld, MTB |
| fluid/DeadHeadBesideFlowingBranchTest | 1 | | KEEP | K3 | Dead-head closure gate beside flow (owner decision on the closure pending); exact zero, no water moved. | FTS, MTB |
| fluid/DeadHeadedLineIslandTest | 4 | | KEEP | K3 | D19/D58 rest: closed line moves no material or energy; raising generator reopens and settles one column below. | BlockLineHarness |
| fluid/DrumReservoirTest | 2 | | KEEP | K2 | Drum registration (ports, level height) round-trips topology codec; pipes join only at nozzles. | EventPathWorld |
| fluid/DrySuctionTest | 2 | | KEEP | K3 | D58 A1 gate (seed 922): pump on an empty tank refused dry; thresholds and reason text. | EventPathWorld, MTB |
| fluid/ElevatedBlockLineIslandTest | 4 | | KEEP | K6 | Only elevated-line numbers: tank settles one water column off generator, pump shutoff less column. | BlockLineHarness |
| fluid/EmptyRiserTest | 1 | | KEEP | K3 | D54 gate: riser holds a column to its head; nothing reaches the tank 8 m up. | FTS, MTB |
| fluid/EmptyTankFillsTest | 1 | | KEEP | K3 | D54 gate on E5 card: empty bag fills full, rests at 130.7 kPa head pressure. | FTS, MTB |
| fluid/ExtremeTopologyIslandTest | 4 (10 runs) | | KEEP | K3 | D12 regression: extreme grids integrate with ledger, direction and rest oracles. See optional trim below. | FTS, MTB |
| fluid/FedTankOpenOutletsTest | 2 | | KEEP | K3 | Defect F7 in-game placement order replayed; D54 tank runs dry and rests, nothing fed. | FTS |
| fluid/FilterBlockLineIslandTest | 11 | | KEEP | K3 | Filter/valve line defects (phantom trace, pump shutoff); tank behind filter holds what plain line holds. Optional trim below. | BlockLineHarness |
| fluid/FixedBoundarySplitTest | 2 | | KEEP | K3 | D17 gate (seed 33): a void splits islands; edit of shared void replaces both. | EventPathWorld, MTB |
| fluid/FluidCheckpointCodecTest | 10 | | KEEP | K2 | Checkpoint format 12: round trip, field refusals, basis change, level height, crystals by record. | CheckpointImages, PCTC |
| fluid/FluidCheckpointFormatTest | 8 | | KEEP | K2 | Certificate fields validated, older formats refused untouched, certified unit reused, hold save restarts. | CertifyingRig, CheckpointImages |
| fluid/FluidCheckpointStoreTest | 9 | | KEEP | K2 | Real file layout: round trip, crash between pack and core, damaged pack refused, compaction. | CertifyingRig, CheckpointImages |
| fluid/FluidDeviceSpecDomainTest | 3 | | KEEP | K3 | Input box from the package envelope, a refusal not a hold (owner rule 2026-10-16); presets basis. | bundled catalog |
| fluid/FluidPacketCodecTest | 15 | | KEEP | K2 | Wire protocol fluid-10: bounds, axis, payload split, compressor ratio, outlets, crystal record. | none |
| fluid/FluidPropertyReloadGuardTest | 3 | | KEEP | K6 | Reload holds on changed science (viscosity, crystal Gibbs, D23), not on metadata. | PCTC |
| fluid/FluidRuntimeDiagnosticsTest | 1 | | KEEP | K6 | Unsure: counters are the evidence of the no-tick-work gates; only test of enable and pause semantics. | none |
| fluid/FullTankSolidsEventTest | 4 | | KEEP | K1 | Compiled junction carries no boundary stock (no created solids); config-event rebuild keeps integrating. | BlockLineHarness |
| fluid/HoldClassWorldsTest | 40 | | KEEP | K3 | Hold-class gate worlds (E-classes, cold 850/829/778/743/183), owner-decision gates, no duplicates in scope. | EventPathWorld, PCTC, MTB |
| fluid/InlineFilterLineHoldTest | 4 | | KEEP | K3 | Hold F8 in-game line replayed; D18 dead-end pump in no island; filter pocket (seed 61). | EventPathWorld |
| fluid/IslandCertificateTest | 23 | | KEEP | K6 | REST/STEADY entry, horizon, replay within budget and exact conservation; wrong replay corrupts state. | CertifyingRig, FTS |
| fluid/IslandClockTest | 11 | | KEEP | K3 | Online-tick clock: no offline time, fences, retry ladder, ready-tick exactness (AGENTS simulated-time rule). | none |
| fluid/IslandCoordinatorTest | 18 | | KEEP | K3 | Held owner accepts fence (owner 2026-10-15), hold ladder, phase-hold park, barriers keep debt. | FTS |
| fluid/IslandSchedulerTest | 9 | | KEEP | K3 | Idle and certified islands cost nothing per tick (no tick-loop work); deadline heap; fair queue. | FTS |
| fluid/JunctionPhaseAppearanceTest | 1 | | KEEP | K3 | Hold class E3 world (seed 30): nitrogen into water-seeded junction gets a vapour phase. | EventPathWorld, MTB |
| fluid/JunctionSeedingGateTest | 7 | | KEEP | K3 | Junction-seeding gates (owner 2026-10-19), bundled 145 buffer sizing, D46 gate (bundled 188). | EventPathWorld, PCTC |
| fluid/LiquidCo2LetDownTest | 2 | | KEEP | K3 | Owner test-world row D23-1: dry ice at 184.8 K through 4 mm; flood above triple point at 5 cm. | FTS |
| fluid/LiquidFullTankOutletsTest | 1 | | KEEP | K3 | Defect F2 and trace-flush decision 2026-09-27: liquid-full tank overflows steadily, feed = overflow. | FTS |
| fluid/LiquidJunctionTransientTest | 2 | | KEEP | K4 | Prints the LIQUID_JUNCTION reference lines the lane compares; D46 ledger closes at roundoff. | JunctionLedger, FTS |
| fluid/LoneBoundaryJoinTest | 1 | | KEEP | K2 | D32: lone generator/void joined then save, reload, save: no island without a device (save crash). | EventPathWorld |
| fluid/McpGameplayRegressionTest | 3 | | KEEP | K4 | Archived gameplay captures replayed against refined solves; sonic case conserves components and energy. | FTS, MTB, fluid/mcp-*.json |
| fluid/MixedGasJunctionStaticTest | 1 | | KEEP | K1 | 32-case junction matrix: every case converges and closes its component balance. | FTS, MTB |
| fluid/MixedGasJunctionTransientTest | 2 | | KEEP | K4 | MIXED_GAS lines and MIXED_GAS_COST reference (AGENTS lean rule); D46 ledger per interval. | JunctionLedger, FTS |
| fluid/PhysicalFluidTopologyTest | 15 | | KEEP | K3 | D20 only pipes connect, tank faces to ports (D9), OD4 actuator bore, compiled stack conserves. | EventPathWorld, FTS |
| fluid/PhysicalRegistryTest | 11 | | KEEP | K3 | Joined-neighbour fencing (owner 2026-10-15), batch apply, held network edit, junctions not booked (D46). | EventPathWorld |
| fluid/PipePresentationTest | 3 | | KEEP | K6 | Pipe panel: throughput not double-counted, velocity and pressure-drop numbers. Junction balance part overlaps MixedGasJunctionStaticTest. | FTS, MTB |
| fluid/PresentationBucketTest | 10 | | KEEP | K3 | Engine-owned presentation: cached replay, one payload per bucket, replies next bucket, science unchanged by viewers. | CertifyingRig, FTS |
| fluid/PropertyReloadCoordinatorTest | 1 | | KEEP | K6 | Property suspension keeps clocks, fences, inventory; held status round-trips; resume keeps 5 s span. | FTS |
| fluid/PumpBottomPortDrainTest | 3 | | KEEP | K3 | Defect F1 and A37: pump drains tank then refused; pumped line through its trace lifetime. | FTS |
| fluid/PumpCavitationTest | 4 | | KEEP | K3 | D59 (4) and t38 decision: suction at vapour or bubble pressure; seed 889 arrival stays liquid. | EventPathWorld, PCTC, MTB |
| fluid/QueuedRemovalWalkTest | 2 | | KEEP | K3 | D27 gate (hold class X): an event beside a queued removal is fenced on the islands it changes. | EventPathWorld, MTB |
| fluid/RestGaugeTest | 2 | | KEEP | K3 | D58 A3 gate (seeds 619, 354): nothing can move, island rests before any solve. | EventPathWorld, MTB |
| fluid/RestIslandTest | 4 | | KEEP | K3 | D19, D22/D54: at-rest island never solved, states kept exactly; placed tank fills to source. | EventPathWorld, MTB |
| fluid/RestResolutionWorldTest | 3 | | KEEP | K3 | D42/D43: tie, zero-flow loop and card E9 certify STEADY at the 0.1 Pa gauge. | EventPathWorld, MTB |
| fluid/RetainedSolverTest | 2 | | KEEP | K5 | Carried handle solves bit-identically to a fresh one; exclusive handle released after failures. | FTS |
| fluid/SealedDischargeTest | 2 | | KEEP | K3 | D58 A4 and D59 (4) gates (seeds 358, 826): pump into its generator dead-heads. | EventPathWorld, MTB |
| fluid/SlurryTankPumpTest | 3 | | KEEP | K3 | Owner fix A and D40 A: pump under LNG slurry tank draws 2.2 kg/s; deposition closes and reopens. | FTS |
| fluid/SolidRuntimeTest | 8 | | KEEP | K2 | Filter cake and solids round-trip codec, capacity change loses no stock, subnormal mass, status text. | FTS |
| fluid/TankOutletsTest | 2 | | KEEP | K6 | Tank outlet lines: per-port draw, faces, net flows and level head (D9, D11) shown to players. | FTS |
| fluid/TracePhaseFoldTest | 3 | | KEEP | K3 | D33/D34/D41 fold rules with nothing leaving the node; E5/E4 worlds (seeds 156, 762). | EventPathWorld, MTB |
| fluid/TraceFlushLedgerTest | 2 | | KEEP | K1 | Trace flush booked exactly as a boundary transfer, no energy, interval not certified. | FTS |
| fluid/VesselPhaseSeedTest | 2 | | KEEP | K3 | D40 A gates H1/H3: vessel seed from own layout; methane pool and slush tank keep up. | FTS |
| fluid/VesselStateFollowsContentsTest | 1 | | KEEP | K3 | D36 G2: every tank stated at its contents within closure after every interval, never above P_max. | ExtremeTopologyIslandTest constants, FTS |
| fluid/VirtualJunctionHoldupTest | 1 | | KEEP | K1 | D46: tank ledger plus junction term closes at roundoff; term bounded by m_J and enthalpy span. | JunctionLedger, FTS |
| fluid/VoidColumnTest | 1 | | KEEP | K3 | D59 (5) gate (world defaults 244): water above an outlet falls into the void. | EventPathWorld, MTB |
| fluid/WorkerAllocationTest | 3 | | KEEP | K6 | Automatic worker sizing, burst hold, never zero workers, invalid clocks refused. | none |
| fluid/WorkerTrajectoryEquivalenceTest | 1 | | KEEP | K5 | 1, 2 and 12 workers (and unissued certificates) give bit-identical trajectories, ledger closed. | FTS |
| fluid/WorldTopologyLedgerTest | 8 | | KEEP | K2 | Topology unit round trip, basis refusal, event ordering; constructed = destroyed incl. dry ice (D23). | PCTC, FTS |
| fluid/BlockLineHarness (helper) | – | | KEEP | – | Used by DeadHeadedLine, ElevatedBlockLine, FilterBlockLine, FullTankSolidsEvent (all KEEP). | FTS |
| fluid/CertifyingRig (helper, incl. Islands) | – | | KEEP | – | Used by CrystalIslandRuntime, FluidCheckpointFormat, FluidCheckpointStore, IslandCertificate, PresentationBucket. | FTS |
| fluid/CheckpointImages (helper) | – | | KEEP | – | Used by FluidCheckpointCodec, FluidCheckpointFormat, FluidCheckpointStore. | none |
| fluid/EventPathWorld (helper) | – | | KEEP | – | Event-path rig of 20 KEEP classes (all hold-class gate worlds). | FTS |
| fluid/JunctionLedger (helper) | – | | KEEP | – | D46 ledger of LiquidJunctionTransient, MixedGasJunctionTransient, VirtualJunctionHoldup. | none |
| resources fluid/mcp-held-drain-checkpoint.json | – | | KEEP | – | Read by McpGameplayRegressionTest (sonic refusal capture). | – |
| resources fluid/mcp-clamped-liquid-full-checkpoint.json | – | | KEEP | – | Read by McpGameplayRegressionTest (clamped drain capture). | – |
| resources fluid/mcp-held-drain-checkpoint.md | – | | KEEP | – | Provenance note of the two captures, named in the test's comment. | – |

Not in this scope: `fluid/regression/chain-100.json` (fluid/benchmark) and `fluid/reference/*` (science tests).

## TRIM rows

None. No class has most of its methods A2/A3.

## Below the TRIM threshold (optional, owner's call)

- FilterBlockLineIslandTest (2 of 11 methods are A2):
  - `filterFreeLineToATankKeepsIntegratingOnceTheTankIsFull`. Kept test: `filterLineToATankKeepsIntegratingOnceTheTankIsFull`, whose control already runs clearLine(400 kPa) for 40 intervals.
  - `aPassiveLineAtThePumpsShutoffPressureFillsTheSameTank`. Kept tests: `aFilterBlockCostsAPumpedLineNoIntervalOfItsOwn` (clearLine 601325) and `filterLineToATankKeepsIntegratingOnceTheTankIsFull` (clearLine 300 kPa).
- ExtremeTopologyIslandTest: `theCasesAtAFifthOfTheSpreadIntegrate` (3 runs) is A1. It is the mechanism evidence of a fixed defect, and its reduced spread is weaker than the kept `theFullCasesIntegrateAtTheDefaultCap`.
- PhysicalRegistryTest `placementCostIsFlatInWorldSize`: the counter assertion (6 compiled devices at every size) is the guard. The two wall-time medians (< 1 ms, ratio <= 4) are A4 and can flake under load. This needs an edit, not an archive.

## Summary

1. Verdicts: 71 test classes KEEP, 0 ARCHIVE, 0 TRIM; 5 helpers KEEP; 3 resources KEEP.
2. Tests kept: 347 methods (353 runs with parameters). Tests archived: 0. Seconds: not measured (no build/test-review/xml).
3. Most fluid classes are owner-decision or hold-class gate worlds (K3) or format tests (K2). No duplicate covers a whole class.
4. Optional, below the threshold: 3 methods (2 A2 in FilterBlockLineIslandTest, 1 A1 in ExtremeTopologyIslandTest), plus the wall-time asserts in `PhysicalRegistryTest.placementCostIsFlatInWorldSize` (A4).
5. Unsure: FluidRuntimeDiagnosticsTest (measurement infrastructure, no game effect; kept because the no-tick-work gates read its counters), and CadenceTrajectoryQualificationTest and WorkerAllocationTest (K6 judged conservatively).
