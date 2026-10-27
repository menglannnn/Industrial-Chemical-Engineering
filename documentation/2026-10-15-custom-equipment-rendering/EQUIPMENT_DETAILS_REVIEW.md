# Air cooler height, fans and plenum, colour config, adaptive connections, boot, skirt junction, manways, bolts: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · plan `EQUIPMENT_DETAILS_PLAN.md`, survey `EQUIPMENT_DETAILS_PHOTO_SURVEY.md`. Committed on the branch as 668961be (code) and 2a2f9a19 (changelog), not merged or pushed.

## Physical expectation (from the plan)

- A forced-draught fan needs about half its diameter of free air under its ring: at a 3 m bundle every bay keeps 0.6 D or more, so the cooler can stand 2 m lower (least 3 m, default 4 m).
- Owner report with a photo from below: a plenum is a closed sheet-metal casing, one hopper per fan from the bundle's frame down to its ring; the fins show only through the rings. The rings with their inlet bellmouths (about 1.1 D), not the blades, set how close two fans in a bay can sit.
- Free water settles at the outlet end: the boot hangs inboard of the far saddle, 0.15 m clear, with no support of its own; its drain needs a block above grade, so a boot drum stands higher (3 × 9 m drum: 4 m, against 1 m without).
- A standing drum's manway sits 0.76 m over the bottom tangent; a lying drum's on its far head. A 1 m shell cannot hold a 0.45–0.6 m cover clear of its nozzle row, so a short pot has none (a body flange in real life).
- A real fan turns at about 5 rev/s; drawn at 1.5 rev/s so its blades do not alias.

## What the code did after the first test, and the fix

- Fans were sized by their blades (the share less 0.1 m), their rings drawn at 1.2 D: the 3 × 6 m bay's two 2.05 m fans had 2.46 m rings 2.42 m apart. Now `fanDiameter` sizes a fan by its ring (`RING_OD` 1.12 D, flange and bellmouth narrowed in the Blender part), rings 0.02 m apart: 2 × 1.9 m there (46 % of the face). Gate `fanRingsKeepApartInsideThePlenum`, every bay.
- The plenum was open below, its panels gridded. Now `ProcessUnit.plenum` draws a square-to-round hopper per fan (frame sides and corners to 32 ring points), both faces, in `AC_PLENUM` (restored).
- Ring sizing moves the bay set (API 661: 40 % coverage): 1.5 m wide bays fit from 3 m long (44 %); 2.5 × 3 m drops out (35 %), so `minLength(2.5)` is 4.

## What was built

- Bolts: `Mesh.bolts` on every flange (pipes, devices, nozzles, units, manways); class 150 counts and sizes from `PipeSizes.BOLTING`; closed hex nuts 1.6 d across, 1.2 d high; none where the land is narrower than a nut.
- Drums: `Manway` (shared with columns), skirt band and vents (`ShapeRasterizer.junction`), boot (`EquipmentShape.Boot`, drain `BOOT_DRAIN`, clashes `MANWAY`, `BOOT`). The boot toggle is on a lying drum's Nozzles page: as a design-page row it pushed Build and Done off an 854 × 480 screen. The datasheet panel widens to its longest line.
- Connections: `fit` and `place` work on a template drum; a change of orientation gives the new form's default connection (root cause of the saddle clash: the standing drum's side 3 faces down on a lying drum).
- `FanAnimation`: a GPU buffer per rotor size and light; the shader is set up once a frame, then each fan costs its matrix and one draw call; 96-block range, frustum-culled; the chunk mesh leaves the rotors out while it is on.
- Config `equipment.primaryColour`, `secondaryColour`; Paint page Primary/Secondary. Protocol `equipment-8`; NBT `Boot`; command word `boot`.

## In-game check (bridge lane, fresh superflat worlds, `details-check/`)

- `d-boot`, `d-back`, `d-heads`, `d-junction`, `d-bolts`: boot inboard of the far saddle with its drain down, bolted covers with davits, band and vents, nuts on every flange.
- `h-under-owner` (the owner's angle), `h-under-ac`, `h-side-ac`: two separate rings, a sheet casing tapering from the frame to each, fins seen only through the rings. `g-constructor-boot`: the Boot row on a lying drum's Nozzles page, the design page's four bottom buttons on screen at 854 × 480.
- Rotation: shots 2.1 s apart show the blades at other angles; shots under 1 s apart repeat a frame the bridge caches.
- FPS, 6 × 4 bank of 3 × 6 m bays (48 fans) in view, vanilla lane, 60 s + 60 s. One session, turning then still then turning: 936, 1555, 1486 FPS (p50 0.97, 0.57, 0.60 ms). The fans cost about 0.03 ms a frame; the first window, just after the 1,739-cell bank was placed, was slow for that reason (the earlier session's 1461 against 2448 had the same order). A 30 s JFR sample of the turning client: 0 of 266 render-thread samples in `FanAnimation`, the frame waits on the buffer swap. Four other projects' Java jobs (`geosim`) ran on the machine, so FPS between sessions is not comparable (this one ran 1.57 times slower).

## Gates

- Full unit suite: 1,245 tests, 0 failures (51 equipment tests, new `fanRingsKeepApartInsideThePlenum`, `bootsAndManwaysStandClear`, `fanRotorsTurnAboutTheirHubs`). The last edit after it is a javadoc line.

## Tools

- `tools/custom-equipment-rendering-poc/details-check/` (scenes, views, FPS command files, shots), `ac-blender/` (ring part), `logs/details-check-2026-10-16.tar.gz`. `VesselTextures.java` writes `equipment_bolt.png`.
- Code guide: refreshed at the merge (equipment chapter: `Manway`, `Boot`, bolts, hoppers, `FanAnimation`).

## Propositions for the owner

1. Skirt fireproofing (50 mm concrete with a flashing at the seam, survey B) as an option; not drawn now.
2. Manway covers keep the columns' red; they could take the primary colour instead.
3. The photo's square mesh fan guard could be drawn under each ring; it needs a cut-out render type (the shell renders solid).
4. Fan speed could follow a process state (fan on/off) once air coolers carry duty.
