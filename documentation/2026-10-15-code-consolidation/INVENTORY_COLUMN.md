# Column (V3) consolidation inventory (read-only, opus agent, 2026-10-15, base 009cdf5c)

Reflection: tests call private codec methods by name: ColumnV3Network and ColumnCalculatorV3BlockEntity `writeInput`/`readInput`/`writeDisplayResult`/`readDisplayResult`/`writeState`/`readState`/`freshInput`, plus `maximumBipartiteMatching`, `normalizedNegativeGradient`, `rachfordRiceRoot` and the `STREAM_CODEC` field. GameTests use BE `tryBegin`/`state`/`tryLoadPreset`/`DATA_VERSION`, V3Status CALCULATING/SUCCESS/DIRTY, `ProcessSolveServices.submitV3Column`, `V3PengRobinsonThermo.fromRegisteredPackage`. Wire/NBT: V3ControlledQuantity and V3Status are serialized by name string (BE:516, Network:693, Network:561/594). The only ordinal is `pumparound.split().ordinal()` (Network:663). build.gradle runs only `HollandExample32BenchmarkTest` by name.

## 1. DEAD-PROD ((a) unused anywhere, (b) used only by tests)

| id | kind | file(s) | evidence | action | lines |
|---|---|---|---|---|---|
| C1 | (b) class | client/.../V3ResultProvenance + V3ResultProvenanceTest | 0 main hits; Javadoc describes a retained-result model that no longer exists | delete both | 101 |
| C2 | (a) method | V3ColumnCalculator.calculateOnSuppliedProfile (L661-705) | 0 hits | delete | 45 |
| C3 | (a) members | `V3WetTraySet.asSolved` L107; `V3ColumnTopology.hasVaporLiquidEquilibriumEquation` L72; `V3CondenserComponentPhases.hasVaporLiquidEquilibrium` L32; `V3TruncationSupport.componentFloorMolPerSecond` L343; `V3TrayHydraulics.diameterMetres()` L70; `V3EditorDraft.columnFields` L108; `V3DryMeshCoordinateMap.sum` L152; `V3ColumnInitializer.MAXIMUM_COLD_TRAFFIC_SWEEPS` L27; private overloads `continuationPressureSteps(double)` L3208, `continuationPressurePath(double)` L3225; `V3AcceptanceAuditor.independentWaterSlipCoefficient()` L882; IndependentHollandMeshOracle `vaporDistillate`/`sideDraw`/`bottoms`/`emergentRefluxToVaporDistillate` L105-123; `HollandExample32Data.fahrenheitFromKelvin` L131; `Correction.PROMOTED_2026_09_14` | declaration only | delete | ~60 |
| C4 | (a) BE members | ColumnCalculatorV3BlockEntity `activeOperation(long,long)` L237, `statusCode()` L233, `V3Status.code` field/ctor arg | 0 callers | delete | ~15 |
| C5 | (b) framework | V3SequentialPreconditioner (78), V3BubblePointPreconditioner (45) | production uses only `Result.Prepared → state` (Calculator:1540-1553); `Id.SUM_RATES`, `NotApplicable`, `Failure`, `Evidence` read only by test-tree V3SumRatesPreconditioner/V3HybridPreconditioner | one private calculator method: checkpoint → `V3ColumnInitializer.projectMaterialBalancesAtFixedTemperature` → catch (V3ThermoException \| IAE) → previous state; delete the 2 files; also remove test-only `V3ColumnInitializer.solveMaterialBalances` (L196) and 3-arg `phaseFlowRatios` (L231); adapt V3BubblePointPreconditionerTest, StageContinuationTest:412, SimultaneousColumnSolverTest:107, ExactWarmStartSweepTest:107 | ~140 |
| C6 | (b) overloads | V3ColumnCalculator `calculate` 1/2/3(cutoff)/4/6 args, `calculateWithNeuralTrace`×2 and observer forwarding in NeuralProgress (L843-911) | only main caller ProcessSolveServices:547 (7-arg); 5-arg internal. Test calls: 1-arg 44, 2-arg 28, 3-arg 13, 4-arg 6, 6-arg 25; NeuralTrace 1 (NeuralInitializationTest:132) | keep 7-arg and 5-arg; one test helper; keep `calculateWithAcceptedProfile` (UNSURE) | ~90 |
| C7 | (b) test oracle in prod | `V3BlockJacobianAssembler.assemble` (L16-60) + `block`/`maximumAbsolute`; `V3FiniteDifferenceJacobian.evaluate` 4/5-arg, `evaluateStageColored`, `Jacobian.storedValueCount` | main uses only `assembleLocal`, `evaluateCompact`, `evaluate`→6-arg | move to test tree | ~110 |
| C8 | (b) absent-field defaults | V3ColumnDisplayResult 8/9/10/11-arg ctors (L26-66); V3ConvergenceEvidence 5-arg (L41); V3ColumnResult.accepted 4/6/7(thermo)/8 args (L75-107); ProcessSolveServices.V3ColumnCommand 2/3/5/6-arg (L497-515); V3InitializationOptions DEFAULT/CURRENT and 4/6-arg ctors | main uses only canonical forms (DisplayResult 12, Result 9 and 7-mw, Command 7, Options 5 and 7) | delete; tests pass explicit values | ~130 |
| C9 | (b) V3ColumnInput shorthand ctors | 11/13/14-arg (12-arg used only by V3HollandExample32) | tests: 11-arg 40 files, 12-arg 12, 13-arg 9, 14-arg 21 | test builder (with D8) | ~45 |
| C10 | (b) overloads | Solver `solve` 7/9-arg, `solveWithContinuationLocalBlocks` 8-arg, `toBandedMatrix(2)`, `RungBudget.withoutVerificationCascade`; `Resolver.withTruncation(2)` (24 test calls); `Ledger.create(3)`; `NormalEquations.dampedMatrix(2)`; `Auditor.audit(2)` (33 test calls); `condenserEnergyBalance(5)`; `FactorizedNeuralFeatures.decode(5)`; `AnchorTransformer.read(1)`/`read(2)`; `WaterProperties.dVaporizationEnthalpyDT(1)`; `rampRungBudget(2)` | no main caller at these arities | delete; tests pass the default explicitly | ~120 |
| C11 | (b) fixtures in prod | BE `literatureCduInput`/`methaneCduInput`/`LITERATURE_PACKAGE` (L54, L429-437) | 0 main callers; 12 test files | move to test fixture | 12 |
| C12 | keep | small test-only accessors that test real behaviour | – | keep | – |

