# equipment-geometry-estimate

Purpose: size the rendering and collision budget of parametric process equipment (drums, columns, reactors) placed on the block grid, before any code exists. Minecraft-free, one Java source file.

Batch: [2026-10-15-custom-equipment-rendering](../../documentation/2026-10-15-custom-equipment-rendering/) (report `CUSTOM_EQUIPMENT_RENDERING_REVIEW.md`).

## Model

- A vertical vessel: a regular N-gon prism (faceted cylinder, N = max(8, 4·ceil(πD/2)), so a facet is at most about 0.5 m wide) and two 2:1 ellipsoidal heads (depth D/4, N/8 latitude bands). 1 block = 1 m. The axis sits at the centre of a ceil(D) × ceil(D) footprint.
- Counts per case: blocks to place (shell + interior cells), shell cells (crossed by the surface), unique per-cell geometry keys (a body cell repeats at every height, so its key drops y), chunk-mesh quads after clipping every facet to its block cell (cell-projected UV, and arc-length UV with extra splits at each metre of circumference), the uncut mesh one renderer would submit every frame (facets × 1 m texture bands), and the per-cell collision box count after a greedy 3D merge of the solid at 1/16 and 1/8 block resolution (weighted mean, max).
- A horizontal vessel is the same geometry with the axes permuted; its counts are the same within grid-alignment noise.

## Run

```bash
java EquipmentGeometryEstimate.java
```

Any JDK 21+ (source-file launch). Runs in under a second. Output is a Markdown table.

## Result (2026-10-15, Zulu 25)

| case | D m | T/T m | facets | blocks | shell cells | unique keys | chunk quads (cell UV) | chunk quads (arc UV) | uncut quads (BER) | boxes/cell 1/16 mean, max | boxes/cell 1/8 mean, max |
|---|---|---|---|---|---|---|---|---|---|---|---|
| small drum | 2 | 3 | 16 | 16 | 16 | 12 | 128 | 152 | 112 | 38.0, 69 | 11.4, 18 |
| flash drum | 3 | 9 | 20 | 99 | 90 | 34 | 500 | 590 | 260 | 15.8, 86 | 6.2, 23 |
| reactor | 4 | 5 | 28 | 112 | 92 | 44 | 500 | 560 | 308 | 25.0, 74 | 8.0, 22 |
| column | 2 | 30 | 16 | 124 | 124 | 12 | 560 | 746 | 544 | 13.6, 69 | 5.8, 18 |
| large column | 6 | 40 | 40 | 1540 | 892 | 132 | 3240 | 3978 | 2000 | 10.0, 104 | 3.9, 28 |
| max column | 10 | 60 | 64 | 5664 | 2428 | 340 | 8680 | 10571 | 4864 | 9.9, 133 | 4.0, 36 |

No code was removed from a tracked path; nothing to re-attach.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Minecraft-free single-file Java estimator: blocks, shell cells, unique cell keys, chunk quads (cell and arc-length UV), per-frame BER quads and per-cell collision boxes of faceted vessels with 2:1 ellipsoidal heads

Batch: 2026-10-15-custom-equipment-rendering

Status: Concluded 2026-10-15. Index: [../INDEX.md](../INDEX.md).

### Entry points

README.md, EquipmentGeometryEstimate.java

### History

- Concluded 2026-10-15 (investigation; nothing detached from a tracked path)
