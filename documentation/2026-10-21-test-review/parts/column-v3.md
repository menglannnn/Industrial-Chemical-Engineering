# Test review part: science/column (V3 column)

Scope: `src/test/java/com/wormzjl/icheme/science/column/` (81 files: 75 test classes, 6 helpers), branch at main 138f4993.
This review applies CRITERIA.md. Context came from `code-guide/column-v3.md` and AGENTS.md. No Gradle, test or Java run was made.
Seconds column: blank, because `build/test-review/xml/` does not exist. Tests are counted from source; a parameterized method counts as one.

| Class | Tests | Seconds | Verdict | Code | Reason (max 20 words; for A2 name the kept test) | Helpers/resources used |
|---|---|---|---|---|---|---|
| v3/ColumnInputPresetTest | 3 | | KEEP | K2 | Preset NBT round trip and catalog override; only guard of preset save format | V3TestInputs |
| v3/CrystalPackageConsumersTest | 4 | | KEEP | K3 | P6 crystal-package refusal in column and loader paths; owner rule gate | Cdu17 catalog |
| v3/HollandExample32BenchmarkTest | 2 | | KEEP | K4 | Holland Ex 3-2 reference; run by the v3HollandBenchmark gate task | V3TestInputs |
| v3/V3ActiveComponentBasisTest | 1 | | KEEP | K6 | Zero-feed component elimination and public index mapping; no other guard | V3TestInputs |
| v3/V3BlockJacobianAssemblerTest | 12 | | KEEP | K5 | Analytic/local blocks vs FD oracle on every row feature; bitwise probe decode identity | V3JacobianOracle, V3TruncationSupportTest.problem, Cdu17FixtureExtension |
| v3/V3BoundedSolvePathTest | 5 | | KEEP | K6 | A path-length bound never fails a solve; bounded-path contract | V3TestInputs |
| v3/V3BundledRegroupedModelTest | 2 | | KEEP | K4 | Independent float64 fixtures and SHA pin of the bundled model; 4th-draw decline | regrouped-predictions.json |
| v3/V3ColdStartTest | 5 | | KEEP | K6 | Typed cold outcomes, total condenser without outlet gas (see method notes) | V3TestInputs, Cdu17 catalog |
| v3/V3ColumnCalculatorTest | 10 | | KEEP | K1 | Calculator facade product balances, digest and outcome contract | V3TestInputs |
| v3/V3ColumnDisplayResultTest | 1 | | KEEP | K2 | Display certificate content sent to the GUI | V3TestInputs |
| v3/V3ColumnFeedAndDrawLimitsTest | 2 | | KEEP | K1 | Nitrogen conserved; fourth-draw limit | V3TestInputs |
| v3/V3ColumnInitializerTest | 9 | | KEEP | K1 | Seed material projection closes balances | V3TestInputs, V3ManufacturedFlash |
| v3/V3ColumnInputTest | 4 | | KEEP | K2 | Input validation, digest and audit contract | V3TestInputs |
| v3/V3CondenserPhaseAuditTest | 8 | | KEEP | K1 | Condenser energy balance and phase audit | V3TestInputs |
| v3/V3CondenserPhaseTransitionTest | 6 | | KEEP | K1 | Condenser branch transitions keep balances | V3TestInputs |
| v3/V3ContinuationSeedTest | 5 | | KEEP | K1 | Continuation cap conserves delivered material | V3TestInputs |
| v3/V3ConvergenceClosureTest | 6 | | KEEP | K6 | Closure range, label and final-step gate; one method pins iteration counts (note) | V3TestInputs |
| v3/V3DecoderPresenceFloorTest | 2 | | KEEP | K6 | Decoder prune and fail-closed options; unsure, may be A2 of V3NeuralInitializationTest | V3TestInputs |
| v3/V3DegreeOfFreedomLedgerTest | 7 | | KEEP | K6 | Equation/unknown ledger square and families | V3TestInputs |
| v3/V3DryMeshCoordinateMapTest | 5 | | KEEP | K6 | Encode/decode coordinate contract | V3TestInputs |
| v3/V3EnergyShiftPredictorTest | 6 | | KEEP | K6 | Energy shift ledger (-41.93 MW) predictor contract | V3TestInputs |
| v3/V3ExactWarmStartSweepTest | 2 | | KEEP | K6 | Exact hot start: zero iterations and same audit; unsure, partly overlaps SimultaneousColumnSolverTest | V3TestInputs, V3SimultaneousColumnSolverTest.projectedOrNull |
| v3/V3FiniteDifferenceJacobianTest | 5 | | KEEP | K5 | Compact vs dense Jacobian bit-identical | V3JacobianOracle |
| v3/V3FlashTruncationColumnTest | 3 | | TRIM | K6/A2 | Keep positive-cutoff observability; two methods duplicate kept tests (list below) | V3TestInputs |
| v3/V3FreeWaterTrayTest | 12 | | KEEP | K3 | Owner decision: below-dew-point tray warns, not rejects; free-water balance | V3TestInputs |
| v3/V3LearnedRecoveryTest | 6 | | KEEP | K6 | Learned recovery admission and authored feature recovery | regrouped-authored-feature-recovery.json |
| v3/V3LiquidSupplyScreenTest | 8 | | KEEP | K6 | Liquid-supply screen (draw-wall root cause) contract | V3TestInputs |
| v3/V3LiteraturePresetTest | 3 | | KEEP | K3 | Dew-point warning owner decision; Ledezma literature reference | V3TestInputs |
| v3/V3MeshResidualEvaluatorTest | 6 | | KEEP | K1 | MESH residual rows close material and energy | V3TestInputs, V3ManufacturedFlash |
| v3/V3MethaneColumnTest | 3 | | KEEP | K1 | Methane conserved; input NBT round trip (writes build/ json, prints timing) | V3TestInputs |
| v3/V3NeuralInitializationTest | 8 | | KEEP | K6 | Neural seed admission, decoder floor, fallback contract | V3TestInputs |
| v3/V3NeuralRegistryTest | 4 | | KEEP | K2 | Registry path and SHA validation, projection, admission snapshot | V3TestInputs |
| v3/V3NewtonMatrixTest | 2 | | KEEP | K6 | Small couplings survive; off-band guard | V3TestInputs |
| v3/V3NormalEquationsTest | 8 | | KEEP | K5 | Damped normal equations deterministic and exact | none |
| v3/V3OperatingDomainValidatorTest | 5 | | KEEP | K3 | PROPERTY_OUT_OF_RANGE admission (extrapolation rule) | V3TestInputs |
| v3/V3PathDependentInfeasibilityTest | 3 | | KEEP | K6 | INFEASIBLE vs NONCONVERGENCE outcome contract | V3TestInputs |
| v3/V3PhaseTruncationTest | 9 | | KEEP | K1 | Phase truncation keeps component balances | V3TestInputs |
| v3/V3PotColumnTest | 1 | | KEEP | K6 | Pot (0/1-tray) columns wet and dry; only guard | V3TestInputs |
| v3/V3PumparoundCalculatorTest | 13 | | KEEP | K1 | Pumparound duty and product balances; two methods are A2/A3 (note) | V3TestInputs |
| v3/V3PumparoundContractTest | 8 | | KEEP | K2 | Heat-free digest pin, duty expansion | V3TestInputs |
| v3/V3PumparoundSteamCalculatorTest | 7 | | KEEP | K1 | Steam plus pumparound energy ledger closure | V3TestInputs |
| v3/V3RegroupedTransformerTest | 5 | | KEEP | K5 | Artifact validation; deterministic shared weights | V3TestInputs |
| v3/V3SideDrawAuditTest | 4 | | KEEP | K1 | SIDE_DRAW_SPLIT audit; truncation defect | V3TestInputs |
| v3/V3SideDrawCalculatorTest | 4 | | KEEP | K1 | Side-draw product balances | V3TestInputs |
| v3/V3SideDrawContractTest | 6 | | KEEP | K2 | MAX_SIDE_DRAWS, legacy digest pin | V3TestInputs |
| v3/V3SimultaneousColumnSolverTest | 8 | | KEEP | K6 | Both acceptance gates, phase gate rejection, budgets, verification acceptance | V3TestInputs, V3ManufacturedFlash, Cdu17 catalog |
| v3/V3Sotelo2019SideDrawCaseTest | 2 | | KEEP | K4 | Sotelo 2019 side-draw reference | V3TestInputs |
| v3/V3StageContinuationTest | 11 | | KEEP | K6 | Stage and steam continuation schedules, resume, stall publication (note) | V3TestInputs |
| v3/V3StageTraceCalculatorTest | 5 | | KEEP | K2 | Zero-cutoff legacy digest/revision; truncated-solve defect | V3TestInputs |
| v3/V3SteamFeedContractTest | 8 | | KEEP | K1 | Steam-table sanity and condenser energy | V3TestInputs |
| v3/V3StructuralMatchingOracleTest | 1 | | KEEP | K6 | Ledger structural rank vs oracle | V3TestInputs |
| v3/V3TraceFloorSupportTest | 2 | | KEEP | K4 | Pinned stalled-state regression (prints, but asserts) | tjl19-...-stalled-state-java21.txt |
| v3/V3TrayHydraulicsColumnTest | 7 | | KEEP | K4 | Review section 4.4 magnitudes; prescribed-drop digest bytes | V3TestInputs |
| v3/V3TrayHydraulicsTest | 10 | | KEEP | K4 | Tray pressure-drop correlation values | none |
| v3/V3TruncationFallbackTest | 3 | | KEEP | K6 | Truncation retry policy (event count borderline A3) | V3TestInputs |
| v3/V3TruncationNumericsTest | 6 | | KEEP | K1 | Truncated numerics conserve material | V3ManufacturedFlash |
| v3/V3TruncationStructureTest | 6 | | KEEP | K6 | Truncation structure contract | V3TestInputs |
| v3/V3TruncationSupportTest | 14 | | KEEP | K1 | Support validation and top-tail support | V3TestInputs |
| v3/V3WaterPhaseQualificationTest | 3 | | KEEP | K6 | Wet-label qualification; observer gets certified profile | V3WaterPhaseQualification, tjl20-wet-boundary.json |
| v3/linalg/V3BandedPivotedSolverTest | 9 | | KEEP | K6 | Banded pivoted solve vs dense; singular handling | none |
| v3/linalg/V3PivotGrowthTest | 5 | | KEEP | K6 | Pivot-growth evidence vs dense oracle (borderline A3) | none |
| v3/oracle/IndependentIdealMeshOracleTest | 4 | | KEEP | K4 | Independent dry MESH oracle closes all families and side-draw component balances | manufactured-two-component-four-tray.json |
| v3/thermo/RegroupedCrudeTest | 5 | | KEEP | K1 | Feed mass/volume/caloric moments; draw volume targets | V3TjlReferenceCatalog, column-draw-volume-targets.json |
| v3/thermo/V3Cdu17TiaJuanaPackageTest | 7 | | KEEP | K4 | Kesler-Lee vectors, ACS density; unsure: fixture package test-only (possible A5) | V3Cdu17TiaJuanaPackage, characterization-kesler-lee.json |
| v3/thermo/V3CharacterizationOracleTest | 2 | | KEEP | K4 | Kesler-Lee/Twu characterization reference | V3CharacterizationVectors, characterization-kesler-lee.json |
| v3/thermo/V3FeedFlashEquivalenceTest | 3 | | KEEP | K5 | Bit-equivalence to the frozen db46e88 feed flash | none |
| v3/thermo/V3FlashCheckpointTest | 4 | | KEEP | K6 | Flash cancellation and identity escape | none |
| v3/thermo/V3FlashTruncationTest | 6 | | KEEP | K1 | Truncated flash material closure | none |
| v3/thermo/V3FlashTruncationValuesTest | 21 | | KEEP | K6 | Truncation evidence validation | none |
| v3/thermo/V3PengRobinsonDerivativesTest | 7 | | KEEP | K6 | Analytic PR78 derivatives vs FD | V3Cdu17TiaJuanaPackage |
| v3/thermo/V3PengRobinsonKernelTest | 4 | | KEEP | K4 | Independent PR78 reference values | V3Cdu17TiaJuanaPackage |
| v3/thermo/V3PengRobinsonThermoTest | 5 | | KEEP | K1 | Flash material closure; captured feed regression | none |
| v3/thermo/V3Tjl20MethanePropertyPackageTest | 3 | | KEEP | K4 | NIST Shomate reference on extended package | none |
| v3/thermo/V3TruncatedFlashNumericsTest | 12 | | KEEP | K1 | Rachford-Rice conservation and budgets | none |
| v3/thermo/V3WaterPropertyDerivativesTest | 6 | | KEEP | K6 | Water derivatives (also feed the fluid network) | none |
| helper v3/V3TestInputs | - | | KEEP | K6 | Shared fixtures of most KEEP classes | - |
| helper v3/V3JacobianOracle | - | | KEEP | K5 | FD oracle for BlockJacobianAssembler/FiniteDifference tests | - |
| helper v3/V3ManufacturedFlash | - | | KEEP | K6 | Used by KEEP SimultaneousColumnSolver/TruncationNumerics tests | - |
| helper v3/V3WaterPhaseQualification | - | | KEEP | K6 | Used by V3WaterPhaseQualificationTest | - |
| helper v3/thermo/V3Cdu17TiaJuanaPackage | - | | KEEP | K4 | Used by Kernel, Derivatives, Cdu17Package tests | - |
| helper v3/thermo/V3TjlReferenceCatalog | - | | KEEP | K4 | Used by RegroupedCrudeTest (and TjlReferenceExtension) | tjl-reference-resources.json |
| res column/v3/manufactured-two-component-four-tray.json | - | | KEEP | K4 | IndependentIdealMeshOracleTest | - |
| res column/v3/tjl-reference-resources.json | - | | KEEP | K4 | V3TjlReferenceCatalog | - |
| res .../v3/thermo/characterization-kesler-lee.json | - | | KEEP | K4 | CharacterizationOracle, Cdu17TiaJuanaPackage tests | - |
| res science/column/v3/neural/regrouped-predictions.json | - | | KEEP | K4 | V3BundledRegroupedModelTest | - |
| res science/column/v3/neural/tjl20-wet-boundary.json | - | | KEEP | K6 | V3WaterPhaseQualificationTest | - |
| res science/column/v3/regrouped-authored-feature-recovery.json | - | | KEEP | K6 | V3LearnedRecoveryTest | - |
| res science/column/v3/tjl19-wet-three-pumparound-three-draw-stalled-state-java21.txt | - | | KEEP | K4 | V3TraceFloorSupportTest | - |
| res materials/column-draw-volume-targets.json | - | | KEEP | K4 | RegroupedCrudeTest | - |

