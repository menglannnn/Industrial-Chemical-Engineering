# Custom equipment rendering: feasibility review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-15 · Status: D1–D5 accepted as proposed by the owner (2026-10-15); proof of concept built and measured, see `CUSTOM_EQUIPMENT_POC_REVIEW.md`.

## 1. Question and answer

Owner request (2026-10-15): a constructor builds equipment in the world (drums, columns, reactors). The player sets parameters such as diameter and height. A 3D model with a matching hitbox appears, like a scalable multiblock. Can this rendering be FPS-friendly and compatible with the common optimization mods?

Answer: yes, if the shell is treated as terrain. Each block cell of the equipment carries a small geometry key. A procedural baked model turns that key into the quads of the shell inside that cell, and the chunk mesher (vanilla, Sodium 0.6 and 0.8, Embeddium, Iris) bakes them like any block. The shell then costs nothing per frame. Immersive Engineering draws its 1.21.1 multiblocks this way, and Create's fluid tank bakes its shell into the chunk mesh and redraws only the fluid each frame. Only parts that move or show process state use a per-frame renderer, on one block per equipment.

## 2. Physical picture

- Real vessels are surfaces of revolution: a cylindrical shell and two heads (2:1 ellipsoidal, hemispherical, flat or conical). Typical sizes: reflux drum D 1–3 m, L/D 3–5, horizontal; flash drum D 1–4 m, vertical; stirred reactor D 1–4 m, H/D 1–1.5; column D 0.5–10 m, height 10–60 m. At 1 block = 1 m this is 1×1×2 to 10×10×65 blocks. The tallest spans 5 chunk sections; the widest spans up to 2×2 chunks.
- The shape changes only when the player builds, edits or breaks the equipment. During play only the process state changes (level, temperature, phases), and the engine publishes it about every 5 s.
- Expectation: the shell geometry should cost what terrain costs (built once, drawn by the chunk renderer). Only the process overlay is dynamic, and it changes at about 0.2 Hz.
- One form covers every shape: an axis, a polyline profile r(s) along it (cylinder, cone, head bands) and an N-gon cross-section. Nozzles, legs and skirts are small profiles of the same form. Horizontal vessels are the same profile with the axes permuted.

## 3. What exists (verified in the pinned jars)

