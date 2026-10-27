# Moved references of the crystal and junction holds (t56, 2026-10-20)

Javac-lane suites (`tools/unified-thermo-port-m5/d40`) at the final tree `7b810779`, against `a08c7134` (0.9.1), compared with
`compare-test-output.pl` (timings and byte counts blanked). All pass: science-fluid-thermo 66, science-fluid-network 184, runtime-fluid 311,
science-material 85, junction seeding gates 7, extras 7. Chain-100 deviates by exactly 0. The full Gradle `test` is at the end.

## Physics expectation
- **D61** moves a reference only where a crystal reaches a junction above its melting point. No suite world does, so no line moves.
- **(d)** moves a reference only where the balance sweeps a sealed junction. No suite line moves.
- **Subnormal flush** moves nothing measurable: the dropped masses are below 2.2e-308 kg.
- **D62** moves every reference in which a closure stood at the end of an interval and nothing moved its pipe at the next start: such a pipe now stays plugged.

## Lines that move (all from D62; everything else equal or timing only)
| Test | Before, at `a08c7134` | After | Physics |
|---|---|---|---|
| `FullTankSolidsEventTest.theFilterCapturesOnceTheDrainedTankLetsTheLineFlow` | Line `0->1` open (`blocked=0`); tank 509.6737898696548 kg | Line `0->1` plugged (`blocked=3`); tank 509.6737898645926 kg (1e-11) | The line is still and an end holds the solids that closed it, so the plug carries. 0.9.0 had it plugged too (by the roundoff donor pick); 0.9.1 reopened it. |
| `SlurryTankPumpTest.aSideOutletClosedByDepositionLeavesThePumpOnItsTarget` | Pump on target 2.18013 kg/s; no reason recorded | The same numbers; the side outlet's DEPOSITION closure (velocity 0, deposition 1.016 m/s) is declared at t=0 | The carried closure is reported at each interval start, as a closure always is. |

## New lines and tests
- `HoldClassWorldsTest`: `aCrystalArrivingInAWarmJunctionMeltsThere` (cold 778, prints `MELT_GATE seed=778 tankLine=0/1 flow=0.0`), `aSealedJunctionTheBalanceHasBalancedKeepsItsPressure` (cold 743, 14 blocks), `aClosureNothingMovesCarriesAcrossTheIntervalStartInATankDrainingToAVoid` (cold 183, 10 blocks), `aClosureNothingMovesCarriesAcrossTheIntervalStart` (cold 15, 11 blocks).
- `SolidRuntimeTest.aSubnormalMassIsNoStock`.
- Rule-off proofs (one test each): D61 holds 778 E4 at 1185 (1.9329645729104783e-7, bitwise the base); (d) holds 743 E5 at 11358; D62 holds 183 E5 at 2379.

## Quick check (200 seeds, 8 workers, alone, against `a08c7134`)
| Distribution | Clean | Seeds that differ (holds; class) |
|---|---|---|
| cold | 189 → 190 | 183 E5 → clean; 118 52 → 20 holds (held both); 72 11 → 13 (held both, E5 at 4143 where the base held F at 1347); 35 3 → 5 (F, wall deadline, held both); 48, 100, 163 clean both (crystal mass moves at 1e-3 to 1e-6) |
| bundled | 195 → 195 | none |
| defaults | 200 → 200 | none |

| Clean in both | Newton iterations | Jacobian builds | Step attempts |
|---|---|---|---|
| cold (189) | +0.10 % (+191) | +0.44 % (+238) | −0.33 % |
| bundled (195) | −0.15 % | 0.00 % | −0.01 % |
| defaults (200) | +0.01 % (+1) | −0.02 % | 0.00 % |
- **Cost gate missed** on cold (Newton iterations and Jacobian builds rise) and defaults (+1 iteration). Per seed: 48 +531 (D62), 181 +28; 15 −188, 163 −171, 100 −8, 122 −1. Defaults: seed 1 +1, from (d).

## Named runs (1 worker, Newton iterations, base → final)
- Cold: 778 held (6 212, 14 holds) → clean (5 139); 743 held at 11358 → held at 13758 (open item in `HOLDS_PLAN.md`); 15 8 498 → 8 310; 829 6 230 =; 850 3 322 =; 573 13 355 → 13 356 (item 4); 762 59 477 =; 720 96 179 → 96 252; 315 9 497 =; 579 29 919 =; 736 80 476 → 82 243; 183 held → clean; 118 held both; 72 held both; 163 clean both.
- Bundled 145 and 188 clean on both; the D46 gate (`JunctionSeedingGateTest`) prints `D46_GATE seed=188 createdWaterKg=1.286498`, unchanged.

## Full Gradle `test` (alone, `gradle-locked.sh test --rerun`, tree `7b810779`)
1494 tests, 0 failures, 0 errors: m19's 1489 plus the five new tests (four `HoldClassWorldsTest` gates, `SolidRuntimeTest.aSubnormalMassIsNoStock`). The 78 V3 result files ran fresh. 3 min 32 s. Results in `tools/crystal-junction-holds/logs/t56-logs.tar.gz` (`gradle-results/`).

## Round 2 (t56b, final tree `34c79758`: a junction nothing reaches is stated at its own enthalpy)
- **Expectation.** It moves a reference only where a balance puts a junction nothing reaches at a new pressure. No suite world has one, so no line moves.
- **Suites.** All pass (runtime-fluid 312: the new gate `HoldClassWorldsTest.aJunctionPutAtANewPressureIsStatedAsWhatItsRowMixes`, the full cold 743 world). The moved lines are round 1's two D62 lines and nothing else. Chain-100 exact. Full Gradle `test`: 1495 tests, 0 failures. Rule off (`rule-off-row-seed.patch`): the gate holds E5 at 13758.
- **Quick check against `a08c7134`.** Cold 189 → 190 clean. Differing seeds: 183 clean (was E5); 118 52 → 20 holds; 72 11 → 13; 29 holds 6 → 6, its class now with `b:trace-underflow`; 35 3 → 5 (F); 48, 65, 100 and 163 clean in both. Bundled and defaults: no seed differs.

| Clean in both | Newton iterations | Jacobian builds |
|---|---|---|
| cold (189) | +0.16 % (+317) | −0.37 % |
| bundled (195) | −0.13 % | +0.01 % (+2) |
| defaults (200) | +0.01 % (+1) | −0.02 % |
- **Cost gate missed narrowly**: cold iterations, bundled builds and the defaults' one iteration rise. Per seed (cold iterations): 48 +699 (+531 from D62, +168 from this fix), 181 +28, 65 +16; 15 −188, 163 −171, 71 −40.
- **Named, one worker (Newton iterations).** 743 clean (41 414); 778 clean (5 139); 183 clean (241 927); 163 1 495; 48 60 736; 15 8 310; 573 13 344 (report only); 829 6 231; 850 3 322; 762 59 477; 720 96 533; 315 9 902; 579 29 659; 736 83 501. Bundled 145 and 188 clean; D46 1.286498 kg.
