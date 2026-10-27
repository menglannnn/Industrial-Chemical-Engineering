# New units: the drawing kit and the lane workflow (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, step "new units". Each new unit kind is one file
`src/main/java/com/wormzjl/createcheme/science/equipment/<Name>Design.java` extending `UnitDesign` (read its javadoc and
`CompressorDesign.java` / `VerticalHeaterDesign.java` as examples). Its `build(ProcessUnit u)` draws into the unit
through `u`'s package-private kit; the eight older kinds' builders in `ProcessUnit.java` (`pump()`, `compressor()`,
`verticalHeater()`, `cabinHeater()`, `exchanger()`, `airCooler()`) are the reference for style and proportions.

## Frame and rules (the gates check all of them)
- Frame: y up from the ground (y 0 = grade), the unit's front (the constructor's side) at frame -z, +x is the
  constructor's LEFT. 1 unit = 1 m = 1 block. The box is the parts' and flange faces' bounds rounded out to whole cells:
  x and z may start anywhere (corrected 2026-10-18; no need to shift a layout), y starts at grade 0 and nothing goes
  below it. Centre line rules still hold (`ProcessUnit.centre(half)` centres a body on a cell centre).
- `u.shape` gives `diameter()` (main size), `tangentLength()` (second size), `support()`; never read anything else.
- Every nozzle (`u.nozzles.add(...)`) runs its centre line through cell centres (x.5) and ends its flange face on a
  block face, with the block beyond the face free (no part there) and at y >= 0. Helpers: `ProcessUnit.front(role, x,
  y, z, maxOd, defaultOd)` (points -z), `radial(...)` (up/down from a round body), `radialSide(...)`, or
  `new Nozzle(role, base, axis, sign, length, maxOd, defaultOd, riser, exit)` (see `EquipmentShape.Nozzle`). maxOd <= 1.
- Owner rule 2026-10-17: the pipe blocks (the block just beyond each flange face) of two nozzles never touch, not by a
  face, an edge or a corner (Chebyshev distance >= 2). Gate `ProcessUnitTest.pipesApart`.
- Collision: every drawn part a player can bump into adds a solid: `u.solids.add(ProcessUnit.Solid.box(lo, hi))`,
  `Solid.rod(centre, axis, lo, hi, r, headHi)`, `Solid.cone(...)`, `Solid.ring(...)`; closed bodies also add the same
  solid to `u.volumes` (their inside cells become interior cells, which must be sealed by shell cells).
- Round bodies: `u.round(x, y, lo, hi, r)` / `u.round(x, y, lo, hi, r, axis)` / `u.round(centre, axis, lo, hi, r)`
  register a body's outline (selection outline follows the eye). Fixed outline edges: `u.edges.add({x0,y0,z0,x1,y1,z1})`.
- Platforms and ladders: `u.platform(Access.Run, level, width, closedStart, closedEnd, gaps...)`, `u.railing(...)`,
  `u.ladder(x, z, out, back, base, top, walkThrough)`, `u.verticalLadder(...)` (see the heaters). Decks 0.8 m clear,
  2.1 m headroom; caged ladders. Gate `heatersCarryWalkableDecksAndClimbableLadders` style checks apply to every deck.
- Moving parts (only where the survey shows something turning or sliding in sight): `u.mesh.part(u.motions.size())`,
  draw the part, `u.mesh.part(-1)`, `u.motions.add(ProcessUnit.Motion.turning(pivot, axis, speed, phase))` (or a
  slider-crank `new Motion(...)`); unpainted materials only (`material.channel() < 0`). Set `moves()` true.
- `science/` code calls `DeterministicMath.sin/cos/tan/atan2/acos/asin/pow/cbrt/hypot/log/exp`, never `Math.` versions
  (`Math.sqrt`, `Math.fma`, `Math.abs/min/max/floor/ceil/round` are fine). Gate `DeterministicMathSourceTest`.

## Mesh primitives (`u.mesh`, `Mesh.java`)
- `Frame(origin, axis, ref)` / `Frame.of(origin, axisIndex, sign)`: a local frame; z along `axis`.
- Round: `prism(f, r, z0, z1, n, mat)` (side only), `cylinder` (capped), `frustum(f, r0, r1, z0, z1, n, mat)`, `bore`
  (inside face), `annulus(f, rIn, rOut, z, n, sign, mat)`, `ring(f, rIn, rOut, z0, z1, n, faces, mat)`, `head(f, r,
  at, depth, n, mat)` (2:1 or hemispherical by depth), `torus(...)`, `sphere(c, r, n, mat)`; `n` sides from
  `ProcessUnit.sides(od)` (12 at least).
- Flat: `box(lo, hi, faces, mat)` (faces mask `ALL_FACES & ~bit` drops buried faces; bits -x 1, +x 2, -y 4, +y 8, -z 16,
  +z 32), `block(centre, ex, ey, ez, mat)` (rotated box), `bar(a, b, width, up, [ends], mat)`, `rail(points, width,
  up, ends, mat)`, `flat(polygon, outward, mat)`, `bolts(f, od, inner, outer, z, sign, mat)` (flange nuts).
- `ProcessUnit` helpers worth reusing: `box(lo, hi, mat, solid)`, `segment`, `leg`, `girder`, `brace`, `knees`,
  `header`, `bottle`, `line`, `extrude`, `panels`, `seam`, `joint`, `stack`, `stackJoint`, `door`, `burners`, `motor`,
  `largeMotor`, `grilledFan`, `shaft`, `saddle`, `plate`, `table`, `spool`, `manifold`.
