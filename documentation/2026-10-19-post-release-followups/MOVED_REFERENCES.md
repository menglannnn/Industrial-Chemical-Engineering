# Moved references of the post-0.9.0 follow-ups (t51, 2026-10-19)

The javac-lane suites ran on `6c00db3e` (items 1 to 4) and were compared with t44d's logs of `81bf147f`, whose code is that of `5b49f940`. The comparison tool was `compare-test-output.pl`, with timings and byte counts blanked by `notime.pl`. All suites pass: science-fluid-thermo 66, science-fluid-network 184, runtime-fluid 305, science-material 84, the junction seeding gates 6 and the two extra classes 7. Chain-100 deviates by exactly 0. The D46 gate (`73fbe48c`) was run alone on both trees.

## Physics expectation
- **Item 1** (one buffer sizing) acts only where an island is replaced by one of the same graph. No suite world does that, so no suite line moves.
- **Item 2** (a still pipe deposits nothing) moves every reference in which a pipe closed on "DEPOSITION, velocity 0.0", that is, at a flow below its resolution. Such a pipe now stays open while it is still. It closes only when it carries a real flow below the suspension velocity.
- **Items 3 and 4** move nothing printed. The fingerprint fix moves no bundled revision.

## Lines that move (all from item 2; 9 tests, everything else equal or timing only)
| Test | Before, at `5b49f940` | After | Physics |
|---|---|---|---|
| `WaterIntoCryogenicMethaneTest` gate (c) | The vessel jumps between 0.35, 1.0, 0.42, 0.97 and 1.55 MPa over intervals 2 to 6. Pipe 3 closed on DEPOSITION at velocity 0.0 at intervals 2, 4 and 6. Ice 14.5, 6.1, 21.6, 9.4 and 35.5 kg. | Steady at 148.9 K and 0.997 MPa. About 1 290 mol of liquid stand-in water passes each interval. Ice grows 13.1 to 18.4 kg. | Water is driven in at 1 MPa, so the vessel stands at the supply pressure. The old swings came from closing a still line at each interval start. The assertions still hold: no thermo hold, and the refresh freezes the stand-in. **For the owner:** the stand-in water passes through the 149 K vessel to the void and freezes only at a refresh. That is the stand-in simplification, which the old closures hid. |
| `HoldClassWorldsTest.aBubbleTheNextPassDrivesToNothingIsRefused` | Tanks at 2.51 and 2.53 MPa, 158.7 and 155.1 K | 4.89 and 4.91 MPa, 159.9 and 156.3 K | Both runs end with liquid-full stiff tanks behind closed lines, above the 1.95 MPa source. Trapped liquid methane rises about 2 MPa per K, so 1.2 K warmer means +2.4 MPa. The assertion (no vapour) holds. |
| `FullTankSolidsEventTest` | A filter line closed both ways (`blocked=3`); junction at 399 992 Pa | Open (`blocked=0`); junction at 400 000 Pa, the valve's set point. Tank 509.7 kg becomes 502.2 kg. | A still line from a solids-holding vessel stays open. |
| `SlurryTankPumpTest` side outlet | The same numbers, plus one DEPOSITION refusal at velocity 0.0 | The same numbers, no refusal | Same as above. |
| `PumpCavitationTest` LNG | Flows 0.43604 / 0.08595 kg/s | 0.43624 / 0.08599 kg/s; tank liquid 0.0813 becomes 0.0822 m3 (+1.1 %) | The same mechanism at the start of the fill. |
| `MixedGasJunctionTransientTest` (both) and `MIXED_GAS_COST` | `newtonSolves` 299, 97.2 MB | `newtonSolves` 299, 97.3 MB; each case allocates about 3.9 kB more | No physics line moves. Only the allocation counters change. |

The `ExtremeTopologyIslandTest`, checkpoint, `LiquidJunctionTransientTest`, `SolidChainTransportTest` and `PhysicalRegistryTest` lines are timing only.

## New lines
- `HoldClassWorldsTest.aStillPipeBesideATankHoldingCrystalsDepositsNothing` prints `STILL_PIPE_GATE seed=850 solidTransitionRejections=0`. With `rule-off-still-pipe.patch` the same line reads 1 146.
- `JunctionSeedingGateTest` prints `D46_GATE seed=188 createdWaterKg=1.286498 washedBufferWaterKg=1.653124 lives=5`. The values are the same on `5b49f940` with the test added: item 1 leaves D46's composition error unchanged in this world.

