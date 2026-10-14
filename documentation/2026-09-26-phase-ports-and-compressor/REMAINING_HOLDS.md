# Remaining holds: one card per class (2026-09-28)

## Remaining issues sorted for the owner (2026-10-14, on `es40`, branch `claude/phase-ports-compressor`; restated the same day under D58)

Campaign `es40` (the empty start with the junction's start from its suppliers, D54, D55): 933 random / 970 world defaults of 1000; holds 67 / 30, of them 49 / 20 on the first interval after a placement. Every hold's first status was read from the campaign reports; a group's mechanism is a hypothesis from its residual signature, its world's contents and the code's rules until a trace names the row. Sorted under D58: **category A, nothing moves**, decided before anything is solved, at the island level by `NetworkReduction.atRest` (D19, D22; A327 for an island holding no stock) and at the connection level by the start-point closures (`closeDeadHeads`, `closeIllegalStarts` with the dry-end rule A319, D6's refusal in `InletPhase.decide`, F8's `relieveSealedPockets`); **category B, the solver's own**; the thermodynamics-model cases are removed (D44).

### A. Nothing moves: decided before the solve

**A1. A pump on a dry suction** (world defaults 922, 6, 333 with the exact signature; 750, 600, 654 through the valve; 485, 322, 978 likely; random 707). In real life a pump with nothing at its suction runs dry: no flow, no head. In the code `InletPhase.decide` skips a supply vessel that holds nothing (`if(node.empty())continue;`), so the pump is never refused and keeps its target or head-limit row while the dry end pins its flow at zero: the line search stalls at pass 0 at residual 500000 / 101325 = 4.934616 (exact in 922 and 333). Under the chain the placed tank held nitrogen and the same rule refused the pump (`INLET_WRONG_PHASE`). A second consequence: the unrefused pump at its target feeds a pocket whose only exit is a closed relief valve, so F8's relief opens that valve to regulating, and its row `P_up = P_set` stands with `P_up` near 1 atm: residual (P_set - P_up) / P_set = 0.487 to 0.500 (750, 600, 654). Fix, in the stage that owns the refusal: an empty supply is refused with its own reason ("pump inlet dry, from node n"), with the hysteresis the phase refusal has (the port thresholds A1 to A7), decided per slice as D6 states; a refused mover is closed for the solve and its pocket keeps its pressure (F8). Nothing else changes. Measure: 922 alone with and without, then the quick check and the campaign. The water-cooled group (world defaults 285, 696, 896, 889, 563: a trial at a pump's suction leaves the data below 273.16 K) is traced after it (285): if the suction is dry, A1 covers it; if the suction is a fed junction pulled below the vapour pressure, the pump cavitates in real life and the proposition is the mirror of the head limit, a suction limit `P_suction = P_vapour(T)` as a mode, not a curve (the pump rule: no mechanistic pump).
*Status (t25, `rc22`): resolved by A334 for 922, 6, 333, 485, 978 and 285 (clean); 750, 600, 654 hold no valve (600, 654 resolved by A336; 750 moved to B, A342); 322 is a passive residue tank, clean in the campaign (A343); random 707 moved to B (crude filling); 696, 896, 889, 563 are cavitation at a fed suction, left for the owner's suction-limit decision (A335).*
*Status (t28, `cc54`, branch `claude/chord-closures-cavitation`): resolved. 696, 896, 889, 563 by the suction-limit mode (A390; gate 696); 322's residue by the bag's resolution (A394; gate 322, which had turned E5 on the merged head). All clean in the campaign.*

