# Constructed mover arrow software renders

Batch: documentation/2026-10-23-mover-port-markings/. Code 07e45392, delivery 1730cf14.

MoverPortRender.java adapts the existing tools/new-units/UnitRender to current IChemE packages. It reads actual ShapeRasterizer, LineGeometry and MoverPortMarkings polygons plus product sprites. It attaches one matching pipe per process nozzle and renders depth-tested, back-face-culled orthographic full/close views. This is a software look check, not a game/GUI/solver test.

From the built feature worktree (JDK 21), run:

```powershell
$portJava='C:/Program Files/Java/jdk-21.0.11/bin'
$portTool='D:/Minecraft/Modding/1.21/CreateChemE/tools/mover-port-markings'
& "$portJava/javac.exe" -cp build/classes/java/main -d build/tmp/mover-port-markings "$portTool/MoverPortRender.java"
& "$portJava/java.exe" '-Djava.awt.headless=true' -cp 'build/tmp/mover-port-markings;build/classes/java/main' com.wormzjl.icheme.science.equipment.MoverPortRender PUMP 0.3 1 auto preview iso NORTH 0
```

Args: KIND size count support-or-auto outputPrefix views-or-none heading drawn-OD-or-0-for-default. Cases: PUMP 0.3/0.75 with count 1, COMPRESSOR 0.45 count 2, CENTRIFUGAL_COMPRESSOR 1 count 2; NORTH/EAST/SOUTH/WEST at default OD, plus NORTH at drawn OD 0.0625 and 1. Each invocation also emits suction/discharge closeups. make_sheet.py uses local Pillow to assemble the final preview into the canonical batch renders folder.

52 final renders and arrows-preview.png are in the batch. No bitmap asset was added to the product. Source tools were never tracked/detached; no removing commit or reattach patch applies. No new Gradle task or switch.

logs/mover-port-markings-logs-2026-10-23.tar.gz contains compile/package and final software-render counts. No tests or automated game/GUI checks were run. The existing integration client remains running on its earlier build.

Single-set refinement e612cff3 / delivery 763939e8: renderer omits equipment-side decals and now emits 0 nozzle / 32 adjoining-pipe marker polygons in every matrix case. All renders and preview replaced. logs/single-arrow-logs-2026-10-23.tar.gz contains the current build and renderer evidence. No tests or client interaction.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Production equipment/pipe arrow software renders and preview sheet, no game or solver work

Batch: 2026-10-23-mover-port-markings

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `MoverPortRender.java`, `make_sheet.py`, `logs/mover-port-markings-logs-2026-10-23.tar.gz`

### History

- Archived 2026-10-23 (feature `763939e8`; jar passed, 24 single-set render cases; no tests)
