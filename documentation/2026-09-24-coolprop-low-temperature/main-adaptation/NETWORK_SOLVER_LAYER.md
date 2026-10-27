# Network solver layer: porting the coolprop branch onto main ffeafe8a

Batch `2026-09-24-coolprop-low-temperature`, main-adaptation analysis, 2026-10-15. Read-only: no tracked file changed, no
Gradle run, no ref moved. Branch `claude/coolprop-multiphase-thermo-37f6b0` HEAD `9aaafd4`, base `f9d6be1`; main
`ffeafe8a` (0.8.0). Scope: `science/fluid/network/{PassiveStepSolver, PassiveIntervalSolver, InventoryEquilibrium,
ConservativeTransport, PassiveNetwork, TrBdf2StepSolver}`, `science/fluid/solver/PhaseLayout`,
`science/fluid/state/{CrystalInventory, CrystalStock}` and their tests. Line numbers: `PSS` = PassiveStepSolver, `PIS` =
PassiveIntervalSolver, `IE` = InventoryEquilibrium, `CT` = ConservativeTransport, `PN` = PassiveNetwork, `PL` =
PhaseLayout, `TB` = TrBdf2StepSolver; "branch NNN" is a line at `9aaafd4`, "main NNN" a line at `ffeafe8a`.

Dispositions: **R** re-apply mechanically; **D** redesign on main's structure (how is stated); **X** obsolete on main
(main's code cited). **OWNER DECISION** marks a choice that neither physics, main's code nor a recorded D-rule forces.

## 0. Summary

- **X (obsolete on main):** the stagnant-junction donor-cycle fix (`a2f48d4`), every TR-BDF2 hunk including the
  companion clip (`db43b22`), the pump-column rule E2 (`8e5274e`), the embedded-error branches of the interval solver.
  Main removed the frozen junction donor re-pass with the owned junction holdup (3dba4b02), deleted TR-BDF2 (640d87b6),
  and already states every mover column on its suction density (main PSS 4131-4137).
- **D (redesign):** the crystal equilibration (UV at V_max is only the taut half of main's D54 bag; a slack vessel needs
  PH at P_amb with the bag work booked), its hook in main's interval loop, its place before the rest test, the phase
  decision merge (`phaseCheck`, pure coexistence, `vesselSplit`) into main's D34/A85 `phaseCorrection`, crystal
  attachment of seeds (one per-pass normalisation instead of per-site patches), the typed-refusal plumbing (one supertype
  for `ThermoDomainViolation` and `PhaseHold`), `emptyingGraph` with crystals, the empty/dry notions for a crystal-only
  vessel, `InletPhase` held share, `Reservoir` constructor merge.
- **R (mechanical):** the crystal field of `Inventory`, crystals carried through `ConservativeTransport` and `flushTraces`,
  the frozen-crystal layout and node closures, the step's crystals-unchanged assert, the approximate refusal (renamed
  `ApproximationAnchor.Rejected`), the `PhaseHold` reason key, the two new state files.
- **Defects on main:** `a2f48d4` gone; `db43b22` gone, and D54 also changes rows B/D (the vessel cannot fall below the
  ambient, so the ambient water generator never feeds it); `dd3da29` cause unchanged in main's phase correction, outcome
  not decidable by reading (D34 folding changes the cycle); failure 2 (budget) and failure 1 (D21) persist once the thermo
  is ported, and main adds a PH (slack) variant of failure 1.
- **Crystal physics on main's features:** never drawn by a port; neither condensed nor liquid for `InletPhase`; no
  weight in the level head (a settled bed rests on the floor); occupies V_max in the wall row; a vessel whose fluid runs
  out keeps its crystal and is not empty; a supersaturated vessel is not at rest.
- **OWNER DECISIONS:** OD1 crystal-only vessel state, OD2 level head and crystals, OD3 crystal heat over an emptying step,
  OD4 form of the pre-rest equilibration, OD5 D21 (now also PH), OD6 `ScheduledTransfer` as the tests' heat input.

## 1. What main changed under the branch (the facts the port turns on)

