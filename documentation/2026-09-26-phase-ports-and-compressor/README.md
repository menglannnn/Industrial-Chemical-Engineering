# 2026-09-26-phase-ports-and-compressor

Purpose: Phase-specific tank outlets (top gas, sides bulk, bottom liquid) drawing phases by priority (D11), level head at the bottom port (D9, fixed 1 m), liquid-only pump with a typed `INLET_WRONG_PHASE` closure, gas compressor block with a pressure-ratio limit and isothermal work as heat, one-way generators at the cold start (D12), equation-gate polish (D13), junction water trace (D10); checkpoint format 6, wire fluid-8 (fluid-7 at WP5; outlet faces since the GUI fixes F3-F5)

Status: Implemented 2026-10-14 (0.7.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [HANDOFF.md](HANDOFF.md) (current)
- [HANDOFF_2026-09-26_start.md](HANDOFF_2026-09-26_start.md)
- [PHASE_PORTS_PLAN.md](PHASE_PORTS_PLAN.md) (Appendix C)
- [DECISION_LOG.md](DECISION_LOG.md)
- [LEVEL_HEAD_REVIEW.md](LEVEL_HEAD_REVIEW.md)
- [EXTREME_TOPOLOGY_TESTS.md](EXTREME_TOPOLOGY_TESTS.md)
- [PHASE_PORTS_REVIEW.md](PHASE_PORTS_REVIEW.md)

## Code areas

`PassiveNetwork` (pipe-end port), `PassiveStepSolver`, `PhaseDraw`, `InletPhase`, `ConservativeTransport`, `FlowControl` (Mover, Compressor, inlet modes), `FluidThermodynamics`, `PhysicalFluidTopology`, `FluidCheckpointCodec` (format 6), `FluidNetwork` (fluid-8), `FluidDeviceScreen`, `TankOutlets`; tools/phase-ports-probes/, tools/cloud-science-harness/; `test`, `fluidSolverRegression`, `runFluidGameTestServer`

## History

- Implemented 2026-10-14 (merged to `main` as 0.7.0 by squash
- in progress since 2026-09-26
- WP0-WP6 done on `claude/phase-ports-compressor`, commits `35e354d` WP1, `d836cf2` WP2 (superseded), `07e7426` D10, `fdf3574` D9, `548a9bf` D11, `418ca7e` D12, `4a71629` D13, `fc9574d` WP3+WP4, `9744a45` WP5, `cb41860` WP6, with their tools/docs commits
- pushed to origin up to `5f38474`, the WP6 commits not pushed
- awaiting the owner's in-game check (three scenarios, review WP5 section 11) and merge (0.7.0, fresh world)
- gates at the close: fluid suites 472/472 with the junction lines bitwise and MIXED_GAS_COST 286, exact regression 0.000e+00, 45/45 adjacent, fluid GameTests 33/33 on a fresh world, full `test` 1165/1166 with the one failure (`RegroupedCrudeTest` wall-clock deadline) pre-existing on the base, `build -x test` green
- review `PHASE_PORTS_REVIEW.md` "WP6: gates and close"
- 2026-09-27 in-game check found F1-F6, island holds F1/F2 fixed, GUI fixes F3-F5 done 2026-09-27 (`f089edd1`: outlet list as a scrolled tab at every GUI scale, face names, ratio bounds message
- fluid suites 476/476, MIXED_GAS_COST 286, GameTests 33/33, checked in game at scales 1-3 at 854x480, review "GUI fixes F3-F5 (2026-09-27)")
- island hold F7 (a fed tank with open outlets: D11 per-step capacity pool `N q dt` locks the state-change controller, 14 holds in game) diagnosed 2026-09-27, no code change, options O1-O3 measured for the owner (O2 recommended
- merge blocker), review "Island hold F7: state-change rejections on a fed tank (2026-09-27)")
- GUI layout at small scaled screens done 2026-09-27 (`8b43cfdd`: every device page's rail clipped above the status separator and scrolled, bottom buttons narrowed, cut lines on hover
- fluid suites 476/476, MIXED_GAS_COST 286, checked in game at 854x480 scales 2 and 1 and 1920x1080 auto, decision A58, review "GUI layout at small scaled screens (2026-09-27)")
- island hold F8 fixed 2026-09-27 (`28675064`, `4855fe82`, `65583a38`: a mover at its target into a sealed pocket of junctions had no root, singular at the first slice: every pump into a pressure control valve or into a filter with nothing past it
- a relieving valve starts regulating, otherwise the mover starts closed at its shutoff, a sealed pocket keeps its pressure, an edge into one gets no cap
- gate test `InlineFilterLineHoldTest`
- fluid suites 480/480 bitwise, MIXED_GAS_COST 286, exact regression 0, adjacent 45/45, GameTests 33/33
- decisions A59-A62
- the "reload cured it" was a retry not yet due
- review "Island hold F8: singular Jacobian after a topology event (2026-09-27)")
- hold search by placement-sequence fuzz 2026-09-27 (`tools/phase-ports-probes/hold-fuzz/`, 1000 seeds in 2-4 min on 8 workers: 703/1000 random worlds and 702/1000 default-play worlds run clean on `65583a38` (593 before F8)
- 13 new hold classes with reproductions, the largest vapour partial-pressure floor in water-seeded junctions, interval-start rate pass, step Newton limits, and a topology-event exception
- owner decisions open
- decision A63
- review "Hold search by placement-sequence fuzz (2026-09-27)"
- hold classes worked through 2026-09-27/28: X, E3, E1a and the cold generator closure in actuated islands fixed (`9079a15a`, `7e5391a7`, `bbb79790`, `a3d3a789`
- clean 703/702 -> 770/775 of 1000 random/default worlds with the orchestrator's F7 and trace flush), E1 rest, E6, F, E2 rest and the smaller classes diagnosed with options for the owner, decisions A66-A69, review sections "Hold class ..." and "Hold search: status after the class fixes")
- boundary split, dead ends and rest 2026-09-28 (owner decisions D17-D19, `28f8c16f`, `73624241`, `89af686c`: fuzz 845 random / 822 defaults clean of 1000, gates 498/498 with every reference bitwise
- follow-up group A 2026-09-28: only pipes connect D20 and the one-way rule named D21, `983cc915`-`4beea57a`, gates 503/503 bitwise, new fuzz baseline with a pipe between devices 816 / 805
- group B 2026-09-28: tank islands at rest D22 `857963d9`, P3 per branch and P4 measured and not implemented, fuzz 816 / 817, REMAINING_HOLDS.md written
- group C 2026-10-12: trace phases folded in the flash (D34, `051ad5f5`, tests `0eb6655c`), fluid suites 508/508, junction lines identical, MIXED_GAS_COST 286, exact 0.000e+00, GameTests 33/33, fuzz 822 / 817, stop points on the fold's latent-heat cycle (A85), nitrogen bubbles in water (A84) and `closeDeadHeads` (A87) in the review's group C section
- E6 option (a) 2026-10-12 on the side branch `claude/e6-row-scale` (owner decision D35, `0563d4b1`, tests `464b154b`): a connection's row scale from both ends' cap drops, fuzz 833 random / 853 defaults, the 24 mixed-gas lines and MIXED_GAS_COST 286 -> 273 move at roundoff (state within the Newton tolerance of a refined reference), two extreme-grid tests failed on a tolerance-level overshoot
- review "Hold class E6, option (a) implemented (2026-10-12)").
- Option (a) taken (D36, 2026-10-12) and measured before it merges: G2, a vessel stated at what it holds (`b6f2c072`, `e350fd08`, `bf830c78`), closes the grid item (suites 511/0, GameTests 33/33, fuzz 837 random / 856 defaults, class E4 12 -> 0 / 21 -> 6), the mixed-gas lines move again at roundoff, MIXED_GAS_COST 273 -> 309 and chain-100 by 6.3e-15 (re-baselined in C1, A440), the one-way scale refinement left out (A95)
- review "Hold class E6, grid overshoot (G2) and the one-way scale (2026-10-12)"
- G2 follow-up (`fa45d02a`, `b4b468b6`): the closure solves a vessel in the step's own trace-truncated equilibrium, random seeds 400 and 737 cleared (suites 513/0, GameTests 33/33, fuzz 839 random / 856 defaults), the four G2 budget seeds traced as step failures, no by-construction closure skip
- review "G2 follow-up: cushion vessels, budget seeds, closure cost (2026-10-12)"
- branch `claude/e1-e4-measure` (D37), measured on the E6 rule and merged onto this branch 2026-10-12: R1 (a failed start check declares no closure) and the junction start from reachability (E4 option (c)), fuzz on its own branch 884 random / 866 defaults with no reference moved, variant "strongest supplier first" 893 / 881
- reviews "Hold class E1, R1 on the E6 rule" and "Hold class E4, the junction start from reachability"
- the integration (t7, 2026-10-12): merge `62e6b790` with no reference moved (suites 516/0, GameTests 33/33), fuzz 887 random / 869 defaults, E6 gate worlds rebuilt (`96d9be65`), start variants measured (strongest 895 / 885, mixture 889 / 874), choice open (D39)
- review "Integration of R1 and the junction start rule (2026-10-12)", in progress since 2026-10-12
- junction start (m') on side branch `claude/start-mixture` (t8): `437aa3f4`, gate test `009b77c5`, fuzz 898 / 883, no reference moved, stop point A119 (start pressure, 919 / 879)
- review "Junction start (m'): the pressure-balanced mixture (2026-10-12)", in progress since 2026-10-12
- start at the balance pressure on side branch `claude/start-pressure` (t9): `5999da45`, gate test `80d8f010`, fuzz 920 / 883, no recorded reference moved, options G (drain-side gas column, 920 / 884) and K open
- review "Junction start at the balance pressure (2026-10-13)", in progress since 2026-10-13
- drain-side gas column and the outlets GameTest at a fixed online tick on side branch `claude/gascolumn-gametest` (t10): `c09dc06d`, gate test `f42dbd0f`, GameTest `a9bc988b`, fuzz 919 / 883, no recorded reference moved, A133 (amends D40 for gas) for the owner to confirm
- reviews "Junction start: the drain-side gas column (2026-10-13)" and "GameTest reads at a fixed online tick (2026-10-13)", in progress since 2026-10-13
- option K, a junction keeps its contents across a topology edit, on side branch `claude/keep-contents` (t11): `078aa67f`, tests `e80feff0`, fuzz 915 / 874 (strict variant 923 / 875
- with an unchanged island not replaced 922 / 879), no recorded reference moved, A142-A151 for the owner
- review "A junction keeps its contents across a topology edit (2026-10-13)", in progress since 2026-10-13
- t12 on the same branch: an unchanged island kept, a displaced kept junction started as what arrives, `fe9ec799` `67a329f8`, tests `caf0c129`, fuzz 921 / 881, no recorded reference moved, A152-A162 for the owner
- review "Kept contents: unchanged islands and the displacement criterion (2026-10-13)"
- t13 on side branch `claude/e6-cards` from `claude/keep-contents`, diagnosis only, no solver code: hold class E6 split by mechanism into four cards (E6-T trace gas phase in water, E6-S start flows from a static estimate, E6-B forbidden boundary directions on a first solve, E6-C capped gas junction) with a "Distinct defects remaining" table in REMAINING_HOLDS.md, A163-A169 for the owner
- review "Hold class E6 on the final code, by mechanism (2026-10-13)", in progress since 2026-10-13
- t14 on side branch `claude/bubble-through` from `ca7880ec`: a gas bubble in a liquid-full junction is carried by the water, dry, with no saturation row (owner decision D41), `c767503a`, gate test `dbebfddd`, fuzz 933 / 930 against `o02` 919 / 883, hold group E6-T gone, A170-A178 for the owner
- review "A gas bubble in a liquid-full junction is pushed through (2026-10-13)", in progress since 2026-10-13
- side branch `claude/start-flows` (t15): the step starts from the committed end flows, one column rule for the estimate, checkpoint format 7, 923 / 901, five rest-identity tests wait for D42, rule (c) measured and recommended out
- review "The step starts from the committed flows
- forbidden directions closed on first solves (2026-10-13)", A180-A193, owner to decide, 2026-10-13). Side branch `claude/rest-resolution` (t16, D42-D44, A210-A223
- in progress since 2026-10-13, not merged): the rest certificate reads a 0.1 Pa gauge (connections, paths of junctions between tanks/generators/voids, loops
- flows at a step's end
- at every interval start and accepted step), valves read statically, device rail pressures to 0.1 Pa
- campaign `z18` 923 / 894 (from `o02` 919 / 883), ten of twelve tie worlds clean
- seven assertions restated for the owner (A221), one recorded reference moved (A222)
- random crude two-phase holds saved in tools/phase-ports-probes/saved-cases/ (D44)
- review "The certificate is the gauge: rest at 0.1 Pa, zero-flow loops, saved crude cases (2026-10-13)"). Integration branch `claude/integration-1` (t17, in progress since 2026-10-13, not merged, owner to decide): t14 + t15 (a)+(b) + t16 on one branch, t16's rows renumbered A210-A223, owner decision D46 (the junction holdup is virtual) implemented with every solver number unchanged, campaign `ig10` 935 / 957, re-baseline prepared not applied
- review "Integration of the carried gas, the committed flows and the gauge
- the virtual junction holdup (2026-10-13)", A230-A240. Taken as `claude/phase-ports-compressor` and re-baselined once (D47, `0d841a5f`). Branch `claude/e9-start-column` (t18, in progress since 2026-10-13, not merged, owner to decide): every junction start stands where its stream stands (card E9 (a), `47618332`), t15's rule (c) re-taken (`af735b6b`, `e810b6e3`), a falling drain primed by the junction's liquid (`1be85dac`, amends D40 for liquid, owner to confirm), `ElevatedBlockLineIslandTest` minted (`f1b4b682`)
- campaign `ec19` 937 / 959, every gate green, GameTests and junction lines unchanged
- review "The junction start in hydrostatic balance
- rule (c) re-taken (2026-10-13)", A250-A261. Side branch `claude/cap-active-set` (t19, 2026-10-13, owner to decide): the velocity cap as an active-set mode (card E6-C done, campaign `ca44` 951 / 962), review section "The velocity cap as an active-set mode (2026-10-13)", A270-A279. Side branch `claude/flow-form` (t21, 2026-10-13, not recommended): the flow row in flow form on the law's laminar branch (card E6-S (c), campaign `ff13` 949 / 958, 7 suite failures), review section "The flow row in flow form near its root (2026-10-13)", A290-A298. Branch `claude/cycles-flush` (t26, 2026-10-14, owner decision D58 B1 and B3
- merged 2026-10-14, D60): the emptying form's start and its decision per active set, the cap entry of a failed iterate, the partition booking of a phase stream (`3334aa89` `6d850c94` `2739caa3` `01350240`
- campaign `cf29` 937 / 971, none worse, gates green), review section "Active-set cycles and the flush family (2026-10-14)", A350-A364
