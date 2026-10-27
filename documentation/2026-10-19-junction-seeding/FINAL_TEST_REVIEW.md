# Final tests of the junction seeding batch (Opus, 2026-10-19)

Two lanes on `claude/junction-seeding`, judged against m14 (`32bf3fa4`, t43): m15 at `8e470a3e` (t46) and m16 at `798bedd9` (t48: the carry `373c8fee`, the valve modes `bbbd8626`, the changelog `798bedd9`), then the full-test re-run m17 at `81bf147f` (t49) against m16. No code change, no commit. The owner's geosim JVM ran throughout, so cost is judged by counters; they are exactly deterministic (t46's 980-seed bundled sums reproduce to the unit at 8 workers). Logs: `tools/junction-seeding/logs/m15-logs.tar.gz`, `m16-logs.tar.gz`, `m17-logs.tar.gz`. Code guide: no change (test only).

## Full-test re-run (`81bf147f`, m17, t49)
**Verdict: GREEN.** The re-run the owner chose after t44d's pre-merge fixes (`9ce2553a` unit format 4 and `line-cap-saturation-v1`, `47391899` the six balance counters detached, `81bf147f` comments and MATERIALS.md): the lane's full test and G2 only, alone (no dev client, no other test JVM), judged against m16.
- **Full test:** 1482 tests, 0 failures, the same 1482 names as m16 (no test source changed since `798bedd9`). No format test restated: the unit-format refusal test reads `UNIT_FORMAT` and covers 4 as is. None removed: no test pinned the six counters.
- **Printed output** (`compare-junit-xml.pl`): of 272 classes 256 equal, 14 timing only (98 lines), 2 with one size line each: the core record +10 bytes, `FluidCheckpointFormatTest` 143 235 to 143 245 and `FluidCheckpointStoreTest` 1 367 861 to 1 367 871 (the revision text five characters longer in two string entries); in the latter line the gzip core file follows, 393 745 to 393 738 bytes (t44d read the same). No other line moved. **G2** chain-100 exact deviation 0.

## Final test 2 (`798bedd9`, m16, t48)

