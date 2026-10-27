# Tank level-gauge enclosure review

Status: In progress since 2026-10-25; delivered `db936513` on `codex/tank-gauge-enclosure`. Base `e5591c04` includes the pending bubble-direction fix. No integration/main merge or version bump.

Expected: an enclosed, readable liquid column with clear glass and top/bottom connection pipes to the tank. The gauge remains beside the front ports with neutral styling and the actual liquid colours.

Previous drawing: rails and liquid bands had no glass mesh. Thin square taps were behind the scale and terminated short of the visible column, weakening the connection to the tank.

Implemented:
- Four transparent sleeve faces at half-width .123 m, between the .120 m liquid and .125 m hollow-marker opening. Existing rails/cap radius and marker clearances remain. End manifolds are .12 m high; .09 m round equalizing lines enter their midplanes, with shell sockets/union fittings. Vent, drain and intermediate supports remain.
- Custom `gauge_glass.png`: 64x256 RGBA, neutral clear centre and restrained reflection strokes, alpha2..55. An editable Java2D source graphic lives in the tool folder. One full sprite per pane avoids metre-tiled borders. The equipment model references the custom asset; no Minecraft glass texture is used.
- `EquipmentShellModel` caches solid/glass/all quads for still/moving states. Only cells with glass request the translucent pass. `TankRenderer` draws dynamic liquid/collar at AFTER_BLOCK_ENTITIES, before terrain glass writes depth. No per-frame geometry baking.
- Static chamber extends .3 m above fill height as before; dynamic liquid stops at fill. Tank ports/datum/volume, process scheduling, subscriptions and saved formats are unchanged. Native refresh remains the layout mechanism.

Validation: 8 focused checks pass (three ShapeRasterizer methods, five EquipmentRefresh tests), no failures/errors/skips; final package passes. The added single test checks native clipped pane area/extent/winding/clearance and pipe continuity on minimum/maximum tanks across all headings. Initial test assertion was corrected to distinguish static headspace from dynamic fill height; retained evidence is `test-correction-before.*`.

Twelve native software views cover filled/empty gauges, oblique/low viewpoints and top/bottom connections across small/medium/maximum sizes. Final custom-texture overview inspected; no cyan repetitions or new overlapping glass caps. Software shading/translucency approximates game rendering. No game/world check or full-suite pass is claimed.

Tooling: `tools/tank-gauge-enclosure/` contains native renderer, custom texture source, runner, sheets and compressed logs/XML. No tracked probe/task/gate exclusion was introduced/detached. Code guide README/equipment/client-gui/verification updated from implemented source.

Integration update2026-10-25: merged as8b2112cf into claude/floating-roof-tanks-81d68b with latestfffbd514 equipment/ladder work preserved. Full quick1419/0, slow187/0 andpackage pass; target clean. Main pending; no version bump/client launch. See tank-gauge-enclosure/MERGE_REVIEW.md.
