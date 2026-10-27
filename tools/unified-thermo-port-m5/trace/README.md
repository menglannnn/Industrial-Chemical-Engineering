# unified-thermo-port-m5/trace

Purpose: minimize and trace the two smallest new worse worlds of the M5 hold-fuzz campaign (bundled random, `7bd5fafc`
against main's `cl18`): seed 322 (class E6) and seed 47 (class F). Batch `2026-09-24-coolprop-low-temperature`, branch
`claude/unified-thermo-port`, 2026-10-17. Report: `documentation/2026-09-24-coolprop-low-temperature/M5_WORSE_WORLDS_TRACE.md`.
Diagnosis only: nothing here was committed, and no tracked path holds any of it.

## Contents

| Path | What |
|---|---|
| `m5-trace.init.gradle` | Attaches the bundled hold-fuzz harness (`../../unified-thermo-port-forms/hold-fuzz-bundled/src`, read in place) and `src/` to the test source set; `-Pm5.instr=true` adds `src-instr/`. Forwards every `-P` named `holdFuzz.*`, `minimize.*`, `replay.*`, `m5.*` as a system property |
| `src/M5TraceWorld.java` | HoldFuzzTest.World with one record per island solve. `checkpoints` = the solve's cancellation checks (each Newton iteration, line-search trial, Jacobian stage, pass, attempt and flash checks one): a deterministic work count, so a budget hold can be minimized on it |
| `src/M5MinimizerTest.java` | Main's `HoldMinimizerTest` (phase-ports batch) on the port's harness, classes as `hold-fuzz/classify.sh` names them; at most 4 threads. `-Dminimize.work=K`: the class is `W` when one solve makes K checks (use with the budgets off) |
| `src/M5SolveLogTest.java` | `replay`: one SOLVE line per island solve (checks, ms, counters, substeps, reasons), with `-Preplay.budgets=wall:soft,...`; `variants`: a summary per `label/keep/retemp` variant (a KEEP list, a generator restated at another temperature) |
| `src-instr/M5RowTrace.java`, `M5RowTraceTest.java` | The row trace (needs the patch below): passes, converged passes, active-set changes, cycles, and each failed Newton's largest rows by node/edge/meaning at the failed iterate and the pass start, node states, edges; the substep attempts, the network at the interval start, the TP answer at each vessel's own T, P and amounts; `-Preplay.history=true` prints each vessel's T, P, liquid volume per committed interval |
| `patches/instr-m5-trace-hooks-on-7bd5fafc.patch` | Temporary hooks in `PassiveStepSolver` (a static `M5_TRACE` consumer called at pass start, cycle, failed Newton, converged pass, active-set change). Applied for logs 11 and 13, reverted with `git checkout --` afterwards |
| `src-main/M5MainContrastTest.java`, `m5-main-contrast.init.gradle` | The contrast on main's code: run in a temporary detached worktree at `ffeafe8a` (removed afterwards) with the bundled harness (main's phase-ports harness no longer compiles there); prints each vessel's state per commit (`M5MAIN`), `-Dreplay.retemp`, a counter summary |

## How to run (worktree root, Git Bash, one Gradle invocation at a time)

```
G="bash tools/unified-thermo-port-m1/gradle-locked.sh"; I="--no-configuration-cache --init-script tools/unified-thermo-port-m5/trace/m5-trace.init.gradle"
$G build/m5-logs/09-minimize-322.gradle.txt $I test --tests '*M5MinimizerTest' -Pminimize.seed=322 -Pminimize.threads=4
$G build/m5-logs/12-minimize-47-work.gradle.txt $I test --tests '*M5MinimizerTest' -Pminimize.seed=47 -Pminimize.work=300000 -Pminimize.slices=10 -Pminimize.threads=4 -PholdFuzz.wallNanos=600000000000 -PholdFuzz.softNanos=590000000000
git apply tools/unified-thermo-port-m5/trace/patches/instr-m5-trace-hooks-on-7bd5fafc.patch   # row trace only; git checkout -- the file after
$G build/m5-logs/13-trace-322-min-tp.gradle.txt $I test --tests '*M5RowTraceTest' -Pm5.instr=true -Preplay.history=true "-Preplay.cases=322/<KEEP>"
```
A row-trace case is `seed[/keep][#island:start][!off]` (`!off`: budgets out of the way); cases are separated by `|`.
Minimized lists: 322 `KEEP 0@498,1@498,3@498,4@498,6@498,7@498,8@498,9@498,10@498,13@498,14@680,16@729,17@770` (13 blocks);
47 `KEEP 1@140,2@140,4@140,8@140,10@140,11@140,12@140,13@140,14@140,15@140,17@140,18@140,19@140,20@159,21@165,22@165,23@165,26@217` (18 blocks, work criterion).

