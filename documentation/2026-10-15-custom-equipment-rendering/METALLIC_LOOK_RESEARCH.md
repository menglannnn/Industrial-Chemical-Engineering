# Making equipment surfaces a bit more metallic: research

Date 2026-10-17. Batch `2026-10-15-custom-equipment-rendering`, worktree `custom-equipment-rendering-805e7d` at a118d219. This was a read-only study. No tracked file was changed.

## 1. Physical expectation
- Bare metal has almost no diffuse light. What you see is the reflected surroundings: narrow bright highlights next to dark reflections. The contrast is high, and the highlight takes the metal's colour. Brushed metal stretches its highlights across the brushing direction.
- Painted steel is a dielectric. The paint gives the diffuse colour and adds a weak white gloss (F0 about 4 %). Metallic paints such as RAL 9006 and 9007 add a fine sparkle and a sheen tinted by the paint.
- What reads as metal at a glance is a sharp light-dark profile across a curved surface, plus some streaks up close. Grain that only shows at texel scale does not do it.
- The atlas is mipmapped (vanilla default 4 levels), so 1 px features average out at a few blocks. Beyond about 10 blocks, the metal cue has to come from shading over the geometry, not from the sprite.

## 2. What the code does today
- `EquipmentShellModel.quad` (also used for pipeline devices; `FanAnimation` repeats the same code) bakes each quad with `setShade(false)` and `setHasAmbientOcclusion(false)`.
  - Vertex colour = 255 · `QuadLighter.calculateShade(n)` = 0.6 nx² + ((3+ny)/4) ny² + 0.8 nz², capped at 1.
  - Tint index = `Mesh.Material.channel()`. The `BlockColor` returns `Paint.tint(rgb)` = rgb · 255/236, capped at 255.
- Final pixel = texel × tint × vertex colour × lightmap, every factor at most 1, on every pipeline: vanilla `rendertype_solid`; Sodium 0.6.13 `BlockRenderer.colorizeQuad` (`ColorMixer.mulComponentWise(tint, quad.color(i))`, baked colour read in `MutableQuadViewImpl.fromVanilla`); Embeddium 21.1 `ModelQuadUtil.mixARGBColors(tint, quad.getColor(i))`. Bridge-lane shots 48–51 show the baked shade in vanilla.
- Walls range from 0.60 (east/west) to 0.80 (north/south) on a broad cos² profile. Tops are 1.00 and bottoms 0.50. There is no highlight, only a gentle gradient (shots 94 and 97 look matte).
- Measured sprites (Java, this study):

| Sprite | Mean | sd | Streak (column) sd | sd after 4×4 mip |
|---|---|---|---|---|
| painted_shell | 237 | 4.4 (1.9 %) | 4.0 | 3.5 (1.5 %) |
| painted_head | 237 | 3.5 | 3.0 | 2.5 |
| painted_cladding | 235 | 5.2 | 3.7 | 2.8 |

  The polish step's metallic streaks are near the ~2 % threshold below which a brightness difference goes unnoticed. That is why they do not show.
- Headroom: vertex colour 0.8 → 1.0 on north/south walls (+25 %), 0.6 → 1.0 on east/west; sprite only 236 → 255 (+8 %).
- Facets around: drums 4⌈0.75πD⌉ (D 3: 32, D 2: 20, at least 12); columns 4⌈πD⌉; pipes 4⌈0.75π·OD⌉, at least 8.

## 3. Options
### 1. Sprite design (vanilla, Sodium, Embeddium and Iris alike)
- **How:** strengthen the existing `metal()` streaks in `tools/custom-equipment-rendering-poc/textures/VesselTextures.java`.
  - Broad bands 4–8 px wide, so they survive mip level 2. Target a column sd of about 4 % (≈10 levels) instead of 1.7 %.
  - Skew the bands dark: a bright base around 245–250 with darker streaks down to about 205, rescaled to mean 236 as `Neutral.java` does. This avoids clipping at 255, so `Paint.SPRITE_VALUE` stays at 236.
  - Keep the streaks along v (the axis). Grazing views along a column then blur along the streaks, not across them.
