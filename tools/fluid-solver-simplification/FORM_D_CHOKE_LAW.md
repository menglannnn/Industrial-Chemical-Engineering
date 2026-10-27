# Form D: a choke law with real pressure dependence (t38, 2026-10-19; owner request of 2026-10-18)

The owner's words: "try this instead of hard velocity cap for choked flow: a choke law with real pressure dependence:
√(2ρ(P − P_sat)) for flashing liquid, ∝ P for gas, replacing the constant velocity. A junction then gets a pressure handle
back, and the law is closer to the physics t37 described." Form A's smooth saturation stays,
q = q_law (1 + (q_law/q_cap)^8)^(-1/8); form D changes what q_cap is: A · G_choke of the upstream stream instead of
ρ · min(100 m/s, c_T). Prototyped on `git archive` exports of `6c073298` only (patches in `patches/`); nothing lands
until the owner decides. Detail here; the plan (`documentation/.../SOLVER_SIMPLIFICATION_PLAN.md`) holds the summary.

## The law (patch `form-d1-choke-law.patch`, `FluidThermodynamics.chokeFlux`)

| Stream | Choke mass flux G (kg/(m2 s)) | From the engine state | Notes |
|---|---|---|---|
| Gas (hydrocarbon vapour and steam) | G* = ρ c_T √γ K(γ), K(γ) = (2/(γ+1))^((γ+1)/(2(γ−1))) | ρ and c_T from the vapour's EOS volume and (∂V/∂P)_T (the isothermal acoustic bound the old cap already read); γ = cp/cv with cv = cp + T (∂V/∂T)_P² / (∂V/∂P)_T from the same EOS record; steam alone 4/3 | Equal to the owner's P √(γM/(ZRT)) K for a gas whose Z does not move with pressure; 0.667 ρ c_T at γ = 1.3, within 3 % from γ 1.2 to 1.4. Near a critical point γ grows as c_T falls: the isentropic sound speed √γ c_T stays finite, so the flux does not collapse there. dq_cap/dlnP = q_cap for an ideal gas: the junction's pressure handle. |
| Hydrocarbon liquid | G = √(2 ρ (P − P_sat) + G_2²) | ρ the phase density; P_sat = the node's own pressure where it holds vapour (two-phase, as K6 reads it), 0 for a subcooled single-phase liquid (the proxy); G_2 from the two phases where both exist | Bernoulli to the vapour pressure: a flashing liquid cannot pass a throat below P_sat without choking there. The sum in quadrature is smooth in P and continuous at saturation. |
| Free water | the same, with P_sat = water's saturation curve at T | Region 1 and the ideal steam | |
| Two-phase floor G_2 | 0.9 h_lg / (v_lg √(c_pl T)) | h_lg, v_lg per kg from the two phases' EOS enthalpies and volumes, c_pl the liquid's EOS heat capacity (water: 4180 J/(kg K)) | Leung's ω-method for a saturated liquid, G_c = η_c √(P ρ_l / ω) with ω = c_pl T P ρ_l (v_lg/h_lg)², which reduces to this with η_c at 0.9 (its polynomial gives 0.89 to 0.96 from ω 20 to 90). |
| Bulk of a mixed state | the smallest of its held phases' fluxes | | A bubbly liquid chokes at its two-phase flux. |

Patch `form-d2-gas-choke.patch` is the gas row alone (liquids keep the configured 100 m/s).

## What it deletes and adds

| Deleted | Added |
|---|---|
| The configured maximum velocity: the config entry `fluidMaximumVelocity`, `FluidOptions.maximumVelocity`, the model parameter and its part in the model revision (certificate signatures), `FluidPropertyReloadGuard`'s comparison, `maximumVelocityMetresPerSecond()` (the prototype leaves the field in place and stops reading it) | `chokeFlux(state, phase)` and `twoPhaseChokeFlux` (about 60 lines, one constant 0.9) |
| `vaporVelocityLimit`'s and `phaseVelocityLimit`'s min with the configured maximum | nothing in the solver: `Transport.velocityLimit`, `capLimit`, PhaseDraw's capacities and the junction holdup sizing read the same quantity, now the choke velocity at the stream's density |

## Numbers (`probe/ChokeLawProbeTest.java` on the D1 export; velocities at the stream's own density; old cap = min(100 m/s, the isothermal acoustic bound))