## Logs (`build/m5-logs/` of the worktree)

| Label | What |
|---|---|
| `08-verbose` | Seeds 322 and 47 alone, verbose (`fuzz.sh`): both reproduce (E6; F at 29 s) |
| `09-minimize-322` | 322: 19 to 13 blocks in class E6, 17 s |
| `10-solvelog-47` | 47: every solve of island 42 at the default budgets and with the budgets off (first interval 334 k checks, 1.6 s) |
| `11-trace-322-47` | Row traces: 322 original and minimized, 47's interval [1726,1826] budgets off; 47 variants (full, without the methane generator, generator at 293.2 K) |
| `12-minimize-47-work` | 47: 27 to 18 blocks on the work criterion (K = 300 000 checks), 70 s |
| `13-trace-322-min-tp` | 322 minimized: vessel history per interval and the TP answers at the held interval's start |
| `14-main-contrast-322` | 322 minimized on main (`ffeafe8a`): no hold; T1's trace liquid comes and goes, dry from tick 1797 |
| `15-main-contrast-47-293K` | 47 with the generator at 293.2 K on main: 26.2 s, two range holds (methane at 293.1499999 K) |

## M7 cold traces (2026-10-17)

Purpose: minimize and trace the two new cold-fuzz holds: seed 30 (E5 from S1, `eec02758`; held at `65036f2b`, clean at
`22b76697`) and seed 19 (E7 from D36, `03bc70f6`; clean at `4199dbe6`). Report:
`documentation/2026-09-24-coolprop-low-temperature/M7_NEW_COLD_HOLDS_TRACE.md`. Diagnosis only: nothing committed; the
hooks were applied for each row trace and reverted with `git checkout --`.

| Path | What M7 added |
|---|---|
| `m5-trace.init.gradle` | The harness switch `-Pm5.harness=bundled\|cold\|cold-pre-s6`, read in place from `../../unified-thermo-port-forms/hold-fuzz-{bundled,cold,cold-pre-s6}/src`: `cold` for commits at or after `9445106a` (movers without efficiency), `cold-pre-s6` before it. A cold harness sets `holdFuzz.cold=true`. `m7.*` properties are forwarded too |
| `src/M5TraceWorld.java` | `within(...)`: runs a world inside the pilot catalog's `MaterialRuntime` context when the harness is cold (reflection on `HoldFuzzTest.COLD`/`PACKAGE`), as `HoldFuzzTest.runSeed` does; `M5MinimizerTest`, `M5SolveLogTest` and `M5RowTraceTest` wrap each world in it |
| `src/M7CandidatesTest.java` | The class of each world: `-Pm7.seed`, `-Pm7.lists="keep\|keep"`, or `-Pm7.base=<keep> -Pm7.drops=true` (the base and each single drop), `-Pm7.slices` (40), at most 4 threads. One `M7CAND seed= keep= class= tick= status=` line per world |
| `m7-pair-minimize.sh` | `m7-pair-minimize.sh <seed> <class> <held-sha> <clean-sha> <label> [base-keep]`: a greedy minimization that keeps the world in `<class>` at one commit and clean at the other (detached checkouts on a clean tracked tree, harness chosen per commit, back to `claude/unified-thermo-port` after each run); prints `PAIR KEEP` |
| `src-instr/M5RowTrace.java` | Per edge: dz, column end, column density and head (`-Pm5.columns=true` prints them at each pass start); per phase: density with PIP, Z and root count; z in the node states |
| `src-instr/M5RowTraceTest.java` | `-Preplay.historyAll=true`: every vessel and junction per interval (T to 1e-4 K, P to 0.01 Pa, mass, liquid volume, solids, phase bits) and the interval's flows; "no candidate" lines; `-Preplay.after=N` keeps ticking N ticks after the traced solve |
| `patches/instr-m5-trace-hooks-on-{65036f2b,22b76697,03bc70f6,4199dbe6}.patch` | The `7bd5fafc` hooks rebased onto each traced commit: `GIT_INDEX_FILE=<tmp> git read-tree <sha>`, `git apply --cached -3 <7bd5fafc patch>`, `git diff-index --cached -p <sha>`; the HEAD copy checked with `git apply --check` |

