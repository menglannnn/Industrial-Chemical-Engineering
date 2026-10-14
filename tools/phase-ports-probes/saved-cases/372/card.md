# Saved case random 372 (E2, D44: waits for the thermodynamics work)

**World (random seed 372, 14 blocks after minimization keeping class E2 and a two-phase crude node; `case.txt`).**

| Block | Position | Setting |
|---|---|---|
| Crude generator | 5, 0, -2 | two-phase light crude, 347.9 K, 336.6 kPa, 0.02 m bore |
| Nitrogen generator | 2, -2, -1 | 338.8 K, 321.4 kPa |
| Void | 1, 3, 0 | nitrogen, 101.3 kPa |
| Pipes | 2,0,0; 1,0,0; 3,0,0; 1,1,0; 4,0,0; 5,0,0; 1,2,0 | 0.01 m |
| Pipes | 2,0,-1; 3,0,-1; 2,-1,-1 (0.05 m); 5,0,-1 (0.02 m) | |

A hot crude generator and a nitrogen generator feed a row of narrow (1 cm) junctions that rises to a void three blocks up.

**Settings.** Random distribution. Placed at ticks 258 to 2009; the island holds at tick 3636 (committed 3536).

**Expectation from physics.** Both generators drive into the void, 220-235 kPa above it, through 1 cm runs: the flows are small and set by the runs' losses (the 1 cm runs at or near their velocity caps). Crude and nitrogen mix in the junctions; the mixture flashes as it expands towards the void. A steady flow within seconds.

**What the solver did.** At the hold two junctions hold a crude-nitrogen mixture, two-phase, at 316.7 and 321.3 kPa; a third is liquid-full crude at 321.4 kPa. Every step size ends on a singular Jacobian at pass 0. The worst row is the two-phase junction's continuity row (1.0e-6), then the next junction's enthalpy (1.2e-9) and its fugacity rows (8.7e-10, every component alike).

**Cause (unconfirmed).** A two-phase crude-nitrogen junction whose fugacity rows all carry the same residual: the phase split is not determined at that point. Not traced further: owner decision D44.

**Replay.** `../hold-classes/src-t16/T16CrudeCaseTest.replay` with the keep list in `case.txt`; failing rows `trace.txt`.
