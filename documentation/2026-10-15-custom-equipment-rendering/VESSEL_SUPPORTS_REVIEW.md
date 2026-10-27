# Vessel supports, horizontal vessels and the refined shell

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` (committed 02bf33f0..9a9ef8cc, not merged) · 2026-10-16 · Status: In progress since 2026-10-15.

Owner decisions: a skirt for vertical vessels; two saddles at 0.2·T/T for horizontal ones (both 2026-10-15). Owner requests 2026-10-16: "Model and texture needs some refinement", "Look for actual pictures", "model need a bit more polygon".

## Physical picture

- **Skirt.** A cylinder of the shell's outside diameter, welded to the bottom head, on a base ring with anchor-bolt chairs. The minimum height ⌈D/4⌉ encloses the head. The default ⌈D/4 + 1⌉ leaves at least 1 m under the head for outlet piping and pump suction: 2 m for D 2, 3 m for D 6, 4 m for D 10.
- **Saddles.** Centred 0.2·T/T from each tangent line (Zick: the span and the overhangs share the bending), cradling 120°. A saddle is a curved top (wear) plate, a web, a rib at each horn and a base plate.
- **Photos** (image survey 2026-10-16, shop and field shots): rolled plate in courses of about 2 m with circumferential welds and staggered longitudinal seams; seamless pressed heads with one weld at the tangent line; supports in plain structural steel, often painted differently.

## Built

- **`EquipmentShape(D, T/T, orientation, support)`.** Orientation VERTICAL, ALONG_X or ALONG_Z; box `sizeX/Y/Z`, widened by a skirt's base ring; `anchorChairs()`. Facets of at most about 1/3 m (n = max(12, 4⌈3πD/4⌉)) and heads of n/4 bands; D3 had 0.5 m facets and n/8 bands.
- **`ShapeRasterizer`.** The vessel is built in its own frame and laid down by a proper rotation.
  - Parts: the skirt; a base ring 1/8 m wide with chairs; saddles with a 1/16 m top plate, 1/8 m web and ribs and a 1/16 m base plate, each 0.375 m along the axis.
  - Materials SHELL, HEAD and SUPPORT, each facet with its own UV frame. Shell plates are fitted to the circumference (about 4 m) in 2 m courses from the bottom tangent line; a head's sprite runs once from the weld to the apex; supports repeat every 2 m.
  - Polygons are split where a tile wraps. Body keys carry a texture class, at most 8 per vessel.
- **Sprites `vessel_shell`, `vessel_head`, `vessel_support`.** The mod's first own textures, made by `tools/custom-equipment-rendering-poc/textures/VesselTextures.java` from a fixed seed.
- **Constructor.** The design is relative to the constructor: the form (Vertical, Lengthwise or Crosswise; `VesselPlacement.Form`, resolved with the facing like the offsets) and the support height. A skirt left at its default follows the diameter.
  - Protocol `equipment-2`. NBT: Form and Support on the constructor; Orientation and Support on each shell cell.
  - Interior cells find their vessel by walking east, then south. Command: `place <pos> <D> <T/T> [rows | vertical|along_x|along_z <m>]`.
  - The datasheet adds form, length, top and support lines.

## Checks

- **Tests.** `ShapeRasterizerTest` 11/11:
  - new: materials per part, the box filled exactly, supports carry the vessel;
  - area: pieces equal the facets' area, plus a closed form for bare vessels;
  - volume: a closed form per part.

  `VesselPlacementTest` 3/3 (non-square boxes; the form turns with the constructor); `DeterministicMathSourceTest` 2/2.
- **Chunk quads (`LayoutCounts`).** 6 × 40 m column 3,978 → 6,760; 10 × 60 m column 10,571 → 17,592; 6 × 40 m on a 3 m skirt 5,309; 3 × 9 m drum on saddles 1,681. FPS was not re-measured; in the PoC, 80 k quads cost nothing on vanilla or Sodium and +0.06 ms per frame under Iris.
- **Dev client** (bridge lane, fresh worlds), screenshots 18–30:
  - 18–19 first look; 20–24 refined scene and close-ups; 25–27 the three forms with the ghost.
  - 28 lengthwise build ("24 cells from 210, -60, 1"); 29 datasheet.
  - 30 a player dropped on a 2.5 m drum on 1 m saddles rests at y = −56.25 = −60 + 1 + 1.5 + 1.25.

## Findings

1. **Crash, fixed before the check.** `Design`'s compact constructor validated through `shape()`, which reads fields a compact constructor has not yet assigned: an NPE in class initialisation on the first constructor placed. It now validates from the parameters; the mapping lives in `VesselPlacement.Form`, with a test.
2. **Volume check narrowed.** The 2 % check now applies from D 2.5 instead of D 2. The base ring widens a skirted D 2 box to 3 cells, and on that alignment 1/8 sampling alone reads +2.1 % (probe). The theoretical band check is unchanged.
3. **Hidden overdraw kept for simplicity:** the shell under the top plates, the base ring under the chairs.
4. **Not built:** skirt manway and vents, stiffeners between the horns, nozzles. The ghost draws every part with the shell sprite.

**Proposition for the owner.** Accept the look in screenshots 20–24. Optionally re-measure one FPS pair (vanilla and Iris) with the finer tessellation. Follow-ups: manway, vents and nozzles as parts, and the construction kit (already proposed). Detached material: `tools/custom-equipment-rendering-poc/` (`supports-check/`, `textures/`, `logs/supports-check-2026-10-16.tar.gz`). Before merge, the code-guide chapter "equipment" also covers parts, materials, texture classes and `equipment-2`.