KEEP lists (`i@tick`, minimizer format):
- 30, pair-minimal, 11 blocks: `0@100,1@466,2@470,3@470,4@470,5@599,6@647,7@696,8@971,11@1283,12@1593` (pipe0.1@2,0,-1; tank@-2,0,0; gen[CO2gas 292K 2032kPa]@0,0,0; pipe0.05@0,0,-2; pipe0.01@-1,0,-1; pipe0.01@-2,0,-1; pipe0.1@1,0,-1; gen[LNG 170K 6000kPa]@2,1,-1; void@0,0,-3; pipe0.05@0,0,-1; pipe0.05@-1,0,0). Held island 18 [1593,1594].
- 19, pair-minimal, 14 blocks: `0@104,3@646,4@667,5@977,8@1050,9@1192,10@1192,13@1306,14@1445,16@1941,19@2252,24@2612,25@2612,26@2824` (pipe0.1@1,0,0; pipe0.05@0,-1,1; pipe0.1@1,-1,1; pipe0.1@1,-1,0; gen[LNG 127K 441kPa]@-2,0,2; pipe0.1@0,-1,0; pipe0.01@-1,0,2; pipe0.02@0,0,1; pipe0.05@1,-1,-1; pipe0.1@0,-1,-1; pipe0.02@0,0,2; gen[CO2gas 300K 5410kPa]@2,-1,-1; tank@0,-2,1; void@0,0,0). Held island 19 [2824,2825].
- Not discriminating (they hold at both commits): 30 plain-minimized `0@100,1@100,3@100,4@100,5@100,6@100,7@100,8@100,11@100,12@410`; 19 plain-minimized `0@104,3@104,4@104,5@104,9@104,14@104,16@104,24@104,25@104,26@316`.
- 19's pair start (the last of the minimizer's lists that is clean at D34, 21 blocks): `0@104,3@646,4@667,5@977,8@1050,9@1192,10@1192,13@1306,14@1445,15@1728,16@1941,17@1993,18@2029,19@2252,20@2307,21@2307,22@2501,23@2555,24@2612,25@2612,26@2824`.

Commands (worktree root, Git Bash, one Gradle call at a time; a parent commit by `git checkout -q --detach <sha>` on a clean tracked tree, then back with `git checkout -q claude/unified-thermo-port`):
```
G="bash tools/unified-thermo-port-m1/gradle-locked.sh"; I="--no-configuration-cache --init-script tools/unified-thermo-port-m5/trace/m5-trace.init.gradle"; L=build/m7-logs/trace
$G $L/01-minimize-30-head.gradle.txt $I test --tests '*M5MinimizerTest' -Pm5.harness=cold -Pminimize.seed=30 -Pminimize.threads=4
$G $L/09-check-19-min-d34.gradle.txt $I test --tests '*M7CandidatesTest' -Pm5.harness=cold-pre-s6 -Pm7.seed=19 "-Pm7.lists=<keep>|<keep>" -Pminimize.threads=4   # at 4199dbe6
bash tools/unified-thermo-port-m5/trace/m7-pair-minimize.sh 30 E5 65036f2b 22b76697 04-pair-30
bash tools/unified-thermo-port-m5/trace/m7-pair-minimize.sh 19 E7 03bc70f6 4199dbe6 11-pair-19 <19's pair start>
git apply tools/unified-thermo-port-m5/trace/patches/instr-m5-trace-hooks-on-65036f2b.patch
$G $L/05-trace-30-pair-head.gradle.txt $I test --tests '*M5RowTraceTest' -Pm5.harness=cold -Pm5.instr=true -Pm5.columns=true -Preplay.history=true -Preplay.historyAll=true "-Preplay.cases=30/<KEEP>"
git checkout -- src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java
```
At a parent the case names the island and its start (`30/<KEEP>#18:1593`, `19/<KEEP>#19:2824`); log 13 adds `-Preplay.after=2000`.

