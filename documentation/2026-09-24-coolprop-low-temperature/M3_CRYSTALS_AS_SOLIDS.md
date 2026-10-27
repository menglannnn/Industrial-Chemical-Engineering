# M3: crystals as solids (finished; three methane-floor seeds and the cold cost stay open)
2026-10-16. Branch `claude/unified-thermo-port`, head `76e87740` on M2's `a0f20364`, not pushed; owner decisions D30,
D28 A′ and D29 A applied. Tools, rule-off specs, cold fuzz and logs: `tools/unified-thermo-port-m3/`. Code guide: at merge.
## Commits
- `acc775b6` ThermoHold supertype. `675db18b` crystal kind in SolidInventory (component, u0), one size per solid, outlet
  per-solid mean, PACKED_SLURRY 0.5. `4f7ba970` D23 refresh (VesselCrystals: drive, taut/slack crystal test, amount as a
  closure unknown, u0 and wall work, size law, merges). `92d39275` stand-in, `34b80286` solids rest; tests `7b5f1f4c`,
  `aef76c89`, `1eeb8673`; then `63926c52` D30, `741ce603` D28 A′, `76e87740` D29 A, each message led by its physics.
## E1, fixed by D30: the refresh's joint closure runs in the vessel's pass loop
- Physics: seed 35's rigid tank, full of LNG beside 37.28 kg of CO2-I, stays full: the record's crystal (1571 kg/m3)
  takes the room of the engine's 0.45 L bubble. Code: the joint closure had no phase rule (stalled at a 6e-24 m3 bubble;
  E1 on 35, 72, 148); it now runs in InventoryEquilibrium.passes, whose trace fold drops the bubble. No new rule.
## D28 A′: a failed iterate's fold is a trial layout and never writes A85
- Physics: STACK_PASS's upper tank passes its water down under a saturated headspace; the last 1.3 g drains as liquid.
  Code: a failed pass drained it to a trace, the fold wrote A85, and later passes kept it as steam, 25 % supersaturated
  at 295.7 K: 0.47 g stayed (bound 0.3 g). A failed iterate's fold now reads A85 and never writes it.
## D29 A: the stand-in liquid takes its trace support from the answer's own fluid
- Physics: until a refresh freezes it, the frost's share is a supersaturated solution that dissolves arriving methane.
  Code: the stand-in (pure CO2) made every other species vapour-only; it now admits all the vapour holds (three paths).
## Measurements at `76e87740` (one lane, each once; logs 24 to 26)
- Full `test`: 1441 tests, 0 failures. Of 8581 printed lines one moves beyond timings, STACK_PASS: upper tank 0.47 g ->
  0.0 g, lower 0.30 g less, both headspaces 412 Pa higher, still 22.7 Pa apart: the last water drains as liquid instead
  of evaporating into the upper headspace and cooling it (reasoned, not probed). M3's six classes, the 33 junction lines
  and MIXED_GAS_COST (339 solves) print as at `1eeb8673`; no reference re-recorded; log 10's moves against M2 stand.
- Bundled quick check (200 seeds, 8 workers): random 195, defaults 200 clean; every seed line, class and counter as at
  `1eeb8673` (= `a0f20364`; Newton 115216 / 21211): A′ and D29 A move no bundled world.
- Cold quick check (200 seeds, 8 workers): 179 clean.

| code | clean | B | C | E1 | E5 | E6 | E7 | E8 | F | end with crystal |
|---|---|---|---|---|---|---|---|---|---|---|
| `a0f20364` | 181 | 0 | 3 | 0 | 5 | 4 | 3 | 1 | 3 | 0 |
| `1eeb8673` stand-in | 174 | 1 | 10 | 3 | 4 | 2 | 4 | 0 | 2 | 23 |
| `76e87740` | 179 | 3 | 6 | 0 | 3 | 2 | 4 | 0 | 3 | 26 |

- The 8 trace-support seeds: 75, 78, 170 clean, with M2's adoption form's crystal (1.128, 50.21, 4.330 kg). 34, 98, 114
  still hold C at methane's 90.6941 K floor at the stand-in's tick and node, in the first interval after LNG arrives:
  34 and 114 into a fresh tank of nitrogen, whose first step starts from gas with no stand-in for D29 to widen; 98 after
  its LNG generator is placed. 101 holds E7 (was E5) at the stand-in's tick; 183 F (wall deadlines) from tick 8287.
- Other moves against the stand-in: 35, 148 clean (D30); 72 B at tick 1318, island 32 (the A′ probe's place); 110 B
  (was E7) at its tick; 48 C -> F: past its methane-floor hold at tick 1256 it deposits 67.7 kg, then meets wall
  deadlines from tick 1781. No new class or new hold world, so nothing was traced. Against `a0f20364`: 30, 35, 125
  clean now; 34, 98, 114 (C), 101 (E7), 118 (F) hold.
- Cold counters: Newton iterations 4.63 M (stand-in 0.69 M, `a0f20364` 1.41 M), step attempts 152 k (54 k, 68 k), phase
  seeds 161 k (21 k, 7 k). By seed time, 48 is 123 s and 183 64 s of the run's 348 s.
## Open
- 34, 98, 114: LNG into a node of gas only still cools to methane's floor (clean on `a0f20364`); not traced (lean rule).
- Cold cost: the counters above and seed 48's F, for M5's campaign. The water-domain hold names no node; crystal ledger
  totals (M4). Forced choices as in the first report (crystal record at 101 325 Pa, phi after the refresh, vapour port).