| State | ρ (kg/m3) | c_T (m/s) | choke flux (kg/(m2 s)) | choke velocity (m/s) | old cap (m/s) |
|---|---|---|---|---|---|
| methane gas 300 K, 1 atm | 0.653 | 393 | 172 | 263 | 100 |
| methane gas 300 K, 1 MPa | 6.56 | 386 | 1708 | 260 | 100 |
| nitrogen 298 K, 1 atm | 1.145 | 297 | 233 | 204 | 100 |
| methane 230 K, 6 MPa (gas) | 71.5 | 244 | 13 700 | 192 | 100 |
| methane 195 K, 4.5 MPa (near-critical gas) | 82.6 | 147 | 11 340 | 137 | 100 |
| methane 200 K, 6 MPa (dense, liquid-like root) | 155 | 118 | 43 200 (Bernoulli, P_sat 0) | 278 | 100 |
| LNG 112 K, 222 kPa, subcooled (seed 397's generator) | 488 | 1079 | 14 700 | 30.2 (24 with the true P_sat 99 kPa) | 100 |
| LNG 112 K, 3 MPa | 489 | 1111 | 54 200 | 111 | 100 |
| LNG 150 K, 6 MPa | 431 | 703 | 71 900 | 167 | 100 |
| boiling LNG 111.8 K, 1 atm: oil / gas / bulk | 488 / 1.87 / 211 | 28.6 (mixture) | 1359 / 291 / 291 | 2.8 / 156 / 1.4 | 100 / 100 / 28.6 |
| water 298 K, 1 atm | 997 | 1490 | 13 990 | 14.0 | 100 |
| water 372.5 K, 1 atm (near boiling) | 959 | 1462 | 2282 (the floor) | 2.4 | 100 |
| crude 311 K, 316 kPa, subcooled (seed 407) | 855 | 1106 | 23 200 | 27 | 100 |
| boiling crude 311 K, 120 kPa: oil / gas / bulk | 855 / 1.46 / 761 | 37.6 (mixture) | 519 / 268 / 268 | 0.61 / 184 / 0.35 | 100 / 100 / 37.6 |

The gas law stands 2 to 2.6 times above the configured 100 m/s at every state read, finite at the near-critical point; the liquid law stands below it everywhere but far above saturation, and the two-phase floor puts boiling liquids at 0.6 to 2.8 m/s and their bulk mixtures at 0.35 to 1.4 m/s.

## Physics gaps the owner named

- At P = P_sat the Bernoulli term is zero; the two-phase floor G_2 keeps a finite flux, and the quadrature sum keeps the law smooth in P.
- P_sat: a two-phase node's own pressure (as K6); water's curve; a subcooled hydrocarbon liquid has none without a bubble-point flash, so the prototype takes 0 and overstates the flux near the bubble point (seed 397's LNG at 222 kPa: 32 against 24 m/s), by nothing 3 MPa above it. A bubble-point flash per node and pass would close that at the cost of one flash per node per pass (the Transport is rebuilt at every decode).
- γ and Z come from the EOS record the state already carries (`heatCapacity`, `volumeTemperatureDerivative`, `volumePressureDerivative`, `compressibility`): no new property model.
- dq_cap/dP at a junction between choked lines: gas, q_cap ∝ P/c_T so dq_cap/dlnP = q_cap for every outflow line; a liquid 3 MPa above saturation, dq_cap/dlnP = q_cap P/(2(P − P_sat)) ≈ 0.5 q_cap; at saturation the floor is flat in P. Seed 19 (an all-choked gas network) is the first world to read.

## Evidence (200-seed quick check on exports of `6c073298`, 4 threads; the baseline at `6c073298`: cold 191, bundled random 197, defaults 200; seed 19 held E5 on the baseline)

| Form | Cold / bundled / defaults | Seed 19 | Clean-seed counters against the baseline (Newton iterations; residual evaluations) | Note |
|---|---|---|---|---|
| D1 full law (`form-d1-choke-law.patch`: gas choke on every gas stream, liquids Bernoulli to P_sat with the two-phase floor) | 173 / 188 / 200 | held (E6, bundled list shows 19 as a bundled hold too) | cold x7.9 (961k against 122k); bundled x72 (8.25M against 114k); 12 clean seeds held on their wall budgets | the two-phase floor (1 to 4 m/s at the liquid's density) makes every boiling-tank drain crawl: 808k passes against 24k; rejected as prototyped |
| D2 gas choke on phase-port gas streams (`form-d2-gas-choke.patch`) | 191 / 197 / 200 (defaults bitwise) | clean | cold -0.05 %; -0.5 % residual evaluations; bundled +0.4 %; -0.3 % | the cold held set is the baseline's less 19 (101 moves from the iteration limit to a stall) |
| D3 the gas choke on bulk gas ends too (`form-d3-gas-choke-bulk.patch`) | 189 / 196 / 200 (defaults' counters move by 0.8 %) | clean | cold +0.2 %; +8 % residual evaluations; bundled +17 %; +75 % residual evaluations, +40 % Jacobian builds | cold held set: 15, 118, 183 (B), 6, 29, 98, 101, 110, 116 (E5), 77 (E6), 178 (E8): 19 and 85 clear, 6 and 98 new; bundled 116, 157 (E5), 47 and 173 on wall budgets |

Seed 988 (bundled; t40: deep saturation at a join, the chord runs out at the limit of 30, bisects to form A, a limit of 40 clears it): held (E6) on the baseline and on D2; clean on D1 and D3. Seed 19 (cold, a line-search stall on the baseline): clean on D1, D2 and D3. So the gas choke on every gas stream gives both junctions their pressure handle without raising the limit; on phase-port streams alone it gives it to seed 19 only.

**Suites against `6c073298` (javac lane, `science-fluid-network` and `runtime-fluid`).** D2: 184/184 and 301/303, two gate worlds hold: `HoldClassWorldsTest aFilterBetweenTwoWanderingJunctionsConverges` and `aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn`; 15 + 12 tests print moved physics lines (the phase-port tests' vents and top ports, the fed-tank outlets, the stack pass, the mixed-gas junction cases). D3: 183/184 (`VelocityClampTest aWarmWorkspaceReleasesTheClampWhenThePressureDriveFalls`: the configured 1 m/s limit no longer binds a gas, a restatement) and 301/303 (`aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn` holds; `MixedGasJunctionTransientTest everyCaseConvergesAtTheFiveSecondCycleWithinTwiceTheMeasuredCost`: the twelve mixed-gas cases cost more than twice their measured reference); 30 + 36 tests print moved lines. Logs: `logs/t38-logs.tar.gz` (`d1q-*`, `d2q-*`, `d3q-*`, `*-clean-*`, `*-988-*`, `*-19-*`, `suites/d2-*`, `suites/d3-*`, the `cmp-d2-vs-d38s-*` and `cmp-d3-vs-d38s-*` comparisons).

## Recommendation

Do not land form D now. The gas law is the physics and gives the junction its pressure handle (seeds 19 and 988 clear at the limit of 30), but as prototyped it holds two gate worlds (D2) or one gate world and the mixed-gas cost gate (D3), makes the configured velocity limit meaningless for gases (delete it, with its config, model parameter, revision and reload guard), costs D3 17 % more Newton work on the bundled clean seeds, and moves 27 to 66 printed references. If the owner wants the handle, the next step is one Opus trace of the two gate worlds under the gas law, then the restatements. The liquid law is rejected as prototyped: the two-phase floor is right for a flashing line but wrong as a cap on every liquid draw from a boiling tank (1 to 3 m/s, cost 8 to 72 times), and a subcooled liquid needs a bubble-point flash to be exact; keep liquids on the configured cap until a flashing line has a regime of its own.

## Owner decisions (2026-10-19)

1. The gas law goes no further for now. An Opus thread, t42 (`D:/Minecraft/Modding/claude-threads/t42-d3-gates-trace.md`), traces D3's failing gates first: the liquid trace at a tank's bottom port (`HoldClassWorldsTest aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn`), the mixed-gas cost gate (`MixedGasJunctionTransientTest everyCaseConvergesAtTheFiveSecondCycleWithinTwiceTheMeasuredCost`), D2's filter world (`aFilterBetweenTwoWanderingJunctionsConverges`), and the source of the +17 % bundled cost. Nothing lands until the owner has read that; t42 may read the patches and logs here.
2. Liquids keep the configured 100 m/s cap; the liquid law is shelved, so question 2 below (the bubble-point flash) lapses. For the record, the orchestrator found that the liquid law reads P_sat as the node's own pressure, so the liquid head above a bottom port never subcools it: a real boiling tank drains at about √(2gh), 6 m/s for 2 m of liquid, where the prototype's floor gave 1 to 3 m/s.
3. Form D is dropped (owner, 2026-10-19, on t42b's `FORM_D3C_COLD_SEEDS_TRACE.md`): the configured 100 m/s cap stays for gas and liquids, bundled seed 988 stays a known E6 hold, and the D-form patches in `patches/` stay as evidence only. What decided it: the corrected D3c's quick check equals the baseline's (cold 191 / bundled 198 / defaults 200 both); 988 is cleared by the cap's level and not by a pressure handle (t40: a stale chord after the cap knee at a choked join; t42b: 988 is clean under D3c and D3d at 607 Newton iterations where the baseline holds E6 at 2,610, and clean on the baseline too with the old law's limit at 150 or 260 m/s); cold seeds 6 and 15 are marginal cold starts that hold only under raised caps (D2, D3, D3c, D3d and the old law at 260 m/s), never on the baseline.
4. The junction pressure-range guard (t42b's item for seeds 6 and 15; it would cover the baseline's marginal cold starts too) is on hold.
5. Counting junctions in the step's state-change measure (t42b's item for seed 199, whose first step hops between steady branches at a cost of x9 without a hold) is on hold.

## Questions for the owner (as asked; all answered by the decisions above: form D dropped, the liquid law shelved)

1. Pursue the gas law (D3) with the two traces, or keep the limit of 30 as the handle?
2. For subcooled liquids, a bubble-point flash per node and pass, or the proxy (nothing, up to a third over near saturation)?
3. Is a boiling tank draining at 1 to 3 m/s through its bottom port the game you want, or should flashing stay in the line's law only?

## Risks

- Gas vent lines choke 2 to 2.7 times higher than at 100 m/s: every gas-vent reference moves (the junction holdups too: `PassiveNetwork.sizeJunctionHoldups` reads the same cap).
- Saturated liquids drawn from boiling tanks choke 30 times lower than at 100 m/s (1 to 4 m/s at the liquid's density): draining a two-phase tank through its bottom port becomes slow in the model where it was fast; this is the physics of flashing flow, and it moves every such reference.
- A subcooled liquid without a bubble-point flash overstates its flux near saturation (the proxy).
