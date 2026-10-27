# Fluid and shared consolidation inventory (read-only, opus agents, 2026-10-15, base 009cdf5c)

Paths: RF = `src/main/java/com/wormzjl/createcheme/runtime/fluid`, SF = `.../science/fluid`, TR = `src/test/java/com/wormzjl/createcheme/runtime/fluid`, TS = `src/test/.../science/fluid`, GT = `src/fluidGameTest/java/com/wormzjl/createcheme/fluid/gametest`, FCC = FluidCheckpointCodec, FWA = FluidWorldAuthority, IC = IslandCoordinator, PSS = runtime/ProcessSolveServices, M = `src/main/java/com/wormzjl/createcheme/`, T = `src/test/java/com/wormzjl/createcheme/`.

Constraints: Gson keys are field names and the checkpoint binary uses tags, so nesting changes no saved name. `ProcessSolveServices$RequestContext` keeps its name (`Class.forName` at GT/FluidContentionProbe.java:35). FlowControl.Mode untouched. `tools/development.gradle` compiles `tools/neural` against the test output: test helpers used there count as used.

Not dead (checked): `createcheme.fluid.scheduler.verify` (build.gradle 98, 286, 387; read IC:136, FCC:68); approximate fallback reachable (IC:507); restDetection, adaptiveCadence live; every benchmark property read (GT FluidServerBenchmark:66-76); UNBOUND live (FWA:310, :414); every FlowControl.Mode, SolidMobility.Reason, NodeKind, PhasePort value produced; all 62 SolverDiagnostics counters incremented; observation hooks retainsSolver, waitsForInputs, PhysicalRegistry.cake, subscribed, packLimit, stationaryRefusal; GameTest-used diagnosticSnapshots, pendingCount, withCertificates, FluidCheckpointStore.reopen, FluidMaterialCatalog.nitrogenIndex/waterIndex, lastView. Every JUnit test runs in some task. fluid/support and material helpers have no unused public member. Keep ScheduledTransfer (+tests; MixedGasJunctionTransientTest:85), VirtualJunctionHoldupTest, McpGameplayRegressionTest.

## Fluid: DEAD-PROD

| id | kind | evidence | action |
|---|---|---|---|
| F1 | OWNER: causal modules RF CausalModuleCoordinator, FixedSplitModule, BufferedTransfers, ModuleTransferPlanner, MaterialParcel, PendingTransfers, BufferBackpressure + module parts of FWA, IC, PSS (BufferedIslandCommand), FCC Ledger | host built only at FWA:68 from a loaded checkpoint with modules; module checkpoints written only by FluidServerBenchmark profile "module" (:396-401), FluidModuleGameTests, FluidPropertyReloadGameTests:125-176 | owner decision (format 8, G5 33→31) |
| F2 | OWNER: SF/thermo TranslatedPengRobinson Derivatives/newDerivatives/differentiate (:168-287); FluidThermodynamics:676-691; HydrocarbonModel:72-89 | `newHydrocarbonDerivatives`/`hydrocarbonDerivatives(` declaration only | owner decision |
| F3 | legacy format-3 reader FCC:842-870 `decodeGraph` + private Graph/Node/Pipe/Phase/Control | callers TR/FluidCheckpointCodecTest:203-219, McpGameplayRegressionTest:33 | move to test sources as a McpGameplay fixture loader; delete codec test :196-221 |
| F4 | FCC:772-814 reseal, row, ledgerJson, withLedger, unitBytes, withIsland, withUnit ("for tests") | main callers only these helpers | move to a package-private test helper in TR |
| F5 | no caller: SF NetworkReduction.directions FORWARD/REVERSE :99-116; RF MinecraftFluidRuntime.repartition + 2 single-event topology overloads :59-72; SF ConservativeTransport 6-arg Projection, 8- and 11-arg reconstruct :91-122; PhaseLayout 9-arg residual, 8-arg balanceRows :322-347; InventoryEquilibrium 4-arg close :84; PassiveStepSolver.startFlows :3747; IslandCertificate.intervalTicks :453; FluidPresentation.WAITING :34; CreateChemE.FluidOptions 8-/9-arg ctors :209-210; PSS.Config 5-arg; SlurryTransport.DEFAULT_SUSPENSION_MULTIPLIER, DEFAULT_IMMOBILE_VISCOSITY :11,13; FluidDeviceMenu.lastDelivered/showingLastDelivered :29,35,49,57 | declaration or Javadoc only | delete |
| F6 | SF TopologyCompiler PipeView, Compiled.views, Segment.start/endElevation :18-23,54 | read only at TS/topology/TopologyCompilerTest:15-16 | delete; drop 2 asserts |
| F7 | RF/FluidView hydraulicOwner, intervalQuality (:12, :70) | written FWA:269, FWA:317; 0 readers | delete; protocol fluid-8 → fluid-9 |
| F8 | test-only overloads: WorldTopologyLedger applyFirst, readyEvents, applyReady :160-192; IslandClock 1-arg ctor, DETACHED, fresh, accrueOnlineTicks, debtTicks; IC repartition + single-event topology; BoundedCpuSolveService 1-/2-arg ctors + Config.defaults; FallbackAllowance.fullRecovery; FluidNetwork.MenuData.liveData() :93; SolidMobility 5-arg check; FluidThermodynamics 3 short ctors (PSS V3ColumnCommand overloads belong to thread A) | src/test only | rewrite tests on production entry points (`applyBatch`, `footprints`, epoch ctor), then delete |

