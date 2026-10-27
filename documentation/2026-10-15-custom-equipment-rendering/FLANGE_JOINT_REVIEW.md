# Flange joint review: a pipe wider than a nozzle (2026-10-20)

Batch `2026-10-15-custom-equipment-rendering`, owner items 1 and 2 of `MERGE_PREP_REVIEW.md`. Branch
`claude/competent-franklin-43848a` from main `2e28a20a` (0.10.0). Not merged, not pushed. Code guide: no change (comments only).

## Item 2: stale javadocs (done)
- `bf38dee9` (comment only, compileJava green with `-Xshare:off`), `0451759e` (CHANGELOG `[Unreleased]`).
- Kind lists in `EquipmentShape`, `ProcessUnit`, `UnitDesign`, `EquipmentConstructorScreen` now follow the code: 22 unit
  kinds registered in `UnitDesign.DESIGNS` (8 of 2026-10-16/17, the owner's eleven of 2026-10-18 as fourteen kinds).
- Ladders: `Geometry.climbs()` is a column's (`ColumnStructure`) or any unit's (`ProcessUnit.ladders`); a drum has none.
- `boot` under `vertical`: the command tree registers it for both orientations; `EquipmentShape` refuses it ("Only a lying
  drum has a boot"). Documented. Optional code change (not made): register `boot` under `horizontal` only.

## Item 1: flange mismatch

**Physics.** A bolted joint is two mating flanges of one size and class: same OD, bolt circle and bore. Where a line is
larger than the nozzle, one concentric reducer takes the step, on one side of the joint only.

**What the code does.** Yes, a pipe wider than a nozzle's `maxOd` shows two flange sizes at one joint.
- Pipe side, `FluidDeviceBlockEntity.computeFaces`: the face code is `min(nozzle.maxOd(), outside(bore))`. In the pipe's model
  (`LineGeometry` end: `met < od` gives a reducer) the pipe narrows in its own block and flanges at `maxOd`.
- Vessel side, `EquipmentStructure.refreshNozzles` then `ShapeRasterizer.nozzle`: the flange is at `outside(bore)`, the pipe's
  size, and the neck widens to it from `maxOd` in its own reducer (owner decision 2026-10-16, "never at the flange").
- So the line runs OD, then down to `maxOd` (pipe reducer), up to OD at the face (vessel flange), then down to `maxOd`
  (vessel reducer). Two reducers and two flange sizes face to face.
- Example: a 1 m drum, vapour outlet `maxOd = min(1, D/2) = 0.5 m`, a 24" line (OD 0.61 m). The pipe flange is
  1.25 × 0.5 + 0.085 = 0.71 m (one texel). The vessel flange is 1.25 × 0.61 + 0.085 = 0.85 m (two texels, OD > 0.6).
- Cause: the clamp came in with `d1701780` (2026-10-16 08:27). `91fcb448` (12:21 the same day) moved the reducer to the
  vessel but left the pipe's clamp. Any nozzle whose `maxOd` is under a connected pipe's OD shows it: drums under 2 m with
  a large line, and small unit nozzles.

## Proposition for the owner
- **A (recommended, keeps the 2026-10-16 decision).** In the pipe stage, `computeFaces` flanges at the pipe's own size at a
  vessel nozzle: `code = PipeSizes.code(PipeSizes.outside(bore))`. Both sides then read the same `bore` through the same
  `PipeSizes.code(outside(...))`, so the codes are equal by construction. This deletes the pipe's `maxOd` read (the vessel
  alone owns its resize) and the pipe's reducer at nozzles. Gate: a pipe-block test, a pipe wider than `maxOd` at a nozzle
  gets the face code that `cellCodes` gives the nozzle. Risk: none known; drawing only, no format change.
- **B (refinery practice, reverses the 2026-10-16 decision).** The nozzle is the vessel's size and the line reduces to it
  on the piping side. `refreshNozzles` clamps `outside` to `maxOd`, so the flange pair is at `maxOd`. This deletes the neck
  reducer branch in `ShapeRasterizer.nozzle` and `inNozzle`. Risk: the step moves into the pipe's block (reducer
  0.8 OD, capped at 0.3 m by `reducerLength`), and a nozzle no longer shows the line's size; it overturns an owner decision.
- Not implemented: drawing change waits for the owner's word. Either form is judged by gates, the sweep and software renders.
- Docs: this file is in the worktree's git-ignored `documentation/`; copy it to the main checkout's batch folder and add
  the `INDEX.md` file row (the session's hook blocks writes outside the worktree).
