# unified-thermo-port-m5/simplifications

Purpose: the D37 fix agent's material (owner decision D37, 2026-10-17: the six approved small physics simplifications S1-S6). Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, one commit per item on `2d2f5a2c`: S5 `bff22e37` (a liquid stream's velocity limit is the cap), S3 `819d774b` (one refresh per interval start), S6 `9445106a` (no mover efficiency; checkpoint format 10), S4 `22b76697` (nitrogen's cp held below 273.16 K), S1 `eec02758` (no potential energy in enthalpies or ledgers), S2 `65036f2b` (no approximate fallback). Report: `documentation/2026-09-24-coolprop-low-temperature/SIMPLIFICATIONS_S1_S6.md`. The S1 gate is tracked: `BackwardEulerStepTest#aPumpedTenMetreLiftOfWaterDeliversItsLiftWorkAsHeat`. Nothing here was removed from or is attached to a tracked path; the edit scripts were applied once and are kept as the record of the mechanical part of S6 and S2.

## Contents

| Path | What |
|---|---|
| `lane.sh` | The Gradle-free javac lane of `../d36/lane.sh`, exporting under `/tmp/simp-<name>`. `export <name> <tree-ish> [patch...]`, `compile <name> [test files or all]` (prints `lane compile <name> main=<rc> test=<rc> gametest=<rc>`), `run <name> <selectors>` (M3Run, one JVM at -Xmx6g, `JAVA_OPTS` as system properties) |
| `tree.sh` | Prints a git tree object of the worktree's current `src/` through a temporary index (HEAD, the index and the stash untouched), for `lane.sh export <name> "$(bash tree.sh)"` |
| `run-touching.sh <name> [group...]` | Runs the class groups `classes-<group>.txt`, one JVM per group, with `-Dcreatecheme.fluid.scheduler.verify=true` as the Gradle test tasks set it. Logs: `$LOGS/<name>-<group>.txt` (default `/tmp/simp-logs`) |
| `classes-*.txt` | 17 groups (class count): `science-fluid-network` (37), `runtime-fluid` (61), `runtime` (7; FluidFallbackQualificationTest dropped with S2), `science-fluid-thermo` (16), `science-material` (15), `science-thermo` (5), `-phase` (12), `-qualification` (18), `-reference` (2), `science-fluid-solver` (4), `-transport` (2), `-topology` (1), `science-fluid` (4), `network` (4), `client-gui-common` (3), `client-gui-screens-inventory` (4), `fluid-benchmark` (3) |
| `movesize.pl`, `notime.pl` | Read a `compare-test-output.pl` output: the largest relative change per moved test, and which moved tests differ only in wall-clock fields (TIMING-ONLY against PHYSICS) |
| `drop-efficiency.pl` | S6: rewrites every `new FlowControl.Pump(a,b,c)` / `Compressor(a,b,c)` to `(a,b)` with matched parentheses; prints each dropped efficiency that is not 1 (the tests restated to the ideal work) |
| `s1-rule-off.patch` | `git diff -R 22b76697 eec02758 -- src/main`: S1's main changes reversed (the gate and the restated tests stay); applies on `eec02758` |
| `s2-edits/` | S2's edit scripts (perl, CRLF-aware, each dies on a missed site): `coord.pl` (IslandCoordinator), `codec.pl` (FluidCheckpointCodec), `tests.pl` (the mechanical test restatement: acceptance, allowance, anchor), `command-test.pl`, `codec-test.pl`, `reload-test.pl` (the restated tests), `errors.pl` (a javac log summarised per file or per line; compile with `-J-Duser.language=en -Xmaxerrs 3000`) |
| `probe/S3RowEProbe.java` | `science.fluid.network`: CrystalFlushBudgetTest's row E (CO2-I freezing beside liquid methane, the solid-event path), six 5 s intervals, every committed double as raw bits; identical before and after S3 |
| `probe/LegacyPinPointProbe.java` | `science.fluid.thermo`: LegacyNetworkPathPinTest's digest point by point, so two trees diff to the points that moved (S4: only cold enthalpies) |
| `logs/simp-logs.tar.gz` | `/tmp/simp-logs`: the group runs `<tree>-<group>.txt` (`base` = `2d2f5a2c`, then `s5`, `s3`, `s6`, `s4`, `s1`, `s2`, each item's tested tree), `<item>-runs.txt` (pass counts), `cmp-<item>-<group>.txt` (against the group's latest earlier run), `s1-phys.txt` (S1's physics-moved tests), `mg-s4.txt`/`mg-s1.txt` (the MIXED_GAS_TRANSIENT lines), `probe-rowe-s5done.txt`/`-s3.txt`, `pinprobe-s6.txt`/`-s4.txt` and `pp-*` (hash-only form), `head-s1-gate.txt` and `s1off-gate.txt` (the gate on HEAD and with the rule off), `head-edited-tests.txt` (the three test files edited after the S2 run, re-run at HEAD), `commits.txt` (the six commit messages), `s2-wrong-cwd/` (outputs of a first S2 run started from this folder instead of the worktree root; MaterialPresetsTest failed there for its relative `examples/` path and was re-run from the root) |

## How to run (Git Bash, from the worktree root; no Gradle)

Tests read `examples/` and write `build/reports/fluid/` and `logs/latest.log` relative to the current directory, so run from the worktree root, never from this folder.
```
D=tools/unified-thermo-port-m5/simplifications; L=$D/lane.sh
bash $L export head 65036f2b && bash $L compile head all && bash $D/run-touching.sh head science-fluid-network runtime-fluid
bash $L export s1 eec02758 && bash $L compile s1 all && bash $D/run-touching.sh s1 science-fluid-network runtime-fluid
perl D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl /tmp/simp-logs/head-runtime-fluid.txt /tmp/simp-logs/s1-runtime-fluid.txt > /tmp/simp-logs/cmp-head-runtime-fluid.txt
perl $D/notime.pl /tmp/simp-logs/cmp-head-runtime-fluid.txt; perl $D/movesize.pl /tmp/simp-logs/cmp-head-runtime-fluid.txt
bash $L export s1off eec02758 "$(cygpath -m "$PWD/$D/s1-rule-off.patch")" && bash $L compile s1off all
bash $L run s1off "com.wormzjl.createcheme.science.fluid.network.BackwardEulerStepTest#aPumpedTenMetreLiftOfWaterDeliversItsLiftWorkAsHeat"   # fails: 1.476 J of 99.257 J, +0.00000 K
```
`WT` (default: the `coolprop-multiphase-thermo-37f6b0` worktree) names the checkout whose `build/moddev` files give the classpath and whose history is exported. A probe is copied into the export's `src/test/java/com/wormzjl/createcheme/<package>/` (its package is in its first line) and compiled by name, e.g. `bash $L compile s3 science/fluid/network/S3RowEProbe.java`, then `bash $L run s3 com.wormzjl.createcheme.science.fluid.network.S3RowEProbe`. Extract the logs with `tar --force-local -xzf logs/simp-logs.tar.gz`.
