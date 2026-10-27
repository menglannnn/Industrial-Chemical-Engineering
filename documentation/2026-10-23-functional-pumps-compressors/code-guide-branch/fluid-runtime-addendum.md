

## Functional pumps and compressors — branch addendum

**Snapshot:** 29ff371b (claude/floating-roof-tanks-81d68b), 2026-10-23. Code 4e96e8ab; canonical main chapter refresh remains due at the future main merge.

- `PhysicalFluidTopology.Port` now holds `(cell, face, phase port, ConnectionRole)`. The three-argument source constructor describes a VESSEL port.
- Connection roles have explicit stable checkpoint codes: VESSEL 0, SUCTION 1, DISCHARGE 2. Unknown codes are rejected.
- Device validation keeps distinct contacts and matching dimensions, permits reservoir VESSEL ports, and requires ported PUMP/COMPRESSOR devices to have exactly two BULK ports, one of each process role, with the suction cell equal to the binding position. Other kinds cannot carry ports; one-block devices retain empty port lists.
- Compilation marks driven links by their actual discharge contact. One-block actuators use their facing contact. The old dot-product actuator inference is removed; filter-side inference is unchanged.
- A mover's node elevation is its suction cell's y; the ordinary graph elevations include downstream vertical displacement. Resistance still uses the mover's existing 1 m geometry, split into halves, taking adjoining pipe bore and roughness as before.
- `placeMover()` shares the ordinary placement factory: pump target 0.01 m³/s and water-reference rise cap 500000 Pa; compressor target 0.05 m³/s and ratio cap 3. Both are self-powered and use the existing phase protections and work/heat accounting.
- Fluid checkpoint VERSION is 14; UNIT_FORMAT is 7. Registration port encoding is count, then each port's position, face byte, phase-port byte and role-code byte. Both active registrations and pending replacement edits use this encoding. The rest of the clock/material/event/optimisation-state encoding is retained.
- Old formats require a fresh world. No compatibility reader or migration. Fluid GUI wire fields and protocol `fluid-11` are unchanged.

