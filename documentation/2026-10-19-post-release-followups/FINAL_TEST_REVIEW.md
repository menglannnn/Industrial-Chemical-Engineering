# Final tests of the post-0.9.0 follow-ups (Opus, 2026-10-19)

Two lanes on `claude/post-0.9.0`: m18 at `197e1aa7` (t53) and m19 at `fea62919` (t54). Baselines: m17 (`81bf147f`, the code of `5b49f940`) for the full test; m16 (`798bedd9`) and m14 (`32bf3fa4`) for the campaign. Alone on the machine except the owner's geosim JVM, so cost is judged by counters (deterministic). No code change, no commit. Code guide: no change (test only).

## Final test 2 (`fea62919`, m19, t54)
**Verdict: GREEN.** Full test, printed output, G2, GameTests, holds and the cost gate meet every expectation of the brief. Against m16 the batch keeps 944 / 988→989 / 1000 clean seeds, cuts cold Newton iterations 6.1 % on clean-in-both seeds, and leaves bundled and defaults flat. Cold 778 and 743 still hold, as t51 announced; both come from `235d69bb` and are traced below. Logs: `tools/post-release-followups/logs/m19-logs.tar.gz`.
- **Full test: 1489 tests, 0 failures.** The lane's step 1 was UP-TO-DATE: Gradle replayed t51's results of `235d69bb`, which has the same code. The test was run again for real with `--rerun` through `gradle-locked.sh` (3 min). The count is m17's 1482 plus 7 new tests, and none is removed: `JunctionSeedingGateTest` carried-buffer sizing (`a313400b`) and D46 (`73fbe48c`); `HoldClassWorldsTest` 850 still pipe and 829 drain closed at the interval start (`235d69bb`); three `MaterialFluidDataTest` tests (4(c), 4(d), option 2).
- **Printed output** (`compare-junit-xml.pl` against m17): 272 classes. 251 are equal, 16 timing only (all 8 V3 classes among them), and 5 move physics (18 lines). These are exactly round 3 of `MOVED_REFERENCES.md`:
  - WaterIntoCryogenicMethane gate (c) holds steady at 0.997 MPa and 148.9 K, with ice 13.1 to 18.4 kg.
  - FullTankSolids: the line stays open (`blocked=3` becomes `blocked=0`), and the tank stays at 509.67 kg.
  - SlurryTankPump: the zero-velocity DEPOSITION refusal is gone.
  - New lines `STILL_PIPE_GATE seed=850 solidTransitionRejections=0` and `D46_GATE seed=188 createdWaterKg=1.286498`.
  - No other line moves.
- **G2:** chain-100 exact, deviation 0. **GameTests:** 31/31.

| Campaign, m16 to m19 | Clean | Newly held (class, blocks, first tick) | Newly clean | Held both, class or tick changed |
|---|---|---|---|---|
| cold random | 944 → 944 | 35 F 22 t1614; 743 E5 22 t11358; 778 E4 10 t1185 | 77, 274, 850 | 72 E5@1051→F@1347, 85 E5→E1@1829, 473 F→E8@2372, 555 E5→B, 694 F→E5, 919 E5 t2648→2248; ticks 521 683 833 866 |
| bundled random | 988 → 989 | none | 145 | 157 F→E4 @845 (as m18) |
| bundled defaults | 1000 → 1000 | none | none | none |
- **Expected, all confirmed:** cold 850, 829, 348, 15 and bundled 145 are clean; 778 (E4 at 1185) and 743 (E5 at 11358) still hold; the net change is cold 0, bundled +1. Against m18, cold 568 and 757 are back to their m16 holds.
- **Alone replays** (THREADS=1) of the new C/F holds: cold 35 holds F at 2214 and cold 72 holds F at 1347, both on the wall deadline. With budgets lifted, 35 (40 s per seed; 3.5 s at m16) is clean: load only, as at m18. 72 holds E5 at 1443 (m16: E5 at 1051): held in both, no new hold.

| Clean in both (counters) | Newton iterations | Jacobian builds | Residual evaluations | Step attempts |
|---|---|---|---|---|
| m16→m19: cold 941 / bundled 988 | −6.08 / −0.29 % | −11.46 / −0.19 % | −13.99 / −0.31 % | −2.41 / 0.00 % |
| m16→m19: defaults 1000 (campaign) | +0.02 % (+13) | −0.04 % | −0.05 % | 0.00 % |
| m14→m19: cold 910 / bundled 981 | −10.66 / +1.03 % | −16.83 / +2.67 % | −15.36 / +2.82 % | −4.35 / +2.03 % |
| m14→m19: defaults 1000 | −21.07 % | −22.04 % | −15.78 % | +2.36 % |
- **The owner's gate holds on the totals.** Bundled against m14 stays at m16's accepted +1 % (483).
- **The split** (per-seed, 1 worker): t53's five seeds give back part of their fall: 48 107 303 → 60 037, 720 95 339 → 96 179, 315 48 679 → 9 497, 579 39 608 → 29 919, 736 86 086 → 80 476. Together that is −26.8 %, against m18's −65.2 %. The other 933 seeds rise +2.00 % (m18: +1.14 %). This is carried by 573 (2 708 → 13 355) and 762 (51 828 → 59 477), both as at m18, and by 818 (+1 534). t51's −20.0 % is exact on seeds 1 to 200 only.

