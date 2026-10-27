# Test review part: material, equipment, network, client, fluid/support, fluid/benchmark

Tree: 138f4993 (0.11.0). Criteria: `documentation/2026-10-21-test-review/CRITERIA.md`. Timings: `build/test-review/xml/TEST-*.xml` (`tests=`, `time=`); `FluidSolverRegressionTest` from `build/test-results/fluidSolverRegression/`. The two benchmark tests have no XML (no run of their tasks), so their seconds are blank.

## Test classes

| Class | Tests | Seconds | Verdict | Code | Reason (max 20 words; for A2 name the kept test) | Helpers/resources used |
|---|---|---|---|---|---|---|
| science/material/CrystalReferenceTest | 6 | 0.023 | KEEP | K4 | CO2-I record equals Jaeger-Span function; crystal loader refusals (K2); segment C1 continuation gate (K3, owner rule 2026-10-16) | PilotCryogenicTestCatalog |
| science/material/FluidAppearanceTest | 5 | 0.103 | KEEP | K2 | Only guard of appearance loader validation, failed reload keeping the published catalog, fingerprints unchanged by appearance | MaterialCatalogTest.changed |
| science/material/GroupContributionInteractionsTest | 7 | 0.147 | KEEP | K4 | E-PPR78 Table S4 matrix and appendix A kij; group-matrix loader refusals; fingerprints follow the used pairs | PilotCryogenicTestCatalog |
| science/material/JaegerSpanGibbsFunctionTest | 4 | 0.002 | KEEP | K4 | Jaeger and Span 2012 Table 5 check values, analytic derivatives, integration constants; only pin of the paper | – |
| science/material/LiquidMixtureCorrectionTest | 4 | 0.020 | KEEP | K6 | Only test of the liquid viscosity mixing correction (bundled in 7 packages) against an independent pair sum | – |
| science/material/MaterialAuthoringTest | 4 | 0.044 | KEEP | K1 | MaterialAxis projection keeps amounts and refuses nonzero retired species; physics fingerprint per axis; basis-cycle refusal | – |
| science/material/MaterialCatalogTest | 8 | 0.091 | KEEP | K2 | Catalog loader: invalid reload never publishes, duplicate/missing refusals, mass-basis assay conversion, scoped runtime context | Cdu17TestCatalog, V3TestInputs |
| science/material/MaterialFluidDataTest | 5 | 0.142 | KEEP | K2 | Fluid-data and solid-record refusals with file and field; water without ice refused; fingerprint anchors | – |
| science/material/MaterialPresetsTest | 5 | 0.087 | KEEP | K4 | Six column presets pinned to frozen inputs; five assay conversions with flash component balance (K1); override example | column-preset-inputs.json, crude-assay-conversion-fixture.json, examples/material-override |
| science/material/MethaneSpineTest | 5 | 0.006 | KEEP | K4 | Methane spine against ExoMol line list and GERG2008.cpp transcription; JANAF disagreement pinned | – |
| science/material/PilotCryogenicCatalogTest | 8 | 0.181 | KEEP | K4 | Pinned revisions and fingerprints of all nine packages; pilot loader rules (spine, D6 ideal-gas source, volume translation) | PilotCryogenicTestCatalog, pilot-cryogenic/** |
| science/material/PilotVolumeAnchorTest | 2 | 0.004 | KEEP | K4 | Pilot volume translations recomputed from the Helmholtz oracle to 1e-12; P1 shifts reproduced | – |
| science/material/ReferenceSpineTest | 11 | 0.092 | KEEP | K4 | Spines against Helmholtz oracle and JANAF/Gurvich holdouts; anchors, joins, bitwise reload (K5), loader refusals (K2) | PilotCryogenicCatalogTest.refused |
| science/material/ViscosityTest | 8 | 0.134 | KEEP | K4 | IAPWS SR6-08 table 8 and >1000 DWSIM check points; viscosity loader refusals; C1 continuation past data | dwsim-viscosity-api.json, dwsim-ambient-viscosity-api.json, Cdu17TestCatalog |
| science/material/Cdu17TestCatalogTest | 3 | 0.024 | ARCHIVE | A3 | Tests the test fixture's own scoping; scope restore kept in MaterialCatalogTest.runningContextKeepsBothPrAndWaterAcrossPublicationAndRestoresAfterFailure | Cdu17TestCatalog |
| science/equipment/ColumnStructureTest | 7 | 0.390 | KEEP | K3 | Pipe spacing gate for columns, climbs within owner decision C1; trays imported from a calculator | ProcessUnitTest.pipesApart, climbedFromShellCells |
| science/equipment/EquipmentRefreshTest | 5 | 0.090 | KEEP | K3 | Gate of the AGENTS.md equipment-refresh exception: stamps, plan, another block never overwritten | – |
| science/equipment/LineGeometryTest | 7 | 0.707 | KEEP | K3 | Owner rule 2026-10-18: drawing changes judged by these gates and renders; watertight pipe and device models | – |
| science/equipment/PaintTest | 4 | 0.002 | KEEP | K2 | Paint encode/decode is NBT and wire format (constructor block entity, EquipmentNetwork); round trip and refusals | – |
| science/equipment/ProcessUnitTest | 19 | 25.469 | KEEP | K3 | Pipe spacing gate over every unit size; owner decisions C1, toe boards, stairs; walk and climb checks | everySize, pipesApart (shared helpers) |
| science/equipment/ShapeRasterizerTest | 25 | 1.952 | KEEP | K3 | Pipe spacing, drum port classes by position (owner decision 2026-10-16), dedicated-server layout equals drawing side (K6) | ProcessUnitTest.pipesApart |
| science/equipment/UnitTextTest | 1 | 0.018 | KEEP | K6 | Only guard that every unit kind has its lang keys and fits screens (in-game report 2026-10-18); unsure | ProcessUnitTest.everySize, assets lang en_us.json |
| science/equipment/VesselPlacementTest | 3 | 0.013 | KEEP | K6 | Placement box never covers the constructor; drum outlet faces per owner report 2026-10-16; offset bounds | – |
| network/ColumnV3NetworkCodecTest | 19 | 1.489 | KEEP | K2 | Wire and NBT round trip of column input and result; forged counts and missing lists refused | ColumnCodecs, Cdu17FixtureExtension, Cdu17TestCatalog |
| network/MaterialNamesCodecTest | 3 | 0.006 | KEEP | K2 | V3State with server name descriptors round-trips; retired axis refused by the real NBT reader | ColumnCodecs, V3TestInputs |
| network/V3EditorCatalogCodecTest | 3 | 0.007 | KEEP | K2 | Editor catalogue codec round trip; truncated, trailing, oversized and nonphysical payloads refused | ColumnCodecs |
| network/V3InspectionCodecTest | 4 | 0.010 | KEEP | K2 | Inspection codec round trip at size extremes; payload refusals; display result through wire and NBT | ColumnCodecs, V3TestInputs |
| client/gui/common/ProcessPresentationTest | 5 | 0.009 | KEEP | K6 | Only guard of TemperatureUnit conversion and NumericDraft keeping untouched precision; RelativeComposition basis switch | – |
| client/gui/common/ComponentSearchTest | 6 | 0.035 | ARCHIVE | A3 | Pins fuzzy ranking order of a GUI search; no number, material or load path depends on it; unsure | – |
| client/gui/common/TextInputPriorityTest | 2 | 0.465 | ARCHIVE | A3 | Key routing of a focused edit box; GUI behaviour only, no K criterion; unsure | – |
| client/gui/screens/inventory/V3ClientPresetsTest | 7 | 0.284 | KEEP | K2 | Client preset JSON files: round trip, invalid and schema-1 files refused, never overwrite, path escape refused | V3EditorDraftTest.input |
| client/gui/screens/inventory/V3ColumnInputDraftTest | 12 | 0.009 | KEEP | K6 | Editor scalars to SI (kmol/h, °C, kPa, MW) and signed cooler duties reaching the solver | V3TestInputs |
| client/gui/screens/inventory/V3EditorDraftTest | 15 | 0.014 | KEEP | K6 | Editing one field never rounds other inputs; tray-count edits move connections and steam correctly | – |
| client/gui/screens/inventory/V3PumparoundDraftTest | 9 | 0.004 | ARCHIVE | A2 | Signs and rows kept in V3ColumnInputDraftTest.assembledInputCarriesTheAuthoredCoolersWithTheirSplits; tray rules enforced by V3PumparoundSpec/V3ColumnInput; dutyWattsToCoolingMegawatts unused (A5) | – |
| fluid/benchmark/FluidSolverRegressionTest | 1 | 0.858 | KEEP | K4 | chain-100 exact replay, gate G2 (`fluidSolverRegression` task) | SolverRegressionHarness, chain-100.json, FluidTestSupport |
| fluid/benchmark/FluidNetworkBenchmarkTest | 1 | | ARCHIVE | A4 | Unpaced scaling screen, timing output; 100-chain convergence kept in FluidSolverRegressionTest; no gate runs it | FluidTestSupport |
| fluid/benchmark/FluidPropertyBenchmarkTest | 1 | | ARCHIVE | A4 | Property microbenchmark; only assertion is a finite sink value; no gate runs it | FluidTestSupport |

## Helpers

| Helper | Verdict | Used by |
|---|---|---|
| science/material/CatalogScopedExtension | KEEP | Base of Cdu17FixtureExtension and TjlReferenceExtension |
| science/material/Cdu17FixtureExtension | KEEP | ColumnV3NetworkCodecTest and about 29 science/column/v3 tests |
| science/material/Cdu17TestCatalog | KEEP | MaterialCatalogTest, ViscosityTest, ColumnV3NetworkCodecTest, V3ColumnCalculatorTest, V3PumparoundCalculatorTest, V3PumparoundSteamCalculatorTest, V3PengRobinsonThermoTest, V3TraceFloorSupportTest |
| science/material/MaterialTestBasis | KEEP | About 60 runtime/fluid and science/fluid tests and rigs (BlockLineHarness, CertifyingRig, FluidTestSupport) |
| science/material/PilotCryogenicTestCatalog | KEEP | 17 test classes (material, thermo, runtime gates incl. HoldClassWorldsTest, JunctionSeedingGateTest) and the cold hold fuzz in tools/ |
| science/material/TjlReferenceExtension | KEEP (conditional) | V3TraceFloorSupportTest, V3WaterPhaseQualificationTest (other parts decide those) |
| network/ColumnCodecs | KEEP | The four network codec tests, ColumnInputPresetTest, V3MethaneColumnTest |
| fluid/support/ConservationAssertions | KEEP | FluidTestSupport, SharedSourceDepletionQualificationTest (K1 helper) |
| fluid/support/FluidTestSupport | KEEP | 98 test and rig files in src/test |
| fluid/support/LegacyLiquidPath | KEEP (conditional) | Only DirectLiquidContinuityTest.belowTwoMegapascalsTheLiquidMovesOnlyByTheFirstOrderCorrection, which compares against the retired liquid path (A5 candidate); archive with that method if its part archives it |
| fluid/benchmark/SolverRegressionHarness | KEEP | FluidSolverRegressionTest (G2) |

## Test resources

| Resource | Verdict | Used by |
|---|---|---|
| materials/cdu17-reference/** (index.json + 40 records) | KEEP | Cdu17TestCatalog (all its users above) |
| materials/pilot-cryogenic/** (index.json, networks/default.json, 3 presets) | KEEP | PilotCryogenicTestCatalog, PilotCryogenicCatalogTest |
| materials/column-preset-inputs.json | KEEP | MaterialPresetsTest |
| materials/crude-assay-conversion-fixture.json | KEEP | MaterialPresetsTest |
| materials/dwsim-viscosity-api.json | KEEP | ViscosityTest |
| materials/dwsim-ambient-viscosity-api.json | KEEP | ViscosityTest |
| materials/column-draw-volume-targets.json | KEEP (conditional) | RegroupedCrudeTest (other part) |
| fluid/regression/chain-100.json | KEEP | FluidSolverRegressionTest, SolverRegressionHarness, `fluidSolverRegression` task |
| fluid/mcp-held-drain-checkpoint.json, .md; fluid/mcp-clamped-liquid-full-checkpoint.json | KEEP (conditional) | McpGameplayRegressionTest (other part); the .md is the provenance note of the json |
| fluid/reference/hydraulic-friction.json | KEEP (conditional) | HydraulicReferenceQualificationTest (other part) |
| fluid/reference/nitrogen-cp-checks.json | KEEP (conditional) | NitrogenInitializationTest (other part) |
| fluid/reference/water-vapor-checks.json, water-vapor-compression.json | KEEP (conditional) | FluidPropertyCoverageTest (other part) |
| fluid/reference/README.md | KEEP (conditional) | Provenance of the three reference files; goes with them |
| column/v3/manufactured-two-component-four-tray.json | KEEP (conditional) | IndependentIdealMeshOracleTest (other part) |
| column/v3/tjl-reference-resources.json | KEEP (conditional) | V3TjlReferenceCatalog (TjlReferenceExtension, RegroupedCrudeTest) |
| science/column/v3/neural/regrouped-predictions.json | KEEP (conditional) | V3BundledRegroupedModelTest (other part) |
| science/column/v3/neural/tjl20-wet-boundary.json | KEEP (conditional) | V3WaterPhaseQualificationTest (other part) |
| science/column/v3/regrouped-authored-feature-recovery.json | KEEP (conditional) | V3LearnedRecoveryTest (other part) |
| science/column/v3/tjl19-wet-three-pumparound-three-draw-stalled-state-java21.txt | KEEP (conditional) | V3PumparoundSteamCalculatorTest, V3TraceFloorSupportTest (other part) |

"Conditional": every user is outside this part. Keep the resource if any of its users is kept, else archive it with them.

## TRIM rows

None. Notes on methods inside kept classes (each is a minority of its class, so the class is not TRIM):
- `ColumnV3NetworkCodecTest.roundedServerDefaultPublishesAndSolvesTheQualifiedLiteratureSideDraws` is a full column solve inside a codec class (0.92 s). It duplicates the preset solve in `V3PumparoundCalculatorTest.aCoolerCancelledByAnEqualHeaterReachesTheHeatFreePresetState` (A2). Archive it on its own if single-method archives are accepted.
- `V3ColumnInputDraftTest.largerWindowFitsEverySupportedTrayCountWithoutScrolling` and `smallWindowsScrollInsteadOfMakingTraysUnclickable` pin GUI layout only (A3).

## Archive side effects

- Archiving the two benchmark tests needs the `fluidPropertyBenchmark` and `fluidNetworkBenchmark` tasks removed from `build.gradle`. Both tasks set `failOnNoMatchingTests`. The CHANGELOG and verification.md also need a line.
- AGENTS.md keeps "harnesses a documented Gradle task runs". These are documented tasks, but no gate runs them. The owner must confirm before they leave.

## Summary

- Verdicts: 31 KEEP, 6 ARCHIVE, 0 TRIM. Archived: Cdu17TestCatalogTest, ComponentSearchTest, TextInputPriorityTest, V3PumparoundDraftTest, FluidNetworkBenchmarkTest, FluidPropertyBenchmarkTest. Helpers: 11 KEEP, of which 2 are conditional. Resources: those of this part are KEEP; the rest depend on other parts.
- Tests: 222 kept, 22 archived. Seconds: 32.40 kept (ProcessUnitTest alone is 25.47 s), 0.53 archived. The two benchmarks have no measured time.
- Unsure: ComponentSearchTest and TextInputPriorityTest (GUI-only, archived); UnitTextTest (kept, cosmetic); Cdu17TestCatalogTest (it tests a fixture, archived).
- Unsure: the two benchmarks, because AGENTS.md keeps harnesses that documented tasks run. LegacyLiquidPath depends on the verdict for DirectLiquidContinuityTest.
