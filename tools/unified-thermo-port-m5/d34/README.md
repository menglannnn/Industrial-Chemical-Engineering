# unified-thermo-port-m5/d34

Purpose: owner decision D34 (2026-10-17). A phase port's static column is the connection's own volume, filled with the port's phases in priority order (`PassiveStepSolver.columnDensity`, `PhaseDraw.fill`). It is the fix for M5 seed 322, a trace of condensate at a tank's bottom port that held its island. Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, commit `4199dbe6` (on `4eaae450`). Report: `documentation/2026-09-24-coolprop-low-temperature/D34_STATIC_COLUMN.md`. The gate is tracked: `HoldClassWorldsTest#aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn`. Nothing here is attached to a tracked path.

## Contents

| Path | What |
|---|---|
| `lane.sh` | Gradle-free javac lane, adapted from `../../unified-thermo-port-forms/scripts/`. It has three commands. `export <name> <tree-ish> [patch...]` writes a git archive to `/tmp/d34-<name>` and applies the patches. `compile <name> [test files or all]` compiles main, then the tests through `-sourcepath`. `run <name> <selectors>` runs `../../unified-thermo-port-m3/scripts/M3Run.java` in one JVM at -Xmx6g, with `JAVA_OPTS` as system properties |
| `run-touching.sh <name>` | Runs every class of `science.fluid.network` (`classes-science.txt`, 37), then every class of `runtime.fluid` (`classes-runtime.txt`, 61) with `-Dcreatecheme.fluid.scheduler.verify=true`. Logs go to `/tmp/d34-logs/<name>-{science,runtime}.txt` |
| `rule-off-reverse.patch` | `git diff 4199dbe6 4199dbe6~1 -- src/main`: the rule reverted (the main sources only; the gate stays) |
| `probe/StaticColumnTraceTest.java`, `probe/StaticColumnTraceProbe.java` | The unit case (`science.fluid.network`, extends `PortFixture`): a methane tank with a 4.5e-5 mol crude_pc08 trace drains through 1 m of 0.05 m pipe into a gas tank 2 m below. It does not reproduce the hold: on `4eaae450` it drains in one slice and rests, with 20 refused substeps (replay start, 369 K). The probe varies the lower tank's T and the replay start |
| `probe/TraceColumnProbe.java` | The unit case's trace vessel: component indices, the saturation amount, liquid volume and densities |
| `probe/D34Seed322Probe.java` | Prints seed 322's KEEP blocks at full precision. It needs `../../unified-thermo-port-forms/hold-fuzz-bundled/src/HoldFuzzTest.java` in `runtime/fluid` of the export |
| `probe/D34WorldTrajectoryProbe.java` | Seed 322's 13-block world, one line per committed interval (`D34TRAJ`), plus the old and new column on that state (`D34COL`). `-Dd34.end=<tick>` sets the end |
| `probe/D34StackProbe.java` | `PhysicalFluidTopologyTest`'s stacked tanks, one line per slice: the moved STACK_PASS numbers |
| `logs/d34-logs.tar.gz` | The baseline (`base` = `4eaae450`) and new runs of `run-touching.sh`, `compare-*.txt` (from `compare-test-output.pl`), the rule-on and rule-off gate runs, the trajectories and the stack probes |

## How to run (Git Bash, from anywhere; no Gradle)

```
L=tools/unified-thermo-port-m5/d34/lane.sh
bash $L export on 4199dbe6; bash $L compile on com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java
bash $L run on "com.wormzjl.createcheme.runtime.fluid.HoldClassWorldsTest#aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn"
bash $L export off 4199dbe6 "$(cygpath -m "$PWD/tools/unified-thermo-port-m5/d34/rule-off-reverse.patch")"   # rule off
bash $L compile off com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java; bash $L run off "...same selector..."   # holds at 1097
bash $L export base 4eaae450; bash $L compile base all; bash tools/unified-thermo-port-m5/d34/run-touching.sh base
perl D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl /tmp/d34-logs/on-runtime.txt /tmp/d34-logs/base-runtime.txt
```
`WT` (default: the `coolprop-multiphase-thermo-37f6b0` worktree) names the checkout whose `build/moddev` files give the classpath and whose git history is exported. To use a probe, copy it into the export's `src/test/java/com/wormzjl/createcheme/<package>/` and compile it by name. The `science.fluid.network` probes go in that package; the others go in `runtime.fluid`.