## 2. DEAD-CHECK

| id | kind | file(s) | evidence | action |
|---|---|---|---|---|
| K1 | OWNER | `Recovery.RAMP_HANDOFF` (Calc ~L700-780, NeuralProgress terminal capture, `NEURAL_RAMP_HANDOFF_BUDGET_MILLIS`, `recoveryBudgetMilliseconds`) | config uses 5-arg ctor → Recovery.NONE (Options:114-116); only V3LearnedRecoveryTest selects it | owner decision |
| K2 | OWNER | `DecodeOptions.lift` (FactorizedNeuralFeatures L55-115, ~L200-215) | shipped decode `zeroPhaseFloor(10)` (TransformerArtifact:48); V3DecoderPresenceFloorTest 4 of 5 tests pin lift | owner decision |
| K3 | enum never produced | `CandidateRule.DECODE_VARIANTS` + variant branch of `V3AnchorTransformerInitializer.candidates` (L42, L156-170) | artifact passes SINGLE (TransformerArtifact:49); 0 refs | delete enum, field, override |
| K4 | parameter always one value | `V3ColumnInitializer.Mode` through `calculate`/`calculateInCatalog`/`calculateBranch`; branches Calc:1049-1051, Calc:1269-1273; `Mode.PHASE_AWARE_TRAFFIC` + `phaseAwareTrafficSeed` (Init L32-33, L114-116, L280-312) | every caller passes SEQUENTIAL_MATERIAL_VLE (Calc:181/192/1003-1009) | drop parameter, branches, enum value, seed method |
| K5 | values never produced | V3SolverFailureCode DOF_MISMATCH, PHASE_REGIME_MISMATCH, CANCELLED, DEADLINE_EXCEEDED; V3Status.IDLE + lang `gui.createcheme.column.status.idle` | `failureCode()` (Calc:3408) maps to 3 codes; CANCELLED/DEADLINE come from BoundedCpuSolveService.TerminalStatus (Network:317-323) | delete; ColumnInputTest:79 uses DOF_MISMATCH as an arbitrary code, swap it |
| K6 | re-validation | V3ColumnProblem ctor (L55-74) profile length/finite, ledger-matches-problem, `requireCompatible`; `Resolver.requireProfile` topology check (L210-216) | upstream: Resolver:37 → `validateInput` → `requireProfileShape` (L187-197); V3ColumnInput:97-101; ledger built from the same arguments (Resolver:51, :90); `requireCompatible` Ledger:92 and Resolver:78; only TruncationStructureTest:152 constructs the problem directly | one check per invariant, in the resolver |
| K7 | duplicate | `V3SequentialPreconditioner.Request` seed-shape check (L21-28) vs V3ColumnInitializer:147-150 | – | goes with C5 |
| K8 | duplicate | Network `validateResolvedInput` schema check (L251) vs `Resolver.validateInput` (L111) called at L267; Holland inputs validated twice (HollandExample32:68, Network:267) | – | keep one |
| K9 | redundant mapping | `V3Status.serializedName` = `name()` for all 6; `fromSerializedName` | wire bytes identical | `name()`/`valueOf` |
| K10 | trivial duplicates | `calculateBranch` requireNonNull (Calc:1205-1207) repeats Calc:1043-1045; `V3SteamFeeds.upwardVaporProfile` length check | – | delete |
| K11 | OWNER | `Correction.WALLS` branches in `neuralRungBudget`/`neuralExtension` | production uses PROGRESS (CreateChemE:227) | owner decision |

