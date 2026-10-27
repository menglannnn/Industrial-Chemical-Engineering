# Centrifugal pump, reciprocating compressor, fired heaters: plan

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-17 · owner request "Design 3 more items: centrifugal pump, reciprocal compressor, furnace, all scalable".

## Owner decisions (2026-10-17)

- M1: drawn only, as the other units: nozzles take the pipe's size, a pipe there ends dead; the one-block pump and compressor stay the working devices.
- M2: pump = API 610 OH2 (overhung, centreline mounted, end suction, top discharge, bearing housing, coupling guard, motor on a baseplate), scaled by its discharge size.
- M3: furnace = both forms: a vertical cylindrical heater (VC) and a cabin (box) heater.
- M4: compressor = API 618 balanced-opposed, 2, 4 or 6 throws, suction bottles above and discharge bottles below the cylinders, motor at the end; scaled by bore and throws.

## Physical expectation (sizes and what each part does)

- Pump, discharge d (0.05 to 0.4 m): suction one size up (≈ 1.3 d); liquid at about 3 m/s in the discharge, Q = 21 m³/h at d 0.05, 85 m³/h at 0.1, 1,360 m³/h at 0.4. At a 100 m head and 70 % efficiency the motor takes 8, 33 and 530 kW; a TEFC motor's body is about 0.19 P^0.25 m across (0.32, 0.46, 0.91 m) and twice as long. Impeller 0.18 + 1.2 d (0.24 to 0.66 m); the volute grows from 0.62 to 0.8 impeller diameters round it. The casing's feet sit at the shaft's height (centreline mounting, hot service); the baseplate stands on a concrete plinth.
- Compressor, bore b (0.15 to 0.8 m): stroke 0.1 + 0.25 b; double-acting cylinders, displacement throws × 2 × π/4 b² × stroke × rpm (900 − 600 b rpm): 4 × 0.4 m about 8,000 m³/h. Opposed throws on alternate sides of the crankshaft balance each other. Gas enters from above (suction bottles, about 10 swept volumes, damp the pulsation) and leaves below (discharge bottles); the cylinders' outer ends and the bottles need supports to grade, so the frame stands on a concrete block high enough for the discharge bottles. A synchronous motor 0.8 + b + 0.1 per throw across drives the crankshaft's end.
- Fired heaters (API 560): radiant tubes take about 35 kW/m² on average and the radiant section about 70 % of the duty; 6" tubes at two diameters' pitch give π²/2 m² of tube per metre of wall run. The floor stands 2 m or more up for burner access; burners fire upward through it, about 2.5 MW each in a VC heater, 4 MW in a cabin. Flue gas rises through the convection section (feed in at its top, flowing down against the gas), a breeching and a stack; the feed crosses over to the radiant coil and leaves near the floor.
  - VC: radiant diameter D 2.5 to 10 m, radiant height 1.5 D to 2.75 D (API 560's slenderness limit); D 4 × 8 m about 6 MW, D 10 × 25 m about 57 MW. Convection box about 0.45 D × 0.85 D on the roof, stack 0.25 D + 0.3 across.
  - Cabin: width W 4 to 12 m, length 1.2 W to 40 m, walls W + 1 high, a hipped roof up to the convection section (0.4 W wide) along the whole length; tubes along both side walls, return bends in header boxes on the end walls. W 6 × 12 m about 13 MW, W 12 × 40 m about 95 MW; one stack per 15 m of length.

## Reversible decisions (mine)

- Kinds `PUMP`, `COMPRESSOR`, `VERTICAL_HEATER`, `CABIN_HEATER`, appended after the four units. Protocol `equipment-9`.
- Pump: size = discharge d in 0.05 m steps; no length, no support row (it stands on its plinth at grade; the constructor's Up row lifts it onto a deck). Suction faces the constructor along the shaft, discharge up over the casing, motor at the back.
- Compressor: size = bore in 0.05 m steps, length = throws (2, 4, 6), support = foundation height (whole metres, least so the discharge bottles clear grade by 0.3 m). Crankshaft along the frame's z, motor at the back; on each side a suction nozzle up from its bottle and a discharge nozzle to the front from its bottle's head (four nozzles: each side's bottles are its own header; the player joins them).
- VC heater: size = radiant diameter in 0.5 m steps, length = radiant height, support = floor height (2 to 16). Cabin heater: size = width in 1 m steps, length = radiant length, support = floor height. Nozzles at the front (towards the constructor): process inlet into the convection section's front header box, process outlet from the radiant section near the floor, fuel gas to the burner header under the floor.
- New sprites: painted heater casing (panels on 2 m stiffener spacing, tinted by the body paint), concrete, a safety-yellow coupling guard. Stacks and bottles in the shell's paint; frames, cylinders and casings in the machine's paint; legs and baseplates in the structure's.
- Not in this step (owner propositions): a compressor's interstage cooler and two-stage arrangements, working fluid behaviour (M1). Platforms and ladders on heaters and a pump's seal pot came with the improvement pass below.

## Steps

1. `EquipmentShape.Kind`, `Role.FUEL`, sizes and validation in `ProcessUnit` (`maxLength` by size, even throws).
2. The four builders: parts, collision, enclosed volumes (radiant boxes, convection sections), nozzles on cell centres with a free block beyond each flange.
3. Constructor: the choice page in two columns (eleven kinds), a design page per kind, datasheet lines (pump flow and motor, compressor displacement, heater duty); `VesselScreen`; command words; lang.
4. Sprites, model JSON and materials.
5. Gates: `ProcessUnitTest` runs every new size (free pipe blocks, flanges apart, sealed insides, on the ground, nozzle sides) and the physics numbers above; `ShapeRasterizerTest` shape set. One unit suite run.
6. Dev client on the bridge lane, fresh world: each kind at its smallest and largest, the choice page at 854 × 480.

## Improvement pass (owner feedback 2026-10-17: "gross shape looks ok, but needs significant improvement")

7. Survey (`MACHINES_HEATERS_SURVEY.md`, opus research subagent): what makes each unit recognisable at 1 m blocks.
8. Shared access: deck, handrail and caged ladder taken out of `ColumnStructure` into `Access`, along a round wall (`Arc`) or a straight one (`Line`); `ProcessUnit` gains ring solids cut to an angle range, rung planes and climbable cages, walk-through ladders (rung plane ends at the landing, a gap in the landing's rail).
9. Detail per unit from the survey: pump volute, feet, bracket, bearing housing, seal pot, hood, motor, plinth; compressor guides, distance pieces, caps, supports, frame covers, box motor; heaters' legs and bracing, casing bands, muffler burners, platforms at the hearth, every 8 m and the roof, caged ladders, peep and access doors, header boxes, soot blowers, crossovers, stack damper and lever.
10. Quad budget: drop details a 1 m block cannot show (valve-cap studs), one piece per straight run, arc chords from a 5 mm sagitta.
11. Gate `heatersCarryWalkableDecksAndClimbableLadders`; one unit suite run; dev client on a fresh world. Review `MACHINES_HEATERS_REVIEW.md`.

## Second pass (owner feedback 2026-10-17)

Owner list: heater nozzles for fuel, radiant in/out and convection in/out; ladders on both heaters not functional; heater supports more rigid; one compressor inlet and outlet; compressor on steel beams like the other units; a better compressor motor; larger pump, compressor and heaters at real refinery scale; investigate a more metallic surface.

Physical expectation and what the code did:
- Ladders: Minecraft asks the block at a player's feet whether it is a ladder. The heaters' rung planes, cages and climb zones were right in the shape, but `EquipmentShellBlock.isLadder` answered only for columns, and below a cage (the first 2.2 m) nothing is drawn in front of the rungs, so the cell where a climber stands could be air (8 of 17 climb points at grade on most heaters). Fix in the stage it belongs to: `Access.Climb` is one ladder for columns and units (rung plane, climb zone, footprint); the layout keeps every cell a climb zone reaches as a shell cell; the block asks every shape.
- Heater coils: the convection coil and the radiant coil are separate services in practice (feed preheat, then the radiant pass; crossover piping often external), each with its inlet and outlet; fuel gas to the burner headers. Five nozzles at the front; the drawn convection-to-radiant crossovers go (the player pipes them); a cabin keeps one crossover between its back radiant header boxes (one pass along the right wall, back along the left).
- Rigid supports: heater legs are wide-flange columns (about W12 to W18) on concrete pedestals with base plates, a ring or perimeter girder under the floor, X-bracing in some bays and knee braces in the open ones (burner access).
- Compressor: a single-stage machine has one suction header over the cylinders feeding each suction bottle and one discharge header joining the discharge bottles; headers about √2 times a bottle's neck. Steel table: posts and beams like the air cooler's stand. Motor: a large low-speed synchronous motor is a fabricated octagonal stator frame on feet with a top-hat air cooler, an outboard pedestal bearing, an exciter and a side terminal box.
- Metallic look: research `METALLIC_LOOK_RESEARCH.md`; sizes from `MACHINES_SCALE_RESEARCH.md`: pixel = texel × tint × vertex colour × light, every factor at most 1; today's sprite streaks vary 1.9 % (below notice) and the baked shade is a broad gradient with no highlight. Prototype: a fixed highlight baked into the vertex colour of curved body polygons, k(|nz|^8 + 0.5|nx|^8), config `equipment.metallicSheen` (default 0 until the owner chooses: it goes beyond the earlier "metallic by texture only").

Reversible decisions (mine): roles `CONVECTION_INLET/OUTLET`, `RADIANT_INLET/OUTLET` (fresh worlds); VC radiant inlet at the front just under the roof; cabin radiant inlet in the right front header box near its top, outlet in the left one over the floor; convection outlet near the bottom of the front convection header box (a cabin's convection section grows up to a metre so its two rows fit); suction header over the frame on a cell centre with its inlet on top, discharge header in front of the frame with its outlet to the front; leg H-section `min(0.45, 0.28 + 0.015 size)`; girder the same depth.

Sizes (reversible, from `MACHINES_SCALE_RESEARCH.md`): compressor bore 0.15–1.2 m and 2–10 throws, speed from a 4 m/s mean piston speed (was 900 − 600 b rpm), power n(0.05 + 1.7 b²) MW, motor 0.3 + 1.1 (kW/rpm)^⅓ across, suction header 0.5 b√n + 0.05, discharge header 0.7 of it, rails min(0.9, 0.4 + 0.1 √MW); VC D 2.5–12 m, radiant height at most 20 m; pump and cabin unchanged (owner options). Outcome: `MACHINES_HEATERS_SECOND_REVIEW.md`.

## Third pass (owner decisions 2026-10-17)

Owner answers to the second review's propositions: BB1 pump above 0.4 m "ok"; twin-cell cabin "ok"; sheen "0.2 default, but test with popular shaders and fps optimization mod compatibility first"; fireproofed legs "yes". Owner reports: an error building a max-size compressor; the box furnace's walkway end hitbox not working.

- Walkway ends: collision samples 1/8-block voxel centres, so a 0.1 m end fence can fall between them. Closed ends take the side rails' 0.2 m band (`Access.RAIL_BAND`), line and arc decks alike; a gate (`stoppedAtClosedEnds`) checks every closed end of every heater deck fills voxels.
- Compressor build: the owner's world held a first-pass compressor's cells; the constructor refuses occupied cells (no save compatibility). Verify a 1.2 m × 10 build on a fresh world.
- Fireproofing: legs cased in concrete (0.45 m or the H-section depth + 0.1) from a 0.3 m pedestal to the radiant floor; VC and cabin.
- Twin cell: above 24 m (`MAX_CABIN_CELL`), up to 48 m, two cells of half the length side by side across a 1.5 m aisle (`AISLE`), throat walls straight up to one convection section, eave walls sloped; radiant coils fed and joined by inlet and outlet manifolds across the front of the aisle; duty counts both cells.
- BB1: discharge 0.45–0.75 m (0.95 m suction is the largest nozzle the 1 m pipes connect); double-suction casing split at the shaft, side nozzles 0.5 m under it (suction to the constructor's right, discharge to its left), finned bearing housings at both ends, an induction motor shared with the compressor (`largeMotor`).
- Sheen 0.2 by default once vanilla, Sodium with the usual optimisation mods, and Iris with five popular packs render it without fault; one FPS pair (0 against 0.2) under Iris.
- Correction: the second pass named the cabin's radiant header boxes from the heater's side; seen from the constructor the radiant inlet is in the left (+x) box and the outlet in the right one. Texts corrected, nozzles unchanged.
