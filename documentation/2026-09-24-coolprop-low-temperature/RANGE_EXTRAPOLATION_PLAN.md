# Range extrapolation and ice Ih: plan (2026-10-16)

Batch `2026-09-24-coolprop-low-temperature`, branch `claude/unified-thermo-port` at `1b75ce08`. Owner direction of 2026-10-16: leaving any data range (low or high T or P, any property model) must not hold an island; properties extrapolate and the simulation continues. Owner answer to Q5: ice Ih is modelled now, as a crystal (C). This supersedes M4c item 1 and Q1 to Q3. **Status: approved 2026-10-16, all options A ("ok keep things lightweight")**; in progress. Recommended options are marked **A**; "(probe)" numbers come from `tools/unified-thermo-port-extrapolation-plan/` (one javac lane run at `1b75ce08`).

## 1. Physics first
- A material does not change behaviour at the end of a data table. Outside it a model's error grows with the distance, but the mass, energy and flow balances stay right while properties stay smooth and bounded. The expected steady states are the in-range ones, continued; a held island is the one wrong answer.
- Hydrocarbons below their triple points stay subcooled liquids (no methane or ethane crystal is modelled; AGENTS.md rule of 2026-10-16). PR78 liquids are smooth to 30 K (probe): methane 459.6 kg/m3 at 90.69 K, 481.7 at 70 K; LNG (0.90 CH4, 0.05 C2H6, 0.0495 N2, 0.0005 CO2) 461.4 at 111.6 K, 512.7 at 70 K.
- An ideal gas below its fit tends to the rigid-rotor cp (4R for CH4 and H2O) as vibrations freeze, so cp held at the fit's end errs by at most its vibrational part there (bundled methane: 35.4 held, about 33.3 J/mol K at 120 K). Gas viscosity goes as a power of T; liquid viscosity rises as exp(b/T) and ends in a glass (1e12 Pa s).
- Water freezes below its melting line T_m = 273.16 K - 7.43e-8 K/Pa (P - 611.657 Pa): Clapeyron with dv_fus +1.63 cm3/mol and dH_fus 6.007 kJ/mol, -0.0074 K/bar, 272.42 K at 10 MPa; IAPWS R14-08 agrees within 0.012 K up to 8.7 MPa (probe). Below 273.16 K its vapour deposits as frost above the ice sublimation pressure p_sub (R14-08). Water and hydrocarbons stay immiscible.
- Humid gas: nitrogen at 1 bar and 20 °C, 50 % RH (p_w 1170 Pa, dew point 9.3 °C), cooled to -10 °C, dews from 9.3 °C, freezes at 0 °C, then frosts. At -10 °C it keeps p_sub = 260 Pa (a supercooled liquid would keep 287 Pa, probe), so 0.91 % of its moles, 78 % of its water, leave as ice.
- Water into LNG at 111.6 K: p_sub = 5.6e-12 Pa (probe), so all of it freezes. From 20 °C it gives up 1.5 + 6.0 + 4.5 = 12.0 kJ/mol (liquid, fusion, ice to 111.6 K) and boils 1.46 mol of methane (8.19 kJ/mol): 1.3 kg of LNG per kg of water.

