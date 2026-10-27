# Pipeline device rendering and the constructor's first page: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · commits `d677b607` (devices), `714f4833` (constructor), `0a4db3a2` (changelog) · owner report with shot 43.

## Physical expectation

- A strainer, valve, pump or tank is a closed steel body in daylight: lit like the ground beside it, with the ground under it still there. An unused nozzle is closed by a blind flange; a nozzle bolted to a pipe or another device meets a flange of its own size.
- A 1 m line does not end in a 1 m pump nozzle: it narrows in a reducer to the device nozzle (at most 24", 0.6 m), as at a real pump suction.

## What the code did

1. **Filter black, ground missing.** `ModBlocks` kept the inline filter occluding (it was a full-cube model before this batch). An opaque full cube holds no light, so the Y-strainer inside drew black, and the grass under it was culled (the light-blue square in shot 43).
2. **Faces missing on every device.** A device flange left its face undrawn ("the flange met there covers it"), so an unused nozzle was an open ring. Nozzles larger than the body poked out of it with an open start. Pump, compressor and box bases were not drawn. The strainer body ended in rings sized to the line, not to the nozzle. A 1 m pipe's ball and a 0.4 m valve's actuator stuck out of the block and were clipped open. A sight-line test found holes in 119 of 324 keys, every device kind and the 1 m pipes.
3. **Flat faces lit from the neighbour.** Vanilla lights a full-cube block's axis-aligned quads from the block they face (`ModelBlockRenderer.calculateShape`), so a pump's underside took the ground's darkness.
4. **Mismatched joints.** A 1 m pipe flanged at 1 m to a device drawing 0.6 m; a tank bolted to a pump drew no nozzle.

## Fixes (in their stage)

- Registration: every device `noOcclusion`.
- `LineGeometry`: every part is a closed solid. Flange faces are always drawn (where two meet, the coplanar faces look opposite ways, so neither shows). Arms starting in a body or ball close their start. The 1 m pipe ball fits inside the block. A flange without a hub has a whole back. Bases and strainer ends are drawn. The actuator room counts its 0.02 m rim.
- `LineGeometry.nozzle` (at most 0.6 m) is the one size a pipe and a device flange at. `FluidDeviceBlockEntity` hands it to both sides, and two devices bolt at the larger of their sizes.
- `FluidDeviceBlock`: devices and the largest pipe are a texel short of a full cube.
- **Constructor:** opens on "What to build" (vertical drum, horizontal drum, distillation column, each with what it is); the draft's kind is marked. The design page shows the kind with Change, a horizontal drum's Axis row, and for a column Import beside Build and Done. Import overlapped the title before. `Form.next()` (the old cycle) is removed.

## Checks

- New test `aViewerOutsideTheBlockSeesOnlyOuterFaces`: 200 sight lines per key, lines entering through a connected face skipped. It failed on 119 keys before the fixes and passes now.
- Probe sweep (`device-check/Sweep.java`): 8,064 keys × 4,000 lines, 0 holes. `VesselSweep.java`: drums and columns had 0 holes before and after, so the vessel shells were already closed.
- Equipment unit tests (32) pass; fluid GameTests 31/31 (fresh folder). Solver code untouched: no junction, chain-100 or campaign runs.
- In game (bridge lane, fresh worlds), shots 44–47: devices lit and closed, grass under the filter, the bolted line pump–valve–filter–compressor flanged flush; choice page, column page, Change, Build of a vertical drum.

## For the owner to validate (reversible)

- The screen always opens on the choice page, even when the constructor holds a design (one click to return to it).
- Unused nozzles are drawn as blind flanges.
- Device collision is the block less 1/16 at the sides.
- A line above 24" reduces at the device.

Code guide: still to be written with the batch (unchanged item). Tool folder: `tools/custom-equipment-rendering-poc/device-check/` (scene, views, probes `BackFaces`, `Sweep`, `VesselSweep`, shots, `before/`); logs `logs/device-check-2026-10-16.tar.gz`.
