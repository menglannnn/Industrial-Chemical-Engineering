# New units plan (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, step "new units". The owner asked for eleven units: stirred tank reactor,
centrifugal compressor, cyclone, steam turbine, FCC reactor and regenerator, adsorption column, coker, trickle-bed
reactor, ebullated-bed reactor with its external recirculation pump, fluidized-bed combustor/gasifier, and fixed-bed
combustor/gasifier. The definitions come from five opus surveys in this folder (`NEW_UNITS_SURVEY_*.md`), which give the
physics, proportions, sources and parts for each unit. The units are drawn only: no process model is added.

## Owner decisions (2026-10-18)
- **D1. Solid fuel:** four kinds. CFB boiler, pressurised fluidized-bed gasifier (HTW type), grate-fired boiler, and Lurgi
  fixed-bed dry-bottom gasifier. A unit has no mode switch, and each of these looks different.
- **D2. Steam turbine:** a generator set (turbine, gear, generator), as two kinds.
  - Back-pressure: at grade, exhaust up.
  - Condensing: on a concrete table, with its surface condenser underneath. Its 0.5–3.2 m exhaust does not fit the 1 m
    pipe cap, so it exposes cooling water, condensate and air extraction instead.
- **D3. Sizes:** real ranges. FCC up to 66 m, coker derricks up to 95 m, CFB up to about 60 m overall. Detail goes
  before size does.
- **D4. Structure:** built in. Each unit carries the table, frame, decks, stairs or ladders it really stands on or in.
  The per-unit list was given in the session's reply on 2026-10-18. Halls, cranes, pipe racks and separate auxiliaries
  stay out.

## Logged decisions (taken from the surveys' recommendations; reversible)
- **Forms.**
  - Stirred tank: jacketed, top-driven DIN reactor, hung from lugs on its deck.
  - Adsorber: a TSA set of 1–4 beds. 1 bed is the single column; the survey's default is 2.
  - Trickle bed: hemispherical heads at every size, H2 tee on the feed elbow.
  - Ebullated bed: the ebullating pump sits coaxially under the bottom head in an open skirt bay, with a visible recycle
    loop; no animation.
  - Centrifugal compressor: horizontally split API 617, motor plus step-up gear, nozzles down through its table.
  - FCC: side-by-side, single-stage regenerator. Its second size is the riser length.
  - Cyclone: Stairmand high-efficiency proportions, 1–4 cyclones in parallel on manifolds.
  - Coker: 1–2 drum pairs with derricks and chute.
- **Lines wider than 1 m** (FCC air, flue gas and vapour lines) are drawn at true size and end in a concentric reducer to a
  nozzle of at most 1 m. Flue gas from boilers goes to a drawn stack, as on the heaters.
- **One STEAM nozzle per unit.**
  - HTW gasifier: the syngas cooler's steam feeds the moderator steam inside the unit, and STEAM exports the surplus.
  - FBDB gasifier: the jacket steam joins the agent steam inside the unit, and STEAM imports the balance. The steam/O2
    ratio of 5–6 kg/kg is more than the jacket raises.
- **Solids** enter and leave through ordinary nozzles: SOLIDS_INLET on bin or hopper tops, SOLIDS_OUTLET under chutes and
  valves. The solid-phase fluid system carries them.

## Kinds
Each kind is one file, `<Name>Design.java`. Size is the main size in m. The second size is a whole number.

