# Equipment rendering performance: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · owner question "Could some common components with fixed size such as manholes and stairs [be] modeled by blender to potentially improve performance?", "include ram cost as well", then "Try to optimize the performance as much as possible". Committed on the branch as 8f0b81fa (code) and 3ad9884e (changelog), not merged or pushed.

## Expectation

- Equipment is baked into the chunk mesh: a static part costs no frame time (proof of concept: 20 columns of 6 × 40 m, no cost on vanilla or Sodium, +0.06 ms with Iris). It costs quads (128 B each in vanilla's chunk buffers), client heap (about 180 B a baked quad, about 750 B a facet of the geometry cache) and the time to cut a cell (ms per vessel).
- Blender changes none of that, and a fixed size buys nothing: each vessel copies a part and cuts it into its own cells. Blender parts cost more: a chunk draws each triangle as a whole quad.
- Before (`QuadShares`): standing drum 3 × 9 m 3,522 quads, 1.3 MB geometry; 6 m column 26,983, 9.4 MB; 2 × 2 air cooler bank 22,272, 11.6 MB, three quarters of it Blender parts. Nuts are 25–31 % of a drum, ladders and handrails a third of a column; manways a few percent.

## What was changed

- `Part` merges two triangles into one quad where the result is exact: flat, convex, and texture and normals one affine field (within 1e-4). `ac_parts.py` lays the ring's texture by arc length, which makes its flat and conical bands affine (paint unchanged). Faces: ring 792 → 516, motor 384 → 208, rotor 268 → 238.
- The plenum hopper meets the ring at its 24 corners (`ProcessUnit.RING_SIDES`; it was 32).
- `Mesh.bar` takes an ends mask; ends buried in a rail, a deck, a post or a hoop are left out (rungs, posts, cage bars' feet, brackets, closing rails, ladder rails' feet). `Mesh.rail` draws deck-edge rails and cage hoops as one mitred bar: no end faces at the bends.
- Texture frames are shared: box projections per material and facing (`Mesh.BOX_UVS`); `include` maps a unit's shared frames and shadings once.
- Client cache: one entry per cell, two quad lists only where a cell holds a rotor (no second full cache after the fans switch).
- Dedicated server: `ShapeRasterizer.serverOnly()` at mod construction; a geometry drops its facets, facets by cell and nozzle meshes once laid out, and refuses to draw.
- Tried and reverted: nuts without their seat (8 quads, not 10). `LineGeometryTest` found four large devices whose flange does not cover every seat, so the nut's inside showed. Not done: flat head crowns (the crown drops by 1.3 % of D, 13 cm on a 10 m head, where a top nozzle's neck could show a gap).

## Result

| Vessel | Quads | Geometry heap, client | Dedicated server |
|---|---|---|---|
| Drum 3 × 9 m, standing / lying | 3,522 / 2,831, unchanged | 1.3 → 1.2 / 0.9 → 0.8 MB | 0.02 MB |
| Column 3 m, 20 trays | 13,140 → 11,891 (−9.5 %) | 4.9 → 3.4 MB | |
| Column 6 m, 30 trays | 26,983 → 25,035 (−7.2 %) | 9.4 → 6.6 MB | 0.12 MB |
| Air cooler bank 2 × 2 of 3 × 6 m | 22,272 → 16,840 (−24 %) | 11.6 → 8.1 MB | 0.14 MB |

Chunk buffers and the baked cache fall with the quads. Cutting stays 2–25 ms a vessel. A server's layout and collision boxes are the same as before; they are built lazily, about 0.3 ms a cell on first touch.

## Gates and in-game check

- Full unit suite on the final code: 1,246 tests, 0 failures (equipment 52). The run before the nut revert failed two `LineGeometryTest` gates (watertight runs; outer faces only), as above.
- New gate `ShapeRasterizerTest.aDedicatedServerLaysOutKeysAndCollidesWithoutFacets`: layout, keys with pipes on every nozzle, solids and ports match the drawing side, and a surface is refused. Proof: with the server also dropping its nozzle-reach table, it fails (line 31).
- Dev client, fresh world (`perf-check/`, shots `o-*`, batch shots 107–114): rails and hoops closed at every bend, rung ends clean, air cooler from below and the side as before, fans switch shows the still rotor and hides it while turning.

## Tools

- `tools/custom-equipment-rendering-poc/`: `QuadShares.java` and `ServerHeap.java` (counts and heap per vessel), `perf-check/PartPairs.java` (merge refusals per part), `perf-check/` (scene, views, shots), `logs/perf-check-2026-10-16.tar.gz`. `ac-blender/ac_parts.py` with the arc-length ring texture.
- Code guide: at the merge (equipment chapter: `Part` merging, `Mesh.rail`, ends mask, `serverOnly`).

## Propositions for the owner (look changes, not applied)

1. Nuts smaller than a texel (all class 150 nuts are under 62 mm) as closed square prisms: 6 quads instead of 10, about −10 % on a drum and −3 to −6 % on a column.
2. Flat head crowns where the drop stays under a texel (D under 5 m): about −4 % on a drum.
