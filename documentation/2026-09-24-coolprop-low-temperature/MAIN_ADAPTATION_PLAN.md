# Adapting the unified multiphase thermo branch to main 0.8.0

**Batch:** 2026-09-24-coolprop-low-temperature · **Written:** 2026-10-15 · **Status:** approved 2026-10-15 with changes (`DECISION_LOG.md` D21, D22, D23). M0, M1 (`M1_ENGINE_PORT.md`), M2 (`M2_FLUID_THERMO_PORT.md`, complete 2026-10-16; STACK_PASS fixed by D28 A′ in M3) and M3a (`M3A_D21_FOLLOWUPS.md`) and M3 (`M3_CRYSTALS_AS_SOLIDS.md`, complete 2026-10-16 at `76e87740`: D28 to D30; full test 1441, 0 failures; bundled quick check identical; cold 179 clean against 181 at `a0f20364`, seeds 34, 98 and 114 traced in `M3_COLD_ARRIVAL_TRACE.md`) and M4 (`M4_RUNTIME_FORMATS_GUI.md`, complete 2026-10-16 at `61a1eba0`: checkpoint format 9, unit format 2, protocol `fluid-10`; full test 1450, 0 failures; GUI checked through the bridge) are done. Before M5: owner decisions D31 to D33 and the general design review (`UNIFIED_THERMO_PORT_REVIEW.md`). M3 and M4 follow `PRECIPITATED_SOLIDS_PLAN.md`, decided 2026-10-15 (D23).
- Crystals are solids in main's `SolidInventory`.
- Each stock holds one size per solid, with no size distribution for now. New precipitate's size grows with the freezing phase's fraction.
- Crystal mass moves only in the vessel refresh. Precipitation is evaluated only in reservoirs.
- Tanks are well mixed.

**Owner's changes (D22, D23).**
- Crystals are solids: the solid system gets two kinds, solids formed by a phase transition and custom solids.
- Particles in a vessel are distributed homogeneously, for now.
- Precipitation during transport was first to block the pipeline. D23 (5) replaced this: precipitation is evaluated only in reservoirs.
- The precipitated particle size and the effect on solver convergence are decided in D23.
- M3 and M4 below are rewritten to that design. OD-P3, OD-P4 and OD-P5 are answered by main's solid handling, and OD-P7's crystal lines are dropped.
- D21 is decided: option A for both crystal solves, with theta = 1e-6, raised to 1e-5 after M1, and option C as the typed fallback.

**Branch:** `claude/coolprop-multiphase-thermo-37f6b0` at `9aaafd4` (61 commits over `f9d6be1`, P0 to P6b done, not merged).
**Main:** `ffeafe8a` (0.8.0), 88 commits over the same base.
**Inputs:** main's code guide (`documentation/reference/code-guide/`, snapshot `ffeafe8a`) and three read-only analyses in [main-adaptation/](main-adaptation/):

- [THERMO_MATERIALS_LAYER.md](main-adaptation/THERMO_MATERIALS_LAYER.md): engine, materials, fluid thermo, determinism, references that move.
- [NETWORK_SOLVER_LAYER.md](main-adaptation/NETWORK_SOLVER_LAYER.md): crystal coupling on the backward-Euler solver, P6/P6b fixes, crystals against phase ports, inlets, wall and rest.
- [RUNTIME_FORMATS_GUI_LAYER.md](main-adaptation/RUNTIME_FORMATS_GUI_LAYER.md): checkpoint and protocol formats, views, screen, consumers, tests, the owner's test world.

Nothing was compiled or run for this plan. Every per-file disposition is in those three files; this plan orders them.

## 1. What main changed, and what it means for this branch

