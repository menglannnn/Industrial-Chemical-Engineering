# Condensation spec (batch 3) at 5c5b51b6. Entries: [file, start, count, phrase, text].
my $M = 'src/main/java/com/wormzjl/createcheme';
my $IC = "$M/runtime/fluid/IslandCoordinator.java";
my $CE = "$M/runtime/fluid/IslandCertificate.java";
my $PN = "$M/science/fluid/network/PassiveNetwork.java";
my $IE = "$M/science/fluid/network/InventoryEquilibrium.java";
my $PID = "$M/science/thermo/phase/PhaseIdentification.java";
my $PD = "$M/science/fluid/network/PhaseDraw.java";
my $SE = "$M/science/fluid/network/SolidEventIntegrator.java";
my $CC = "$M/runtime/fluid/FluidCheckpointCodec.java";
my $SD = "$M/science/fluid/diagnostics/SolverDiagnostics.java";
my $HM = "$M/science/fluid/thermo/HydrocarbonModel.java";
my $IP = "$M/science/fluid/network/InletPhase.java";
my $PC = "$M/science/thermo/phase/PhaseContract.java";
my $PR = "$M/runtime/fluid/PhysicalRegistry.java";
my $TP = "$M/science/fluid/thermo/TranslatedPengRobinson.java";
my $CS = "$M/runtime/fluid/FluidCheckpointStore.java";
my $CP = "$M/science/thermo/phase/CrystalPhaseEvaluator.java";
(
[$IC, 9, 28, 'Server-thread authority over island clocks', <<'X'],
/**
 * Server-thread authority over island clocks and atomic result publication. Workers receive only commands; a round
 * contains at most the available dispatch capacity, and the dispatcher must deliver terminal callbacks asynchronously
 * on this coordinator's owning thread. Scheduling is driven by deadlines, dependency changes and completions, never by
 * visiting every island on every tick: each island is ready (its next slice can be dispatched now), holds one deadline
 * in the {@link IslandScheduler} for the epoch tick at which time alone makes it ready, or waits for an event that
 * re-examines it (a completion, a fence change, a property resume); every change to what decides readiness re-examines
 * exactly the island it changed. An island whose solved intervals repeat is certified ({@link IslandCertificate}): never
 * ready, its solver caches released, one deadline at its certificate horizon; its committed time is materialised on
 * demand by the identity or by scaled replay of its last solved interval, never past the online clock, a fence or a
 * hold. {@link #register} restores a saved certificate while its signature holds. Every snapshot carries a payload
 * generation, a JVM-wide unique number that changes whenever anything a checkpoint payload records about the island
 * changes and never when a certified island is only materialised, so a checkpoint can keep an encoded payload while
 * its island's generation stands.
 */
X
[$IC, 558, 21, 'The hold policy', <<'X'],
    /**
     * The hold policy: what a held attempt (no committed candidate, no refused transaction) changes before the retry.
     * Every hold hands the retry a fresh {@link RetainedSolver}: the one the attempt used carries the step solver's last
     * flows, modes and factorizations from wherever inside the slice that attempt stopped, and a retry that warm-starts
     * from them starts the slice again from its first state with the flows of a later one (a rate solve starting from
     * 0.45 and -0.22 kg/s on tanks still at rest failed the same way at every retry). With a fresh solver a retry is the
     * same computation as the first attempt, so every hold also changes the problem: a slice longer than one tick is
     * halved (a shorter whole interval, never a partial commit; the first full acceptance doubles it back), and a hold
     * at one tick waits {@code cadence * 2^k} ticks for its retry, k the number of such holds in a row, at most
     * {@link #MAXIMUM_RETRY_DOUBLINGS}. Anything that changes the island's inputs (a fence, a released fence, a property
     * resume) clears the wait and the count. Online ticks only: no wall clock decides a delay.
     */
X
[$CE, 171, 31, 'How far the later of two consecutive intervals is from a stationary repeat', <<'X'],
    /**
     * How far the later of two consecutive intervals is from a stationary repeat of the earlier one: the first discrete
     * difference, or null when there is none, and for each continuous test the smallest tolerance it passes at. The
     * pair is stationary at a tolerance {@code eps} exactly when {@code discrete} is null and every measure is at most
     * {@code eps} ({@link #refusal}); {@link #measure} is the smallest such {@code eps}. {@code discrete}: unequal
     * durations; an interval not FULL, or with a solid transport transition, a closure, cake, phase or node-solid change
     * inside it; unequal modes, phases, blocked masks, filter cakes or fixed-node states. {@code state}: the larger
     * interval's largest relative temperature or pressure drift of a finite node (a node drifting at a constant rate is
     * not stationary). {@code component} and {@code energy}: the largest change between the intervals of a finite node's
     * component or internal-energy change, relative to that inventory or {@link #energyScale}. {@code flow}: per pipe
     * the smaller of two ratios, the largest over the pipes: the change of its flow relative to the larger of that flow
     * and the island's largest flow, and the mass it moves in one interval relative to the smaller finite inventory it
     * draws on ({@link Summary#referenceMass}), so a pipe that moves less than {@code eps} of that inventory is quiet
     * (otherwise the relative test is ill-conditioned wherever the island's largest flow is solver noise, a settled
     * closed island at 1e-10 kg/s). {@code flowChange} and {@code flowMoves} are the two ratios of the pipe that sets
     * {@code flow}; {@code largestRelativeFlowChange} the largest first ratio over all pipes, for diagnostics. Nothing
     * here decides accounting: replay scales the recorded interval, so conservation is exact whatever the tolerance.
     */
X
[$CE, 275, 11, 'The certificate the last interval supports', <<'X'],
    /**
     * The certificate the last interval supports, or empty with the reason. When every change, flow, gross pipe
     * transfer, boundary transfer and pump work of the interval is exactly zero (the identity map, d = 0) its horizon is
     * the policy's recheck period, or {@link Long#MAX_VALUE} without one; none when the averages are zero but something
     * still moved back and forth. Otherwise it also needs no solids in transport and no filter on the island, and a
     * horizon of at least one interval: {@code min(K_max, floor(budget / d))} intervals, d the largest relative
     * per-interval change of any finite node component, internal energy, temperature or pressure, cut short before any
     * extrapolated component could reach zero.
     */
X
[$PN, 12, 14, 'What one end of a connection draws from its node', <<'X'],
    /**
     * What one end of a connection draws from its node when the flow leaves the node through it: the whole state
     * ({@code BULK}), or its phases by priority (decision D11, {@code PhaseDraw}): a {@code LIQUID} port (a tank's bottom
     * face) the heavier liquid, then the lighter, then the gas, a {@code VAPOR} port (its top face) the gas, then the
     * lighter liquid, then the heavier, each phase up to what the vessel held of it at the step start and the next phase
     * beyond that, a liquid with its volume share of the solids. A port never closes for want of a phase, and acts on
     * outflow only (inflow is delivered as through a bulk end). The port's driving pressure is the node's pressure plus
     * a per-end offset ({@code PassiveStepSolver.endPressure}: the level head at a LIQUID port, decision D9). Only a
     * finite vessel has phases to select from, so a non-BULK port is valid only on a {@link NodeKind#RESERVOIR} or its
     * rate-solve copy, a {@link NodeKind#PORT}.
     */
X
[$PN, 74, 14, 'Sizes every junction that is not sized yet', <<'X'],
    /**
     * Sizes every junction that is not sized yet ({@link #unsized}), once, from its seed state and its connections:
     * {@code m_J = PassiveStepSolver.HOLDUP_TAU * max over its connections of rho_seed * minimumArea * velocityLimit(seed)},
     * the velocity cap's mass flow on the junction's seed state; an unconnected junction is given 1 kg so its stock is
     * positive. The mint is once, at the seed state (a later density only changes the mixing lag); the buffer's mass
     * then stays exactly m_J (the mass pin of {@code ConservativeTransport.reconstruct}), a topology edit recompiles the
     * island and mints its junctions again, and nothing is booked at a mint (owner decision D46). A graph with no
     * unsized junction is returned unchanged.
     */
X
[$PN, 91, 10, 'The junctions of an island formed from placed blocks, minted', <<'X'],
    /**
     * The junctions of an island formed from placed blocks, minted (owner decisions D54 and D55): a junction holds
     * nothing a placement put there, so its buffer, the state of its relaxation row (D46), starts from a guess, which its
     * 0.05 s relaxation washes out: the island-wide fixed point's last round ({@link PassiveStepSolver#fixedPointStarts}),
     * from which every junction is sized ({@link #sizeJunctionHoldups}), so on a fluid a source supplies. A graph built
     * directly keeps the junction states it was given.
     */
X
[$PN, 156, 12, 'is a two-bit mask', <<'X'],
    /**
     * {@code blockedDirections} is a two-bit mask (1 forbids flow from {@link #first} to {@link #second}, 2 the reverse),
     * an int because the saved checkpoint format is one; every closure the solid transport machinery makes produces 3 (a
     * settled bed and a filter at capacity are properties of the connection), while a saved checkpoint or a caller may
     * carry 1 or 2. {@code firstPort} and {@code secondPort} are what the connection draws from its first and second node
     * when the flow leaves that node through it ({@link PhasePort}); every constructor but the canonical one passes
     * {@link PhasePort#BULK} at both ends.
     */
X
[$PN, 173, 12, 'Everything about this connection that decides the shape and the coefficients', <<'X'],
        /**
         * Everything about this connection that decides the shape and the coefficients of the equations it contributes,
         * and nothing that only moves their right-hand side. The captured cake is exactly that right-hand side: each step
         * rebuilds the pipe list with the cake it left, so a retained Newton workspace, sparsity pattern, ordering or warm
         * flow keyed on the whole {@link Pipe} record would miss on every step of any island with a filter. The filter's
         * clean resistance and capacity are coefficients and belong here, as do the two ports, which decide which phase an
         * outflow's donor terms read.
         */
X
[$IE, 165, 14, 'The vessel', <<'X'],
    /**
     * The vessel's equilibrium from {@code state}: {@code closure} solved in a phase layout, and the layout decided by the
     * outer phase check (owner decisions D25 and D26, {@code FluidThermodynamics.phaseCheck}). A phase leaves only when
     * the closure empties it (the trace fold of its point, and the vanishing rule of owner decision D38 C: under 1e-6 of
     * the vessel's fluid amount and at a smaller share than in {@code state}); where the TP answer finds a phase missing,
     * the vessel is seeded at its own split, the closure started from the vessel's own state at the crossing (owner
     * decision D40 A), and no populated phase set is swapped for another on a TP verdict. A failed closure's last iterate
     * is no state (D28 A', D39, D40 A): it is only folded by the same two rules and proposes nothing. A phase the check
     * removed from a converged answer, or the own split refused, is not added again within this solve (A85), and a
     * repeated regime is the cycle that refuses the solve. One rule for a vessel's own closure ({@link #solve}) and for
     * the crystal refresh's joint closure (decision D30).
     */
X
[$IE, 210, 24, 'Decision D36, G2: the vessel', <<'X'],
    /**
     * Decision D36, G2: the vessel's state at what it holds. Its inventory (amounts, internal energy) and its capacity
     * are the conserved truth and fix temperature, pressure and volume through the equation of state and the wall
     * (decision D54, {@link PhaseLayout#wallRow}), so the energy and wall equations are solved in the phase layout of
     * {@code state} and from it, to {@link #CLOSURE}, and the result is given exactly the inventory's amounts
     * ({@link ConservativeTransport#repartition}). A state already within the closure is returned as it is (an island at
     * rest stays bit for bit). The phase set is {@code state}'s less any phase the restatement folds away (decision D34),
     * the vessel then closed again without it; whether the set holds is the caller's question. A vessel holding dry
     * solids only is at the temperature its energy gives the solids, at {@code state}'s pressure. The equilibrium it is
     * closed to is the one the step solves the vessel with: {@code support}, the frozen phase support of the step's own
     * layout for this node, or, when null, the support the next solve derives from {@code state} (closed in full
     * equilibrium instead, a crude tank at rest stood 0.71 Pa above the truncated equilibrium the step states, and the
     * gate refused every step). {@code workFrom}: the bag volume a step started from ({@link PhaseLayout#bagVolume}) when
     * {@code inventory}'s energy is the step's booked energy before the wall's work; the closed state then holds that
     * energy less {@code P_ambient (V_bag - workFrom)}. {@code NaN}: the inventory's energy is the state's.
     */
X
[$IE, 248, 8, 'The restatement applies the flash', <<'X'],
        // The restatement applies the flash's trace-phase rule (decision D34), so a phase the closure left as a trace is
        // folded away and the vessel is closed again without it, in the folded state's own layout (decision D30). Folded
        // and only checked, a liquid-full LNG tank whose closure left its last bubble just under the trace share missed
        // its volume by that bubble (1.9e-8 against the 1e-8 check) and its step was refused at every size. A fold only
        // removes a phase, so this ends; a step whose reconstruction folded a phase is solved again from the folded state.
X
[$IE, 266, 13, 'Owner decision D26: a vessel', <<'X'],
    /**
     * Owner decision D26: a vessel's own split, the seed of a phase the outer phase check adds
     * ({@code FluidThermodynamics.phaseCheck}): its closure ({@link #close}) at {@code inventory}, from {@code start} (a
     * state holding the phases to lay the vessel out with), the wall's work booked from the bag volume {@code workFrom}:
     * the equilibrium of its energy and volume while the wall is taut, of its enthalpy at the ambient pressure while
     * slack. The split answers by where the closure went: a phase it drove under the trace share is not in it, and the
     * check refuses that phase for the solve, as it refuses one the closure is driving to nothing
     * ({@code FluidThermodynamics.vanishing}). {@code null} where the closure stops short of its tolerance or refuses the
     * state: a failed closure proposes nothing (owner decision D40 A; its last iterate standing as the split put all of a
     * liquid-full slush tank's 14 420 mol in the vapour slot and every later pass failed).
     */
X
[$PID, 5, 49, 'The phase identification parameter of Venkatarathnam and Oellrich', <<'X'],
/**
 * The phase identification parameter of Venkatarathnam and Oellrich (2011, Fluid Phase Equilibria 301, 225-233) on
 * the untranslated PR78 cubic: a liquid-like or vapour-like label for one fluid state that needs no saturation
 * property and no mixture critical point. {@code PIP = v [ (d2P/dT dv) / (dP/dT)_v - (d2P/dv2) / (dP/dv)_T ]}, which
 * is {@code d ln[(dP/dT)_v / -(dP/dv)_T] / d ln v} at constant temperature: the ideal gas has {@code PIP = 1}, a dense
 * liquid tends to {@code v/(v - b) > 1}, a dilute real gas is {@code 1 + (B - T dB/dT)/v} with the cubic's second
 * virial coefficient {@code B = b - a/(R T)}, below one while {@code B < T dB/dT}; so {@code PIP > 1} is liquid-like
 * and {@code PIP < 1} vapour-like.
 *
 * <p>The label rule: liquid-like when {@code PIP > max(1, Z)}, {@code Z = P v/(R T)} of the same untranslated root;
 * vapour-like otherwise. The bare {@code PIP > 1} misreads a hot gas: {@code B - T dB/dT} turns positive above the
 * Boyle temperature and a dilute gas then has a parameter just above one (nitrogen above about 620 K: 1.000079 at 700 K
 * and 0.1 MPa), but such a gas has {@code Z = 1 + B/v} above one and above its parameter, since to first order
 * {@code PIP - Z = -T (dB/dT)/v < 0} wherever the alpha function decreases (to 1388 K for the bundled components, above
 * every fluid domain's 1200 K). A liquid-like state has its parameter well above one and above {@code Z}: every liquid
 * and dense supercritical state with {@code Z < 1} (nitrogen at 130 K and 6 MPa: {@code Z} 0.39, {@code PIP} 4.97), and
 * a compressed liquid whose {@code Z} has reached one ({@code crude_pc12} at 293.15 K and 2 MPa: {@code Z} 1.15,
 * {@code PIP} about 92), which a bare {@code Z < 1} guard would call vapour. Measured over the one-root states with
 * {@code PIP > 1} and {@code Z >= 1} of every bundled component inside their fluid domains, those less dense than the
 * cubic's critical volume ({@code v > 3.95 b}) all have {@code PIP < Z} and the denser ones {@code PIP - Z > 91}. The
 * three-root case does not use this rule: there the stability root decides.</p>
 *
 * <p>For {@code P = R T/(v - b) - a/D}, {@code D = v^2 + 2 b v - b^2}, the four derivatives are the ones
 * {@code TranslatedPengRobinson.fill} forms for its volumetric block:</p>
 * <pre>
 *   dP/dv    = -R T/(v - b)^2 + 2 a (v + b)/D^2
 *   dP/dT    =  R/(v - b) - (da/dT)/D
 *   d2P/dv2  =  2 R T/(v - b)^3 + 2 a/D^2 - 8 a (v + b)^2/D^3
 *   d2P/dTdv = -R/(v - b)^2 + 2 (da/dT)(v + b)/D^2
 * </pre>
 * <p>so the parameter needs the mixture's {@code a}, {@code b}, {@code da/dT} and the root's volume only, evaluated on
 * the untranslated volume {@code v = Z R T/P} (the label belongs to the root, not to the density correction). It is
 * undefined where {@code (dP/dT)_v = 0} and at a mechanically unstable root; at a pure-component critical point the
 * ratio has a finite limit but separates nothing, which is why the critical band ({@link FluidTpEquilibrium}) grades
 * such states research-only whatever their label.</p>
 */
X
[$PD, 10, 33, 'What a vessel', <<'X'],
/**
 * What a vessel's phase port draws (decision D11: ports carry mixed phases by priority). Priority: a bottom
 * ({@link PassiveNetwork.PhasePort#LIQUID}) port draws the heavier of the vessel's two liquids
 * ({@link FluidThermodynamics#heavierLiquid}), then the lighter, then the gas; a top ({@link PassiveNetwork.PhasePort#VAPOR})
 * port the gas, then the lighter liquid, then the heavier; each liquid carries its volume share of the solids
 * ({@link FluidThermodynamics#phaseSolidShare}). A BULK end draws the bulk, except on a donor holding solids but no
 * liquid, whose bulk end draws its gas (owner decision D33, {@link FluidThermodynamics#bulkDrawsGas}).
 *
 * <p>Capacity and the priority stream: a phase can be drawn through a port over one step only up to what the vessel
 * held of it at the step start, {@code c = m_start / (N dt)} (kg/s), {@code N} the vessel's phase-port ends whose
 * connection may carry outflow by the static rule, {@code dt} the step (infinite in a rate solve). A port carrying the
 * outflow {@code q} draws the first phase of its order up to its capacity, the second with what is left, and so on; the
 * last phase the vessel holds takes whatever the earlier ones do not (a port's total flow is limited by the hydraulics
 * and the velocity cap alone). A phase drawn at its capacity (pinned) is drawn at the composition, enthalpy and density
 * of the step's start state, which exists whatever the end state holds (a phase drawn at its capacity usually ends the
 * step at zero, on its own phase boundary); the phase that carries the rest (marginal) is drawn at the step's end state,
 * and must be held by the pass's seed. Which phases are pinned and which one is marginal is a kink in the stream's
 * composition as a function of {@code q}, so it is frozen for one active-set pass (a {@link Segment}, decided from the
 * pass's start flow and seeds) and re-decided from the converged flow by the pass loop.</p>
 */
X
[$SE, 11, 19, 'Every transport closure names both directions', <<'X'],
    /**
     * Every transport closure names both directions: a filter that has met its capacity carries nothing either way, and
     * a bed that has settled out of a carrier too slow to suspend it is in the connection, not in one end of it. It is
     * also what the step solver can work with: {@link PassiveStepSolver} presets {@code boundaryClosed} only for a fully
     * closed connection, and a one-directional closure left the first Newton pass free to drive flow through the
     * forbidden direction (a ten-reservoir chain 70 to 51 ms and a thirty-reservoir chain 215 to 103 ms over eleven warm
     * repeats once both were closed). The next interval's rate pass reconsiders every closure from scratch, so none is a
     * permanent physical deposit.
     */
X
[$SE, 117, 14, 'Which of an over-populated receiver', <<'X'],
    /**
     * Which of an over-populated receiver's flagged connections actually has to stop: one per receiver, and never one
     * whose delivery the limit did not need to refuse. Every open inbound connection of a receiver whose union does not
     * fit is flagged, so the shared rule (lowest velocity ratio, then lowest identity) could name a feed whose removal
     * leaves the union over the limit anyway (a receiver holding sixty grades closed a four-grade feed first and a
     * ten-grade feed on the next pass). So the feed that closes is the one contributing the most keys no other open
     * inbound donor supplies; the rest stay open and are reconsidered next pass. Ties fall back to the shared rule.
     */
X
[$SE, 166, 14, 'The conserved population keys that can reach each node over one step', <<'X'],
    /**
     * The conserved population keys that can reach each node over one step, or {@code null} when this island cannot put
     * more than {@link SolidInventory#MAXIMUM_POPULATIONS} of them anywhere. The reconstruction's population system has
     * only positive coefficients, so a stock reaches every node connected to it by open connections a filter does not
     * empty, and the union that decides whether an inventory can be built is the transitive one; a fixed node neither
     * receives nor forwards. If the whole island holds 64 or fewer populations with duplicates, no union can exceed 64,
     * one field read per node.
     */
X
[$SE, 272, 8, 'Reconsider every closure on a new requested interval', <<'X'],
        // Reconsider every closure on a new requested interval; none is a permanent physical deposit. A closure the
        // interval starts with is reported like any other, at the interval's own t0. The check reads one rate solve of the
        // port graph (PassiveStepSolver.portRate); it only pre-empts a closure the steps would find, so a rate solve that
        // fails declares no closure and the steps decide (hold class E1).
X
[$CC, 20, 43, 'Checkpoint format 10 of committed science and runtime state', <<'X'],
/**
 * Checkpoint format 10 of committed science and runtime state: what one storage unit holds and what the core record
 * holds. Rebuilds properties through the immutable model; it never deserializes implementation caches or creates a
 * nitrogen charge. Where the units live is {@link FluidCheckpointStore}'s business.
 *
 * <p>Core record (one NBT compound, the world's saved data): {@code FluidFormat = 10}; the world {@code Epoch};
 * {@code Strings}, an append-only table of the long texts units refer to; {@code Packages}, one entry per property
 * package with the thermodynamic revision and energy reference the units were written under; the pack files and their
 * sizes; the topology unit's reference; and {@code Islands}, the island index in columns: identity, revision and unit
 * generation, the clock ({@code Online}, {@code Committed}, {@code Retry}, {@code Cadence}), the certificate summary
 * ({@code Base} or {@link #AWAKE}, {@code Since}, {@code Start}, {@code Horizon}) and where the island's unit lies with
 * its SHA-256. {@code SHA256} covers every field.</p>
 *
 * <p>Island unit (binary, {@link Writer}, {@link Reader}): a header naming the island, its revision and the unit's
 * generation (all three must match the index), then its dimension and package, status and fences; the graph's topology
 * once (node identities, elevations, kinds and volumes, pipe ends, sections, controls as tag 0 passive, 1 pump, 2
 * valve, 3 compressor, and the two ends' phase ports) and its state; the last solved interval with its end flows,
 * which the island's next job starts from; and, for a certified island, its certificate signature and the graph its
 * certified interval started from. Nothing derived is stored, and a certified island's unit records its certificate
 * base, so while it stays certified its unit never changes. Solids (every stock, stream, cake and pending recovery):
 * each population as its material's id, revision, density and heat capacity, then its size and mass; the id names a
 * crystal the package admits or a custom solid (owner decision D23), and the reader refuses an id that names no record
 * of the package or saved constants that are not the record's. No other format is read: format 9 and older are refused
 * with the instruction to create a fresh world (format 10, unit format 3, dropped the movers' efficiency, the
 * approximate fallback and the saved interval's acceptance: every interval is a full solve).</p>
 */
X
[$SD, 6, 20, 'Opt-in instrumentation of the fluid solver', <<'X'],
/**
 * Opt-in instrumentation of the fluid solver. {@link #ENABLED} defaults to {@code false}; every recording site then
 * performs one volatile boolean read, allocates nothing and reads no clock, so production numerics are unchanged
 * whether or not it is enabled. The counters are process-wide {@link LongAdder}s, so any number of workers may record
 * into them at once; the nesting markers ({@link #inJacobian()}, {@link #inReconstruct()}), which decide whether a
 * factorization, solve or ordering is charged to the Newton side or the transport side, are per thread. The timers are
 * per-thread {@code nanoTime} deltas summed into the same adders: under a pool they measure occupied thread time. A job
 * already running when the switch flips contributes only the part of itself that follows the flip, which is why the
 * server benchmark resets and enables at the start of its measurement window.
 */
X
[$HM, 14, 18, 'Fluid-specific property snapshot', <<'X'],
/**
 * Fluid-specific property snapshot. Every hydrocarbon phase is the translated PR78 evaluated directly at its own
 * pressure: a liquid at the state pressure, a vapour at its hydrocarbon partial pressure; a liquid's volume, enthalpy,
 * {@code ln phi} and compressibility are the equation of state's own, and each component's volume translation is
 * anchored at its reference point. Two paths, chosen by the package: a package without a reference spine takes the
 * coefficient path (the {@code shifted_polynomial_5} ideal-gas fits, the constant interaction matrix and the global
 * calibration anchors, pinned by {@code LegacyNetworkPathPinTest}); a package that declares a {@code spine} takes the
 * spine path ({@link SpinePackageData}: the spine records as the ideal gas with each species' formation enthalpy
 * subtracted, the network's sensible datum, the E-PPR78 pair interactions, the records' own {@code volume_translation}
 * anchors, and its {@link ThermoIdentity} as the thermodynamic revision), on which a phase's volume and {@code ln phi}
 * are bit for bit those of {@code CubicPhaseEvaluator.forPackage} and its enthalpy the evaluator's minus
 * {@code sum x_i DeltafH_i}.
 */
X
[$HM, 165, 19, 'One hydrocarbon phase at {@code p}', <<'X'],
    /**
     * One hydrocarbon phase at {@code p}: the state pressure for a liquid, the hydrocarbon partial pressure for a vapour.
     * The slot's root must exist, of the slot's kind, or the evaluation is refused with the typed {@link RootAbsent} (an
     * {@link IllegalArgumentException}: the line search backtracks, and for a liquid slot the outer check re-decides the
     * regime). A root is absent where floating point loses the cubic's physical root
     * ({@link PengRobinsonKernel.NoPhysicalRoot}: a trial far outside any physical state, the coefficients overflowing at
     * 1.5e292 Pa or the root's distance above the co-volume below one ulp of it at 6.6e25 Pa), and where at the state
     * pressure the cubic has one physical root that is vapour-like ({@link PhaseIdentification#vapourLike(double, double)}):
     * the liquid root is then absent if the phase's node also carries a hydrocarbon vapour ({@code vaporCoexists}), since
     * the two slots would evaluate the same kind of root; in a liquid-only node the single root is the same fluid
     * continuing and is accepted.
     */
X
[$IP, 11, 43, 'The inlet phase check of a pump or compressor', <<'X'],
/**
 * The inlet phase check of a pump or compressor (decisions D1, D2, D6): what the device's suction line delivers,
 * measured on a committed state, and whether the device is refused ({@link FlowControl.Mode#INLET_WRONG_PHASE}) for the
 * slice that starts there. Every method is a pure function of the graph, the slice duration and, for the decision, the
 * committed endpoint modes, so the solver and the device view ({@code FluidWorldAuthority}) read the same answer.
 *
 * <p>The supply walk: from the device edge's first node, a vessel, generator, void or port there supplies the stream of
 * that end; a junction is walked through (the device's own junction if it has exactly one other, passive, connection,
 * and every following junction with two connections neither of which carries an actuator), and the walk ends at the
 * first node that is not such a junction: a vessel, generator, void or port supplies the stream of the last link's end
 * there, a junction its state mixture. So the check reads the source, not the device's own junction: flashing at the
 * inlet does not refuse a device drawing a saturated liquid, it only lowers the suction density. What a phase port
 * supplies (decision A37) is the priority draw of decision D11 at the device's target volume flow over the slice: the
 * target volume {@code Q T} is filled from the port's priority order, each phase up to its share of the vessel
 * ({@code V_phase / N}), the last phase the vessel holds with the rest, so a bottom port holding less liquid than the
 * device would move in one slice reads the gas it would break through to; with no target volume it is the port's
 * leading phase. A BULK end supplies the node's bulk. Measures: for a pump the vapour share of the supply's fluid
 * volume (solids excluded); for a compressor the condensed share of its mass and the largest solid population's share
 * of its volume.</p>
 *
 * <p>A dry suction (owner decision D58, A1): a pump or compressor with nothing at its suction runs dry. What can feed
 * the suction is what stands upstream of it ({@link #held}): the device's first node if it is not a junction, else
 * every node reached from it through junctions by a connection that may carry flow towards the device
 * ({@link PassiveStepSolver#boundaryAllowed}). A generator always feeds; a void, a dry end and a junction never do; a
 * vessel feeds by the share of its capacity its contents fill. The suction is dry when the largest such share is below
 * {@link #DRY_REFUSE}, the share a drained port's throttle lands on, and a device refused on it runs again once that
 * share reaches {@link #DRY_RESUME}. A slice decides on its committed graph with the transport closures read as open
 * (owner decision D33): a closure is the solid events' to decide again, so it never leaves a suction dry by itself.</p>
 */
X
[$PC, 90, 9, 'The fluid network package', <<'X'],
    /**
     * The fluid network package's contract with the package's own spine: {@link #forNetworkPackage(MaterialCatalog,
     * String, String)} with the package's {@code spineFingerprint()}, or {@link ThermoIdentity#NO_SPINE} without one.
     * The identity is the engine's ({@link CubicPhaseEvaluator#FAMILY}); the package's crystals, if any, are not
     * qualified here (the network requests fluid-only competition until its crystal inventory exists; {@link #forPackage}
     * is the contract with the package's crystal set).
     */
X
[$PC, 104, 8, 'The package', <<'X'],
    /**
     * The package's contract for the equilibrium engine with its crystals: {@link #forNetworkPackage(MaterialCatalog, String)}
     * qualified in addition for the competition of the fluid phases with every crystal the package admits
     * ({@link #admittedCrystals}), as one whole set. A package that admits no crystal gets the fluid-only contract, so a
     * crystal request on it is refused as not qualified.
     */
X
[$PR, 9, 21, 'The physical registry', <<'X'],
/**
 * The physical registry's compiled form and its topology events, apart from the server: which island owns each
 * registered device, what the compiler said about each device, which devices sit in which chunk, and the path that
 * queues an edit as a ledger event and later applies it to the islands. {@link FluidWorldAuthority} owns one and
 * supplies the world through {@link Host}; a test supplies an {@link IslandCoordinator} of its own. An event costs time
 * in the part of the registry it touches, never in the size of the world: {@link #submit} walks from the edited
 * position through the devices no island owns and no queued event edits, and stops at the first device an island owns
 * (that island is fenced whole) or a queued event edits (that event orders this one); {@link #applyPending} applies the
 * queued events in batches (every queued event of one tick and one dimension whose owners are aligned), compiling only
 * the connected components of the devices it touched and of the islands it replaces. The compiled form is kept per
 * device, so replacing a component replaces exactly the entries of its devices; an island no event touched is never
 * read, recompiled, materialised or replaced.
 */
X
[$PR, 147, 9, 'Queues edits as one ledger event', <<'X'],
    /**
     * Queues edits as one ledger event and fences it on the islands it touches; {@link #applyPending()} applies it. The
     * touched set is the edited devices, every device joined to an edited position before or after the edit, and every
     * device reached from them through applied devices that no island owns and no queued event edits. An owned device
     * stands for its island, which the fence covers whole, and a queued device for the event that edits it; the walk
     * stops at both. A fixed boundary (a generator or a void) the event does not edit stops the walk and stands for none
     * of its islands (owner decision D17); one the event edits stands for all of them.
     */
X
[$PR, 402, 8, 'The islands an event', <<'X'],
    /**
     * The islands an event's touched device stands for: every island of a device the event edits, the island of any
     * other owned device, and none for a fixed boundary it only passes (islands that share a generator or a void are
     * independent of each other, owner decision D17). A lone boundary is the exception: an edit that touches it joins it,
     * and the batch that puts it into the joined island ends its island of one, so that island is fenced and replaced
     * with the rest (D32).
     */
X
[$TP, 14, 20, 'Constant volume-translated PR78 with analytic fixed-composition', <<'X'],
/**
 * Constant volume-translated PR78 with analytic fixed-composition volumetric and caloric derivatives. The equation of
 * state itself is the shared {@link PengRobinsonKernel} with {@link PengRobinsonKernel.Mixing#SPARSE_PAIRS} (the
 * network's package has 11 nonzero binary interactions among 21 components); what stays here is what the kernel does
 * not model: the ideal-gas caloric functions, the constant volume translation, and the volumetric block (dv/dT, dv/dP,
 * d2v/dT2, cp, dh/dP, the isothermal compressibility and the partial molar volumes), explicit algebra in
 * {@code (v, a, b, da/dT, d2a/dT2, S_i)}. Two ideal-gas sources, one per model: the coefficient path (every constructor
 * taking heat-capacity coefficients) evaluates the {@code shifted_polynomial_5} fits, past a fit's range holding the
 * heat capacity at the end value and integrating it; the spine variant
 * ({@link #TranslatedPengRobinson(List, PairInteractions, List, double[], double[])}) takes one {@link IdealGasFunction}
 * per component and a datum offset per component subtracted from the function's enthalpy (the network passes the
 * formation enthalpy, so its energies stay on the sensible datum, decision D10).
 */
X
[$TP, 178, 9, 'One exact temperature', <<'X'],
    /**
     * One exact temperature's prepared state, and the scratch every evaluation at it reuses: the kernel's 3n
     * pure-component vectors, the composition-dependent sums formed per evaluation in O(n + pairs). Mutable scratch, so
     * it belongs to one solving thread at a time; the caller that holds it (a node's {@link FluidThermodynamics.Prepared})
     * already has that confinement.
     */
X
[$CS, 19, 34, 'Where checkpoint format 10 lives', <<'X'],
/**
 * Where checkpoint format 10 lives, and what a save writes (the owner's decision F3): per-island storage with the mod's
 * own dirty tracking, no world-wide size bound, a small core record in the world's saved data.
 *
 * <p>Layout: the core record ({@link FluidCheckpointCodec#coreTag}) is the world's saved data file
 * {@code data/createcheme_fluid_core.dat}. Every island (and the world topology) is one storage unit; units live in
 * pack files {@code data/createcheme_fluid/fluid-<n>.pack}, and the core record's index says, per island, which pack,
 * offset, length and SHA-256 hold its unit. A save writes one new pack (more only past 256 MiB): the units whose island
 * changed since they were written (revision and payload generation), plus any live units it moves out of a pack that
 * has become mostly dead; unchanged units keep their place in their pack. Packs rather than one file per island
 * because a durable atomic file write costs about 2.5 ms whatever its size (2.4 s of the IO thread for 1,000 changed
 * islands at every autosave, 10,000 file opens at every load), while a pack costs one flushed write per save (6 ms for
 * 5 MB) and a bounded number of files.</p>
 *
 * <p>Commit protocol: the new pack is written first (temporary file, flushed, atomically renamed), then the core record
 * the same way; a crash before the core's rename leaves the previous core and every pack it references intact. Packs
 * are never modified after they are written; a pack is deleted only when neither the new core record nor the one
 * before it references it, and a load deletes every pack its core does not reference. Commits run on the IO thread in
 * submission order; a commit whose predecessor failed is not written either, and the next save writes every unit
 * afresh. Load: every pack the core lists must be present with exactly the listed size and header, every unit's
 * SHA-256 must match the index and its header the island, revision and generation the index gives; anything else
 * refuses the world with the fresh-world instruction, its files left as they are. The loaded index seeds the dirty
 * tracking.</p>
 */
X
[$CP, 15, 32, 'of a crystal record', <<'X'],
/**
 * The {@link SolidPhaseEvaluator} of a crystal record: the record's model ({@link CrystalReference}, for CO2 the Gibbs
 * function of Jaeger and Span 2012) anchored at the triple point to the fluid family's pure liquid.
 * {@link #anchor(CubicPhaseEvaluator)} evaluates the family's pure liquid of the species at the record's triple point
 * (T_t, P_t) and returns its chemical potential mu_L and enthalpy h_L; the crystal then satisfies g_s(T_t, P_t) = mu_L
 * and s_s(T_t, P_t) = (h_L - mu_L)/T_t - dH_fus/T_t (Jaeger and Span's equations 19 and 20), which for a Gibbs-function
 * record fix its integration constants g0 and g1. The evaluator fills a one-component {@link PhaseKind#SOLID} state
 * naming the record's id: v, h and s of the anchored crystal, split against the species' ideal gas (the package's
 * spine) as {@code h = h_ig(T) + (h - h_ig)} and {@code s = s_ig(T, P) + (s - s_ig)}, and
 * {@code ln phi = (g_s - g_ig(T, P)) / (R T)}, so that {@link PhaseState#chemicalPotential(int)} equals g_s and compares
 * directly with a fluid phase's. Past the record's range and the ideal-gas function's, both continue; an unusable
 * record is refused at construction. {@link #anchored(TriplePointAnchor)} solves the integration constants once and
 * returns an immutable {@link Anchored} crystal, which an equilibrium service holds per crystal so a flash never
 * re-solves g0 and g1; {@link #evaluate(double, double, TriplePointAnchor, PhaseState)} reuses the last anchored view
 * while the caller passes an equal anchor. Derivatives ({@link #derivatives}): {@link Capability#FUGACITY_STATE_DERIVATIVES}
 * ({@code d ln phi/dT = -(h_s - h_ig)/(R T^2)}, {@code d ln phi/dP = v_s/(R T) - 1/P}),
 * {@link Capability#VOLUMETRIC_DERIVATIVES} ({@code dv/dT = alpha v}, {@code dv/dP = -kappa v}) and
 * {@link Capability#CALORIC_DERIVATIVES} ({@code c_p}, {@code dh/dP = v (1 - alpha T)}); no composition derivatives.
 */
X
);
