# unified-thermo-port-m3-arrival

Purpose: the trace of the cold fuzz's methane-floor holds (class C at 90.6941 K) in the first interval after LNG arrives
in an empty tank (D54: the 298.15 K nitrogen is the ambient placeholder state, the inventory is zero), cold seeds 114 and
34 (the two smallest of 34, 98 and 114), at the M3 head `76e87740` and at M2's `a0f20364`, where both are clean. Batch
`2026-09-24-coolprop-low-temperature`; report `documentation/2026-09-24-coolprop-low-temperature/M3_COLD_ARRIVAL_TRACE.md`
(first divergence: `92d39275`, the stand-in adoption of a frost answer). Then the probe of the report's proposition A,
`staging/a-own-split.patch` on `76e87740` (logs 05 to 10; the report's "Probe" section). Diagnosis and probe only: no
source of the worktree was changed, no Gradle was run, no commit was made. Nothing here is a gate and nothing here was
ever tracked.

## How to run

The index lane of `tools/unified-thermo-port-m3/scripts/` (index-compile.sh, index-run.sh), adapted so that each commit
is exported into its own folder, `/tmp/m3/trace-<name>`, and the prints are added to that copy only:

    bash scripts/export.sh 76e87740          # git archive of src/main, src/test and both resource folders, the trace
    bash scripts/export.sh a0f20364          # switch (trace/M3ArrivalTrace.java) and the prints (trace/apply-prints.js)
    NAME=A PATCH=$PWD/staging/a-own-split.patch bash scripts/export.sh 76e87740    # 76e87740 with A: /tmp/m3/trace-A
    bash scripts/index-compile.sh 76e87740   # javac: every main source, then the harness and the test sources it needs
    bash scripts/index-run.sh ...            # M3Run with the exported tree's own resources, -Xmx6g, JAVA_OPTS
    bash scripts/trace-seed.sh <name> <seed> <from tick> <to tick> <label>
    bash scripts/run-classes.sh <name> <label> $(grep -v '^#' scripts/classes.txt)
    node scripts/masked-moves.js <before log> <after log>

`trace-seed.sh` waits while a dev client is live, then runs `harness/HoldFuzzTest.java` (the M3 cold harness,
`tools/unified-thermo-port-m3/hold-fuzz/src/HoldFuzzTest.java`, plus a trace window: the prints run for online ticks
`m3arr.from` to `m3arr.to`) on one seed, one worker, one slice after the last placement, verbose, with counters. For the
test classes, compile with `EXTRA_TESTS=` naming their sources in the exported tree (`scripts/classes.txt`); the prints
stay off outside the harness's window (every print site tests the switch, and their arguments only read state). The
worktree is read only (`git archive` and the classpath files of its `build/moddev`); `WT=` names another one.

## Contents

| Path | What it is |
|---|---|
| `scripts/export.sh` | Exports a commit into `/tmp/m3/trace-<name>` (`NAME=`), applies `PATCH=` and the prints (`PRINTS=0`: none) |
| `scripts/index-compile.sh`, `index-run.sh`, `M3Run.java` | The M3 index lane, pointed at the exported folders |
| `scripts/trace-seed.sh` | One traced seed (see above); logs to `/tmp/m3/arrival-logs` (`LOGS=`) |
| `scripts/run-classes.sh`, `classes.txt` | Test classes on the index lane as Gradle's `test` runs them (scheduler check on), after the dev client closes; the probe's 27 classes: the six M3 classes, group L, and every other test class citing D54 |
| `scripts/masked-moves.js` | The printed lines of two M3Run logs that move, test by test, under `printed-moves.js`'s masks |
| `staging/a-own-split.patch`, `staging/README.md` | Proposition A against `76e87740`, probed, never applied |
| `trace/M3ArrivalTrace.java` | The trace switch and its state and graph printers, added to an exported tree only |
| `trace/apply-prints.js` | The 29 print sites (interval attempts, step passes with seeds and supports, Newton iterations and line searches, phase checks, every adopted TP answer); every anchor is checked to occur once at both commits |
| `harness/HoldFuzzTest.java` | The M3 cold harness with the trace window |
| `logs/arrival-logs.tar.gz` | The traced runs and the probe, below |

## Logs (`logs/arrival-logs.tar.gz`, each run's `<label>.txt` and, for a seed, `<label>-report.txt`)

| Label | Run | Trace window (online ticks) | Outcome |
|---|---|---|---|
| `01-s114-76e87740` | 114, M3 head | 169 to 173 | held, class C, at tick 172 (again 179; island 7 at 180) |
| `02-s114-a0f20364` | 114, M2 | 169 to 173 | clean; the first interval converges in pass 1 at 112.11 K, 101 325 Pa |
| `03-s34-76e87740` | 34, M3 head | 480 to 483 | held, class C, at tick 482 (again 582, 667; island 15 at 668) |
| `04-s34-a0f20364` | 34, M2 | 480 to 483 | clean; pass 1 converges at 113.05 K |
| `05-s114-A` | 114, A | 169 to 173 | clean; pass 0 starts at its root, 112.114 K and 101 325 Pa; 12.89 kg of crystal at the end |
| `06-s34-A` | 34, A | 480 to 483 | clean; 113.048 K, 101 325 Pa; 1.70 kg of crystal |
| `07-s98-A` | 98, A | 1559 to 1564 | clean; node 14 is an empty tank fed LNG at 142 K: 112.167 K, 101 325 Pa; no crystal |
| `08-classes-76e87740`, `09-classes-A` | the 27 classes, at the head and with A | none | 147 of 147 pass in each; 501 printed lines in each |
| `10-masked-moves-A.txt` | `masked-moves.js` of 08 against 09 | | 74 lines move, 73 in EmptyTankFillsTest and 1 in TracePhaseFoldTest, at most 1.9e-12 relative (the first step starts at its own split; same root) |

Reading notes: the `ADOPT` lines at 298.15 K (1 and 40.89 mol of N2) at a window's start evaluate the ambient placeholder
state, not an inventory. `NEWTON` lines between `PHASECHECK` and `own split` (log 02: T=355 K, "describe failed") are the
nested own-split solve read through the network's describer. Log 01's report also lists `x:exception` ("Unknown island
5", after the hold): the verbose harness observing an island the filter's placement replaced; not part of the hold. In
logs 05 to 07 the second `ADOPT` of the first attempt is the engine's PH answer of what arrives (A's start).

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Trace of the cold fuzz's new class C holds (LNG into an empty tank at methane's floor, seeds 114 and 34) at `76e87740` against `a0f20364` on the index lane, and the D31 probes: lane scripts, print specs, staged patches, archived logs

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`; `staging/`

### History

- Done 2026-10-16 (D31 decided, A and B in the port; report `M3_COLD_ARRIVAL_TRACE.md`)
- merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed)
