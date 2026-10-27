# Fluid format reset — runtime and equipment contracts

Snapshot: `7234bb46` (integration delivery of `49e57860`), 2026-10-23.

## Runtime (`fluid-runtime.md`)

- The binary schema stays checkpoint 15 / unit 8. Core, pack and unit version guards throw package-private `FluidCheckpointCodec.FormatMismatchException`, an IllegalArgumentException subtype; all direct codecs/store/read methods remain strict.
- Only `FluidSavedData.open` converts that refusal into an empty dirty authority and logs `fluid_checkpoint status=RESET`. No old contents, settings, clocks, event/recovery queues or optimization state survive. Other malformed data and property mismatch paths are unchanged.
- `FluidCheckpointStore.resetForFormatChange` accepts only a store before load/save success and gives its fresh replacement the same backend/executor. New pack numbers skip retired files; ordinary ordered atomic commit replaces the core before removing unreferenced retired packs. No destructive file work occurs in the loader.
- IChemE chunk hooks always tell a TickTask, rather than executing inline before chunk visibility. FluidWorldAuthority retains early dimension/chunk notices until start and drops them at forget; unbound structure requests live only in the server-owned authority and clear on close.
- On each loaded chunk, inspect static IChemE cell metadata including pending block entities; rebind matching registrations and queue defaults when missing. Pending structures retry only on intersecting chunk-load notifications. No forced chunks, new tick polling, eager process solve or extra process packets.

## Equipment (`equipment.md`)

- `EquipmentStructure.restoreFluidBinding` binds an existing node or queues the existing default registration after every bounding chunk is present and every expected shell/interior cell matches. A missing chunk defers; a partial/stale layout waits for normal construction or refresh.
- Saved one-block identities are replaced when their position receives a new default registration; constructed nodes likewise receive the new registration identity. Defaults follow normal placement, including largest-neighbor pipe bore inheritance.
- Placed blocks, sight-glass states, layout origins, paint and shell nozzle metadata remain. Fluid inventories/settings do not. Existing scheduled device views refresh dynamic displays after rebinding.