- **What it looks like:** brushed or rolled plate up close; little change at distance.
- **Limits:**
  - A sprite cannot place a highlight. The shell's u runs around the circumference with a 4 m period, so a bright band in the sprite repeats at arbitrary angles and reads as a stripe pattern.
  - Edge wear and bare-metal scratches on painted sprites get tinted by the paint, so they read as dirt. Use them only on unpainted `equipment_*` sprites; flange rims could get a 1 px brighter edge.
  - A cool tint belongs only in unpainted sprites (pipe is already 150/155/160). In a painted sprite it would shift every RAL colour.
- **Cost:** none per frame, no extra memory (same sizes). About 15 changed lines in the generator, then regenerate.

### 2. Baked view-independent sheen in the vertex colour (all pipelines)
- **How:** in `quad()`, `s = min(1, calculateShade(n) + K·|nz|^P)`, applied to curved polygons only (vertex normals not all equal) of the body materials. Two lobes face north and south, where vanilla's flat shade is already brightest. Values with K = 0.2, P = 8:

| Wall azimuth from +z | 0° | 15° | 30° | 45° | 60–90° |
|---|---|---|---|---|---|
| today | 0.80 | 0.79 | 0.75 | 0.70 | 0.65–0.60 |
| sheen | **1.00** | **0.94** | 0.81 | 0.71 | unchanged |

  - From the south, a vertical vessel shows a centred highlight stripe about 40° wide. From the east it shows two bright rims on a darker core, the Fresnel-like edge cue. Diagonal views fall in between.
  - A drum lying along x gets a side band, 0.80 → 1.00, between its 1.00 top and darker lower half, which reads like a horizon reflection. A drum lying along z is unchanged.
  - Optional: add a weaker ±x lobe (K 0.1–0.18) so both orientations get one.
- **Facets:** shade is interpolated linearly between vertices. The worst error against the exact profile is 0.9 % at 32 facets, 2.4 % at 20 and 6 % at 12 (small drums).
  - Pipes (8 facets) would flicker with their phase. Leave PIPE out, or use P ≈ 3 for it.
  - P must be fixed per material. A P that varied per quad would break colour at cell-cut seams, because cut pieces of one facet must agree.
- **Tint:** colour is multiplicative, so the highlight is the paint at full brightness. That is correct for metal and for metallic paint (tinted specular). A white gloss highlight on coloured paint cannot be made this way; see 5b.
- **Ambient occlusion:** off for these quads. Iris's separate-AO path writes AO to alpha, so it does not interact.
- **Cost:** zero per frame and zero memory (the colour is already stored per vertex). Bake cost is one `pow` per vertex, negligible next to cutting. About 10–15 lines.
- **Shader packs:** under Iris the baked colour multiplies albedo (`XHFPTerrainVertex` writes `vertex.color × ao`), so a fixed stripe sits on top of the pack's own lighting. Today's baked shade already double-shades there (open item in `DRUM_OUTLET_SHADING_GUI_REVIEW.md`).
  - Proposed follow-up: when `IrisApi.getInstance().isShaderPackInUse()` (Iris API v0, reachable by reflection), bake neither shade nor sheen, and add the flag to the cache key (the D4 option of `CUSTOM_EQUIPMENT_RENDERING_REVIEW.md`).

### 3. LabPBR `_s` / `_n` maps (Iris shader packs only)
- **Loading:** Iris `AtlasPBRLoader` looks up `<ns>:textures/<sprite path>_s.png` and `_n.png` for every block-atlas sprite. That includes ours, for example `assets/createcheme/textures/block/painted_shell_s.png`.
  - A map of a different size is rescaled (nearest-neighbour for integer ratios, bilinear otherwise). Author it at the base size.
  - Animation metadata is honoured. OptiFine does not exist on NeoForge 1.21.1.