## Quick check (200 seeds, 4 workers, against `5b49f940`), named runs
- **Clean seeds:** cold 189 becomes 188 (newly held: 15, an E5 at 11039, transient: it holds for about 500 ticks and recovers at a one-tick slice; also 35, which is clean when run alone; newly clean: 77). Bundled 194 becomes 195 (145 newly clean). Defaults stay at 200.
- **Clean in both, Newton iterations and Jacobian builds:**
  - cold (187 seeds) −41.6 % and −51.0 %, seed time 0.69×;
  - bundled (194) −0.03 % and −0.03 %;
  - defaults (200) 0 % and −0.19 %.
- **Named runs:** cold 850 and bundled 145 are clean. 768, 161 and 6 are unchanged. 72 moves E5@1051 to E5@1543 (alone). 85 moves E5@3392 to E1@1829. 101 moves E6 to E8 at the same tick, 1294. Bundled 157 moves F to E4 at the same tick, 845.
- **Split by rule-off export (8 seeds):** item 2 makes cold 15 and 35 hold. Item 1 makes 77 clean and adds no hold.

## Second round (owner decisions on the options, final tree `c6b7d07b`)
- **Final tree:** `701fb42e` plus option 2 only. Options 3 and 1 were built and reverted. The solver code is byte-identical to `701fb42e`, so the quick check, clean-in-both counters and named runs above stand. Named runs: cold 15 holds E5 at 11039 (transient), 850 clean, 145 clean, 188 clean with the D46 gate at 1.286 kg.
- **Suites at `c6b7d07b`:** all pass (thermo 66, network 184, runtime 305, material 85, gates 7, extras 7). Chain-100 is exact. The same five physics moves as above; `MixedGas` is timing only. Option 2 moves nothing printed and adds one material test.
- **Option 1 (reverted)** against `5b49f940`, clean in both:

  | Distribution | Seeds | Newton iterations | Jacobian builds |
  |---|---|---|---|
  | cold | 188 | +25.1 % | +76.0 % |
  | bundled | 194 | +21.0 % | +49.4 % |
  | defaults | 200 | +0.4 % | −2.0 % |

  - Clean seeds: cold 189→188 (15 holds E9 at 2439, the three-phase limit), bundled 194→197 (6, 123 and 145 clean), defaults 200.
  - Its D46 gate read 0.219 kg of unbooked water and −1.606 kg of resized buffer mass.
- **Option 3 form A (reverted):** cold 15's gate kept up; with the rule off it holds at 10939. `PumpCavitationTest` failed: the pump stood CLOSED with 0 flow, where 0.086 kg/s at its suction limit is the reference.

## Round 3 (owner decisions on t53's final test, tree `235d69bb`)
- **Full test (Gradle `test`, alone):** 1489 tests, 0 failures: m18's 1488 plus the 829 gate. Against m17 (`compare-junit-xml.pl`): 272 classes, 251 the same, 16 timing only, 5 physics.
  - All 8 V3 classes are timing only: V3's printed output does not move (fixture `987db979`).
  - Physics moves left: WaterIntoCryogenicMethane gate (c) (steady at 1 MPa, as in round 1), FullTankSolids (the still pipe stays open, `blocked=0`; the tank is back to 509.67 kg), SlurryTankPump (the zero-velocity DEPOSITION refusal is gone), and the two new gate lines.
  - PumpCavitation and HoldClassWorlds' refuted-proposal line are back to their reference values.
- **Chain-100:** exact.
- **Quick check against `5b49f940`:** cold 189 → 189 clean (cold 15 is now clean); bundled 194 → 195; defaults 200.
  - New C/F holds: 35 (clean with budgets lifted) and 72 (E5 at 1443 with budgets lifted; the base holds E5 at 1051). Two geosim JVMs ran during these runs.
- **Clean in both:**

  | Distribution | Seeds | Newton iterations | Jacobian builds |
  |---|---|---|---|
  | cold | 188 | −20.0 % | −35.0 % |
  | bundled | 194 | −0.03 % | −0.03 % |
  | defaults | 200 | 0 % | −0.19 % |

- **Named runs:**
  - Clean: 850, 829, 348 and 15; the cost fallers 48, 720, 315, 579 and 736; bundled 145 and 188 (the D46 gate reads 1.286 kg).
  - Still held: **778** (E4 at 1185) and **743** (E5 at 11358). Both are clean at `5b49f940`. A roundoff-still pipe stays open, where `5b49f940` closed it by the roundoff donor pick.
