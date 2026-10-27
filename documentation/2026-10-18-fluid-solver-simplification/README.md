# 2026-10-18-fluid-solver-simplification

Purpose: Minimalist resolutions of the m11 hold mechanisms (t37 groups K1-K8, rig holds H4, H5) that simplify the fluid network solver (owner 2026-10-18: prevent piling up complexity): the complexity map, three prototyped forms on a branch (A: the velocity cap in every throttling line's law, the cap active set and its modes gone, checkpoint format 11; C: a failed iterate's collapsing phase folded, a bag's resolution a millilitre; B: the pass cycle key carries its proposal) and proposals with numbers for K4-K8

Status: Implemented 2026-10-19 (0.9.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [SOLVER_COMPLEXITY_MAP.md](SOLVER_COMPLEXITY_MAP.md)
- [SOLVER_SIMPLIFICATION_PLAN.md](SOLVER_SIMPLIFICATION_PLAN.md)
- [MOVED_REFERENCES.md](MOVED_REFERENCES.md)
- [FINAL_TEST_REVIEW.md](FINAL_TEST_REVIEW.md)
- [BUNDLED_HOLDS_TRACE.md](BUNDLED_HOLDS_TRACE.md)
- form D detail in `tools/fluid-solver-simplification/FORM_D_CHOKE_LAW.md`

## Code areas

src/main/java/com/wormzjl/createcheme/science/fluid/network (PassiveStepSolver, FlowControl, InventoryEquilibrium), science/fluid/thermo/FluidThermodynamics, runtime/fluid (checkpoint format 11), client/gui (velocity-limited note gone)

## History

- Implemented 2026-10-19 (main `5b49f940`, mod 0.9.0, release 0.9.0 by fast-forward of `claude/junction-seeding` with the pre-merge commits `9ce2553a`, `47391899`, `81bf147f`
- not pushed
- final gates t48 (m16 at `798bedd9`): full test 1482/0, G2 exact, fluid GameTests 31/31, campaign cold 944 / bundled 988 / defaults 1000
- t49 (m17 at `81bf147f`): 1482/0, G2 exact
- merge plan `2026-09-24-coolprop-low-temperature/MERGE_STACK_PLAN.md`
- code guide refreshed at `5b49f940`).
- Open: K4 (the stiff-liquid item)
- the junction pressure-range guard and junctions in the step measure (on hold).
- Next: the custom equipment rendering branch merges as its own batch with checkpoint format 12.
- History: in progress since 2026-10-18 (thread t38, Fable 5.1, branch `claude/fluid-solver-simplification` from `688f4cd7`
- owner decisions of 2026-10-18 implemented: one Newton limit 30, K6 suction rule, flush bound 10000, chain-100 recaptured unchanged, CHANGELOG line
- HEAD `32bf3fa4` (573b050b, a comment-and-docs commit, the D38 C amendment of t40's trace and the 678 rule), quick check cold 191 / bundled 197 / defaults 200 against 183 / 198 / 200
- Opus final test (thread t39) green 2026-10-18: full test 1475/0 failures, G2 0, GameTests 31/31, campaign cold 924 / bundled random 980 / defaults 1000 against 911 / 986 / 1000
- t40 trace of bundled seeds 407 and 26 2026-10-18: one D38 C fold/proposal cycle at a liquid-full fill, form A only its trigger
- t40 round 2 2026-10-19: 678 (K3, receiver at its bubble point; proposition: a proposal below the resolution both ways is refused, two direction tests deleted, probe green) and 988 (choked gas series at a join, chord out of iterations at the limit 30; proposition: the carried flow as the edge unknown, not probed), owner decisions pending
- D38 C by both shares landed on the owner's decision (`6c073298`)
- Opus final test 2 (thread t41) green 2026-10-19: 1476/0, G2 0, GameTests 31/31, campaign cold 926 / bundled random 984 / defaults 1000, no newly held seed
- form D, a pressure-dependent choke law (owner request, t38), prototyped on exports 2026-10-19 and not landed: the gas choke on every gas stream clears cold 19 and bundled 988 at the Newton limit of 30, but holds a gate world and the mixed-gas cost gate and costs +17 % bundled Newton work
- the liquid law was rejected (boiling drains at 1 to 3 m/s, cost x8 to x72)
- owner 2026-10-19: liquids keep the configured 100 m/s cap, and D3's failing gates are traced first (thread t42, `FORM_D_GATES_TRACE.md`)
- t42 trace done 2026-10-19 (`FORM_D_GATES_TRACE.md`): the world 1 hold and the +17 % bundled cost (seed 173, a new D3 hold inside the clean list; D3 is -8.3 % on the seeds clean in both) are one cap discontinuity at trace phases, the mixed-gas cost and `BackwardEulerStepTest` follow from the faster gas transients (restate), D2 fails world 3 by capping one gas two ways
- the corrected form D3c (throat share of the phases) clears the hold and seed 173, quick check cold 191 / bundled 198 / defaults 200 as the baseline
- owner 2026-10-19: cold seeds 199, 6 and 15 under D3c are traced before anything lands (t42b, `FORM_D3C_COLD_SEEDS_TRACE.md`), and world 1's column gate is restated to the band between the two tanks' columns when D3c lands
- t40 round 2 2026-10-19 (`BUNDLED_HOLDS_TRACE.md`): 678 is a vapour proposed at a bubble-point receiver below the resolution, and 988 is a stale chord after the cap knee at a choked join
- owner: land 678's proposal-at-resolution rule (t38) and give it one Opus final lane together with D3c
- decide 988 after t42b
- 678 rule landed `32bf3fa4` 2026-10-19 (phaseCheck refuses a proposed phase its own split holds below the vanishing share both by moles and by volume, whatever its direction, the two direction tests gone; gate from seed 678's nine blocks with its rule-off proof; javac suites 66/66, 184/184, 304/304, three tiny moves in `MOVED_REFERENCES.md`)
- t42b done 2026-10-19 (`FORM_D3C_COLD_SEEDS_TRACE.md`): cold 199 is a multistable mixing junction (LNG plus CO2-rich gas near its critical region) whose interval-start step hops branches, so it never certifies STEADY (cost x9, no hold)
- 6 and 15 are marginal LNG cold starts that any raised cap tips (proposal: a junction pressure-range guard in the step Newton, its own item)
- the corrected form D3d was rejected (`LiquidCo2LetDownTest`, cold 190)
- bundled 988 is cleared by the cap level, not by a new pressure handle
- owner 2026-10-19: form D dropped (the configured 100 m/s cap stays, 988 a known E6 hold), the junction pressure-range guard and junctions in the step measure on hold
- Opus final test 3 (thread t43, lane m14 at `32bf3fa4`, the 678 rule) green 2026-10-19: 1477/0, G2 0, GameTests 31/31, campaign cold 924 / bundled random 985 (678 clean) / defaults 1000, cold 698 and 797 F under load and clean alone, no new worse world, clean-in-both cost unchanged
- not merged)
