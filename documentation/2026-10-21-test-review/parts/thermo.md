# Test review: science/thermo (2026-10-21)

Scope: `src/test/java/com/wormzjl/icheme/science/thermo/**` (37 test classes, 14 helpers) and `src/test/resources/science/thermo/**` (29 files). Tree: 138f4993. Criteria: `../CRITERIA.md`.
Seconds are blank: `build/test-review/xml/` does not exist in the worktree. The Tests column is the source count of `@Test`/`@ParameterizedTest` methods. Runtime cases are higher: RootPrecision 1 x 2 rows, Helmholtz tests x 6 fluids.
Outside this scope, these classes use helpers from this scope: `V3PengRobinsonKernelTest` and `fluid/support/LegacyLiquidPath` (use `PengRobinson78`), and `ReferenceSpineTest`, `PilotVolumeAnchorTest`, `SpineNetworkPathTest` and `DirectLiquidContinuityTest` (use the Helmholtz oracle and the CoolProp files). For that reason the oracle helpers and the CoolProp files are KEEP whatever happens to the tests in this scope.

## Test classes

| Class | Tests | Seconds | Verdict | Code | Reason (max 20 words; for A2 name the kept test) | Helpers/resources used |
|---|---|---|---|---|---|---|
| PairInteractionsTest | 7 | | KEEP | K4 | E-PPR78 k_ij(T) against appendix A table and the closed form to 1e-12; only reference pin of the rule. | PengRobinsonKernel, bundled catalog |
| PengRobinson78Test | 7 | | TRIM | K4 / A5 | Keep the oracle's two Z/ln phi pins (they ground V3PengRobinsonKernelTest); the rest tests oracle features nobody consumes. | PengRobinson78, PhaseProperties |
| PengRobinsonKernelPairInteractionsTest | 4 | | KEEP | K5 | Constant model = SPARSE_PAIRS bit for bit; kernel E-PPR78 derivatives; partial overlap with fluid TranslatedPengRobinsonPairInteractionsTest. | PairInteractionsTest statics |
| PengRobinsonKernelRootPrecisionTest | 5 | | KEEP | K6 | Root selection of every engine against a 90-digit oracle; cancellation roots, coalescence band, bit preservation. | none |
| TangentPlaneStabilityTest | 10 | | KEEP | K6 | Stability verdicts against brute-force minima and network flash; bitwise repeats; per-branch pressures. Code guide pin. | FluidTestSupport, bundled catalog |
| phase/CrystalPhaseEvaluatorTest | 5 | | TRIM | K6 / A2 | Keep anchoring and contract tests; oracle Figure 9/11 replays duplicate JaegerSpanGibbsFunctionTest.reproducesTableFiveCheckValues and SolidCarbonDioxideQualificationTest. | SolidCarbonDioxideReferences, HelmholtzFluid, PilotCryogenicTestCatalog |
| phase/CubicPhaseEvaluatorTest | 6 | | KEEP | K6 | Engine fluid evaluator against TranslatedPengRobinson, spine convention, derivatives; only pin of the adapter. | PhaseTestSupport |
| phase/FluidTpEquilibriumP3Test | 16 | | KEEP | K3 | Critical band with D14 corner/D15 box, free-water network rule, phase labels, Newton finish; code-guide routing pins. | PhaseTestSupport |
| phase/FluidTpEquilibriumPhUvTest | 8 | | KEEP | K1 | PH/UV round trips, coexistence from balance, enthalpy continuity, extrapolated vessel (owner rule); code-guide pin. | PhaseTestSupport |
| phase/FluidTpEquilibriumTest | 7 | | KEEP | K6 | TP splits, typed contract refusals, water chemistry contract, trace precision, bitwise repeats. | PhaseTestSupport |
| phase/FreeWaterIceTest | 7 | | KEEP | K3 | Ice rule gates (a)(b) of the range plan: sublimation pressure, water in LNG, PH/UV on melting line. | PhaseTestSupport |
| phase/GasSolidEquilibriumTest | 12 | | KEEP | K1 | Crystal deposition conserves to 1e-13 with equal chemical potentials; PH/UV reversibility; typed refusals; bitwise. | PhaseTestSupport |
| phase/NearPureCrystalStepTest | 4 | | KEEP | K3 | Decision D21 (near-pure crystal step, trace threshold 1e-5) encoded as a gate. | PhaseTestSupport |
| phase/SolidCarbonDioxideQualificationTest | 5 | | KEEP | K4 | CO2-I record against Span-Wagner, Fray-Schmitt, Trusler, Giauque-Egan; pins the chosen 9019 J/mol fusion enthalpy. | SolidCarbonDioxideReferences, HelmholtzFluid, CoolProp CarbonDioxide.json |
| phase/SolidPhaseEvaluatorTest | 2 | | KEEP | K1 | Only test that conservationDefect reports a nonzero defect with a solid phase; result validation refusals. | PhaseTestSupport, test double |
| phase/SpinePackageEvaluatorTest | 3 | | KEEP | K5 | Engine evaluator equals network model bit for bit (V, ln phi) on two energy datums. | PhaseTestSupport |
| phase/ThermoIdentityTest | 2 | | KEEP | K6 | Cache identity moves with every part; a stale identity would serve wrong cached results. | none |
| qualification/G3F1PureFluidOracleTest | 2 | | KEEP | K4 | Pilot pure fluids against Helmholtz oracle with D7/D14/D15 bounds; only Psat and hvap sweep. | G3Support, CoolProp N2/CH4/C2H6/CO2 |
| qualification/G3F2GergBubblePointTest | 1 | | KEEP | K4 | Six pilot binaries against GERG-2008 bubble points; only user of that reference. | G3Support, gerg2008/pilot-binaries.json |
| qualification/G3F3HighTemperatureIdealGasTest | 4 | | KEEP | K4 | Oracle residual at range ends and network h = spine + residual are unique; ideal-gas holdouts overlap ReferenceSpineTest. | G3Support, CoolProp files |
| qualification/G3F4HighPressureContinuityTest | 2 | | ARCHIVE | A2 | SpineNetworkPathTest.reproducesTheP1DirectEvaluationWithTheTr08Anchors and DirectLiquidContinuityTest isotherms and secondDifferencesShowNoJumpAtTwoMegapascals. | G3Support, CoolProp files |
| qualification/G3F5CriticalRegionPathTest | 1 | | KEEP | K6 | Only sweep holding critical paths converged or typed CRITICAL_BAND; no NOT_CONVERGED outside the band. | G3Support, CoolProp N2/CO2 |
| qualification/G3F6CurrentCutProbeTest | 4 | | ARCHIVE | A1 | Declares itself probes only, no qualification claim; checks are monotonicity and NBP echoes. | G3Support |
| qualification/G3F7CarbonDioxideInLiquidMethaneTest | 1 | | KEEP | K3 | Owner's below-freezing example (CO2 in LNG) answered by the fluid-only contract; crystal conservation 1e-13. | G3Support |
| qualification/G3F8ConservationAndDerivativeTest | 4 | | KEEP | K1 | TP/PH/UV conservation 2.3e-16, closed vessels, network inventory = engine UV; code-guide PH/UV pin. | G3Support |
| qualification/G3F9DataRangeBoundaryTest | 1 | | KEEP | K3 | Owner rule 2026-10-16: continuous answers past every data boundary, EXTRAPOLATED grade; code-guide pin. | G3Support |
| qualification/G3F10QualifiedRegionTest | 3 | | KEEP | K4 | Bundled network package N2/CH4 against oracle (saturation, gas grid); hot-gas method duplicates FluidTpEquilibriumP3Test. | G3Support, CoolProp N2/CH4 |
| qualification/G4F1CarbonDioxideFrostPointHoldoutTest | 4 | | KEEP | K4 | CO2 frost points in methane gas against Le-Trebble, Zhang, Xiong literature holdouts. | 5 holdout TSVs (le-trebble, zhang, xiong, souza, fandino) |
| qualification/G4F2CarbonDioxideNitrogenFrostPointHoldoutTest | 1 | | KEEP | K4 | CO2 frost points in N2 against Sonntag/Smith; extrapolated grade above 10 MPa asserted. | sonntag, smith TSVs; G4F1.rows |
| qualification/G5F1PureCarbonDioxideMeltingTest | 3 | | KEEP | K4 | Engine melting line against Span-Wagner (D16 0.13 K); PH melting step and UV triple point conserve. | G5Support, SolidCarbonDioxideReferences |
| qualification/G5F2CarbonDioxideLiquidusTest | 1 | | KEEP | K4 | CO2 liquidus in liquid methane against Shen 2012 and Davis 1962. | G5Support, shen, davis TSVs |
| qualification/G5F3ThreePhaseLineTest | 2 | | KEEP | K4 | SLV three-phase line and compositions against Davis 1962 and Souza 2020. | G5Support, davis, souza TSVs |
| qualification/G5F4TernarySolidLiquidVapourTest | 1 | | KEEP | K4 | Ternary SLVE with N2 against Campestrini 2022 and Riva-Stringari 2018. | G5Support, campestrini, stringari-riva TSVs |
| qualification/G5F5ReportOnlyLiquidusTest | 1 | | ARCHIVE | A1 | Report-only by its own statement; no-hold of extrapolated onsets also asserted by G4F2 and G3F9. | G5Support, gao, sampson, kurata-im TSVs |
| qualification/G5F6ConsistencyTest | 3 | | KEEP | K1 | Crystal beside liquid: conservation 1e-13, mu 1e-9, PH/UV round trips, reversibility, P4 three-phase states answered. | G5Support |
| reference/HelmholtzReferenceParityTest | 2 | | KEEP | K4 | Oracle parity with CoolProp 8.0.0 at 1e-10; certifies the oracle many kept K4 tests rely on. | JsonFluidFiles, parity-*.json, six fluid files |
| reference/HelmholtzReferenceTest | 3 | | ARCHIVE | A2 | HelmholtzReferenceParityTest: same oracle's properties and saturation states checked at 1e-10 against CoolProp. | HelmholtzFluid, Nitrogen.json |

