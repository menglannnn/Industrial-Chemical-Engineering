# Merge review: multi-machine framework, first stage (0.15.0, 2026-10-27)

Branch `claude/distillation-column-framework-922ec9`, 23 commits `660650cd` to `ce71b45c` on main 0.14.1 (`0e697177`, merged in at `e69cec50`). A distillation column built from a saved design and validated against it as an assembly of parts; package data identified by declared revisions. Release prep `7f03b072` (CHANGELOG [0.15.0], `mod_version=0.15.0`, the owner's choice).

## First lane and its fix

- Lane at `7f03b072`: `slowGates` 187 with 1 failure, `ProcessUnitTest.everyNozzleNeckAtItsLargestStandsClear`. F3 sized a side nozzle's room by its design bore, but the gate checks a pipe's largest (0.8 m).
- [NOZZLE_ROOM_GATE_REVIEW.md](NOZZLE_ROOM_GATE_REVIEW.md): the owner chose (C), a column nozzle takes pipes up to the largest its place clears (`67343274`, changelog `ce71b45c`). No layout change, no moved pipe block, no new refusal.

## Final lane at `ce71b45c` (run alone, `lane.ps1`)

| Step | Result | Against 0.14.1's merge lane |
|---|---|---|
| `test slowGates --rerun-tasks` | `test` 1643 in 288 classes, `slowGates` 188 in 18 classes, 0 failures | +2 over the first lane: the bore pin in `ProcessUnitTest`, the nozzle refusal in `PipeLineResizeTest` |
| Junction lines | 33 | identical once `ms=`, `bytes=` and `allocatedMB=` are removed |
| Chain-100 | exact | identical |
| Fluid GameTests | 34/34 (run `multi-machine-merge-final`) | +7: `AssemblyGameTests` 6, a fourth `PipeLayingGameTests` |
| Campaign cold random | clean 975 | same seeds; 833 classed F (was B) |
| Campaign bundled random / defaults | clean 994 / 1000 | same seeds and classes |

- Seed 833, replayed alone: B (trace underflow), "HELD: wall deadline", 14 holds, 9.40505 kg crystal, as the baseline record. Under the campaign's load it took a different hold path (52 s). Timing, not a change.
- Printed lines (8503 against 8494): with timings and object hashes removed, 57 lines move, each from this batch. `AssemblyStoreTest` adds 3 lines. The identity strings (`LegacyNetworkPathPinTest`, `SpineNetworkPathTest` `thermo-identity-v2`, `PilotCryogenicCatalogTest`) carry the declared revisions of `660650cd` and `43e7872e`.
- `FluidFactsCompatTest` reads the schema-4 image, and every older image restates once (energy delta 2.7e-9 J, nothing removed). The checkpoint core record grows by a fixed 25,928 bytes, the same at 1000 and 10,000 islands: the basis records' revision identities.

## Formats

- Facts: fluid facts schema 4 (basis hash retired, basis revision appended; corpus `compat/fluid/schema-4/`). Equipment shape fields 2 and 3 (corpus `compat/equipment/shapes-2.snbt`, `shapes-3.snbt`).
- `layouts.txt` re-recorded at F3: its moved nozzle pipe blocks are what the owner chose with F3 option (A) (plan F3, `COLUMN_NOZZLE_ROWS_REVIEW.md`).
- State: `icheme_assemblies` format 1 (a mismatch drops it; the walk rebuilds it). State format 22 is unchanged.
- Wire, no compatibility work: column protocol "13" (wire schema 17), `equipment-12`, `assembly-2`.
- Material revisions: the eight crude packages kept `crude-regrouped-r1` while their floor went from 50 to 1 kPa in `660650cd`, the commit where declared-revision identities start. No saved identity carries `r1` with the old floor, and older worlds restate once, so there is no bump. The owner may still ask for `r2`.

## Checks outside the lane

- In game, through the MCP bridge: one check per task (README history, `tools/multi-machine-framework/ingame-checks/`). The last check (W7) found two defects, fixed in `23d8a19d` and seen after the fix. Option C is judged by `PipeLayingGameTests.aColumnNozzleTakesNoLargerPipeThanItsPlaceClears`.
- `git ls-files -i -c --exclude-standard`: empty.

## Code guide, tools, changelog

- Code guide: new chapter `machine-assemblies`; updated README, `thermo-materials`, `equilibrium-engine`, `fluid-science`, `fluid-runtime`, `column-v3`, `equipment`, `client-gui`, `instrument-accessories`, `pipe-laying`, `verification`; `process-control` only for its facts schema line.
- Tools, `tools/multi-machine-framework/`: probes, bridge drivers, the review-world NBT tool, in-game evidence, logs `multi-machine-framework-logs-2026-10-27.tar.gz` and `final-lane-logs-2026-10-27.tar.gz`. Nothing was detached from a tracked path.
- CHANGELOG [0.15.0] names every commit, this review and the batch folder.

## Open items (after the merge)

- In-game checks for line resize and for option C's refusal (the owner's). The review world is in `run/saves`.
- 177 of 1,544 swept column nozzles have a limit under 0.8 m. A later off-centre port reuses `NozzleBore`.
- A 15 m column's top outlet in its own head (28 necks, existing). Main's compiler may link two rack lanes through a plain pipe (seen, not reproduced).
