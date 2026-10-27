# Test review part: science.fluid (fluidScienceTest)

Scope: `src/test/java/com/wormzjl/icheme/science/fluid/**` at 138f4993 (64 test classes, 2 helpers). Criteria: `../CRITERIA.md`.
Seconds are blank: `build/test-review/xml/` does not exist in the worktree. Tests = `@Test` methods in the class.

| Class | Tests | Seconds | Verdict | Code | Reason (max 20 words; for A2 name the kept test) | Helpers/resources used |
|---|---|---|---|---|---|---|
| DeterministicMathSourceTest | 2 | | KEEP | K5 | Only guard that science/runtime sources call no non-correctly-rounded Math function (cross-JDK bitwise identity). | none (scans src/main, fluid/benchmark) |
| DeterministicMathTest | 7 | | KEEP | K5 | Pins pow/atan/tanh bits, fdlibm equality and table regeneration; powAllocatesNothing is an allocation check kept with class. | none |
| PackagedSparseLuTest | 1 | | KEEP | K6 | Only check that the built jar embeds EJML via jarjar and ships no test mod; a miss crashes load. | `fluid.modJar` system property (build.gradle) |
| SparseLuSolverTest | 6 | | KEEP | K6 | Sparse LU solve, pivoting, equilibration, singular and malformed refusal: base of every network solve. | none |
| solver/DirectLiquidJacobianRowsTest | 2 | | KEEP | K6 | Swept ln P Jacobian columns equal the EOS analytic dv/dP, dh/dP, vbar/RT; acoustic bound from own compressibility. | HydrocarbonValues |
| solver/PhaseLayoutTest | 3 | | KEEP | K3 | Decision A423 wall row and bag volume per branch; UV closure recovers TP targets with exact totals. | FluidTestSupport, MaterialTestBasis |
| solver/SparseNewtonTest | 7 | | KEEP | K6 | Newton convergence, stale preconditioner and structure reuse cannot fake convergence, typed nonconvergence, cancellation. | none |
| solver/TraceTruncationLayoutTest | 9 | | KEEP | K5 | Off switch is the full layout bit for bit; omitted phase exact zero, totals exact; reactivation finite; cutoff cap. | MaterialTestBasis |
| thermo/AmbientCrudePresetTest | 3 | | KEEP | K3 | Crude presets flash and flow at ambient; overridden fits continued below their range (owner rule 2026-10-16). | none |
| thermo/DataRangeContinuationTest | 4 | | KEEP | K3 | No range holds: below-data states carried, numerical box the only refusal, free-water ice stand-in, envelope required. | FluidTestSupport |
| thermo/DirectLiquidContinuityTest | 4 | | TRIM | K4 | Keep Helmholtz isotherm targets and liquid-N2 adiabat (G3F8 defers here); archive two retired-path comparisons (A5). | FluidTestSupport, HelmholtzFluid, HydrocarbonValues, LegacyLiquidPath (archived methods only) |
| thermo/DirectLiquidDeterminismTest | 3 | | KEEP | K5 | Reused Region 1, prepared-temperature and PR workspaces equal fresh ones bit for bit. | none |
| thermo/FluidNitrogenCryogenicTest | 6 | | KEEP | K4 | NIST nitrogen Psat, liquid density, vapour Cp, held ideal-gas Cp (D37 S4), liquid viscosity; triple point no bound. | none |
| thermo/FluidPropertyCoverageTest | 3 | | KEEP | K4 | NIST steam volume, viscosity, compression checks; finite flash with component closure over six presets' grid. | water-vapor-checks.json, water-vapor-compression.json |
| thermo/FluidThermodynamicsTest | 3 | | KEEP | K1 | flashTP conserves components and U = H - PV for water, gases, three-phase crudes; defensive state arrays. | FluidTestSupport, MaterialTestBasis |
| thermo/HydrocarbonModelTest | 6 | | KEEP | K3 | Direct liquid at state pressure, chemical-potential volume identities, RootAbsent and partial-pressure box guards (rule 2026-10-16). | FluidTestSupport, MaterialTestBasis |
| thermo/LegacyNetworkPathPinTest | 1 | | KEEP | K4 | Only pin of the bundled network package's coefficient path (SHA-256 digest, revision); SpineNetworkPathTest reads its REVISION. | FluidTestSupport |
| thermo/NetworkPhaseEngineTest | 8 | | KEEP | K3 | Engine adapter conserves, workspace bitwise, CriticalBandHold/UnsupportedPhases typed, D25/D26/D40 A splits, hot gases. | FluidTestSupport |
| thermo/NitrogenInitializationTest | 4 | | KEEP | K4 | Shomate nitrogen cp checkpoints; placement charge mass; network package resolution; wet charge UV conserves energy. | nitrogen-cp-checks.json |
| thermo/RangeContinuationGateTest | 6 | | KEEP | K3 | Gate of owner rule 2026-10-16: ideal gases, water models, viscosity tables, CO2-I continue C1; viscosity cap. | none |
| thermo/SpineNetworkPathTest | 7 | | KEEP | K4 | Pilot spine path: P1 study numbers, Helmholtz isotherms on D7 targets, anchors, sensible datum, thermo identity. | FluidTestSupport, HelmholtzFluid, PilotCryogenicTestCatalog, LegacyNetworkPathPinTest; coolprop resources (shared) |
| thermo/TranslatedPengRobinsonPairInteractionsTest | 2 | | ARCHIVE | A2 | Kept: PengRobinsonKernelPairInteractionsTest (constant pairs bitwise) and G3F8ConservationAndDerivativeTest (pilot evaluator derivatives vs differences). | PilotCryogenicTestCatalog |
| thermo/TranslatedPengRobinsonTest | 3 | | KEEP | K6 | Network translated PR78: analytic derivatives, translation effect, partial volumes against differences; no other direct test. | none |
| thermo/WaterRegion1Test | 3 | | KEEP | K4 | IAPWS-IF97 Region 1 official checkpoints and derivatives; continuation past the region (owner rule 2026-10-16). | none |
| topology/TopologyCompilerTest | 5 | | KEEP | K6 | Island compilation, dead legs, pump-only and compressor-only cycle rejection, input-order independence. | none |
| transport/MixtureViscosityTest | 3 | | KEEP | K4 | Dissolved-solute viscosity against API checkpoints; unsupported pure solute refused; continued crude liquid finite. | main resource transport/dissolved_viscosity.json, MaterialTestBasis |
| transport/SlurryTransportTest | 6 | | KEEP | K4 | Settling and deposition speeds against reviewed magnitudes; slurry viscosity limit; blockage cutoff; invalid inputs refused. | none |
| network/BackwardEulerStepTest | 9 | | KEEP | K1 | Boundary, compressor and pump-lift energy ledgers (D8, D37 S1); first-order BE vs refined steps (D4). | FluidTestSupport, MaterialTestBasis |
| network/BlockJacobianEquivalenceTest | 2 | | KEEP | K6 | Block Jacobian sweep equals whole-island column differences bit for bit on solid chain and filter island. | FluidTestSupport |
| network/CompressorTest | 5 | | KEEP | K3 | D2/D6/D7/D8: target flow, isothermal work, ratio-limit shutoff, about 93 K heating, refusals; ledgers 1e-12. | PortFixture, FluidTestSupport |
| network/ConservativeTransportTest | 3 | | KEEP | K1 | Reconstruction positive, invents no component, keeps declared volume; D54/D59 untouched vessel keeps its solids. | FluidTestSupport, MaterialTestBasis |
| network/CrystalDepositionIslandTest | 10 | | KEEP | K4 | Vessels against the engine's UV under crystal competition: deposition, reversible sublimation, withdrawal share, typed holds. | FluidTestSupport |
| network/CrystalFlushBudgetTest | 1 | | KEEP | K3 | Row E: crystal tested once per interval (D23), owner-set checkpoint bounds; warm UV equals cold. | FluidTestSupport |
| network/EmptyNetworkTest | 4 | | KEEP | K3 | D54/D59: empty vessel is a slack bag that fills and gives nothing; solids-only vessel is a dry end. | FluidTestSupport, MaterialTestBasis |
| network/FlowControlTest | 3 | | KEEP | K3 | Compressor target and D8 work, ratio shutoff; pressure valve closes below and regulates above setpoint. | FluidTestSupport, MaterialTestBasis |
| network/HydraulicMatrixQualificationTest | 4 | | KEEP | K5 | Order-independent random graphs with balances; Poiseuille parallel pipes; hydrostatic rest and D37 S1 ledger. | FluidTestSupport, MaterialTestBasis |
| network/HydraulicReferenceQualificationTest | 6 | | KEEP | K4 | Decimal friction references across regimes both directions; Poiseuille and fittings; transition slopes; section aggregation. | hydraulic-friction.json, MaterialTestBasis |
| network/InletPhaseTest | 6 | | KEEP | K3 | Liquid-only pump D1/D4/D6/A37: supply walk, thresholds, hysteresis, refused lines bitwise, bitwise replay. | PortFixture, FluidTestSupport |
| network/JunctionStartFormTest | 4 | | KEEP | K3 | D54/D55 junction mint: ambient nitrogen, minted as the arriving gas or water, neighbour seeds. | FluidTestSupport |
| network/JunctionWaterTraceTest | 3 | | KEEP | K3 | D10 water entry trace and group L arriving phase; ledgers 1e-12; holds the moved JUNCTION_ARRIVING_PHASE reference. | PortFixture, FluidTestSupport |
| network/LevelHeadTest | 9 | | KEEP | K3 | D9 level head at bottom port; drum height kept through solve; equalisation, manometer, pump shutoff, bitwise replay. | PortFixture, FluidTestSupport |
| network/LiquidFullBubblePointTest | 3 | | KEEP | K4 | Liquid-full vessels cross their bubble point (D25/D26); split equals engine UV; components conserved. | FluidTestSupport |
| network/NetworkRegimeTest | 9 | | KEEP | K6 | Junction holdup mixing law, valves, rejected-trial and cancellation restarts, phases vanish without loss, cap mode. | FluidTestSupport, MaterialTestBasis |
| network/PassiveStepSolverTest | 6 | | KEEP | K1 | Equalisation conserves amounts and energy; sign symmetry; isolated junction keeps pressure; branch flows balance. | FluidTestSupport, MaterialTestBasis |
| network/PassiveTimeRefinementTest | 1 | | KEEP | K6 | Adaptive interval against 400 fixed steps on a large-signal gas blowdown; only large-signal accuracy check. | FluidTestSupport, MaterialTestBasis |
| network/PhasePortPriorityTest | 10 | | KEEP | K3 | D11 port priority: drain order, breakthrough, overflow, shared capacity, closed lines bitwise; ledgers 1e-12. | PortFixture, FluidTestSupport |
| network/PhasePortTest | 7 | | KEEP | K3 | Ports carry exactly their phase; ledgers; solids leave only by liquid port; closed ports bitwise inert. | PortFixture, FluidTestSupport |
| network/PipeTransferTest | 4 | | KEEP | K1 | Pipe history equals tank inventory change; every crude preset conserves; reversal keeps both donor compositions. | FluidTestSupport, MaterialTestBasis |
| network/PrecipitatedSolidsTest | 17 | | KEEP | K3 | D22/D23/D26/D31/D33: crystal size, refresh, slush stop, let-down, rest, ice freeze, flush keeps crystal mass. | FluidTestSupport, MaterialTestBasis |
| network/PumpJunctionStartupTest | 1 | | KEEP | K1 | Pump start through zero-storage junction: component and energy ledger with pump work, no reverse nitrogen. | none |
| network/PumpRiseScalingTest | 5 | | KEEP | K3 | Pump reference density, compressor shutoff (D7, D54), no hold past a record (rule 2026-10-16), D21 cause. | none |
| network/RestResolutionTest | 4 | | KEEP | K3 | D42/D43/D58 rest gauge 0.1 Pa, resolution flow, path and loop drives. | FluidTestSupport |
| network/ScheduledTransferTest | 5 | | ARCHIVE | A5 | No production class constructs a ScheduledTransfer (code guide, grep); kept tests use it only as fixture. | FluidTestSupport, MaterialTestBasis |
| network/SharedSourceDepletionQualificationTest | 2 | | KEEP | K1 | Finite source depletion: no negative inventory, atomic refusal keeps snapshot, permutation-independent intervals. | FluidTestSupport, ConservationAssertions, MaterialTestBasis |
| network/SolidChainTransportTest | 4 | | TRIM | K1 | Keep ten-node chain (no line-search stall, solid mass closed); archive wall-clock, substep-count and counter pins. | FluidTestSupport |
| network/SolidClosureFeasibilityTest | 3 | | KEEP | K6 | Deposition closure located against a tight integration; filter fill at arithmetic time; ledger closed. | FluidTestSupport |
| network/SolidConservationTest | 5 | | KEEP | K1 | Particle mass and moments conserved through steps, adaptive stages and filter capture; 64-population limit. | FluidTestSupport |
| network/SolidTransportAcceptanceTest | 10 | | KEEP | K3 | D33 dry particles stay; strict suspension boundary; reverse filtration energy; population limit closes one feed. | FluidTestSupport |
| network/StagnantJunctionTest | 2 | | KEEP | K1 | Junction between CO2-depositing and cold vessels settles; species with crystals conserved; junction never precipitates (D23). | FluidTestSupport |
| network/TraceTruncationStepTest | 3 | | KEEP | K1 | Truncated step conserves every component and agrees with the exact set; reactivation creates no methane. | MaterialTestBasis |
| network/TransientQualificationTest | 3 | | KEEP | K4 | Analytic adiabatic Poiseuille relaxation (NIST viscosity); refined BE trajectories for water, crude, compressor. | FluidTestSupport, MaterialTestBasis |
| network/VelocityClampTest | 5 | | KEEP | K3 | 100 m/s cap as saturated line law both ways, three-phase, ledgers; revision moves with the limit. | MaterialTestBasis |
| network/VentGatePolishTest | 3 | | KEEP | K3 | D13 polish of gate-refused points; no equation-gate rejection; ledgers 1e-12 and 1e-10. | FluidTestSupport |
| network/WaterIntoCryogenicMethaneTest | 4 | | KEEP | K3 | D54 slack wall keeps water out; WP4 ice freezes at refresh without hold; D40 A refresh answer. | FluidTestSupport |
| network/PortFixture (helper) | - | | KEEP | - | Used by kept CompressorTest, InletPhaseTest, JunctionWaterTraceTest, LevelHeadTest, PhasePortTest, PhasePortPriorityTest. | FluidTestSupport |
| thermo/HydrocarbonValues (helper) | - | | KEEP | - | Used by kept DirectLiquidJacobianRowsTest and DirectLiquidContinuityTest (adiabat); also SpinePackageEvaluatorTest, G3Support. | none |
| resource fluid/reference/hydraulic-friction.json | - | | KEEP | - | Only HydraulicReferenceQualificationTest (KEEP). | generator examples/Generate-Hydraulic-References.py |
| resource fluid/reference/nitrogen-cp-checks.json | - | | KEEP | - | Only NitrogenInitializationTest (KEEP). | generator examples/Build-NitrogenFluidProperties.py |
| resource fluid/reference/water-vapor-checks.json | - | | KEEP | - | Only FluidPropertyCoverageTest (KEEP). | generator examples/Fetch-FluidSteamReferences.py |
| resource fluid/reference/water-vapor-compression.json | - | | KEEP | - | Only FluidPropertyCoverageTest (KEEP). | generator examples/Fetch-FluidSteamReferences.py |
| resource fluid/reference/README.md | - | | KEEP | - | Provenance of the four reference files above. | none |

