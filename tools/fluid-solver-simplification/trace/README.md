# fluid-solver-simplification/trace (t40)

Purpose: pair-minimize, bisect and trace the two smallest new bundled holds of the fluid solver simplification (bundled
random seeds 407 and 26, E8, held at `573b050b`, clean at m11 `688f4cd7`). Batch `2026-10-18-fluid-solver-simplification`,
report `documentation/2026-10-18-fluid-solver-simplification/BUNDLED_HOLDS_TRACE.md`. Diagnosis only: nothing here is
tracked or committed; every probe was applied to a `git archive` lane export, never to the worktree.

| Path | What |
|---|---|
| `lane-pair-minimize.sh` | `m7-pair-minimize.sh` on the Gradle-free javac lane: `lane-pair-minimize.sh <seed> <class> <held-export> <clean-export> <label> [base-keep]` (exports of `tools/unified-thermo-port-m5/d40/lane.sh` compiled with the bundled harness and `../../unified-thermo-port-m5/trace/src`); budgets out of the way (class deterministic); `THREADS` (4); logs in `$LOGS` (`build/t40-logs`) |
| `src/T40VariantsTest.java` | One KEEP list with one generator restated at other pressures: `-Dt40.seed -Dt40.keep -Dt40.index -Dt40.pressures=p1,p2,...`; one `T40VAR p= class=` line each |
| `patches/probe-interval-steps.patch` | `PassiveIntervalSolver` (equal at every commit of the batch): `-Dt40.steps=true` prints each attempt's step, its dominant vessel (P, m, T, liquid and vapour volume before and after) and the edge flows, and each rejection |
| `patches/probe-pass-trace-on-573b050b.patch` | t38's pass probe (`../patches/probe-pass-trace-on-form-a.patch`) with the Newton call's context moved to `SparseNewton.ITERATIONS`; use with `../patches/probe-newton-trace.patch` and `-Dcreatecheme.fluid.trace=true` |
| `fix-crlf-literals.pl` | t38's `../patches/probe-pass-trace-on-688f4cd7.patch` splits two literals over a line; after exporting with it run `perl fix-crlf-literals.pl < PassiveStepSolver.java > tmp` and copy back (the export is CRLF) |
| `patches/proposition-vanishing-both-shares.patch` | The proposition as a probe on `573b050b`: `FluidThermodynamics.vanishing` folds a shrinking phase only below `VANISHING_SHARE` both by moles and by volume (export `t40-both`; quick check `bothq`, counters `bothc`, the nine seeds `bothn`, suites `suites2/`) |
| `patches/proposition-proposal-at-resolution.patch` | Round 2, 678: a proposed phase its own split holds below `VANISHING_SHARE` both by moles and by volume is refused, whatever its direction (on `6c073298`) |
| `patches/rejected-vanishing-volume-only.patch` | The rejected variant, the share by volume alone (export `t40-vol`; `volq`, `volc`, `voln`, suites `suites/`: 4 tests fail) |
| `logs/t40-logs.tar.gz` | `build/t40-logs/`: base classes (`00`, `01`), pair minimizations (`pair-*`), bisect (`02`), step traces (`03`, `08`), pass traces (`04`), tick, temperature and pressure variants (`05`-`07`), the proposition probe on the worlds and seeds (`09`), quick checks on the same lane (`headq` HEAD, `volq`, `bothq`), clean-in-both counters (`headc`, `volc`, `bothc`), the nine new seeds (`headn`, `voln`, `bothn`) and the suites (`suites/`, `suites2/`) |

Minimized worlds (pair-minimal; held at `6f1a524e` and every later commit, clean at `688f4cd7`):
- 407, 3 blocks: `1@291,2@291,16@1208` (pipe0.05@1,0,0; tank@0,0,0; gen[crude 311K 316kPa]@2,0,0). Held [1435,1535].
- 26, 10 blocks: `4@660,7@680,8@680,9@697,10@750,11@757,14@864,15@914,17@981,19@1034` (pipe0.05@0,0,-1; tank@2,-2,0;
  gen[crude 294K 311kPa]@0,0,0; pipe0.05@1,-2,0; pipe0.05@0,-1,0; pipe0.05@0,-1,-1; pipe0.05@0,-2,0; pipe0.02@1,-2,1;
  tank@0,-2,-1; pump[0.01,500kPa]@1,-1,1>UP). Held [1361,1461].