## TRIM methods to archive

- V3FlashTruncationColumnTest:
  - `zeroCutoffKeepsTheLegacyCallResultsAndHasNoFlashTraceEvents` is A2 of V3StageTraceCalculatorTest.zeroCutoffPreservesLegacyDigestStreamsAuditAndDiagnosticsExactly.
  - `stageSupportAndFlashPolicyShareExactlyTheSameCutoffDomain` is A2 of V3FlashTruncationValuesTest and V3TruncationSupportTest validation.

## Method notes: these classes stay KEEP because most of their methods are K

The criteria allow TRIM only when most methods are A2/A3. The owner may still archive these methods:
- V3ColdStartTest.v1ScaleRealCrudeColdMatrixReportsInitialResidualsAndBoundedOutcomes: A1, a report with weak asserts.
- V3SimultaneousColumnSolverTest.registeredTiaJuanaPrNewtonAttemptIsBoundedAndRecordsItsActualConvergenceState: A1, it prints and accepts either outcome.
- V3PumparoundCalculatorTest:
  - aStalledDropOnlyFloorRefreshCostsABoundedRepeatOnTheFortyMegawattCase: A3/A4, a checkpoint-count budget.
  - coolingAboveTheBaseCondenserDutyStopsTheSolveWithThatDutyNamed: A2 of V3PathDependentInfeasibilityTest.
