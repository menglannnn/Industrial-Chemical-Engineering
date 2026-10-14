# phase-ports-probes/holds3

Diagnosis and fix of island hold F8 (a line placed block by block in the GUI-layout check, `../gui-layout/NOTES.md`: `fluid_island=46 committed_tick=11283 status=HELD: Singular Newton Jacobian: Sparse LU rejected a singular matrix; active-set pass=0`, 9 lines, the first 60 ms after an inline filter was placed) of the batch `documentation/2026-09-26-phase-ports-and-compressor/`, 2026-09-27, on `claude/phase-ports-compressor` from `8a58e4c1`. Review section: `PHASE_PORTS_REVIEW.md` "Island hold F8: singular Jacobian after a topology event (2026-09-27)". Code commits `28675064`, `4855fe82`, `65583a38`; the gate test `src/test/java/com/wormzjl/createcheme/runtime/fluid/InlineFilterLineHoldTest.java` stays in the code. Nothing here is run by a Gradle task.

## How to run

`holds3.init.gradle` attaches `src/` to the test source set for one invocation (and `instr/` with `-Pholds3Instr=true`):

```
./gradlew --no-configuration-cache --init-script tools/phase-ports-probes/holds3/holds3.init.gradle test --tests '*F8ReloadProbeTest'
```

`summarize.sh <prefix>` (from the worktree root) summarises the last test run's JUnit XML: counts, the 33 junction lines with `ms`, `allocatedMB` and `bytes` masked and their diff against `../holds/12-junction-lines.txt`, the test names.

## src/

- `F8ReloadProbeTest.java`: the in-game line (the gate test's `World`) run to online tick 24168, saved and loaded through the checkpoint codec, run on: on `8a58e4c1` the island stays held after the load, with the saved retry at tick 30418, 6250 ticks after the load (the second client run lasted 3080 ticks, so it logged nothing); it holds again at 30418 (`logs/06`).
- `F8LayoutProbeTest.java`: straight lines placed in one tick (generator, pump, filter, valve, void, tank in 13 arrangements); on `8a58e4c1` every pump discharging into a pressure control valve or into a filter with nothing past it holds at its first slice (`logs/04`), with the fix none (`logs/05`). The gate test carries 12 of them.

## instr/ (needs `instr-f8-singular-dump.patch`; never commit)

- `instr-f8-singular-dump.patch` (against `8a58e4c1`; applies to the fixed tree too): `SparseNewton.INSTR_SINGULAR` receives the Jacobian of a factorization that failed, `PassiveStepSolver.INSTR_LAYOUT` the Newton layout (every node's columns, every edge's flow and control column, modes) of a failed pass. `git apply` it, run with `-Pholds3Instr=true`, `git apply -R`.
- `F8NullVectorProbeTest.java`: replays the line up to the filter and prints the layout and the Jacobian's smallest singular values with their right and left singular vectors (EJML dense SVD), then solves the held island directly: its port graph's rate solve, backward-Euler steps of 0.05, 0.0125 and 0.001 s, a one-tick interval (`logs/03`). Also used by `../hold-fuzz/instr/`.

## logs/

| file | what |
|---|---|
| `01-baseline.gradle.txt` | the reproduction on `8a58e4c1`: prefixes 1-6 clean, 7 (the filter) and later hold at the filter's tick |
| `02-null-vector-and-reload.gradle.txt` | first probe run (the dump hook missed the factorization path; kept for the reload lines) |
| `03-null-vector.gradle.txt` | the layout and SVD at the hold: sigma 1.36e-17, null vector on the filter junctions' pressures and the pump head |
| `04-layouts-baseline.gradle.txt`, `05-fix1.gradle.txt` | the 13 layouts before and after the first fix |
| `06-baseline-reload-and-gate.gradle.txt` | `F8ReloadProbeTest` and the gate test on `8a58e4c1` (the gate test fails 3/3) |
| `07-f8-*` | fluid suites with the first fix and the gate test's first (too strict) assertion |
| `08-f8-gate-test.*` | the gate test alone, green, with its layout modes |
| `09-*`, `10-*` | the seed-61 pocket case (`aFilterPocketFedThroughASmallPipeNeverHolds`) green with `4855fe82`, failing without it |
| `11-G1-*` | gate G1 on `4855fe82`: 480/480, 33 junction lines identical, MIXED_GAS_COST 286; `11-G1-stdout.txt` the suites' printed lines |
| `12-G2-*`, `13-G3-*`, `14-G5-*` | gates G2 (exact regression 0.000e+00), G3 (adjacent 45/45), G5 (GameTests 33/33, run id `hold-f8-20260927`) on `4855fe82` |
| `15-baseline-*` | the fluid suites on `8a58e4c1`, stdout for the bitwise comparison (5469 lines, 0 differ, `../holds/src/StdoutCompare.java`) |
| `16-*`, `17-G1-*` | the gate test and G1 on `65583a38`: 480/480, junction lines identical, MIXED_GAS_COST 286, stdout 0 of 5469 lines differ |
| `18-G2-*`, `19-G3-*`, `20-G5-*` | G2 0.000e+00, G3 45/45, G5 33/33 (run id `hold-f8-final-20260927`) on `65583a38` |

`logs/` is matched by the repository's `logs/` ignore rule: force-add it (`git add -f`).
