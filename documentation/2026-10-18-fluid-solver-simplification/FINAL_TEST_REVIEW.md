# Final tests of the fluid solver simplification (t39, t41, t43; Opus, 2026-10-18 and 2026-10-19)

Each lane ran `tools/unified-thermo-port-m5/scripts/lane.sh` (full test, G2 chain-100 exact, fluid GameTests, 1000-seed campaign at 8 workers) with no dev client; the owner's geosim JVMs ran throughout (load, not touched). Printed output is compared with `tools/unified-thermo-consolidation/compare-junit-xml.pl`; cost on the seeds clean in both runs comes from javac-lane counter runs (`07-clean-*`). Code guide: no change (test only).

## Final test 1 (`573b050b`, m12, t39) against m11 (`688f4cd7`)

Forms A, C, B, Newton limit 30 and K6. Full test 1474 -> 1475, 0 failures (+ `PumpCavitationTest` LNG suction gate); 47 classes print moved values, all equal to t38's `k6s` suites except 4 round-off lines; MIXED_GAS_COST 339 -> 299 newtonSolves; G2 0; GameTests 31/31. Clean: cold 911 -> 924, bundled random 986 -> 980, defaults 1000. Cold active-set cycles (E7) 25 -> 0; residual evaluations on clean-in-both -18 % cold, -9 % bundled. New worse worlds: cold E5 x13, B x5, E2 228, E6 793; bundled E5 116, 358, 573, 678, E8 407 and 26, E6 988, F 301 and 788 (both hold alone). Verdict **green**. Logs `tools/fluid-solver-simplification/logs/m12-logs.tar.gz`.

## Final test 2 (`6c073298`, m13, t41) against m12

D38 C amended (`FluidThermodynamics.vanishing` folds a shrinking phase only below `VANISHING_SHARE` both by moles and by volume). Full test 1476, 0 failures (+ gate `aCrudeGeneratorFillsATankToLiquidFullAndRests`); 3 physics classes, 44 lines, all equal to t38's `d38s` suites (oil-water overflow slice 43 P +0.18 Pa and its consequences; methane 120 K +0.012 K); G2 0; GameTests 31/31. Clean: cold 924 -> 926 (683, 797), bundled 980 -> 984 (26, 407, 504, 779), defaults 1000; no seed newly held, no new C or F hold. Cost on clean-in-both: cold Newton iterations -0.5 %, bundled within 0.1 %. Verdict **green**. Logs `.../m13-logs.tar.gz`.

## Final test 3 (`32bf3fa4`, m14, t43) against m13

`32bf3fa4`: `FluidThermodynamics.phaseCheck` refuses a proposed phase that its own split holds below `VANISHING_SHARE` both by moles and by volume, whatever its direction (`nothing`); the two direction tests are deleted. Gate `HoldClassWorldsTest.aReceiverLandingOnItsBubblePointIsNotProposedAVapourBelowItsResolution`. Logs `tools/fluid-solver-simplification/logs/m14-logs.tar.gz`. The lane ran alone (t38, t40 and t42 idle).

| Check | m13 | m14 | Judgement |
|---|---|---|---|
| Full test | 1476, 0 failures | **1477, 0 failures** | +1 the 678 gate; none removed |
| Printed output | | 2 physics classes, 1300 lines; 16 classes timing only | all 650 new lines equal t38's `r678s` suite output; no unlisted move |
| `PhasePortPriorityTest` water drain | | as listed, plus consequences | slices 1380-1382 as listed (3.3e-10 water trace not proposed back, P -0.15 Pa, outGas 5.46e-6 -> 4.53e-6 kg); slices 1383-1999 carry the shift; final P +0.7 mPa, condensate drawn after breakthrough -0.03 % |
| `PhasePortPriorityTest` pentane over water | | as listed | oil-water-drain slices 12-39: a 5.6e-11 water trace in the gas tank is gone, P -0.3 mPa, outGas 1.295e-5 -> 1.280e-5 kg |
| `HoldClassWorldsTest` static column | | as listed | bottom-port liquid trace 4.968 -> 4.958 mL, gas column +0.2 mPa |
| G2 chain-100 exact | 0 | **0** | green |
| GameTests | 31/31 | **31/31** | first try did not start: the JVM could not commit 1.5 GB (paging file, errno 1455, geosim load); rerun on run id `m14b` passed |

| Campaign | m13 clean | m14 clean | Newly clean | Newly held | Seeds 1-200 |
|---|---|---|---|---|---|
| cold random | 926 | 924 | 0 | 698 (F, 29 blocks, tick 3122), 797 (F, 18 blocks, tick 1429) | 191 |
| bundled random | 984 | **985** | **678** (E5) | 0 | 197 |
| bundled defaults | 1000 | 1000 | 0 | 0 | 200 |

Class changes on held seeds: cold 685 E5 -> F at the same tick 1261 (its later eight holds are identical). Bundled: none.

Alone replays (`06-alone-cold-*`, THREADS=1, the three new C or F holds): 698 and 797 are clean (load only, as 698 under geosim load in t41's reruns and 797's F in m12); 685 holds alone at tick 1261 on the wall deadline, the same interval that held E5 in m13 (refinement and wall deadline both near the end of one interval; m12 campaign F, m12 alone E5). It is held in both runs, not a new worse world.

Cost, COUNTERS m13 -> m14, whole runs:
- cold: Newton iterations +4.1 %, residual evaluations +2.5 %, Jacobian builds +6.5 %. The campaign ran under more load (seedMsSum 731554 -> 908278 ms), and its three wall-deadline holds (685, 698, 797) refine until their deadline;
- bundled random: within 0.1 %; phase seeds 1501 -> 1846, phase refusals 531 -> 900;
- defaults: identical.

On the seeds clean in both runs (javac lane, exports `t43hd` = `6c073298`, `t43nw` = `32bf3fa4`, 4 workers):
- cold 924 seeds: every counter identical (the rule fires on no clean cold seed);
- bundled 984 seeds: Newton iterations +0.02 %, residual evaluations +0.1 %, phase seeds 1067 -> 1428 and refusals 472 -> 837 (the proposals at the resolution, refused now).

t40's expected "cold Newton iterations about -1.7 %" (quick check, whole run) is not seen: on the clean cold seeds nothing moves.

**New worse worlds:** none. The cold seeds newly held (698, 797) are F holds under load, and both are clean alone.

**Verdict: green.** Every check passes, and every printed move is one that t38 measured on this tree. The expected effect is there: bundled 678 clears, and the other held seeds keep their class and tick (except 685's label at the same tick). There is no regression: no seed holds alone that was clean, and the cost on the seeds clean in both runs is unchanged.
