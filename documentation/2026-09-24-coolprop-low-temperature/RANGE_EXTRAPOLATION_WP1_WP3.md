# Range extrapolation, WP1 to WP3: report (2026-10-16)

**Physics first.** A fluid does not stop where a table ends: CO2 dissolved in LNG at 111.6 K is a solute in a liquid, nitrogen compressed past 300 K is still nitrogen, a liquid below its viscosity table keeps thickening (Andrade), and an ideal gas's c_p changes by a few per cent over a hundred kelvin. Expected: every state an island reaches is carried. States inside the data keep their numbers bit for bit. Outside, the properties continue smoothly (value and slope where the plan's C1 column says so), and the status line carries an advisory notice. Holds remain only for capability limits: phase sets the network cannot carry, and free liquid water below 273.16 K (interim ice hold until WP4). The only numerical rejection is a trial outside 10..6000 K and 1 Pa..1 GPa.

**What the code does now.**
- WP1: ideal gases (spines, both `shifted_polynomial_5` paths, Shomate steam) hold c_p at the end, with h and s integrating it. Below a liquid viscosity table, ln mu follows 1/T at the end slope; above the table, the end value is held. Vapour tables follow ln mu against ln T at both ends. Every viscosity is capped at exactly 1e12 Pa s. IF97 Region 1 and SR6 are held at 273.16 and 623.15 K, and Region 1's own sums continue below saturation and above 100 MPa. Wagner-Pruss continues itself; the dilute steam viscosity follows a power of T below 273.16 K. Crystal records hold c_p, dv/dT and dv/dP at their ends. `NumericalBox` refuses with a plain IllegalArgumentException.
- WP2: `ThermoDomainViolation` is deleted, and `ThermoHold` covers phase holds only. `FluidDomain` is the envelope and the input box of device settings. The engine grades such answers EXTRAPOLATED instead of OUT_OF_DOMAIN. Seed windows use the box, and the catalog's range rules are gone. The ICE hold is decided in `equilibriumTP` before the hydrocarbon phases. A PH or UV search starts inside the carried species' data and restarts warmer (×1.25, at most 8 times) when a start gets no answer.
- WP3: the worker reads the data ranges once per accepted interval, on its end state (`ProcessSolveServices.notices` → `FluidThermodynamics.dataRanges` → `DataRangeNotices`). The coordinator then appends one line to the FULL, APPROXIMATE or STEADY status, e.g. `; outside data range: Nitrogen ideal-gas heat capacity extrapolated above 300 K at reservoir at 4, 64, 0`. The line is saved with the status and restored from it. There is no new packet or format, no tick work and no solve on read. The device screen shows the notice after the status words.
- Found on the way, fixed in its stage: the seed helpers paired fluid-only amounts with a mass and enthalpy that included the solids (at stored energy). LNG under dry ice then seeded a pump's suction junction 1.3 % vapour, and the line closed. The CO2 seed window had masked this. Commit `333dd6e7`: the helpers now return the fluid alone, as `endStream` already did.

**Commits** (on `1d08ef1a`): `333dd6e7` seed streams, `a169fa85` WP1+WP2, `085676ea` WP3 + gate c, `90d68ca1` gate a. Each compiles main and tests on the index lane. Gate b is `DataRangeContinuationTest#aPressureBelowTheEnvelope...`, in `a169fa85`.

**Tests** (index lane, JDK 21.0.11, Gradle's `scheduler.verify` property): 83 classes, 446 tests, all passing at their last run. This covers every class that pinned the old refusals, the thermo, network, runtime, solids, checkpoint and hold-class suites, WorkerTrajectoryEquivalenceTest, DeterministicMathSourceTest and LegacyNetworkPathPinTest. Not run per brief: the full suite, gates.sh, quick checks and campaigns.

**Rule-off proofs** (`tools/unified-thermo-port-extrapolation/rule-off/run.sh`, replayed on `90d68ca1`):
- Gate a, continuations off: 3 methods of RangeContinuationGateTest fail, plus CrystalReferenceTest#segments (enthalpy slope 0 below the end against 52.2 and 25.0 above).
- Gate a, cap alone off: propane is 4.67e15 Pa s at 10 K.
- Gate b, box off in `state`: nothing is thrown.
- Gate c, notice reading off: no accepted interval carries a notice.
- Every rule was restored byte for byte. The worktree was never edited; a temporary index was used.

**Moved numbers, from physics.**
- LegacyNetworkPathPinTest is re-pinned. Probe points inside the data keep their bits (293.15, 350, 450 and 700 K).
  - 77, 110 and 150 K lie below the fits' 293.15 K end: these states are now answered with c_p held, where the translated EOS used to run the polynomial past its end.
  - A wet flash below 273.16 K is now the ice hold.
  - 450 K, 2 MPa wet sits past the steam's qualified partial pressure and is carried.
- Pilot flash grid: 620 → 684 states evaluated. The other 42 wet states below 273.16 K are ice holds, since water there is ice.
- P3 free water: 672/672 answered by both, against some refused before. Hot network states: 120 answered, 2 of them past the steam qualification.
- Held liquid water at 270 K is 2.1e-4 larger than at 273.16 K: dv/dT < 0 below 277 K.
- Slurry pump: the suction junction now starts as subcooled LNG at 111.65 K and 106.6 kPa (the tank plus its 1 m column) and draws 2.17435 kg/s, against 2.17436 at HEAD.

**GameTests touched but not run** (dev client running, so no Gradle): none were edited. These may read the new status suffix or the solids seed: FluidCheckpointGameTests, FluidPresentationGameTests, SolidPhaseGameTests. Test-world rows D23-1 F1/F5 and R10 should now run with a notice. One MCP GUI check of the notice line is still open.

**Code guide** (fluid-thermo, equilibrium-engine and fluid-runtime chapters):
- New: `NumericalBox` and `DataRangeNotices`.
- Deleted: `ThermoDomainViolation`.
- Changed: `FluidDomain`, `ThermoHold`, `EquilibriumResult` (EXTRAPOLATED), `PhaseDomain`, `FluidTpEquilibrium` (window, start, restart), `FluidThermodynamics` (ICE, `dataRanges`), `PassiveStepSolver` (box windows, fluid seed streams), `FluidIslandSolveResult.notices`, `IslandCoordinator` (notice on status), `FluidDeviceScreen`, `ViscosityCorrelation.MAXIMUM_VISCOSITY`, `MixtureViscosity.liquidData`, `TranslatedPengRobinson` (fit ranges), `WaterRegion1`, `V3WaterProperties.wagnerPruss`, `HydrocarbonModel.FORMULATION` (+`range-continuation-v1`: new revision, fresh world).
- Detached material: `tools/unified-thermo-port-extrapolation/`.

**Open items and options for the owner.**
- (1) Notice sources are each model's own data range plus the envelope. Option B: the component `fluid_domain` alone.
- (2) The seed-stream fix assumes a seed leaves the solids behind. The alternative is to seed the crystals too (moles plus u0), which needs a crystal-aware seed flash.
- (3) The start window and the warmer restart are search robustness for the box window. The alternative is a single restart from the envelope.
- (4) The `fluid_domain` evidence texts in the data still describe the old rule.
- (5) The engine does not grade liquid water outside Region 1 as EXTRAPOLATED; only the notice reports it.
- (6) The CO2-I 300 K crystal ceiling stays as an existence bound.
- (7) Still to do: WP4 (ice), WP5 (quick checks, campaign), the GUI check. On the index lane, FluidCheckpointFormatTest needs Gradle's verify property; it fails without it at HEAD too.
