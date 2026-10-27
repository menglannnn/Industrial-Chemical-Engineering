# Pipeline shutoff valve software renders

Local ignored tooling for batch `2026-10-23-pipeline-shutoff-valve`, production source commit `4dd96fde`. No detached tracked code or extra Gradle switch.

`ShutoffValveRender.java` draws the actual `ShutoffValveGeometry` mesh with flat Java2D material lighting. It creates the 18-case open/closed X/Y/Z and 0.001/0.05/1 m sheet, a 0.25 m preview and accessory/reducer/vertical-handle details. These are geometry reviews, not Minecraft shader/texture or in-game validation. Minimum bore and default bore share the existing minimum drawn outside-diameter convention.

From the owned worktree after `gradlew.bat test --tests '*PipelineShutoffValveTest' jar --max-workers=4`, with JDK 21:

```powershell
$taskJava = 'C:\Program Files\Java\jdk-21.0.11\bin'
& "$taskJava\javac.exe" -cp build/classes/java/main -d build/tmp/shutoff-render D:\Minecraft\Modding\1.21\CreateChemE\tools\pipeline-shutoff-valve\ShutoffValveRender.java
& "$taskJava\java.exe" '-Djava.awt.headless=true' -cp 'build/tmp/shutoff-render;build/classes/java/main' ShutoffValveRender D:\Minecraft\Modding\1.21\CreateChemE\documentation\2026-10-23-pipeline-shutoff-valve\renders
```

Outputs stay in the canonical batch's `renders/`. Build/test/render logs, the targeted JUnit XML and resource/package checks are compressed in `logs/pipeline-shutoff-valve-logs-2026-10-23.tar.gz`. Earlier compile/test failure logs are retained as investigation evidence; the `*-final.log` and final JUnit XML record the passing result. No client was launched for this task.

## Owner-requested integration

Integration fast-forwarded to `beb5c2fc` on `claude/floating-roof-tanks-81d68b`. Required quick/slow checks finished with the same five previously reproduced failures (1364 quick / 185 slow tests); target jar passed with ten local files preserved. Ordinary runClient was then launched and left running. `logs/pipeline-shutoff-valve-merge-logs-2026-10-23.tar.gz` holds the full-gate XML/logs, failure summary, preservation manifest, merge/package logs and a snapshot of startup logs. No in-game world was opened by the agent.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Actual production-mesh valve render sheets, targeted test/package and resource evidence

Batch: 2026-10-23-pipeline-shutoff-valve

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `ShutoffValveRender.java`, `logs/pipeline-shutoff-valve-logs-2026-10-23.tar.gz`

### History

- Archived 2026-10-23 (integration `beb5c2fc`, source `4dd96fde`; target jar passed, ten local files preserved, client running; five existing full-gate failures)