## Helpers

| Helper | Verdict | Used by (kept) |
|---|---|---|
| PengRobinson78 | KEEP | PengRobinson78Test (trimmed), V3PengRobinsonKernelTest, fluid/support/LegacyLiquidPath |
| PhaseProperties | KEEP | PengRobinson78, V3PengRobinsonKernelTest |
| phase/PhaseTestSupport | KEEP | 10 kept phase classes, TangentPlaneStabilityTest |
| phase/SolidCarbonDioxideReferences | KEEP | SolidCarbonDioxideQualificationTest, CrystalPhaseEvaluatorTest, G5F1 |
| qualification/G3Support | KEEP | G3F1, F2, F3, F5, F7, F8, F9, F10 |
| qualification/G5Support | KEEP | G5F1-F4, F6 (its `gao()` and `sampson()` lose their only caller with G5F5) |
| reference/AlphaDerivatives, HelmholtzFluid, HelmholtzState, IdealHelmholtz, JsonFluidFiles, ResidualHelmholtz, SaturationAncillary, SaturationState | KEEP | Parity test, G3*, solid CO2 tests; outside the scope: ReferenceSpineTest, PilotVolumeAnchorTest, SpineNetworkPathTest, DirectLiquidContinuityTest |

## Test resources (`src/test/resources/science/thermo/`)

