# Final test m22 of the custom equipment rendering merge (Opus, 2026-10-20)

## Verdict: NOT GREEN
Two gate tests fail. The campaign has four new worse worlds against 0.9.2 (m20) that m21 did not have: cold 583 and 743, bundled 322 and 354. Fix 2 of `f983306b` causes all six (the arriving-phase seed as a fluid-only PH split). With fix 2 off, the two tests pass and the four worlds are clean. Fix 1 (the column ends in the cycle key) moves nothing else. 145 and 889 are clean.
Lane at `5511cbb7`, the only Gradle run. The owner's three geosim JVMs ran. JAVA_OPTS=-Xshare:off, no dev client, no code change, no commit. Logs and scripts: `tools/new-units/logs/merge-final-lane-m22-2026-10-20.tar.gz` (main checkout).

## 1. Full test (`--rerun`): 1571 tests, 2 failures
- `HoldClassWorldsTest.aJunctionPutAtANewPressureIsStatedAsWhatItsRowMixes` (the cold 743 world): held E6 at 14158 ("Newton iteration limit", "Solid transport transition inside the step").
- `HoldClassWorldsTest.aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn` (the minimized seed 322 world, D34): held F (wall deadline) at 4497.
- Both fail again with the class run alone (`08-holdclass`), so they do not come from load. Both pass with fix 2 off (`od/fix2off`). They still fail with fix 1 off.
- `OD4_HOLDS_REVIEW.md` ran 218 tests; it did not include `HoldClassWorldsTest`.

## 2. Printed output: m21 → m22, 281 classes: 260 same, 16 timing only, 5 physics
- Listed in `MOVED_REFERENCES.md`, and the values match: `JunctionSeedingGateTest` (145 passes, seed 15 has 240 attempts and no refused Newton), `JunctionWaterTraceTest` (18.51159 → 18.32124 kg), `PumpCavitationTest` LNG (+6e-6).
- Against m20, the OD4 lines match the table: LetDown 0.28264 → 0.23068 kg/s; PumpCavitation steady 8.0132 → 7.2201; FluidPumpedFillLine RWCPR → RWCWR. The checkpoint size lines show unit format 5.
- **Not listed:**
  - `HoldClassWorldsTest`: the two failures above. Also round-off moves: VANISHING_RULE nodes at 1e-14 relative; MELT_GATE seed 778 tankLine flow -6.4e-7 → 0.0 kg/s.
  - `PumpBottomPortDrainTest`: 450 lines, the same modes and counts, at most 9e-11 relative (round-off of the PH seed).

## 3. G2 chain-100 exact: deviation 0. GameTests: 31/31 passed.

## 4. Campaign: 1000 seeds, 8 workers (`cmp-seeds.txt`)
| Distribution | Clean m20 / m21 / m22 | Newly held against m20 | Newly clean against m20 | New against m21 |
|---|---|---|---|---|
| cold | 955 / 955 / 956 | 285 F, 583 E5 (18 blocks, t1481), 743 E6 (22, t14158), 866 F | 35, 547, 592, 757, 779 | held: 583, 743; clean: 547, 592, 768 |
| bundled | 990 / 988 / 988 | 322 F (19, t5151), 354 E5 (27, t1221) | none | held: 322, 354; clean: 145, 889 |
| defaults | 1000 / 1000 / 1000 | none | none | none |
- Defaults counters are identical: 74 403 Newton iterations, 26 391 Jacobian builds.
- The OD4 cold F holds from m21: 285 still held (F at 3421); 768 clean; 866 still held (F, first hold 5677 → 10277).
- Held at m20 and m21, class changed: 994 E5 → F, 6 → B, 555 → B, 897 → B, 919 B → E5, 673 F → E5, bundled 157 E4 → F (157 was F at m20).
- **Alone** (`06-alone`): the new C/F holds that were not in m20 or m21 are cold 994 and bundled 322.
  - 994 holds E6 at 4744. It was held E5 in both baselines, so it is not a worse world.
  - 322 holds F at 5151. It is a real worse world: 12 663 Newton iterations and 479 phase seeds, of which 435 were refused, in 5.4 s. At m20 (one worker), 4 604 iterations and 395 phase seeds, clean in 1.1 s.
- **Fix attribution** (`od/`). Scratch copies of `5511cbb7` with one fix off (patches `rule-off-fix1off.patch` and `rule-off-fix2off.patch`), on the 20 moved seeds:
  - Fix 2 off: 583, 743, 322 and 354 are clean, and 889 holds E9 again.
  - Fix 1 off: those four still hold, and 145 holds E7 again.

## Two smallest new worse worlds (against m20; not minimized)
- **Cold 583** (18 blocks).
  - Physics: at 1474 a 50 mm pipe joins a 7 m dead-end run of 50 mm line, filled with 1 atm N2 at 298 K, to an LNG supply at 127 K and 665 kPa. Steady state: the N2 compresses to about 665 kPa (isothermal estimate: LNG takes 85 % of the 14 L line), the gas cools against the liquid, and the run stops with no flow.
  - The code: 7 ticks after the join, at 1481, every substep stalls (17 line-search stalls, 3 iteration limits), and the island holds E5. It is clean with fix 2 off: the PH seed of LNG arriving in warm-N2 fittings sends the Newton on another path.
- **Bundled 322** (19 blocks). The same world as the failing D34 gate.
  - Physics: about 0.9 mL of crude condensate runs from the upper 1 m3 tank down 1 m of 50 mm line into the lower tank, 2 m down. The line stands as a gas column of about 45 Pa. Methane at 431 kPa vents to the 1 atm voids. The lower tank's gas is at 437 K from the compression of its N2 (298 K × (4.25)^0.286 ≈ 451 K).
  - The code: the island does not keep up. 435 of 479 phase seeds are refused, and it holds F at 5151 (gate: 4497).

## Proposition for the owner
- (a) Keep fix 1. Re-form fix 2 so that the PH split acts only where the TP flash leaves the seed without the phase the row needs: 889's suction junction at its vapour pressure. Re-run the HoldClassWorldsTest class and the 200-seed quick check on 583, 743, 322 and 354 before the next final lane.
- (b) Keep fix 2 as it is, since the enthalpy seed is the physical one. Then treat 583's and 322's paths in the junction-seed stage, and re-pin the two gates.
- Code guide: no change (test lane only). This lane did not touch `documentation/INDEX.md`.
