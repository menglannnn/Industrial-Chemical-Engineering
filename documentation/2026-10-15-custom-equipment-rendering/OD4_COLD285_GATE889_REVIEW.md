# OD4 open items: cold 285 and a gate for the arriving PH seed (Opus, 2026-10-20)

Javac lane at `2e28a20a` (0.10.0), prototypes on exports only: no tracked change, no commit. Probes, prototypes P2-P2d, draft gate and logs: `tools/new-units/od4-holds/r4/`, `tools/new-units/logs/od4-holds-r4-2026-10-20.tar.gz` (main checkout). Code guide: no change.
## 1. Cold 285, physics first (island 43)
- LNG generator (264.1 kPa, 119.8 K) → 50 mm → pump [0.01 m3/s, 500 kPa] with 10 mm half links (OD4) → junction J1. J1 → 10 mm → a 1 atm void; J1 → 10 mm → junction J2 → two 1 atm voids.
- Per kg: CH4 48.58 mol, C2H6 3.39, CO2 2.66 (4.86 mol %, 11.7 wt %), N2 0.06. At 120 K CO2 dissolves to about 0.15 mol % (the engine's (mu - mu_s)/RT = 3.5). In a real plant 2.6 mol/kg would freeze: 115 g/kg, 0.09 kg/s of dry ice. The model carries it dissolved until a vessel refresh (D23); the voids take it away.
- Expected steady state (model): a constant throughflow set by the pump's head limit and 10 mm friction. Pump +500 kPa; 0.80 kg/s ≈ 1.9 L/s, 24 m/s in its 10 mm half link, which takes 582 kPa (J1 at 182 kPa). J1 holds subcooled liquid at 120.0 K. 0.566 kg/s goes to the void; 0.236 kg/s goes to J2, which flashes at 125 kPa and 115.4 K and splits 0.115 / 0.121. Nothing is singular; the code reaches these numbers.
- OD4 off: J1 at 233.7 kPa, 1.040 kg/s, 2 680 Newton iterations (0.9.2's).
## 2. What the code does instead
- All 300 refusals are at J1, not at J2 (J2 is two-phase already). Each step the check proposes vapour at J1; the next pass refutes it (D39 B).
- J1's TP answer and own PH split run under the crystal competition. 2.62 mol/kg of CO2 frosts; the CO2-poor liquid boils above 182 kPa; the frost's heat flashes it. Result: 2.07 mol/kg of vapour (TP), or 4.22 mol/kg at 120.09 K (PH, 92 % of J1's volume).
- Stated as a network state (adopt, D23: the frost back in the liquid slot), that seed is 33.7 kJ/kg above J1's enthalpy. Its vapour faces a liquid that still holds all the CO2.
- J1's rows carry that liquid; their VLE has no vapour at 182 kPa. The pass ends at the iteration limit with J1's vapour at 0 (residual -1.5e-2 on its four equilibrium rows).
- Since `8fd61a13` the arrival seed does the same. The fluid-only PH fails (the engine finds the fluid-only liquid unstable, liquid-liquid). The TP fallback (crystal) gives 65 % vapour by volume, and pass 0 fails.
- Two failed network Newtons per step: 22 392 iterations, 749 attempts, 210 refused. Each refused attempt's reason fails `transitionFree`, so no interval certifies and the island ends FULL.
## 3. Proposed fix: a junction's own split is its own rows (owning stage: `FluidThermodynamics.phaseCheck`, used by `arrivingPhases`)
- The rows are `PhaseLayout.junctionResidual`, isolated: mass fractions, specific enthalpy and retained pressure held. They are closed from the engine's answer, as a vessel's own split is its closure (D26, D40 A).
- The engine says which phase may be missing; the rows the pass will state decide whether it stands. A closure that fails, or that drives a phase of its start below VANISHING_SHARE while shrinking (D38 C, read on each iterate), proposes nothing.
- The arrival seed uses the same split, only where the engine's answer adds a phase.
- Absorbed: the junction's TP-fallback seed that "refuses nothing", and the arrival's engine/TP answer as a seed (both become starts). No new constant; about 40 lines.
- To finish at implementation: state the closed point at exactly the retained pressure. `NetworkPhaseEngineTest` reads the junction's P to 0 ulp and the closure's log-pressure unknown gives 2 ulp; the seed itself matches (100.143483 K, 4.757782e-2 mol).
## 4. Options (not forced by physics)
- (a) Recommended: as section 3 (P2c).
- (b) As (a), but a crawling closure runs to its iteration limit (P2b). Same holds, but cold seed 118 costs +174 k closure iterations.
- (c) The junction books the frost (crystal rows in a junction): the real-plant answer, J1 two-phase. It contradicts D23 and extends the model. Not proposed.
## 5. Measurements (P2c against 0.10.0, same lane)
- 285: clean, island 43 STEADY at 0.8021 kg/s from 621 s. 3 483 Newton iterations (912 in 125 closures) against 22 392 to 26 113; 1 phase seed.
- Named worlds 583, 743, 145, 322, 354, 889: clean on P2c, as on 0.10.0 (cold 15.3 s, bundled 8.9 s of seed time against 32.1 s and 13.0 s).
- 200-seed quick check, held sets identical: cold 192, bundled 195, defaults 200. Cold 101, held in both, moves from E6 to E5.
- Counters, quick check: defaults identical; bundled 176 175 against 168 976 Newton iterations (+4 %, seed time 81 s against 90 s). Clean-in-both cold, 192 seeds on one worker: 492 203 against 467 600 (+5.3 %), of which closures 18 479; network +1.3 %; seed time 230.2 s against 229.5 s. Main mover: seed 118 (+3.2 s).
- Targeted classes (round 3's 19 plus `CrystalIslandRuntimeTest` and `CrystalDepositionIslandTest`, 148 tests): base 147/148, P2c the same plus the `NetworkPhaseEngineTest` line above. The common failure (`FluidCheckpointFormatTest` reuse check) is on base too; it passed in m23's Gradle lane, so it belongs to this lane.
## 6. Gate for the arriving PH seed (`f983306b` fix 2, basis `8fd61a13`)
- Bundled 889, pair-minimized to 11 blocks: E9 with the m21 TP-flash arrival, clean at 0.10.0. Keep list `0@152,1@194,3@255,4@312,6@504,9@1442,10@1496,17@2341,19@2642,21@2873,22@2873`.
- World: a 326.6 K, 398 kPa water generator 3 m up feeds, through 20 and 10 mm lines, a junction beside an N2 tank and a 10 mm suction. The pump [0.001 m3/s, 1 MPa] discharges through 10 mm into a second N2 tank.
- Physics: the target needs more suction loss than the 413 kPa above P_sat(326.6 K) = 14.6 kPa, so the pump cavitates (D59 (4)). Its suction holds liquid water with a steam trace.
- Draft `r4/gate/ArrivingSuctionWaterGateDraftTest.java`, about 1.3 s. It asserts: kept up for 120 slices; PUMP_SUCTION_LIMIT; suction at P_sat within 1e-3; liquid water with a steam share below 1e-3; 0 < q < target.
  - 0.10.0 passes: 14 675.48 Pa = P_sat(326.67 K), 0.479 kg/s against 0.986.
  - Rule off: holds at tick 2875, "Nonfinite trial residual".
  - P2: passes with the same numbers.
- `8fd61a13`'s buffer basis is already pinned by `HoldClassWorldsTest`'s 743 world (m22).
- Proposed home: `PumpCavitationTest`, a fourth test. Widening the suite is the owner's call.
## 7. Implemented (owner validation 2026-10-20: option a, gate added), `a6a305a1` on `claude/stoic-bell-ae6019`, not merged
- `FluidThermodynamics.junctionSplit` (owning stage) with `PhaseLayout.junctionClosure`; phaseCheck and `arrivingPhases` use it; the gate is `PumpCavitationTest.waterArrivingAtACavitatingSuctionStaysLiquid`. CHANGELOG under [Unreleased].
- The 21 affected classes on the lane: 148/149. The one failure (`FluidCheckpointFormatTest` reuse check) fails on base in this lane too. `NetworkPhaseEngineTest` passes (pressure restated exactly).
- Moved lines: the D34 gate world (drop line 6.3e-6 → 9.6e-9 kg/s, upper tank +0.28 Pa). Everything else at most 9e-11 relative (`MOVED_REFERENCES.md`, round 4). Replays: 285 STEADY 0.8021 kg/s, 3 485 iterations; 583, 743, 145, 322, 354, 889 clean.
- Relevant parts on `a6a305a1` (owner 2026-10-20). Chain-100 exact. Quick check: bundled and defaults match base. Cold is 191 against 192: seed 170 is newly held (clean on base and on P2c). All three forms stall pass 1 on the same 449 kPa flash seed; on the commit the dt ladder runs out at digit level.
## 8. Pass-1 seed of a flashing junction (owner request 2026-10-20), `48fb5678` on the same branch, not merged
- Physics. In 170 a 3.14 MPa, 153 K LNG source opens to air through 10 mm. The liquid-only pass leaves the junction at 449 kPa, a third of its bubble pressure (≈1.3 MPa). Once vapour is there, the outflow at the 100 m/s cap is lighter, so the junction backs up; the pass converges near 0.92 MPa. A seed at 1.5 to 3 times the pressure clears 170.
- Fix. `PassiveStepSolver.balancedProposal` takes the seed pressure from the balance start, whose `BalanceNet` tables follow the junction's own density and cap (801 kPa for 170), and forms the junction's split again there. It acts only for a vapour appearing in a junction that held none. A first form that acted on every junction proposal was worse on 118: 98 condensation proposals at 127 ms each, and the island hit its wall deadline.
- Results. Quick check: cold 193 clean (base 192, `a6a305a1` 191; 170 and 72 cleared, nothing new); bundled 196 (123 cleared). Cold Newton iterations: 601 374 (`a6a305a1` 655 697, base 569 115). Affected classes 148/149, the same lane failure. Chain-100 exact. Only JUNCTION_ARRIVING_PHASE moves, at 2e-14. Named worlds 285, 583, 322, 354, 743, 145, 889 clean in both distributions. Open: the final lane; at the merge the code guide gains `junctionSplit` and `balancedProposal`.
