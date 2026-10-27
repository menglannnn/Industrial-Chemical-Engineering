# Port of the coolprop branch onto main: thermo, materials and fluid-thermo layer

Date: 2026-10-15. Read-only analysis (no edits to tracked files, no Gradle).

- Branch `claude/coolprop-multiphase-thermo-37f6b0`: HEAD `9aaafd43`, 61 commits over the base `f9d6be1`.
- main: `ffeafe8a` (0.8.0), 88 commits since the base, including:
  - 0.6.0: backward Euler;
  - 0.7.0: phase ports and the `DeterministicMath` facade;
  - 0.7.2: code consolidation (ids F/S/M/T from `documentation/2026-10-15-code-consolidation/CONSOLIDATION_PLAN.md`);
  - 0.8.0: GUI fixes.
- Scope: `science/thermo/**`, `science/material/**`, `science/fluid/thermo/**` and `science/fluid/transport/**`, with their tests and the bundled material data.

Disposition codes:

- **T**: take the branch file as is (main did not touch it, or main's edit is trivial).
- **R**: re-apply the branch hunks on main's file.
- **D**: redesign. Both sides changed the same behaviour for different reasons, so the merged form is new code.
- **X**: drop. The file, or main's change to it, serves only a path the branch retired.
- **OWNER DECISION**: the choice is not forced by physics, by main's code or by a recorded D-rule. Options and a recommendation are given.

Recorded rules used below:

- **D1**: the extended cubic is chosen for production, with liquids at state pressure.
- **D9**: the formulation changes apply to every network package, and chain-100 is re-captured. The formulation changes are the direct liquid, water at state pressure, and the stability-driven phase decision.
- **D10**: the energy datum, for spine packages only.
- **D12**: rejects keeping a reference-pressure water path.
- **Plan P3**: "delete the 2 MPa reference path and the global compressibility".
- **Main CONSOLIDATION_PLAN owner decisions (2026-10-15)**:
  - remove F2 (analytic derivatives);
  - keep S10 (appearance is required in the datapack).

`git merge-tree main HEAD` reports these conflicts in this layer:

- Production: `FluidThermodynamics`, `GlobalLiquidResponse` (modify/delete), `HydrocarbonModel`, `TranslatedPengRobinson`, `MaterialCatalog` and `PengRobinsonKernel`.
- Tests: `FluidThermoDomainHoldTest`, `AmbientCrudePresetTest`, `EmptyWaterFillingAuditTest` (modify/delete), `FluidPropertyCoverageTest`, `FluidThermodynamicsTest`, `HybridDerivativeTest` (modify/delete), `HydrocarbonModelTest`, `ThermoDomainViolationTest` and `TranslatedPengRobinsonDerivativesTest` (modify/delete).
- Test resources: `chain-100.json`.

`FluidDomain`, `WaterRegion1`, `MixtureViscosity` and `materials-index.json` merge automatically.

---

## A. Compile breakages against main's removed, moved and nested classes

Every branch-added file in this layer was checked for its imports and the members it calls on classes that main deleted, moved or nested. In this layer, the only imports of classes that main removed are `PengRobinson78` and `PhaseRoot` (S1b and S32). The other breakages are members that main removed or renamed (S1a, S9, F2, F8, S33, S34). Branch-added classes that call only same-package or branch-added code (`ReferenceSpine`, `IdealGasFunction`, `JaegerSpanGibbsFunction`, `CrystalReference`, `CrystalInventory`, `TangentPlaneStability`, `PhaseState`, `PhaseContract`, `GroupContributionInteractions`, `PairInteractions`, `WaterRegion1`, `FluidDomain`) compile against main unchanged.

| file | branch change | main change | disposition | proving tests |
|---|---|---|---|---|
| `science/fluid/thermo/SpinePackageData.java` (branch-added) | Computes the anchored liquid Z of non-spine components with `PengRobinson78 raw.evaluate(t, pRef, pure, PhaseRoot.LIQUID)`. Uses `PengRobinson78.GAS_CONSTANT`. Reads `MaterialFluidData.VolumeReference`. | S1b moved `PengRobinson78` and `PhaseProperties` to the test tree. `HydrocarbonModel` has the private static `pureLiquidCompressibility(ThermoComponent, double kcc, double t, double p)` plus `realCubicRoots`, a bitwise copy of the PR78 expressions. S32 replaced `PhaseRoot` with `PengRobinsonKernel.Root`. S33 nested the volume reference as `MaterialCatalog.FluidData.VolumeReference`. | **R**. Call `HydrocarbonModel.pureLiquidCompressibility(component, kcc, t, p)` (raise it to package-private). Use `FluidThermodynamics.R`, `Root.LIQUID` and `catalog.fluidData(...).volumeReferences()`. G2 exact stays bitwise because main's copy is bitwise PR78. | `SpineNetworkPathTest`, `SpinePackageEvaluatorTest`, `MethaneSpineTest`, `PilotVolumeAnchorTest`, `PilotCryogenicCatalogTest`, G2 `fluidSolverRegression -PfluidRegressionMode=exact` (after the re-capture of part D). |
| `science/fluid/thermo/HydrocarbonModel.java` | `PengRobinson78 raw` field, used for the anchored liquid volume at its own (T, P) (959ea0d9). `PhaseRoot` in signatures. | As above (S1b, S32). | **R** inside the **D** of part C: the same `pureLiquidCompressibility` replacement, with `PhaseRoot` becoming `Root`. | `HydrocarbonModelTest` (main's merged form plus the branch rows), `DirectLiquidContinuityTest`. |
| `science/thermo/PengRobinsonKernel.java` | WP1 (43d10812): plan `TEMPERATURE_DEPENDENT_PAIRS`, the constructor with `PairInteractions`, `interactions()`, `prepareInteractions`, workspace `pairK/pairKDt/pairKDt2` with `pair*View()`, `addInteractionTemperatureTerms`, and `crossA` through `pairIndex`. Branch tests call `mixing()`. | S1a and S9 removed `mixing()` and `evaluatePair`. `DeterministicMath` log, exp, cbrt, acos and cos. | **R** (part C). Tests that call `mixing()` (`TranslatedPengRobinsonPairInteractionsTest:66,191`, `PengRobinsonKernelPairInteractionsTest:78`) assert on `interactions()` or on the plan through results instead. | `PengRobinsonKernelPairInteractionsTest`, `TranslatedPengRobinsonPairInteractionsTest`, `PengRobinsonKernelRootPrecisionTest`, column `V3PengRobinsonKernelTest`. |
| `science/fluid/thermo/TranslatedPengRobinson.java` | `residualOnly(...)`, the `PairInteractions` and spine constructors, `evaluateValues`, `kernel()`, `translation(i)`, `spineVariant()`, `idealGas(i)`, `datumOffset(i)`. `Values.phaseIdentificationParameter()` and `physicalRootCount()` replace `vaporBranch`. `Phase` record gains a last field `phaseIdentificationParameter`. Uses `newDerivatives()`, `differentiate(...)` and `Derivatives`. | F2 deleted `Derivatives`, `newDerivatives` and `differentiate`. Main has its own `evaluateValues` with the same semantics. R is `FluidThermodynamics.R`. Roots are `PengRobinsonKernel.Root`. `DeterministicMath.log`. | **R** plus an **OWNER DECISION** (F2, part C). | `TranslatedPengRobinsonDerivativesTest` (if restored), `TranslatedPengRobinsonTest`, `CubicPhaseEvaluatorTest`, `FluidTpEquilibrium*Test`, `G3F8ConservationAndDerivativeTest`. |
| `science/fluid/thermo/CubicPhaseEvaluator.java` (branch-added) | `derivatives(...)` calls `eos.newDerivatives()`, `eos.differentiate(...)`, `Derivatives.values()`, `logFugacityCompositionDerivative(i,j)`, `logFugacityTemperatureDerivativeView()` and `logFugacityPressureDerivativeView()`. `FluidTpEquilibrium` calls it at lines 1275, 2723 and 2757 (Newton finish, critical band). | F2 removed every callee. | Follows the F2 **OWNER DECISION**. Otherwise **T**. | `NetworkPhaseEngineTest`, `FluidTpEquilibriumTest`, `FluidTpEquilibriumP3Test`, G4/G5 families. |
| `science/fluid/thermo/NetworkPhaseEngine.java` (branch-added) | Uses `fluid.hydrocarbon().translated()`, `catalog.fluidValidity`, `CubicPhaseEvaluator.forPackage/fromPackage` and `PhaseContract`. | No removed dependency: `fluidValidity` survives S6. | **R** for determinism only (part B). | `NetworkPhaseEngineTest`. |
| `science/material/MaterialCatalog.java` | Readers use the private JSON helpers (`string`, `number`, ...) and `MaterialFluidData.VolumeReference`. The helpers became package-private for the spine and crystal readers. | S34 added one reader, `JsonFields` (`text`, `number`, `positive`, `object`, `array`, `bool`, `strings`, `numbers`, `integer`, `MAX_TEXT=2048`), via `import static`. S33 nested `FluidData`, `VolumeReference`, `FluidAppearance` and private `Authoring`. S6 removed `propertyPhysicsFingerprint`, `columnSideDrawRates`, `columnFeedStandardVolume` and `packagePreset` (no branch caller). | **R** (part C). In the branch readers (spine, crystal, group interactions, volume translation), `string` becomes `JsonFields.text` and `MaterialFluidData.VolumeReference` becomes `FluidData.VolumeReference`. | `MaterialCatalogTest`, `PilotCryogenicCatalogTest`, `GroupContributionInteractionsTest`, `CrystalReferenceTest`, `CrystalPackageConsumersTest`. |
| `science/fluid/thermo/FluidThermodynamics.java` | Branch constructors `(catalog,pkg)`, `(cat,pkg,v,policy)`, `(cat,pkg,v,policy,solid)` plus two `@Deprecated` 3-arg shims. Static `hydrocarbonDerivatives`. | F8 left one constructor, `(catalog,pkg,k,v,policy,solidSettings)`. F2 removed `hydrocarbonDerivatives`. Fields `SlurryTransport.Settings`. | **D** (part C). | Part C. |
| Test tree, 36 branch test files | `PhaseRoot.*` and test-tree `PengRobinson78` through `LegacyLiquidPath` (fluid/support). | Test-tree `PhaseRoot` deleted (bbc23f70). Test-tree `PengRobinson78` kept, on `Root`. | **R**: mechanical `PhaseRoot` to `Root` rename. | Compile of the test source set. |
| Test tree: `new FluidThermodynamics(cat,pkg[,k])` and `forNetwork(cat,pkg,k...)` | 37 branch call sites without k. | 75 call sites in 43 main test and GameTest files pass k. Main added `FluidTestSupport.model(catalog,pkg,k)`, `chargedBoundary`, `nitrogen` and `water`. | **R**: keep main's helper with k removed (`FluidTestSupport.model(cat,pkg)`). Branch sites call the helper. Main's 75 sites drop k. | Compile, then the touched suites. |
| Tests that use derivative wrappers | `DirectLiquidJacobianRowsTest` (science/fluid/solver), `DirectLiquidContinuityTest:182,217`, `G3Support:100-101`, `FluidNitrogenCryogenicTest:73,102`, `LegacyNetworkPathPinTest:102,116`, `SpineNetworkPathTest:427-434`. | F2 removed `HydrocarbonModel.newDerivatives/differentiate` and `FluidThermodynamics.hydrocarbonDerivatives`. Main rewrote `FluidNitrogenCryogenicTest` without derivatives. | Follows the F2 **OWNER DECISION**. Under option (a) these tests call `TranslatedPengRobinson.differentiate` directly (via `model.hydrocarbon().translated()`). `FluidNitrogenCryogenicTest` takes main's version. | Those six classes. |

Effort: **medium**. About eight production files, two of them (TPR and the evaluator) gated on F2. The test edits are mechanical.

---

## B. Determinism: direct `Math` and `StrictMath` calls in branch production code

Method: a word-boundary scan `(^|[^A-Za-z0-9_.])(java\.lang\.)?(StrictMath|Math)\s*\.` with comments stripped, run over `science/` and `runtime/` on the branch HEAD and on main. Only the functions that `DeterministicMathSourceTest` bans were counted: exp, expm1, log, log1p, log10, pow, cbrt, trig and hyperbolic functions. Main shows 0. The branch has no static imports of `Math` and no `StrictMath` calls.

Base-era calls in files that both sides modified (`PhaseLayout` 21, `PengRobinsonKernel` 14, `TranslatedPengRobinson` 3, `MixtureViscosity` 2, `FluidThermodynamics`) are already converted on main and auto-resolve or resolve in part C. The table lists only branch-added calls, which survive the port as banned calls.

| file | branch change (banned calls) | main change | disposition | proving tests |
|---|---|---|---|---|
| `FluidTpEquilibrium` | 94 calls: exp 32, expm1 2, log 59, pow 1 (line 4472 `Math.pow(10.0, attempt-9)`) | facade has exp, expm1, log and pow | **R**: mechanical, to `DeterministicMath.*`. The pow at 4472 is an integer power of ten, so it could become a constant table; `DeterministicMath.pow` is also fine. | `DeterministicMathSourceTest`, `FluidTpEquilibrium*Test` (self-consistency), G4/G5 classification counts |
| `ReferenceSpine` | 20 calls: exp 4, expm1 3, log 7, log1p 1, pow 3 (455, 469, 483), tanh 2 (476, 490: GERG cosh terms) | facade lacks **tanh** | **R** plus a facade addition: `DeterministicMath.tanh` delegating to `StrictMath.tanh`, added first (main's rule: "a new caller adds its function to the facade first") | `ReferenceSpineTest`, `MethaneSpineTest`, main's `DeterministicMathTest`, `G3F1PureFluidOracleTest` (tolerance) |
| `JaegerSpanGibbsFunction` | 15 calls: atan 8, exp 1, log 4, pow 2 (107, 218) | facade lacks **atan** | **R** plus a facade addition: `DeterministicMath.atan` delegating to `StrictMath.atan` | `JaegerSpanGibbsFunctionTest`, `GasSolidEquilibriumTest`, `G4F1CarbonDioxideFrostPointHoldoutTest` |
| `CrystalReference` | 6 calls: log 2, pow 4 (494, 500, and two more) | - | **R** | `CrystalReferenceTest`, `CrystalPhaseEvaluatorTest` |
| `TangentPlaneStability` | 6 calls: exp 1, expm1 1, log 4 | - | **R** | `TangentPlaneStabilityTest` |
| `NetworkPhaseEngine` | 5 calls: log 4 (Shomate steam entropy, CoefficientIdealGas entropy), pow 1 (`powers()`) | - | **R** | `NetworkPhaseEngineTest` |
| `GroupContributionInteractions` | 3 calls: pow 3 (92, 99, 106: (298.15/T)^(B/A-1)) | - | **R** | `GroupContributionInteractionsTest`, `PilotCryogenicCatalogTest` (JSON pins unaffected) |
| `PairInteractions` | 1 call: pow (154) | - | **R** | `PengRobinsonKernelPairInteractionsTest` |
| `CubicPhaseEvaluator` | 1 call: log | - | **R** | `CubicPhaseEvaluatorTest` |
| `PhaseState` | 1 call: log | - | **R** | `FluidTpEquilibriumTest`, `FluidTpEquilibriumPhUvTest` |
| `FluidThermodynamics` | 1 call: log (`shortTieLine`) | - | **R** inside the part C **D** | `FluidThermodynamicsTest` |
| `MixtureViscosity` | 1 call: log (new line `prepared.liquidLog[component] = Math.log(mu)`, 8e5274ea) | rest converted | **R** | `MixtureViscosityTest` |

Total after the port: **154 banned calls in 12 files**. Functions needed: exp, expm1, log, log1p and pow (present), plus **atan** and **tanh** (missing from the facade).

Consequences for pins:

- `exp`, `log`, `expm1` and `log1p` in the facade delegate to `StrictMath`, which is fdlibm. On HotSpot x86-64, `Math.log` and `Math.exp` are intrinsics that can differ from fdlibm by 1 ulp. Every converted call can therefore move results by ulps.
- `DeterministicMath.pow` is `DeterministicPow` (within 1 ulp of fdlibm, not bitwise `StrictMath`), so the 13 pow sites move by ulps too.
- Absolute pins that need **only a re-pin** after the conversion:
  - `LegacyNetworkPathPinTest.DIGEST = "778b87e5..."`. It also hashes the derivatives (F2), and after the conversion it becomes JDK-independent.
  - Runtime P12 and P31 fingerprints.
  - GameTest prints.
  - `chain-100.json`, which is re-captured for D9 anyway (part D).
- Unaffected:
  - Self-consistency bitwise tests: workspace reuse and path equivalence in `SpineNetworkPathTest`, `NetworkPhaseEngineTest`, `FluidTpEquilibrium*Test`, `TangentPlaneStabilityTest`, `ReferenceSpineTest` and `HydrocarbonModelTest:25`. Both sides of each comparison use the same functions.
  - `PilotCryogenicCatalogTest` SHA pins, which hash JSON, not results.
- Expected unchanged, to be confirmed by the gate run: tolerance-based families (G3, G4, G5, CoolProp oracle parity). One exception needs the run to confirm it: the classification counts that could flip on an ulp near a decision threshold (G4F2 "at most 14 holds", G5F6 "four THREE_PHASES", G4F1 holds).

Effort: **medium**. The conversion is mechanical, but 154 sites is a large count. Two facade functions and their `DeterministicMathTest` rows are needed. Then one gates run classifies any ulp-sensitive counts.

---

## C. Conflicting production files

| file | branch change | main change | disposition | proving tests |
|---|---|---|---|---|
| `science/thermo/PengRobinsonKernel.java` | WP1 E-PPR78 kij(T): plan `TEMPERATURE_DEPENDENT_PAIRS`, the 8-arg constructor with `PairInteractions`, `interactions()`, `prepareInteractions`, `pairK/pairKDt/pairKDt2` workspace and `pair*View()`, `addInteractionTemperatureTerms`, `crossA` via `pairIndex`. Legacy plans stay bitwise. | `DeterministicMath` (log, exp, cbrt, acos, cos). S1a and S9 removed `mixing()` and `evaluatePair`. | **R**. Re-apply the WP1 hunks; any new transcendental call goes through `DeterministicMath`. **OWNER DECISION (minor)**: `interactions()` and `pair*View()` are test-only on the branch, and main's F8 and S1a removed test-only members. Checked: there is no production reader. `kernel().interactions()` is read only by `SpineNetworkPathTest:62` and `TranslatedPengRobinsonPairInteractionsTest:192`. `pairInteractions*View()` is read only by `PairInteractionsTest` (lines 127 to 285), which compares kij(T) and its first and second T-derivatives against the group-contribution reference. Options: (a) keep both accessors as a named exception; (b) rewrite on results (`a_mix` and its T-derivatives against a hand-computed mixture). Recommendation: keep the `pair*View()` workspace views, because the kij(T) derivative proof has no cheaper observable. Replace `kernel().interactions()` with `SpinePackageData.interactions()` or the package record in those two tests. Rewrite the `mixing()` callers on `interactions().temperatureDependent()`. | `PengRobinsonKernelPairInteractionsTest`, `TranslatedPengRobinsonPairInteractionsTest`, `PengRobinsonKernelRootPrecisionTest`, column `V3PengRobinson*Test` (legacy plans bitwise) |
| `science/fluid/thermo/TranslatedPengRobinson.java` | Translation per component, `residualOnly`, the `PairInteractions` and spine constructors (ideal gas, datum offsets for D10), `evaluateValues`, accessors, PIP and `physicalRootCount` in `Values` (WP7c replaces `vaporBranch`). `Phase` gets `phaseIdentificationParameter`. Uses the analytic derivative chain. | R from `FluidThermodynamics.R`, `Root`, its own `evaluateValues` (same semantics), `DeterministicMath.log`. **F2 removed `Derivatives`, `newDerivatives` and `differentiate` (owner decision 2026-10-15, "Remove F2 analytic derivatives"; on main they had no production caller).** | **R**, plus the **OWNER DECISION F2**. On the branch, `FluidTpEquilibrium` (1275, 2723, 2757) uses the analytic derivatives in production (Newton finish, critical band), so the reason for F2 does not hold for the ported code. Options: (a) restore a minimal `TranslatedPengRobinson.newDerivatives/differentiate/Derivatives` with the engine as the only production caller, and no `HydrocarbonModel` or `FluidThermodynamics` wrappers; (b) move the translation chain into `CubicPhaseEvaluator` on `kernel.evaluateDerivatives`, so TPR keeps main's F2 shape; (c) finite differences in the engine (rejected: Newton and critical-band accuracy). Recommendation: **(a)**. It is closest to the reviewed branch code, keeps the derivative next to the translation it differentiates, and lets `TranslatedPengRobinsonDerivativesTest` return as the proof. (b) is equivalent in size and is the choice if the owner wants TPR to stay derivative-free. | `TranslatedPengRobinsonDerivativesTest` (restored under a), `CubicPhaseEvaluatorTest`, `FluidTpEquilibriumTest`, `G3F8ConservationAndDerivativeTest`, `NetworkPhaseEngineTest` |
| `science/fluid/thermo/HydrocarbonModel.java` | Constructor `(catalog, packageId)`. Liquid evaluated at the state pressure (WP4, D1/D9), with anchors at their own (T, P). Spine path (`SpinePackageData`, `ThermoIdentity`, `NETWORK_FAMILY`). `FORMULATION = "direct-liquid-v1:tp-engine-v1"`, revision `fluidThermoFingerprint + ":" + FORMULATION + ":fluid-domain-data-v1:cp-segments-v1:catalog-volume-reference-v1"`. `LiquidRootAbsent`. `Phase(molarVolume, molarEnthalpy, molarInternalEnergy, volumePressureDerivative, logFugacity, vaporLike, liquidRootAbsent)` with `isothermalCompressibility()`. `phase(t,p,amounts,root,terms,vaporCoexists)`. `checkPhaseTemperature(t,amounts,root==VAPOR)`. Derivative wrappers. | `REFERENCE_PRESSURE = 2e6`. `GlobalLiquidResponse liquidResponse` field and constructor `(catalog,pkg,compressibility)`. S1b private `pureLiquidCompressibility` plus `realCubicRoots`. `logFugacityInto` (allocation-free Wilson flash, 0.7.0). `checkPhase`. 6-field `Phase(..., vaporBranch)`. Revision `...:fluid-shared-k-v1:...:k=<hex>`. | **D**: the branch model on main's structure. Keep the branch formulation, revision and `Phase` record. Replace `raw.evaluate(...)` with `pureLiquidCompressibility` (package-private, shared with `SpinePackageData`). `PhaseRoot` becomes `Root`. Drop `REFERENCE_PRESSURE`, `liquidResponse` and `logFugacityInto` (**X**: the Wilson flash is retired by WP7). Do not carry the derivative wrappers (F2, either option). Callers outside this layer: main's `PassiveStepSolver:2063` `vaporBranch()` becomes `vaporLike()` (solver-layer port). | `HydrocarbonModelTest` (main's M15 merge of `HybridDerivativeTest` and `GlobalLiquidResponseTest`, with the branch rows; the `GlobalLiquidResponse` section dropped), `DirectLiquidContinuityTest`, `SpineNetworkPathTest`, `LegacyNetworkPathPinTest` (re-pin) |
| `science/fluid/thermo/GlobalLiquidResponse.java` | Deleted (WP4; plan P3 "delete the 2 MPa reference path and the global compressibility"; D9, D12). | `DeterministicMath` exp, expm1 and log, plus `logFugacityInto` support (0.7.0). Consumers: `FluidThermodynamics.isothermalAcousticBound` (main line 159) and `phaseVelocityLimit` (main line 341, phase ports) read `liquidResponse.compressibilityPerPascal()`. `waterLiquidRaw` and `Prepared.referenceWater`. Config `fluid.liquidCompressibility` (`CreateChemE.java:96`, `FluidOptions`). | **X** for the file. Main's edits serve only the retired path. Main's consumers need a **D**, in `FluidThermodynamics` below. Removing the config key and the `FluidOptions` field is runtime-layer scope. | Compile, `FluidThermodynamicsTest` |
| `science/fluid/thermo/FluidThermodynamics.java` | Water at the state pressure through IF97 Region 1 (D12); Region 1 for the water datum and the pump reference density (997.05 kg/m3). `State` gains `waterCompressibility` and `CrystalStock crystals`, plus `nodeVolume()`. Engine adapter `flash(...)`, `phaseCheck(node, converged, checkpoint)`, `shortTieLine`. Crystals: `equilibrateCrystals`, `withCrystals`, `crystalUv`. `coexistence`, `vesselSplit` (P6, dd3da296), `onePurePhase`, PhaseHold types. Constructors and `forNetwork` without k; two `@Deprecated` 3-arg shims. | Phase ports: `PhaseStream`, GAS=0/OIL=1/WATER=2, `foldTracePhases`, `PHASE_TRACE_FRACTION = 1e-9`, `carriedGas`, `carriedTrace`, `foldedHydrocarbonPressure`, `folded(state)` and `folded(state, junction)` (D33, D34, D41). `flashTP(t,p,overall,checkpoint,prepared) = folded(equilibriumTP(...))`. Wilson flash with `logFugacityInto`. F8 left one constructor `(catalog,pkg,k,v,policy,solidSettings)`. `forNetwork` overloads `(cat,pkg,k)`, `(cat,pkg,k,v)`, `(cat,pkg,k,v,cutoff)` and `(...,solidSettings)`. F2 removed `hydrocarbonDerivatives`. `State` keeps its 17-arg convenience constructor. | **D**. Keep main's phase streams, fold rule and API names. `equilibriumTP` = the branch engine adapter (`flash`, `phaseCheck`) without folding. `flashTP` = `folded(equilibriumTP)`, as on main. Main's phase-correction path calls the `phaseCheck`-shaped `equilibriumTP`, then `folded`. Crystals must pass through `folded` and `withSolidState` unchanged (the trace fold acts on fluid phases only). Acoustic bound: the branch form `waterVolume * waterCompressibility + liquidVolume * liquidProperties().isothermalCompressibility()`. `phaseVelocityLimit`: OIL uses the EOS kappa of the oil liquid, WATER uses `state.waterCompressibility()`. `waterLiquidRaw` and `Prepared.referenceWater` become the branch's `waterAt` and `Prepared.water()`. Overloads: main's set minus k, i.e. the branch shapes `(cat,pkg)`, `(cat,pkg,v,cutoff[,settings])` and the 5-arg constructor. **Do not** add `forNetwork(cat,pkg,double v)`: an un-migrated `forNetwork(cat,pkg,1e-9)` would bind silently as a velocity (WP4 note). Do not carry the `@Deprecated` shims (no-compat rule). `shortTieLine` uses `DeterministicMath.log`. | `FluidThermodynamicsTest` (both sides' rows), `FluidThermoDomainHoldTest`, `ThermoDomainViolationTest`, `AmbientCrudePresetTest`, `LiquidFullBubblePointTest` (vesselSplit), `CrystalPackageConsumersTest`, `CrystalIslandRuntimeTest`, main's `PhasePortTest`, `PhasePortPriorityTest`, `TracePhaseFoldTest`, `DirectLiquidDeterminismTest`, G2 (re-capture), G5 GameTests |
| `science/material/MaterialCatalog.java` | `Property.volumeTranslation`. `Package.spine`, `crystals`, `spineFingerprint`, `groupContributions` and `temperatureDependentInteractions()`. Record kinds `spine`, `crystals` and `group_interactions`. `ideal_gas_cp` optional under the D6 rules. `requireSpine`, `volumeTranslation`. `fluidThermoFingerprint` skips calibration points for anchored components. `physicsFingerprint` and `fingerprint` hash `groupContributions` only when present (legacy pins hold). `inferenceView` refuses crystals (`CrystalPhasesUnavailable`) and group rules. `validateInteractions(o,names,matrices)` with `rule`. | S33 nesting (`FluidData`, `VolumeReference`, `FluidAppearance`, private `Authoring`). S34 `import static JsonFields.*`. S6 removed `propertyPhysicsFingerprint`, `columnSideDrawRates`, `columnFeedStandardVolume` and `packagePreset`. Appearance required, no default (009cdf5c, S10 kept by the owner). | **R**: re-apply on main's structure (`text` for `string`, `FluidData.VolumeReference`). Records are the same on main, so the bundled pins hold: `PilotCryogenicCatalogTest.BUNDLED_PINS`, and the `tjl20_methane` revision `crude-regrouped-r1:dc4be6bf...` matches main's neural sidecar. Data item: see part E (CO2 appearance). | `MaterialCatalogTest`, `PilotCryogenicCatalogTest`, `FluidAppearanceTest`, `verifyMaterialIndex` |
| `science/fluid/thermo/NetworkPhaseEngine.java` | Branch-added WP7 engine front: tangent-plane stability, Newton finish, critical band, PIP labels, typed holds (`PhaseHold`, `CriticalBandHold`, `UnsupportedPhases`). | - | **R** (part B only) | `NetworkPhaseEngineTest`, G4/G5 |
| `science/fluid/thermo/SpinePackageData.java` | Branch-added spine data (D6, D13). | - | **R** (part A) | `SpinePackageEvaluatorTest`, `SpineNetworkPathTest`, `PilotVolumeAnchorTest` |
| `science/fluid/thermo/WaterRegion1.java` | `Workspace`, `evaluateLiquid`, `requireTemperature`, `METASTABLE_MARGIN_PASCAL = 2e6` (metastable liquid to psat - 2 MPa), `fill` (959ea0d9). | unchanged | **T** | `WaterRegion1Test`, `FluidThermodynamicsTest` water rows |
| `science/fluid/thermo/FluidDomain.java` | Crystal vapour floors: `vaporMinimumTemperature`, `crystalWidened`, `vaporRange`, `requireSplit`, 3-arg `checkPhaseTemperature`. | unchanged | **T** | `FluidThermoDomainHoldTest`, `G3F9DomainRefusalTest`, `GasSolidEquilibriumTest` |
| `science/fluid/transport/MixtureViscosity.java` | A supercritical component in the liquid slot takes its dilute vapour value, only where the base threw `PROPERTY_UNAVAILABLE` (8e5274ea). Revision suffix `:supercritical-liquid-slot-dilute-v1`. | `DeterministicMath` log and exp. | **R** (automatic merge). The new `Math.log` line converts (part B). | `MixtureViscosityTest` |

Effort: **large**, mostly `FluidThermodynamics` (899 branch lines against 419 main lines, two API redesigns: phase streams and liquid compressibility) and `HydrocarbonModel`. The other rows are small.

---

## D. Changes that move bundled-package results, and the references that move

| file | branch change | main change | disposition | proving tests |
|---|---|---|---|---|
| `HydrocarbonModel` (WP4) and `FluidThermodynamics` water (D12) | Direct liquid at the state pressure; water through Region 1 at the state pressure. Chain-100: liquid volume +1.03e-3, water -9.3e-4, moles 2.8e-5 (relative). | 2 MPa reference plus k = 1e-9 | Moves. **Re-capture chain-100** (D9 already requires it). | `fluidSolverRegression -PfluidRegressionMode=exact` after the re-capture |
| `FluidThermodynamics.flashTP` (WP7, WP7b, WP7c) | Stability-driven phase decision. Fixed-point wet partial pressure. PIP label instead of `vaporBranch`. Used to build chain-100 (main's `SolverRegressionHarness:100-101` calls `flashTP` twice per node at 350 K, about 150 kPa), boundary and initial charges, the outer check, seeds and pure-vessel coexistence. | Wilson flash | Moves (D9). | as above, G4/G5 |
| Pump reference density | 997.05 kg/m3 (Region 1) instead of 996.0. | 996.0 | Moves every pump head and flow print. | GameTest prints, pump tests |
| `fluid_domain` data | `pressure_max` 2 MPa to 10 MPa in `packages/tjl20.json` and `tjl20_nitrogen.json`, `properties/nitrogen.json` and `tjl20_methane.json`, plus evidence texts. The network package's `fluidThermoFingerprint` moves (f5e0178e to 95cd8e6d); physics and scientific pins do not move. | 2 MPa | Moves revisions and fingerprints (by design). Packets and checkpoints carry the revision, and there is no compatibility work. | `FluidPropertyReloadGameTests`, runtime P12/P31 fingerprints |
| `FluidThermodynamics.vesselSplit` (P6, dd3da296), `PassiveStepSolver` stagnant-junction donor rule (P6b, a2f48d48) | A liquid-full mixture vessel at the bubble point splits. A stagnant junction between two mixtures no longer cycles its donors. | - | Moves only cases with liquid-full mixture vessels or stagnant mixed junctions. The donor rule is solver-layer scope. db43b226 touches `TrBdf2StepSolver`, which main removed (**X**; re-apply to the backward-Euler path if the solver layer finds the equivalent). | solver-layer report |
| D10 datum, spine, WP1 kij(T), crystals | Pilot package `createcheme:pilot_cryogenic` only. | - | Legacy plans stay bitwise. **No movement** of bundled-package results. | `LegacyNetworkPathPinTest` (re-pin only for part B), `V3PengRobinsonKernelTest` |

References on main that move:

1. **chain-100 exact** (`src/test/resources/fluid/regression/chain-100.json`): moves, re-capture (D9). It is also a modify/modify conflict: take neither side, re-capture on the merged code.
2. **33 junction lines**:
   - LIQUID_JUNCTION lines move (water density enters m_J).
   - MIXED_GAS lines (pure methane and N2 gas at 350 to 400 K) are expected bitwise unchanged, because no liquid, water or flash enters them. Part B ulps in `log`/`exp` may still reach them through the gas fugacity, so confirm by run.
3. **`MIXED_GAS_COST`** (newtonSolves 339): expected unchanged; confirm by run.
4. **GameTest prints**: every print that involves a liquid, water or a pump moves.
5. **Fluid thermo fingerprints and revisions**: the network package's `fluidThermoFingerprint` and the `HydrocarbonModel` revision (new FORMULATION; the `k=<hex>` tail goes). `LegacyNetworkPathPinTest.DIGEST` re-pins. Runtime P12/P31 fingerprints re-pin.

Gating: technically feasible, because `HydrocarbonModel` already chooses the spine or coefficient path per package, so the old path could stay for bundled packages. It is excluded by the recorded rules:

- D9: the formulation changes apply to every network package;
- D12: rejects a reference-pressure water path;
- D1, and plan P3's "delete the 2 MPa reference path".

**Not recommended**, and no owner decision is needed.

Effort: **small to medium**. One re-capture, one junction-line comparison, re-pins, and an explanation of each moved reference in the review. The work is mostly measurement time in the serial lane.

---

## E. Tests that main deleted, merged or moved, and test resources

| file | branch change | main change | disposition | proving tests |
|---|---|---|---|---|
| `HybridDerivativeTest`, `GlobalLiquidResponseTest` | Branch rewrote `HybridDerivativeTest` for the direct liquid (two derivative tests). `GlobalLiquidResponseTest` deleted with the file. | Merged into `HydrocarbonModelTest` (893e3cf2, M15). | **R**: the branch's `HydrocarbonModelTest` rows plus the two `HybridDerivative` rows, written into main's merged `HydrocarbonModelTest`. Drop the `GlobalLiquidResponse` section (**X**). The derivative rows follow the F2 decision. | `HydrocarbonModelTest` |
| `TranslatedPengRobinsonDerivativesTest` | Trivial edits. | Deleted (F2). | Follows the **OWNER DECISION F2**: restored under option (a); under (b) its rows move to `CubicPhaseEvaluatorTest`. | itself, `G3F8ConservationAndDerivativeTest` |
| `EmptyWaterFillingAuditTest` | Dropped k only. | Deleted (T4/S23, dead pre-D54 audit). | **T** (main's deletion). | - |
| `FluidPropertyCoverageTest` | Dropped k. | T4 removed the per-package models. | **T** main's file, then drop k. | itself |
| `LiquidCompressionQualificationTest` | Not touched. | Deleted on main. | **T** (main's deletion). | - |
| `FluidNitrogenCryogenicTest` | Uses the derivative wrappers (73, 102). | Rewritten without derivatives (F2). | **T** main's file, then drop k. Re-check any branch-only assertion. | itself |
| `FluidThermodynamicsTest`, `FluidThermoDomainHoldTest`, `ThermoDomainViolationTest`, `AmbientCrudePresetTest` | Rows for water at state P, the engine, crystal floors; k dropped. | Phase-port rows, k kept, `FluidTestSupport` helpers. | **R**: union of the assertions on main's file, k dropped. Rows that pin `vaporBranch` or the 2 MPa reference are **X**. | themselves |
| Test-tree `PhaseRoot`, `LegacyLiquidPath` | 36 branch test files use `PhaseRoot`. `LegacyLiquidPath` uses test-tree `PengRobinson78`. | `PhaseRoot` deleted (bbc23f70). `PengRobinson78` lives in the test tree on `Root`. | **R**: rename. | compile |
| `FluidTestSupport` | Branch helpers without k. | Main added `model(catalog,pkg,k)`, `chargedBoundary`, `nitrogen` and `water`. | **R**: main's helpers with k removed. | compile |
| `src/main/resources/.../components/carbon_dioxide.json` | New component without `appearance`. | `readAppearance` requires `appearance` with `color` `#RRGGBB`, `transparency` and `estimated` (no default, S10 kept by the owner). `FluidAppearanceTest:18` asserts `estimated()` for every bundled component. | **OWNER DECISION (minor)**: the CO2 colour. Without it, `MaterialCatalog.bundled()` fails ("appearance: required field"). Options: (a) colourless gas tint like nitrogen (`#DCE8FA`, transparency 0.95, estimated true); (b) a distinct pale tint so CO2 is visible in the GUI. Recommendation: (a), because CO2 is colourless, and `estimated: true` is required by the test. | `FluidAppearanceTest`, `MaterialCatalogTest`, `PilotCryogenicCatalogTest` (it hashes the JSON, so its CO2 pin is re-pinned once) |
| `materials-index.json` | 13 bundled files added (component, crystal, group interactions, interactions, package, 4 properties, 3 spine). | Adds `presets/column_tia_juana_nitrogen.json`. | **R** (automatic merge). Merged total 93 entries, equal as a sorted set to the files present, so main's `verifyMaterialIndex` passes. | `verifyMaterialIndex` (Gradle) |
| Pilot test datapack `src/test/resources/materials/pilot-cryogenic/` | `index.json`, `networks/default.json` (override to `pilot_cryogenic`), 3 component fluid presets. | Formats unchanged. | **T**. Note that `pilot_cryogenic` has no `liquid_mixture` and relies on the zero-coefficient default (code-guide open item 1). | `SpineNetworkPathTest`, `CrystalIslandRuntimeTest`, `FluidCheckpointCodecTest` (pilot rows) |
| Test fixtures: `coolprop/` (6 fluids, parity, LICENSE, README), `gerg2008/pilot-binaries.json`, 14 solid-CO2 holdout TSVs | Branch-added, test-only. | Main moved its `fluid/reference` TSVs to `research/` (T7/S26). | **T**. Test-only, no index requirement. They are read by gate tests, so they stay under `src/test/resources` (the T7 move covered unread TSVs only). | `G3F1PureFluidOracleTest`, `G3F2GergBubblePointTest`, `G4F1CarbonDioxideFrostPointHoldoutTest`, `SolidCarbonDioxideReferences` (support) |
| `chain-100.json` | Branch re-capture (D9). | Main's re-capture after backward Euler and phase ports. | Re-capture on the merged code (part D). | G2 exact |

Effort: **small to medium**. Most of the work is mechanical: the `PhaseRoot` rename, k removal at 75 plus 37 sites, and union merges. The judgement items are the F2 test set, the CO2 appearance and the `HydrocarbonModelTest` merge.

---

## Summary of decisions needed

- **OWNER DECISION, F2 analytic derivatives** (main removed them on 2026-10-15; the branch engine uses them in production). Recommendation (a): restore a minimal `TranslatedPengRobinson` derivative API with the engine as the only caller, no wrappers, and `TranslatedPengRobinsonDerivativesTest` back. Alternative (b): move the chain into `CubicPhaseEvaluator`.
- **OWNER DECISION, CO2 appearance**. Recommendation: `#DCE8FA` / 0.95 / estimated true.
- **OWNER DECISION, test-only accessors** (main's F8/S1a removed such members): `PengRobinsonKernel.pairInteractions*View()` (read only by `PairInteractionsTest`), `PengRobinsonKernel.interactions()` (two tests), the `mixing()` callers, and `NetworkPhaseEngine.tpWithFreshWorkspace` (`NetworkPhaseEngineTest:99`). Recommendation: keep the pair views and `tpWithFreshWorkspace` as named exceptions, because they prove kij(T) derivatives and workspace independence and have no cheaper observable. Rewrite the `interactions()` and `mixing()` callers on package data.
- No gating (D1, D9, D12).
