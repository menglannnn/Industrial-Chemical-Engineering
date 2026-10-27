# Balance-start convergence (Opus, 2026-10-21), `a6b14b22` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-20: "Keep bde9013c. Next: make the balance start converge where it stops short (654's run under the void, 743's 140 unconverged balances), physics first, affected parts only, then the final lane." Follows `COLD654_REVIEW.md`. Probes and prototypes: `tools/new-units/od4-holds/r7/`. Logs: `tools/new-units/logs/balance-convergence-2026-10-21.tar.gz`. Code guide: no change (not merged).

## Verdict: the final lane is not run on `a6b14b22`, which fails two gates; owner chose (a) (section 6)
## 1. Physics first
- The balance is the island's quasi-steady Kirchhoff state: every junction's net inflow is zero under each line's own law. The loss rises strictly with the flow, so each line's flow at given end pressures is unique, and a one-unknown balance is a monotone root that bisection always finds.
- 743's one-junction balance (#5 at 2.675 MPa, lines of 1.27 and 1.54 kg/s, one into a void) still ended unconverged. Its stored residual was 7.5e-4 kg/s, but a fresh evaluation at the same pressures gave −0.2675 kg/s.
## 2. Two defects in the balance (`a6b14b22`, two code lines plus comments)
- **G1, the line inversion (`BalanceNet.runFlow`).** The search ended on `hi-lo<=1e-14*hi` while `hi` was still +∞ (∞ ≤ ∞). Every search started under its root stopped after one laminar step.
  - 743's 1 cm void line under 2.57 MPa read 1 484 kg/s, capped at 3.44 kg/s, where its law gives 1.536 kg/s.
  - The residual at fixed pressures therefore moved with the history of warm starts. This is the `lastQ` sensitivity of `COLD654_REVIEW.md`. It has been there since `c623b29b` (0.9.0).
- **G2, the balance's slopes.** They were finite differences over 1e-7 of the pressure (0.32 Pa at 3.16 MPa). In 729, lines between neighbouring junctions carry 2 g/s across 6 mPa, so each slope was a secant across the zero flow and the column's dead band. After two iterations no Newton step descended, and 117 balances ended on the 12-sweep budget. They now use 1e-9, which is still a thousand times the inversion's noise.
- Unconverged balances, alone:
  - 729: 124 with G1 → 7 with G2.
  - 743: 140 on `37797661` → 84 → 37. Island 38 ends STEADY again.
  - 654: 15 → 18, all far from tolerance (2e4 times). That is the void's real kink, which `bde9013c` handles.
## 3. Results on `a6b14b22` (against `bde9013c` unless stated)
- Alone, one worker:
  - 654: clean, 1 994 Newton iterations (5 761).
  - 743: STEADY on all three islands, 23 050 iterations (44 940; 23 039 on `37797661`).
  - 729: clean, 21 010 iterations.
  - Bundled 157: the same F holds.
  - 994: held E6 from 4744 (clean on `bde9013c`). It held E6 on m23 too.
- Quick check, 200 seeds, 4 workers, nothing newly held:
  - cold 195 against 194 (161 cleared; 29 stays held, B → E6), Newton −2.1 %;
  - bundled 198 against 197 (123 cleared), Newton −13.3 %, gate refusals 564 → 101;
  - defaults 200, Newton −0.7 %.
- Named worlds: all clean. Cold Newton 127 826 against 146 183 (109 835 on `37797661`; 729 +11 858 and 768 +5 058 against that). Bundled −0.5 %.
- Chain-100 exact.
- Targeted classes 146/149. Two gate failures are new; the third failure is the known lane failure:
  - `PumpCavitationTest` starved suction (from G1): see section 4.
  - `HoldClassWorldsTest` still pipe 850 (from G2): 25 solid transitions refused at rest against 0. Not traced to its mechanism: the balance starts at rest look alike on both trees (probe `r7-rest-balance.pl`).
## 4. The starved suction, and the third form (G3, stored only)
- Physics: a 1 atm water generator feeds a 10 kg/s pump through 30 mm. The pump cavitates, its suction at water's vapour pressure (3.17 kPa), passing about 7.2 kg/s.
- What the code did: the correct inversion now puts the pump on its suction limit in balance round 0. `prepare` re-anchors a pump arriving on that limit only on the hydrocarbon floor, so in round 1 the water pump drew 72.6 kg/s on its rise law. The junction fell to the hull's 101 Pa and was stated as steam. Pass 0 was then singular.
- G3 (`r7/prototypes/`, two shapes) gives one `suctionFloor` statement for both places. The starved gate then passes: 7.22 kg/s at 3 173.9 Pa, exactly the vapour pressure.
- But the 889 gate's pump then ends on `PUMP_HEAD_LIMIT` instead of `PUMP_SUCTION_LIMIT`. The suction is at the vapour pressure (14 659.8 Pa) on both, the flows agree to 1e-4, and the steam share is 3.8e-5. This is the corner where both limits bind, and the gate asserts the label.
## 5. Proposition for the owner
- (a) Recommended: keep G1 + G2 and commit G3. Change the 889 gate to assert the cavitation itself: the suction at the vapour pressure, the flow below target, the steam share under 1e-3. Trace 850's rest refusals on G2. Then the final lane.
- (b) Keep G1 + G2 + G3 and keep the 889 label assertion. This needs a mode rule at the corner, a new design item.
- (c) Revert `a6b14b22` to `bde9013c`. The two balance defects stay, recorded here.
- Separate item: 994's E6 on G2 is not the balance. Junction #49's committed state is 150 K, while the fluid-only PH of its buffer is 172 K at the same enthalpy (crystal family), and pass 1's layout lost its vapour.
## 6. Owner decision (a), 2026-10-21
- G3 committed as `aca82e8f` (`BalanceNet.suctionFloor`). `PumpCavitationTest.waterArrivingAtACavitatingSuctionStaysLiquid` now asserts the cavitation: suction at the vapour pressure (14 659.80 Pa against 14 659.80), flow below target, steam share 3.8e-5. Pump tests 4/4.
- Still pipe 850 traced and fixed in `STILL850_REVIEW.md` (a failed rate solve left no pipe still; G2 only moved which intervals fail).
- Found on the final lane (`FINAL_LANE_E5357594_REVIEW.md`): from `a6b14b22`, cold 430's 8-block gate world (`HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold`) holds E6 at tick 406. The gate passes (it asserts no capability hold), and section 3 missed its printed line.
