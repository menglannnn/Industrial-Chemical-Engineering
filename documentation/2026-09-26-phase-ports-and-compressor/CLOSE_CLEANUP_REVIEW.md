# Close C2: the batch's cleanup and detach (2026-10-14, thread t31)

Conclusion: nothing to detach. Every probe, rig, trace, scratch switch and analysis script of the batch already lives in `tools/phase-ports-probes/`. `src/` and `build.gradle` at `d15661b8` hold only product code, gate tests and switches of documented Gradle tasks. No tracked code changed, so no Gradle run was made. The tools documentation was brought up to the close (commit `26059fb5`). Rule: AGENTS.md "Test classes and tools after a batch". Branch `claude/close-cleanup` from `d15661b8`.

## Inventory

| Item | Where | Kept or detached | Why |
|---|---|---|---|
| Probes, init scripts, instrumentation and scratch patches, lane/replay/minimize scripts, `*.js` analysis, logs (23 subfolders) | `tools/phase-ports-probes/` | Kept there (already outside `src/`) | Detached material of the batch. Each subfolder now has a README section (added: `windows-lane/`, `in-game/`, `trace-flush/`, `hold-classes/`) |
| The close's material: logs `cl01`-`cl16`, `printed-maxrel.js`, `slice-pairs.js`, G1 widened to `runtime.Fluid*` (`cc7c9ae9`) | `tools/phase-ports-probes/hold-classes/` | Kept there | New last bullet of `hold-classes/README.md` |
| The 59 test classes and 2 helpers (`EventPathWorld`, `JunctionLedger`) the batch added | `src/test/.../runtime/fluid/`, `.../science/fluid/` | Kept | G1 runs them (`science.fluid.*`, `runtime.fluid.*`), including `FedTankOpenOutletsTest` and the `Trace*Test` classes |
| Restated tests outside those patterns (`FluidFallbackQualificationTest`, `FluidPumpedFillLineTest`, `FluidThermoDomainHoldTest`), V3 column re-tune, regression harness | `src/test/.../runtime/`, `.../column/v3/`, `.../fluid/benchmark/` | Kept | G1 (`runtime.Fluid*`), the full `test` task, G2 `fluidSolverRegression` |
| `FluidPhasePortGameTests`, `TankCharge` and their `PHASE_PORT_GAMETEST` prints | `src/fluidGameTest/` | Kept | G5 (`runFluidGameTestServer`); the prints are the gate's readout at the fixed tick (A141) |
| `FluidContentionProbe` | `src/fluidGameTest/` | Kept | Pre-batch (`a5dedf6f`, 2026-09-16); `FluidServerBenchmark` reads it (`fluidServerBenchmark` task) |
| System properties read in `src/main` | `FluidCheckpointCodec`, `IslandCoordinator` | Kept | Only `createcheme.fluid.scheduler.verify` (2026-09-23, pre-batch). `fluidScienceTest`/`fluidRuntimeTest` set it; `FluidCheckpointFormatTest` asserts it |
| Scratch switches `-Dt24*` to `-Dt29off`, `-Dinstr`, `-Dcreatecheme.junctionStart`, trace prints (`T13_FAIL`, `T29_*`) | none in `src/` | Already detached | Grep of `src/` (main, test, fluidGameTest, mcpCompat) for `System.getProperty`, `Boolean.getBoolean`, `Integer.getInteger`, `getenv`, `println`: no hit outside the rows above. The switches live only in the `hold-classes/src/*.patch` files |
| `-PmcpClientWidth`, `-PmcpClientHeight` (the batch's only `build.gradle` change, `8b43cfdd`) | `build.gradle` `runMcpClient` | Kept | GUI checks through the bridge (AGENTS.md GUI rule); the in-game check C3 uses `runMcpClient` |
| The other 25 Gradle properties | `build.gradle` | Kept | All older than the batch (base `ea16150b`). Each feeds a registered task: GameTest runs, `fluidServerBenchmark`, `fluidSolverRegression`, `fluidNetworkBenchmark`, `v3HollandBenchmark`, `testForks` |
| Run configurations | `.idea/`, `.vscode/`, `.claude/launch.json` | None present | Nothing to sort |
| `run/` (GameTest worlds `t26-cf31`, `t28-cc56`, `win-lane-1`; `mcp-client`; bridge jar in `run/mods`), `.mcp.json`, root `logs/` (client logs) | worktree root | Kept, untracked | Git-ignored local state; the jar and `.mcp.json` stay (GUI rule) |

## Removed from tracked paths

None. Earlier removals of this batch stay recorded where they were made: `wp6/removed/` (WP1 condensed-stream builders, `cb41860`) and `extreme/src/` (`ExtremeTopologyIslandTest`), each with a reattach patch.

## Left in place, with the reason

Nothing measurement-only was found in a tracked path, so nothing was left in place for that reason. The close's traces (`cl06`-`cl08`, `cl12`) used the batch's failing-rows trace (`T13_FAIL`, the `instr-t29-trace-on-cb26806e.patch` family). It was applied in the working tree and reverted. `src/` at `d15661b8` has no trace line, and no new patch was kept.

## Proposed CHANGELOG line (under the batch's entry, "Changed")

- Close C2 (`26059fb5`): the batch's measurement material checked against AGENTS.md "Test classes and tools after a batch"; nothing left in a tracked path to detach; `tools/phase-ports-probes/` README and `tools/INDEX.md` brought up to the close (`CLOSE_CLEANUP_REVIEW.md`).

## Gates to re-run

The tree under `src/` and `build.gradle` is byte-identical to `d15661b8`, so `cl14` (gates) and `cl15` (full suite, 1258/0) already describe it. AGENTS.md still asks for one gates run after a cleanup. Run `tools/phase-ports-probes/hold-classes/gates.sh logs/<tag> <gametest-run-id>` once (G1 fluid suites with `runtime.Fluid*`, the 33 junction lines and MIXED_GAS_COST, G2 chain-100 exact, G3 adjacent, G5 fluid GameTests on a fresh world), after C1's re-baseline. Record it here.