- **LabPBR 1.3 `_s` channels:**
  - R = perceptual smoothness; roughness = (1 − R/255)².
  - G = 0–229 linear F0. 230–237 are predefined metals (iron, gold, aluminium, chrome, copper, lead, platinum, silver), whose albedo tints the reflection. 255 means the albedo is the F0.
  - B = 0–64 porosity, 65–255 subsurface scattering.
  - A = 0–254 emission; 255 means none.
- **`_n` channels:** RG = normal in DirectX format (Y−), B = material AO, A = height (minimum 1).
- **Values to ship:**
  - painted_* (paint): R ≈ 115 (roughness 0.30), G 10 (F0 0.04), B 0, A 255; weld rows R ≈ 70.
  - equipment_cladding (aluminium jacketing): G 232, R ≈ 130.
  - grating (galvanised): G 255, R ≈ 70.
  - flange and bolt: G 230, R ≈ 60–90.
  - Optional `_n` for weld beads and brushing.
- **Faceting:** Iris builds terrain normals from each quad's face (`NormalHelper.computeFaceNormalManual` in `XHFPTerrainVertex`), so our smooth normals are lost under shaders.
  - A glossy map turns every 11° facet into a flat mirror. Keep R at 130 or less on curved shells, so the lobe spans about three facets.
  - A normal map cannot restore the curvature, because sprite texels do not line up with facets.
- **Reach:** Complementary Reimagined defaults to RP_MODE 1, "Integrated PBR+", where LabPBR maps are ignored. That mode keys materials by `block.properties`, which belongs to the pack. BSL needs its Advanced Materials option (from memory, off by default). Photon, Kappa and BSL support all of LabPBR 1.3; Complementary supports the metals, emission and POM. So only players who switch their pack to labPBR see the maps.
- **Cost:** nothing without Iris. With Iris and a pack, the first `_s` or `_n` in the block atlas makes Iris allocate a companion texture the full atlas size with all mips (`PBRAtlasTexture.upload` → `TextureUtil.prepareImage(atlasW, atlasH, mips)`): about 85 MiB per map type for a 4096² atlas, unless another pack already pays it. Proposed: ship the maps as a built-in optional resource pack (`AddPackFindersEvent`, off by default; about 20 lines plus PNGs from the generator).

### 4. Custom render type or shader in the chunk mesh (rejected)
- Chunk geometry goes only into the five chunk layers (`ChunkRenderTypeSet` → solid, cutout, translucent and the like) and is drawn by one shared terrain program: vanilla's core shader, Sodium's own chunk shaders, or the pack's `gbuffers_terrain` under Iris. There is no per-block shader slot.
- A core-shader override in a resource pack is global. Sodium ignores it and Iris replaces it.
- A matcap or environment map therefore needs a block-entity renderer or Flywheel material. That costs per-frame CPU resubmission: about 40 k quads per pass for the proof-of-concept scene, compared with 0 today. Iris also culls block entities in the shadow pass. Not acceptable for many vessels.

### 5. Other options
- **5a. Second translucent sheen shell:**
  - doubles the shell quads into the translucent layer (sorted by Sodium) and z-fights at distance;
  - Iris draws translucent geometry with the water/glass program, so it would look like glass.
  - Rejected.
- **5b. Paint baked into the vertex colour instead of the tint index:**
  - Vertex colour = lerp(paint·shade, white, sheen) gives a white gloss highlight on coloured paint (glossy paint rather than metal).
  - The paint joins the cell key (more unique bakes in the 4096-entry cache); about 40 lines.
  - Not needed for "metallic".
- **5c. Emissive specks** (full-bright vertices, or the LabPBR alpha channel) glow like LEDs at night and are wrong for metal. Rejected.
- **5d. Filtering:** NeoForge, Sodium and Iris add no anisotropic filtering. Design for mip levels 1–2: features at least 4 px, streaks along the direction you usually see at a grazing angle.
- **5e. Stronger version of option 2 (later, if wanted):** `0.85·shade + 0.30|nz|^8 + 0.18|nx|^8`. Tops drop to 0.85 and every wall gets a stripe (east/west 0.69). More chrome-like, but darker than neighbouring blocks.

