# Condensation spec (batch 2) at 5e4b4722: PhaseLayout, ConservativeTransport, NetworkReduction, PassiveIntervalSolver,
# VesselCrystals, EquilibriumResult, PengRobinsonKernel, TangentPlaneStability. Entries: [file, start, count, phrase, text].
my $M = 'src/main/java/com/wormzjl/createcheme';
my $PL = "$M/science/fluid/solver/PhaseLayout.java";
my $CT = "$M/science/fluid/network/ConservativeTransport.java";
my $NR = "$M/science/fluid/network/NetworkReduction.java";
my $PI = "$M/science/fluid/network/PassiveIntervalSolver.java";
my $VC = "$M/science/fluid/network/VesselCrystals.java";
my $ER = "$M/science/thermo/phase/EquilibriumResult.java";
my $PK = "$M/science/thermo/PengRobinsonKernel.java";
my $TS = "$M/science/thermo/TangentPlaneStability.java";
(
[$PL, 107, 9, 'span by which the bulk difference floor', <<'X'],
    /**
     * The span by which the bulk difference floor of an amount may exceed the amount where a row reads it through a
     * logarithm: 1e4, so its step (the Newton's 1e-6 of the scale) stays within 1 % of the amount and the logarithm's
     * secant within 0.5 % of its tangent. The bulk floor of a gas-rich vessel (1e-4 of its amount) is kept wherever such
     * an amount is at least 1e-8 of its vessel; the stand-in of ice in a cold gas holds steam of about 1e-5 Pa at 150 K,
     * 1e-11 of its vessel, where the floor's step was 8 times the steam and the Newton on the saturation row alternated.
     */
X
[$PL, 173, 17, 'The scale of the finite-difference step of this column', <<'X'],
    /**
     * The scale of the finite-difference step of this column: {@link #differenceScale}, except that an amount whose value
     * and seed both lie below {@link #TRACE_DIFFERENCE_GUARD} is differenced relative to its own size, the guard applying
     * only where both are zero. Applied to every such amount, a trace of 1e-36 of its node's amount was differenced with
     * a step as large as itself, and the equilibrium rows, which read a trace phase's amounts through logarithms and
     * ratios, got a secant 20 to 35 % below the tangent: the Newton contracted by only 0.06 to 0.5 per iteration and
     * ended a little above the tolerance at every step size. Bitwise wherever the value or the seed is at least the
     * guard. Not reached: a trace near the bottom of the double range (about 1e-300 of its node), whose logarithmic rows
     * overflow the factorization; that end needs a decision on traces, not a difference step.
     */
X
[$PL, 286, 14, 'every vessel is a bag with a stiff wall', <<'X'],
    /**
     * Owner decision D54: every vessel is a bag with a stiff wall. Its wall carries no load while the contents are
     * smaller than the capacity, so the pressure is the ambient and the volume the contents' own; once the contents
     * would exceed the capacity the wall holds them there and the pressure comes from the contents. The wall law
     * {@code P = P_ambient + k max(0, V - V_max)} in its stiff limit: {@code V <= V_max}, {@code P >= P_ambient}, one of
     * the two an equality, as one row {@code max(taut, slack) = 0} with {@code taut = (V - V_max)/V_max} (the fixed-volume
     * row it replaces, to the bit) and {@code slack = 2 (P_ambient - P)/P_ambient}, twice as steep in pressure as any
     * contents' own volume, so a vessel exactly at the kink reads the taut row. Continuous and smooth on each branch; a
     * Newton on it takes the slopes of the branch the iterate is on ({@code branch}, {@link Wall}).
     */
X
[$PL, 306, 17, 'The branches of a vessel', <<'X'],
    /**
     * The branches of a vessel's wall at one state: {@code taut}, whether the wall carries the contents ({@link #wallRow}
     * on its taut branch); {@code full}, whether the bag is at the capacity ({@link #bagVolume}). Decision A423: the wall
     * row and the bag volume are continuous and piecewise smooth, and a one-sided difference across their kink reads the
     * far branch's slope for a row on the near one; a full tank of air at the ambient, the common state of a vessel in
     * the game, sits on that kink (five full nitrogen tanks seeded 7.5e-9 of their capacity over it stalled the line
     * search at 5.3e-8 at every step size, the chord steps keeping the taut slopes across the branch flips). The rule: a
     * Jacobian sweep states every wall row and bag volume on the branches of the point it is built at
     * ({@link SparseNewton.Equations#linearising}, a semismooth Newton), and a chord step is refused once a vessel's
     * branches differ from the Jacobian's ({@link SparseNewton.Equations#linearisationHolds}). The rows and roots are
     * unchanged.
     */
X
[$PL, 395, 19, 'The mixing block of', <<'X'],
    /**
     * The mixing block of {@link #junctionResidual}: everything the inflow and the net flow move, and nothing the
     * equilibrium rows below it read. Returns the first row after this block. {@code retainedPressure} is positive only
     * for a junction no open connection reaches, and is then the pressure it keeps: nothing but the hydraulics decides a
     * junction's pressure, and with every connection closed the net flow row is the sum of the closed rows, satisfied
     * identically, so the block would be one equation short of its own pressure and the factorization singular (a
     * junction between two stopped filters or two shut pumps). An isolated junction keeps the pressure it had, as it
     * keeps the composition it holds.
     */
X
[$PL, 430, 11, 'Three aggregate balances', <<'X'],
    /**
     * Three aggregate balances; population identities are reconstructed outside Newton. The moments are read from the
     * unknowns rather than from the decoded state, so that each row stays exactly linear in its own unknown everywhere,
     * including past the nonnegative boundary {@link #decode} projects onto: read from the projected state the row would
     * be constant for any trial with a negative moment, a column of zeros, and every node downstream of a filter sits on
     * that boundary. The two agree at every point the solver can accept.
     */
X
[$PL, 468, 18, 'Components whose omitted phase this converged state no longer supports', <<'X'],
    /**
     * Components whose omitted phase this converged state no longer supports omitting, marked in {@code promoted} for
     * the next active-set pass; returns how many were newly marked. The test is V3's reinsertion inequality on this
     * layout's own equilibrium relation: at a converged point the retained components satisfy
     * {@code ln y_i = ln x_i + ln phi_i^L - ln phi_i^V + ln(P/Pc)}, and the equation of state fills {@code ln phi_i} of
     * an absent component at infinite dilution, so the same expression says what mole fraction the omitted phase would
     * hold; when that reaches {@link TraceTruncationPolicy#REINSERTION_FACTOR} times the cutoff the component gets both
     * phases back (the factor is the hysteresis against alternation). {@link #encode} already seeds a component with an
     * empty phase at exactly this ratio.
     */
X
[$PL, 506, 18, 'per-component phase support of one node, derived from a full-basis reference', <<'X'],
    /**
     * The per-component phase support of one node, derived from a full-basis reference state and then frozen: V3's
     * rule, on the seed's own phase compositions. The reference is the seed the layout is built from and never a Newton
     * iterate, so the unknown set of a pass is fixed before the pass starts, which lets the sparsity pattern, the
     * colouring, the ordering and the retained factorization be keyed on it. A component is reduced to one phase when
     * its mole fraction in the other phase is below the cutoff while this one's is not; when both are below it both are
     * kept. {@code promoted} components are kept in both phases regardless (a reactivation may not be undone by a later
     * pass of the same solve). A node with one active hydrocarbon phase gets that phase for every present component; at
     * {@link TraceTruncationPolicy#OFF} the support is the unreduced layout's.
     */
X
[$PL, 527, 10, 'for a node that is a pipe junction', <<'X'],
    /**
     * {@link #support} for a node that is a pipe junction ({@code junction}) or a vessel. A junction whose seed holds a
     * carried trace gas ({@link FluidThermodynamics#carriedTrace}, decision D41) has no phase of its own for it: its gas
     * components are omitted traces with no unknown and no row, below what the junction's rows resolve (an amount
     * unknown there has a difference step no row registers, a zero column, and with the gas's water vapour the
     * saturation row read the ratio of two such amounts and never contracted). The transport reconstruction carries them
     * exactly and books more when more arrives; a share at or above the trace is a phase appearance.
     */
X
[$PL, 542, 13, 'with the support read from the fluid phases of the', <<'X'],
    /**
     * {@link #support(FluidThermodynamics.State, boolean[], TraceTruncationPolicy, boolean[], boolean)} of a node of
     * {@code model}'s network, under its trace truncation policy, with the support read from the fluid phases of the
     * answer the seed was adopted from (owner decision D29 A). Where the seed's liquid is a crystal's condensed share alone
     * ({@link FluidThermodynamics#condensedShareOnly}), that liquid is no phase of the answer and its composition says
     * nothing of what a liquid there holds: the answer's one fluid phase is the vapour, and every component the vapour
     * holds is admitted to the liquid too ({@link PhaseSupport#BOTH} where the rule gives {@link PhaseSupport#VAPOR_ONLY}),
     * so the supersaturated liquid takes up the species that arrive with it (methane held in the vapour only cooled an
     * LNG-fed tank to methane's 90.69 K floor). Elsewhere the same support as without the model.
     */
X
[$PL, 570, 7, 'A masked component this reference holds none of', <<'X'],
            // A masked component this reference holds none of, on a point with no hydrocarbon phase to hold it in, is an
            // omitted trace: inventing a phase for a species nothing delivers is the phantom-trace defect. It stays in the
            // mask, so a pass whose seed does carry the component states it again. A component this reference does hold
            // with no phase to hold it in is a real appearance problem and is left to the layout to refuse.
X
[$CT, 15, 19, 'transport linear algebra, retained across its reconstructions', <<'X'],
    /**
     * One island's transport linear algebra, retained across its reconstructions as {@code SparseNewton.Workspace.Shared}
     * is retained across a fork family: one EJML storage every reconstruction refills in place, and the fill-reducing
     * ordering of each structure the island produces (a step solve and a rate solve of the port graph do not share a
     * sparsity pattern, so the orderings are a small keyed cache). Without it each reconstruction built its own
     * {@link SparseLuSolver.Storage} and ordering, three or four times per interval. Reuse is bitwise: the storage clears
     * every buffer it reads, an ordering is only reused for a matrix with the identical structure, and each factorization
     * is consumed inside the reconstruction that produced it.
     */
X
[$CT, 101, 20, 'every junction is held at its buffer', <<'X'],
    /**
     * {@code frozenJunctions} (rate evaluations only): every junction is held at its buffer's composition, as the
     * rate-only rows of {@code PassiveStepSolver.Equations.junctionInflow} hold it, and its accumulation over dt is held
     * back in the projection's {@link HeldJunctions} (owner decision D46: a junction's buffer is not inventory).
     * {@code draws}: per connection, the mass flow each phase ({@link FluidThermodynamics#GAS}, {@link FluidThermodynamics#OIL},
     * {@link FluidThermodynamics#WATER}) supplied to the outflow of a phase-port end at the solved flow (decision D11),
     * null for a connection that drew the bulk, null altogether for a graph none of whose outflows drew phases.
     * {@code supports}: per node, the frozen phase support the step solved it with; each vessel is closed to its booked
     * inventory in that equilibrium ({@link InventoryEquilibrium#close}). {@code emptying} (null: none): per node, a
     * vessel that runs dry within the step and stands in {@code graph} as a junction holding its stock (decision D54):
     * {@link Emptying#drain} its stock's mass over dt and {@link Emptying#work} the ambient's work on its collapsing bag;
     * its booking pins its end mass to zero, and its energy's residual leaves to the ambient boundary at the vessel.
     */
X
[$CT, 133, 19, 'mass fractions of a node whose composition this reconstruction does not solve for', <<'X'],
    /**
     * The mass fractions of a node whose composition this reconstruction does not solve for: a prescribed boundary. It
     * is a composition, not a conserved amount, so every entry is read off one state, the candidate, and the stored
     * inventory supplies nothing but the split of the candidate's aggregate solid mass across its populations (a Newton
     * candidate carries its solids as the three aggregate moments and keeps the seed's population list unscaled,
     * {@link com.wormzjl.createcheme.science.fluid.solver.PhaseLayout#decode}). Reading the fluid half off the candidate
     * and the solid half off the stored inventory, both over the candidate's total mass, left a junction's fractions
     * summing to 1.000115804 after the Newton had moved its free total by 0.099 %. Where candidate and inventory agree
     * the scale is exactly 1.0 and the result is bit for bit what the two reads produced.
     */
X
[$CT, 278, 9, 'Unbacked traces', <<'X'],
                // Unbacked traces. A vessel's Newton seed carries an entry trace (1e-12 of its amount) of every component the
                // island can bring it, and the candidate keeps some of it; the implicit bulk booking lands such a component
                // at exactly zero in a donor whose inventory lacks it, but a frozen stream would book the candidate's trace
                // out of an inventory of zero and refuse the step as a negative reconstruction at every step size (5e-25 mol
                // of nitrogen in a methane tank drawn through a VAPOR port). A donor that neither holds a component nor
                // receives anything in the step cannot deliver it, so its stream books none of it; the ledger stays exact.
X
[$CT, 348, 16, 'Nothing is drawn that is not there', <<'X'],
        // Nothing is drawn that is not there (B3). A phase stream's fractions are frozen from the Newton's candidate, whose
        // balance rows resolve a node's amounts to the tolerance of its total, so a trace the candidate states in the drawn
        // phase can exceed what the node holds and receives over the step (a tank holding nitrogen at 4.8e-24 of its amount
        // drew twelve times its 4.9e-22 mol through its gas stream), and the explicit booking left the component negative.
        // In real life a phase carries a component in proportion to what the node holds of it. So where the explicit
        // booking leaves a component negative anywhere, that component's streams out of solved nodes are booked implicitly
        // at the candidate's partition, r = w_stream / w_bulk, times the donor's own end fraction: the donor's column gains
        // dt q r on its diagonal and -dt q r at its receiver, so it stays diagonally dominant and the solution cannot be
        // negative; donor and receiver book the same amount (the ledger stays exact) and the energy booking is unchanged.
        // Only a component that came out negative is re-solved, so every step accepted before is unchanged to the bit.
X
[$CT, 530, 10, 'a vessel is stated at what it holds', <<'X'],
        // Decision D36, G2: a vessel is stated at what it holds. Its amounts and energy are booked above, exactly; its
        // temperature and pressure follow from them at its volume (InventoryEquilibrium.close), not from the candidate's,
        // so no booked inflow is ever held in a state that does not show it. A junction keeps its state: it has no volume,
        // and its mass is pinned. The equilibrium a vessel is closed to is the one the step solved it with (its phase
        // support), which the equation gate then checks it against. Decision D54: the wall moves its bag volume at the
        // ambient pressure, so the energy booked above is what the vessel holds plus the work it did on the ambient,
        // P_ambient (V_bag,end - V_bag,start), which leaves the island as the ambient boundary's energy, booked at the vessel.
X
[$CT, 567, 25, 'The flows a reconstruction books so that every junction passes exactly what it receives', <<'X'],
    /**
     * The flows a reconstruction books so that every junction passes exactly what it receives and its buffer keeps the
     * mass m_J it was sized with ({@link PassiveNetwork#sizeJunctionHoldups}): a junction is a fitting full of fluid, its
     * mass does not change (the fitting's mass continuity, owner decision D46). The Newton's net-mass row at a junction
     * closes only to its tolerance, so {@code dt (In - Out) != 0} by that residual, which a buffer with no restoring term
     * would integrate into m. The residual is booked onto the junction's own connections, per junction J in flow order:
     * with a positive outflow its outflows are scaled by {@code f_J = In'/Out}, In' what its inflows deliver as booked (an
     * upstream junction's factor applied, filter capture removed), so the excess leaves with the junction's own
     * composition; with no outflow its inflows from non-junction donors are scaled by
     * {@code g_J = (Out - In'_junction)/In'_other} when that lies in [0, 1] (an inflow is only ever reduced, since
     * amplifying one moves the junction's composition off the Newton's); otherwise (a junction cycle, or nothing to
     * scale) it stays unpinned. Applies to every reconstruction. Exact: every edge still books one amount at both ends,
     * so the change of the vessels plus the junction term equals the boundaries per component, solid population and
     * energy; only the booking moves, the flows, states and pipe histories are unchanged.
     */
X
[$CT, 630, 12, 'The node restated with the booked amounts in the phases it already holds', <<'X'],
    /**
     * The node restated with the booked amounts in the phases it already holds, each component split as it was. A
     * hydrocarbon booked into a node that holds no hydrocarbon phase at all (a junction seeded as pure water that the
     * step's flows deliver nitrogen to, hold class E3) has no phase to go to and no partial pressure to be evaluated at,
     * so such a node is flashed at its temperature and pressure instead, the phase set the booked amounts form; it is
     * never committed as it stands, the pass loop reseeds the node from it and solves the pass again. Every other node is
     * restated exactly as before, with the flash's trace-phase rule applied to the split (decision D34,
     * {@link FluidThermodynamics#foldTracePhases}): a node is never committed with a phase its flash would not give it.
     */
X
[$NR, 10, 31, 'The network reduction stage', <<'X'],
/**
 * The network reduction stage: what of a placed network is solved, and as what (owner decisions D17 to D19). It runs
 * where islands are formed ({@code TopologyCompiler.compile}, called by {@code PhysicalFluidTopology.compile} for every
 * topology event and every load), in this order: connectivity (placed blocks become nodes and links; only pipes
 * connect, owner decision D20); dead ends ({@link #pruneDeadEnds}, D18: a branch that leads to no tank, generator or
 * void carries no flow and is left out, its devices owned by no island); runs (maximal chains through blocks with two
 * connections become one connection); split at fixed boundaries ({@link #splitAtFixedBoundaries}, D17: parts of a
 * network that meet only at a generator or a void are separate islands, the boundary belonging to each); direction
 * feasibility ({@link #drives}); rest ({@link #atRest}, D19: an island nothing can drive is not solved, its interval is
 * the identity). A junction has no start state of its own (owner decision D54): its buffer is a relaxation row's state
 * (D46), minted from the island-wide fixed point's last round ({@link PassiveNetwork#mintJunctions}). Dead ends and the
 * boundary split read no state and commute, so every compile of the same blocks reduces them the same way; the drives
 * and the rest test read the island's states and are evaluated on the graph every interval job receives
 * ({@link PassiveIntervalSolver#solve}), so every path that solves an island decides the same way, from a worker.
 */
X
[$NR, 121, 43, 'Step 6, rest (owner decisions D19 and D22)', <<'X'],
    /**
     * Step 6, rest (owner decisions D19 and D22): whether nothing in an island can drive a flow, so its interval is the
     * identity and it is not solved ({@link PassiveIntervalSolver#solve} returns {@link #restInterval}; the island then
     * certifies as at rest). Decided from the structure and the present states only, before any Newton:
     * <ul>
     * <li>Only islands holding no trace an accepted step would flush (decision D16) and no module transfer are asked.
     * Solids do not keep an island awake (owner decision D23 (6)): a particle moves only with the flow that carries it
     * and grows or melts only in a vessel's refresh, which runs before this test. At an interval start every transport
     * closure is read as open (owner decision D33), since that interval re-decides them ({@link #reopened}); a filter
     * stopped at its capacity stays closed. A pressure valve drives forward by the smaller of its static drive and its
     * excess over the set point, never back; the rest interval reports it CLOSED. An empty vessel stands at the ambient
     * and gives nothing (decision D54).</li>
     * <li>An island that holds no stock (no generator, no vessel holding anything) is at rest whatever its devices are
     * set to: a junction's buffer is never material (D46), and one no source reaches holds a placeholder (D55).</li>
     * <li>A pump or compressor that cannot move anything this slice (set to no flow, or refused for the slice,
     * {@code refused}) is a closed connection; any other at its target moves its set flow whatever the pressures, and
     * the island is solved.</li>
     * <li>Otherwise it is at rest when the gauge reads no drive and no flow (owner decisions D42, D43, D58). A gauge stands
     * where stock is; a junction holds no stock and no gauge. So: no connection between two stock nodes has a static drive
     * ({@link #drives}) above {@link #REST_RESOLUTION} in an allowed direction, no path of junctions between two such
     * nodes drives above it ({@link #pathDriven}), no loop of connections has a net drive above it ({@link #loopDriven}),
     * and, when the state is a step's end, no connection carries more than the flow the resolution drives through it
     * ({@link #resolutionFlow}). The state itself is never rounded; it is only no longer stepped.</li>
     * </ul>
     *
     * @param flows   the mass flows of the step that ended in this state, or null at an interval start (flows are not state)
     * @param refused the movers refused for the slice ({@link InletPhase#decide}), one flag per connection; null for none
     */
X
[$NR, 296, 10, 'read where a gauge stands', <<'X'],
    /**
     * Owner decision D42, read where a gauge stands: a junction holds no stock and no gauge, so the drive that counts is
     * the one between two nodes that do, summed along a path of junctions between them (the junctions' pressures cancel,
     * so a path's drive is its end pressures less its columns: a chain of five tees each 0.09 Pa apart is 0.45 Pa between
     * its tank and its generator, and the tank still fills). Whether some path between two such nodes drives above
     * {@link #REST_RESOLUTION}: the largest path drive arriving at each junction from a stock node, relaxed through the
     * junctions (at most one round per junction), then closed at a stock node. A loop of junctions with a net drive below
     * the resolution can only raise a path by that much per round: the test may read a drive that is not there, never
     * miss one.
     */
X
[$PI, 10, 11, 'Adaptive internal substeps', <<'X'],
/**
 * Adaptive internal substeps; the requested simulated interval is independent of the caller's wall deadline. Every
 * step is one backward-Euler step, {@link PassiveStepSolver#solve}: one Newton with its active-set passes, one exact
 * conservative reconstruction, the equation gate and the conservation audit. A converged step is accepted when no
 * vessel's pressure or mass changed by more than {@link #STATE_CAP} of itself. Against the second-order TR-BDF2 it
 * replaced: one Newton solve per step instead of two plus an embedded companion, no rate solve per step, and zero
 * nonconvergence on the mixed-gas junction matrices; trajectories are first order (a decay lags by up to one 5 s
 * cycle), which the owner accepted (decision D4).
 */
X
[$PI, 105, 12, 'which pumps and compressors are refused for the whole slice', <<'X'],
    /**
     * Decision D6: at a slice's start, on its committed state {@code initial} and the committed endpoint modes, which
     * pumps and compressors are refused for the whole slice because their supply is the wrong phase
     * ({@link InletPhase#decide} over the slice {@code duration}); the step solver holds them in
     * {@link FlowControl.Mode#INLET_WRONG_PHASE} in every step, rate seed and segment of the slice. The input is the
     * committed endpoint mode, never solver-local history, so a certified island woken with a fresh solver decides
     * exactly as the island that solved every interval. {@code initial} is the interval start with its transport closures
     * read open (owner decision D33, {@link NetworkReduction#reopened}): read as they stood, a suction line the last
     * interval closed left its pump "inlet dry", and a refused pump cannot drive the flow that would suspend the crystals.
     */
X
[$PI, 162, 29, 'The interval, stepped by backward Euler under the state-change controller', <<'X'],
    /**
     * The interval, stepped by backward Euler under the state-change controller.
     * <ul>
     * <li>Acceptance: a converged step is accepted when the largest relative change of a vessel's pressure (over
     * max(100 Pa, P0)) or mass (over m0) is at most {@link #STATE_CAP}; otherwise it is refused and the step halved. A
     * change that halving did not reduce (at least 0.9 of the refused change at twice the step) is an algebraic jump of
     * the node's state, not a trajectory error, and the step that shows it is accepted (water entering a dry gas tank
     * drops its pressure about 5 % at any step size). A step at or below the transition floor is accepted whatever its
     * change. No rule on accepted-mode transitions: a symmetric pair of void edges opens and closes together, and a
     * one-transition rule halved every event to the floor.</li>
     * <li>Growth: after an accepted step the estimate grows by {@code clamp(0.9 sqrt(cap/change), 0.5, 2)} (2 at zero
     * change), bounded by the maximum step; a Newton, equation-gate or conservation failure halves.</li>
     * <li>Boundary reopen: a converged step that holds a passive run closed although its own end states drive the run in
     * an allowed direction ({@link PassiveStepSolver#reopenable}) is refused and solved again with that run exempt from
     * the start-of-solve closures, each run at most once per attempted step.</li>
     * <li>Cold start: while no step of this structure has been accepted, one rate solve of the port graph supplies the
     * junctions' starting states, and the balanced pressure seed treats vessel neighbours as held.</li>
     * <li>Events: the stage guard's {@code checkRate} runs at every step start, on the previous accepted endpoint, else on
     * the endpoint carried from the last interval while no connection's identity or closure changed, else on one rate
     * solve of the port graph; {@code checkFilters} runs on every step's end. A declared transition is searched onto by
     * halving.</li>
     * <li>Limits: the attempt cap and the twenty-consecutive-rejection limit; transition refusals have their own bound.
     * An exhausted interval states its rejection map.</li>
     * </ul>
     */
X
[$PI, 322, 10, 'Removes every component below', <<'X'],
    /**
     * Removes every component below {@link #TRACE_FLUSH_FRACTION} of its vessel's or junction's total amount, restates
     * the node at its temperature and pressure, and books exactly what it removed: a boundary transfer at the node's
     * own id, signed like a port's (the removed moles negative), with no energy (no stream carries potential energy,
     * owner decision D37). The interval's ledger therefore closes by construction and a checkpoint sees the flush as any
     * other boundary transfer; a certificate is not issued on an interval that contains one, since a one-time removal
     * is not a rate. Returns null when nothing was flushed.
     */
X
[$PI, 388, 21, 'The start of an island job', <<'X'],
    /**
     * The start of an island job ({@code graph}, the island's committed state) given the island's committed interval
     * ({@code committed}; null for an island with none). Afterwards this solver holds exactly what a fresh solver derives
     * from the committed interval: when the job's island is the committed interval's (the same pipe identities and node
     * ids), its endpoint (modes, heads and flows) is carried into the job as every accepted step's endpoint is carried
     * into the next step ({@link PassiveStepSolver#replayStart}, {@link #carriedFlows}); otherwise nothing is carried, and
     * the cold-start seed graphs are dropped. Committed state only, because a certified island is woken with a fresh
     * solver and a replay must reproduce the island that solved every interval bit for bit: a Newton workspace carried
     * across a job boundary opened the next job's first solve on an older factorization, and through the controller's
     * step growth the last-digit difference reached the committed inventories (decision D8). The endpoint is carried
     * because flows are continuous in time: started from the static per-connection estimate instead, a riser at rest
     * restarted at its velocity cap and the Newton walked it back by halving until it ran out of iterations.
     */
X
[$VC, 17, 44, 'The crystal part of a vessel', <<'X'],
/**
 * The crystal part of a vessel's refresh (owner decisions D22 (5) and D23): the one place crystal mass moves. A crystal
 * is a population of the vessel's solids ({@link NetworkCrystals}); inside a step it is inert, and at each interval and
 * solid-event segment start the vessel's refresh ({@link InventoryEquilibrium#refresh}) equilibrates it with the
 * vessel's fluid, about 5 s of lag, which the owner accepted.
 *
 * <p>Physics. A well-mixed closed tank at fixed energy and volume (a taut wall), or at the ambient pressure and fixed
 * enthalpy (a slack bag, D54), has one equilibrium; where a crystal is in it, the crystal's chemical potential equals its
 * component's in the fluid, and the latent heat of the mass that froze or melted is in the tank's energy. Presence: at
 * the vessel's own closed state the fluid's drive to each crystal, {@code D = ln f_i(fluid) - ln f_s}, is read; a vessel
 * holding no crystal with no positive drive, or holding its crystals at zero drive ({@link #DEADBAND}), keeps every bit
 * (a vessel at rest is a fixed point); otherwise the equilibrium engine's crystal test decides which crystals are
 * present, at fixed volume while the wall is taut and at the ambient pressure while it is slack, switching to the other
 * when a fixed-volume answer lies below the ambient or a constant-pressure one above the capacity. Amount: each present
 * crystal's amount is one more unknown of the vessel's own closure beside its fluid's ({@link Joint}), its chemical
 * potential equal to its component's its row, the energy and wall rows holding the crystal's mass at its record's
 * constants; the vessel's pass loop lays its fluid out ({@link JointClosure}, decision D30), and a crystal the closure
 * would hold below zero mass is absent and the closure solved again without it. Booking: the offset of the mass that
 * froze or melted, and the ambient's work on a moving bag {@code -P_amb (V_bag - V_bag,0)} (D54), are booked as a
 * boundary transfer of energy alone at the vessel. Size (D23, one size per solid in each stock): mass that freezes
 * enters at {@link #precipitateDiameter} of the liquids it freezes from, the vessel's own before the refresh
 * ({@link #freezing}), merged into the stock's one population at the mass-weighted mean
 * ({@link SolidInventory#oneSizePerSolid}); mass that melts leaves at the stock's size (methane with 30 % CO2 freezes at
 * about 0.5 mm; frost from a gas or the snow of a let-down at 0.25 mm). Above the crystals' ceiling a vessel holds none of
 * the package's crystals. The free water's ice is water's own rule, on every package: free water below its melting line
 * freezes here, from its liquid and from the gas's steam; ice and a competing crystal coexist. A vessel holding no fluid
 * is the dry-solids vessel: its crystals have no fluid to exchange with.
 */
X
[$VC, 307, 12, 'The joint closure', <<'X'],
    /**
     * The joint closure ({@link Joint}) as the inner closure of the vessel's pass loop ({@link InventoryEquilibrium#passes},
     * decision D30): each pass lays the fluid out with the phases of its start, the crystals at the engine's amounts;
     * {@link #joint} and {@link #solved} are the last converged closure, the one the loop answers with. The engine's
     * answer states its own crystal, whose volume differs from the record's constant one (CO2-I in LNG at 143.8 K: 1625
     * against 1571 kg/m3), so in a rigid tank full of LNG the engine's answer holds a 0.45 L bubble the record's crystal
     * leaves no room for: the closure started in the answer's layout empties the bubble, the loop's trace fold removes
     * the phase, and the closure is solved again without it.
     */
X
[$ER, 248, 8, 'prefix of the detail of the typed hold of a crystal competition', <<'X'],
    /**
     * The prefix of the detail of the typed hold of a crystal competition whose answer needs a phase set the engine does
     * not implement: two liquids beside a crystal, three fluid phases beside a crystal (that fluid has a product proved
     * unstable), or two crystals forming. The result is {@link Status#UNSUPPORTED} with
     * {@link UnsupportedReason#NOT_IMPLEMENTED}, the fluid answer's diagnostics, and no phases.
     */
X
[$PK, 8, 10, 'Allocation-free PR78 hydrocarbon kernel', <<'X'],
/**
 * Allocation-free PR78 hydrocarbon kernel. One {@link Workspace} belongs to one solve; liquid and vapor evaluations at
 * a common temperature share its temperature-dependent pure-component arithmetic. Promoted unchanged from the V3
 * column's package, the critical constants read into arrays by the constructor, so the column and the fluid network
 * evaluate their phases on the same arithmetic.
 */
X
[$PK, 24, 9, 'How flat the cubic may be at the selected root', <<'X'],
    /**
     * How flat the cubic may be at the selected root before its derivative is refused. Every state derivative divides by
     * this slope, so its smallness makes the whole bundle meaningless; the bound is relative to the size of the terms
     * the slope is assembled from, so it measures cancellation, and lies far below any separation a column state
     * reaches (refusing costs the caller a fallback to differencing).
     */
X
[$PK, 410, 20, 'Fills {@code output} with the value evaluation and its first derivatives on the very same root', <<'X'],
    /**
     * Fills {@code output} with the value evaluation and its first derivatives on the very same root: the values come from
     * {@link #evaluatePrepared} itself, and everything after reads the mixture terms that evaluation left in the
     * workspace. {@code Z} is a root of the fixed cubic {@code Z^3 + c2 Z^2 + c1 Z + c0} whose coefficients are
     * polynomials in {@code (A, B)}, so the implicit function theorem gives {@code dZ/dA} and {@code dZ/dB} from one
     * derivative of that cubic, {@code 3Z^2 + 2 c2 Z + c1}, which is also the quantity that vanishes when two roots
     * coalesce: the guard refuses such a state instead of returning the enormous number the division would give. The
     * partial molar residual enthalpies are {@code -R T^2 (d ln phi_i / dT)}, and their mixture sum reproducing
     * {@code H^R} is a check of the temperature derivative against the closed-form residual enthalpy.
     */
X
[$PK, 791, 9, 'No physical root (one above', <<'X'],
    /**
     * No physical root (one above {@code B}) found for the requested slot. The cubic always has one mathematically, being
     * {@code -2 B^2 < 0} at {@code Z = B} and unbounded above; floating point loses it only far outside any physical
     * state ({@code A B} or {@code p^3} overflow, a hydrocarbon partial pressure of 1e183 Pa, or the root's distance above
     * {@code B} below one ulp of {@code B}, 6.6e25 Pa). An {@link IllegalStateException}, the type every caller of the
     * kernel catches; the fluid network's property layer turns it into its refused trial ({@code HydrocarbonModel.RootAbsent}).
     */
X
[$TS, 9, 38, 'tangent-plane test of whether one phase of a feed is stable', <<'X'],
/**
 * Michelsen's (1982a) tangent-plane test of whether one phase of a feed is stable, on the PR78 kernel's own fugacity
 * coefficients and its analytic composition derivatives. The feed {@code z} is evaluated on its physical root with the
 * lower Gibbs energy, which fixes the tangent plane {@code d_i = ln z_i + ln phi_i(z)}. A trial phase of mole numbers
 * {@code W} is measured by the modified tangent-plane distance {@code tm(W) = 1 + sum_i W_i (ln W_i + ln phi_i(W) - d_i - 1)},
 * whose minimum over the amount scale is {@code 1 - exp(-D(w))}, so {@code tm(W) < 0} at any {@code W} proves the feed
 * unstable; every trial phase is evaluated on its own lower-Gibbs root. Trials run in a fixed order: Wilson vapour-like
 * {@code W = z K}, Wilson liquid-like {@code W = z / K}, and, when at most {@link Settings#pureComponentTrialLimit()}
 * components are present, each pure present component in basis order. A trial iterates {@code ln W_i = d_i - ln phi_i(W)}
 * by successive substitution with a fixed-cycle dominant-eigenvalue extrapolation, then Newton on
 * {@code alpha_i = 2 sqrt(W_i)} with the Hessian {@code delta_ij (1 + g_i / 2) + sqrt(w_i w_j) d ln phi_i/d n_j}, a
 * decrease test on {@code tm} and step halving; it ends converged, at the trivial solution {@code W = z}, or out of
 * iterations. Components absent from the feed stay absent from every trial; a feed with one present component is
 * decided by comparing its two roots' fugacities directly. Deterministic: a fixed trial order and one caller-owned
 * {@link Workspace} whose only carried state is the kernel's prepared temperature, recomputed bit-identically.
 *
 * <p>Per-branch pressures ({@link #test(double, double, double, double[], Workspace)}): the fluid network's separate
 * free-water rule evaluates a hydrocarbon liquid at the state pressure {@code P} and a hydrocarbon vapour at its partial
 * pressure {@code pc = P - p_w}. The test then compares fugacities: every liquid-branch evaluation ({@code LIQUID} root)
 * is made at the liquid pressure, every vapour-branch evaluation ({@code VAPOR} root) at the vapour pressure, and the
 * vapour branch carries {@code ln(pc/P)} (plus {@code (pc - P) c_i/(R T)} for a translated model) on its {@code ln phi_i}.
 * A single physical root at the vapour pressure counts as the vapour branch only when it is vapour-like by the phase
 * identification parameter ({@code PhaseIdentification}): a dense fluid at the lower pressure is the same liquid at the
 * wrong pressure, and admitting it would make every liquid look unstable against itself. With equal pressures the call
 * is the one-pressure test, bit for bit.</p>
 */
X
);