**A2. A relief valve below its setpoint** (world defaults 244, the one valve world without a pump; E6, residual 0.498). In real life a valve set at 200 kPa in a world that never exceeds 1 atm stays closed. In the code it does start closed (the start mode reads `P_up > P_set + 0.01`), so 244's stalled row is another one: trace 244 (28 blocks, minimized first). Kept in A because its physics is a closed valve and voids: nothing moves.
*Status (t25): moved to B. The held island has no valve; a generator drains into the lower of two voids, and the stalled row is the upper void's connection driven the forbidden way on a first solve (A342; 750 likewise). Still E6 on `rc22`.*
*Status (t28): clean since `tw15` (A371); the trace still showed the forbidden flow on every step (the void's air column), resolved by A392 in the column stage (gate 244 as placed: the upper void now receives the water above it, 8.44 kg/s). The brief's first-solve closure measured worse, not taken (A398).*

**A3. A tie at the ambient** (world defaults 228 at 5.0e-8, 107 at 2.7e-8, 354 at 5.4e-9, just above the gate; 524, 794, 683 at small residuals). In real life a level 1 atm generator against 1 atm voids through water junctions moves nothing; the tie is at rest whatever else the island does. In the code `atRest` tests the whole island and returns false for any island holding a mover, so the tie's junctions are solved together with the pump's branch (228: a pump and a tank; 354: a pump, a valve, a filter), and the Newton stalls on the stiff liquid tie (A331). 107 has no pump (a 1 atm generator, a filter, a void): its drive is the generator's elevation, to trace. Fix: the rest test applied per part after the closures: a closed or refused mover splits its island, each part is tested with D42's gauge, and a part at rest is not solved (its interval is the identity, `restInterval`); A327's stockless rule becomes one case of it. Trace 228 first for the tie's row scale (the thread's advice), then 107.
*Status (t25): 354, 794, 683 resolved (354 by A336: nothing moves and the rest test now reads the gauge where stock stands); 228, 107, 524 moved to B: they flow, and their Newton stops at its iteration limit converging on chord steps (A337, A338); still E6 on `rc22` with 547.*
*Status (t28): 107, 524, 547 cleared by t27's A371 (D61); 228 (E2 on `tw15`) resolved by A393 (a pump's discharge junction with no line on its law). All clean in `cc54`.*

**A4. A pocket behind closed devices** (singular at every step size: E2 random 529, 342, 358, 683, 855, 891, 660, 456, 123, 957; E5 random 681; world defaults 619, 358, 600). In real life a sealed pocket keeps its pressure; nothing moves in it. In the code F8's `pockets` and `relieveSealedPockets` handle a mover at its target into a pocket; a pocket with no open mover and no anchor (behind a liquid-only pump refused on gas, a closed valve, a filter's dead end) is still solved, its pressure level is free, and the LU is singular. Fix, at the start point with the other closures: an anchorless pocket with no open mover keeps its state and leaves the solve (`Equations.retainedPressures`, which F8 already uses). Trace 619 (15 blocks: a 1 atm water generator, a pump, a filter, two voids) and 529 (15 blocks: methane at 496 kPa, two pumps, two tanks, a void) to confirm the pocket before the rule.
*Status (t25): world defaults 619 and 600 resolved by A336 (nothing moves; no pocket); world defaults 358 resolved by A339 (a pump dead-headed into its generator); random 342 and 855 clean on `rc22`; random 529, 358, 683, 891, 660, 456, 123, 957 moved to B: flowing gas worlds whose junctions are surrounded by capped lines (A341); 681 is a flowing compressor world (E5, B).*
*Status (t28, `cc54`): the capped junctions resolved by A393 (gate 529): random 529, 358, 683, 891, 660, 456, 123 clean, and 498 (added under D61); 957 moved to E10 (the Jacobian probe leaves the data, D44); 886 left (two caps of one gas in series at the cap's edge, E6).*
*Status (t29, `fx35`, branch `claude/last-six`): 886 resolved by A414 (not a cap matter: the pass reached the physical active set and its chord steps overshot on stale pressure slopes; gate 886). 957 is C (methane below the data, D44).*

**A5. An island holding no stock** is at rest whatever its devices are set to (A327, done on `claude/empty-start`): the pattern the rules above follow.
*Status (t25): unchanged; A336 extends the same rest test with closed movers and the gauge where stock stands.*

### B. The solver's own

**B1. Active-set cycles** (E7 random 337, 257, 922, 200, 678, 824, 116; world defaults 952, 573, 826). In real life a mode settles once in a step. In the code the pass loop re-decides modes on each converged point (`nextMode`: pump target, head limit, closed, with the shutoff band and `atShutoff` decided once; valve regulating, open, closed; the cap mode of t19 with hysteresis; the phase layout of t22; the emptying form A313); a repeated active-set key is a cycle and holds after the budget. The cycling pair is not known. Candidates: in 337 (methane at 479 kPa through a filter into an empty tank and two voids) the cap on the filter's run against the tank's slack-to-taut fill; in 952 (water at 1 atm, a pump, a void, a filter) the pump's target against the filter's cap, which A1 may close. Fix shape: the cycling pair gets a decide-once rule like `atShutoff`, in the stage that owns the mode. Trace 337 and 952 with the pass-loop trace patch, after A1.
**t26 (2026-10-14, `cf29`, A351-A356): resolved for 257 (clean), 678 (moved to C, the crude floor) and 200 (moved to E2); left 337, 824 (the mint's start, t25's area), 922, 116 / 573, 826, 952 (moved to A1, a pump drawing its suction to the floor).** Three mechanisms, none in `nextMode`: the emptying form (fixed: its pass starts from the step's flows; emptying decided once per active set), the cap corrections of failed iterates (the entry made sign-consistent with the release), and a pump drawing its suction to 100 Pa.
*Status (t28, `cc54`): resolved. The pump group by the suction limit (A390: random 922, 116; world defaults 573, 952, 101) and the own-end shutoff (A391: world defaults 826); 337 clean since `tw15`, 824 clean on this branch (the mint's start, item 7, needed no rule). E7 left: random 600, 698 (crude and water worlds).*

**B2. The turbulent walk on gas lines** (E6-S random 498, 886, 728, 151, 629, 397, 600, 883; the cap residue 248, A276; the tight-tolerance headspace 430, A315). In real life a gas line between two sources at different pressures carries one flow, and any start reaches it. In the code the step's Newton starts from the static estimate (the law inverted on the end pressures) and walks the junction pressures; on gas lines the walk crosses near-zero flows, where the turbulent law's slope vanishes, and plateaus (A298 (c); t21's laminar flow form measured worse). 498 is the clean case: two nitrogen generators at 480 and 218 kPa and pipes, nothing else, held on its first interval at 9.5e-7. Options: (i) keep as the known family; (ii) one thread: the flow row's Jacobian slope floored at its laminar value near zero flow, as a regularisation of the walk only (t21's blend applied to the derivative, not to the row), measured on 498 and the quick check; (iii) the line solved in flow variables where it carries no junction stock. Recommendation: (ii).
*Status (t27, `tw15`, branch `claude/turbulent-walk`, merged at `620edf33`, D61): resolved, by a different rule. The slope does not vanish (the law's laminar branch floors it; the Jacobian states it), and (ii) measured inert (A372). The walk stalls on chord steps that keep the slope of a flow twice as large (A370); the rule: a chord step reuses the Jacobian only while every line's slope is within a factor two of it (A371). Of the B2 list, 248 629 883 are clean (and 728 151 397 already were on `mh01`); 498 moves to E2 (capped junctions, at a later placement), 600 to E7, 886 stays E6 (capped junctions), 430 stays E6 (the small-headspace tolerance, A315). Campaign 951 / 994 against `mh01` 943 / 987; world defaults E6 6 -> 0 (the chord-budget worlds of S5 cleared).*
*Status (t29, `fx35`): 886 cleared by A414 (the chord test of A371 extended to the pressure slopes, one bound for both). 430 left: not the headspace's tolerance but a two-phase crude trace in a water junction whose fugacity rows are noise (A417); 431 a different case, the first grams of hot crude in an empty tank (A418). Proposition S2 of the review (with 803).*

**B3. The flush family, E9 (c)** (random 579, 720 since `ca44`; 322, 431 new; 121, 514, 94 nearby). In real life a trace below the flush fraction is gone before anything draws it. In the code D16 flushes components below 1e-24 of the node total when a step is accepted; a trace just above it is drawn by a port over the step by more than the node holds, and the reconstruction books a negative amount ("Negative transport reconstruction"). Fix shape: the reconstruction bounds every component's draw by its holding, as D11 bounds a phase's draw at m / (N dt), and books the excess as the flush (D16's booking), so it never goes negative. Trace 322 (19 blocks).
**t26 (2026-10-14, `cf29`, A357-A359): resolved for 322 and 720 (clean); 579 moved to F (a budget hold at tick 6231), 431 to E6; left 121, 94 (a sealed ring of junctions, moved to A4) and 514 (an inventory with no moles and nonzero energy, not traced).** The mechanism is a frozen phase stream (D11) drawing more of a component than its vessel holds and receives: none held in 322, 579, 431 (the seed's entry trace), 12 times the holding in 720 (above the flush fraction). Fix: where the explicit booking leaves a component negative, its streams are booked at the candidate's partition against the vessel's own end fraction (`2739caa3`), not a flush booking.
*Status (t28, `cc54`): 121 and 94 clean since `tw15` (the ring needs no rule, A396); 514 clean on this branch (A397). E9 left: random 446, 708 (a phase-port overdraw).*
*Status (t29, `fx35`): resolved. 708 was a roundoff circulation through an empty tank, booked from nothing (A410): cleared by A411 (a dry end's outflow within the tolerance is no flow) and A415 (holding nothing is laid out dry); gates 708 twice. 446 was never an overdraw: a crude-plus-water flash at the bubble point (A412), moved to D44 (E3).*

**B4. Budget worlds** (F random 941, 188, 26, 788, 419; world defaults 29). Slow transients in crude worlds with pumps and filters; they hold again alone (8 of 9, 4 of 5). Leave; measure again on the final code.

**Removed as thermodynamics-model matters (D44):** random C 206, 444, 101, 175, 80, 978, 241, 453, 889, 937, 994 (typed waits at the data floors), E10 699, 399, 333, 446, 4 (the Jacobian probe leaves the domain), E3 157 (water vapour appearing in hot crude), E8 521, 246 and E2 298, 372, E5 698, 274 (crude two-phase worlds). One note for the owner, a tooling choice and not a model issue: the random distribution spawns sources outside the data (nitrogen at 255 to 274 K, methane at 265 to 290 K); a campaign meant to measure the solver should draw its sources inside it.

**Order.** One thread for category A: A1 (the refusal rule) with 922 as its gate world, then A3 (rest per part) with 228, then A4 (pocket retention) with 619 and 529, 244 traced on the way; a quick check after each rule, one campaign and one gates run at the end. Then one thread for B1 and B3 (traces first), and B2 on the owner's call. The close items follow.

*D59 (2026-10-14): category A merged at `ecc4a19d` (t25, A334-A346). S2 (one meaning of empty) and S4 (the cavitation suction limit) go to the next B thread with the chord budget, the first-solve void closure and the capped gas junctions (S5), after t26; B2 runs as thread t27 (`claude/turbulent-walk`).*

*D60 (2026-10-14): B1 and B3 merged (t26, A350-A364). Baseline `mh01` (campaign) and `mh02` (suites, chain-100, junction lines) on the merged head next, then the B thread of D59 (5) with t26's stop points (337 and 824 from the mint, the ring 121 and 94, 514).*

*D61 (2026-10-14): B2 merged (t27, A370-A378, `620edf33`): the chord rule A371 clears the chord-budget group (107, 524, 547) and 244, 750 by the faster Newton; every converged-point reference moved within tolerance, re-recorded once at C1. t28 resumed with the rest of the B items, the column densities (A377) and 228 added.*

*D62 (2026-10-14): t28 merged at `e9820925` (A390-A403): world defaults 1000 / 1000, random 966 on `cc54`. A395 taken; the suction limit reads free water only (a bubble point for the thermodynamics work). Thread t29 takes the six solver holds (886, 803, 708, 446, 430, 431); then the close items C4, C1, C2, C3, C5 and the merge to `main` on the owner's go.*

*t29 (2026-10-14, `fx35` on `cb26806e`, branch `claude/last-six`, not merged): 969 / 1000 (`cc54` 966 / 1000), none worse. Resolved 708 (A411, A415) and 886 (A414); 446 moved to D44 (A412); left 803 and 430 (one design item, review S2: a junction's minor gas and traces), 431 (the first grams of a hot crude, thermodynamics work).*

*D63 (2026-10-14): t29 merged at `5b24744e` (A410-A422): random 969 / 1000 on `fx35`, world defaults 1000 / 1000. The solver's own holds are down to 803 and 430, one design item (a junction's minor gas and traces: absolute fraction rows, D41's omitted traces, A84's folding), recorded as the first solver item of the next batch; 431 to the first-grams family (D56), 446 and 957 to D44. The close now: C4, C1, C2, C3, C5, the merge to `main` on the owner's go.*

*The close (2026-10-14): C4 merged the determinism branch at `a8e49dae`, with A414's `exp` routed and the rejection reasons in insertion order (A427). Its gates were green (`cl01`), but the full suite failed five tests of the runtime package that G1 did not run (A426); all five are older than C4. Two pumped chains stalled at the kink of the wall law, fixed by A423 (a Jacobian sweep states the wall on its base point's branches; a chord step is refused once they change). Three tests, and a fourth found after A423, asserted a cooling or a refusal that D54 and D58 removed and were restated (A424, A425). Full suite 1258 / 0 (`cl15`). Campaign `cl16`: random 967 / 1000 (`fx35` 969), world defaults 1000 / 1000; cleared 957; worse 71 (E8), 707 (E10), 788 (F). Next: thread t32 traces 71 and 707 (owner decision D64), 788 is replayed alone (`cl17`), thread t31 cleans up (C2), then C1 on the final code, C3 and C5.*

*D64 (2026-10-14, taken by the orchestrator on t32's recommendation, confirmed by the owner 2026-10-14): `claude/close-worse` merged at `1cce9a2b` (A432-A436). Neither hold was A423's: C4's roundoff exposed two holds already in the code (A434). W1 (`92f419c9`, A432): a vessel that holds nothing after its booking is stated at its flash, with no closure (707). W2 (`efc3682f`, A433): a polished point that flows the way a boundary forbids closes that boundary, and the pass is solved again (71, and 26, filed under D44 before). Quick check `cw28`: random 195 / 200 (`cl16` 193), world defaults 200 / 200, none worse, references identical. Form (a) measured, not taken (A435). The stiff-liquid amplification is a design item for the next batch. C3 done by thread t33 (`05fce69c`, A437): five of six checks pass, G1-G4 are GUI proposals (A438). Next: the campaign `cl18` on the merged code, C1, the winsweep, C5.*

### The batch's close, not holds


| # | Item | When |
|---|---|---|
| C1 | The single re-baseline on the final code: 26 of 33 junction lines, MIXED_GAS_COST 264 recorded -> 362, printed suite lines 6086 -> 6184, the outlets and compressor GameTest prints (A332) | done 2026-10-14 (`cl19` on `1cce9a2b`, commit `0041d224`, A440): chain-100 re-captured, G1 583 / 0, G2 exact 0, G3 45 / 45, GameTests 33 / 33, the junction lines identical to `cl14`'s and copied to both references; then the winsweep `cl20`, five runtimes identical (A441) |
| C2 | Cleanup and detach (AGENTS.md): the probes under `tools/phase-ports-probes/` (`src-t24`, the instrumentation patches, the lane scripts), the run configurations and switches that served them | done 2026-10-14 (thread t31, `26059fb5` `82a408bb`, merged `280bf0e7`): nothing left in a tracked path to detach (`CLOSE_CLEANUP_REVIEW.md`); the gates run AGENTS.md asks for after a cleanup is the final gates run of C1 |
| C3 | In-game check through the bridge: the tank view's fill and "EMPTY: holds nothing", a placed tank filling from a generator, a compressor into an empty tank (A327) | done 2026-10-14 (thread t33, `05fce69c`, on `d2321885`; `CLOSE_IN_GAME_REVIEW.md`, A437): checks 1 and 3-6 pass, no HELD line, no exception; check 2 passed its slack stage, its taut and STEADY stage not observed (stopped at 53 % on the usage limit) and not re-run (D50: a 1000 L tank at 0.333 kg/s needs about 53 minutes to fill, and check 3 shows the same bag going slack then taut with the island STEADY, A327, on the same code path); W1 and W2 are exercised by none of the six checks, so the evidence stands for `1cce9a2b`; G1-G4 GUI proposals for the owner (A438) |
| W1 | A vessel that holds nothing after its booking is stated at its flash, with no closure (D59 (2); random 707) | done 2026-10-14 (thread t32, `92f419c9`, A432; gate `HoldingNothingBookingTest`, held at tick 167 without, kept up with); merged at `1cce9a2b` (D64) |
| W2 | A polished point that flows the way a boundary forbids closes that boundary, and the pass is solved again from the reconstruction (random 71 and 26) | done 2026-10-14 (thread t32, `efc3682f`, A433; gate `PolishedClosureTest`, held at tick 734 without, kept up with; quick check `cw28` 195 / 200, 200 / 200, references identical); merged at `1cce9a2b` (D64) |
| C4 | Determinism integration (branch `claude/determinism-rehearsal` `289c6f2d`, the column re-tune patch; A414's `Math.exp` routed through `DeterministicMath`; the rejection-reason map of `PassiveIntervalSolver.Result` in insertion order; the Windows sweep of the five runtimes after C1) | done 2026-10-14: `a8e49dae`, `a68e8e1d`, `ce4809e9` (A427); the five full-suite failures it exposed fixed by A423-A426; the Windows sweep after C1 |
| C5 | Docs copied to the main checkout; CHANGELOG `[Unreleased]` -> 0.7.0; `mod_version`; the merge to `main` | last: the close |
| C6 | Superseded records: `claude/flow-form` (t21, not taken), `claude/e9-start-column` (A255, A256: moot, the chain is gone), t23 (start flows: superseded by D55); the plan's reserve options (one node type; the per-run fill length) stay as later measured options | owner to confirm |
| C7 | The owner's earlier open item "rounding pressure for the whole mod" (D42 took the rest gauge at 0.1 Pa; the display's rounding is open) | owner |

## Distinct defects remaining (2026-10-14, on cl16, branch claude/phase-ports-compressor, the close after A423)

Code: `claude/phase-ports-compressor` at `d3365664`: t29's merge `5b24744e` with C4 (the determinism integration, A427), A423 (a Jacobian sweep states every wall row and bag volume on its base point's branches; a chord step is refused once they change) and the tests restated under D54 and D58 (A424, A425); review "The close: C4, the full suite and the kink (2026-10-14)", A423-A431. Full suite 1258 / 0 (`cl15`). Campaign `cl16` (8 workers, alone): **967 random, 1000 world defaults** (`fx35` 969 / 1000: cleared random 957; worse 71, 707, 788). First-interval family 14 / 0 (`fx35` 14 / 0; 957 left, 707 new). Counters random (world defaults): Newton iterations 1.133 M (109089), solves 0.321 M (39780), Jacobian builds 0.495 M (36852); `fx35` 1.076 M (109768), 0.288 M (39799), 0.460 M (36987).

Class counts `fx35` -> `cl16` (random; world defaults all clean): C 11 -> 10; E10 4 -> 5; E2 1; E3 2; E4 1 -> 0; E5 3; E6 2; E7 1; E8 3 -> 5; F 3 -> 4.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| Resolved here | The pumped chains' cold first tick: one-sided differences across the wall's kink, chord steps across branch flips (A423); tests that asserted what D54 and D58 removed (A424, A425) | Full suite only (`cl02` 5 failed, `cl15` 0) | - |
| Cleared on the merged code (t32, D64; `cl18` 972 / 1000 random, 1000 / 1000 world defaults, A439: also 246 and 521 cleared, none worse) | 707: a residue vessel's closure Newton differenced 1e19 times its contents, no finite trial (A432, W1); 71 and 26: a polished point pushing 0.01 Pa back into a one-way generator refused the step instead of closing the boundary (A433, W2). Neither is A423's: C4's roundoff exposed them (A434) | E8 random 71; E10 random 707; E8 random 26 (filed under D44 on `cl16`, below) | Done: W1 `92f419c9`, W2 `efc3682f`, merged at `1cce9a2b`; quick check `cw28` 195 / 200 |
| New worse | 788: budget | F random 788 | Alone once (`cl17`): F again, counts as worse (D50); not touched by W1 or W2 |
| A junction's minor gas and traces | As on `fx35` (A416, A417) | E8 random 803; E6 random 430 | The first solver item of the next batch (D63) |
| The first grams of a hot crude | As on `fx35` (A418) | E6 random 431 | With the thermodynamics work (D56) |
| D44 and the data floors | As on `fx35`; 521 moved E4 -> E8 (not touched by W1 or W2) | C 10, E10 4 200 333 699, E2 372, E3 157 446, E5 678 698 752, E7 600, E8 26 246 521 (26 cleared by W2 on the merged code, above: it was a solver hold, not a D44 case) | Thermodynamics work |
| Budget | slow transients | F 579 889 941 | Leave |

Seeds per class on `cl16`: random C 101 175 206 241 444 453 853 937 978 994; E10 4 200 333 699 707; E2 372; E3 157 446; E5 678 698 752; E6 430 431; E7 600; E8 26 71 246 521 803; F 579 788 889 941. World defaults none.

## Distinct defects remaining (2026-10-14, on `fx35`, branch `claude/last-six`, thread t29 after owner decision D62 (4))

Code: `claude/last-six` at `cb26806e` on the batch head `2226df88`: a dry end's outflow within the tolerance is no flow (A411), a chord step must still halve every row's gap, the pressure slopes included (A414), a vessel holding nothing is laid out dry (A415); review "The last six of the solver's own (2026-10-14)", A410-A422. Campaign `fx35` (8 workers): **969 random, 1000 world defaults** (`cc54` 966 / 1000: cleared random 707, 708, 886; worse none). First-interval family 14 / 0 (`cc54` 17 / 0). New C hold alone (`fx36`): random 957 holds again.

Class counts `cc54` -> `fx35` (random; world defaults all clean): C 10 -> 11; E10 6 -> 4; E2 1; E3 1 -> 2; E4 0 -> 1; E5 2 -> 3; E6 3 -> 2; E7 2 -> 1; E8 4 -> 3; E9 2 -> 0; F 3.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| Resolved here | A roundoff circulation booked from an empty tank, a residue laid out with phases, chord steps on stale pressure slopes (A410-A415) | - | - |
| A junction's minor gas and traces | Fraction rows absolute in mass fraction (a gas at 9e-4 of the mass, 58 % of the volume, resolved to 1e-6 of itself); traces omitted at one junction (D41) booked into one that resolves them; a two-phase crude trace whose fugacity rows are noise (A416, A417) | E8 random 803; E6 random 430 | One design item: own-share fraction rows, omitted traces carried consistently in rows and booking, D41 for any hydrocarbon trace (review S2) |
| The first grams of a hot crude | An empty tank's first 2.7e-7 kg of 383 K crude holds a real vapour; its fugacity rows close 0.36 per chord step (A418) | E6 random 431 | With the thermodynamics work (D56's first grams) |
| D44 and the data floors | crude two-phase, data floors, the Jacobian probe leaving the data, a flash at the bubble point with trace water | C 11 (957 from E10), E10 4, E2 372, E3 157 446, E4 521, E5 678 698 752, E7 600, E8 26 246 | Thermodynamics work |
| Budget | slow transients | F 579 889 941 | Leave |

Seeds per class on `fx35`: random C 101 175 206 241 444 453 853 937 957 978 994; E10 4 200 333 699; E2 372; E3 157 446; E4 521; E5 678 698 752; E6 430 431; E7 600; E8 26 246 803; F 579 889 941. World defaults none.

## Distinct defects remaining (2026-10-14, on `cc54`, branch `claude/chord-closures-cavitation`, thread t28 after owner decisions D59, D60 and D61)

Code: `claude/chord-closures-cavitation` at `bd8fc1cd` on the batch head `96eeb42f` (A371 merged in): the pump's suction limit (A390), the mover's own end (A391), the column of an end that may not fill a connection (A392), the capped-junction anchor (A393), holding nothing at the bag's resolution (A394), the column density on the trial (A395); review "The B items after the merge: cavitation, the chord budget, the closures and the capped junctions (2026-10-14)", A390-A403. Campaign `cc54` (8 workers): **966 random, 1000 world defaults** (`tw15` 951 / 994: cleared 16 / 6, worse random 803; `mh01` 943 / 987). First-interval family 17 / 0 (`tw15` 31 / 2). New C and F holds alone (`cc55`): random 853 (C) and 889 (F) hold again.

Class counts `tw15` -> `cc54` (random; world defaults all clean): C 12 -> 10; E2 12 -> 1; E3 1; E5 4 -> 2; E6 3; E7 3 -> 2; E8 4; E9 2; E10 4 -> 6; F 4 -> 3.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| Resolved here | Cavitation, pump into a generator, void column, capped junctions, residue tanks (A390-A394) | - | - |
| Two caps in series at the cap's edge | A chain carries one gas flow through two lines of nearly one density, one on its cap, one on its law at its cap drop (105.3 against 104.95 Pa) | E6 random 886 | A form for caps in series (t19's E6-C) |
| A filling transient | A 345 kPa water generator fills two placed tanks at up to 69 kg/s; the interval runs out of attempts on Newton limits | E8 random 803 (new) | With A336's filling family |
| A phase-port overdraw | "Candidate overdraws a reservoir" | E9 random 708, 446 | Trace |
| Small-headspace tolerance | A315 | E6 random 430 (431 untraced) | With the trace rules |
| D44 and the data floors | crude two-phase, data floors, the Jacobian probe leaving the data | C 10, E10 6 (957 new from E2), E2 372, E3 157, E5 678 752, E7 600 698, E8 26 246 521 | Thermodynamics work |
| Budget | slow transients | F 579 889 941 | Leave |

Seeds per class on `cc54`: random C 101 175 206 241 444 453 853 937 978 994; E10 4 200 333 699 707 957; E2 372; E3 157; E5 678 752; E6 430 431 886; E7 600 698; E8 26 246 521 803; E9 446 708; F 579 889 941. World defaults none.

## Distinct defects remaining (2026-10-14, on `tw15`, branch `claude/turbulent-walk`, thread t27 after owner decision D59 (6))

Code: `claude/turbulent-walk` at `694b0144` on the merged head `d0ec5ff5` (a chord step reuses the Jacobian only while every line's loss slope is within a factor two of it; review "The turbulent walk on gas lines (2026-10-14)", A370-A378). Campaign `tw15` (4 workers): **951 random, 994 world defaults** (`mh01` 943 / 987: cleared 10 / 9, worse 2 / 2). First-interval family 31 / 2 (`mh01` 38 / 8). New C and F holds alone (`tw16`): random 446, 922 (C) and 824 (F) hold again.

Class counts `mh01` -> `tw15` (random / world defaults): C 11 -> 12 / 4 -> 1; E2 11 -> 12 / 0 -> 1; E3 1 / 0; E5 8 -> 4 / 0 -> 1; E6 8 -> 3 / 6 -> 0; E7 4 -> 3 / 2 -> 3; E8 3 -> 4 / 0; E9 2 / 0; E10 4 / 0; F 5 -> 4 / 1 -> 0.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| Turbulent walk (B2) | Resolved: chord steps on stale slopes (A370, A371); the chord-budget worlds (A337, A338) with it | - | - |
| Capped gas junctions | A junction whose every connection is on its cap has no pressure row (A276, A341) | E2 random 123 200 358 456 498 529 660 683 891 957 (298 372 are D44's); E6 random 886 | S5: the junction's pressure placed when all its lines are capped |
| A pump world singular after the walk | World defaults 228 (A337's pump, 10 kg/s into a void) leaves E6 and holds singular; not traced | E2 world defaults 228 | Trace in the B thread with S5 |
| Small-headspace tolerance | The line search cuts every step to 1/16 on a trace phase's fugacity rows; the residual stays above the 1e-10 a small headspace asks (A315) | E6 random 430 | With S5 / the trace rules |
| Per-pass column density | A pocket compressed within a step circulates on two risers' frozen seed columns (A370: 0.62 Pa, 4.3e-5 kg/s) | the root of 498's pocket; no hold of its own | The column read at the iterate's density, or the pass re-entered when it moves (review S3) |
| Active-set cycles, flush, budget, D44, cavitation | as in the owner's sorted section (B1, B3, B4, D44) and S4 | E6 random 431 (not traced here); E7 random 116 600 678, world defaults 101 573 696; E9 514 708; F 188 579 824 941; E8 26 59 246 521; C random 12, world defaults 563; E5 random 698 752 842 853, world defaults 826; E10 4 333 699 707; E3 157 | as there |

Seeds per class on `tw15`: random C 101 175 206 241 444 446 453 889 922 937 978 994; E10 4 333 699 707; E2 123 200 298 358 372 456 498 529 660 683 891 957; E3 157; E5 698 752 842 853; E6 430 431 886; E7 116 600 678; E8 26 59 246 521; E9 514 708; F 188 579 824 941. World defaults C 563; E2 228; E5 826; E7 101 573 696.

## Baseline on the merged head (2026-10-14, `mh01` / `mh02` on `fe380f62`, branch `claude/phase-ports-compressor`, after owner decisions D59 and D60)

Code: t25's three rules and t26's four rules merged (`ecc4a19d`, `80037faf`). Campaign `mh01` (8 workers): **943 random / 987 world defaults** (`rc22` 940 / 987, `cf29` 937 / 971, `es40` 933 / 970). First-interval family 38 / 8. Newton iterations 1.700 M / 0.462 M, solves 0.328 M / 0.077 M. References `mh02`: suites 544 / 0, chain-100 exact, the 33 junction lines identical to `rc24` and `es43`, MIXED_GAS_COST 362, printed lines 6186 with 264 moved against `rc24` (t26's `PhasePortPriorityTest` overflow runs, as `cf31`), G3 45 / 45; no GameTest run (t26's `cf31` prints stand). Threads t27 (B2) and t28 (the B items of D59 and D60) measure against `mh01` and `mh02` (A365).

Seeds per class on `mh01`: random C 101 175 206 241 444 453 678 889 937 978 994; E10 4 333 446 699; E2 123 200 298 358 372 456 529 660 683 891 957; E3 157; E5 108 274 681 698 707 752 842 853; E6 248 430 431 498 600 629 883 886; E7 116 337 824 922; E8 26 246 521; E9 121 514; F 188 419 579 788 941. World defaults C 563 696 889 896; E6 107 228 244 524 547 750; E7 573 952; F 29.

## Distinct defects remaining (2026-10-14, on `cf29`, branch `claude/cycles-flush`, thread t26 after owner decision D58)

Code: `claude/cycles-flush` at `01350240` (the emptying form's start and its decision per active set, `3334aa89` `01350240`; the cap entry of a failed iterate, `6d850c94`; the partition booking, `2739caa3`; review "Active-set cycles and the flush family (2026-10-14)", A350-A364). Campaign `cf29` (4 workers): **937 random, 971 world defaults** (`es40` 933 / 970: cleared random 257 322 419 720, world defaults 29; worse none). First-interval family 45 / 20 (`es40` 49 / 20). New C and F holds alone (`cf30`): random 678 and 579 hold again.

Class counts `es40` -> `cf29` (random / world defaults): C 11 -> 12 / 5; E2 12 -> 13 / 2; E3 1 / 0; E5 7 / 12; E6 10 -> 11 / 7; E7 7 -> 4 / 3; E8 2 -> 3 / 0; E9 7 -> 3 / 0; E10 5 / 0; F 5 -> 4 / 1 -> 0.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| The mint's start under choked lines | The physical active set fails its Newton from the mint's junction pressures (337: J1 two hops from a 479 kPa generator at 478.5 kPa, J3 beside it at 101.7 kPa); the failed iterates then release and enter caps | E7 random 337, 824 | Trace the mint balance of 337 after A3/A4 (`NetworkReduction`, t25's area) |
| A pump drawing its suction to the floor | A pump on its head limit pulls its suction junction to 100 Pa; the junction's phase flips water/vapour and its cap enters and leaves; 952's pump discharges into a generator | E7 random 922, 116; world defaults 573, 826, 952 | A1 (refusal on a dry or starved suction), t25 |
| A sealed ring of junctions | Every boundary closed, a ring of junctions circulates above its cap; the dead-end rule withholds every ring line's cap | E9 random 121, 94 | A4 (a pocket keeps its state and leaves the solve), t25 |
| An inventory booked empty with energy | "Invalid empty/overflowed inventory" | E9 random 514 | Trace |
| Moved by t26 | Random 200 E7 -> E2 (its start is a junction whose lines are all stated, A276); 431 E9 -> E6; 579 E9 -> F (budget at tick 6231); 678 E7 -> C (crude floor, D44); 26 F -> E8 | | Unchanged categories |

Seeds per class on `cf29`: C random 80 101 175 206 241 444 453 678 889 937 978 994, defaults 285 563 696 889 896; E2 random 123 200 298 342 358 372 456 529 660 683 855 891 957, defaults 358 619; E3 random 157; E5 random 274 681 698 707 752 842 853, defaults 6 322 333 485 600 654 683 752 771 794 922 978; E6 random 151 248 397 430 431 498 600 629 728 883 886, defaults 107 228 244 354 524 547 750; E7 random 116 337 824 922, defaults 573 826 952; E8 random 26 246 521; E9 random 94 121 514; E10 random 4 333 399 446 699; F random 188 579 788 941.

## Distinct defects remaining (2026-10-14, on `rc22`, branch `claude/rest-closures`, thread t25 after owner decision D58)

Code: `claude/rest-closures` at `4cbb4d4e` (A1 dry suction `969798b2`, A3 the gauge where stock stands `083f0b8d`, A4 one-way exits `4cbb4d4e`; review "Nothing moves: the closures decided before the solve (2026-10-14)", A334-A346). Campaign `rc22` (8 workers): **940 random, 987 world defaults** (`es40` 933 / 970: 8 / 17 cleared, 1 / 0 worse). First-interval family 42 / 8 (`es40` 49 / 20). New C and F holds alone (`rc23`): random 446 is E10 as on `es40`.

Class counts `es40` -> `rc22` (random / world defaults): C 11 -> 10 / 5 -> 4; E2 12 -> 10 / 2 -> 0; E3 1 -> 1 / 0; E5 7 -> 8 / 12 -> 0; E6 10 -> 7 / 7 -> 6; E7 7 -> 7 / 3 -> 2; E8 2 -> 3 / 0; E9 7 -> 6 / 0; E10 5 -> 3 / 0; F 5 -> 5 / 1 -> 1.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| Category A | Resolved: no world of the sorted section's category A holds as "nothing moves" (A334-A343) | - | - |
| Cavitation at a pump's suction | A fed suction junction pulled to the vapour pressure by a head-limit trial; water flashes to 273.16 K (A335) | C world defaults 563 696 889 896 | A suction limit `P_suction = P_vapour(T)` as a pump mode mirroring the head limit (owner) |
| Chord budget | Flowing liquid worlds whose Newton converges on stale-Jacobian steps and stops at its 20-iteration limit just above the gate (A337, A338) | E6 world defaults 107 228 524 547 | Refresh the Jacobian when a chord step contracts too little; never a larger budget |
| Void driven the forbidden way on a first solve | A void connection into a junction below the ambient carries nothing but is not closed before the first solve (A342) | E6 world defaults 244 750 | The forbidden-direction start closure on first solves (t13's L1) on the seed's balance pressures |
| Capped gas junctions | A junction whose every connection is on its velocity cap has no pressure row (t19's C0, A276; A341) | E2 random 123 358 456 529 660 683 891 957 | B: a junction's pressure placed when all its lines are capped (the cap's anchor) |
| Filling path change | A new island's first rate solve next to exactly empty tanks (A336) | E5 random 108 | With B2 / the first-interval family |
| Active-set cycles, flush, budget, D44 | as in the owner's sorted section (B1, B3, B4, D44) | E7 7 / 2, E9 6 / 0, F 5 / 1, C 10 / 0, E10 3, E3 1, E8 3 | as there |

Seeds per class on `rc22`: C random 101 175 206 241 444 453 889 937 978 994, defaults 563 696 889 896; E2 random 123 298 358 372 456 529 660 683 891 957; E3 random 157; E5 random 108 274 681 698 707 752 842 853; E6 random 248 430 498 600 629 883 886, defaults 107 228 244 524 547 750; E7 random 116 200 257 337 678 824 922, defaults 573 952; E8 random 26 246 521; E9 random 121 322 431 514 579 720; E10 random 4 333 699; F random 188 419 446 788 941, defaults 29.

## Distinct defects remaining (2026-10-14, on `es40`, branch `claude/empty-start`, thread t24 after owner decision D55)

Code: `claude/empty-start` at `ef987556` (the empty start of D54 with the junction's start from its suppliers, D55, `340c782a`; review "The empty start ...", section 10, A324-A333). Campaign `es40` (8 workers): **933 random, 970 world defaults** (`es26` 882 / 943: 75 / 47 cleared, 24 / 20 worse; `ca44` 951 / 962: 23 / 29 cleared, 41 / 21 worse). First-interval family 49 / 20 (`es26` 87 / 34, `ca44` 28 / 21). New C and F holds alone (`es41`): 8 of 9 random and 4 of 5 world defaults hold again.

Class counts `es26` -> `es40` (random / world defaults): C 24 -> 11 / 7 -> 5; E2 22 -> 12 / 6 -> 2; E3 1 -> 1 / 0; E4 0 / 4 -> 0; E5 11 -> 7 / 4 -> 12; E6 24 -> 10 / 13 -> 7; E7 13 -> 7 / 5 -> 3; E8 3 -> 2 / 5 -> 0; E9 3 -> 7 / 0; E10 8 -> 5 / 2 -> 0; F 9 -> 5 / 11 -> 1.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| D44 data floors | A supplied stream throttled or mixed below a component's data floor (methane and crude 293.15 K, water 273.16 K): random 699, a nitrogen junction at 273 K that methane reaches | C 11 / 5, E10 5 / 0 (part) | The thermodynamics work |
| Stiff liquid tie | Liquid junctions seeded at their balance near 1 atm between 1 atm generators and voids; the Newton stalls at 5.0e-8 (world defaults 228) | E6 / E5 among the world-default first-interval holds (20) | Trace the liquid junction's row scale at a 1 atm tie before any change |
| Active-set cycles | Phase/device active-set cycles on supplied fluids; not the dry-end closure (decided once per solve, A328) | E7 7 / 3 | Trace the smallest (random 116) |

Seeds per class on `es40`: C random 80 101 175 206 241 444 453 889 937 978 994, defaults 285 563 696 889 896; E2 random 123 298 342 358 372 456 529 660 683 855 891 957, defaults 358 619; E3 random 157; E5 random 274 681 698 707 752 842 853, defaults 6 322 333 485 600 654 683 752 771 794 922 978; E6 random 151 248 397 430 498 600 629 728 883 886, defaults 107 228 244 354 524 547 750; E7 random 116 200 257 337 678 824 922, defaults 573 826 952; E8 random 246 521; E9 random 94 121 322 431 514 579 720; E10 random 4 333 399 446 699; F random 26 188 419 788 941, defaults 29.

## Distinct defects remaining (2026-10-14, on `es26`, branch `claude/empty-start`, thread t24, first pass; superseded by `es40`, previous section)

Code: `claude/empty-start` at `a5d9c91e` (every vessel a bag with a stiff wall, `cc001df3`; nothing at the mint, `8e3d1fbf`; review "The empty start: every vessel a bag with a stiff wall; nothing at the mint (2026-10-14)", A310-A323). Every fuzz world now starts empty, so this is a new baseline. Campaign `es26` (8 workers): **882 random, 943 world defaults** (`ca44` 951 / 962: 11 / 24 cleared, 80 / 43 worse). First-interval family 87 / 34 (`ca44` 28 / 21). New C and F holds alone (`es27`): 23 of 24 random and 17 of 18 world defaults hold again.

Class counts `ca44` -> `es26` (random / world defaults): C 16 -> 24 / 4 -> 7; E2 8 -> 22 / 10 -> 6; E3 0 -> 1 / 0; E4 0 / 0 -> 4; E5 1 -> 11 / 0 -> 4; E6 6 -> 24 / 13 -> 13; E7 3 -> 13 / 2 -> 5; E8 2 -> 3 / 2 -> 5; E9 4 -> 3 / 2 -> 0; E10 3 -> 8 / 0 -> 2; F 6 -> 9 / 5 -> 11.

| Group | Mechanism in one line | Where it shows | Proposition in one line |
|---|---|---|---|
| Mint guess | The fixed point's first rate solve fails wherever liquid reaches a junction held at the ambient's composition, so the junction is minted as air; the guess then leaves the buffer as real nitrogen (world defaults 933: a 7.8 L pocket in a full tank, gate at 1.8e-8) or starts the first Newton far from its root (random 745: 2 MPa) | most of the first-interval family (87 / 34) | Measure the first round from the step's start estimate with a buffer that holds nothing until the first step (A321) |
| Placed suite worlds | A pump whose suction junction starts as air never runs; card E6-S's top junction keeps its minted gas; a crude tank fed through a junction is singular at its first steps | G1: `InlineFilterLineHoldTest`, `StepStartFlowsTest`, `VesselClosureEquilibriumTest` | The same as the mint guess |
| GameTests | Placed tanks hold nothing: the GameTests read an empty tank's state, expect a charge, or meet a compressor heating an empty tank's first grams past 900 K | 10 fluid GameTests | Restate for placed-empty tanks after the mint guess; trace the compressor case |
| D44 | Crude cooled below the data's 293.15 K floor, crude two-phase nodes | C 24 / 7 (part) | The thermodynamics work |

Seeds per class on `es26`: C random 19 26 101 108 116 163 170 206 223 279 326 401 429 444 483 736 745 839 853 908 911 978 990 994, defaults 163 475 485 499 696 823 978; E2 random 59 159 236 241 246 271 342 358 407 435 518 521 548 629 711 737 818 850 891 896 897 941, defaults 33 328 336 358 727 990; E3 random 681; E4 defaults 101 518 757 929; E5 random 47 430 514 556 651 698 718 786 907 957 961, defaults 173 366 773 892; E6 random 9 12 86 123 157 164 273 354 446 485 553 589 597 654 719 748 750 758 763 780 824 842 883 932, defaults 29 226 244 273 541 556 568 621 629 660 788 847 889; E7 random 48 71 145 178 200 257 547 579 621 886 889 937 951, defaults 71 161 829 839 853; E8 random 219 526 952, defaults 271 678 896 933 951; E9 random 431 720 752; E10 random 4 284 450 475 707 708 768 859, defaults 794 833; F random 6 118 188 298 311 678 788 860 922, defaults 39 78 108 123 164 241 348 446 597 786 826.

## Distinct defects remaining (2026-10-13, branch `claude/group-l`, thread t22)

Code: `claude/group-l` at `e55056d8` (a junction's first pass laid out for the phases its step inflow brings, L1, `45ff3328`; review "Group L: a junction's first step admits the phase that arrives (2026-10-13)", A300-A307). The default start stays the chain (no retirement, A304).

**On the final default (the chain).** No full campaign (D50): the quick check `gl10` gives 192 random / 192 world defaults on seeds 1-200 against `ca44`'s 191 / 192 (random 9, E8, cleared; nothing worse). The table on `ca44` below stands for the chain, with random 9 out of E8.

**Under the physical start with L1** (`-Dcreatecheme.junctionStart=air`, campaign `gl09`): **860 random, 950 world defaults** (`ps01` 822 / 933; chain `ca44` 951 / 962). Class counts (random / world defaults): C 28 / 2, E2 34 / 8, E3 1 / 0, E4 0 / 1, E5 6 / 2, E6 35 / 11, E7 9 / 8, E8 4 / 1, E9 2 / 1, E10 10 / 0, F 11 / 16. First-interval holds traced (`gl14`), world defaults / random:

| Group | Mechanism in one line | P-air + L1 | Proposition in one line |
|---|---|---|---|
| L, start flows | The arriving phase is admitted; the first step's flows start from an estimate read on air junctions (1.145 kg/m3, their caps) and the pass stalls on a flow row | 9 / 32 (41) | The first step's start flows on the mixture's columns and caps (F's round 1 in the cold start, A283) |
| L, data floor | Crude or methane cooled below 293.15 K by evaporation into 298 K nitrogen | 0 / 21 | D44 (thermodynamics work) |
| L, other | Two-phase crude rows 8, singular 7, other junction rows 7 | 7 / 15 | D44; C0; none |
| C0 | Gas-start junctions with every inflow on its cap | 0 / 16 | A276 (open) |
| S, B, C | t13's groups | 5 / 13, 3 / 3, 0 / 1 | As on `ca44` |

## Distinct defects remaining (2026-10-13, on `ff13`, branch `claude/flow-form`, thread t21; measured, the thread recommends not taking the branch, owner decision pending; the batch's current table is `ca44`, next section)

Code: branch `claude/flow-form` at `9c3198fb` (t19's cap mode `22edb2a9` plus the flow form on the law's laminar branch, `1b49f341`; review "The flow row in flow form near its root (2026-10-13)", A290-A298). Campaign `ff13` (8 workers): **949 random, 958 world defaults** (`ca44` 951 / 962: 2 / 2 cleared, 4 / 6 worse; `ig10` 935 / 957). New budget-class holds replayed alone (`ff14`): world defaults 786 clean; 431, 778, random 803 budget holds alone too. Worse worlds were not replayed alone (D50).

Class counts `ca44` -> `ff13` (random / world defaults): C 16 -> 17 / 4 -> 2; E2 8 -> 9 / 10 -> 9; E5 1 -> 3 / 0 -> 2; E6 6 -> 6 / 13 -> 14; E7 3 -> 3 / 2 -> 2; E8 2 -> 0 / 2 -> 1; E9 4 -> 4 / 2 -> 2; E10 3 -> 3 / 0; F 6 -> 6 / 5 -> 10.

| Group | Mechanism in one line | E6 defaults / random on `ff13` | Classes it spans | Proposition in one line |
|---|---|---|---|---|
| E6-S | The Newton walks a line from a turbulent flow towards a root near rest; on gas lines the walk is turbulent to turbulent | 7 / 3 (`ca44` 8 / 3; 214 364 594 cleared, 322 757 916 new) | E6 | Option (c) taken on its laminar branch only, measured: does not carry the campaign (A298); the turbulent walk is open (A298 (c)) |
| E6-B | A first solve keeps a forbidden boundary direction | 6 / 2 (`ca44` 4 / 2; 556 951 new, 981 from cap residue) | E6 | t15's rule (c) (A192), frozen start chain (D48) |
| E6-C | A junction between velocity-capped lines | 1 / 0 (854: a water junction into a void, new) | E6 | t19's mode taken; 854 not minimized |
| E6-T | Random 698, a three-phase crude junction (T by its row) | 0 / 1 | E6 | D44 |
| D44 | Random crude two-phase nodes | - | E2 random 279 298 372 521 786 818, F random 547 | Saved for the thermodynamics work |

Seeds per class on `ff13` are in `tools/phase-ports-probes/hold-classes/logs/ff13-campaign-*-report.txt.classes.txt`; the E6 groups are t13's classifier on this code (`ff17-classification.md`).

## Distinct defects remaining (2026-10-13, on `ca44`, branch `claude/cap-active-set`)

Code: branch `claude/cap-active-set` at `22edb2a9` (the velocity cap as an active-set mode of the connection on `0d841a5f`, D47; review "The velocity cap as an active-set mode (2026-10-13)", A270-A279). Campaign `ca44` (4 workers, lane 2): **951 random, 962 world defaults** of 1000 (`ig10` 935 / 957: 20 / 9 cleared, 4 / 4 worse). Every worse seed holds again alone in its class, every budget hold holds again alone (`ca45`). Against the D44 goal line: random 951 + 7 saved = 958 (42 held), world defaults 962 (38 held).

Class counts `ig10` -> `ca44` (random / world defaults): C 15 -> 16 / 3 -> 4; E2 7 -> 8 / 10 -> 10; E3 1 -> 0 / 0; E5 1 -> 1 / 0; E6 24 -> 6 / 17 -> 13; E7 2 -> 3 / 2 -> 2; E8 1 -> 2 / 2 -> 2; E9 4 -> 4 / 3 -> 2; E10 3 -> 3 / 0; F 7 -> 6 / 6 -> 5.

| Group | Mechanism in one line | E6 defaults / random on `ca44` | Classes it spans | Proposition in one line |
|---|---|---|---|---|
| E6-S | The Newton walks from start flows that are not the solution's; plateaus on near-zero flows | 8 / 3 (214 364 random 660 new; random 594 moved here from C) | E6 | Card E9 (a), as before |
| E6-B | A new island's first solve keeps a forbidden boundary direction | 4 / 2 (485 and random 597 new) | E6 | t15's rule (c), after card E9 (a) (A192) |
| E6-C | A junction between velocity-capped connections | **0 / 0** (`ig10` 0 / 6) | none | **Done on `claude/cap-active-set`**: the cap is an active-set mode (A270-A275); five of the six worlds clear, random 594 holds as S |
| Cap residue | A cap stated in a direction the boundary forbids (world defaults 981); a junction pair between stated flows stalling at the projection bound (random 248, E5) | 1 / 0 | E6, E5 | Measured options worse (A276, A278 (c)); left for the owner |
| E6-T | Random 698: a three-phase crude junction (T by its row only) | 0 / 1 | E6 | D44 (thermodynamics work) |
| D44 | Random crude two-phase nodes | - | E2 random 279 298 372 521 786 818, F random 547 | Saved for the thermodynamics work |

Seeds per class on `ca44`: C random 12 26 59 116 206 241 444 483 504 518 779 787 889 908 937 978, defaults 246 606 792 990; E2 random 163 246 279 298 372 521 786 818, defaults 29 94 163 228 328 358 499 727 823 978; E5 random 248; E6 random 594 597 660 698 768 853, defaults 17 175 214 244 273 352 364 397 401 485 915 937 981; E7 random 145 629 922, defaults 547 952; E8 random 9 803, defaults 39 778; E9 random 579 651 720 761, defaults 181 579; E10 random 4 699 990; F random 77 431 547 678 707 788, defaults 156 298 563 622 788. The E6 groups are read from `ig13` for seeds that kept their class and from t19's traces (`ca49`, `ca53`) for the new ones; t13's classifier was not re-run.

## Distinct defects remaining under a physical start (2026-10-13, branch `claude/physical-start`, thread t20; measured, not taken)

Code `483360c4`, the start chosen by `-Dcreatecheme.junctionStart` (default the chain, so the cards below stay on `ig10`). Campaigns: chain `ig10` 935 random / 957 world defaults; P-air (`ps01`) 822 / 933; P-empty (`ps02`) 821 / 909; fixed point F (`ps05`) 833 / 936. Review "The physical start measured against the chain; the fixed point as the fallback (2026-10-13)", A280-A288. First-interval holds traced with t13's instrumentation (`ps06`-`ps08`) and grouped by `t20-groups.js`; world defaults / random:

| Group | Mechanism in one line | P-air | P-empty | F | Proposition in one line |
|---|---|---|---|---|---|
| L (new) | A junction minted gas-only receives liquid in its first step; the pass's gas-only layout cannot hold it, the junction's mixing and enthalpy rows carry the residual and the trials leave the temperature data | 26 / 102 | 57 / 106 | 29 / 100 | The first step admits the phase that arrives (layout from the inflow, card E5 (b)) |
| C0 (new) | Gas-start junctions whose every connection in is on its velocity cap and none out: continuity flat in every pressure, singular | 0 / 14 | 0 / 15 | 0 / 14 | The cap as an active-set mode (card E6-C (a), thread t19) |
| E6-S | Start flows that are not the solution's | 6 / 12 | 6 / 12 | 6 / 12 | Card E6-S (c) |
| E6-B | A forbidden boundary direction open on a first solve | 2 / 12 | 2 / 12 | 2 / 7 | t15's rule (c) |
| E6-C | A junction between capped gas connections | 0 / 15 | 0 / 14 | 0 / 13 | Thread t19 |
| T | Saturation row of a real two-phase junction | 2 / 0 | 0 / 0 | 1 / 0 | none |

## Distinct defects remaining (2026-10-13, on `ec19`, branch `claude/e9-start-column`)

Code: branch `claude/e9-start-column` at `f1b4b682` (thread t18, from `0d841a5f`): every junction start stands where its stream stands (card E9 (a), `47618332`), t15's rule (c), the start closure of forbidden directions on first solves (`af735b6b`, `e810b6e3`), a junction's falling drain primed by its liquid (`1be85dac`, amends D40 for liquid, owner to confirm) and `ElevatedBlockLineIslandTest` minted as the world mints (`f1b4b682`); review "The junction start in hydrostatic balance; rule (c) re-taken (2026-10-13)", A250-A261. Campaign `ec19`: **937 random, 959 world defaults** of 1000 (`ig10` 935 / 957: random 4 cleared / 2 worse, world defaults 9 cleared / 7 worse, two of them budget holds that hold in E8 alone). Every worse seed holds again alone (`ec20`). Against the D44 goal line: random 937 + 7 saved = 944 (56 held), world defaults 959 (41 held).

Class counts `ig10` -> `ec19` (random / world defaults): C 15 -> 15 / 3 -> 2; E2 7 -> 8 / 10 -> 10; E3 1 -> 1 / 0; E5 1 -> 2 / 0; E6 24 -> 22 / 17 -> 15; E7 2 -> 2 / 2 -> 3; E8 1 -> 2 / 2 -> 0; E9 4 -> 3 / 3 -> 2; E10 3 -> 3 / 0; F 7 -> 5 / 6 -> 9. First-interval family 44 -> 43 / 23 -> 23.

| Group | Mechanism in one line | E6 defaults / random on `ec19` | Classes it spans | Proposition in one line |
|---|---|---|---|---|
| E6-S | The Newton walks from start flows that are not the solution's | 12 / 13 | E6 | Unchanged by the start rules; later-life holds in world defaults, first intervals in random |
| E6-B | A new island's first solve keeps a forbidden boundary direction | 2 / 3 (19 717 / 460 499 651) | E6 | Rule (c) taken on this branch (`ig10` 5 / 5); of what remains, 19 starts at an exact tie with a one-way boundary (no sign for (c) to read, A259) and 717 is a level generator spilling into falling drains with a forbidden flow at a nitrogen junction above (review section 7); random 460 499 651 not traced individually |
| E6-C | A junction between velocity-capped gas connections | 0 / 6 | E6 (random high-pressure gas); random 475 is rule (c)'s own worse world in this family | The velocity cap as an active-set mode (thread t19) |
| E6-T | Row only | 1 / 0 (352: a water junction holding 1.17 m3 of nitrogen) | none | No trace family left (D41) |
| Start-stream findings (t18) | A port's trace phase read as a full supply (d529); a two-phase mixture's density as a primed column (d951, r829); a pump drawing a barometric column into cavitation (d573) | - | E6, E8, E7 | New items; not changed here (review section 7) |
| E9 (c) | A trace just above the flush fraction booked out of a node faster than it holds | - | E9 (random 579 720 761, world defaults 181 579) | Card E9 (c) |
| D44 | Random crude two-phase nodes | - | E2 random 279 298 372 521 786 818 (saved); E5 547 saved, F on `ec19` | Saved for the thermodynamics work (D44) |

Seeds per class on `ec19`: C random 12 26 59 241 444 483 504 518 707 779 787 889 908 922 978, defaults 246 734; E2 random 163 246 279 298 372 521 786 818, defaults 94 163 328 485 499 606 727 823 978 990; E3 random 301; E5 random 173 698; E6 random 19 65 151 206 244 286 336 460 475 499 529 594 600 651 768 824 842 853 886 915 916 937, defaults 19 145 175 273 336 352 397 401 529 681 717 915 937 951 961; E7 random 116 145, defaults 298 573 952; E8 random 803 829; E9 random 579 720 761, defaults 181 579; E10 random 4 699 990; F random 77 431 547 678 788, defaults 29 39 72 156 547 563 622 778 788.

## Distinct defects remaining (2026-10-13, on `ig10`, branch `claude/integration-1`)

Code: branch `claude/integration-1` at `074632b6` (t14's carried gas, t15's rules (a) and (b), t16's gauge, and the virtual junction holdup of D46, which changes no solver number; review "Integration of the carried gas, the committed flows and the gauge; the virtual junction holdup (2026-10-13)"). Campaign `ig10`: **935 random, 957 world defaults** of 1000 (`o02` 919 / 883: 20 / 76 cleared, 4 / 2 worse; t14 `z16` 933 / 930, t15 `sf22` 923 / 901, t16 `z18` 923 / 894). Against the D44 goal line: random 935 + 7 saved = 942 (58 held), world defaults 957 (43 held). Every worse seed holds again alone (`ig12`); of the budget holds, world defaults 929 runs clean alone and 622 holds in E8.

Class counts `o02` -> `ig10` (random / world defaults): C 15 -> 15 / 5 -> 3; E2 9 -> 7 / 13 -> 10; E3 0 -> 1 / 3 -> 0; E4 0 / 7 -> 0; E5 6 -> 1 / 14 -> 0; E6 30 -> 24 / 44 -> 17; E7 3 -> 2 / 3 -> 2; E8 6 -> 1 / 12 -> 2; E9 3 -> 4 / 6 -> 3; E10 4 -> 3 / 4 -> 0; F 5 -> 7 / 6 -> 6. First-interval family 42 -> 44 / 32 -> 23.

| Group | Mechanism in one line | E6 defaults / random on `ig10` | Classes it spans | Proposition in one line |
|---|---|---|---|---|
| E6-S | The Newton walks from start flows that are not the solution's | 12 / 12 (later 9 / 1) | E6; the integration's worse world random 556 (t15's rule (b) on a new island's first solve) | Card E9 (a): a new junction's start pressure restated hydrostatically from the boundary that sets it; the later world-default S worlds are `MIXED` gas-water columns |
| E6-B | A new island's first solve keeps a forbidden boundary direction | 5 / 5 (all first) | E6; E3 (random 301); the integration's worse worlds world defaults 244 and random 301 (t15's rule (b) on a new island's first solve) | t15's rule (c), after card E9 (a) (A192) |
| E6-C | A junction between velocity-capped gas connections | 0 / 6 (all first) | E6 (random, high-pressure gas) | The velocity cap as an active-set mode |
| E6-T | A trace gas phase in free water | 0 / 0 (random 698 is T by its row only: a three-phase crude junction) | none | Done (D41) |
| E9 (c) | A trace just above the flush fraction booked out of a node faster than it holds | - | E9 (random 761) | Card E9 (c) |
| D44 | Random crude two-phase nodes | - | E2 random 7, E5 random 629, F random (slow crude islands) | Saved for the thermodynamics work (D44) |
| (typed) | Thermodynamic domain edges reached physically | - | C: world defaults 246 (an adiabatic tank cooling to 273.16 K; reached through t15's rules), 164 (nitrogen drawn to 94 Pa) | Typed waits, correct as holds; wall heat is outside the model |

E6 on `ig10` was traced seed by seed with t13's instrumentation and group rule (`ig13`, every E6 seed in S, B, C or T). Seeds per class on `ig10`: C random 12 26 59 206 241 444 483 504 518 779 787 889 908 922 978, defaults 164 246 990; E2 random 163 279 298 372 521 786 818, defaults 29 94 163 328 358 485 499 727 823 978; E3 random 301; E5 random 629; E6 random 19 65 92 118 151 173 244 286 336 460 499 529 556 594 600 698 768 824 842 853 886 915 916 937, defaults 17 145 175 228 244 273 336 352 397 401 475 606 681 833 915 937 961; E7 random 116 145, defaults 734 952; E8 random 803, defaults 39 778; E9 random 579 651 720 761, defaults 181 579 792; E10 random 4 699 990; F random 77 246 431 547 678 707 788, defaults 156 298 563 622 788 929. No E1, E4, D or A seed on `ig10`.

## Distinct defects remaining (2026-10-13, on `z16`, side branch `claude/bubble-through`)

Code: side branch `claude/bubble-through` (`c767503a`, the carried gas of owner decision D41, on `o02`'s code `ca7880ec`). Campaign `z16`: **933 random, 930 world defaults** of 1000 (`o02` 919 / 883; 14 / 50 cleared, 0 / 3 worse). Replayed alone (`z17`): the three worse seeds hold again; of the budget holds, defaults 616 and 669 run clean alone (932 counting them), the others hold. Review "A gas bubble in a liquid-full junction is pushed through (2026-10-13)".

| Group | Mechanism in one line | E6 defaults / random on `z16` | Proposition in one line |
|---|---|---|---|
| E6-T | A trace gas phase in free water, the saturation row of a vanishing phase | **0 / 0** (`q04` 17 / 3) | Implemented: a gas bubble in a liquid-full junction is carried by the water, dry, with no row; a trace has no unknown in the junction's solve (D41, A170-A177) |
| E6-S | The step starts its flows from a static per-connection estimate | 26 / 14 | Unchanged (t15: committed end flows carried, one column rule) |
| E6-B | A new island's first solve leaves forbidden boundary directions open | 9 / 5 | Unchanged (t15: the closure on first solves, with E6-S) |
| E6-C | A junction between velocity-capped gas connections | 0 / 6 | Unchanged: the cap as an active-set mode |
| (not E6) | Random crude two-phase nodes (E2, E5; saved for the thermodynamics work, D44); random E8 with refused attempts only | - | Not traced here |

E6 on `z16` was traced seed by seed with t13's instrumentation and group rule (`z22`, 60 seeds, every one in S, B or C). Class counts `o02` -> `z16` (random / defaults): C 15 -> 15 / 5 -> 3; E2 9 -> 8 / 13 -> 10; E3 0 / 3 -> 0; E4 0 / 7 -> 3; E5 6 -> 4 / 14 -> 0; E6 30 -> 25 / 44 -> 35; E7 3 -> 2 / 3 -> 2; E8 6 -> 1 / 12 -> 1; E9 3 / 6 -> 7; E10 4 -> 3 / 4 -> 0; F 5 -> 6 / 6 -> 9. In world defaults, three distinct defects remain behind E6 (S, B; C is random only); E5 has no world-default seed left.

## Distinct defects remaining (2026-10-13, on `q04`)

Final code of the stack: side branch `claude/keep-contents` at `9fd8e5e1` (campaign `q04`: **921 random, 881 world defaults** of 1000). Class E6 (defaults 50, random 27) is split by mechanism from the failing-row trace of every seed (t13; review "Hold class E6 on the final code, by mechanism (2026-10-13)"). Four mechanisms, one card each below (E6-T, E6-S, E6-B, E6-C). The E2, E5 and E8 counts are the class counts; two worlds of each were traced to see which mechanism they share.

**Not taken (D45, 2026-10-13).** The kept-contents branch is not taken: the `p03` and `q04` lines below, and the `q04` basis of the table above, describe code that is not on the branch. The branch's code is `claude/gascolumn-gametest` (`o02`: 919 random, 883 world defaults). Counts on `o02` (random / defaults): C 15 / 5, E2 9 / 13, E3 0 / 3, E4 0 / 7, E5 6 / 14, E6 30 / 44, E7 3 / 3, E8 6 / 12, E9 3 / 6, E10 4 / 4, F 5 / 6. The E6 mechanism groups stand; the threads acting on them (t14 on E6-T, t15 on E6-S and E6-B) restate their seed lists on `o02`.

**Side branch `claude/start-flows` (t15, 2026-10-13, from `ca7880ec`; owner to decide).** The step starts from the committed flows, with one column rule for the estimate (per link on a contracted run) and no carried flow in a forbidden direction (`8721a138`, `09f3afbe`, `6b6c94b2`). Final campaign `sf22` (the gate of rule (c) restored): **923 random, 901 world defaults** (902 counting defaults 118, clean alone); every worse seed holds again alone in its class (`sf13`). Class counts `o02` -> `sf22` (random / defaults): C 15 -> 15 / 5 -> 3; E2 9 -> 8 / 13 -> 11; E3 0 -> 1 / 3 -> 2; E4 0 -> 1 / 7 -> 7; E5 6 -> 4 / 14 -> 6; E6 30 -> 27 / 44 -> 33; E7 3 -> 2 / 3 -> 2; E8 6 -> 5 / 12 -> 17; E9 3 -> 4 / 6 -> 6; E10 4 -> 3 / 4 -> 3; F 5 -> 7 / 6 -> 9. Five suite tests fail on rest identities (A191, waits for D42). The start closure on first solves (rule (c), `fde8aa74`) measured 921 / 900 on the final code with two more suite failures; recommended out (A192). The E6-S, E6-B and E2 cards below end with their restatement (first on `sf08`, `09f3afbe`; on `sf22` only defaults 902 and random 788 differ, both cleared).
**Side branch `claude/rest-resolution` (t16, 2026-10-13, from `f1261205`): the rest certificate is the gauge (D42, D43). Campaign `z18` (`f2b6a987`): 923 random, 894 world defaults; every budget hold and the one worse seed hold again alone (`z21`).** Counts `o02` -> `z18` (random / defaults): C 15 -> 16 / 5 -> 4; E2 9 -> 8 / 13 -> 13; E3 0 / 3 -> 2; E4 0 / 7 -> 7; E5 6 -> 3 / 14 -> 13; E6 30 -> 28 / 44 -> 40; E7 3 / 3; E8 6 -> 7 / 12 -> 11; E9 3 -> 4 / 6 -> 4; E10 4 / 4; F 5 -> 4 / 6 -> 5. Ten of the twelve tie worlds of `o02` (families B and C) are clean; the C, E2, E5, E9 and F cards end with their restatement on `z18` (review "The certificate is the gauge (2026-10-13)").

**Saved for the thermodynamics work (D44): random E2 279 298 372 521 786 818, random E5 547 (7 seeds; E2 6, E5 1).** They leave this batch's zero goal: on `z18` the goal counts random 923 clean + 7 saved = 930 of 1000 (70 held), world defaults 894 (106 held). Card "D44" below; cases in `tools/phase-ports-probes/saved-cases/`.

| Group | Mechanism in one line | E6 defaults / random | Classes it spans | Proposition in one line |
|---|---|---|---|---|
| E6-T | A trace gas phase in free water (far below the gas's solubility): the water-vapour saturation row of a vanishing phase | 17 / 3 | E6; E5, E2, E8 world defaults (6 of 6 sampled); E10, E4 by the earlier cards | Gas solubility in water (D35's separate work): a trace below its solubility is dissolved and has no phase and no row |
| E6-S | The step starts its flows from a static per-connection estimate: every island job forgets the flows, and the estimate's column rule differs from the flow row's | 24 / 15 | E6 | The committed interval carries its end flows into the next job; the estimate states its column as the row does. t15 (`sf22`): E6 44 -> 33 / 30 -> 27, campaign 923 / 901; five rest-identity suite tests wait for D42 (A191) |
| E6-B | A new island's first solve leaves forbidden boundary directions open (a void supplying, a generator receiving): pass 0 must converge on an unphysical network | 9 / 3 | E6; E2 at the same tick when the Newton gets more iterations | The start-of-solve closure of forbidden directions runs on a first solve too (its "first solve is a guess" gate predates the physical junction start), taken with E6-S's consistent start: alone it is net negative on the quick check. t15 (`sf24`): clears the card world, net -2 / -1 on top of E6-S's rules and two suite failures; waits for card E9 (a) (A192) |
| E6-C | A junction between velocity-capped gas connections: the capped row is flat in the end pressures, so the junction's pressure is undetermined in the Newton step | 0 / 6 | E6 (random, high-pressure gas, first interval) | A connection at its cap becomes an active-set mode (its flow stated, like a pump at its target) |
| (not E6) | Random E2/E5 on crude two-phase nodes; random E8 with no failed Newton in the hold tick (refused attempts) | - | E2, E5, E8 random | Not traced here |
| Ties, zero-flow loops (t16, `z18`) | Sources in exact ties approach rest; the solver stepped below any gauge until a Newton failed at a root of roundoff flows | 3 / 1 on `o02` (616 651 719 / 188) | E9, E5, E6, E2, E4, F on `o02`: 12 worlds | **Done on `claude/rest-resolution`**: the rest certificate reads a 0.1 Pa gauge (D42, D43); 10 of 12 clean, 78 and 719 hold on other mechanisms |
| D44 crude (t16, `z18`) | Hot light crude flashing in a tank or junction: singular at or near a converged point, or a stall | - | E2 6, E5 1 (random 279 298 372 521 786 818; 547) | **Saved for the thermodynamics work** (`tools/phase-ports-probes/saved-cases/`), out of the zero goal |
| E9-V (t16, `z18`) | "Invalid inventory volume/energy" while water falls into a gas-filled junction or tank at a real drive (5.5-13 kPa) | - | E9 defaults 19 23 273 771 | Not traced here (not a rest) |
| E9-T (t16, `z18`) | A flow at a real drive draws a trace a step books beyond what its node holds; 761: nitrogen at 1.7e-24 of a vented tank, just above the flush fraction 1e-24 (D16) | - | E9 random 579 651 720 761 | Card E9 option (c), or the flush fraction against the step's draw; not taken here |

Counts restated on `q04` (defaults / random): E6 50 / 27 (= T 17 / 3 + S 24 / 15 + B 9 / 3 + C 0 / 6), E2 16 / 8, E5 12 / 5, E8 10 / 6, C 9 / 17, E9 6 / 4, E10 4 / 4, E4 4 / 1, F 4 / 5, E7 3 / 2, E3 1 / 0. Distinct defects behind E6, E5, E2 and E8 in world defaults: four (T, S, B, C), of which T is shared with E5, E2 and E8.

For the one-by-one review with the owner before any merge preparation (owner decision D27). Branch `claude/phase-ports-compressor` at `0eb6655c` (network reduction stage with rules D17 to D22, trace phases folded in the flash, D34). Campaign `tools/phase-ports-probes/hold-classes/logs/f28-campaign-groupC-*`: 1000 seeds per distribution, 120 slices of 5 s, a pipe block always placed between a device and the block it attaches to (D28). Clean: **822 random, 817 world defaults** (group B, `d10` on `857963d9`: 816 / 817).

**Side branch `claude/e6-row-scale` (option (a) taken, D35/D36, merging after its measurements).** Option (a) alone (`g32`, `0563d4b1`): 833 random, 853 world defaults. With G2, a vessel stated at what it holds (`h22`, `bf830c78`): **837 random (838 counting seed 244, clean when replayed alone), 856 world defaults**. G2 removes most of class E4 (12 -> 0 random, 21 -> 6 defaults). With the G2 follow-up, a vessel closed in the step's own equilibrium (`j18`, `fa45d02a`): **839 random, 856 world defaults**. The E6 and F cards below are restated on `h22` and `j18`; the other cards keep their `f28` counts (current counts per class: review "Hold class E6, grid overshoot (G2) and the one-way scale", section 8).

**Merged code (t7, 2026-10-12).** `claude/e1-e4-measure` (R1 and the junction start rule, nearest supplier, D37) merged onto this branch at `62e6b790` (gate test rebuilt at `96d9be65`). Campaign `k02` (8 workers): **887 random, 869 world defaults** (`j18` 839 / 856; `i26`, the start rule on its own branch, 884 / 866). Every worse seed and every budget hold was replayed alone (`k03`): all hold again, none runs clean alone. Every card below ends with its restatement on `k02` (counts, seeds, what left the class since `j18` and since `i26`, and how the two uncommitted start-rule variants move it). Variants measured on the same code: strongest supplier first 894 / 885 (895 counting random 788, clean alone), mixture of the suppliers 889 / 874. Review "Integration of R1 and the junction start rule (2026-10-12)". **Side branch `claude/start-mixture` (t8, 2026-10-13): junction start (m'), the pressure-balanced mixture of the suppliers (D39). Campaign `m05`: 898 random (900 counting 6 and 23, clean alone), 883 world defaults.** The E4, E5 and E6 cards end with their restatement on `m05`; the other cards keep their `k02` lines. Variant A119 (every draining junction started at its balance pressure, scratch): 919 / 879 (review "Junction start (m'): the pressure-balanced mixture (2026-10-12)", section 8). **Side branch `claude/start-pressure` (t9, 2026-10-13): every draining junction starts at its balance pressure, a lone stream throttled to it (A126). Campaign `n02`: 920 random, 883 world defaults; every worse seed and budget hold holds again alone (`n03`).** The E4, E5, E6 and E8 cards end with their restatement on `n02`; the other cards keep their earlier lines (current class counts in the review "Junction start at the balance pressure (2026-10-13)", section 4). Option G (the drain-side gas column, scratch): 920 / 884. **Side branch `claude/gascolumn-gametest` (t10, 2026-10-13, from `claude/start-pressure`): the drain-side gas column counted in the junction start (option G as a rule, `c09dc06d`, amends D40 for gas). Campaign `o02`: 919 random, 883 world defaults; every worse seed and budget hold holds again alone (`o03`).** Class counts `n02` -> `o02` (random / defaults): C 15 -> 15 / 5 -> 5; E2 10 -> 9 / 13 -> 13; E3 0 / 3 -> 3; E4 1 -> 0 / 4 -> 7; E5 4 -> 6 / 13 -> 14; E6 30 -> 30 / 42 -> 44; E7 2 -> 3 / 2 -> 3; E8 6 -> 6 / 14 -> 12; E9 2 -> 3 / 7 -> 6; E10 3 -> 4 / 3 -> 4; F 7 -> 5 / 11 -> 6. The C, E2, E4, E5, E6, E7, E8, E9, E10 and F cards end with their restatement on `o02` (review "Junction start: the drain-side gas column (2026-10-13)"). **Side branch `claude/keep-contents` (t11, 2026-10-13, from `claude/gascolumn-gametest`): a junction keeps its contents across a topology edit (option K, `078aa67f`). Campaign `p03`: 915 random, 874 world defaults; every worse seed and budget hold holds again alone (`p11`).** Class counts `o02` -> `p03` (random / defaults): C 15 -> 21 / 5 -> 10; E2 9 -> 9 / 13 -> 14; E3 0 / 3 -> 1; E4 0 / 7 -> 4; E5 6 -> 5 / 14 -> 10; E6 30 -> 27 / 44 -> 56; E7 3 -> 2 / 3 -> 3; E8 6 -> 7 / 12 -> 11; E9 3 -> 2 / 6 -> 6; E10 4 -> 4 / 4 -> 5; F 5 -> 8 / 6 -> 6. Variants (patches): strict 923 / 875, an unchanged island not replaced 914 / 876, both 922 / 879. The C, E2, E3, E4, E5, E6, E7, E8, E9, E10 and F cards end with their restatement on `p03` (review "A junction keeps its contents across a topology edit (2026-10-13)"). **Same side branch (t12, 2026-10-13): option K with both rules, an island the edit leaves identical kept as it is (`fe9ec799`) and a kept junction the new flows displace within the first step started as what arrives (`67a329f8`). Campaign `q04`: 921 random, 881 world defaults; every worse seed and budget hold holds again alone (`q05`; random 246 F in the campaign, clean alone).** Class counts `o02` -> `q04` (random / defaults): C 15 -> 17 / 5 -> 9; E2 9 -> 8 / 13 -> 16; E3 0 / 3 -> 1; E4 0 -> 1 / 7 -> 4; E5 6 -> 5 / 14 -> 12; E6 30 -> 27 / 44 -> 50; E7 3 -> 2 / 3 -> 3; E8 6 -> 6 / 12 -> 10; E9 3 -> 4 / 6 -> 6; E10 4 -> 4 / 4 -> 4; F 5 -> 5 / 6 -> 4. Each rule alone (`q07`): rule one 915 / 877, rule two 922 / 879. The same eleven cards end with their restatement on `q04` (review "Kept contents: unchanged islands and the displacement criterion (2026-10-13)").

**What the trace fold (group C) changed.** Cleared: random 156 and 705 (E5), 762 (E4), 561 (E7), 170 and 629 (C); none worse in random. World defaults: no seed cleared by the fold (285 cleared since `d10` with the fold disabled as well, a budget seed), one worse, 707 (now E6, card E6). The world-default worlds of E5 and E2 hold on a nitrogen bubble in a water-only junction, a trace phase with no receiving phase in the model (A84 keeps it); the random worlds the fold cleared held on the 1e-12 crude entry trace condensed as a hydrocarbon liquid of a few 1e-12 of a nitrogen node. Counts and seed lists below are from `f28`; the worlds and traces of each card are from group B (`d11`, `d13`) and every card world still holds in its class.

How to read a card:
- **World.** The smallest world of the class's smallest default seed, found by removing blocks one at a time while the same class of hold remains (`BoundarySplitProbeTest.minimizeForCards`, log `d11`). "After reduction" means what is actually solved: pipes and devices on dead-end branches are left out (D18), and the island stops at generators and voids (D17).
- **Settings.** World defaults unless stated: water generator 298 K and 101.3 kPa; tank 1 m3 of nitrogen at 298 K and 101.3 kPa; void 101.3 kPa nitrogen; pipe blocks 1 m long, 0.05 m bore; pump 0.01 m3/s with a 500 kPa limit. Height is y, one block is 1 m.
- **Expectation.** From physics. A water column weighs about 9.8 kPa per metre. A generator only supplies, a void only takes, a tank does both. Flows and times marked "estimate" are hand calculations, not measurements.
- **Solver terms.** A *junction* is a pipe block with three or more connections; the model gives it a small owned holdup (a few grams to a kilogram), whose mass stays fixed during a step. A *row* is one equation of the solve. A *Newton iteration limit* means the solver took its 20 allowed corrections and was still not within tolerance. A *line search stall* means no correction reduced the error. A *singular Jacobian* means the equations could not be told apart (one of them does not depend on anything the solver can change). *At every step size* means the solver halved its time step down to its floor and the same failure came back each time. The node and row named come from the trace `instr-failing-rows.patch`, logs `d13-trace-*`.
- **Cause.** "Confirmed" when a trace or an earlier measurement shows it; otherwise "unconfirmed".

The cards are ordered by the number of world-default seeds in the class.

---

## E6: the step solve stops at its iteration limit, by mechanism (on `q04`: defaults 50, random 27)

All 77 seeds were replayed alone with the failing-row trace (`src/instr-t13-failing-rows-and-levers.patch`, log `y03`); every first hold reproduces the campaign's to the last digit. The group of a seed is read from the last failed Newton of its hold tick, in this order: T if its worst row is a saturation or partial-pressure closure row; B if it is a new island (its first second) and a connection flows the way its boundary forbids; C if the worst connection sits on its velocity cap in three quarters of the iterates; S otherwise. Two diagnostics confirm the reading (scratch, working tree only): with 60 Newton iterations instead of 20, 44 of the 77 seeds run clean (S 26 of 39, B 9 of 12, C 6 of 6, T 3 of 20); and each group's own lever clears its own card world and no other. Per-seed table: review, section 3.

---

## E6-T: a trace gas phase in free water (defaults 17, random 3)

**World (defaults seed 844, 5 blocks, minimized keeping the group, log `y16`).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, 0, 0 | 101.3 kPa |
| Pipe (junction) | 0, -1, 0 | 0.05 m |
| Tank | 0, -1, -1 | nitrogen, 101.3 kPa, beside the junction |
| Pipe | 0, -2, 0 | 0.05 m |
| Void | 0, -3, 0 | 101.3 kPa |

A generator drains through a junction 3 m down into a void; a nitrogen tank stands beside the junction.

**Settings.** World defaults. Placed at ticks 477 (lower pipe), 514 (tank), 672 (void), 694 (junction), 1040 (generator).

**Expectation from physics.** Water falls 3 m (about 29 kPa of head) through the junction into the void, about 17 kg/s. The junction stands near 103.8 kPa, the tank beside it at the same pressure, at rest. Water passing a nitrogen tank takes up dissolved nitrogen: at about 1 atm nitrogen dissolves to 0.62 mol per m3 of water (Henry's law at 25 C), a share of about 1.1e-5 of the water's moles. Below that share no gas phase can exist: the nitrogen is in solution. Steady within seconds.

**What the solver did.** The island ran 46 s to its steady state (16.8 kg/s into the void, the junction at 103 766.85 Pa, 2e-7 kg/s in the tank's connection). The junction holds 4.1e-11 mol of nitrogen beside 5.5e4 mol of water (share 7e-16), laid out as a gas phase of 1e-12 m3. The next interval's step: the Newton starts at 6e-5, and every correction shrinks the gas phase (3.7e-12 to 1e-12 m3) while the saturation row rises to 0.5 and comes back to 8.8e-7 after 20 iterations. At larger steps the same node gives "No finite Jacobian trial at variable 8" (class E10's text) or a stalled line search (E5). The island is held.

**Cause confirmed by the trace (`y20`).** The worst row at the limit is the junction's water-vapour saturation row, `ln(p_w / p_sat)`, in a phase of 1e-12 m3. In all 20 seeds of the group it is the same: a node full of free water carrying a trace gas phase with a share of 1.8e-19 to 6.0e-6 of the water's moles, every one below the solubility share at its pressure. In real life none of these phases exists. More iterations do not help (60 iterations clear 3 of 20; the card world still holds), nor does any start lever. The trace comes from a tank's bulk port (this world), or, in seeds 123 and 678, from a void that pass 0 of a first solve lets supply nitrogen into a water junction (group B).

**Same mechanism elsewhere.** Two traced worlds each of E2 (defaults 792, 681: singular on a water junction's continuity row with 1e-13 mol of nitrogen), E5 (926: saturation row stalled, 7e-17 mol; 138: continuity stall in a water junction) and E8 (1, 778: saturation row of a water junction with a nitrogen phase, inside an interval that runs out of attempts). With 60 iterations or the start levers none of them clears (926 with 60 iterations only). Card E5's mechanism, D35's nitrogen bubble.

**Proposition.**
- (a) Gas solubility in water, the separate work the owner set (D35, D37): free water holds dissolved gas up to its solubility; a trace below it is dissolved, with no gas phase and no saturation row. Absorbs: the trace fold's water exception (A84), card E5's world-default seeds, the E2, E8, E10 and E4 water-junction traces, this group. Moves: every mixed-gas reference (the dissolved amount enters the balances).
- (b) The phase-set step of card E5 (b): a pass lays a node out with the phases its inventory and inflow form; a phase with no amount has no row. Solver-side; does not say where the nitrogen goes in real life.
- Recommended: (a), as already decided; no fix of its own here. Measure this group's 20 seeds on (a) when it exists.

**On `ig10` (t17, the integration branch): none of this mechanism.** Random 698 is T by its row only (a three-phase crude junction, 7.0 mol of vapour).

**Seeds.** Defaults: 17 39 123 145 271 402 431 592 616 654 678 773 839 844 929 937 961. Random: 430 511 752.

**On `z16` (t14, side branch `claude/bubble-through`, the carried gas of D41).** Implemented, proposition (a) replaced by the owner's transport rule: a gas bubble in a liquid-full junction is carried by the water, dry at the junction's pressure, with no saturation row; a trace (below 1e-9 of the junction) has no unknown in the junction's solve and is carried by the transport reconstruction. The card world (defaults 844 minimized) is the gate test `CarriedGasTest`: clean with the rule, held at tick 2067 with it disabled ("Newton iteration limit at residual 8.756e-7", the campaign's hold, `z15`). The group's seeds on `o02` (replayed alone, `z01`) and on `z16` (campaign; alone `z08`): defaults 17 E6 -> clean, 39 E5 -> F (E8 alone), 123 E6 -> clean, 145 E6 -> clean, 271 E6 -> E6 (S), 402 E6 -> E6 (S), 431 E6 -> E6 (S), 592 E6 -> clean, 616 E6 -> F (clean alone), 654 clean -> clean, 678 E6 -> E6 (S), 773 E6 -> clean, 839 E6 -> clean, 844 E6 -> clean, 929 E4 -> F (E8 alone), 937 E6 -> clean, 961 E6 -> clean; random 430 E10 -> E5 (a crude junction, D44), 511 E6 -> clean, 752 E6 -> clean. No E6 seed of `z16` is in group T (`z22`).

---

## E6-S: the step starts its flows from a static estimate (defaults 24, random 15)

**World (defaults seed 786, 8 blocks, minimized keeping the group, log `y18`).**

| Block | Position | Setting |
|---|---|---|
| Water generator | -1, -1, 2 | 101.3 kPa |
| Pipe (junction J1) | 0, -1, 2 | 0.05 m, beside the generator |
| Void | 0, -2, 2 | 101.3 kPa, below J1 |
| Pipes | 0,0,2 (above J1); -1,0,2 (junction J2, above the generator) | 0.05 m |
| Pipes | -2,0,2; -3,0,2 | 0.05 m, from J2 to the tank (placed last) |
| Tank | -3, 0, 1 | nitrogen, 101.3 kPa |

A generator drains through a junction beside it 1 m down into a void; both rise 1 m to a gas-filled junction above, which the last pipe joins to a nitrogen tank.

**Settings.** World defaults. Placed at ticks 158 to 694; the last pipe at 1283.

**Expectation from physics.** Water falls 1 m into the void, about 8.4 kg/s. The two risers above hold gas at their tops. J2 and the tank both stand at 96.8 kPa (the tank had cooled to 281.7 K after venting), below the 99.4 kPa at J1 and the generator's 101.3 kPa: so the riser over J1 holds a standing water column of (99.4 - 96.8) / 9.78 = 0.27 m and the riser over the generator one of (101.3 - 96.8) / 9.78 = 0.46 m, at rest (manometers). Joining the tank at its own pressure moves nothing. Flows in the risers: zero.

**What the solver did.** The island formed at tick 1284 and committed a 6-tick first slice with the risers at 0 kg/s (closed) and -0.0024 kg/s. The next job (tick 1290) starts its first step from the static estimate: both risers at -3.10 kg/s of water (start rows 0.72 and 0.53), the estimate reading the water column (drive -5.3 kPa) where the row reads the gas column (+4.2 kPa). The Newton walks them back through three sign changes, damped and on chord steps, from 11.7 to 7.1e-7 at its 20th iteration. The same kind of end at 20 step sizes (2.6e-9 to 0.18). Held at tick 1298.

**Cause confirmed by the trace (`y20`, levers `y21`-`y24`).**
- `PassiveStepSolver.replayStart` forgets the flows at every island job start ("no warm start: the first solve starts from `initialMassFlows` of the committed states"), although the committed interval carries its end modes and heads (`PassiveIntervalSolver.Result`, the checkpoint codec). Every interval's first step therefore rebuilds the island's hydraulics from scratch.
- `initialMassFlow` chooses its column density from the end with the higher pressure; the flow row chooses the column nearest its own rest (`headDensities`). On a vertical connection between a gas end and a water end the two disagree, and a gas connection's cap drop (1-5 kPa) is far below a metre of water (9.8 kPa), so the estimate puts a cap-level flow on a connection the row sees at rest or driven the other way (seed 241: nitrogen at 0.227 kg/s down into a water junction where the row's drive is -0.004 Pa).
- Near a small root the turbulent loss makes Newton halve the flow per fresh step and less per chord step: from a cap-level start (0.2-3 kg/s) to a root of 1e-4-1e-3 kg/s takes 10 to 30 iterations. The residual does not depend on the step size (the flows are algebraic), so halving the step never helps.
- Group evidence: 24 of the 39 seeds hold within the first second of a new island, 15 later (2 s to 481 s); 26 of 39 run clean with 60 iterations. The card world runs clean with the flows of its last solve carried (scratch L3) and with 60 iterations; with the column rule alone (scratch L2) it holds later.

**Proposition.**
- (a) Flows are state: the committed interval carries its end flows (beside its end modes and heads, in the result and the checkpoint), and a job's first step starts from them; a connection the edit added starts from rest, and only a connection with no committed flow uses the estimate. Stage: the island's committed state and `replayStart`. Keeps replayStart's rule that a job depends only on committed state. Absorbs: the "no warm start" restriction, the stale-flow gate of the rate solve (review 7.7) at a job start, the pump's head-limit start (`headLimitMassFlow`) for a committed pump. Scratch L3 (the solver's own last flows, which is not the committed form): clears 17 of the 77 E6 replays (S 10, T 5, B 2); quick check of seeds 1-200 random 184 -> 186 (0 worse), defaults 173 -> 176 (5 cleared, 96 and 108 worse); the 7 suite failures are the scratch form's (fresh-solver bitwise replays, checkpoint round trips, a certificate), which the committed form would not have. References: not measured.
- (b) One column rule: the estimate states its column as the flow row does (both drives one sign: that column; drives towards each other: the column nearest rest). Absorbs `initialMassFlow`'s "higher pressure end" density and the chain's. Scratch L2: 6 of 77 E6 replays clean (S 6).
- (c) State a passive connection's row in flow form near its root (the flow minus the flow its drive carries), so a fixed-pressure flow is exact in one step. Removes the halving walk; moves every reference. Not measured.
- Recommended: (a) with (b) as one item, "the step starts where the island is", measured on the full campaign before a decision. (c) only if (a)+(b) leave the walk.

**On `ig10` (t17): defaults 12 (17 145 175 336 352 397 401 606 681 833 915 961; 9 later in the island's life), random 12 (19 65 92 244 556 600 768 824 842 886 915 916; 11 on the first interval).** Worse world random 556 (minimized, 15 blocks) is clean on `o02`, t14 and t16 and holds with t15's rules: rule (b)'s estimate on a new island's first solve.

**On `ec19` (t18, branch `claude/e9-start-column`: the column, rule (c), the primed falling drain): defaults 12 (145 175 273 336 397 401 529 681 915 937 951 961), random 13 (19 65 206 244 475 600 768 824 842 853 886 916 937).** `ig10`'s twelve replayed alone (`ec20`): defaults 17 and 833 clean, 606 E2, nine still E6; random 92 and 556 clean, ten still E6. World random 556 is clean: its held junction's balance cycled with period 2 and stopped 6.6 kPa above the step's root (A261); with its falling drains primed it starts in reach. New in S: 273 and 937 (B on `ig10`), 529 and 951 (the primed drain's worse worlds: a tank's trace water read as a supply, mixture columns; review section 7).

**On `ff13` (t21, `claude/flow-form`: option (c) on the law's laminar branch): defaults 7 (175 322 352 397 401 915 937), random 3 (660 757 916).** Option (c) is measured (A290-A298): the walk is the loss row linearised at a turbulent flow, halving towards a root near rest; the flow form removes it on the laminar branch, but on gas lines that branch ends at a few hundredths of a pascal, and the flow form reaching into the turbulent branch puts the square root of the law on the end pressures and holds more worlds at every reach measured. Gate world: random 594 minimized (`FlowFormRowTest`). Cleared on the campaign: defaults 214 364, random 594; new: defaults 322, random 757 916.

**Seeds.** Defaults: 29 173 175 183 220 241 246 298 352 357 397 424 529 547 606 720 758 768 786 886 915 932 967 981. Random: 19 92 98 107 118 151 244 600 633 660 794 842 886 916 937.

**Restated on `sf08` (t15, side branch `claude/start-flows`, (a)+(b) as rules: the committed end flows carried into every job, one column rule; review "The step starts from the committed flows ...").** The card world on `o02` runs a circulation (water lifted 4.8 kg/s up one riser to the junction under a tank vented to 90.9 kPa, falling back down the other): with the old job start it holds at tick 3210 on the Newton limit (0.079), with (a)+(b) it runs 120 slices (`sf09`; gate test `StepStartFlowsTest`). The 39 seeds replayed alone (`sf01` on `o02`, `sf15`): defaults E6 14 -> 9, clean 5 -> 7 (cleared 220 529 547 720 886 967; still E6 175 352 397 915 981 and, worse, 241 246 758 606; moved 29 E2, 173 768 E4, 183 357 786 932 E8, 298 E9); random E6 12 -> 10, clean 3 -> 5 (cleared 98 633). In the campaign E6 falls from 44 to 34 in world defaults and from 30 to 27 in random; world-default Newton iterations fall 27 %. What remains in E6 holds mostly on a new island's first interval (the first-interval family grows from 32 to 43 in world defaults): those first steps still start from the estimate, and (b) alone does not reach them. On the final code (`sf22`, `6b6c94b2`, the per-link column of a contracted run, A193): E6 33 in world defaults (902 cleared), 27 random; family 42 / 44. The worse worlds are traced in the review, section 6: five first-interval holds from (b) (one, 902, an end-to-end column over a hump, corrected; the others from a physical start the re-minted island's first solve does not reach), six later-job holds from (a) near rest (A191's regime, world 758 traced).

---

## E6-B: a new island's first solve leaves forbidden boundary directions open (defaults 9, random 3)

**World (defaults seed 228, 8 blocks, minimized keeping the group, log `y17`).**

| Block | Position | Setting |
|---|---|---|
| Water generator A | 0, -1, -4 | 101.3 kPa |
| Pipe | 0, -1, -3 | 0.05 m |
| Pipe (junction) | 0, -1, -2 | 0.05 m |
| Water generator B | 0, -1, -1 | 101.3 kPa |
| Pipe (junction) | 0, 0, -2 | 0.05 m, above |
| Pump | 0, 0, -3 | facing north, 0.01 m3/s, 500 kPa |
| Pipe (junction J3) | 0, 0, -4 | 0.05 m, above generator A |
| Void | 0, 0, -5 | 101.3 kPa |

Two generators on a lower line feed a pump 1 m up, which discharges into a junction over generator A and on into a void at the junction's height.

**Settings.** World defaults. Placed at ticks 120 to 804; generator B and the pipe at 0,-1,-3 at tick 834 (the island forms there).

**Expectation from physics.** The pump draws 0.01 m3/s (about 10 kg/s) from both generators through the lower line and lifts it 1 m into J3, which discharges into the void. J3 stands a few kPa above 101.3 kPa (the void's pressure plus the pipe's friction at 5 m/s). Generator A, 1 m below J3, cannot take the water back (a generator only supplies), so its riser stands full at rest. The void only takes. Steady within the first second.

**What the solver did.** On the island's first interval, pass 0 starts with every boundary open both ways: the pump at its 9.96 kg/s target, the other flows from the estimate (continuity residuals up to 37.9). The Newton damps four steps, then contracts linearly (0.45 per chord step) towards an unphysical network: 10.2 kg/s pumped back down into generator A, and 0.225 kg/s of nitrogen (the cap) drawn from the void into J3 at 94.3 kPa. It ends at 8.116e-5 after 20 iterations, the same residual to six digits at 21 step sizes. With 60 iterations pass 0 converges to that point and pass 1 fails singular (class E2).

**Cause confirmed by the trace (`y20`, lever `y22`).** `closeIllegalStarts` closes, at the start of a solve, every passive connection whose start flow runs the way its boundary forbids; it skips a first solve because "a first solve's junction pressures are the compiler's property guess, not a state". Since D37, A126 and A133 a new junction starts at its balance pressure from what reaches it: a physical state. With the closure run on the first solve too (scratch L1) the card world runs its 120 slices clean. In the group, the failing iterate carries 1 to 4 forbidden flows in every seed; L1 clears 5 of the 12 (228 651 842, random 499 719); 9 of 12 clear with 60 iterations. Seeds 123 and 678 of group T get their nitrogen trace this way.

**Proposition.**
- (a) Run the start-of-solve closure on every solve, the first included. Stage: the step solver's start (`closeIllegalStarts`). Absorbs the "accepted structure" gate. Scratch L1 (one condition): clears 5 of the 12 group seeds and no other E6 seed on the replays; but the 200-seed quick check is net negative, random 184 -> 183 (132 worse), defaults 173 -> 172 (1 cleared; 72 and 113 worse), and one suite test fails (`ElevatedBlockLineIslandTest.elevatedLinesIntegrateFortyIntervalsLikeTheirFlatControl`). None of the three worse seeds or the test was traced.
- (b) A first solve's start estimate never puts a forbidden flow on a boundary connection (the estimate closes it itself). Equivalent for these worlds; touches the estimate (card E6-S (b)) instead.
- (c) Take it with E6-S (a)+(b): a first solve whose start flows are consistent with its rows gives the closure a meaningful direction to read. L1+L2+L3 together: quick check random 184 -> 185, defaults 173 -> 175.
- Recommended: (c), not (a) alone: the closure reads the start point's directions, and on a first solve those come from the estimate that card E6-S corrects. Trace the failing suite test and seeds 72, 113, 132 before any decision.

**On `ig10` (t17): defaults 5 (228 244 273 475 937), random 5 (118 460 499 853 937), all on a new island's first interval.** Worse worlds 244 (defaults) and 301 (random, E3) come from t15's rule (b); rule (c) is not taken on the branch (A192).

**On `ec19` (t18, branch `claude/e9-start-column`: rule (c) taken with the start restated): defaults 2 (19 717), random 3 (460 499 651).** `ig10`'s ten replayed alone (`ec20`): defaults 228 244 475 clean, 273 937 E6 (now S); random 118 clean, 460 499 853 937 E6. Rule (c)'s two suite failures of A192 are resolved: the pump-suction junction starts at its standing pressure (column, the fixture minted, A256) and the junction-start world's falling drain is primed (A255). The card world (defaults 228) runs clean (`FirstSolveClosureTest`). Of the new B seeds, 19 starts at an exact tie with a one-way boundary, where (c) has no sign to read (A259; a closure at a tie was measured and breaks pump lines, A260), and 717 spills a level generator into falling drains.

**Seeds.** Defaults: 228 336 475 563 600 651 824 842 951. Random: 460 499 719.

**Restated on `sf08` and `sf12` (t15).** The card world holds on `o02` and on (a)+(b) at tick 836 (8.116e-5) and runs 120 slices with proposition (a), the closure on first solves (`fde8aa74`, gate test `FirstSolveClosureTest`). The 12 seeds alone: (a)+(b) defaults E6 5 -> 3 (651 and 600 cleared; 228 336 951 still E6), random unchanged (460 499 E6). With (a) on top: 228 cleared, 600 C, 842 E6 (worse), 475 E10. In the campaign (a) is net negative on top of (a)+(b) (921 / 899 against 922 / 900; on the final code 921 / 900 against 923 / 901) and fails two suite tests: a pump's suction junction minted 2 m above its generator at the generator's pressure (no hydrostatic restatement, card E9 (a)) reads as a drive into the generator and is closed for good; and a right closure (a void that would supply) leaves pass 0 on a first-solve start it cannot reach. Recommended: (a) after card E9 (a)'s hydrostatic restatement of the junction start (A192).

---

## E6-C: a junction between velocity-capped gas connections (defaults 0, random 6)

**World (random seed 529, 7 blocks, minimized keeping the group, log `y19`; specs `y25`).**

| Block | Position | Setting |
|---|---|---|
| Methane generator | 1, -1, -2 | 311.7 K, 495.9 kPa |
| Pipe (junction J1) | 0, -1, -2 | 0.05 m |
| Pipes | 0,-1,-1; 0,0,-1 | 0.05 m, from J1 up into tank B's side |
| Tank B | 0, 0, -2 | nitrogen 101.3 kPa at placement; fed from J1 through its bottom and its side |
| Pipe | -1, 0, -2 | 0.02 m (placed last) |
| Tank A | -2, 0, -2 | nitrogen, 101.3 kPa |

A 5 bar methane generator fills tank B through a junction and two paths; the last block joins tank B to tank A through a 2 cm pipe.

**Settings.** Random distribution. Placed at ticks 100 to 1104; the 2 cm pipe at 1157. At that moment tank B holds 100 mol methane and 41 mol nitrogen at 441.2 kPa and 376 K (compression heating).

**Expectation from physics.** The generator still drives 55 kPa into tank B: the inflow runs at its limit (in real life choked; in the model the velocity cap, a cap drop of about 3 kPa), about 0.6 kg/s. J1 then sits just above tank B (the outlets' friction, about 1 kPa), its two outlets below their caps. Tank B now also drives 340 kPa into tank A through the 2 cm pipe, capped at about 0.09 kg/s. Tank B rises towards 496 kPa over minutes.

**What the solver did.** On the island's first interval the inflow to J1 is capped (61 kPa against 3 kPa); J1's pressure jumps by about 2e5 Pa per iteration (434.6 to 492.9 kPa iterates), its two outlets switch between capped and uncapped, and the flow rows alternate in sign at every iteration with damped steps. 0.65 after 20 iterations; the rate pass and every step size end alike. 60 iterations: clean.

**Cause confirmed by the trace (`y20`, `y21`).** On the capped branch a flow row is `(+-cap drop - loss(q)) / S`: it does not depend on either end's pressure. With J1's inflow capped, J1's pressure enters only its outlets' rows, and each time an iterate caps them too it is free in the linearized system and the step overshoots. All 6 seeds of the group: pressurised gas (generators of nitrogen or methane at 2.5-5 bar in four, compressors in three), the first interval of a new island, the worst connection capped in 17 to 21 of 21 iterates, its row changing sign in 5 to 20 of 20 iterations; all 6 run clean with 60 iterations; no start lever clears any.

**Proposition.**
- (a) A connection at its cap is an active-set mode, as a pump at its target is: its row states the cap flow of its donor; the pass loop enters the mode when a converged pass finds the drive above the cap drop and leaves it when below. Then a junction's pressure is always fixed by the rows that depend on it. Stage: the step solver's active set and `edgeRows`. Absorbs the clamp branch of the flow row and the cap part of its scale. Not measured.
- (b) A soft clamp: the capped branch keeps a small slope in the drive. Lets the flow exceed the cap slightly; a physics change.
- (c) Leave it: 6 random worlds, all converging with more iterations.
- Recommended: (a), measured before a decision; it states the cap as the choke it models (the flow set by the donor, the downstream pressure free).

**On `ig10` (t17): random 6 (151 173 286 336 529 594), all first intervals; defaults 0.**

**On `ec19` (t18): random 6 (151 286 336 529 594 915), defaults 0.** 173 moved to E5; 915 joined from S. Random 475 (clean on `ig10`) holds with rule (c) alone in this family: a 4.2 bar methane and a 3 bar nitrogen generator, two connections on their cap at -291 kPa of drive beside rows near zero flow (`ec25`).

**On `ca44` (t19, `claude/cap-active-set`): random 0, defaults 0.** Proposition (a) implemented with the rules measurement forced (A270-A279). Alone (`ca52`): 151 173 286 336 529 clean; 594 holds at the same tick on a 1.2e-8 plateau of near-zero flows behind a junction (group S, no line near its cap); world defaults 164 (group C on `ig13`, a typed C hold) clean. The card world is the gate test `VelocityCapModeTest` (clean with the mode; with the old cap restored it holds at tick 1159 at residual 0.6514199613667255, `ca43`). Options (b) and (c) of this card are not needed; the hybrid that keeps the clamp branch was measured and clears two of the six (A278).

**Seeds.** Random: 173 286 336 456 529 594.

---

### History of card E6 (as written up to t12)

#### E6: the step solve stops at its iteration limit (defaults 68, random 27; with option (a): 31, 52)

**World (defaults seed 1, 4 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | -2, 0, 0 | 101.3 kPa |
| Pipe (junction) | -1, 0, 0 | 0.05 m |
| Tank | 0, 0, 0 | nitrogen, 101.3 kPa |
| Void | -1, -1, 0 | 101.3 kPa |

A generator feeds a junction pipe that leads sideways into a tank at the same height and down into a void one block lower.

**Expectation.** Water falls 1 m from the junction into the void, driven by about 9.8 kPa. A steady flow of roughly 10 kg/s from the generator through the junction into the void (estimate). The junction's pressure settles a little below 101.3 kPa, so the tank vents a few percent of its nitrogen into the flow and then stays at the junction's pressure. Steady within the first seconds.

**What the solver did.** On the first slice after the last block, the equation of the generator's connection (the flow law between the generator and the junction) ends at 3.9e-7 after 20 iterations, at every step size. The island is held.

**Cause (confirmed for the class, unconfirmed for this world).** The junction starts as the tank's nitrogen at exactly 101.3 kPa, level with the generator, so the generator's connection starts at exactly zero drive. At zero drive the scale the solver divides that equation by jumps, because it is taken from the end the fluid would come from, and water and nitrogen give scales orders of magnitude apart. The solver's slope estimate straddles the jump, so each correction is off by a constant factor and the error only shrinks linearly. Measured on seed 33 (review "Hold class E6").

**Proposition.**
- (a) Take the scale from both ends (the smaller of the two). Cleared 25 of 86 E6 seeds and 13 of 25 default budget holds when measured. Moves 25 of the 33 junction lines, MIXED_GAS_COST 286 to 273, and fails two extreme-grid tests.
- (b) Freeze the scale per pass from the direction the pass starts in. Clears seed 33. Moves 50 junction-line diff lines, MIXED_GAS_COST 277, four tests fail.
- (c) A scale equal to today's away from zero drive and blended across a band around it. Not prototyped. Would still move every approach-to-rest path.
- Recommended: measure (c) on the current code; if a re-baseline is acceptable, (a) is the largest single lever of this list.
- **Group C: defaults seed 707 joined this class** (clean before the fold, held alone too, `f11`). Tank 21 drains its last water through its bottom port into a junction one metre below; at the hold its seed's free water is 1.30e-9 of its moles, just above the trace share, so the phase stays in the layout. The Newton cannot resolve a phase at its own tolerance; its failed iterates throw the water to 1e-4 to 6e-4 and it stops at 1.11e-9 at every step size (`f12`). This is the vanishing-phase mechanism one notch above the threshold, reached on a trajectory the fold changed (review, group C, section 6).

**Seeds.** Defaults: 1 19 29 35 74 116 145 173 178 199 220 244 246 247 322 325 348 354 357 377 394 401 402 412 424 438 446 450 491 518 521 529 556 559 561 568 572 579 589 592 606 621 624 629 633 645 646 683 707 719 724 734 740 750 752 757 778 786 824 847 850 892 932 937 951 961 978 981. Random: 9 30 35 89 123 178 271 348 430 459 499 505 526 553 600 736 743 748 752 773 794 853 872 886 889 937 992.

**With option (a) (owner decision D35; side branch `claude/e6-row-scale`, code `0563d4b1`; campaign `g32`): defaults 31, random 52.** Review "Hold class E6, option (a) implemented (2026-10-12)".
- Clean seeds: defaults 817 -> 853 (40 cleared, 4 worse: 138 E5, 236 E7, 584 E6, 872 E4), random 822 -> 833 (20 cleared, 9 worse: 6 E1, 23 C, 132 E6, 170 C, 223 E6, 301 E6, 316 E6, 728 E6, 945 E1). Every worse seed holds alone too (`g34`).
- Of the 68 default E6 seeds: 21 cleared (19 29 35 246 247 401 491 518 521 568 572 606 624 633 645 646 707 724 740 757 892); 9 now E4 (325 354 394 412 438 529 561 750 824), 5 E5 (116 173 244 446 559), 4 E8 (1 556 589 932), 4 C (220 450 847 981), 3 E2 (199 752 978), 1 E7 (74), 1 E9 (145), 1 E10 (937); 19 remain E6. New default E6 seeds: 17 208 356 397 (were F), 123 156 720 (E8), 547 (E5), 118 (E2), 336 (C), 736 (E10), 584 (was clean).
- Of the 27 random E6 seeds: 4 cleared (89 459 736 773), 2 C (35 992), 1 D (348), 1 E1 (499); 19 remain. New random E6 seeds: 22 from C (77 78 98 151 168 175 208 214 336 401 431 456 514 594 622 750 832 833 842 891 895 976), 511 719 (E4), 354 681 (E5), 818 (E2), 556 (F), and 132 223 301 316 728 (were clean).
- The label now holds two mechanisms. The zero-flow signature (a step ending just above the tolerance after linear contraction) falls from 41 to 7 default seeds. Step solves ending far from convergence (residual at or above 1e-3) grow in random worlds, 18 -> 34: high-pressure transients whose Newton iterates used to leave the nitrogen domain (typed as waits, class C) and now stop at the iteration limit inside it.
- The card world (defaults seed 1) no longer holds here: it commits its first interval and holds 62 ticks later on "Interval substep limit" (E8), the minted dry-nitrogen junction meeting water (trace family E5/E10, the mint). Gate tests: `ConnectionRowScaleTest`. Defaults seed 707 is cleared (its hold was this mechanism at a tank's liquid port holding a trace of water).
- Defaults E6 seeds with option (a): 17 118 123 156 178 208 322 336 348 356 357 377 397 402 424 547 579 584 592 621 629 683 719 720 734 736 778 786 850 951 961. Random: 9 30 77 78 98 123 132 151 168 175 178 208 214 223 271 301 316 336 354 401 430 431 456 505 511 514 526 553 556 594 600 622 681 719 728 743 748 750 752 794 818 832 833 842 853 872 886 889 891 895 937 976. No smallest world was minimized for the new list.
- Open with option (a): two extreme-grid gate tests fail on tanks stranded 1.06e-9 above the highest generator (a tolerance-level overshoot; options in the review, section 5), so it is not taken until that is decided.

**With option (a) and G2 (owner decision D36; code `b6f2c072`, `bf830c78`; campaign `h22`): defaults 34, random 52.** Review "Hold class E6, grid overshoot (G2) and the one-way scale (2026-10-12)".
- The grid item is closed: every tank is stated at what it holds, the extreme tests pass (worst overshoot 5.7e-5 Pa, 2.8e-10 of 200 kPa), and the suites pass (511 tests with the new gate test). The one-way scale refinement (A91) is left out (A95): with G2 it keeps a generator's backflow just under its closing threshold.
- G2 does not touch the E6 mechanisms. Final residuals of the first hold (below 1e-6 / 1e-6 to 1e-3 / above): random 7 / 10 / 35, defaults 9 / 10 / 15 (with option (a) alone: 7 / 11 / 34 and 7 / 10 / 14).
- Changes against `g32` (`h23-transitions-*.txt`). Defaults: enter 59, 224, 518 (clean before; all three hold alone in E6), 145 (E9), 589 (E8), 937 (E10); leave 156 (now E4), 208 (E9), 720 (E2). Random: enter 219 and 432 (E4 before); leave 431 (F) and 511 (E10).
- **A class of its own inside the random E6 list** (D36): the 22 worlds that moved from C into E6 with option (a) (77 78 98 151 168 175 208 214 336 401 431 456 514 594 622 750 832 833 842 891 895 976). 21 are still E6; 431 ended on its budget in the campaign and holds in E6 alone. Every one of the 21 holds on the first interval after a block is placed (committed tick = placement tick, held 1 to 5 ticks later), 18 of them at a residual of 1e-3 or more. A start-point question on a freshly compiled island, not E6's zero-flow root. Smallest world without a pump or compressor: seed 168. Proposition: trace it on its own after the merge (review section 7).
- Defaults E6 seeds with G2: 17 59 118 123 145 178 224 322 336 348 356 357 377 397 402 424 518 547 579 584 589 592 621 629 683 719 734 736 778 786 850 937 951 961. Random: 9 30 77 78 98 123 132 151 168 175 178 208 214 219 223 271 301 316 336 354 401 430 432 456 505 514 526 553 556 594 600 622 681 719 728 743 748 750 752 794 818 832 833 842 853 872 886 889 891 895 937 976.

**With the G2 follow-up (A98: a vessel closed in the step's own equilibrium; code `fa45d02a`; campaign `j18`): defaults 34, random 53.** Review "G2 follow-up: cushion vessels, budget seeds, closure cost (2026-10-12)".
- Clean: random 837 -> 839 (cleared 244, 400, 737; worse 274, back in E2 as on `f28`), defaults 856 -> 856 (no seed changes class except seed 1, F in the campaign and E8 alone). The default worlds hold no hydrocarbon, so the fix does not touch them.
- E6 changes: random 633 enters (C before). Defaults unchanged.
- Defaults E6 seeds: as with G2 above. Random: 9 30 77 78 98 123 132 151 168 175 178 208 214 219 223 271 301 316 336 354 401 430 432 456 505 514 526 553 556 594 600 622 633 681 719 728 743 748 750 752 794 818 832 833 842 853 872 886 889 891 895 937 976.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 47 (17 26 39 47 77 78 98 108 118 145 175 220 226 244 336 357 397 402 424 444 446 459 511 529 547 584 600 606 616 651 681 719 720 757 768 773 779 780 794 844 892 915 937 951 961 967 992), random 44 (9 17 30 48 55 65 71 92 98 132 151 164 223 244 271 273 274 282 336 407 408 446 483 499 505 526 556 579 594 600 654 699 719 727 736 752 768 824 842 886 915 916 937 981).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 34 on `j18` (cleared 11: 59 178 348 356 377 518 589 621 683 736 850; moved out 9: 123>F 224>E3 322>E3 579>E2 592>E8 629>E9 734>E7 778>E8 786>E8; new 33: 26<C 39<clean 47<E9 77<E2 78<E3 98<E2 108<E5 175<E1 220<F 226<E1 244<E5 444<clean 446<E5 459<clean 511<E5 529<E4 600<E3 606<clean 616<clean 651<clean 681<clean 720<E2 757<F 768<E5 773<E5 779<clean 780<clean 794<clean 844<E5 892<clean 915<clean 967<clean 992<clean), 43 on `i26` (cleared 0; moved out 3: 181>E8 322>E3 678>E2; new 7: 77<E4 529<clean 720<E2 773<E4 844<E4 892<E4 915<clean); random: 53 on `j18` (cleared 27: 77 78 123 175 178 208 214 219 301 316 354 401 432 456 514 622 728 743 748 750 794 832 833 853 872 891 976; moved out 8: 168>E2 430>E2 553>E5 633>E2 681>E2 818>E2 889>E2 895>C; new 26: 17<clean 48<clean 55<clean 65<E1 71<clean 92<F 164<C 244<clean 273<clean 274<E2 282<clean 407<E1 408<clean 446<C 483<C 499<E1 579<E9 654<E7 699<clean 727<clean 736<clean 768<clean 824<C 915<C 916<clean 981<clean), 46 on `i26` (cleared 0; moved out 2: 681>E2 698>E5; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears random 9 48 71 273 274 407 916 and defaults 26 78 424 459 779 780 794 992, adds random 72 214 354 401 475 622 773 794 853 880; mixture clears random 9 30 48 71 164 273 274 407 768 916 and defaults 26 175 529 779 794 915 992, adds random 152 175 214 257 401 475 622 673 794 and defaults 364 401 518 673.

**On the side branch `claude/start-mixture` (t8: junction start (m'); `437aa3f4`, campaign `m05`): defaults 40 (17 26 71 77 78 98 108 118 145 164 175 226 228 244 336 357 397 402 431 444 446 518 547 556 584 600 606 616 651 678 719 720 773 844 892 915 937 951 961 967), random 42 (17 55 65 92 98 132 151 173 223 244 271 282 336 408 446 483 499 526 556 563 600 613 622 629 633 654 699 707 719 727 736 752 773 794 824 842 853 886 915 937 951 981).** Review "Junction start (m'): the pressure-balanced mixture (2026-10-12)".
- Against `k02` (nearest) and `k11` (strongest): defaults: 47 on `k02` (cleared 7: 424 459 529 779 780 794 992; moved out 7: 39>E5 47>E9 220>E8 511>E5 681>E5 757>F 768>E5; new 7: 71<E10 164<F 228<E2 431<F 518<clean 556<E8 678<E2), 38 on `k11` (cleared 1: 529; moved out 3: 220>E8 681>E5 757>F; new 6: 26<clean 78<clean 164<E5 228<E2 518<clean 556<C); random: 44 on `k02` (cleared 9: 9 30 48 71 164 273 274 407 916; moved out 4: 505>E5 579>E9 594>C 768>E2; new 11: 173<clean 563<E3 613<E8 622<clean 629<C 633<E2 707<E9 773<clean 794<clean 853<clean 951<E8), 49 on `k11` (cleared 8: 72 164 214 354 401 475 561 880; moved out 3: 430>E2 594>C 768>E2; new 4: 173<clean 613<E8 633<E7 707<E9).
- Every new seed that was clean before holds alone in E6 (`m06`). Random 30, cleared here, is the gate world of (m') (`JunctionStartMixtureTest`).
- On the side branch `claude/start-pressure` (t9, `5999da45`, campaign `n02`; review "Junction start at the balance pressure (2026-10-13)"): **defaults 42** (26 29 71 78 98 108 118 123 145 173 175 228 244 271 336 357 364 397 402 431 444 446 518 547 556 584 600 606 616 651 678 719 720 773 839 844 892 937 951 961 967 992), **random 30** (19 65 92 118 151 173 188 244 257 286 336 430 460 499 529 556 594 600 633 681 719 752 768 842 853 886 915 916 937 951). Against `m05`: defaults cleared 1 (17), moved out 4 (77>E5 164>E5 226>E8 915>E8), new 7 (29<E2 123<F 173<E5 271<E10 364<clean 839<C 992<clean); random cleared 19 (17 55 98 132 223 271 282 408 446 526 622 629 654 727 736 773 794 824 981), moved out 5 (483>C 563>E4 613>E8 699>E10 707>F), new 12 (19<F 118<C 188<C 257<clean 286<clean 430<E2 460<clean 529<clean 594<C 681<E2 768<E2 916<clean). Against `m14`: defaults cleared 2 (17 529), random new 4 (19 118 430 556). The new default worlds 364 and 992 were traced (section 5): 364 holds 41 s after its first interval on a gas path's rows, both starts alike after it; 992 is the first interval after a placement, whose junctions are re-minted as water because the drain-side gas column is not counted (option G) and two tanks tie to the ulp. Every worse seed holds alone (`n03`). Gate world: `JunctionStartMixtureTest`, random 17.
- The start-pressure variant (A119, every draining junction at its balance pressure; scratch, campaign `m14`) takes random E6 from 42 to 26 (19 cleared: 17 55 98 132 223 271 282 408 446 526 556 622 654 727 736 773 794 824 981) and adds defaults 364 424 529 992 (E6, all hold alone, `m15`): the stop point of the review's section 8.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column counted in the junction start; `c09dc06d`, campaign `o02`): defaults 44 (17 26 29 108 118 123 145 164 173 175 220 228 271 298 336 352 357 397 401 402 431 444 518 529 547 556 592 600 616 651 678 719 720 736 773 786 839 844 886 892 937 951 961 967), random 30 (19 65 92 98 118 151 173 188 244 257 286 336 460 499 511 529 594 600 633 681 752 768 824 842 853 886 915 916 937 951).** Review "Junction start: the drain-side gas column (2026-10-13)".
- Against `n02`: defaults 42 -> 44 (cleared 5: 244 364 446 584 992; moved out 4: 71>F 78>E4 98>E7 606>C; new 11: 17<clean 164<E5 220<E8 298<E9 352<clean 401<C 529<clean 592<E8 736<clean 786<E8 886<clean); random 30 -> 30 (cleared 2: 556 719; moved out 1: 430>E10; new 3: 98<clean 511<E8 824<clean). Against t9's scratch `n13`: defaults new 17 (clean there), 98 moved to E7; random new 98 (clean there) and 19 (F there).
- Every new seed holds alone in E6 (`o03`). World 736, traced (`o04`, `o05`): a level 1 atm water generator and a nitrogen tank 1 m below; without the column the water never moves (an exact tie), with it the water falls into the tank, and 3 ticks after a valve is placed the tank's vapour connection into the water-full junction fails (cap drops 1149 Pa against 957 790 Pa), this card's water-beside-gas mechanism. World 17: holds 331 s after its last block, the starts alike but for a few pascals. Gate world: `JunctionStartMixtureTest`, defaults 992 (cleared here).

**On the side branch `claude/keep-contents` (t11: a junction keeps its contents across a topology edit, option K; `078aa67f`, campaign `p03`): defaults 56 (17 26 29 39 98 107 118 123 145 173 175 183 220 228 241 246 271 298 336 352 397 402 424 431 444 446 475 529 547 563 592 600 606 616 621 651 654 660 678 720 736 758 768 773 786 788 842 844 886 915 932 937 951 961 967 981), random 27 (9 19 78 92 98 151 286 336 430 460 499 511 594 600 615 633 654 698 719 752 768 794 824 842 886 916 937).** Against `o02`: defaults 44 -> 56 (cleared 5: 108 357 401 518 719; new include 107 183 241 246 424 446 621 654 660 758 842 from clean); random 30 -> 27 (cleared 9: 65 173 188 244 257 529 681 853 915; new 9 78 615 654 719 794 from clean). Traced: defaults 246 (a cold restart of an island the edit did not change, at a near-zero-drive counterflow) and 107 (water displacing a kept gas junction within the first step). Variant strict (`p09`): defaults 47, random 21. Review "A junction keeps its contents across a topology edit (2026-10-13)".

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 50 (17 29 39 123 145 173 175 183 220 228 241 246 271 298 336 352 357 397 402 424 431 475 529 547 563 592 600 606 616 651 654 678 720 758 768 773 786 824 839 842 844 886 915 929 932 937 951 961 967 981), random 27 (19 92 98 107 118 151 173 244 286 336 430 456 460 499 511 529 594 600 633 660 719 752 794 842 886 916 937).** Against `o02`: defaults 44 -> 50 (cleared 6: 108 401 444 518 719 736; moved out 5: 26>C 118>E2 164>C 556>E8 892>E4; new 17: 39<E5 183<clean 241<clean 246<clean 424<clean 475<E5 563<E8 606<C 654<clean 758<clean 768<E5 824<E8 842<clean 915<E8 929<E4 932<C 981<E5); random 30 -> 27 (cleared 7: 65 188 257 681 824 853 915; moved out 2: 768>E2 951>E8; new 6: 107<clean 430<E10 456<clean 660<clean 719<clean 794<clean). Against `p03` (K): defaults 56 -> 50 (cleared 6: 107 444 446 621 660 736; moved out 4: 26>C 98>E2 118>E2 788>C; new 4: 357<clean 824<E5 839<C 929<E4); random 27 -> 27 (cleared 5: 9 78 615 654 824; moved out 2: 698>E10 768>E2; new 7: 107<E8 118<F 173<clean 244<clean 456<E5 529<clean 660<clean).

---

## F: the solve does not finish inside its time budget (defaults 28, random 12; with E6 option (a): 7, 6)

**World (defaults seed 17, 9 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 6, 1, 0 | 101.3 kPa |
| Pipes | 5,1,0; 4,1,0; 3,1,0; 2,1,0 (upper row); 5,0,0; 4,0,0 (a loop under two of them); 2,0,0 | 0.05 m |
| Tank | 2, 0, -1 | nitrogen, 101.3 kPa, fed through its side |

A generator feeds a pipe row with a small loop in it, which drops one block into the side of a tank.

**Expectation.** Water falls 1 m into the tank and compresses its nitrogen. The tank ends at about 111 kPa (101.3 kPa plus 1 m of water) holding about 90 kg of water (estimate; the gas is squeezed to about 0.91 m3). Most of that within a minute.

**What the solver did.** The first slice after the last block ran out of its 2 s wall budget ("wall deadline").

**Cause (unconfirmed for this world).** A budget hold is a step failure that spends the budget before the step halving runs out, or a slice that converges but costs more than 2 s. With option (a) of E6, 13 of 25 default budget holds cleared when measured, so most default budget holds are E6 in disguise. Budget holds depend on wall time; this machine now runs two Gradle jobs at once, so a budget seed must be replayed alone before it is counted.

**Proposition.** No fix of its own (a budget is never raised). It follows E6. The expensive random worlds (crude, methane, compressors) are a solve-cost question outside this list.

**On `ig10` (t17): defaults 6 (156 298 563 622 788 929), random 7 (77 246 431 547 678 707 788).** Alone (`ig12`): defaults 929 clean, 622 E8, the others budget again; random all budget again (the slow islands are crude two-phase junctions, D44's family).

**Seeds.** Defaults: 17 23 71 72 84 89 96 108 113 181 208 301 356 397 431 511 514 622 698 705 768 829 833 896 911 976 994 1000. Random: 4 71 116 118 298 441 556 579 757 839 844 922.

**With option (a) of E6 (side branch `claude/e6-row-scale`, campaign `g32`): defaults 7, random 6.**
- Defaults, of 28: 7 cleared (23 84 96 514 698 976 1000); 14 now fail inside the budget in a step class (C 301 431; E2 829; E4 181 705 768; E5 108 511 896; E6 17 208 356 397; E8 622); 7 remain F (71 72 89 113 833 911 994); no new F seed.
- Random, of 12: 4 cleared (71 757 839 844); 3 moved (441 E2, 556 E6, 579 E9); 5 remain (4 116 118 298 922); 1 new (311, was E2).
- This confirms the card's reading: 21 of the 28 default budget holds were step failures that spent the budget. With the step solve converging or failing early, they clear or show their own class. The 7 that remain have not been traced.

**With option (a) and G2 (campaign `h22`, every budget seed replayed alone with one worker, `h21`, `h24`): defaults 11, random 4.**
- Defaults: 71 72 89 113 833 911 994 (the 7 of `g32`) and 220, 431 (C before), 705 (E4 before), 757 (clean before) hold on their budget alone. Seeds 1, 622 and 792 end F in the campaign but E8 alone.
- Random: 116 118 298 922 hold on their budget alone. In the campaign 4, 19, 23, 92, 244, 311 and 431 also ended F; alone they are C (4, 19, 23), E8 (92), E2 (311), E6 (431), and 244 is clean.
- G2 adds cost: a vessel whose lag passes 1e-11 is closed by a small Newton of its own at every reconstruction (on the extreme grid at 5 s, 173 closures beside 154 network solves). The summed per-seed time rose 20 to 24 % (not timing-grade: two Gradle lanes). The four new default budget seeds are consistent with that cost; none was traced.

**With the G2 follow-up (campaign `j18`, budget seeds replayed alone with one worker, `j19`): defaults 10 alone (12 in the campaign), random 4 alone (8).**
- Defaults alone: 71 72 89 113 220 431 705 833 911 994. Seeds 1 and 757 are E8 alone.
- Random alone: 116 118 298 922. In the campaign 4, 92, 311 and 431 also ended F; alone they are C, E8, E2 and E6.
- **The four G2 budget seeds traced (A100):** they are step failures, not closure cost. With the budget lifted: 220 C (the water-floor wait it had without G2, reached after 1024 attempts instead of 330), 431 E6, 705 E8 (a nitrogen bubble in a water junction refused by the gate, the trace family), 757 E8. Each spends 12 to 70 one-iteration closure Newtons in its whole run, beside 1700 to 4600 network solves; the slice that spends the budget runs to the substep limit at 2 to 4 ms per attempt. Smallest world: defaults seed 705, 11 blocks (review, G2 follow-up, section 5).

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 11 (72 123 156 164 431 450 563 705 833 994 1000), random 7 (246 279 431 678 757 788 829).** Review "Integration of R1 and the junction start rule (2026-10-12)". Replayed alone with one worker (`k03`): defaults all 11 hold on the budget again; random 246, 678, 788 hold on the budget, 279 and 829 hold in E8, 431 in C, 757 in E6.
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 12 on `j18` (cleared 4: 1 89 113 911; moved out 3: 71>E10 220>E6 757>E6; new 6: 123<E6 156<E4 164<E9 450<C 563<E1 1000<clean), 10 on `i26` (cleared 0; moved out 0; new 1: 705<E4); random: 8 on `j18` (cleared 1: 298; moved out 6: 4>E10 92>E6 116>C 118>C 311>C 922>E8; new 6: 246<E2 279<E2 678<E2 757<clean 788<C 829<E2), 3 on `i26` (cleared 0; moved out 0; new 4: 279<E2 678<C 757<C 829<E4).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears defaults 72 1000; mixture clears defaults 1000, adds random 123 298 (budget seeds alone: k13, k17).

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 6 (71 156 705 757 833 994), random 5 (246 431 678 707 788).** Against `n02`: defaults 11 -> 6 (cleared 3: 246 424 450; moved out 3: 116>E4 563>E8 929>E4; new 1: 71<E6), random 7 -> 5 (moved out 2: 651>E9 698>E7). Every one holds on the budget again alone (`o03`).

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 6 (71 116 705 833 892 994), random 8 (118 298 311 431 651 678 788 922).** Against `o02`: defaults cleared 156 (the gate world), moved out 757>E2, new 116<E4 892<E6; random moved out 246>E2 707>C, new 118<E6 298<E2 311<E2 651<E9 922<C. Every one holds on the budget again alone (`p11`; defaults 116 holds in E8 alone).

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 4 (71 116 705 994), random 5 (246 311 431 678 788).** Against `o02`: defaults 6 -> 4 (cleared 1: 156; moved out 2: 757>E2 833>E5; new 1: 116<E4); random 5 -> 5 (cleared 0; moved out 1: 707>E9; new 1: 311<E2). Against `p03` (K): defaults 6 -> 4 (cleared 0; moved out 2: 833>E5 892>E4; new 0); random 8 -> 5 (cleared 0; moved out 4: 118>E6 298>E2 651>E9 922>C; new 1: 246<E2).

**On the side branch `claude/rest-resolution` (t16: the gauge's rest, D42/D43; `f2b6a987`, campaign `z18`): defaults 5 (156 579 757 833 994), random 4 (246 431 678 788).** Against `o02`: defaults 6 -> 5 (cleared 2: 71 705; moved out 0; new 1: 579<E9); random 5 -> 4 (cleared 0; moved out 1: 707>C; new 0). Every one holds on its budget again alone (`z21`). `o02`'s eleven budget seeds replayed alone on the base code (`z10`) all hold on their budget; of them defaults 705 is a tie (the held interval started at 0.03 Pa, the solver stepping an approach to rest at roundoff until the budget ran out) and defaults 71 an approach to rest (250, 89, 72, 26 Pa at its last interval starts): both clear with the gauge.

---

## E5: the step solve stalls (defaults 18, random 8)

**World (defaults seed 12, 7 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, 3, 1 | 101.3 kPa |
| Pipes | 0,2,1; 0,1,1; 0,1,0 (junction, on the tank's top face); 0,1,-1; 0,0,-1 (on the tank's north side) | 0.05 m |
| Tank | 0, 0, 0 | nitrogen, 101.3 kPa |

A generator three blocks up feeds a junction on top of a tank; from the junction a second pipe goes round into the tank's side.

**Expectation.** Water falls about 3 m into the tank through the top and the side. The tank ends at about 130 kPa (3 m of water) holding about 220 kg of water, its gas squeezed to about 0.78 m3 (estimate). Mostly within a minute or two.

**What the solver did.** The tank had taken 7.3 kg at 105 kPa. Then every step size stalled: no correction reduced the error, which stayed at 6.09 on the junction's water-vapour equation (the equation that sets the water vapour in a gas phase to its saturation pressure).

**Cause (confirmed by the trace, `d13-trace-defaults-12-E5`).** The junction is almost pure water but still holds a trace of gas (1e-10 m3 of nitrogen from the tank, which the flow washes out). Its water vapour is at 0.2 % of saturation, so its water-vapour equation is far off, and every correction that fixes it moves the vanishing gas phase by more than the phase holds. A phase that is disappearing gives the solver an equation it cannot satisfy smoothly.

**Proposition.**
- (a) A phase flush: a phase below a small share of its node is removed at the accepted step and booked, the phase counterpart of the trace flush (D16).
- (b) The phase-set step of the review's group B section 3: a node's phases for a pass are what its inventory and its inflow form, and a phase with no amount has no equations.
- (c) Resolve traces relative to their own size in a junction's mixing equations.
- Recommended: (b), measured before a decision; it would also address E2, E3, E4 and E10. References moved: not measured.
- **Group C (D34, implemented).** The owner chose a fold in the flash, which is (a) without the booking: a phase below 1e-9 of its node is put in the node's other phase. It cleared random 156 and 705 (a nitrogen tank whose 1e-12 crude entry trace condensed as a liquid of 2e-12 to 4e-12 of the tank). It cannot clear this world: the junction's trace gas is nitrogen, and the model's free water has no place for it (A84). What would: a dissolved-gas amount in free water (a model change), or a booked removal of a trace phase that has no receiving phase (D16's mechanism at the phase share, against "nothing booked"); review, group C, section 4.

**On `ig10` (t17): defaults 0, random 1 (629, a crude two-phase junction's fugacity row; minimized to 13 blocks it holds on `o02`'s code too).**

**Seeds.** Defaults: 12 39 77 183 188 224 228 311 333 371 387 435 526 547 665 681 792 844. Random: 19 257 354 681 807 829 854 1000.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 9 (138 157 301 311 316 475 559 847 926), random 3 (553 698 807).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 15 on `j18` (cleared 0; moved out 11: 6>E3 108>E6 116>E2 173>E8 244>E6 446>E6 511>E6 768>E6 773>E6 844>E6 896>E8; new 5: 157<E4 301<C 316<clean 475<E1 847<C), 11 on `i26` (cleared 1: 698; moved out 2: 788>C 981>E10; new 1: 926<E4); random: 2 on `j18` (cleared 1: 854; moved out 0; new 2: 553<E6 698<C), 4 on `i26` (cleared 0; moved out 2: 547>E2 786>E2; new 1: 698<E6).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears defaults 157; mixture adds random 992 and defaults 821 853.

**On the side branch `claude/start-mixture` (t8: junction start (m'); `437aa3f4`, campaign `m05`): defaults 13 (39 138 173 301 311 316 475 511 559 681 768 847 926), random 5 (505 553 807 815 818).** Review "Junction start (m'): the pressure-balanced mixture (2026-10-12)".
- Against `k02` (nearest) and `k11` (strongest): defaults: 9 on `k02` (cleared 1: 157; moved out 0; new 5: 39<E6 173<E8 511<E6 681<E6 768<E6), 13 on `k11` (cleared 0; moved out 1: 164>E6; new 1: 681<E6); random: 3 on `k02` (cleared 0; moved out 1: 698>F; new 3: 505<E6 815<clean 818<E2), 4 on `k11` (cleared 0; moved out 1: 698>F; new 2: 815<clean 818<E2).
- Random 815 holds alone in E5 (`m06`). The default worlds keep the nitrogen bubble in a water-only junction (D37: they wait for the gas-solubility work).

**On the side branch `claude/start-pressure` (t9: every draining junction starts at its balance pressure; `5999da45`, campaign `n02`): defaults 13 (77 138 164 301 311 316 475 511 559 768 847 926 981), random 4 (553 807 815 818).** Review "Junction start at the balance pressure (2026-10-13)".
- Against `m05` and `m14`: defaults: 13 on `m05` (cleared 1: 681; moved out 2: 39>C 173>E6; new 3: 77<E6 164<E6 981<E10), 11 on `m14` (cleared 0; moved out 0; new 2: 164<F 981<E10); random: 5 on `m05` (cleared 1: 505; moved out 0; new 0), 5 on `m14` (cleared 1: 629; moved out 0; new 0).
- The worse default world 824 (E8 on `n02`, traced) holds on this card's mechanism: at its steady state a water-full junction carries 1e-22 mol of nitrogen from a tank's bulk port.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 14 (39 77 138 301 311 316 475 559 717 768 847 853 926 981), random 6 (547 553 629 807 815 829).** Against `n02`: defaults 13 -> 14 (moved out 2: 164>E6 511>E2; new 3: 39<C 717<clean 853<clean), random 4 -> 6 (moved out 1: 818>E2; new 3: 547<E2 629<clean 829<E2). Against `n13`: unchanged. New seeds hold alone (`o03`).

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 10 (77 138 224 301 311 316 717 824 916 926), random 5 (279 456 553 807 815).** Against `o02`: defaults cleared 853, moved out 39 475 768 981 to E6 and 559 847 to C, new 224<E3 824<E8 916<clean; random cleared 629, moved out 547>E2 829>C, new 279<E2 456<clean.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 12 (77 138 224 301 311 316 717 833 847 850 916 926), random 5 (279 547 553 807 815).** Against `o02`: defaults 14 -> 12 (cleared 1: 853; moved out 5: 39>E6 475>E6 559>C 768>E6 981>E6; new 4: 224<E3 833<F 850<E8 916<clean); random 6 -> 5 (cleared 1: 629; moved out 1: 829>C; new 1: 279<E2). Against `p03` (K): defaults 10 -> 12 (cleared 0; moved out 1: 824>E6; new 3: 833<F 847<C 850<C); random 5 -> 5 (cleared 0; moved out 1: 456>E6; new 1: 547<E2).

**Traced on `q04` (t13, logs `y10`-`y12`).** Defaults 926 stalls on a water junction's saturation row (7e-17 mol of nitrogen) and 138 on a water junction's continuity row at 6.7e-8: card E6-T's mechanism (926 clears with 60 iterations, 138 does not). Random 807 is the same (a water junction with 1.6e-16 mol of nitrogen); random 547 stalls on a two-phase crude junction. The flow carry clears none.

**On `z16` (t14, the carried gas of D41).** Defaults 14 -> 0, random 6 -> 4 (430 547 553 629). Every world-default seed of the class cleared or moved (77 138 301 311 316 475 559 768 847 853 926 981 cleared; 39 to F, 717 to E6): the class was card E6-T's mechanism in world defaults. The sampled worlds: defaults 926 and 138 clean, random 807 clean; random 547 holds (a two-phase crude junction, D44).
**On the side branch `claude/rest-resolution` (t16: the gauge's rest, D42/D43; `f2b6a987`, campaign `z18`): defaults 13 (39 77 301 316 475 559 717 768 788 847 853 926 981), random 3 (547 815 829).** Against `o02`: defaults 14 -> 13 (cleared 2: 138 311, both ties; moved out 0; new 1: 788<C); random 6 -> 3 (cleared 3: 553 629 807; 553 and 807 ties, 553 behind an open valve; moved out 0; new 0). Random 807 was this card's water-trace world: it held at the end of its approach to rest (1.7e-4 Pa), so the gauge stops it before the trace's saturation row fails; the trace mechanism itself (card E6-T) is unchanged. The random crude worlds of this class are saved for the thermodynamics work (D44).

---

## E1: the check at the start of an interval does not converge (defaults 15, random 9)

**World (defaults seed 45, 9 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator A | -4, 0, 0 | 101.3 kPa |
| Pipe | -3, 0, 0 | 0.05 m |
| Inline filter | -2, 0, 0 | facing west |
| Pipes | -1,0,0; -1,-1,0; -1,-2,0 (junction); -1,-2,1 | 0.05 m |
| Water generator B | -1, -3, 0 | 101.3 kPa, one block below the junction |
| Tank | -1, -2, 2 | nitrogen, 101.3 kPa, fed through its side |

Generator A feeds water through an inline filter and down two blocks into a tank; generator B stands one block below the junction.

**Expectation.** Water from generator A falls 2 m through the filter into the tank. The tank ends at about 121 kPa holding about 160 kg (estimate). Generator B is below the junction and cannot push up against the column, so it stays shut.

**What the solver did.** An island with a filter checks at the start of every interval whether a filter or a connection must close (a flow-only solve of the network). That check stopped at its iteration limit with 9.6e-8 left on the equation of generator A's connection to the filter. A failed check holds the island.

**Cause (unconfirmed).** The junctions start as the tank's nitrogen at 101.3 kPa, level with generator A, so its connection starts at exactly zero drive: the same zero-drive scale jump as E6, here inside the start-of-interval check. Earlier measurements put two more mechanisms in this class: a far start that needs 21 iterations where 20 are allowed, and a junction whose composition in the check is its starting guess.

**Proposition.**
- (a) Option R1: a failed start-of-interval check declares no closure, and the steps (which run the same closure rules) decide. Measured on an earlier commit: 25 random and 19 default seeds cleared, none worse, classes D, E1 and G gone; campaign time about four times longer, because the same islands then fail in their steps. References: not measured on the current code.
- (b) Fix the E6 scale (card E6).
- Recommended: (a), which the review has recommended since the E1 section.

**On `ig10` (t17): none.**

**Seeds.** Defaults: 45 175 214 226 352 475 485 541 563 717 826 842 886 895 973. Random: 15 45 159 206 236 407 485 541 563.

**With R1 on the E6 rule and the junction start rule (owner decision D37; branch `claude/e1-e4-measure`, campaign `i26`): defaults 0, random 0.** Review "Hold class E1, R1 on the E6 rule (2026-10-12)".
- The card world (defaults seed 45) already runs clean on the E6 rule without R1 (`i01`); the full seed was cleared by option (a).
- On the E6 rule the class was 9 defaults / 12 random (`g32`). R1 removed it: 5 seeds cleared (random 6, 866; defaults 524 and 902 of class D, 842), none worse; the others now hold in their steps (mostly E6; E2, E4, E5, C; defaults 214 and 563 on the budget, alone too).
- Under the start rule R1 still matters: without it E1 returns (7 defaults / 10 random) and R1 clears 3 random and 4 default seeds on top (`i33`).
- Gate test `StartCheckRateFailureTest` (random 866 for R1 alone, random 122 under the start rule).

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 0, random 0.** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 10 on `j18` (cleared 4: 214 541 717 842; moved out 6: 175>E6 226>E6 475>E5 563>F 788>C 895>E4; new 0), 0 on `i26` (cleared 0; moved out 0; new 0); random: 12 on `j18` (cleared 6: 6 159 236 485 866 945; moved out 6: 45>C 65>E6 206>C 407>E6 499>E6 563>E3; new 0), 0 on `i26` (cleared 0; moved out 0; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): none in any form.

---

## E4: the step's material bookkeeping fails its check (defaults 10, random 14)

**World (defaults seed 6, 10 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | -1, -3, 1 | 101.3 kPa, three blocks below the pipes |
| Pipes | -1,-2,1; -1,-1,1; -1,0,1 (junction); -1,0,2 (junction); 0,0,1 (junction); 0,0,3 | 0.05 m |
| Pump | 0, 0, 2 | facing north, 0.01 m3/s, 500 kPa |
| Tank | -1, 0, 3 | nitrogen, 101.3 kPa |
| Void | 0, 0, 0 | 101.3 kPa |

A pump between a tank and a void, with a generator three blocks below the network.

**Expectation.** The pump's suction reaches only the tank's nitrogen, and a liquid pump refuses gas, so it stays shut. The generator cannot lift water 3 m. The tank and the void are both at 101.3 kPa at the same height. Nothing flows: rest from the start.

**What the solver did.** Every step size was refused by the check that compares the material a step moved with what the equations said (the "equation gate"), at 3.46.

**Cause (unconfirmed).** The junctions start as the generator's water (the generator has the lowest identity), so pipes that can hold only gas here start full of water at 101.3 kPa at every height. The step then moves this water and meets nitrogen from the tank; the bookkeeping of the phases that appear does not match the equations. The node was not traced (the failure is after the solve, not in it).

**Proposition.**
- (a) The phase-set step (card E5, (b)).
- (b) Start a junction from what can reach it rather than from the island's first boundary (the junction mint; the mixed and supply-first starts were measured and each broke as many worlds as it cleared).
- Recommended: (a), measured before a decision.
- **Group C (D34, implemented).** The trace fold cleared random 762 (a junction holding the crude entry trace as a liquid of 3.7e-11, gate world of `TracePhaseFoldTest`) but not this world, where the cause is the junction's start, not a trace phase.

**On `ig10` (t17): none (`o02` 0 / 7).**

**Seeds.** Defaults: 6 78 157 419 437 613 660 728 859 916. Random: 131 139 199 219 394 432 438 468 470 511 714 719 734 815.

**With the junction start rule, option (b) of this card (owner decision D37: "option (c)" of the brief, measured before a decision; branch `claude/e1-e4-measure`, campaign `i26`, on top of R1): defaults 23, random 5.** Review "Hold class E4, the junction start from reachability (2026-10-12)".
- The card world (defaults seed 6) already runs clean on the E6 rule (`i02`). With the rule its junctions start as the tank's nitrogen, as the expectation above says (`i14`).
- The rule: a junction starts as the stream of the nearest supplier (a generator, or a tank holding fluid) that is driven into it (strictly above the lowest pressure it drains into, its own column counted, the drain's not), crossing valves above their set point and pumps or compressors that accept the stream; a pumped stream comes first, a pocket goes to the strongest supplier, equal ones to the denser fluid; a junction nothing reaches holds the fluid standing highest among the boundaries it touches.
- E4 random 12 -> 5 (11 cleared: 199 219 357 394 432 438 468 470 714 734 815; 669 new); defaults 23 -> 23 (12 cleared: 325 354 394 412 419 438 526 529 541 561 750 859; 7 new: 247 320 517 654 718 892 919).
- Clean overall: random 835 -> 884, defaults 856 -> 866 (R1 -> rule; against `g32` 833 / 853). The variant "strongest supplier first" everywhere measures 893 / 881 and is the recommendation (review, stop item S1).
- The new E4 worlds are the trace family: a junction started as water at zero drive beside gas (traced: defaults 320, 6 blocks, equation gate 3.8e-6). They wait for the phase-set step of card E5 (b).
- Gate test `JunctionStartStateTest` (defaults 438, 6 blocks). If the rule is taken, the two E6 gate worlds of `ConnectionRowScaleTest` need rebuilding: the rule removes the nitrogen-against-water start they were built on.
- Seeds with the rule. Defaults: 77 247 320 352 437 517 556 592 613 621 654 669 705 718 773 824 844 872 892 895 919 926 929. Random: 131 511 669 803 829.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 5 (669 718 824 872 895), random 1 (511).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 6 on `j18` (cleared 1: 354; moved out 4: 156>F 157>E5 352>E8 529>E6; new 4: 669<E3 718<clean 824<E8 895<E1), 23 on `i26` (cleared 5: 247 320 517 621 919; moved out 13: 77>E6 352>E8 437>E8 556>E8 592>E8 613>E8 654>E2 705>F 773>E6 844>E6 892>E6 926>E5 929>E2; new 0); random: 0 on `j18` (cleared 0; moved out 0; new 1: 511<E10), 5 on `i26` (cleared 1: 669; moved out 3: 131>E8 803>E8 829>F; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears defaults 718; mixture adds random 815.

**On the side branch `claude/start-mixture` (t8: junction start (m'), the pressure-balanced mixture; `437aa3f4`, campaign `m05`): defaults 5 (669 763 872 895 929), random 1 (511).** Review "Junction start (m'): the pressure-balanced mixture (2026-10-12)".
- Against `k02` (nearest) and `k11` (strongest): defaults: 5 on `k02` (cleared 2: 718 824; moved out 0; new 2: 763<clean 929<E2), 4 on `k11` (cleared 1: 824; moved out 0; new 2: 763<E8 929<E2); random: unchanged (511).
- Defaults 763 holds alone in E4 (`m06`). The class keeps its mechanism (the trace family, card E5 (b)); (m') neither adds to it nor removes it.

**On the side branch `claude/start-pressure` (t9: every draining junction starts at its balance pressure; `5999da45`, campaign `n02`): defaults 4 (437 669 763 895), random 1 (563).** Review "Junction start at the balance pressure (2026-10-13)".
- Against `m05` and `m14`: defaults: 5 on `m05` (cleared 1: 872; moved out 1: 929>F; new 1: 437<E8), 4 on `m14` (cleared 0; moved out 1: 929>F; new 1: 437<E8); random: 1 on `m05` (cleared 0; moved out 1: 511>E8; new 1: 563<E6), 1 on `m14` (unchanged).

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 7 (78 116 437 669 763 895 929), random 0.** Against `n02`: defaults 4 -> 7 (new 3: 78<E6 116<F 929<F), random 1 -> 0 (moved out 563>E8). Against `n13`: defaults new 116 (F there).

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 4 (78 178 437 929), random 0.** Against `o02`: defaults cleared 669, moved out 116>F 763>E8 895>E8, new 178<clean.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 4 (78 178 437 892), random 1 (563).** Against `o02`: defaults 7 -> 4 (cleared 1: 669; moved out 4: 116>F 763>E8 895>E8 929>E6; new 2: 178<clean 892<E6); random 0 -> 1 (cleared 0; moved out 0; new 1: 563<E8). Against `p03` (K): defaults 4 -> 4 (cleared 0; moved out 1: 929>E6; new 1: 892<F); random 0 -> 1 (cleared 0; moved out 0; new 1: 563<E8).

---

## C: typed waits on the material data (defaults 9, random 72)

A typed wait means the solver reached a state the material data does not cover, reported it by name, and waits for the network to change. This is legitimate only when the physical state really is outside the data. Two examples:

**Legitimate (random seed 12, 5 blocks).**

| Block | Position | Setting |
|---|---|---|
| Crude generator | 0, 3, 1 | two-phase light crude, 294.85 K, 232 kPa |
| Pipes | 0,2,1; 0,1,1; 0,1,2 | 0.05 m |
| Tank | 0, 1, 3 | nitrogen, 101.3 kPa |

Crude at 295 K and 232 kPa flows two blocks down into a nitrogen tank at 101.3 kPa. The crude's light ends flash as it expands to 101 kPa, and flashing cools it. The data for the lightest crude cut starts at 293.15 K. The flash wants to go below that, and the solver reports "crude_pc01 at 293.1499999999999 K is below 293.15 K". The state really is outside the data. Fix: extend the cut's data downwards, not the solver.

**Not legitimate (defaults seed 26, 8 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, 0, 0 | 101.3 kPa |
| Pipes | 0,0,-1; 0,-1,0; 0,-1,-1; 0,-2,0; 1,-2,0; 1,-2,1 | 0.05 m |
| Tank | 2, -2, 0 | nitrogen, 101.3 kPa |

Water falls 2 m into a tank, which should end near 121 kPa. The solver reported "the package envelope at 2.7e55 Pa is above 2000000 Pa" in a junction. No real state reaches 2 MPa in a world-default world (the strongest device adds 500 kPa). This is a trial point of a failing solve that left the data, reported as a data wait.

**Split of the class.** Random: most are legitimate (crude or methane cooling below their data floor, nitrogen above 2 MPa behind a compressor or a strong pump, water below its triple point as a vented gas expands, which decision D14 accepts). Defaults: 1 of the 10 is a water triple-point wait (273.1599 K, D14); the other 9 report nitrogen or the envelope at 2.06 MPa to 1e55 Pa, which world defaults cannot reach.

**Proposition.** Report a data violation reached only by a Newton trial as the numerical hold it is, and keep the typed wait for committed or boundary states. Options: (a) classify by where the violation arose (a trial versus a committed state); (b) leave as is. Recommended (a); no reference moves (status text only).

**On `ig10` (t17): defaults 3 (164 246 990), random 15 (12 26 59 206 241 444 483 504 518 779 787 889 908 922 978).** World defaults 246 (worse; an adiabatic tank at the top of a draining network cools to 273.16 K, reached through t15's rules) and 164 (nitrogen drawn to 94 Pa at a capped connection; holds on `o02`'s code when minimized) are physical domain edges, correct as typed waits.

**Seeds.** Defaults: 26 161 271 336 773 788 915 922 990. Random: 12 47 59 65 72 77 78 92 98 101 122 151 161 164 168 173 175 188 208 214 228 241 284 286 315 336 372 401 424 431 446 450 456 475 483 504 509 514 518 547 594 621 622 633 651 654 683 698 707 708 717 750 763 767 779 787 788 824 832 833 842 847 859 891 895 896 908 915 961 976 978 994.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 6 (12 96 161 788 932 990), random 30 (12 15 26 35 45 47 59 116 118 188 206 241 286 311 424 453 504 518 521 575 615 621 629 717 779 787 895 908 978 994).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 12 on `j18` (cleared 3: 371 435 922; moved out 7: 26>E6 301>E5 450>F 485>E2 696>E2 847>E5 981>E10; new 4: 96<clean 161<E2 788<E1 932<E8), 4 on `i26` (cleared 0; moved out 0; new 2: 96<clean 788<E5); random: 46 on `j18` (cleared 18: 19 23 101 139 161 170 173 284 315 444 450 475 509 767 847 859 896 992; moved out 11: 164>E6 446>E6 483>E6 541>E2 547>E2 651>E9 698>E5 707>E9 788>F 824>E6 915>E6; new 13: 15<clean 26<E2 45<E1 116<F 118<F 206<E1 286<clean 311<F 453<clean 575<E10 615<E7 629<clean 895<E6), 33 on `i26` (cleared 1: 853; moved out 2: 678>F 757>F; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears random 15 45 47 286 453 575 615 621 895 994 and defaults 12 96, adds random 19 444 767 and defaults 839; mixture clears random 15 47 286 615 621, adds random 767 and defaults 839.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 5 (606 788 829 932 990), random 15 (unchanged).** Against `n02`: defaults cleared 161, moved out 39>E5 401>E6, new 606<E6 788<E10 829<E2.

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 10 (164 322 459 559 829 839 847 850 990 1000), random 21 (12 26 59 94 108 206 219 241 444 483 504 505 518 621 707 779 787 829 889 908 978).** Against `o02`: defaults new 459 and 1000 from clean, 164 839 from E6, 322<E3 559<E5 847<E5 850<E8, moved out 606 788 932 to E6; random new 94 108 219 505 621 from clean (random 108 minimized: a nitrogen trial above 2 MPa in a pump pocket), 707<F 829<E5, moved out 922>F.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 9 (26 164 322 459 559 788 829 990 1000), random 17 (12 26 59 94 206 241 444 483 504 518 779 787 829 889 908 922 978).** Against `o02`: defaults 5 -> 9 (cleared 0; moved out 2: 606>E6 932>E6; new 6: 26<E6 164<E6 322<E3 459<clean 559<E5 1000<clean); random 15 -> 17 (cleared 0; moved out 0; new 2: 94<clean 829<E5). Against `p03` (K): defaults 10 -> 9 (cleared 0; moved out 3: 839>E6 847>E5 850>E5; new 2: 26<E6 788<E6); random 21 -> 17 (cleared 4: 108 219 505 621; moved out 1: 707>E9; new 1: 922<F).

**On the side branch `claude/rest-resolution` (t16: the gauge's rest, D42/D43; `f2b6a987`, campaign `z18`): defaults 4 (606 829 932 990), random 16 (12 26 59 206 241 444 483 504 518 707 779 787 889 908 922 978).** Against `o02`: defaults 5 -> 4 (cleared 0; moved out 1: 788>E5; new 0); random 15 -> 16 (cleared 0; moved out 0; new 1: 707<F). No class C seed of `o02` is a tie: every held island stands at a real drive (49 Pa to 272 kPa at the hold, probe `z01`).

---

## E2: the step solve's equations cannot be told apart (defaults 10, random 19)

**World (defaults seed 98, 6 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, 2, 0 | 101.3 kPa |
| Pipes | 0,1,0; -1,1,0 (junction); -1,2,0 (junction); -2,2,0 | 0.05 m |
| Tank | -2, 1, 0 | nitrogen, 101.3 kPa, fed through its side and its top |

A generator one block above a tank feeds it through its side and through its top by two paths.

**Expectation.** Water falls 1 m into the tank. The tank ends at about 111 kPa holding about 90 kg (estimate). Some of the tank's gas can rise back through the top path while water runs in; at the end it stays in the tank.

**What the solver did.** The tank had 20 kg at 108 kPa. Then every step size ended on a singular Jacobian. The worst equation at the failure was the upper junction's mass balance (4.69).

**Cause (unconfirmed).** The upper junction is almost pure water with a gas trace of 1.6e-15 m3. When a phase is that small, the equations of how the components split between phases no longer depend on the split: shifting it changes nothing the equations can see, so they cannot be told apart. Measured on random seed 4 (review "Hold class E2"): the smallest singular value belonged to exactly such a uniform shift.

**Proposition.** The phase flush or the phase-set step (card E5). Recommended: the phase-set step, measured before a decision. **Group C:** the trace fold (D34) leaves this world held: its 1.6e-15 m3 gas trace is nitrogen in a water-only junction, a phase with no receiving phase (A84; card E5, options). Random E2 went from 18 to 19 seeds by class shifts, with none cleared and none worse.

**On `ig10` (t17): defaults 10 (29 94 163 328 358 485 499 727 823 978), random 7 (163 279 298 372 521 786 818; six of them saved crude cases, D44).**

**On `ec19` (t18, the start restated, rule (c), the primed falling drain): defaults 10 (94 163 328 485 499 606 727 823 978 990), random 8 (163 246 279 298 372 521 786 818).** Against `ig10`: defaults 358 cleared, 29 moved to F (E8 alone), 606 joined from E6 S, 990 from C; random 246 joined from F. World defaults 94, 823 and 978 hold as on `ig10` and were traced (A259, `ec15`): a compressor's suction junction whose only other connection is a void (which never supplies); the hold is a singular Jacobian at active-set pass 2 (read from the minimized 6-block world and its start print; the singular pass was not traced row by row). With the column dropped they ran clean on (c) + primed: their junction then started 11.2 Pa below the void's hydrostatic level, so rule (c) closed the void at the start; with the column it starts exactly at that level and (c) has no sign to read. Proposition for a later card: a mover whose suction has no supply closes at the start of a solve (its suction is a dead end, D18 for movers).

**Seeds.** Defaults: 98 118 163 328 358 696 823 839 889 929. Random: 26 108 163 244 246 274 279 311 342 358 399 435 521 678 786 818 907 941 967.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 23 (29 94 116 163 199 228 328 333 358 485 499 579 654 678 696 727 758 823 829 889 908 929 978), random 13 (163 168 430 541 547 561 633 681 786 818 839 889 1000).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 20 on `j18` (cleared 6: 526 665 750 752 839 933; moved out 4: 77>E6 98>E6 161>C 720>E6; new 13: 29<clean 94<clean 116<E5 333<clean 485<C 499<D 579<E6 654<clean 678<E7 696<C 727<D 758<A 908<clean), 21 on `i26` (cleared 0; moved out 1: 720>E6; new 3: 654<E4 678<E6 929<E4); random: 20 on `j18` (cleared 10: 108 342 358 372 399 435 441 815 907 941; moved out 7: 26>C 246>F 274>E6 279>F 678>F 829>F 967>E8; new 10: 168<E6 430<E6 541<C 547<C 561<clean 633<E6 681<E6 818<E6 839<clean 889<E6), 11 on `i26` (cleared 0; moved out 1: 279>F; new 3: 547<E5 681<E6 786<E5).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears random 168 541 839 1000 and defaults 758 889 908, adds random 372 and defaults 401; mixture clears random 561 839 1000 and defaults 758, adds random 372.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 13 (94 163 328 358 485 499 511 681 696 727 792 823 978), random 9 (163 279 298 311 372 521 786 818 854).** Against `n02`: defaults cleared 199 333, moved out 829>C, new 511<E5 681<clean 792<E8; random moved out 547>E5 829>E5, new 818<E5. New seeds hold alone (`o03`).

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 14 (94 163 328 358 485 499 511 681 696 727 757 792 823 978), random 9 (163 246 274 357 521 547 786 818 855).** Against `o02`: defaults new 757<F; random cleared 372 854, moved out 279>E5 298>F 311>F, new 274 855 from clean, 246<F 357<E8 547<E5.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 16 (94 98 118 163 328 358 485 499 511 681 696 727 757 792 823 978), random 8 (163 274 298 521 768 786 818 855).** Against `o02`: defaults 13 -> 16 (cleared 0; moved out 0; new 3: 98<E7 118<E6 757<F); random 9 -> 8 (cleared 2: 372 854; moved out 2: 279>E5 311>F; new 3: 274<clean 768<E6 855<clean). Against `p03` (K): defaults 14 -> 16 (cleared 0; moved out 0; new 2: 98<E6 118<E6); random 9 -> 8 (cleared 0; moved out 3: 246>F 357>E8 547>E5; new 2: 298<F 768<E6).

**Traced on `q04` (t13, logs `y10`-`y12`).** Defaults 792 and 681 are singular on a water junction's continuity row with a nitrogen trace of 1e-13 mol: card E6-T's mechanism. Random 521 and 786 are singular on crude nodes (a liquid crude junction, a two-phase crude tank): not E6-T. No sampled world clears with 60 iterations or the flow carry.

**On `z16` (t14, the carried gas of D41).** Defaults 13 -> 10, random 9 -> 8. Cleared: defaults 681, random 854; moved: defaults 511 to E6, 792 to C (a typed wait). The sampled worlds: defaults 681 clean, 792 C; random 521 and 786 hold (crude, D44). The ten world-default seeds left (94 163 328 358 485 499 696 727 823 978) were not traced; none was T on the sample.
**On the side branch `claude/start-flows` (t15, the committed end flows carried and one column rule, campaign `sf08`): defaults 11 (29 94 163 328 358 485 499 518 727 823 978), random 8 (163 279 298 311 372 521 786 818).** Against `o02`: defaults 13 -> 11 (cleared 696; moved out 511 > E4, 681 > E6, 792 > E8; new 29 < E6, 518 < E6); random 9 -> 8 (cleared 854). The traced E6-T worlds 792 and 681 leave E2 for E8 and E6; the crude worlds stay.
**On the side branch `claude/rest-resolution` (t16: the gauge's rest, D42/D43; `f2b6a987`, campaign `z18`): defaults 13 (94 163 328 358 485 499 511 696 720 727 792 823 978), random 8 (163 279 298 372 521 786 818 854).** Against `o02`: defaults 13 -> 13 (cleared 1: 681; moved out 0; new 1: 720<E6); random 9 -> 8 (cleared 1: 311, a tie: a chain of nine loops of junctions settling at 3e-4 Pa; moved out 0; new 0). The random crude worlds of this class are saved for the thermodynamics work (D44, header and card "D44").

---

## E8: an interval takes more than its allowed number of attempts (defaults 6, random 3)

**World (defaults seed 123, 6 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, 0, 0 | 101.3 kPa |
| Pipes | 0,0,1 (junction); 0,0,2; 0,1,2 | 0.05 m |
| Lower tank | 0, -1, 1 | nitrogen, 101.3 kPa, below the junction (top face) |
| Upper tank | 0, 2, 2 | nitrogen, 101.3 kPa, fed through its bottom face |

A generator feeds a junction that drops into one tank and rises to another two blocks up.

**Expectation.** Water falls 1 m into the lower tank. It ends at about 111 kPa with about 90 kg (estimate). The generator cannot lift water 2 m into the upper tank; the upper tank's gas ends at the lower tank's pressure (gas moves through the junction until the two agree).

**What the solver did.** 518 of 1024 attempts were refused inside one slice; each ended at the iteration limit just above tolerance (1e-9) on the generator's connection, which the solver closes and reopens ("boundary reopened"). At the hold the upper tank stood at 89 kPa and 281 K: it had sent gas down and cooled by expanding.

**Cause (unconfirmed).** The generator's connection sits near zero drive while the junction's pressure moves with the tanks' gas; the solver closes it, reopens it, and each attempt ends a hair above tolerance (the E6 behaviour at the edge of convergence).

**Proposition.** Follows E6. Recommended: decide E6 first and re-measure.

**On `ig10` (t17): defaults 2 (39 778), random 1 (803).**

**Seeds.** Defaults: 123 156 316 720 926 933. Random: 357 613 803.

**With R1 and the junction start rule (branch `claude/e1-e4-measure`, campaign `i26`): defaults 8, random 5.** The card E6 world's later hold (defaults seed 1, A92: the junction minted as dry nitrogen meeting water, E8 on the E6 rule) is gone: the junction now starts as water and the world runs its 120 slices clean (`i27`); full seed 1 is cleared. The card world above (defaults 123) is now a budget hold (F). Seeds with the rule. Defaults: 173 183 622 728 778 786 792 896. Random: 23 613 922 951 967. Not traced.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 14 (173 181 183 352 437 556 592 613 622 728 778 786 792 896), random 6 (131 613 803 922 951 967).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 10 on `j18` (cleared 0; moved out 2: 824>E4 932>C; new 6: 173<E5 352<E4 592<E6 778<E6 786<E6 896<E5), 8 on `i26` (cleared 0; moved out 0; new 6: 181<E6 352<E4 437<E4 556<E4 592<E4 613<E4); random: 4 on `j18` (cleared 1: 357; moved out 0; new 3: 922<F 951<E7 967<E2), 5 on `i26` (cleared 1: 23; moved out 0; new 2: 131<E4 803<E4).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest adds random 357 and defaults 763; mixture clears defaults 352 556, adds defaults 665.

**On the side branch `claude/start-pressure` (t9: every draining junction starts at its balance pressure; `5999da45`, campaign `n02`): defaults 14 (181 183 220 226 459 592 622 728 778 786 792 824 896 915), random 6 (131 357 511 613 803 967).** Review "Junction start at the balance pressure (2026-10-13)", section 5. (`m05`: defaults 12 (181 183 220 352 437 592 622 728 778 786 792 896), random 4 (131 357 803 967).)
- Against `m05` and `m14`: defaults: 12 on `m05` (cleared 1: 352; moved out 1: 437>E4; new 4: 226<E6 459<clean 824<clean 915<E6), 14 on `m14` (cleared 1: 96; moved out 1: 437>E4; new 2: 181<E9 915<E6); random: 4 on `m05` (cleared 0; moved out 0; new 2: 511<E4 613<E6), 6 on `m14` (unchanged).
- The two new default worlds were traced (both hold alone, `n03`). 824: a hold at the steady state, 580 s after the last block, on trace nitrogen in a water-full junction (card E5's mechanism). 459: 21 s after the last block, water reaches junctions that still hold nitrogen bubbles. In both, the two starts give the same states after the first interval to 6-9 digits.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 12 (1 181 214 563 622 629 728 778 824 850 896 915), random 6 (131 357 563 613 803 967).** Review "Junction start: the drain-side gas column (2026-10-13)".
- Against `n02`: defaults 14 -> 12 (cleared 3: 183 226 459; moved out 4: 220>E6 592>E6 786>E6 792>E2; new 5: 1<clean 214<clean 563<F 629<E9 850<clean); random 6 -> 6 (moved out 511>E6; new 563<E4). Against `n13`: defaults unchanged.
- World 1, traced (4 blocks, the E6 card's world: a level water generator and nitrogen tank, a void 1 m below the junction). With the column both the generator and the tank stand 11.2 Pa above the void's bound and the junction starts as their flow-weighted mixture (2 % nitrogen by mole, about 97 % gas by volume); without it they tie with the void exactly and the contact rule starts it as water. The run with the column holds 16 s later while water displaces the gas (476 "Backward-Euler boundary reopened" rejections), this card's family as in world 459 (t9). All new seeds hold alone (`o03`).

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 11 (1 181 214 556 622 665 728 763 778 895 896), random 7 (107 131 563 613 803 951 967).** Against `o02`: defaults cleared 629, moved out 563 915 to E6, 824>E5, 850>C, new 665<clean 556<E6 763<E4 895<E4; random new 107<clean 951<E6, moved out 357>E2.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 10 (1 181 214 556 622 728 763 778 895 896), random 6 (131 357 613 803 951 967).** Against `o02`: defaults 12 -> 10 (cleared 1: 629; moved out 4: 563>E6 824>E6 850>E5 915>E6; new 3: 556<E6 763<E4 895<E4); random 6 -> 6 (cleared 0; moved out 1: 563>E4; new 1: 951<E6). Against `p03` (K): defaults 11 -> 10 (cleared 1: 665; moved out 0; new 0); random 7 -> 6 (cleared 0; moved out 2: 107>E6 563>E4; new 1: 357<E2).

**Traced on `q04` (t13, logs `y10`-`y12`).** Defaults 1 and 778 hold on a water junction's saturation row with a nitrogen gas phase (0.0038 and 0.0052 mol beside 5.3e4 and 41 mol of water): card E6-T's mechanism. Random 131 and 951 have no failed Newton in the hold tick (refused attempts only): a mechanism of their own, not traced further. No sampled world clears with 60 iterations or the flow carry.

**On `z16` (t14, the carried gas of D41).** Defaults 12 -> 1 (98, from E7), random 6 -> 1 (829, from E5). Cleared: defaults 1 214 629 728 778 850 896, random 131 357 563 613 967; moved: defaults 181 to E9, 563 and 824 to E6, 622 to F, 915 to E4, random 803 to F (E8 alone). The sampled worlds: defaults 1 and 778 clean, random 131 clean, random 951 cleared from E6.

---

## E9: other step failures (defaults 6, random 2)

**World (defaults seed 47, 12 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, 0, -1 | 101.3 kPa |
| Pipes | eight blocks at heights 0 to 3, forming junctions at heights 0, 1 and 2 | 0.05 m |
| Void | 1, 0, -4 | 101.3 kPa, level with the generator |
| Tank | 0, 3, -1 | nitrogen, 101.3 kPa, three blocks up |

A generator and a void at the same height joined through a small pipe network that also rises to a tank three blocks up.

**Expectation.** The generator and the void are at the same pressure and height: nothing drives water from one to the other. The generator cannot lift water to the tank. Nothing flows. The only real effect is that the gas in the pipes and the tank stands in hydrostatic balance (the tank 3 m up at 101.29 kPa). Rest from the start.

**What the solver did.** Every step size failed with "Negative transport reconstruction": the step booked more of a component out of a node than it held. The flows at the hold were 1e-9 kg/s: rounding noise.

**Cause (unconfirmed).** The junctions sit a rounding error away from exact balance (one at 101325.0000002 Pa next to the void at 101325 Pa), so the exact rest test (D19) sees a drive of 2e-7 Pa and hands the island to the solver. The solver then produces rounding-noise flows, and such a flow drawing a trace component books slightly more than the trace.

**Proposition.**
- (a) Start junctions in hydrostatic balance with what they are joined to, so that a world like this is exactly at rest and never solved (a start-state rule; D25 allows setting a junction's pressure start, not its contents).
- (b) A rest test with the solver's own zero-flow threshold as its band (a tolerance: the owner's decision).
- (c) Clip a transfer that draws more of a trace than the node holds (a bookkeeping change; conservation must stay exact).
- Recommended: (a), measured first; no reference moves if only fresh junctions change (to be checked).

**On `ig10` (t17): defaults 3 (181 579 792), random 4 (579 651 720 761).** Random 761 is t16's card E9 (c) world (minimized to 14 blocks: it holds on t16 alone).

**On `ec19` (t18): defaults 2 (181 579), random 3 (579 720 761).** Defaults 792 cleared; random 651 moved to E6 (B). Card E9 (a), the junction start in hydrostatic balance with what it is joined to, is implemented on this branch: every single-stream start stands at its stream's standing pressure (`47618332`) and a falling drain is primed by the junction's liquid (`1be85dac`, owner to confirm). It reaches no remaining E9 world: what is left is option (c)'s trace bookkeeping (random 761) and the "Invalid inventory" family (`z18`).

**Seeds.** Defaults: 47 164 273 298 489 858. Random: 720 761.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 6 (9 19 273 298 629 771), random 4 (651 707 720 761).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 7 on `j18` (cleared 3: 208 489 858; moved out 2: 47>E6 164>F; new 4: 9<clean 19<clean 629<E6 771<E7), 6 on `i26` (cleared 0; moved out 0; new 0); random: 3 on `j18` (cleared 0; moved out 1: 579>E6; new 2: 651<C 707<C), 4 on `i26` (cleared 0; moved out 0; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears defaults 9; mixture clears defaults 9 629, adds random 322.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 6 (19 23 47 273 579 771), random 3 (579 651 720).** Against `n02`: defaults moved out 298>E6 629>E8, new 23<clean (holds alone, `o03`); random new 651<F.

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 6 (19 23 47 273 579 771), random 2 (579 720).** Against `o02`: defaults unchanged; random moved out 651>F.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 6 (19 23 47 273 579 771), random 4 (579 651 707 720).** Against `o02`: defaults 6 -> 6 (cleared 0; moved out 0; new 0); random 3 -> 4 (cleared 0; moved out 0; new 1: 707<F). Against `p03` (K): defaults 6 -> 6 (cleared 0; moved out 0; new 0); random 2 -> 4 (cleared 0; moved out 0; new 2: 651<F 707<C).

**On the side branch `claude/rest-resolution` (t16: the rest certificate reads a gauge of 0.1 Pa, D42/D43; `f2b6a987`, campaign `z18`): defaults 4 (19 23 273 771), random 4 (579 651 720 761).** Against `o02`: defaults 6 -> 4 (cleared 1: 47; moved out 1: 579>F; new 0); random 3 -> 4 (cleared 0; moved out 0; new 1: 761<clean). Review "The certificate is the gauge (2026-10-13)".
- **The card's world is a tie and is at rest** (family B): the generator and the void stand at the same pressure and height, the junctions a roundoff from balance; the gauge reads no drive, and the island certifies "STEADY: no flow" from the start. Gate test `RestResolutionWorldTest.cardE9sWorldIsAtRest` (defaults 47 minimized, 12 blocks); with the rest test of D19 it holds at tick 692 on "Negative transport reconstruction". Option (b) above is the one taken, as the owner's gauge (D42); (a) and (c) are not needed for it.
- **What is left in E9 is not rest.** Defaults 19 23 273 771: "Invalid inventory volume/energy" while water falls into a gas-filled junction or tank at a real drive (5.5 to 13 kPa at the hold). Random 579 651 720 761: "Negative transport reconstruction" in an island flowing at a real drive, a flow drawing a trace a step books beyond what its node holds (option (c)'s mechanism). Random 761 (worse, traced, minimized to 14 blocks): a methane vent through a nitrogen tank at 0.108 kg/s, the tank's washed-out nitrogen at 1.7e-24 of its moles, just above the trace flush fraction 1e-24 (D16); the gauge moved an earlier island's rest by 1.3e-3 Pa and with it the step at which the trace crosses the fraction.

---

## D: the check at the start of an interval finds equations it cannot tell apart (defaults 5, random 1)

**World (defaults seed 15, 7 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, -1, 1 | 101.3 kPa |
| Pipe | 0, 0, 1 | 0.05 m, above the generator |
| Inline filter | 0, 0, 0 | facing south |
| Pipes | 0,0,-1 (junction, on the tank's top face); 0,1,-1 | 0.05 m |
| Tank | 0, -1, -1 | nitrogen, 101.3 kPa |
| Void | 0, 2, -1 | 101.3 kPa, two blocks up |

The generator's water would have to climb one block over a filter to fall into a tank at the generator's own height; the same hump also rises to a void.

**Expectation.** A siphon over a 1 m hump that is not primed: the generator at 101.3 kPa cannot push water up 1 m against a tank at 101.3 kPa. Nothing flows. Rest.

**What the solver did.** The start-of-interval check (the filter's closure check) failed on a singular Jacobian at its second pass. The worst equation was the junction's mass balance (0.45).

**Cause (unconfirmed).** The junctions start as the generator's water, so the hump starts full of water: a primed siphon that physics would never have. The solver pulls the hump's water to 91.6 kPa (a hanging column), closes the generator and the void, and the filter's two junctions are then left joined to the rest by one connection with nothing to fix their pressure level.

**Proposition.** Option R1 (card E1) removes the class. The start state (card E9, (a)) would find this world at rest. Recommended: R1.

**On `ig10` (t17): none.**

**Seeds.** Defaults: 15 499 524 727 902. Random: 866.

**With R1 (owner decision D37; branch `claude/e1-e4-measure`, campaigns `i05`, `i26`): defaults 0, random 0.** The singular check no longer holds an island: defaults 524 and 902 run clean; 15, 499 and 727 (and random 348) now hold in their steps on a singular Jacobian (E2), the siphon start of this card's world. Without R1, under the start rule, D returns as 3 defaults / 2 random seeds (`i33`). Class G stays at 0.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 0, random 0.** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 5 on `j18` (cleared 3: 15 524 902; moved out 2: 499>E2 727>E2; new 0), 0 on `i26` (cleared 0; moved out 0; new 0); random: 1 on `j18` (cleared 1: 348; moved out 0; new 0), 0 on `i26` (cleared 0; moved out 0; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): none in any form.

---

## E7: the solver returns to a set of closures it already tried (defaults 3, random 5)

**World (defaults seed 678, 10 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | 0, -1, -2 | 101.3 kPa |
| Pipes | 0,-1,-1; 0,-1,0 (junction) | 0.05 m |
| Void | 0, 0, 0 | 101.3 kPa, above the junction |
| Upper tank | 0, -2, 0 | nitrogen, 101.3 kPa, below the junction (top face) |
| Pipes | 0,-3,0; 0,-4,0; 0,-5,0; 1,-5,0 | 0.05 m, from the upper tank's bottom down to the lower tank's side |
| Lower tank | 1, -5, -1 | nitrogen, 101.3 kPa |

A generator feeds a junction with a void above it and a tank below it; that tank's bottom drains into a second tank three blocks lower.

**Expectation.** Water from the generator falls into the upper tank and through it into the lower tank. The displaced nitrogen rises through the upper tank's top and escapes into the void. Both tanks fill with water over a few minutes (about 2000 kg, estimate) and end water-full at about 106 and 136 kPa (their depths below the generator).

**What the solver did.** Every step size ended in an active-set cycle: the solver's sequence of closure decisions (which connections are open or shut, which phases exist) came back to one it had already tried. The trial states on the way reached water above 900 K and below 273.16 K.

**Cause (unconfirmed).** The junction starts as the upper tank's nitrogen. Water arriving makes a liquid phase appear and the pass is restarted; the tank's top port alternates between drawing gas and water. Measured on random seed 325 (review "The remaining classes"): the junction's start from a void's or tank's gas is where the cycle begins. Group C: the trace fold cleared random 561 (methane and crude into a void, a junction's phase correction cycling a 3e-8 liquid, above the share; the fold changed the path, not that phase); this world's cycle is not a trace phase.

**Proposition.** The phase-set step (card E5) or a start from the source's fluid (a mint change, measured: 23 cleared and 10 worse random, 19 and 19 defaults). Recommended: the phase-set step.

**On `ig10` (t17): defaults 2 (734 952), random 2 (116 145).**

**Seeds.** Defaults: 678 771 952. Random: 145 325 615 758 951.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 2 (734 952), random 1 (145).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 5 on `j18` (cleared 2: 74 236; moved out 2: 678>E2 771>E9; new 1: 734<E6), 2 on `i26` (cleared 0; moved out 0; new 0); random: 6 on `j18` (cleared 2: 325 758; moved out 3: 615>C 654>E6 951>E8; new 0), 1 on `i26` (cleared 0; moved out 0; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): no move.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 3 (98 734 952), random 3 (116 145 698).** Against `n02`: defaults new 98<E6, random new 698<F.

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 3 (573 734 952), random 2 (116 145).** Against `o02`: defaults new 573<clean, moved out 98>E6; random moved out 698>E6.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 3 (573 734 952), random 2 (116 145).** Against `o02`: defaults 3 -> 3 (cleared 0; moved out 1: 98>E2; new 1: 573<clean); random 3 -> 2 (cleared 0; moved out 1: 698>E10; new 0). Against `p03` (K): defaults 3 -> 3 (cleared 0; moved out 0; new 0); random 2 -> 2 (cleared 0; moved out 0; new 0).

---

## E3: the water-vapour model is used outside its range (defaults 2, random 2)

**World (defaults seed 600, 7 blocks).**

| Block | Position | Setting |
|---|---|---|
| Water generator | -4, -2, 0 | 101.3 kPa |
| Pipes | -3,-2,0; -2,-2,0; -3,-1,0; -2,-1,0; -2,0,0 (a small loop rising two blocks) | 0.05 m |
| Void | -3, 0, 0 | 101.3 kPa, two blocks above the generator |

A generator feeds a small pipe loop that rises two blocks to a void.

**Expectation.** The generator cannot lift water 2 m against a void at its own pressure. Nothing flows. Rest from the start.

**What the solver did.** Every step size was refused with "water-vapor approximation outside qualified partial-pressure range".

**Cause (unconfirmed).** The junctions start as the generator's water at 101.3 kPa at every height, which is not a balanced column. Because the junctions form a loop, the rest test (D19) asks for exact balance of every connection, finds the column unbalanced, and hands the island to the solver, which then evaluates a gas phase of water outside the range the water-vapour model is qualified for.

**Proposition.** The hydrostatic start (card E9, (a)) makes this world exactly at rest. Recommended: that.

**On `ig10` (t17): defaults 0, random 1 (301: a first solve that walks to a void supplying against a 418 kPa drive, card E6-B's mechanism; from t15's rule (b)).**

**Seeds.** Defaults: 600 669. Random: 444 932.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 4 (6 224 322 505), random 1 (563).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 3 on `j18` (cleared 0; moved out 3: 78>E6 600>E6 669>E4; new 4: 6<E5 224<E6 322<E6 505<clean), 3 on `i26` (cleared 0; moved out 0; new 1: 322<E6); random: 1 on `j18` (cleared 1: 932; moved out 0; new 1: 563<E1), 1 on `i26` (cleared 0; moved out 0; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): no seed of the class moves with either variant (strongest clears defaults 505).

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 1 (6), random 0.** Against `o02`: defaults moved out 224>E5 322>C.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 1 (6), random 0 (none).** Against `o02`: defaults 3 -> 1 (cleared 0; moved out 2: 224>E5 322>C; new 0); random 0 -> 0 (cleared 0; moved out 0; new 0). Against `p03` (K): defaults 1 -> 1 (cleared 0; moved out 0; new 0); random 0 -> 0 (cleared 0; moved out 0; new 0).

---

## E10: no finite slope at the first step (defaults 2, random 4)

**World (defaults seed 61, 8 blocks after reduction 7).**

| Block | Position | Setting |
|---|---|---|
| Water generator A | 1, 0, -2 | 101.3 kPa |
| Pipes | 2,0,-2; 2,-1,-2 (junction); 2,-1,-1; 2,-1,0 | 0.05 m (3,0,-2 is a dead end, left out) |
| Water generator B | 2, -2, -2 | 101.3 kPa, one block below the junction |
| Tank | 2, -1, 1 | nitrogen, 101.3 kPa, fed through its side |

Generator A feeds water down one block to a junction and on into a tank at the junction's height; generator B stands below the junction.

**Expectation.** Water falls 1 m into the tank. It ends at about 111 kPa with about 90 kg (estimate). Generator B cannot push up; it stays shut.

**What the solver did.** The tank had 61 kg at 110.9 kPa. Then at every step size the solver could not compute a finite slope for the junction's variable 8, and its water-vapour and gas-closure equations stood at 1.05 and 0.29.

**Cause (unconfirmed).** The junction is pure liquid water (it started as the generators' water). As the tank's pressure passes the junction's, nitrogen and water vapour can move back into it: a gas phase appears in a liquid-full junction, and the trial states needed for the slope leave the property data.

**Proposition.** The phase-set step (card E5). Recommended: that, measured before a decision.

**On `ig10` (t17): defaults 0, random 3 (4 699 990).**

**Seeds.** Defaults: 61 736. Random: 94 575 927 990.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 4 (61 71 271 981), random 3 (4 333 990).** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 4 on `j18` (cleared 2: 188 387; moved out 0; new 2: 71<F 981<C), 3 on `i26` (cleared 0; moved out 0; new 1: 981<E5); random: 5 on `j18` (cleared 2: 94 927; moved out 2: 511>E4 575>C; new 2: 4<F 333<clean), 3 on `i26` (cleared 0; moved out 0; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): strongest clears random 333; mixture leaves it.

**On the side branch `claude/gascolumn-gametest` (t10: the drain-side gas column; `c09dc06d`, campaign `o02`): defaults 4 (61 188 387 613), random 4 (4 430 699 990).** Against `n02`: defaults moved out 788>C, new 188<clean 387<clean (both hold alone, `o03`); random new 430<E6.

**On the side branch `claude/keep-contents` (t11, option K; campaign `p03`): defaults 5 (61 188 333 387 613), random 4 (4 699 895 990).** Against `o02`: defaults new 333<clean; random new 895<clean, moved out 430>E6.

**On the side branch `claude/keep-contents` (t12: option K with both rules, an unchanged island kept and a displaced junction started as what arrives; `67a329f8`, campaign `q04`): defaults 4 (188 333 387 613), random 4 (4 698 699 990).** Against `o02`: defaults 4 -> 4 (cleared 1: 61; moved out 0; new 1: 333<clean); random 4 -> 4 (cleared 0; moved out 1: 430>E6; new 1: 698<E7). Against `p03` (K): defaults 5 -> 4 (cleared 1: 61; moved out 0; new 0); random 4 -> 4 (cleared 1: 895; moved out 0; new 1: 698<E6).

---

## A: the step-size controller locks (defaults 1, random 0)

**World (defaults seed 758, 8 blocks after reduction 7).**

| Block | Position | Setting |
|---|---|---|
| Water generator A | 0, 0, 0 | 101.3 kPa |
| Water generator B | 0, -2, 0 | 101.3 kPa |
| Pipes | 0,-1,0 (junction between the generators); 0,-1,1 (junction); 0,0,1 (junction); 0,0,2 | 0.05 m (0,-2,2 under the tank is a dead end, left out) |
| Tank | 0, -1, 2 | nitrogen, 101.3 kPa, fed through its side and its top |

Two generators, one above the other, feed a small pipe loop that enters a tank through its side and its top.

**Expectation.** Generator A (at the top) pushes water down into the tank. Generator B (at the bottom) cannot push up and stays shut. The tank's gas has nowhere to go (generators do not take gas), so it stays in the tank as a cushion: the tank ends at about 111 kPa holding about 90 kg of water (estimate).

**What the solver did.** At the hold the tank was water-full (996 kg) at 132 kPa, and two junctions held its nitrogen: 13 and 61 m3 of gas. The step-size controller then refused every step for changing the tank's state by more than 5 % (510 accepted, 514 refused in one slice).

**Cause (confirmed for the state, unconfirmed for the path).** A junction's mass is fixed during a step but its volume is not: the model treats a junction's volume as a placeholder. So the tank's nitrogen could be pushed into the junctions, which grew to hold it, and the tank overfilled. Physically a pipe block holds a few litres. The controller lock is the class A (F7) behaviour on the resulting water-full tank.

**Proposition.**
- (a) Bound a junction's gas volume by its pipe block's volume (a model change to the junction holdup; D25 keeps the holdup's size and contents, so this needs the owner).
- (b) Leave it: a single seed in 2000.
- Recommended: discuss (a); it is the only card where the model itself allows an unphysical state.

**On `ig10` (t17): none.**

**Seeds.** Defaults: 758. Random: none.

**On the merged code (t7: option (a), G2, R1 and the junction start rule, nearest supplier; `62e6b790`, campaign `k02`): defaults 0, random 0.** Review "Integration of R1 and the junction start rule (2026-10-12)".
- Against `j18` (option (a) and G2) and `i26` (R1 and the start rule): defaults: 1 on `j18` (cleared 0; moved out 1: 758>E2; new 0), 0 on `i26` (cleared 0; moved out 0; new 0); random: 0 on `j18` (cleared 0; moved out 0; new 0), 0 on `i26` (cleared 0; moved out 0; new 0).
- The start-rule variants against nearest (`k11` strongest first, `k15` mixture, not committed): none in any form.

---

## D44: random crude two-phase holds, saved for the thermodynamics work (random 7: E2 6, E5 1)

Owner decision D44 (2026-10-13): "Thermo package issue not covered in this work, save the test case for later use". These worlds leave the batch's zero goal. Each is minimized on the final code of `claude/rest-resolution` (`f2b6a987`) keeping its class and a two-phase hydrocarbon node in the held island, and saved with its blocks, hold, failing rows and card in `tools/phase-ports-probes/saved-cases/<seed>/` (README there).

**World (random seed 818, 5 blocks, the smallest; card `saved-cases/818/card.md`).**

| Block | Position | Setting |
|---|---|---|
| Crude generator | 1, 0, 0 | two-phase light crude, 309.3 K, 278.4 kPa, 0.01 m bore |
| Pipe (junction) | 0, 0, 0 | 0.05 m |
| Pipe | -1, 0, 0 | 0.1 m |
| Tank | -2, 0, 0 | nitrogen, 101.3 kPa, 1 m3 |
| Void | 0, 1, 0 | nitrogen, 101.3 kPa, above the junction |

**Expectation from physics.** The crude rises 1 m into the void and fills the tank level with the junction; its light ends flash as it expands, so the tank holds a two-phase crude at the junction's pressure while the through-flow goes to the void. A steady flow within a minute.

**What the solver did.** At tick 2974 the tank and the junction stand at 194 052.98 Pa (the tank 45 % liquid, 55 % vapour). Every step size ends on a singular Jacobian at pass 0; the worst row is the junction's continuity row (1.7e-8), next to the tank's fugacity rows (1e-14): a point near the tolerance whose matrix cannot be factored.

**The seven cases.**

| Seed | Class | Blocks | Failing row | Two-phase node |
|---|---|---|---|---|
| 279 | E2 | 6 | tank's flow row 3.4e-9 | tank 292.4 kPa, 325.5 K |
| 298 | E2 | 13 | two junctions' continuity rows +-0.44 | three tanks 215-225 kPa |
| 372 | E2 | 14 | two-phase junction's continuity row 1.0e-6 | crude-nitrogen junctions 317-321 kPa |
| 521 | E2 | 6 | tank's top-port flow row -0.028 | tank 299.4 kPa, 329.7 K |
| 547 | E5 | 13 | junction's continuity row 13.44 (stall) | junctions 99.4 and 114.5 kPa draining into 1 atm voids |
| 786 | E2 | 8 | tank's energy row 2.7e-9 | tank 229.7 kPa, 320.3 K |
| 818 | E2 | 5 | junction's continuity row 1.7e-8 | tank 194.1 kPa, 309.2 K |

**Cause (unconfirmed).** Hot light crude flashing in a tank or a junction; in three cases the Newton stands near its tolerance and the matrix there is singular. Not traced further (D44). Not saved: random E2 163 and 854 and E5 815 and 829 of `z18` (no two-phase hydrocarbon node in the held island: water or gas nodes); `o02`'s random E2 311 and E5 553, 629, 807 are clean on the final code.

**Proposition.** None in this batch: the cases wait for the thermodynamics package's work.

**On `ig10` (t17): all seven still hold: E2 279 298 372 521 786 818, and 547 now in budget (F).**

**Seeds.** Random: 279 298 372 521 547 786 818.

---

## Material

Worlds and exact specs: `tools/phase-ports-probes/hold-classes/logs/d11-cards-*.gradle.txt` (CARD_KEEP replays a world with `HoldMinimizerTest.replay`, `-PholdFuzzPipeBetween` default on). Traces: `d13-trace-*` with `src/instr-failing-rows.patch` (applied, run, reversed; never committed). Campaigns: `d10-campaign-groupB-*` (group B), `f28-campaign-groupC-*` (counts and seed lists of the cards); group C worlds, traces and gate-world minimizations: logs `f12`, `f14`, `f17`. E6 option (a) (cards E6 and F only, side branch `claude/e6-row-scale`): campaign `g32-campaign-rule-*`, transitions `g33`, worse seeds alone `g34`, residual bands `g35`.

R1 and the junction start rule (cards E1, E4, D, E8 only; branch `claude/e1-e4-measure`): baseline at 4 workers `i03` (= `g32`), R1 campaign `i05`, the committed start rule `i26`, its variant c2 `i39`, without R1 `i33`, worse seeds alone `i06`, `i16`, `i24`, `i40`, `i41`; minimized worlds `i07`, `i17`, `i34`, `i42`; mints `i14`, `i21`, `i43` (`src/instr-mint-print.patch`).
