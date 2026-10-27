# Solid system and solver convergence: crystals as solids (D22)

Date: 2026-10-15. This is a read-only analysis for the lead: no code was changed and Gradle was not run.

- Main is `ffeafe8a` (0.8.0), read in the main checkout. Every line number below is main's unless it is marked `9aaafd4`, which is the branch, read with `git show`.
- Particle sizes come from `PRECIPITATED_SOLIDS_RESEARCH.md` (e).
- Owner input folded in: D22 (crystals are solids of two kinds; vessels are homogeneous; precipitation during transport blocks; size left open); "settling depends on size, density, pipe and fluid"; the plan answer of 2026-10-15 (size grows with the freezing phase's fraction per Brauner; a slush at phi >= 0.5 blocks above about 0.5 solids; vessels well mixed, nothing settles; d_dilute constant until the agitation work).
- The core of this note is sections 4 to 7.

## 1. Physics first: what should happen, in numbers

- **Formation (owner plan).** A dispersed liquid freezes drop by drop, so each particle is one drop: d = d_dilute * max(1, 4.04 (phi/(1-phi))^0.6).
  - d stays at d_dilute up to phi = 0.09.
  - It reaches 1.76, 2.43, 3.17 and 4.04 d_dilute at phi = 0.2, 0.3, 0.4 and 0.5.
  - At phi >= 0.5 the freezing phase is the continuous one. It forms a slush that stops flowing near 0.5 solids.
  - Solid formed from a gas has no liquid to carry it.
- **Transport under main's law** (section 2). Required liquid velocity in m/s; the densities and viscosities in the headers are approximate:

| d | CO2-I in LNG (1560 / 430 kg/m3, 0.12 mPa s) | CO2-I in CO2-rich liquid (1560 / 1100, 0.2) | ice in light oil (917 / 800, 2) |
|---|---|---|---|
| 0.1 mm | 0.33 | 0.09 | 0.003 |
| 0.25 mm | 1.0 | 0.34 | 0.02 |
| 1 mm | 2.8 | 1.1 | 0.22 |
| 4 mm | 5.8 | 2.3 | 1.1 |

- At the usual 1 to 2 m/s:
  - CO2 crystals in LNG settle out above about 0.3 mm. This is why LNG plants remove CO2 to ppm levels before liquefaction.
  - Ice in oil keeps flowing up to a few mm.
  - Brauner's own criterion (research (e)) asks 1.6 to 3.5 times more velocity at 1 to 3 mm, with the same ranking.
- **Steady state:** a vessel at equilibrium keeps its crystals in suspension. An outlet carries them when its liquid velocity exceeds what the largest particle needs, otherwise it shuts for that interval. A let-down that would freeze inside the line blocks it (D22 (4)). The island Newton never needs to know a particle size.

## 2. Main's settling and deposition model, and size storage (short)

| Item | Main |
|---|---|
| Settling | Ferguson-Church with C1 = 18 and C2 = 0.4. Buoyancy uses abs(rho_p/rho_c - 1), so rising particles are covered too. SlurryTransport L44-61 |
| Deposition velocity | 10 x the settling speed, with the comment "a calibrated multiplier is not claimed": L63-71, Settings L16-18 |
| Carrier | Liquid mass over liquid volume. Viscosity is volume-averaged over the organic and water liquids. Vapour and solids are excluded: SolidMobility L58-68 |
| Particles counted | Populations above 1e-8 of the occupied volume, vapour included: L61, SlurryTransport L84-90. The largest diameter: L71. The largest deposition velocity, each population at its own density: L72-73 |
| Velocity | abs(q) / drawn mass x liquid volume / widest area, i.e. the lowest velocity in the line: L93 |
| Tests in order (`check`, L87-101) | 1. A gas outlet is always mobile (L89). 2. Immobile liquid (L90-91). 3. No liquid carrier (L95). 4. Largest particle >= narrowest bore: PARTICLE_TOO_LARGE (L96). 5. Solids over condensed volume >= 0.62: PACKED_SLURRY (L99-100). 6. No filter and v <= required: DEPOSITION (L101) |
| Pipe in the law | Only the area (through the velocity) and the narrowest bore. There is no diameter term (no Durand form) |
| When it is checked | Never in the Newton. Only in SolidEventIntegrator (SEI): the rate pass at the interval start (L277-291, at most 2 x pipes attempts) and the guards at step ends (L299-319, at most 2 x pipes + 1 events) |
| A failed check | The connection closes both ways (L283, L305). It stays closed for the rest of the interval and is re-decided from scratch at the next one (L269). There is no bed and no pipe inventory: solids and liquid stay in the donor. The reason is shown (FluidDeviceScreen L247-265) |
| Vessel | Already well mixed. The suspension is uniform in the combined liquid (FluidThermodynamics L268). A phase draw carries its liquid's volume share of the solids (PhaseDraw L14-18). The Krieger-Dougherty (KD) viscosity does not depend on size (SlurryTransport L32-39; PassiveStepSolver L2925-2933, phi capped at 0.62 - 1e-9). The level head counts solids (L1935-1937). Size acts only at outlets, so the owner's well-mixed rule needs no change |
| Size | ParticleSize is an exact decimal, stripped of trailing zeros, positive only; "physics consumes its double value, never its key" (L6-17). The key is (material id, size) (SolidInventory L10). Equal keys merge (L42). A population with zero mass is dropped (L41). A trace never frees a slot (L6). At most 64 keys (L8, exception at L46). Distinct sizes never merge (v1 decision) |
| Newton | Three moments per node: mass, volume, heat capacity. None depends on size (PhaseLayout L86; rows L351 for vessels, L397 for junctions). Populations are rebuilt as extra right-hand sides of one LU (ConservativeTransport L236; a negative weight fails at L392). Per-population audit: PassiveStepSolver L2890-2904. In transport, the 64-key cap is enforced by the POPULATION_LIMIT closure (SEI L92-98) |

## 3. What a size set at formation needs: the lightest form

| Need | Lightest form (main mechanism reused) |
|---|---|
| A bounded number of keys | Snap d to the R10 preferred numbers (1, 1.25, 1.6, 2, 2.5, 3.15, 4, 5, 6.3, 8 per decade), stored as a decimal ParticleSize. The range [d_dilute, 4.04 d_dilute] then gives at most 7 keys per crystal. A later agitation formula stays at 10 keys per decade. Snapping moves d by 12 % at most |
| phi | The crystal component's volume share of the vessel's liquids before the closure: n_c in the liquids x solid molar volume / liquid volume, capped at 0.5. It equals the freezing phase's fraction when that phase is nearly pure, is small for a solute, and is 0 when there is no liquid (OWNER DECISION 1) |
| When the size is set | Only at onset. A vessel with no population of that crystal creates one key once the solid passes main's 1e-8 trace fraction. A population is never re-sized |
| Growth and melting | Proportional to mass over that crystal's populations in the vessel (takeFraction L84, merge L42). A population melted to zero drops its key (L41) |
| Moments | Unchanged. The crystal record has a density and a heat capacity, like an inert one. Its energy offset is applied only in the closure (section 4) |
| Slush | PACKED_SLURRY already measures solids over the condensed volume. Give that test a blocking fraction of 0.5; KD keeps phi_max = 0.62 (OWNER DECISION 3) |
| Anything beyond the slush | Nothing. At phi >= 0.5 the slush rule blocks, and d is capped at 4.04 d_dilute |

## 4. Forms S and R: what each changes in main's solver

**Recommended: S-refresh.**

- **Where the crystal enters.** It joins the regime of `InventoryEquilibrium.solve` (L24-55).
  - `refresh` runs `solve` for every vessel at the start of each interval and of each solid-event segment (PassiveIntervalSolver L184, SEI L264).
  - The class's own rule is "never inside a network residual" (L8).
- **When it is present.** A crystal is present when its stability test says so; the 1e-8 nucleation threshold acts as hysteresis.
  - When present, its amount is one more unknown of `close`, with mu_s = mu_c as its row.
  - Main already has the pass loop (16 passes, cycle check L41, pass limit L54) and the domain violation typed at the node (L16-17).
- **Limit to refresh.** The crystal letter is allowed only when `solve` is called from refresh.
  - The dry-vessel call inside a step (ConservativeTransport L533) keeps the letter off.
  - Otherwise crystals would appear inside a step, where the gate or the per-population audit (L2904) rejects them.
- **Inside the interval.** Crystals are inert populations. The step's own closure keeps the step's phase support (L518-534).

| | S-refresh | R: crystal row in the island Newton |
|---|---|---|
| Island Newton, Jacobian, PassKey | Unchanged | One more unknown and row per crystal and vessel. Latent heat appears in the energy rows. Every onset is a kink like D54's, so A423's branch-consistent Jacobian and chord refusal are needed again |
| Reconstruction and gate | Unchanged; a crystal population is an ordinary key | Frozen mass is booked into population columns inside the step and checked by the 1e-8 gate |
| Energy datum | The population keeps main's U = C (T - 298.15) (SolidInventory L32). Refresh solves with true U = main U + u0 m_s and writes main U back; u0 is the crystal's constant offset on the fluid datum, of the order of minus its sublimation energy (roughly -0.5 MJ/kg for CO2-I). Transport carries the offset with the mass | The offset appears in every energy row |
| Lag | Crystals change once per interval or segment. A 5 s interval is often one step, so this is close to the branch's per-substep placement. The projection cannot overshoot | None |
| Cost | One stability test per crystal and cold vessel per interval; one more unknown when the crystal is present | A larger Newton at every step, and more iterations near onsets |
| Bundled fuzz | Bit-identical: the crystal letter exists only in a package that has crystal species | Identical only if the rows are gated. Otherwise roundoff reshuffles holds; C4's roundoff exposed W1 and W2 (A434) |
| Recorded stance | Consistent with D18 | Rejected by D18 |

Two other forms:

- **S-substep** is the branch's placement: the same closure after every accepted substep. It lags by one substep and costs one closure per substep (P6B failure 2).
- **S-engine** is the branch's engine UV (`9aaafd4` FluidThermodynamics L455-640). It brackets with up to 30 inner flashes, uses a deadband of 1e-10, and throws an IllegalStateException that becomes class X unless it is typed.

## 5. Convergence impact by hold class

- **Scope.** The table covers pilot worlds. The bundled fuzz has no crystal species, and S-refresh changes nothing there: the class tables and counters are expected to be identical.
- **Baseline.** The last recorded campaign is `cl18` on 0.7.0: random 972 / 1000, world defaults 1000 / 1000. By class (`cl16` less the seeds `cl18` cleared): C 10, E10 4, E2 1, E3 2, E5 3, E6 2, E7 1, E8 1, F 4.

| Class | S-refresh | R | Size rule (either form) |
|---|---|---|---|
| A state-change lock | None new: refresh jumps are not steps (stateChange L432-441) | Yes: a sealed vessel on a frost-point plateau moves P past STATE_CAP 0.05 at every step | None |
| B trace underflow | None: no crystal unknown and no key below 1e-8 | Yes: an emerging solid at 1e-20 sits in a scaled row | None |
| C typed thermo wait | The main pilot class. Refresh's violation is typed at the node (L16-17). Causes: near-pure CO2 on its sublimation line (P6B failure 1, D21 still open) and the cold package's data floors | The same causes surface as E10 or E2 inside the Jacobian | None |
| D, G interval start | None new. A vessel frozen down to a residue takes holdsNothing (L38). A vessel shut on every side takes the dead-end paths (PassiveStepSolver L445, L450) | Crystal rows can be singular at an onset | More closures, but through the same paths |
| E1 rate pass | None: a failed rate solve declares no closure (option R1, SEI L273-280) | Crystal flips enter the rate pass's PassKey | None |
| E2, E5, E6 step Newton | Slight: steps start from a refreshed state. The 0.5 blocking fraction keeps KD's steep range (13x at 0.5, 200x at 0.6) out of every open line | Yes: the onset kink and the plateaus sit inside steps | None: the Newton does not see size |
| E3 vapour floor | None: the floor is 1e-6 Pa (HydrocarbonModel L17) | None | None |
| E4 equation gate | None | Yes: population sources inside the step | None |
| E7 active-set cycle | None. Refresh's own cycle check (L41) reports as E9 | Yes: crystal presence cycles at the frost line, like the water/vapour flips at suctions (E7 random 922 and 116 on `cf29`) | None |
| E8 substep limit | Rare: a closure restarts the step grid | More substeps on plateaus | A larger d gives more DEPOSITION closures, bounded by 2 x pipes + 1 |
| E9 other | New but rare: "Inventory equilibrium phase cycle / limit" for a vessel exactly on its frost line, damped by the 1e-8 threshold. "Solid event limit exhausted" (L321), "did not settle" (L290) and "Negative solid reconstruction" (L392) are unlikely: closures only accumulate within an interval, and no key is below the trace fraction | The same, plus population sources inside steps | Continuous sizes would grow keys until POPULATION_LIMIT closures fire. R10 snapping and onset-only sizing keep at most 7 keys per crystal |
| E10 no finite Jacobian | None: the closure is outside the island Jacobian | Yes: Jacobian probes leave the crystal data | None |
| F budget | The main cost class. An island holding any solid never rests (NetworkReduction L159). It has no soft-budget fallback (PassiveIntervalSolver L81-83; ProcessSolveServices L403-414, "HELD: soft budget; approximate fallback refused"). Refresh adds one test per cold vessel per interval | The same, plus the larger Newton | Each closure adds a segment |
| X exception | Only from an untyped exception: type the branch's IllegalStateException. The 64-key exception (L46) is unreachable | The same | Continuous sizes make L46 reachable in long-running worlds |

Expected outcome for S-refresh on pilot worlds:
- C and F carry the holds.
- The E classes stay near zero.
- D, G and X are zero.

R moves C into E10 and E2, and adds A, B, E4 and E5 to E7.

## 6. Two kinds, homogeneous vessels, transport blocking (short)

- **Two kinds, one SolidInventory.**
  - Inert records (SolidMaterialCatalog L17-48: density, heat capacity).
  - Phase-transition crystals: the same two numbers, plus their component and u0.
  - Both share the key, transport, filters, mobility, KD viscosity, codec and GUI. Only refresh exchanges a crystal's mass with its component's moles.
  - This supersedes `9aaafd4`'s CrystalInventory (immobile, at most 16 stocks) and CrystalStock (D22 (1)).
  - Density is fixed per crystal: CO2-I is about 1560 kg/m3, and Jaeger-Span varies by a few % between 150 and 216 K. The moments and refresh therefore use one volume per kg.
- **Homogeneous vessels.** Main is already well mixed (section 2). Consequences:
  - A liquid draw carries the suspension whatever the particle size.
  - A gas port never carries solids (L89).
  - A vessel's whole suspension is shut in when its largest active size class fails an outlet's check.
- **Transport blocking (D22 (4)).** Decided per slice before the solve, following InletPhase's pattern: `decide` (L217) reads the committed modes (L222), with hysteresis like DRY_REFUSE / DRY_RESUME (L56-58).
  - For each connection carrying a clear stream to a lower pressure, run one PH flash of the drawn stream at the receiving end's pressure.
  - If a crystal above 1e-8 appears, the connection is refused under its own appended mode. It stays refused until the flash is crystal-free.
  - A stream that already carries active crystals passes, and its crystals grow at the receiver (OWNER DECISION 5).
  - Solver effect: a refused connection is a closed boundary (refusedInlets, PassiveStepSolver L439), with no Newton change. Cost is one PH flash per carrying connection per slice, in crystal packages only. Its domain failures are typed (class C).

## 7. Cheapest measurement under the lean rule

1. **Reference, once.** Run the 200-seed quick check (random and world defaults) on the port branch's head before the solid system. It is the reference for step 2; `cl18` is from 0.7.0.
2. **Bundled identity.** Run the same quick check on S-refresh. The class tables and the campaign's counters (Newton iterations, solves, Jacobian builds) should be identical; any difference is a gating defect.
3. **Pilot quick check.** A new distribution is needed: today's fuzz draws water, nitrogen, methane and crude at 250 to 400 K (HoldFuzzTest L95-102), so no crystal can form.
   - Add a `-PholdFuzzPilot=true` option to the detached tool `tools/phase-ports-probes/hold-fuzz`. It uses the pilot cryogenic package with sources inside its data:
     - LNG-like liquid at 110 to 170 K with 0.5 to 5 mol % CO2;
     - CO2-rich gas at 220 to 300 K and 1 to 6 MPa, let down into tanks at 0.1 to 0.5 MPa;
     - nitrogen voids, valves and filters.
   - Run 200 seeds on S-refresh.
   - Only if C or F holds there come from the closure, run S-substep on the same seeds (form 2).
   - Run R only after an owner decision reopens D18 (form 3).
4. **One full campaign for the chosen form.** 1000 seeds of bundled random, world defaults and pilot, on 8 workers (2 to 4 minutes each per the tool README).
   - The pilot has no baseline, so its holds are listed by class.
   - Only C and F holds are replayed alone, in one batched run.
   - The two smallest seeds of the largest class are minimized and traced.
5. **Unit gates.** Each is proven by running that one test with its rule off:
   - phi to size, with R10 snapping;
   - proportional growth keeps the keys;
   - at most 7 keys per crystal, and POPULATION_LIMIT never fires;
   - the slush blocks at 0.5;
   - a freezing let-down refuses, then resumes at zero;
   - a package without crystals leaves refresh unchanged.
6. **One final `gates.sh`.** The suites, chain-100, the junction lines and the GameTests all run inside it.

## 8. OWNER DECISION items

1. **phi for one liquid and for gas.** Every pilot onset is one of these, since the pilot has no second liquid.
   - (a) The crystal component's volume share of the liquids, and 0 from gas. Recommended: one formula, no need to identify which phase freezes.
   - (b) The freezing liquid's own fraction, with a single liquid counted as continuous. Every solute would then get the slush size.
2. **d_dilute (reference mixing).** The required velocity for CO2-I in LNG over phi from 0 to 0.5:
   - 0.1 mm (the precipitation scale, research (b)): 0.33 to 1.5 m/s, so it mostly flows;
   - 0.25 mm: 1.0 to 2.8 m/s;
   - 1.3 mm (pipe Hinze, research (e)): 3.2 to 6.7 m/s, so it mostly settles out.
   - Ice in oil needs at most 1.3 m/s in all three cases. Recommended: 0.25 mm.
3. **Slush blocking at 0.5.** Either for all solids (recommended: one rule, and it keeps inert slurries out of KD's steep range too; the slurry tests between 0.5 and 0.62 would be restated) or for crystals only.
4. **Deposition law.** Keep main's 10 x Ferguson-Church (recommended: tested, no new parameter), or adopt Brauner's criterion, which adds the pipe diameter through the friction factor.
5. **Growth against nucleation in transport.** Recommended: a clear stream blocks, and a stream carrying crystals passes and its crystals grow.
6. **Islands holding solids never rest and have no soft-budget fallback.** Recommended: accept this for now. Letting unchanged solids rest is a later item.
7. **Generators feed no crystals; inert solids only.** Recommended.
8. **R stays rejected (D18).** Reopen it only if the refresh's lag shows in play.
9. **Size keys snapped to R10** (recommended), or classes of phi.