| Main change (version) | Effect on this branch |
|---|---|
| Backward Euler replaced TR-BDF2; owned junction holdup (0.6.0) | `TrBdf2StepSolver` is gone. The P6b fixes `db43b22` (negative TR-BDF2 companion) and `a2f48d4` (junction donor cycle) fix code that no longer exists. |
| Vessel bag wall: below capacity a tank stands at ambient pressure, the ambient's work is booked (D54, 0.6.0 to 0.7.2) | The branch equilibrates crystals at fixed volume. That is right only for a full (taut) tank. A slack tank needs the constant-pressure answer at 101 325 Pa. |
| Phase ports, compressor, per-slice inlet refusal (0.7.0) | New devices the crystal inventory has never met: top/bottom/side outlets, the bottom outlet's level head, dry-suction shares. |
| `DeterministicMath` mandatory in `science/` and `runtime/`, enforced by a source scan (0.7.0) | 154 direct `Math` calls in 12 branch files; the facade lacks `atan` and `tanh`. Results move by ulps. |
| Consolidation: 245 to 192 files; `PhaseRoot` to `PengRobinsonKernel.Root`; `PengRobinson78` to the test tree; the analytic composition-derivative chain removed; causal modules removed; checkpoint format 8, protocol `fluid-9` (0.7.2) | The new engine does not compile on main. It uses the removed derivative chain in production (`CubicPhaseEvaluator` lines 142 and 165). The branch's "format 6" collides with main's numbering. |
| Tank rail with phase ratio and status blocks, kmol and kg; held islands accept fenced events (0.8.0) | The crystal lines move to the new rail; one crystal runtime test is rewritten to `acceptAtFence`. |

A dry merge (`git merge-tree`) conflicts in 76 files: about 22 production files, nearly all in the fluid network and runtime, and about 50 tests.

The branch also changes **bundled-package** results, not only the pilot. These are the direct liquid path at state pressure (P3 WP4), Region 1 water at state pressure (D12), the engine flash (WP7), the pump reference density from the model's own water (996.0 to 997.05 kg/m³) and the network package's pressure limit (2 to 10 MPa). D1, D9 and D12 make these changes apply to every package, so main's references move once (section 4).

## 2. Route

**Recommended: port onto a new branch cut from main.** Create `claude/unified-thermo-port` from `ffeafe8a` in this worktree. Bring the branch's self-contained files over by path from `9aaafd4` and redesign the coupling on main's stages, one layer per work package. Each package compiles and passes its own checks before the next starts. The old branch stays unchanged as the record, so every commit id cited in `DECISION_LOG.md`, the plan and the CHANGELOG still resolves.

Alternatives:

- **Merge main into the old branch.** One merge commit would resolve 76 files and carry the redesign. Nothing compiles until that commit is complete, and it cannot be reviewed layer by layer.
- **Rebase.** Rejected. It rewrites 61 commits and repeats the network conflicts in about twenty of them. History rewrites were refused once already on this branch.

## 3. Work packages

The packages run in order, one opus agent each, in this worktree, with the `build/gradle.lock` protocol. They touch the same files and each needs the previous one to compile. The lead keeps the serial measurement lane and the documents.

### M0 — Setup (lead, small)

- Cut the port branch; copy this plan and the analyses to the main checkout; record the owner's answers to section 5 and the rule-forced choices of section 6 as D22.

### M1 — Engine and data, bundled-neutral (medium to large)

Everything that does not change a bundled network result:

- **New code by path.** `science/thermo/**`, the new `science/material/**` classes, the material records (`spine`, `crystals`, `group_interactions`, pilot properties and package), `materials-index.json` (merges to 93 entries), the Helmholtz oracle, fixtures and the engine-level tests.
- **API moves.** `PhaseRoot` becomes `Root` (36 test files). `PengRobinson78` uses become `HydrocarbonModel.pureLiquidCompressibility`, made package-private and shared with `SpinePackageData`. Re-apply the `MaterialCatalog` record kinds on main's structure (`JsonFields`, nested `FluidData`), and the kij(T) pair plan on main's `PengRobinsonKernel`.
- **Derivatives.** The engine's cubic evaluator builds its derivatives on the kernel's, adding the two volume-translation terms itself (OD-P2, decided: adapt, restore nothing).
- **Determinism.** Convert the 154 calls. Add `atan` and `tanh` to the facade with `DeterministicMathTest` cases.
- **CO2 record.** Give CO2 the `appearance` block main requires (nitrogen-like, estimated). Re-pin its SHA once.