- CreateChemE: JSON block models only. No block-entity renderer (BER), no Flywheel visual, no model loader; `CreateChemEClient` registers screens only. Fluid devices are single blocks with `noOcclusion()`.
- Parameters the equipment would later feed (out of scope here): reservoir volume (`reservoirVolumeCubicMetres`, default 1 m³, `FluidDeviceSpec.volume`), reservoir elevation in the network graph, and the V3 column input `columnDiameterMetres`.
- Create 6.0.10 precedent, the fluid tank: one block per cell, each with a block entity pointing to a controller. The shell is `FluidTankModel extends CTModel`: connected textures computed in `getModelData(level, pos, state, data)` and baked into chunk meshes. Only the fluid is drawn per frame, by `FluidTankRenderer` on the controller (`createRenderBoundingBox` spans the tank, `shouldRenderOffScreen` is true). Copycat blocks read their block entity the same way (`CopycatModel.gatherModelData`).
- NeoForge 21.1.219: `IBakedModelExtension.getModelData(BlockAndTintGetter, BlockPos, BlockState, ModelData)`, `getRenderTypes(state, rand, data)` returning a `ChunkRenderTypeSet`, `IDynamicBakedModel`, `BakedQuad(..., shade, hasAmbientOcclusion)`, `AddSectionGeometryEvent`, `RenderHighlightEvent`, `RenderLevelStageEvent`, `IGeometryLoader`, `BlockBehaviour.Properties.dynamicShape()`, BER `getRenderBoundingBox` and `shouldRenderOffScreen`.
- Flywheel 1.0.6 (bytecode of `Backends`): the instancing and indirect backends report `isSupported() == false` while `ShadersModHelper.isShaderPackInUse()`. With a shader pack Flywheel runs the off backend, so every Flywheel visual needs a BER fallback (Create's pattern).
- Vanilla lighting (`ModelBlockRenderer.renderModelFaceFlat`): diffuse shade is `level.getShade(quad.getDirection(), shade)`, so a curved surface is shaded in at most six steps. A quad inside the cell takes the light of its own cell. NeoForge's `QuadLighter.calculateShade` uses the vertex normal (smooth on curves), but that pipeline is off by default (`experimentalForgeLightPipelineEnabled = false`).

## 4. Budget, from geometry (tools/equipment-geometry-estimate)

Vertical vessel: N-gon shell with facets of at most about 0.5 m, 2:1 ellipsoidal heads, axis centred on the block grid.

| case | D × T/T m | blocks | shell cells | unique keys | chunk quads (arc UV) | BER quads every frame | collision boxes/cell at 1/8 (mean, max) |
|---|---|---|---|---|---|---|---|
| small drum | 2 × 3 | 16 | 16 | 12 | 152 | 112 | 11.4, 18 |
| flash drum | 3 × 9 | 99 | 90 | 34 | 590 | 260 | 6.2, 23 |
| reactor | 4 × 5 | 112 | 92 | 44 | 560 | 308 | 8.0, 22 |
| column | 2 × 30 | 124 | 124 | 12 | 746 | 544 | 5.8, 18 |
| large column | 6 × 40 | 1,540 | 892 | 132 | 3,978 | 2,000 | 3.9, 28 |
| max column | 10 × 60 | 5,664 | 2,428 | 340 | 10,571 | 4,864 | 4.0, 36 |

- Clipping facets to block cells multiplies the quad count by 1.4–2.3 against one uncut mesh. In a chunk mesh this is paid once per rebuild. The largest column is about 2.1 k quads per 16 m of height; for scale, a 5-block-thick fancy-leaves canopy over one section is about 7.7 k quads (1,280 leaf blocks × 6 faces).
- The same column through a BER resubmits 19.5 k vertices every frame on the render thread, and again in a shader shadow pass. Ten such columns put about 200 k vertices per frame on the CPU. That is the cost this design avoids.
- Unique keys are few (12–340) because a body cell repeats at every height, so a quad cache keyed by the normalized cell key stays at a few hundred kB.
- Shell cells that carry a key: 16 to 2,428 per equipment. Create's largest default fluid tank (width capped at 3, height 32) has 288 block entities, so the 10 m column carries about eight times that. These block entities never tick, so they cost memory and chunk-send bytes, not frame time. If the proof of concept shows that cost matters, route A2 removes them (D1).
- Collision at 1/16 block costs 10–38 boxes per shell cell (max 133); at 1/8 it is 4–11 (max 36). Use 1/8. Draw the selection outline as the equipment silhouette, not as per-cell boxes.
- Arc-length texture mapping (seamless plates around the circumference) adds 12–33 % quads over cell-projected mapping. Take it.

## 5. Routes compared

Every route needs blocks in the cells for the hitbox, because collision is per block. The routes differ only in how the shell is drawn.

| Route | Cost per frame | Shader packs (Iris) | Sodium / Embeddium | Weak point | Verdict |
|---|---|---|---|---|---|
| A1 Per-cell procedural model in the chunk mesh (Immersive Engineering split models, Create connected textures) | none | drawn as terrain: shadows, block id per state | all hooks called (model data, render types, per-quad AO) | one small block entity per shell cell | **proposed** |
| A2 Controller adds section geometry (`AddSectionGeometryEvent`) | none | injected quads may carry a stale block id (unconfirmed) | supported; Sodium skips all-air sections | client spatial index, chunk-watch sync, explicit section rebuilds | fallback |
| B One BER draws the whole shell | full mesh every frame, again in the shadow pass | shaded through vanilla render types | needs `shouldRenderOffScreen`; then drawn every frame with no frustum test | CPU cost grows with every vessel in view | overlay only |
| C Flywheel visual | near zero (GPU instancing) | backend off with any pack, so it falls back to B | supported | unique meshes gain nothing from instancing | moving parts only |
| D Entity with a large box | as B, plus entity tracking | shaded | works | one axis-aligned box, no cylinder hitbox | rejected |

## 6. Recommended design

Each layer owns one job, so a later shape (box housing, nozzle, cone bottom) is a new profile, not a new renderer.

1. **Shape core, Minecraft-free and unit-tested** (a new package next to `science/`). `EquipmentShape`: axis, origin cell, profile knots (s, r), facet count, with validated bounds (for example D 0.5–10 m, height ≤ 64 m, a block cap). `ShapeRasterizer` classifies cells as SHELL or INTERIOR. For each shell cell it returns a normalized `CellKey` (the axial coordinate is dropped where the profile is constant), the cell's surface polygons (cell-local positions, normals, arc-length UV) and its solid at 1/8 block. Tests: the polygons close the surface (area equals the analytic faceted area), solid volume equals the analytic volume, symmetry, determinism, and equal keys give equal geometry.
2. **Blocks.** `equipment_controller`: one per equipment; its block entity holds the shape and, later, the process identity; it owns the GUI. `equipment_shell`: a non-ticking block entity with the shape (interned on the client) and the cell offset; `noOcclusion()`, `dynamicShape()`, transparent to light, because a quad inside a cell takes that cell's light (a vessel of 2 m or less therefore has no interior cells and casts no block shadow, like glass). `equipment_interior`: no block entity, `RenderShape.INVISIBLE`, full cube, opaque; it keeps mobs, fluids and placed blocks out of the vessel. All are in `create:non_movable` with push reaction BLOCK. Using any cell opens the controller GUI.
3. **Static shell mesh.** `EquipmentShellModel implements IDynamicBakedModel`, loaded by an `IGeometryLoader` named in the shell blockstate JSON (no baking-result swap, which suits ModernFix's lazy model loading). `getModelData` reads the cell's block entity; `getQuads(..., side = null, data, renderType)` returns the cached quads of the key. Curved quads have no cull face, carry vertex normals and set `hasAmbientOcclusion = false`. Every quad stays inside its cell, which keeps it within Sodium's section culling box. The cache is concurrent (meshing runs on worker threads), bounded, and cleared on resource reload because quads embed atlas UVs. Sprites come from the block atlas, so resource packs and PBR packs can retexture the plates. The 3-argument `getQuads` returns nothing and the particle sprite is the plate texture, which gives Distant Horizons its LOD colour.
4. **Hitbox and selection.** `getShape`, `getCollisionShape` and `getInteractionShape` return the cell's 1/8 solid, cached per key. A `RenderHighlightEvent.Block` handler replaces the per-cell outline with the equipment silhouette (end rings and four meridians), built once per equipment on the client.
5. **Dynamic overlay, only if decision D2 allows it.** A controller BER draws the level in a sight glass or a cut-away, a few tens of quads, with `getRenderBoundingBox` spanning the equipment and `shouldRenderOffScreen` true (Mekanism's multiblock pattern). Sodium then draws it every frame without a frustum test, so it stays small and returns early beyond a set distance. It uses vanilla render types only, so Iris shades it. Rotating parts (agitator, motor) use a Flywheel visual with the BER as fallback, which is Create's kinetic pattern.
6. **Constructor preview.** Client only. While the player edits the parameters, the same shape core draws a translucent ghost and marks blocked cells (Catnip `Outliner`, shipped with Ponder). The ghost is rebuilt only when a parameter changes.
7. **Construction.** The server validates the space (every cell replaceable, loaded and inside the world height) and places the cells. A partly built vessel renders correctly, because each cell draws only its own part. A vessel across a chunk border needs no cross-chunk lookup for the same reason.

## 7. Compatibility matrix

For the proposed design (A1 shell, optional overlay BER). Facts come from source at the cited tags (research agent, 2026-10-15). "Unconfirmed" marks an inference.

| Mod (NeoForge 1.21.1) | Static shell (A1) | Overlay BER / Flywheel | Constraint on the design |
|---|---|---|---|
| Vanilla NeoForge | `SectionCompiler` calls `getModelData`, `getRenderTypes` and the 5-argument `getQuads` | default render box is one block; `AABB.INFINITE` allowed | curved quads shaded in six steps unless the NeoForge light pipeline is on |
| Sodium 0.6.13 and 0.8.13 backport | `NeoForgeModelAccess` calls all three hooks; per-quad AO flag honoured; curved quads shaded by their face normal (smooth lighting uses vertex normals) | BEs are culled by section visibility only, never by render box; `shouldRenderOffScreen` puts the overlay in the global list, drawn every frame | vertices must lie within −8…+24 blocks of the section origin, and a section's culling box is only 1 block larger than the section, so every quad stays inside its cell |
| Embeddium 21.1 (no commit since 2025-02-07) | same three hooks | as vanilla | assumed as Sodium's (same lineage; vertex range and culling box not checked separately) |
| Iris 1.8.12 / 1.8.14-beta | drawn with the terrain program, in shadows, block id per state (`block.properties` can name `createcheme:` blocks) | BER output through vanilla render types is shaded and in the shadow pass | never a custom `ShaderInstance` or a vanilla-format VBO; with an entity shadow-distance multiplier the shadow pass culls BEs by a ±1 box (overlay shadow may drop) |
| Flywheel 1.0.6 | not used | both GPU backends off with a shader pack; `supportsVisualization` false | every visual needs the BER fallback (Create's `renderSafe` guard); third-party bridges exist but are not required |
| Entity Culling | not affected (chunk geometry) | box = `getRenderBoundingBox`; `shouldRenderOffScreen` BEs are never culled before 1.21.9 | none |
| Distant Horizons | LOD colour comes from the 3-argument `getQuads` (no model data), falling back to the particle sprite | absent from LODs | particle sprite of shell and interior blocks = plate texture; the interior blocks give the solid LOD voxel |
| ModernFix | dynamic resources off by default; an `IGeometryLoader` model takes the normal lazy path | — | no bulk iteration in `ModifyBakingResult` (not used by this design) |
| Nvidium | Fabric/Quilt only, no NeoForge build | — | not applicable |

Main sources: Sodium `neoforge/.../model/NeoForgeModelAccess.java`, `CompactChunkVertex.java`, `OcclusionCuller.java`, `SodiumWorldRenderer.java` and `ChunkBuilderMeshingTask.java` at `mc1.21.1-0.6.13` (same in 0.8.13); Embeddium `21.1` `ChunkBuilderMeshingTask.java`; Iris `1.21.1` `MixinGameRenderer.java`, `ShadowRenderer.java`, `MixinShaderInstance.java`; Flywheel `1.21.1/dev` `Backends.java` and issue #252; Entity Culling `CullTask.java`, `BlockEntityRenderDispatcherMixin.java`; Distant Horizons `ClientBlockStateColorCache.java`; Immersive Engineering `1.21.1` `AbstractSplitModel.java`; Mekanism `1.21.x` `MultiblockTileEntityRenderer.java`.

## 8. Owner decisions

- **D1 Route.** Proposed: A1, per-cell procedural model in the chunk mesh, as in section 6. Alternative: A2, section geometry from the controller, which removes the per-cell block entities but adds a client spatial index, a chunk-watch sync and explicit section rebuilds (see section 5 for its mod support).
- **D2 Process state in the world.** Proposed for the first stage: none. The shell is static and every process value stays in the GUI, which fits the presentation rule as written. Option: a visual subscription, where a client within a set distance of an equipment with a sight glass receives a few numbers (phase level fractions, colours) on the engine's 5 s schedule. That needs an amendment to "send GUI/process presentation data only in response to a client request".
- **D3 Look.** Proposed: smooth, facets of at most about 0.5 m (the table above). Options: low-poly 8/12/16-gon by diameter (about half the quads, wider facets make the shading steps less visible), or block-stepped like Create's square tanks (no curved geometry, connected textures only, the simplest form).
- **D4 Shading of curves.** Sodium and Embeddium shade a curved quad by its normal; Iris does its own lighting. Only plain vanilla, without the NeoForge light pipeline, shades in six steps and shows bands at 45°. Proposed: accept that for vanilla. Option: bake a normal-based shade into the vertex colour when neither Sodium nor a shader pack is active (the flag joins the cache key); this adds a code path for the one setup that does not seek FPS.
- **D5 Size grid and breaking.** Proposed: diameters in 0.5 m steps, heights in whole metres; breaking any cell deconstructs the whole equipment and drops its kit, so there is no partly broken state to simulate.

## 9. Next step

A proof of concept after the owner decides D1–D5: the shape core with its tests, the three blocks, the shell model and the 1/8 collision for one vertical vessel with 2:1 heads, placed by a debug command instead of the constructor. Check it in the dev client through the MCP bridge: appearance, hitbox, partial build, a vessel across a chunk border. Then one FPS pair (60 s warm-up, 60 s measurement) of 20 large columns against the empty scene, with Sodium and with Sodium + Iris added to the dev runtime only for that run. The constructor GUI, the overlay and the process coupling (volume, elevation, column diameter) follow as their own tasks.

Done 2026-10-15: results, deviations and the next tasks are in `CUSTOM_EQUIPMENT_POC_REVIEW.md`.

Code guide: a chapter is due when the batch merges (see the PoC review). Detached material: `tools/equipment-geometry-estimate/`, `tools/custom-equipment-rendering-poc/`.