| Log (`build/m7-logs/trace/` of the worktree) | What |
|---|---|
| `01-minimize-30-head` | 30 at `65036f2b`, plain minimizer: 13 to 10 blocks, E5, 8 s |
| `02-trace-30-min-head`, `03-trace-30-min-s4` | That 10-block world traced at `65036f2b` and at `22b76697`: E5 at both ([1137,1237], stalls at 1.012e-9 and 1.128e-9), so it does not separate the commits |
| `04-pair-30-r1`..`r3-{held,clean}` | Pair minimization of 30 (E5 at `65036f2b`, clean at `22b76697`): 13 to 11 blocks |
| `05-trace-30-pair-head` | The 11-block world at `65036f2b`: island 18 [1593,1594] held E5 (68 failed Newtons, 405 k checks), with every node's history |
| `06-trace-30-pair-s4` | The same at `22b76697`: FULL, one step, 22 382 checks |
| `08-minimize-19-d36` | 19 at `03bc70f6`, plain minimizer: 27 to 10 blocks, E7, 29 s |
| `09-check-19-min-d34` | That 10-block world at `4199dbe6`: E7 too |
| `10-sequence-19-d34` | The minimizer's accepted lists at `4199dbe6`: the last clean one (21 blocks) starts the pair minimization |
| `11-pair-19-r1`..`r8-{held,clean}` | Pair minimization of 19 (E7 at `03bc70f6`, clean at `4199dbe6`): 21 to 14 blocks |
| `12-trace-19-pair-d36` | The 14-block world at `03bc70f6`: island 19 [2824,2825] held E7 (215 passes, 202 failed Newtons, 355 k checks) |
| `13-trace-19-pair-d34` | The same at `4199dbe6`: converges on the 11th attempt (147 passes, 129 failed Newtons), then runs clean to tick 4751 |

There is no log 07 (an optional S4 contrast that was not needed).

## M8 campaign traces (2026-10-18)

Purpose: minimize and trace the two smallest new worse worlds of the final cold campaign at `482264fc`
(`build/m8-logs/04-campaign-cold-random-report.txt`): seed 430 (class C, clean at M5 `7bd5fafc`) and seed 48 (E5, newly
held at S1 `eec02758`). Report: `documentation/2026-09-24-coolprop-low-temperature/M8_CAMPAIGN_WORSE_WORLDS_TRACE.md`.
Diagnosis only: nothing committed; the hooks were applied for each row trace and reverted with `git checkout --`.

| Path | What M8 added |
|---|---|
| `patches/instr-m5-trace-hooks-on-482264fc.patch` | The `65036f2b` hooks rebased onto `482264fc` (temporary index: `GIT_INDEX_FILE=<tmp> git read-tree <sha>`, `git apply --cached -3 <patch>`, `git diff-index --cached -p <sha>`); `git apply --check` passes at HEAD |
| `patches/instr-m8-phase-hold-hook-on-{482264fc,22b76697,4199dbe6}.patch` | A static `FluidThermodynamics.M8_HOLD` consumer, told (t, p, amounts, detail) of each THREE_PHASES hold `adopt` raises, just before the throw; rebased the same way, each checked against its commit |
| `src-instr/M5RowTrace.java` | `install()` sets `M8_HOLD` by reflection where the field exists; `M8HOLD #n` lines with the CreateChemE stack that asked for the flash (`-Dm8.holdLines`, 40); `M5FAIL` names the Newton's remembered phase hold (`[phase hold: ...]`) |
| `m7-pair-minimize.sh` | `LOGS=<dir>` picks the log folder (default `build/m7-logs/trace`) |

