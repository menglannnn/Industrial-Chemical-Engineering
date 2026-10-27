# 2026-09-23-fluid-scheduling-rest

Purpose: Fluid island scheduling, rest and steady-flow certificates; engine-owned presentation

Status: Implemented 2026-09-23 (0.3.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [FLUID_ISLAND_REST_PLAN.md](FLUID_ISLAND_REST_PLAN.md)
- [FLUID_ISLAND_REST_PLAN_REVIEW.md](FLUID_ISLAND_REST_PLAN_REVIEW.md)
- [wp0-wp1/FLUID_SCHEDULER_WP0_BASELINE.md](wp0-wp1/FLUID_SCHEDULER_WP0_BASELINE.md)
- [wp0-wp1/FLUID_SCHEDULER_WP1_REVIEW.md](wp0-wp1/FLUID_SCHEDULER_WP1_REVIEW.md)
- [wp2/FLUID_SCHEDULER_WP2_REVIEW.md](wp2/FLUID_SCHEDULER_WP2_REVIEW.md)
- [wp2/wp2-tables.md](wp2/wp2-tables.md)
- [wp2/BENCHMARK_WINDOW_LENGTH_ANALYSIS.md](wp2/BENCHMARK_WINDOW_LENGTH_ANALYSIS.md)
- [wp3/FLUID_SCHEDULER_WP3_REVIEW.md](wp3/FLUID_SCHEDULER_WP3_REVIEW.md)
- [wp3/wp3-tables.md](wp3/wp3-tables.md)
- [wp4/FLUID_SCHEDULER_WP4_REVIEW.md](wp4/FLUID_SCHEDULER_WP4_REVIEW.md)
- [wp4/wp4-tables.md](wp4/wp4-tables.md)
- [wp5/FLUID_SCHEDULER_WP5_REVIEW.md](wp5/FLUID_SCHEDULER_WP5_REVIEW.md)
- [wp5/wp5-tables.md](wp5/wp5-tables.md)
- [FLUID_ISLAND_REST_REVIEW.md](FLUID_ISLAND_REST_REVIEW.md)
- [FLUID_NETWORK_STRESS_TEST_WP5.md](FLUID_NETWORK_STRESS_TEST_WP5.md)
- [FLUID_NETWORK_ACCEPTANCE_WP5.md](FLUID_NETWORK_ACCEPTANCE_WP5.md)

## Code areas

`runtime/fluid/IslandCoordinator`, `IslandClock`, `FairIslandQueue`, `world/inventory/FluidDeviceMenu`

## History

- Implemented 2026-09-23, MERGED TO MAIN 2026-09-23 (fast-forward 42fdf41 to 23beadd, 31 commits, version 0.3.0; not pushed).
- WP0 baselines and WP1 deadline scheduler done and verified 2026-09-23 on `claude/fluid-scheduler` @f3ccbae (161/148 tests, 21 GameTests, exact regression zero, P12/P31 bitwise, idle-tick island visits 700 to 0), not merged
- WP2 REST/STEADY certificates done and verified 2026-09-23 on the same branch @9b954a4 (161/177 tests, 22 GameTests, exact regression zero, P12/P31 byte-identical; rest100 full solves 2400 to 18 and after-GC heap 1000 to 331 MiB, mixed100 2400 to 1807; closed islands certify STEADY not REST, through-flow ladders never certify at eps 1e-9 because they drift for hours), not merged
- WP3 engine-owned presentation (fluid-4 protocol, 100-tick island buckets, queued inputs, viewers profile, dev-client check through the MCP bridge) done and verified 2026-09-23 @94077cd (161/189 tests, 26 GameTests, exact regression zero, P12/P31 byte-identical; physics bitwise unchanged with 16 menus and 10,803 loaded devices, one live payload per 100 ticks per menu, all 80 edits answered on the following bucket), not merged
- WP4 checkpoint format 3 (NBT, cached island payloads, persisted certificates with validity signatures, no upgrade from format 2, energy-datum migration removed) done and verified 2026-09-23 @3b6f206 (161/193 tests, 28 GameTests, exact regression zero, P12/P31 byte-identical; 1,000 certified islands load with zero solves until their horizon; warm save 4 to 8 ms against 250 to 340 ms cold; dev-client save and reload keeps RESTING since-times), not merged
- WP5 in-game CPU/RAM measurement (dedicated server and integrated client, before eb28fc5 vs after, 60 s windows, soaks, paced transient100 x3 within noise, module profile) done and verified 2026-09-23 @1401cab: 1,000 resting networks cost 0.12 cores against 0.56 (empty world 0.10), engine 1.52 to 0.011 ms per tick, whole tick p50 1.61 to 0.17 ms, no GC
- pre-existing findings: pumped fills held at about 12 cores on both builds, quadratic placement cost
- five owner decisions open in `FLUID_ISLAND_REST_REVIEW.md`

## Notes

- plan revision 3; all six revision-2 decisions answered on 2026-09-23 (requalify after hold, shared clock, configurable tolerances, engine-owned presentation, checkpoint format 3 without compatibility, scheduler first). WP0/WP1 reports are in `wp0-wp1/` (copied 2026-09-23 after verification: 161/148 tests, 21 GameTests, exact regression zero, P12/P31 byte-identical, idle-tick island visits 700 to 0 and topology snapshots 1 to 0 in all four profiles). WP2 (REST and STEADY certificates) done and verified 2026-09-23 @9b954a4, reports in `wp2/` (161/177 tests, 22 GameTests, exact regression zero, P12/P31 byte-identical; rest100 full solves 2400 to 18, heap 1000 to 331 MiB; through-flow never certifies at the default tolerance, sensitivity at 1e-7 certifies 92 with deviations under 1.5e-8; two benchmark runs and four JVM crashes traced to machine instability, see the review section 7). WP3 (engine-owned presentation, fluid-4, viewers profile) done and verified 2026-09-23 @94077cd, reports and 24 dev-client screenshots in `wp3/` (161/189 tests, 26 GameTests, exact regression zero, P12/P31 byte-identical, physics bitwise unchanged with viewers). Benchmark window analysis in `wp2/BENCHMARK_WINDOW_LENGTH_ANALYSIS.md` (60 s warm-up + 60 s window from WP4 on). WP4 (checkpoint format 3, persisted certificates, save time) done and verified 2026-09-23 @3b6f206, reports and 12 screenshots in `wp4/` (161/193 tests, 28 GameTests, exact regression zero; 1,000 certified islands load without a solve; a certified rest100 save is 7.6 ms warm against 78 ms with certificates off). WP5 (in-game CPU and RAM before/after on a dedicated server and in the client, paced repeats, batch review) done and verified 2026-09-23 @1401cab; reports, rig scripts, run summaries and screenshots in `wp5/` (the 48 JFR recordings, 1.5 GB, stayed in the agent worktree until it was removed with them on 2026-09-24 at the owner's instruction; the analysed summaries in `wp5/` are the record). Batch review `FLUID_ISLAND_REST_REVIEW.md` and the stress-test/acceptance sections are at the batch root. Merged to main 2026-09-23 by fast-forward to 23beadd, version 0.3.0. Existing development worlds use checkpoint format 2 and are refused: create fresh worlds.
