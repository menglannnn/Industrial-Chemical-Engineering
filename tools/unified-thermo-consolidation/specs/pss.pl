# Condensation spec for science/fluid/network/PassiveStepSolver.java at 3a1e2f9e (line numbers of that state).
# Each entry: [start line, line count, a phrase the block must contain, replacement text].
(
[22, 12, 'solid moment unknown stops bounding', <<'X'],
    /**
     * How far below its own difference floor a solid moment unknown stops bounding the Newton step length (the floors are
     * {@code max(1e-6, encoded value)}). Swept at 0, 1e-6, 1e-3 and 1 on the ten- and thirty-reservoir water chains: every
     * nonzero value gives the same accepted and rejected counts, 0 fails the thirty-reservoir chain; the smallest kept.
     */
X
[35, 34, 'fluid amount unknown stops bounding', <<'X'],
    /**
     * How far below its own difference floor a fluid amount unknown stops bounding the Newton step length, and what a
     * trial keeps of it instead: {@link Equations#project} holds such a trace at {@value #TRACE_RETENTION} of its value
     * rather than let the whole step shrink to the trace's size. The case: a connection the step reverses predicts that
     * the far vessel loses the near side's composition, drives a {@code 1e-12} entry trace negative by orders of magnitude,
     * and the {@code .99*x/-d} rule of {@link Equations#maximumStep} cut every step of the island to 2e-10 and then 2e-12
     * until the line search stalled (73 of the 76 Newton failures of a three-tank pumped fill). Safe because such a trace
     * moves every row it enters by less than 1e-10, and inventories are rebuilt from the converged flows by
     * {@link ConservativeTransport#reconstruct}.
     */
X
[70, 9, 'saturated filter inlet aims', <<'X'],
    /**
     * How far below its stated capacity the saturated filter inlet aims: the committed cake is reconstructed by another
     * summation than the loading row solves, so a law aimed exactly at capacity lands one ULP over it on either side;
     * aiming a hair under makes the landing one-sided, and {@link SolidEventIntegrator} treats a cake this close as full.
     */
X
[80, 7, 'chord step may leave', <<'X'],
    /**
     * The share of a row's gap a chord step may leave before it is not worth taking ({@link Equations#linearisationHolds}):
     * a reused Jacobian stating slope s where the row's own is t leaves |1 - t/s| of the gap; past one half either way the
     * chord step does no better than the halving a fresh step makes.
     */
X
[91, 16, 'holdup of a junction', <<'X'],
    /**
     * The holdup of a junction, in seconds of its largest connection's velocity-cap flow: its buffer holds
     * {@code m_J = HOLDUP_TAU * max over its connections of rho_seed * minimumArea * velocityLimit(seed)}, minted from its
     * seed when first solved ({@link PassiveNetwork#sizeJunctionHoldups}). Without a buffer a junction mixes by a ratio
     * closure that is singular at zero through-flow; with one its mixing and enthalpy rows are the backward-Euler vessel
     * rows {@code (m_J/dt + Q) w = (m_J/dt) w_old + sum q w_e}, regular at Q = 0. The buffer is the row's state, not
     * inventory (owner decision D46): nothing is booked at a mint, and the ledger reports the buffers' change as its own
     * term ({@link #checkConservation}). A numerical regularisation sized to the flow: its mixing lag at full flow is tau.
     */
X
[127, 7, 'Which connections of {@code graph} this solve holds', <<'X'],
    /**
     * Which connections of {@code graph} this solve holds in {@link FlowControl.Mode#INLET_WRONG_PHASE}: the slice's
     * refusals when an interval solver set them ({@link #refuseInlets}, decided once per slice on the committed state so
     * every step and replay of the slice agrees); otherwise {@link InletPhase#decide} on this solve's input states.
     */
X
[143, 10, 'target mass flow on the start state', <<'X'],
    /**
     * Whether a mover's target mass flow on the start state exceeds the velocity cap of a link on its suction walk
     * ({@link InletPhase#walk}, each link's cap on the stream its donor end draws, {@link #massFlowLimit}), or a link of
     * the walk may not carry flow towards the device at all. Such a target has no solution, so the device starts on its
     * head limit, where the flow is free. A port's capacities bound only the phases before its last, never its total, so
     * they are no limit here; a device drawing straight from its first node walks no link.
     */
X
[172, 15, 'passive connections a converged step holds closed', <<'X'],
    /**
     * The passive connections a converged step holds closed ({@code boundaryClosed}) whose run (the chain of closed passive
     * connections through degree-two junctions no actuator touches, as {@link #closeDeadHeads} decides it) is driven by the
     * step's own end states in a direction every link allows, by more than {@link #reopenBand}; a forward run stands in
     * the first end's fluid, a reverse run in the second's, and a junction end counts only while one of its connections
     * is open. Closures are decided on the solve's start point and never reopened within a solve, so a step that should
     * open part-way (an injection into tanks at rest) would be solved shut throughout: the interval solver refuses such a
     * step and solves it again with the run exempt.
     */
X
[259, 12, 'device active set the last pass', <<'X'],
    /**
     * The device active set the last pass of the last solve on this graph settled on. A pump on its head limit or at its
     * shutoff corner is a property of the island's state, not of one step: restarting every solve on the target branch
     * re-walks the power-law curve down from a flow the limit has refused (9.96 kg/s to 0.07 near shutoff) until it no
     * longer fits the Newton budget at any step size. Valid only while {@link #previousPipes} and {@link #previousNodeIds}
     * still describe this graph.
     */
X
[291, 20, 'state a fresh solver would be in', <<'X'],
    /**
     * Puts this solver, at the start of an island job, in the state a fresh solver would be in given only the island's
     * committed interval {@code committed} (null without one on this structure), so a job's solves depend on the committed
     * interval and the slice, not on earlier jobs ({@link PassiveIntervalSolver#replayStart}): every retained numeric
     * Jacobian and LU is dropped (the sparsity pattern, colouring and ordering stay, being functions of the structure);
     * the first step starts from the committed endpoint's flows, heads and modes ({@link #startPoint},
     * {@link #previousModes}), or from {@code initialMassFlows} without a committed interval; "this structure has been
     * accepted" is true exactly when the island has a committed interval on it; the reopen exemption and the retained
     * last solve are dropped.
     */
X
[360, 9, 'One rate solve of {@code graph} with its vessels held as ports', <<'X'],
    /**
     * One rate solve of {@code graph} with its vessels held as ports ({@link PassiveNetwork.NodeKind#PORT}, dt = 1), or
     * null when it has no connection or does not converge: the interval solver's guard and the solid closure check
     * ({@link SolidEventIntegrator}) read the island through it. It only pre-empts a closure the steps would find, so a
     * failed rate solve declares nothing and the steps decide (hold class E1).
     */
X
[398, 6, "A pump's start mode reads its own lines only", <<'X'],
        // A pump's start mode reads its own lines only (owner decision D40 A): its connection's velocity cap at the suction
        // end, its suction walk's caps and one-way rules (suctionCapped), and the sealed pockets on either side of it
        // (relieveSealedPockets). A connection blocked elsewhere in the island changes none of them.
X
[432, 5, 'closed to all transport before the solve starts', <<'X'],
        // A connection closed to all transport before the solve starts is in the active-set state the pass-by-pass closure
        // produces, actuator included: an actuator left on its setpoint across a closed edge is a second equation for a
        // flow the closure has decided, and on a junction whose other edges are closed the factorization is singular.
X
[458, 7, 'species this island can carry', <<'X'],
        // The species this island can carry, and the trial states built from them, are read off the connections this pass
        // leaves open, so they are stated after the closure above: a connection the closure has taken delivers nothing,
        // and an entry trace seeded behind it for a component only it could bring puts an unknown on a nonnegativity
        // boundary whose only root is zero, which refused the whole Newton step ({@link #closeDeadHeads}).
X
[477, 8, 'Decision D34 with A85', <<'X'],
        // Decision D34 with A85: the phases (phaseCode bits) the trace rule removed at each node in this solve, those a
        // node's own split refused at a converged point (the D26 amendment) and those the pass after a proposal refuted
        // (owner decision D39 B, {@link Proposal}). Monotone within the solve: the phase correction does not give such a
        // phase back, else a condensate just below the trace share is folded, the gas left behind is cooler, its flash
        // condenses more than the share again, and the pass sequence repeats (1.0e-9 folded, 2.4e-9 given back).
X
[514, 5, 'structure key is', <<'X'],
            // The active-set state is what varies between passes, so the structure key is also the cycle key: repeating one
            // means the pass sequence cannot make progress. The frozen junction donors and the phase-port segments belong to
            // it for the same reason the modes do: they decide which equations this pass states (see {@link PassKey}).
X
[529, 16, 'Small headspaces amplify', <<'X'],
            // Small headspaces amplify inventory roundoff into pressure and flow errors, so such nodes are solved more
            // tightly. Only a vessel whose wall carries it has a headspace (decision D54: a slack vessel is at the ambient,
            // a dry one holds no fluid); a generator, a void and a junction own no roundoff and no row, so none of them may
            // tighten the tolerance every other node is held to (a filter's two junctions seeded liquid-full demanded 1e-10
            // of a tank that floors its water saturation row near 1.03e-10, and the island refined its step to nothing).
            // A port keeps its say: a finite reservoir held fixed for one rate evaluation.
X
[563, 11, 'Decision A274: a pass that states the wrong cap flows', <<'X'],
                // Decision A274: a pass that states the wrong cap flows can have no root inside the property domain, and the
                // failed Newton's last iterate shows which: a capped line whose drive there has fallen below its cap's
                // pressure drop is released, otherwise a line on its law that the iterate holds at its cap (the trial
                // projection, A275) is put on its cap. A line cannot carry more than its cap, and a stated flow its ends
                // cannot push is no state. The pass is solved again from its start, before the phase corrections below.
X
[580, 15, 'A point the Newton never reached', <<'X'],
                // A point the Newton never reached is not read as evidence about this pass's frozen junction donors or the
                // junction basis they state (unlike the converged tests below): a diverged trial's flow sign handed a
                // junction the tank's nitrogen as a seeded trace nothing delivers, and every swept pressure lost an interval
                // by it (a tank settled at 399999.99971 Pa went to 399998.24 Pa). Owner decisions D28 A' and D39: the same
                // holds for its phases; the iterate's check folds what the iterate empties and proposes nothing.
X
[596, 8, 'Owner decision D39 B: the iterate is read for one thing more', <<'X'],
                // Owner decision D39 B: the iterate is read for one thing more, the solve's own proposal. This pass is the
                // network's closure of the layout the last converged point's check proposed, and a proposed phase it drives
                // to nothing (vanishing from the proposal's seed, FluidThermodynamics.vanishing) is refuted: the pass is
                // solved again from the point it was proposed at, the node laid out without it, and the refusal recorded
                // (a tank on its bubble point proposed 3.4e-6 of its moles as vapour and stalled at every step size).
X
[618, 6, 'converged Newton point is not phase-checked here', <<'X'],
            // The converged Newton point is not phase-checked here: the reconstructed point below, which an accepted step
            // commits, is, and the two agree to the Newton tolerance (checking both cost 43 % of a flowing island's wall
            // time). The reactivation asks the same stability question per component of the frozen trace support.
X
[647, 6, 'one active-set change per pass, a cap change first', <<'X'],
            // Decision A272: one active-set change per pass, a cap change first, then a device's mode, then a boundary
            // closure. A point where a line carries more than its cap is no point a device may read its mode from, a
            // boundary closure is never taken back within a solve, and a forbidden direction can be the symptom of a wrong
            // cap as of a wrong device mode, so the reversible corrections come first.
X
[659, 14, 'showing a direction its own boundary forbids, next to a device', <<'X'],
            // A passive connection showing a direction its own boundary forbids, next to a device that has just decided it
            // cannot run in its current mode, is that device's symptom: a pump held above its shutoff head pushes its whole
            // suction line backwards. {@code boundaryClosed} is never cleared within a solve, so taking the symptom first
            // pins the wrong active set for good (the closed suction edge forces the pump's flow to zero, the reverse-flow
            // test never fires again, and the pump is left at its head limit where the Newton cannot converge). The device
            // is decided first; the boundary closure waits for a pass where every device holds a mode it can.
X
[674, 10, 'a cap change re-solves the same pass from its own start', <<'X'],
            // Decision A272: a cap change re-solves the same pass from its own start (its seeds, start flows and phase-port
            // segments): it changes which equation states one flow, not the point the pass was laid out on (re-seeded from
            // the converged junction a line's column came out 365 kg/m3 lighter and the cap left and entered until the
            // sequence cycled). A confirmed emptying (keptEmptying) was decided on the network this active set states, so
            // a cap, mode or closure change decides it again.
X
[727, 5, 'booked states show a drive the converged point did not state', <<'X'],
                    // The booked states show a drive the converged point did not state: a one-way end that would receive
                    // (a tank full of water 0.01 Pa above its generator, pushed back into it by the polish). The boundary
                    // is closed as at a converged point, and the pass is solved again from the reconstruction.
X
[769, 7, "Closes connection {\@code edge}'s boundary", <<'X'],
    /**
     * Closes connection {@code edge}'s boundary for the rest of the solve: a direction its one-way end forbids (a void
     * supplying, a generator receiving, a dry end giving). Decision A272: a closure changes what reaches a junction at its
     * ends, so the caps decided there are decided again on the next pass's start point.
     */
X
[789, 26, 'Decision D13', <<'X'],
    /**
     * Decision D13: one re-solve of a point the reconstruction's equation gate refused, from its reconstruction ({@code x}:
     * the converged point with every node at its reconstructed state, the flows the Newton's), on a fresh Jacobian of this
     * step's own dt at 1e-2 times the pass's Newton tolerance; checked exactly as the pass loop checks a converged point
     * (no trace reactivation due, no device mode change ({@link #nextMode}), no illegal direction, the segments its flows
     * land in the ones the pass froze, the velocity constraint, the reconstruction with its own draws, the phase
     * correction) and gated again. Returns it only if all of that holds; a point that flows the way a boundary forbids
     * returns that boundary ({@link Polished#closing}); otherwise null and the step is refused as before.
     *
     * <p>Why: the Newton's last iterates run on a chord Jacobian inherited from another step, so a balance row closes only
     * to the tolerance, stated over the node's total amount; a vessel whose gas is a small share of its amounts (a nitrogen
     * cushion over water, 0.1 to 0.2 %) sees that residual magnified by n_total/n_gas (about 500) in its cushion's pressure.
     * A fresh Jacobian closes such a row in one iteration (1.6e-14 to 1.5e-12). It starts from the reconstruction because
     * the converged point itself may already meet the tolerance (9.2e-12) and a re-solve from there sees nothing. Only a
     * refused point reaches this method, and the fresh workspace is never retained, so every accepted step is bitwise what
     * it was.
     */
X
[879, 5, 'cavitation, the mirror of the head limit', <<'X'],
            // Owner decision D59 (4), cavitation, the mirror of the head limit: a pump whose target or head limit holds its
            // suction below the vapour pressure of the water it draws goes on its suction limit, where its flow is what the
            // suction line delivers; it leaves when that exceeds the target, needs more head than the pump has, or is nothing.
X
[898, 7, 'how far the suction end of connection', <<'X'],
    /**
     * Owner decision D59 (4): how far the suction end of connection {@code edge} stands above the vapour pressure of the
     * water its pump draws, read on {@code states} (Pa); positive infinity where the connection is not a liquid pump or
     * the pass laid its suction out with no free water. The temperature is the state's, or the pass's seed's where the
     * state stands outside the water data.
     */
X
[913, 7, 'on a failed Newton: the first pump', <<'X'],
    /**
     * Owner decision D59 (4) on a failed Newton: the first pump at its target or head limit whose suction the last iterate
     * holds below the vapour pressure ({@link #cavitationMargin}, by more than {@link #shutoffBand}) is put on its suction
     * limit, its cap decided afresh; true if one was. Like the cap corrections of a failed iterate (A274) it is part of the
     * cycle key, so the sequence stays finite.
     */
X
[954, 11, 'cycle key of one active-set pass', <<'X'],
    /**
     * The cycle key of one active-set pass: its structure and the phase-port segments it froze ({@link PhaseDraw.Segment},
     * which change a port stream's values but no structural entry, so they are not part of the workspace key), plus the
     * phases the trace rule has removed and the own splits and refuted proposals have refused so far in the solve (D34,
     * A85, D26 amendment, D39 B): that record only grows, so a pass that returns to an earlier structure after a fold or
     * a refusal is a pass under a different phase-correction rule, not a cycle.
     */
X
[969, 7, 'A proposal (owner decision D39 B)', <<'X'],
    /**
     * A proposal (owner decision D39 B): the phases ({@link FluidThermodynamics#phaseBits} bits) the check of a converged
     * pass's point added to one node ({@link #phaseCorrection}), the node's seed holding them and the point they were
     * added at. The passes after it are the network's own closure of that layout; one that fails with a proposed phase
     * vanishing from the seed refutes it.
     */
X
[988, 6, 'frozen per-component phase support', <<'X'],
    /** The cap states of a pass as codes for its cycle key ({@link PassKey}). */
X
[994, 1, 'capCodes', <<'X'],
    private static byte[] capCodes(int[] caps){byte[] codes=new byte[caps.length];for(int i=0;i<caps.length;i++)codes[i]=(byte)caps[i];return codes;}
    /** Every node's frozen per-component phase support for one pass, from that pass's seeds and never from a Newton iterate,
     * with the components this solve has already reactivated kept in both phases. A fixed node owns no unknowns. */
X
[1003, 8, 'Components whose omitted phase the converged point no longer supports', <<'X'],
    /**
     * Components whose omitted phase the converged point no longer supports omitting, marked for the next pass; returns
     * how many. The test runs on the Newton solution of the reduced system, where both phases' fugacity coefficients are
     * converged and {@code ln phi_i} of the omitted phase is filled at infinite dilution. Every node is swept before any
     * pass is repeated, so one reactivation pass restores everything the state asks for.
     */
X
[1096, 31, 'A trial point for one pass, per node', <<'X'],
    /**
     * A trial point for one pass, per node. A vessel is seeded with its own state, its committed layout (owner decision
     * D40 A): a reachable component it lacks ({@link #reachableComponents}) enters as an entry trace of {@code 1e-12} of
     * its amount, restated in the phases it holds ({@link ConservativeTransport#repartition}) and placed by the layout's
     * own split; a phase what is about to arrive carries and the layout lacks enters with what arrives of it
     * ({@link #arrivingInVessel}). The seed was the TP flash of the inventory with up to a quarter of its mass of what
     * arrives: for a pool of pure methane at saturation fed CO2 that put 99 % of the moles in the vapour, 183 m3 in a 1 m3
     * tank. A junction is seeded with its own state, its owned holdup a small stock the backward-Euler rows move.
     *
     * <p>Water trace in a junction (decision D10): a junction whose reachable set holds water while its state holds none
     * is seeded with its state flashed at its own temperature and pressure with a water entry trace of {@code 1e-12} of
     * its amount. A node's {@link PhaseLayout} carries water only when its seed holds it, and water has no entry in the
     * reachable mask, so without the trace a junction seeded dry that receives unsaturated water vapour (no phase code
     * change, so the phase correction never reseeds it) is solved without the water the reconstruction then books into
     * it, and the equation gate refuses every step. The seed is a basis, never a reason to refuse a step; a junction
     * whose flash leaves the domain keeps its state. A junction's first pass admits the phase that arrives
     * ({@link #arrivingPhases}).
     */
X
[1147, 14, "Owner decision D40 A at a vessel's step start", <<'X'],
    /**
     * Owner decision D40 A at a vessel's step start: its committed layout {@code seed}, and a phase that what is about to
     * arrive carries and the layout lacks laid out with what arrives of it, in its own phase: per inflow the stream its
     * donor end draws ({@link #endStream}), at most a quarter of the vessel's mass over {@code dt}, at the vessel's
     * temperature and pressure. The phases the vessel holds keep their committed amounts, except the steam its gas takes
     * beside arriving free water: the free water's own vapour pressure ({@code FluidThermodynamics.freeWaterVaporPressure},
     * Dalton), since steam left at its entry trace is ten orders of magnitude under it and no Newton climbs that (water
     * let into a tank of nitrogen at 298 K stopped its first step at the iteration limit, |r| 2.5e-5). The seed itself
     * where nothing arrives in a phase it lacks, or where its state refuses that phase (LNG let into a warm gas boils).
     */
X
[1191, 29, 'Group L (A300-A303)', <<'X'],
    /**
     * Group L (A300-A303): {@code seeds} with every junction laid out for the phases the step's inflow leaves in it. A
     * fitting full of air that water reaches is full of water a few milliseconds later, and a pass is laid out for its
     * seeds' phases ({@link PhaseLayout}), so a junction seeded as its held gas has no unknown for the water its mixing
     * rows must hold: the residual sits in those rows, the trial temperatures leave the data and the step halves to its
     * floor (an 11 g air buffer receiving about 7 kg/s of water). Each junction's mixture over the step is its stock plus
     * what its connections deliver at the pass's start flows ({@link #startPoint}) over {@code dt}, each the stream its
     * donor end draws ({@link #endMoles}), at the mass-weighted temperature and the seed's pressure, flashed and folded as
     * the pass folds a junction (D34, D41). Where that mixture holds a phase the seed lacks, the seed becomes the mixture
     * at the seed's mass; a junction donor delivers its own updated seed, so a phase reaches a run of junctions in one
     * stage. A seed that already holds every phase of its mixture is kept to the bit; a rate-only solve holds a
     * junction's composition at its stock and this does nothing there.
     *
     * <p>A vessel that holds no fluid (decision D54) is laid out from what its connections deliver over {@code dt}, beside
     * its solids: the delivered amounts at the bag's own split (decision D26, {@link #emptyBagSplit}), at the ambient while
     * the bag is slack and at the capacity once what arrives overfills it, folded as a vessel; where the engine answers
     * nothing the network carries, the TP flash of the delivered amounts at their mass-weighted temperature and the
     * ambient (an isothermal let-down: LNG at 139 K and 1 atm is a vapour of several bag volumes beside CO2 frost, and a
     * pass laid out without the liquid cooled the vapour to its 90.69 K floor). One nothing reaches stays dry.
     */
X
[1267, 10, "Decisions D54 and D26: an empty bag's own split", <<'X'],
    /**
     * Decisions D54 and D26: an empty bag's own split of what arrives over a step, the amounts {@code n} with the enthalpy
     * {@code h} they bring (J, the network's datum). The engine's PH answer at the ambient pressure holds the phases; the
     * vessel's closure from it ({@link InventoryEquilibrium#split}, the wall's work booked from a zero volume) is the
     * equilibrium of that enthalpy at the ambient while the bag is slack and of its energy at the capacity once what
     * arrives overfills it; the PH answer itself where the closure leaves nothing. {@code null} where the engine answers
     * nothing the network carries.
     */
X
[1300, 8, 'balanced pressure seed of a cold junction', <<'X'],
    /**
     * The balanced pressure seed of a cold junction: every junction of degree three or more joined by open passive
     * connections to nodes that hold their pressure over the solve, whose guess lies outside its neighbours' pressure
     * range, starts from the pressure at which its connections' estimated flows balance, and from the mixture those flows
     * deliver there. A guess outside the range makes every flow of the star point the same way and the Newton descend
     * from a state no solution is near. A starting guess only: the junction's owned inventory is not touched.
     */
X
[1316, 5, 'vessel neighbour counts as held while this structure is cold', <<'X'],
                // A vessel neighbour counts as held while this structure is cold: a step is the only solve a cold junction
                // sees, and a vessel's pressure moves by at most the state cap over it. Only until a step of the structure
                // has been accepted: at a hydrostatic rest the seed would fire at every step (480 seeds, 119040 flashes).
X
[1389, 15, 'Owner decisions D54 (nothing at the mint) and D55', <<'X'],
    /**
     * Owner decisions D54 (nothing at the mint) and D55 (the guess from the connected suppliers): the start guess of a
     * minted island's junctions, the last round an island-wide fixed point reached. A junction holds nothing a placement
     * put there, so the guess only has to be a point the first step's Newton starts near, of a fluid a source supplies.
     * Round 0 is {@link #supplierSeeds}; each round after it is one rate solve of the port graph with every junction's
     * composition held at the current iterate, on a fresh solver; each junction then becomes the flow-weighted mixture
     * of the streams that solve drives into it, flashed at its solved pressure (one nothing flows into keeps its
     * composition). It stops when no mass fraction moves by more than 1e-9 and no pressure by more than 1e-9 of itself,
     * after {@link #FIXED_POINT_ROUNDS} rounds, or at a round whose rate solve fails, returning the last completed round.
     * Per-kilogram states, one per node, null for every node that is not a junction.
     */
X
[1469, 24, 'Owner decision D55, round 0', <<'X'],
    /**
     * Owner decision D55, round 0 of {@link #fixedPointStarts}: a junction's start guess is what its connected suppliers
     * would send it. A supplier is a node that is not a junction and holds something; a void and a dry end send nothing
     * ({@link #boundaryAllowed}). Each supplier joined to a junction sends the stream its end draws ({@link #endMoles}) at
     * the flow its static drive gives against the ambient ({@link #supplyFlow}); the junction is the flow-weighted mixture
     * of what reaches it, flashed at the pressure where the estimate's flows balance ({@link #balancePressure}). A junction
     * no supplier drives into takes it hop by hop from the junctions that have it (a breadth-first sweep weighted by the
     * flow that seeded them); one neither reaches takes the streams of the suppliers joined to it, one share each, and
     * the sweep runs again. A junction no source reaches keeps the state it was built with as a placeholder
     * ({@link NetworkReduction#atRest}).
     *
     * <p>A mover's suction line is seeded first, from the stream it draws (owner's fix A of F2): the junctions its supply
     * walk crosses ({@link InletPhase#walk}) take the stream of the supplier the walk ends at, as that end draws it
     * ({@link #endStream}), neither flashed nor moved to a balance pressure. A mover's target row converts its volume flow
     * at its suction junction's density, so that junction must hold what the mover draws (a pump under a vented LNG tank
     * kept the 298 K nitrogen placeholder, 1.1 kg/m3 for 447 kg/m3, and moved 0.006 kg/s for 2.24 kg/s).
     */
X
[1564, 9, 'the pressure of junction {@code j} at which the start estimate', <<'X'],
    /**
     * Owner decision D55: the pressure of junction {@code j} at which the start estimate's flows into and out of it
     * ({@link #initialMassFlow}, as far as {@link #boundaryAllowed} lets it) balance, bisected between the lowest and
     * highest of its neighbours' end pressures referred to its height, within the numerical box. A junction's pressure is
     * what its connections' flows make it, never the ambient's (seeded at the ambient a liquid junction stood 9.8 kPa above
     * a 1 m spill and 240 kPa below its two generators, and its first Newton did not converge).
     */
X
[1609, 5, "flash of junction {\@code j}'s mixture", <<'X'],
    /** {@link #supplierSeeds}' flash of junction {@code j}'s mixture (amounts and enthalpy summed over {@code mass}) at
     * {@code pressure}, per kilogram: the temperature at which the mixture has the mixed enthalpy, bisected within 20 K of
     * its streams' temperatures inside the numerical box (no data range narrows the window, owner rule of 2026-10-16).
     * False, the junction left as it was, where a flash is refused. */
X
[1626, 7, 'supplier {@code supplier} sends into the junction', <<'X'],
    /**
     * The flow (kg/s, at least zero) supplier {@code supplier} sends into the junction at the other end of {@code pipe}
     * before anything is solved, as {@link #initialMassFlow} reads a connection: a mover's suction flow out of its suction
     * end; otherwise the supplier's end pressure over the ambient, less its own end's column up to the junction, through
     * the connection's loss in its end stream, capped by the velocity limit. Zero where that direction is refused.
     */
X
[1652, 24, "whole island's connection flows as the states this solve starts at imply", <<'X'],
    /**
     * The whole island's connection flows as the states this solve starts at imply them, with every chain of zero-holdup
     * junctions carrying one flow. {@link #initialMassFlow} answers one connection against the two pressures at its ends,
     * the right question on a vessel; across a zero-holdup junction the junction's own pressure is an output of the
     * previous solve carrying its roundoff, and on a settled line the per-connection answer is noise (at 150 kPa it said
     * the tank pushed -7.2e-7 kg/s back into the filter's outlet junction while the filter pushed +1.6e-5 kg/s into it).
     * Everything a pass states about a junction is read off this point, so a maximal run of degree-two junctions joined
     * by passive connections is contracted to one resistance between the two nodes at its ends, which own their
     * pressures, and the single flow that resistance passes is laid on every connection in it. An actuator is never
     * contracted through (its own setpoint decides what it passes), nor a junction of degree three or more (its split
     * needs the solve).
     */
X
[1753, 11, 'connection flows and actuator heads a pass with this active set starts from', <<'X'],
    /**
     * The connection flows and actuator heads a pass with this active set starts from, the whole of
     * {@link Equations#buildInitial}'s edge half; it lives here because {@link #closeIllegalStarts} reads it before the
     * equations exist. {@code seeds} may be {@code null} before this solve has any, which only a regulating valve reads.
     */
X
[1783, 8, 'whose pump is holding its limit starts from a flow that limit can', <<'X'],
            // A connection whose pump is holding its limit starts from a flow that limit can produce, never from one it has
            // refused ({@link #headLimitMassFlow}); a point already on the limit carries its own flow forward. A pump the
            // carry has just offered its head limit back from CLOSED starts from the limit's own flow and head, not from the
            // closed point's (from that head a long step's pass ended every 5 s step past the shutoff, 12.3 kPa short).
X
[1866, 8, 'static driving pressure of one passive connection from end', <<'X'],
    /**
     * The static driving pressure of one passive connection from end {@code a} to end {@code b} (a contracted run sums its
     * links' columns the same way, {@link #initialMassFlows}), given their end pressures' {@code difference}, with the
     * column {@link #columnFromFirst} states and each end's {@link #columnDensity}. Its start sign, where both columns are
     * admissible, is the sign of {@code difference}. An end that may not donate has no column of its own ({@link #donates}).
     */
X
[1880, 10, 'whether end {@code node} of {@code pipe} may fill the connection', <<'X'],
    /**
     * Owner decision D59 (5): whether end {@code node} of {@code pipe} may fill the connection, that is, whether the
     * connection may carry flow out of that end ({@link #boundaryAllowed}). A void only receives, a generator's far end
     * never sends into it, a blocked way and a dry end (decision D54) send nothing: such an end's fluid never stands in
     * the connection, so the connection stands in what the other end sends. The one statement of that fact for every
     * static column (the row's {@link Equations#columnEnds}, {@link #staticDrive} and a run's in {@link #initialMassFlows});
     * before, a void line whose junction stood below the ambient was given the void's air column and closed for good.
     */
X
[1893, 15, 'one column rule of a vertical connection', <<'X'],
    /**
     * The one column rule of a vertical connection: whether its static head is stated in the first end's column
     * ({@code first}, the density of what that end's port stands in the connection) rather than the second's, given the
     * end pressures' {@code difference} (actuator included) and {@code dz = z_second - z_first}. The flow row states it
     * once per pass ({@link Equations#columnEnds}) and the start estimate on the same question ({@link #staticDrive}).
     * <ul>
     * <li>Both columns drive one way: the column of the end that drive leaves (the only admissible one).</li>
     * <li>The two drives point towards each other (neither column admissible, where a settled line lives): the column
     * nearest its own rest.</li>
     * <li>Both admissible (the pipe can stand full of either fluid): the start's direction {@code forwardStart}, the
     * history of which fluid is in the pipe.</li>
     * </ul>
     */
X
[1915, 15, 'flow a pump holding its maximum added pressure can actually push', <<'X'],
    /**
     * The flow a pump holding its maximum added pressure can push through its own connection, from the start-of-step
     * endpoint states: the bisection {@link #initialMassFlow} runs for a passive connection, with the pump's limit added
     * to the driving pressure. A pump reaches {@code PUMP_HEAD_LIMIT} because its target flow was refused, and that target
     * is the warm start; near shutoff the two are orders apart (9.96 kg/s against 0.07) and Newton walks the power law
     * down by halving per iteration, which runs out of iterations at every step size. Seeding from the limit starts the
     * walk where it would have ended.
     */
X
[1939, 17, "A mover's pressure-rise limit", <<'X'],
    /**
     * A mover's pressure-rise limit ({@link FlowControl.Mover#riseLimit}); a compressor's is {@code (r_max - 1) P_s} on its
     * suction pressure. A pump's is the setting, the rise for water at 298.15 K and 1 atm
     * ({@link FluidThermodynamics#pumpReferenceDensity()}), scaled by the suction's density over water's: a head, the one
     * quantity a real pump fixes. On nitrogen at 1 atm the limit is about 1/870 of the setting, so a pump moving gas
     * between closed vessels reaches its limit and its shutoff within a second instead of evacuating the suction vessel.
     * The density is the trial's suction stream density ({@link #endDensity}: the bulk at a BULK end, the drawn phase's at
     * a phase port), the same density the target row and the velocity clamp read, so the Jacobian carries the limit's
     * dependence on that node and every mode test reads the same density at the converged point.
     */
X
[1966, 13, 'driving pressure of one end of a connection', <<'X'],
    /**
     * The driving pressure of one end of a connection: the node's pressure plus the static offset of the port at that
     * end ({@link #portHead}: the level head at a vessel's LIQUID port, zero elsewhere). Every static driving pressure the
     * solver states (the hydraulic row, the pass's column choice, the start-of-solve closures, the start flows, the valve
     * start head, the pump's head-limit start, its carry offer and shutoff margin ({@link #demand}) and the reopen test)
     * reads an end's pressure through here and nowhere else, because they must agree: a closure decided on one pressure
     * and reopened on another bottles a line for good. The offset is a pressure, so it applies in both directions.
     */
X
[1987, 6, "height every vessel's level head is stated over", <<'X'],
    /**
     * The height every vessel's level head is stated over (decision D9, option B): one metre, the one-block tank's own
     * height at the default 1 m3 volume; a prismatic tank of this height over the footprint {@code V/H}.
     */
X
[1994, 18, "static head of a port over its node's pressure", <<'X'],
    /**
     * The static head of a port over its node's pressure: the level head of a bottom outlet (decision D9, option B), zero
     * everywhere else. At a LIQUID port of a vessel (RESERVOIR, or its rate-solve copy PORT) the node's pressure is the
     * headspace's, and the bottom stands below the weight of the condensed phases over the footprint {@code A = V/H}:
     * {@code h = g * m_c * H / V}, {@code m_c} the condensed mass of {@code state} ({@link FluidThermodynamics#liquidMass}),
     * {@code H} = {@link #LEVEL_HEAD_HEIGHT}, {@code V} the vessel's inventory volume. Linear in the decoded condensed
     * amounts, evaluated by every caller on the state it is stating a pressure for (so the block Jacobian carries its
     * derivative with no new structural entry); a pressure, so inflow through a bottom port works against it; not in the
     * port's capacities nor the step controller. A VAPOR or BULK port, any other node kind and a state with no condensed
     * mass get exactly {@code 0.0}, so gas-only and all-BULK islands are the doubles they were.
     */
X
[2019, 7, 'density of what leaves a node through {@code port} at a vanishing flow', <<'X'],
    /**
     * The density of what leaves a node through {@code port} at a vanishing flow: the bulk {@code mass/volume} at a BULK
     * end, and at a phase port the stream of the first phase of its priority order the state holds
     * ({@link PhaseDraw#leading}, decision D11); the bulk's when the state holds none of the three. The draw reads an
     * end's density here (a start flow's loss and cap, a mover's suction, rise limit and column); what stands in the
     * connection is {@link #columnDensity}.
     */
X
[2037, 19, 'density of the static column', <<'X'],
    /**
     * Owner decision D34: the density of the static column a node's end {@code port} stands in a connection of its own
     * {@code volume} (m3; a contracted run's summed): the connection filled with the port's phases in priority order by
     * volume ({@link PhaseDraw#fill}), each up to what the state holds of it, the last phase it holds taking the rest.
     * Every static column reads an end's density here (the hydraulic row and the pass's column choice, the start drives,
     * the sign tests and the rest gauge's drives); a mover's column is its suction stream instead (decision D6). A phase
     * that fills the connection alone gives its own density to the bit; a BULK end, and a state holding none of the
     * port's phases, the bulk's; in between the column is continuous in the phase volumes. Read from the leading phase
     * instead, 1.83e-8 m3 of condensate (880 kg/m3) at a bottom port over a 2 m drop of 2 L line stood as a 17.3 kPa
     * column no step could drain; as the line's gas plus the condensate's share it is 44.5 Pa. Read on the trial state,
     * so the Jacobian differentiates it.
     */
X
[2103, 5, 'conserved amounts (water last) of what leaves through', <<'X'],
    /** The conserved amounts (water last) of what leaves through {@code port} at a vanishing flow; the node's total amounts
     * at a BULK end and when the state holds none of the phases. Over {@link #endMass} they are the moles per kilogram of
     * the fluid the connection carries: the solids a liquid or bulk end also draws are left behind, as {@link #endStream}
     * leaves them (LNG drawn with 1.3 % of dry ice at its stored energy flashed 1.3 % of its methane to vapour). */
X
[2148, 13, 'head this connection demands of its device', <<'X'],
    /**
     * The head this connection demands of its device at the point one active-set pass converged to: the discharge
     * pressure less the suction pressure, plus the static column between them. The one scalar the
     * {@code PUMP_HEAD_LIMIT}/{@code CLOSED} pair is decided on, the same expression in both modes: in
     * {@code PUMP_HEAD_LIMIT} the actuator row pins the head to the maximum, so this is {@code maximum - loss(q)} and the
     * margin is the edge's own loss; in {@code CLOSED} the actuator row pins the flow to zero, so the free head unknown is
     * this demand and the margin is how much head the pump has in hand. One quantity in both modes makes the pair a
     * threshold on a line rather than two one-sided tests of two unknowns.
     */
X
[2166, 21, "How much head margin this edge's own rows can actually resolve", <<'X'],
    /**
     * How much head margin this edge's own rows can resolve, and so the band around the shutoff corner inside which a
     * device's mode may not be decided by the sign of a flow. The pressure-dimensioned rows of an edge are stated in
     * {@link Equations#pressureScales}, so a solve converged to {@code tolerance} has decided them to
     * {@code tolerance*scale} Pa (1e-4 Pa on an atmospheric island, 6e-4 Pa at 6 bar), a flow resolution of order
     * 1e-5 kg/s; the rule this replaced closed a pump on a reverse flow of 1e-10 kg/s. The floor of 0.01 Pa is the band
     * the {@code CLOSED} side used, kept so the reopening test is bit for bit what it was at or below 1e5 Pa (every island
     * of the regression reference). Both sides read the same margin, so no interval leaves a pump at its shutoff corner
     * holding its head limit; that corner is degenerate and {@code CLOSED} is its well-posed representative (it pins the
     * flow to exactly zero through the actuator row).
     */
X
[2197, 20, "outer phase check of a converged pass's point", <<'X'],
    /**
     * The outer phase check of a converged pass's point, node by node ({@link FluidThermodynamics#phaseCheck}, owner
     * decisions D25 and D26): a phase leaves a node only when the solve empties it (the trace fold, which the
     * reconstruction has applied, and the vanishing rule of D38 C: under {@link FluidThermodynamics#VANISHING_SHARE} of the
     * node's fluid and at a smaller share than at the step's start, {@link #inputStates}), a phase the node's TP answer
     * holds and the node does not is added at the node's own split (a junction's PH answer, a vessel's closure,
     * {@link #vesselSplit}), and no populated phase set is swapped for another on a TP verdict. Never inside a residual or
     * Jacobian evaluation. One node per call, as one active-set change per pass; null when none changes.
     *
     * <p>{@code prepared}, when given, is the pass's per-node prepared temperature state (a flash at exactly that
     * temperature reuses its workspace). {@code foldedPhases}, when given, holds per node the phases ({@link #phaseCode}
     * bits) the trace rule removed in this solve and those an own split refused (A85, the D26 amendment): the check does
     * not add them back and records there what its fold removes and its own split refuses, so the pass sequence stays
     * finite. A failed pass's last iterate is not checked here: it is only folded ({@link #foldedIterate}) and read for
     * whether it refutes a phase this check added ({@link Proposal}).
     */
X
[2229, 9, "check of a failed pass's last iterate, node by node", <<'X'],
    /**
     * The check of a failed pass's last iterate, node by node (owner decisions D28 A' and D39): the iterate with the
     * phases it empties folded, by the trace fold and the vanishing rule against the step's start ({@link #inputStates}),
     * one node per call; null when it empties none. A failed iterate is not a state: this trial layout neither reads nor
     * writes the A85 record and adds no phase, so no own split is formed and no hold is raised at a point the network
     * never reaches.
     */
X
[2316, 53, 'every passive connection whose own starting point leaves it no', <<'X'],
    /**
     * Closes, before the first pass, every passive connection whose own starting point leaves it no direction it is
     * allowed to carry. The hydraulic row is {@code driving - loss(q)} with {@code loss} zero at zero flow and increasing
     * away from it, so a root with {@code q > 0} exists only when the driving pressure a forward flow stands in is
     * positive, and with {@code q < 0} only when the reverse drive is negative: a connection one of whose ends forbids a
     * direction has no admissible root when the only sign with a root is the forbidden one, and its one answer is a flow
     * of exactly zero, the answer {@code boundaryClosed} produces. On a dead-headed line the pass cannot converge to that
     * point (the reverse root lies across zero flow, where the friction slope changes by three orders between a liquid and
     * a gas), so the closure is taken from the starting point instead.
     *
     * <p>Three conditions keep it a statement that cannot be wrong for the solve: both columns are asked (a forward flow
     * stands in the first end's fluid and a reverse flow in the second's, the pairing {@link Equations#columnEnds} states);
     * neither end of the run is a zero-holdup junction (a junction's pressure is an output of the previous solve, while a
     * boundary's is fixed and a vessel's moves monotonically against the flow that delivers it, so a drive that refuses a
     * direction at the start refuses it everywhere the solve can go; the run is the chain {@link #initialMassFlows}
     * contracts); only passive connections (an actuator decides its own mode, and a boundary direction is never closed on
     * a device's symptom). It cannot cycle: monotone within a solve, taken once before any pass, never cleared, and per
     * solve, so a line whose generator is raised afterwards opens again.
     */
X
[2424, 24, '{@code illegalDirection} closure of the pass loop', <<'X'],
    /**
     * The {@code illegalDirection} closure of the pass loop, taken once at the start of the solve on the point pass 0
     * starts from: a passive connection whose start-point flow runs in a direction its own boundary forbids
     * ({@link #boundaryAllowed}) is closed for this solve exactly as that rule closes it after a converged pass. The
     * direction is the one {@link #startPoint} gives it; where that gives none ({@code |q| <= 1e-10}, every connection the
     * previous point had closed), the direction of {@link #initialMassFlow} on the current states. Derived afresh from
     * this solve's start point, so a connection whose pressures turn legal opens again at the next solve. Passive
     * connections only (devices are decided first, see the pass loop). Why: an island coming to rest next to a void or a
     * generator leaves the one-way connections with flows that point the forbidden way at the next solve's start, and the
     * pass loop would close them only after a converged pass, which at rest is a zero-flow stall. Only once this solver
     * has accepted a solve on the same structure (a first solve's junction pressures are the compiler's guess, not a
     * state), and only where the static driving pressure forbids the direction with either end's density
     * ({@link #illegalWithEitherDensity}). {@code keep} names the connections the interval solver reopened.
     */
X
[2488, 9, 'pockets of a pass (hold F8', <<'X'],
    /**
     * The pockets of a pass (hold F8): for every node, the pocket it belongs to (the index of a member), or -1. A pocket
     * is a set of junctions joined by pressure-coupling connections ({@link #couples}) with no pressure anchor: no coupling
     * connection to a node that is not a junction and no regulating valve drawing from it. A junction's pressure is
     * algebraic, and nothing places a pocket's level: shifting every member's pressure together changes no row but those
     * of the connections on its boundary, all prescribed-flow rows with a free head or none.
     */
X
[2510, 28, 'start modes of a pass must leave every pocket', <<'X'],
    /**
     * Hold F8: the start modes of a pass must leave every pocket ({@link #pockets}) a root. A pocket whose boundary carries
     * only closed connections and movers at their target flow balances its mass against the targets alone, which no
     * unknown of the pocket moves, so with a nonzero target it has no root and its pressure level has no row: the
     * factorization is singular before the first Newton step (a pump discharging into a pressure control valve closed at
     * the start, or into an inline filter with nothing past it), and such a start was refused at every step size. Decided
     * here, at the start of the solve only, in the order the physics does it: a mover pushing into the pocket raises its
     * pressure until a closed pressure control valve drawing from it opens (a valve whose far end has an anchor starts
     * regulating); otherwise a mover with a sealed pocket on either side ({@link #sealedWithout}) can carry nothing and
     * starts closed at its shutoff for this solve ({@code atShutoff}, never reopened within it), the pocket isolated at its
     * pressure ({@code Equations.retainedPressures}); starting it at its head limit instead would place the pocket at the
     * mover's shutoff pressure, outside the package's range for a compressor or a strong pump. Repeated until no pocket is
     * left without a root; an island without such a pocket keeps its start modes exactly.
     */
X
[2591, 7, "for a mover's side (owner decision D58", <<'X'],
    /**
     * {@link #sealedWithout} for a mover's side (owner decision D58, A4): with {@code relief} 1 the pocket must let flow
     * out (a mover's discharge), with -1 it must let flow in (its suction), and a connection whose one-way rules
     * ({@link #boundaryAllowed}) forbid that direction counts as closed. With 0 every open connection counts, either way.
     */
X
[2618, 37, 'Decision D12 (phase-ports batch): a generator never receives', <<'X'],
    /**
     * Decision D12: a generator never receives, from the cold start's first pass on. On a rate solve with no accepted solve
     * of its structure behind it (the cold start's rate seed), a passive generator run (the generator's connection and the
     * degree-two junctions after it, the run {@link #closeDeadHeads} decides on) that ends at a junction of degree three or
     * more is closed for this solve when the island's one-way pressure estimate ({@link #oneWayPressures}) puts that
     * junction above the generator by more than {@link #reopenBand}; if any run is closed, the island's junctions start
     * from the estimate's pressures (a Newton starting guess only).
     *
     * <p>Why: {@link #closeDeadHeads} skips a run that ends at a junction and {@link #closeIllegalStarts} waits for an
     * accepted solve, so pass 0 of the cold rate seed treated every generator as a two-way fixed-pressure boundary:
     * between generators tens of kPa apart, a through-flow network whose low generators are sinks, and every step was
     * refused at pass 0 at every step size. Measured on the extreme-topology fixtures, closing the receiving generators
     * from the compiler's junction guess does not converge and a uniform junction pressure converges only near the
     * solution; the estimate supplies both the closures and the start. A closure binds this rate solve only; the step
     * solves decide afresh on the seeded states. An island in which no generator would receive is solved exactly as
     * before. Islands with an actuator or solids are left to the pass loop. An inline filter is a passive connection here
     * (hold class E1): the estimate reads its connection by its block's geometry.
     */
X
[2700, 13, 'one-way pressure estimate of', <<'X'],
    /**
     * The one-way pressure estimate of {@link #closeColdReceivingGenerators}: every junction's pressure at which the
     * island's open connections balance with every held node at its start end pressure, each connection carrying the flow
     * of {@link #initialMassFlow}'s law in the column of its donor end (the run's loss inverted, capped at the donor's
     * velocity limit) and nothing in a direction {@link #boundaryAllowed} refuses. A junction donor holding gas only has
     * its density, and so its cap, scaled with its pressure (ideal gas at its own temperature): a cap read at the guessed
     * density put a grid's junctions 10 kPa too high, and it is the one pressure dependence a capped junction outflow has.
     * A damped Newton on the junction pressures alone, held within the held pressures' hull widened by the island's
     * largest static column. Returns per node the estimated pressure of a junction some held node reaches, NaN elsewhere,
     * or null if the Newton does not converge (nothing is then closed).
     */
X
[2888, 11, 'has the sign of {@code flow} with rho = either', <<'X'],
    /**
     * Whether the static driving pressure {@code P_a - P_b - rho g (z_b - z_a)} has the sign of {@code flow} with rho =
     * either endpoint's density. {@link #initialMassFlow} states the row's column ({@link #columnFromFirst}), and where
     * the pipe could stand full of either fluid that is the column of the end with the higher pressure: on a falling water
     * line into a nitrogen tank that is the tank's gas once the tank passes the generator's pressure, so the estimate read
     * a 4 m water column as nitrogen and reported a 4.8 kg/s backflow while the column still drove 4.2 kPa forward, and
     * the connection stayed closed for good. Between two gas endpoints both densities give the same sign.
     */
X
[2918, 8, 'leaves its connection through a dry end', <<'X'],
    /**
     * Whether {@code flow} leaves its connection through a dry end ({@link #dryEnd}). Such an end gives nothing in the step:
     * a larger flow is a forbidden direction ({@link #boundaryAllowed}), and one within that rule's tolerance, 1e-10 kg/s,
     * is no flow, at a converged point and at a polished one alike (an empty tank looped twice to one junction converged
     * with 1.6e-22 kg/s round the loop, and the booking ended it at zero at every step size).
     */
X
[2929, 7, 'an end with nothing behind it to draw', <<'X'],
    /**
     * Decision D54 (every vessel a bag with a stiff wall): an end with nothing behind it to draw. A vessel that holds no
     * fluid ({@link #holdsNoFluid}: placed empty, or holding only solids, which ride the liquid, decision D33) has nothing
     * to carry anything out, so all its ports are dry; a top (VAPOR) port of a slack vessel that holds no gas is dry too:
     * what is above the liquid is the atmosphere the model does not carry.
     */
X
[2942, 14, 'A vessel, or its rate-solve copy, that holds no fluid', <<'X'],
    /**
     * A vessel, or its rate-solve copy, that holds no fluid (decision D54): its fluid, the bag less its solids, at most
     * the bag's resolution, {@link #HOLDS_NOTHING} of its capacity (owner decision D59 (2)), the resolution at which the
     * bag's volume is already snapped to the capacity ({@link PhaseLayout#bagVolume}); below it a vessel's fluid is
     * roundoff, typically a residue a junction's buffer leaves in an emptied tank (1.3e-71 kg, 2.2e-17 kg). Solids ride
     * the liquid and have no fluid to exchange with, so whatever solids it holds such a vessel is neither feed nor stock:
     * an end of it gives nothing ({@link #dryEnd}), its column is the other end's, a junction seed's sweep passes through
     * it, the rest test does not count it, a step lays it out dry ({@link #dry}), and the booking states it at its flash
     * with no closure ({@link InventoryEquilibrium#solve}). The residue stays booked in its inventory (the ledger is exact,
     * D46). A mover's dry suction keeps its port band ({@link InletPhase}).
     */
X
[2967, 9, 'A vessel a step lays out dry when nothing arrives in it', <<'X'],
    /**
     * A vessel a step lays out dry when nothing arrives in it (decision D54) and books nothing for when nothing reaches
     * it: one that holds no fluid at the bag's resolution (owner decision D59 (2)), whatever solids it holds. A residue
     * below it has no phases worth stating (a 4e-33 kg crude residue laid out as two phases stalled the line search at
     * every step size), and solids without a fluid neither move nor change in a step: a bed of them has no unknowns until
     * a fluid arrives.
     */
X
[2981, 11, 'every vessel that runs dry within the step', <<'X'],
    /**
     * Decision D54 (a bag can be emptied): {@code graph} with every vessel that runs dry within the step ({@code emptying})
     * standing as a junction that holds its stock. A vessel drained to nothing has no state of its own to solve for at the
     * step's end; what leaves it is what it held and what reached it, mixed (the junction's backward-Euler rows with the
     * stock as their held term), at the pressure its outlets drain it at, at most the ambient. The junction's buffer is
     * the vessel's inventory, its energy field the vessel's energy plus the ambient's work on its collapsing bag,
     * {@code U + P_ambient V_bag}; its continuity row gains the stock's mass over the step ({@link #emptyings}), and its
     * ports draw the bulk. The vessel's own record is untouched: the reconstruction books it to nothing
     * ({@link ConservativeTransport.Emptying}).
     */
X
[3017, 9, 'every vessel released that a converged pass puts above the ambient', <<'X'],
    /**
     * {@code emptying} with every vessel released that a converged pass puts above the ambient (it holds contents at the
     * step's end after all), or null when none is. A vessel a converged pass of this solve has held at or below the ambient
     * is confirmed in {@code emptied} and never released again within the solve: whether a bag runs dry within the step is
     * decided once (a release that restored the vessel form after the phase correction laid out its last 3e-17 kg as two
     * phases cycled the pass sequence at every step size).
     */
X
[3038, 7, 'whether a pass lays {@code node} out as dry', <<'X'],
    /**
     * Decision D54: whether a pass lays {@code node} out as dry, with no unknowns. A vessel that holds no fluid at the step
     * start ({@link #holdsNoFluidVessel}), whose seed is still its own state, is a receiver at the ambient with no fluid:
     * its state is its pressure and its solids, and it gives nothing. Once the start flows deliver into it
     * ({@link #arrivingPhases}) or a pass's flows reach it ({@link #filled}), it is laid out like every vessel.
     */
X
[3053, 10, "The step's ledger (owner decision D46)", <<'X'],
    /**
     * The step's ledger (owner decision D46): the change of the vessels and filter cakes equals the boundary transfers plus
     * the junction term, per component, per solid population and for energy (pump work included). The junction term is
     * the change of the junctions' buffers (a buffer is the state of its relaxation row, not inventory) plus what a rate
     * evaluation's frozen junctions held back ({@link ConservativeTransport.HeldJunctions}). Each tolerance is scaled by
     * the boundaries' turnover and the stocks held. The energy is {@code U + sum h dm}: no stream carries potential energy
     * (owner decision D37, simplification S1), so a pump's lift work stays in the fluid as heat (0.023 K per 10 m of water).
     */
X
[3117, 7, "must be this state's own temperature bundle", <<'X'],
    /** {@code prepared} must be this state's own temperature bundle, or {@code null} to evaluate the pure-component terms
     * here. A state holding no fluid (a vessel holding only solids, decision D33) has no stream: its ends are dry. The
     * solver still reads a stream at every end where no flow passes (a decode, a closed row, a start estimate, a row's
     * scale), and there it reads an empty vessel's, the ambient's (decision D54), not the 0/0 of no fluid
     * ({@link #carrierViscosity} too). */
X
[3147, 5, 'connections as the warm-flow check may compare them', <<'X'],
        /** The connections as the warm-flow check may compare them: never including the cake. They are the solve's own
         * connections, also in a pass that stands an emptying vessel as a junction (decision D54, {@link #emptyingGraph}),
         * so every pass of the step starts from the same carried flows. */
X
[3168, 6, 'Which nodes an end draws phases of', <<'X'],
        /** Which nodes an end draws phases of ({@link PassiveNetwork.PhasePort} VAPOR or LIQUID) through a connection this
         * pass leaves open: only those nodes build their phase streams at each decode, and only their non-amount columns
         * keep their material-row entries ({@link #buildSparsity}). A closed passive connection carries exactly zero by its
         * own row, so its donor terms are zero whatever it would draw. */
X
[3175, 13, "Each vessel's per-phase capacity", <<'X'],
        /**
         * Each vessel's per-phase capacity (decision D11; {@link PhaseDraw}), kg/s by phase id, for a node with a phase port
         * whose connection may carry outflow from it; null for every other node: {@code c = m_start / (N dt)}, with
         * {@code m_start} the phase stream's mass on the accepted state the step starts from (its solid share included),
         * {@code N} the vessel's phase-port ends whose connection may carry outflow by the static rule
         * ({@link #boundaryAllowed}, a constant of the solve), and {@code dt} this solve's step (the backward-Euler step the
         * interval solver attempts, never the slice). A rate solve has no step: every capacity is infinite, and a port
         * draws the first phase of its order its vessel holds.
         */
X
[3194, 8, 'phase-port segment of each connection end for this pass', <<'X'],
        /**
         * The phase-port segment of each connection end for this pass ({@link PhaseDraw.Segment}; {@code [edge][0]} the
         * first end, {@code [edge][1]} the second), null for a BULK end and for a closed passive connection. Decided once
         * per pass from the start flow ({@link #initialPoint}) and the seeds, like {@link #junctionDonorFirst}: which
         * phases are pinned at their capacity and which one carries the rest is a kink of the stream's composition in the
         * flow, so freezing it keeps each residual smooth; the pass loop re-decides it from the converged flow.
         */
X
[3208, 8, 'Whether a device in this island prescribes its own mass flow', <<'X'],
        /**
         * Whether a device in this island prescribes its own mass flow this pass. A saturated filter inlet and a pump
         * holding a target volume flow are two equations for the same edge once continuity has related them, so the filter
         * capacity is not imposed as a law while one is active; those islands meet the capacity by step refusal. A pump on
         * its head limit prescribes a pressure, not a flow, so it does not count.
         */
X
[3238, 36, "pressure each edge's pressure-dimensioned rows are stated in", <<'X'],
        /**
         * The pressure each edge's pressure-dimensioned rows are stated in, so that the single Newton tolerance means the
         * same relative closure on every island: {@code max(1e5, max(P_first, P_second))} from the endpoint states of this
         * pass. Those rows (the hydraulic balance, a pump's head limit, a valve's regulated inlet pressure and an open
         * valve's zero head) used to divide by a literal 1e5 Pa: at a tolerance of 1e-9 that demands 1e-4 Pa whatever the
         * island runs at, a relative 2.5e-10 at 400 kPa, below what the endpoint pressures can be resolved to through the
         * logarithmic pressure unknown, so a driven line stalled a fraction of a milli-pascal apart (an inline filter, about
         * 150x a plain pipe's resistance, first). Constant through the Newton solve, so out of the Jacobian; floored at
         * 1e5 Pa, so every island at or below atmospheric pressure is bit-identical; per edge, so an island spanning a 4 bar
         * header and an atmospheric vent loosens only the rows at high pressure. The live iterate's pressures were
         * rejected: the scale would enter every row's derivative with a kink.
         */
X
[3277, 26, 'Which end of each connection donates into a zero-holdup junction', <<'X'],
        /**
         * Which end of each connection donates into a zero-holdup junction for the whole of this pass, taken from the point
         * the pass starts at rather than from the live iterate. A junction's mass fractions and specific enthalpy are the
         * mixture of what arrives, a ratio that does not shrink with the flows that form it, so deciding the donor inside
         * the residual put a finite jump in those rows at exactly zero flow, and the one-sided differences the Jacobian is
         * built from sample only the positive side: on a settled island the derivative the step was computed from is not
         * the derivative along the step (on the valve block line at 400 kPa the linear model predicted a residual of 8e-25
         * and the full step produced 1.7e-3, every backtrack on the same plateau). Freezing it makes one pass's residual
         * smooth in its own unknowns, the rule every other switch here follows; a converged point whose flow disagrees
         * with the donor is one more active-set change, and the donor signs join the cycle key. Only the junction mixture
         * reads them; every edge row keeps the live upwind, so an accepted point is upwinded exactly as
         * {@link ConservativeTransport} reconstructs it.
         */
X
[3304, 61, "density each connection's static head {\@code rho*g*dz} is stated with", <<'X'],
        /**
         * Which end's fluid stands in each connection for the whole of this pass (0 the first end's stream, 1 the second's),
         * read off the point the pass starts at rather than from the live iterate: the twin of {@link #junctionDonorFirst}
         * on the hydraulic row. The head {@code rho*g*dz} does not shrink with the flow, so a head read from the upwind end
         * of the current flow had a jump of {@code (rho_first - rho_second)*g*dz} in its row at exactly zero flow, and a
         * settled line landed on a plateau it could not leave (a water generator four blocks below a nitrogen tank at
         * 400 kPa: the row jumped by 2.754e-2, the predicted {@code (996.31-715.46)*9.80665*4/400000}, the linear model
         * predicted 1.08e-19 and the full step produced 2.76e-2). Stating it once per pass makes the column a property of
         * the connection for that pass and the rest point {@code P_b = P_a - rho*g*dz} an actual root.
         *
         * <p>Which column it is, is stated from the pass's own seed pressures and elevations ({@link #columnFromFirst}),
         * never from a flow sign, and it is deliberately not revised by the outer active-set loop (unlike the junction
         * donor): near its own hydrostatic balance each column puts the flow on the other one, so a rule that turned the
         * head over on a converged disagreement would alternate forever. The pass keeps the column it solved under; the
         * friction term keeps the live donor, being odd in the flow with no jump to freeze. Nor is it part of
         * {@link WorkspaceKey}: adding it churned the workspace cache and cost a vertical line at rest its whole run (forty
         * intervals settled at 360918.3068 Pa went to a hold at residual 2.202e-7). What is frozen is the end, never its
         * density: the row reads that end's density on its own trial state, so a riser at rest balances its own fluid's
         * weight at the state solved for, and a dead-end loop of risers carries nothing but the column's discretisation
         * (frozen at the seeds, a dead-end pocket's columns did not cancel round its loops: 1.8e-5 kg/s on the seeds,
         * 2.4e-7 kg/s on the trial's densities).
         */
X
[3445, 5, 'A junction no open connection reaches keeps the pressure it had', <<'X'],
            // A junction no open connection reaches keeps the pressure it had (PhaseLayout.junctionRows). The same holds
            // for a sealed pocket of junctions (hold F8; see pockets): every connection on its boundary is closed, so its
            // members' mass rows sum to zero identically and its pressure level has no row. The pocket's first member
            // keeps its pressure in place of its mass row; the others keep theirs.
X
[3491, 44, 'The column that is its own donor', <<'X'],
                // The column that is its own donor: a column is admissible exactly when the driving pressure it produces
                // points away from the end it was read off. Where both columns agree on the direction only one is
                // admissible (a line pouring downhill stands in what feeds it from above). Taking the start point's upwind
                // end instead reads a numerical zero as a direction: a water generator four blocks under a nitrogen tank at
                // the same pressure, read as the water column, asks for 39 kPa the generator has not got and stalls on the
                // friction kink at zero; a liquid line resting on its own two-metre column, read the other way, takes the
                // lighter density and the 0.18 Pa that leaves drives a permanent 8.3e-7 kg/s through a graph at rest.
                // Neither column admissible (the drive below the heavier head and above the lighter) is where a settled
                // line lives: the column nearest its own rest point, so rest is a fixed point of the rule as of the
                // equations. Both admissible is genuine bistability, and the start point's flow sign is the only evidence
                // of which fluid is in the pipe (deciding by rest point stopped a filling line at the lighter column's
                // balance, 429851 Pa against the 439082 Pa its water column demands). On a flat island dz is zero and the
                // choice multiplies nothing. The rule is {@link #columnFromFirst}, which the start estimate states too.
X
[3541, 15, 'segments of this pass, deciding them from its start flows', <<'X'],
        /**
         * The segments of this pass, decided from its start flows, and those start flows moved inside the velocity cap of
         * the priority mixture they land in (decision D11). The start point starts every connection inside its own cap
         * ({@link #initialMassFlow}), but that is the cap of the stream a port draws at a vanishing flow (the water of a wet
         * tank's bottom port); a start flow beyond the first phase's capacity lands in a mixture whose cap
         * ({@link #capLimit}) can be far lower (137.7 kg/s of start flow against a mixture cap of about 8.7 kg/s on a 2 MPa
         * tank with 2 % water stalled the Newton). Only a connection drawing through a phase port moves (no sign changes,
         * so the frozen donors and columns are unchanged); the segments are then decided again from the moved flows. An
         * end drawing the phase the start point read keeps its flow to the bit.
         */
X
[3604, 12, 'seeds of a pass whose Newton failed', <<'X'],
        /**
         * The seeds of a pass whose Newton failed, with every vessel whose phase-port segments together draw a phase's whole
         * start content (each end that may draw from it pins the phase at its share of the capacity, {@code sum c dt =
         * m_start}) seeded without that content, flashed at the seed's temperature and pressure; null when no seed changes
         * its phase regime that way. A phase drawn to zero usually ends the step on its own phase boundary (a gas no liquid
         * dissolves cannot exist once it has all left), and a pass that keeps its unknowns has no root while halving the
         * step doubles the capacity and the pinned draw is still the whole phase. A starting point and a phase regime only:
         * a converged pass that holds the phase after all is corrected back by the converged-point phase correction.
         */
X
[3685, 12, 'What connection {@code edge} carries out of its end', <<'X'],
        /**
         * What connection {@code edge} carries out of its end {@code end} for the connection flow {@code q}, on that end's
         * node transport {@code node}: the node's bulk at a BULK end (or a closed connection's), the one phase stream the
         * segment draws where it pins none, and otherwise the priority stream's mixture ({@link PhaseDraw}): the pinned
         * phases at their capacities, at the step's start state ({@link #startPhases}), and the marginal phase at the end
         * state with the rest of {@code |q|}. The mixture is a {@link Transport} of rates ({@code mass} the mass flow, every
         * extensive field the matching rate), so every per-kilogram donor term reads it unchanged; its density is the
         * no-slip mixture's, its viscosity the phases' volume-flow average, its velocity limit the smallest of its phases'.
         */
X
[3717, 9, 'velocity cap (kg/s) of the direction leaving end', <<'X'],
        /**
         * The velocity cap (kg/s) of the direction leaving end {@code end} of {@code edge}, on that end's node transport: the
         * drawn stream's {@code density * area * velocityLimit} at a BULK end and where one phase is drawn (the form the cap
         * always had, to the bit); for a priority mixture, a volume flow of {@code area * v}, {@code v} the smallest
         * velocity limit of its phases: the pinned phases at their capacities and the marginal one filling the rest,
         * {@code C + rho_m (A v - sum c/rho)}, or, where the pinned phases alone would exceed it, the pinned mixture scaled
         * down to it, {@code C A v / sum c/rho} (continuous at {@code A v = sum c/rho}).
         */
X
[3821, 5, 'Exact structural zeros', <<'X'],
            // Exact structural zeros: phase allocation, T and P cannot change bulk component flow at fixed total amounts and
            // kg/s, so cancellation-noise derivatives are suppressed, not small physics. Only for a BULK end: what a phase
            // port carries is the phase split itself, so a node any end of which draws a phase keeps every entry.
X
[3845, 21, 'Whether a chord step on the Jacobian built at', <<'X'],
        /**
         * Whether a chord step on the Jacobian built at {@code built} still closes at least half of every row's gap at
         * {@code now} ({@link #chordHolds}): the slopes a connection's row states are its loss slope in its flow and its
         * slope in each end's pressure unknown. The loss slope (decision A371): a line's resistance follows its flow,
         * proportional to it on the turbulent branch, so a Newton walking a gas line from 2.0e-3 kg/s (Re 15 000) to a root
         * near rest (1.5e-5 kg/s, Re 109) halves its flow per fresh step, while a chord step keeping the resistance of a flow
         * twice as large closes only half the gap and the contraction test reads the halving as progress until the
         * iteration limit. The pressure slopes: a row's slope in an end's {@code ln(P / 1e5)} is that end's pressure, so a
         * node whose pressure has moved since the Jacobian was built has every row through it stated with the old pressure
         * (two junctions seeded 373 kPa apart stated a filter's pressure columns at 1/1.55 of their own and overshot its
         * gap by 0.55 per step). The rows and roots are unchanged; what moves is which Jacobian a step uses.
         */
X
[3898, 6, 'Approach a nonnegative amount boundary directly', <<'X'],
            // Approach a nonnegative amount boundary directly instead of repeatedly halving a step that misses it by
            // roundoff; this bounds a trial step and never clips accepted inventory. Below clampFloors the unknown is not a
            // boundary this step has to respect: a solid moment 1e-30 below its scale would bound alpha at 1e-12, where
            // x+alpha*d is bitwise x for every unknown of order one.
X
[3907, 8, 'other half of the trace rule in', <<'X'],
        /**
         * The other half of the trace rule in {@link #maximumStep}: a fluid amount at or below its floor does not bound the
         * step, so a trial that would take it below {@value #TRACE_RETENTION} of its value keeps that fraction instead, the
         * share the {@code .99} rule would have left it; the rest of the step is taken as the Newton direction says
         * ({@link #TRACE_CLAMP_FRACTION}). Solid moments keep their own projection in {@link PhaseLayout#decode}.
         */
X
[3921, 8, 'Decision A275: a line carries at most its cap', <<'X'],
        /**
         * Decision A275: a line carries at most its cap, in a trial as in a converged point. A trial that puts a line on its
         * law past the cap flow of the end it leaves (on the trial's own donor state) is moved to that cap flow; a capped
         * line's row states it already. Without it the first steps of a pump starting on a junction at 1 atm drew kilograms
         * of nitrogen per second from a void branch whose cap is a fraction of that, and the Newton ended on a plateau at
         * 4e-8. A trial outside the property domain is left to the residual, which refuses it.
         */
X
[3991, 11, 'What a connection carries out of one node, per kilogram and for its loss and cap', <<'X'],
        /**
         * What a connection carries out of one node, per kilogram and for its loss and cap: the conserved amounts and their
         * mass (the normaliser), the volume and density, the viscosity, the specific enthalpy, the velocity limit and the
         * solid moments. A node's cached entry is its BULK stream, the whole state as the donor terms have always read it
         * ({@code moles/mass} is the double {@code moles/state.mass()} was), and on a donor holding solids but no liquid its
         * gas stream (owner decision D33, {@link FluidThermodynamics#bulkDrawsGas}); {@code phases} are its phase streams by
         * phase id (null for a phase it does not hold), built only for a node an end of which is a phase port (decision D11).
         */
X
[4111, 19, 'Whether {@code node} is a junction', <<'X'],
        /**
         * Whether {@code node} is a junction whose every connection other than {@code edge} is closed, or a sealed pocket
         * behind {@code edge} (hold F8, {@link #pockets}: with {@code edge} left out, the junctions its coupling connections
         * reach touch no vessel, port, generator or void, no regulating valve draws from them, and every other connection
         * leaving them is closed). Its mass rows then force zero flow through {@code edge} at every solution, so the edge's
         * cap can never be active there and its row's only job is to place the junction pressure; the capped branch has no
         * pressure column, so an iterate landing on it with the flow fixed by the mass row makes the Jacobian structurally
         * singular (a dead-headed pump, and a pipe from a generator into an inline filter's junctions with a closed pump
         * behind them). Such an edge gets no cap.
         */
X
[4155, 8, 'start cap state of every connection neither the pass loop nor the carry decided', <<'X'],
        /** The connections {@link #startCaps} put on their cap in this pass (decided on the start point, not by the pass loop). */
X
[4164, 1, 'private void startCaps() {', <<'X'],
        /** The start cap state of every connection neither the pass loop nor the carry decided (decision A271): on the cap when
         * its driving pressure at this pass's start point exceeds the cap's pressure drop, in the driving pressure's direction
         * (a device's or a filter's: its start flow's), otherwise on its law, the branch the flow row's clamp read at a
         * Newton's first iterate before the cap was a mode. A connection that cannot be capped in this pass is on its law. */
        private void startCaps() {
X
[4182, 28, 'the capped junctions (A276, A341)', <<'X'],
        /**
         * Owner decision D59 (5), the capped junctions (A276, A341): a junction whose lines are all choked must still be able
         * to balance. A junction holds nothing, so its mass balance is what places its pressure; a line on its law reads
         * that pressure, a line on its cap only when the cap leaves the junction (the cap flow of the junction's own state
         * rises with its pressure), and a cap that enters it, a mover at its target and a closed line read it not at all.
         * So a junction with no line on its law balances only if some cap leaves it and some cap enters it; where every cap
         * enters it nothing places its pressure and the step is singular, where every cap leaves it its balance has no root.
         * In real life its pressure moves until one of its lines leaves its choke: the one whose drive exceeds its cap's
         * pressure drop by the least, in pascals. That line is on its law in this pass, read on the start point, decided
         * with the other cap states before the Newton. The same holds for a group of junctions joined by law lines (the
         * pockets of hold F8 with capped lines as their boundary): one line is released per group and round, until every
         * group can balance. Only a cap the start point decided is released ({@link #startCaps}): a cap the pass loop
         * entered from a converged point or a failed iterate (A271, A274) is evidence from a Newton that ran, and
         * releasing it returns the pass loop to the state that produced it.
         */
X
[4250, 9, 'cap state the pass loop moves connection {@code edge} to at the converged point', <<'X'],
        /**
         * The cap state the pass loop moves connection {@code edge} to at the converged point {@code x}, or its own when the
         * point holds it (decision A271). On its law, it enters the cap when its flow exceeds the cap flow of the end that
         * flow leaves, by the margin the velocity constraint was always checked with ({@code 2e-8} relative); on its cap, it
         * leaves when the driving pressure in its direction falls short of the cap's pressure drop by more than the row
         * resolves ({@code tolerance} of its pressure unit). Inside that band a connection keeps its mode, so a point at the
         * cap's edge, where both modes are the same equation, never alternates.
         */
X
[4271, 8, "cap corrections a failed Newton's last iterate", <<'X'],
        /**
         * The cap corrections a failed Newton's last iterate {@code x} shows (decision A274), applied to {@code caps}; true
         * when any was made. Every capped connection {@link #capTransition} would release there is released; when none is,
         * every connection on its law that the iterate holds at its cap ({@link #projectFlows}: its flow at the cap flow of
         * the end it leaves, its drive past the cap's pressure drop that way) is put on its cap. A drive past the cap drop at
         * a flow still below the cap is a Newton not yet converged. A point outside the property domain changes nothing.
         */
X
[4308, 7, 'A filter has a second inlet limit', <<'X'],
                // A filter has a second inlet limit: the cake it can still hold. The retained volume at the end of this step
                // is the expression the loading row states, so bounding the inlet by the room divided by the step's own
                // retention per unit mass makes the filter fill exactly at a step boundary. A rate solve carries no step and
                // no retention, so it is exempt, and a base already at capacity is left to the stage guard.
X
[4341, 9, "static column is this pass's, never the live iterate's upwind end", <<'X'],
            // The static column is this pass's, never the live iterate's upwind end ({@link #columnEnds}); on a flat island
            // dz is zero and the term is bitwise the zero it was. A pump's column is the fluid it moves, the suction node's
            // transported density at this trial (the density its rise limit, target row, velocity clamp and shutoff margin
            // read), never the destination's (decision D6). The column end is the pass's; its density is the trial's, the
            // column that end's port stands in the connection ({@link #columnDensity}, owner decision D34).
X
[4361, 12, 'velocity cap is an active-set mode of the connection', <<'X'],
                // The velocity cap is an active-set mode of the connection (decision A270): {@link #capDirections} says, for
                // the whole pass, whether this connection runs on its law or at its cap, and in which direction. On the cap
                // its row states the cap flow of the end it leaves, reading that end's state and neither end's pressure: a
                // choked line passes what its upstream state lets through. Which mode a pass runs in is the pass loop's
                // decision ({@link #capTransition}), never the live iterate's, so the row is smooth in its own unknowns
                // (before, a junction between capped lines saw its pressure column vanish and come back between iterates).
                // On the law a plain passive connection's scale reads both ends' cap drops, so it needs no direction; a
                // device or a filter keeps its flow's donor.
X
[4381, 21, 'A saturated pressure/flow law', <<'X'],
                // A saturated pressure/flow law: the unused driving pressure is throttled, and the same bounded flow unknown
                // enters every component and enthalpy balance; no post-solve clipping. Both modes are stated in one pressure
                // unit, the loss against the driving pressure on the law and against the cap's own drop on the cap, over
                // S = min(pressureScale, 2 scaleDrop): as a flow row the capped branch sat on a scale 80x apart from the
                // hydraulic row's on a 50 mm line and a Newton crossing the cap exit stalled on the mismatch. The floor keeps
                // a filter with no room left from a zero scale. On a plain passive connection scaleDrop is the smaller of the
                // two ends' cap drops, not the donor's: the scale weighs the row and says nothing about which way the fluid
                // goes, and at a zero-flow root neither end is the donor; taken from the donor the sign chose, S jumped at
                // that root wherever the two ends hold different fluids (water and nitrogen: cap drops orders apart), every
                // differenced Jacobian straddled the jump and the Newton contracted only linearly (hold class E6). A device
                // or a filter keeps its flow donor's drop.
X
[4427, 20, 'Jacobian from per-node block perturbations', <<'X'],
        /**
         * The Jacobian from per-node block perturbations instead of a coloured whole-island sweep: one local column at a
         * time, the node decoded at the perturbed point and only the rows that can see it re-assembled (its own block, the
         * block of each neighbour across an incident edge, and the hydraulic, actuator and filter-loading rows of those
         * edges; the flow, actuator and filter columns sweep the same way over their own edge). Every such row is produced
         * by the same {@link #nodeAccumulate}, {@link #edgeRows} and {@link #nodeRows} the whole-island residual uses, in the
         * same accumulation order, so each entry is the double the coloured sweep computed. The cost this removes is the
         * assembly: the coloured sweep paid a complete island residual for each of its 143 to 191 colour groups.
         */
X
[4551, 11, "This node's three aggregate solid rows", <<'X'],
        /**
         * This node's three aggregate solid rows against the targets this evaluation accumulated, which is what makes them
         * rows a neighbour's column can move. {@link PhaseLayout#balanceRows} and {@link PhaseLayout#junctionRows} write
         * these rows against the layout's own seed reference, so both whole-island assembly and block sweep overwrite them
         * here with the same targets in the same order, or a neighbour's column would be differentiated against the seed
         * reference while the base residual holds the real one: a silently wrong derivative rather than a missing one.
         */
X
[4575, 17, "Fills {\@link #fractions} with the mass fractions this junction's rows mix to", <<'X'],
        /**
         * Fills {@link #fractions} with the mass fractions this junction's rows mix to and returns the specific enthalpy,
         * and sets {@link #junctionWeight}. A step solve is backward Euler over the step on the junction's owned inventory,
         * {@code (m/dt + Q) w = (m/dt) w_old + sum q w_e} and {@code (m/dt + Q) h = E_old/dt + sum q (h_e + g dz)}, with m,
         * w_old and E_old = m h_old read from the graph this step solves. The rows are stated in amount form over the
         * junction's own mass, {@code (dt/m)(m/dt + Q)(w - w_mix)}: a row at the Newton tolerance bounds the step's species
         * error at tol m_J, and the rounding floor {@code eps (1 + dt Q/m)} does not grow as the step shrinks (in rate form
         * it did, and every refinement chain ended on it). A rate-only solve holds the junction's composition and enthalpy
         * at its owned inventory's, its pressure algebraic, and the reconstruction books its accumulation as a
         * pseudo-boundary (frozen junctions).
         */
X
);