KEEP lists (`i@tick`):
- 430, pair-minimal, 8 blocks: `0@100,1@100,2@122,3@277,4@314,6@364,9@404,10@404` (pipe0.05@-3,0,0; pipe0.02@-3,-1,0; tank@-5,-1,0; pipe0.05@-5,0,0; gen[CO2gas 293K 4890kPa]@-3,-2,0; gen[LNG 128K 884kPa]@-2,-1,0; pipe0.02@-4,-1,0; pipe0.05@-4,0,0). Held island 13 [404,405]; the tank is node index 1 (id 3), n5 the upper 0.05 m run.
- 48, pair-minimal, 6 blocks: `1@166,5@323,10@598,12@1185,13@1207,14@1254` (gen[LNG 149K 1954kPa]@0,0,0; pipe0.1@0,-1,-3; tank@0,0,-3; pipe0.1@0,0,-1; tank@0,-2,-3; pipe0.1@0,0,-2). Held island 11 [1254,1255]; n1 the upper tank, n2 the lower.

Commands (worktree root, Git Bash, one Gradle call at a time; a parent by `git checkout -q --detach <sha>` on a clean tracked tree, then back with `git checkout -q claude/unified-thermo-port`):
```
G="bash tools/unified-thermo-port-m1/gradle-locked.sh"; I="--no-configuration-cache --init-script tools/unified-thermo-port-m5/trace/m5-trace.init.gradle"; L=build/m8-logs/trace; P=tools/unified-thermo-port-m5/trace/patches
bash tools/unified-thermo-port-m5/scripts/bisect-seeds.sh 26090bf0 m8-430-d38a 430          # logs in build/m7-logs/bisect/
LOGS=$L bash tools/unified-thermo-port-m5/trace/m7-pair-minimize.sh 430 C 482264fc 4199dbe6 01-pair-430
LOGS=$L bash tools/unified-thermo-port-m5/trace/m7-pair-minimize.sh 48 E5 482264fc 22b76697 03-pair-48
git apply $P/instr-m5-trace-hooks-on-482264fc.patch; git apply $P/instr-m8-phase-hold-hook-on-482264fc.patch
$G $L/02-trace-430-head.gradle.txt $I test --tests '*M5RowTraceTest' -Pm5.harness=cold -Pm5.instr=true -Pm5.columns=true -Preplay.history=true -Preplay.historyAll=true "-Preplay.cases=430/<KEEP>"
$G $L/04-trace-48-head.gradle.txt $I test --tests '*M5RowTraceTest' -Pm5.harness=cold -Pm5.instr=true -Pm5.columns=true -Pm5.fullFails=40 -Preplay.history=true -Preplay.historyAll=true "-Preplay.cases=48/<KEEP>"
git checkout -- src/main/java/com/wormzjl/createcheme/science/fluid/network/PassiveStepSolver.java src/main/java/com/wormzjl/createcheme/science/fluid/thermo/FluidThermodynamics.java
```
At a parent the two patches are that commit's and the case names the island: log 05 `48/<KEEP>#11:1254` at `22b76697` (`-Pm5.harness=cold`), log 06 `430/<KEEP>#13:404` at `4199dbe6` (`-Pm5.harness=cold-pre-s6`).

| Log (`build/m8-logs/trace/` of the worktree) | What |
|---|---|
| `01-pair-430-r1`..`r5-{held,clean}`, `01-pair-430.console` | Pair minimization of 430 (C at `482264fc`, clean at `4199dbe6`): 12 to 8 blocks (r5 has no clean run) |
| `02-trace-430-head` | The 8-block world at `482264fc`: island 13 [404,405] held C; 20 attempts to 9.5e-8 s, 184 passes, 153 failed Newtons, 1.03 M checks; six `M8HOLD` lines (17.07-18.30 K), all from the failed-iterate phase correction |
| `03-pair-48-r1`..`r10-{held,clean}`, `03-pair-48.console` | Pair minimization of 48 (E5 at `482264fc`, clean at `22b76697`): 15 to 6 blocks (r10 has no clean run) |
| `04-trace-48-head` | The 6-block world at `482264fc`: island 11 [1254,1255] held E5; 29 attempts to 3.0e-9 s, 24 failed Newtons, 19 of them stalls at 3.96-4.04e-8 (one at 2.06e-8) on the lower tank's equilibrium rows |
| `05-trace-48-s4` | The same at `22b76697`: FULL (23 steps accepted, 9 rejected), the lower tank two-phase with 4e-5 of its moles as vapour |
| `06-trace-430-d34` | The 8-block world at `4199dbe6`: FULL on its first attempt (pass 8 of the 0.05 s step), the tank committed liquid at 135.46 K and 1 atm |
| `build/m7-logs/bisect/m8-430-{d34,d36,bff2,s6,s4,s1,fb,d38a}*` | Seed 430 (12 blocks) alone at `4199dbe6`, `03bc70f6`, `bff22e37`, `9445106a`, `22b76697`, `eec02758`, `65036f2b`, `26090bf0` (`bisect-seeds.sh` writes there) |

