# Branch resume review (2026-10-25)

Branch `claude/floating-roof-tanks-81d68b`, worktree `rename-ichem-mod-30e50c`. Phase-2b work stopped on 2026-10-21 (agent rate limit) on top of `1a806141`. Since then 85 commits were fast-forwarded in: HEAD is `f658697c`.

## What landed since `1a806141`, and how it affects this batch

| Area | Commits | Effect on the nozzle and skirt work |
|---|---|---|
| Square flanges | `acd0fd14` `fa81579b` | `hubOutside(od) = od`: the hub frustum is now a straight neck. `NozzleRoom` still subtracts `hub(od)` (harmless). |
| Pump castings | `b52732c9`, `7cb42268` | `ProcessUnit.nozzleRoots`: the pump and recip-compressor castings blend into the pipe-sized neck. Their own Class 300 flanges are deleted. Consistent with "a 1 m pipe has a 1 m root". |
| Movers functional | `4e96e8ab` ff. | Pumps and compressors register as fluid movers (suction is the fluid cell). Their nozzle layout is now load-bearing: moving it changes port cells. |
| Inline mover / valves / sight glass / gauges | many | Pipe accessories, sized from the pipe's OD (`outsideOf(code)`). `InlineMoverGeometry` casing radius is 0.78 OD + 0.04. |
| Pipeline tool | `d30694ed` `459eda31` | Creative tool, bore 0.001–1 m; placed pipes inherit the largest neighbour bore. |
| Tank layout | `08c7127e` `b853fe9f` `63ca8f91` `fde01628` | Bottom nozzles centred front and back, overflow front; gauge always at the front beside the ports; small-tank ladders on the right side. Replaces my 2026-10-21 gauge placement; the gauge is still on the shell, reaching grade. |
| Fluid formats | several | Checkpoint 19 / unit 12 / fluid-14; incompatible saves reset. No effect on equipment shapes. |

**Uncommitted in the tree** (stopped phase 2b, 2026-10-21; preserved through the merges):
- Reasoned layout moves in 9 files: heater fuel ring under the mufflers, coker headers under the slab, grate steam drum line over its rail, hydro top run over its deck rail, trickle-bed quench face at `ProcessUnit.NECK`, compressor slab opening and lube skid, condensing-turbine steam elbow.
- Temporary debug hooks (`DEBUG-NOZZLE-ROOM` in `ProcessUnit`, `ShapeRasterizer`; `TmpDiagTest`). These go before the commit.
- Codex's stash `codex-pressure-valve-merge-2026-10-24-ProcessUnit-local-edits` equals the `ProcessUnit` part of this tree. It is applied; leave it for its owner to drop.

**Running:** a dev client (PID 25812) from this worktree. No Gradle runs until it closes.

## Owner changes 2026-10-25, and the plan

1. **Pipe cap 0.8 m OD.** `PipeSizes.MAX_OUTSIDE = 0.8`, so a flange is ≤ 1 m (flange(0.8) = 1.085, held at 1). Changes:
   - Add `MAX_BORE = MAX_OUTSIDE / 1.1` (0.727 m). It bounds the Pipeline tool, `FluidDeviceBlockEntity` NBT, and `code()`.
   - Replace the hard-coded 1 m caps on nozzle sizes (`Math.min(1, …)` in `EquipmentShape`, `CondensingTurbine`, coker, EB, trickle bed, fluidized bed, fixed-bed gasifier, `ColumnStructure.size(…, 1)`) with `MAX_OUTSIDE`.
   - Every room rule written for `MAX_OUTSIDE` shrinks with it.
2. **Lower minimum skirt.** Move the bottom line to row 0.5 m (`SKIRT_OUTLET`). It is the lowest cell centre; a 0.8 m line's bottom is then 0.1 m over grade. Minimum = ceil(0.5 + 0.8 + 0.05 + D/4):

   | D (m) | Old minimum (m) | New minimum (m) |
   |---|---|---|
   | up to 2.5 | 3–4 | 2 |
   | 3–6.5 | 4–5 | 3 |
   | 7–10.5 | 5–6 | 4 |
   | 11–14.5 | 6–7 | 5 |
   | 15 | 7 | 6 |

   The 0.8 m port's collar (radius 0.48 m) dips 0.04 m into the base ring: an accepted overlap. The 1 D elbow keeps the line drainable from the pole. A horizontal nozzle on the head would leave a sump.
3. **Rotatable bottom outlet.** Add a design field `outlet` (0–3 quarter turns from the front). It goes the way the boot flag does: Design, `EquipmentNetwork`, constructor NBT, shell NBT, shape record.
   - Applied to a standing drum's skirt outlet, its skirt port and collision, and the trickle-bed effluent and its sleeve.
   - Columns stay at the front (owner: columns left alone). Side connections and the manway check against the turned outlet.
   - The constructor GUI gets a rotate button.
4. **Boot drum's second outlet behind the boot** (Kanes Dictionary boot diagram, photo survey A). The hydrocarbon outlet sits on the shell's bottom centreline between the boot and the outlet-end head, facing down, behind an overflow baffle.
   - With a boot, `LIQUID_OUTLET` leaves the front head and moves there.
   - `bootFor` places the outlet first, inboard of the far saddle (its 0.8 m flange 0.15 m clear), then the boot ahead of it, two cells or more apart (pipe spacing rule).
   - No room means no boot. The engine is unaffected: ports carry a phase class, not a height.
5. **Phase 2b resumes at 0.8 m.** Re-run the neck gate; fix visible clashes by moving parts; keep one reasoned accepted list (replacing `MET_BEFORE`); remove the debug hooks.
6. **Gates and quick suite, then commit.** The owner tests in game; no client test by agents.

Code guide: to update at merge (equipment chapter: `PipeSizes`, skirt outlet, boot outlet, design field `outlet`).
