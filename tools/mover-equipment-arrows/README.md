# Equipment-side mover arrows

Batch: documentation/2026-10-24-mover-equipment-arrows. Source 7cb42268, codex/mover-arrows-on-equipment, not merged.

Offline software verification only. MoverPortRender.java is adapted from tools/mover-port-markings: actual ShapeRasterizer and MoverPortMarkings polygons, product textures, per-pixel depth, culled back faces. It adds unmarked adjoining pipes and emits full views plus two-sided/oblique nozzle closeups. Args: KIND size count support-or-auto output-prefix views-or-none heading drawn-OD-or-0-default attached-boolean.

From the built owned worktree (Java 21):

```powershell
$arrowTool='D:/Minecraft/Modding/1.21/CreateChemE/tools/mover-equipment-arrows'
$arrowJdk='C:/Program Files/Java/jdk-21.0.11/bin'
& "$arrowJdk/javac.exe" -cp build/classes/java/main -d "$arrowTool/classes" "$arrowTool/MoverPortRender.java" "$arrowTool/PumpPorts.java"
# baseline/ProcessUnit.java is the exact git show b70e92aa:src/main/java/com/wormzjl/icheme/science/equipment/ProcessUnit.java.
& "$arrowJdk/javac.exe" -cp build/classes/java/main -d "$arrowTool/baseline-classes" "$arrowTool/baseline/ProcessUnit.java"
& 'C:/Users/wormz/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' "$arrowTool/run.py"
& 'C:/Users/wormz/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' "$arrowTool/make_sheet.py"
```

PumpPorts compares 120 exact connection fingerprints against the baseline class loaded first. run.py checks connected/bare render counts across all headings and minimum/default/maximum drawn OD. The existing centrifugal-compressor table/supports occlude side closeups; oblique views retain that real occlusion.

build.log, verification.log, renders.log and port fingerprints are archived under logs/mover-equipment-arrows-logs-2026-10-24.tar.gz. Renders are in the batch. No product harness, Gradle switch, test or temporary instrumentation was introduced/detached; no removing commit or reattach patch applies.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Depth-tested equipment/pipe software renders and before/after pump connection fingerprint

Batch: 2026-10-24-mover-equipment-arrows

Status: Archived 2026-10-24. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `MoverPortRender.java`, `PumpPorts.java`, `run.py`, `make_sheet.py`, `logs/`

### History

- Archived 2026-10-24 (feature `7cb42268`; compile/jar, 120 connection cases and 28 render cases pass; integrated `4ad6dc97` with local edits preserved; no game input)
