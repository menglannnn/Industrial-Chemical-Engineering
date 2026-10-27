# Equipment arrows and pump flange review

2026-10-24. Implementation 7cb42268 / delivery 2fe26e21, branch codex/mover-arrows-on-equipment; base b70e92aa.

- Expected: the equipment neck carries the direction arrow behind the joint flange; suction points into the machine, discharge away. A connected pipe carries no equipment arrow.
- Previous model: pipe-side arrows identified the port, and fixed pump flanges plus shared nozzle flanges left an extra flange inside the equipment neck. Moving paint alone trapped the split-case arrows between them.
- Implementation: MoverPortMarkings.surface uses each equipment cell's nozzle codes and transformed neck frame. Four facet stencils at one axial station form one arrow band. Auxiliary/coolant ports are excluded. Bare nozzles use design OD.
- EquipmentShellModel keeps the same static stencil list during stopped/running rendering. FluidDeviceModel no longer adds mover decals; check-valve fitting marks stay unchanged.
- Removed transient MoverPorts block-update metadata, Drawn field and pipe cache-key field. No process snapshots, subscriptions, controls or fluid formats changed (18/11).
- Both pump variants omit the redundant fixed inlet/outlet flanges. The shared nozzle mesh owns the equipment plate at the existing connection plane. Split-case branches end at the sized neck with a casting shoulder; resized necks carry ordinary nozzle collision.
- compileJava jar passed with four workers. No tests or GUI/in-game actions; integration client was active initially and later saved all dimensions and exited normally at 16:23:54. No client restart was requested for this follow-up.
- Offline before/after sweep: all 120 pump size/heading/minimum-and-maximum-support cases retain model bounds, nozzle faces, physical port cells/directions and hydraulic binding cells.
- 28 production-mesh render cases cover four machine variants, four headings, minimum/default/maximum drawn OD and bare nozzles; every case has equipment stencils and zero pipe stencils. 184 rendered views plus three preview sheets are retained.
- Reviewed the split-case joint/arrow preview and size-extreme sheet. Existing centrifugal-compressor concrete/supports can occlude its neck from side views; stencil geometry follows the actual nozzle and is visible through the existing slab opening from below. No support redesign was included.
- Tools: tools/mover-equipment-arrows/ (renderer, connection fingerprint and replay scripts); no probe or temporary harness was added to tracked code. Logs are compressed there.
- Code guide: equipment, client-gui and fluid-runtime updated from this branch. Integrated at 4ad6dc97 into claude/floating-roof-tanks-81d68b on 2026-10-24 with the pressure accessory batch; quick/slow suites retain only five known baseline failures, and local ProcessUnit edits were preserved. No main merge/version bump.

Manual follow-up: inspect the joint and arrow in game; check resize and running animation. No old-world verification was performed.