## Fluid: DEAD-CHECK

| id | evidence | action |
|---|---|---|
| C1 | SF InventoryEquilibrium.solve `forceProperties` :22-35; both callers pass false (:16, ConservativeTransport:543) | drop parameter |
| C2 | SF PassiveStepSolver.reachableComponents `directions` :1047-1067; only call :458 passes null | drop parameter and 2-arg overload |
| C3 | SF SolidEventIntegrator:323 repeats :320; Transition.direction always BOTH_DIRECTIONS | delete |
| C4 | FluidNetwork.deliver:181 vs FWA.open:393-395; FWA.edit:159 vs FluidNetwork.edit:219 and WorldTopologyLedger.queue:154; MenuData:75,78 vs LiveData ctor :108; FluidDeviceBlockEntity.acceptFluidView:51 vs FWA:419; FluidDeviceSpec:50 vs validate:38; PipePresentation.inspect:68; FWA.place:148 vs Overlay.check:264 | delete the downstream copy; keep the first validation of disk/packet data |
| C5 | PassiveStepSolver:1406,1541 `catch ApproximationRejected` (already caught :372); dt checks PassiveStepSolver:383 and ConservativeTransport:238; finite-flow loop ConservativeTransport:239 = :283; basis checks InventoryEquilibrium:30 and PhaseLayout:91 vs Reservoir:136 | delete inner copies |
| C6 | owner-thread checks inside IslandClock (18), FairIslandQueue (8), IslandScheduler (7), reached only through IC methods that call owned() | drop inner copies (after M10) |
| C7 | GlobalLiquidResponse logFugacity :46-59 vs logFugacityInto :61-73; HydrocarbonModel preamble :96-102 vs :117-127; PipeSpeedWindow.volume :48 == PipePresentation.volume :47 | reuse one copy, same operation order |
| C8 | solid defaults stated 4 times: CreateChemE:87-91, SolidTransportSettings.defaults, SlurryTransport.DEFAULT_*, InlineFilter.empty | one source |
| C9 | TS/network/SolidClosureFeasibilityTest:20 `@Tag("solid-feasibility")`, no tag filters | delete |

## Fluid: DEAD-TEST

