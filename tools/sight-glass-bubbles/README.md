# Sight-glass motion and crude colour tooling

Serves batch documentation/2026-10-25-sight-glass-bubbles, implementation2c9c6d00. All files are canonical ignored tooling; none was tracked/detached from product source. No Gradle switches/tasks or gate membership changes.

1. With no dev client running, compile/package and run focused coverage in the owned feature worktree:
   `gradlew test --tests '*SightGlassDisplayTest' --tests '*FluidAppearanceTest' --tests '*TankDisplayTest' --tests '*DeterministicMathSourceTest' jar --console=plain`.
2. `run.ps1 [-SourceRoot <compiled-worktree>]` compiles BubblePreview.java against native main classes with JDK21 and renders64 frames plus size/orientation sheets into the batch. It uses actual SightGlassGeometry/window dimensions, SightGlassBubbles paths/colours and SightGlassDisplay.turbid. No Minecraft/world/solver is loaded; Java2D lighting is illustrative.
3. Run animate.py with the bundled Python/Pillow runtime to assemble a fixed-palette GIF and representative still. Native output assembly only, not modification of a user reference.
4. `palette.py --source-root <worktree> [--apply]` reads each petroleum fraction's unique property-record NBP, derives RGB through the documented Celsius anchors, and writes the swatch chart/nbp-palette.json. Only --apply changes component appearance.color; it preserves transparency and every other field. It does not alter science properties/revisions or override datapack appearance at runtime.

Final focused result:14 tests/zero failures-errors-skips and jar successful. Evidence verification.json and compressed logs/test XML under logs/sight-glass-bubbles-logs-2026-10-25.tar.gz. No new in-game check/client launch; prior integration stays paused. Full quick/slow are due on resumed integration, not claimed by these focused results.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Native bubble animation/size views, NBP palette generator and focused evidence

Batch: 2026-10-25-sight-glass-bubbles

Status: Concluded 2026-10-26 (batch in 0.12.0). Index: [../INDEX.md](../INDEX.md).

### Notes

- status added 2026-10-26 (the row had none): the batch reached `main` in 0.12.0.