**Check:**

- `science.thermo.*`, `science.material.*`, the determinism source test, and the engine-level qualification families (G0, G1, G2, G4F1, G5F1 to G5F5) equal to their recorded values at printed digits.
- Bit-level pins moved by the facade are re-pinned in one listed commit. A classification count that changes is a finding, not a re-pin.
- **Neutrality proof:** `chain-100` exact gives 0 against main's committed reference, and the 33 junction lines are unchanged.

### M2 — Fluid thermo on the network: bundled results move by design (large)

- `FluidThermodynamics` and `HydrocarbonModel` are redesigned. Main's phase streams, trace folding and API stay. `equilibriumTP` becomes the engine adapter and `flashTP` stays its folded form. Crystals pass through the fold unchanged.
- Main's two readers of the liquid compressibility take the EOS liquid and water compressibilities.
- `GlobalLiquidResponse` and the `fluid.liquidCompressibility` key are deleted (D12, WP4). The compressibility parameter leaves the model constructors: main's 75 call sites in 43 test files become `FluidTestSupport.model(catalog, package)`, with no deprecated shims.
- `WaterRegion1` and `FluidDomain` take the branch's version. `MixtureViscosity`, `NetworkPhaseEngine` and `SpinePackageData` are re-applied.
- `PassiveStepSolver` reads `vaporLike()`. The network package gets the 10 MPa domain. The checkpoint drops the compressibility from its package key (format 9 starts here; see section 6).
- **Added 2026-10-15 after the first stop** (`M2_FLUID_THERMO_PORT.md`):
  - **D24 A.** The engine solves Rachford-Rice in the minor phase's fraction.
  - **D25.** The outer phase check adds missing phases, keeps populated ones, and drops a phase only when the solve empties it. There is no pure-compound test. This was moved here from M3, so that every change that moves bundled results lands before the single re-record.

**Check:**

- `fluidScienceTest`, `fluidRuntimeTest` and the G3 families.
- One quick check: 200 seeds, both distributions, against the same seeds of main's last campaign.
- `chain-100` re-captured, and the 33-line junction reference re-recorded. Each moved line gets a one-line physical reason (section 4).

### M3 — Crystals as solids on the backward-Euler solver (large)

The design is in `PRECIPITATED_SOLIDS_PLAN.md` (D23). This section lists the work.

- **Crystal kind.**
  - Crystals join main's `SolidInventory` as a second kind of population beside custom solids, keyed by (material, `ParticleSize`) like them.
  - A crystal record adds its component and its energy offset.
  - The branch's `CrystalInventory` is dropped, and `Inventory` gains no field.
  - Generators feed custom particles only.
- **Refresh closure.** Crystal mass moves only in `InventoryEquilibrium.refresh`, at interval and solid-event segment starts (a lag of about 5 s).
  - The engine's crystal test decides presence, with D21's pure step (theta = 1e-5) in both the fixed-volume and the ambient-pressure solve.
  - The amount is one closure unknown, with the crystal's chemical potential equal to its component's.
  - The energy offset is booked in the refresh.
  - Inside a step a crystal is an inert population, so the island Newton, the equation gate and the per-population audit do not change.
- **Wall.**
  - The closure equilibrates at fixed volume when the tank is full and at 101 325 Pa when it is slack.
  - The ambient's work on the bag is booked as main's transport does (D54).
  - A fixed-volume answer below ambient switches to the constant-pressure solve, and a constant-pressure answer above capacity switches back.
