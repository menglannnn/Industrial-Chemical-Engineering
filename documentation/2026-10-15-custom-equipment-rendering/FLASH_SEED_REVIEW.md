# Seed pressure of a junction where vapour first appears (Opus, 2026-10-20)
Branch `claude/stoic-bell-ae6019`, commit `ca21bab8` on `48fb5678`, not merged or pushed. Owner request 2026-10-20: "design the seed pressure of a junction where vapour first appears far below its bubble pressure ... physics first, then re-run the final lane". Logs: `tools/new-units/logs/flash-seed-ca21bab8-2026-10-20.tar.gz`. Code guide: no change (not merged; at the merge the network chapter gains `junctionSplit` and `flashSeeds`).

## Verdict: NOT GREEN by one world (729), which is not this item's
- Gates: full test 1 572 tests, 0 failures, 0 errors. Chain-100 exact. GameTests 31/31. Affected classes on the javac lane: 148/149 (the known lane failure), with reference lines identical to `a6a305a1`'s. The 2e-14 move of `48fb5678` is gone.
- Campaign against m23: cold 971 clean against 962; bundled 990 = 990; defaults 1 000.
- Alone: all six named worlds are clean. 729 B is worse; it comes from `a6a305a1` (section 5).
## 1. Physics first
- A junction holds no pressure of its own: its connections set it where its net inflow vanishes. Every connection is capped at 100 m/s of its donor, so an outlet of area A passes at most ρ·A·v. A liquid-only pass carries the outflow at the liquid's density, so a junction that must flash is drained far below its bubble pressure, as a superheated liquid. With the vapour there, the outflow is lighter at the cap, and the junction backs up until what leaves passes what arrives.
- 509: LNG at 4.34 MPa and 160 K enters through 0.1 m at its cap, 311.9 kg/s. As a liquid (377 kg/m3) the junction drains to 170 kPa, a tenth of its bubble pressure. Its two 0.1 m outlets pass 311.9 kg/s only at about 206 kg/m3, which the mixture reaches at 1.24 MPa. Base settles at 1.23 MPa, 155.5 K, with half the moles vapour.
- 592: 298 kg/s through 9.8e-3 m2 of outlets needs about 304 kg/m3, against 356 as a liquid. The root is 1.78 MPa and the bubble point about 1.86 MPa, so the junction barely flashes.
- 170: 3.14 MPa, 153 K LNG to air through 10 mm. The liquid pass leaves the junction at 449 kPa; the network settles near 0.92 MPa.
- 757: a run of two junctions. Vapour proposed upstream (563 kPa, 24 % of the moles, 138 K) feeds a liquid-only junction at 484 kPa and 162 K, below its bubble point. A two-phase stream into a liquid layout below its bubble point has no root, so the run must flash from its downstream end.
## 2. What the code did instead (`48fb5678`)
- The seed pressure came from the island's balance start (`BalanceNet`). Its tables use the fluid-only PH, which answers nothing for CO2-bearing LNG, so 509 was seeded at 168 kPa: the liquid's answer.
- A seed at the root sits just under the bubble point (592). Every step from it walked off to an all-vapour iterate.
- The connections kept the flows of the old pressure. In 170 the junction moved from 450 to 674 kPa, and the next pass stalled on its three connections' hydraulic rows.
- The phase check went in node order. In 757 the upstream junction flashed first, into a liquid neighbour.
## 3. Design (owning stage: the pass loop's phase proposal, `PassiveStepSolver`)
- `flashSeeds` acts only where a junction that held no vapour gets one, both in the phase check's proposal and in the arrival seed. Condensation keeps the point's pressure (118: 98 such proposals).
- `settledFlash` finds the junction's own balance, every other node held. Each connection runs at its own law, saturated at its donor's cap. The junction is the donor of what leaves, at its own split (`junctionSplit`, no engine call). Net inflow falls as the pressure rises. The pressure is marched up ×1.5, at most 12 steps and never past the highest neighbour. The seed is the last trial that still takes in more than it passes: within one step under the root, on the side where the vapour holds.
- `restateFlashFlows`: the restated junction's connections (not movers, not closed) start at their law on the new end pressures.
- `phaseCorrection` checks nodes from the lowest pressure up.
- Absorbed: `balancedProposal` and the balance start as a seed source (deleted). New constants: `FLASH_STEP` 1.5, `FLASH_STEPS` 12.
- The form was built up on the alone worlds (root bisection, then the last trial under the root, the restated flows, the pressure order). Each step cleared one world: 509, 592, 170, 757. Only the last two forms went through the quick check. Quick check (200 seeds): cold 192 = base (178 cleared, 183 F newly held, wall time only); bundled 195 = base.
## 4. Alone (one worker, Newton iterations; H = held)
| Seed | base `2e28a20a` | `48fb5678` | `ca21bab8` |
|---|---|---|---|
| 509 | 288 | 76 109 H | 293 |
| 592 | 5 153 | 46 852 H | 4 266 |
| 170 | 4 987 | 5 197 | 5 674 |
| 757 | 23 194 | 30 298 H | 22 605 |
| 768 | 25 646 | 33 121 H | 27 623 |
| 285 | 22 392 H | clean | 3 496 |
| 6 | 6 440 H | 15 358 H | 28 945 H (held on all) |
## 5. Final lane (m23's lane re-pointed, Gradle alone, `-Xshare:off`, no dev client)
- Cold newly clean (12): 178, 272, 285, 316, 397, 567, 628, 673, 694, 866, 919, 994. Newly held: 183 F, 475 F, 729 B. Class changes: 6, 555, 957 from B to E5; 651 from F to B; also 72, 683, 803, 833.
- Alone, base against the `ca21bab8` tree: 475 clean on both at 22 416 iterations each, so wall time only. 183 is clean on both, at 227 381 against 259 604, so wall time only.
- 729 is clean on base (5 887 iterations) and held B on the branch (15 021). It was already held on `a6a305a1` (19 193) and does not involve a flashing junction. A CO2-rich gas at 257 K and 3.16 MPa mixes into LNG lines; pass 0 stalls at residual 0.17 at tick 1255. Not traced (lean rule: the item's own worlds first).
- Cold counters: Newton 3 044 782 against 2 772 420 (+9.8 %), step attempts 251 919 against 235 902, seed time +1.4 %. Phase seeds 17 515 against 33 247. Refusals 70 879 against 20 938: since `a6a305a1` a vanishing junction closure counts as a refusal, and each is a cheap Newton.
- Bundled: Newton 822 947 against 811 025 (+1.5 %; 579 changes class E10 → E5). Defaults: 74 392 against 74 403.
## 6. Proposition for the owner
- (a) Recommended: trace 729 next on this branch (the `a6a305a1` split where a CO2 gas meets LNG). Re-run only the affected parts, then merge-prep the branch: CHANGELOG version, code guide, tools sort.
- (b) Merge-prep now, with 729 recorded as a known worse world against twelve cold worlds cleared, among them 285.
- Cost: +9.8 % cold Newton iterations at +1.4 % seed time. That is acceptable at game level, but it is the owner's call.