| id | evidence | action |
|---|---|---|
| T1 | `CertifyingRig` FluidCheckpointFormatTest:29-133 ≡ FluidCheckpointStoreTest:31-128 (+ IslandCertificateTest variant); EventPathWorld re-implemented in InlineFilterLineHoldTest:43-130, PhysicalRegistryTest:36-100; dispatcher `submit`/`deadlineNanos`/`isCancellationRequested` + NEVER token in 9-11 files; block-line `device`/`island`/`run`/`shape` harness in 4 files (DeadHeaded, ElevatedBlock, FilterBlock, FullTankSolids); placed-line slice loop in 7 files; `water()`/`nitrogen()` in 8 files; science port helpers `owned`/`mass`/`index`/`nitrogenAt`/`waterSupply`/`line` in 4-7 files; ~30 files build the model from a literal instead of `FluidTestSupport.networkModel()` | extend EventPathWorld and FluidTestSupport; add CertifyingRig and BlockLineHarness |
| T2 | TS/ConservationHarnessTest (ReservoirFixture, ConservationAssertions self-test), TR/RuntimeHarnessTest (ManualNanoClock, uniqueDeliveries/DeliveryKey); helpers have no other user | delete tests and the 4 helpers |
| T3 | fluid/benchmark island fixtures quiet-11312, quiet-11324, cold-11312 (FluidSolverRegressionTest:82-87), SolverRegressionHarness snapshot/islands, build.gradle:375-376 `fluidRegressionSnapshot`, `assumeFalse` at FluidSolverRegressionTest:47, `FLUID_REGRESSION_SNAPSHOT` env alias (SolverRegressionHarness:100) | references deleted in b5ed4079; nothing writes build/probe/core.dat | delete (chain-100 unchanged) |
| T4 | TS/thermo LiquidCompressionQualificationTest (+3 `n-*-300K.tsv`; superseded gate, asserts a test-only accessor); EmptyWaterFillingAuditTest (pre-D54 audit, = S23); network/PassiveTimeRefinementTest (second solve :29 repeats :20; covered by BackwardEulerStepTest, TransientQualificationTest); FluidPropertyCoverageTest:74 per-package models production never builds | delete |
| T5 | LiquidFullTankOutletsTest vs TraceFlushLiquidFullTankTest (same 9-device world); PhasePortTest:285 vs PhasePortPriorityTest:334 `steps()`; Format vs Store round trips (~60 lines); PipeResistanceTest ⊂ HydraulicReferenceQualificationTest; ElevatedBlockLineIslandTest dead-headed half repeats DeadHeadedLineIslandTest; RestIslandTest second loop pass same world (reservoir ignores spec pressure, FluidDeviceSpec:54) | merge, union of asserts |
| T6 | ~10 science tests write `build/reports/fluid/*.json` (HRQ, HMQ, TQT, FPCT, SharedSourceDepletion, FluidPropertyCoverageTest, LiquidCompressionQualificationTest) with 0 readers | drop writes, keep asserts |
| T7 | OWNER: 16 TSVs in src/test/resources/fluid/reference (steam-*, nitrogen-*), examples/ Python scripts | owner decision |

## Fluid: MERGE

