# Saved case random 521 (E2, D44: waits for the thermodynamics work)

**World (random seed 521, 6 blocks after minimization keeping class E2 and a two-phase crude node; `case.txt`).**

| Block | Position | Setting |
|---|---|---|
| Crude generator | 2, 0, 0 | two-phase light crude, 329.8 K, 283.0 kPa |
| Pipes | 3,0,0; 4,0,0 | 0.1 m |
| Pipes | 3,-1,0; 4,-1,0 | 0.05 m |
| Tank | 4, -2, 0 | nitrogen, 101.3 kPa, 1 m3, fed through its top |

A hot crude generator feeds a small loop of junctions that drops two blocks into the top of a nitrogen tank.

**Settings.** Random distribution. Placed at ticks 366 to 768; the island holds at tick 1195 (committed 1095), its first interval after the last block.

**Expectation from physics.** Crude falls into the tank and compresses its nitrogen until the tank's pressure is the generator's plus the 2 m crude column (estimate, liquid crude at about 700 kg/m3: about 297 kPa). The light ends flash in the tank; the tank's top port draws its vapour, so the line above stands full of liquid crude. Rest within a minute or two.

**What the solver did.** At the hold the tank stands at 299.36 kPa, 330 K, 54 % liquid and 46 % vapour; the two junctions are liquid-full (291.2 and 283.0 kPa). Every step size ends on a singular Jacobian at pass 0. The worst row is the tank's flow row through its top (vapour) port (connection 0, -0.0276), then the loop's two parallel runs (5.2e-4 each).

**Cause (unconfirmed).** The tank stands about 2 kPa above its column balance with the generator: its vapour would push back up into a liquid-full junction through the top port, against a liquid-full loop whose two parallel runs carry equal flows. Card E2 (A169) named this world's singularity on a liquid crude junction. Not traced further: owner decision D44.

**Replay.** `../hold-classes/src-t16/T16CrudeCaseTest.replay` with the keep list in `case.txt`; failing rows `trace.txt`.