- **Size and slush (one size per solid in each stock, D23 amendment).**
  - Precipitated mass enters at d = 0.25 mm · max(1, 4.04 (phi / (1 − phi))^0.6). phi is the crystal component's volume share of the liquids, capped at 0.5; it is 0 from a gas.
  - The refresh merges each stock's populations of one solid into one, at the mass-weighted mean diameter rounded to 9 significant digits. Stocks are tanks, junction buffers and filter cakes, and custom solids are included. A material with a single population is left untouched.
  - `SolidMobility.donor` reads the per-solid mean instead of the largest population.
  - Main's packed-slurry test blocks at 0.5 for all solids, while Krieger-Dougherty keeps 0.62.
  - Main's deposition law decides in pipes.
- **Reservoirs only.**
  - Junctions, pumps, compressors, valves, generators and voids call `tpCrystalsHeldOut` with the package's crystal competition.
  - There is no per-slice flash and no new refusal mode.
- **Rest.**
  - Islands holding unchanged solids may rest; main's solids exclusion in `NetworkReduction.atRest` is lifted (decision 6).
  - The crystal refresh runs before the rest test, because a supersaturated closed tank is not at rest.
  - The approximate fallback stays refused for solid intervals.
- **Phase decisions.** Done in M2 by D25: one rule for any composition, which absorbs the branch's pure-coexistence phaseCheck. P6's `vesselSplit` is ported there only if `LiquidFullBubblePointTest` still fails. M3 keeps main's trace folding and prepared workspaces as they are.
- **Main's devices.** Main's solid handling answers the earlier owner items:
  - The level head already counts suspended solids (OD-P3).
  - A tank left with only crystals is main's dry-solids vessel (OD-P4).
  - Crystals are inert in an emptying step (OD-P5).
  - The trace flush never deletes crystal mass.
- **Structure.** `ThermoDomainViolation` and the phase hold get one shared supertype.
- **Engine.**
  - P6b's warm start (`4c21104`) is re-applied in the engine.
  - **D21 follow-ups: done early as M3a** (`cc1a39a1`, 2026-10-16). theta = 1e-5, and the inner UV secant is taken only within one classification.
  - **Watch item from M3a.** Near CO2's triple point with traces above theta (1e-4 CH4), 13 of 42 UV targets fail at the binary's three-phase step, where the phase rule leaves one degree of freedom.
    - If the cold-package fuzz shows them in the crystal refresh, the structural fix is the engine-side analogue of D25: answer the binary's three-phase line directly from the balances, as the pure triple point already is.
    - They stay untyped until then (D21).

**Check:**

- The unit gates of `PRECIPITATED_SOLIDS_PLAN.md` section 4, each proven with its rule off. Added to them:
  - a supersaturated closed tank precipitates before it rests;
  - an unchanged solid island rests;
  - the trace flush keeps crystal mass.
- Crystal tests:
  - Re-homed onto main's rigs: `CrystalDepositionIslandTest`, and `CrystalIslandRuntimeTest` on `CertifyingRig`.
  - `CrystalFlushBudgetTest` re-measures failure 2 under the refresh form.
- Regressions for the dropped P6b fixes:
  - `StagnantJunctionTest` proves the donor cycle is gone.
  - `WaterIntoCryogenicMethaneTest` is re-derived for the slack tank.
- The G4F2 and G5 network families.
- The fuzz checks:
  - a 200-seed quick check, whose class tables must be identical on the bundled packages;
  - the cold-package option in `tools/`, 200 seeds.

### M4 — Runtime, formats, presentation, GUI, consumers (medium)

- **Formats.** Checkpoint format 9, island and topology unit format 2, menu protocol `fluid-10`. Every older format is refused with the fresh-world message.
- **Codec and packets.**
  - Crystals ride main's solids encoding: a population's material id names its record.
  - The reader looks up crystal records beside custom ones, and refuses an id that names no record of the package.
- **View and screen.**
  - Crystals show in main's Solids tab and in the tank rail's solid share. OD-P7's separate crystal lines are dropped.
  - The Solids tab shows one row per solid at its mean size, including arrivals since the last refresh.
  - Language file.
- **Consumers.** The column and neural refusal (`CrystalPhasesUnavailable`), the reload guard, the ledger totals and certificate replay with crystals.

**Check:**