## 3. DEAD-TEST

| id | file(s) | evidence | action |
|---|---|---|---|
| D1 | V3ColdCoreBenchmarkWorker (615, `main`) + V3ColdCoreBenchmarkWorkerTest (33) + src/test/resources/v3-benchmarks/v3-cold-core-v1.json | launched by the Python supervisor in tools/v3-cold-core-benchmark; no Gradle task | detach with re-attach patch |
| D2 | V3SumRatesPreconditioner (234) + Test (63), V3HybridPreconditioner (88) + Test (26) | test-only code | delete with C5 |
| D3 | V3ThirtyStageColdStartTest | asserts its own input and "not INTERNAL_ERROR" | delete |
| D4 | V3RealCrudeColdSweepTest | asserts not-INTERNAL_ERROR and case count; slow | merge into the cold-start test (M9) |
| D5 | OWNER: linalg/V3BandedSolverBitIdentityTest + V3TreeMapBandedSolverReference | frozen old implementation | owner decision |
| D6 | OWNER: thermo/V3CatalogParityTest, V3Tjl19PropertyPackage, V3Tjl20MethanePropertyPackage | Java→JSON parity | owner decision |
| D7 | TruncationStructureTest:149-154 | depends on K6 | test through the resolver |
| D8 | 42 test files define their own V3ColumnInput builders (25 named `input`); 10 rebuild the cdu17 binary (feed[6]=feed[13]=50, 550 K, 250 kPa); 4 codec tests copy reflective `invoke`/`writeWire`/`readWire`/`writeNbt`/`readNbt`/`result`/`ledger` | grep counts | one `V3TestInputs` + one codec-reflection helper |
| D9 | V3Cdu17TiaJuanaPackage STANDARD_PRESSURE/TEMPERATURE, ACS_BULK_DENSITY | declaration only | delete |

## 4. MERGE (codecs keep string names and record component names)

| id | change | files gone |
|---|---|---|
| M1 | V3SideDraws→V3SideDrawSpec, V3SteamFeeds→V3SteamFeedSpec, V3Pumparounds→V3PumparoundSpec (package-private static helpers) | 3 |
| M2 | V3ControlledQuantity → nested enum in V3ColumnSpecification (serialized by `name()`) | 1 |
| M3 | V3CalculatedQuantity: delete with the ledger field, ctor arg and accessor (only LedgerTest:50 reads it) | 1 |
| M4 | V3ContractDiagnostic → V3DegreeOfFreedomLedger; V3WaterCondenserRegime → V3ColumnProblem | 2 |
| M5 | V3NeuralModels → `V3NeuralRegistry.bundled()`; V3TransformerArtifact → V3NeuralRegistry; V3NativeAnchor → V3AnchorTransformerInitializer | 3 |
| M6 | V3FlashPhaseSupport → V3TruncatedFlash; V3PropertyPackageRegistry → static on V3CatalogPropertyPackage; V3MaterialInputs → BE (MaterialCatalogTest:119 updated) | 3 |
| M7 | V3NewtonTrace → V3SimultaneousColumnSolver | 1 |
| M8 | client drafts V3SideDrawDraft, V3SteamFeedDraft, V3PumparoundDraft, V3ColumnScalarDraft, V3ColumnInputDraft, V3CompositionDraft, V3DiagramLayout → nested in their single user | 6 |
| M9 | ColdStartSweep, RealCrudeColdSweep, LowPressureColdProbe, NearbyInputRecovery (+D3) → one V3ColdStartTest | 4 |
| M10 | SideDrawsTest→SideDrawContractTest; VerificationAcceptanceTest→SimultaneousColumnSolverTest; SampleCrudeInitializerTest→ColumnInitializerTest; RequestedRecoveryTest→LearnedRecoveryTest; BlockJacobianOwnershipTest→BlockJacobianAssemblerTest; DryMeshStateOwnershipTest→DryMeshCoordinateMapTest; CondenserPhaseContinuationTest→CondenserPhaseTransitionTest; TruncationAuditTest→TruncationSupportTest; BandedWorkspaceTest→BandedPivotedSolverTest; FeedFlashTest→FeedFlashEquivalenceTest; PengRobinsonRootPrecisionTest→PengRobinsonThermoTest | 11 |
| M11 | nest single-use fixtures: V3WaterPhaseQualification (CHECK: tools/neural uses it, keep top-level if so), V3Sotelo2019SideDrawCase, thermo/V3FeedFlashReference, oracle/IndependentIdealMeshOracle | 4 |
| M12 | SideDraw/Pumparound/Closure/TrayHydraulics codec tests → one ColumnV3NetworkCodecTest | 3 |
| M13 | client tests SideDrawDraftTest, SteamFeedDraftTest, DiagramLayoutTest, ColumnScalarDraftTest → V3ColumnInputDraftTest | 4 |
| M14 | OWNER/optional: V3SolverFailureCode → V3ColumnOutcome; V3TrayHydraulicsSummary → V3TrayHydraulics.Summary | 2 |
