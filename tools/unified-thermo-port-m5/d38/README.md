# unified-thermo-port-m5/d38

Purpose: the D38 agent's material (owner decision D38, 2026-10-17). Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, two commits on `65036f2b`: `26090bf0` (D36 reverted: gas weighs its density again in every static column) and `67e5f3e7` (rule C: one vanishing rule for every node). Report: `documentation/2026-09-24-coolprop-low-temperature/D38_REVERT_D36_AND_VANISHING_RULE.md`. The rule C gate is tracked: `HoldClassWorldsTest#aJunctionsVapourCollapsingInASubcooledLiquidLeavesAtAFailedIterate`. Nothing here was removed from or is attached to a tracked path (the D36 gates of `RestResolutionTest` left with the revert and stay in `03bc70f6`).

## Contents

| Path | What |
|---|---|
| `lane.sh` | The Gradle-free javac lane of `../simplifications/lane.sh`, exporting under `/tmp/d38-<name>`: `export <name> <tree-ish> [patch...]` (absolute patch paths), `compile <name> [test files relative to src/test/java, or all]`, `run <name> <selectors>` (M3Run, one JVM at -Xmx6g, `JAVA_OPTS` as system properties) |
| `tree.sh` | Prints a git tree object of the worktree's current `src/` through a temporary index (HEAD, the index and the stash untouched) |
| `run-touching.sh <name> [group...]` | Runs the groups `classes-<group>.txt` (17, as in `../simplifications`), one JVM per group, with `-Dcreatecheme.fluid.scheduler.verify=true`. Logs: `$LOGS/<name>-<group>.txt` (default `/tmp/d38-logs`) |
| `notime.pl`, `movesize.pl` | Read a `compare-test-output.pl` output: TIMING-ONLY against PHYSICS per moved test (allocatedMB counts as physics there), and the largest relative change |
| `rule-c-off.patch` | `git diff 1dd486c4 26090bf0 -- src/main`: `67e5f3e7`'s main changes reversed (the gate and the cold `EventPathWorld` stay); applies on `67e5f3e7` |
| `probe/D38SeedWorldProbe.java`, `probe-export.sh` | Prints a cold seed's KEEP-list world block by block to full precision (where the gate's specs come from) and its class at the exported code (`M5MinimizerTest.run`). `probe-export.sh <name> <tree-ish>` exports with the cold harness (`../../unified-thermo-port-forms/hold-fuzz-cold`) and the M7 trace world (`../trace/src`) added, then compiles; run with `-DholdFuzz.cold=true` (`-Dd38.seed`, `-Dd38.keep`, `-Dd38.slices`) |
| `instr.sh <name>` | The trace hooks on an export of `67e5f3e7`'s phaseCheck: `D38V fold` (a phase only the vanishing bits remove: node kind, bits, the A85 record's identity, which repeats on the converged path and is new on a failed iterate's copy, amounts now and at the solve's start), `D38V refuse` (the D26 trajectory refusal), `D38V cycle` (before "Phase/device active-set cycle") |
| `logs/d38-logs.tar.gz` | `/tmp/d38-logs` and the run summaries (below) |

The logs: `rev-<group>` (commit 1's tree), `c2b-<group>` (the literal rule: 12 failures, all pass cycles), `c2c-<group>` (the committed rule), `cmp-rev-*` (commit 1 against its parent), `cmp-c2c-*` (commit 2 against commit 1), `jl-{d34,d36,head,rev,c2c}.txt` (the 33 junction lines, ms/bytes/allocatedMB as `*`), `gate-{c2b,c2c,c2off}.txt` (the gate with the rule, and with `rule-c-off.patch`), `trace-overflow5.txt`, `trace-failing-v2.txt`, `trace-failing-v3.txt` (the 12 failures traced: humidity-only fold, then the same-check re-offer, then all pass), `trace-wicm.txt` (WaterIntoCryogenicMethaneTest's folds), `trace-seed48.txt` (seed 48 with the hooks), `probe-seed30-{c1,c2a,c2final}.txt` (seed 30's 11-block world: E5 at tick 1595 at commit 1, clean to 5593 after), `bisect-d38-{c1,c2}.txt` and `bisect/` (`bisect-seeds.sh` reports: 30 and 48 at `67e5f3e7`, 48 at `26090bf0`), `runs-{rev,c2b,c2c}.txt` (pass counts), `commit{1,2}-msg.txt`.

## How to run (Git Bash, from the worktree root; no Gradle)

Tests read `examples/` and write `build/reports/fluid/` relative to the current directory, so run from the worktree root.
```
D=tools/unified-thermo-port-m5/d38; L=$D/lane.sh; C=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl
bash $L export c2 67e5f3e7 && bash $L compile c2 all && bash $D/run-touching.sh c2 science-fluid-network runtime-fluid
bash $L export rev 26090bf0 && bash $L compile rev all && bash $D/run-touching.sh rev science-fluid-network runtime-fluid
perl $C /tmp/d38-logs/c2-runtime-fluid.txt /tmp/d38-logs/rev-runtime-fluid.txt > /tmp/d38-logs/cmp.txt; perl $D/notime.pl /tmp/d38-logs/cmp.txt
G='com.wormzjl.createcheme.runtime.fluid.HoldClassWorldsTest#aJunctionsVapourCollapsingInASubcooledLiquidLeavesAtAFailedIterate'
bash $L export off 67e5f3e7 "$(cygpath -m "$PWD/$D/rule-c-off.patch")" && bash $L compile off com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java
JAVA_OPTS=-Dcreatecheme.fluid.scheduler.verify=true bash $L run off "$G"   # fails: held at tick 1595, |r| = 1.4743852805310098E-9
bash $D/probe-export.sh p 67e5f3e7 && JAVA_OPTS=-DholdFuzz.cold=true bash $L run p com.wormzjl.createcheme.runtime.fluid.D38SeedWorldProbe
bash tools/unified-thermo-port-m5/scripts/bisect-seeds.sh HEAD <label> 30,48   # Gradle, clean tracked tree; detaches and returns
```
Extract the logs with `tar --force-local -xzf logs/d38-logs.tar.gz`.
