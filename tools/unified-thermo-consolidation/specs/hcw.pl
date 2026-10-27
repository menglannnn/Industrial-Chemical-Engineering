# Condensation spec for HoldClassWorldsTest at 517a6278. Entries: [file, start, count, phrase, text].
# Each world keeps: the rule (decision, document), the world (seed, blocks, tick, minimizer logs), what held before
# the rule (message, tick) and what the test asserts. The physics narratives and rule histories go.
my $F = 'src/test/java/com/wormzjl/createcheme/runtime/fluid/HoldClassWorldsTest.java';
(
[$F, 44, 12, 'Hold class E2 of the placement-sequence fuzz', <<'X'],
    // ---------------- ActuatedIslandGeneratorClosure ----------------
    /**
     * Hold class E2 (PHASE_PORTS_REVIEW.md, 2026-09-28; decision D12): the cold-start closure of receiving generators
     * also covers islands with an actuator out. Fuzz seed 4's smallest world: a 472 kPa methane generator and a 261 kPa
     * two-phase crude generator feeding a junction network with two pressure control valves (120 and 200 kPa) and a void,
     * placed in one tick (rebuilt for D18-D20: a pipe between the crude generator and the 200 kPa valve, a pipe and a void
     * below the 120 kPa valve). Without the closure in actuated islands (a3d3a789) it held on a singular step at every
     * step size.
     */
X
[$F, 73, 17, q{A failed pass's last iterate is not a state}, <<'X'],
    // ---------------- FailedIterateIsNoState ----------------
    /**
     * A failed pass's last iterate is not a state (owner decisions D28 and D39, M8_CAMPAIGN_WORSE_WORLDS_TRACE.md): its
     * check folds what the iterate empties and proposes nothing, so no TP answer forms there and no capability hold is
     * raised about a point the network never reaches. Cold fuzz seed 430, pair-minimized to 8 blocks on the cold pilot
     * package: at tick 404 two pipes join an empty tank to a junction fed by a CO2-rich gas generator (4.89 MPa, 293 K)
     * and an LNG generator (884 kPa, 128 K); nothing can cool below about 110 K. Before the rule a junction's failed
     * iterates on the 1 Pa box floor were flashed and the island held at tick 406 on "a third fluid phase ... at 17.35 K"
     * (class C). The world still holds a step stall (class E5, open), so it is not asserted to keep up: it pins that no
     * hold is a capability hold.
     */
X
[$F, 105, 13, 'Hold class E1 of the placement-sequence fuzz', <<'X'],
    // ---------------- FilterIslandGeneratorClosure ----------------
    /**
     * Hold class E1 (PHASE_PORTS_REVIEW.md, 2026-09-28; decision D12): the cold-start closure of receiving generators
     * also covers filter islands, whose interval-start rate solve otherwise held on a generator that can only receive.
     * Fuzz seed 6's smallest world: a 130 kPa water generator three blocks below a junction pipe joining two 1 atm
     * nitrogen voids and an inline filter facing down, with a pipe and a void past it (rebuilt for D18-D20), placed in one
     * tick. Without the filter in the cold closure (bbb79790) it held at its first slice on "Newton line search stalled".
     */
X
[$F, 135, 14, 'the capped gas junctions', <<'X'],
    // ---------------- CappedJunction ----------------
    /**
     * Owner decision D59 (5), the capped gas junctions (A276, A341, t28): where every line of a junction group is choked
     * and every cap enters it (or every cap leaves it), nothing places its pressure; the line whose drive exceeds its
     * cap's pressure drop by the least is put on its law for the pass. Random fuzz seed 529's smallest world (7 blocks,
     * logs cc34): a 496 kPa methane generator feeds pipes around a placed tank and a pipe added at tick 162 closes a
     * loop. Without the rule the first Jacobian was singular at every step size (held at tick 164).
     */
X
[$F, 163, 15, 'a chord step reuses the Jacobian only while it still closes', <<'X'],
    // ---------------- ChordPressureSlope ----------------
    /**
     * Thread t29: a chord step reuses the Jacobian only while it still closes at least half of every row's gap, the
     * rows' slopes in their ends' pressures included (a row's slope in {@code ln P} is the pressure itself). Random fuzz
     * seed 886's smallest world (18 blocks at tick 239, logs fx16): a 490 kPa methane generator feeding two placed tanks
     * and, through a filter, a void, every run 10 mm at its narrowest. The filter's junctions walked from 143 to 222 kPa
     * while the chord steps stated the filter row's pressure columns at the Jacobian's 143 kPa; the Newton ran out of
     * iterations at every step size (held at tick 240).
     */
X
[$F, 203, 22, 'Hold class E6 of the placement-sequence fuzz', <<'X'],
    // ---------------- ConnectionRowScale ----------------
    /**
     * Hold class E6, option (a) (PHASE_PORTS_REVIEW.md 2026-10-12, owner decision D35): a plain passive connection's
     * hydraulic row is divided by a scale taken from the velocity-cap drops of both its ends, since at a zero-flow root
     * neither end is the donor; taken from the donor picked by the driving pressure's sign, the scale jumped by orders at
     * that root wherever the two ends held different fluids, and the step Newton stopped at its iteration limit at every
     * step size. Both worlds are world-default blocks (0.05 m pipes, 1 atm water generators at 298.15 K, 1 atm nitrogen
     * voids) rebuilt on the junction start rule (D37) and minimized with the donor's scale (logs k05).
     */
    /**
     * World-default fuzz seed 724 (4 blocks at tick 110): two 1 atm water generators level with a pipe between them, and a
     * 1 atm nitrogen void one block below that pipe. With the donor's scale the first interval held at tick 113 on
     * "Newton iteration limit at residual 1.13e-8".
     */
X
[$F, 233, 7, 'world-default fuzz seed 247', <<'X'],
    /**
     * World-default fuzz seed 247 (5 blocks at tick 140): a 1 atm water generator two pipe blocks from a junction with a
     * 1 atm nitrogen void one block above and one below. With the donor's scale the island held at tick 203 on "Newton
     * iteration limit at residual 0.357".
     */
X
[$F, 248, 14, 'a vessel that starts a step holding nothing gives nothing', <<'X'],
    // ---------------- DryEndOutflow ----------------
    /**
     * Thread t29: a vessel that starts a step holding nothing gives nothing in that step (decision D54's dry end); an
     * outflow from a dry end within the forbidden-direction tolerance reads as no flow. Random fuzz seed 708's smallest
     * world (8 blocks at tick 360, logs fx03): a 366 K crude generator at 172 kPa feeding a 200 kPa relief valve, an
     * empty placed tank looped by two lines to one junction, and a void. A pass reached the empty tank with 1.6e-22 kg/s
     * in by one line and out by the other, and the booking ended it at exactly zero: "Candidate overdraws a reservoir"
     * at every step size (held at tick 361).
     */
X
[$F, 280, 18, 'A vessel that runs dry within a step', <<'X'],
    // ---------------- EmptyingVessel ----------------
    /**
     * A vessel that runs dry within a step (decision D54, A313) is solved as a junction holding its stock from the step's
     * own start, with the same carried flows as every other pass (PHASE_PORTS_REVIEW.md "Active-set cycles and the flush
     * family (2026-10-14)", B1). Random fuzz seed 257 minimized to 7 blocks (logs cf10): a 384 kPa, 350 K water
     * generator pushes through a 1 cm pipe into a junction feeding an empty 1 m3 tank's top and, round a loop, a
     * junction at the tank's side draining 1 m down into a 1 atm void; the tank's drain carries ten times what reaches
     * it, so it runs dry within each step. Started from the static estimate the emptying pass read the void's air as
     * the drain's column and the pass sequence cycled at every step size: held at tick 154 on "Phase/device active-set
     * cycle".
     */
X
[$F, 311, 12, 'Thread t32, owner decision D59 (2)', <<'X'],
    // ---------------- HoldingNothingBooking ----------------
    /**
     * Thread t32, owner decision D59 (2): a vessel that holds nothing at the bag's resolution (1e-12 of its capacity) after
     * a step's booking keeps its residue in its inventory and is stated at its flash; no closure is solved for it. Random
     * fuzz seed 707's smallest world (12 blocks, logs cw06): a 327 K crude generator at 371 kPa and a 311 K water generator
     * at 363 kPa filling three empty tanks, the water line joined at tick 165. The booking closed a tank holding 1e-42 mol
     * of crude and 1e-84 mol of water vapour by a Newton whose water column found no finite difference trial (held at tick
     * 167).
     */
X
[$F, 347, 13, 'Thread t29, owner decision D59 (2)', <<'X'],
    // ---------------- HoldingNothingLayout ----------------
    /**
     * Thread t29, owner decision D59 (2): a vessel that holds nothing at the bag's resolution is laid out dry, and a
     * roundoff that reaches it is not an arrival: it stays booked in the inventory and the vessel stays dry. Random fuzz
     * seed 708 after its first hold was cured (17 blocks at tick 332, logs fx30): a 366 K crude generator at 172 kPa, a
     * 200 kPa relief valve, two placed tanks and two voids. A pass reached an empty tank with 2e-29 kg of crude, the tank
     * was laid out from it as two phases and its fugacity rows stalled the line search at every step size (held at tick
     * 333).
     */
X
[$F, 387, 24, q{The junction start of form (m')}, <<'X'],
    // ---------------- JunctionStartMixture ----------------
    /**
     * The junction start of form (m') (PHASE_PORTS_REVIEW.md 2026-10-12, owner decision D39): a junction several
     * suppliers drive into starts as the flow-weighted mixture still flowing in at the pressure that balances its
     * suppliers against its drains, the strongest supplier alone where the drain backs up. The start chain is retired
     * by owner decision D54 (a minted junction starts from the island's fixed point, PassiveNetwork.mintJunctions); its
     * worlds stay as regressions: each keeps up.
     */
    /**
     * Random fuzz seed 30's smallest world in which the three start rules differ (8 blocks, logs m09, m10): a 361 kPa
     * water generator at 356 K and a 366 kPa nitrogen generator at 333 K feed a junction pipe level with them, which
     * vents up into a 1 atm nitrogen void; at tick 756 a second void makes a second junction. At rest the junction holds
     * about 2.4 % nitrogen at 332 kPa. Started as the nearest supplier's water the second island held at tick 758 on
     * "Newton iteration limit at residual 0.0041"; started as the strongest supplier's nitrogen the first island held at
     * tick 104 on the water-vapour range.
     */
X
[$F, 424, 11, 'The start pressure of a junction one supplier reaches', <<'X'],
    /**
     * The start pressure of a junction one supplier reaches (PHASE_PORTS_REVIEW.md 2026-10-13): random fuzz seed 17's
     * smallest world in which the two starts differ (9 blocks, logs n09, n10). A 195 kPa nitrogen generator at 288 K
     * feeds, through two 0.01 m pipe blocks at their velocity cap, three junctions draining into two 1 atm nitrogen
     * tanks, so the junctions stand near the tanks (109.27, 101.33, 101.33 kPa). Started at the generator's own 195 kPa
     * the world held at tick 101 on "Newton iteration limit at residual 2.9e-7".
     */
X
[$F, 445, 13, 'The drain-side gas column', <<'X'],
    /**
     * The drain-side gas column (PHASE_PORTS_REVIEW.md 2026-10-13): world-default fuzz seed 992's smallest world in which
     * the start with and without it differ (11 blocks, logs o07, o08). Two 1 atm nitrogen tanks at y 0 and a 1 atm water
     * generator at y -1 meet at junctions at y 0 and y -1; at tick 165 a pipe re-forms the island. The lower junction
     * stands under 1 m of the tanks' nitrogen, 11 Pa above the generator's head, so it starts as the tanks' nitrogen and
     * nothing flows. Without the column the bound was the tanks' pressure, a few 1e-11 Pa below the generator's head, the
     * junction started as water and held at tick 168 on "Newton iteration limit at residual 0.0012".
     */
X
[$F, 471, 21, 'Hold class E4 of the placement-sequence fuzz', <<'X'],
    // ---------------- JunctionStartState ----------------
    /**
     * Hold class E4, the junction start from reachability (PHASE_PORTS_REVIEW.md 2026-10-12, owner decision D37): a
     * junction of an island formed from placed blocks starts as the stream of the nearest boundary that can drive fluid
     * into it, or, when none can, as the fluid standing highest among the boundaries in contact with it, not as the
     * island's first boundary by identity (the start chain is retired by D54; the world stays as a regression).
     * World-default fuzz seed 438 with the start rule reverted (6 blocks at tick 510, logs i17): a 1 atm water generator
     * feeds, through two level pipe blocks, a junction with one void a block below it and another beside it. Started as
     * the lower void's nitrogen, the island held at tick 573 on "Conservative reconstruction fails equation gate:
     * 1.16e-4" at every step size.
     */
X
[$F, 500, 14, 'Nothing is drawn that is not there', <<'X'],
    // ---------------- PartitionDraw ----------------
    /**
     * Nothing is drawn that is not there (PHASE_PORTS_REVIEW.md "Active-set cycles and the flush family (2026-10-14)",
     * B3): where the explicit booking of a frozen phase stream leaves a component negative, that component's streams
     * are booked at the candidate's partition against the donor's own end fraction
     * ({@code ConservativeTransport.reconstruct0}). Random fuzz seed 322 minimized to 10 blocks (logs cf21): a 276 kPa
     * crude generator and a 431 kPa methane generator feed a tank and a riser to a 1 atm void; the tank drains into a
     * second tank below. The upper tank's gas stream drew 1e-34 to 5e-24 mol per step of a component the tank never held
     * and the explicit booking held at tick 513 on "Negative transport reconstruction".
     */
X
[$F, 529, 8, 'Random fuzz seed 720 minimized to 10 blocks', <<'X'],
    /**
     * Random fuzz seed 720 minimized to 10 blocks (logs cf21): a 316.2 kPa methane generator fills two stacked tanks, the
     * upper one's top joined through 2 cm pipes to a 315.9 kPa nitrogen generator that cannot receive. The upper tank's
     * nitrogen washes out to 4.8e-24 of its amount, just above the flush fraction (D16); its frozen gas stream booked
     * twelve times what it held and the explicit booking held at tick 145 on "Negative transport reconstruction".
     */
X
[$F, 553, 17, 'a point the D13 polish re-solves', <<'X'],
    // ---------------- PolishedClosure ----------------
    /**
     * Thread t32: a point the D13 polish re-solves from the reconstruction may show a drive the converged point did not,
     * a one-way end that would receive; the pass loop closes that boundary and solves the pass again instead of refusing
     * the step. Random fuzz seed 71's smallest world (9 blocks at tick 207, logs cw05): a 317 K water generator at
     * 334 kPa fills an empty 1 m3 tank 3 m below it, beside a 146 kPa generator its lines may not feed; full, the tank
     * stands at 363.4 kPa and nothing flows. The booking stated the full tank 0.01 Pa above what the generator holds, the
     * polish converged with that pushing water back into the generator, and the step was refused at every step size
     * (held at tick 734).
     */
X
[$F, 587, 18, 'A proposal the next pass refutes is refused', <<'X'],
    // ---------------- RefutedProposal ----------------
    /**
     * A proposal the next pass refutes is refused (owner decision D39 B, 2026-10-18; D39_FAILED_ITERATE_AND_REFUTED_PROPOSAL.md):
     * a phase the check of a converged point added to a node, which the next pass drives under
     * FluidThermodynamics.VANISHING_SHARE and below the share it was added at, fails that pass; the pass is solved again
     * without it and the refusal holds for the solve (A85). Cold fuzz seed 48, pair-minimized to 6 blocks
     * (M8_CAMPAIGN_WORSE_WORLDS_TRACE.md), on the cold pilot package: at tick 1254 a pipe joins a 1.95 MPa, 149 K LNG
     * generator to an empty tank whose bottom drains into a second empty tank 2 m below; while both bags are slack the
     * lower tank holds the flashed liquid exactly on its 113.07 K bubble point, with no vapour. Before the rule the check
     * proposed 3.4e-6 of its moles as vapour, the two-phase pass drove it to 6e-9 and stalled, and the island held at tick
     * 1257 (class E5, residual 4.04e-8).
     */
X
[$F, 623, 13, q{a residue a junction's buffer leaves}, <<'X'],
    // ---------------- ResidueTank ----------------
    /**
     * Owner decision D59 (2), t28: a residue a junction's buffer leaves in an emptied tank is nothing (the bag's
     * resolution, 1e-12 of its capacity), so a tank holding it gives nothing. World-defaults fuzz seed 322's smallest
     * world (13 blocks at tick 498, logs cc40): two 1 atm water generators, two voids, pipes, and two placed tanks, one
     * 2 m above the other. The upper tank was left holding about 1e-71 kg of water, the line from it stated a 2 m water
     * column (19.5 kPa) with nothing to move, and the step stalled at 4.2106 at every step size (held at tick 501).
     */
X
[$F, 656, 18, 'R1 on the E6 rule', <<'X'],
    // ---------------- StartCheckRateFailure ----------------
    /**
     * Hold class E1, R1 on the E6 rule (PHASE_PORTS_REVIEW.md 2026-10-12, owner decision D37): a filter island's
     * start-of-interval rate solve only pre-empts a closure the steps would find, and on the island's first interval the
     * junctions it holds fixed are the mint, so its failure is no evidence: it declares no closure and the steps decide
     * (option R1). Random fuzz seed 866 with R1 reverted (13 blocks at tick 289, logs i07): a 348 kPa methane generator
     * feeds a junction whose only ways out are a 120 kPa back-pressure valve joined upstream to a 1 atm void (shut) and a
     * line through an inline filter to an 88 kPa methane generator (supplies only); nothing flows and the island rests.
     * Without R1 the check stopped at its iteration limit (residual 0.0231 on pass 0) and held the island at tick 290.
     * This world fails its check from the old junction start only; the next test guards R1 under the start rule (D37).
     */
X
[$F, 694, 10, 'The same guard with the junction start rule in place', <<'X'],
    /**
     * The same guard under the junction start rule (D37): random fuzz seed 122 with R1 reverted (9 blocks at tick 217,
     * logs i34). A 273 kPa nitrogen generator feeds, through a 0.01 m pipe, a junction draining into a 1 atm void; a
     * branch rises through an inline filter to a 91 kPa nitrogen generator that only supplies. Without R1 the check
     * stopped at its iteration limit (residual 2.51 on pass 0) and held the island at tick 218.
     */
X
[$F, 720, 19, 'Owner decision D34 of the coolprop-low-temperature batch', <<'X'],
    // ---------------- StaticColumn ----------------
    /**
     * Owner decision D34 (M5_WORSE_WORLDS_TRACE.md "Seed 322 (E6)", 2026-10-17): a phase port's static column is filled
     * with the port's phases in priority order by volume over the connection, so a trace in a long line stands as a gas
     * column; the draw still reads the port's leading phase. Random fuzz seed 322's smallest world (13 blocks,
     * tools/unified-thermo-port-m5/trace logs 09): a 431 kPa methane generator vents to two 1 atm voids; the upper of two
     * 1 m3 tanks drains through 1 m of 0.05 m line (2 L) into the top of the lower tank 2 m down. About 0.9 mL of crude
     * vapour condenses in the upper tank, and the drop line holds its gas (2.29 kg/m3, 44.9 Pa over 2 m). Read from the
     * leading phase the line was the condensate's 17.3 kPa column, which milligrams could not fill: drawn alone at a
     * carried flow of about zero it had no root, and the island held at tick 1097 at every step size.
     */
X
[$F, 770, 17, 'The step starts from the committed flows', <<'X'],
    // ---------------- StepStartFlows ----------------
    /**
     * The step starts from the committed flows (PHASE_PORTS_REVIEW.md 2026-10-13, card E6-S): an island job's first step
     * starts from the flows its committed interval ended with (PassiveIntervalSolver.Result.endpointFlows), as every
     * later step starts from the step before. Card E6-S's world (world-default fuzz seed 786, 8 blocks, logs y18, sf09):
     * a 1 atm water generator feeds a junction beside it that drains 1 m down into a void; both rise 1 m to a junction
     * joined to a nitrogen tank near 90.9 kPa, so about 4.8 kg/s circulates up one riser and down the other. With the old
     * job start (the static per-connection estimate) every job restarted the circulation and the world held at tick 3210
     * on "Newton iteration limit at residual 0.0789" at every step size.
     */
X
[$F, 800, 15, 'Hold class X of the placement-sequence fuzz', <<'X'],
    // ---------------- TopologyEventFence ----------------
    /**
     * Hold class X (PHASE_PORTS_REVIEW.md 2026-09-27): {@code PhysicalRegistry.apply} threw "Topology event reached
     * boundary/filter ... outside the islands it fenced" on the server tick where an event's fence missed an island its
     * component walk reaches. Seed 28: a pipe placed beside a lone void mid-slice, the next pipe beside it a tick later;
     * the second event's walk stopped at the first pipe, a dead end no island owned when the first event applied.
     * Seed 234: two inline filters facing each other with no boundary, then a tank beside one of them; the tank's walk
     * stopped at the first filter, whose island does not hold the second (pipe blocks between, decision D20).
     */
X
[$F, 835, 18, 'The turbulent walk on gas lines', <<'X'],
    // ---------------- TurbulentWalk ----------------
    /**
     * The turbulent walk on gas lines (B2, decision A371; PHASE_PORTS_REVIEW.md 2026-10-14): a chord step reuses a
     * Jacobian only while every connection's loss slope stays within a factor two of the slope that Jacobian states.
     * Random fuzz seed 498 minimized to 9 blocks (logs tw08), all placed at tick 100: a 480 kPa nitrogen generator feeds,
     * through two 1 cm pipes, a dead-end ladder of 1, 2 and 5 cm pipes that fills and then rests. The first solve starts
     * turbulent (Re up to 2e5) for a laminar root; the chord steps kept the resistance of flows up to twice as large,
     * closed only part of each gap (contractions 0.75 to 0.8) and the first step ran out of its 20 iterations at every step
     * size: held at tick 101 on "Newton iteration limit at residual 1.5497028030015203E-5" (logs tw09).
     */
X
[$F, 868, 20, 'One vanishing rule for every node', <<'X'],
    // ---------------- VanishingPhase ----------------
    /**
     * One vanishing rule for every node (owner decision D38 C, 2026-10-17; D38_REVERT_D36_AND_VANISHING_RULE.md): a phase
     * holding less than FluidThermodynamics.VANISHING_SHARE (1e-6) of its node's fluid moles, and a smaller share than
     * where the solve judging it started, leaves at a converged pass and at a failed iterate, in a junction as in a
     * vessel. Cold fuzz seed 30, pair-minimized to 11 blocks (M7_NEW_COLD_HOLDS_TRACE.md), on the cold pilot package: an
     * LNG generator (6 MPa, 169.7 K) pours about 72 kg/s into a junction (n5) that feeds a 1 atm void and, through 1 cm
     * lines, a 1 m3 tank; at tick 1593 a pipe joins a CO2-gas generator (2.03 MPa, 292 K) to the tank and that line. The
     * re-minted start states n5 flashed at 2.03 MPa with 4.5 % vapour that the solve must remove (n5 goes back to about
     * 3.49 MPa, all liquid); the rate solve's failed iterate left 1.0e-8 of its moles as vapour, above the trace share,
     * and the trial layout kept it: held at tick 1595 (class E5, a line-search stall at residual 1.47e-9).
     */
X
[$F, 911, 16, 'The velocity cap is an active-set mode of the connection', <<'X'],
    // ---------------- VelocityCapMode ----------------
    /**
     * The velocity cap is an active-set mode of the connection (PHASE_PORTS_REVIEW.md 2026-10-13, card E6-C): a line at
     * its cap states the cap flow of the end it leaves, as a pump at its target states its flow, and the pass loop decides
     * when a line enters and leaves the mode. Card E6-C's world (random fuzz seed 529, 7 blocks, logs y19, y25): a
     * 496 kPa methane generator fills tank B (nitrogen, 441 kPa by tick 1157) through a junction and two pipes at their
     * cap, and the last block joins tank B to tank A (1 atm) through a 2 cm pipe. With the cap as a branch of the flow
     * row the junction's pressure jumped about 2e5 Pa per iteration and it held at tick 1159 on "Newton iteration limit
     * at residual 0.65" at every step size.
     */
X
[$F, 938, 17, 'G2 follow-up', <<'X'],
    // ---------------- VesselClosureEquilibrium ----------------
    /**
     * G2 follow-up (PHASE_PORTS_REVIEW.md 2026-10-12): every reconstruction closes each vessel to its booked inventory
     * (decision D36, InventoryEquilibrium.close) in the equilibrium the step solved it with, a trace component held in
     * one phase included. Closed in full equilibrium instead, a crude tank at rest stood 0.71 Pa (3.2e-6) above the
     * step's solution, the equation gate refused every reconstruction and the island held on "Interval substep limit".
     * Both worlds are the smallest of the random fuzz seeds that held this way (logs j01, j04), with the seeds' exact
     * doubles.
     */
    /**
     * Random seed 400, 3 blocks: a light crude generator (339.33 K, 220.17 kPa, liquid) feeds a 1 m3 nitrogen tank at
     * 1 atm through one 50 mm pipe at one height; the tank fills to the generator's pressure and rests 0.222 m3 liquid
     * (log j03). Before the fix the closure moved the resting tank 0.71 Pa up and the island held at tick 1289.
     */
X
[$F, 963, 7, 'Random seed 737, 5 blocks', <<'X'],
    /**
     * Random seed 737, 5 blocks: a junction pipe joins a liquid crude generator (299.34 K, 326.9 kPa, 10 mm), a two-phase
     * one (391.7 K, 248.8 kPa, 20 mm, which closes) and, one block up through a 100 mm pipe, a 1 m3 nitrogen tank that
     * rests 71 % liquid at 318.48 kPa. Before the fix the closure moved it 0.032 Pa up, no polish passed the gate (520 of
     * 520 refused) and the island held at tick 978.
     */
X
)
