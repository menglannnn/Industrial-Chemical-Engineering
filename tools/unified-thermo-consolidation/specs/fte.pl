# Condensation spec for science/thermo/phase/FluidTpEquilibrium.java at b0ef7e44 (line numbers of that state).
(
[24, 170, 'TP equilibrium of fluid phases on a', <<'X'],
/**
 * TP equilibrium of fluid phases on a {@link CubicPhaseEvaluator}: the hydrocarbon phases by stability test and flash,
 * water as the fluid network's separate free water, pure crystals competing with the fluid phases; PH and UV search over
 * the TP answers.
 *
 * <p>Hydrocarbon phases: contract checks ({@link #contractChecks}; a request asking for crystals on a service without
 * their evaluators is {@link UnsupportedReason#NOT_IMPLEMENTED}); tangent-plane stability of the feed
 * ({@link TangentPlaneStability} on the evaluator's kernel, where the constant translation cancels at one pressure).
 * Stable: one phase on the feed's lower-Gibbs root, classified from the root situation; a pure component with three
 * roots whose fugacities agree within {@link Settings#coexistenceTolerance()} is
 * {@link Classification#PURE_COEXISTENCE_UNDERDETERMINED}; a one-root phase is labelled by its phase identification
 * parameter ({@link PhaseIdentification}), except a pure component above its critical point
 * ({@link Classification#SUPERCRITICAL_FLUID}, {@link PhaseKind#FLUID}). Unstable: Rachford-Rice with successive
 * substitution on {@code ln K_i = ln phi_i^L - ln phi_i^V} from the stability test's stationary composition, each phase
 * on its own lower-Gibbs root, a dominant-eigenvalue extrapolation every {@link Settings#accelerationCycle()} steps,
 * solved in the minor phase's fraction so a trace phase keeps its precision; converged when a step moves no
 * {@code ln K_i} by {@link Settings#lnKTolerance()}. When substitution is slow ({@link #SLOW_SUBSTITUTION_STEPS} steps
 * with the eigenvalue above {@link #SLOW_EIGENVALUE}), past {@link Settings#substitutionLimit()}, collapsing onto the
 * feed or losing the split, a Newton minimises the Gibbs energy over the vapour mole numbers ({@code l_i = z_i - v_i}):
 * gradient {@code ln f_i^V - ln f_i^L}, Hessian {@code sum over both phases (delta_ij/n_i - 1/N + Phi_ij/N)} with the
 * kernel's {@code Phi_ij = d ln phi_i/d n_j}, scaled by {@code sqrt(v_i l_i / z_i)} and shifted by {@code mu I} until
 * Cholesky succeeds, the step cut so no amount shrinks below a tenth of itself, an Armijo line search on
 * {@code Delta G/RT} from the feed, which stays negative so the finish never reaches the trivial solution; converged at
 * {@code max |g_i| < }{@link #FUGACITY_TOLERANCE}. Amounts are split per component, the smaller share directly and the
 * larger by difference, so each component conserves to rounding. A converged split with
 * {@code sum z_i (ln K_i)^2 <} {@link Settings#mergeTieLine()} whose molar volumes differ by less than
 * {@link Settings#mergeVolumeGap()} is one fluid ({@link Classification#SINGLE_FLUID}, inside the critical band);
 * otherwise both products are re-tested, and an unstable product is {@link EquilibriumResult.Status#NOT_CONVERGED} with
 * the detail {@link EquilibriumResult#UNSTABLE_PRODUCT} or {@link EquilibriumResult#UNRESOLVED_PRODUCT}. The denser
 * phase is {@link PhaseKind#LIQUID} and the lighter {@link PhaseKind#VAPOR}; two liquid-like products denser than the
 * cubic's critical volume ({@link PhaseIdentification#denseLiquidLike}, {@code v < 3.9514 b}) outside the critical band
 * are {@link Classification#LIQUID_LIQUID}, the lighter first. An unresolved feed stability test is repeated once with
 * {@link #UNRESOLVED_STABILITY_BUDGET_FACTOR} times the per-trial budget.</p>
 *
 * <p>Declared critical band: a pure fluid (one component at least 0.95 of the amount) in the reduced boxes of
 * {@link #pureBandBox}; a split with {@code sum z_i (ln K_i)^2 <} {@link #BAND_TIE_LINE}; a single mixture phase whose
 * stability test met a stationary point other than the feed within {@link #BAND_STATIONARY_DISTANCE}
 * ({@link TangentPlaneStability.Result#nearestStationaryDistance()}); a failed split whose last iterate or stability
 * seed was that short. Inside it a converged answer is graded {@link EquilibriumResult.CoverageGrade#RESEARCH_ONLY} and
 * a failure is typed {@link EquilibriumResult#CRITICAL_BAND}. No split is fabricated and no single phase is reported
 * stable when the test said unstable.</p>
 *
 * <p>Free water ({@link WaterParticipation#SEPARATE_FREE_WATER} with a {@link FreeWaterModel}), the fluid network's
 * rule: the hydrocarbon liquid at the state pressure {@code P}, the hydrocarbon vapour at its partial pressure
 * {@code pc = P - p_w}, ideal steam sharing the gas volume, liquid water from the water model at {@code P}. Without
 * hydrocarbons the water is all liquid when {@code P >= p_sat} and all steam otherwise. With both, when
 * {@code P > p_sat + 1e-6 Pa} the hydrocarbons are split at {@code (P, P - p_sat)} and the steam that saturates the gas,
 * {@code p_sat V_gas/(R T)}, is taken from the water if there is that much; otherwise all water is steam and {@code pc}
 * solves {@code pc + n_w R T / V_gas(pc) = P}: up to {@link #PARTIAL_PRESSURE_FIXED_POINT_STEPS} fixed-point steps, each
 * accepted only on the bisection's stop test, else a bisection on {@code [1e-6 Pa, P]}
 * ({@link #PARTIAL_PRESSURE_BISECTIONS} halvings, stop at {@link #PARTIAL_PRESSURE_TOLERANCE}, accepted within
 * {@link #PARTIAL_PRESSURE_ACCEPTANCE}). Steam above the model's qualified partial pressure is answered and graded
 * {@link EquilibriumResult.CoverageGrade#EXTRAPOLATED}. The split with branch pressures uses the stability test's
 * per-branch form and the vapour branch's shift {@code ln(pc/P) + (pc - P) c_i/(R T)} in the flash (the translation does
 * not cancel between two pressures). Ice ({@link FreeWaterIce}): below the melting line {@code T_m(P)} the condensed
 * water is ice, a solid phase naming the water model's ice record, and the rule runs with the ice's fugacity in place of
 * {@code p_sat}; ice is water's own rule, never a crystal of a competition, and forms beside a competing crystal too.</p>
 *
 * <p>PH and UV are outer searches over TP answers inside the numerical box ({@link NumericalBox}; a specification
 * beyond it is {@code NOT_CONVERGED}, a numerical failure). PH: a bracketed search in {@code T} at fixed pressure
 * ({@code H(T)} increases for stable equilibria) from the ideal-gas temperature of the enthalpy; Newton with the phase's
 * {@code c_p} for one dry phase, a secant between answers with the same phases otherwise, bisection when two steps fail
 * to halve the bracket's smaller residual. UV: Newton on {@code (ln T, ln P)} for
 * {@code ((U - U_spec)/(N R T), ln(V/V_spec))} (the analytic Jacobian of one dry phase, else differences and Broyden's
 * update, a halving line search), started where the feed as one phase of the cubic has the specified energy at the
 * specified volume; when Newton stalls, a nested search in {@code T} at the specified volume, each temperature solving
 * {@code V(P) = V_spec} in {@code ln P}; a bracket that closes at the box's pressure bound with the energy still short is
 * {@code NOT_CONVERGED} naming that bound. Steps the TP answer leaves undetermined (a pure hydrocarbon at its
 * equal-fugacity point, free water boiling or subliming at the state pressure, free water freezing at its melting line)
 * are determined from the balance once a bracket straddles the jump, {@code target = (1 - beta) x_1 + beta x_2}, at the
 * kernel's equal-fugacity temperature or pressure, the water model's saturation or the melting line
 * ({@link #freezingAtVolume} at a fixed volume and energy; pure water inside its triple point's triangle at that point,
 * {@link #waterTriplePoint}): {@link Classification#VAPOR_LIQUID} or {@link Classification#FREE_WATER} with
 * {@link Diagnostics#coexistenceFromBalance()}. A target inside any other step is {@code NOT_CONVERGED}. Every search is
 * bounded ({@link Settings#maximumOuterIterations()}); near a critical point the searches take secants and brackets.</p>
 *
 * <p>Crystals: a service built with crystal evaluators ({@link #forPackage}) answers the competitions its contract
 * qualifies ({@link PhaseCompetition#FLUID_AND_CRYSTALS} resolves to the package's set). Each crystal is anchored once,
 * at construction, to this evaluator's pure liquid at its triple point ({@link CrystalPhaseEvaluator.Anchored}). A
 * crystal's record continues past its data range; the lowest record ceiling of a competition ({@link #crystalCeiling},
 * 300 K for CO2-I) is where its crystals cease to exist. With free water the crystals compete against the free-water
 * rule's hydrocarbon phases at the hydrocarbon partial pressure ({@link #solveWetWithCrystals}; ice and CO2 frost
 * coexist). TP: the fluid answer of the feed, then each crystal against it by fugacity, {@code D = (mu_i - mu_s)/(R T)};
 * above {@link #DEPOSITION_TOLERANCE} the solid amount is solved so that the fluid the crystal leaves, re-flashed with
 * the stability test, has the crystal's chemical potential to {@code 1e-9}: the vapour-solid Newton on the reduced
 * feed's vapour root first ({@link #depositFromVapour}), else the general solve whose every iterate is a re-flash
 * ({@link #depositFromFluid}; {@link Classification#VAPOR_SOLID}, {@link Classification#LIQUID_SOLID} or
 * {@link Classification#VAPOR_LIQUID_SOLID}). A pure feed freezes or deposits completely
 * ({@link Classification#SOLID_PRESENT}), as does a species below {@link #DEPOSITION_TRACE} in the fluid. Two liquids
 * beside the crystal, three fluid phases and two crystals are the typed {@link EquilibriumResult#CRYSTAL_PHASES} hold.
 * PH and UV answer the pure species' sublimation and melting steps, its triple point (UV) and a binary's three-phase
 * step from the balances; a start in a held region restarts from the competition's ceiling. Onsets
 * ({@link #depositionTemperature}, {@link #freezingTemperature}, {@link #depositionPressure}) are bracketed searches on
 * {@code D} of the fluid answer of the whole feed.</p>
 *
 * <p>Deterministic (fixed arithmetic order; a reused {@link Workspace} carries nothing between calls but prepared
 * temperatures, recomputed bit-identically) and allocation-light: the iterations allocate nothing.</p>
 */
X
[224, 7, 'tested once more with this many times the per-trial budget', <<'X'],
    /**
     * A feed whose stability test ends unresolved (a trial out of its budget without proving instability) is tested once
     * more with this many times the per-trial budget: next to a mixture critical point a trial can crawl towards the
     * trivial solution on a root whose Hessian is indefinite, where only substitution moves it.
     */
X
[247, 7, 'TP equilibria of a warm-started UV Newton phase', <<'X'],
    /**
     * TP equilibria of a warm-started UV Newton phase before the cold search takes over. 16, not 8: a vessel's crystal
     * beside a vapour and a liquid of three species moves between substeps by more than two Newton iterations can close
     * (warm answers 180 of 211 per interval at 16 against 2 of 211 at 8 on the pilot world's liquid methane flush).
     */
X
[296, 12, 'a dry feed whose species other than its largest total at most this fraction', <<'X'],
    /**
     * D21: a dry feed whose species other than its largest total at most this fraction of it is near-pure. When that
     * major species has a competing crystal, PH and UV answer its crystal steps (sublimation, melting, triple point) as
     * a pure feed's, the other species carried as traces in the fluid phase the step leaves ({@link #STEP_FLUID_TRACES});
     * the pure species' pressure or temperature is then about 1.3 times the trace fraction off (0.1 mK at 101 kPa at
     * this bound). TP cannot resolve the feed's own vapour-solid region, about the trace fraction wide in pressure, up
     * to about 1e-6 and not reliably just above (a PH target at 2e-6 closed on a 0.57 J step of H(T) over 6e-10 K).
     * Takes precedence over the pseudo-binary rule of {@link #TRACE_FRACTION}.
     */
X
[309, 11, 'the largest share of a near-pure step', <<'X'],
    /**
     * D21: the largest share of a near-pure step's fluid its traces may take. The step at the pure species' point leaves
     * out the traces' dilution of that fluid, which moves the real step by about this share in ln P; a step needing a
     * smaller fluid (deep deposition) is the general solve's, whose vapour-solid region is then at least this share
     * wide in drive. At 10 times {@link #NEAR_PURE_TRACES} every near-pure feed keeps at least 90 % of its step on the
     * rule, and the handover moves the answer by at most 1e-4 in ln P; unbounded, the rule and the general solve
     * alternate along {@code H(T)} where the traces make most of the fluid.
     */
X
[998, 9, 'fluid phases of a crystal competition', <<'X'],
    /**
     * The fluid phases of a crystal competition's material with its crystals held out: the fluid answer of the whole feed
     * at the request's (T, P), no crystal taking part, labelled {@link PhaseCompetition#FLUID_ONLY} ("solid phases not
     * assessed") with the held-out competition in its detail. For a consumer whose crystal inventory is frozen over a
     * step and equilibrated between steps (the fluid network's operator split): the regime of its mobile fluid inside
     * the step. A request that is not a crystal competition is refused ({@code IllegalArgumentException}).
     */
X
[1032, 7, 'UV answer of a crystal competition', <<'X'],
    /**
     * The UV answer of a crystal competition's material with its crystals held out: {@link #uv} over the fluid answers of
     * {@link #tpCrystalsHeldOut}, labelled {@link PhaseCompetition#FLUID_ONLY} with the held-out competition in its
     * detail (the split of a vessel's own fluid at its own energy and volume). A request that is not a crystal
     * competition is refused ({@code IllegalArgumentException}).
     */
X
[1059, 9, 'the temperature at the request', <<'X'],
    /**
     * PH: the temperature at the request's pressure whose TP equilibrium has the request's enthalpy (J, over all phases),
     * by the bracketed search of the class comment: Newton with the phase's {@code c_p} for one dry phase, secant
     * otherwise, bisection whenever a step leaves the bracket or the residual stops halving; a step of {@code H(T)} the
     * bracket closes on (a pure component's coexistence, free water boiling) is answered from the energy balance, any
     * other step is typed not converged.
     */
X
[1084, 9, 'the temperature and pressure whose TP equilibrium has the request', <<'X'],
    /**
     * UV: the temperature and pressure whose TP equilibrium has the request's internal energy (J) and volume (m3): the
     * Newton on {@code (ln T, ln P)} of the class comment and, when it does not converge, the nested bracketed search
     * ({@code U(T)} increasing at the fixed volume, {@code V(P)} decreasing at each temperature); a pure component
     * inside its two-phase dome, or free water boiling, closes the pressure bracket on the step of {@code V(P)} and the
     * phase fraction comes from the volume balance at the saturation pressure.
     */
X
[1097, 10, 'warm-started from a previous answer', <<'X'],
    /**
     * {@link #uv} warm-started from a previous answer's temperature and pressure (the fluid network's crystal
     * equilibration of a vessel already holding crystals): the UV Newton phase starts at
     * {@code (hintTemperature, hintPressure)} with at most {@link #WARM_UV_EVALUATIONS} TP equilibria; when it does not
     * converge there, the cold search of {@link #uv} runs with its own budget. The answer meets the same specification
     * and tolerance; the diagnostics' kernel counts include the warm attempt. A hint that is not finite is the cold search.
     */
X
[1303, 9, 'The search window: the numerical box', <<'X'],
    /**
     * The search window: the numerical box ({@link NumericalBox}, where the mathematics stops; no data range bounds it);
     * resets the search's counters and state. A search starts inside the data of every model the material uses, where
     * they overlap (the package envelope, the carried species' ideal-gas functions and the steam's range), since the
     * first temperature is a lower bound from the ideal gas, which a latent heat can put far below the answer (35 K for a
     * frost answer at 150 K); from inside the data the search moves out only as far as the specification asks.
     */
X
[2056, 13, 'pressure at temperature {@code t} whose TP answer has volume', <<'X'],
    /**
     * The pressure at temperature {@code t} whose TP answer has volume {@code v}: {@code ln(v/V(P))} increases with
     * {@code ln P}, bracketed like PH. Started from the cubic's own pressure of the dry feed as one phase when the last
     * answer was one phase, otherwise from the pressures found at earlier temperatures extrapolated linearly in
     * {@code (1/T, ln P)} (one earlier answer: Trouton's slope {@code -10 T}); the first slope without a compressibility
     * is the last secant measured between two answers on one piece of {@code V(P)} ({@link #samePhases}). Accepted within
     * {@code innerLooseness} in {@code ln V} (the nested search's inexact solves far from the answer) or at the
     * specification. A step of {@code V(P)} is the volume jump between the bracket's ends, wet or dry (owner decision
     * D40 A): a pure coexistence, a crystal's step, a binary's three-phase pressure, the free water's boiling or
     * freezing, each answered from the volume balance, or a typed failure.
     */
X
[2133, 6, 'A secant only between answers on one piece of V(P)', <<'X'],
                // A secant only between answers on one piece of V(P), the same phases (owner decision D40 A): across a bubble
                // or dew point V(P) has a kink, across a step a jump whose secant is no slope; stored, such a secant started
                // the next temperature's search with steps below the spacing of ln P (100 TP equilibria at one point).
X
[2325, 12, 'one hydrocarbon of a wet feed at its equal-fugacity pressure', <<'X'],
    /**
     * The one hydrocarbon of a wet feed at its equal-fugacity pressure at temperature {@code t} (UV, owner decision
     * D40 A): the dry pure coexistence ({@link #pureCoexistence}) beside the free water as the rule's TP answers on both
     * sides of the jump hold it, condensed at the state pressure (ice below the melting line), its steam in the gas at
     * the condensed water's fugacity {@code p_w}, the hydrocarbon's vapour at {@code P - p_w}
     * ({@link #coexistencePressure}). The vapour fraction from the volume balance
     * {@code V = n [(1 - beta) v_L + beta v_V] + (n_w - beta n s) v_c}, {@code s = p_w v_V/(R T)}. Returns the answer when
     * {@code beta} lies in [0, 1] (within the tolerance, or always when {@code clamp}); otherwise null with the jump's end
     * on the target's side in {@code coexistenceX} ({@code ln P}) and {@code coexistenceResidual}, NaN when no
     * equal-fugacity pressure was found or the gas would take all of the water as steam.
     */
X
[2554, 10, 'Free water boiling (or subliming) at the state pressure', <<'X'],
    /**
     * Free water boiling (or subliming) at the state pressure {@code p}, the condensed water's vapour pressure at
     * {@code t}: the hydrocarbons as on the condensed side (their liquid at {@code (t, p)}, no vapour), the water split
     * into steam at {@code p_w = p} and condensed water (liquid, or ice below the melting line), the steam fraction from
     * the energy (PH) or volume (UV) balance. Null when the balance puts the fraction outside [0, 1] by more than the
     * specification tolerance (the target lies in a part of the step no state of the rule spans, which the caller types
     * as not converged), and beside a competing crystal, whose hydrocarbons are not one liquid.
     */
X
[2684, 16, 'freeze at a fixed volume and energy', <<'X'],
    /**
     * The free water's freeze at a fixed volume and energy: a rigid vessel of gas over water cooled below its melting
     * line freezes on the line, ice and liquid water at {@code T_m(P)}. At each pressure {@code P} of the line the warm
     * side's TP answer at {@code (T_m(P), P)} ({@link #meltingLine}) gives the ice fraction from the energy balance,
     * {@code beta = (U - U_L) / (n_c (u_s - u_L))}, and with it the volume {@code V = V_L + beta n_c (v_s - v_L)} (ice is
     * 9 % less dense than the water and compresses the gas cushion; {@code T_m} moves at -7.42e-8 K/Pa). The volume
     * balance sets {@code P}: a secant in {@code P} from {@code p0}, its first slope from the phases' compressibility and
     * bisection once bracketed, closes it in two to six TP equilibria. A {@code beta} beyond [0, 1] on the side the root
     * lies ends the solve: the line holds no answer. Returns the answer; null where the line holds none or its warm side
     * has no liquid water at {@code p0}; {@code NOT_CONVERGED} where the root lies beyond the pressure window.
     */
X
[2863, 25, 'TP with competing crystals (dry material', <<'X'],
    /**
     * TP with competing crystals (dry material; with free water {@link #solveWetWithCrystals} runs this on the
     * hydrocarbons at their partial pressure). The fluid answer of the whole feed first; each competing crystal whose
     * species is carried is tested against it, {@code D = (mu_i - mu_s)/(R T)}, the species' chemical potential in the
     * fluid answer (fugacity, equal in every phase) against the crystal's; a fluid answer whose split has a product
     * proved unstable gives its split's chemical potential as the test (below the crystal's triple point that third
     * phase is typically a subcooled liquid of the species, which the crystal replaces). No {@code D} above
     * {@link #DEPOSITION_TOLERANCE}: the fluid answer, the solids assessed. Otherwise a pure feed of the species is its
     * crystal alone (except within the coexistence tolerance of its sublimation or melting point, undetermined as a
     * pure fluid's coexistence); a mixture's solid amount is solved so that the fluid the crystal leaves, re-flashed
     * with the stability test, has the crystal's chemical potential: {@link #depositFromVapour} first, and where that
     * does not leave one stable fluid phase on its root, {@link #depositFromFluid}; two liquids beside the crystal,
     * three fluid phases and two crystals forming are the typed {@link EquilibriumResult#CRYSTAL_PHASES} hold.
     */
X
[2956, 17, 'TP with competing crystals and free water', <<'X'],
    /**
     * TP with competing crystals and free water (ice and a competing crystal coexist): the free-water rule's answer of
     * the whole feed first ({@link #solveWet}), each competing crystal tested against its hydrocarbon phases as in
     * {@link #solveWithCrystals}. No crystal forms: that answer, the solids assessed. Otherwise the hydrocarbons and their
     * crystal are solved as a dry feed at the hydrocarbon partial pressure {@code pc = P - p_w}, where their vapour lies,
     * and the water is laid around them by its rule (the steam in their vapour at the condensed water's vapour pressure
     * and the rest condensed, or all steam where that leaves the gas unsaturated); {@code pc} then follows the new gas
     * (the fixed point of {@link #solveWet}), started at the rule's own {@code pc}. The liquid and the crystal are
     * restated at the state pressure: solving them at {@code pc} moves their chemical potentials by
     * {@code v p_w / (R T)}, below 1e-7 where a crystal forms beside ice, 2e-5 for a liquid beside liquid water at 280 K.
     * A wet pure hydrocarbon at its crystal's own coexistence is the typed {@link EquilibriumResult#CRYSTAL_STEP} failure.
     */
X
[3129, 17, 'vapour-solid solve of P4 stage 2a', <<'X'],
    /**
     * The vapour-solid solve, the first attempt of a mixture: the solid amount is the one unknown. With {@code r} the
     * species' amount left in the fluid and {@code s = ln(r/z_i)} in {@code (-infinity, 0]}, the fluid holds
     * {@code z - (z_i - r) e_i} and {@code f(s) = (mu_i^V - mu_s)/(R T)} on the reduced feed's vapour root increases with
     * {@code s}. Newton on {@code s} with {@code f' = 1 - y_i + y_i Phi_ii}, bracketed (bisection when a step leaves the
     * bracket), to {@code |f| <= }{@link #DEPOSITION_TOLERANCE}; below the trace floor ({@code y_i = }{@link #DEPOSITION_TRACE})
     * with {@code f} still positive the deposition is complete. The reduced feed is then re-flashed with the stability
     * test; the answer is returned only when that fluid is one phase meeting the tolerance on its own root
     * ({@link Classification#LIQUID_SOLID} for a liquid-like one-root fluid, else {@link Classification#VAPOR_SOLID});
     * every other outcome returns {@code null} and {@link #depositFromFluid} decides. A second crystal forming is the
     * typed hold. The smaller of the solid and the fluid's share is computed directly ({@code -z_i expm1(s)} or
     * {@code z_i exp(s)}), the larger by difference, so the species is conserved to rounding.
     */
X
[3253, 24, 'The general crystal solve', <<'X'],
    /**
     * The general crystal solve: the solid amount of crystal {@code c} out of a mixture whose fluid the crystal leaves
     * may be a vapour, a liquid or a vapour-liquid split. The unknown is {@code s = ln(r/z_i)} in {@code (-infinity, 0]};
     * every iterate re-flashes the reduced feed {@code z - (z_i - r) e_i} with the stability test, and
     * {@code f(s) = (mu_i - mu_s)/(R T)} of that fluid answer is nondecreasing in {@code s} (a binary's vapour-liquid
     * split holds it constant, where the step goes on), with {@code f(0) = D > 0}. Steps: Newton with
     * {@code f' = 1 - y_i + y_i Phi_ii} on a one-phase fluid's root, a secant otherwise, {@code s - max(1, |f|)} before a
     * bracket exists, bisection when a step leaves the bracket or two steps fail to halve {@code |f|}; at most
     * {@link #CRYSTAL_ITERATIONS} re-flashes. Converged on a converged fluid answer each of whose phases has
     * {@code |mu_i - mu_s|/(R T) <= }{@link #DEPOSITION_TOLERANCE} (a split's phases agree on {@code mu_i} only to the
     * flash's fugacity tolerance: stopped on its first phase alone, the search left the other outside and the answer was
     * refused, LNG at 140.9 K and 620 kPa freezing CO2). A reduced fluid whose split has a product proved unstable steers
     * the search but is never an answer. Below the trace floor with {@code f} still positive the deposition is complete.
     * The answer: the fluid's phases and the crystal ({@link Classification#VAPOR_SOLID},
     * {@link Classification#LIQUID_SOLID} or {@link Classification#VAPOR_LIQUID_SOLID}); two liquids beside the crystal
     * are the typed {@link EquilibriumResult#CRYSTAL_PHASES} hold, a search that does not converge {@code NOT_CONVERGED}.
     * The solid is {@code -z_i expm1(s)} and the fluid carries {@code z_i e^s}, so the species is conserved to rounding.
     */
X
[3688, 16, 'pure species of a competing crystal at its equal-chemical-potential point', <<'X'],
    /**
     * The pure species of a competing crystal at its equal-chemical-potential point with its own fluid (PH at the fixed
     * pressure, UV at the fixed temperature): below the family's triple point the sublimation step with the vapour, above
     * it the melting step with the liquid. The energy or volume balance determines the split,
     * {@code target = N [(1 - beta) x_s + beta x_F]}; as {@link #pureCoexistence}, the answer when {@code beta} lies in
     * {@code [0, 1]}, otherwise null with the jump's end in {@code coexistenceX} and {@code coexistenceResidual}. A
     * near-pure feed (D21) takes the same step at the same point, its traces in the fluid ({@code beta} the fluid's share
     * of all the moles, the fluid evaluated at its own composition, a few re-evaluations converging the split), while the
     * traces are at most {@link #STEP_FLUID_TRACES} of that fluid; a target beyond that end is the general solve's, and a
     * closed search there is the typed {@link EquilibriumResult#CRYSTAL_STEP} hold.
     */
X
[3911, 15, 'pure species of a competing crystal at its family', <<'X'],
    /**
     * The pure species of a competing crystal at its family's triple point in UV: the vapour, the liquid and the crystal
     * at the family's triple point ({@link Crystal#tripleTemperature()}, {@link Crystal#triplePressure()}), with the
     * amounts from the three balances (moles, energy, volume). A target inside the triple point's triangle (every amount
     * nonnegative within the specification tolerance) has this answer, {@link Classification#VAPOR_LIQUID_SOLID}, before
     * any search (at that volume {@code U(T)} steps at the triple temperature); otherwise null. A near-pure feed (D21)
     * carries its traces in the larger fluid phase of the balances, that fluid evaluated at its own composition and the
     * balances solved again until the amounts settle; with more than {@link #STEP_FLUID_TRACES} of traces in it the
     * answer is the typed {@link EquilibriumResult#CRYSTAL_STEP} hold.
     */
X
[3983, 11, 'Pure water with only its own steam at its triple point', <<'X'],
    /**
     * Pure water with only its own steam at its triple point, the end of the free water's melting line where no gas holds
     * the pressure above the steam's: steam, liquid water and ice at the ice record's triple point (273.16 K and
     * 611.657 Pa), with the amounts from the mole, energy and volume balances over the corners {@link #waterTriangle}, as
     * {@link #pureTriplePoint} answers a crystal's species. A target inside the triangle ({@link #insideTriangle}) has
     * this answer before any search (at its volume {@code U(T)} rises by the latent heats within 2e-9 K of the triple
     * temperature, a step no search converges on); a rigid vessel of water and its steam cooled through 0 °C stands here
     * until its liquid is gone. Otherwise null.
     */
X
[4163, 14, 'three-phase step of a dry binary with a competing crystal', <<'X'],
    /**
     * The three-phase step of a dry binary with a competing crystal: at the fixed pressure (PH) the temperature, at the
     * fixed temperature (UV) the pressure where the vapour, a liquid and the crystal coexist, and the three amounts from
     * the two species' balances and the energy or volume balance. The three-phase point is solved from the bracket's two
     * ends: Newton on {@code (X, ln x_i, ln y_i)} ({@code X = 1/T} for PH, {@code ln P} for UV) for
     * {@code (mu_i^L - mu_s, mu_i^V - mu_s, mu_j^L - mu_j^V)/(R T) = 0}, a finite-difference Jacobian, steps capped, to
     * {@code 1e-11}; it must lie inside the bracket with two distinct fluid phases. One free amount along
     * {@code d = (1 - x_i, -(1 - y_i), x_i - y_i)} keeps both species' balances and the third balance fixes it. As
     * {@link #pureCoexistence}: the answer when every amount is nonnegative within the tolerance (or clamped), otherwise
     * null with the step's end in {@code coexistenceX} and {@code coexistenceResidual}, NaN when no three-phase point
     * lies in the bracket. The liquid's other species and the crystal are taken by difference, so both species conserve.
     */
X
[4387, 16, 'The onset temperature of crystal', <<'X'],
    /**
     * The onset temperature of crystal {@code crystal} ({@code null}: the one competing crystal whose species is carried)
     * at {@code pressure}: the frost point of a gas, the liquidus of a liquid, a three-phase point of a split, whatever
     * the fluid answer of the whole feed is at the onset. A bracketed search in {@code x = 1/T} ({@code D} increases with
     * {@code x}; {@code mu_i} is continuous across the fluid's own phase boundaries) from the partial-pressure
     * approximation (or the window's warm end), Newton with the ideal-gas sublimation enthalpy as the first slope and
     * secants after, steps capped at a fifth of {@code x} before a bracket, bisection when a step leaves it; converged at
     * {@code |D| <= }{@link #DEPOSITION_TOLERANCE}. The window is the numerical box below the competition's ceiling
     * ({@link #crystalCeiling}); an onset beyond it is {@code NOT_CONVERGED}, one outside a data range is graded
     * extrapolated, one whose fluid is two liquids the typed {@link EquilibriumResult#CRYSTAL_PHASES} hold.
     *
     * @throws IllegalArgumentException when no competing crystal (or {@code crystal}) is made of a carried species, or
     *         several are and none is named
     */
X
[4408, 9, 'frost point of a gas of composition', <<'X'],
    /**
     * The frost point of a gas of composition {@code amounts} as one vapour phase: the temperature at {@code pressure}
     * where crystal {@code crystal} and the feed on the cubic's vapour root have equal chemical potentials of the
     * crystal's species, the standard solid-vapour temperature at a measured vapour composition, whether or not the gas
     * would condense at that pressure. The mirror of {@link #freezingTemperature(String, double, List, double[], Workspace)};
     * {@link DepositionOnset#fluid()} is the fluid answer of the whole feed at the onset.
     */
X
[4422, 13, 'The freezing point (P5): the liquidus', <<'X'],
    /**
     * The freezing point: the liquidus of a liquid of composition {@code amounts} at {@code pressure}, the temperature
     * where crystal {@code crystal} ({@code null}: the one competing crystal whose species is carried) and the feed as one
     * liquid phase (the cubic's liquid root) have equal chemical potentials of the crystal's species; for a pure feed its
     * melting temperature. The standard solid-liquid solubility temperature at the liquid's own composition, whether or
     * not a vapour would form from it. The same bracketed search in {@code x = 1/T} as
     * {@link #depositionTemperature(String, double, List, double[], Workspace)}, started at the ideal-solubility estimate
     * {@code 1/T = 1/T_t - R ln x_i / Delta h_fus}; no fluid flash per step. {@link DepositionOnset#fluid()} is the
     * fluid answer of the whole feed at the onset; the partial-pressure approximation is {@code NaN}.
     */
X
[4845, 11, 'A dry split. The equilibrium', <<'X'],
    /**
     * A dry split. The denser phase is the liquid and the lighter the vapour, unless both are liquid-like
     * ({@code PIP > max(1, Z)} on each product's own root) and denser than the cubic's critical volume
     * ({@link PhaseIdentification#denseLiquidLike}: a dense gas beside a liquid at 8 to 10 MPa has a parameter just above
     * one while less dense than {@code 3.9514 b}) and the split lies outside the declared critical band: then it is
     * {@link Classification#LIQUID_LIQUID} with both phases {@link PhaseKind#LIQUID}, the lighter first. Inside the band
     * the parameter is no label to trust (next to a critical point it runs to plus or minus infinity), so a split there
     * stays vapour-liquid.
     */
X
[5454, 11, 'Rachford-Rice for the current {@code ln K}, solved in the minor phase', <<'X'],
    /**
     * Rachford-Rice for the current {@code ln K}, solved in the minor phase's fraction: the vapour fraction {@code beta}
     * of {@code sum z_i (K_i - 1)/(1 + beta (K_i - 1)) = 0} when the root lies at or below one half, otherwise the liquid
     * fraction {@code delta = 1 - beta} of the same equation ({@code 1 + beta (K_i - 1) = K_i - delta (K_i - 1)}), so the
     * minor fraction carries full relative precision (a liquid of 1e-14 of the feed is {@code delta = 1e-14}, a split
     * while {@code delta > 0}). The first evaluation, at one half, tells which phase is minor; safeguarded Newton with
     * bisection between that fraction's pole and one half, stopped when a step moves the fraction by at most 1e-15 of
     * itself. Fills {@code k}, the normalised {@code x} and {@code y}, and both fractions; false when the K-values
     * straddle no split.
     */
X
);