## 4. Ranking and recommendation
1. **Vanilla, Sodium and Embeddium: option 2 (baked sheen), the smallest change with a visible result.** It gives +25 % on the north/south wall band, a real highlight profile on every curved body, no frame cost, and about 15 lines in one method. Combine it with option 1 for close-up streaks.
2. **Option 1 (sprite contrast to about 4 % in broad, dark-skewed streaks).** Close-up only; it does not create a highlight by itself.
3. **With shader packs:** first stop baking shade and sheen when a pack is active (Iris gate). Then optionally add LabPBR `_s` maps (smoothness ≤ 130, F0 10 for paint) in an opt-in built-in resource pack. Expect a modest gain, seen only in labPBR mode.
4. Rejected: custom shaders, block-entity renderer overlays, a translucent sheen layer, emissive specks.

## 5. Quick A/B prototype
- **`client/model/EquipmentShellModel.java`, `quad()`:**
  - Add `static final float SHEEN_K = 0.2F; static final int SHEEN_P = 8;`
  - Add a set `SHEEN = EnumSet.of(SHELL, HEAD, CLADDING, HEATER, AC_HEADER)`.
  - Add a per-polygon `curved` flag (any vertex normal differs from the first by more than 1e-4).
  - Replace the shade line with `float s = calculateShade(...); if (sheen && curved) s = Math.min(1F, s + SHEEN_K * (float) Math.pow(Math.abs(nz), SHEEN_P));`
  - Pass `polygon` (already in scope) for the material.
  - The A/B switch is a system property or a client config boolean read at bake time. F3+T rebuilds the model and with it the quad cache.
- **`tools/custom-equipment-rendering-poc/textures/VesselTextures.java`, `metal()`:**
  - Broad streak 34 → 85, fine 12 → 18, skewed dark.
  - Renormalise each painted sprite to mean 236, then rerun the generator into `src/main/resources/assets/createcheme/textures/block/`.
  - Measure with a column-sd check: target about 10 levels, and at least 2.5 % after 4×4.
- **Check:** shots of the same scene (`drum-check/scene.txt` or `details-check/`) from the south and the east, with the sheen off and on, painted RAL 9006, 3000 and 7016, on the bridge lane. No timing run is needed.

## Sources
- Code and docs: `EquipmentShellModel`, `EquipmentClient`, `Paint`, `Mesh`, `EquipmentShape.facets()`, `PipeSizes.sides()`, `VesselTextures`, `Neutral`; `EQUIPMENT_POLISH_PLAN/REVIEW.md`, `CUSTOM_EQUIPMENT_RENDERING_REVIEW.md`, `CUSTOM_EQUIPMENT_POC_REVIEW.md`, `DRUM_OUTLET_SHADING_GUI_REVIEW.md`.
- NeoForge 1.21.1 `QuadLighter` (github.com/neoforged/NeoForge, branch 1.21.1); Sodium `BlockRenderer`, `MutableQuadViewImpl` (github.com/CaffeineMC/sodium, tag mc1.21.1-0.6.13); Embeddium `BlockRenderer` (github.com/FiniteReality/embeddium, branch 21.1/neoforge).
- Iris `AtlasPBRLoader`, `PBRType`, `PBRAtlasTexture`, `XHFPTerrainVertex`, `IrisApi` (github.com/IrisShaders/Iris, branch 1.21.1, common/src).
- LabPBR 1.3: https://shaderlabs.org/wiki/LabPBR_Material_Standard; supported packs: https://shaderlabs.org/wiki/LabPBR_Supported_Packs; Complementary Reimagined `shaders/lib/common.glsl` (`#define RP_MODE 1`) and `lang/en_US.lang` (github.com/ComplementaryDevelopment/ComplementaryReimagined).
