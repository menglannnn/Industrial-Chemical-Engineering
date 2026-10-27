# unified-thermo-port-m5/d36

Purpose: owner decision D36 (2026-10-17), "a gas column does not exist". In every static column the vapour weighs nothing; condensed phases keep their weight, filled by volume over the line as D34 states (`PassiveStepSolver.columnDensity`, `endColumn`, `condensedOverBulk`; a mover's column is `Equations.Transport.column`). Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, commit `03bc70f6` (on D34's `4199dbe6`). Report: `documentation/2026-09-24-coolprop-low-temperature/D36_NO_GAS_COLUMN.md`. The gates are tracked: `RestResolutionTest#twoGasTanksJoinedByARiserRestAtOnePressure`, `RestResolutionTest#aLoopOfGasRisersCarriesNoCirculation`, and the assertion added to `HoldClassWorldsTest#aGeneratorFillingADeadEndLadderOfGasLinesSettlesWithinTheNewton`. Nothing here is attached to a tracked path.

## Contents

| Path | What |
|---|---|
| `lane.sh` | The Gradle-free javac lane of `../d34/lane.sh`, exporting under `/tmp/d36-<name>`. `export <name> <tree-ish> [patch...]`, `compile <name> [test files or all]`, `run <name> <selectors>` (M3Run, one JVM at -Xmx6g, `JAVA_OPTS` as system properties) |
| `tree.sh` | Prints a git tree object of the worktree's current `src/` through a temporary index (HEAD, the index and the stash untouched), for `lane.sh export <name> "$(bash tree.sh)"` |
| `run-touching.sh <name>` | The 37 `science.fluid.network` classes (`classes-science.txt`), then the 61 `runtime.fluid` classes (`classes-runtime.txt`) with `-Dcreatecheme.fluid.scheduler.verify=true`. Logs: `/tmp/d36-logs/<name>-{science,runtime}.txt` |
| `rule-off-reverse.patch` | `git diff 03bc70f6 4199dbe6 -- src/main`: D36 reverted in the main sources only (the gates and restated tests stay) |
| `movesize.pl`, `notime.pl` | Read a `compare-test-output.pl` output: the largest relative change per moved test, and which moved tests differ only in wall-clock fields |
| `probe/D36GateProbe.java` | `science.fluid.network`: the two gate graphs without assertions (drives, the gauge, three 5 s steps); with the rule off, the riser drains 1.03e-4 kg/s down and the loop circulates 0.016 to 0.023 kg/s |
| `probe/D36LadderProbe.java` | `runtime.fluid`: random 498's ladder (the TurbulentWalk world) every 1000 ticks and in full at the end; `-Dd36.end=<tick>` |
| `probe/D36TankOutletsProbe.java` | `runtime.fluid`: `TankOutletsTest`'s tank, one 5 s interval and twenty 0.25 s intervals |
| `probe/D36DrainSideProbe.java` | `runtime.fluid`: the drain-side gas column world (seed 992) at its end |
| `logs/d36-logs.tar.gz` | `base` (`4199dbe6`), `on` (the tested tree) and `final` (`03bc70f6`) runs of `run-touching.sh`; `compare-*.txt` (`final` and `on` against `base`, `final` against `on`), `moved-classes.txt`; the rule-off gate runs (`off-*.txt`); the probe outputs; `mg-*.txt` (the mixed-gas 5 s cases); the commit message |

## How to run (Git Bash, from the worktree; no Gradle)

```
D=tools/unified-thermo-port-m5/d36; L=$D/lane.sh
bash $L export final 03bc70f6 && bash $L compile final all && bash $D/run-touching.sh final
bash $L export base 4199dbe6 && bash $L compile base all && bash $D/run-touching.sh base
perl D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl /tmp/d36-logs/final-runtime.txt /tmp/d36-logs/base-runtime.txt > /tmp/d36-logs/compare-runtime.txt
perl $D/notime.pl /tmp/d36-logs/compare-runtime.txt
bash $L export off 03bc70f6 "$(cygpath -m "$PWD/$D/rule-off-reverse.patch")" && bash $L compile off all
bash $L run off "com.wormzjl.createcheme.science.fluid.network.RestResolutionTest#twoGasTanksJoinedByARiserRestAtOnePressure"   # fails: -116.717 Pa
```
`WT` (default: the `coolprop-multiphase-thermo-37f6b0` worktree) names the checkout whose `build/moddev` files give the classpath and whose history is exported. A probe is copied into the export's `src/test/java/com/wormzjl/createcheme/<package>/` (its package is in its first line) and compiled by name.
