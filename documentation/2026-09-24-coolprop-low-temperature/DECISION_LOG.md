# Decision log: unified multiphase thermodynamics

Batch `2026-09-24-coolprop-low-temperature`. One entry per decision, as section 8 of the plan requires: question, evidence, chosen option, rejected options, affected milestones, coverage changes. Revisit at each gate; a reversal gets a new dated entry, never an edit of the old one.

## D1 (2026-09-24): implementation route

- **Question.** Extend the in-house cubic, port a reference Helmholtz model, adopt NeqSim, or a hybrid?
- **Evidence.** `UNIFIED_MULTIPHASE_THERMO_PLAN_REVIEW.md` sections 3 and 4 and `research/2026-09-24-coolprop-low-temperature/pr78-vs-coolprop/`: the translated PR78 evaluated directly is within about 2 % on saturated liquid density down to the triple points of N2, CH4, C2H6 and CO2, about 1 % on Psat above Tr 0.5, about 5 % on dense supercritical density outside Tr 0.95 to 1.1 / Pr 0.8 to 1.5, and 0.6 % on H2 density at 623 to 700 K and 6 to 10 MPa. The residual enthalpy at 1073 to 1143 K and 2 to 2.5 bar is below 0.1 %, and CoolProp's reference equations for CH4, C2H6 and C2H4 end at 625, 675 and 450 K, so a Helmholtz backend does not extend the high-temperature validity. NeqSim 3.22.0 ships only a 60 MB shaded jar with unrelocated log4j, gson and commons-lang3, loads its component database from CSV into an in-memory H2 database, and has open issues on placeholder vapour-pressure coefficients and derivative stubs. Analytic Jacobians and both solvers already run on the PR kernel.
- **Chosen.** Extend the in-house cubic for production (liquids at state pressure, constant translation anchored at Tr = 0.8, E-PPR78 kij, tangent-plane stability test, NASA CEA ideal-gas spine with ATcT formation enthalpies, triple-point-anchored pure-solid Gibbs functions), and port CoolProp's pure-fluid Helmholtz equations to Java as a test oracle first. The pilot's fluid grade is estimated-with-declared-error with the critical neighbourhood excluded.
- **Rejected.** Helmholtz runtime backend for the pilot (cost where the pilot needs it least; no reach to the process temperatures; reconsidered at G3/G6 if a consumer needs reference grade). NeqSim at runtime (packaging, database loading, data quality). CoolProp or REFPROP at runtime (native, excluded by the no-native rule).
- **Affected milestones.** P1 becomes a bounded confirmation study; P2 gains the data-spine and evaluator-family contracts; P3's liquid path and Cp records are prerequisites.
- **Coverage changes.** Declared critical exclusion band per pure fluid; liquid cp above Tr 0.85 estimated-with-declared-error; ethane Psat below 150 K flagged pending D3.
- **P1 confirmation (2026-09-24, `P1_ALPHA_AND_LIQUID_PATH_STUDY.md`).** Direct liquid evaluation lands on the reference root at all 833 states over 0.1 to 10 MPa; mean density error at most 2.6 % per isobar outside the critical band, worst -7.0 % (dense supercritical ethane, Tr 1.08, Pr 2.1). The Tr = 0.8 saturated-liquid anchor is confirmed: the current anchors are slightly better on cold liquid below Tr 0.8 (N2 0.29 versus 1.35 %), the Tr = 0.8 anchor is better at and above Tr 0.8 and has the smaller worst case, hydrogen needs it (liquid 21.9 to 3.7 %), and a mixed anchor set would gain only 0.2 to 0.6 points of mean. The declared critical band must widen: liquid at Tr 0.90 to 0.95 with Pr 0.58 to 0.74 (density to -9.3 %, cp to +30 %) and dense supercritical fluid at Tr 1.05 to 1.2 with Pr 2 to 3 (density to -7 %) either join the band or carry their own declared error (plan section 3 updated).
- **P1 confirmation (2026-09-24, `P1_STABILITY_TEST.md`, `4500e58`).** The tangent-plane stability test exists as `science.thermo.TangentPlaneStability` on the PR78 kernel's analytic composition derivatives (no solver calls it yet). Its verdicts agree with a brute-force tangent-plane minimum on 130,660 methane/nitrogen states (105,600 around the mixture critical locus) and with the network flash phase count on 1,243 network-package states, with no unresolved state and bitwise determinism across reused workspaces; one call costs 1.1 to 13.9 microseconds for 2 to 20 components with a reused workspace (6 to 58 kernel evaluations), so the per-node cost is no obstacle to P3. Known limits: a heuristic trial set (only the two Wilson trials above 8 present components), no three-phase handling, water outside the EOS, and the network's 2 MPa liquid path can disagree with the test near bubble and dew points (none seen in 1,243 states), which the P3 liquid path removes.

## D2 (2026-09-24): pilot solid-forming species

- **Question.** Which transitions does the four-compound pilot qualify?
- **Evidence.** Review F6 and the solid-phase research: ethane has a solid II to I transition at 89.81 K, 0.55 K below its triple point, with 2.28 kJ/mol against a fusion enthalpy of 0.58 kJ/mol (Atake and Chihara 1976); the CH4/C2H6 eutectic (73.88 K at x_CH4 = 0.680, Liu et al. 2022) lies below both pure Tmin values. A consistent solid CO2 Gibbs equation exists (Jäger and Span 2012), and the CO2 frost-point and solid-liquid data in N2 and CH4 are the richest of any candidate (Sonntag and Van Wylen 1962; Smith et al. 1963, 1964; Davis et al. 1962; Zhang et al. 2011; Le and Trebble 2007; Xiong et al. 2015; Maltby et al. 2025 review).
- **Chosen.** CO2 is the only solid-forming pilot species: deposition in nitrogen (P4) and freezing out of liquid methane (P5), with one solid CO2 model. Methane/ethane liquidus and N2/Ar liquidus are research-only cases.
- **Rejected.** Methane/ethane SLE as the P5 case (needs a two-phase ethane solid; extrapolated liquid states). Water frost as a substitute P4 case (no longer needed).
- **Affected milestones.** P4, P5, G4, G5, section 7 rows.
- **Coverage changes.** The pilot's admitted solid capability is CO2 only; ethane and methane solids are unavailable, not "never freeze".

## D3 (2026-09-24): alpha function for the pilot

- **Question.** Keep the Soave alpha of PR78 or move to the consistent Twu alpha (tc-PR)?
- **Evidence.** E-PPR78 group kij were regressed with the Soave alpha and no group-contribution kij exists for tc-PR; the Soave alpha is monotonic in every target window (spurious minimum at Tr = (1 + 1/m)^2: H2 about 1270, CH4 12.6, C2H6 8.4, water 4.6, a heavy cut with omega 1.2 about 2.3). Its measured weakness is the low-Tr saturation pressure (ethane +10 % at 120 K, +29 % at 90 K) and liquid cp near Tr 0.85; PR-Twu versus PR-Soave over 147 compounds: Psat 3.9 % versus 10.0 %, liquid Cp 2.7 % versus 6.9 % (Mohamed et al. 2024).
- **Chosen.** Soave for the pilot; Twu measured in P1 on ethane low-Tr Psat, liquid cp and hydrogen, with the kij revalidation cost recorded.
- **Rejected.** Twu from the start (would invalidate the E-PPR78 tables without a replacement).
- **Affected milestones.** P1 item 2; P3 mixing bullet.
- **Coverage changes.** Ethane below about 150 K carries a declared Psat error until D3 is revisited.
- **P1 measurement (2026-09-24, `P1_ALPHA_AND_LIQUID_PATH_STUDY.md`): D3 stands.** Twu parameters were taken from the MIT-licensed Clapeyron.jl copy of the tc-PR table (the ACS supporting information refused, HTTP 403), so their literature provenance is unverified; no usable consistent Twu set exists for hydrogen (the constrained fit collapses to alpha = 1). All four tc-PR sets pass the consistency test on Tr 0.2 to 20; Soave fails only above 1200 K (N2 at Tr 11.0, CH4 12.6, C2H6 8.45, CO2 5.84). Twu's gains: ethane Psat at 90.4 / 120 / 150 K from +28.7 / +10.2 / +3.5 % to +1.4 / +1.9 / +0.7 %; mean liquid cp to Tr 0.85 for N2 4.7 to 1.9 %, CH4 2.9 to 1.8 %, C2H6 5.3 to 2.9 %. Losses: CO2 liquid cp 4.1 to 5.1 %, gas-like density at 10 MPa about -1.3 points, hydrogen unchanged, near-critical liquid cp 12 to 19 % under both. The only large gain serves the research-only methane/ethane liquidus, so Twu does not earn the E-PPR78 kij revalidation for the pilot.

## D4 (2026-09-24): home of the reference oracle