## 2. Survey: range holds (in scope) and capability holds (listed only); paths under `src/main/java/com/wormzjl/createcheme/`
| Place (file line) | Range guarded | Becomes |
|---|---|---|
| `science/fluid/thermo/FluidDomain` range 85, check 95, checkTotals 106 (wet/dry 109), checkPhaseTemperature 119, envelope 179; `vaporMinimumTemperature`/`requireSplit` 38, 97 | package envelope; each component's `fluid_domain` (pilot triple points; CO2 216.592 and 90 K; bundled records 293.15 K) | data-range reader giving notices (section 5) |
| `science/fluid/network/PassiveStepSolver` seedAt 1585, fixedPointStarts 1370, balancedBoundarySeed 1303 (clips 1336, 1417, 1587) | seed windows clipped to envelope and ranges; one empty window stops a round | the state's own window ±20 K inside the numerical box |
| `science/thermo/phase/PhaseDomain` of 53, window 126; `FluidTpEquilibrium` crystalRange 627, searchWindow 1185 and 1240, 3792, 3916 | engine windows, FLUID_ONLY's contract domain, OUT_OF_DOMAIN | numerical box; CONVERGED with coverage EXTRAPOLATED |
| `TranslatedPengRobinson` 416, `CubicPhaseEvaluator` 228, `science/material/ReferenceSpine` 166, `NetworkPhaseEngine` 135, 187 | ideal-gas segments and fits: methane spine top 1200 K (D13), bundled 293.15..900 K, steam 273.16..900 K | cp held at the ends |
| `science/material/ViscosityCorrelation` 64; `MaterialCatalog` 427-432, 529-533 (load time: domain inside viscosity data, components and water inside the envelope) | viscosity tables | continued; load-time rules deleted |
| `science/fluid/thermo/WaterRegion1` 63, 78, 93, 164; `science/column/v3/thermo/V3WaterProperties` 181, 188 | water 273.16..623.15 K, p_sat - 2 MPa..100 MPa, steam viscosity 273.16..900 K (D8, D12) | held or continued; ice below T_m (WP4) |
| `FluidTpEquilibrium.steamViolation` 5009 (`FluidThermodynamics.waterVaporPressureLimit` 542) | ideal steam qualified to 0.125..2 MPa partial pressure | notice |
| `science/fluid/thermo/HydrocarbonModel` 173 | 10 MPa ceiling | notice |
| `science/material/CrystalReference` 139, `JaegerSpanGibbsFunction` (CO2-I 80..300 K) | crystal data | held ends; the 300 K competition ceiling stays (existence, not data) |
| `runtime/fluid/IslandCoordinator` 596-602, `FluidWorldAuthority.domainHeld` 358, `ThermoDomainViolation`, `ThermoHold` | parking and the held line of range holds | parking for phase holds only |

Capability holds, unchanged: `UnsupportedPhases` LIQUID_LIQUID, THREE_PHASES (with UNRESOLVED_PRODUCT), CRYSTAL_PHASES, CRYSTAL_STEP and `CriticalBandHold` (`FluidThermodynamics` 214-242, 1028-1054), active-set cycles, substep limits, species outside the package, hydrate requests and aqueous chemistry (`FluidTpEquilibrium` 4004, 4010, 4016). D8's wet exclusions go in WP4.

## 3. Continuation per model (deterministic: DeterministicMath, StrictMath/fdlibm)
| Model | Continuation | C1 | Evidence (probe) |
|---|---|---|---|
| PR78 kernel, E-PPR78 kij(T) (no range), translation | none, already open | yes | liquids to 30 K; methane vapour at 2401..5000 K: Z 1.0015..1.0007, residual cp at most 0.005 J/mol K |
| Ideal-gas spines, `shifted_polynomial_5` (and `below`), Shomate steam | cp held at the end value; h and s integrate it | yes | the methane quintic continued rises (36.6 J/mol K at 120 K, by hand) |
| Liquid viscosity tables | below: ln mu linear in 1/T with the end segment's slope (Andrade); above: end value held (C0, like the table's own nodes) | below | tables ending at Tc have end slopes to 4e6 K and underflow to 0 when continued; CO2 4.1e-3 Pa s at 90 K, 1.8e-2 at 60 K |
| Vapour viscosity tables | ln mu linear in ln T with the end slope (n 0.66..1.23) | yes | bundled methane 1.10e-5 Pa s at 293.15 K, 5.1e-6 at 117 K |
| IF97 Region 1, SR6 viscosity | every property held at 273.16 K (cp 76.0 J/mol K, v, dv/dT, dv/dP; mu 1.79e-3 Pa s) and at 623.15 K; pressure bounds give notices | h, v | IF97's own sums pass its check to 100 K but diverge below about 230 K (cp 911 J/mol K, 664 kg/m3 at 200 K); held h within 41 J/mol of IF97 at 250 K; SR6 continued: 5.3 Pa s at 200 K |
| Wagner-Pruss saturation | its own formula below 273.16 K (the supercooled liquid); above Tc water is steam | yes | 1.10 times p_sub at 263 K, the measured ratio |
| Steam viscosity (IAPWS dilute) | formula above 900 K; below 273.16 K mu ∝ T^n with its slope there | yes | pole near 134.5 K |
| Crystal records (segments, Jaeger-Span) | cp, expansion, compressibility held at the range ends | yes | - |

