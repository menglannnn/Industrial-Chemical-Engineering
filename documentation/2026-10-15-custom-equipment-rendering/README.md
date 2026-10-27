# 2026-10-15-custom-equipment-rendering

Purpose: Owner request 2026-10-15: parametric equipment (drums, columns, reactors) built in-world by a constructor, diameter and height chosen by the player; feasibility of dynamic 3D shell rendering and hitbox, FPS cost, and compatibility with Sodium, Embeddium, Iris, Flywheel, Entity Culling, Distant Horizons and ModernFix

Status: Implemented 2026-10-20 (0.10.0); follow-up 2026-10-21 (0.10.1). Index: [../INDEX.md](../INDEX.md).

## Documents

- [CUSTOM_EQUIPMENT_RENDERING_REVIEW.md](CUSTOM_EQUIPMENT_RENDERING_REVIEW.md)
- [CUSTOM_EQUIPMENT_POC_REVIEW.md](CUSTOM_EQUIPMENT_POC_REVIEW.md)
- [EQUIPMENT_CONSTRUCTOR_REVIEW.md](EQUIPMENT_CONSTRUCTOR_REVIEW.md)
- [VESSEL_SUPPORTS_REVIEW.md](VESSEL_SUPPORTS_REVIEW.md)
- [PROCESS_EQUIPMENT_PHOTO_SURVEY.md](PROCESS_EQUIPMENT_PHOTO_SURVEY.md)
- [PROCESS_EQUIPMENT_PLAN.md](PROCESS_EQUIPMENT_PLAN.md)
- [PROCESS_EQUIPMENT_REVIEW.md](PROCESS_EQUIPMENT_REVIEW.md)
- [DEVICE_RENDERING_REVIEW.md](DEVICE_RENDERING_REVIEW.md)
- [DRUM_OUTLET_SHADING_GUI_REVIEW.md](DRUM_OUTLET_SHADING_GUI_REVIEW.md)
- [DRUM_SKIRT_CONNECTIONS_REVIEW.md](DRUM_SKIRT_CONNECTIONS_REVIEW.md)
- [COLUMN_AUXILIARIES_PLAN.md](COLUMN_AUXILIARIES_PLAN.md)
- [COLUMN_AUXILIARIES_REVIEW.md](COLUMN_AUXILIARIES_REVIEW.md) (superseded)
- [STANDALONE_UNITS_PLAN.md](STANDALONE_UNITS_PLAN.md)
- [STANDALONE_UNITS_REVIEW.md](STANDALONE_UNITS_REVIEW.md)
- [EQUIPMENT_POLISH_PLAN.md](EQUIPMENT_POLISH_PLAN.md)
- [EQUIPMENT_POLISH_REVIEW.md](EQUIPMENT_POLISH_REVIEW.md)
- [EQUIPMENT_DETAILS_PLAN.md](EQUIPMENT_DETAILS_PLAN.md)
- [EQUIPMENT_DETAILS_PHOTO_SURVEY.md](EQUIPMENT_DETAILS_PHOTO_SURVEY.md)
- [EQUIPMENT_DETAILS_REVIEW.md](EQUIPMENT_DETAILS_REVIEW.md)
- [EQUIPMENT_PERFORMANCE_REVIEW.md](EQUIPMENT_PERFORMANCE_REVIEW.md)
- [MACHINES_HEATERS_PLAN.md](MACHINES_HEATERS_PLAN.md)
- [MACHINES_HEATERS_SURVEY.md](MACHINES_HEATERS_SURVEY.md)
- [MACHINES_HEATERS_REVIEW.md](MACHINES_HEATERS_REVIEW.md)
- [MACHINES_SCALE_RESEARCH.md](MACHINES_SCALE_RESEARCH.md)
- [METALLIC_LOOK_RESEARCH.md](METALLIC_LOOK_RESEARCH.md)
- [MACHINES_HEATERS_SECOND_REVIEW.md](MACHINES_HEATERS_SECOND_REVIEW.md)
- [MACHINES_HEATERS_THIRD_REVIEW.md](MACHINES_HEATERS_THIRD_REVIEW.md)
- [GHOST_PIPE_SPACING_REVIEW.md](GHOST_PIPE_SPACING_REVIEW.md)
- [HEATER_JOINTS_MACHINE_MOTION_PLAN.md](HEATER_JOINTS_MACHINE_MOTION_PLAN.md)
- [HEATER_JOINTS_MACHINE_MOTION_REVIEW.md](HEATER_JOINTS_MACHINE_MOTION_REVIEW.md)
- [NEW_UNITS_PLAN.md](NEW_UNITS_PLAN.md)
- [NEW_UNITS_SURVEY_STIRRED_ADSORPTION.md](NEW_UNITS_SURVEY_STIRRED_ADSORPTION.md)
- [NEW_UNITS_SURVEY_HYDROPROCESSING.md](NEW_UNITS_SURVEY_HYDROPROCESSING.md)
- [NEW_UNITS_SURVEY_ROTATING.md](NEW_UNITS_SURVEY_ROTATING.md)
- [NEW_UNITS_SURVEY_FCC_COKER.md](NEW_UNITS_SURVEY_FCC_COKER.md)
- [NEW_UNITS_SURVEY_SOLID_FUEL.md](NEW_UNITS_SURVEY_SOLID_FUEL.md)
- [NEW_UNITS_REVIEW.md](NEW_UNITS_REVIEW.md)
- [KIT_CONSOLIDATION_PLAN.md](KIT_CONSOLIDATION_PLAN.md)
- [KIT_CONSOLIDATION_REVIEW.md](KIT_CONSOLIDATION_REVIEW.md)
- [SAVE_REFRESH_REVIEW.md](SAVE_REFRESH_REVIEW.md)
- [FCC_PROPORTIONS_RESEARCH.md](FCC_PROPORTIONS_RESEARCH.md)
- [FCC_PROPORTIONS_PLAN.md](FCC_PROPORTIONS_PLAN.md)
- [FCC_PROPORTIONS_REVIEW.md](FCC_PROPORTIONS_REVIEW.md)
- [MERGE_PREP_REVIEW.md](MERGE_PREP_REVIEW.md)
- [FINAL_TEST_REVIEW.md](FINAL_TEST_REVIEW.md)
- [FINAL_TEST_M23_REVIEW.md](FINAL_TEST_M23_REVIEW.md)

