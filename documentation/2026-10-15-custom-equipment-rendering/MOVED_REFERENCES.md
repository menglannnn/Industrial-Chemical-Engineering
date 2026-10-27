# Moved references against 0.9.2 (OD4 and the OD4 hold fixes, 2026-10-20)

Owner choice 2026-10-20: "Option 1: keep OD4." Rule OD4 (`d1701780`, 2026-10-16, `PhysicalFluidTopology.half`): a pump's,
compressor's or valve's half link takes the bore and roughness of the pipe it joins. Before OD4, an actuator had the bore
of its own block. With OD4 off (the m21 lane's patch `09-od4off/rule-off-od4-half-link.patch`), every line in the first
table is the 0.9.2 line (m20). The fixes of `OD4_HOLDS_REVIEW.md` (fix 1 `f983306b`; the arriving seed on the junction's
buffer, round 2, uncommitted) move the second group.

Sources:
- m20/m21 printed output: `tools/new-units/logs/merge-final-lane-2026-10-20.tar.gz` and
  `tools/crystal-junction-holds/logs/m20-logs.tar.gz`.
- m22: `tools/new-units/logs/merge-final-lane-m22-2026-10-20.tar.gz`.
- Round 2 full test and its comparisons (`cmp-vs-m20/m21/m22.txt`): `tools/new-units/logs/od4-holds-r2-2026-10-20.tar.gz`.

All three archives are in the main checkout.

## Moved by OD4 (these lines are the same as m21 after the fixes)

| Test | 0.9.2 (m20) | Now | Physical reason |
|---|---|---|---|
| `LiquidCo2LetDownTest`, 0.004 m slice, valve flow | 0.28264 kg/s, VALVE_OPEN | 0.23068 kg/s (-18.4 %), VALVE_OPEN | The valve block is 50 mm. Its two half links now take the 4 mm of the let-down line they join, so the open valve adds about 1 m of 4 mm line to the let-down's resistance. The tank lines move with the flow (slice 4: 2.36 kg of fluid against 2.89 kg). |
| `PumpCavitationTest`, `PUMP_CAVITATION_STEADY` | 8.0132 kg/s, suction 3173.93 Pa | 7.2201 kg/s (-9.9 %), suction 3173.93 Pa | The 50 mm pump block between 30 mm pipes now has 30 mm halves. The suction line is longer in 30 mm, so at the same suction pressure (the water's vapour pressure, still the suction limit) it delivers less. |
| `FluidPumpedFillLineTest`, gas transfer (compressor) | rig RWCPR: certified at online 400, 4 jobs, replaying 3.792e-35 kg/s since 20.0 s | rig RWCWR: certified at online 300, 3 jobs, replaying 1.694e-22 kg/s since 15.0 s | The branch's rig puts a wide (0.1 m) pipe on both sides of the compressor, so with OD4 both halves are 0.1 m. The transfer reaches the ratio limit (1.01) and its shutoff one job earlier. Asserts unchanged (ratio held to 3 Pa, CLOSED, certified). |
| `JunctionSeedingGateTest.aJunctionCarriedAcrossAReplacementHasItsBufferSizedOnWhatItHolds` (bundled 145) | passes; valve 10 mm, choked | m21: held E7 at 3867. Now passes (fix 1). | The valve's halves are 50 mm. The second tank fills and drains through the valve, which ends wide open at 12.56 kg/s (154.6 kPa upstream, set point 150 kPa); 33.5 kg/s leaves by the drain (46.0 kg/s through). The gate asserts only that the island keeps up; no expected value moved. Its Javadoc now states the OD4 world. |

## Moved by the hold fixes (against m21; OD4 on)

| Test | m21 | Now | Physical reason |
|---|---|---|---|
| `JunctionSeedingGateTest`, `JUNCTION_SEEDING_GATE seed=15` | attempts=242, newtonRefused=[0,0,0,0,1] | attempts=240, newtonRefused=[0,0,0,0,0] | The arriving seed is the PH split of the buffer and its deliveries: one refused Newton fewer. Same outcome (ok at 0.05). |
| `JunctionWaterTraceTest`, `JUNCTION_ARRIVING_PHASE` | waterToVoidKg=18.51159, share 1.0, junctionT 298.148917 | waterToVoidKg=18.32124 (-1.0 %), share 1.0, junctionT 298.148917 | Water falls into an air-filled fitting. The arriving seed is now the PH split of 11 g of air plus the water, which humidifies the air at its own enthalpy instead of a TP flash at the mixed temperature. The first steps take another path and pass 0.19 kg less over the run. The physics asserted (air displaced, liquid share 1.0) is unchanged. |
| `PumpCavitationTest`, `PUMP_CAVITATION_LNG` | flows 0.0859470 kg/s, tankP 127676.80 Pa, suctionP 128100.34 Pa | flows 0.0859475 kg/s (+6e-6), tankP 127673.40 Pa, suctionP 128096.88 Pa | The LNG suction junction's seed is the PH split (boil-off at the suction's bubble point) instead of the TP flash at the mixed temperature: 3.4 Pa on a 128 kPa tank. |
| `HoldClassWorldsTest`, `MELT_GATE seed=778 tankLine=1` | flow -6.39e-7 kg/s (m22: 0.0) | flow -1.02e-4 kg/s | The CO2-frost tank's line is at rest in all three (the gate's bound is 1e-3 kg/s). The run takes another path from the arriving seeds; the line is at rest within the bound. |
| `HoldClassWorldsTest.aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn` (D34, minimized bundled 322) | passes; STATIC_COLUMN_WORLD gas column 44.999 Pa | fails: held F (wall deadline) at 4497 | This is not a moved value: the gate world is on a knife edge. With the m21 TP rule, a mixing temperature times (1±1e-9) or (1+1e-12) also holds F at 4497. The upper tank's trace condensate (0.086 mL) stops the step Newton at its iteration limit. Owner item in `OD4_HOLDS_REVIEW.md`. |