## 4. Numerical guards that stay (trial rejection and backtracking, never a hold)
- T = 350 exp(x) and P = 1e5 exp(y) are positive by construction. One numerical box, 10..6000 K and 1 Pa..1 GPa (the engine's SEARCH_MINIMUM/MAXIMUM_PRESSURE): a trial outside it is rejected and the step backtracks. Distance from play: LN2 at 63 K is 6 times the floor, the CEA spines reach 6000 K, 1 GPa is 100 times the envelope. A committed state outside the box is a solver defect and stays a numerical hold.
- Kept rejections: PR's LiquidRootAbsent, the vapour partial-pressure floor (`HydrocarbonModel` 172), IF97's fill check (it misses the divergence above, hence the hold at 273.16 K).
- Viscosity capped at 1e12 Pa s in log space: the heaviest cut (6.8e4 Pa s at 293 K) would overflow a double below about 24 K.
- PR alpha past its zero (methane 2401 K): **A** no clamp, the probe shows nothing to guard; B alpha = 0 beyond, C1 since alpha = u².

## 5. Presentation
- The worker reads the data ranges once per committed interval, on the end state: one notice per component, property and side, e.g. "outside data range: CO2 liquid viscosity extrapolated below 90 K at node 7 (+2 more)".
- **A**: the notice rides the island's status line, published with the scheduled snapshot (~5 s) and replayed on GUI open: no tick work, no solve on read, no new packet; the checkpoint already round-trips the status text. An advisory line, not HELD. B: its own snapshot field.
- Device inputs are still checked against the package envelope when the player sets them: an input box, not a hold.

## 6. WP4: ice Ih as a crystal (owner answer C)
- **Model, A**: a segments record `crystals/ice_ih.json` (existing kind, no new evaluator), anchored to IF97 at the triple point: 273.16 K, 611.657 Pa, dH_fus 6007 J/mol; 19.65 cm3/mol (916.7 kg/m3), expansion 1.6e-4 /K, compressibility 1.2e-10 /Pa; cp segments fitted to R10-06 (37.8 J/mol K at 273.16 K, about 16 at 100 K). Sources: IAPWS R10-06(2009) (Feistel and Wagner 2006) and R14-08(2011), free releases cited in the record (non-commercial project). B: R10-06's Gibbs function as a new kind (exact, about a day more; A can match it within 1 % in h).
- **Engine**: ice is decided by water's own rule, not by the hydrocarbon crystal competition, so no CRYSTAL_PHASES pair arises beside CO2-I. TP: free water below T_m(P) is ice, with steam over it at p_sub(T) (three-term R14-08 equation). PH and UV: the freeze step at T_m(P) is answered from the balance, in the form of the water-boiling step (`FluidTpEquilibrium.waterBoiling` 2273). Mixed nodes carry ice beside LNG, gas and CO2-I.
- **Network (D23 pattern)**: ice is a solid record, one size per solid by D23's rule, its mass changed only at a refresh and precipitated only in reservoirs; pipes carry supercooled water as the stand-in; slush blocks at 0.5. The stand-in is free water held at its 273.16 K values (section 3), smooth and bounded at any temperature.
- **Stand-in energy offset** (F3's form; CO2 there: 10 K): per mol of ice, h(stand-in) - h(ice) = 6007 J/mol - integral of (76.0 - cp_ice) dT: +5.6 kJ/mol at 263 K, -1.8 kJ/mol at 111.6 K (sign change near 145 K). The humid gas of section 1 runs 1.8 K colder than its refresh; in LNG the stand-in boils 15 % too much methane between refreshes (13.8 against 12.0 kJ/mol) and the refresh recondenses 0.22 mol per mol of ice. B: ice's cp below 273.16 K in the stand-in keeps the offset at +6.0 kJ/mol, with a cp kink at 273.16 K.
- **Changes**: D8's hold and its tests go (WaterIntoCryogenicMethaneTest becomes the ice gate; PumpCavitationTest's flash below 611 Pa now freezes, as vacuum freezing does; G3F9's water rows). Deleted: `VesselCrystals` 55 (free water holds no crystal), the water exclusion in `FluidThermodynamics.crystalsCompete` 994 and the wet-crystal refusal `FluidTpEquilibrium` 4021-4025. Water record revision bump: every package gains ice, the bundled ones too; fresh world. Test-world row 10 (R1 + R10, `rows/10-R1.json`: water at 1 MPa into a 120 K methane tank) expects "ice forms" and boil-off; in bundled worlds water below 0 °C freezes instead of holding.
- **Effort** L (3 to 4 days), after WP1 and WP2.

## 7. What this deletes
- Q1 to Q3 collapse: there is no floor to choose. Gone: per-component floors as bounds (`fluid_domain` becomes the notice threshold), `vaporMinimumTemperature`, `requireSplit`, `checkTotals`' wet/dry branch, `crystalRange`'s floor lowering, FLUID_ONLY's range clip, the engine's range OUT_OF_DOMAIN, `ThermoDomainViolation` and its 12 construction sites (`ThermoHold` then permits phase holds only), parking for range holds, the catalog's load-time range rules, the seeds' empty-window stop, and D8.

## 8. Effects and measurement (lean)
- Tests: 27 files pin thermo-domain holds or OUT_OF_DOMAIN (182 references: ThermoDomainViolationTest 31, FluidTpEquilibriumPhUvTest 15, GasSolidEquilibriumTest 14, G3F9DomainRefusalTest 12, FluidTpEquilibriumTest 11, IslandCoordinatorTest 7, ...). They become continuation assertions (value and slope at each end, notice text, EXTRAPOLATED grade); parking tests move to a phase hold; G3F7's 216.592 K contract assertion goes.
- Test world: row 10 (R1 + R10) freezes; D23-1's F1 (CO2 below 90 K) and F5 (methane below 90.69 K) run with a notice; acceptAtFence moves to a phase-hold row; bisect run 6's bundled cold rows run.
- Quick checks once, form A only: cold and bundled, 200 seeds. Expected: class C (`HoldFuzzTest` 293, prefix "HELD (thermo domain): ") keeps only phase holds; no new class; counters rise by the islands that used to park; replays stay bitwise (DeterministicMathSourceTest covers the new code). Then M5's single campaign (1000 seeds, both distributions, 8 workers).

## 9. Work packages and owner decisions
| WP | Content | Size |
|---|---|---|
| 1 | continuations in the model classes (section 3), the numerical box | M |
| 2 | FluidDomain to a data-range reader, ThermoDomainViolation deleted, engine EXTRAPOLATED, seeds, catalog rules | M-L |
| 3 | notice on the status line, one MCP GUI check | S |
| 4 | ice Ih (section 6) | L |
| 5 | tests and test world, quick checks once, then M5's campaign | M |

Order 1, 2, 3, 4, 5; no measurement before 4; S is about half a day, M one to two days, L three to four. Decisions, **A** recommended in each: (1) liquid viscosity below a table: A Andrade end slope, B hold; (2) ideal-gas cp: A held, B formula continued; (3) water stand-in: A held at 273.16 K, B ice cp below it; (4) ice record: A segments from R10-06, B its Gibbs function; (5) melting line: A Clapeyron from the record, B R14-08's equation; (6) hydrates: A declared not modelled, ice and CO2 frost coexist, B wet states fluid-only for CO2-I; (7) notice: A status line, B own field; (8) box: A 10..6000 K and 1 Pa..1 GPa; (9) input box: A package envelope for device inputs, B none; (10) PR alpha: A no clamp, B clamp at 0.
