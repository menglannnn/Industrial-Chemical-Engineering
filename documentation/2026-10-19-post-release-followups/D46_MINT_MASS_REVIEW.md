# D46: what an unbooked junction buffer creates (t52, Opus, 2026-10-19, analysis only, at `5b49f940`)

## Physics first
- **A placed fitting** (one block, 1 m of pipe) holds air, none of the process fluid: 0.08 / 0.31 / 1.96 / 7.85 L at 10 / 20 / 50 / 100 mm.
- **At steady flow** it holds that volume of water in kg, or 0.4 / 1.5 / 9.6 / 38 g of N2 at 415 kPa.
- **The fill comes from the suppliers,** in 1.6 s for 100 mm water at 5 kg/s and in milliseconds for gas. Gas that later displaces the liquid pushes it on to the next vessel, and the supplier booked that liquid.
- **The code's buffer** is `m_J = 0.05 s x 100 m/s x rho A`, that is 5 m of pipe of the seed fluid: 0.39 / 1.57 / 9.8 / 39.3 kg of water. The 39.1 kg of seed 107 is the 100 mm case.

## What the code does
- **Total mass is exact.** The mass pin (`ConservativeTransport.pinnedFlows`) keeps every buffer at m_J, so the vessels gain exactly what the boundaries book. Measured: `|net| <= 3e-11 kg` per seed once crystals in buffers are counted.
- **Composition and its enthalpy are not booked.** Each buffer life puts `m_J (w_install - w_end)` into the vessels. A buffer whose composition never changes creates nothing: seed 107's 39 kg water buffer on a water line creates 0 kg.
- **t44's "mint mass"** (sum of m_J) is only an upper bound.
- **The ledger.** `checkConservation` closes with the junction term as its own term, so nothing shows as created. The vessels drift by minus the sum of that term, which the probe (`tools/post-release-followups/d46/`, 200-seed quick check, 4 workers) measures.

| at `5b49f940` | cold | bundled | defaults |
|---|---|---|---|
| t44's sum of m_J (fresh junction ids) | 237 kg | 757 kg | 2 644 kg |
| composition created, sum of \|kg\| over components | 3.48 kg | 3.66 kg | 0.000 kg |
| per kg booked at the boundaries; seeds above 0.1 kg / 1 kg | 1.3e-5; 8 / 0 | 1.8e-5; 3 / 1 | 0; 0 / 0 |

- **Cold:** 0.96 kg of the 3.48 kg are crystals held in buffers (one component, fluid out, solid kept).
- **Energy:** 2.6 / 3.8 / 0 MJ, which follows the created components at their own enthalpy. Seed 188's -3.2 MJ is 1.29 kg x 2.5 MJ/kg, liquid water against N2. It is not a separate temperature error.
- **Worst world, bundled seed 188** (21 blocks, clean, 0.2 s). At 1740 junction 9 (20 mm) is minted as 1.56 kg of liquid water from the water generator.
  - At 1782 an N2 generator at 332 kPa takes the line over and washes the buffer for the rest of the run.
  - 1.29 kg of unbooked water reach the 40.7 kg tank (3 %), and 1.29 kg of supplied N2 stay in the buffer. A real fitting passes on 0.31 kg of booked water within seconds.
- **Seed 123** (t45's 38.9 kg) came from `33455d4c`'s placeholder start; at `5b49f940` it creates 15 g.

## Forms (quick check where cheap; counters: cold / bundled Newton iterations and Jacobian builds against the base)
- **(a) A junction becomes a fixed-volume node that fills from its suppliers** (EMPTY_START_PLAN's "one node type"): exact, and it deletes m_J, the mass pin, the mint balance and the carry.
  - Risk: a taut liquid-full 2 L bag (the 0.6.0 stiffness); every reference moves. Not measured.
- **(b) Mint what a real fitting holds** (ambient gas, gas-sized). With the mass pin this is t51's bundled 145 (0.5 ms lag, E9).
  - Its proxy, round 0 from the placeholder, creates 4.19 / 9.45 / 0.045 kg (bundled 4: 4.15 kg, t45's wrong branch), at +49 % / -19 % iterations.
  - Rejected.
- **(c) Book the junction term as a source with a status notice.** The ledger already carries the term with D46's bound; a notice at 1e-5 of turnover is noise and adds a special case. Rejected.
- **(d) A buffer of one fitting** (`HOLDUP_TAU` 0.05 to 0.01 s): created 0.66 / 0.83 / 0 kg (seed 188: 0.31 kg); holds 189 / 196 / 200 against 189 / 194 / 200.
  - Iterations +43 % / -31 %, builds +74 % / -43 %: it misses the cold cost gate, and the 33 junction lines move.
- **(e) Keep D46 and restate the watch item:** total mass exact, composition at about 1e-5 of turnover, worst 1.3 kg. No cost and no reference moves.

## With t51 (one sizing for a minted and a carried junction)
- An install is not a commit, so sizing a carried buffer on its state creates nothing by itself; it only changes the bound `m_J |w_install - w_end|` (in 145 water flows on: nothing is created). t51's option, every buffer sized on its current state at each interval, would shrink seed 188's buffer as the N2 arrives (unmeasured).

## Proposition
- **(e) now, with seed 188 as the D46 gate world:** a test reads the created water from the junction term (1.29 kg at `5b49f940`), and no later form may raise it. **(d)** is revisited with t51's per-interval sizing; **(a)** stays the structural end state for a separate batch.
- The code guide's README item 9 ("pushes mass ... into a vessel") is corrected at the next merge. Code guide: no change in this task.

## Owner's choices
1. Close D46's watch item with the corrected statement, form (e) (recommended), or keep it open.
2. Seed 188's world as a D46 gate: yes (recommended) or no.
3. Form (d), a buffer of one fitting: not now (cold cost +43 %), now, or with t51's per-interval sizing.
4. Form (a), a junction as a fixed-volume node: a later research batch, or never.
