# Constructed mover arrow contracts

Snapshot: `763939e8` (`codex/mover-port-markings`), 2026-10-23; initial implementation `07e45392`, single-set refinement `e612cff3`.

## Equipment and geometry

- Only the adjoining pipe section receives flow arrows. EquipmentShellModel uses the original surface/fixedSurface paths; equipment nozzle necks and bare unconnected flanges carry no decals, avoiding a duplicate set across the flange.
- Minecraft-free MoverPortMarkings.pipe(surface, roles) derives panels from actual PIPE-material axial planes near a machine connection. Coplanar hub/neck/UV segments are joined, then at most four radial panels selected per port, all at one axial station. Reducers, end faces, glass and non-pipe surfaces are excluded.
- The private nozzle surface API/builder was removed. Local decal frames use actual pipe-plane axes. Yellow GUARD and charcoal OPENING sprite texels supply a border and arrow with no text or new bitmap/font. Geometry is drawing only; layouts, stamps and collision are unchanged.

## Client rendering

- FluidDeviceModel.Look includes MoverPorts alongside the sight-glass key. Solid pipe/metal polygons receive arrow overlays; glass/liquid geometry remains unchanged. Items default to no roles. fluid_device.json maps guard to the existing equipment_guard sprite.
- Local mesh builders return immutable lists for existing concurrent model caches. Resource reload rebuilds cached geometry. No new per-frame draw or process computation.

## Runtime static geometry metadata

- FluidDeviceBlockEntity.Drawn carries moverPorts: two bits per Minecraft face (0 none, 1 suction, 2 discharge). computeFaces derives them from connected, loaded neighboring constructed mover nozzle roles. refreshFaces compares role changes alongside diameter/flange bytes.
- getUpdateTag carries MoverPorts as static metadata. loadAdditional bounds the 12-bit value; model generation ignores unknown per-face roles. saveAdditional does not persist it; existing geometry refresh/load paths rebuild it.
- Pipe arrows point toward a suction neighbor and away from a discharge neighbor. Auxiliary/coolant roles and one-block movers produce no role marks. These are fixed design directions; no process contents, GUI, subscription, checkpoint or protocol format changes.
