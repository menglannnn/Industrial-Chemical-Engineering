# M1: the multiphase engine on main (engine and data, bundled-neutral) and D21

2026-10-15. Branch `claude/unified-thermo-port` (main `ffeafe8a`, 0.8.0) in worktree `coolprop-multiphase-thermo-37f6b0`,
porting `claude/coolprop-multiphase-thermo-37f6b0` at `9aaafd4` per `MAIN_ADAPTATION_PLAN.md` M1. Not pushed, not merged.
Tools, logs and probes: `tools/unified-thermo-port-m1/` (README).

## Commits
`348d06e1` DeterministicMath atan/tanh (StrictMath, pinned) · `2b31cb90` materials (pilot package, spines, CO2-I crystal,
E-PPR78 groups on main's catalog; CO2 appearance) · `49ad6968` kernel kij(T) · `efb222ae` Helmholtz oracle + CoolProp files
(`-text`) · `de34347c` engine (TangentPlaneStability, `science/thermo/phase/**`, SpinePackageData) + engine-level tests ·
`7a3ea017` D21.

## Ported, as disposed
PhaseRoot -> `PengRobinsonKernel.Root`; PengRobinson78 -> `HydrocarbonModel.pureLiquidCompressibility` (package-private);
record kinds on main's `MaterialCatalog`; kij(T) on main's kernel. OD-P2 as recorded in D22 (2): nothing restored,
`CubicPhaseEvaluator.derivatives` = `evaluateValues` + kernel `evaluateDerivatives` + the two translation terms; the
derivative rows moved to `CubicPhaseEvaluatorTest`, the sum x_i h_i row dropped. Test-only members removed (F8):
`TangentPlaneStability(kernel)`, `FluidTpEquilibrium(contract, evaluator[, settings])`, `Settings.withOuterSearch`,
`PhaseCompetition.withHydrates`, `MaterialCatalog.spines()/groupInteractions()`, `Package.temperatureDependentInteractions()`,
`PairInteractions.none/constant(matrix)/pairIndex/isGroupContribution/constantValue/GroupParameters/Builder.groupContribution`,
kernel `mixing()/interactions()`/pair views (tests keep reference helpers). Kept (C12, real behaviour): `conservationDefect`,
`frostTemperature`, `idealGasEntropy`, `packageFingerprint`. Oracle: the CO2 coexistence Newton stops on a 1e-10 pressure
step (with StrictMath bits it 2-cycled at +-6.9e-9 J/mol at 270 K / 315 MPa; the old 1e-15 ln p stop was below double spacing).

## Checks
- Tests: step scope 232/0; full `test` 1357/0 in 239 classes (at `de34347c`); after D21 `science.thermo.*`,
  `science.material.*`, `science.fluid.thermo.*` 264/0 in 49 classes.
- Printed numbers (lane, 29 engine classes vs 9aaafd4): G4F1, G4F2, G5F1-G5F5, crystal, solid, material tests equal at
  printed digits. Residual diagnostics differing in last digits return bit for bit with a Math-delegating facade
  (StrictMath vs Math). FluidTpEquilibriumTest/PhUvTest keep pre-WP7 rows (main's 2 MPa network window; a WP7 data
  overlay restores the branch's). Re-pins: none (no asserted pin moved). No classification count changed.
- Neutrality: G1 fluid suites 540/0 in 112 classes, 33 junction lines identical (MIXED_GAS_COST newtonSolves 339);
  G2 chain-100 exact max deviation 0 on moles, T, phase fraction, flow vs main's committed record (not re-captured).
  D21 cannot move them: nothing outside `science/thermo/phase` calls the engine.

## Deferred (they change bundled results or belong to later milestones)
M2: G3 families with the GERG-2008 fixture (G3F2), G5F6, SpinePackageEvaluatorTest, SpineNetworkPathTest,
LegacyNetworkPathPinTest, DirectLiquid*; the 10 MPa network data and its test rows (WP7/WP7b, `diffs/`); FluidDomain crystal
floors, WaterRegion1, MixtureViscosity, NetworkPhaseEngine, FluidThermodynamics/HydrocarbonModel network path, network free
water. M3: CrystalInventory/CrystalStock, the network's hold on CRYSTAL_STEP. M4: IdealGasFitUnavailable, column-neural refusals.

## D21: near-pure CO2 at its sublimation line (physics first)
Real life: N2 traces stay in the vapour (none enter dry ice) and dilute it by y = f N / n_V; the step moves by about y in
ln P, or 12 K times y in T at 101 kPa. Row A (1 m3, 249.14 mol CO2, 212.22 K, 381.70 kPa, 19.867 mol crystal) moves by ~1e-6.
The code failed (TP cannot resolve a region narrower than its 1e-9 drive). Now (option A, theta = 1e-6, before P5's
pseudo-binary rule): the pure step from the balances, traces in its fluid.
- Row A UV, N2 f = 1e-12..1e-6: converged VAPOR_SOLID in 1.3 k-2.8 k kernel calls (was failing after 7.7 k-57 k);
  P +0.52 Pa at 1e-6 (1.37 f), crystal -4.3e-5 mol (2.1 f). CH4: same convergence; +1.87 Pa because methane added at the
  same U brings its own (formation-based) energy, ~20 J, not because of the rule.
- PH 101.325 kPa, f <= 1e-6, 3 % to 95 % crystal: T = 194.627787659 K, the pure step's, 83-274 kernel calls.
- Above theta (2e-6) the general solve answers: row A within 1.2e-6 of the rule's pressure at 1e-6.
- Forced choice, the fluid's trace share bound Y = 1e-4 (`STEP_FLUID_TRACES`): deeper deposition is the general solve's
  (resolved, drive >= 1e-4); the handover moves T by <= 1.25 mK. Y = theta: row A at f = 1e-6 fails (30.9 k calls).
  Y = 1 (only "a fluid of nothing but traces" refused): the rule claims T_sub where the answer is 24-79 mK colder and
  alternates with the general solve along H(T).
- Triple point (structural): a UV target inside the triple-point triangle is answered before any search (cached corners
  screen, balances decide; the bracket test is gone): 2-14 kernel calls; pure G5F1 98 -> 0 TP equilibria, same amounts.
  Traces go to the larger fluid (smallest share, smallest error). Too little fluid (f = 1e-6, fluids 2e-3/3e-3 mol): typed
  CRYSTAL_STEP hold in 5 calls (before, at a target with 1.25e-3 mol vapour: 281 k calls; at f = 1e-7 a failure after 37 k).
- Option C: CRYSTAL_STEP prefixes a one-major feed's closed crystal step and the triple hold; tests `NearPureCrystalStepTest`.
- Engine classes after D21: printed lines identical except G5F1's TP-equilibria count.

## Proposals for the owner
1. Validate Y = 1e-4 and the triple point's larger-fluid carrier (alternative: split traces by the model's dilute K).
2. CRYSTAL_STEP stays for one-major feeds (pure ones' closed crystal steps now typed too); feeds above theta stay untyped.
3. Finding: PH at 101 kPa just above theta fails once (f = 2e-6, dh = -1000 J: 0.57 J step over 6e-10 K); 0 of 45 fail
   at 5e-6..1e-3. theta = 1e-5 (P6b's margin option) would cover it.
4. Finding (general path, not fixed): `innerUv` keeps a secant across a VS/LS step (same phase count) as `innerSlope`; the
   next inner search stalls 100 TP equilibria. Comparing classifications would fix it; it may move P5 numbers.
5. `7a3ea017`'s message says 1.37 f for N2 and CH4; it holds for N2 (CH4: see above).
