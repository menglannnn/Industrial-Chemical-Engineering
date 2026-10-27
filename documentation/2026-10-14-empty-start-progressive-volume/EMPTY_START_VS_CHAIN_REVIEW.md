# The empty start against the old chain: an evaluation of its potential (2026-10-14, restated the same day on the second pass)

Status: for the owner (D56, "evaluate the potential of the new method compared with the old chain"). Data: the chain's campaign `ca44` (951 random / 962 world defaults of 1000, charged tanks), the empty start's first pass `es26` (882 / 943, the ambient seed) and its second pass `es40` (933 / 970, the seed from the connected suppliers, D55). Code: `claude/empty-start` at `bbdea606` (A310-A333); decisions D52-D56 in the phase-ports batch's `DECISION_LOG.md`. The decomposition below reads the campaign reports seed by seed (first hold status per seed).

## 1. What each method claims, in physics

| | The chain (D37 to A133, `NetworkReduction.junctionStarts`, deleted) | The empty start (D54, D55) |
|---|---|---|
| A placed tank | holds 1 atm nitrogen, a charge the game put there | holds nothing: a slack bag at the ambient until full, then as stiff as its contents (A310) |
| A placed pipe | is minted full of the fluid its nearest or strongest supplier would send, at the balance pressure, with pockets, ties, gas columns, device cases and fallbacks (twelve rules, frozen by D48) | holds nothing; its buffer (D25, virtual since D46) starts from a guess, the stream its connected suppliers would deliver at its balance pressure (A324, A325), washed out in 0.05 s |
| The first step | starts beside its solution, because the answer was placed as material | is a filling transient from nothing: fronts reach dry ends, bags fill, a bag can empty (A313, A319) |
| In real life | neither the charge nor the minted fluid exists | a tank that holds nothing fills completely; a pipe carries what arrives |

The chain was a hand-written steady-state solver inside the reduction stage (the owner's words, D48). Its minted fluid was material: 11 g of air per junction, or water that never flowed there. The empty start's guess has no mass. That is the whole difference, and it cuts both ways: the chain's holds were Newtons fighting a fiction; the empty start's holds are the price of solving a real transient.

## 2. The chain's holds and the empty start's, decomposed (seeds 1-1000; lists in `REMAINING_HOLDS.md`)

| holds | random, `es26` | random, `es40` | world defaults, `es26` | world defaults, `es40` |
|---|---|---|---|---|
| chain holds (`ca44` 49 / 38) that run clean started empty | 11 | 23 | 24 | 29 |
| chain holds that hold under both (the common floor) | 38 | 26 | 14 | 9 |
| new holds under the empty start | 80 | 41 | 43 | 21 |
| holds, and clean of 1000 (chain 951 / 962) | 118, 882 | 67, 933 | 57, 943 | 30, 970 |

- **The dividend is the chain's own fiction.** Of the 29 world-default worlds that run clean started empty, 11 are the chain's first-solve Newton holds (its E6-S and E6-B groups, every named E6-S world among them: 214, 364, random 594, 660), 4 are water cooled to 273.16 K by a charged tank's nitrogen, 6 singular, the rest budget holds. Of the 23 random, 8 are crude or methane cooled below 293.15 K by evaporation into the charge. Started empty these worlds have no gas to fight. The chain could never pay this dividend: its material is the nuisance D52 names. The seed rule also shrank the floor (38 to 26, 14 to 9): part of what held under both was the mint.
- **The floor is mostly the crude data (D44):** random 26 with 6 typed waits at a data floor and D44's saved two-phase worlds; world defaults 9.
- **The new holds on `es40`.** Random 41: 11 singular Jacobians, 7 line-search stalls, 7 Newton limits, 5 active-set cycles, 5 typed waits at a data floor (random 699 traced: a 273 K nitrogen junction that methane reaches, D44), 6 other; the mint's nitrogen signature (an evaporative-cooling floor named in the hold) fell from 24 to 7. World defaults 21: 13 are the E5 family (4 to 12), of which 3 stall just above the gate at a 1 atm liquid tie (228 traced: water junctions seeded at their balance between 1 atm generators and voids, residual 5.0e-8; 107, 354) and 10 stall at pass 0 at residuals that recur across worlds (4.93 in three, 0.5 in two; smallest 771, 15 blocks): one row at one state, untraced (D50: the two smallest were 699 and 228); 4 water typed waits; 2 cycles; 2 singular.

## 3. The potential, measured

| | random | world defaults |
|---|---|---|
| clean of 1000: the chain `ca44` / the empty start `es40` | 951 / 933 | 962 / 970 |
| the ceiling if every new hold is a defect (the common floor only) | 974 | 991 |
| first-interval family, chain / empty start | 28 / 49 | 21 / 20 |
| Newton iterations, chain / empty start | 1.89 M / 1.74 M | 9.11 M / 0.50 M |

- World defaults: above the chain by 8, at a fifth of the Newton solves and an eighteenth of the iterations: there is no charged gas to integrate. Random: below by 18; 16 of its 67 holds are data floors (C 11, E10 5) and the rest are first-interval singular Jacobians and stalls on supplied fluids, not traced beyond the two smallest.
- What remains has a physical event behind it (a tie at exactly the ambient, a bag emptying, a front at a dry end), which is where structural work belongs (D48); the chain's residue was frozen because every fix was another rule.
- Costs, one-time: 26 of 33 junction lines, MIXED_GAS_COST 264 recorded, 335 under the cap mode, 362 now; printed lines 6086 to 6184; the outlets and compressor GameTest prints; 4 suite tests and 10 GameTests restated for placed-empty tanks (A329); the single re-baseline at the close. Kept: the emptying form (A313), the one piece of mode-like machinery that survived. Changed on the way: the seed stands at its balance pressure, not the ambient (A325); a stockless island is at rest whatever its devices are set to, replacing D19's junction-and-void tree rule (A327, `junctionForest` deleted). Code: 16 files in `src/main`, 624 lines added, 665 deleted.
- The product: a tank fills completely (996 kg of water on the E5 world against 220 kg over a cushion), no air pockets, the view shows the fill; a compressor into an empty tank fills it slack, then taut to the ratio limit at the discharge temperature (A327).

## 4. Proposition

Take `claude/empty-start` into the batch branch: it is the product the owner asked for (D52), its physics is right, world defaults stand above the chain at a fraction of the cost, and the chain's remaining holds were frozen with no path forward (D48). The chain retires with it (D37, D39, D40, A112, A119, A126-A133 deleted). On the new baseline the next structural items are the E5 pass-0 stalls (771) and the 1 atm liquid tie (228: in real life a level 1 atm generator against 1 atm voids moves nothing, D42's rest gauge from the start), then the E7 cycles (116); the data floors go to the thermodynamics work (D44). The single re-baseline is made on the final code at the close, not before.
