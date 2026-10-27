# D61 junction-crystal labels and the final lane (Opus, 2026-10-20), `416f8601` + `2def39dc` on `claude/stoic-bell-ae6019`, not merged or pushed
Owner request 2026-10-20: "Look at the D61 label gap first (prototype A, a quick check, explain 729's FULL ending), then the final lane. No merge or push." Follows `COLD729_REVIEW.md` section 5. Logs: `tools/new-units/logs/d61-labels-final-lane-2026-10-20.tar.gz`. Code guide: no change (not merged; at the merge the network chapter records that `JunctionCrystals` carries the island's populations).

## Verdict: GREEN against m23, one world returned against `ca21bab8`
- Gates:
  - Full suite: 1 570/1 572 on `416f8601`. The two failures were A's own and are fixed in `2def39dc` (section 3); the targeted classes give 86/86 on `2def39dc`. No printed reference line moved against `ca21bab8`.
  - Chain-100: exact. GameTests: 31/31.
- Campaign against m23 (cold on `416f8601`, bundled on `2def39dc`; same arithmetic below 64 populations, and no report names the limit):
  - cold 973 clean against 962, with 11 newly clean and none newly held;
  - bundled 990 = 990; defaults 1 000;
  - no new C/F holds, so no alone replays are owed.
- Against `ca21bab8`: 183, 475 and 729 are newly clean; 183 and 475 are wall time only (round 5). 994 is held again, exactly as on m23 (E6 at tick 5144); section 5.

## 1. Physics first (cold 729, before the rest)
- The gas-side tank stands at 3.163 MPa, 3 kPa above the gas source, and sends its slurry back.
- The slurry passes the tee at 207 K, below CO2's melting point near 217 K, so the crystal stays.
- It then enters the gas junction at 244 to 257 K: 0.035 to 0.1 kg/s at 1.9 % crystal, so 1 to 2 g/s of crystal must melt there (D61), about 1 kW.
## 2. What the code did instead (`ff71d370`)
- The rows split a junction's arrivals with `JunctionCrystals.split` over the donor's population list. A Newton state names its solids by its seed's list (`PhaseLayout`: `solidBasis = seed.solids()`). A state whose seed held no solid therefore carries moments but no population, and its crystal arrived inert.
- The reconstruction labels the same moments with the island's populations and melts them.
- The equation gate refused the junction's rows until the step was refined: 12 times on 729 (9 on 0.10.0, 47 on `ca21bab8`).
## 3. Fix (owning stage: `JunctionCrystals`, the one statement both stages read)
- `416f8601`: `split` reads an unlabelled stream as the island's populations, its holders' and its injections'. That is the basis `ConservativeTransport` labels by (lines 220 to 222), so both stages book the same melt.
- `2def39dc`: the basis is a list merged by key, built only where a crystal melts in a junction. In `416f8601` it was a `SolidInventory`, capped at 64 populations and built before the crystal check, so `SolidTransportAcceptanceTest`'s two population-limit worlds refused every step. The arithmetic is unchanged below 64 populations.
- Residual: an island holding a melting crystal beside an inert solid weights them by its stock, where the reconstruction labels by reach. None was seen.
## 4. 729's ending: FULL instead of STEADY
- 729 is clean in 8 930 iterations with 0 refusals, against 5 984 on `ff71d370`.
- The crystal now melts in the gas junction, so the tee ends crystal-free and only the tank pipe settles a bed (one vessel with crystal, against two). The tee's other two pipes stay open with round-off flows of 1e-13 to 5e-13 kg/s.
- The interval is therefore not the exact identity, and `IslandCertificate.issue`'s drift branch refuses any island with a filter (112 times). The island is at rest but solved every interval.
- On `ff71d370` the inert crystal sealed the tee's three pipes, so the flows were exactly zero and it was certified "no flow". That ending rested on the booking mismatch.
- Latent item, proposed: 1e-13 kg/s is about 3 mg a year. Options:
  - (i) a round-off bound in the rest test;
  - (ii) a drift certificate for a filter island when no solids move (`solidsInTransport` already guards that).
  Either changes the rest rule.
## 5. Cold 994 (one worker, export-only probe `r5/fallback-probe.pl`)
- `ff71d370`: clean, FULL, 8.8 kg crystal in 3 vessels, 122 771 iterations. With A: held from tick 5144, as on m23.
- A's fallback fires 5 501 times before the hold, mostly between junctions n7, n8 and n9, but it melts at most 6e-11 kg/s (1e-10 kg per kg of flow, about 1e-5 W). That is physically nothing; it only moves the path.
- The hold itself: those three junctions sit at 215.6 K and 982 kPa with a vanishing liquid (volume fraction about 1e-313). Their enthalpy rows go singular, and the next passes run them to 380 to 390 K. This is the vanishing-phase family, held on m23 too; not traced further.
## 6. Quick check (200 seeds) and counters
- Against `ff71d370`:
  - cold: 193 against 192 (183 F); Newton −1.3 %;
  - bundled: one worker 196 = 196, only seed 157 moves (+37); the 4-worker +4.7 % moves with seeds that a wall-time budget cuts;
  - defaults: identical.
- Cold campaign Newton iterations: 2 975 100, against 3 044 782 on `ca21bab8` and 2 772 420 on m23.
- Equation-gate refusals: 6 392, against 6 399 on `ca21bab8`, 4 649 on `48fb5678` and 100 on m23. The rise predates A; not traced.
## 7. Proposition for the owner
- (a) Recommended: keep A. It is the consistent booking (structure over patches), and the lane is green against m23. Trace 994's vanishing-liquid hold next (held on m23 too); it may also explain the branch's gate-refusal rise.
- (b) Revert `416f8601` + `2def39dc` to `ff71d370`. 994 is clean again and 729 is STEADY, but the label gap stays: its refusals cost refinements and hold nothing.
- Either way, the certificate item (section 4) is a separate owner decision.