Commands (worktree root, Git Bash; `D=tools/unified-thermo-port-m5/d40`, `H` = the bundled harness, the four
`unified-thermo-port-m5/trace/src` sources and `src/T40VariantsTest.java` as Windows paths):
```
bash $D/lane.sh export t40-held 573b050b; bash $D/lane.sh compile t40-held $H     # t40-clean 688f4cd7; t40-A/C/B/L the batch commits
THREADS=2 bash tools/fluid-solver-simplification/trace/lane-pair-minimize.sh 407 E8 t40-held t40-clean pair-407
bash $D/lane.sh export t40-held-p 573b050b <pass-trace> ../patches/probe-newton-trace.patch patches/probe-interval-steps.patch
JAVA_OPTS="-Dreplay.seed=407 -Dreplay.keep=1@291,2@291,16@1208 -Dreplay.slices=4 -Dreplay.budgets=600000000000:590000000000 -Dt40.steps=true -Dcreatecheme.fluid.trace=true" \
  bash $D/lane.sh run t40-held-p com.wormzjl.createcheme.runtime.fluid.M5SolveLogTest#replay
bash $D/lane.sh export t40-both 573b050b patches/proposition-vanishing-both-shares.patch patches/probe-interval-steps.patch
OUT=build/t40-logs bash tools/fluid-solver-simplification/quick-lane.sh t40-both bothq 1-200 "bundled-random cold-random"
```

## Round 2 (2026-10-19): seeds 678 (E5) and 988 (E6)

Held tree: `573b050b` with `patches/proposition-vanishing-both-shares.patch` (export `t40-held2`), clean `688f4cd7`; bisect
exports `t40-{A2,C2,B2,L2,K2}` (each commit plus the amendment; before form C the patch context differs, so
`apply-both-shares-pre-form-c.pl` applies the same edit by script: `perl apply-both-shares-pre-form-c.pl < FluidThermodynamics.java > tmp`).
Probes on the branch HEAD `6c073298` (the amendment committed): `t40-h6` plain, `t40-prop` with
`patches/proposition-proposal-at-resolution.patch` (678's proposition), `t40-it40` with `SparseNewton.ITERATIONS` set to 40 by
sed (988's diagnostic). Logs in `logs/t40-logs.tar.gz` under `t40-logs/r2/` (`pair-*`, `02-bisect-*`, `03-steps-*`, `04-trace-*`,
`05-net-988*`, `06-*`, quick checks `h6q`/`propq`, new holds `h6qn`/`propqn`/`it40n`, suites `suites/`).
`DETAIL-407-26.md` is the full first version of the report (seeds 407 and 26).

Pair-minimal KEEP lists:
- 678, 9 blocks: `0@335,1@336,2@386,3@605,4@620,8@1218,16@1683,17@1696,18@1700` (pipe0.05@0,-4,0; pipe0.05@0,-3,0;
  pipe0.1@0,-1,-1; tank@1,-5,-1; pipe0.05@0,-5,0; pipe0.05@1,-5,0; tank@0,-2,0; gen[crude 310K 332kPa]@0,-1,-2;
  pipe0.01@0,-1,0). Held [1701,1704].
- 988, 11 blocks: `0@452,2@716,3@969,4@969,9@1118,11@1163,14@1576,15@1576,16@1576,18@1677,20@1944` (pipe0.05@0,3,0;
  pipe0.05@0,0,0; pipe0.05@3,0,-1; pipe0.05@0,2,0; pipe0.02@0,0,-1; pipe0.1@3,-1,-1; filter@0,1,0>DOWN; tank@0,4,0;
  gen[CH4 261K 148kPa]@3,-2,-1; pipe0.05@1,0,-1; pipe0.05@2,0,-1). Held [1944,1945].

Gate gotcha: a wait on another thread's progress file must match every closing phrase it writes (t41 closed with
"lanes finished", not "lane done"), or bound the wait.
