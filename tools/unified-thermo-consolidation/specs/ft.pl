# Condensation spec for science/fluid/thermo/FluidThermodynamics.java at b0ef7e44 (line numbers of that state).
(
[32, 9, 'ceiling on the trace cutoff', <<'X'],
    /**
     * The network's ceiling on the trace cutoff, a thousandth of the column's: the network has no reference to answer to
     * inside a Newton pass, and the energy it can misplace by folding an omitted phase into the retained one is bounded
     * by the cutoff times the phase amount times the largest enthalpy of vaporization on the basis, which at 1e-5 is
     * still inside the interval controller's own energy gate.
     */
X
[66, 11, 'The canonical gameplay basis, which includes nitrogen', <<'X'],
    /**
     * The canonical gameplay basis, which includes nitrogen. {@code packageId} is resolved through
     * {@link FluidMaterialCatalog#resolveNetworkPackage}, which accepts only the registered network package, and a
     * package without nitrogen is refused rather than run on a shorter basis. No overload takes a single number after
     * the package: the velocity limit comes with the trace cutoff.
     */
X
[151, 6, 'The model', <<'X'],
    /**
     * The model's full revision: the thermodynamic one with the hydraulic acceptance law (the velocity clamp, the trace
     * cutoff, the solid settings). A certificate holds only under the revision it was issued with, and the world's
     * property guard resumes a held dimension only on its own.
     */
X
[199, 9, 'The crystal answer of a vessel', <<'X'],
    /**
     * The crystal answer of a vessel's refresh (owner decision D23): the engine's answer for the vessel's fluid and
     * crystals together, the package's crystals competing where they compete and the free water's ice by water's own
     * rule. {@code fluid} is its fluid phases as a network state without solids (null where the crystals hold
     * everything); {@code crystalMoles} the moles of each of the package's crystals ({@link NetworkCrystals#all} order);
     * {@code volume} the fluid's and the crystals' volume, the crystals in the engine's own model.
     */
X
[214, 11, 'The crystal test of a taut vessel', <<'X'],
    /**
     * The crystal test of a taut vessel (D23, with D21's near-pure step): the equilibrium of {@code totals} (the conserved
     * basis, water last) at internal energy {@code energy} (J, the network's datum, inert solids excluded) and volume
     * {@code volume} (m3, the bag less the inert solids), the package's crystals competing where {@code competing}
     * ({@link #crystalsCompete}), the free water freezing by its own rule, warm-started at {@code (hintT, hintP)} when
     * both are finite. Refusals are typed where the engine types them ({@link UnsupportedPhases} of kind
     * {@code CRYSTAL_PHASES} or {@code CRYSTAL_STEP}, {@link CriticalBandHold}); any other failure is a plain
     * {@link IllegalArgumentException}.
     */
X
[294, 8, 'The data ranges the state at network node', <<'X'],
    /**
     * The data ranges the state at network node {@code node} lies outside, into {@code notices} (every model continues
     * past its data and the island's status line reports it): the package envelope; each carried component's ideal-gas
     * data and the viscosity data of each phase it is in; free liquid water's Region 1 range (its viscosity's too) and the
     * pressure its metastable liquid has data to; the steam's range and qualified partial pressure; each crystal's
     * record. Reads the state and the declared ranges only, evaluating no model but the water's saturation pressure.
     */
X
[421, 27, 'The fluid a phase-selective vessel outlet draws', <<'X'],
    /**
     * The fluid a phase-selective vessel outlet draws, read off the vessel's own state with no flash and no extra
     * equation-of-state root: every term is a sum over the phase amounts, volumes and molar properties {@link #state}
     * already computed. The vapour stream is the hydrocarbon vapour and the water vapour ({@code moles} are {@code vapor}
     * with the water vapour last, the gas volume, enthalpy {@code n_v h_v + n_wv h_wv(T)}, no solid). The condensed phase
     * is the hydrocarbon liquid, the free water and every solid; a port draws its two liquids one after the other
     * (decision D11), so each liquid is its own stream ({@link #phaseStream}) with its volume share of the solids and of
     * {@code H_solids(T,P)}; {@link #liquidMass}, {@link #liquidStreamVolume} and {@link #liquidDensity} state the whole
     * condensed phase. The streams partition the state. The vapour's velocity limit is {@link #isothermalAcousticBound}'s
     * formula restricted to the vapour ({@code sqrt(stiffness/rho_v)}, capped by the configured maximum), not the far
     * lower bound of the two-phase mixture; a liquid stream's limit is the configured maximum. A stream whose phase the
     * state does not hold is not built ({@code null}): a port then draws the next phase of its priority order.
     */
X
[490, 8, 'The product of a phase', <<'X'],
    /**
     * {@code V / sqrt(M C)}. The product of a phase's mass and compliance goes subnormal for a trace phase (a vanishing
     * nitrogen vapour below about 2e-150 mol) and is zero below about 2e-158 mol, the bound then infinite; such a product
     * is evaluated as {@code sqrt(V/M) sqrt(V/C)}, ratios of like-sized quantities, and every product at or above
     * {@link Double#MIN_NORMAL} takes the original expression, bit for bit.
     */
X
[516, 10, 'three phases a vessel port can draw one after another', <<'X'],
    /*
     * The three phases a vessel port can draw one after another (decision D11: ports carry mixed phases by priority). GAS
     * is the vapour stream above. The condensed phase splits into the hydrocarbon liquid (OIL) and the free water (WATER),
     * each carrying its volume share of every solid population (the solids are a uniform suspension in the combined
     * liquid, the carrier the solid mobility check reads), so the OIL and WATER streams partition the condensed phase, and
     * with one liquid present its stream carries every solid. All read the state's own amounts and molar properties.
     */
X
[536, 11, 'Owner decision D33 (2026-10-16): solids ride the liquid', <<'X'],
    /**
     * Owner decision D33: solids ride the liquid. A connection's BULK end draws a uniform share of what its donor holds,
     * the solids carried by the liquid at {@code solids * liquid drawn / liquid held}; where no liquid is held nothing
     * carries them (frost on a tank's walls, a dry bed), so the end draws the donor's gas exactly where that differs from
     * its bulk: a gas beside solids with no liquid (a stand-in liquid, D29 A, is a liquid here). The solids are read as
     * the populations the state carries, which a step's iterates keep from their pass's seed, so the answer is fixed for
     * a pass. The one reading of a bulk end: the stage guard's outlet, the static end reads ({@code PhaseDraw.leading}),
     * the step's donor terms and the reconstruction's booking.
     */
X
[732, 7, 'a fluid phase holding less than this share', <<'X'],
    /**
     * Owner decision D34: a fluid phase holding less than this share of its node's fluid amount is a trace phase and does
     * not exist; its material is in the node's other phase (A83). The share is taken in moles over every fluid phase and
     * component of the node, solids not counted: the unit the Newton states a node's balances in, so a phase below it is
     * below what the solve resolves, the same number for a condensate film in a gas and for the last bubble in a liquid.
     */
X
[740, 14, 'The share under which a phase that is shrinking is vanishing', <<'X'],
    /**
     * The share under which a phase that is shrinking is vanishing ({@link #vanishing}), in any node: a phase holding
     * less than this share of its node's fluid amount, and a smaller share than where the solve that judges it started,
     * leaves, at a converged point as at a failed iterate (owner decision D38 C). It is what a solve that stops short
     * resolves: a closure's 24 iterations end at a residual near 1e-6 of the node's amount, and a liquid-full tank beside
     * dry ice stalled with its vapour at 1e-9 to 3e-9 of the moles and still falling, which the trace share alone keeps or
     * drops at rounding. 1e-6 of a junction's holdup is about a milligram of LNG. A phase that does not shrink keeps the
     * trace share ({@link #PHASE_TRACE_FRACTION}) alone; a proposal the next pass drives under this share is refused
     * (owner decision D39 B).
     */
X
[755, 18, 'Folds the trace phases of one split in place', <<'X'],
    /**
     * Folds the trace phases of one split in place (owner decisions D33, D34): nothing leaves the node and nothing is
     * booked, so every component amount and the node's mass are unchanged. The rule (A84): a trace hydrocarbon liquid
     * goes into the gas phase when that holds hydrocarbons; a trace of free water goes into the gas as water vapour; a
     * trace gas goes into the liquids, its hydrocarbons into the hydrocarbon liquid and its water vapour into the free
     * water, when each part has its liquid; a trace gas whose gas components have no hydrocarbon liquid, in a node with
     * free water, is a carried gas (owner decision D41, {@link #carriedGas}): its water vapour goes into the free water and
     * its gas components stay a dry gas at the node's pressure; a trace phase none of whose receiving phases exists stays,
     * and a receiving phase that is itself a trace receives nothing. {@code water} is {free water, water vapour}. Returns
     * whether anything moved. {@link #folded} and every flash apply the rule to a state.
     */
X
[774, 10, 'with the phases {@code vanishing} names', <<'X'],
    /**
     * {@link #foldTracePhases(double[],double[],double[])} with the phases {@code vanishing} names ({@link #phaseBits};
     * owner decision D38 C) folded as traces by the same rule: a hydrocarbon liquid (1) or free water (4) it names, and a
     * gas every part of which it names (2 for its hydrocarbons, 8 for its water vapour). A phase it names receives
     * nothing either. A vanishing gas whose hydrocarbons have no hydrocarbon liquid to go into stays whole: the carried
     * gas's split is a trace gas's only (such a gas, nitrogen over water, leaves through the node's ports, and its humidity
     * folded alone is given back by the next TP answer, a pass cycle).
     */
X
[800, 18, 'a gas bubble in a liquid-full pipe is carried by the liquid', <<'X'],
    /**
     * Owner decision D41: a gas bubble in a liquid-full pipe is carried by the liquid, at the liquid's velocity, to the
     * next gas space, where it joins the gas there; in the pipe it sits at the liquid's temperature and pressure with its
     * own volume and is in no equilibrium with the water around it. Such a carried gas is a dry gas at the node's
     * pressure (its hydrocarbon partial pressure is the pressure) whose water is in the free water, so the node states no
     * saturation row and no partial-pressure row for it; it leaves with the liquid in proportion, and the first node with
     * a gas space gives it its vapour again. A node holds a carried gas when its gas is a trace ({@link #foldTracePhases}),
     * or when it is a pipe junction ({@code junction}) whose only liquid is free water and whose free water fills more of
     * it than its gas (a vessel separates its gas upwards by gravity; a junction is a piece of pipe the liquid runs
     * through). Returns whether {@code state} holds a gas of the second kind that is not yet dry.
     */
X
[863, 10, 'The check of a failed pass', <<'X'],
    /**
     * The check of a failed pass's last iterate (owner decisions D28 and D39): {@link #folded(State,boolean)} with the
     * phases vanishing since {@code from} folded too (D38 C, {@link #vanishing}), and nothing else. A failed iterate is not
     * a state, so its check only folds what the iterate empties and proposes nothing: no TP answer and no own split of a
     * non-state (a failed iterate on the box's 1 Pa floor seeded a junction at 51 K with 13 300 m3 of gas, and a third
     * fluid phase was raised at 17 K about a state the network never reached). A phase the pass needs appears at a
     * converged point's check ({@link #phaseCheck}) or at a smaller step.
     */
X
[895, 64, 'The outer phase check of one node of the fluid network', <<'X'],
    /**
     * The outer phase check of one node of the fluid network (owner decisions D25 and D26), never inside a residual. A
     * node at fixed amounts, energy and volume has one equilibrium, which its own rows find once its layout holds every
     * phase that equilibrium needs; a TP answer at the node's temperature and pressure tells which phases are missing but
     * not how much of each the node holds. So the check never swaps one populated phase set for another on a TP verdict,
     * and an added phase starts at the node's own split:
     * <ul>
     * <li>a phase leaves only when the solve empties it: the trace fold ({@link #folded}, D34), and a phase held below
     * {@link #VANISHING_SHARE} and at a smaller share than at {@code from} (owner decision D38 C, {@link #vanishing}), in a
     * junction as in a vessel, also at a vessel closure's unsuccessful last iterate (D28 A'). A phase the fold removed as
     * vanishing is not proposed again by this check;</li>
     * <li>a node holding every phase its material can form has none to add, and no TP answer is formed. A stand-in liquid
     * ({@link #condensedShareOnly}, owner decision D31 B) is no liquid of the node's own and never makes it
     * liquid-complete; in a TP answer it is a phase like any other, decided by the node's own split ({@link #lacking});</li>
     * <li>where the TP answer ({@link #flashTP}) holds a phase the node lacks that is not in the solve's record
     * ({@code record}, A85), the node's own split decides it (D26): a pipe junction's PH answer at its pressure and
     * enthalpy ({@link #equilibriumPH}), a vessel's {@code vesselSplit}, the network's closure of its inventory (UV while
     * its wall is taut, PH at the ambient while slack), started from the vessel's own state at the crossing (owner
     * decision D40 A, {@link #crossing}). The TP answer says which phase is missing, never how much a vessel holds: its
     * amounts ignore the vessel's volume. A vessel with no crossing, or whose closure from it fails, proposes nothing.
     * Where a junction's PH answer fails, the TP answer with the junction's populated phases ({@link #tpSeed}) seeds
     * instead;</li>
     * <li>the own split decides by where its closure went, converged or not (the D26 amendment): a proposed phase it drove
     * under the trace share, that it is driving to nothing ({@link #vanishing}) or that it leaves vanishing against
     * {@code from} is refused, and the refusal is written to the record so the TP answer does not propose it again in
     * this solve (a boiling liquid in a vessel with no room for a bubble pressurises and stays liquid). A proposed phase
     * that stands seeds the node, and the seed is taken only when it then holds a phase the node lacks that is not in the
     * record and none that was refused.</li>
     * </ul>
     * The rule decides which phases a node is laid out with; how much of each it holds is its rows' answer: a rigid
     * vessel cooled past its bubble point gains the small vapour of its own energy and volume, not the TP split's several
     * vessel volumes.
     *
     * <p>{@code record}, {@code at}: the solve's A85 record of the node's phases ({@link #phaseBits}), read as the phases
     * the solve removed or refused and written with those this check removes and refuses ({@code null}: none); the
     * network solver also writes there a phase this check added that the next pass refutes (owner decision D39 B).
     * {@code junction}: whether the node is a pipe junction; {@code from}: the node's state where the solve that reached
     * {@code state} started, which the vanishing rule reads, or {@code null}; {@code prepared}: the node's prepared
     * temperature state, used only at exactly that temperature; {@code vesselSplit}: a vessel's own split from a start
     * holding the phases to lay it out with, {@code null} where its closure fails (ignored for a junction).
     *
     * @return {@code state} itself when it keeps its phases and gains none; otherwise the node's new seed
     */
X
[1055, 21, 'A vessel', <<'X'],
    /**
     * A vessel's own state at a crossing (owner decision D40 A), the start of its own split in {@link #phaseCheck}: the
     * engine's equilibrium of the fluid it holds under its wall's law, the problem its own closure solves
     * ({@code InventoryEquilibrium}), as a network state ({@link #adopt}) folded as a vessel: a slack bag at its fluid
     * enthalpy and the ambient pressure ({@link #equilibriumPH}), a taut vessel at its fluid's internal energy and volume
     * ({@link #equilibriumUV}), warm-started at its temperature and pressure, both under the competition of the vessel's
     * TP request ({@link #crystalsCompete}); a crystal the engine forms is carried as its stand-in (D23). The fluid alone
     * is no answer there: LNG with 0.7 % of CO2 cooled past its dew point in a bag of nitrogen is three fluid phases with
     * a CO2-rich liquid, which the network does not carry. The TP answer says only which phase is missing: its amounts
     * ignore the vessel's volume and are never the seed (a liquid-full slush tank drawn below its bubble point was offered
     * 13 to 29 m3 of vapour for 0.78 m3 of fluid space, and its closure from there failed). {@code null} where the engine
     * does not converge, answers a phase set the network cannot carry, or answers none of the phases {@code proposed}
     * adds (no crossing), so the vessel proposes nothing.
     */
X
[1102, 43, 'The equilibrium split of {@link #flashTP} before its trace phases are folded', <<'X'],
    /**
     * The equilibrium split of {@link #flashTP} before its trace phases are folded; not a state the network lays out or
     * commits ({@code flashTP} is {@code folded(equilibriumTP(...))}). The step solver's phase correction reads both to
     * know which phase the trace rule, rather than the equilibrium, removed (D34, A85). The phases of {@code overall}
     * (the conserved basis, water last) at {@code (t, p)} are decided by the equilibrium engine
     * ({@link NetworkPhaseEngine}: stability test, split with its second-order finish, critical band, phase
     * identification, the separate free-water rule); this adapter states that answer as a network {@link State} with the
     * network's own direct phase evaluations ({@link #state}). Slots: a split's lighter phase is the vapour slot and its
     * denser phase the liquid slot; a one-root phase takes the slot its label or its own phase identification parameter
     * gives; with water the engine's free-water record gives the water split and the hydrocarbon vapour's partial
     * pressure; a pure component exactly at its coexistence pressure is a single liquid here. Refusals: a state outside
     * the numerical box is a plain {@link IllegalArgumentException} (no data range bounds a state); an answer the engine
     * could not converge inside the critical band is the typed {@link CriticalBandHold}; a liquid-liquid split and a
     * split with a product proved unstable are {@link UnsupportedPhases} (a node carries one hydrocarbon liquid and one
     * vapour); any other failure is a plain {@link IllegalArgumentException}. Below its melting line the engine states
     * the free water's condensed part as ice; a TP answer never moves material into a crystal (owner decision D23), so the
     * network carries it as free liquid water, the stand-in ({@link #state}), until a vessel's refresh freezes it; on a
     * package that admits crystals a request carrying a crystal's species at or below the crystals' ceiling is made under
     * the crystal competition ({@link #crystalsCompete}), and where a crystal forms the answer's vapour is the vapour and
     * the liquid slot takes the rest with the crystal's share condensed in it ({@link #adopt}). A crystal beside a phase
     * set the engine does not implement is the typed {@link UnsupportedPhases.Kind#CRYSTAL_PHASES} hold.
     * {@code checkpoint} runs once before the engine and once per equation-of-state evaluation the engine reports.
     */
X
[1164, 11, 'A pipe junction', <<'X'],
    /**
     * A pipe junction's own split (owner decision D26, {@link #phaseCheck}), the phases of an empty bag's
     * ({@code PassiveStepSolver.emptyBagSplit}, D54) and a slack vessel's crossing ({@link #crossing}): the engine's PH
     * equilibrium of {@code overall} (the conserved basis, water last) at the node's pressure {@code p} and its fluid
     * enthalpy {@code h} (J, the network's datum), under the competition of the node's TP request at {@code t}
     * ({@link #crystalsCompete}), as a network state ({@link #adopt}, unfolded); {@code null} where the engine does not
     * converge or answers a phase set the network cannot carry. A spine package's engine states energies on the spine's
     * formation datum, the network on its sensible one (D10), so the target moves by the formation enthalpies.
     */
X
[1221, 10, 'whether {@code state}', <<'X'],
    /**
     * Owner decision D29 A: whether {@code state}'s hydrocarbon liquid is the stand-in's condensed share alone
     * ({@link #adopt}): a liquid holding crystal species only, beside a vapour, as a frost answer's crystal is carried. It
     * is no phase of the answer, so the trace support reads the answer's own fluid phase, the vapour, and the stand-in
     * admits every component the node holds ({@code PhaseLayout.support}); in the outer phase check it is no liquid of
     * the node's own (owner decision D31 B) and never makes the node liquid-complete. Never on a package without crystals.
     */
X
[1272, 8, 'A crystal in the answer', <<'X'],
            // A crystal in the answer. A TP phase decision never moves material into a crystal (owner decision D23: only a
            // vessel's refresh does), so the mobile phases carry the crystal's share until then as condensed matter: the
            // vapour is the answer's vapour and the liquid slot takes the rest, a supersaturated liquid, per component by
            // difference and never negative, so the amounts are conserved exactly. In the vapour slot the crystal's mass
            // would take a gas's volume and energy (a CO2 let-down below the triple point, stated all vapour, ran to CO2's
            // 90 K floor).
X
);
