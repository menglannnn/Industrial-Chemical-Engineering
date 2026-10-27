# Final lane on `e8ec6455` (Opus, 2026-10-26), `claude/floating-roof-tanks-81d68b`, not merged or pushed
Merge prep after the consolidation (`CONSOLIDATION_REVIEW.md`). Reference: fl8 at `c7b883cb`, main's last lane (`FINAL_LANE_C7B883CB_REVIEW.md`; the 0.11.x merges since then changed no solver code). Lane: the m23 scripts pointed at this worktree, with the test step changed to `test slowGates`. Logs: `tools/floating-roof-tanks/logs/final-lane-e8ec6455-2026-10-26.tar.gz`. Another project's simulation (2 threads) ran on the machine, so timings are indicative only.

## Verdict: green, and equal to fl8 on every held seed

## 1. GameTests were broken on the branch (fixed first)
- The lane's first run at `dfad3366` stopped after 40 min in the GameTests (main: 48 s). 4 tests had failed, and `fluid-solids` was waiting out its 200 000 ticks.
- Hidden because the `fluidGameTest` source set had not compiled since `dc174f27` (24 Oct). It named the removed pressure-control-valve block, and later batches ran `jar` or unit suites only.
- Causes and fixes. All are test-side; no game code changed:
  1. `0f208c52` places generators switched Off, so the tests' tanks never charged. `TankCharge.run` now sets the feed and switches the generator on in one queued edit, as a player does; 6 tests use it (`c1d44b9b`).
  2. `49e57860` queues the chunk-load binding and marking as a server task. The identity test read the mark in the same tick; it now reads it on the next tick, before the bucket. The update tag now carries the pipe's accessories (`AccessoryRevision`, `Instruments`), which are drawn state, not process data (`0be3ef64`).
  3. The identity test's leftover reservoir failed the unloaded-chunks test's empty-world check (fixed by 2).
  4. The removed block's name in the movement test (`6fe81478`).

## 2. Gates (one run, alone in the Gradle sense)
- `test slowGates`: 1606 tests, 0 failures (quick 1419 plus slow 187). Chain-100 exact: deviation 0. Fluid GameTests: 19 of 19 in 38 s.
- Printed lines against fl8:
  - The physics values are unchanged on every line kept: the junction lines (`LIQUID_JUNCTION`, `MIXED_GAS_TRANSIENT`), `MIXED_GAS_COST` (299 Newton solves), the V3 columns and the extreme topologies.
  - Only timings and allocations moved, apart from the checkpoint sizes below.
  - Checkpoint sizes: 259 bytes per island unit (was 258), core record 30 bytes smaller. These are the branch's fluid formats (generator switch, pipe accessories); fresh world.
  - Lines on one side only: tests archived by the 0.11.1 test review, the `createcheme:` → `icheme:` namespace, and new tests (`OpenTankTest`, the tank layout timing).

## 3. Campaign, 1000 seeds, 8 workers
| Distribution | fl8 (`c7b883cb`) | `e8ec6455` |
|---|---|---|
| cold random | 975 | 975 |
| bundled random | 994 | 994 |
| bundled defaults | 1000 | 1000 |
- Against fl8: the same held seeds, with the same classes and first ticks, in all three; nothing newly held or cleared; no new C or F hold.
- Counters:
  - cold: Newton iterations 3.036 M (fl8 3.055 M, −0.6 %); equation-gate rejections 2801 (2789).
  - bundled: 0.929 M (0.930 M); defaults identical.
- Wall time, indicative only: cold −1.6 %, bundled equal.

## 4. Also in this merge prep
- Code guide: 7 chapters plus README folded and checked against HEAD by an Opus thread; new chapter `instrument-accessories.md`. The snapshot lines read `<MERGE>` until the merge commit exists.
- CHANGELOG: three missing batch lines (pump inlet adapter, pipe flange spacing, pipe end hitboxes), one Changed section, and the GameTest line. Two comments corrected (`EquipmentStructure.reregister`, `LineGeometry` flange spacing) in `e8ec6455`.
- `git ls-files -i -c --exclude-standard` is empty. No build-script changes, and no probes left in `src/`.

## Open for the owner
1. Merge (fast-forward of main to this branch) and the version: 0.12.0 proposed (new features: floating-roof tanks, sphere, pipe accessories, working pumps and compressors), or 0.11.3.
2. Optional: make the quick suite compile `fluidGameTest`, so a dangling reference shows on the day it lands (a build change).