| Kind | Size: range / step | Second size | Support | Nozzles (roles) | Moving |
|---|---|---|---|---|---|
| STIRRED_REACTOR | shell ID 1.0–4.0 / 0.2 | straight side, m, about 0.8–2 D | clearance under the bottom valve | VAPOUR_OUTLET FEED NITROGEN VENT LIQUID_OUTLET COOLANT_INLET COOLANT_OUTLET | drive shaft, motor fan |
| ADSORBER | bed ID 1.0–4.6 / 0.2 | beds 1–4 | skirt | FEED PRODUCT REGENERATION_INLET REGENERATION_OUTLET NITROGEN VENT DRAIN | — |
| TRICKLE_BED_REACTOR | ID 1.0–5.0 / 0.5 | beds 1–4 | skirt, at least D/2 + t + 1.5 | FEED HYDROGEN EFFLUENT QUENCH | — |
| EBULLATED_BED_REACTOR | ID 2.5–6.0 / 0.5 | T/T, m, 4–7 D | skirt bay holding the pump | EFFLUENT FEED HYDROGEN CATALYST_INLET CATALYST_OUTLET | — |
| CENTRIFUGAL_COMPRESSOR | impeller 0.3–1.5 / 0.1 | impellers 2–9 | table deck, at least 3.5 m | SUCTION DISCHARGE COOLANT_INLET COOLANT_OUTLET | motor fan |
| STEAM_TURBINE | exhaust-end casing 0.6–3.0 / 0.1 | stages 1–12 | plinth | STEAM EXHAUST | generator fan (optional) |
| CONDENSING_TURBINE | as STEAM_TURBINE | stages 1–12 | table deck, at least max(3.5, 2.4 Dc + 0.5) | STEAM COOLANT_INLET COOLANT_OUTLET CONDENSATE VENT | as STEAM_TURBINE |
| CYCLONE | body 0.3–2.0 / 0.1 | count 1–4 | dust outlet elevation, 1–8 | FEED VAPOUR_OUTLET SOLIDS_OUTLET | airlock shafts |
| FCC_CONVERTER | regenerator OD 4–17 / 0.5 | riser length 24–44 m (32) | regenerator bottom tangent, 6–14 | FEED STEAM AIR FUEL FLUE_GAS VAPOUR_OUTLET CATALYST_INLET CATALYST_OUTLET | — |
| DELAYED_COKER | drum OD 4–10 / 0.5 | drum pairs 1–2 | bottom flange, 8–16 (12) | FEED VAPOUR_OUTLET QUENCH STEAM WATER CUTTING_WATER BLOWDOWN RELIEF SOLIDS_OUTLET | drill stem, one drum per pair |
| FLUIDIZED_BED_BOILER | furnace width 3–26 / 1 | cyclones 1–4 | pedestal | FEEDWATER STEAM BLOWDOWN DRAIN FUEL SOLIDS_INLET SOLIDS_OUTLET | ash-cooler drum, feeder shafts |
| FLUIDIZED_BED_GASIFIER | ID 1.5–6.0 / 0.25 | none (1) | support ring | OXIDANT SYNGAS FEEDWATER STEAM BLOWDOWN NITROGEN VENT SOLIDS_INLET SOLIDS_OUTLET | feeder shafts, lock cylinders |
| GRATE_BOILER | grate width 2–12 / 0.5 | grate length 3–10 m | grate elevation | FEEDWATER STEAM BLOWDOWN DRAIN FUEL SOLIDS_INLET SOLIDS_OUTLET | ram feeders or drag chain |
| FIXED_BED_GASIFIER | ID 2.0–5.0 / 0.25 | none (1) | ash-lock bottom, 3–5 | OXIDANT STEAM SYNGAS QUENCH LIQUID_OUTLET FEEDWATER VENT NITROGEN SOLIDS_INLET SOLIDS_OUTLET | grate drives, lock cylinders |

- Nozzle sizes, envelopes, visible parts and what to leave out first are in the surveys.
- **Budgets.** At most about 60k quads at a kind's largest size; 80k is the hard ceiling, measured with `UnitCounts`. The
  largest existing unit is 52k. Layout under 200 ms.
- **Animation.** One motion per part: a part turns or slides, not both. On the coker the stem's crosshead slides; the
  stem's turning is optional, as a separate part.

