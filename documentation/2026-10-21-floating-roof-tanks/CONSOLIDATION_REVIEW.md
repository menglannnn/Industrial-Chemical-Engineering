# Code consolidation review (batch 2026-10-21-floating-roof-tanks)

Date 2026-10-25. Branch `claude/floating-roof-tanks-81d68b`, on `61343f3d` (after the small-tank platform fix
`d8746d00`). Owner request: "too many new classes and tests again". Scope: fold tiny classes into their owners, delete
duplicated helpers, merge per-accessory tests; no behaviour, physics, solver, GUI layout or equipment geometry change;
the ladder work (`PlayerWalk`, `FloatingRoofs` climb/tread/rim/join, `TankDesign.span` and the rolling ladder, the two
ladder gates) untouched. Not merged, not pushed, no version bump.

## Counts

| | `61343f3d` | `65886b15` |
|---|---|---|
| `src/main/java` files | 356 | 320 (-36) |
| `src/test/java` files | 329 | 319 (-10) |
| lines in `src/` | | +1771 / -2267 |
| `test`: classes / tests / failures | 271 / 1419 / 0 | 262 / 1419 / 0 |
| `slowGates`: classes / tests / failures | 18 / 187 / 0 | 18 / 187 / 0 |

## Commits

1. `7b3137bf` Instrument runtime nine files to three: `Instrument` holds the accessory type enum (moved from the store),
   `Mount`, `Holder`, `Selectable` and `Metadata`; `InstrumentHosts` nests `PipeAccessory`; `InstrumentService` nests
   its saved `Store`. The five one-line fitting item subclasses and the sight-glass item become one
   `PipelineFittingItem(properties, Instrument.Type)`. `PipelineToolSettings` nests in `PipelineToolItem` as `Settings`;
   `ModDataComponents` folds into `ModItems`, `ModSounds` into `ModBlocks`. Deleted 15 files.
2. `2d3593c8` The tank, sight-glass, mover, instrument and pipeline-tool channel classes become one section of
   `FluidNetwork`; the identical sight-glass and mover target and watch records become one `Target` and one `Watch`
   (payload type names the kind). Client caches move into `TankRenderer`, `SightGlassRenderer`, `InstrumentRenderer`;
   `TankWatchers` nests in `FluidWorldAuthority` as `Watchers`. Deleted 9 files.
3. `38825518` Check, shutoff and pressure valve castings become one `ValveGeometry` (three copies of the flanged arms
   become one helper; the fitting end the valves, movers and sight glass each rebuilt becomes `LineGeometry.fittingEnd`).
   `InstrumentSockets` folds into `InstrumentGeometry`, `SightGlassBubbles` into `SightGlassGeometry`,
   `MoverSoundVolume` into `UnitDesign.soundGain`. Deleted 6, added 1. Geometry digest over 864 pipe keys, housing,
   needles, connectors, sockets, bubbles and sound gains bit-identical before and after: `4a19ecc3855c29f7`.
4. `36fb0965` `SightGlassDisplay` and `PumpStatus` join `PipePresentation` (`Sample` becomes `PipePresentation.SightGlass`);
   `PressureUnit` and `FlowRateUnit` nest in `DisplayUnit`; `FluidGuiLayout` and `PhaseRatioBasis` nest in
   `FluidDeviceScreen`, their only user; `PipeObservation` nests in `PassiveStepSolver`. Deleted 7 files.
5. `65886b15` Tests (criteria K1-K6: every method kept, 48 methods before and after in the merged classes):
   `PipelineFittingTest` = the check, shutoff, pressure-valve, filter and inline-mover tests on one shared line fixture
   (each test keeps its own reservoir charges); `PipePresentationTest` gains the sight-glass, pump status and mover
   sound tests; `GaugeAccessoriesTest` gains the pipeline tool settings round trip; `OpenTankRuntimeTest` gains the tank
   display and watcher tests; `NozzleRoom` nests in `ProcessUnitTest`. Merged classes run 0.1 to 1.0 s each (under the
   3 s slow-gate line); no class moved between `test` and `slowGates`.
6. `4f8ab994` The `CHANGELOG.md` `[Unreleased]` Changed line.

## Formats and protocols

No save, checkpoint or wire format change. Every payload keeps its id, byte layout and version string (`fluid-14` on the
fluid registrar; `sight-glass-3`, `mover-1`, `instruments-1`, `pipeline-tool-1` each on its own registrar in
`FluidNetwork.register`). Item ids, data component ids (`pipeline_tool_settings`, `gauge_settings`), sound ids and the
accessory store (format 1, `icheme_accessories`) unchanged. Only behaviour-visible change: the exception texts of the
shared target and watch-list checks are worded once for both kinds.

## Tool folders

Nothing was detached from a tracked path (all removals were folds or merges), so no re-attach patch. The digest probe
and the gate logs are in `tools/floating-roof-tanks/` (`consolidation/`, `logs/consolidation-logs-2026-10-25.tar.gz`),
README section "Code consolidation".

## Gates

Baseline `test slowGates` at `61343f3d`: BUILD SUCCESSFUL, 3 min 59 s. Final at `65886b15`: BUILD SUCCESSFUL, 3 min 37 s, the same 1419 + 187 tests pass, 0 failures.
The 9 fewer `test` classes are the 10 merged-away classes less the new `PipelineFittingTest`; the @Test method names (1496 in `src/test`) are the same before and after except two renamed on merging: `componentRoundTripsExactDiameters...` to `pipelineToolSettingsRoundTripExactDiameters...`, `gainIsBounded...` to `soundGainIsBounded...`. Between commits only the affected classes ran, all green. GameTests, the final lane and campaigns were not run.

## Found, not fixed

- The `fluidGameTest` source set does not compile before or after this work: `FluidPacketGameTests.java:29` uses
  `ModBlocks.PRESSURE_CONTROL_VALVE`, removed by `dc174f27` (pressure valve became a pipe accessory). GameTests were not
  run (out of scope); fix at the final lane.
- Code guide (main checkout, at merge): `instrument-accessories.md`, `equipment.md`, `fluid-runtime.md`, `client-gui.md`
  and `verification.md` still name removed classes (`AccessoryStore`, `InstrumentMount`, `ModDataComponents`,
  `*Network` channel classes, `CheckValveGeometry`, `SightGlassBubbles`, `MoverSoundVolume`, `PumpStatus`,
  `SightGlassDisplay`, `PressureUnit`, `PipeObservation`, the removed test classes and others).

## Owner proposals (not done)

1. Merge `GaugeMenu` (25 lines) and `PipelineToolMenu` (67 lines): both open a hand-held item's settings screen and
   write its data component back. Saves one class; touches GUI code, so left for the owner.
2. Give `AccessoryWrenchItem` and `GaugeItem` one base for their identical `onItemUseFirst`/`use` plumbing (both end in
   `instruments().request`). A few lines only; kept.
3. Fold `TankGaugeGeometry` into `TankDesign`: kept, because `TankDesign` is package-private and already has a `Gauge`
   record; the fold needs renames in the tank drawing the brief kept out of scope.
4. `FluidWorldAuthority` has three near-identical watch, replay and publish paths (tank, sight glass, mover), each
   dropping watchers that left, changed dimension or went out of range. One generic helper would remove about 40 lines
   but changes the engine's publish path, so it is left as it is.
5. `FluidWorldAuthority.Watchers.watched(UUID)` is used only by `OpenTankRuntimeTest` (FR7's per-player limit). Kept.