## The two new worse worlds: both from `235d69bb` (clean with `rule-off-roundoff-still.patch`, pair-minimal against `5b49f940`)
**Cold 778, E4, 7 blocks (as t53's: `0@100,1@131,2@153,4@181,7@232,8@435,9@458`).**
- **Real plant:** a 1 m³ tank is liquid-full of LNG at 164.9 K with 19.25 kg of CO2 crystal. It stands at 4.156 MPa, held by a junction below it. A CO2 header (312 K, 4.43 MPa) blows through that junction to air at 2.39 kg/s, the 100 m/s cap. The LNG generator (3.38 MPa) is below the tank, so pipe 0 carries nothing. **Steady state: rest.** `5b49f940` holds it (pipe 1 at −1e-6 kg/s, no solid in the junction).
- **What the code did:** both trees run the same rate pass. At m19, pipe 0 stayed open from 461, where the base closed it on DEPOSITION at velocity 0. This shifted the history by 26 g of crystal and 0.03 K.
  - At 1085 the step's pass 0 lands on a second root: 0.47 kg/s of tank liquid flashes into the junction. It cools the junction from 309 K to 242 K, the denser gas lets the capped vent pass 2.99 kg/s, and both pressures fall 0.22 MPa. The flow scales with dt, so this root is a large-step one.
  - A 0.5 s substep is accepted and carries 0.5 g of CO2 crystal into the 307 K junction.
  - **Failing node: node 3, the junction under the tank.** Its rows solid moment 2 and temperature fail. The residual is exactly proportional to dt (0.013 at 0.107 s, 1.93e-7 at 1.6e-6 s), so it is a constant rate of about 0.011 kg/s: the crystal share of the drain (5.3 % of 0.24 kg/s). The reconstruction books this rate into the junction, and the Newton point does not state it.
- **Proposition (not built):** CO2 crystal at 307 K and 4.1 MPa is 90 K above its melting point, so it melts on arrival. The solid stage should state this once, for the Newton and the reconstruction alike: a junction gives no solid support to a crystal above its melting temperature, and the crystal it receives is booked as fluid. Expected: 778 accepts the step and the tank goes back to rest. Risk: references in which crystal crosses a warm junction may move.

**Cold 743, E5, 14 blocks (`1@739,3@784,4@839,5@840,6@894,7@1222,9@1547,10@1548,11@1548,14@1767,17@2179,18@2198,20@2626,21@2631`).**
- **Real plant:** a 1 m³ tank holds two-phase LNG at 4.96 MPa and 200.7 K with 36.7 kg of CO2 crystal, pressurised by a CO2 generator (291 K, 4.957 MPa). A second CO2 generator (3.77 MPa, pipe 3) is below the tank. Junction 0 joins the tank (50 mm), a 10 mm vent to air and an LNG generator (120 K, 485 kPa, pipe 2). Both tank↔junction 0 and junction 0↔vent are crystal-plugged. **Steady state: rest.** The inflow decays from 0.75 kg/s at 5058 to 0.0077 kg/s at 11258.
- **What the code did:**
  - `5b49f940` plugged pipe 2 from 2632 and pipe 3 from 5758 at velocity 0. It also unplugs and re-plugs the vent pair in every interval at 1.37 m/s (about 30 solid transitions per interval). It reaches rest.
  - m19 keeps pipes 2 and 3 open (still, closed by the boundary in each pass). It keeps the vent pair plugged with no transitions until 11258. Then the vent plug lifts while pipe 0 stays plugged.
  - Junction 0 (2.2 kg) blows down toward air. The iterate takes it to 0.73 MPa with the LNG line feeding 0.47 kg/s, and every step size stalls. The failing rows are the momentum rows of pipes 1 and 2 (0.61 and 0.51) and junction 0's temperature row.
- **Physics:** the blowdown is real. Junction 0 vents in seconds, and below 485 kPa the LNG supply flows through it to air (about 0.5 kg/s at 120 K). The base never met this, because it had plugged a supply line that carries no solid.
- **Proposition (not built):** keep m19's open supply lines, which are the physical choice, and treat what starts the blowdown.
  - Trace why the vent plug lifts at 11258 and not before. t54 did not trace it; the suspect is the still decision flipping on a plugged pipe as the inflow decays.
  - A still pipe should keep its plug as well as its open state, and a plug should lift only on a real interval-start drive.
  - A junction that crosses a supply's pressure should be sized as a start transient (substeps from the interval start).
  - Measure on the quick check.

## Final test 1 (`197e1aa7`, m18, t53), condensed
- **NOT GREEN** on the full test only: 1488 tests and 222 V3 failures. `Cdu17TestCatalog.withProduction` laid the frozen CDU17 water record (no `ice`) over production, and option 2's check (`340e43da`) refused it. Fixed by `987db979`.
- Printed fluid output equalled `MOVED_REFERENCES.md` (5 moves, 2 gate lines). G2 exact, GameTests 31/31.
- Campaign against m16: cold 944 → 944 (newly held 15, 35, 348, 743, 778, 829; newly clean 77, 274, 473, 568, 757, 850), bundled 989 (145), defaults 1000.
- Cost, clean in both, against m16: cold −17.93 % iterations / −19.26 % builds (48, 720, 315, 579 and 736 carried it; the other 933 seeds rose +1.14 %), bundled −0.29 %, defaults +0.02 %.
- All 5 new holds came from `50fd053f`: "still" was read at D42's resolution flow (about 1 kg/s in a 100 mm LNG line). 829 started at that threshold inside the step, and in 778 a still pipe stayed open. t53 proposed roundoff decided at the interval start, which `235d69bb` implements.