| id | change | files gone |
|---|---|---|
| M1 | FluidRuntimeDiagnostics ← FluidRuntimeMeter | 1 |
| M2 | FluidPropertyReloadGuard ← SolidCompatibility.validate | 1 |
| M3 | FallbackAllowance ← FluidFallbackPolicy (as `Policy`) | 1 |
| M4 | ApproximationAnchor ← ApproximationRejected (as `Rejected`) | 1 |
| M5 | PassiveIntervalSolver ← StageGuard | 1 |
| M6 | SlurryTransport ← SolidTransportSettings (as `Settings`, fixes C8) | 1 |
| M7 | WorldTopologyLedger ← RecoveredSolid, FluidBasis; FluidBasisTest → WorldTopologyLedgerTest | 2 (+1 test) |
| M8 | PipePresentation ← PipeSpeedWindow (one `volume()`) | 1 |
| M9 | ProcessSolveServices ← CompletionWakeup; FCC ← FluidUnitIO | 2 |
| M10 | IslandScheduler ← FairIslandQueue + their tests | 1 (+1 test) |
| M12 | optional: IslandCertificate ← CertificatePolicy; FluidDeviceSpec ← SlurryFeed; SolidInventory ← ParticleSize; PassiveNetwork ← InlineFilter, ScheduledTransfer | 5 |
| M13 | one `HoldClassWorldsTest` ← Actuated/FilterIslandGeneratorClosure, CappedJunction, ChordPressureSlope, ConnectionRowScale, DryEndOutflow, EmptyingVessel, HoldingNothingBooking/Layout, JunctionStartMixture/State, PartitionDraw, PolishedClosure, ResidueTank, StartCheckRateFailure, StepStartFlows, TopologyEventFence, TurbulentWalk, VelocityCapMode, VesselClosureEquilibrium (each place → runTo → assertKeptUp on EventPathWorld) | 18 |
| M14 | IslandCertificateDomainTest→IslandCertificateTest; FluidDeviceSpecTest→FluidDeviceSpecDomainTest; TraceFlushFedPumpTest→PumpBottomPortDrainTest; OnlyPipesConnectTest→PhysicalFluidTopologyTest; FallbackAllowanceTest→FluidFallbackQualificationTest | 5 |
| M15 | HoldsNothingTest→EmptyNetworkTest; PipeResistanceTest, PipeSectionAggregationTest→HRQ; HybridDerivativeTest→HydrocarbonModelTest; JunctionArrivingPhaseTest→JunctionWaterTraceTest; ScheduledTransferEstimatorTest→ScheduledTransferTest; StructuralReuseTest→SparseNewtonTest; BoundaryAndPhaseTest→BackwardEulerStepTest | 8 |

## Shared (S) and build (G)