**Verdict: not green, on the cost gate only.** The full test, printed output, G2, GameTests and holds are green, and every expected move occurred. The Newton's counters rise on clean seeds: against m15 by about 1 % on both random distributions (the follow-ups' own cost, five worlds carry most of it), and against m14 on bundled by +1.2 % iterations / +2.7 % Jacobian builds, all of it seed 483 (the carry's gate world: clean in m14 at 8 164 Newton iterations, held in m15, clean now at 17 372; without it bundled falls -0.3 / -0.2 %).
**The whole batch against m14:** clean seeds cold 924 to 944, bundled 985 to 988, defaults 1000 to 1000; Newton iterations / Jacobian builds on clean-in-both seeds cold -4.1 / -3.6 %, bundled +1.2 / +2.7 % (-0.3 / -0.2 % without 483), defaults -21.1 / -22.0 %.

- **Full test:** 1482 tests, 0 failures: m15's 1480 plus `JunctionSeedingGateTest`'s two new gates (`aBackPressureValvePlacedUnderAChokedGasSupplyOpensAtTheStart`, 271; `aFilterPlacedBeyondATankLeavesAnUnchangedVentHeaderItsState`, 483). None removed.
- **Printed output** (`compare-junit-xml.pl`): against m15 no physics line moves (16 classes, 101 timing lines). Against m14 the same 27 classes and 1369 diff lines as m15's, equal once timings and byte counts are blanked: the moves of `MOVED_REFERENCES.md` with `FluidPumpedFillLineTest` and `FluidDataRangeIslandTest`, as t44c expected. **G2** chain-100 exact deviation 0. **GameTests** 31/31.

| Campaign, m15 to m16 | Clean | Newly clean | Newly held | Held both, class or tick changed |
|---|---|---|---|---|
| cold random | 942 to 944 | 271 (E2), 654 (E6), 737 (E9) | 850 E5, 27 blocks, t9179 | 77 F to E6; 768 E5 t2531 to F t5536; 833 F to B; 521 628 694 tick |
| bundled random | 988 to 988 | 483 (E5) | 145 E9, 25 blocks, t3867 | 157 E5 to F (t845) |
| bundled defaults | 1000 to 1000 | none | none | none |
- Expected (t44c): cold 271 654 737 and bundled 483 clean, bundled 145 held E9: all occurred. Not expected: cold 850.
- **Alone replays** (THREADS=1) of the new C/F holds: cold 768 holds F alone (wall deadline at 5536, 49 kg of crystal in 9 vessels; E5 with budgets lifted); bundled 157 holds E2 at 845, where m15 held E5: a class change.

| Clean in both (COUNTERS) | Newton iterations | Jacobian builds | Residual evaluations | Step attempts |
|---|---|---|---|---|
| cold 941 / bundled 987, m15 to m16 | +0.91 / +1.07 % | +1.78 / +1.39 % | +1.82 / +1.35 % | +1.40 / -0.03 % |
| defaults 1000 (campaign), m15 to m16 | -0.51 % | -0.51 % | -0.37 % | -0.09 % |
| cold 911 / bundled 980, m14 to m16 | -4.13 / +1.22 % | -3.56 / +2.73 % | +1.53 / +3.09 % | -1.93 / +2.03 % |
| bundled 979 (without 483), m14 to m16 | -0.27 % | -0.21 % | -0.91 % | +1.49 % |
| defaults 1000 (campaign), m14 to m16 | -21.08 % | -22.01 % | -15.74 % | +2.36 % |
- **Where the rise against m15 sits** (per seed with one worker; each commit off on `798bedd9` with t44c's `rule-off-carry.patch` / `rule-off-valve.patch`): 792 of 941 cold and 818 of 987 bundled seeds are equal. The carry adds cold +0.62 % / bundled +0.91 % Newton iterations (cold 579 28 710 to 39 608; bundled 678 8 828 to 13 116, 271 4 255 to 5 677). The valve rule adds +0.30 / +0.16 % (cold 1000 4 387 to 8 393; bundled 973 1 943 to 2 963) and most of the cold Jacobian builds (+1.3 %).
- **The balance's counters**, m15 to m16 on the same seeds (balances, unconverged, PH flashes): cold 18 613 (-0.2 %), 3 191 (-1.2 %), 214 k (+1.3 %); bundled 6 333 (-4.5 %), 536 (-3.3 %), 55 k (-2.7 %); defaults 4 210 (-3.1 %), 529 (-6.0 %), 5.7 k (-2.9 %). m14 has no balance.
- **Seed time** (informational, 8 workers): cold 1.08x m15; against m14, an alternated pair on the 187 clean-in-both seeds of 1-200 reads 1.31x and 1.28x (wall 0.95x, 0.92x; the single 911-seed pair read 1.91x, not alternated; t44c's quick check 1.13x); bundled 0.85x / 0.87x; defaults 0.61x / 0.75x.

### New worse worlds against m15 (t44's classifier: budgets lifted, 120 slices)
Neither has the start signature; both are interval-triggered: cold 850 E5 (27 blocks, t9179, 5 427 ticks after the last placement, pass 0) and bundled 145 E9 (25 blocks, t3867, 1 327 ticks after, an invalid junction inventory). Held in both, re-sorted: cold 768 interval-triggered, bundled 157 E2 63 ticks after a placement.

**Trace 1, bundled 145** (pair-minimal 16 blocks: `0@137,1@323,2@323,5@462,6@462,7@462,8@646,10@1041,12@1206,14@1273,15@1572,18@1945,20@2114,21@2152,22@2164,24@2440`).
- Real plant: a water generator (354 K, 374 kPa) fills tank 1 at 51.5 kg/s; 21.5 kg/s drain through a 50 mm bottom line (junction 15) into a void at 1 atm, so the tank fills at 30 kg/s until FULL; tank 2 beyond a closed 150 kPa valve stays empty. The placements at 2152, 2164 (20 mm pipes on a dead-end line) and 2440 (a filter below tank 2) change nothing a plant notices.
- Every tree mints island 22 at 2114, before water reaches junction 15: it holds the N2 placeholder and its buffer is sized on gas, m_J = 0.05 s x 0.225 kg/s = 0.011 kg. `8e470a3e` and `32bf3fa4` re-mint at each later placement and size it on water, 9.5 kg; clean.
- `798bedd9`: each placement is a same-graph replacement, and the carry keeps the 0.011 kg buffer while 21.5 kg/s of water pass (a 0.5 ms mixing lag against the intended 0.44 s). From 2152 each interval costs 2 to 3.5 times the work (429 against 121 checks), and at 3767 the step fails 19 times on the junction inventory: E9.
- Mechanism: a buffer is sized once, on its seed state; `sizeJunctionHoldups` relies on a topology edit re-minting it, and the carry (`withJunctionStates` copies `old.inventory()`) removes that re-mint. An export probe that carries the state and sizes the buffer on it (`patches/probe-resize-carried-buffer.patch`) runs the 16 blocks clean, but does not lower the S1 counters (cold +1.66 %, bundled +0.53 % against m15).
- **Proposition (nothing implemented):** a junction is a small fixed volume whose mixing lag does not depend on what fills it, so its buffer follows its contents. A carried junction keeps its state and has its buffer sized on that state by the mint's own rule: one sizing for the mint and the carry. A broader form sizes every buffer on the state it holds at each compile, which also covers an island minted on a placeholder and never replaced (2114 to 2152 here, on every tree). Risk: D46's unbooked buffer mass also at a carry.

**Trace 2, cold 850** (pair-minimal 20 blocks: `0@121,2@121,3@203,4@597,5@645,6@704,9@1259,11@1293,12@1332,13@1354,15@1903,16@1949,18@2087,19@2449,20@2641,21@2857,22@3251,24@3440,25@3489,26@3652`).
- Real plant: a CO2-rich gas generator (65 mol % CO2, 270 K, 2.83 MPa) and two LNG generators (128 K at 0.83 MPa, 152 K at 2.06 MPa) share a 1 m3 tank. The gas sets 2.83 MPa, the tank fills with LNG at 156 K and goes FULL, the LNG generators are pushed back and nothing flows; the CO2 that met the cold liquid froze, 2.9 kg in the tank. Steady state: rest, the solids stay in the tank.
- `798bedd9`: at 3652 a 400 kPa valve on a dead-end 10 mm line is a same-graph replacement; the carry starts from the committed junction 6 (liquid, 1612 kPa) where `8e470a3e`'s mint balance starts it two-phase at 1690 kPa. Step counts differ from 3667. The intervals at 4279 and 5179 each take 40 solid-transport rejections on the still 20 mm pipe from junction 6 to the tank (1.3e-23 m/s against a 1.275 m/s suspension velocity: DEPOSITION); at 9079 the transitions return and the line search stalls after 162 active-set passes: E5.
- `8e470a3e` reaches the same state at 9079 (junction 6 2831.40 against 2831.35 kPa, solids 2.908 against 2.919 kg) and solves the interval in 3 steps and 7 Newton iterations; `32bf3fa4` is clean.
- Mechanism: the deposition test reads a roundoff flow in a still pipe; its sign picks the donor (the tank, holding solids: DEPOSITION; the junction: MOBILE). The carry only changed the roundoff history: on the same tree the t45 probe alone moves the hold to 4479.
- **Proposition (nothing implemented):** a pipe at rest carries no solids and deposits nothing. The solid transition test reads a flow at or below the connection's resolution flow (owner decision D42, `NetworkReduction.resolutionFlow`, about 0.02 kg/s for 1 m of 50 mm water pipe) as no flow, and such a pipe keeps its open or closed state. It sits in `SolidEventIntegrator.failed`, which decides the closure, and removes the in-step rejections on still pipes. Risk: D42's resolution leaves the rest test, for which the owner reserved it.

## Final test 1 (`8e470a3e`, m15, t46), condensed
**Verdict: GREEN.** Full test 1480 / 0 failures (m14's 1477 plus three gates); 27 classes move physics lines (`MOVED_REFERENCES.md`'s 54 tests, `FluidPumpedFillLineTest`'s first 0.6 s, `FluidDataRangeIslandTest` at roundoff); chain-100 0; GameTests 31/31. Campaign cold 924 to 942 (+33 / -15), bundled 985 to 988 (+8 / -5), defaults 1000. Clean in both, Newton iterations / Jacobian builds: cold -4.1 / -5.1 %, bundled -1.5 / -1.7 %, defaults -20.7 / -21.6 %.
- 20 new held worlds: start signature 9 (cold 6 161 271 654 683, bundled 6 151 483 889), pass 2 after a placement 1 (cold 316), interval-triggered 8 (cold 72 85 274 475 568 705 737 994), budget only 2 (cold 866, bundled 123).
- Bundled 483 (11 blocks): a filter beyond a tank re-minted an unchanged vent header; the balance's round 1 re-mixed it as water and pulled it from 127 to 101 kPa; the rate seed failed, pass 0 stalled. Built as `373c8fee` (the carry).
- Cold 271 (10 blocks): `initialModes` read the minted 100.9 kPa inlet and started a 200 kPa back-pressure valve CLOSED; the balance solved the closed network, pass 1 was singular. Built as `bbbd8626` (the balance decides valve modes).
