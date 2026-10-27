# skirt-shadow

Purpose: software renders and a skylight probe for the skirt shadow fix: what darkens a vertical vessel's skirt, before and after. Batch `documentation/2026-10-27-skirt-shadow/` (`SKIRT_SHADOW_REVIEW.md`, `figures/`).

Nothing here was detached from a tracked path: the probes ran from the worktree's scratch `tmp/skirt-shadow/` and were never on a tracked path, so no re-attach patch applies.

## Running

From the worktree root, without Gradle. The science tree imports nothing from Minecraft, so javac with the jars of any checkout's `build/moddev/clientLegacyClasspath.txt` (`;`-joined) plus `org.jetbrains:annotations:24.1.0` serves:

```
CP="$(cat tmp/skirt-shadow/cp.txt)"
find src/main/java/com/wormzjl/icheme/science -name '*.java' > tmp/skirt-shadow/science-sources.txt
javac -proc:none -encoding UTF-8 -cp "$CP" -d tmp/skirt-shadow/after-classes @tmp/skirt-shadow/science-sources.txt
javac -proc:none -encoding UTF-8 -cp "tmp/skirt-shadow/after-classes;$CP" -d tmp/skirt-shadow/after-probe tools/skirt-shadow/SkirtRender.java tools/multi-machine-framework/f3-nozzle-rows/NewSweep.java tools/multi-machine-framework/f3-nozzle-rows/RowSweep.java
java -cp "tmp/skirt-shadow/after-probe;tmp/skirt-shadow/after-classes;src/main/resources;$CP" com.wormzjl.icheme.science.equipment.SkirtRender out.png 60 low light drum:6:8 column:4:10:kettle
java tools/skirt-shadow/Stack.java sheet.png "Before" before.png "After" after.png
```

The "before" images ran the same lines on `995dbd9c`'s sources into `tmp/skirt-shadow/base-classes`.

## Entry points

- `SkirtRender.java` (adapted from `tools/column-sump/ColumnRender.java` of batch `2026-10-27-column-sump`): a vessel's real shell cell polygons, sprites, paint tint, the game's directional shade, culled back faces, a depth buffer, a pipe stub at every port. Args `out.png pxPerMetre view light|flat spec...`; view `front|back|left|right|iso|iso3|low`; spec `drum:D:TT[:skirt]`, `column:D:trays:config[:skirt]` (NewSweep's configs) or `unit:KIND[:size[:length]]`. With `light`, Minecraft's skylight over the cells (interior cells opaque, shell cells and air passing skylight down, -1 a step sideways), each quad lit by its own cell (vanilla flat lighting, the shell model has no ambient occlusion) through the vanilla noon lightmap at brightness 0.5, divided by full skylight's value. Prints the skylight of every cell holding the skirt's outer face and of the shell cells inside the skirt.
- `Stack.java` (copied from `tools/column-sump/Stack.java`): stacks sheets with a label over each.

## Results

- Every cell holding a skirt's outer face is at skylight 15, before and after, on a D 3 and a D 6 drum and a D 4 and a D 8 column. Shell cells inside the skirt (the bottom head's underside, the floor) sit at 12 to 14 (10 to 14 under the D 8 column); they show only through the slots and the port.
- Figures: `documentation/2026-10-27-skirt-shadow/figures/`.

## History

- 2026-10-27: written for the skirt shadow fix (owner request "Remove the shadow in drum/vessel skirt").
