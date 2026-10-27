# staging: D29 option A and D28 option A′ (both applied)

Applied 2026-10-16 on the owner's go (D28 A′ and D29 A, "ok"): `d28-option-a-prime.patch` as `741ce603`, then
`d29-option-a.patch` as `76e87740`. Both patches stay here as the record of what was staged; do not apply them again.
Their measurements at `76e87740` are logs 24 to 26 and the report `M3_CRYSTALS_AS_SOLIDS.md`.

Also here: `d28-option-a-prime.patch`, owner decision D28 restated as A′ against `63926c52`. A failed iterate's fold
is a trial layout, not a verdict: it reads the A85 record and never writes to it. The code changes are two lines:
`PassiveStepSolver` L581 passes `foldedPhases.clone()`, and the failure branch of `InventoryEquilibrium.passes` drops
`excluded|=...`. The rest of the patch is comments. It was probed alone, without D29 A (logs 22 and 23):
- STACK_PASS passes; the upper tank ends with 0.0 g of liquid water.
- Cold seeds 35 and 148 stay clean (37.27 kg and 19.93 kg of crystal). D30's removal of the bubble stands.
- Seed 72 is class E6 at tick 1318, island 32. The tick 1080 hold is gone.

The rest of this file is D29 A as staged, before it was measured.

`d29-option-a.patch` is owner decision D29's option A. It keeps the condensed stand-in (`92d39275`) and reads the trace
support of a frost answer's stand-in liquid from the answer's own fluid phases, so that liquid accepts every component.
The patch is against `63926c52` (E1, D30); `git apply --check` passes there. Apply it with `git apply
tools/unified-thermo-port-m3/staging/d29-option-a.patch` only after the owner's go: the lean cap allows three measured
forms, and this is the fourth.

## Physics
LNG at 149 K and 1.58 MPa flows into an empty tank at the ambient pressure. It flashes, and the tank fills with boiling
LNG: 133.7 K, 414 kPa and 9 % liquid after one interval on `a0f20364`. At 101 kPa and below CO2's frost line, the
engine's TP answer is CO2-I beside a vapour. The stand-in puts the frost's share in the liquid slot, a liquid of pure CO2.
That liquid is no phase of the answer: it stands for supersaturated condensed matter, and a real liquid there dissolves
the methane that arrives with it.

## What changes
- `FluidThermodynamics.condensedShareOnly(State)` (new). It is true where the state's hydrocarbon liquid holds crystal
  species only (`crystalSpecies`), beside a vapour. It is false at its first test (`crystalIds.isEmpty()`) on a package
  without crystals.
- `PhaseLayout.support(FluidThermodynamics model, State seed, mask, promoted, junction)` (new overload). It gives the
  support of the existing policy overload, with `VAPOR_ONLY` changed to `BOTH` where `condensedShareOnly(seed)` is true.
  `supportOf` and the policy overloads are unchanged.
- Three trace-support paths change. They are the only call sites that derive a support under the model's policy (all
  uses of `model.traceTruncation()` were checked):
  1. `PassiveStepSolver.supports`: every node's frozen support for each pass of a step (vessels and junctions).
  2. `InventoryEquilibrium.layout`: a vessel closure without a fitting step support. That covers the interval-start
     refresh (`solve`), the D26 own split (`split`), and a reconstruction whose step support no longer fits the state.
  3. `VesselCrystals.Joint`: the crystal refresh's joint closure.
- These stay unchanged:
  - reactivation (`PhaseLayout.reactivate` acts only on single-phase supports);
  - the junction's carried-trace rule (it needs no liquid, so it never meets a stand-in liquid);
  - layouts under `TraceTruncationPolicy.OFF`;
  - the phase-level folds (`foldTracePhases`, `folded`, `phaseCheck`);
  - failed-pass folding and A85 (D28).

## Why bundled worlds stay identical
- The bundled network packages have no crystals. `CrystalDepositionIslandTest` pins that the bundled package has none.
- So `condensedShareOnly` returns false at its first test, and the model overload returns exactly the array of the policy
  overload. The patch changes no other code.
- Stand-in liquids come only from crystal answers (`FluidThermodynamics.adopt`), and a package without crystals never
  forms one.
- On the pilot package the rule acts only where a liquid holds nothing but CO2 beside a vapour. Where such a liquid is
  real rather than a stand-in, `BOTH` is the untruncated support of its full equilibrium: there are more unknowns, but
  the physics is the same.

## To measure after the go
1. Run the M3 test classes: PrecipitatedSolids, CrystalDepositionIsland, CrystalIslandRuntime, CrystalFlushBudget,
   StagnantJunction and WaterIntoCryogenicMethane.
2. Run one cold fuzz and one bundled quick check (`../fuzz.sh`; the bundled counters must equal `1eeb8673`'s).
3. Run one full `test`.
- Expected: cold seeds 34, 75, 78, 98, 114 and 170 (methane's floor) leave class C.
- PrecipitatedSolidsTest's let-down is pure CO2: its vapour holds no other species, so nothing changes there.
- Possible moves are the frost cases with a second gas, where N2 is now admitted to the CO2 stand-in liquid:
  - CrystalDepositionIslandTest (a) and (c);
  - StagnantJunctionTest's CO2/N2 vessel.

## Measured at `76e87740` (logs 24 to 26)
- Full `test`: 1441 tests, 0 failures. No M3 test line moved (neither possible move above occurred); the one printed
  line that moves is STACK_PASS (D28 A′).
- Bundled quick check: every seed line, class and counter as at `1eeb8673`.
- Cold: 179 clean. Of the six methane-floor seeds, 75, 78 and 170 left class C; 34, 98 and 114 still hold there.