| Main change | Where (main) | Consequence for the branch's network layer |
|---|---|---|
| Backward Euler, TR-BDF2 deleted (D4) | 640d87b6; PIS `run` 179-300, `STATE_CAP` 0.05 | No stages, no embedded companion, no `AccuracyRejection`; one Newton and one reconstruction per step |
| Owned junction holdup, donors no longer re-passed (D46) | 3dba4b02; `junctionInflow` PSS 4378-4389; `JUNCTION_INFLOW_FLOOR` gone | The donor-turn cycle of `a2f48d4` has no mechanism left |
| Every vessel closed to its booked inventory in each reconstruction (D36, G2) | CT 527-541 -> `IE.close` 87-112 | The node closures (energy, volume) are evaluated every step on main, not only at refresh: crystal terms must be in `PL.balanceRows` |
| Bag with a stiff wall, `wallRow`, `bagVolume`, wall work (D54, A423) | PL 270-315, `balanceRows` 342-353; CT 531-540 | A vessel below capacity stands at P_amb; the branch's "volume = V_max" closure is the taut branch only |
| Empty vessels, `holdsNothing`, `dry`, `emptyingGraph` (D54, D59 (2)) | PSS 2751-2856; PN `emptyVessel` 126 | A vessel whose fluid runs out within a step becomes a JUNCTION for the step; a junction refuses crystals on the branch |
| Rest test before refresh (D19, D42, D43) | PIS `solve` 72-73; NetworkReduction `atRest` 153-191 | A closed supersaturated vessel is "at rest" and never equilibrates |
| Trace flush at accepted steps (T1, 1e-24) | PIS `flushTraces` 322-345 | Builds a 4-field `Inventory` and a bare `flashTP` state: would delete crystals |
| Phase ports, `PhaseDraw`, `InletPhase`, level head A17 | 1dba69b3; PSS `portHead` 1933-1939; `InletPhase.heldShare` 147-153 | New readers of vessel volume, mass and phases |
| D34 trace-phase folding, A85 `foldedPhases`, `equilibriumTP` with prepared workspaces | PSS `phaseCorrection` 2054-2075, `foldedSeeds` 2080 | The branch's engine-based phase decision must be merged, not pasted |
| `ScheduledTransfer` has no runtime producer; tests only | code guide, open item 5 | The crystal tests' heat input depends on it |

## 2. Section A: branch hunks, their main location and disposition

### 2.1 PassiveStepSolver (`8e5274e`, `a3716ed5`, `0ea358a`, `dd3da29`, `a2f48d4`)

| # | Hunk / feature | Branch location | Main location | Disp. | Proving test |
|---|---|---|---|---|---|
| A1 | Stagnant junction: `stagnantTurns`, `STAGNANT_DROP` = 10, turned connections stated at zero for one solve, mode kept, warm-start memory (P6b `a2f48d4`; assumptions: factor 10, plain passive only, one-solve scope) | branch 210-260, 368-403, 679-703 | None. Main has no `donorsTurned`/`junctionsTurned` (removed in 3dba4b02); `junctionDonorFirst` (main 3263-3270) only keys workspaces and the Jacobian's `carries` (4393-4399); no inflow-floor switch | **X** | `StagnantJunctionTest` (both cases) on the ported main without A1 |
| A2 | Vessel seeds carry the inventory's crystals (`model.withCrystals(flashTP(...))`) | branch 786 (`initialPhaseSeeds`) | main `initialPhaseSeeds` 1112-1145; other vessel-state producers: `arrivingPhases` 1176ff, `phaseCorrection` 2054, `drainedSeeds`, `filled` reseeds, `polishRefusedPoint` | **D**: one normalisation per pass beside `foldedSeeds` (called at the pass start, main 494) re-attaches `inventory().crystals()` to every RESERVOIR/PORT seed; plus an invariant in `PN.Reservoir`: a vessel state carries its inventory's crystal stock. Per-site attachment is the patch form and would miss main's extra producers | `CrystalDepositionIslandTest` (c); a unit check on every pass seed of a crystal vessel |
| A3 | `PhaseHold` caught beside each `ThermoDomainViolation` and attributed to its node | branch 788, 867, 1178-1196 | main: 4 `violation.at` sites in PSS, 2 in CT, 1 in IE; seed sites that keep a guess on a domain refusal (`waterTraceSeed` 1148-1152, `closeColdReceivingGenerators` 2513) | **D**: give `ThermoDomainViolation` and `PhaseHold` one sealed supertype with `at(node)` (thermo layer) and catch that once per site; a seed-only site keeps its guess for both | `UnsupportedPhasesIslandTest` (holds name the node) |
| A4 | Phase decision by the engine: `phaseCheck(state, converged)`, `vaporLike()`, `shortTieLine` in the "all phases populated" shortcut (P3 WP7) | branch 1155-1175 | main `phaseCorrection` 2054-2075 (`equilibriumTP(..., prepared)`, `folded` D34, `foldedPhases` A85) | **D**: `phaseCheck` replaces `equilibriumTP` inside main's method; keep the prepared-workspace reuse (needs a prepared-aware `phaseCheck`), keep the D34 fold and the A85 record after it | `NearCriticalNitrogenIslandTest`, `TraceTruncationStepTest`, chain-100 exact re-capture |
| A5 | One pure hydrocarbon across its saturation line: the engine's UV coexistence, not the flipped slot (`pureHydrocarbonVessel`, `coexistence`) | branch 1180-1188, 1201-1204 | none on main | **D**: same place as A4 (converged, no change yet, RESERVOIR); taut vessels only, a slack vessel's coexistence is its TP answer at P_amb | `NearCriticalNitrogenIslandTest.aVesselOfPureNitrogenCondensesAndBoilsOutsideTheBand` |
| A6 | P6 `dd3da29`: a one-phase mixture vessel the check splits takes the engine's UV answer for its own mobile inventory as the two-phase seed (`vesselSplit`; crystals held out by `uvCrystalsHeldOut`) | branch 1189-1196 | none; main seeds from the TP split at the converged (T, P), then folds | **D**: in main's `phaseCorrection` before the D34 fold; taut vessels only (a rigid volume the TP vapour overshoots); a slack vessel keeps the TP seed at P_amb (the vapour inflates the bag). Keep only if section 3's evidence run fails without it (prefer simplification) | `LiquidFullBubblePointTest` (4) |
| A7 | Crystals unchanged across a step: `IllegalStateException` | branch 1327 (`checkConservation`) | main `checkConservation` 2871-2910, called with `original` (713) | **R**: first statement of the node loop | any step of `CrystalDepositionIslandTest` (b) |
| A8 | Layout built with the inventory's crystals (7-argument `PhaseLayout`) | branch 1560 (`Equations`) | main `Equations` 3205 | **R** | `CrystalDepositionIslandTest` (a), (b) |
| A9 | Pump connection column = suction density (P3 WP4 fix E2) | branch 1623-1631 | main 4131-4137 (3dba4b02, "suction density for every pump column") | **X** | `ElevatedBlockLineIslandTest` (main) |
| A10 | (new on main) small-headspace tolerance reads `vaporVolume()/volume()` | none | main 538 | **D** (small): divide by the node volume, so a vessel nearly full of crystal with a thin gas is a small headspace | `CrystalDepositionIslandTest` (c) |

