# Kit consolidation: plan (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, on the branch after the new-units baseline (d084b777 + 4dc78964).
Owner: "go back to consolidation" (after abandoning `UNIT_ENGINE_PLAN.md`). The 14 new designs were drawn by lanes that
could not edit the shared kit, so they hand-built what it lacked. This step moves those pieces into the kit
(`ProcessUnit`, `Access`, `Mesh`, `PipeSizes`) and makes every design call it. Survey: 6 stair towers, 3 concrete
tables, 2 own deck functions, about 20 smaller duplicates, 5 cross-design dependencies.

## Owner decisions
- **C1 (2026-10-18): rest every 9 m on units.** `MAX_CLIMB` (9 m, the old 30 ft cage rule the columns follow) moves to
  the kit; every unit ladder climbing more than 9 m gets rest platforms (adsorber about 14 m, stirred reactor 12,
  cyclone 16, FCC 16, HTW gasifier up to 20), and a gate checks every unit ladder.
- **Equivalence (E3 of the abandoned plan, kept):** a shared component may change a unit's look slightly where it
  replaces a hand-built copy; checked by the gates, counts against the baseline and renders, and listed in the review.

## Logged decisions (reversible)
- **Stair tower:** flights 0.9 m wide on lanes 1.0 m apart with their centres on cell centres (rails and stringers then
  never straddle a whole metre, which cost the coker about 1.5 × quads); equal flights of at most 3.6 m between the
  stops; a landing 1.2 m deep at each turn, bracket-less on corner posts, optional beams under it. Drops the copies'
  own widths (1.0 to 1.1), landing depths (1.1 to 1.25), the 3.7 m rise, the coker's 1/8 m rounding and the
  compressor's top flight landing straight on its walkway (it gains a top landing).
- **Toe boards run only over rail pieces** (none across a ladder's or a stair's opening, where a real one would trip the
  person stepping off): a visible change on heater and column platforms too.
- **Braces are X braces;** the coker's single diagonal per storey becomes an X (more quads, checked against the ceiling).
- **Stacks** have one joint pitch of 8 m, drawn as one band.
- Columns keep their structure: only `MIN_DECK`, `MAX_CLIMB`, the cage clearance and the davit move to the kit, unchanged.

## Kit API (one worker, before the migration)
Primitives, leaving existing kinds unchanged:
- `Mesh.hoop(f, rIn, rOut, z0, z1, n, m)` (a ring without bore); `Mesh.head(f, r, at, depth, n, bands, m)`;
  `Mesh.bend(f, bend, r, a0, a1, segments, n, m)` (a segmented torus).
- `ProcessUnit.box(lo, hi, faces, m, solid)`; `pipe(r, m, solid, points...)` (colliding, axis-aligned);
  `line(r, n, m, points...)`; `elbow(start, d0, d1, bend, r, n, segments, m)`; `end(role, base, axis, sign, maxOd,
  defaultOd)` (a nozzle at a line's end); `radialSide(..., sign)`; `saddle(..., floor)`; `static clamp(x, lo, hi)`.
- `Solid.rod` and `Solid.cone` with a head at either end (`headLo`, `headHi`).
- `transition(axis, ...)` along any axis; `brace(p, q, low, high, width, tier)`.
- `verticalLadder(x, z, wall, t, base, top, walkThrough)`; `Access.clearance(wall)` (the cage clearance, five copies);
  `Access.MAX_CLIMB`, `Access.MIN_DECK`, `Access.climbs(base, wanted, top)` (rest levels at most 9 m apart).
- `motor(Frame f, r, z0, z1, shaft, floor, detail)` on any axis, detail FULL (fins and turning fan), FINNED or PLAIN.
- `stack(x, z, od, base, top, paint, breeching, damper)` without the 30 m cap, and `stackTop(neck, roof)`.
- `PipeSizes.od(want, least, most)` (the nozzle size rounding, from the stirred reactor).
Access parts:
- `Access.deck(m, run, level, width, parts, railGaps)` and `platform(run, level, width, parts, closedStart, closedEnd,
  gaps...)` with parts BRACKETS | RAILED | WALL_EDGE (the old overloads: all three); toe boards over rail pieces only.
- `squareDeck(x, z, half, body, level, ...)` with `fill(...)`: a square deck with an opening round a round body (six
  copies with five corner fills).
- `ringAccess(...)`: ring platforms with arc ladders (vertical heater, trickle and ebullated bed, FCC, HTW gasifier).
- `concreteTable(x0, x1, z0, z1, top, slab, holes, column, posts)`: slab cut round its holes (drawn the coker's way,
  outer and hole faces only), columns on pads, solids and volumes.
- `stairTower(x, z, axis, sign, side, stops, beams)` returning `Tower(openings, end)`; callers cut the openings into
  their deck rails.
- The davit (column and hydro copies) and the hydro's generic parts (pedestal, skirt, base ring, studs, heavy joint,
  levels) move to the kit; a post-and-beam pair replaces `FixedBedGasifierHouse.column/beam`.

## Migration lanes (parallel opus workers, disjoint files, `tools/new-units/check.sh`, no Gradle)
| Lane | Files | Also |
|---|---|---|
| A | CentrifugalCompressorDesign, CondensingTurbineDesign, SteamTurbineTrain | stair tower, concrete table |
| B | DelayedCoker* (5) | stair tower, table, own deck, braces |
| C | FccConverter* (3) | stair tower, own deck, head, bend, ring access; rest platforms |
| D | FixedBedGasifierDesign, FixedBedGasifierHouse (removed), GrateBoilerDesign | stair tower, motor, stack |
| E | FluidizedBed* (5) | stair tower, motor, stack, hoop, ring access; rest platforms (HTW) |
| F | StirredReactorDesign, AdsorberDesign | square deck, motor, od; rest platforms |
| G | TrickleBedReactorDesign, TrickleBedReactorParts (shrinks or goes), EbullatedBedReactorDesign | kit parts, ring access |
| H | CycloneDesign | transition, brace, motor; rest platforms |

Each lane reports counts and renders against the baseline (`build/tmp/units/fix/counts.md`, baseline renders in
`build/tmp/units/base/render/`), the visible changes, and leaves no design calling another design's file.

## Gates and close
- New in `ProcessUnitTest`: every unit ladder climbs at most `MAX_CLIMB` between its foot and its landing (C1), and no
  design file references another design's class (a source scan, like the determinism gate).
- One Gradle gates run after the kit batch, one at the end (equipment tests and `DeterministicMathSourceTest`);
  `UnitCounts` for all 22 kinds against the baseline (the 80k ceiling holds); renders; one bridge-lane check of the
  changed access steel; review (at most 50 lines), CHANGELOG line, docs and tools to the main checkout.
