# Pipeline sight-glass software renderer

Batch: `documentation/2026-10-23-pipeline-sight-glass/`. Product source: branch `codex/pipeline-sight-glass`, code `465f8af8`, revised model/straight-only rule `e1bffe54`, player-facing installation `065683c6`.

`SightGlassRender.java` renders production `SightGlassGeometry` polygons and calls production `SightGlassDisplay.turbid`; base liquid colours are examples. It does not load Minecraft, run solvers, open a client or run tests. Flat Java2D shading is evidence of shape/scale/apertures, not a claim about Minecraft textures/shaders. Corners/junctions deliberately fall back to ordinary pipe geometry.

This tool was never tracked or detached from product/build source. No removing commit or re-attach patch is applicable. There are no temporary instrumentation or Gradle switches in the product.

From the owned feature worktree in PowerShell, using JDK 21:

```powershell
.\gradlew.bat jar --max-workers=4
$sightJava='C:\Program Files\Java\jdk-21.0.11\bin'
& "$sightJava\javac.exe" -cp build/classes/java/main -d build/tmp/sight-glass-render 'D:\Minecraft\Modding\1.21\CreateChemE\tools\pipeline-sight-glass\SightGlassRender.java'
& "$sightJava\java.exe" '-Djava.awt.headless=true' -cp 'build/tmp/sight-glass-render;build/classes/java/main' com.wormzjl.icheme.runtime.fluid.SightGlassRender 'D:\Minecraft\Modding\1.21\CreateChemE\documentation\2026-10-23-pipeline-sight-glass\renders'
```

Outputs: `preview.png` (revised fitting), `views.png` (clear/liquid/turbid in three straight axes), `sizes.png` (0.001/0.05/0.25/0.5/1 m bores), `details.png` (reducer, ordinary corners/junctions, item, gas/solids and depth). At the smallest bores the existing 1/16 m drawn-OD minimum is intentional.

`orientations.png` calls production `SightGlassGeometry.towards` and renders its selected orientation for all three run axes, each permitted viewing axis and opposite player sides. This sheet includes top/bottom windows on horizontal pipes. It requires the composite `SightGlassGeometry.Key` API at `065683c6`; logs are in `logs/orientation-logs-2026-10-23.tar.gz`.

Logs are compressed in `logs/sight-glass-logs-2026-10-23.tar.gz`: compilation/package output and render counts/colour samples, including earlier model iterations. The final files are `sight-glass-build-final.log` and `sight-glass-render.log`. No tests were run, by owner instruction.

Owner-requested client follow-up: `old-world-crash-logs-2026-10-23.tar.gz` records rejection of an existing checkpoint-12 world by the checkpoint-14 build. `client-startup-logs-2026-10-23.tar.gz` records a subsequent feature-worktree `runClient` boot/resource load. No automated world/GUI check or compatibility work was performed; use a fresh world.

Target integration at `b3b7a347`: `merge-preservation-logs-2026-10-23.tar.gz` holds pre-merge SHA256/status evidence, the target's binary local diff and a backup of its one untracked test file (none staged or committed). `target-integration-logs-2026-10-23.tar.gz` holds the passing target jar build and live-client startup snapshot. All ten target local files were unchanged by the fast-forward. Client was left running at owner request; no tests or automated world/GUI check.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Render production geometry, installer-facing orientation and haze; preserve target local changes and record target build/client evidence

Batch: 2026-10-23-pipeline-sight-glass

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `SightGlassRender.java`, `logs/*-logs-2026-10-23.tar.gz`

### History

- Archived 2026-10-23 (integrated at target `b3b7a347`, ten local files preserved; target jar passed, client left running; no tests, owner checks pending)