### 2.2 PassiveIntervalSolver (`8e5274e`, `a3716ed5`, `0ea358a`)

| # | Hunk / feature | Branch location | Main location | Disp. | Proving test |
|---|---|---|---|---|---|
| B1 | Approximate interval refused for crystal models (D18; kept by P6b 2.4, measured) | branch 55 | main `solveApproximate` 79-89 | **R** as `ApproximationAnchor.Rejected("Crystal equilibria require a full solve")` | unit: `solveApproximate` throws for the pilot model |
| B2 | Equilibration after every accepted substep, both TR-BDF2 branches, inside the try (a refused split rejects the substep), FULL only | branch 134, 151, 207-217 | main `run` 243-246: `replace` -> `flushTraces` -> `markBackwardEulerAccepted` -> rest stop | **D**: one hook after `flushTraces`, before `markBackwardEulerAccepted` and the `atRest` stop, inside the try; it returns boundary transfers (the slack wall work, C3) appended like the flush's; outside `STATE_CAP` (D18 item 4: a projection, not a trajectory) | `CrystalDepositionIslandTest` (b), `CrystalMorphologyPolicyTest` |
| B3 | `PhaseHold` rejection counted under its own key | branch 175-179 | main catch 274-285 (`count`) | **R** through the A3 supertype | `UnsupportedPhasesIslandTest` |
| B4 | Embedded and refinement error branches carrying crystals | branch 131-151 | gone (state-change controller) | **X** | none |
| B5 | (new on main) the rest test runs before any refresh | none | main `solve` 72-73 | **D**: for a crystal model, refresh and equilibrate first; if any vessel moved, skip the rest shortcut and integrate (OD4) | `CrystalDepositionIslandTest` (a): on main without B5 a closed supersaturated vessel (no pipes) is at rest and never deposits |
| B6 | (new on main) the trace flush rebuilds inventories and states | none | main `flushTraces` 322-345 (Inventory 338, `flashTP` 333) | **R**: carry `inventory.crystals()` and `withCrystals` on the flashed state; the threshold's total stays the fluid total | unit: a crystal vessel with a 1e-25 trace keeps its crystal |

### 2.3 InventoryEquilibrium (`8e5274e`, `0ea358a`)

