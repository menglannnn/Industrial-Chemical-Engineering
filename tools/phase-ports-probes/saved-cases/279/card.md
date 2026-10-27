# Saved case random 279 (E2, D44: waits for the thermodynamics work)

**World (random seed 279, 6 blocks after minimization keeping class E2 and a two-phase crude node; `case.txt`).**

| Block | Position | Setting |
|---|---|---|
| Tank | 2, 0, 1 | nitrogen, 101.3 kPa, 1 m3 |
| Void | 2, 0, 3 | nitrogen, 101.3 kPa |
| Crude generator | 3, 0, 0 | two-phase light crude, 325.6 K, 330.7 kPa |
| Pipes | 3,0,1; 3,0,2; 3,0,3 | 0.1 m |

A hot crude generator feeds a junction, level with it, joined to a nitrogen tank beside it and to a void.

**Settings.** Random distribution. Placed at ticks 125 to 2239; the island holds at tick 2866 (committed 2766).

**Expectation from physics.** Crude at 331 kPa runs into the 1 atm void without limit (the 0.1 m run at its velocity cap) and into the tank, which fills and compresses its nitrogen. The light ends flash where the crude expands; the tank holds a two-phase crude (liquid and vapour) at the junction's pressure. The junction stands between the generator and the void, set by the void line's loss.

**What the solver did.** At the hold the tank stands at 292 447.39 Pa and 325.5 K, its volume 54 % crude liquid and 46 % vapour; the junction is liquid-full at 292 447.39 Pa. Every step size ends on a singular Jacobian at pass 0. The worst rows at the last failed Newton are the tank's flow row (connection 0, 3.4e-9) and the void's (6.4e-11), then the junction's enthalpy row (9e-12) and the tank's fugacity rows (1e-14): the point stands near the tolerance (1e-9), but the matrix at it cannot be factored.

**Cause (unconfirmed).** A two-phase crude tank at a zero-flow root beside a liquid-full crude junction: the factorization fails at a converged point, the family of card E2's crude worlds (A169). Not traced further: owner decision D44, the thermodynamics package's work.

**Replay.** `../hold-classes/src-t16/T16CrudeCaseTest.replay` with the keep list in `case.txt`; failing rows `trace.txt` (`src/instr-t13-failing-rows-and-levers.patch`).
