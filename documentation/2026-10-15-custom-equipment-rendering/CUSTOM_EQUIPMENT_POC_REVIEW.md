# Custom equipment rendering: proof of concept

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` (uncommitted) · 2026-10-15 · Status: In progress since 2026-10-15. Owner decisions D1–D5 accepted as proposed in `CUSTOM_EQUIPMENT_RENDERING_REVIEW.md` §8.

## Expectation

The shell is terrain: 20 columns of 6 m × 40 m are 20 × 3,978 ≈ 80 k quads, baked once into chunk meshes. Per frame the CPU does nothing new. The GPU draws 80 k more quads, which is negligible for vanilla and Sodium. Under a shader pack it draws them twice (G-buffer and shadow pass) and lights the pixels they cover, so a small GPU cost is expected there and nowhere else.

## Built (≈1,300 lines, nothing committed)

- `science/equipment/EquipmentShape` (D 0.5–10 m in 0.5 m steps, T/T 1–64 whole metres, 2:1 heads) and `ShapeRasterizer` (cell layout, normalized `CellKey`, cell-clipped polygons with arc-length UV and smooth normals, 1/8 solid as merged boxes). `DeterministicMath.sin` was added; the shape is computed with StrictMath, so client and server agree.
- Blocks `equipment_shell` (block entity with shape and cell, `dynamicShape`, no occlusion, collision cached per key) and `equipment_interior` (invisible opaque cube); `EquipmentStructure` handles placing, finding and dismantling (D5), plus the debug command `/createcheme equipment place <bottom centre> <D> <T/T> [rows]`.
- Client: `EquipmentShellModel` (`IDynamicBakedModel` through the `createcheme:equipment_shell` geometry loader, quad cache per key) and the silhouette outline in `EquipmentClient` (`RenderHighlightEvent.Block`).
- Tests: `ShapeRasterizerTest`, 6/6. It checks that the surface area equals the faceted area and that every piece stays in its cell and faces outwards. A body key repeats at every height, the interior is sealed, and the solid matches the faceted volume. `DeterministicMathSourceTest` passes 2/2. The production rasterizer reproduces the estimate table in §4 of the feasibility review exactly; a layout takes 0.4–3 ms.

## Dev-client checks (bridge lane, vanilla pipeline)

- Appearance (`screenshots/02`): smooth shell, seamless plates, heads closed.
- Partial build: with `rows=5` the vessel ends flat at the fifth row (`04`).
- A vessel across a chunk corner is seamless (`05`). The outline is the whole-vessel silhouette (`06`).
- Collision: a player dropped on a 3 m × 6 m vessel rests at y = −52.5, the apex of its upper head (−60 + 6 + 1.5).
- D5: breaking one shell cell, or one interior cell, removes every cell of the vessel (checked cell by cell with `execute if block`).

## FPS pair: 20 columns 6 × 40 against the empty scene (854×480, RTX 4070 Ti, uncapped, 60 s warm-up + 60 s)

| lane | columns FPS | empty FPS | Δ mean | p50 ms (col / empty) | p99 ms (col / empty) |
|---|---|---|---|---|---|
| vanilla pipeline | 2,412 | 2,364 | +2.0 % | 0.37 / 0.37 | 0.94 / 1.09 |
| Sodium 0.8.12 | 3,790 | 3,639 | +4.2 % | 0.24 / 0.25 | 0.55 / 0.58 |
| Sodium + Iris 1.8.14-beta.1, Complementary Reimagined r5.7.1 | 1,309 | 1,419 | −7.8 % | 0.72 / 0.66 | 1.42 / 1.30 |

- Vanilla and Sodium: no cost. In both lanes the columns ran slightly faster. That bounds what one pair resolves, at about ±4 %.
- Iris: +0.06 ms per frame. That is 0.4 % of a 60 FPS frame and 0.9 % of a 144 FPS frame. It is the GPU price of 80 k quads in the G-buffer and shadow passes, which any terrain of that size pays. A block-entity renderer would add, on top of it, CPU resubmission of about 40 k quads in each of the two passes. The 64–94 ms maxima occur in both scenes, so they come from the pack.
- Under Iris the vessels go through the terrain program and cast shadows onto the ground (`09`). Under Sodium they are shaded smoothly by their normals (`08`); vanilla shows the six-step bands accepted in D4 (`07`).

## Findings

1. With Sodium present, the bridge's `enter_control_mode` crashes the client: `EXCEPTION_ACCESS_VIOLATION` in `glfw.dll`, from `glfwSetInputMode`. The crash log is in `tools/custom-equipment-rendering-poc/`. The Sodium and Iris lanes therefore ran unattended, through an autopilot in the measurement-only `FrameTimeProbe` (`src/mcpCompat`). The autopilot opens the save, sends the scene commands, measures, screenshots and quits.
2. Supports. A vertical vessel on the ground touches it only at the apex of its lower head; real vessels stand on a skirt or legs, horizontal ones on saddles. This is a profile primitive for the shape core.
3. Deviation from §6.2 of the feasibility review: there is no `equipment_controller` block yet. The shell block entity holds the shape, and an interior cell finds its vessel by walking east to the shell. The controller comes with the constructor GUI.

## Before this batch merges

- Detach `FrameTimeProbe` (source plus a patch) and the lane scripts to `tools/custom-equipment-rendering-poc/`, then re-run the equipment and determinism tests. Add a code-guide chapter "equipment" with a README row, the `CHANGELOG.md` version entry and a minor `mod_version` bump.

## Proposition for the owner

Next tasks, each on its own: (a) a constructor block with its GUI, which also creates the controller block; (b) support primitives (skirt, legs, saddles); (c) horizontal vessels, which use the same profile with the axes permuted; (d) a report of the bridge/Sodium crash to langyo/minecraft-mod-mcp, which needs your OK because it is an outward post.

Detached material: `tools/custom-equipment-rendering-poc/` (bridge client, scene command files, lane logs, crash log, `LayoutCounts`), `tools/equipment-geometry-estimate/`. Code guide: chapter due at merge.
