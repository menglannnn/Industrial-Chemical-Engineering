# Ghost, pipe ends, bottom line, metallic paint, smaller units, air cooler banks: plan

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · follows `STANDALONE_UNITS_REVIEW.md` (2f1ddb85, a2b4c2c8).

## Owner requests (2026-10-16, after the in-game look at 2f1ddb85)

1. The constructor's ghost shows without aiming at it, and goes once built.
2. The air cooler's model and textures are improved. Decision: Blender parts, scripted (headless Blender 5.2).
3. Air coolers are built in series and in parallel. Decision: one bank unit, N bays in parallel × M in series, with manifolds and crossovers drawn, and one inlet and one outlet.
4. Smaller ejectors and air coolers are allowed.
5. A 1 m pipe on the bottom of a drum or column looks wrong. Decision: the line rises with the skirt.
6. An unconnected pipe tip ends in a flange.
7. Drum sides: fainter seams, more shiny and metallic, by texture only ("no need to do reflection on the surface").
8. Paint colours for the metal parts are chosen when building, from a palette with good industrial defaults.

## Physical expectation

- Bottom line: its run must clear grade by its flange radius, its port must clear the base plate, and its elbow (1 OD) must fit under the head's low point. A D 3 drum on a 2 m skirt (low point 1.25 m) takes 0.69 m. A full 1 m line needs its run at 1.5 m and the low point at 2.5 m or more, so a skirt of about 2.5 m + D/4.
- Air cooler bay: the bundle face is (W − 0.22) × (L − 1 − h) m, with header depth h = min(0.6, 0.15 L). API 661 asks fans to cover at least 40 % of it. A bay takes one to three fans along its tubes, whichever covers the face best, each as large as its share of the plenum floor allows. A 3 × 6 m bay keeps two fans of about 2 m, covering 54 %.
- Series bays are single-pass: the inlet is on top of the front header, the outlet low at the back (condensate drains out). Series bays stand back to back, joined by a short spool from header to header. Parallel bays stand side by side, with an inlet manifold over the first row's front headers and an outlet manifold behind the last row's back headers.
- Under the bundle: plenum 0.2 + 0.4 D deep, the fan ring at most 0.9 m tall (scaled to 0.35 of it), the motor under the guard, and the motor beams level with the side beams. A 5 m bundle leaves at least 2.5 m of headroom under the beams for every fan size.
- Small ejectors (suction ≤ 0.2 m) are carried by their piping in a plant. Here they get a single post behind the body, so the footprint is one cell.

## Reversible decisions (mine)

- Ghost: every constructor within 48 blocks shows its design until its vessel stands there. Red outlines show only for the open screen's draft or the aimed constructor.
- Pipe ends: an unconnected face ends in a blind flange on the block face. A dead end runs straight through its block to a blind flange opposite its connection, not to a ball.
- Paint: two channels, Body (shell, heads, cladding, unit casings, air cooler headers, plenums and fan rings) and Structure (skirts, saddles, steelwork). Pipes, flanges, nozzles, gratings, handrails (safety yellow), fins, fans and motors keep their own colours.
  - The palette has 16 RAL colours (common sRGB renderings). Defaults: Body RAL 9006 white aluminium, Structure RAL 7031 blue grey (changed from 7035 during the build: it keeps today's darker skirt, and 7035 is lighter than the body). Any colour can be typed as #RRGGBB.
  - Painted sprites become light and neutral (mean 236), tinted through a tint index (a `BlockColor` that reads the shell's block entity). The ghost shows the paint.
- Sizes (revised during the build from the fan-coverage table):
  - Ejector suction 0.05–1 m in 0.05 m steps. At 0.2 m or less, one post in the cell behind (two cells, not one: the cell under the discharge is its pipe's).
  - Air cooler bays 1.5–3 m wide in 0.5 m steps, 3–6 m long, only where 1–3 fans can cover 40 % of the bundle face: 14 bays. A 1 m bay never reaches 33 %, and a 3 × 3 m bay reaches 33 %.
  - Banks: 1–6 bays in parallel, 1–4 in series. The outlet manifold runs behind the last back headers, with the outlet underneath, because the legs under a narrow bay's header would block its pipe.

## Steps

1. Ghost; pipe ends; bottom line rising with the skirt; metallic sprites (generator in `tools/.../textures/`).
2. Paint: Design and shell NBT `Paint`, protocol `equipment-7`, a constructor Paint page, a tinted bake.
3. Ejector sizes and the single-post stand.
4. Air cooler bank: sizes, fans, single-pass headers, manifolds, crossovers, nozzles.
5. Blender parts, a separate agent: a fan rotor, a fan ring with guard, a motor, and the fins, header and plenum sprites. These are OBJ and PNG files with previews; an OBJ part loader turns them into facets.
6. Gates: free pipe blocks for banks of every size; flanges at pipe ends; outlet rows by skirt. Then an in-game check, a review, the changelog and docs.