## Test world rig traces (2026-10-18)

The H1-H4 diagnosis of the test world rig (batch `TEST_WORLD_RIG_HOLDS_TRACE.md`) used these hooks on `git archive`
exports only, through `tools/unified-thermo-port-test-world/index-lane/compile.sh` (`PATCH=`, `EXTRA_PROBES=`); the
worktree was never patched, so nothing needs `git checkout --`.

| Patch | What |
|---|---|
| `patches/instr-m5-trace-hooks-on-{00ee8385,26090bf0}.patch` | The M5 hooks plus M8's `M8_HOLD`, in one patch per commit (the `482264fc` pair rebased onto each commit) |
| `patches/instr-m9-phase-check-hook-on-{00ee8385,26090bf0}-after-m5.patch` | A static `FluidThermodynamics.M9_CHECK` consumer told of every phase check that returns (state, `from`, kept, the TP answer, the TP seed, the own split, the seed, the proposed, refused and excluded bits, the checked state, junction, shrinking). Apply after the M5 patch; the `00ee8385` one also applies at `482264fc`. At `26090bf0` `from` and shrinking are null and 0. Consumer: `tools/unified-thermo-port-test-world/index-lane/probes-trace/M9CheckTrace.java` |
| `patches/instr-m10-uv-search-hook-on-00ee8385.patch` | A static `FluidTpEquilibrium.M10_UV` consumer told of the UV search: the warm start's verdict, the cold start, the Newton, each temperature's inner outcome (with the stored secant `innerSlope`) and each TP equilibrium of the pressure search. Consumer: the `h4` trace in `TestWorldTrace.java` |

Example (from `tools/unified-thermo-port-test-world`; `T` is the worktree's `tools` folder as a Windows path):
```
P=$T/unified-thermo-port-m5/trace/patches
PATCH="$P/instr-m5-trace-hooks-on-482264fc.patch $P/instr-m8-phase-hold-hook-on-482264fc.patch $P/instr-m9-phase-check-hook-on-00ee8385-after-m5.patch" \
EXTRA_PROBES="$T/unified-thermo-port-m5/trace/src-instr/M5RowTrace.java $T/unified-thermo-port-test-world/index-lane/probes-trace/M9CheckTrace.java" \
  bash index-lane/compile.sh trace9-pre 482264fc
bash index-lane/run.sh trace9-pre com.wormzjl.createcheme.runtime.fluid.TestWorldRigTest "-DtestWorld.rows=$(cygpath -m rows)" \
  -DtestWorld.presets=$(cygpath -m mixturepreset) -DtestWorld.only=D23-2 -DtestWorld.m5=D23-2@185.65-185.65 -DtestWorld.m5Max=2 -Dm9.lines=100000 -Dm5.passLines=100000
PATCH="$P/instr-m10-uv-search-hook-on-00ee8385.patch" bash index-lane/compile.sh h4-head 00ee8385
bash index-lane/run.sh h4-head com.wormzjl.createcheme.runtime.fluid.TestWorldRigTest "-DtestWorld.rows=$(cygpath -m rows-rig)" \
  -DtestWorld.presets=$(cygpath -m mixturepreset) -DtestWorld.only=R1-h4 -Dm10.lines=2000
```
Logs: `tools/unified-thermo-port-test-world/logs/2026-10-18-session/` (`h1-*`, `h2-*`, `h3-*`, `h4-*`, `h12-*`).
