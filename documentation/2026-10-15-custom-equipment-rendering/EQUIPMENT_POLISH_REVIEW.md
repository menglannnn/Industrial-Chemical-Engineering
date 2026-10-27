# Ghost, pipe ends, bottom line, metallic paint, smaller units, air cooler banks: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · plan `EQUIPMENT_POLISH_PLAN.md` · committed as ada9b340 (code) and 768e6fb6 (changelog), not merged or pushed.

## Physical expectation (from the plan)

- Bottom line: the elbow (1 OD) fits under the head's low point and the run clears grade by a flange radius. A D 3 drum on a 2 m skirt takes 0.69 m; a full 1 m line needs a skirt of about 2.5 m + D/4.
- Air cooler bay: bundle face (W − 0.22)(L − 1 − h), h = min(0.6, 0.15 L); fans cover at least 40 % of it (API 661). A 1 m bay never does; a 3 × 6 m bay keeps two fans of about 2 m (54 %).
- Banks: series bays are single-pass and stand back to back on spools; parallel bays share an inlet manifold over the front headers and an outlet manifold behind the back headers.
- Under the bundle: plenum 0.2 + 0.4 D, ring at most 0.9 m, motor beams level with the side beams: 2.5 m of headroom under a 5 m bundle.

## What was built

- Ghost: every constructor within 48 blocks shows its design, painted, until its vessel stands; red outlines only for the open draft or the aimed constructor.
- Pipe ends: an unconnected face ends in a blind flange; a dead end runs through its block to a blind flange (`LineGeometry`).
- Bottom line: the lowest cell row that carries min(D/2, 1 m) under the elbow, port and plate limits (`EquipmentShape.skirtOutletLine`). The D 3 drum on a 4 m skirt carries its 1 m line at 1.5 m.
- Metallic look: new neutral sprites (mean 236), welds only faintly brighter, a fine grain, soft sheen streaks along the plate and sparse bright flakes; no reflection (`textures/VesselTextures.java`).
- Paint (`Paint`): Body and Structure channels, 16 RAL swatches, any #RRGGBB; defaults RAL 9006 and 7031. A `BlockColor` reads the shell's block entity and the quads carry the tint index of `Mesh.Material.channel()`. The constructor has a Paint page; NBT `Paint` on constructor and shell.
- Ejectors: suction 0.05–1 m in 0.05 m steps; at 0.2 m or less, one post behind the body on a base plate.
- Air coolers (`ProcessUnit`): 14 bays (1.5–3 m wide, 3–6 m long, by coverage), banks of 1–6 parallel × 1–4 series (`Bank`), 1–3 fans per bay. NBT `Bank`; command `unit … air_cooler W L h front [parallel series]`.
- Blender parts (`tools/.../ac-blender/`, headless Blender 5.2, deterministic): fan rotor, ring with guard, motor as OBJ; fins, header and plenum sprites. `Part` loads OBJ triangles into facets with per-triangle affine UVs and interpolated normals.
- Protocol `equipment-7`. Fresh worlds.

## In-game check (bridge lane, fresh superflat world)

- `polish-check/scene.txt` placed three drums (D 3 on 2 m and 4 m skirts, D 2 on 3 m), pipes, three coolers (3 × 6; 2 × 4 bank 3 × 2; 1.5 × 4), four ejectors (0.05, 0.1, 0.2, 0.25 m) and two constructors.
- Shots `p-*`: blind flanges on both pipe ends; bank with inlet manifold, crossovers and outlet; underside with ring, guard, rotor and motor; small ejectors on one post; both ghosts showing without aim. Batch copies are shots 82–93.
- Paint page `g-3`, `g-4`: picking RAL 3000 turned the ghost red live; RAL 7016 for the structure. Built (`g-6`): red body, anthracite skirt, ghost gone, the other ghost kept. The client log holds no mod error.

## Gates

- Full unit suite on the final code, after the client stopped and the probe was detached: 1,241 tests, 0 failures.
- New or changed gates: `PaintTest` (4), `ProcessUnitTest` (bank sizes, bad sizes, 14 bays, minimum lengths, 3 × 6 fans, small ejector footprint, headroom), `ShapeRasterizerTest.bottomLineRisesWithTheSkirt`, `LineGeometryTest.unconnectedEndsCloseInABlindFlange`.

## Tools and leftovers

- `tools/custom-equipment-rendering-poc/polish-check/`: scene, views, shots, `probes/OutletRenderProbeTest.java` (never committed) and its `renders/`; logs in `logs/polish-check-2026-10-16.tar.gz`.
- `textures/VesselTextures.java` (painted sprites), `textures/Neutral.java` (neutralises a tinted sprite), `ac-blender/`.
- Code guide: refreshed at the merge (the equipment chapter gains Paint, Part, Bank).

## Propositions for the owner

1. Measure the FPS of a large bank (6 × 4 bays, 1–3 fans each, OBJ parts) on the three lanes before the merge.
2. The RAL values are common sRGB renderings, not colour-managed; a swatch may look slightly off against a printed card.