## TRIM: methods to archive

DirectLiquidContinuityTest (keep `isothermsCrossTwoMegapascalsWithALiquidRootAndMeetTheTargets`, `aClosedLiquidNitrogenVesselCompressedThroughTwoMegapascalsFollowsTheAdiabat`):
- `belowTwoMegapascalsTheLiquidMovesOnlyByTheFirstOrderCorrection`: A5, compares against the retired reference-pressure liquid (`fluid/support/LegacyLiquidPath`).
- `secondDifferencesShowNoJumpAtTwoMegapascals`: A5, 2 MPa was the retired path's regime switch; the EOS is evaluated at the state pressure.

SolidChainTransportTest (keep `tenReservoirChainIntegratesFiveSecondsWithoutALineSearchStall`):
- `thirtyReservoirChainIntegratesOneIntervalWithinItsBudget`: A4, 1500 ms wall-clock bound; its mass closure duplicates the ten-node test.
- `clearChainSubstepCountsAreUnchanged`: A3, pins accepted/rejected substep counts.
- `filterIslandReusesItsSolverWorkspacesAcrossIntervals`: A3, pins workspace-reuse and Jacobian counters.

## Summary

- 64 test classes: KEEP 60, TRIM 2 (DirectLiquidContinuityTest, SolidChainTransportTest), ARCHIVE 2 (ScheduledTransferTest A5, TranslatedPengRobinsonPairInteractionsTest A2). Helpers PortFixture and HydrocarbonValues KEEP. The 5 scope-only resources KEEP.
- Tests: 302 in total, 290 kept, 12 archived (5 + 2 + 2 + 3). Seconds: not available (no `build/test-review/xml/`).
- Unsure: ScheduledTransferTest. The A5 call rests on the code guide plus a grep: no main class constructs one. LiquidFullBubblePointTest, CrystalDepositionIslandTest and PhasePortPriorityTest use it as their energy input. TranslatedPengRobinsonPairInteractionsTest is A2 only if PengRobinsonKernelPairInteractionsTest and G3F8ConservationAndDerivativeTest are kept in their parts.
- Not trimmed because the archivable methods are not "most" of the class: the 0.1 s cadence twins in PhasePortPriorityTest, PhasePortTest, LevelHeadTest and VentGatePolishTest; `DeterministicMathTest.powAllocatesNothing`; `SolidClosureFeasibilityTest.heavyLiquidCoolingStopsAtTheActualViscosityTableCrossing` (A1 probe); the counter test in TraceTruncationStepTest (A3).
- Cross-scope: after the trim, `fluid/support/LegacyLiquidPath` has no user. SpineNetworkPathTest reads `LegacyNetworkPathPinTest.REVISION`. The code guide's routing table names TranslatedPengRobinsonPairInteractionsTest and ScheduledTransferTest, so it needs an update if they are archived.
