# junction-seeding/trace-branch (t45)

Purpose: thread t45's trace of the branch selections behind the fallback's bundled Newton rise (owner request of 2026-10-19),
batch `documentation/2026-10-19-junction-seeding/`, report `BRANCH_SELECTION_TRACE.md`. Analysis only: nothing here is tracked
or committed; every probe was applied to `git archive` lane exports (`tools/unified-thermo-port-m5/d40/lane.sh`) of `33455d4c`
(the fallback, branch `claude/junction-seeding`) and `32bf3fa4` (the baseline), never to a worktree. No Gradle.

| Path | What |
|---|---|
| `src/T45BranchTest.java` | M7CandidatesTest with a branch class: a world's class at its fixed end tick (last placement + 100 x `m7.slices`, 40): `H<cls>` held, `U` an island not certified STEADY, `S` every island STEADY; the status carries each island's end status |
| `src/T45TraceTest.java` | Replays one KEEP list (`-Dreplay.seed`, `-Dreplay.keep`, `-Dreplay.slices` 40, `-Dt45.every` n) and prints per solve its detail, the end flows of its last accepted step and its junctions (P, T, density, liquid share, N2 / CH4 / hydrocarbon mass shares, buffer mass `mJ`), then the end statuses; needs the probe |
| `probe/T45Probe.java`, `probe/apply-probe.pl` | The probe class (network package, export only) and the script that inserts its calls; `-Dt45.trace=true` prints the balance's rounds (`T45BAL`: pressures, flows, donor densities and caps, the junction states in and out), the old mint's fixed point (`T45FP-*`, baseline) and the cold rate seed's root (`T45RATE`) |
| `patches/probe-on-33455d4c.patch`, `patches/probe-on-32bf3fa4.patch` | The same probe as patches (the script's result, CR stripped); `patches/rounds-switch-on-probe-33455d4c.patch`: `BALANCE_ROUNDS` read from `-Dt45.rounds` (2), on top of the probe; `patches/placeholder-switch-on-33455d4c.patch`: `PhysicalFluidTopology`'s junction placeholder from `-Dt45.placeholder` (`first`, today's; `last`; `ambient`, the compile's idle seed, `NetworkReduction.ambient`). Export `t45fbr` = probe + both switches |
| `lane-pair-minimize-t45.sh <seed> U <held> <clean> <label>` | t42b's pair minimizer with T45BranchTest: keeps a world U at `<held>` and `S` (`CLEAN_CLASS`) at `<clean>`; `EXTRA_OPTS` to both runs (`-Dm7.slices=120` for 123); logs in `$LOGS` |
| `signatures.sh <out> "<export[:opts]> ..." seed...` | M5SolveLogTest per seed and export (at most `PAR`=4 JVMs, budgets lifted, `SLICES` 120): the end statuses and the summed Newton counters |
| `logs/t45-logs.tar.gz` | `t45-logs/`: `01-replay4-*` seed 4 with island 33's networks; `02-pair4.txt`, `pair4/` the minimization of 4; `03-trace-*` the minimized world traced on both trees; `04-long-fb` 200 slices; `05-swap-*`, `07-swap-trace-fb` the generators placed the other way round; `06-rounds8-fb` BALANCE_ROUNDS 8; `08-counters-*` the minimized world's counters; `09-signatures.txt`, `sig/` seeds 4 123 200 78 on three trees; `10-end-*` the end states of 200 and 78; `11-pair123.txt`, `pair123/` the minimization of 123; `12-quick/` the counters-only quick checks (labels `base` = `32bf3fa4`, `fb` = `33455d4c`, `amb` ambient placeholder, `amb8` ambient + 8 rounds, `r8` 8 rounds; 4 threads); `13-trace123-*`, `14-net123-*` the minimized 123 traced; `15-ph-*` placeholder x rounds on both minimized worlds; `16-signatures-ambient.txt`; `17-net123full-*` the whole 123's island 60 |

Commands (worktree root, Git Bash; `D=tools/unified-thermo-port-m5/d40`, `T=tools/junction-seeding/trace-branch`, `H` = the bundled harness
`tools/unified-thermo-port-forms/hold-fuzz-bundled/src/HoldFuzzTest.java`, `S` = `tools/unified-thermo-port-m5/trace/src/*.java` and `$T/src/*.java`, as Windows paths):
```
bash $D/lane.sh export t45fb 33455d4c; bash $D/lane.sh compile t45fb $H $S        # t45b: 32bf3fa4
bash $D/lane.sh export t45fbp 33455d4c $T/patches/probe-on-33455d4c.patch; bash $D/lane.sh compile t45fbp $H $S   # t45bp: 32bf3fa4, probe-on-32bf3fa4.patch
bash $D/lane.sh export t45fbr 33455d4c $T/patches/probe-on-33455d4c.patch $T/patches/rounds-switch-on-probe-33455d4c.patch $T/patches/placeholder-switch-on-33455d4c.patch
#   (or the probe by script: cp $T/probe/T45Probe.java into the export's network package, then perl $T/probe/apply-probe.pl /tmp/d40-<name>)
THREADS=4 LOGS=<dir> bash $T/lane-pair-minimize-t45.sh 4 U t45fb t45b pair-4
JAVA_OPTS="-Dreplay.seed=4 -Dreplay.keep=1@179,2@184,4@227,5@234,6@281,10@415,11@605,14@1404,16@1498,18@1853 -Dt45.trace=true -Dt45.every=10" \
  bash $D/lane.sh run t45fbp com.wormzjl.createcheme.runtime.fluid.T45TraceTest#trace
bash $T/signatures.sh <out> "t45b t45fb t45fbr:-Dt45.rounds=8" 4 123 200 78
OUT=<dir> THREADS=4 EXTRA_OPTS="-Dt45.rounds=8" bash tools/fluid-solver-simplification/quick-lane.sh t45fbr r8q 1-200
```
Extract the logs with `tar --force-local -xzf logs/t45-logs.tar.gz`.
