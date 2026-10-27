# D40 A: vessel seeds from their own state, a pump's start on its own lines, one UV step rule

Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port`, 2026-10-18, owner decision D40 option A (test world H1 to H4). Three commits on `00ee8385`. Tests ran on the Gradle-free javac lane. Tools and logs are in `tools/unified-thermo-port-m5/d40/` (README; `logs/d40-logs.tar.gz`). Each commit message gives its physics, the change and what it deletes.

| Commit | Rows | Change | Gate; with the rule off, run alone |
|---|---|---|---|
| `b81d3491` | H2 | A pump's start mode reads its own lines only; the island-wide clause is deleted | `SlurryTankPumpTest#aSideOutletClosedByDepositionLeavesThePumpOnItsTarget`: PUMP_TARGET 2.174 and 2.180 kg/s over two slices. Off: holds slice 1, line search stalled at 0.456 |
| `88817099` | H4 | One step rule in the UV pressure search, wet or dry | `WaterIntoCryogenicMethaneTest#theRefreshOfALiquidFull...`: 124.6056 K, 263.47 kPa, 551.097 mol of ice. Off: no convergence in 100 TP equilibria |
| `3db5e5d3` | H1, H3 | A vessel's seed is its committed layout, with entry traces and arrivals in their own phase (gas beside free water humidified). A crossing is the engine's own-fluid equilibrium: PH while slack, UV while taut, under the TP request's competition. A failed vessel closure proposes nothing. Deleted: the vessels' TP-amount seeds and D26's failed-iterate splits | `VesselPhaseSeedTest`: H1's first step takes 1 pass and 2 iterations (off: 6 passes, 45); H3 vents to 136.90 K and 544 kPa in 30.15 s, passing 1.87 kg/s of gas (off: holds slice 1, line search stalled at 0.0075) |

Totals at `3db5e5d3`: 975 tests in 16 groups; 974 pass. The one failure is `PackagedSparseLuTest`, which needs the built jar (same at the base). No reference was re-recorded.

Moved numbers:
- `b81d3491` moves none. `88817099` moves UV TP call counts only (16.7 against 16.8 on average), with the same answers.
- `3db5e5d3`, against `88817099`:
  - MIXED_GAS_COST newtonSolves 339 (340). The 8 liquid junction lines are unchanged. STACK_PASS upperLiquidKg moves by 1.9e-10 kg.
  - Row E: interval 1 costs 7,992 checkpoints (2,089; the bound is 9,000) and interval 3 costs 53,012 (58,167); the states are unchanged.
  - Seed 48's gate tanks: 2.433 and 2.444 MPa, 27.45 and 34.53 kg of frost (2.439 and 2.449, 27.38 and 34.59). Seed 30: 2.394975 MPa (2.394931).
  - The 120 K row books 7.4e-12 mol of water (0; asserted to 1e-9). Gate (c) ice is within 0.16 %.
  - The three-tank fill takes 5 steps with 1 rejected (8 with 4). Phase-port water traces are 3.3e-10 and 5.6e-11 of volume (0).
  - Every other line moves at most 1e-7 relative.

Incident: the first suite run of the final tree (14:34:57 to 14:36:18) overlapped a running dev client, against AGENTS.md. It was discarded and re-run from 14:39:41 to 14:40:50, with the guard clear before and after; every number above comes from the re-run.

Skipped in the lead's re-scope: the seed replays (30/48/430/19), the four rig rows and the fold-redundancy experiment (its patches are kept, not run); the lead's final lane and the rig re-run cover them. Left as candidates: `drainedSeeds`' vessel TP flash (D11) and `emptyBagSplit`'s TP fallback. Code guide: at the merge, update the fluid network and thermo chapters (vessel seed, `phaseCheck` crossing, null failed split).