- `fluidRuntimeTest`, `FluidCheckpointCodecTest`, the format test (formats 1, 2, 3, 7 and 8 refused) and `FluidPacketCodecTest`.
- Four new tests:
  - the codec refuses an unknown crystal id;
  - ledger totals;
  - certificate replay;
  - the reload guard on a crystal-record edit.
- The GUI checked in the dev client through the MCP bridge: the runtime analysis's checklist (section C4), re-read for the Solids tab.

### M5 — Gates and review (lead lane plus one opus agent for documents, medium)

- **Suites and gates.** Full `test`, then one `gates.sh` run: G1 with the new junction-line reference, G2 exact, G3, and the 31 fluid GameTests on a fresh run id.
- **Fuzz.** One hold-fuzz campaign (1000 seeds, both distributions, 8 workers) against main's last campaign. Only new budget-class holds (C and F) are replayed, in one batch.
- **Qualification.** The batch's families G0 to G6 re-run and compared with their recorded numbers.
- **Review and records.** `MAIN_ADAPTATION_REVIEW.md`. The CHANGELOG `[Unreleased]` line rewritten to the port commits, with the old branch named as the record. INDEX rows updated. Anything the port added for measurement detached to `tools/`.
- **Old tools.** The batch's existing tool folders keep their re-attach commits on the old branch and are not rewritten. The exception is the P6 MCP helpers, which the test world needs.

### After the port: the work the owner already asked for

1. **Failure 1 (D21), decided 2026-10-15.**
   - The network analysis found that it persists on main, and backward Euler does not change it.
   - Main adds a second form: a slack tank equilibrates at ambient pressure and meets the same unresolved step at 194.7 K.
   - The fix is option A with θ = 1e-6 (1e-5 after M1, owner 2026-10-15), plus option C as the typed fallback, and covers both the fixed-volume and the ambient-pressure solves. It lands in M1's engine step and in M3's refresh closure.
