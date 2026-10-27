# M3: LNG into an empty tank stops at methane's floor (cold seeds 114 and 34), traced
2026-10-16. Diagnosis only, no source change. `76e87740` against `a0f20364`, each exported and run on the index lane
with prints in the copy, one run per seed and commit; tools and logs in `tools/unified-thermo-port-m3-arrival/`. 114
and 34 are the two smallest held seeds (98 is not traced); each held island is one generator, one pipe and one tank.
## Physics expectation, with the trace's numbers
- The tank holds no nitrogen. Main places every vessel empty (D54, `PassiveNetwork.emptyVessel`): its 298.15 K, 1 atm
  nitrogen is the ambient placeholder state, its inventory zero. The trace shows it: the tank's first seed is only what
  arrives (0.2247 mol of N2 per 228.3 mol of CH4, the LNG's own 0.094 %). The 41 mol of warm N2 are not there.
- Seed 114: LNG at 139.02 K, 1.1446 MPa (CH4 95.9 %, C2H6 1.5 %, CO2 2.5 %), 80.8 kg/s through the 0.05 m run (its
  100 m/s cap); seed 34: 148.51 K, 1.5845 MPa (CH4 88.7 %, C2H6 6.5 %, CO2 4.2 %), 1.60 kg/s through the 0.01 m run.
  The held interval is the island's first tick, 0.05 s: 238 and 4.4 mol arrive.
- An empty bag is slack at 101.3 kPa: the subcooled LNG flashes at constant enthalpy into boiling LNG, 112.11 K with
  19.3 % vapour by mole (seed 114: 0.41 m3 of vapour, 7.1 L of liquid) and 113.05 K with 25.9 % (seed 34), as
  `a0f20364` converges; CO2 above its solubility is frost for the next refresh. The bag is full after about 10 kg
  (0.12 s), then pressurises on the bubble line (D29: 133.7 K, 414 kPa after one interval). 90.69 K needs methane at
  11.7 kPa: at 101 kPa only methane that cannot condense, and pays its latent heat as sensible heat, gets there.
## What the code does at `76e87740`
- Each step's first pass lays the empty bag out from `arrivingPhases`: a TP flash of what arrives at its own 139.02 K
  and 101 325 Pa, an isothermal let-down. The engine answers VAPOR_SOLID, CO2-I beside an all-vapour LNG of 2.6 bag
  volumes; the stand-in puts 5.54 of 5.93 mol of CO2 in the liquid slot (a pure CO2 liquid); D29 A makes all BOTH.
- The Newton must book the arriving liquid's enthalpy. Its one liquid is that CO2 liquid, on the equation of state's
  CO2-rich branch, which barely dissolves methane: at the floor x(CH4) = 2.9 %, K = 34 against Raoult's 0.12. At
  0.0125 s, 57.03 of 57.08 mol of methane stay vapour and cool 139.0 -> 98.0 -> 91.5 -> 90.694 K at 101 325 Pa, where
  the line search stalls with 3.35e5 J unbooked (5.9 kJ per mol of methane vapour: its latent heat, net of 48 K of
  cooling); seed 34 alike. Methane has BOTH support (D29 A): not the support constrains this hold but the liquid.
- The failed iterate holds both hydrocarbon slots, so the outer phase check (D25) forms no TP answer: nothing proposes
  the LNG liquid. The first step into an empty bag is scale-free (what arrives has one intensive state at any step
  size): all 20 halvings, 0.05 s to 9.5e-8 s, stop at the floor (one residual, 1.399, from 0.0125 s down). Class C's
  budget goes on one state; the retry and island 7 (filter placed at tick 178) hold the same way.
## At `a0f20364`: the divergence and its cause
- The same engine answer (VAPOR_SOLID at 139.02459 K, 101 325 Pa, the same amounts) is adopted all vapour. That pass
  fails at the floor too (90.694 K, 73.3 kPa: the isothermal seed is far from the bag's state), but its iterate lacks a
  liquid: the phase check forms the TP answer (LIQUID_SOLID), seeds the liquid at the bag's own split (D26: UV, 126.7 K,
  217 kPa), and pass 1 converges in 12 iterations at 112.11 K, slack. Seed 34: the same, 113.05 K in 15.
- First divergence: how the bag's first TP answer is adopted, commit `92d39275` (a TP answer holding a crystal keeps the
  crystal's share condensed). The stand-in fills the liquid slot of a layout whose liquid is the LNG: the phase check
  sees no missing liquid, and the Newton has only a CO2 liquid to condense methane into. M3 log 18 (M2's adoption on the
  M3 code) runs 34, 98 and 114 clean.
## Proposition for the owner (D31), A probed
- A (recommended), in the step's seeding (`PassiveStepSolver.arrivingPhases`, D54): an empty bag is laid out at its own
  split of what arrives: PH at the delivered enthalpy (with the height drop) and 101 325 Pa, closed by D26's
  `InventoryEquilibrium.split` (PH while slack, UV at the capacity once it overfills; TP stays the fallback). It absorbs
  the isothermal estimate and the failed first pass D25 needs (`a0f20364`'s route). Patch `staging/a-own-split.patch`
  (`equilibriumPH` public, one enthalpy line in the edge loop, helper `emptyBagSplit`). Probe, index lane, one run each:
  seeds 114, 34 and 98 (its node 14 is also an empty tank fed LNG) clean, each first pass at its root (0 Newton
  iterations), first intervals 112.114, 113.048, 112.167 K at 101 325 Pa (0.3 K above the engine's 111.80 K: the frost's
  share stays dissolved until the refresh freezes it, 5.89 mol); 27 classes (six M3, group L, all citing D54) 147 of 147
  before and with A; 74 of 501 printed lines move (73 EmptyTankFillsTest, 1 TracePhaseFoldTest), at most 1.9e-12.
- B (not run), in `FluidThermodynamics.phaseCheck` (D25): a liquid of crystal species only counts as no fluid liquid.
- C, M2's adoption for a frost answer (D29 B): clears the three; the CO2 let-down hits the 90 K floor. Not recommended.
