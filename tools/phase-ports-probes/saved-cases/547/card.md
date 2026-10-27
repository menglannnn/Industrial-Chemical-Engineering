# Saved case random 547 (E5, D44: waits for the thermodynamics work)

**World (random seed 547, 13 blocks after minimization keeping class E5 and a two-phase crude node; `case.txt`).**

| Block | Position | Setting |
|---|---|---|
| Crude generator | -1, 1, -2 | two-phase light crude, 361.4 K, 307.7 kPa |
| Voids | -2,0,0; -1,-2,0 (0.1 m); 2,-1,0 (0.1 m) | nitrogen, 101.3 kPa |
| Tank | -2, 3, 0 | nitrogen, 101.3 kPa, 1 m3 |
| Pipes | -2,1,0; -1,1,0; -1,1,-1; -2,2,0; -1,0,0 | 0.05 m |
| Pipes | 0,-1,0; -1,-1,0; 1,-1,0 | 0.1 m |

A hot crude generator feeds a network of junctions that drains into three voids below and rises to a tank two blocks up.

**Settings.** Random distribution. Placed at ticks 341 to 1931; the island holds at tick 1936 (committed 1932), five ticks after the last block.

**Expectation from physics.** The crude drains into the three voids, 206 kPa above them, and flashes hard as it expands (361 K crude to 1 atm): the lower junctions hold a foaming two-phase stream. The tank above fills slowly through its bottom port. A steady flow, mostly vapour by volume in the lower junctions.

**What the solver did.** At the hold the lower junctions are two-phase (liquid and vapour, 114.5 and 99.4 kPa; the second 1.9 kPa below the voids it drains into). Every step size ends on a stalled line search at pass 0, residual 13.44. The worst row is that junction's continuity row (13.44), then three neighbours' (0.21, 0.15, 0.15).

**Cause (unconfirmed).** A two-phase crude junction whose state sits below its downstream voids while draining into them: the step cannot find a pressure at which its continuity closes. Card E5 (A169) named this world as stalling on a two-phase crude junction. Not traced further: owner decision D44.

**Replay.** `../hold-classes/src-t16/T16CrudeCaseTest.replay` with the keep list in `case.txt`; failing rows `trace.txt`.
