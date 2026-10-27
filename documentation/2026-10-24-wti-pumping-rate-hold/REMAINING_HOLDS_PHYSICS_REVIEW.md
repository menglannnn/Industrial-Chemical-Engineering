# Remaining holds grouped by physics

2026-10-24. Read-only correction to the previous numerical-stage ordering, revised to retain prior minimized-world evidence and rank investigations by discriminating power. Scope: eight cases still held at the end of final-code coverage (cb89f23a, numerical 98f3dce3); four other held cases recovered. This revision performs no simulation, world load, code change or client input.

## 1. Expected physical behaviour

Pressure reduction and mixing should reach a component- and energy-conserving, pressure/enthalpy-consistent equilibrium over the supported phases; one phase is a valid answer. Cryogenic precipitation or melting should exchange latent heat and material conservatively, with solids transported or retained. Crude and water should mix thermally, partition into supported phases and fill/drain vessels through the appropriate phase draws.

Flow may diminish, stop or become limited; none of these regimes alone is physically impossible, and a numerical stop does not prove an impossible state. Investigation should start from the smallest current-code reproduction and the actual held island, not from a whole-world proxy.

## 2. What the code did and the physical evidence

Final coverage leaves cold 6/85/101/430/446 and bundled 157/430/579 held. Cold 85/101/430 and bundled 157 have different first/latest island IDs, so every trace must carry its committed tick, graph and island rather than assume one persistent mechanism. The whole-world observations below group scenarios; they do not localize a cause.

| Group | Cases | Physical evidence and expected coupling |
|---|---|---|
| Cryogenic fluid/solid equilibrium and transport | cold 85, 101, 446 | LNG/CO2-bearing feeds; world-end retained crystals ~0.347, 12.506, 0.117 kg respectively. Only cold 101 records solid-transport transitions in its latest rejections. Coupling: freezing/melting, latent heat, composition, transport and vessel inventory. |
| Cryogenic liquid/gas mixing and pressure reduction, with no crystals retained at the recorded end | cold 6, 430 | Cold 6 supplies LNG at ~164 K/5.609 MPa into atmospheric voids. Cold 430 combines LNG ~128 K/0.884 MPa with CO2-rich gas ~293 K/4.890 MPa. Coupling: phase partition, enthalpy, density, pressure loss and any participating mover. |
| Crude/water mixing, vessel filling and phase-dependent withdrawal | bundled 430, 579 | Separate water/crude feeds, tanks and narrow connections; bundled 430 also has a pump, 579 a pressure valve. Coupling: hydrocarbon/aqueous/vapour partition, mixing enthalpy, inventory and phase-selective outflow. |
| Physical trigger unassigned | bundled 157 | Crude/water feeds, a tank, 10 mm restriction and 150/200 kPa valves; its stop is a wall deadline, with no established phase, valve or pump cause. |

Feed labels are rounded and pressures absolute. These are whole-world layouts, not solved held-island snapshots:

| Case | Feed conditions from layout | Recorded end crystals |
|---|---|---:|
| cold 85 | LNG ~119 K / 0.307 MPa | 0.346666 kg |
| cold 101 | LNG ~155 K / 2.894 MPa; CO2-rich gas ~282 K / 4.044 MPa and ~285 K / 3.643 MPa | 12.5059 kg |
| cold 446 | LNG ~114 K / 0.262 MPa; CO2-rich gas ~287 K / 4.992 MPa | 0.116715 kg |
| bundled 430 | Water ~341 K / 0.236 MPa; crude ~326 K / 0.142 MPa | not reported |
| bundled 579 | Water ~347 K / 0.168 MPa; crude ~397 K / 0.300 MPa | not reported |
| bundled 157 | Water ~332 K / 0.278 MPa; crude ~389 K / 0.353 MPa | not reported |

The pilot admits CO2-I and the cold LNG feeds carry CO2, so precipitation is the physical lead, not an assigned cause. Crystal totals are per world, with no population or held-island assignment; zero at the end does not exclude earlier solids. The 2026-10-24 extracts have no held-node pressure, phase or flow history. Valve chatter, cavitation, dry-out and transition timing still require state/flow evidence.

Cold 430 already has an eight-block gate (`HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold`). At tick 404, CO2-rich gas should expand into the empty tank and push back the LNG. On a6b14b22..e5357594 the mint balance failed to converge on flat choked/one-way rows; its unbalanced point seeded the E6 step at tick 406. Sweep-first form A cleared it but was rejected after three new worse worlds, so A is evidence, not an approved fix (`documentation/2026-10-15-custom-equipment-rendering/COLD430_889_REVIEW.md`).

Cold 6 already has a 12-block minimized trace: subcooled LNG floods a 1 atm network, downstream junctions flash at 112 to 144 K, and failed Newton trials put junctions at 4 to 19 MPa, two to six times the source pressure. The proposed passive-junction pressure-range guard was not prototyped or approved; re-establish this signature on final code before treating it as current (`documentation/2026-10-18-fluid-solver-simplification/FORM_D3C_COLD_SEEDS_TRACE.md`).

The direct numerical clues still localize the physics: bundled 430's non-finite Jacobian variable changes from 96 to 5, and bundled 579's rejections also include non-finite and singular stages. Map these to their node, unknown and phase before a broad inventory audit. Cold 85's ~2.12e-11 and cold 446's ~1.46e-9 residuals are solver-scaled, not permission to relax acceptance.

## 3. Proposed physics-led investigation order

1. Cold 430 first: replay only its existing eight-block gate on final code and capture balance, phase and flow evidence. Separate the tick-406 first hold from the latest record committed at 454; the full-world pump was placed at 454 and cannot explain the first hold.
2. Cold 6 next: start from its 12-block world, verify the flash/cap states and the 4 to 19 MPa trial signature, then inspect full-world mover or valve branches only if the minimized mechanism does not reproduce.
3. Cold 101 leads the crystal-specific group: identify the crystal population and vessel, its stream and latent-heat balance, and whether each transport transition changes the accepted trajectory; compare cold 85/446 afterward. Rank by localization, not world crystal mass.
4. Bundled 430/579 together: map the failed Jacobian perturbations to physical unknowns, then check mixing enthalpy, bag fill/empty events and the phase actually withdrawn. Check valve switching only where pressure/flow history supports it.
5. Bundled 157 remains separate: in an isolated lane with the dev client closed and no overlapping measurement, use existing counters to distinguish budget exhaustion from a process transition before any new campaign.

This supersedes both the Newton-symptom order and the crystal-mass-first order. Owner validation is requested for this order before a separately authorized tracing batch. It does not reclassify the 17 historical cases or change any physics/solver rule, test or client state. The read-only `sort_remaining_holds.py` observations remain in `remaining-holds/physics-observations-2026-10-24.json`. Code guide: no change.
