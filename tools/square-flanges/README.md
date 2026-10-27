# Square flange profile renders

Purpose: render the actual `LineGeometry` spool-joint polygons before/after the square shoulder change.
Batch: `documentation/2026-10-23-square-flanges/`. Code: `acd0fd14`; baseline: `b3bb813e`.
Nothing here is production code, a Gradle gate, or detached from a tracked path.

From a worktree built with `gradlew.bat jar --max-workers=4`, using Java 21:

```powershell
$taskTool='D:/Minecraft/Modding/1.21/CreateChemE/tools/square-flanges'
New-Item -ItemType Directory -Force build/tmp/square-flanges/render,build/tmp/square-flanges/baseline | Out-Null
javac -cp build/classes/java/main -d build/tmp/square-flanges/render "$taskTool/FlangeRender.java"
javac -cp build/classes/java/main -d build/tmp/square-flanges/baseline "$taskTool/baseline/PipeSizes.java" "$taskTool/baseline/LineGeometry.java"
java '-Djava.awt.headless=true' -cp 'build/tmp/square-flanges/baseline;build/tmp/square-flanges/render;build/classes/java/main' FlangeRender before.png 'Before: tapered flange neck'
java '-Djava.awt.headless=true' -cp 'build/tmp/square-flanges/render;build/classes/java/main' FlangeRender after.png 'After: square flange shoulder'
```

`baseline/PipeSizes.java` and `baseline/LineGeometry.java` are exact sources from `git show b3bb813e:src/main/java/com/wormzjl/icheme/science/equipment/<Name>.java`.
The first classpath entry replaces those classes for the before render. Both runs use the actual mesh/lighting normals, no Minecraft client or simulation.
Logs are under `logs/`; the batch folder holds the before/after PNGs and owner screenshot.

The seam follow-up (`fa81579b`) adds an OPENING-material junction band; `renders/seam.png` in the batch is the resulting 335-polygon sample. No tests were run for that follow-up, per owner instruction; `flange-seam-pipe-gui-build.log` and `flange-seam-render.log` are archived with its target-preservation hashes.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Actual-mesh flange/neck/seam side-profile renderer and exact baseline sources; build/render/cancelled-test evidence

Batch: 2026-10-23-square-flanges

Status: In use since 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `FlangeRender.java`, `baseline/`, `logs/`

### History

- In use since 2026-10-23 (target `7bf50b7c`, tests waived by owner)
