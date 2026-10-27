# Saved case random 818 (E2, D44: waits for the thermodynamics work)

**World (random seed 818, 5 blocks after minimization keeping class E2 and a two-phase crude node; `case.txt`).**

| Block | Position | Setting |
|---|---|---|
| Pipe | -1, 0, 0 | 0.1 m |
| Pipe (junction) | 0, 0, 0 | 0.05 m |
| Tank | -2, 0, 0 | nitrogen, 101.3 kPa, 1 m3 (0.1 m bore) |
| Crude generator | 1, 0, 0 | two-phase light crude, 309.3 K, 278.4 kPa, 0.01 m bore |
| Void | 0, 1, 0 | nitrogen, 101.3 kPa, above the junction |

A hot crude generator feeds a junction that leads sideways into a nitrogen tank and up into a void.

**Settings.** Random distribution. Placed at ticks 129 to 1447; the island holds at tick 2974 (committed 2874).

**Expectation from physics.** The crude rises 1 m into the void (177 kPa of drive less the column) and fills the tank level with the junction. The light ends flash as the crude expands; the tank holds a two-phase crude at the junction's pressure. A steady flow into the void, the tank at rest beside the junction.

**What the solver did.** At the hold the tank and the junction stand at 194 052.98 Pa (the tank 309.2 K, 45 % liquid, 55 % vapour; the junction 83 % liquid). Every step size ends on a singular Jacobian at pass 0. The worst row is the junction's continuity row (1.7e-8), then its enthalpy row (1.7e-10) and the tank's fugacity rows (1e-14): a point converged near tolerance whose matrix cannot be factored.

**Cause (unconfirmed).** A two-phase crude tank at a zero-flow root beside a crude junction carrying the through-flow, as cases 279 and 786. Not traced further: owner decision D44.

**Replay.** `../hold-classes/src-t16/T16CrudeCaseTest.replay` with the keep list in `case.txt`; failing rows `trace.txt`.