- V3StageContinuationTest:
  - acceptedFourStage...EightStage: A2 of certifiedFifteenStage...ThirtyStage, whose chain is a superset.
  - certifiedEightStage...FifteenStage: also A2 of certifiedFifteenStage...ThirtyStage.
- V3ConvergenceClosureTest.theDefaultClosureLeavesEveryEvaluationCaseExactlyWhereItWas: A3, it pins Newton iteration counts. It also checks the digest, so it stays.

## Summary

1. Verdicts: KEEP 74 classes, TRIM 1 (V3FlashTruncationColumnTest), ARCHIVE 0. All 6 helpers and 8 scope-only resources are KEEP.
2. Tests: 427 kept, 2 archived (429 counted from source; a parameterized method counts as one). Seconds are blank because build/test-review/xml is absent.
3. Six more methods in four KEEP classes are listed above as optional A1/A2/A3 archives (ColdStart, SimultaneousColumnSolver, PumparoundCalculator, StageContinuation). ConvergenceClosure has one A3-like method, but it stays because it also checks the digest.
4. Classes I was unsure about, all kept to be safe:
   - V3DecoderPresenceFloorTest: possible A2 of NeuralInitialization.
   - V3ExactWarmStartSweepTest: overlaps the hot-start checks of SimultaneousColumnSolverTest.
   - V3Cdu17TiaJuanaPackageTest: the package is test-only, so possibly A5.
   - V3PathDependentInfeasibilityTest: overlaps PumparoundCalculatorTest.
5. Gate classes: HollandExample32BenchmarkTest (v3HollandBenchmark), plus the owner rules on dew-point warning and extrapolation in FreeWaterTray, LiteraturePreset and OperatingDomainValidator.