- Materials (`Mesh.Material`): SHELL, HEAD, CLADDING, MACHINE, HEATER (painted body), SUPPORT, STEELWORK (painted
  structure), PIPE, FLANGE, BOLT, OPENING (dark), GRATING, HANDRAIL, MOTOR, ACTUATOR, GUARD, CONCRETE, FEED, DRAIN.
  Parts under about 0.05 m are skipped unless a signature part (1 px = 62.5 mm).

## Lane workflow (no Gradle: several lanes share this worktree)
1. Edit only your own `*Design.java` files (and nothing else in `src/`). New roles, materials, kinds, lang keys or
   textures: ask the orchestrator in your report; do not edit shared files.
2. `bash tools/new-units/check.sh <lane> <KIND[,KIND]> <your Design.java files>` compiles your files into
   `build/tmp/units/<lane>/out` and runs ProcessUnitTest, ShapeRasterizerTest, LineGeometryTest and the determinism
   source gate against them (every unit size of every kind, yours included), then prints your kinds' counts (box, cells,
   quads, moving parts, nozzles, layout ms). A FAIL naming another lane's kind is not yours (report it).
3. `bash tools/new-units/render.sh <lane> <KIND> <size> <length> <support> <name> [views]` writes textured PNGs to
   `build/tmp/units/<lane>/render/` (views iso, iso2, iso3, front, back, left, right, top, low). Look at them (Read the
   PNG) at the smallest, middle and largest sizes; compare with the survey's photos and proportions.
4. Never run `./gradlew` (the orchestrator integrates and runs Gradle).

## The eleven-unit batch (NEW_UNITS_PLAN.md, owner decisions D1-D4 of 2026-10-18)
- Your lane replaces its kinds' placeholder designs (`<Name>Design.java`, now a box with a feed nozzle) wholly: keep the
  class name, `final class ... extends UnitDesign`, and the kind's planned main-size range (the plan's table) unless
  physics forces a change (say so in your report). Read your survey section first; its numbers are the definition.
- Helper classes: you may add package-private files whose names start with one of your kinds' design names minus
  `Design` (e.g. `SteamTurbineTrain.java`, `DelayedCokerDerrick.java`). Pass every file of yours to `check.sh`. Never edit
  `ProcessUnit.java`, `Mesh.java`, `Access.java`, `EquipmentShape.java`, tests or other lanes' files; call the kit's
  package-private methods freely. Kit gaps: work round them in your files and name them in the report.
- Roles available (EquipmentShape.Role): the old ones plus PRODUCT, NITROGEN, COOLANT_INLET, COOLANT_OUTLET,
  REGENERATION_INLET, REGENERATION_OUTLET, HYDROGEN, EFFLUENT, QUENCH, CATALYST_INLET, AIR, OXIDANT, WATER, CUTTING_WATER,
  BLOWDOWN, SOLIDS_INLET, FEEDWATER (bulk); VENT, EXHAUST, FLUE_GAS, RELIEF, SYNGAS (vapour); DRAIN, CONDENSATE,
  CATALYST_OUTLET, SOLIDS_OUTLET (dense phase). Use the plan's role list for your kinds; a role may appear once per unit
  unless the plan lists it twice.
- Stairs (new kit primitive): `double len = u.stair(x, z, axis, sign, width, base, top)` draws a straight flight whose
  foot's centre is (x, z), rising along frame axis 0 or 2 in direction sign, width >= 1.0 (fences 0.2 m inside each side,
  a player is 0.6 m wide), from walking level `base` (0 = ground, or a deck's `level + Access.DECK`) to `top` = the
  landing deck's `level + Access.DECK`, exactly. The landing deck (`u.platform(...)`) starts where the flight ends
  (`len` along). Stair towers: flights alternating direction with a landing platform between, at most 3.7 m rise per
  flight (OSHA). Small or tall-slender units keep caged ladders (`u.ladder`), as the heaters do.
- Gates now run on every size of every kind: every deck walkable with headroom, every ladder climbable from a deck or
  the ground, every stair flight joining decks or the ground with fenced, stepped treads; every moving part (`moves()`
  true) is its still part set going, unpainted; every nozzle leaves a free pipe block, one free block between pipe blocks.
  The nozzle-face table (`nozzlesFaceWhereTheirFluidGoes`) skips your kinds: report your faces as `ROLE axis±` (axis 0 x,
  1 y, 2 z; sign of the flange's direction) for every kind, and they join the table at integration.
- Budget: at most about 60k quads at the largest size (80k hard ceiling), layout under 200 ms (`UnitCounts`); detail goes
  before size (owner D3). Parts under 0.05 m only when a signature.
- Lang: write `tools/new-units/lang/<lane>.json`, a flat JSON map key -> English text, for every key your designs use:
  constructor rows `gui.createcheme.equipment_constructor.<row>` and `<row>.hint` (size row, second-size row, support
  row), the held line `gui.createcheme.equipment_constructor.<key>` and `<key>.hint`, datasheet lines
  `gui.createcheme.vessel.<key>`, and `gui.createcheme.vessel.unit_support.<kind>` (one %s, metres). New keys start with
  your kind's lower-case name (`fcc_converter.riser`); reuse an existing key only when its text fits word for word
  (grep `src/main/resources/assets/createcheme/lang/en_us.json`). House style: plain words, units in the text, hints say
  the range and what the size sets.
- Report (your final message, under 60 lines): per kind the ranges chosen and why, the parts drawn, the nozzle faces,
  the UnitCounts table (smallest, middle, largest), which renders you looked at and what you fixed after looking, kit
  gaps worked round, and open choices for the owner. Do not write documents elsewhere.
