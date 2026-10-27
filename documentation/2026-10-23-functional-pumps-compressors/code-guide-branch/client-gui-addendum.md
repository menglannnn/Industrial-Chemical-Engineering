

## Functional pumps and compressors — branch addendum

**Snapshot:** 29ff371b (claude/floating-roof-tanks-81d68b), 2026-10-23. Code 4e96e8ab; canonical main chapter refresh remains due at the future main merge.

- Normal use of any shell or interior cell opens functional equipment's `FluidDeviceMenu`. Other units retain their client-built datasheet screen.
- Both shell and interior delegate Fluid Debugger use to the shared diagnostics path. Menu opening still subscribes, then replays the cached publication; closing unsubscribes. No solver is run or input applied on opening.
- Menu identity, revision, reach and edit validation use the existing contracts. Reach grows with structure size. The title is `gui.icheme.vessel.title.<kind>` rather than always Drum.
- `FluidDeviceScreen` exposes the equipment datasheet for movers as well as reservoirs. A mover's tab is next to Overview; reservoir tab positions are unchanged.
- The waiting-for-identity message and general datasheet hover text are translated. The Pipeline Tool's diameter component/menu/edit path and debugger-only individual-pipe inspection are unchanged.

