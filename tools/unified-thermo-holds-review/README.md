# unified-thermo-holds-review

Purpose: sort the non-converged (held) seeds of the final campaign of the unified thermo port and judge each mechanism
group from physics (owner request 2026-10-18: "Sort the non-converged cases, analyze and see if they actually makes
sense"). Thread t37, batch `2026-09-24-coolprop-low-temperature`. Report:
`documentation/2026-09-24-coolprop-low-temperature/NON_CONVERGED_CASES_REVIEW.md`. Analysis only: no tracked path was
changed; the trace hooks were applied to a `git archive` export, never to the worktree.

## Inputs
- Campaign `build/m11-logs/` of the worktree at `688f4cd7` (cold random `04-…`, bundled random `05-…`), classified by
  `tools/phase-ports-probes/hold-fuzz/classify.sh`.
- Rig holds H4 and H5 from `TEST_WORLD_RIG_HOLDS_TRACE.md` and `DECISION_LOG.md` (not re-run).

## Contents
| Path | What |
|---|---|
| `holds-table.tsv` | One row per held seed (89 cold, 14 bundled) plus H4 and H5: class, first hold tick, committed tick, island, the topology event before it, the primary failure and residual, the group K1-K8, the evidence (`trace (t37)`, `prior: <trace>` or `signature`), the node or line at the hold (from the held island's committed state, where printed), the island's feeds and devices |
| `holds-table.js` | Report → raw table (`raw-cold.tsv`, `raw-bundled.tsv`): first hold, event blocks and the held island rebuilt from the layout with the harness's adjacency rule (`HoldFuzzTest.connects`; only pipes join) |
| `held-islands.js` | Verbose replay log → the held island's committed nodes and lines per seed (`islands-cold.txt`, `islands-bundled.txt`) |
| `tag-seeds.js` | Physical tags per seed (`tags-*.tsv`): feeds, 1 atm sink, flashing or bubble-point junctions, liquid-full tank, warm-meets-cold, blocked lines, caps, pump modes |
| `assign-groups.js` | Tags, traces and prior traces → `holds-table.tsv`; the rules are in the file (traced and prior seeds fixed, the rest by residual, liquid-full tank, mixing) |
| `base-rates.js` | Hold rate by world feature over all 1000 seeds of a report |
| `lane/compile.sh`, `lane/run.sh` | Copies of `tools/unified-thermo-port-test-world/index-lane/` with the export moved out of `/tmp` (job folder) and an empty probe folder allowed (`nullglob`) |
| `logs/t37-logs.tar.gz` | `build/t37-logs/`: the verbose replay (`cold-random*`, `bundled-random*`, console) and the six row traces `trace/01-…`-`06-…` |

## How it was run (worktree root, Git Bash; nothing else running but the owner's geosim)
```
F=D:/Minecraft/Modding/1.21/CreateChemE/tools; R=tools/unified-thermo-holds-review
# 1. Verbose replay of the held seeds (one Gradle run per distribution, 4 workers): classes equal to m11
OUT=build/t37-logs THREADS=4 HARNESS=$F/unified-thermo-port-forms/hold-fuzz-cold DISTS=random EXTRA="-PholdFuzzCold=true -PholdFuzzVerbose=true" bash $F/unified-thermo-port-m3/fuzz.sh cold "<cold seeds>"
OUT=build/t37-logs THREADS=4 HARNESS=$F/unified-thermo-port-forms/hold-fuzz-bundled DISTS=random EXTRA="-PholdFuzzVerbose=true" bash $F/unified-thermo-port-m3/fuzz.sh bundled "<bundled seeds>"
# 2. Tables
node $R/holds-table.js cold build/m11-logs/04-campaign-cold-random-report.txt build/m11-logs/04-campaign-cold-random-report.txt.classes.txt > $R/raw-cold.tsv
node $R/holds-table.js bundled build/m11-logs/05-campaign-bundled-random-report.txt build/m11-logs/05-campaign-bundled-random-report.txt.classes.txt | tail -n +2 > $R/raw-bundled.tsv
node $R/held-islands.js build/t37-logs/cold-random.gradle.txt > $R/islands-cold.txt   # and bundled
node $R/assign-groups.js $R/raw-cold.tsv $R/raw-bundled.tsv $R/islands-cold.txt $R/islands-bundled.txt > $R/holds-table.tsv
# 3. Row traces at 3db5e5d3 (PassiveStepSolver at 688f4cd7 is consolidated; the M5 hooks apply at 3db5e5d3, whose classes are the same)
TR=$F/unified-thermo-port-m5/trace
PATCH="$TR/patches/instr-m5-trace-hooks-on-00ee8385.patch" EXTRA_PROBES="$TR/src-instr/M5RowTrace.java $TR/src-instr/M5RowTraceTest.java $TR/src/M5TraceWorld.java $TR/src/M5SolveLogTest.java $TR/src/M5MinimizerTest.java $F/unified-thermo-port-forms/hold-fuzz-cold/src/HoldFuzzTest.java" bash $R/lane/compile.sh t37 3db5e5d3
bash $R/lane/run.sh t37 com.wormzjl.createcheme.runtime.fluid.M5RowTraceTest -DholdFuzz.cold=true -Dreplay.cases=694 -Dreplay.history=true
```
Cases traced: cold `694`, `1` (`-Dm5.fullFails=6`), `518`, `535`, `521#22:12644` (`-Dreplay.slices=120 -Dm5.passLines=60`);
bundled `504#20:1469` (label `t37b`: the same export with the bundled harness `hold-fuzz-bundled/src/HoldFuzzTest.java`).

Note: in verbose mode the harness's `describe` throws "Unknown island" when a later hold names an island already merged
away; the seed's first hold is printed before that for 56 of 89 cold and 9 of 14 bundled seeds. The others are grouped
from the layout and their status.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Thread t37 (batch `2026-09-24-coolprop-low-temperature`): the held seeds of the m11 campaign (`688f4cd7`) and rig holds H4, H5 sorted by mechanism: `holds-table.tsv` (one row per seed: class, tick, island, event, node or line at the hold, group K1-K8, evidence), report and island parsers, tagging and grouping scripts, base rates, private copies of the index-lane `compile.sh`/`run.sh`, `logs/t37-logs.tar.gz` (verbose replay, six row traces)

Batch: `2026-09-24-coolprop-low-temperature`

Status: Concluded 2026-10-18 (analysis only). Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `holds-table.tsv`, `assign-groups.js`

### History

- Concluded 2026-10-18 (analysis only)
