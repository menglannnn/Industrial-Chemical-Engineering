# unified-thermo-port-m5/d39

Purpose: the D39 agent's material (owner decision D39, 2026-10-18). Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, two commits on `482264fc`: `1482c277` (seed 430, D28 completed: a failed iterate's check folds what the iterate empties and proposes nothing) and 00ee8385 (seed 48: a proposal the next pass refutes is refused). Report: `documentation/2026-09-24-coolprop-low-temperature/D39_FAILED_ITERATE_AND_REFUTED_PROPOSAL.md`. Both gates are tracked in `HoldClassWorldsTest`: `aFailedIterateRaisesNoPhaseHold` (seed 430, 8 blocks) and `aBubbleTheNextPassDrivesToNothingIsRefused` (seed 48, 6 blocks). Nothing here was removed from or is attached to a tracked path.

## Contents

| Path | What |
|---|---|
| `lane.sh` | The Gradle-free javac lane of `../d38/lane.sh`, exporting under `/tmp/d39-<name>`: `export <name> <tree-ish> [patch...]` (absolute patch paths), `compile <name> [test files relative to src/test/java, or all]`, `run <name> <selectors>` (M3Run, one JVM at -Xmx6g, `JAVA_OPTS` as system properties) |
| `tree.sh` | Prints a git tree object of the worktree's current `src/` through a temporary index (HEAD, the index and the stash untouched) |
| `run-touching.sh <name> [group...]` | Runs the groups `classes-<group>.txt` (17, as in `../d38`), one JVM per group, with `-Dcreatecheme.fluid.scheduler.verify=true`. Logs: `$LOGS/<name>-<group>.txt` (default `/tmp/d39-logs`) |
| `notime.pl`, `movesize.pl` | Read a `compare-test-output.pl` output: TIMING-ONLY against PHYSICS per moved test (allocatedMB and checkpoint counts count as physics there; read them by hand), and the largest relative change |
| `rule-1-off.patch` | `git diff 7b887be4 482264fc -- src/main`: `1482c277`'s main changes reversed (the gate stays); applies on `1482c277` |
| `rule-2-off.patch` | `git diff e30cacfb 1482c277 -- src/main`: `00ee8385`'s main changes reversed (the gate stays); applies on `00ee8385` |
| `probe/D38SeedWorldProbe.java`, `probe-export.sh` | `../d38`'s probe: prints a cold seed's KEEP-list world block by block to full precision (where the gates' specs come from) and its class at the exported code. `probe-export.sh <name> <tree-ish>` exports with the cold harness and the M7 trace world added, then compiles; run with `-DholdFuzz.cold=true -Dd38.seed=<s> -Dd38.keep=<list>` |
| `instr.sh` | `../d38/instr.sh` with `/tmp/d39-` paths (D38 C fold, refusal and cycle hooks) |
| `refute-trace.pl` | The D39 B hook: a `D39REFUTE` line per refuted proposal (dt, pass, node, bits, the vapour share at the seed and at the failed iterate) |
| `logs/d39-logs.tar.gz` | `/tmp/d39-logs` and the run summaries (below) |

The logs: `probe-{430,48}-{head,p1,p2}.txt` (the minimal worlds at `482264fc`, commit 1 and commit 2), `gate1-c1.txt`, `gate1-c1off.txt`, `gate2-c2.txt`, `gate2-c2off.txt` (each gate alone with its rule and with its rule-off patch), `gate2-scratch*.txt`, `gate2-trace.txt` (seed 48's world tick by tick, and with `refute-trace.pl`), `c1-<group>.txt`, `c2-<group>.txt` (16 groups, all but fluid-benchmark, at each commit's tree), `cmp-c1-*` (commit 1 against the D38 baseline `c2c` of `../d38`), `cmp-c2-*` (commit 2 against commit 1), `jl-{c1,c2}.txt` (the 33 junction lines, ms/bytes/allocatedMB as `*`), `runs-{c1,c2}.txt` (pass counts), `bisect/` and `bisect-d39-c2.txt` (`bisect-seeds.sh` reports of seeds 430, 48, 30 and 19 at `00ee8385`), `commit{1,2}-msg.txt`, `c2off-export.txt` and `g2off-export.txt` (the rule-off exports).

## How to run (Git Bash, from the worktree root; no Gradle but the bisect)

Tests read `examples/` and write `build/reports/fluid/` relative to the current directory, so run from the worktree root.
```
D=tools/unified-thermo-port-m5/d39; L=$D/lane.sh; C=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl
bash $L export c2 00ee8385 && bash $L compile c2 all && bash $D/run-touching.sh c2 science-fluid-network runtime-fluid
perl $C /tmp/d39-logs/c2-runtime-fluid.txt /tmp/d39-logs/c1-runtime-fluid.txt > /tmp/d39-logs/cmp.txt; perl $D/notime.pl /tmp/d39-logs/cmp.txt
G=com.wormzjl.createcheme.runtime.fluid.HoldClassWorldsTest
bash $L export off2 00ee8385 "$(cygpath -m "$PWD/$D/rule-2-off.patch")" && bash $L compile off2 com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java
bash $L run off2 "$G#aBubbleTheNextPassDrivesToNothingIsRefused"   # fails: held at tick 1257, |r| = 4.0367655433637406E-8
bash $L export off1 1482c277 "$(cygpath -m "$PWD/$D/rule-1-off.patch")" && bash $L compile off1 com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java
bash $L run off1 "$G#aFailedIterateRaisesNoPhaseHold"              # fails: held at tick 406, "a third fluid phase ... at 17.35 K"
bash $D/probe-export.sh p 00ee8385 && JAVA_OPTS="-DholdFuzz.cold=true -Dd38.seed=48 -Dd38.keep=1@166,5@323,10@598,12@1185,13@1207,14@1254" bash $L run p com.wormzjl.createcheme.runtime.fluid.D38SeedWorldProbe
bash tools/unified-thermo-port-m5/scripts/bisect-seeds.sh HEAD <label> 430,48,30,19   # Gradle, clean tracked tree; detaches and returns
```
Seed 430's KEEP list: `0@100,1@100,2@122,3@277,4@314,6@364,9@404,10@404`. Extract the logs with `tar --force-local -xzf logs/d39-logs.tar.gz`.