2. **The owner's test world.** It has 13 scenario rows (runtime analysis, section E), plus two for D23:
   - From P6 and P6b: rows A, B/D and E, the pilot chain through a junction, deposition and freeze-out, and save and reload.
   - New on main: crystals in a tank with top, side and bottom outlets; a compressor on a frosting gas; a pump on a liquid-methane slurry; a held crystal island accepting an edit; an empty tank filling cold; breaking a crystal tank.
   - D23:
     - Liquid CO2 let down into a 1 bar tank: the valve stays open and dry ice appears in the tank within about 5 s.
     - CO2 traces in LNG pass as fine particles or deposit, depending on line speed, while a freezing CO2-rich liquid blocks the bottom outlet at 0.5 solids.

   The world is built from a fresh template, with the `p6_pilot` datapack (it passes main's network checks unchanged) and pilot mixtures as client files. The MCP helper `gen.js` is rewritten to find widgets by label. Then the lead starts `runMcpClient`. The world preparation in `tools/` can run beside M3 and M4 and finish once the screen labels settle.

### At the merge (the owner's call; not part of this plan's execution)

- **Version.** CHANGELOG entry and `mod_version` 0.9.0 (minor, one batch).
- **Code guide.** A new chapter for the equilibrium engine, the spine and the crystal phase, with a README row. Updates to the thermo-materials, fluid-science, fluid-runtime, client-gui and verification chapters, with snapshot lines set to the merge commit.
- **Merge checklist.** Tooling cleanup, the `git ls-files -i -c` check, INDEX rows.

## 4. References that move, and why

| Reference | After the port | Cause |
|---|---|---|
| `chain-100` exact | Moves; re-captured on the ported code, taking neither side's file | Direct liquid at state pressure, water at state pressure (chain-100 deviations +1.03e-3 liquid and −9.3e-4 water, as measured on the branch), engine flash |
| 33 junction lines | `LIQUID_JUNCTION` lines move; `MIXED_GAS_*` lines expected unchanged (pure CH4/N2 gas at 350 to 400 K); confirm by run | As above |
| `MIXED_GAS_COST` | Expected 339 unchanged; confirm by run | Gas-only cases |
| Fluid GameTest prints with liquid, water or pumps | Move | Liquid density; the pump reference density of 997.05 kg/m³ lowers the rise limit by about 0.1 % |
| Network thermo fingerprint, `HydrocarbonModel` revision | Move (`f5e0178e` to `95cd8e6d` on the branch) | 10 MPa domain, new formulation; worlds are fresh anyway |
| Branch bit pins (`LegacyNetworkPathPinTest`, the P12/P31 fingerprints) | Re-pinned once | `DeterministicMath` ulps |
| Qualification counts (G4F2 holds ≤ 14, four G5F6 three-phase states, G4F1 holds) | Expected unchanged; confirm by run | Ulps could cross a threshold |
| kij(T), spine, crystals on bundled plans; JSON SHA pins | Unchanged | Pilot package only; data unchanged |

Each re-record is a reviewed event named in the CHANGELOG, as main's rule asks.

## 5. Decisions for the owner

1. **OD-P1, route.** Port onto a new branch from main (recommended), or merge main into the old branch.
2. **OD-P2, the translated derivative chain. Decided 2026-10-15: adapt to main, restore nothing.** The owner asked whether the engine could adapt to main's removal. It can.
   - **What the removed code was.** `TranslatedPengRobinson.Derivatives`, `newDerivatives` and `differentiate`, about 60 lines over the kernel's own analytic derivatives. Main kept those: `PengRobinsonKernel.evaluateDerivatives` with d ln φ/dn, d ln φ/dT, the partial molar residual enthalpies and dH^R/dT. The wrapper added three things.
     - The volume translation's temperature term: d ln φ_i/dT gains −P c_i/(R T²).
     - The pressure derivative: d ln φ_i/dP = v̄_i/(R T) − 1/P, from the translated partial molar volumes main's `Values` still carries.
     - Partial molar enthalpies.
   - **Adaptation.**
     - `CubicPhaseEvaluator.derivatives` calls `evaluateValues` and `kernel.evaluateDerivatives` itself, then adds the first two terms from the translations it already holds. That is about 25 lines.
     - Nothing returns to `TranslatedPengRobinson`, `HydrocarbonModel` or `FluidThermodynamics`.
     - The partial molar enthalpies are dropped, because no production code reads them; only tests do.
   - **Bit identity.** The kernel's `evaluate` and `evaluateDerivatives` both run `prepareTemperature` then `evaluatePrepared`, so both pick the same root. Both gas constants are 8.31446261815324, and the two added terms are the same expressions. The engine's numbers are therefore expected bit-identical to the branch's; M1's engine-level families confirm it.
   - **Cost.** One extra kernel evaluation per derivative call. Derivative calls occur only in the Newton finish and the critical band.
   - **Tests.**
     - The rows of `TranslatedPengRobinsonDerivativesTest` move into `CubicPhaseEvaluatorTest` (finite-difference checks of d ln φ/dT and d ln φ/dP), and the Σ x_i h̄_i row is dropped.
     - `DirectLiquidJacobianRowsTest`, `DirectLiquidContinuityTest`, `G3Support` and `TranslatedPengRobinsonPairInteractionsTest` call the engine evaluator, which is the production path.
     - `SpineNetworkPathTest` keeps its domain refusals and drops the partial-enthalpy NaN rows.
     - `LegacyNetworkPathPinTest` hashes the evaluator's derivatives (it is re-pinned anyway).
     - `FluidNitrogenCryogenicTest` takes main's version.
3. **OD-P3, crystals and the bottom outlet's level head. Resolved (D22 (6), D23): tanks are well mixed, so suspended crystals add their weight to the head, as main's level head already counts.** The analysis below assumed a settled bed. In a real tank, liquid covering a settled bed is raised by the bed's solid volume, and the pore liquid is hydrostatic. The grains' weight rests on the floor.
   - (a) Count the displaced volume, head = ρ_L g H (V_L + V_s) / V. Recommended. It is exact while the liquid covers the bed and over-reads a partly drained bed, which would need a porosity that D20 declines to invent.
   - (b) Leave crystals out, as main's level head does now. It under-reads a covered bed by V_s / V.

   The network analysis recommended (b); the runtime analysis recommended (a).
4. **OD-P4, a tank left with only crystals. Resolved (D23): main's dry-solids vessel, which is option (a).** In a real tank, the bag collapses onto the bed at ambient pressure and the bed sits at its own temperature. It sublimes when warmed above about 194.7 K at 1 atm.
   - (a) Accept a solid-only state at ambient pressure, like main's dry tank of inert particles. Recommended.
   - (b) Keep the branch's typed refusal: the island holds.
5. **OD-P5, crystal heat in a step where the tank's fluid runs out. Resolved (D23): crystals are inert within a step, which is option (a).**
   - (a) Set the crystals aside at their starting state for that step and let the equilibration after the step settle the temperature. Recommended.
   - (b) Put the crystal heat capacity inside main's emptying solve, a larger change to it.
6. **OD-P6, `ScheduledTransfer`** (code guide open item 5). Four crystal science tests use it as their only heater and withdrawal. Recommended: keep it.
7. **OD-P7, tank rail. Resolved (D23): crystals show in main's Solids tab and in the rail's solid share. The separate crystal lines below are dropped.**
   - A fifth "Crystals" segment in the phase-ratio bar, tanks only. Recommended yes.
   - Rail lines "Crystals (stay in the tank)" in kmol and kg, and crystal volume in L, with "Fluid volume" and "Fluid mass" above them. Recommended over "volume without crystals".
   - No crystal overview tab until P7. Recommended.
8. **D21, failure 1. Decided 2026-10-15** (option A, theta = 1e-6 then 1e-5 after M1, option C as the fallback). It is unchanged by the port except that the fix must also cover the ambient-pressure solve (section 3, "After the port").

## 6. Choices forced by physics or recorded rules (logged as D22, reversible)

- **No gating.** The new liquid path is not gated to the pilot package; D1, D9 and D12 forbid it. Bundled references move once and are re-recorded (section 4).
- **One format bump.** Checkpoint 9, unit 2, protocol `fluid-10`. The branch's intermediate formats were never released, and the no-compatibility rule needs no migration.
- **Dropped P6b fixes.** `a2f48d4` and `db43b22` are not ported, because the code they fix is gone. Their tests stay as regressions, and the water test is re-derived for the slack tank. The assumptions the owner was asked to confirm for `a2f48d4` (factor 10, one-solve scope) lapse with it.
- **Crystal physics on main's devices.** Fixed volume when full, ambient pressure when slack; refresh before the rest test. Each follows from what a real tank does. The earlier "crystals never drawn" and "a crystal-only tank is a dry end" are superseded by D22 (5) and D23: crystals are solids moved by main's solid transport, and a crystal-only tank is main's dry-solids vessel.
- **Main's consolidation rules kept.** No test-only accessors in production; the thermo analysis proposed keeping two, and the plan follows main's rule instead. Tests read kij(T) through the kernel's evaluation. No deprecated shims. New facade functions get their own tests.
- **Small defaults.** The CO2 appearance copies nitrogen's. No pilot GameTest server: pilot runtime tests use `CertifyingRig`, plus the MCP world. Pilot mixtures for the test world are client files in the tool folder.

## 7. Risks

- **Ulp crossings.** A classification count could cross a threshold after the facade conversion. Such a change is explained, not re-pinned.
- **New devices.** The direct liquid path meets phase ports and the compressor for the first time. New holds in the campaign are triaged by class, per the lean rule.
- **Failure 2 under backward Euler.** The crystal flush cost may grow or shrink with backward Euler's step count. It is re-measured in M3.
- **Derivative adaptation.** If an engine-level family moves after OD-P2's adaptation, the cause is a different root or a different expression. That breaks the bit-identity argument and is traced before any re-pin.
