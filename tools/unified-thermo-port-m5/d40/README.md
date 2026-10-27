# unified-thermo-port-m5/d40

Purpose: the D40 agent's material (owner decision D40, option A for H1 to H4, 2026-10-18). Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, three commits on `00ee8385`: `b81d3491` (H2: a pump's start mode reads its own lines only), `88817099` (H4: one step rule in the UV pressure search, wet or dry) and 3db5e5d3 (H1 and H3: a vessel's phase seed is its own state, and a failed vessel closure proposes nothing). Report: `documentation/2026-09-24-coolprop-low-temperature/D40_SEEDS_PUMP_START_UV_STEP.md`. The gates are tracked: `SlurryTankPumpTest#aSideOutletClosedByDepositionLeavesThePumpOnItsTarget` (H2), `WaterIntoCryogenicMethaneTest#theRefreshOfALiquidFullMethaneTankAnswersItsIceBesideMethanesSaturation` (H4) and `VesselPhaseSeedTest` (runtime.fluid; H1 `aVentedMethanePoolAtSaturationTakesItsCo2LineWithNoHold`, H3 `aLiquidFullSlushTankVentsTowardOneAtmosphereWhileItsVentPassesGas`). Nothing here was removed from or is attached to a tracked path.

## Contents

| Path | What |
|---|---|
| `lane.sh` | The Gradle-free javac lane of `../d39/lane.sh`, exporting under `/tmp/d40-<name>`: `export <name> <tree-ish> [patch...]` (absolute patch paths; a tree object works too), `compile <name> [test files relative to src/test/java, or all]`, `run <name> <selectors>` (M3Run, one JVM at -Xmx6g, `JAVA_OPTS` as system properties) |
| `tree.sh` | Prints a git tree object of the worktree's current `src/` through a temporary index (HEAD, the index and the stash untouched), for `lane.sh export` of uncommitted work |
| `run-touching.sh <name> [group...]` | Runs the groups `classes-<group>.txt` (17; the suites ran the 16 but `fluid-benchmark`), one JVM per group, with `-Dcreatecheme.fluid.scheduler.verify=true`. Logs: `$LOGS/<name>-<group>.txt` (default `/tmp/d40-logs`). `classes-runtime-fluid.txt` lists `VesselPhaseSeedTest` |
| `notime.pl`, `movesize.pl` | Read a `compare-test-output.pl` output: TIMING-ONLY against PHYSICS per moved test, and the largest relative change |
| `rule-h2-off.patch`, `rule-h4-off.patch`, `rule-h13-off.patch` | Each commit's `src/main` changes reversed (the gate stays); each applies on its commit |
| `fold-c-off.patch`, `fold-d39-off.patch`, `fold-d39b-off.patch` | Prepared, not run (the lead skipped the fold-redundancy experiment, 2026-10-18); scratch patches on an earlier H1/H3 tree: rule C's vanishing fold at failed iterates off (the step's `foldedIterate` and `InventoryEquilibrium.passes` fold by the trace rule only); D39 off (a failed iterate is phase-checked again, D28 A', as before `1482c277`); D39 B off (no refuted proposal) |
| `probe/`, `probe-export.sh` | `../d38`'s cold-seed world probe (unchanged; not used for D40) |
| `logs/d40-logs.tar.gz` | `/tmp/d40-logs` (below) |

Logs (`logs/d40-logs.tar.gz`, extracted as `d40-logs/`): `<commit>-<group>.txt` per suite group for `h2`, `h4` and `h13c` (`3db5e5d3`'s tree but a doc comment; `h13` is an earlier H1/H3 tree), `cmp-<commit>-<group>.txt` each against the commit before, `h13c-ruleoff-h1.txt` and `h13c-ruleoff-h3.txt` (the rule-off proofs, each run alone). The first `h13c` suite run overlapped a dev client and was discarded; the logs are the guarded re-run (14:39:41 to 14:40:50). The seed replays and rig rows were not run (lead's re-scope).

## How to run (Git Bash, from the worktree root; no Gradle but the bisect)

Tests read `examples/` and write `build/reports/fluid/` relative to the current directory, so run from the worktree root.
```
D=tools/unified-thermo-port-m5/d40; L=$D/lane.sh; C=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl
bash $L export h13 3db5e5d3 && bash $L compile h13 all && bash $D/run-touching.sh h13 science-fluid-network runtime-fluid
perl $C /tmp/d40-logs/h13-runtime-fluid.txt /tmp/d40-logs/h4-runtime-fluid.txt > /tmp/d40-logs/cmp.txt; perl $D/notime.pl /tmp/d40-logs/cmp.txt
G=com.wormzjl.createcheme.runtime.fluid.VesselPhaseSeedTest
bash $L export off 3db5e5d3 "$(cygpath -m "$PWD/$D/rule-h13-off.patch")" && bash $L compile off com/wormzjl/createcheme/runtime/fluid/VesselPhaseSeedTest.java
bash $L run off "$G#aVentedMethanePoolAtSaturationTakesItsCo2LineWithNoHold"   # fails: 6 passes against 1
bash $L run off "$G#aLiquidFullSlushTankVentsTowardOneAtmosphereWhileItsVentPassesGas"   # fails: held at slice 1
H=com.wormzjl.createcheme.runtime.fluid.HoldClassWorldsTest
bash $L export fc 3db5e5d3 "$(cygpath -m "$PWD/$D/fold-c-off.patch")" && bash $L compile fc com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java
bash $L run fc "$H#aJunctionsVapourCollapsingInASubcooledLiquidLeavesAtAFailedIterate"   # seed 30; seed 48: aBubbleTheNextPassDrivesToNothingIsRefused, seed 430: aFailedIterateRaisesNoPhaseHold
bash tools/unified-thermo-port-m5/scripts/bisect-seeds.sh HEAD <label> 430,48,30,19   # Gradle, clean tracked tree
```
The rig rows run from the main checkout's `tools/unified-thermo-port-test-world/` (`index-lane/compile.sh <label> <commit>`, then `index-lane/run.sh <label> com.wormzjl.createcheme.runtime.fluid.TestWorldRigTest "-DtestWorld.rows=<rows>;<rows-rig>" -DtestWorld.presets=<mixturepreset> -DtestWorld.only=R2-R7-R9,D23-2,R1,R10-fence`). Extract the logs with `tar --force-local -xzf logs/d40-logs.tar.gz`.

## Rename to IChemE (2026-10-21)

Batch `2026-10-21-rename-icheme`: the files this folder runs against the current tree use the package `com.wormzjl.icheme` and the namespace `icheme` (mod rename, `8033aed8`). Patches, diffs and probes tied to an older commit keep `createcheme`: they re-attach to that commit. The absolute folder path `D:/Minecraft/Modding/1.21/CreateChemE` is unchanged. A worktree copy of this folder (for example the lane worktree `coolprop-multiphase-thermo-37f6b0`) is refreshed from here before its next run.
