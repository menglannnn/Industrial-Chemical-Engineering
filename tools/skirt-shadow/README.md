# skirt-shadow

Purpose: software renders with the in-game skylight and a skylight probe for the skirt shadow fix: the floor inside a vessel's skirt, seen through its slots and port, before and after. Batch `documentation/2026-10-27-skirt-shadow/` (`SKIRT_SHADOW_REVIEW.md`, `figures/`).

Nothing here was detached from a tracked path: the probes ran from the worktree's scratch `tmp/skirt-shadow/` and were never on a tracked path, so no re-attach patch applies.

## Running

From the worktree root, without Gradle. The science tree imports nothing from Minecraft, so javac with the jars of any checkout's `build/moddev/clientLegacyClasspath.txt` (`;`-joined) plus `org.jetbrains:annotations:24.1.0` serves:

```
CP="$(cat tmp/skirt-shadow/cp.txt)"
find src/main/java/com/wormzjl/icheme/science -name '*.java' > tmp/skirt-shadow/science-sources.txt
javac -proc:none -encoding UTF-8 -cp "$CP" -d tmp/skirt-shadow/sky-classes @tmp/skirt-shadow/science-sources.txt
javac -proc:none -encoding UTF-8 -cp "tmp/skirt-shadow/sky-classes;$CP" -d tmp/skirt-shadow/sky-probe tools/skirt-shadow/SkirtRender.java tools/multi-machine-framework/f3-nozzle-rows/NewSweep.java tools/multi-machine-framework/f3-nozzle-rows/RowSweep.java
java -Dcut=0.3 -cp "tmp/skirt-shadow/sky-probe;tmp/skirt-shadow/sky-classes;src/main/resources;$CP" com.wormzjl.icheme.science.equipment.SkirtRender out.png 32 iso light drum:3:6 drum:6:8 drum:10:12 column:8:40:full
java tools/skirt-shadow/Stack.java sheet.png "Before" before.png "After" after.png
```

The "before" images ran the same lines on `995dbd9c`'s sources (`tmp/skirt-shadow/base-classes`); there `ShapeRasterizer.skylight` does not exist and every interior cell is opaque.

## Entry points

- `SkirtRender.java` (adapted from `tools/column-sump/ColumnRender.java` of batch `2026-10-27-column-sump`): a vessel's real shell cell polygons, sprites, paint tint, the game's directional shade, culled back faces, a depth buffer, a pipe stub at every port. Args `out.png pxPerMetre view light|flat spec...`; view `front|back|left|right|iso|iso3|low`; spec `drum:D:TT[:skirt]`, `column:D:trays:config[:skirt]` (NewSweep's configs) or `unit:KIND[:size[:length]]`; `-Dcut=h` drops every polygon wholly above h m. With `light`, Minecraft's skylight over the cells (interior cells opaque unless `ShapeRasterizer.skylight`, shell cells and air passing skylight down, -1 a step sideways), each quad lit by its own cell (vanilla flat lighting, the shell model has no ambient occlusion) through the vanilla noon lightmap at brightness 0.5, divided by full skylight's value. Prints the skylight of every cell holding the skirt's outer face and of the shell cells inside the skirt.
- `Stack.java` (copied from `tools/column-sump/Stack.java`): stacks sheets with a label over each.

## Results

- Before: cells holding a skirt's outer face at skylight 15; shell cells inside the skirt 14 (D 3 drum), 12 to 13 (D 6), 9 to 14 (D 10), 10 to 14 (D 8 column): a stepped diamond on the floor. After: every one at 15.
- First reading (rejected by the owner): the skirt's darker paint. The outside of a skirt was already at skylight 15.
- Figure: `documentation/2026-10-27-skirt-shadow/figures/floor-before-after.png`.

## History

- 2026-10-27: written for the skirt shadow fix (owner request "Remove the shadow in drum/vessel skirt"; owner clarification: the bottom plane seen through the windows, worse at large diameters).
