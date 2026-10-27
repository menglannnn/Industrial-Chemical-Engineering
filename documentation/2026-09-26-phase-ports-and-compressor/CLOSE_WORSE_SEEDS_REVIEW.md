# The close's two worse worlds: random 71 and 707 (2026-10-14)

Branch `claude/close-worse` from `d15661b8`, thread t32 (the close of D63). Logs `cw01`-`cw29` in `tools/phase-ports-probes/hold-classes/logs/`; scripts and patches in the README's t32 bullet. Code `92f419c9` (W1), `efc3682f` (W2). Rows W1-W5 for the orchestrator to number.

**W3 Finding: neither hold is A423's.** Both seeds hold alone on `d15661b8` the same way with A423 switched off, and run clean alone on `cb26806e` (`fx35`'s code). Between the two, the solver changed only by C4 (the `DeterministicMath` route) and A423, so C4's roundoff moved two knife-edge worlds onto holds that were already in the code. Answer to the brief's (a)/(b)/(c): (c), exposed by C4, not by A423. For 707, A423 changed neither which trial is taken nor when: its counters are identical either way (2670 solves, 3082 iterations).

| Alone (`alone.sh`, 1 worker) | 71 | 707 |
|---|---|---|
| `d15661b8` (`cw01`) | E8, tick 1969, gate 2.79e-8, 520 refusals | E10, tick 1071, "No finite Jacobian trial" |
| `d15661b8`, A423 off (`cw02`) | same, same counters | same, same counters |
| `cb26806e` (`cw04`) | clean, 658 Newton solves | clean, 771 Newton solves |
| W1 + W2 (`cw24`) | clean, 652 Newton solves | clean, 814 Newton solves |

## Random 707 (E10, b:trace-underflow): rule W1

- **Physics.** A 327 K crude generator at 371 kPa and a 311 K water generator at 363 kPa fill three empty 1 m3 tanks; the water line joins at tick 165. The far tank is still empty in real life: nothing has reached it but roundoff. Steady state: all three tanks full near 371 kPa.
- **Minimized:** 12 blocks of 19 (`cw06`), held at tick 167 with A423 on and off.
- **Trace** (`cw08`). The hold is not in the step's Newton but in the booking. A tank holding 1e-42 mol of crude (3.4e-45 m3 in 1 m3) and 1e-84 mol of water vapour, reached by a roundoff flow and laid out dry, is restated by `InventoryEquilibrium.solve`: a flash and then a closure Newton. Its amount scale is floored at 1e-12 mol, and with a gas share above 1 % its amount steps are floored at 1e-4 of that. So the water-vapour column is differenced with 1e-22 mol, 1e19 times the vessel's contents: the minus trial is negative water, and the plus trial leaves the water-vapour range. No trial is finite at any step size.
- **W1 Rule** (`92f419c9`, owner decision D59 (2): a residue below the bag's resolution is nothing except in the ledger). A vessel that holds nothing after its booking is stated at its flash, at its old temperature and pressure, with no closure. Its residue stays booked exactly in the inventory. This is the booking's half of A415, whose layout half already left such a vessel dry. Gate `HoldingNothingBookingTest` (707 minimized): held at tick 167 without (`cw14`), kept up with (`cw15`, `cw16`). References unchanged.

## Random 71 (E8): rule W2

- **Physics.** A 317 K water generator at 334 kPa fills an empty 1 m3 tank 3 m below it, beside a 146 kPa generator its lines may not feed. Full, the tank stands at 334.3 + 29.1 = 363.4 kPa and nothing flows: the generator's one-way end holds the tank.
- **Minimized:** 9 blocks of 19, all placed at tick 207 (`cw05`), held at tick 734 with A423 on and off.
- **Trace** (`cw07`, `cw22`). Once the tank is full, the step's Newton accepts its start at iteration 0, with the tank 1e-11 of its volume over the capacity (within the small headspace's 1e-10). The booking states the tank at its contents, at the capacity, which is 0.01 Pa above what the generator holds: water's stiffness (1 GPa in the model) times 1e-11. The line into the tank reads that as 2.8e-8 against the 1e-8 gate. The D13 polish, started from the reconstruction, converges (1.2e-14) with that 0.01 Pa pushing water back into the generator, a forbidden direction, and returns null. The same refusal repeats at every step size: 513 polishes, 513 refusals.
- **Mechanism.** The stage that owns a forbidden direction is the pass loop's boundary closure. At a converged point it closes the boundary and solves again. At a polished point it refused the whole step instead.
- **W2 Rule** (`efc3682f`). The pass loop takes the closure a polished point shows, as at a converged point (one `closeBoundary` serves both), and solves the pass again from the reconstruction. The polish's own forbidden-direction refusal goes. Gate `PolishedClosureTest` (71 minimized): held at tick 734 without (`cw26`), kept up with (`cw27`, `cw29`). It also clears random 26, which held the same way on `cl16` (gate 3.2e-8, 520 refusals; listed under D44 before). References unchanged: only steps the gate refused take the new path.
- **W4 Measured, not taken: form (a)**, the wall row divided by the contents' stiffness at the seed (`cw18`-`cw21`). It clears 71 and 26, but fails `TransientQualificationTest`'s liquid-full reference. Traced, that is the same amplification through the water-balance rows (1.9e-11 of the amounts gives 0.02 Pa, read as 2.6e-7), so scaling one row of three is a patch. It also changes an exact substep count, moves 25 of 33 junction lines and every printed line of the suites, and raises world-default Newton solves by 14 %.

## Quick checks (seeds 1-200, 4 workers; `cl16`'s seeds 1-200: random 193, world defaults 200)

| After | Random | Defaults | Worse | Cleared | G1 | References against `cl14` |
|---|---|---|---|---|---|---|
| W1 (`cw17`) | 193 | 200 | none | - (707 is above 200) | 565 / 0 | identical (0 of 33 junction lines, MIXED_GAS_COST 339, chain-100 2.695e-10, 0 printed lines) |
| W2 form (a) (`cw20`) | 195 | 200 | none | 71, 26 | 565 / 2 | 25 of 33 junction lines, 3330 printed lines |
| W1 + W2 (`cw28`, on the commit) | **195** | **200** | none | 71, 26 | 566 / 0 | identical |

## Counters

| Seeds 1-200 | Newton solves | Newton iterations | Jacobian builds | Gate refusals |
|---|---|---|---|---|
| random, W1 (`cw17`) | 103 490 | 160 379 | 74 335 | 16 699 |
| random, W1 + W2 (`cw28`) | 42 581 | 114 949 | 43 645 | 1 138 |
| world defaults, both | 9 184 | 23 166 | 6 467 | 0 |

**W5 Finding:** `cl16`'s rise over `fx35` in random Newton solves (+33.6 k) is almost all the worse worlds: 71's hold alone costs +30.3 k solves and +7.8 k gate refusals (`cw01` against `cw04`).

## Left for the owner

The stiff-liquid amplification itself remains. Any row a taut liquid vessel closes to tolerance ε shows in its pressure as the stiffness times ε, which is 1e4 times larger than in a gas. The D13 polish and now W2 resolve it where it appeared. Stating every row of a taut liquid vessel over its stiffness is the structural form, but it is a design item and is not built.
