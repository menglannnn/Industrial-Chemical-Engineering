# Saved case random 786 (E2, D44: waits for the thermodynamics work)

**World (random seed 786, 8 blocks after minimization keeping class E2 and a two-phase crude node; `case.txt`).**

| Block | Position | Setting |
|---|---|---|
| Crude generator | -1, -1, 2 | two-phase light crude, 320.5 K, 280.2 kPa, 0.01 m bore |
| Pipes | 0,0,2; -3,0,2; 0,-1,2; -1,0,2; -2,0,2 | 0.05 m |
| Tank | -3, 0, 1 | nitrogen, 101.3 kPa, 1 m3 (0.1 m bore) |
| Void | 0, 0, 3 | nitrogen, 101.3 kPa |

A hot crude generator one block down feeds a junction row that runs to a tank on one side and a void on the other.

**Settings.** Random distribution. Placed at ticks 158 to 1563; the island holds at tick 1690 (committed 1626).

**Expectation from physics.** The crude drains into the void (179 kPa of drive) and fills the tank, which compresses its nitrogen towards the junctions' pressure. The light ends flash; the tank holds a two-phase crude. A steady flow into the void with the tank at the row's pressure.

**What the solver did.** At the hold the tank stands at 229.70 kPa, 320.3 K, 38 % liquid and 62 % vapour; the junctions are liquid (193.6 and 237.9 kPa). Every step size ends on a singular Jacobian at pass 0. The worst row is the tank's energy row (2.7e-9), then its connection's flow row (7.7e-10) and its component balances (4e-10 to 2e-10): a converged point whose matrix cannot be factored.

**Cause (unconfirmed).** A two-phase crude tank at its connection's zero-flow root; card E2 (A169) named this world's singularity on a two-phase crude tank. Not traced further: owner decision D44.

**Replay.** `../hold-classes/src-t16/T16CrudeCaseTest.replay` with the keep list in `case.txt`; failing rows `trace.txt`.
