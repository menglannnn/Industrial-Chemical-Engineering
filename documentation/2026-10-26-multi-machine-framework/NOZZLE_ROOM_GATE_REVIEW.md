# Nozzle rooms at a pipe's largest (final lane, 2026-10-27)

Batch `2026-10-26-multi-machine-framework`, branch tip `7f03b072`. Found by the merge's final lane: `slowGates` 187 with 1 failure, `ProcessUnitTest.everyNozzleNeckAtItsLargestStandsClear`. The quick `test` suite passed. F3 had left `slowGates` to this lane.

## 1. Expected behaviour

Owner decision 2026-10-21: "a 1 m pipe has a 1 m root; other components give way". Any nozzle may take a pipe up to the largest (0.8 m outside, flange 1.0 m). Its neck at that size stands clear of the rest of the shape. On a real column, decks and ladders are laid out round each nozzle's flange; the game lets any bore connect, so the access must leave a pipe's largest room.

## 2. What the code does

F3 (`ef0ae447`) makes the access give way to each side nozzle's room: deck cuts, ladder moves, the davit, and the row search's `ladderMeets` and `severs`. It sizes that room from the nozzle's design bore (`Rooms.neck(PipeSizes.flange(defaultOd) / 2)`: 0.34 m to each side for a 0.4 m reflux), not a pipe's largest (0.55 m, the `REACH` cap). Before F3 the reflux stood in a row away from the access, so this never showed.

Gate shapes: the reflux of a 3 m, 12-tray column and of a 1.5 m, 25-tray column. 0.10 to 0.25 m out, a ladder or rail post stands inside a 0.8 m neck (`tmp/f3-reflux/probe.txt`). Today the largest clear neck is 0.60 m on the 3 m column and 0.75 m on the 1.5 m one.

F3's 315-design sweep (scratch `RoomSweep`, `NeckWhy`):

| Necks not clear at 0.8 m | Current | Rooms at the largest |
|---|---|---|
| Access structure (reflux on 1, 1.5, 2 and 3 m columns, two returns) | 89 | 0 |
| A 15 m column's top outlet in its own head (existing; no gated shape) | 28 | 28 |

## 3. Options for the owner

- **(A) Rooms at a pipe's largest (recommended; follows the 2026-10-21 rule).** Every side nozzle's room is the same constant (`REACH`, 0.55 m to each side). The per-nozzle neck size is deleted from `Rooms.of` and `Rows.side`. Effects in the sweep:
  - 2 m and larger: decks and ladders give way. No nozzle moves, no new refusal.
  - 1.5 m: 13 of 27 buildable designs move nozzles (37 nozzle pipe blocks: reflux 12, vapour return 8, steam 7, others 10). 14 more designs are refused, 22 of 35 against 8, mostly "steam feed 1 has no free row". Placed 1.5 m columns of those designs refresh and move those pipe blocks.
  - 1 m and below: unchanged.
  - F3's checks hold: walks parted 0, deck and mast clashes 0, the one existing 1 m cage clash only.
  - The layout table re-records. Moved nozzle pipe blocks need your approval (AGENTS.md).
- **(B) Accept the touches.** Add "columns left alone" entries to the gate (reflux at 1, 1.5, 2 and 3 m; return at 1.5 and 2 m). No layout change. A pipe over about 0.6 m on a slim column's reflux draws its neck through a ladder or rail. The pipe's block stays free. This makes an exception to the 2026-10-21 rule.

## 4. The owner's hint: off-centre connections (2026-10-27)

- What exists: off-centre points at rack lanes only (transition pipes). At equipment, port offsets and the per-nozzle bore limit (pipe-laying plan section 6, D12) were not built. Every nozzle takes up to 0.8 m.
- Measured (scratch `ShiftProbe`, the 89 necks): on their centre line they clear 0.30 m (1 m reflux, 12 necks), 0.75 m (1.5 and 2 m, 52) and 0.60 m (3 m reflux, 25). The best in-cell shift under the off-centre limit (flange on its own face: max(|du|, |dv|) + flange/2 ≤ 0.5 m) raises the mean only from 0.647 to 0.662 m: 0.60 to 0.65 m on 20 of the 3 m necks, 0.30 to 0.45 m on 2 of the 1 m necks. The limit takes bore away as fast as the shift gives room.
- What the off-centre design offers here is its limit. **(C) A nozzle's own largest bore:** a column nozzle takes a pipe up to the largest its place clears (computed with the layout, never saved). The laying tool refuses a larger line, the label says "fits up to 0.60 m", and a line resize stops at it. The gate checks every neck at its own largest, so the column entries of its accepted-touch list go. No layout change: no nozzle or pipe block moves, no new refusal. 89 of 1,094 side nozzles in the sweep get a limit under 0.8 m. A later off-centre nozzle reuses the same limit. It replaces "other components give way" with "the nozzle takes what its place allows" for columns.

## 5. Decision and implementation (2026-10-27)

The owner chose (C). `67343274`, changelog `ce71b45c`:
- `science/equipment/NozzleBore` computes each limit from the shape: the gate's ring rule on `DeterministicMath`, cached per shape. A bore that draws at the largest outside reports `MAX_BORE`.
- The world checks:
  - `EquipmentShellBlockEntity.largestBore` / `largestBoreBeside`.
  - `PipeAnchor.Refusal.NOZZLE_SIZE`.
  - The label's "fits up to".
  - `PipelineToolItem`, single pipe and line.
  - `PhysicalRegistry.resizeLine` takes a per-pipe limit, `NOZZLE_TOO_SMALL`.
- The gate's limits:
  - Gate columns: 3 m reflux 0.55 m, 1.5 m reflux 0.7 m, 0.5 m side nozzles 0.25 m. The 3 m top outlet keeps a pipe's largest and its touch.
  - Sweep: 177 of 1,544 nozzles under 0.8 m. The search takes about 2 ms per nozzle, cold.
- Gates:
  - `ProcessUnitTest` 23/0, with the new pinning test.
  - `PipeLineResizeTest` 8/0.
  - Fluid GameTests 34/34 (run `nozzle-bore-c2`). The first run failed: the test's 3-tray 0.5 m column places no feed, so the test uses the reflux.

The final lane re-runs once after the choice: chain-100, GameTests and the campaign did not run, because the lane stops at the gates.
