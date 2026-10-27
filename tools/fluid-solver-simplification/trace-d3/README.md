# fluid-solver-simplification/trace-d3 (t42)

Purpose: thread t42's trace of form D3's failing gates under the gas choke law (owner request of 2026-10-19), batch
`documentation/2026-10-18-fluid-solver-simplification/`, report `FORM_D_GATES_TRACE.md`. Analysis only: nothing here is
tracked or committed; every patch and probe was applied to `git archive` lane exports of `6c073298`, never to the worktree.

| Path | What |
|---|---|
| `patches/form-d3c-throat-share.patch` | The one corrected form (D3c), against `6c073298` (it contains D3's `HydrocarbonModel.values` and gas choke): a stream's velocity limit is the throat share of its phases, `V / (V_gas/v_choke + V_condensed/v_max)`, bounded by the mixture's isothermal acoustic bound (`FluidThermodynamics.velocityLimit`, `throatShare`); a priority mixture's cap gives each pinned phase the area its capacity needs at its own limit and the marginal phase the rest (`PassiveStepSolver` `capLimit`, `drawn`). Deletes D3's branch on `liquidVolume+waterVolume>0` and the smallest-limit min over phases |
| `src/T42MixedGasProbeTest.java` | Compiled beside the tests on a lane export: MixedGasJunctionTransientTest's cases with every SolverDiagnostics counter per interval and the end states; `-Dt42.cases=0.02:4:true,...`, `-Dt42.interval=5` |
| `probe-vlim.sh <export>` | Adds each node's free-water volume and velocity limit to the pass probe's node lines (`../trace/patches/probe-pass-trace-on-573b050b.patch`) |
| `compact.pl` | One line per traced pass (`TRACE pass/converged/fail`): every node's P (kPa), every edge's q (`*` within 2 % of its cap); `T40STEP` lines pass through |
| `perseed.pl <base-report> <new-report> [exclude,...]` | Per-seed `COUNTERS_SEED` sums of two one-worker hold-fuzz reports and the largest movers |
| `lane-pair-minimize-t42.sh <seed> <class> <held> <clean> <label>` | t42b: t40's `../trace/lane-pair-minimize.sh` with `src/T42CandidatesTest` and `EXTRA_OPTS` (cold harness switches, `-Dt42.total`, `-Dm7.slices`) passed to both runs |
| `src/T42CandidatesTest.java` | t42b: M7CandidatesTest with a total-work class: a world's first hold class, else "W" when the run makes at least `-Dt42.total` cancellation checks, else "clean" (for a seed whose behaviour is a cost blow-up) |
| `patches/form-d3d-mixture-choke.patch`, `d3d-edit.pl` | t42b's one corrected form D3d (rejected; `FORM_D3C_COLD_SEEDS_TRACE.md`): D3c with the bulk limit `min(throat share, sqrt(gamma) K(gamma) acoustic)`; the perl script applies the same edit to a D3c export |
| `logs/t42b-logs.tar.gz` | `t42b-logs/`: replays `20-*`, layouts `21-*`, pair minimizations `pair-6`, `pair-15`, `pair-199` (rounds held/clean), seed 15/6/199 step and pass traces `22-*`, `23-*`, `25-*`, `26-*` (199 on D3c, baseline, D3), `27-*`, the D3d probes `28-*`, D3d quick check `30-d3dq-*`, seed 988 `31-*` (baseline, D3c, D3d) and `32-*` (the old law at 150 and 260 m/s), `33-*` cold seeds at 260 m/s, `34-*` seed 85 alone; `suites/` D3d's two suites with comparisons against `d38s` and D3c |
| `logs/t42-logs.tar.gz` | `t42-logs/`: `01-mg-*` mixed-gas counters (b = `6c073298`, d3 = D3), `02-*` its step trace, `03-*`/`04-*` bundled clean-seed counters (per seed; without 173), `05-*`/`06-*`/`07-*`/`08-*` world 1 runs and traces, `09-*` worlds under D2, `10-w3-trace-*` world 3 under D2/D3/baseline, `11-*` D3c worlds and mixed gas, `12-*` world 1 steps on D3c and baseline, `13-*` seed 173, `14-*` the 200-seed quick checks (bq baseline, d3cq D3c), `15-*`/`16-*` clean-in-both counters (16 per seed, cold), `17-*` seed 157 alone; `suites/` D3c's `science-fluid-network` and `runtime-fluid` with the comparisons against t38's `d38s` (`6c073298`) and `d3` logs |

Exports (Git Bash, worktree root; `D=tools/unified-thermo-port-m5/d40`, `P=tools/fluid-solver-simplification` as Windows paths):
```
bash $D/lane.sh export t42xc 6c073298 $P/trace-d3/patches/form-d3c-throat-share.patch && bash $D/lane.sh compile t42xc all
LOGS=<dir> bash $D/run-touching.sh t42xc science-fluid-network runtime-fluid
bash $D/lane.sh export t42xp 6c073298 $P/trace-d3/patches/form-d3c-throat-share.patch $P/trace/patches/probe-pass-trace-on-573b050b.patch \
  $P/patches/probe-newton-trace.patch $P/trace/patches/probe-interval-steps.patch && bash $P/trace-d3/probe-vlim.sh t42xp
JAVA_OPTS="-Dt40.steps=true -Dcreatecheme.fluid.trace=true" bash $D/lane.sh run t42xp \
  "com.wormzjl.createcheme.runtime.fluid.HoldClassWorldsTest#aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn"
OUT=<dir> THREADS=4 bash $P/quick-lane.sh t42xq d3cq 1-200     # t42xq: a second export of the same tree
```
Extract the logs with `tar --force-local -xzf logs/t42-logs.tar.gz`.
