# Equipment constructor, controller and screens

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` (uncommitted, on top of the proof of concept) · 2026-10-15 · Status: In progress since 2026-10-15.

Owner decisions (2026-10-15): the constructor is a **placed block** (the Schematicannon pattern), not a handheld item. The controller is a **role of one shell cell**: the lowest row, then west, then north (`ShapeRasterizer.Layout.controller()`); there is no `equipment_controller` block.

## Physical picture

A vessel is specified on a datasheet: diameter, tangent-to-tangent length and the head type. It is then erected at a fixed station. The constructor holds that datasheet and the erection point. Its figures are those of the smooth vessel:
- Volume: V = πD²/4·T/T + 2·πD³/24.
- Outside area: A = πD·T/T + 2·1.0840·D². The head factor 1.0840 is the area of half an oblate spheroid with eccentricity √3/2.

For D 3 m × T/T 6 m this gives V = 49.5 m³ and A = 76.1 m². The faceted shell lies inside the smooth one, so its volume and area are smaller; the test pins that.

## Built

- **Shape core.** `EquipmentShape.overallHeight/volume/surfaceArea/HEAD_AREA_FACTOR`. `VesselPlacement(forward 1–16, right ±16, up ±16)` sets the box origin from the constructor's facing. With the default offsets the box stands adjacent, on the constructor's level and centred on its line; an even width sits half a block to the left.
- **Constructor.** Block `equipment_constructor` (block item, loot table, Functional Blocks tab). `HORIZONTAL_FACING` points away from the player who placed it. `EquipmentConstructorBlockEntity` holds a `Design(shape, placement)` and syncs it to clients; it is validated on load.
- **Network: protocol `equipment-1`, one payload.** `ConstructorPayload` is sent when the screen closes (`build=false`) or on Build (`build=true`).
  - Admission: the player is within 8 blocks, may build, is not a spectator, and the level `mayInteract` at the constructor.
  - Validation: the records' constructors.
  - Placement: all-or-nothing `EquipmentStructure.place`, with the reply in chat.
  - No process state is involved, so the presentation rule does not apply. A build is a world edit, answered at once like placing blocks.
- **Screens.** `EquipmentConstructorScreen` is a left panel without blur and without pause. It has steppers (Shift = ×5), the derived datasheet, a status line, Build and Done. `VesselScreen` opens on use of any shell or interior cell. It shows the datasheet and the controller cell, read from synced cells, and requests nothing.
- **Ghost (`EquipmentGhost`).** It shows while the screen is open, and while the player looks at a constructor.
  - The shell is drawn with the plate sprite in `RenderType.translucent()` at `AFTER_PARTICLES`.
  - Blocked cells (occupied, unloaded, or outside the build height) are outlined red with Catnip's `Outliner.showCluster` and re-checked every 10 ticks.
  - There is no ghost where the same vessel already stands; the screen then says so.

## Checks

- Tests: `ShapeRasterizerTest` 8/8 (new: controller cell, datasheet bounds), `VesselPlacementTest` 2/2 (four facings, widths 1–10), `DeterministicMathSourceTest` 2/2.
- Dev client (bridge lane, vanilla pipeline), with screenshots in the batch folder:
  - `10`: the ghost on look. `11`–`12`: the screen with the live ghost at 2 × 4 m and 3 × 6 m; the datasheet values match the formulas above. `13`: one stone cell outlined red, "Blocked cells: 1".
  - `14`: Build refused, "Not built: Cell 200, -58, 1 is occupied by Stone". `15`: built, 72 cells (66 shell + 6 interior, as the screen predicted) from (199, −60, 1), centred and adjacent.
  - `16`: the vessel datasheet; its controller cell is local (0, 0, 0) of the 3 m vessel. `17`: the constructor reports that the vessel already stands there.

## Findings

1. **Bridge crash.** The bridge's `use_item` ran `startUseItem` on its HTTP thread. A screen the block opens on the client then rendered before its `init`, and the client crashed with a null font. The crash report is in `tools/custom-equipment-rendering-poc/logs/`. The fix is in the test lane, not the product: `src/mcpCompat` `UseItemMixin` runs the use path on the render thread (`executeBlocking`). In the game, the use path always runs there.
2. **Layout overflow.** At a 240-pixel GUI height, the status line overlapped the buttons. The layout is now laid out top-down from fixed rows, and the shots were retaken.
3. **Interior lookup.** An interior cell still finds its vessel by walking east to the shell, at most 11 steps. Storing a back-reference would need block entities in up to 3,236 interior cells of a 10 m × 60 m column.
4. **Building is free.** Nothing is consumed, and breaking drops nothing. The "kit" of decision D5 is a follow-up: plates and the cost from the outside area.
5. **Panel width.** The panel covers about 46 % of an 854×480 window at GUI scale 2, and about 30 % at 1080p at scale 3. The camera cannot turn while a screen is open; with the screen closed, the ghost on look shows the whole vessel.

**Before this batch merges:** the steps in `CUSTOM_EQUIPMENT_POC_REVIEW.md` still apply. In addition, the code-guide chapter "equipment" covers the constructor, the screens, the ghost and protocol `equipment-1`. `UseItemMixin` stays in `src/mcpCompat`, as part of the `runMcpClient` harness. Detached material: `tools/custom-equipment-rendering-poc/` (`gui-check/` command files, `logs/gui-check-2026-10-15.tar.gz`).

**Proposition for the owner.** Next tasks: supports and horizontal vessels (already proposed), then the construction cost and kit. The kit would consume plates in proportion to the outside area and return them on breaking.
