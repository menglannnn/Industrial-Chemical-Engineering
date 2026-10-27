# Functional machines — branch contract addendum

Snapshot: `4e96e8ab`, 2026-10-23, `codex/functional-pumps-compressors`. This branch extends the floating-roof/pipeline integration at `7bf50b7c`; it has not reached main. Apply these facts to the canonical guide when it merges, retaining other branches' changes.

## Equipment lifecycle (`equipment.md`)

- `EquipmentShape.Kind.mover()` identifies PUMP, COMPRESSOR and CENTRIFUGAL_COMPRESSOR. `EquipmentShape.fluidDevice()` includes these movers and the existing reservoirs. All other units/columns remain drawing-only.
- `ShapeRasterizer.fluidCell()` selects the transformed SUCTION nozzle's flange shell cell for movers. The existing layout builder includes the fluid cell among shell cells. Reservoir binding rules are unchanged.
- `EquipmentStructure.place()` registers fluid equipment only after a whole-height build. `register()` calls `FluidWorldAuthority.placeMover()` for machines; reservoir placement remains `placeVessel()` with its original volume/height/open semantics.
- Machine port construction selects nozzle roles SUCTION and DISCHARGE only, preserving transformed cells/faces and bulk phase class. Auxiliary coolant nozzles still resize/draw with pipes, but have no hydraulic registration.
- Dismantling removes the structure's registration even if its binding cell is unloaded. A block-entity load only binds the registry record held at that node; it never registers a missing machine.
- Refresh preserves geometry/controls via a queued edit. If the binding moves, `relocateMover()` queues removal and a new identity in one event with the same kind, geometry, control and spec. It then releases/rebinds the node entities. Existing reservoir refresh behavior remains unchanged.
- Model dimensions and datasheet motor/design figures do not set operational capacities. Client animation remains cosmetic.

## Fluid adapter and saved formats (`fluid-runtime.md`)

- `PhysicalFluidTopology.Port` now holds `(cell, face, phase port, ConnectionRole)`. The three-argument source constructor describes a VESSEL port.
- Connection roles have explicit stable checkpoint codes: VESSEL 0, SUCTION 1, DISCHARGE 2. Unknown codes are rejected.
- Device validation keeps distinct contacts and matching dimensions, permits reservoir VESSEL ports, and requires ported PUMP/COMPRESSOR devices to have exactly two BULK ports, one of each process role, with the suction cell equal to the binding position. Other kinds cannot carry ports; one-block devices retain empty port lists.
- Compilation marks driven links by their actual discharge contact. One-block actuators use their facing contact. The old dot-product actuator inference is removed; filter-side inference is unchanged.
- A mover's node elevation is its suction cell's y; the ordinary graph elevations include downstream vertical displacement. Resistance still uses the mover's existing 1 m geometry, split into halves, taking adjoining pipe bore and roughness as before.
- `placeMover()` shares the ordinary placement factory: pump target 0.01 m³/s and water-reference rise cap 500000 Pa; compressor target 0.05 m³/s and ratio cap 3. Both are self-powered and use the existing phase protections and work/heat accounting.
- Fluid checkpoint VERSION is 14; UNIT_FORMAT is 7. Registration port encoding is count, then each port's position, face byte, phase-port byte and role-code byte. Both active registrations and pending replacement edits use this encoding. The rest of the clock/material/event/optimisation-state encoding is retained.
- Old formats require a fresh world. No compatibility reader or migration. Fluid GUI wire fields and protocol `fluid-11` are unchanged.

## GUI and item interactions (`client-gui.md`)

- Normal use of any shell or interior cell opens functional equipment's `FluidDeviceMenu`. Other units retain their client-built datasheet screen.
- Both shell and interior delegate Fluid Debugger use to the shared diagnostics path. Menu opening still subscribes, then replays the cached publication; closing unsubscribes. No solver is run or input applied on opening.
- Menu identity, revision, reach and edit validation use the existing contracts. Reach grows with structure size. The title is `gui.icheme.vessel.title.<kind>` rather than always Drum.
- `FluidDeviceScreen` exposes the equipment datasheet for movers as well as reservoirs. A mover's tab is next to Overview; reservoir tab positions are unchanged.
- The waiting-for-identity message and general datasheet hover text are translated. The Pipeline Tool's diameter component/menu/edit path and debugger-only individual-pipe inspection are unchanged.

## Verification (`verification.md`)

- No Gradle tasks or tests added/changed. Owner-selected verification for this branch is `gradlew.bat jar --max-workers=4`; no tests or automated/in-game GUI checks were run. Manual acceptance is listed in `FUNCTIONAL_MACHINES_REVIEW.md`.