Round-off only (same lines and modes): `PumpBottomPortDrainTest` (453 lines, at most 4.5e-11 relative) and
`HoldClassWorldsTest` VANISHING_RULE nodes (1e-14 relative). Against m22 (the property-state enthalpy of round 1),
`PumpCavitationTest` LNG and `JunctionWaterTraceTest` differ only at 1e-15. Round 1's form had the same physics there;
it differed only where a junction's property state and buffer were different mixtures (cold 583 and 743).

## Not moved

- G2 chain-100 and the GameTests are not run here (next final lane). The size lines of `FluidCheckpointFormatTest` and
  `FluidCheckpointStoreTest` (unit format 5, +8 bytes per island) come from the branch's level height, not from OD4.
- Fix 1 changes nothing where no pass sequence was refused as a cycle. It only lets a sequence continue where a column change made a pass look like a repeat.
- Every other class of the 1571 tests prints the m21 lines.

## Round 3 (2026-10-20, E5: a hydrocarbon liquid vanishes on its volume share)

This round ran the targeted classes only (owner: "no full test this time, only test affected parts"): 19 classes, 135 tests, 0 failures. The list is in `OD4_HOLDS_REVIEW.md`. The printed lines were compared with `compare-junit-xml.pl` against round 2's full run (the code of `8fd61a13`/`48546bb7`), against m21 and against m20. Against m21 and m20, the classes that move are the same as in round 2, with the same lines, apart from the two below. Against round 2:

| Class | Line | Before | After | Reason |
|---|---|---|---|---|
| HoldClassWorldsTest | D34 gate (`aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn`) | `EVENT_PATH_HELD ... Held[tick=4497 ...]` (round 2's failure) | `STATIC_COLUMN_WORLD upper P=429223.359 liquid=0.0 lower P=429268.367 liquid=0.0 gas column=45.008 Pa ... drop line=6.33e-6 kg/s` | The gate passes. Against m20/m21 (`upper P=429223.633`, `lower liquid=4.25e-7`, `45.0 Pa`, `6.58e-6 kg/s`), the lower tank's 0.43 mL trace liquid now vanishes on its volume share (4.3e-7 of a 1 m3 tank at 437 K). The upper pressure moves 0.27 Pa and the gas column 0.009 Pa. |
| FluidCheckpointFormatTest | first line | none | `WARN StatusConsoleListener Advanced terminal features are not available in this environment` | Log4j start-up notice. In the targeted run this class is the first to start logging. Not physics. |

## Round 4 (2026-10-20, `a6a305a1`: a junction's own split is its own rows; the 889 gate)

The 21 affected classes on the javac lane, 149 tests: 148 pass. The one failure, `FluidCheckpointFormatTest.aCertifiedPayloadIsCopiedNotReencodedUntilASolveOrACertificate`, fails on base `2e28a20a` in the same lane too. Comparison: `OD4_COLD285_GATE889_REVIEW.md`, logs `tools/new-units/logs/od4-holds-r4-2026-10-20.tar.gz` (`out/targeted-tb.txt`, `out/targeted-fix.txt`).

| Class | Line | Before | After | Reason |
|---|---|---|---|---|
| HoldClassWorldsTest | `STATIC_COLUMN_WORLD` (D34 gate) | upper 429223.359 Pa, drop line 6.33e-6 kg/s | upper 429223.634 Pa, drop line 9.60e-9 kg/s | the upper tank's arriving trace condensate is now judged by the junction rows' closure; the gate still passes |
| PumpCavitationTest | `PUMP_CAVITATION_ARRIVAL` (new) | none | suction 14675.478 Pa = P_sat(326.666 K), 0.4788 kg/s, PUMP_SUCTION_LIMIT | the new gate |
| PumpBottomPortDrainTest, PumpCavitationTest LNG, JunctionWaterTraceTest | slice lines | | at most 9e-11 relative | round-off of the closed seed |

## Round 5 (2026-10-20, `48fb5678`: a flashing junction's proposal starts at the balance)

| Class | Line | Before (`a6a305a1`) | After | Reason |
|---|---|---|---|---|
| JunctionArrivingPhase world (affected classes) | `JUNCTION_ARRIVING_PHASE` | waterToVoidKg 18.321242189788805, junctionT 298.14891729314854 | 18.32124218978838, 298.14891729314866 | 2e-14 relative, round-off of the restated steam seed |

Every other printed line of the 21 affected classes is unchanged; chain-100 deviation 0.

## Round 6 (2026-10-20, `ca21bab8`, flash seed, `FLASH_SEED_REVIEW.md`)
No line moves against `a6a305a1`: the 21 affected classes print exactly `a6a305a1`'s reference lines, so round 5's `JUNCTION_ARRIVING_PHASE` move is undone. Chain-100 deviation 0; full test 1 572/0/0.
`ff71d370` (729, the at-rest junction seed, `COLD729_REVIEW.md`): no line moves either; chain-100 deviation 0.

## Round 7 (2026-10-20, `416f8601` + `2def39dc`, D61 labels, `D61_LABELS_REVIEW.md`)
No line moves against `ca21bab8`: every printed line of the full suite (`416f8601`) is `ca21bab8`'s once timings and object hashes are stripped; `SolidTransportAcceptanceTest`, which failed on `416f8601`, prints `ca21bab8`'s line on `2def39dc`. Chain-100 deviation 0.

## Round 8 (2026-10-20, `37797661`, a junction seed laid out for its mixture's phases, `COLD994_REVIEW.md`)
Against `2def39dc` (whose lines equal `ca21bab8`'s, round 7), on the javac lane's 21 affected classes (148/149, the known lane failure); chain-100 deviation 0.

| Class | Line | Before | After | Reason |
|---|---|---|---|---|
| HoldClassWorldsTest | `MELT_GATE seed=778 tankLine=1` | flow=-1.0200123231406369E-4 | flow=0.0 | the tank's line is exactly at rest over the last interval (the gate asserts abs(q) < 1e-3) |
| HoldClassWorldsTest | `VANISHING_RULE island 18 node 17` | m=0.5047766967120231 liq=0.0014355568996411794 | m=0.5139828992978396 liq=0.001461738828655833 | the junction's property state at another mass, the same density (2.8439e-3 m3/kg) and T; a junction's stock is its buffer |
| HoldClassWorldsTest | `VANISHING_RULE island 18` nodes 2, 6, 15; `PUMP_CAVITATION_LNG` | | | round-off, 1e-13 relative |

## Round 9 (2026-10-20, `bde9013c`, one-way closures only off a converged balance, `COLD654_REVIEW.md`)
Against `37797661` (round 8), on the javac lane's targeted classes (148/149, the known lane failure): no printed line moved. Chain-100 deviation 0.

## Round 10 (2026-10-21, `a6b14b22` + `aca82e8f` + `H`, the converging balance, G3 and the still rule, `BALANCE_CONVERGENCE_REVIEW.md`, `STILL850_REVIEW.md`)
Against `bde9013c` (round 9), on the javac lane's targeted classes (148/149, the known lane failure). Chain-100: see the final lane.

| Class | Line | Before | After | Reason |
|---|---|---|---|---|
| HoldClassWorldsTest | `MELT_GATE seed=778 tankLine=1` | flow=0.0 | flow=-1.5684984929373684E-10 | the tank's line at rest to round-off over the last interval (the gate asserts abs(q) < 1e-3); G1 + G2 |
| HoldClassWorldsTest | `STILL_PIPE_GATE seed=850` | solidTransitionRejections=0 | 0 (25 on `a6b14b22` and `aca82e8f`) | the still rule where the rate solve fails (`H`) |
| PumpCavitationTest | `PUMP_CAVITATION_ARRIVAL` | modes=[PUMP_SUCTION_LIMIT, ...] flows 0.47882 suctionP=14675.478 | modes=[PUMP_HEAD_LIMIT, ...] flows 0.47887 suctionP=14659.797 | the corner where both pump limits bind; suction at the vapour pressure on both (the gate asserts the cavitation, G3) |
| PumpCavitationTest | `PUMP_CAVITATION_LNG` | flows 0.4360433 tankP=127673.40 | flows 0.4360413 tankP=127674.29 | 1.5e-4 relative (G3) |
| StaticColumnWorld, D46, VANISHING_RULE, JUNCTION_WATER_TRACE, F1_SLICE | | | | 3.7e-4 (drop line 9.60e-9 kg/s), 3.1e-6, 1.9e-7 and round-off (ledgers at 1e-16, 8.5e-12) |
| SlurryTankPumpTest | `SLURRY_PUMP minted junction` | 106641.4 Pa | 106611.9 Pa | the mint's balance, 2.8e-4 (moved between `37797661` and `e5357594`; the class is outside the targeted set, so the commit is not separated) |
| HoldClassWorldsTest | `FAILED_ITERATE_HELD` (cold 430, 8 blocks) | none (keeps up) | Held[tick=406, island=13, ... Newton iteration limit at residual 0.7654; active-set pass=0] | from `a6b14b22`; the gate asserts no capability hold, not keep-up (`FINAL_LANE_E5357594_REVIEW.md`) |
| VirtualJunctionHoldupTest | `VIRTUAL_HOLDUP_DIGEST` (6 lines) | | | trajectory digests move with round-off ledgers (1e-16); not asserted against fixed values |

Full suite on the final lane (`e5357594`) against fl5 (`37797661`): no other printed line moves beyond 1e-6 once timings, ledgers at 1e-16 and object hashes are stripped. Chain-100 deviation 0.

## Round 11 (2026-10-21, `d139fe93` + `8d69949d`, the balance's sweep first and the water suction floor, `COLD430_889_REVIEW.md`, `FINAL_LANE_8D69949D_REVIEW.md`)
Against the final lane on `e5357594` (fl6), full suite on the final lane at `8d69949d` (fl7). Chain-100 deviation 0.

| Class | Line | Before | After | Reason |
|---|---|---|---|---|
| PumpCavitationTest | `PUMP_CAVITATION_ARRIVAL` | modes=[PUMP_HEAD_LIMIT, ...] flows 0.4788658 suctionP=14659.797 suctionT=326.6437 steamShare=3.8e-5 | modes=[PUMP_SUCTION_LIMIT, ...] flows 0.4788222 suctionP=14675.478 suctionT=326.6658 steamShare=0.0 | D: the floor of the arriving liquid (22 mK warmer than the stated junction), m23's value again; the gate asserts the cavitation |
| JunctionSeedingGateTest | `D46_GATE seed=188` | createdWaterKg=1.286502 washedBufferWaterKg=1.653129 | createdWaterKg=1.286502 washedBufferWaterKg=3.214227 | the bound (water minted into buffers whose water fell) rose by 1.561 kg, one 20 mm liquid-water buffer's m_J; created water unchanged; the gate asserts created <= washed |
| HoldClassWorldsTest | `FAILED_ITERATE_HELD` (cold 430, 8 blocks) | Held[tick=406, ... Newton iteration limit] | line removed: the gate asserts keep-up (A) | |
| HoldClassWorldsTest | `MELT_GATE seed=778 tankLine=1`, `STATIC_COLUMN_WORLD` | flow -1.568e-10; drop line 9.5993e-9 kg/s, gas column 45.013795 Pa | flow -1.677e-10; drop line 9.6025e-9 kg/s, gas column 45.013599 Pa | round-off flows (3.3e-4 of 1e-8 kg/s), column 4e-6 |
| PumpCavitationTest | `PUMP_CAVITATION_696` | flows [..., 0.0] | flows [..., 2.8e-38] | round-off |

No other printed fluid line moves beyond 1e-6 once timings, ledgers at 1e-16 and object hashes are stripped (LevelHeadTest and VesselStateFollowsContentsTest at 1e-14).

## Round 12 (2026-10-21, `c7b883cb`: A reverted, C + D kept, `FINAL_LANE_C7B883CB_REVIEW.md`)
Against the final lane on `e5357594` (fl6), full suite on the final lane at `c7b883cb` (fl8). Chain-100 deviation 0. Round 11's A lines (MELT_GATE, STATIC_COLUMN_WORLD, the 430 gate's keep-up) are fl6's again.

| Class | Line | Before | After | Reason |
|---|---|---|---|---|
| PumpCavitationTest | `PUMP_CAVITATION_ARRIVAL` | modes=[PUMP_HEAD_LIMIT, ...] flows 0.4788658 suctionP=14659.797 steamShare=3.8e-5 | modes=[PUMP_SUCTION_LIMIT, ...] flows 0.4788222 suctionP=14675.478 steamShare=0.0 | D, as round 11 (m23's value) |
| JunctionSeedingGateTest | `D46_GATE seed=188` | washedBufferWaterKg=1.653129 | washedBufferWaterKg=11.409813 | the bound rose by 9.757 kg, one 50 mm liquid-water buffer's m_J (the pump's suction junction minted liquid); createdWaterKg=1.286502 unchanged |
| PumpCavitationTest | `PUMP_CAVITATION_696` | flows [..., 0.0] | flows [..., 2.8e-38] | round-off |

No other printed fluid line moves beyond 1e-6 once timings are stripped (LevelHeadTest at 1e-14).
