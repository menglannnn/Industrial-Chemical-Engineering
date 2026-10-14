# Saved case random 298 (E2, D44: waits for the thermodynamics work)

**World (random seed 298, 13 blocks after minimization keeping class E2 and a two-phase crude node; `case.txt`).**

| Block | Position | Setting |
|---|---|---|
| Crude generator | 0, 0, -3 | two-phase light crude, 339.5 K, 241.6 kPa |
| Tanks | 0,2,-4; 0,3,-1; 1,2,-1 | nitrogen, 101.3 kPa, 1 m3 each (the last on a 0.02 m bore) |
| Pipes | 0,2,-2; 0,0,-2; 0,2,-1; 0,2,-3; 0,1,-1; 0,-1,-1; 0,0,-1 | 0.05 m |
| Pipes | 0,-1,-2 (0.01 m); 1,1,-1 (0.02 m) | |

A hot crude generator at the bottom fills three tanks two and three blocks up through a small mesh of junctions.

**Settings.** Random distribution. Placed at ticks 478 to 2362; the island holds at tick 4089 (committed 3989).

**Expectation from physics.** The crude rises 2-3 m into the tanks, compressing their nitrogen until each tank's pressure plus the crude column reaches the generator's 241.6 kPa (estimate, liquid crude at about 700 kg/m3: about 228 kPa at 2 m and 221 kPa at 3 m; less where the columns hold vapour). The light ends flash in the tanks; each holds a two-phase crude. Then rest.

**What the solver did.** At the hold the three tanks stand at 225.08, 215.11 and 225.07 kPa, two-phase (20-24 % liquid); the junctions are liquid-full. Every step size ends on a singular Jacobian at pass 0. The worst rows are two junctions' continuity rows, +0.4407 and -0.4406 (the junctions at heights 1 and 0), then a third junction's (0.0038): mass leaves one junction and arrives at the next, and the step cannot move it.

**Cause (unconfirmed).** Liquid-full crude junctions between two-phase crude tanks near their column balance; the singular matrix at a large residual pair is not explained here. Not traced further: owner decision D44, the thermodynamics package's work.

**Replay.** `../hold-classes/src-t16/T16CrudeCaseTest.replay` with the keep list in `case.txt`; failing rows `trace.txt`.