## Shared changes (orchestrator, before the lanes start)
- **Model.**
  - `EquipmentShape.Kind`: the 14 kinds, appended after CABIN_HEATER.
  - `Role`: 26 roles. PRODUCT, NITROGEN, COOLANT_INLET, COOLANT_OUTLET, REGENERATION_INLET, REGENERATION_OUTLET,
    HYDROGEN, EFFLUENT, QUENCH, CATALYST_INLET, AIR, OXIDANT, WATER, CUTTING_WATER, BLOWDOWN, SOLIDS_INLET and FEEDWATER
    are BULK. VENT, EXHAUST, FLUE_GAS, RELIEF and SYNGAS are VAPOUR. DRAIN, CONDENSATE, CATALYST_OUTLET and SOLIDS_OUTLET
    are LIQUID: bottom outlets, dense phase first.
  - Stub designs registered in `UnitDesign`, plus `VesselPlacement.Form` entries.
- **Gates** in `ProcessUnitTest`:
  - Deck and ladder checks, and moving-part checks, run on every size of every kind.
  - The nozzle-face map skips a kind until its lane reports its faces. At integration it becomes strict for all kinds.
- **Kit: stairs.** The kit had decks and caged ladders but no stair. `ProcessUnit.stair` adds a straight flight:
  - grating treads 0.25 m deep with rises of at most 0.2 m, on channel stringers;
  - a handrail and a mid rail on both sides, 0.9 m over the nosings;
  - collision: each tread filled down to the one below, plus a fence band on each side.

  A `Stair` record lists the flights, and the gate checks that every flight joins decks or the ground, and that every
  tread can be stepped on, has room above it and is fenced.
- **Fix found by the generalised moving gate.** The moving-part cut chose cells with `floor(min)..floor(max)` and no
  tolerance. A piston rod's end disc lying exactly on a block face (compressor 1.1 × 2, at x = 10) was lost from the
  moving rod. When animated, the still mesh dropped the disc and the moving rod lacked it. `moving()` now uses the chunk
  mesh's own cell assignment, `cells(facet)`. All 77 moving sizes now pass.
- **Constructor.** The choice page becomes two columns:
  - left, the category buttons: Vessels, Heat (heat transfer), Machines, Reactors, Solid fuel;
  - right, the selected category's kinds.
  - Its lang covers the kinds, short names, hints, roles, vessel-screen titles, kinds and supports.

## Lanes (parallel opus subagents, disjoint files, no Gradle; `tools/new-units/KIT.md`)

| Lane | Kinds |
|---|---|
| vessels | STIRRED_REACTOR, ADSORBER |
| hydro | TRICKLE_BED_REACTOR, EBULLATED_BED_REACTOR |
| rotating | CENTRIFUGAL_COMPRESSOR, CYCLONE |
| turbines | STEAM_TURBINE, CONDENSING_TURBINE |
| fcc | FCC_CONVERTER |
| coker | DELAYED_COKER |
| fluidbed | FLUIDIZED_BED_BOILER, FLUIDIZED_BED_GASIFIER |
| fixedbed | GRATE_BOILER, FIXED_BED_GASIFIER |

Each lane:
- edits its own `*Design.java` files, plus any helper class prefixed with its design's name;
- writes its lang keys to `tools/new-units/lang/<lane>.json`;
- reports, for each kind: nozzle faces, counts at the smallest, middle and largest sizes, renders, and open choices.

## Integration
1. Merge the lane lang files, then make the nozzle-face map strict.
2. Run Gradle `compileJava`, then the equipment tests together with `DeterministicMathSourceTest`.
3. Run `UnitCounts` on every kind.
4. Bridge-lane check on a fresh world: place each kind once and use the chooser page.
5. Write the review (at most 50 lines), the CHANGELOG `[Unreleased]` line, the docs, and the tools folder.
6. Commit only on request.

## Status (2026-10-18)
Steps 1 to 5 done; nothing committed (step 6 waits for the owner). The in-game check found a missing table rail on the
condensing turbine, constructor lines wider than the panel and datasheets taller than a 240-pixel GUI; all three are
fixed, the text by a gate (`UnitTextTest`) and the datasheets by a fitted line pitch. Results: `NEW_UNITS_REVIEW.md`.
