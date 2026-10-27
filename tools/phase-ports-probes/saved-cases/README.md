# phase-ports-probes/saved-cases

Replayable hold worlds saved for the thermodynamics work (owner decision D44, 2026-10-13: "Thermo package issue not covered in this work, save the test case for later use"). Batch `documentation/2026-09-26-phase-ports-and-compressor/`, thread t16, branch `claude/rest-resolution`. Review section: `PHASE_PORTS_REVIEW.md` "The certificate is the gauge: rest at 0.1 Pa, zero-flow loops, saved crude cases (2026-10-13)", section on D44. Card: `REMAINING_HOLDS.md` "D44: random crude two-phase holds, saved for the thermodynamics work".

**These worlds wait for the thermodynamics work.** They are random-distribution fuzz worlds whose island holds on a hot two-phase crude node (hydrocarbon vapour and liquid in one tank or junction). They are out of the phase-ports batch's zero goal (D44). Nothing here is run by a Gradle task or a gate.

## One folder per seed

`<seed>/case.txt` holds:
- the fuzz seed, the distribution (random) and the minimized placement list (`keep`: `index@tick` into the seed's original placement order);
- every block as the fuzz harness places it: its placement tick, kind, position, facing, bore, control and its fluid spec (temperature, pressure, composition);
- the first hold: online tick, island, committed tick and the full status text (the failing row's reason);
- the class (E2: singular step at every step size; E5: line search stalls) and whether the held island has a two-phase crude node;
- the held island at the hold: every node (kind, height, pressure, temperature, mass, phase volumes) and every connection (ends, control, bore, ports).

`<seed>/card.md` is the case's card in `REMAINING_HOLDS.md` format (world, settings, expectation from physics, what the solver did, cause, seeds).

## How to replay

From a worktree root with `tools/` (a Gradle run; at most two at a time on the machine, AGENTS.md):

```
bash tools/phase-ports-probes/hold-classes/t16-crude.sh logs/<tag> <seed>      # minimize again and rewrite <seed>/case.txt
./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-classes/t16.init.gradle test \
    --tests '*T16CrudeCaseTest.replay' -Pt16case=<seed> -Pt16keep="<keep list from case.txt>" -PholdFuzzDefaults=false
```

`T16CrudeCaseTest` (`../hold-classes/src-t16/`) builds the seed's world with `HoldFuzzTest.world` and `placements`, keeps the listed blocks at their listed ticks, and runs the fuzz world (the real `PhysicalRegistry` and `IslandCoordinator`, one synchronous worker) to its first hold. The failing rows of a hold are printed by `../hold-classes/src/instr-t13-failing-rows-and-levers.patch` with `../hold-classes/src-t13/T13TraceProbeTest.trace` (`-Pt13seeds=<seed> -Pt13keep=<keep>`).

## Cases

Minimized on the branch's final code (`f2b6a987`, campaign `z18`; A223): the random E2 and E5 seeds of `z18` whose first hold's island holds a two-phase hydrocarbon node. Random E2 163 and 854 and E5 815 and 829 hold elsewhere (water or gas nodes) and are not saved; `o02`'s random E2 311 and E5 553, 629, 807 are clean on the final code. Each case reproduces its hold to the tick under the trace (`trace.txt`, log `z23`).

| Seed | Class | Blocks | Hold tick | Failing row (last failed Newton, pass 0) | Two-phase node |
|---|---|---|---|---|---|
| 279 | E2 singular | 6 | 2866 | tank's flow row 3.4e-9 (converged, matrix singular) | tank, 292.4 kPa, 325.5 K, 54 % liquid |
| 298 | E2 singular | 13 | 4089 | two junctions' continuity rows +-0.44 | three tanks, 215-225 kPa, 20-24 % liquid |
| 372 | E2 singular | 14 | 3636 | two-phase junction's continuity row 1.0e-6; fugacity rows all 8.7e-10 | crude-nitrogen junctions, 316.7 and 321.3 kPa |
| 521 | E2 singular | 6 | 1195 | tank's flow row through its top port -0.028 | tank, 299.4 kPa, 329.7 K, 54 % liquid |
| 547 | E5 line search stall | 13 | 1936 | junction's continuity row 13.44 | lower junctions, 99.4 and 114.5 kPa, draining into 1 atm voids |
| 786 | E2 singular | 8 | 1690 | tank's energy row 2.7e-9 (converged, matrix singular) | tank, 229.7 kPa, 320.3 K, 38 % liquid |
| 818 | E2 singular | 5 | 2974 | junction's continuity row 1.7e-8 (converged, matrix singular) | tank, 194.1 kPa, 309.2 K, 45 % liquid |

Common to all seven: a hot (309-361 K) light crude from a generator at 242-337 kPa expanding into a nitrogen tank or towards 1 atm voids, the node at the hold two-phase. In three (279, 786, 818) the Newton stands at or near its tolerance and the matrix at that point cannot be factored; in 372 it stands at 1e-6 with every fugacity row of a two-phase junction carrying the same residual; in two (298, 521) the singular matrix comes with a large residual; one (547) stalls with its two-phase junction below the voids it drains into. Causes not traced (D44).