| # | Hunk / feature | Branch location | Main location | Disp. | Proving test |
|---|---|---|---|---|---|
| C1 | `refresh` equilibrates every non-empty RESERVOIR of a crystal model | branch 22-35 | main `refresh` 11-21 | **R**, plus C3's wall form | `CrystalDepositionIslandTest` (a) |
| C2 | `equilibrateCrystals(graph)` | branch 39-55 | none | **R**, returning the graph and its boundary transfers | as B2 |
| C3 | `crystals(...)`: engine UV of (species totals, U, V = inventory volume), then the network closure on the new inventory | branch 64-72 | main `solve` 24-45 (package-private, `workFrom`) | **D**: wall-aware. Taut: UV at V_max (the branch's answer). Slack: PH at P_amb with H = U + P_amb V_bag, and P_amb (V_bag,new - V_bag,old) booked as the ambient's boundary energy at the vessel, as CT 536-540 books a step's wall work. Complementarity: a UV answer below P_amb is redone as PH; a PH answer above V_max as UV. Forced by D54 and energy conservation, not an owner choice | new: 10 % CO2 in N2, 0.1 m3, charged at 170 K and 101.325 kPa (slack), deposits at P_amb; ledger closes with the work |
| C4 | "A vessel holding crystals and no fluid is not a network state"; engine `SOLID_PRESENT` refused | branch 77; FluidThermodynamics 588 | main `close` 89-93 (dry inert solids at T from energy, state's pressure) | **D**, OD1 | crystal-only vessel test (section 4, C-5) |
| C5 | Every candidate carries the crystals (`withCrystals` on each flash) | branch 87-104 | main `solve` 24-45, `close` 87-112, `layout` 116-118 | **D**: `layout()` takes the inventory's crystals; flashed candidates re-attached in `solve`'s loop | `NearCriticalNitrogenIslandTest.anInventoryInsideTheDomeRefreshesToTwoPhases`; (a) |
| C6 | Regime test by `phaseCheck`; one pure hydrocarbon inside its dome via `coexistence` | branch 102-118 | main `solve` loop 33-44 | **D**: merged with main's D34 `folded` start and the D59 (2) `holdsNothing` exit | `NearCriticalNitrogenIslandTest` |

### 2.4 ConservativeTransport, PassiveNetwork, TrBdf2StepSolver, PhaseLayout, state (`0ea358a`, `db43b22`)

| # | Hunk / feature | Branch location | Main location | Disp. | Proving test |
|---|---|---|---|---|---|
| D1 | Vessel inventory keeps its crystals | CT branch 267 | CT main 549 and `booked` 532 | **R** both | (b) |
| D2 | `repartition` keeps the crystal stock | CT branch 284-286 | CT main 628-644 (two returns) and the dry-vessel flash 410 | **R** | (b) |
| D3 | (new on main) vessel closure in every reconstruction | none | CT main 527-541 -> `IE.close` | **R** through G2 | (b), `LiquidFullBubblePointTest` |
| E1 | `Inventory` fifth field `crystals`, `speciesMoles`, `withCrystals`, equals, pre-crystal hash | PN branch 45-67 | PN main 145-153 | **R** | `FluidCheckpointCodecTest` (runtime) |
| E2 | 4-argument `Reservoir` constructor from node volume, node energy and crystals | PN branch 33 | PN main 135 (junction -> `unsizedJunction`) | **D**: junction branch as main, vessel branch as the branch | none |
| E3 | A junction refuses crystals | PN branch 34-37 | PN main 136 | **R**; the reason is now D46 (a buffer is not inventory), not "zero holdup" | emptying test (C-6) |
| E4 | `empty()` false while crystals remain | PN branch 41 | PN main 142 | **R** | crystal-only test |
| E5 | Crystal species must be a hydrocarbon index; zero-fluid energy rule admits crystals | PN branch 57-59 | PN main 147-151 | **R** | none |
| F1 | Stage and companion inventories carry crystals | TB branch 126, 137, 195 | file deleted (640d87b6) | **X** | none |
| F2 | Companion takes a negative amount at zero (P6b `db43b22`) | TB branch 174-179 | file deleted | **X** | `WaterIntoCryogenicMethaneTest` test 3 dropped |
| G1 | Frozen crystals in the layout; `decode` attaches them at the trial (T, P) | PL branch 31-32, 55-67, 213 | PL main 54-115, `decode` 238 | **R**: attach after `adoptingState` (238), before the solid projection | `PhaseLayoutTest`, (a) |
| G2 | Node closures: energy row `nodeInternalEnergy`, volume row `nodeVolume` | PL branch 248-250 | PL main `balanceRows` 342-353 (energy plus P_amb (V_bag - workFrom), `wallRow`) | **D** (rule S1): the energy row reads `nodeInternalEnergy()`; `wallRow`, `taut`, `wall`, `bagVolume` read `nodeVolume()`: the bag holds fluid and crystal. Readers that mean "fluid" (`InletPhase.heldShare`, `dryEnd`, `holdsNothing`) then subtract the crystal volume (section 4) | (b), slack test |
| G3 | `energyScale`, `solidScale`, `resolveTraces` read the mobile seed | PL branch 97-102 | PL main 97-101 | **R** (keep; scales only) | none |
| H1 | `CrystalInventory` (new) | state/ | none | **R** (no transcendental call) | `FluidCheckpointCodecTest` |
| H2 | `CrystalStock` (new) | state/ | none | **R**; may be nested into `CrystalInventory` in main's consolidation style (single-use type) | none |

## 3. Section B: the P6/P6b defects on main

| Defect | Mechanism (branch) | On main as it is | Once the branch thermo is ported | Evidence that settles it |
|---|---|---|---|---|
| P6 `dd3da29`: liquid-full mixture vessel at its bubble point | Converged one-phase liquid below its bubble pressure; the TP split at that (T, P) seeds 7.6 vessel volumes of vapour; the two-phase Newton hits its 20 iterations; at small steps the vapour is a trace read as one liquid, the pass repeats: cycle | Bundled packages reach it only with crude liquids holding light ends in a taut vessel (CH4/C2H6 liquids start at 293.15 K). Main's `phaseCorrection` still seeds from the TP split (root cause unchanged), but D34 folds the trace vapour and the A85 `foldedPhases` (part of the cycle key, main 511) forbids giving it back within the solve: the pass may converge on a metastable liquid and the next solve re-split. Cycle or crossing-with-lag cannot be decided by reading | The CH4 mixtures become reachable; same question | `LiquidFullBubblePointTest` fluid-only cases (CH4 + 2 % C2H6, CH4 + 2 % N2; 1 L, 172 K, 5 MPa, -200 W) on the ported main without A6, one run; port A6 only if they fail. Note: under D54 a vessel whose pressure would fall below P_amb goes slack, so crossings at bubble pressures under 1 atm are slack (TP seed right) |
| P6b `db43b22`: water into cryogenic methane, rows B/D | Water arrives only in TR-BDF2 stage two; the order-three companion (weight e2/alpha < 0) is a negative amount at every step size; untyped "Negative/nonfinite inventory" | **Gone**: no companion; BE has one implicit stage and the reconstruction re-books a would-be-negative component implicitly. **Scenario changed by D54**: the 1 m3 vessel starts at the kink (N2 at P_amb); cooling makes it slack at P_amb instead of sub-ambient, so the water generator at 101.325 kPa has no drive into it | The typed water-domain hold remains the outcome wherever water is driven into a vessel that falls below 273.16 K; the original rows should commit with no water entering | `WaterIntoCryogenicMethaneTest` rows on the ported main: assert no untyped refusal; original rows: commit and no water in the vessel; variant with the water generator at 1 MPa: typed `ThermoDomainViolation` (Water, below 273.16 K) naming the vessel |
| P6b `a2f48d4`: zero-flow junction donor cycle | Junction mixture frozen on its donor per pass; at equal vessel pressures the flow sits at the pressure rows' resolution and each donor gives it the other sign; `donorsTurned` re-passes until the cycle guard | **Gone**: no donor re-pass; the junction mixes its owned buffer with the live inflow (`junctionInflow`), continuous through zero flow; no `JUNCTION_INFLOW_FLOOR` (the branch's second stall mechanism) | Gone | `StagnantJunctionTest` (CO2/N2 and CH4/N2) on the ported main, without A1, ledger including junction buffers; main's own `MixedGasJunctionTransientTest` |
| P6b `4c21104` (failure 2): budget holds under kg/s flushes | Cold crystal UV after every accepted substep beside V+L (21,576 to 60,133 kernel evaluations each), 59 to 209 TR-BDF2 substeps per interval | Not reachable (no crystals) | **Exists**: still one crystal UV per accepted step; size depends on BE's step count under `STATE_CAP` 0.05 and the 20 s maximum, not on TR-BDF2's error control. The warm start (thermo) ports with it | `CrystalFlushBudgetTest` re-measured on main (bounds re-recorded; warm = cold to 1e-10 kept) |
| P6b failure 1 (D21): near-pure CO2 at its sublimation line | Absolute 1e-9 drive tolerance cannot resolve the vapour-solid region of one species with a trace 1e-12 to 1e-7: the nested UV closes on a step of V(P) or U(T) | Not reachable | **Exists**, unchanged by BE (section 5); main adds the slack (PH at P_amb) form | Row A rig: 1 m3, CO2 229.28 mol gas + 19.86 mol crystal + N2 2.34e-5 mol at 212.22 K, 381.7 kPa, fed by CO2 at 210 K / 400 kPa (the `P6bWorldProbe 47 1` island, `tools/p6b-runtime-failures/`) |

## 4. Section C: crystals against main's new features (physics first)

| # | Feature | What happens in a real vessel | Code answer on main | Disp. | Proving test |
|---|---|---|---|---|---|
| C-1 | Phase ports, `PhaseDraw` priority draws | A top outlet takes gas, a bottom outlet liquid; a CO2-I bed (1.56 g/cm3, sinks in LNG) rests on the floor. D20: no outlet carries it | `CrystalStock` is no phase: `PhaseDraw.held/order/Segment` capacities read GAS/OIL/WATER, `phaseSolidShare` inert solids only: crystals are never drawn. A crystal-only vessel would give `leading = -1` and `endDensity` 0/0: it must be a dry end | **X** for the draw, **D** for `dryEnd` (C-5) | VAPOR and LIQUID ports on the P5 liquid-solid vessel: streams carry the gas's and the liquid's x_CO2, crystal stays |
| C-2 | `InletPhase` refusal shares | The device ingests the fluid stream; the bed is in neither the condensed nor the liquid share of what it draws | `measure` reads mobile `mass`, `liquidMass`, `solids` and the drawn phase volumes: crystals excluded, correct. `heldShare` (dry suction) reads `bagVolume/V_max`: under S1 it would count crystal volume, so it becomes the fluid's share, `(bagVolume - V_cr)/V_max` | **X** shares, **D** held share (forced by D20) | compressor on a vapour-solid vessel not refused; pump on a crystal-only vessel refused "inlet dry" |
| C-3 | LIQUID port level head `g m_c H / V` | A granular bed's weight is carried by the floor (and walls); the pore liquid at the outlet carries only the liquid's hydrostatic head. The bed raises the liquid surface by its submerged volume, which needs a bed porosity (D20 declines to invent one) | `liquidMass` (main FluidThermodynamics 214-218) reads liquids, free water and inert `solidMoments`; crystals are outside it, so the head excludes them by default. A17 includes inert solids (suspended slurry); crystals are never suspended (D20) | **OD2** | LS vessel: head = g (m_liquid + m_inert) H / V, no crystal term |
| C-4 | Wall row, V_max | The crystal occupies the vessel; the fluid's room is V_max - V_cr; a slack bag holds fluid and crystal at P_amb | S1 (G2): `wallRow/taut/wall/bagVolume` on `nodeVolume`; the bag work then includes the crystal's thermal expansion | **R/D** | (a) volume closure to 1e-9; slack test |
| C-5 | Empty vessel rules: fluid runs out, crystal remains | Not empty: the bag collapses onto the bed at P_amb; the bed keeps its own temperature; above T_sub(P_amb) (pure CO2 194.7 K) it sublimes and the gas re-inflates the bag, below it a solid in a collapsed bag is stable | `empty()` false (E4); `holdsNothing` false while crystals remain (as for inert solids, main 2773-2779); fluid-level "gives nothing" for `dryEnd`, `dry` layout and `InletPhase` (fluid bag share at most `HOLDS_NOTHING`); state = solid-only state at P_amb and the temperature its energy gives, like main's dry-solids `close` (89-93), not the 298.15 K ambient; the equilibration then answers sublimation or `SOLID_PRESENT` | **D**, **OD1** | drain the liquid of a slack liquid-solid vessel through its LIQUID port to a lower void: crystal kept, state at P_amb, next interval sublimes or stays per T |
| C-6 | `emptyingGraph` | The departing fluid leaves the bed behind | Main stands the vessel as a JUNCTION holding `stock.internalEnergy() + P_amb V_bag` (2803-2815); a junction refuses crystals (E3). Redesign: the junction holds the fluid only, U_mobile = U_node - U_cr(T0, P0), work P_amb (V_bag - V_cr); `Emptying.drain` stays the fluid mass; the vessel's record keeps its crystals; the end state is the crystal-only state (C-5), not the ambient (main 731) | **D**, **OD3** | pump drawing a taut crystal vessel dry |
| C-7 | Rest (`NetworkReduction.atRest`) and certificates | A supersaturated vessel is not at rest (deposition proceeds); a vessel at its crystal equilibrium with no drive is | Main asks `atRest` before any refresh (PIS 72-73): a closed vessel charged below its frost point is "at rest" and never deposits. B5: equilibrate first, skip the shortcut if anything moved. With the 1e-10 deadband a settled vessel is a bitwise fixed point, so rest certification works unchanged. A crystal change is a node-solid change: no certificate (runtime, the branch's `IslandCertificate` rule) | **D**, OD4 | `CrystalDepositionIslandTest` (a) |
| C-8 | `STATE_CAP` | Deposition follows what the step did to the vessel | The equilibration stays outside the controller (D18 item 4); the next step measures from the equilibrated state; `stateChange` reads mobile mass, frozen across the step | **R** | (b) substep counts re-recorded |
| C-9 | `flushTraces` | A 1e-24 trace of the fluid is roundoff; the bed is stock | Carry crystals (B6); measure on the fluid total; flush before equilibrating, so the committed state is the equilibrium of the committed inventory. The CO2 vapour above a bed at the 90 K floor is about 1e-9 of N2 at 1 MPa, far above 1e-24 | **R** | B6 unit test |
| C-10 | `checkConservation` and ledgers | Species and energy are conserved through deposition | Inside a step crystals are frozen, the energy fields already include them, the bed's g z cancels: only A7. The interval ledgers must read `speciesMoles()` and crystal mass (`PortFixture.owned/ownedEnergy`, `FluidTestSupport.finiteLedger`, runtime `WorldTopologyLedger`). The equilibration gets its own audit in IE (species totals equal to 1e-12, energy field exact or less the booked work) | **R** | every crystal test's ledger |
| C-11 | `SolidEventIntegrator`, inert particles beside crystals (D20) | Particles may settle or be filtered; the crystal never becomes a particle; both share one temperature | SEI reads `SolidInventory` and filters only; its segments run through `run`, so B2's hook covers them; the particles' temperature bracket is thermo (`crystalUv`) | **X** | `CrystalMorphologyPolicyTest` particle case (on main it takes the SEI path) |
| C-12 | Junction holdup | A fitting holds a little fluid; D20 forbids crystals there | Junction refuses crystals (E3); its supersaturated mixture is stated fluid-only by the TP adapter | **R** | (c): no crystal in the junction |

## 5. Section D: D21 on main

- **Where it lives.** Options A and B are engine changes (`science/thermo/phase/FluidTpEquilibrium`: the UV and PH crystal
  solves beside `pureCrystalStep`, the pseudo-binary precedence). Option C has a network half:
  `FluidThermodynamics.equilibrateCrystals` maps the engine's typed non-convergence to a `PhaseHold` kind;
  `PassiveIntervalSolver.run` ends the interval at once on it, because it does not depend on the step size, and carries
  it as the typed cause (main's `Nonconvergence` carries only a `ThermoDomainViolation`, `domainCause` 352-355: the A3
  supertype generalises it); `IslandCoordinator` parks it as it parks a domain hold (main 624-637).
- **BE does not change the failure.** The failing UV inputs are the vessel's inventory after accepted steps; BE changes
  only which states are visited. Each failing equilibration is a rejection and a halving; twenty consecutive rejections
  end the interval (main 285), so the hold may come sooner than the branch's 2.7 s, but it is still an untyped
  numerical hold on the retry ladder.
- **Main adds a PH form.** A slack crystal vessel equilibrates at P_amb (C3). A near-pure CO2 vessel at the ambient sits
  at 194.7 K, where the vapour-solid band at fixed P has the same trace-relative width: option A must cover PH as well as
  UV.
- **Main's trace flush does not help.** It acts at 1e-24; the failing window was measured from 1e-12 to 1e-7, and a held
  island does not advance to dilute its trace. Raising the flush fraction to theta is option D in ledger form: it would
  book 1 ppm of real material as a boundary transfer, a fabricated removal. Still rejected.
- **Recommendation (OD5):** A with theta = 1e-6 for UV and PH, the traces carried in the fluid, and C as the typed
  fallback through the A3 supertype and the coordinator's domain-hold parking.

## 6. Section E: tests

### 6.1 Branch-modified tests main deleted

| Test | Branch change | Main replacement | Disp. |
|---|---|---|---|
| `TrBdf2Test` | model constructor only (1e-9 liquid compressibility argument removed, `959ea0d`) | `BackwardEulerStepTest` (640d87b6, b5ed4079) | **X** |
| `BoundaryAndPhaseTest` | constructor only | merged into `BackwardEulerStepTest` (893e3cf2) | **X** |
| `ScheduledTransferEstimatorTest` | constructor only | merged into `ScheduledTransferTest` (893e3cf2) | **X** |
| runtime `BufferedCommitTest`, `CausalModuleCoordinatorTest`, `ModuleTransferPlannerTest` | constructor or module edits | deleted with the causal modules (0.7.2) | **X** |

### 6.2 Branch-modified tests present on main

| Test | Branch change | Disp. |
|---|---|---|
| `EmptyNetworkTest`, `FlowControlTest`, `HydraulicMatrixQualificationTest`, `HydraulicReferenceQualificationTest`, `NetworkRegimeTest`, `PassiveStepSolverTest`, `PassiveTimeRefinementTest`, `PipeTransferTest`, `PumpJunctionStartupTest`, `ScheduledTransferTest`, `TransientQualificationTest`, `VelocityClampTest`, `ConservativeTransportTest`, `PhaseLayoutTest`, `TraceTruncationStepTest` | constructor only | **R** mechanical, after the thermo constructor port |
| `PumpRiseScalingTest` | `pumpReferenceDensity` 996.0 -> 997.05 kg/m3 (Region 1 at 1 atm), water compressibility 4.525e-10 /Pa | **R** on main's class; moves every pump limit by about 0.1 %: re-capture chain-100 |
| `SolidChainTransportTest` | step-count pins re-recorded under the direct liquid path | **re-record** on main (BE counts, not TR-BDF2's) |
| `SharedSourceDepletionQualificationTest` | fingerprint key "compressibility" -> "liquidPath" | **R** |
| `TraceTruncationLayoutTest` | water round trip to 4 ulp (P3 WP7) | **R** |
| `fluid/support/FluidTestSupport` | constructor without compressibility | **R**; `finiteLedger` also to `speciesMoles()` and crystal mass |

### 6.3 Branch-added tests

| Test | Checks that must survive | Main home | Port notes |
|---|---|---|---|
| `CrystalDepositionIslandTest` (10) | species incl. crystals to 1e-12; engine UV reproduced to 1e-9; energy and volume closures 1e-8/1e-9; (b) cooled and warmed back to 1e-9; (c) deposits only in the cold vessel; (d) LIQUID_SOLID; (h) VLS three-phase point; CRYSTAL_PHASES hold names its node; withdrawal stream at the gas's y_CO2 | new class in `science.fluid.network`, pilot model | (a) needs B5; heat input is `ScheduledTransfer.Injection` (OD6); re-record printed trajectories and substep counts, keep the invariants (they are integrator-independent: the end state is a function of the inventory); ledger via crystal-aware `PortFixture.owned` |
| `CrystalMorphologyPolicyTest` (6) | particle populations bit for bit and the engine answer for the rest; free water sublimes the crystal; water reaching a cold LS vessel is the typed domain hold; LS withdrawal takes the liquid; two crystals held typed; above 300 K the fluid-only answer | same package | particle case runs through `SolidEventIntegrator` on main: assert it still equilibrates at segment starts |
| `CrystalWarmStartTest` (3) | warm = cold to 1e-10; resting vessel a fixed point | same (or thermo) | none |
| `CrystalFlushBudgetTest` (1) | the flush rig commits; warm = cold to 1e-10 | same | checkpoint bounds re-measured (main's checkpoint sites differ) |
| `LiquidFullBubblePointTest` (4) | the vessels cross within 6 intervals; species to 1e-12; C2H6 vessel equals the engine's fluid-only UV to 1e-9; held-out UV fluid-only | same | first run without A6 is section 3's evidence |
| `NearCriticalNitrogenIslandTest` (5) | band crossing, dome refresh, bitwise repeatability with fresh workspaces | same | `InventoryEquilibrium.solve(..., false, ...)` -> `Double.NaN` (main's `workFrom`); align repeatability with main's `replayStart` rule |
| `StagnantJunctionTest` (2) | twelve intervals commit; vessels at one pressure; junction at rest; species conserved | same | keep as the regression that proves A1 is X; add junction buffers to the species ledger (D46: their composition moves) |
| `UnsupportedPhasesIslandTest` (3) | liquid-liquid and three-phase holds typed; dense gas beside liquid commits | same | none |
| `WaterIntoCryogenicMethaneTest` (3) | no untyped inventory refusal; typed water hold at the vessel | same | tests 1-2 re-derived for D54 (section 3); test 3 (TR-BDF2 stage two) **X** |
| `DirectLiquidJacobianRowsTest` (2) | liquid rows difference to the EOS at the state pressure | `science.fluid.solver` | compare on main's layout and block Jacobian (`BlockJacobianEquivalenceTest` style) |
| runtime `CrystalIslandRuntimeTest` (2) | bucket-only delivery; held status | runtime | 0.8.0: held islands accept fenced topology events, so "a queued fence stays pending" changes |

### 6.4 New tests the port needs (from sections 2 and 4)

Slack crystal vessel deposits at P_amb with the bag work booked (C3); closed supersaturated vessel deposits at its first
solve (B5); crystal-only vessel after a drain (C-5, OD1); emptying a crystal vessel (C-6); ports and inlet shares never
see crystals (C-1, C-2); level head without crystals (C-3); trace flush keeps crystals (B6). Shared rigs: crystal-aware
`PortFixture.owned/ownedEnergy` and `FluidTestSupport.finiteLedger` / `ConservationAssertions`.

## 7. OWNER DECISIONS

| # | Question | Options | Recommendation |
|---|---|---|---|
| OD1 | A vessel whose fluid ran out but holds crystals (slack bag at P_amb) | (a) a solid-only vessel state like main's dry inert-solids vessel (T from its energy, P_amb), engine `SOLID_PRESENT` accepted at slack; (b) keep the branch rule (typed refusal: no fluid beside the crystal), which holds the island whenever a crystal vessel is drained; (c) force a gas phase by sublimation (not physical below T_sub(P_amb)) | (a): the physical state under D54 (dry ice in a collapsed bag below 194.7 K) |
| OD2 | Crystals in the LIQUID port's level head | (a) none: the bed's weight rests on the floor; (b) add the liquid displaced by the submerged bed, rho_l g V_cr H / V while a liquid is present (needs a porosity D20 declines; a jump where the liquid appears, A17's objection); (c) the crystal mass, as A17 treats inert solids | (a): physically right on the weight, no invented porosity; the omitted displacement is tens of Pa at row E's 0.01 m3 of crystal |
| OD3 | The crystal's heat over a step in which the vessel's fluid runs out (C-6) | (a) set aside at its start state for that step, the equilibration after the step settles T; (b) couple it to the departing fluid (needs a new junction-with-solid form) | (a): the D18 frozen-in-step split, one step long |
| OD4 | Form of the equilibration before the rest test (B5) | (a) full refresh and equilibration of a crystal model before `atRest` (a warm UV per crystal vessel per slice, about 50 to 230 us; certified islands are not solved); (b) only vessels whose inventory changed since their last equilibration (new committed marker); (c) a cheap TP supersaturation check inside `atRest` | (a): no new state, fluid-only packages bitwise unchanged |
| OD5 | D21 (failure 1) | A, B, C, D, E of `P6B_RUNTIME_FAILURE_DIAGNOSIS.md` 1.4; on main also the PH (slack) form | A (theta 1e-6, UV and PH) plus C as the typed fallback |
| OD6 | The crystal tests' heat input is `ScheduledTransfer.Injection`, which main keeps for tests only (code guide open item 5) | (a) keep `ScheduledTransfer` as the tests' energy input; (b) a test-only heater; (c) heat through a generator stream (changes the composition) | (a) |

## 8. Dependencies on other layers

- **Thermo:** `phaseCheck` with main's prepared workspaces; `vesselSplit`, `coexistence`, `uvCrystalsHeldOut`;
  `equilibrateCrystals` with a PH-at-P_amb variant and the slack/taut complementarity; `withCrystals`, `nodeVolume`,
  `nodeInternalEnergy`; one sealed supertype for `ThermoDomainViolation` and `PhaseHold`; `science/thermo/phase` routed
  through `DeterministicMath` (88 direct transcendental `Math` calls on the branch; main's `DeterministicMathSourceTest`
  scans `science/` and `runtime/`).
- **Runtime:** checkpoint (branch format 6, main 8: the crystal block after the particle populations), the certificate's
  crystal-change rule, `WorldTopologyLedger` crystal mass, `FluidView` crystal lines; D18 item 5 ("a held island takes no
  events") is superseded by 0.8.0's fenced topology events.

## 9. Effort

| Section | Effort | Why |
|---|---|---|
| A | medium to large | `phaseCorrection`/`solve` merges, about fifteen crystal-carrying sites, S1 in the wall helpers |
| B | small | four named evidence runs |
| C | large | slack-wall equilibration with booked work, crystal-only vessel state, emptying with crystals, rest pre-equilibration, dry/held-share split |
| D | medium (engine), small (network) | A in UV and PH; C through the typed supertype and the coordinator park |
| E | medium | nine classes to port, BE re-records, crystal-aware ledgers, seven new tests |
