

## Functional pumps and compressors — branch addendum

**Snapshot:** 29ff371b (claude/floating-roof-tanks-81d68b), 2026-10-23. Code 4e96e8ab; canonical main chapter refresh remains due at the future main merge.

- `EquipmentShape.Kind.mover()` identifies PUMP, COMPRESSOR and CENTRIFUGAL_COMPRESSOR. `EquipmentShape.fluidDevice()` includes these movers and the existing reservoirs. All other units/columns remain drawing-only.
- `ShapeRasterizer.fluidCell()` selects the transformed SUCTION nozzle's flange shell cell for movers. The existing layout builder includes the fluid cell among shell cells. Reservoir binding rules are unchanged.
- `EquipmentStructure.place()` registers fluid equipment only after a whole-height build. `register()` calls `FluidWorldAuthority.placeMover()` for machines; reservoir placement remains `placeVessel()` with its original volume/height/open semantics.
- Machine port construction selects nozzle roles SUCTION and DISCHARGE only, preserving transformed cells/faces and bulk phase class. Auxiliary coolant nozzles still resize/draw with pipes, but have no hydraulic registration.
- Dismantling removes the structure's registration even if its binding cell is unloaded. A block-entity load only binds the registry record held at that node; it never registers a missing machine.
- Refresh preserves geometry/controls via a queued edit. If the binding moves, `relocateMover()` queues removal and a new identity in one event with the same kind, geometry, control and spec. It then releases/rebinds the node entities. Existing reservoir refresh behavior remains unchanged.
- Model dimensions and datasheet motor/design figures do not set operational capacities. Client animation remains cosmetic.

