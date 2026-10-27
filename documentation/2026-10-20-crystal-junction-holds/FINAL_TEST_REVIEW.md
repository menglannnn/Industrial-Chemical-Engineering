# Final test of the crystal and junction holds (t57, Opus, 2026-10-20)

Run m20 at `bb9de943` (`claude/crystal-junction-holds`, code tip `34c79758`). Baseline m19 (`fea62919`, the code of `a08c7134`); also m16 (`798bedd9`) for cost. Alone on the machine except the owner's geosim JVMs. Counters are deterministic (m16 bundled replays m19's 648 452 exactly). No code change, no commit. Code guide: no change (test only). Logs: `tools/crystal-junction-holds/logs/m20-logs.tar.gz`.

## Verdict: GREEN
The batch clears 11 cold seeds and 1 bundled seed against m19 and makes no seed newly held. It lowers the Newton counters on the cold and bundled seeds clean in both. The quick check's cold +0.16 % does not hold on 1000 seeds: the result is −0.07 % iterations and −1.38 % builds. The one rise left is defaults' +3 iterations and +2 builds over 1000 seeds. That is t56's accepted narrow miss (+1 on 200 seeds), confirmed at +0.004 %.

## Full test, G2, GameTests
- **1495 tests, 0 failures.** The lane step was UP-TO-DATE, so it was rerun for real with `--rerun` through `gradle-locked.sh` (4 min 5 s).
- The count is m19's 1489 plus 6 new tests, none removed: 5 in `HoldClassWorldsTest` (`aCrystalArrivingInAWarmJunctionMeltsThere` 778, `aSealedJunctionTheBalanceHasBalancedKeepsItsPressure` 743/14, `aClosureNothingMovesCarriesAcrossTheIntervalStartInATankDrainingToAVoid` 183, `aClosureNothingMovesCarriesAcrossTheIntervalStart` 15, `aJunctionPutAtANewPressureIsStatedAsWhatItsRowMixes` full 743) and `SolidRuntimeTest.aSubnormalMassIsNoStock`.
- **Printed output** (`compare-junit-xml.pl` against m19): 272 classes. 253 are equal and 16 differ in timing only (all 8 V3 classes among them). 3 move physics, 8 lines in all, each expected by `MOVED_REFERENCES.md`:
  - FullTankSolids: line `0->1` `blocked=0` becomes `blocked=3`, and the reservoir mass goes 509.6737898696548 → …645926 kg (expected, D62).
  - SlurryTankPump: the side outlet's DEPOSITION closure is declared at t=0 (velocity 0, deposition 1.016 m/s), and the pump numbers are unchanged (expected, D62).
  - HoldClassWorldsTest, new: `MELT_GATE seed=778 tankLine=0 flow=0.0` and `tankLine=1 flow=-6.393579826392406E-7`. Both match t56b's Gradle run byte for byte. The file abbreviates line 1 as "0/1 flow=0.0", so it expects these lines; −6.4e-7 kg/s is rest.
- **G2:** chain-100 exact, deviation 0. **GameTests:** 31/31.

## Campaign (1000 seeds, 8 workers) against m19
| Distribution | Clean | Newly held | Newly clean (was) | Held both, class or first tick changed |
|---|---|---|---|---|
| cold | 944 → 955 | none | 183 E5, 473 E8, 475 F, 521 E8, 568 E8, 589 E4, 743 E5, 768 F, 778 E4, 786 E8, 866 F | 29 E5→B @3114; 72 F@1347→E5@2143; 543 E8@7332→E5@6932; 555 B→E5 @2434; 683 F 2272→2268; 694 E5→F @1386; 757 E9@10394→B@6594; 833 B@1252→F@1435; 919 E5@2248→B@2548; 994 E6@4744→E5@4844 |
| bundled | 989 → 990 | none | 889 E9 | 157 E4→F @845 |
| defaults | 1000 → 1000 | none | none | none |
- **Expected, all confirmed:** 778, 743 and 183 are newly clean, and 163 is clean in both.
- **Alone replays** (THREADS=1, `06-alone-*`) of the new C/F holds:
  - Cold 694 holds F at 1386. Its next hold is m19's E5 stall at the identical residual 0.010393330404524545, so the island is the same, held in both.
  - Cold 833 holds F at 1435 on island 41. m19 already held island 41 on the wall deadline from 1403; m20 loses m19's B at 1252 and island 39's holds.
  - Bundled 157 holds E4 at 845, as at m19 (equation gate 1.0763917149497415e-7).
  - None of the three is a new hold.

## Cost on the seeds clean in both
| Clean in both | Newton iterations | Jacobian builds | Residual evaluations | Step attempts |
|---|---|---|---|---|
| m19→m20 cold 944 (1 worker) | −0.07 % (−916) | −1.38 % | +0.20 % | +0.02 % (+29) |
| m19→m20 bundled 989 (1 worker) | −0.20 % | −0.01 % | −0.03 % | −0.00 % |
| m19→m20 defaults 1000 | +0.00 % (+3) | +0.01 % (+2) | +0.02 % | 0.00 % |
| m16→m20 cold 943 / bundled 988 (8 workers) | −4.59 / −0.49 % | −9.60 / −0.20 % | −12.43 / −0.34 % | −1.34 / −0.00 % |
| m16→m20 defaults 1000 | +0.02 % | −0.03 % | −0.03 % | 0.00 % |
- **Split, cold Newton iterations against m19:** 55 of the 944 seeds move (27 rise, 28 fall).
  - Seed 48: +699 (60 037 → 60 736), as t56b measured.
  - The next five risers, together +4 895: 736 +3 025 (80 476 → 83 501), 818 +722, 315 +405, 385 +389 and 720 +354.
  - The other 938 seeds: −6 510. The main fallers are 698 (−3 444), 534 (−2 331), 311 (−423), 788 (−288), 579 (−260), 15 (−188) and 163 (−171). 573 is 13 355 → 13 344 (report only).
- **Defaults:** seven seeds move, by 1 to 4 iterations each: 1 +1, 446 +4, 990 +3, 915 +1; 681, 771 and 847 −2 each.
- **The owner's gate:** cold and bundled meet it on both Newton counters. Defaults does not: +3 iterations and +2 builds. This is the accepted narrow miss, and the campaign confirms it.
- **Riser for the owner:** 736 now carries more than 48. It is +2.2 % in round 1 and +3.8 % at the tip; the round-2 fix adds +1 258 of it (82 243 → 83 501 in t56's named runs).

## New worse worlds against m19
None: the batch makes no seed newly held in any distribution, so nothing was minimized or traced. Three held-both seeds first hold earlier: 757 (29 blocks, B at 6594, was E9 at 10394), 543 (16 blocks, E5 at 6932, was E8 at 7332) and 683 (30 blocks, F 2272 → 2268). Held in both, so they are not new holds and were not traced (lean rule).