## Code areas

`science/equipment` (`EquipmentShape`, `ShapeRasterizer`, `VesselPlacement`, tests), `equipment_shell` / `equipment_interior` / `equipment_constructor` blocks, `EquipmentStructure` (`/createcheme equipment place`), `EquipmentNetwork` (protocol `equipment-9`), `ColumnStructure`, `ProcessUnit`, `ColumnTrays`, `Mesh`, `LineGeometry`, `PipeSizes`, client `FluidDeviceModel` and `ColumnDiagram`, drum ports and level height in the fluid runtime (checkpoint format 12 after the merge of 0.9.2), sprites `vessel_shell` / `vessel_head` / `vessel_support`, client `EquipmentShellModel`, silhouette outline, `EquipmentGhost`, `MachineAnimation`, `UnitDesign` (one `*Design` file per unit kind), `Manway`, `EquipmentConstructorScreen`, `VesselScreen`; test lane `UseItemMixin`, `WindowHandleMixin` (`src/mcpCompat`); tools `tools/equipment-geometry-estimate/`, `tools/custom-equipment-rendering-poc/`, `tools/new-units/`

## History

- Implemented 2026-10-20 (main 2e28a20a, 0.10.0, a fast-forward of the branch
- not pushed).
- Follow-up 0.10.1 (OD4 holds after 0.10.0, branch `claude/stoic-bell-ae6019`): Implemented 2026-10-21 (main `144321e7`, 0.10.1, a fast-forward
- not pushed) (`MERGE_PREP_0_10_1_REVIEW.md`).
- History: in progress since 2026-10-15 (owner accepted D1-D5 as proposed
- proof of concept built and measured
- constructor, controller role and screens built and checked in the dev client
- 2026-10-16 supports, horizontal vessels and the refined shell built and checked
- branch `claude/custom-equipment-rendering-805e7d`, committed 2026-10-16 as 02bf33f0..9a9ef8cc, not merged
- 2026-10-16 drums as reservoirs, column structure, pipes and pipeline accessories built, checked in the dev client and gated (1,225 tests, chain-100 exact, GameTests 31 + 2), committed on the branch as d1701780 and 80fdce9b
- the owner validated the two changed gate tests 2026-10-16
- the same day pipeline-device rendering fixed after an owner report and the constructor given a choice page first, committed on the branch as d677b607, 714f4833 and 0a4db3a2
- the same day the drum bottom outlet through the skirt, smooth shell shading, constructor descriptions and the drum datasheet tab built, unit-tested (1,226 tests) and checked in the dev client
- the same day the hollow skirt with port and slots, the silhouette outline, side connections and nozzles at the pipe's size (reducer at the vessel, bottom line and skirt port up to 0.69 m) built, unit-tested (1,229 tests) and checked in the dev client
- both steps committed on the branch as 91fcb448 and d43509f5, not merged
- the same day the column auxiliaries (condenser with reflux drum, steam ejector, PA coolers, reboiler) laid out for review, `COLUMN_AUXILIARIES_PLAN.md` and `figures/column-auxiliaries/`
- the owner decided the seven points the same day (side structure with the air cooler on its own stand, face 8.5 × the column cross-section
- PA returns on the structure side
- pumps
- kettle
- upright ejector
- a constructor page)
- built the same day: `ColumnAuxiliaries` and `ColumnOptions`, nozzles moved to the structure side, constructor Units page, protocol `equipment-5`, NBT `Options`
- found in the dev client and fixed: walkways (0.8 m clear, 2.1 m headroom, EN ISO 14122-2)
- unit suite 1,234 tests 0 failures, gate `ColumnAuxiliariesTest`
- committed on the branch as 2b642fd1 and abec84a6, not merged
- owner decisions R1–R4 the same day: AC, exchanger and reboiler standalone and scalable, the air cooler one largest bay scaled out by number, the column's connections left for the player on fixed sides, the bottoms through the skirt like the drum's, the ejector standalone
- built the same day: `ProcessUnit` (exchanger, kettle, air cooler bay, ejector), constructor with seven kinds, `/createcheme equipment unit`, every nozzle a port at the pipe's size (per-cell codes), protocol `equipment-6`, no NBT `Options`
- `ColumnAuxiliaries` and `ColumnOptions` removed
- gates `ProcessUnitTest` and pipe-block checks
- unit suite 1,233 tests 0 failures
- checked in the dev client (shots 71–81)
- committed on the branch as 2f1ddb85 and a2b4c2c8, not merged
- polish step 2026-10-16 (owner requests after the look at 2f1ddb85
- decisions: Blender parts scripted, one bank unit, line rises with the skirt
- addition: metallic by texture only, paint chosen when building): ghost without aim, blind-flanged pipe ends, bottom line by skirt, neutral metallic sprites, `Paint` (Body and Structure, 16 RAL, #RRGGBB), ejectors 0.05–1 m, 14 air cooler bays by 40 % fan coverage and `Bank` 1–6 × 1–4 with manifolds and crossovers, Blender fan ring, rotor and motor (`Part`), protocol `equipment-7`
- unit suite 1,241 tests 0 failures
- checked in the dev client (shots 82–93)
- committed on the branch as ada9b340 and 768e6fb6, not merged
- details step 2026-10-16 (owner requests after the dev client on ada9b340, decided the same day: the plenum shows the grid, colours in the common config, a fans switch for debugging
- then an owner report with a photo: two fans' rings overlapped, no bottom casing): class 150 nuts on every flange, manways on drums (`Manway` shared with columns), a band and vents at the skirt junction, a lying drum's water boot inboard of the far saddle (Nozzles page), connections fitted on a template drum (the saddle clash), air coolers 2 m lower, a sheet hopper per fan and fans sized by their rings, turning fans (`FanAnimation`, about 0.03 ms a frame for 48 fans), `equipment.primaryColour` / `secondaryColour`, protocol `equipment-8`
- unit suite 1,245 tests 0 failures
- checked in the dev client (shots 94–106)
- committed on the branch as 668961be and 2a2f9a19, not merged
- performance step 2026-10-16 (owner question whether fixed-size parts modelled in Blender would be cheaper, then "include ram cost", then "optimize the performance as much as possible"): answer measured (`QuadShares`): Blender changes nothing in a chunk-baked part, and triangulated parts cost double
- built: exact triangle-pair merging in `Part` with the ring texture by arc length, the hopper at the ring's 24 corners, buried bar ends left out and mitred rails (`Mesh.rail`), shared box texture frames, one baked list per cell unless it holds a rotor, `ShapeRasterizer.serverOnly` on a dedicated server
- air cooler bank −24 % quads, columns −7 to −10 %, client geometry heap −30 % on columns and units, server −98 %
- nut seats tried and reverted (LineGeometryTest), flat crowns not done
- unit suite 1,246 tests 0 failures, new server gate with its proof
- checked in the dev client (shots 107–114)
- committed on the branch as 8f0b81fa and 3ad9884e, not merged
- machines and heaters step 2026-10-17 (owner request "Design 3 more items: centrifugal pump, reciprocal compressor, furnace, all scalable", decisions M1–M4, then "gross shape looks ok, but needs significant improvement"): API 610 OH2 pump by discharge, API 618 balanced-opposed compressor by bore and throws, vertical cylindrical and cabin heaters after API 560, drawn only, protocol `equipment-9`
- improvement pass from a survey (`MACHINES_HEATERS_SURVEY.md`): eccentric volute, seal pot, guides and windowed distance pieces, braced legs, muffler burners, platforms with handrails and caged ladders on the heaters (`Access` shared with the columns, walk-through ladders, ring solids)
- unit suite 1,247 tests 1 failure (determinism source gate: `Math.asin`/`pow`/`hypot`), fixed with `DeterministicMath.asin`, gate and equipment package re-run 61/61
- checked in the dev client (`machines-check/` shots `n-*`)
- committed on the branch as 974e64b2 and a118d219, not merged
- machines and heaters second pass 2026-10-17 (owner list of eight items): heater ladders climbable (`Access.Climb` for columns and units, climb-zone cells kept as shell cells, every shape answers `isLadder`), five heater nozzles (convection and radiant inlets and outlets, fuel), H-section legs on concrete pedestals with ring or perimeter girders, X-braces and knees, compressor suction and discharge headers on a steel table with an octagonal synchronous motor, sizes after refinery practice (compressor bore to 1.2 m and 10 throws, VC D to 12 m and radiant height to 20 m
- `MACHINES_SCALE_RESEARCH.md`), an optional baked sheen (`metallicSheen`, default 0
- `METALLIC_LOOK_RESEARCH.md`)
- unit suite 1,247 tests 1 failure (determinism source gate: `Math.cbrt`, then `Math.tan`), fixed, gate and equipment package re-run 55/55
- ladder drop tests and datasheets checked in the dev client, sheen 0 against 0.2 (shots 115–125)
- `MACHINES_HEATERS_SECOND_REVIEW.md`
- third pass 2026-10-17 (owner answers to the second review and two reports): closed walkway ends now collide (0.2 m rail band, gate with proof), concrete-fireproofed heater legs, twin-cell cabins above 24 m up to 48 m, BB1 pumps 0.45–0.75 m, sheen default 0.2 after Sodium + four optimisation mods and Iris with five packs rendered cleanly, wording of unit sides corrected, bridge window-handle fix (`WindowHandleMixin`) and `prepareMcpControlMod` without `project.delete`
- equipment and determinism gates 55/55, unit suite 1,247 tests 1 failure (`RegroupedCrudeTest` wall-clock deadline under external CPU load, no column code changed
- re-run on a quiet machine open), fence drops 7/7 on the fence tops, max compressor built
- FPS pair not run (machine never quiet
- zero per-frame cost by construction)
- `MACHINES_HEATERS_THIRD_REVIEW.md`
- the same day two owner reports after the shader client: constructor ghost invisible under Iris (drawn as chunk translucent geometry
- now translucent entity geometry, checked under Iris, Sodium and vanilla), pipes of neighbouring nozzles joining (110 pairs of touching pipe blocks, most on the vertical heater
- one rule `Nozzle.crowds` applied by drums, columns and every unit layout, gate `pipesApart` on every unit size, the test columns and drums, drum-clash proof)
- equipment and determinism gates 55/55
- `GHOST_PIPE_SPACING_REVIEW.md`
- second pass, third pass and both fixes committed on the branch as 228a6237 and 9cadc501, not merged
- 2026-10-18 four owner notes: soot blowers removed (gas firing keeps convection tubes clean), heater section joints from photos (flanged seams at the casing columns, bolted convection module joints with corner columns, stack joints), open compressor distance pieces with piston rods, and one moving-parts path replacing the fan special case (fans, pump shafts with seal collars and motor fans behind cowl grilles, piston rods on a slider-crank
- datasheet switch "Moving parts")
- quads +10 to +25 %
- checked on the bridge lane and under Iris (Complementary Reimagined)
- equipment and determinism gates 56/56
- `HEATER_JOINTS_MACHINE_MOTION_PLAN.md`, `HEATER_JOINTS_MACHINE_MOTION_REVIEW.md`
- committed on the branch as 3714cedf and 41ef764f, not merged
- 2026-10-18 new units: fourteen kinds from the owner's eleven (D1 CFB boiler, HTW gasifier, grate boiler and Lurgi gasifier
- D2 back-pressure and condensing turbine sets
- D3 real size ranges, detail trimmed first
- D4 each with the structure it stands on), one `UnitDesign` file each from eight parallel opus lanes gated without Gradle (`tools/new-units/`)
- `ProcessUnit.stair`, `MOTION_PERIOD` 2400 ticks, the moving-part cut by the mesh's own cells, the choice page in five categories, defaults per kind
- the in-game check found a missing table rail (condensing turbine), constructor lines past the panel and datasheets past a 240-px GUI, all fixed (`UnitTextTest`, fitted datasheet pitch)
- largest 78.5k quads
- equipment and determinism gates 58/58
- `NEW_UNITS_PLAN.md`, `NEW_UNITS_REVIEW.md`
- not committed
- new units committed as d084b777 + 4dc78964
- a JSON drawing-engine plan (`UNIT_ENGINE_PLAN.md`) abandoned the same day for the kit consolidation: the fourteen kinds moved onto the shared kit by eight lanes plus one integration step (tower plan, one flat-face rest, base-plated posts, valve, thermowell and reducer, kit fixes K1-K5
- `FixedBedGasifierHouse` and `TrickleBedReactorParts` gone, `HydroprocessingParts` shared by the two reactors, gate `designsShareOnlyTheKit`), owner decision C1 a rest every 9 m of caged climb gated on every unit ladder, cabin heater side walkways
- gates 68/68, sweep of 19,638 shapes, in-game check pass
- `KIT_CONSOLIDATION_PLAN.md`, `KIT_CONSOLIDATION_REVIEW.md`
- committed on the branch as c20fb5fb + 1ca05c1c, not merged
- 2026-10-18 owner request "Add a compatibility system so when loading an old save updated model is replaced by new ones" (an AGENTS.md exception, equipment layouts only): each shell cell saves its layout stamp and box, a stale vessel is rebuilt on a tick once its region loads and ticks, standing where it covers the most of its old cells, never overwriting another block, a drum keeping its registration, chunks held until relit (`EquipmentRefresh`, `EquipmentStructure.refreshLater`, gate `EquipmentRefreshTest`)
- owner report "cyclone inlet looks weird": one eccentric transition instead of a dog-leg
- gates 73/73
- two in-game checks on copies of the review world (the first found a 1-block shift and black faces until reload, both fixed
- the recheck passes)
- `SAVE_REFRESH_REVIEW.md`
- committed on the branch as 4b6344e9 + 1c5fd421, not merged
- owner remark "Regenerator is too big, reactor by itself does not have appropriate proportions": research of 16 data points and a prototype with forms A (research rules) and B (reactor 0.75 D) for the owner's choice, `FCC_PROPORTIONS_RESEARCH.md`, `FCC_PROPORTIONS_PLAN.md`
- the owner chose A the same day: regenerator 9 + 1.5 D, reactor 6 m + 1.2 OD, stripper 0.35 D x (5 + 0.3 D), the datasheet reading the layout
- gates 73/73, sweep 2,388 FCC shapes 0 failed, a partial in-game look (both review-world FCCs rebuilt in place by the refresh)
- `FCC_PROPORTIONS_REVIEW.md`
- committed on the branch as 11dac35e + d8f6a375, not merged)
- 2026-10-20 merge preparation (owner: "prepare for merge"): main 0.9.2 merged into the branch (6cd0d020, checkpoint format 12, unit format 5), frame-time probe detached (03be157b, tools/custom-equipment-rendering-poc/frame-time-probe/), code guide brought to 8f9009b3
- final lane m21 at 8f9009b3 NOT GREEN: 1,571 tests, 1 failure (main's JunctionSeedingGateTest on bundled 145), bundled campaign 990 → 988, all from the branch's OD4 actuator half link (with OD4 off, identical to 0.9.2)
- owner kept OD4 (option a)
- 2026-10-20 the OD4 holds fixed (f983306b, 8fd61a13, 9de0d85f with owner decision E5 amending D38 C) and the final check m23 at 8be1efef GREEN (chain-100 exact, GameTests 31/31, defaults identical, cold 955 → 962, bundled 990 → 990
- cold 285 is OD4's own cost item, `FINAL_TEST_M23_REVIEW.md`), released as 0.10.0

## Notes

- investigation 2026-10-15. Proposed route A1: a procedural baked model per shell cell, meshed with the chunk (no per-frame cost; Sodium 0.6.13/0.8.13, Embeddium 21.1 and Iris checked in source; Immersive Engineering 1.21.1 uses the same route); per-frame rendering only for an optional controller overlay (BER; Flywheel only with a BER fallback, because Flywheel 1.0.6 turns its backends off under shader packs). Budget from `tools/equipment-geometry-estimate/`: a 10 m x 60 m column is 5,664 blocks, 2,428 shell cells and 10.6 k chunk quads. Open: owner decisions D1 route, D2 in-world process state against the presentation rule, D3 look, D4 vanilla curve shading, D5 size grid and breaking; then a proof of concept with one FPS pair. Proof of concept 2026-10-15 (D1-D5 accepted as proposed): vertical vessels with 2:1 heads, per-cell baked shell, 1/8 collision, silhouette outline, whole-vessel removal; checked in the dev client (appearance, partial build, chunk corner, collision, D5). FPS pair with 20 columns of 6 m x 40 m: no cost on the vanilla pipeline or Sodium 0.8.12, +0.06 ms per frame with Iris 1.8.14 and Complementary Reimagined (shell drawn as terrain, casts shadows). Findings: the bridge `enter_control_mode` crashes the client with Sodium (unattended autopilot in the measurement-only `FrameTimeProbe`), vessels need supports (skirt, legs, saddles), no controller block yet. Next tasks proposed: constructor and controller, supports, horizontal vessels. Constructor 2026-10-15 (owner decisions: a placed block, the Schematicannon pattern; the controller is a role of the lowest/west/north shell cell): screen with steppers and datasheet (height, volume, outside area with the 2:1 head factor 1.0840 D², cells), translucent ghost with blocked cells outlined red (Catnip outliner), build on request (protocol `equipment-1`), vessel datasheet on use of any cell; checked in the dev client (screenshots 10-17). Findings: the bridge ran `use_item` off the render thread (crash; fixed in `src/mcpCompat` `UseItemMixin`); building is free (kit and cost are a follow-up). Supports 2026-10-16 (owner decisions 2026-10-15: skirt, default ⌈D/4 + 1⌉ m; two saddles at 0.2·T/T, 120°; owner requests 2026-10-16 to refine model and texture from real pictures and to add polygons): skirt with base ring and anchor chairs, saddles of top plate, web, ribs and base plate, vessels lying along x or z (a proper rotation of the vessel frame), constructor forms Vertical/Lengthwise/Crosswise relative to its facing (protocol `equipment-2`); own sprites from an image survey (2 m plate courses with staggered seams, seamless pressed heads, painted support steel); facets at most about 1/3 m and heads with n/4 bands (D3 had 0.5 m and n/8), 1.7x the chunk quads of the budget cases, FPS not re-measured. Checked in the dev client (screenshots 18-30; collision on a drum exact). Findings: a record compact constructor read unassigned fields (class-init crash, fixed before the check); the 2 % volume check now starts at D 2.5 (lattice alignment). Not built: skirt manway and vents, nozzles.
- 2026-10-26: `FLANGE_JOINT_REVIEW.md` (2026-10-20, branch `claude/competent-franklin-43848a` from `2e28a20a`, commit `bf38dee9` and later, not on `main`) was found only in a session worktree and copied here.