| Resource | Verdict | Used by |
|---|---|---|
| coolprop/{Nitrogen,Methane,Ethane,CarbonDioxide}.json | KEEP | Oracle: G3*, parity, solid CO2 tests, out-of-scope spine/fluid tests |
| coolprop/{Hydrogen,Water}.json | KEEP | HelmholtzReferenceParityTest |
| coolprop/parity-*.json (6) | KEEP | HelmholtzReferenceParityTest |
| coolprop/LICENSE, coolprop/README.md | KEEP | Licence and provenance of the kept files (non-commercial, provenance rule) |
| gerg2008/pilot-binaries.json | KEEP | G3F2 |
| solid-co2-holdouts: le-trebble-2007, zhang-2011, xiong-2015, souza-2020, fandino-2015 | KEEP | G4F1 (souza also G5F3) |
| solid-co2-holdouts: sonntag-1960, smith-1963 | KEEP | G4F2 |
| solid-co2-holdouts: shen-2012, davis-rodewald-kurata-1962 | KEEP | G5F2, G5F3 (via G5Support) |
| solid-co2-holdouts: campestrini-2022, stringari-riva-2018 | KEEP | G5F4 |
| solid-co2-holdouts: gao-2012-iecr-51-9403.tsv, sampson-2023-aiche-aic18001-table2.tsv, kurata-im-1971-jced-16-295-table6.tsv | ARCHIVE | G5F5 only |

## TRIM: methods to archive

- PengRobinson78Test (A5): `diluteGasApproachesTheIdealGasLimit`, `mixtureResidualEnthalpyMatchesFugacityTemperatureDerivative`, `residualEnthalpyApproachesZeroAtLowPressure`, `propaneLiquidResidualEnthalpyIsMoreNegativeThanVapor`, `returnedFugacityVectorCannotMutateTheResult`. Kept: `methaneReferencePointMatchesPengRobinsonCalculation`, `liquidAndVaporRootsAreSelectedAtAThreeRootState`.
- CrystalPhaseEvaluatorTest (A2): `oracleAnchoringRecoversThePublishedIntegrationConstants`, `oracleSublimationFollowsTheSpanWagnerCorrelationAsInFigureNine`, `oracleMeltingFollowsTheSpanWagnerCorrelationAsInFigureEleven` (and its `jaegerSpanRecord()` helper). Kept: `anchorsToThePilotLiquidAndFillsAConsistentSolidState`, `refusesForeignAnchorsAndContinuesPastItsRange`.

## Summary

- Classes: KEEP 31, TRIM 2, ARCHIVE 4 (G3F4, G3F6, G5F5, HelmholtzReferenceTest). Helpers: 14 KEEP. Resources: 26 KEEP, 3 ARCHIVE (the three TSVs used only by G5F5).
- Test methods: 136 kept, 18 archived (of 154 source methods). Seconds: not available (no `build/test-review/xml/`).
- The G3/G4/G5 families are mostly still the only guards: the GERG, literature-holdout, oracle and owner-rule checks (G3F7, G3F9), and conservation (G3F8, G5F6). Only G3F4 is a duplicate. G3F6 and G5F5 are self-declared probes or reports.
- Unsure: G3F4 is ARCHIVE only if the fluid reviewer keeps SpineNetworkPathTest and DirectLiquidContinuityTest. If either is archived, keep G3F4. The PengRobinson78Test trim tests a test oracle, not production.
- Also unsure: PengRobinsonKernelPairInteractionsTest and G3F8's derivative test overlap fluid TranslatedPengRobinsonPairInteractionsTest. G3F3 (ideal-gas holdouts) and G3F10 (hot gas) each have one duplicated method, but they are kept because most of their methods are unique.