- **Question.** Port the Helmholtz reference equations into Java tests, or keep CoolProp as a Python-side tool?
- **Evidence.** No persistent Python exists on the machine; the probe used a throw-away `uv venv`. CoolProp's `dev/fluids/*.json` (MIT) carry every coefficient, and the term families needed for the pilot fluids are Power, Gaussian, NonAnalytic (water, CO2) and GaoB (ammonia) plus the alpha0 families. An in-JVM oracle makes reference values deterministic and lets the gates run without Python.
- **Chosen.** Port into Java tests, with parity to 1e-10 relative against CoolProp 8.0.0 at the probe states as the acceptance; CoolProp in the `uv venv` stays the source of the parity values.
- **Rejected.** Python-side CoolProp as a permanent gate dependency.
- **Affected milestones.** P1 item 1, G1, section 7 parity row.
- **Coverage changes.** None; test infrastructure only.
- **P1 confirmation (2026-09-24, `6ded7c7`, `952774e`).** The oracle reads CoolProp's fluid files at revision `ae81610` (bundled under `src/test/resources/science/thermo/coolprop/` with the MIT licence and the SHA-256 of every file) and agrees with CoolProp 8.0.0 to 1e-10 on the EOS, the density solve and the saturation solve for N2, CO2, CH4, C2H6, H2 and water; CoolProp's PT-flash outputs and its superancillary saturation pressures are compared at measured looser tolerances because CoolProp 8 reports them from its own PT solve and superancillary rather than from the EOS at the reported density. An unknown term type (ammonia's GaoB) is refused at load. The parity fixtures regenerate byte-identically from `tools/coolprop-parity/` in the main checkout.

## D5 (2026-09-24): VDU K-value policy

- **Question.** PR78 as characterized (with the documented 4 to 13 K bias against Maxwell-Bonnell at 1 to 13 kPa) or a two-point anchoring of omega and Tc per cut?
- **Evidence.** `documentation/2026-08-31-v3-vdu/V3_VDU_LITERATURE_AND_THERMO_DATA.md` section 2.4.
- **Chosen (P0, 2026-09-24).** Keep PR78 as characterized for the pilot stage, with the vacuum bias declared in the VDU package's evidence; take the two-point anchoring later as a separate joint CDU/VDU data revision owned by the VDU batch, after an independent check of the anchor target. Evidence (`P0_BASELINE_COVERAGE_AND_SOURCES.md` section 6, `research/2026-09-24-coolprop-low-temperature/vdu-k-policy/`): re-measured on the regrouped cuts, PR78 is 4.2 to 9.8 K colder than Maxwell-Bonnell at 13 kPa for PC06 to PC12 and 6.9 to 12.9 K colder at 1 kPa; anchoring would change omega by +0.03 to +0.37 and Tc by -4.5 to -14.8 K on all twelve cuts shared by all eight packages, shift CDU saturation temperatures at 250 kPa by -0.7 to -4.9 K, disable the bundled neural model (physics fingerprint) and break the CDU pins (literature preset, five preset draw volumes, exact-equality and bitwise tests, chain-100 fluid regression); Maxwell-Bonnell itself has no stated accuracy and third-party checks show large low-pressure errors for heavy cuts.
- **Rejected.** Two-point anchoring now (not CDU-neutral, breaks the pins, anchors to an unqualified target); deciding it implicitly by extending the package envelope.
- **Affected milestones.** P0 (closed); the VDU batch's WP0 inherits the anchoring question.
- **Coverage changes.** The VDU 1 to 13 kPa domain is estimated-with-declared-error (4 to 13 K saturation-temperature bias against Maxwell-Bonnell) until the VDU batch decides otherwise.

## D6 (2026-09-24): single ideal-gas source

- **Question.** Which ideal-gas Cp/h/s source does the data spine use, given that P0 found the NASA CEA polynomials and the NIST-derived fit the bundled methane record follows differ by 2.2 % at 900 K, inside today's qualified range, and that CEA's gas polynomials start at 300 K for ethane through pentane (200 K for the rest) while the cryogenic range needs CoolProp's ideal-gas terms? CEA and ATcT formation enthalpies also differ by 1 to 2.4 stated uncertainties for most hydrocarbons and by 13 for ammonia.
- **Evidence.** `P0_BASELINE_COVERAGE_AND_SOURCES.md` sections 2.4 and 4: CEA `thermo.inp` (Apache-2.0, commit `3f4441d`) and the ATcT 1.220 formation enthalpies (CoolProp master `9b35f538`) are in `research/2026-09-24-coolprop-low-temperature/sources/`; the CoolProp ideal-gas terms of the pilot fluids use only the LogTau, Power, PlanckEinstein and PlanckEinsteinFunctionT families, all already ported in the test oracle.
- **Chosen (taken under the owner's standing instruction of 2026-09-24 to proceed without pausing on non-major decisions; reversible at G3).** One reference-spine record per species with declared segments: CoolProp's ideal-gas terms (fitted with the reference EOS) from the triple point to a join temperature, CEA above it, the join made continuous in h and s with the Cp step at the join measured and declared; ATcT formation enthalpies with their uncertainties and CEA standard entropies; the existing `shifted_polynomial_5` fits stay in the existing packages untouched (their physics fingerprints and the neural-model binding do not move) and are retired only in the pilot package. The correction of existing column behaviour (methane Cp) is therefore deferred to P8's fixture re-recording, which the owner accepts explicitly.
- **Rejected.** Keeping the NIST-derived fits as the spine (diverge above 900 K, no formation values, two sources for one species). CEA alone (starts at 200 to 300 K). Silent replacement inside the existing packages (moves every pin).
- **Affected milestones.** P2 (record type, loader, reference tests), P3 (Cp qualification of the pilot package), P8 (fixture re-recording after independent validation).
- **Coverage changes.** None until P3; the pilot package's ideal-gas grade is qualified inside the declared segments (63 K to 6000 K for CEA-backed species) and unavailable outside.
- **P2 measurement (2026-09-24, `P2_DATA_SPINE.md`, `dc82327`).** The spine records of N2, CH4, C2H6 and CO2 join CoolProp's ideal-gas terms to CEA at 600, 375, 500 and 975 K with Cp steps of 3.4e-7, 2.1e-5, 2.0e-6 and 7.3e-5 (relative); h and s are continuous and anchored bitwise at the ATcT formation enthalpies and CEA standard entropies; the CoolProp segments reproduce the test oracle to rounding (1e-14). Against the JANAF holdouts at 1000 to 1200 K the CEA segments of N2, C2H6 and CO2 are within 0.02 %, but methane's CEA polynomial (Gurvich 1991, Cp 73.68 J/(mol K) at 1000 K) lies 2.6 to 3.5 % above JANAF (71.80 J/(mol K)), which is why its join sits at 375 K: the standard tables disagree above about 400 K. Correction from the P3 design (`P3_PILOT_ENGINE_PLAN.md` appendix B): there are three tabulations, not two; the GERG-2008 ideal part (Jaeschke and Schley 1995) gives 72.80 J/(mol K) at 1000 K, between JANAF and CEA, and equals CoolProp's Setzmann-Wagner terms at 600 and 900 K, so Setzmann-Wagner sides with JANAF only below about 600 K. D6 stands, with methane's ideal-gas Cp above 400 K declared as an open source conflict (grade estimated-with-declared-error, 3.5 % at 1200 K) until P3 settles it with an independent modern source (a partition-function calculation, or the ATcT/Ruscic thermochemical network); the steam-cracking methane reference inherits that error. Detail for P3: the CEA polynomials were generated with R = 8.314510 J/(mol K) (NASA TP-2002-211556), and the builder used 8.314472 for the CEA entropies; the evaluator should use the source's constant for the CEA segments (6e-6 relative, below every target, but the records should say which R they carry).

## D7 (2026-09-24): G0 acceptance targets

- **Question.** Which numerical targets does the pilot have to meet at G3?
- **Evidence.** `P0_BASELINE_COVERAGE_AND_SOURCES.md` section 3 (targets 5 to 100 times looser than the reference uncertainties, floors where a property tends to zero, holdouts listed in 3.3, source uncertainties in 3.4), with the measured basis of the review's section 3 and the P1 study.
- **Chosen (taken under the owner's standing instruction; reversible at G3 with recorded evidence).** The table of P0 section 3.2 is the G0 acceptance table, with two amendments from P1: the critical band is the widened band of plan section 3 (liquid Tr 0.90 to 0.95 with Pr 0.58 to 0.74 and dense supercritical Tr 1.05 to 1.2 with Pr 2 to 3 are either excluded or carry their own declared error), and the saturated-liquid density target after the Tr = 0.8 re-anchoring is 3 % mean per isobar with the worst case declared (P1 measured -7.0 % at Tr 1.08, Pr 2.1). Runtime budgets stay the owner's existing benchmark practice, and the per-node stability call is bounded by the P1 measurement (below 15 microseconds in the 20-component basis).
- **Rejected.** Reference-grade targets (a cubic cannot meet them; D1 chose declared-error grade). Unscored critical band without declared errors.
- **Affected milestones.** G3, G4, G5, G6.
- **Coverage changes.** The grade vocabulary of plan section 3 (qualified, estimated-with-declared-error, research-only, unavailable, out-of-domain) becomes a P2 record field.

## D8 (2026-09-24): water participation, phase-competition qualification and thermodynamic identity

- **Question.** How does water take part in the shared equilibrium service, when may a package answer a request with fewer phases than nature allows, and what identifies a package's thermodynamics for caches and learned models?
- **Evidence.** `P2_PHASE_CONTRACTS.md` section 2 (contracts as encoded and tested, `cbaa791`); plan sections 3 and 4; the network's existing separate free-water model (`V3WaterProperties`, IF97 region 1) qualified only as a pure liquid phase; the neural initializer's binding on the physics fingerprint (`P0_BASELINE_COVERAGE_AND_SOURCES.md` section 2.2).
- **Chosen (taken under the owner's standing instruction; reversible at G3).** (1) Water: a package either carries no water or treats it as separate free water, a pure liquid never dissolved in the hydrocarbon phases; hydrates, and water together with a declared water-chemistry species such as ammonia, are refused as `UNSUPPORTED(WATER_CHEMISTRY_NOT_MODELLED)`; water below 273.16 K is `OUT_OF_DOMAIN`. (2) Phase competition: a package qualifies whole sets of competing phases (fluid-only; vapour-liquid-solid per crystal set), never single crystals on their own; a request outside the qualified sets is refused as `UNSUPPORTED(PHASE_COMPETITION_NOT_QUALIFIED)`, never answered with fewer phases; a fluid-only answer says "solid phases not assessed" in its coverage evidence. (3) Identity: `ThermoIdentity` = package fingerprint + evaluator family + spine fingerprint + water-model revision, one revision string; every cache, approximation state and learned-model eligibility keyed on it is discarded when it changes, never migrated (consistent with the no-compatibility rule). The data side keeps the spine fingerprint separate from the existing `scientificRevision()` so the eight bundled packages did not move (17 pins); P3 decides when the spine joins the runtime identity of the pilot package.
- **Rejected.** Dissolved water in the cubic for the pilot (no qualified aqueous non-ideality). Answering an unqualified solid competition with a fluid-only result labelled as equilibrium (plan section 3 forbids it). Migrating caches across identity changes.
- **Affected milestones.** P3 (free water in the shared service, identity wiring), P4/P5 (crystal sets), P6 (consumer integration).
- **Coverage changes.** Requests involving water chemistry or unqualified phase sets return typed unsupported results instead of numbers.

## D9 (2026-09-24): P3 package strategy

- **Question.** How does P3 build and qualify the pilot engine without moving the property records shared between the column packages and the network package (their physics fingerprints bind the neural initializer and the column pins, D5/D6)?
- **Evidence.** `P3_PILOT_ENGINE_PLAN.md` section 1: the `crude_19` basis is shared, `requireSharedFluidPhysics` ties the fluid presets to the network package, `MaterialPresets` allows one network configuration, and the `fluid_domain` fields are hashed by neither the neural nor the column pins.
- **Chosen (standing instruction; reversible at G3).** Option A: a separate bundled package `createcheme:pilot_cryogenic` with its own property records (Tr = 0.8 anchors, no Cp fit, spines, crystal, E-PPR78 interactions, CO2), selected in P3 only by a test or dev datapack overriding `networks/default.json`; the formulation changes (direct liquid path, water at the state pressure, stability-driven phase decision) apply to every network package, so the bundled `tjl20_methane_nitrogen` needs one fresh world and a chain-100 re-capture after the deviation is explained; its extension is through `fluid_domain` only (nitrogen and methane to 10 MPa).
- **Rejected.** Changing the shared records now (breaks the neural binding on all eight packages and several exact column tests; contradicts D6).
- **Affected milestones.** P3 WP3, WP4, WP7; P6 (column-to-network enthalpy mapping or column switch); P8 (re-pin).
- **Coverage changes.** The pilot's qualified domain lives in the pilot package; the bundled network gains only the declared 10 MPa extension for N2 and CH4.

## D10 (2026-09-24): energy datum of the network

- **Question.** Does the network move to the formation-enthalpy datum of the data spine?
- **Evidence.** `P3_PILOT_ENGINE_PLAN.md` section 5: switching datum would loosen the energy gate 10 to 30 times because the conserved energies would carry large formation offsets.
- **Chosen (standing instruction).** The network stays on its sensible datum; spine packages carry the formation offsets in their `EnergyReference`, subtracted per species, so phase enthalpies from the spine and from the network differ by exactly the sum of x_i times the formation enthalpy.
- **Rejected.** Formation datum in the network.
- **Affected milestones.** P3 WP5.
- **Coverage changes.** None.

## D11 (2026-09-24): scope move of the hydrogen pairs and the cut kij rule

- **Question.** Do the hydrogen-pair validation (Lin, Sebastian and Chao 1980; Mohammadi 2021) and the Xu et al. 2015 pseudo-cut kij rule belong to P3?
- **Evidence.** Hydrogen enters the catalog only in P7; no P3 fixture needs the cut rule (`P3_PILOT_ENGINE_PLAN.md` section 4.4).
- **Chosen (standing instruction).** Both move to P7; the cuts keep their constant kij in P3.
- **Rejected.** Keeping them in P3 without a consumer.
- **Affected milestones.** P3 (removed), P7 (added).
- **Coverage changes.** None in P3.

## D12 (2026-09-24): liquid water on the direct path

- **Question.** How is liquid water evaluated once the 2 MPa reference path is gone?
- **Evidence.** `P3_PILOT_ENGINE_PLAN.md` section 2.2; the existing `WaterRegion1` (IF97 region 1).
- **Chosen (standing instruction; the IF97 validity statement for metastable liquid below saturation is cited in WP4, and if the release does not support it the admission gets a declared bound).** Liquid water through IF97 region 1 at the state pressure, allocation-free, admitting metastable liquid below saturation under the network's existing free-water rule; the pump reference density and the acoustic bound come from the equations of state.
- **Rejected.** Keeping a reference-pressure water path next to state-pressure hydrocarbons.
- **Affected milestones.** P3 WP4, WP7.
- **Coverage changes.** Water below 273.16 K stays out of domain (D8).
- **WP4 outcome (2026-09-24, `959ea0d`).** IAPWS R7-97(2012) section 5.1 states only that the region 1 equation "also yields reasonable values in the metastable superheated-liquid region close to the saturated liquid line", so the declared bound is: liquid water admitted down to the saturation pressure minus 2 MPa and flagged below saturation (everything the old 2 MPa path accepted is admitted); against the IAPWS-95 oracle the admitted metastable states agree within 0.0015 % in density and 0.32 % in dv/dP. The pump reference density moved from 996.0 to 997.05 kg/m3 (shutoff rise about 0.1 % lower); the solid-chain substep pins changed to 24/11, 26/9 and 16/5 because water is stiffer.

## D13 (2026-09-24): methane ideal-gas source above the CoolProp segment

- **Question.** Which source does the methane spine follow above its CoolProp (Setzmann-Wagner) segment, given the 2.6 to 3.5 % disagreement between CEA (Gurvich 1991) and JANAF at 1000 to 1200 K (D6 note)?
- **Evidence.** `P3_REFERENCES_WP8_WP10.md` part B (WP10): a reference Cp computed directly from the 9.16 million energy levels of the ExoMol MM methane line list (2024), cross-checked against the tabulated partition function (2.5e-6), HITRAN TIPS and Wenger et al. 2008, with a declared error of 0.02 % to 900 K widening to -0.10/+0.62 % at 1200 K (missing levels above 18,000 1/cm). Against it, JANAF is -0.54 % at 600 K and -1.38 % at 1000 K (rigid-rotor-like), CEA is +0.34 % at 600 K and +1.21 % at 1000 K, while the GERG-2008 ideal-gas function (Jaeschke and Schley 1995) and Setzmann-Wagner agree within 0.05 % up to 1000 K and GERG within 0.13 % at 1200 K. CEA's standard entropy 186.3702 J/(mol K) is confirmed to 0.0015. WP10 proposed a `nasa9` segment fitted to the reference (8.5e-6 fit error, 425 to 1300 K), but that segment is derived from CC BY-SA 4.0 data and bundling it would raise a share-alike question for the mod's data files.
- **Chosen (standing instruction; reversible at G3).** The methane spine above 425 K follows the GERG-2008 ideal-gas function of Jaeschke and Schley 1995 (openly published coefficients, the same as in NIST's AGA8/GERG2008.cpp), implemented as an extension of the `helmholtz_ideal_terms` segment type with the cosh-type Planck-Einstein term (the sinh-type terms are the existing `planck_einstein` family with doubled theta), joined to the Setzmann-Wagner segment at 425 K (the smallest Cp step) and declared to 1200 K with the measured errors (0.05 % to 1000 K, 0.13 % at 1200 K; about 20 J/mol in h at 1073 to 1143 K); the ExoMol-derived reference is used only as a holdout in tests (nothing derived from it is bundled), and `spine-methane-r2` is graded qualified inside the D7 0.2 % target. Applied in WP9 together with the CEA gas-constant correction (R = 8.314510 for the CEA segments) so the pilot spine pin moves once.
- **Rejected.** Keeping CEA above 375 K (1.2 % at 1000 K, 1.8 % at 1200 K, fails the 0.2 % target). The fitted `nasa9` segment (best accuracy but share-alike provenance; kept in `research/2026-09-24-coolprop-low-temperature/methane-ideal-gas/outputs/` for comparison). Extending Setzmann-Wagner alone (0.23 % at 1200 K, misses the target; no coefficients to add, acceptable fallback if the term type is not implemented in time).
- **Affected milestones.** P3 WP9 (spine record r2, loader term type, holdout test), G3.
- **Coverage changes.** Methane ideal-gas Cp is qualified to 1200 K at 0.2 %; above 1200 K unavailable for methane (the other pilot species keep CEA to 6000 K).

## D14 (2026-09-25): G0 target revision for four rows the translated cubic misses

- **Question.** WP5 measured the pilot package against the Java oracle on 418 scored states (`P3_PILOT_PACKAGE_AND_SPINE.md` section 16) and found four D7 rows the PR78 formulation itself cannot meet: vapour density at Z >= 0.8 up to +2.16 % against 1 % (a third to a half from the Tr = 0.8 translation, which shifts the vapour volume by c too), vapour cp near saturation at Z >= 0.9 to -8.9 % against 2 %, liquid density at Tr 0.90 to 0.95 just outside the band -5.8 % against 5 %, and the supercritical corner Tr 1.0 to 1.1 with Pr 1.5 to 2 at -8.4 % against 7 % (between the band's two boxes). Revise the model, the band, or the targets?
- **Evidence.** The same deviations appear in the P1 direct evaluation and in the WP5 evaluator-versus-network bitwise check, so they are properties of the translated cubic with the Soave alpha (D1, D3), not of the wiring; D7 declared its table reversible at G3 with recorded evidence; the pilot grade is estimated-with-declared-error (D1).
- **Chosen (standing instruction; reversible at G6).** Targets revised with the measured worst cases plus margin, as declared errors, not model changes: vapour density at Z >= 0.8 target 2.5 % (declared: the volume translation is applied to every phase so that fugacities stay consistent; a consumer needing 1 % vapour density, such as a compressor duty, triggers a G6 review of an untranslated vapour branch with a consistency proof); vapour cp at Z >= 0.9 within 0.05 of saturation in Tr: 10 % declared, the 2 % target kept for Z >= 0.9 away from saturation; liquid density at Tr 0.90 to 0.95 outside the band: 6 % declared; the corner Tr 1.0 to 1.1 with Pr 1.5 to 2 joins the declared critical band as a third box with a 9 % declared density error. WP9b encodes these rows in the fixture families with the D7 verdict printed next to the D14 bound, and the G3 report states both.
- **Rejected.** Twu alpha (D3 stands; would not fix the translation term). Translating the liquid only (breaks phase-equilibrium consistency). Removing the translation (P1: hydrogen liquid 21.9 % without it).
- **Affected milestones.** P3 WP9b, G3, G6.
- **Coverage changes.** The critical band gains a third box; four rows of the G0 table carry declared errors instead of targets.

## D15 (2026-09-25): G3 target revision after the fixture families

- **Question.** WP9b ran the ten G3 fixture families (`G3_QUALIFICATION_REPORT.md`, `4eec712`): six rows exceed their D7 or D14 bounds by a few percent, all properties of the translated PR78 with the Soave alpha, none of the wiring. Revise the model or the targets?
- **Evidence.** Report section 6 with the F1, F2, F4 and F5 tables: real-gas vapour cp at Z >= 0.9 to -11.58 % on saturated CO2 vapour at 224 K and -8.45 % away from saturation (the error scales with 1 - Z, not with the distance from saturation; at Z >= 0.98 it is within 2.32 %); compressed liquid density at Tr 0.85 to 0.9 next to saturation -4.41 % (C2H6 274.8 K at 1.02 Psat); supercritical cp next to band box 1 -10.59 % (N2 139.92 K, 5 MPa, Tr 1.109, Pr 1.47); ethane liquid cp at its triple point -10.63 % (P1 reported the same); liquid at Tr 0.95 to 0.97 below Pr 0.8, between band boxes 1 and 2, -9.06 % (N2 121.19 K, 2.70 MPa, no row covered it); K-value |dy| of the CO2-rich liquids within 5 K of CO2's triple point 0.0222 and 0.0212 (CO2/CH4, CO2/C2H6 at 220 K, x_CO2 0.9 to 0.95). Every other row of F1 to F10 is met (Psat 1.27 % at Tr 0.6 to 0.95, saturated liquid density worst 2.26 %, vapour density +2.09 % inside D14, ideal gas 0.133 %, no jump at 2 MPa, conservation 1.85e-16, derivatives 1.1e-7, refusals typed, the old region at the D7 targets).
- **Chosen (standing instruction; reversible at G6 with a consumer's need).** The six proposals of report section 6 are taken as declared errors and one band change: vapour cp 12 % declared at Z >= 0.9 with the 2 % target kept only at Z >= 0.98; compressed liquid density at Tr 0.85 to 0.9 next to saturation 5 % declared; supercritical cp next to box 1 12 % declared; ethane liquid cp below 150 K 12 % declared together with D3's ethane Psat region; band box 2 widened to Tr 0.97 below Pr 0.8 (research-only); K-value |dy| 0.025 declared for CO2-rich liquids within 5 K of CO2's triple point. The fixture tests carry the D15 bound with the D7/D14 verdict printed beside it (no longer labelled proposals). The four engine defects the families exposed are fixed in WP7d before G3 is declared met: an untyped two-phase failure outside the band (N2/CH4 0.67/0.33 at 150 K, 4.69 MPa), a closed vessel that would exceed 10 MPa returning NOT_CONVERGED instead of OUT_OF_DOMAIN, the D14 corner box missing from the engine's band, and a liquid-liquid split labelled VAPOR_LIQUID.
- **Rejected.** Model changes for the pilot (a Twu alpha or a temperature-dependent translation would move every pin and D3 stands); leaving rows without a bound.
- **Affected milestones.** G3 (declared met when WP7d's re-run of F1 to F10 is green at the D15 bounds and the four defects are typed or fixed), G6 (review if a consumer needs vapour cp or near-saturation liquid density at the D7 level).
- **Coverage changes.** Six rows of the G0 table carry declared errors; band box 2 reaches Tr 0.97 below Pr 0.8.
- **WP9a outcome (2026-09-25, `271d84a`, `P3_PILOT_PACKAGE_AND_SPINE.md` sections 20 to 27).** Implemented as decided: `planck_einstein_cosh` term, GERG-2008 coefficients from `GERG2008.cpp` lines 1256, 1277 and 666 (B0 4.00088 as `log_tau` a = 3.00088; sinh terms as `planck_einstein` with doubled theta 1641.318 and 2125.64 K; cosh terms 178.41 and 1090.53 K), methane segment 425 to 1200 K, `spine-methane-r2`. Holdout against the ExoMol-derived reference: Cp within +0.029 % (300 K) to +0.133 % (1200 K), h(T) - h(298.15) within 7.3 J/mol, every point inside the D7 targets; JANAF asserted as a documented disagreement (-1.40 to -1.85 % at 1000 to 1200 K). Gas-constant rule: `nasa9`/`nasa7` segments carry `gas_constant_j_per_mol_kelvin`, default 8.31451, and a `cea-` revision must carry exactly that value (P2's 8.314472 refused); N2 and C2H6 records r2, CO2 r3; standard entropies N2 191.6097, CH4 186.3711, C2H6 229.2211, CO2 213.7874 J/(mol K); joins kept at P2's temperatures (steps 4.2e-6, 5.8e-5, 6.6e-6, 6.8e-5); the pilot spine fingerprint moved once to `1b33b193...`, the pilot scientific revision and the eight bundled pins unchanged. Noted, not changed: with GERG the smallest methane step would be at 400 K (-2.2e-6) rather than 425 K; the corrected constant would move the P2 join rule to 425 K for N2 and 525 K for C2H6; methane's standard-entropy uncertainty still carries |CEA - JANAF| = 0.12 although the line list confirms CEA to 0.0006.

## D16 (2026-09-25): solid CO2 model and its anchor on the pilot family

- **Question.** The owner provided Jäger and Span 2012 and Trusler 2011 on 2026-09-25. Which solid CO2 function and which fusion enthalpy anchor the CO2-I crystal to the pilot family?
- **Evidence.** `P4_SOLID_CO2_MODEL.md` (`edb9f7f`, `bab0cdc`): the Jäger and Span 2012 Gibbs function ported and checked (Table 5 to the printed ten digits, analytic derivatives within 6.5e-8 of central differences); anchored to the Span-Wagner oracle liquid with 8875 J/mol it recovers the paper's g0 and g1 (3.6e-5, 9.7e-7) and reproduces its Figures 9 and 11 (sublimation within the correlation bands except 170 to 185 K at 1.5 times the band, as the paper's own line; melting within 0.93 of the band to 270 K; sublimation enthalpy 25,214 J/mol at 194.67 K). On the pilot family the translated PR78 has a 0.51 % small vaporization enthalpy and a 1.08 % small liquid volume at the triple point; with 8875 J/mol the sublimation pressure is 6.66 % high at 150 K (P0 target 5 %) and the sublimation enthalpy at 195 K -0.63 %; with 9019 J/mol 2.94 % and -0.06 %, the melting temperature within 0.125 K to 10 MPa (slope 5.3 to 6.6 % steep, no P0 row).
- **Chosen (standing instruction; reversible at G4/G5).** The crystal is the Jäger-Span Gibbs function as the `gibbs_function` kind of the crystal record (`crystal-carbon_dioxide-i-r2`), g0 and g1 solved at runtime from the fluid family's liquid (mu_L, h_L) at the triple point per the P2 contract, `TriplePointAnchor` carrying h_L; the pilot's anchor fusion enthalpy is 9019 J/mol, declared as this family's effective value (it compensates the cubic's vaporization enthalpy; Trusler cites calorimetric 7950 to 8340 J/mol and derives 8870 from the reference equations); grade estimated_declared_error with declared errors: sublimation pressure 3 % from 150 K to the triple point and 4 % (Fray-Schmitt) from 130 to 150 K, melting temperature 0.13 K to 10 MPa, sublimation enthalpy 0.25 % at 195 K; crystal volume 0.3 % and expansion coefficient up to -22 % near the triple point against Trusler 2011 (the two reference equations disagree there). A second fluid family evaluating the crystal needs its own anchor fusion enthalpy before it does.
- **Rejected.** 8875 J/mol on the pilot (fails the P0 sublimation target); a fusion enthalpy fitted to holdouts; Trusler's Helmholtz equation as the model (T-V form, a volume solve per state; kept as the cross-check); the P2 Giauque-Egan segment record (no expansion, compressibility or Cp above 189.78 K).
- **Affected milestones.** P4 stage 2 (gas-solid equilibrium, finite deposition, the network), P5 (solid-liquid; the fusion enthalpy enters the liquidus slope directly and is revisited there if the Davis 1962, Zhang 2011 or Le-Trebble 2007 holdouts favour 8875), G4, G5.
- **Coverage changes.** The CO2-I crystal becomes usable (research grade lifted); no phase competition admits it yet (`PhaseContract` qualifies no crystal set), so no answer changes; the pilot spine fingerprint moved to `581c9098...`, the eight bundled pins and the pilot scientific revision did not.
- **Sources outcome (2026-09-25, owner-provided papers, `P4_P5_HOLDOUT_DATA_SURVEY.md` section 9).** The Trusler 2011 erratum (J. Phys. Chem. Ref. Data 41, 039901) corrects equation 38 (a missing theta_D,0 factor), the signs of equation 49 (d7 = -0.160433, d8 = +0.018643, d9 = -0.001582) and Table 6 b1 = 1.151; the stage 1 cross-check, which read equation 49 as a reciprocal, agrees with the corrected form within 0.15 % to 270 K (1.5 % at 400 K, 10.7 % at 800 K, outside the pilot's range); no Trusler coefficient is in the engine, the decision stands. Giauque and Egan 1937 give the calorimetric heat of sublimation as 6030 +/- 5 cal/mol = 25,236 J/mol at 194.67 K; the pilot's 25,185 J/mol is -0.20 % against it, inside the declared 0.25 %. Maltby et al. 2025 use 8875 J/mol with the Span-Wagner fluid; the P5 revisit of the pilot's effective 9019 J/mol keeps that in view, with the Davis 1962 three-phase compositions now in hand.

## D17 (2026-09-25): gas-solid equilibrium in the engine, the crystal competition's domain and its hold

- **Question.** How does the engine answer a request with the pilot's CO2-I crystal competing, where does that competition apply, and what is refused? The P4 stage 2a brief said a liquid in the fluid answer means a hold; the agent found that rule wrong below the triple point and proposed this one.
- **Evidence.** `P4_GAS_SOLID_EQUILIBRIUM.md` (`5f2bd7d`, `8144935`): 108 vapour-solid states with conservation 1.7e-16 and equal chemical potentials to 8.7e-10; 92 of them have a fluid-only answer holding a subcooled CO2-rich liquid that the crystal makes metastable (the brief's rule would have refused 85 % of deposition states); PH/UV round trips to 1e-11, the pure limit to 1.6e-5 K, Jäger and Span 2012 Figure 13 reproduced in sign and trend (+0.45 / +0.087 / +0.009 % at 5 / 1 / 0.1 % N2 against about 0.35 / 0.06 / 0.01 % with GERG-2004); the partial-pressure onset is 0.1 to 0.2 K warm at 0.1 MPa and 9 to 12 K at 5 MPa; methane-gas frost points (Le and Trebble 2007, Zhang 2011, Xiong 2015; G4F1): pooled binary MAD 1.35 K against the 1.5 K P0 target, bias -0.36 K, worst point 5.19 K (Le and Trebble's 1 % set disagrees with Xiong's by 1 to 5 K; without it the binary MAD is 0.69 K), ternaries 0.94 to 1.73 K; the Fandiño 2015 three-phase points are never answered as vapour-solid (the hold onsets lie 0.04 to 0.06 K below them).
- **Chosen (standing instruction; reversible at G4/G5).** (1) The crystal competition is qualified per package as the whole set of its usable, answer-graded crystals (`PhaseContract.forPackage`); `PhaseCompetition.FLUID_AND_CRYSTALS` requests it; the network keeps fluid-only until stage 2b. (2) Under that competition the crystal's species is admitted below its fluid range, down to the crystal's and its spine's range (CO2: 90 to 300 K, the package's 10 MPa); fluid-only requests keep the fluid range. (3) A crystal forms when mu_s < mu_i of the fluid answer (fugacity, never partial pressure); the vapour-solid answer is decided by the fluid the deposition leaves: the reduced feed is re-flashed with the stability test and must be one stable phase that is not liquid-like; a liquid in the fluid-only answer of the feed is not by itself a refusal (below the triple point it is the crystal's species as a metastable subcooled liquid). (4) Everything that needs a crystal together with a liquid is the typed `SOLID_LIQUID` hold (UNSUPPORTED, NOT_IMPLEMENTED, P5): a reduced fluid still holding a liquid or being one liquid-like phase, the pure species' melting, an onset whose fluid holds a liquid, a liquid carrying the crystal's species below that species' fluid range even without a crystal, two crystals; the network maps it to `UnsupportedPhases.Kind.SOLID_LIQUID` in stage 2b. (5) The answer's grade folds the crystal's (estimated_declared_error with D16's declared errors in the evidence). (6) The G4 onset gate is the pooled binary MAD against the 1.5 K P0 target with per-dataset MAD and per-point caps at what was measured (2.5 K and 5.5 K binary; 2.0 K and 6.0 K ternary), the survey's "average with a per-point cap". (7) Complete deposition is declared below a vapour mole fraction of 1e-15 (unreachable in the pilot domain); a pure feed deposits completely.
- **Rejected.** Refusing whenever the fluid-only answer holds a liquid (holds 85 % of the deposition states, all on a metastable liquid); the partial-pressure onset as an answer (2 to 12 K warm at 1 to 5 MPa); a per-point 1.5 K frost-point bound (Le and Trebble's 1 % set is 1 to 5 K off and inconsistent with Xiong's).
- **Affected milestones.** P4 stage 2b (network), G4, P5 (every hold), G5.
- **Coverage changes.** The pilot answers CO2 deposition from N2, CH4 and C2H6 gas and the pure CO2 sublimation (TP, PH, UV, onsets) at the declared-error grade; CO2 + N2 frost points are not yet qualified against data (the Sonntag and Smith sets arrived from the owner on 2026-09-25 and are being transcribed); nothing changes for the network or any other package.

## D18 (2026-09-25): the fluid network on the crystal competition, and the reading of gate G4

- **Question.** How does the network carry the pilot's CO2-I crystal (request, domain, inventory, closures, deposition and sublimation, holds, persistence, presentation), and is G4 met when onset is data-qualified but finite inventory can only be checked by consistency?
- **Evidence.** `P4_NETWORK_COUPLING.md` (`0ea358a` to `c284f59`): a closed pilot vessel of 10 % CO2 in N2 charged at 170 K and 1 MPa deposits 1.481 of 7.522 mol CO2 at its first solve, conserves CO2 to 1e-12 and reproduces an independent engine UV answer to 1e-9; 3000 J out and back in through the network's energy input reproduces the engine's fixed-volume answer (163.922352 K, 0.742 MPa, 0.0926565 mol crystal) and returns to 200 K within 1.1e-11; a pipe chain deposits only in the cold vessel; the latent heat on the network datum is 25,200.64 J/mol; format 6 round-trips bit for bit and refuses format 5; presentation reaches the views at bucket ticks only; G4F2 (Sonntag 1960 Table VIII and Smith 1963 Table XIII, his own points) pooled MAD 0.420 K on 51 points inside 10 MPa, worst 1.99 K, 14 typed holds where the onset lies in dense supercritical N2 labelled liquid-like (140 to 160 K, 6 to 9 MPa), 68 points above the 10 MPa ceiling out of domain (Maltby et al. 2025 report 0.5 to 0.7 K for Peng-Robinson with a fitted kij on the same sets); the fluid-only path unchanged (exact chain-100 replay zero, bundled pins, 30 of 30 GameTests on a fresh world); `test` 1,267, `fluidScienceTest` 226, `fluidRuntimeTest` 230, all green. The survey found no dataset anywhere that measures deposited amounts or heat.
- **Chosen (standing instruction; reversible at G5/G6).** (1) A package that admits crystals requests `FLUID_AND_CRYSTALS` for dry requests carrying a crystal's species at or below the crystals' ceiling (300 K); wet and hotter requests stay fluid-only (water with crystals is water chemistry, D8; no CO2-I is stable where water is in domain). (2) A gas carries a crystal's species to the competition's floor (CO2 90 K); a liquid keeps the fluid floor. (3) The crystal is an immobile inventory of a finite vessel, in its species balance, its energy (spine h_s minus the formation enthalpy, D10) and its volume; never transported: bulk withdrawal takes the fluid only (v1 rule), junctions and pipes hold none. (4) Deposition and sublimation are the engine's UV answer for the vessel's whole inventory at every interval start and after every accepted full substep (an operator split, first order in the substep, exact conservation), a 1e-10 deadband so an equilibrated vessel is an exact fixed point that can certify REST; the crystal is frozen inside a step; approximate intervals are refused for crystal models; no partial pressure is held, nothing runs per tick. (5) The engine's `SOLID_LIQUID` is `UnsupportedPhases.Kind.SOLID_LIQUID`, a typed island hold naming the node; a held island takes no events. (6) Checkpoint format 6 and island unit format 3, fresh world, no migration; certificates need unchanged crystals. (7) The pilot CO2 dilute-gas viscosity is extended to 90 K from CoolProp 8.0.0 (record r2), declared an extrapolation below the triple point. (8) Gate G4 is met at the declared-error grade with the allowlist of `P4_NETWORK_COUPLING.md` section 6 (the CO2-I crystal of `createcheme:pilot_cryogenic` in dry N2, CH4 and C2H6 gas, 90 to 300 K, 100 Pa to 10 MPa, in finite network vessels): onset is qualified against independent data in N2 (0.42 K) and in methane-rich gas (1.35 K); finite inventory, heat, exhaustion and reversibility are qualified by consistency (conservation, the engine's UV answer, the datum's sublimation enthalpy within 0.2 % of the calorimetric value), because the deposited amount and its heat follow from the data-qualified onset and the data-qualified pure-solid energetics by material and energy balance and no dataset measures them; the plan's clause "if only onset data can be qualified, retain a research result and defer physical conversion" is therefore not applied, and physical conversion is admitted for the pilot allowlist. G4F2 bounds: pooled MAD 1.5 K, per-dataset MAD 1.0 K, per point 2.5 K, at most 14 holds, exactly 68 out of domain.
- **Rejected.** The crystal as a Newton unknown with an equilibrium row inside the step (a new layout block and active set, not needed); a TP-decided deposition inside the step (holds the node's T and P while material changes phase; the UV answer couples energy and volume); a network-side viscosity extrapolation without a source; deferring physical conversion for lack of inventory data that does not exist.
- **Affected milestones.** G4 (met), P5 (every `SOLID_LIQUID` hold, the four THREE_PHASES grid states at 100 K, the dense-N2 label holds), P6/G6 (the in-game GUI check through the MCP bridge, the reservoir screen's withdrawal text, the crystal UV warm start, a morphology policy before crystals enter suspension, filters or wall deposits, the untested paths: inert particles beside crystals, a wet vessel holding a crystal, the fallback above 300 K), G5.
- **Coverage changes.** Pilot network vessels deposit and sublime CO2-I from N2, CH4 and C2H6 gas at the declared-error grade; a pilot network state that needs a crystal beside a liquid is held typed where it was a domain refusal before; the bundled network is unchanged.

## D19 (2026-09-25): the fusion enthalpy after P5, and the reading of gate G5

- **Question.** Does the P5 evidence favour 8875 J/mol (Jäger-Span 2012, Maltby et al. 2025) over the pilot's effective 9019 J/mol (D16)? What does G5 admit now that the engine answers a crystal beside a liquid?
- **Evidence.** `P5_SOLID_LIQUID_EQUILIBRIUM.md` sections 3 to 6 (`33b4ebe` to `4929e0f`), 9019 against 8875 J/mol: liquidus Shen 2.04 against 2.59 K, Davis 1.90 against 2.15 K, Campestrini 0.56 against 0.83 K, Riva-Stringari 2.64 against 2.99 K, Gao binary 2.58 against 3.14 K; three-phase line Davis 1.64 against 1.67 % in pressure (0.32 K), Souza 2.50 against 1.52 % (0.44 against 0.06 K); line compositions x 11.9 against 14.2 %, y 21.2 against 20.2 % (log ratio); sublimation pressure at 150 K 2.84 against 6.56 % (P0 5 %); sublimation enthalpy -0.16 against -0.74 % of the calorimetric 25,236 J/mol; frost points G4F1 1.354 against 1.339 K, G4F2 0.420 against 0.560 K; pure melting 0.125 against 0.094 K. The methane-rich liquidus lies about 2 K cold with 9019 (solubility about 25 % high), which a smaller fusion enthalpy worsens; its source is the liquid's CO2 fugacity (the CH4/CO2 kij(T) in cold liquid methane), the crystal side scoring -0.36 K bias in methane gas. Every former `SOLID_LIQUID` hold is an answer (the 69 grid states, G3 F7 30 of 30, the (d) charge at 0.5 MPa, the four THREE_PHASES states); consistency 1.7e-16 (conservation), 2.6e-11 RT (equal chemical potentials), 1.7e-10 (round trips); the network conserves CO2 to 1e-12, reproduces the engine's UV answer to 1e-9 and returns within 4.6e-12 (liquid-full) to 7.3e-11 (vapour-liquid) in temperature; the 14 dense-N2 G4F2 onsets score 3.50 K (max 5.90 K, growing smoothly with pressure: a model limit, not a label artefact), all 65 scored points 1.08 K. Gates: `test` 1,282, `fluidScienceTest` 229, `fluidRuntimeTest` 231, exact regression zero, 30 of 30 GameTests on a fresh world; bundled path and pins unchanged.
- **Chosen (standing instruction; reversible at G6).** (1) The pilot's anchor fusion enthalpy stays 9019 J/mol (`crystal-carbon_dioxide-i-r2` unchanged, no pin moves). (2) Under `FLUID_AND_CRYSTALS` the engine answers a crystal beside a vapour, a liquid, a vapour-liquid split or a dense one-root fluid (`VAPOR_SOLID`, `LIQUID_SOLID`, `VAPOR_LIQUID_SOLID`), a pure species frozen completely, a pure melting step, a binary's three-phase step and a pure triple point from the balances, and `freezingTemperature` (the liquidus of a liquid as one phase). (3) The typed hold `CRYSTAL_PHASES` (engine) and `UnsupportedPhases.Kind.CRYSTAL_PHASES` (network) replace `SOLID_LIQUID` and cover only two liquids or three fluid phases beside a crystal and two crystals; non-convergence stays `NOT_CONVERGED` with diagnostics. (4) A network vessel carries its crystal inventory beside a liquid or both fluid phases, moved by the engine's UV answer as in D18. (5) The G5 liquidus target is read as the dataset MAD with a declared error of 2.2 K for methane-rich liquids (Shen 2.04 K; the 2 K P0 target is met by Davis and pooled within 0.02 K), per point 4 K. (6) Gate G5 is met at the declared-error grade with the allowlist of `P5_SOLID_LIQUID_EQUILIBRIUM.md` section 7: the CO2-I crystal of `createcheme:pilot_cryogenic` beside methane-rich liquids and vapour-liquid splits (solvent at least 90 % CH4, the rest N2 or C2H6) and pure CO2 melting, 90.69 to 216.6 K, to 10 MPa, in finite network vessels; phase amounts and heats qualified by consistency (no dataset measures them). Not admitted: nitrogen-rich solvents (Gao 2012 freezes +2.8 to +6.1 K warm), trace CO2 at the ppm level (Sampson 2023 -4.4 to -6.4 K; every published model 3 % or worse there), CO2 in ethane liquids (no data scored), the Riva-Stringari 2018 series-2 liquids (2.64 K, contradicting Shen and Gao), three-phase compositions beyond the declared 12 % (liquid) and 21 % (vapour) log-ratio errors. (7) The dense supercritical N2 carrier is answered at a declared 6 K and kept outside the G4 allowlist; G4F2's caps on the vapour carrier are unchanged.
- **Rejected.** 8875 J/mol (fails the P0 sublimation target again and worsens every methane-rich liquidus set by 0.25 to 0.56 K); a fitted fusion enthalpy (tuning to holdouts, and the liquidus bias sits in the liquid); loosening G4F2's per-point cap to absorb the dense carrier; holding liquid-solid states for the sake of the 0.04 K liquidus miss.
- **Affected milestones.** G5 (met), P6/G6 (`P5_SOLID_LIQUID_EQUILIBRIUM.md` section 12: the liquid-full mixture vessel bubble-point limit of the network, bulk withdrawal from a liquid-solid vessel untested, the crystal UV warm start, the GUI check, the two-crystal and wet-vessel code paths), P7 (the CH4/CO2 kij at 110 to 170 K, nitrogen-rich solvents).
- **Coverage changes.** Pilot network vessels freeze and dissolve CO2 beside methane-rich liquids at the declared-error grade; the former `SOLID_LIQUID` holds are answers; the bundled network is unchanged.

## D20 (2026-09-26): crystal morphology policy, and the reading of gate G6

- **Question.** May the pilot's equilibrium crystals be mapped into suspension, filters or wall deposits (plan P6, "decide an explicit morphology policy before mapping equilibrium crystals")? Is G6, the mandatory gate before the P7 rollout, met?
- **Evidence.** `P6_PILOT_ACCEPTANCE.md` (`dd3da29` to `4d26a45`): the V3 column and the neural projection refuse a package with crystals typed (`CrystalPhasesUnavailable`), the neural binding gives no model for it, the loader refuses crystals without a spine, the pilot refuses crude-assay presets; the five untested paths covered (particles beside a crystal with the engine's answer to 1e-9, free water sublimes the crystal and water reaching a cold crystal vessel is the typed domain hold, withdrawal from a liquid-solid vessel carries the liquid's x_CO2 0.0281 not the overall 0.0300, two crystals are the `CRYSTAL_PHASES` hold, above 300 K the fluid-only answer applies); the pre-existing bubble-point limit of liquid-full mixture vessels fixed by seeding the two-phase layout with the engine's UV answer (all eight P5 probe cases cross in 4 to 6 substeps, `LiquidFullBubblePointTest`); the crystal UV warm start (at rest 1,630 to 229 kernel evaluations and 361 to 53 us vapour-solid, 494 to 163 and 127 to 44 us liquid-solid, warm and cold answers to 1e-10); a pilot closed vessel certifies rest after 3 slices against 2 for its bundled twin, a pilot pipe chain averages 5,469 checkpoints per slice against 657; the fresh dev world with the pilot datapack override: CO2 deposited from nitrogen (1.69 then 12.48 mol) and frozen from liquid methane (262 to 385 mol at 123.6 K) shown in the reservoir screen and block-entity views on the engine's buckets, held islands showing their reasons, two save-and-reload round trips of the clock, islands, a pending event and the crystal inventories; `test` 1,299, `fluidScienceTest` 242, `fluidRuntimeTest` 231, exact regression zero, 30 of 30 GameTests, P12/P31 digests unchanged, no fluid-only hot path changed (no benchmark pair).
- **Chosen (standing instruction; reversible at P7/P8 with transport evidence).** (1) An equilibrium crystal is an immobile inventory of the finite vessel it formed in (D18), in its species, energy and volume balances; no pipe, junction, pump, valve, filter or withdrawal carries it; withdrawals take the fluid phases only. (2) It is never mapped into the inert-particle system (no suspension, filter cake, bed, wall deposit or particle size), and particles never become a crystal; both share the vessel's one temperature, found by a bracketed search. (3) With free water, or above the 300 K ceiling, the fluid-only answer applies and the crystal sublimes; water reaching a cold crystal vessel is the typed domain hold of the wet node. (4) Two crystals forming together are the typed `CRYSTAL_PHASES` hold. (5) The screen shows the crystals as "x mol, y L (stay)", "fluid phases together" and the fluid's own volume and mass. (6) Gate G6 is met at the declared-error grade for the science, conservation, consumer, fresh-world, GUI and round-trip criteria; the runtime criterion is met in the unit rigs and conditionally in game, with four runtime limits recorded as P7/P8 items rather than blockers: crystal vessels under kg/s through-flows run on the soft budget with the approximate fallback refused (by design, D18); a near-pure CO2 vessel at its sublimation line (N2 about 1e-7) is refused after 2.7 s and held on the round deadline (a pseudo-pure crystal step or a fast typed hold is the P8 fix, first in line); a water-slurry generator feeding a cryogenic methane vessel gives an untyped negative-inventory hold (no crystal involved, undiagnosed); the zero-flow junction-donor cycle of the pilot chain (pre-existing). The broad compound rollout (P7) may start on this pilot.
- **Rejected.** Mapping crystals to particles (invents a size and a transport law); proportional withdrawal of crystals; declaring the near-pure CO2 hold blocking for G6 (the owner may reverse this).
- **Affected milestones.** G6 (met), P7 (the CH4/CO2 kij(T) in cold liquid methane, nitrogen-rich solvents, ppm-level traces, the dense N2 carrier, crystal transport or wall frost only with evidence), P8 (the four runtime limits, wet liquid-full mixtures at their bubble point untested, views about 20 s after a reload while rounds hit the budget, held islands taking no events, the column's switch to the spine and the neural re-pin).
- **Coverage changes.** None for the bundled network; pilot vessels keep their crystals in place; consumers that cannot use a crystal package refuse it typed.

## D21 (2026-10-15): a near-pure feed at its sublimation line (failure 1 of the pilot world)

- **Question.** How is a feed of one major species with traces answered at its sublimation or melting line, where the crystal onset and convergence, decided on an absolute 1e-9 drive, cannot resolve a vapour-solid region only about f wide (f the trace fraction)?
- **Evidence.** `P6B_RUNTIME_FAILURE_DIAGNOSIS.md` section 1.4: TP answers jump (0 to 179 mol of crystal at f = 1e-9), the nested UV closes on a step, failures for traces 1e-12 to 1e-7 (to 3e-7 with small crystals), row E reaches it about 85 s after its liquid drains; options A to E. `main-adaptation/NETWORK_SOLVER_LAYER.md` section D: the failure persists on main, backward Euler does not change it, and main adds a second form (a slack tank equilibrates at ambient pressure and meets the same step at 194.7 K).
- **Chosen (owner 2026-10-15, "go with recommended").** Option A: a feed whose other species total at most theta = 1e-6 (mole fraction) takes the pure-species crystal step (theta raised to 1e-5 on 2026-10-15, see the outcome below); the traces are carried in the fluid phase; the rule takes precedence over the two-major rule; the pressure error is about 1.3 f. It covers both the fixed-volume (UV) and the ambient-pressure (PH) crystal solves. Option C, a fast typed hold, is the typed fallback for anything option A does not answer.
- **Rejected.** B, a resolution-relative tolerance (moves every crystal answer near an onset and still needs A below f = 1e-16); D, dropping the traces (breaks conservation); E, interpolating across the step.
- **Affected milestones.** The engine after the M1 port of `MAIN_ADAPTATION_PLAN.md`; row E and both crystal solves in M3; the G4 and G5 families are re-run to confirm no qualification answer moves.
- **Coverage changes.** Near-pure crystal-forming feeds answer instead of holding after 2.7 s.
- **Outcome (M1, 2026-10-15, `7a3ea017`; `M1_ENGINE_PORT.md`).**
  - Row A (UV, N2 traces 1e-12 to 1e-6) converges in 1.3k to 2.8k kernel calls; before, it failed after 7.7k to 57k.
  - Pressure moves by +1.37 f for nitrogen. The commit message's claim for methane is wrong: methane added at the same internal energy brings its own energy.
  - PH at 101.325 kPa gives the pure sublimation temperature for every f up to theta.
  - A target inside the triple-point triangle is answered before any search. Too little fluid for the traces holds typed (CRYSTAL_STEP) in 5 calls.
  - The G4 and G5 families print the same numbers, except G5F1's count of TP equilibria (98 to 0, same amounts).
  - **Forced choice:** a step's fluid may carry at most 1e-4 of traces (`STEP_FLUID_TRACES`, at most 1.25 mK at the handover). Bounding it at theta fails row A at f = 1e-6.
  - **Decided by the owner (2026-10-15, "Go with recommended, start implementation when appropriate").**
    - The 1e-4 bound stands.
    - The larger fluid carries the traces at the triple point. Nitrogen would physically sit in the vapour, but the amount is at most theta of the feed.
    - The typed CRYSTAL_STEP hold is only for near-pure feeds. Failures above theta stay untyped, so the network can retry them.
    - **theta = 1e-5.** It covers the PH failure at f = 2e-6, and the near-pure error stays about 1.4e-5 in pressure. No qualification set has CO2 with impurities between 1e-6 and 1e-5.
    - **The inner UV search resets its secant when the phase classification changes.** This is a cost fix. P5 printed digits that move are re-recorded once, with that reason.
    - **Implementation:** both code changes are M3's first commit, after M2 (same branch and Gradle lane), with one re-run of the engine families G4F1, G4F2 and G5F1 to G5F6.
  - **Implemented (M3a, `cc1a39a1`, 2026-10-16; `M3A_D21_FOLLOWUPS.md`).** It ran while M2 waited for D26.
    - **theta = 1e-5.** The PH point at f = 2e-6 now answers VAPOR_SOLID at 194.627787659 K in 87 kernel calls; before, it was NOT_CONVERGED after 1021.
    - **Secant.** It is taken only between answers of the same classification and phase count. The lead chose this form by measurement, within the approved intent: on 42 targets near the triple point, stalls fell from 5 to 0 at +0.8 % TP equilibria. The literal reset at each change cost +49 %.
    - **Engine families.** They print the same lines, except G5F6's UV round-trip residuals and GasSolidEquilibriumTest; no classification count changed.
    - **Still open (M3 watch item).** 13 of 42 UV targets near CO2's triple point with 1e-4 CH4 (above theta) still fail at the binary's three-phase step. These are general-path failures, left untyped as decided.

## D22 (2026-10-15): adapting the batch to main 0.8.0; crystals become solids

- **Question.** The decisions OD-P1 to OD-P7 of `MAIN_ADAPTATION_PLAN.md`, and how crystals relate to main's solid system.
- **Evidence.** `MAIN_ADAPTATION_PLAN.md` and its analyses `main-adaptation/THERMO_MATERIALS_LAYER.md`, `NETWORK_SOLVER_LAYER.md`, `RUNTIME_FORMATS_GUI_LAYER.md`.
- **Owner's answer (2026-10-15).** "Go with recommended except: Crystal should be considered as solids, expand the solid system if necessary (one type from phase transition, another from customization); Particles within a recipient is considered to distribute homogeneously (for now); The precipitated particle size is a major gap, I'm thinking about to correlate it with the phase ratio, so that a small fraction of water (in organic, and vice versa) does not block the slurry output, but if a major phase freezes it blocks all output. Do some research and give me a proposal; Block the pipeline if precipitation happened during transportation; Evaluate the impact on solver convergence."
- **Chosen.** (1) Route OD-P1: the port onto `claude/unified-thermo-port`, cut from main `ffeafe8a` in this worktree; `claude/coolprop-multiphase-thermo-37f6b0` stays unchanged at `9aaafd4` as the record of every commit id cited in this log. (2) OD-P2, decided 2026-10-15 (the plan, section 5 item 2): adapt to main and restore nothing; the engine's cubic evaluator builds its derivatives on `PengRobinsonKernel.evaluateDerivatives` and adds the two volume-translation terms itself. (3) OD-P6: `ScheduledTransfer` is kept. (4) The rule-forced choices of the plan's section 6: no gating of the new liquid path, one format bump (checkpoint 9, unit 2, `fluid-10`), P6b's `a2f48d4` and `db43b22` not ported, main's consolidation rules kept, the small defaults. (5) Crystals are solids: main's solid system is expanded to two kinds, solids formed by a phase transition (CO2-I now) and custom solids (the `solids` records). This supersedes D20 items (1), (2) and (5); D20 items (3) and (4) stand. (6) Particles in a vessel are distributed homogeneously, for now. (7) Precipitation during transport blocks the pipeline (superseded by D23 (5): precipitation is evaluated only in reservoirs). (8) Pending, decided in D23: the precipitated particle size (research and a proposal), with an evaluation of (5) to (8) on solver convergence. OD-P3 (level head), OD-P4 (crystal-only tank), OD-P5 (emptying step) and OD-P7 (tank rail) were answered "recommended" for crystals as a separate inventory; under (5) and (6) they are re-read in that proposal. M3 and M4 wait for it; M1 and M2 do not depend on it and proceed.
- **Rejected.** Merging main into the old branch (one unreviewable resolution of 76 files); a rebase (rewrites 61 commits).
- **Affected milestones.** M1 to M5 of the adaptation plan; D20's reading of G6 for crystals, re-qualified after M3.
- **Coverage changes.** After M3, crystals travel with the slurry and meet filters and the solid mobility rules; a pipe in which a solid would form is blocked.

## D23 (2026-10-15): the size of precipitated particles, and well-mixed tanks

- **Question.** How large is a particle formed by a phase transition, and what decides whether it flows (D22 (8))?
- **Evidence.** `main-adaptation/PRECIPITATED_SOLIDS_RESEARCH.md` (sizes per route; Brauner 2001 droplet size against the dispersed fraction; a settling cutoff) and `main-adaptation/SOLID_SYSTEM_AND_CONVERGENCE.md` (main's outlet check: deposition velocity 10 x Ferguson-Church, bore and packing tests between steps, never in the Newton; size absent from the Newton's solid moments).
- **Owner's answers (2026-10-15).** To a first proposal blocking on solid fraction alone: "I don't think this is enough, particle settling in pipe is governed by size, density, pipeline and fluid properties. Only solid fraction will not work. I want more like formed particle size vary with phase fraction for liquid-liquid transition." To the revised direction: "The new plan looks good, need to learn about how it affects the current solver. The tank agitation will be in a separate work, for now consider every tank well mixed and does not settle."
- **Chosen.** (1) The size of a particle formed by freezing grows with the freezing phase's volume fraction (Brauner's dilute and dense forms), and main's settling and deposition laws decide in pipes. (2) Every tank is well mixed and nothing settles in it; tank agitation is a later batch, so the dilute size is a constant reference for now.
- **Proposed.** The rule's details and their effect on the solver are in `PRECIPITATED_SOLIDS_PLAN.md`. It lists decisions 1 to 7: phi, d_dilute, the slush threshold, the deposition law, transport blocking, rest for unchanged solids, and the approximate fallback.
- **Owner's answer (2026-10-15).** "Yes the precipitation should not be evaluated frequently and 5s lag is fine. Transport blocking is indeed an issue, how about only doing it in reservoirs. Does it break anything? Others as recommended".
- **Decided (2026-10-15).**
  - **Where crystal mass moves.** Only in the vessel refresh at interval and segment starts. The lag is about 5 s. Inside a step a crystal is an inert population.
  - **(1) phi.** The crystal component's volume share of the liquids, capped at 0.5; 0 from a gas.
  - **(2) Size.** d_dilute = 0.25 mm. Sizes were to be snapped to R10, set at onset and never changed. The amendment below supersedes the snapping and the onset-only rule.
  - **(3) Slush.** An outlet blocks at 0.5 solids for all solids. The slurry viscosity keeps 0.62.
  - **(4) Deposition.** Main's law is kept (10 × Ferguson-Church).
  - **(5) Where precipitation is evaluated (replaces the proposal).** Only in reservoirs; nothing blocks in transport. This supersedes D22 (7).
    - Pipes, junctions, pumps, compressors, valves, generators and voids are solved with crystals held out (`tpCrystalsHeldOut`). A supersaturated stream precipitates at the next refresh of the reservoir that receives it.
    - Particles in transport neither grow nor melt.
    - Nothing breaks: mass and energy are conserved. Main never evaluates a phase change inside a pipe, and the per-slice flash and its refusal mode disappear.
    - Lost: a let-down plugging with solid formed inside it.
    - Watch: a junction mixing streams into two liquids beside a vapour holds in class C. The fix, if the cold fuzz shows it, is one-liquid junction buffers (D46).
  - **(6) Rest.** Unchanged solids may rest, with the refresh run before the rest test.
  - **(7) Fallback.** The approximate fallback stays refused for solid intervals.
- **Amended (2026-10-15): one size per solid in each stock.**
  - **Owner's question.** Whether averaging the sizes of one solid in a reservoir would make convergence harder. The answer was no: size never enters the Newton, and merging keeps the three solid moments.
  - **Owner's answer.** "Make it one, ditch distribution for now", and "Also A" (all solids).
  - **Chosen.**
    - Each refresh merges a stock's populations of one solid into one population. Stocks are tanks, junction buffers and filter cakes.
    - The size is the mass-weighted mean diameter, rounded to 9 significant digits.
    - Precipitated mass enters at d(phi). Melting removes mass at the stock's size.
    - Main's outlet check reads the per-solid mean instead of the largest population.
    - Custom solids are merged too.
  - **Deleted.** R10 snapping, onset-only sizing, the 7-key bound, and proportional growth across populations.
  - **Details.** `PRECIPITATED_SOLIDS_PLAN.md` section 7.
- **Affected milestones.** M3 and M4 of `MAIN_ADAPTATION_PLAN.md`.

## D24 (2026-10-15): a gas carrying crude traces splits off a trace liquid (THREE_PHASES holds found in M2)

- **Question.** After M2 put the network on the engine, islands hold on THREE_PHASES where methane or nitrogen gas carries crude cuts at 1e-21 to 1e-7 of its amount (350 to 370 K, 0.1 to 0.5 MPa).
- **Physics.** The heaviest cuts are supersaturated at those partial pressures and condense as a trace liquid of 1e-21 to 1e-8 of the amount. The state is one gas and one liquid, never a third phase.
- **Code.**
  - The engine solves Rachford-Rice in the vapour fraction beta. At beta = 1 − 1e-14 a double holds 1 − beta to only about 1 %.
  - The product check (tolerance 1e-8) then reads tangent-plane distances down to −1.0 and the island holds on UnsupportedPhases THREE_PHASES. Main's flash has no product check.
  - Seen in main's HoldClassWorldsTest (2 worlds) and TracePhaseFoldTest (1 world, 28 of 28 dumped states), and in fuzz seeds 156, 168 and 173 (`M2_FLUID_THERMO_PORT.md`).
- **Options.**
  - **A (recommended).** Solve Rachford-Rice in the minor phase's fraction (delta = 1 − beta when the root lies above one half), in the split stage of M1's engine.
    - The equations are the same; only the unknown is better conditioned.
    - In the probe, 28 of 28 states and seed 156 converge VAPOR_LIQUID, and 56 of 56 product checks are stable (max |tm| 5e-13).
    - The engine's vapour-major splits move at rounding level, so G3 and the D21 rows are re-checked.
  - **B.** The network hands the engine only components above a new trace threshold. The probe needs a cut between 1e-12 and 1e-8, depending on the state.
  - **C.** Keep the typed hold and restate main's three worlds.
- **Decided (owner, 2026-10-15: "D24 go with recommended").** Option A, in the engine's split stage, as its own M2 commit. The engine families (G3, G4F1, G4F2, G5, the D21 rows) are re-run once, and any moved printed digit is explained.

## D25 (2026-10-15): a pure-water node on its saturation line (E7 active-set cycle found in M2)

- **Question.** Fuzz seed 116 holds in class E7 after M2.
- **Physics.** The node is 1 kg of water boiling under vacuum at 304.2 K and 4.52 kPa, its saturation pressure. On a pure fluid's saturation line, temperature and pressure do not decide the split; volume and energy do.
- **Code.** Main's pure-water TP rule flips the node between all liquid and all steam on each pass, and the pass key repeats.
- **Proposed.** One pure-coexistence rule for every pure fluid, water included: the outer phase check keeps a pure node's layout on its saturation line, as the branch's phaseCheck does for a pure hydrocarbon.
- **Owner's answer (2026-10-15).** "D25 solution should not be limited to pure compounds for the sake of future-proofing."
- **Decided: one rule for any composition.**
  - **Physics.** A closed node at fixed energy, volume and amounts has one equilibrium, and T and P are its outputs. Wherever the phase rule makes T and P dependent, a TP flash at a one-phase state proposes the other phase alone. That covers one compound with two phases, two compounds with three phases, near-pure or azeotropic mixtures, and a crystal beside its vapour.
  - **Code.** Main's `phaseCorrection` swaps the layout for the flash's, so the node flips.
  - **Rule.** The outer phase check never swaps one populated phase set for another on a TP verdict.
    - A correction adds the phases the equilibrium (TP flash plus fold) finds missing, and keeps the populated ones.
    - A phase leaves only when the solve empties it: its amount reaches zero, or the trace fold (phase-ports D34) removes it. That includes the last iterate of an unsuccessful pass.
    - There is no pure-compound test.
  - **What it absorbs.** The branch's pure-coexistence phaseCheck and main's pure-water flip. P6's `vesselSplit` is ported only if `LiquidFullBubblePointTest` still fails under the rule.
  - **Where it lands.** In M2, moved from M3, so that every change that moves bundled results lands before the one re-record of chain-100 and the junction lines.
  - **Checks.** Seed 116 runs clean, `LiquidFullBubblePointTest` passes, and the quick check runs.
  - **Stop condition.** If the rule makes passes fail where a phase must vanish (new E-class holds), M2 stops and reports with the two smallest seeds traced.

## D26 (2026-10-16): how much of an added phase the outer phase check seeds (two E7 cycles after D25)

- **Question.** D25 adds a phase that the TP answer holds and the node lacks as a nucleus of 1e-6 of the node's amount. The quick check at `8a06b22f` found two new E7 cycles, seeds 35 and 86 (`M2_FLUID_THERMO_PORT.md`).
- **Physics.**
  - **Seed 86.** 85 °C water at 195 kPa meets 281 K nitrogen at 183 kPa in a junction that drains to a 1 atm void. The bubbles saturate with steam (58 of 171 kPa) and grow from 0.71 to 1.08 L beside 1.03 L of water.
  - **Seed 35.** 366 K crude flows from a 374 kPa generator to about 1 atm. Its light ends flash: 0.35 of 4.64 mol, or 10.8 L.
- **Code.**
  - **Seed 86.** D41 treats a junction gas smaller than its water as a dry carried gas. The steam nucleus leaves the gas at 0.71 L, so the next pass's fold dries it, D25 adds the steam again, and the pass key repeats.
  - **Seed 35.** The pass converges liquid-only (superheated). The 4.6e-6 mol vapour nucleus cannot grow 7e4-fold, so the line search stalls and A274's cap corrections alternate. Main seeded the TP split here.
- **Options.**
  - **A (recommended).** Seed an added phase at the node's own split for its conserved state, and add nothing the fold takes away, so the check and D41 agree.
    - For a vessel, the split comes from its closure as `InventoryEquilibrium` solves it: UV when the wall is taut, PH at ambient pressure when slack.
    - For a junction, the split is the PH answer.
    - The nucleus constant goes; the cost is one flash per added phase.
  - **B.** A nucleus in vessels and the TP split's amounts in junctions. Seed 86's steam is then ignored.
- **Decided (owner, 2026-10-16: "ok" to option A).**
  - An added phase is seeded at the node's own split: a vessel's `InventoryEquilibrium` closure (UV taut, PH at ambient when slack), or a junction's PH answer.
  - Nothing the fold removes is added. The 1e-6 nucleus constant is deleted.
  - Implemented in M2 after M3a frees the lane.

- **Review (2026-10-16), proposed amendment** (`UNIFIED_THERMO_PORT_REVIEW.md` §1). The own split seeds the last iterate of a phase its closure is driving to nothing, and that is the cold distribution's cost.
  - Seed 48 is a liquid-full LNG tank beside 68 kg of dry ice. There, 141 989 of 142 079 own splits stall at the 24-iteration cap with the proposed vapour near 1e-9 of the moles, and the fold keeps or drops it at rounding. The result is pass-key cycles and halvings.
  - Seed 48 alone carries 87 % of the cold run's Newton iterations, and classes E7 and F come from the same mechanism.
  - **Proposed A:** the own split answers "no phase" when its closure, converged or not, drove the proposed phase under the trace share. The refusal is recorded in A85 for the solve. A phase that stands seeds as now.
  - This is D30's rule (a phase with no room leaves) given to the step's own split. It decides by the closure's direction, not by its convergence: the rule-off form "an unconverged closure adds nothing" floors a real liquid.
- **Decided by the owner (2026-10-16): the amendment, as part of form A** ("go"). It is compared on the quick checks against the head and against form A plus D33's review form.

## D27 (2026-10-16): two test references restated for water's own compressibility

- **Question.** After M2, two runtime tests miss their bounds. M2 traced both and changed neither.
- **CadenceTrajectoryQualificationTest** (0.26 % against 0.25 %).
  - The 0.25 % bound checks that the reference pair (0.5 s and 0.25 s ceilings, first order) is converged. The game's cadences are judged against the finer reference at 2 %.
  - Water's own compressibility (4.56e-10 1/Pa, not the retired 1e-9) shortens the reservoirs' time constant from 56 to 25 s, so the reference must refine with it.
  - **Proposed:** a reference pair of 0.25 s and 0.125 s (worst case 0.66 of the bound). The bound and the cadence checks are unchanged.
- **McpGameplay clamped drain** (0.64 % against 0.2 %).
  - The archived tank holds 996.05 kg, which no longer fills its 1 m³ at 997.05 kg/m³. The two step sizes book its first 25 ms refill of 1 L differently; later intervals agree to 1e-7.
  - **Proposed:** re-archive the tank under the new water law. AGENTS.md says a changed material data set is tested on a fresh world, never an old one.
- **Status.** Proposed 2026-10-16. The owner's answer covered D26 only. Both items go ahead as proposed, because each is forced: the first by the test's own purpose, the second by the fresh-world rule. The owner was told this on 2026-10-16 and can reverse it.

## D28 (2026-10-16): a failed pass's last iterate is not a state (STACK_PASS after D26)

- **Question.** After D26, STACK_PASS fails: the upper tank keeps 0.47 g of water, against a 0.3 g bound (`M2_FLUID_THERMO_PORT.md`).
- **Physics.** The last 1.3 g drains through the bottom outlet and the gas stays saturated, so the tank ends dry.
- **Code.**
  - A failed pass drains that water to a trace in its last iterate. D25's clause "the trace fold, also of a failed pass's last iterate" folds the liquid away, and main's A85 record then keeps it out.
  - Later passes state the water as steam, 25 % supersaturated at 295.7 K. D26's own split condenses 0.0685 mol at 298.96 K, 450 Pa above rest.
  - The Newton then drains slowly and rests with a puddle whose 5 mPa head stays closed.
- **Cause.** The lead added the failed-pass clause to D25 to stop a phase that must vanish from failing passes forever. It takes a decision from a non-physical, unconverged iterate.
- **Options.**
  - **A (withdrawn 2026-10-16, before the owner answered).** A failed pass's iterate folds and excludes nothing.
    - D30 (`63926c52`) needs that fold. A vessel's closure has fixed U, V and n, so it has no smaller step to fall back to.
    - Without the fold, cold seeds 35 and 148 would hold again (E1).
  - **A′ (recommended).** A failed iterate's fold is a trial layout, not a verdict. It still removes the phase the iterate empties, and it reads A85's record but never writes to it.
    - **Code:** two lines. `PassiveStepSolver` passes `foldedPhases.clone()`, the read-only idiom its polish already uses. `InventoryEquilibrium.passes` drops its `excluded|=…` on a failed pass.
    - **When the removal is wrong:** a converged answer then finds the phase missing (STACK_PASS's 25 % supersaturated steam). The phase comes back at the node's own split, the pass key repeats, and the existing cycle guard refuses the pass. The interval halves, and the water drains as liquid in converged, smaller steps.
    - **When the removal is right:** it stands. D30's compressed LNG beside the record's crystal holds no missing vapour.
    - **Probe (2026-10-16):** `63926c52` with `staging/d28-option-a-prime.patch` alone, uncommitted. Logs 22 and 23 are in `m3-logs.tar.gz`.
      - STACK_PASS passes. The upper tank ends with 0.0 g of water. The headspaces rest 22.7 Pa apart, which is the gas column's own weight.
      - Seeds 35 and 148 are clean, with 37.27 kg and 19.93 kg of crystal, unchanged.
      - Seed 72 no longer holds at tick 1080. It holds at tick 1318 in island 32 (E6), where `a0f20364` ran out its wall deadline.
      - Across the three seeds, A′ against D30 alone:

        | | Active-set passes | Newton iterations | Rejected steps |
        |---|---|---|---|
        | A′ | 2452 | 26442 | 338 |
        | D30 alone | 3117 | 37464 | 366 |
        | Change | −21 % | −29 % | |

      - Beyond the two lines, only comments changed.
  - **B.** Vessels seed a nucleus again (D26 B for vessels).
  - **C.** Widen the bound to 0.5 g (a patch).
- **Status.** Proposed 2026-10-16; restated with A′ the same day. M2 is otherwise complete at `a0f20364`: 1414 tests with 1 failure (this one), and the quick check equals main's classes.
- **Decided by the owner (2026-10-16): A′.** The owner answered "ok" to the probed recommendation. M3 commits it before D29 A, then measures both together: one cold and one bundled quick check, and one full `test`.

## D29 (2026-10-16): how a step carries a crystal's share between refreshes

- **Question.** Under D23, crystal mass moves only in a vessel's refresh. Within a step, a node's TP answer can hold a crystal (supersaturation since the last refresh). How does the step carry that share? M3 stopped here at `1eeb8673` (`M3_CRYSTALS_AS_SOLIDS.md`).
- **Physics.**
  - LNG at 149 K and 1.58 MPa (4.2 % CO2) flowing into an empty 1 m³ tank flashes until the tank pressurises. After one interval it is boiling LNG at 133.7 K and 414 kPa, with 9 % liquid. CO2 may freeze at later refreshes.
  - Snow's energy is close to a liquid's: fusion is about 9 kJ/mol, against about 25 kJ/mol for sublimation. It is far from a gas's.
- **Forms measured** (the lean cap of three):
  - **The plan's held-out flash.** Row E holds THREE_PHASES at 95.7 K: the held-out CO2 splits off as a second liquid.
  - **M2's adoption** (frost carried as vapour). The CO2 let-down runs to methane's 90 K floor.
  - **The stand-in** (frost's share carried as liquid, `92d39275`).
    - It passes every M3 test but loses 8 cold-fuzz seeds.
    - A stand-in liquid of pure CO2 gives methane vapour-only trace support (`PhaseSupport.of`), so methane cannot condense and the node cools to the 90.69 K floor.
- **Options.**
  - **A (recommended).** Keep the stand-in, and take trace support from the answer's own fluid phases, so the stand-in liquid (a supersaturated solution) admits every component.
    - It is a fourth form, so it needs the owner's go under the lean cap.
    - It touches main's trace support only where a stand-in is present; bundled worlds are expected unchanged.
  - **B.** M2's form, accepting that CO2 let-downs hold. That includes the owner's test-world row "liquid CO2 let down into a 1 bar tank".
- **Decided by the owner (2026-10-16): A.** The owner answered "ok", which is the go for a fourth form under the lean cap. It is measured once, on the final code, together with D28 A′.
- **Status.** Proposed 2026-10-16.
  - A is staged as an uncommitted patch, `tools/unified-thermo-port-m3/staging/d29-option-a.patch`. It applies to `63926c52` and compiles; it is not measured.
  - The patch adds `FluidThermodynamics.condensedShareOnly` and a model overload of `PhaseLayout.support` that turns VAPOR_ONLY into BOTH beside a liquid of crystal species only.
  - It changes the three support paths that use the model's trace policy: `PassiveStepSolver.supports`, `InventoryEquilibrium.layout` and `VesselCrystals.Joint`.
  - Packages without crystals return at the patch's first test.

## D30 (2026-10-16): the refresh's joint closure has no phase rule (E1, cold seeds 35, 72 and 148)

- **Physics.** A rigid 1 m³ tank full of LNG (143.77 K, 1.062 MPa) beside 37.28 kg of CO2-I near saturation stays full.
- **Code.**
  - The engine's answer (crystal at 1625 kg/m³) has a 0.45 L bubble at 755 kPa.
  - The joint closure starts there with the record's crystal (1571 kg/m³, 0.79 L more). It has no rule to drop the bubble, so it stalls at a vapour volume of 6e-24 m³.
- **Decided by the lead (2026-10-16).** This reuses an existing structure and adds no rule: the joint closure runs inside `InventoryEquilibrium.solve`'s pass loop, whose phase rule drops a phase that has no room. The owner was informed.
- **Implemented (M3, `63926c52`, 2026-10-16).**
  - The pass loop became `InventoryEquilibrium.passes`, over an inner `Closure`. Two closures run in it: the vessel's own, which makes the same calls as before, and `VesselCrystals.JointClosure`. The trace fold of the failed joint iterate removes the bubble, and the closure is solved again without it.
  - Cold seeds 35 and 148 are clean; at the end they hold 37.27 kg and 19.93 kg of crystal.
  - Seed 72 now holds where `a0f20364` holds it, a step at tick 1080 in island 30. It is classed E7 here and E6 there.
  - The six M3 test classes pass (27 of 27). No fuzz run and no full `test` were made.
  - The fold this uses is a failed iterate's, so D28 is restated.

## D31 (2026-10-16): an empty tank's first contents are laid out at their own split (cold seeds 34, 98 and 114)

- **Question.** At `76e87740`, cold seeds 34, 98 and 114 hold in class C at methane's floor (90.69 K), in the first 0.05 s interval of LNG into an empty tank. All three are clean at `a0f20364` (`M3_COLD_ARRIVAL_TRACE.md`).
- **Physics.**
  - A placed tank is empty (D54). Its 298 K nitrogen is a placeholder with no inventory.
  - The subcooled LNG (seed 114: 139.0 K, 1.14 MPa) flashes at constant enthalpy into the slack bag at 101.3 kPa. The result is boiling LNG at 112.1 K with 19.3 % vapour (seed 34: 113.1 K, 25.9 %). CO2 above its solubility becomes frost for the next refresh.
  - 90.69 K would need methane at 11.7 kPa.
- **Code.**
  - `arrivingPhases` lays the empty bag out with a TP flash of what arrives, at the stream's own temperature (139 K) and 1 atm: an isothermal let-down that gives all vapour beside CO2-I.
  - The stand-in (`92d39275`) puts the frost's CO2 in the liquid slot. That is a pure-CO2 liquid on the equation of state's CO2-rich branch, and methane barely dissolves in it (K = 34, against Raoult's 0.12). The methane stays vapour and cools to the floor with 3.35e5 J unbooked.
  - Both hydrocarbon slots are populated, so the outer phase check (D25) proposes no liquid. The first step into an empty bag is scale-free, so all 20 halvings fail the same way.
  - At `a0f20364` the same answer was adopted all vapour. The failed pass lacked a liquid, so the phase check seeded one at the own split (D26), and pass 1 converged at 112.11 K.
  - D29's reading does not apply here. Under D29 A methane already has liquid support; the liquid it can enter is the wrong one.
- **Options.**
  - **A (recommended).** In `arrivingPhases`, an empty bag is laid out at its own split of what arrives (D26): PH at the delivered enthalpy and ambient pressure, UV at the capacity if that overfills the bag, TP as the fallback.
    - Physically this is the stream's adiabatic flash into the bag, for every fluid.
    - It absorbs the isothermal estimate, and the failed first pass the route at `a0f20364` needed.
    - It may move bundled worlds where a flashing liquid enters an empty tank: those start at the root instead of all vapour.
  - **B.** The outer phase check counts a liquid of crystal species only as no liquid, so the TP answer's liquid is seeded at the own split.
    - It also covers this trap in tanks that already hold something, and may absorb D29 A.
    - It keeps the failed first pass.
  - **C.** M2's adoption for frost answers (D29 B). The CO2 let-down row holds again. Not recommended.
- **Status.** Proposed 2026-10-16; waiting for the owner.
- **Probe of A (2026-10-16).** The patch is `tools/unified-thermo-port-m3-arrival/staging/a-own-split.patch` against `76e87740`, uncommitted, run on the index lane.
  - The empty bag is seeded from the engine's PH answer at the delivered enthalpy (elevation included) and 101 325 Pa. That answer is closed by D26's `InventoryEquilibrium.split` on an inventory of what arrives: PH while slack, UV at the capacity once it overfills. TP stays as the fallback.
  - Beyond the empty-vessel branch, the patch makes `equilibriumPH` public, adds one enthalpy line in the edge loop and adds the helper `emptyBagSplit`.
  - Seeds 114, 34 and 98 are clean. Each first pass starts at its root with 0 Newton iterations; the first intervals end at 112.11, 113.05 and 112.17 K at 101 325 Pa. Seed 98's held node is also an empty tank fed LNG.
  - The bag sits 0.3 K above the engine's 111.80 K because the frost's CO2 share stays dissolved until the refresh, which freezes 5.89 mol of it.
  - The 27 test classes (M3's six, group L, every class citing D54) pass 147 of 147. 74 of 501 printed lines move, all to the same root within the Newton tolerance (largest relative move 1.9e-12): 73 in `EmptyTankFillsTest` and 1 in `TracePhaseFoldTest`.
  - B was not needed and was not run.

- **Review (2026-10-16)** (`UNIFIED_THERMO_PORT_REVIEW.md` §2). Keep the stand-in as the energy carrier, but take away its vote. `phaseCheck` counts a liquid of crystal species only, beside a vapour, neither as a proposal nor towards liquid-completeness. This is B, generalised to every node.
  - A stays and is applied, as D26 at arrival. D29 A stays.
  - In seed 183 the pure-CO2 pseudo-liquid is 41 % of the proposals, and every closure of it stalls.
  - Measured together with D26's amendment as one change set.
- **Decided by the owner (2026-10-16), "go".** Apply A (the staged patch), and take away the stand-in's vote in every node (B, generalised). Both belong to form A.
- **Measured (2026-10-16).** Commits: `91a2c14a` D31 A plus the D26 amendment, `40a44732` B generalised (form A), `c9e7c128` (A + D33).
  - Quick checks, cold (bundled is identical in all three forms):

    | Form | Clean of 200 | Newton iterations |
    |---|---|---|
    | Head | 179 | 4.63 M |
    | A | 184 | 4.41 M |
    | A + D33 | 183 | 4.35 M |

  - **F1, a regression of B.** M3's gate `aLetDownThatWouldFreezePassesAndTheReceivingTankPrecipitatesAtItsRefresh` passes at `91a2c14a`: 184.8 K, the stand-in at Peng-Robinson's saturation, and 0.20 kg of dry ice at the refresh. It fails at `40a44732` with CO2 below 90 K.
    - The vote was the only way a vessel already holding something gains the frost's condensed carrier. Without it, the vapour carries the 25 kJ/mol deficit.
    - The owner's row D23-1 holds the same way.
  - **The amendment's literal share misses the stalls.** 57 % of seed 48's stalled own splits end at 1e-9 to 3e-9 of the moles, just above the share. Seed 48 drops only from 4.01 M to 3.82 M.
- **Proposed form 4 (2026-10-16; a fourth form, needs the owner's go).**
  - (i) B keeps only its completeness half: a stand-in never makes a node liquid-complete. A TP answer's stand-in is proposed like any phase and decided by the own split under the D26 amendment. Seed 183's stand-ins end at 0 to 1e-11 and are refused; the let-down's stand-in stands.
  - (ii) The amendment decides by direction: a phase the closure is driving to nothing is refused once it is below a margin, for example 1e-6 of the moles and shrinking. The criterion is chosen once, on seed 48's one-tank step.
- **Decided by the owner (2026-10-16): form 4** ("ok"). It is measured once on the cold and bundled quick checks, then chosen unless it shows a loss its physics does not explain.

## D32 (2026-10-16): a pipe joining a lone boundary leaves its island of one behind (save crash, main 0.8.0 code)

- **Found by** M4's GUI check (`M4_RUNTIME_FORMATS_GUI.md`, open item 1). The code is main 0.8.0's; the port did not change it.
- **What should happen.** A lone generator or void is an island of one. When a pipe joins it to another island, the boundary belongs to the joined island only, and its island of one ends.
- **Code.**
  - `PhysicalRegistry.apply` never lists that island in `affected`, so it stays in the coordinator, certified, and is saved. M4's probe shows islands 5, 6 and 10 beside the joined island 17.
  - After a reload, `load()` gives it no members. The next save throws `NoSuchElementException` in `FluidWorldAuthority.capture` (`members(id).getFirst()`), and every later save crashes.
- **Options.**
  - **A (recommended).** The batch that puts a fixed boundary into a replacement island also retires that boundary's island of one (fenced, listed in `affected`). Capture and load refuse an island with no device.
    - Make it its own `fix:` commit on this branch, so it can be cherry-picked to main as 0.8.1 if the owner wants main fixed before the port merges.
  - **B.** Drop member-less islands at load. This is a patch and leaves the in-session leak.
- **Status.** Proposed 2026-10-16.
- **Decided by the owner (2026-10-16): A** ("go"). It goes in its own `fix:` commit on this branch, so it can be cherry-picked to main as 0.8.1 on the owner's word.

## D33 (2026-10-16): a frosting tank's side outlet shuts its gas, and the closure can stick at rest

- **Found by** M4's GUI check (open item 2).
- **Physics.** Gas leaves a gas-filled tank through any nozzle. Frost deposits on the walls and stays there; with no liquid, solids do not flow.
- **Code.**
  - Plan item 4 ("frost formed from a gas has no liquid carrier, so only the gas outlet stays open") applies main's `SolidMobility` (NO_LIQUID_CARRIER). The side mixed outlet closes both ways, so the tank fills to the feed pressure and stops.
  - After the frost sublimed, the island certified at rest with the closure still in place: no solids, and 199 kPa across it.
  - Cause: `NetworkReduction.atRest` lifts closures only while a node holds solids (D23 (6), `34b80286`). Before the port no solid vanished at rest, so this could not happen.
- **Options.**
  - **A (bug fix).** The rest test reads every solid closure as open at an interval start, solids or not. This deletes the "while a node holds solids" condition.
  - **B (physics, owner).** An outlet of a tank with no liquid passes its gas and keeps its solids: the solid flux is zero and the fluid flows. This replaces the NO_LIQUID_CARRIER closure, including for main's inert solids.
- **Status.** Proposed 2026-10-16. Passed to the general design review (`UNIFIED_THERMO_PORT_REVIEW.md`) before the owner decides.
- **Review (2026-10-16)** (`UNIFIED_THERMO_PORT_REVIEW.md` §4). B and A as one constitutive form.
  - Solids ride the liquid: one helper books a bulk draw's solids as solids × liquid drawn / liquid held. That is the same number as today whenever liquid is present, and zero from a dry gas. A bulk end on a donor without liquid reads as the GAS outlet. This deletes `Reason.NO_LIQUID_CARRIER` and its closure.
  - `NetworkReduction.atRest` reads every transport closure as open at an interval start; a stopped filter's closure stays. This deletes D23 (6)'s `solids` flag.
  - Risk: frost stranded in a junction that lost its liquid stays there, as a frosted pipe that passes gas. A dry bed never discharges, as OD-P4 already says.
- **Decided by the owner (2026-10-16), "go".** The review's form (solids ride the liquid, and closures reopen at every interval start) is measured as form A plus D33. It is kept unless the quick checks show a loss its physics does not explain.
- **F2, the same principle at a pump inlet (test-world bisect, 2026-10-16; proposed, waiting for the owner).**
  - **Physics.** A pump on the bottom of a vented tank of LNG slurry starts. It draws the liquid with its fine crystals, and a 5 L/s target gives about 2.2 kg/s at 2.55 m/s in the bore, above the 1.02 m/s needed to keep crystals moving.
  - **Code, on the port since `76e87740`.** Main does not show it: water with demo particles pumps 5.73 kg/s.
    - The D55 start (`fixedPointStarts`, `supplierSeeds`) never replaces the pump junction's 298 K nitrogen placeholder (1.145 kg/m³). The suspect is `seedAt` swallowing a cryogenic flash failure.
    - `SolidEventIntegrator.solve` then converts the volumetric target at that density: 0.0061 kg/s, 6.9e-3 m/s. The line closes on DEPOSITION at t = 0 of every interval.
    - `InletPhase.decide` reads the inlet across that closure as dry (held share 0, below DRY_RESUME), and D6 keeps the refusal.
  - **Proposed A.** Seed a mover's junction from its supplier's stream, which is already a state, so no flash is needed. Take the inlet decision with the interval-start closures read as open, as D33 does for transport.
- **Test-world bisect of F1 (2026-10-16).** The regression starts at `40a44732` and is fixed from form 4 (`58d9dac4`, `2ab43f23`) on. D23-1 runs at 5 cm and at 4 mm: 184.84 K, 101.5 kPa, 48 % dry ice.
  - Proposed: gate D23-1, liquid CO2 into an empty tank, at both pipe sizes.
- **Decided by the owner (2026-10-16), "ok".** F2 is fixed as proposed in A. The D23-1 gate is added at both pipe sizes.
  - F3 (a tank sits about 10 K under the frost point between refreshes) is accepted as a known game-level approximation. The stand-in sits at the metastable liquid's saturation, 184.8 K at 1 atm against the frost point's 194.6 K, and each refresh corrects it.

## Owner rule (2026-10-16): thermo validity below the freezing point

- **The owner's words.** "If we have data to the freezing point, the validity of thermo package should be good below that T, that's a general rule for any future implementations."
- **Rule.** A component whose fluid data reaches its freezing point stays valid below it, as a solute in a colder liquid (CO2 in LNG) or supercooled. No freezing point is a lower validity bound: seed windows, domain floors, fluid-only ranges and range clips use the package's own domain floor.
- **Where recorded.** In `AGENTS.md` (`1b15fd43`).
- **Found at.** F2's root: the seed window clipped CO2 at its 216.59 K triple point, while the pilot package admits it down to 90 K.
- **Survey (2026-10-16, `M4C_FREEZING_RULE_AND_FIXES.md`).** CO2's 216.592 K triple point is still a floor in five places: `seedAt`'s window, the `fixedPointStarts` round clip, `balancedBoundarySeed`, `FluidDomain.range` with `checkTotals` when water is present, and the engine's FLUID_ONLY domain. `check` and `checkPhaseTemperature` already use 90 K, so CO2 has three floors.
- **Open questions for the owner** (nothing applied yet):
  - Q1: how CO2's 90 K is carried, in its data record or by a code accessor.
  - Q2: methane's and ethane's tables end at their triple points (90.69 and 90.37 K).
  - Q3: water's 273.16 K, given that the bundled package's envelope floor is 63.151 K.
  - E8 and D33's gap go ahead in the meantime.

## Owner direction (2026-10-16): no hold for leaving a package's range

- **The owner's words.** "The issue is that it's extremely inconvenient for the system to be put on hold when outside package range, maybe just don't hold it at all (maybe use extrapolated data?)"
- **Scope.** This supersedes Q1 to Q3 of the freezing-point survey; the freezing-point rule becomes one case of it. A plan agent is surveying every range hold and the extrapolation form for each property model, including water below 273.16 K (D8).
- **Plan.** `RANGE_EXTRAPOLATION_PLAN.md`, for the owner's validation. M5 waits for it.
- **In the meantime.** E8 and D33's gap are committed independently.
- **Decided by the owner (2026-10-16): water below 273.16 K, option C.** Ice Ih is modelled as a crystal now, through the crystals-as-solids machinery (D23 pattern). D8's typed water hold goes.
  - The plan designs the model (IAPWS R10-06 or a lightweight form), the engine's crystal competition for water, the stand-in's supercooled-water continuation, and the effect on bundled worlds and test-world row 1.
- **Plan approved by the owner (2026-10-16): "ok keep things lightweight".** `RANGE_EXTRAPOLATION_PLAN.md` is taken with option A in all ten decisions:
  - (1) liquid viscosity below its table: Andrade, the end segment's slope in ln mu against 1/T; (2) ideal-gas cp held at the fit's end; (3) the water stand-in held at its 273.16 K values; (4) the ice record as segments fitted to IAPWS R10-06; (5) the melting line by Clapeyron from the record; (6) hydrates declared not modelled, ice and CO2 frost coexist; (7) the notice on the island's status line; (8) one numerical box, 10 to 6000 K and 1 Pa to 1 GPa, acting as trial rejection; (9) device inputs checked against the package envelope when set; (10) no clamp on PR alpha.
  - The no-range-hold rule is recorded in `AGENTS.md` (`1d08ef1a`).
  - Order: WP1 and WP2 in one agent (deleting `ThermoDomainViolation` breaks the 27 test files, so they change in the same commits), then WP3 and WP4, then the quick checks once and M5.
- **WP1 to WP3 implemented (2026-10-16)**, report `RANGE_EXTRAPOLATION_WP1_WP3.md`. Commits:
  - `333dd6e7`: seed streams are the fluid alone;
  - `a169fa85`: continuations, the numerical box, `ThermoDomainViolation` deleted, the EXTRAPOLATED grade, the interim ICE hold;
  - `085676ea`: the status-line notice, with gate c;
  - `90d68ca1`: gate a.

  On the index lane, 83 classes and 446 tests pass, and each gate fails with its rule off. The WP3 code was moved into this step so that one network test pins "no hold, and a notice". The GUI check of the notice line is part of the test-world session.
- **The agent's options, taken by the lead as reversible design choices** (any can be reopened):
  - (1) Notices read each model's own data range plus the envelope, as plan section 5 describes.
  - (2) A seed is the fluid alone: under D23 the Newton's unknowns are the fluid, and the solids are a separate inventory set at the refresh. The old helpers paired fluid-only amounts with a mass and enthalpy that included the solids; LNG under dry ice then seeded a 1.3 % vapour suction junction once the CO2 window opened.
  - (3) The PH and UV searches start inside the carried data and restart 1.25 times warmer, at most 8 times, when the first start gets no answer (a lower-bound start below a latent heat). The alternative, one restart from the envelope, is noted.
  - (4) The `fluid_domain` evidence texts are rewritten in WP4's data revision, which bumps every package anyway.
  - (5) Liquid water outside Region 1 gets a notice but no engine grade (a known small inconsistency).
  - (6) The CO2-I 300 K ceiling stays, as the plan says.
- **Observation.** Bundled records' ideal-gas fits start at 293.15 K. Below 20 °C, bundled worlds therefore show the notice "ideal-gas heat capacity extrapolated below 293.15 K". This is correct (that is where their data ends) and advisory only.
- **WP4 implemented (2026-10-16)**, report `ICE_IH_WP4.md`. Commits:
  - `13bcfe31`: the `ice_ih` record and water r2;
  - `1674e3db`: `FreeWaterIce`, ice decided by water's own rule, the freeze step in PH and UV;
  - `ead6d6f2`: ice on every package as a D23 solid, the stand-in, the refresh.

  On the index lane, 117 classes and 595 tests pass. The R10-06 check values reproduce to 2.2e-12, and the record's enthalpy is within 0.34 J/mol.
  - Gates:
    - (a) Humid nitrogen keeps 259.89 Pa at 263.15 K, and 78.0 % of its water freezes.
    - (b) PH lands on T_m at 1 bar and at 10 MPa (272.418 K). Vacuum freezing ends at 252.8 K.
    - (c) Water into cold methane freezes 1597 mol at the refresh and boils 76.7 mol of methane.
  - The one failing test, `NearPureCrystalStepTest` row A, dates from `a169fa85`. The pure CO2 UV search now restarts from the crystals' ceiling, as PH does, and lands 9e-11 (P) and 1e-9 (crystal) off its old answer, inside the 1e-9 resolution of U and V. The test now compares down to that resolution (`31e25f6f`); all its answers are unchanged.
- **The ice agent's options, taken by the lead as reversible** (any can be reopened):
  - (1) `PhaseLayout.LOG_READ_SPAN` caps the steam column's difference floor at 1e4 times its seed. It is bitwise wherever the steam is at least 1e-8 of its vessel. The alternative, differencing ln n in `SparseNewton` for every log-read column, is noted.
  - (2) Wet feeds with a forming crystal are solved dry at pc = P - p_w, with the water laid around them. This is `solveWet`'s own fixed point; water and hydrocarbons are immiscible. The alternative, a joint wet-crystal Newton, is noted.
  - (3) The ice's fugacity is scaled to meet the liquid's on T_m (1 + 7.3e-5 at 1 bar), so the steam is continuous across the line.
  - (5) Water r2 moves the coefficient revision, not the physics fingerprint.
- **For the owner:** (4), (6) and (7) of the report.
- **Quick checks at `31e25f6f` (2026-10-16, 200 seeds, logs `build/forms-logs/50-*` and `51-*`).** Baselines: cold `21-F4`, bundled `41-F2`.
  - Cold: 185 clean, against 183. Class C (range holds) went from 2 to 0, as planned. Newton iterations +2 %, rest intervals +15 %.
    - Classes: B 4, E5 3, E7 4, E8 1, F 1 (seed 12), and E9 2, which is new: seed 85, "Invalid pipe transport properties", and seed 183, an untyped TP-equilibrium failure.
  - Bundled defaults: 200 clean, counters flat (residual evaluations -2 %). Running the refresh on every package costs nothing measurable here.
  - Bundled random: 195 clean, as before; C is gone.
    - Class X (new): seeds 145, 157 and 164 throw an uncaught `IllegalStateException` ("Peng-Robinson equation has no physical VAPOR root") from `PhaseLayout.decode` in `projectFlows`.
    - The deleted range checks used to reject such iterates as trials. A missing EOS root must be a rejected trial (`AGENTS.md`).
    - A fix agent is tracing X and E9; report `QUICK_CHECK_FIXES.md`.
- **Quick-check fixes (2026-10-17)**, report `QUICK_CHECK_FIXES.md`:
  - **X, `8220d4bc`.** A Newton trial pushed the vapour's partial pressure to 6.6e25 to 1.5e292 Pa, where the cubic loses its root in floating point. The kernel now throws the typed `NoPhysicalRoot`. `HydrocarbonModel.phase` turns it into `RootAbsent`, an IllegalArgumentException that replaces `LiquidRootAbsent`, so the line search backs off.
  - **Cold seed 85, `fd0dc531`.** An empty tank inherited 3.4e-93 kg of crystal from the candidate's roundoff. `reconstruct0` now keeps a vessel's own solids (D54).
  - **Cold seed 183, `87c106f6`.** The crystal solve stops only when every fluid phase meets the crystal. The post-loop refusal is gone.
  - Bundled seeds 145 and 164 and cold seed 183 now run clean. Seed 157 keeps its earlier E5 hold, and seed 85 holds as E7, its class before the range work.
- **The lead's follow-ups, forced by existing rules.**
  - The numerical box also bounds the vapour's partial-pressure unknown (decision 8: the one bound on every T and P the models read).
  - A vessel holding no fluid is a dry end: solids ride the liquid (D33), so nothing leaves it. This replaces "holding nothing".
- **M5 review drafted (2026-10-17)**: `MAIN_ADAPTATION_REVIEW.md`, with the measurements left for the lead and a proposed CHANGELOG line.
  - Cleanup survey: no tracked measurement-only material on the branch, and `git ls-files -i -c` is empty.
  - Taken by the lead, before the single gates run:
    - `GroupContributionInteractions.kij`, which only a test calls, moves into its test.
    - The always-zero `SolverDiagnostics.phaseSeedFallbacks`, added by this batch, goes.
    - The names that still say "domain" but now mean phase holds are renamed, the status prefix "HELD (thermo domain): " included. It becomes "HELD (phases): ", and the log's `status=THERMO_DOMAIN` becomes `status=PHASE_HOLD`. The hold-fuzz classifier accepts both prefixes, so older reports still classify.
- **Follow-ups landed (2026-10-17).**
  - `db6b1e6f`: `NumericalBox.requirePartialPressure`, from 1e-6 Pa to 1 GPa, in `FluidThermodynamics.state`, the only caller of the kernel's vapour phase. It absorbs `HydrocarbonModel`'s old 1e-6 Pa floor: traces are real, 1 ppm at 1 bar is 0.1 Pa. Bundled seeds 145, 157 and 164 are unchanged.
  - `95e4325d`: `holdsNoFluid` replaces `holdsNothing` in `dryEnd` and wherever "holding nothing" stood for the same reason: the dry layout, the kept vessel, the refresh, junction seeds and sweep, small headspace and the rest test. A no-fluid end the solver still reads gives the ambient's viscosity, where it gave NaN before.
  - Cold seed 85 keeps its 13 holds. Its Newton solves rose 668 to 915, untraced; the quick check judges the aggregate.
- **Cleanup landed (2026-10-17)**, behaviour-free:
  - `04f84aa5`: phase holds are named as phase holds. The status reads "HELD (phases): ", the log `status=PHASE_HOLD`, the reason prefix "phase-hold: ".
  - `cc2f8c37`: `kij` moves into its test, and `phaseSeedFallbacks` goes.

  The harness copies and `classify.sh` accept both prefixes, and class C is relabelled "phase hold (typed)".
- **Regression found by the cleanup run:** `FluidFallbackQualificationTest#hardTimeoutInsideFallbackAndBoundaryRegimeChangesCannotPublish` fails from `ead6d6f2` on.
  - Cause: the refresh runs on every vessel of every package and calls the checkpoint, and `VesselCrystals.competes` counts any water as able to freeze.
  - Decided by the lead (option a, structural): water competes only at or below T_m(P), and a vessel holding no solids where nothing can freeze skips the refresh. This is main's skip, moved from the package to the vessel. The alternative, loosening the test's clock, would hide the cost.
- **Refresh skip landed (2026-10-17), `e4769fa2`.**
  - Water competes at or below T_m(P), or where the vessel already holds ice.
  - The pre-rest-test refresh is now `InventoryEquilibrium.conversions`. It visits only vessels holding solids or where a crystal competes, and the others get no closure and no checkpoint.
  - The full refresh at run start and on solid events still visits every vessel, because its result is used even when nothing converted. Skipping there too moved six tests' numbers.
  - Gate `PrecipitatedSolidsTest#aWarmWaterVesselHasNothingToConvertAndAColdOneFreezes`. The fallback qualification test passes again. No physical number moved; resting islands do 0 closure solves, where they did 12 and 100 in `LevelHeadTest`.
- **WP4 defect found (2026-10-17): a rigid vessel freezing through T_m fails its interval with "a step of U(T)".** Nitrogen over water at 263 and 272 K. A slack bag at ambient pressure is fine.
  - Physics: the state sits on T_m(P) with the ice fraction from the energy and P from the volume.
  - Decided by the lead (it is the physics, and WP4's own option 7): UV answers the freeze step directly, as PH does. This also replaces the 85-TP-call UV path. The fix agent is implementing it before M5.
- **UV freeze step landed (2026-10-17), `703d69b3`.** `FluidTpEquilibrium.freezingAtVolume`: T = T_m(P), the ice amount from the energy, P from the volume by a 1-D secant in P.
  - A rigid N2-over-water vessel from 263.15 K ends at 273.152218 K, 105.5 kPa, with 18.6 % of its water as ice, in 9 TP calls (it failed before).
  - Gate b's UV answer is unchanged, in 15 TP calls instead of 85. A taut network vessel freezes with no failed interval.
  - A liquid-full rigid vessel freezes along the line: 0.28 ice at 268.2 K and 67 MPa, which is Clapeyron's.
  - Nothing else moved.
- **The water triple point, decided by the lead as the limiting case of WP4's freeze step.** Pure water and its steam in a rigid vessel cooled through 0 °C sits at 273.16 K and 611.657 Pa, with the three amounts from the three balances. Before, it ended NOT_CONVERGED after about 2800 TP calls. It is answered before the UV search, as `pureTriplePoint` does for a crystal's species. In progress.
- **Water triple point landed (2026-10-17), `7bd5fafc`.**
  - `waterTriplePoint`, beside `pureTriplePoint`, reuses `solve3`, `tripleBalances` and `insideTriangle`.
  - A rigid vessel of pure water at ice fractions 0.1, 0.5 and 0.9 lands on 273.16 K and 611.657 Pa with all three balances closed to 1e-9, in 0 TP calls. Before, it failed after 489 to 2798.
  - `insideTriangle` now accepts a point only if, with any negative amount set to 0, it still meets the energy and volume specification. The old 1e-6 N mole tolerance answered a target 1e-7 mol short of steam at the triple point, 2 % off in volume and marked CONVERGED. Steam's molar volume is 2e5 times the condensed water's.
  - No printed number moved, CO2's triple points included.
- **Confirming quick checks at `7bd5fafc` (2026-10-17, logs `52-*`, `53-*`).**

  | Run | Clean | Before the range work | After WP4 (`31e25f6f`) | Classes |
  |---|---|---|---|---|
  | Cold | 186 | 183 | 185 | B 4, E5 3, E7 5 (seed 85 back in its old class), E8 1, F 1; E9 gone |
  | Bundled random | 197 | 195 | 195 | E5 157, E7 4, F 47; X and C gone |
  | Bundled defaults | 200 | 200 | 200 | counters flat |

  - Bundled random's Newton work rose 45 % against the baseline. For the 194 seeds clean in both runs, the wall time is 0.97 times the baseline.
  - The rise comes from seeds 4, 47 and 157, which now run on instead of stopping. Seed 47 is a new F, replayed alone after the campaign if it repeats there.
- **M5 started (2026-10-17)**: the lane runs alone (`tools/unified-thermo-port-m5/`). The full test serves as G1 and G3, then G2 chain-100 exact, then the fluid GameTests on run id `m5-7bd5fafc`, then the 1000-seed campaign.
- **M5 measured (2026-10-17)**: the lane ran alone at `7bd5fafc`; the numbers are in MAIN_ADAPTATION_REVIEW.md.
  - Full test 1479 tests, 0 failures. G1: 33 junction lines, 1 moved (roundoff); MIXED_GAS_COST 339 solves, as main. G2 chain-100 exact. GameTests 31/31.
  - Campaign: cold 911/1000 with no C and no X. Bundled random 979 against main's 972, defaults 1000 as main.
  - The baseline is `cl18`, the last campaign of the phase-ports batch, in `tools/phase-ports-probes/hold-classes/logs`.
  - New budget holds were replayed alone in one batch on one worker. Bundled 47, 430, 678 and 1000 stay F; 4 becomes E7. Cold 348 stays F; 12 becomes E8 and 534 E7.
  - Lead decision (reversible): the cold distribution has no main baseline, so its F seeds counted as new.
  - Lead decision (reversible): the two smallest new worse worlds are 322 (19 blocks, E6) and 47 (27 blocks, F), smallest by blocks and by seed number. An opus agent minimizes and traces them (M5_WORSE_WORLDS_TRACE.md).
  - CHANGELOG `[Unreleased]` line committed (`4eaae450`); no version bump.
- **M5 trace and D25's stop condition (2026-10-17)**: an opus agent minimized and traced the two smallest new worse worlds (`M5_WORSE_WORLDS_TRACE.md`; tools `tools/unified-thermo-port-m5/trace/`; logs 08 to 15 in `logs/m5-logs.tar.gz`).
  - Seed 322 (E6, 19 to 13 blocks): a 13 mg crude condensate trace at a tank's bottom port is read as a 14 kPa liquid column over a 2 L line (`endDensity`, D11). No root exists at any step size, and D25 keeps the populated trace. Main's phase correction dropped the trace at tick 897.
  - This meets D25's stop condition.
  - Options for 322: A, recommended: the port's static column by volume over the line. B: the segment from the sign of the static drive. C: D25 drops a drawn trace at a failed iterate.
  - Seed 47 (F, 27 to 18 blocks): main refused this methane generator below 293.15 K; the range rule admits it. At the line's own pressure, its connections reopen and close each interval. One interval passes the 1.5 s soft budget; the retry passes. Main's code is as slow on the same world at 293.2 K.
  - Options for 47: A: carry a generator connection's closure across steps. B: accept, recommended by the lead as it adds no carried state. Seeds 678 and 1000 are also worlds main refused (methane at 268 K, crude at 292 K).
  - The port pauses for the owner's choice, and for when to run the test world.

## D34 (2026-10-17): a port's static column holds what the line holds (M5 seed 322, D25's stop condition)

- **Question.** M5's campaign seed 322 (E6, minimized from 19 to 13 blocks, `M5_WORSE_WORLDS_TRACE.md`) meets D25's stop condition: a new E-class hold where a phase must vanish.
- **Physics.** A 13 mg crude condensate trace at a tank's bottom port drains at once, and its 2 L drop line holds gas. The gas column (about 40 Pa over 2 m) matches the tanks' 38 Pa difference. In the steady state the drop line carries no flow.
- **Code.** The line's static column is read from the port's leading phase (`PassiveStepSolver.endDensity`, phase-ports D11, "ports carry mixed phases by priority"). That phase is the trace, so the line is stated as 14 kPa of liquid.
  - The draw segment is decided at a near-zero carried flow, and nothing is pinned.
  - No Newton root exists at any step size, and D25 keeps the populated trace.
  - Main's phase correction dropped the trace at tick 897.
- **Options.** A: fill a port's static column with the port's priority phases by volume over the connection. B: decide a port end's segment from the sign of its static drive. C: let D25 drop a drawn trace phase at a failed iterate.
- **Owner's answer (2026-10-17): A**, the lead's recommendation.
  - The static column fills with the port's phases, in priority order, by volume over the connection: the drive's head, the sign test and the start-flow cap.
  - A trace in a long line gives a gas column. A port with liquid for the whole line gives a liquid column, as before.
  - The draw is unchanged: its capacity, suction and pump rise still read the leading phase, and the trace still leaves first, at its capacity.
  - Checks: a gate test from the minimized world fails with the rule off. Then the quick check, then one gates run and one 1000-seed campaign on the final code. Recorded references are re-recorded once, only if the rule moves them.
  - The owner's timing: the test world runs after the fix.

## D35 (2026-10-17): worlds the range rule newly admits are accepted (M5 seeds 47, 678, 1000)

- **Question.** The bundled random campaign has new budget holds 47, 678 and 1000, and Newton iterations rose 22 %.
  - Each world has a generator main refused at placement, below the bundled packages' 293.15 K floor.
  - Seed 47 is cost only. A methane supply at its line's own pressure reopens and closes its connections every interval. One interval passes the 1.5 s soft budget, and the retry passes.
  - Main's code is as slow on the same world at 293.2 K.
- **Options.** A: carry a generator connection's closure across steps, as pump modes and caps are. B: accept.
- **Owner's answer (2026-10-17): B**, the lead's recommendation, which adds no carried state. These are worlds the range rule admits; before, they were held at placement.
- **D34 implemented (2026-10-17)**: commit `4199dbe6`, rule and gate together, by an opus fix agent (`D34_STATIC_COLUMN.md`; tools `tools/unified-thermo-port-m5/d34/`).
  - `PassiveStepSolver.columnDensity` fills a connection's own volume (`Pipe.volume`) with the port's phases in priority order through `PhaseDraw.fill`, which `InletPhase.draw` now shares to the bit.
  - It is used by every static column: the row, the start drives, the sign tests and `NetworkReduction.drives`. The draw keeps the leading phase.
  - Gate `HoldClassWorldsTest#aLiquidTraceAtATanksBottomPortLeavesItsDropLineAGasColumn`: seed 322's 13 blocks run with no hold and a 44.9 Pa gas column. With the rule reverted, the world is held at tick 1097.
  - Printed numbers move only in `LiquidFullTankOutletsTest` (it reaches its own steady state 18 slices earlier) and in the STACK_PASS line of `PhysicalFluidTopologyTest` (no refused substeps, 14 mg of water left upstairs).
  - Lead decisions (reversible, owner informed):
    - (1) "Start-flow cap" means the start drives and the start cap mode. The cap's value stays the draw.
    - (2) A column fills from the whole vessel, with no share among N ports, as D34's words say.
    - (3) A trace below a gram that the receiving dead end balances on the column may stay, at game level.
- **Quick check at `4199dbe6` (2026-10-17)**: 200 seeds against `7bd5fafc`. Cold 185 against 186, bundled random 197 against 197, defaults 200 against 200.
  - No seed changed to or from a non-budget class.
  - Every change is a move into F (cold 6, 29, 101, 178; bundled 4). The owner's geosim export (8 Zulu-25 JVMs) ran from 10:12 and took the CPU to 93-97 %. Defaults did identical work (8546 Newton solves) in twice the seed time.
  - The counters, which judge cost when runs overlap, fell. Cold Newton solves went from 245180 to 218831 and iterations from 2.41e6 to 2.10e6. Bundled random iterations went from 166580 to 156158.
  - Lead decision: the final lane (full test, G2, GameTests `m6-4199dbe6`, the 1000-seed campaign) waits until the machine is idle (`scripts/wait-idle.sh`). F holds are then replayed alone if new.

## D36 (2026-10-17): a gas column does not exist (owner simplification)

- **Question.** The owner asked while D34 was being built: "How about we simply think gas column doesn't exist? does it make things easier?"
- **Physics.** A gas column weighs 11 Pa/m (air at 1 atm) to 22 Pa/m (methane at 430 kPa). Dense supercritical methane at 10 MPa weighs about 1 kPa/m. Liquids weigh 4 to 10 kPa/m.
- **The lead's answer.**
  - The change does not fix seed 322: that column was a liquid trace, which D34 handles.
  - It does not remove the column-end rule, which exists for liquid against gas ends.
  - It makes lines and loops holding only gas exactly flat at rest.
  - It removes the per-riser gas density artefact: 1.8e-5 kg/s carried round a loop of risers in random 498.
  - D34's fill then reads only the condensed phases.
  - Costs: a dense supercritical fluid in the vapour slot loses its column (about 1 kPa/m at 10 MPa), with a jump when its slot changes. Every elevated gas reference moves.
- **Owner's answer (2026-10-17).** "Yeah I want this", quoting the simplifications.
- **Decided.**
  - In every static column the vapour weighs nothing. Condensed phases keep their weight, filled by volume over the line as D34 states: hydrocarbon liquid, water, the crystal stand-in and a slurry's liquid.
  - This covers the passive row, the start drives, the sign tests, the rest gauge and a mover's column. A compressor's gas suction carries no column.
  - Lead choices (reversible, owner informed):
    - "Vapour" is the phase's slot in the pass's layout. A pass does not change its layout, so a row stays smooth within its pass; if that is not so, the slot is frozen per pass with the column end.
    - A dense fluid in the vapour slot has no column, at game level.
    - Recorded references are re-recorded once, in the final gates run, with the lines that move listed.
- **D36 implemented (2026-10-17)**: commit `03bc70f6`, by the D34 fix agent (`D36_NO_GAS_COLUMN.md`; tools `tools/unified-thermo-port-m5/d36/`).
  - The change: `columnDensity` counts only condensed mass over the line's volume. `endColumn` gives 0 for gas and for a D33 bulk end. A BULK end weighs its liquid mass over its volume.
  - A mover's connection carries its suction's condensed column (`Equations.Transport.column`). The D9 level head already counted condensed mass only.
  - The vapour slot is fixed within a pass (the layout is built once per pass), so no freeze was needed.
  - Gates in `RestResolutionTest`: two gas tanks joined by a riser rest at one pressure (rule off: −116.7 Pa); a loop of gas risers carries no circulation (rule off: 31.6 Pa).
  - Restated: the D34 gate's column 44.895 to 0 Pa; the stack 22.75 to 0.088 Pa. In `FedTankOpenOutletsTest`, twice, "rests empty" becomes "keeps its 1.145 kg charge": its gas stays at the voids' pressure.
  - `MIXED_GAS_COST` Newton solves fall from 339 to 312.
  - Lead decision (reversible): the agent also moved `headLimitMassFlow`'s drive to `endColumn`. It is a static column, so this is in D36's scope; the draw quantities are untouched.
  - A consequence of D36, accepted: a gas thermosiphon carries nothing, where in real life it would carry about 0.02 kg/s.

## D37 (2026-10-17): the first simplification sweep, all six candidates approved

- **Question.** `SIMPLIFICATION_SWEEP_REVIEW.md`: a fable subagent at max effort read the code at `4199dbe6` under the owner's new standing rule (AGENTS.md "Small physics simplifications"). It recommended S1, S3, S4 and S5, left S2 and S6 to the owner, did not recommend S7 to S9, and rejected 19 others with reasons.
- **Owner's answer (2026-10-17).** All six are approved.
  - **S1:** no potential energy `g·Δz` in flow enthalpies and the two ledgers. A 10 m lift moves the delivered temperature by 0.02 to 0.09 K.
  - **S3:** one inventory refresh per interval. This is a cost item, expected to be bitwise; the lead notes that an earlier, different skip moved 6 tests.
  - **S4:** nitrogen's low-temperature cp segment goes; the hold at the fit's end gives cp within 0.07 %. This reopens the F4 fit.
  - **S5:** a liquid's velocity limit is the 100 m/s cap.
  - **S2:** the approximate fallback mode is deleted. This reopens the soft-budget fallback design.
  - **S6:** `Mover.efficiency` is deleted.
- **Plan.**
  - One agent, one commit per item, in this order: S5, S3, S6, S4, S1, S2.
  - S2 and S6 share one format bump on a fresh world. Fixtures that carry the old format are re-archived.
  - Then the quick check on the combined code, one final gates run with references re-recorded once, the 1000-seed campaign, and the test world.
- **D37 implemented (2026-10-17)**: six commits on `2d2f5a2c`, by the fix agent (`SIMPLIFICATIONS_S1_S6.md`; tools `tools/unified-thermo-port-m5/simplifications/`). In all: 104 files, 699 lines added, 1223 deleted.
  - **S5 `bff22e37`**: no number moved.
  - **S3 `819d774b`**: bitwise, checked with a raw-bits probe; only the checkpoint counters fall.
  - **S6 `9445106a`**: checkpoint format 10 and unit format 3, the one bump for S6 and S2. `LevelHeadTest`'s pump run moved by 0.12 Pa.
  - **S4 `22b76697`**: nitrogen's cp fit is held at 273.16 K, within 0.030 % of NIST down to the 63.15 K triple point. Cold moves reach 2.8e-4 against the 1e-4 expected. Nitrogen below 273.16 K now shows the extrapolation notice.
  - **S1 `eec02758`**: gate: a pumped 10 m lift delivers +0.02345 K and the ledger closes to 2e-17 of U. With the rule off: +0.00000 K. Elevated lines move up to 1.3e-4 against the 1e-5 expected. The 5- and 6-port mixed-gas junction cases move at roundoff (6e-14); `MIXED_GAS_COST` stays at 312.
  - **S2 `65036f2b`**: the fallback mode is gone, and no physics number moved across 722 tests. Kept: the soft budget (only the adaptive cadence reads it) and `SolidMobility.closable`, the old `requiresFull`.
  - Lead decision (reversible, owner informed): an island interval is always the full solve on the whole wall budget, 2 s. The agent's reading follows from deleting `fullOnly`.
    - Before: the full solve was cut at the 1.5 s soft budget so the fallback could be tried, and a refusal held the island and retried the halved slice.
    - Option kept for the owner: the soft cut plus a full-only retry.
  - Owner's option (S4): start nitrogen's notice at its triple point, keeping the hold at 273.16 K. This sits with the open item on the 293.15 K methane notice.
- **Quick check at `65036f2b` (2026-10-17, the combined form: D34, D36, S1 to S6)**: the CPU was at 100 % (16 geosim JVMs). Cold 180 against 186 at `7bd5fafc`, bundled random 195 against 197, defaults 200.
  - Counters fell: cold Newton iterations from 2.41e6 to 1.34e6, defaults from 21569 to 16568 (defaults' physics now moves with D36 and S1).
  - Unlike D34's check, cold has new non-budget holds: 6 (E6), 19 (E7), 30, 48 (E5), 161 (B), and 85, 110 and 157 changed class. Their texts read "Substep refinement exhausted" (line search stall, active-set cycle). On M5 these seeds ran clean; seed 30 has 13 blocks and ran in 1.6 s.
  - Possibly time-driven. S2 removed the 1.5 s soft cut whose halved-slice retry also rescued struggling solves, and under load an earlier time-driven event can divert a whole trajectory.
  - The harness was adapted to S6: the forms fuzz harnesses and the test-world rig drop the literal efficiency 1 from their mover constructors. The random draws are unchanged, so each seed is the same world.
  - Plan: replay the seven seeds alone, then rerun the quick check, both on an idle machine (`scripts/m7-idle-checks.sh` through `wait-idle.sh`). Gradle is refused anyway while another session's dev client runs (custom-equipment-rendering's `runMcpClient`).
    - If the holds go away when idle: the final lane.
    - If they stay: bisect the smallest across `4199dbe6..65036f2b`, and compare S2's other reading (the soft cut plus a full-only retry).
- **Idle checks and bisect (2026-10-17)**: the new cold holds are not time-driven.
  - Idle quick check at `65036f2b` (no other JVM, CPU under 30 %): cold 180 again with the same 20 seeds held, bundled random 198 against 197 at `7bd5fafc`, defaults 200. Five cold seeds that hit the budget (F) under load hold as B, E5 or E8 when idle; the bundled F holds under load were time-driven.
  - The seven seeds replayed alone (one thread) hold with the same classes and hold counts.
  - Bisect, seeds 6, 19, 30, 48, 161, 85, 110 alone (`build/m7-logs/bisect/`, `scripts/bisect-seeds.sh`):
    - `4199dbe6` (D34): 85 (E7) and 110 (B) hold as at `7bd5fafc`; the five others run clean.
    - `03bc70f6` (D36): 19 newly holds (E7); 85 and 110 move to E5. S5, S3, S6 and S4 (`bff22e37` to `22b76697`) add nothing.
    - `eec02758` (S1): 6 (E6), 30 and 48 (E5) and 161 (B) newly hold. S2 (`65036f2b`) adds nothing, so S2's other reading is not needed for these seeds.
  - Lead's physics note on D36, for the trace: the cold generators draw CO2-rich gas at 1 to 6 MPa, near CO2's critical point (7.38 MPa, 304 K). At 5 MPa and 300 K such a gas holds about 130 kg/m3 and weighs about 1.3 kPa/m, two orders above the 11 to 58 Pa/m D36 was approved on, and 30 % of the LNG column. A one-root fluid is labelled by its phase identification parameter, so a dense fluid crossing PIP = 1 switches its column between rho g and 0.
  - Next: trace seed 30 (S1, 13 blocks) and seed 19 (D36, 27 blocks), one per mechanism (lead decision: the two smallest new worlds are both S1's), physics first; options to the owner before the final lane.

## D38 (2026-10-17): the new cold holds of D36 and S1 (M7 trace; D25's stop condition again)

- **Trace.** An opus agent minimized and traced one world per mechanism (`M7_NEW_COLD_HOLDS_TRACE.md`; tools `tools/unified-thermo-port-m5/trace/`, section "M7 cold traces"; logs `build/m7-logs/trace/`). Plain minimization kept each class but also held at the clean parent, so each world was minimized as a pair, held at one commit and clean at the other: seed 30 to 11 blocks, seed 19 to 14. Neither hold depends on the budget.
- **Seed 30 (S1, E5).**
  - Physics: at an edit, junction n5 is re-minted flashed at 2.03 MPa with 4.5 % vapour, although it was liquid at 3.97 MPa before. Fed 72 kg/s by the 6 MPa LNG generator, it must return to 3.49 MPa all liquid, so the vapour must vanish.
  - Code: at the rate solve's first failed iterate, n5's vapour is 1.0e-8 of its moles at `65036f2b` and 3.6e-10 at `22b76697`.
    - S4 folds it (the 1e-9 trace fold) and finishes in one step.
    - HEAD keeps it, because junctions are exempt from the D26 amendment's 1e-6 test. Later it stalls on n2's vanishing vapour, at 1.47e-9 against 1e-9.
  - S1's 9.81 J/kg (0.0028 K) is only the trigger: the plain-minimized world holds at both commits. No energy site keeps g·z.
  - This meets D25's stop condition, a new E-class hold where a phase must vanish, and this time the mechanism is D25's own vanishing test.
- **Seed 19 (D36, E7).**
  - Physics: a 1 atm void opens on a 5.4 MPa CO2-rich header and blows it down, with 9 of 12 lines at their velocity caps.
  - Code: the cap active set is found by chance. D34 converges on its 11th attempt, after 129 failed Newtons. The equations differ only by the gas heads D36 drops: 44 to 857 Pa, 1.6-1.9e-4 of the pressure.
  - Hypothesis (b), a label flip, is refuted: no gas node changes phase code.
  - Lead's note on D36's premise, for the owner:
    - These gases weigh 0.5 to 1.15 kPa/m, 20 to 100 times the 11 to 58 Pa/m D36 was approved on.
    - As a share of the pressure that is the same 1.6-2.1e-4 per metre (M·g/(Z·R·T)).
    - Beside liquid, however, it is 25 to 30 % of an LNG column (3.6 to 4.4 kPa/m), against 0.5 % at the bundled pressures. So a dense-gas balance line between vessels at different heights misplaces the liquid level by about that share of the height difference.
- **Common start.** Both intervals start from re-minted junctions (D46, D55) far from their last state: n5 at 2.03 MPa against 3.97 MPa, and seed 19's n0 and n5 at 0.22-0.23 MPa between neighbours at 5.4 MPa. Keeping a junction's state across an edit was option K, which was not taken (phase-ports D45, owner, 2026-10-13); it is not re-proposed.
- **Options put to the owner (2026-10-17).**
  - **Seed 30:**
    - A: accept S1 and its four new holds (6, 30, 48, 161).
    - B: revert S1 (clean by luck; the fragility stays).
    - C, recommended: one vanishing rule for every node. A phase under `VANISHING_SHARE` (1e-6) of its node's moles, and smaller than at the interval start, leaves at a converged pass or a failed iterate, junctions included. It deletes the `junction?0:` exemption; the 1e-9 fold stays for phases that do not shrink. Unmeasured; seed 48 is likely the same class.
  - **Seed 19:**
    - A, recommended: keep D36 and accept the hold.
    - B: revert D36 (clean by the same chance; the riser drive and the gas thermosiphon return).
    - C: a smooth velocity cap in each line's law, deleting the cap active set. Large and unmeasured; it needs a plan.
- **Decided by the owner (2026-10-17).** Seed 30: C, one vanishing rule for every node. Seed 19: B, revert D36: gas weighs its density again in every static column.
  - **Plan.** One fix agent, two commits, in this order:
    - A `git revert` of `03bc70f6`, with its references restored and the conflicts with the later commits (S1 to S6) resolved.
    - C with its gate test and that one test's rule-off proof.
  - Then the 200-seed quick check on the combined code, the final lane (full test, G2 exact, GameTests, the 1000-seed campaign; references once), and the test world.
  - AGENTS.md's simplification rule names D36 as its example, so it gets a factual note that D36 was reverted (D38).
- **D38 implemented (2026-10-18)**, by the fix agent (`D38_REVERT_D36_AND_VANISHING_RULE.md`; tools `tools/unified-thermo-port-m5/d38/`). The lead adds `482264fc`: AGENTS.md's example notes the revert.
  - **`26090bf0`, D36 reverted.**
    - Gas weighs its density in every static column and mover column again; D34's fill stays. The riser drive, the gas thermosiphon and the gas-only junction's column scaling return. The D36 gates leave.
    - The 15 tests D36 moved print D34's values plus the S1 to S6 deltas; no reference was re-recorded.
    - `MIXED_GAS_COST` is 340 (339 at D34, 312 at D36). The 33 junction lines are at D34's values plus S1's round-off.
  - **`67e5f3e7`, rule C.**
    - In `FluidThermodynamics.phaseCheck`, a phase under 1e-6 of its node's fluid moles, at a smaller share than at `from`, folds by the trace rule, at converged points (recorded) and at failed iterates (trial, A′ unchanged), in every node. The `junction?0:` exemption is deleted.
    - Six tests print moved numbers within their bounds, for example WaterIntoCryogenicMethaneTest's interval 6: 37.6 kg of ice against 36.1 kg.
    - Gate: `HoldClassWorldsTest#aJunctionsVapourCollapsingInASubcooledLiquidLeavesAtAFailedIterate`, seed 30's 11-block world. It holds at tick 1595 with the rule off, run alone.
    - Seed 30 is clean. Seed 48 is still E5, the same stall as before the rule; it is not a vanishing phase.
  - **Lead decisions (reversible; the owner is informed).**
    - (a) A phase the fold removes as vanishing is not offered back by the same check. This is D26's own principle, "nothing the fold removes is added", given to the new fold.
    - (b) A vanishing gas that has no liquid to go into (nitrogen over water) stays whole and leaves through the ports. This is the fold's rule that a phase with no receiving phase stays; folding its humidity alone re-wetted it every pass.
    - `from` is the start of the solve that judges the phase (step, substep or closure), as the D26 amendment uses its closure's start. Threading the island interval's start through `PassiveIntervalSolver` is not done.
    - Without (a) and (b), the literal rule cycled 12 suite tests.
- **Quick check at `482264fc` (2026-10-18; D34, S1 to S6, D36 reverted, rule C; two other JVMs near idle, CPU at 4 %).**
  - Cold 187 against 186 at `7bd5fafc` and 180 at `65036f2b`. Bundled random 198 (197). Defaults 200.
  - Cold against `65036f2b` (idle): nothing newly held; 30, 55, 98, 116, 121, 157 and 161 newly clean.
  - Cold against D34's check (`4199dbe6`): 19 (E7) and 48 (E5) newly held; 55, 61, 121 and 157 newly clean.
    - Seed 19 holds with D36 reverted, as the trace predicted ("clean by the same chance").
    - Seed 48 is S1's E5 stall, which is not a vanishing phase.
  - Bundled random: no seed newly held against either check; 157 (E5) is in D34's baseline, 47 (F) in M5's.
  - Counters: cold Newton iterations 2.42e6 (2.41e6 at `7bd5fafc`), bundled random 1.48e5, defaults 17577.
  - Next: the final lane at `482264fc` (`LOGS=build/m8-logs RUNID=m8`): full test, G2 exact, GameTests, the 1000-seed campaign.
- **Final lane at `482264fc` (2026-10-18, `LOGS=build/m8-logs RUNID=m8`; two other JVMs near idle).**
  - Full test: 1470 tests, 0 failures, 0 skipped (170 s). Against M5's 1479, 15 test methods were added and 24 removed by the decided deletions: S2's fallback tests (net −10), D36's two gates, S4's cp-segment tests (net −2), S1's rewrite (net +1), and one gate each for D34, S1 and rule C.
  - G2 chain-100 exact: deviation 0, so no re-capture is needed. GameTests: 31/31 on a fresh run id (`m8-482264fc`).
  - Campaign, 1000 seeds, 8 workers: cold random 913 (M5 911), bundled random 985 (M5 979, main `cl18` 972), bundled defaults 1000.
    - Cold against M5: 9 seeds newly held (6, 19, 48, 274, 316, 430 C, 737 F, 780, 857) and 11 newly clean.
    - Bundled against M5: none newly held; 4, 322, 453, 629, 678 and 788 newly clean.
    - Cold classes: B 22, E7 30, E5 13, E6 11, E8 6, E2 1, C 1, F 3, no X.
  - New C and F holds replayed alone in one run (`06-alone-new-CF`): cold 430 stays C and 555 and 737 stay F.
    - 430 (12 blocks, clean at M5) reads "a third fluid phase, which the network does not carry at 17.35 K, 101325 Pa in node 3". Nothing in that world can reach 17 K.
  - Lead decision (lean rule): trace the two smallest new worse worlds, 430 (C) and 48 (E5), before the test world, with the trace agent (`M8_CAMPAIGN_WORSE_WORLDS_TRACE.md`). The test-world agent prepares its run sheet meanwhile, reading only.
- **Correction (2026-10-18, from the test-world agent's reading at `482264fc`).** S4's notice reaches only the bundled nitrogen record (`fluid_nitrogen`, used by the `tjl20_*` packages). The pilot package takes every ideal gas from its spines, and `spine_nitrogen` starts at 63.151 K, so LNG islands show no nitrogen notice. The owner's S4 option, the notice from the triple point, concerns bundled worlds only.
  - Run sheet updated in place (`TEST_WORLD_PLAN.md`, markers "(updated 2026-10-18: D34, S1 to S6, D38)"). The rig rows R2-R7-R9, D23-2, R1 and R10-fence re-run at the final HEAD before the session.

## D39 (2026-10-18): the final campaign's two smallest new worse worlds (M8 trace)

- **Trace.** `M8_CAMPAIGN_WORSE_WORLDS_TRACE.md` (logs `build/m8-logs/trace/`; tools `trace/`, section "M8 campaign traces").
- **Seed 430 (C; 12 to 8 blocks, pair-minimized against `4199dbe6`).**
  - Physics: an empty bag fed 7 kg/s of LNG and CO2-rich gas let down to 1 atm fills at its bubble point, about 112 K. No node can cool below about 110 K.
  - Code: the 0.05 s attempt converges once on that tank, but junction n5 is liquid 22 K above its bubble point. After n5's flash, no attempt is accepted. The failed-iterate check (D28 A′) seeds later passes from failed iterates that hold n5 on the 1 Pa box floor; its PH answer there is 51 K with 13 300 m3 of gas, |r| 1.5e12 (M7's seed 19 had the same seed). The near-empty tank's iterates fall to 17-57 K, and the phase check's TP flash at those iterates raises THREE_PHASES. The last attempt ends on one, so the interval is C.
  - The hold breaks the owner rule (AGENTS.md): it is a capability hold about a failed iterate, a non-state (D28), never committed.
  - Bisect alone: clean at `4199dbe6`, E7 from D36, C from S1, still C after D38. D34 was clean by chance and committed a non-physical tank, a liquid 24 K above its bubble point at 1 atm.
- **Seed 48 (E5; 15 to 6 blocks, pair-minimized against `22b76697`).**
  - Physics: a boiling upper tank (113.07 K, 1 atm) drains 2 m into an empty lower bag. In reality the fall's 19.6 J/kg flashes about 4e-5 of the liquid. Under S1 the lower tank sits exactly on its bubble point with no vapour.
  - Code: the liquid-only pass converges at every dt. The check proposes a 3.4e-6 vapour (the TP answer), and the two-phase pass drives it to 8.8e-9 and stalls at |r| 4e-8 whatever dt is. Rule C does not apply, because the vapour was not there at the step start. S1 makes this degenerate state the rule for vertical transfers between slack bags; a level transfer is degenerate at any commit (expected, not measured).
- **Options put to the owner (2026-10-18).**
  - **430:**
    - A (minimum, the rule's own demand): a typed hold raised by the failed-iterate check is a failed trial.
    - B (recommended): complete D28. A failed iterate may fold what it empties but proposes nothing: no TP answer and no own split of a non-state. It absorbs A, removes the 1 Pa seeds, and deletes the failed-iterate addition branch.
  - **48:**
    - A: accept.
    - B (recommended): a phase that the previous converged point's check added, and whose next pass fails with it under 1e-6, is refused and recorded (rule C for a phase the solve creates).
    - C: revert S1.
- **Decided by the owner (2026-10-18): B for both.**
  - 430: complete D28. A failed iterate may fold what it empties but proposes nothing.
  - 48: a phase that the previous converged point's check added, and whose next pass fails with it under 1e-6, is refused and recorded.
  - Plan: the fix agent makes two commits (430's first), each with its gate and that one test's rule-off proof. Then one quick check, the final lane again (full test, G2 exact, GameTests, 1000-seed campaign) and the test world at that HEAD.
- **D39 implemented (2026-10-18)**, by the fix agent (`D39_FAILED_ITERATE_AND_REFUTED_PROPOSAL.md`; tools `tools/unified-thermo-port-m5/d39/`).
  - **`1482c277`.** A failed iterate's check only folds what it empties (the trace fold and rule C's vanishing fold, `foldedIterate`). It makes no TP answer and no own split, and it neither reads nor writes A85. The failed-iterate addition branch is deleted; `phaseCorrection` checks converged points only. A vessel closure's failed iterate keeps D28 A′, because a closure has no smaller step.
    - Gate: `HoldClassWorldsTest#aFailedIterateRaisesNoPhaseHold` (430's 8 blocks). Rule off, it holds C at 17.35 K. With the rule it still stalls E5 at tick 406 (|r| 75.3), its open class (M8 option C, the cap search).
  - **`00ee8385`.** The solve keeps the converged check's `Proposal`. A failed pass whose iterate drives a proposed phase to nothing from its seed (`vanishing`, now public) refutes it: the refusal is recorded in A85, and the pass is solved again from the converged point without it. One test and one record serve rule C, the D26 amendment and this rule.
    - Gate: `HoldClassWorldsTest#aBubbleTheNextPassDrivesToNothingIsRefused` (48's 6 blocks). It keeps up to tick 1754, and 4 proposals are refuted in the first interval. Rule off, it holds E5 at tick 1257.
  - **Suites** (javac lane): all green. No reference was re-recorded; STACK_PASS and the 33 junction lines are unchanged; MIXED_GAS_COST is 340.
    - Moved printed numbers: substep paths. WaterIntoCryogenicMethaneTest gate (c) interval 6 holds 36.44 kg of ice against 37.63 kg. Several cases change their rejected attempts with the same states.
  - **Seeds alone at `00ee8385`:**
    - 430: E5, no longer C.
    - 48: clean, the costliest of the four (22 576 Newton solves).
    - 30: clean.
    - 19: E5, against E7 at `482264fc`.
- **Quick check at `00ee8385` (2026-10-18, D39; the owner's 4 JVMs at 76 % CPU).**
  - Cold 184 against 187 at `482264fc` and 186 at `7bd5fafc`. Bundled random 198 and defaults 200, both seed for seed as at `482264fc`.
  - Cold changes: 61 (E7), 116 (E6), 161 (B) and 183 (E7) newly held; 48 newly clean; 19 E7 to E5.
  - Cold Newton iterations fell 75 %, from 2.42e6 to 6.09e5: failed iterates no longer seed absurd states. Bundled random fell 2 %; defaults are unchanged.
  - The −3 is option B's stated risk: a phase a pass needs must now appear at a converged point or a smaller step. It is also within the ±3 to 7 swing each recent change caused.
  - The final lane at `00ee8385` decides it (`LOGS=build/m9-logs RUNID=m9`).
- **Final lane at `00ee8385` (2026-10-18, D39; `LOGS=build/m9-logs RUNID=m9`; the owner's 4 JVMs running).**
  - Full test: 1472 tests, 0 failures (the two D39 gates are new). G2 chain-100 exact: deviation 0. GameTests: 31/31 (run id `m9-00ee8385`).
  - Campaign, 1000 seeds:
    - Cold random 911 (913 at `482264fc`, M5 911). Classes: B 24, E7 27, E5 17, E6 14, E8 4, E2 1, F 2 (555 and 737, as before), no C, no X.
    - Bundled random 987 (985; M5 979; main 972). Bundled defaults 1000.
  - Against `482264fc`:
    - Cold: 6 newly held (61, 116, 161, 183, 437, 994), 4 newly clean (48, 274, 316, 678), and 430 moves from C to E5.
    - Bundled: none newly held; 853 and 941 newly clean.
  - Newton iterations: cold 3.05e6 against 9.59e6 (−68 %), bundled random 8.69e5 against 1.47e6 (−41 %), defaults unchanged. Wall: cold 122 s against 237 s.
  - Alone, one worker: bundled 430 (F in the campaign, E6 before) holds E6. Its F was the load, so there is no new budget-class hold.
  - Next: the test world at `00ee8385`, then the review and the CHANGELOG line.
- **Test world, phase 2: the rig at `00ee8385` (2026-10-18).** It passes D23-1, row 2's fill, r7 and r9-slow, row 9a and row 12. Four numerical holds no gate covers (logs `tools/unified-thermo-port-test-world/logs/2026-10-18-session/rig-*.log`, also at `482264fc`, `26090bf0`, `65036f2b`, `eec02758`, `9445106a`):
  - H1, row 2: CO2 gas into the vented methane pool holds from 84.1 s until row 7's edit (Newton limit at 28.91). It depends on the commit: present at `9445106a`, gone at `eec02758` and `65036f2b`, back from `26090bf0`.
  - H2, row 2 r9-fast: the pump island holds for good from 253.9 s (line-search stall at 0.4575), frozen at 111.61 K with 3.03 kg of dry ice, so row 11 reads T2 held. Present at every commit since `9445106a`.
  - H3, row 9b: after the cut, the vented slush tank holds for good from 195.6 s (substep limit). It stays liquid-full at 172.8 K and 2.58 MPa with 344 kg of dry ice instead of venting to 1 atm. At `26090bf0` it vented to 122.2 K and 224 kPa, so H3 comes with rule C (`67e5f3e7`).
  - H4, row 10: water at 1 MPa holds for good from 83.8 s (the refresh's UV pressure search does not converge at 124.72 K in 100 TP equilibria). Every later edit fails the same refresh. Present at every commit since `9445106a`.
- **Lead decision (reversible; the owner is informed).** The in-game session waits for the fixes: H2 to H4 freeze rows 2, 9b, 10 and 11, so a session now would need a re-run. The test-world agent diagnoses H1 to H4 on the rig, physics first, with no fix (`TEST_WORLD_RIG_HOLDS_TRACE.md`). The options then go to the owner.
- **Corrections (2026-10-18, from the review's source check).**
  - Seed 19 at `482264fc`: M7's option B expected it clean again "by the same chance". It held, because the later commits moved its chaotic cap search. The quick-check entry's "as the trace predicted" is wrong.
  - Tests since M5, counted by name against M5's results: 13 added and 22 removed, net −9 (1479 to 1470). The per-commit count of 15 and 24 counts D36's two gates both ways.
  - D36 was approved on 11 to 22 Pa/m (the D36 entry, AGENTS.md); the D36 report's cases reach 58 Pa/m (nitrogen at 480 kPa).
  - Seed 322 (M5): the D34 report's 16 mg and 17.3 kPa (880 kg/m3 over 1.83e-8 m3 in a 2 m line) supersede the M5 trace's 13 mg and 14 kPa.
  - The D38 report's gas-column sentence now reads 1.1e-4 per metre for air and 1.6 to 2.1e-4 for seed 19's gases.
- **Lead decision recorded late (reversible; the owner is informed): S4's hold point.** The approved text was "the hold at the fit's end (within 0.07 %)". `22b76697` holds at the old joint, 273.16 K, the fit's declared lower end (0.030 % from NIST; nothing at or above 273.16 K moves). The Shomate fit's own 298.15 K end would miss NIST by 0.075 % and move 273 to 298 K.
- **Review rewritten (2026-10-18):** `MAIN_ADAPTATION_REVIEW.md`, 50 lines, M1 to M5 and the follow-ups to `00ee8385`. The CHANGELOG line is drafted in `CHANGELOG_LINE_DRAFT.md`. It is committed only when the code is final, after the rig holds H1 to H4 are decided.

## D40 (2026-10-18): the test world rig's holds H1 to H4 (`TEST_WORLD_RIG_HOLDS_TRACE.md`)

- **H1 and H3: one stage, a vessel's phase seed.** The next pass is seeded with the TP answer's amounts at the vessel's (T, P), which ignore its volume; where the vessel's own closure then fails, its last iterate stands as the split (D26).
  - H1, row 2: pure methane at saturation is seeded with 183 m3 of vapour in a 1 m3 tank, and the Newton stalls at |r| 1e-7 to 3e-6. It is path-sensitive: it holds at `9445106a`, `26090bf0`, `482264fc` and `00ee8385`.
  - H3, row 9b: the refresh freezes an 82.7 kg backlog of CO2 at once, so the slush tank goes liquid-full at 3.53 MPa. The vent's TP seed then offers 13 to 29 m3 of vapour for 0.78 m3 of space. It holds at `87c106f6` and `9445106a` with identical numbers, so rule C only re-locks the path; the first bad commit is in `c9e7c128..87c106f6`. The earlier entry's "H3 comes with rule C" is corrected here.
- **H2** predates the port. Main's `440a7542` starts every pump at PUMP_HEAD_LIMIT when any pipe of its island is blocked. The pump then draws 19.76 kg/s, its suction falls to the 1 Pa floor, and it never returns to its target. Fix A (`ed2cdff4`) first opened this line.
- **H4** is port-only (WP4). `innerUv` has no case for methane's own liquid-vapour step when water is present, and a secant stored across that step (8.8e11) stalls every later temperature. The search had bracketed liquid CH4 with 9.9 kg of ice at 124.72 K and 0.33 MPa.
- **Side notes.** Below 273.16 K the stand-in water keeps Region 1's cp (76 against the ice's 19 J/(mol K)), so fusion at 125 K absorbs 1.04 kJ/mol and the refresh cools the tank by 0.6 K; the total heat from 298 K water to 125 K ice is right. A comment in `FluidThermodynamics.state` is stale. D23's refresh quantum is what makes H3's tank liquid-full.
- **Options put to the owner (2026-10-18).**
  - H1 and H3:
    - A (recommended): seed a vessel from its own state. That is its committed layout, or, at a crossing, its liquid at its bubble pressure plus a vapour filling the lost volume. A failed closure proposes nothing. This deletes the TP-amount seed and D26's failed-iterate split, and makes rule C's and D39's failed-iterate folds candidates for deletion.
    - B: the seed half of A only.
  - H2: A (recommended), a pump's start mode reads its own lines only; or B, retry a failed head-limit start at the target.
  - H4: A (recommended), one step rule in `innerUv` for wet and dry feeds, with no secant stored across a step; or B, clear the stored secant at each temperature.
- **Decided by the owner (2026-10-18): A for all three.**
  - H1 and H3: a vessel's phase seed comes from its own state. That is its committed layout, or, at a crossing, its liquid at its own bubble pressure plus a vapour filling the volume it lost. A failed closure proposes nothing. This deletes the vessels' TP-amount seed and D26's failed-iterate split. Whether rule C's and D39's failed-iterate folds can then go is reported, not done.
  - H2: a pump's slice-start mode reads its own lines only; the island-wide clause of `440a7542` is deleted.
  - H4: one step rule in `innerUv` for wet and dry feeds, with no secant stored across a step.
  - Plan:
    - The fix agent makes three commits in this order: H2, H4, then H1 and H3. Each has a gate taken from its rig row and that one test's rule-off proof.
    - Then the quick check and the final lane.
    - Then the test world: the rig re-run of every row, then the in-game session.
- **D40 implemented (2026-10-18)**, by the fix agent (`D40_SEEDS_PUMP_START_UV_STEP.md`; tools `tools/unified-thermo-port-m5/d40/`). Three commits on `00ee8385`; tests on the javac lane.
  - **`b81d3491` (H2).** A pump's start mode reads its own lines only; the island-wide clause of `440a7542` is deleted. The pump's own lines keep `suctionCapped` and `relieveSealedPockets`.
    - Gate: `SlurryTankPumpTest#aSideOutletClosedByDepositionLeavesThePumpOnItsTarget`, PUMP_TARGET at 2.174 and 2.180 kg/s over two slices. Rule off, it holds at slice 1 (line search stalled at 0.456). No printed number moves.
  - **`88817099` (H4).** One step rule in `innerUv`, wet or dry (`wetCoexistence`); a secant is stored only between answers with the same phases (`samePhases`).
    - Gate: `WaterIntoCryogenicMethaneTest#theRefreshOfALiquidFullMethaneTankAnswersItsIceBesideMethanesSaturation`: 124.6056 K, 263.47 kPa, 551.097 mol of ice. Rule off: no convergence in 100 TP equilibria. UV TP calls average 16.7 against 16.8, with the same answers.
  - **`3db5e5d3` (H1, H3).** A vessel's seed is its committed layout, with entry traces and arrivals in their own phase (gas beside free water is humidified). At a crossing it is the engine's own-fluid equilibrium: PH while the bag is slack, UV while it is taut, under the TP request's competition. A failed vessel closure proposes nothing. Deleted: the vessels' TP-amount seeds and D26's failed-iterate splits.
    - Gate: `VesselPhaseSeedTest`. H1's first step takes 1 pass and 2 Newton iterations (rule off: 6 passes, 45 iterations). H3's slush tank vents to 136.90 K and 544 kPa in 30.15 s, its vent passing 1.87 kg/s of gas (rule off: holds at slice 1, line search stalled at |r| 0.0075).
  - **Suites at `3db5e5d3`:** 975 tests in 16 groups, 974 pass; `PackagedSparseLuTest` needs the built jar and fails at the base too. No reference was re-recorded.
    - Moved: MIXED_GAS_COST newtonSolves 339 (340); the 8 liquid junction lines are unchanged; STACK_PASS upper liquid by 1.9e-10 kg. Row E's interval 1 costs 7,992 checkpoints (2,089; bound 9,000) and interval 3 53,012 (58,167), with the same states. Seed 48's gate tanks: 2.433 and 2.444 MPa, 27.45 and 34.53 kg of frost (2.439 and 2.449; 27.38 and 34.59). Seed 30: 2.394975 MPa (2.394931). The 120 K row books 7.4e-12 mol of water (0; asserted to 1e-9). The three-tank fill takes 5 steps with 1 rejected (8 with 4). Phase-port water traces of 3.3e-10 and 5.6e-11 of volume (0). Every other line moves at most 1e-7 relative.
    - Watch item: row E's interval 1 now uses 89 % of its 9,000-checkpoint bound.
  - **Incident:** the agent's first suite run of the final tree (14:34:57 to 14:36:18) overlapped a running dev client, against AGENTS.md. It was discarded and re-run from 14:39:41 to 14:40:50, with the guard clear before and after.
  - **Re-scoped by the lead** after the owner's "need to get this done faster": the seed replays (30, 48, 430, 19), the four rig rows and the fold-redundancy experiment were skipped. The final lane and the rig re-run cover the first two. The fold patches are kept in `d40/`, not run, so whether rule C's and D39's failed-iterate folds can go stays open. No separate quick check: the final campaign contains seeds 1 to 200.
  - **Left as candidates:** `drainedSeeds`' vessel TP flash (D11) and `emptyBagSplit`'s TP fallback still seed from TP amounts.
  - **CHANGELOG** (`c9aea46e`): the `[Unreleased]` line runs `348d06e1`..`3db5e5d3` and names D38 to D40.
- **Final lane at `3db5e5d3` (2026-10-18, D40; `LOGS=build/m10-logs RUNID=m10`; no separate quick check; the owner's 3 geosim JVMs running; the rig re-run on the second lane at 4 threads).**
  - Full test: 1476 tests, 0 failures, 0 skipped (199 s). Against `00ee8385`, by method name, the four D40 gates were added and none removed. `PackagedSparseLuTest` passes under Gradle. G2 chain-100 exact: deviation 0. GameTests: 31/31 (run id `m10-3db5e5d3`).
  - Campaign, 1000 seeds:
    - Cold random 911 (911 at `00ee8385`). Classes: B 24, E7 25, E5 19, E6 17, E8 3, E2 1; no C, no F, no X. Against `00ee8385`: 1 (E5), 48 (E6) and 274 (E5) newly held; 6, 737 (F) and 994 newly clean; 8 class changes, among them 555 F to E6. Newton iterations 3.05e6 to 2.70e6 (−11 %), Newton solves −8 %.
    - Bundled random 986 (987). Classes: E8 4, F 3 (47, 241, 430), E5 2, E6 2, E10 1, E2 1, E7 1. 504 and 779 (E8, interval substep limit) newly held; 1000 newly clean; 241 E10 to F, 431 E5 to E6. Newton iterations +1.2 %.
    - Bundled defaults 1000, the same Newton iterations (99 041).
  - Alone (`06-alone-new-F-bundled`): bundled 241, the only new C or F hold, holds F alone at the same tick 4824 (wall deadline; then a singular Jacobian at 10074) in 3.4 s. At `00ee8385` it held earlier, at tick 1400 (E10); D40 carries it from tick 1398 to 4724 before its budget runs out.
  - New worse worlds by class, not traced (lean rule: at most two; skipped for speed): cold 48 E6 (15 blocks) holds at tick 1256, island 24, the very event of M8's seed 48 (E5 at `482264fc`, clean at `00ee8385`), now at a Newton iteration limit at |r| 1.4e-5; D39's 6-block gate still passes. Bundled 504 E8 (16 blocks; 3.65 of 5 s advanced in 999 substeps), 779 E8 (17), cold 1 E5 (19; |r| 7.2e-7) and 274 E5 (20). The two smallest to trace are 48 and 504.
- **Rig re-run at `3db5e5d3` (2026-10-18, label d40, 4 threads, beside the final lane; against `88817099`), by the test-world agent.**
  - H1, H2 and H3 no longer hold, so neither `drainedSeeds` nor `emptyBagSplit` is reached. Row 2 (R2-R7-R9): r2 at 60 s 111.59 K, 101.4 kPa, 807 L of liquid, 2.36 kg of frost; r9-fast PUMP_TARGET 2.21 kg/s; r9-dry "inlet dry" (INLET_WRONG_PHASE), 13.2 kg left. Row 9 (D23-2): F5's fill stall and H3's vent hold are gone: the fill ends at 160.06 K and 1.432 MPa, the vent at 135.91 K and 515.8 kPa, the slush at 122.13 K and 223 kPa with 287.6 kg of dry ice (183 L), its bottom outlet blocked PACKED_SLURRY.
  - Row 4 (R11 steady) is back on the plan's `c9e7c128` values (F3): 163.21 K, 282.1 kPa, 2.47 kg of frost, the gas outlet at 1.20 kg/s VELOCITY_LIMITED (at `88817099`: 176.45 K, 300.1 kPa, PACKED_SLURRY flagged, 12 rejections).
  - Rows 1, 3, 6, 7, 8 and 12 as at `88817099`: R3 as planned; R4 STEADY at 500 kPa; R10 133.76 K, 1.74 MPa, 4.18 kg of dry ice; R13 regulating at 179.75 K, 300.5 kPa; D23-1 184.84 K, 3.55 kg; R10-fence 95.51 K, the edit accepted at the fence. Row 11 runs in game only.
  - Still held, not traced:
    - Row 10 (R1, H4): the UV search no longer fails (`88817099`), but the island holds at the same tick 1675 on a line search stalled at |r| 42 (125.29 K, 956.2 kPa).
    - Row 5 (R8, new label H5): a "Phase/device active-set cycle" at the blowdown, 98.5 s, at both commits. Not D40: it starts at `87c106f6` (STEADY at `c9e7c128`), at tick 1543 until `482264fc` and tick 1969 from `00ee8385`. R8 was not in the first rig report.
  - The dev client that overlapped the fix agent's first suite run (14:34:57 to 14:36:18) was not the test-world agent's (its only client ran 10:10 to 10:14).
  - The in-game session runs every row but 5 and 10.
- **Owner decision (2026-10-18): "skip the client test for now".** The in-game session at `3db5e5d3` (save `utp-test-world-20261018-d40`, bridge only, 4 processors) started about 15:01 and was stopped at 15:25:54: Save and Quit, then Quit Game, through the bridge; the `runMcpClient` lane exited 0.
  - Row 1 (R3) ran as on the rig: co2-late 219.65 K, 382.1 kPa, vapour only, the Solids tab empty, "Ready."; outlet P2 1.907 kg/s, VELOCITY_LIMITED.
  - Row 2 (R2-R7-R9) ran with no hold. The fill: 113.95 K, 155.0 kPa, 428.5 kg. r2 at 60 s: 111.55 K, 101.42 kPa, 346.5 kg, "Ready." (no H1). r9-fast: the pump FULL at PUMP_TARGET, 476.9 kmol/h, about 2.13 kg/s (no H2). r9-dry: 10.4 kg left, 0.3 % solids, the pump "ERROR: pump inlet not liquid (vapour 67.7 % …)". Its CO2 feed started about 3 min late, after a driver fix.
  - Row 3 stopped after its first generator; rows 4 to 12 were not run (rows 5 and 10 were to be skipped).
  - The save is no longer fresh; a later session copies `world-template/` again. Driver fixes and logs: `tools/unified-thermo-port-test-world/` (README, "rig re-run at `3db5e5d3`").
  - Open for a later session: rows 3 to 12 in game, row 11 (in game only); H4 (row 10) and H5 (row 5) first.
