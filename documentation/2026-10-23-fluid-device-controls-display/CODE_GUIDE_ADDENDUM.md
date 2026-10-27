# Code guide branch addendum: fluid device controls and displays

Snapshot: `232b0466`, 2026-10-23, integrated into `claude/floating-roof-tanks-81d68b` (implementation `0f208c52`, `89168361`). Main/version unchanged.

## Runtime and persistence
- `PhysicalFluidTopology.Device` adds `generatorEnabled`; false is valid only on GENERATOR. World placement explicitly supplies false for generators. Authoring overloads retain enabled sources for explicit scientific fixtures; they are not save readers.
- `Device.connects` closes every hydraulic contact while Off. Normal queued topology edits fence and rebuild affected islands; identity/specification stay registered. `FluidDeviceBlock.onPlace` rotation preserves the flag.
- `FluidCheckpointCodec`: checkpoint 16 / storage unit 9. Every active and pending registration writes a required strict boolean between actuator control and port count. Other records remain structurally unchanged but use the new enclosing version. No migration; `FluidSavedData` retains the owner-approved incompatible-format reset.
- `FluidWorldAuthority.view` gathers all published boundary-island snapshots for a VOID. `PipePresentation.boundaryInflow` selects forward transport when the void is the pipe's second node, reverse when first, excludes outward streams, divides each accepted stream by its own interval duration, and sums using the existing mass/mole/volume/solid accumulator.
- `FluidView.PipeInfo` also carries a void's incoming rates as a one-second equivalent (`intervalSeconds=1`), without connections. Empty/unavailable views have empty transport. Dummy void boundary inventory is never used for the visible composition. The void's boundary pressure remains visible; its dummy temperature is suppressed.
- No solver, thermodynamic refresh, pending-event application or clock advance is invoked by a menu open or rate toggle. Replay and updates retain the existing requested, cached, approximately five-second engine schedule.

## Wire and GUI
- `FluidNetwork.PROTOCOL=fluid-13`. Existing required `Controls.power` also represents generator power. `Controls.from` reads the registration flag; edit permissions/revision validation remain; only generators use it for their new source flag. Pump/compressor power remains constructed-only.
- `MenuData` requires transport presentation for VOID as well as PIPE/FILTER; axis/crystal validation applies. There is no new composition packet or per-tick process message.
- `FlowRateUnit` converts the presentation time basis (/s factor 1, /h factor 3600). Engine and wire values stay SI. `FluidDeviceScreen` remembers the selection for the client session, defaults /s, and converts valid suction drafts when toggled and back to m3/s on Apply; invalid conversion leaves units and draft intact.
- Uniform kg or kmol rates: summaries, tank outlets, pipe connections, component tables and transported solids. Fluid moles become kmol using /1000. Captured filter solids and stored inventory remain kg/kmol; particle size, pressure gradient and velocity keep their physical units.
- New source power button uses existing draft/Apply flow and accepted-power line. Generator rail includes three controls and scrolls. Footer has five buttons with minimum widths and compact labels below 400 GUI pixels.
- Available phase is selected until the owner chooses a phase, including first flow arriving after an initially empty publication. A chosen phase is retained.

- Generator rail uses `renderPhaseRatio` on the published source state phase volumes plus solids volume, with a feed-specific hint. It remains a feed composition display while Off. Reservoir rail shows current / maximum litres from `State.volume` and `State.capacity`; the hover hint preserves full values at narrow widths.

## Build
- `gradlew.bat jar --max-workers=4` only; no tests or automated GUI checks, per owner. No new Gradle task or offline harness.