| id | evidence | action |
|---|---|---|
| S1 | M science/thermo/PengRobinson78 (234), PhaseProperties (46): main uses only HydrocarbonModel:43-54 (`raw.evaluate(...LIQUID).compressibilityFactor()`) and `GAS_CONSTANT` (TranslatedPengRobinson:24); `initialEquilibriumRatios`, `componentCount`, `components` 0 hits; `withoutBinaryInteractions` test-only | (a) delete 3 unused methods; (b) move PR78 + PhaseProperties to test tree (oracle for PengRobinson78Test, V3PengRobinsonKernelTest:25); HydrocarbonModel gets a private pure-component Z copying the PR78 expressions (for pure x, aMix = sqrt(fl(a²)) = a, bMix = b exactly). Do NOT swap in PengRobinsonKernel (different arithmetic order, Newton refinement). Prove with G2 exact |
| S2 | column, thread A: dead server preset packet (ColumnV3Network:88,111-118,176-207,419-460,577-581,605-608; `V3State.presets`; T network/ColumnPresetCodecTest) | thread A |
| S3 | = C1 | thread A |
| S5 | DeterministicMath:58-97 sin, tan, asin, atan, atan2, sinh, cosh, tanh, hypot: 0 hits in src or tools (keep `strictPow`: tools/jdk-determinism, DeterministicMathTest) | delete |
| S6 | MaterialCatalog:135-142 `columnSideDrawRates`, `columnFeedStandardVolume`, `packagePreset`, :178-183 `propertyPhysicsFingerprint`; MaterialRuntime:15-17 `viscosity`, :35-38 `isBundledScience`; ViscosityCorrelation:81 `kinematicViscosity…`: test-only | move to test helpers or inline (skip if users are in column tests) |
| S7 | ViscosityCorrelation:16-21 11-arg ctor unused | delete |
| S8 | client/world members (thread A except FluidDeviceMenu = F5); ColumnInputPreset crude constants + `KNOWN` map test-only (thread A) | – |
| S9 | PengRobinsonKernel:154 `mixing()` 0 hits; :182-188 `evaluatePair` test-only; TraceTruncationPolicy:18 `MAX_CUTOFF` alias (tests 4) | delete; tests use main APIs |
| S10 | OWNER: FluidAppearance stored, 0 readers of `componentAppearance`/`assayAppearance`/`argb` | owner decision |
| S11 | thread A: 37 unused lang keys: `column.{add_component(.hint),close_search(.hint),no_matches,status.idle}`, `fluid.{add_component(.hint),no_matches,from,to}`, root `column_diameter(_hint)`, `column_presets`, `preset_{choose,conditions,replaces}`, 13 `material.createcheme.cdu17_*` | thread A |
| S13 | MaterialRuntime:45 `componentId.equals("h2o")` legacy alias, no producer | delete |
| S14 | thread A: ProcessSolveCoordinator:76,87 unreachable throws on sealed types | exhaustive switches |
| S15 | TraceTruncationPolicy:34 `of` repeats ctor :30 check | drop |
| S16 | optional ThermoComponent re-validation | skip |
| S17 | V3BandedSolverBitIdentityTest `createcheme.v3.dumpedJacobian` (with D5, owner) | skip |
| S19 | thread A: WIRE_SCHEMA_VERSION==15 pinned twice (V3ClosureCodecTest:59, V3TrayHydraulicsCodecTest:53) | thread A |
| S20 | = D1 | thread A |
| S21 | = D2 | thread A |
| S22 | thread A: 5 codec tests repeat reflection helpers | thread A |
| S23 | = T4 EmptyWaterFillingAuditTest | delete |
| S24 | fluid/thermo: HydrocarbonModelTest pure-liquid identity ⊂ HybridDerivativeTest mixture identity; GlobalLiquidResponseTest; material: AmbientViscosityTest (1 test), CrudeAssayConversionTest (1 test) | merge into HydrocarbonModelTest, ViscosityTest, MaterialPresetsTest |
| S25 | orphan fixtures src/test/resources/materials/legacy-baseline.json (54 KB), data/createcheme/structure/material_empty.nbt: 0 readers | delete |
| S26 | OWNER = T7 | – |
| S27 | = T6 | – |
| S28 | T material/TjlReferenceExtension (15), Cdu17FixtureExtension (29): same interceptor | one base class with a supplier; keep both names |
| S29-S31, S35, S36 | thread A (client drafts, ProcessUi, V3Phase) | thread A |
| S30 | thread A | thread A |
| S32 | M science/thermo/PhaseRoot = PengRobinsonKernel.Root (TranslatedPengRobinson:296 maps one to the other); 3 main + 9 test files | use PengRobinsonKernel.Root |
| S33 | MaterialAuthoring (only caller MaterialCatalog:312), MaterialFluidData, SolidMaterial (→ SolidMaterialCatalog), FluidAppearance | nest |
| S34 | five JSON field readers: MaterialCatalog:563-603, MaterialPresets text/number/integer/bool, MaterialFluidData, LiquidMixtureCorrection:42, MaterialAuthoring:51 | one package-private reader (keep texts tests pin) |
| S37 | optional: registry/Mod{Blocks,Items,Menus,BlockEntities}, material/MaterialReloadListener (15 GameTest files import ModBlocks) | one ModRegistry; reload listener into CreateChemE |
| G1 | `gameTestServer` run (build.gradle:84-87) + `enabledGameTestNamespaces = mod_id` :75, :81, :143; namespace `createcheme` has 0 @GameTestHolder | remove |
| G2 | `runData` (:146-154) + `srcDir 'src/generated/resources'` + 2 excludes (:20-24); no data generation | remove |
| G3 | `withType(Test){dependsOn jar; fluid.modJar}` (:266-271) only PackagedSparseLuTest reads it | limit to `test` and `fluidScienceTest` |
| G4 | stale "Milestone-specific fixtures arrive in M1/M2/M9" comment and task descriptions :290-295 | fix text |
