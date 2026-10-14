# phase-ports-probes/hold-fuzz

Hold search by placement-sequence fuzz, batch `documentation/2026-09-26-phase-ports-and-compressor/`, 2026-09-27, on `claude/phase-ports-compressor` (`8a58e4c1` to `65583a38`). Review section: `PHASE_PORTS_REVIEW.md` "Hold search by placement-sequence fuzz (2026-09-27)". Not part of any gate suite: without the init script nothing here is compiled.

## What it does

`src/HoldFuzzTest.java` builds, per seed, a small world on a grid from `new Random(seed)`: 2 to 12 items (tank, generator of water, nitrogen, methane or the two-phase light crude at a random temperature and pressure, void, pipe runs of 0.01/0.02/0.05/0.1 m, pump, compressor, inline filter, pressure control valve with random controls, joined by pipe runs leaving any face of an existing block, so tanks are reached from their top, bottom and side faces and branches make junction pipes). It places the blocks one by one in a random order at random online ticks (several in one tick, a few ticks apart, or up to 400 ticks apart, so topology events fall inside and between 5 s slices) through the world's event path, the real `PhysicalRegistry` and `IslandCoordinator` with one synchronous worker, run tick by tick as `FluidWorldAuthority.tick` runs, then runs 120 slices at the 5 s cadence. Every published HELD status (seed, tick, island, committed tick, reason) and every thrown exception is recorded; a refused placement (a spec outside the property domain) is recorded apart and is not a hold. With `-PholdFuzzDefaults=true` every block is made as `FluidWorldAuthority.place` makes it (0.05 m, default controls, water generators at 298.15 K and 1 atm) from the same draws, so a seed keeps its layout and order. Since 2026-09-28 (owner decision D28, only pipes connect) a device drawn directly against a block that is not a pipe hangs off one pipe block instead (`holdFuzz.pipeBetween`, default on; `-PholdFuzzPipeBetween=false` builds the earlier worlds); campaigns before and after the switch are not comparable.

Deterministic per seed: a seed replays alone (`-PholdFuzzSeeds=137`), with `-PholdFuzzVerbose=true` printing each held island (nodes, states, connections, modes). Workers run seeds in parallel, each world confined to its own thread.

## How to run

```
./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/hold-fuzz/hold-fuzz.init.gradle test --tests '*HoldFuzzTest' \
    -PholdFuzzSeeds=1-1000 -PholdFuzzThreads=8 -PholdFuzzOut=<absolute report path> [-PholdFuzzDefaults=true] [-PholdFuzzVerbose=true] [-PholdFuzzStrict=true]
bash tools/phase-ports-probes/hold-fuzz/classify.sh <report>     # class table by each seed's first hold -> <report>.classes.txt
./gradlew ... --init-script .../hold-fuzz.init.gradle test --tests '*HoldClass*ReproTest'   # the class reproductions
```

A 1000-seed campaign takes 2 to 4 minutes on 8 workers (one Gradle invocation, nothing else running; the heap is 8 GB). `-PholdFuzzInstr=true` adds `instr/` and `../holds3/instr/` (apply `../holds3/instr/instr-f8-singular-dump.patch` first, `git apply -R` after): `HoldSeedSingularProbeTest` prints the layout and SVD of the first singular factorization of each seed.

## Files

- `src/HoldFuzzTest.java`: the generator, the world, the campaign runner and report; `assertSeedRunsClean(seed)` for the reproductions.
- `src/repro/HoldClass*ReproTest.java`: one reproduction per hold class, the class's smallest seed of the campaign on `65583a38` (`logs/08`); all 13 fail on `65583a38` (`logs/12`). The javadoc of each states the class.
- `classify.sh`: the class table of a report (by the primary failure of each seed's first hold, the text before its rejection map).
- `option-r1-rate-pass-failure-tolerated.patch`: option R1 (not committed), a failed interval-start rate pass of `SolidEventIntegrator` declares no closure instead of holding (`logs/10`).
- `instr/HoldSeedSingularProbeTest.java`: see above.

## logs/

| file | what |
|---|---|
| `00-smoke*` | 16 seeds, harness check |
| `01-campaign-1-1000*` | seeds 1-1000 on `28675064` (first F8 commit): 670 clean |
| `02-verbose*` | 27 representative seeds replayed on one thread with the held islands printed |
| `03-singular-probe*`, `05-singular-probe-2*` | SVD of the first singular factorization of seeds 61, 474, 4 and 118, 175, 236 |
| `04-d-seeds-after-deadend*` | the 36 singular-pass-0 seeds with the dead-end generalization: 17 clean |
| `06-campaign-f8-deadend-1-1000*` | seeds 1-1000 on `4855fe82`: 698 clean |
| `07-campaign-baseline-8a58e4c1-1-1000*` | seeds 1-1000 on `8a58e4c1` (the solver before F8): 593 clean |
| `08-campaign-closed-start-1-1000*` | seeds 1-1000 on `65583a38` (final): 703 clean; the class table `08-...-report.txt.classes.txt` |
| `09-smallest-seeds-verbose*` | each class's smallest seed replayed with the held islands printed |
| `10-option-r1-1-1000*` | option R1 on `65583a38`: 720 clean, 503 s |
| `11-campaign-defaults-1-1000*` | seeds 1-1000 on `65583a38` with the world's placement defaults: 702 clean |
| `12-repro-tests-on-65583a38.gradle.txt` | the 13 reproductions, 13 failing |

`logs/` is matched by the repository's `logs/` ignore rule: force-add it (`git add -f`).
