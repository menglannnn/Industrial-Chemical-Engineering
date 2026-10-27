# Fluid display follow-ups

Local ignored tooling for `2026-10-23-fluid-display-followups`, source `c1e567da`.

- GeneratorPhaseAudit.java: fresh bundled WTI feed at 200掳C / 300000 and 401325 Pa, using production generator initialization; reports volume/mass/mole fractions and phase densities, then verifies isolated compile keeps the source state. Opens no world or GUI and advances no simulation.
- AccessoryIconRender.java: actual item meshes with the old vanilla block GUI transform and the two new item JSON transforms; renders 16 px icons and a magnified sheet, reports exact projected bounds and rejects clipping. Flat colours, not Minecraft texture/shader validation.
- run.ps1: compile/run both tools after product `gradlew.bat jar --max-workers=4`, using JDK 21 and cached Gson/EJML. No extra Gradle tasks/switches or detached tracked code. Render files go to the canonical batch renders/ directory.

Invoke `powershell -NoProfile -File tools/fluid-display-followups/run.ps1` from the main checkout (override SourceRoot if needed). Logs are archived under logs/; client/test restrictions still apply to any separate Gradle checks. Do not run these against a saved world.

Integrated 2026-10-24 as target a591404f, after resumed WTI tracing. Ten local target files preserved, target jar passed and runClient relaunched. Source quick/slow gates retained the same five previously reproduced failures (1364 quick / 185 slow tests). The merge log archive contains the preserved-file manifest, full-gate XML, merge/package logs and a client startup snapshot. The rate-cache proposal lives separately in tools/wti-pumping-rate-hold/ and is not in production.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Actual item mesh/GUI-transform slot bounds and fresh WTI feed volume/mass calculation

Batch: 2026-10-23-fluid-display-followups

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `run.ps1`, `AccessoryIconRender.java`, `GeneratorPhaseAudit.java`, `logs/`

### History

- Archived 2026-10-23 (integrated `a591404f`; jar/render bounds pass; ten target files preserved; client relaunched)
