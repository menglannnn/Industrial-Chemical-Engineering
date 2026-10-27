# ladder-probe

Batch `2026-10-21-floating-roof-tanks`, 2026-10-25.

## Owner reports
- The rolling ladder of a floating-roof tank did not reach the platform.
- A furnace's ladder did not let the player off onto its walkways.
- "Review other ladders."
- Later: "The broken 3d model of the floating roof tank is not fixed" (a 7 x 4 m tank's platform at the ladder's head).

## Fixes
- `e2619287`: walk-through openings, rest platforms, the sphere's bridge.
- `68af29c2`: the rolling ladder's tread rule, the tank's wall collision and the roof in its shell cells.
- `d8746d00`: a tank platform's inner edge wide enough for the ladder's opening and a railed post either side (`TankDesign.span`).
- Review: `documentation/2026-10-21-floating-roof-tanks/LADDER_REVIEW.md`.

## Contents
- `LadderStepOffProbeTest.java`: the investigation probe, removed from `src/test` before `e2619287` and never tracked.
  - It has its own copy of the player simulation (later `PlayerWalk`) and the trace helpers used to find each defect: `traceExit`, `whatIsAt` (solids at a point), `dump` (decks near a ladder), `deckLanes` (solids on a deck's walking lanes).
  - Its early `tryLadder` climbed to 1.2 m over a landing and started on a floor voxel's inside. Both are artefacts: `PlayerWalk.leave` fixed them.
- `StorageRender.java`: `tools/storage-vessel-renders/StorageRender.java` with `-Dfocus=pivot` (a 3 m box round the rolling ladder's hinge) and `d:x:y:z` views.
- `renders/`: the rolling ladder's head at liquid level 5.5 m (before and after `68af29c2`), side views with the roof landed, and the head with the tank full (20 x 12 tank).
- `renders/platform-7m-*`: the 7 x 4 m tank's platform before and after `d8746d00` (inner rail missing, end rails in the air; then railed either side of the opening).
- `CellCount.java`: prints the tank sizes whose layout has a given number of cells (the "Built a Floating-roof tank: N cells" chat line in `run/logs/latest.log`). Run like the renders: `... com.wormzjl.icheme.science.equipment.CellCount 347`.
- `logs/ladder-probe-logs-2026-10-25.tar.gz`: the probe runs (every unit, gasifiers, columns/tanks/spheres) and the gasifier deck-lane comparison at 1 m and 1.45 m deck width.

## Run (from a worktree)
The tracked gates replace the probe:
- `./gradlew slowGates --tests '*ProcessUnitTest.aPlayerClimbs*'`
- `./gradlew test --tests '*FloatingRoofsTest'`

To re-attach the probe, copy it to `src/test/java/com/wormzjl/icheme/science/equipment/TmpLadderStepOffProbeTest.java`. Its `probe` calls `PlayerWalk.leave`, and it needs the package-private `ShapeRasterizer.climbs` (tracked since `e2619287`).

Renders, after `./gradlew compileJava`:
```bash
javac -d build/tmp/ladder/classes -cp "build/classes/java/main;build/resources/main" <this folder>/StorageRender.java
MSYS_NO_PATHCONV=1 java -Xshare:off -Xmx1536m -Dfocus=pivot -cp "build/tmp/ladder/classes;build/classes/java/main;build/resources/main" com.wormzjl.icheme.science.equipment.StorageRender TANK 20 12 0 5.5 out/head "d:0:-0.3:1,d:-0.94:-0.35:0.33"
```
