# pipe-laying-system

Purpose: the probes and gate logs of the pipe laying batch's last fixes and merge preparation. It holds the route probe that found the second-click bug, the consolidation digest probe, and the final gate logs. The renders and the in-game rig of the same batch are in `../pipe-laying-renders/` and `../pipe-laying-ingame/`.

Batch: `documentation/2026-10-26-pipe-laying-system/` (`RACK_FLANGES_AIM_PLAN.md` sections 5 and 6, `CONSOLIDATION_REVIEW.md`, `MERGE_REVIEW.md`). Nothing here was detached from a tracked path, so no re-attach patch is needed.

## How to run

The probes are JUnit classes in package `com.wormzjl.icheme.science.equipment`. They are run from the test source set and are not gates. From the worktree root:

```
cp tools/pipe-laying-system/route-probe/RackClickProbeTest.java src/test/java/com/wormzjl/icheme/science/equipment/
./gradlew test --tests com.wormzjl.icheme.science.equipment.RackClickProbeTest -i
rm src/test/java/com/wormzjl/icheme/science/equipment/RackClickProbeTest.java
```

Read the printed `TALLY`, `KINDS`, `TIME us`, `WORST us` and `SLOW` lines. Never commit the copy.

The digest probe runs the same way (`consolidation/ConsolidationDigestTest.java`, test `com.wormzjl.icheme.science.equipment.ConsolidationDigestTest`). It writes `tmp/consolidation-digest/last.txt`. Run it at the start commit and after the last change, then compare the section hashes and the `overall` line (the times in brackets vary).

## Entry points

- `route-probe/RackClickProbeTest.java`: the client's `PipeRoute.search` against the server's `check` and `blocked`. It tries every aimed pair of clicks on a 10-cell straight rack run, with 2", 6" and 14" lines (16 400 pairs) and both halves of each cell. It prints:
  - per outcome: the count, the total and worst search time;
  - the routes that pass a cell twice, by click layout;
  - the searches over 2 ms, by layout.
  - The gate `PipeRouteTest.everyAimedRouteAlongARunIsOneTheServerLays` holds its main property: every route found is one the server lays.
- `consolidation/ConsolidationDigestTest.java`: SHA-256 digests, in 12 sections, of:
  - the rack geometry (frames, lanes, fits, aims, picks, placement, steel);
  - the line geometry (plain pipes, devices, transition pipes);
  - the route search (cells, options, refusal, and the server's check of each found route).
  - `before.txt` (at `9b5f1fcb`) and `after.txt` (at `71ad53ef`): 1 348 749 items, overall `bf89be369ae600a5` in both.
- `logs/pipe-laying-system-logs-2026-10-27.tar.gz`:
  - `merge-prep/gates/`: `test slowGates`, the fluid GameTests and the route probe at `71ad53ef`;
  - `consolidation-digest/`: the per-commit test logs, the quick suite and GameTests of the consolidation, and the test method lists before and after (with `test-names.awk`);
  - `rack-build/`: the test logs of the junction model, sturdy frame, flange and aim builds.

## History

- 2026-10-27 (commit `43ad8063`, owner report "right-clicking the rack 2nd time with the tool often cancels the ongoing placement"): the probe found 4170 of 13702 routes passing back through their own start cell, all refused by the server. After the fix: 12484 laid, none refused by the server, worst refusal 3 ms (76 ms before the approach-cell rule).
- 2026-10-27 (commit `eeb0742f`, owner report "you cannot place single-block pipe on the rack"): the same probe counted the one-cell pairs: laid 12484 → 12704; the 1200 one-cell pairs on two slots now refuse with `ONE_SLOT`.
- 2026-10-27 (consolidation `32de136e`..`71ad53ef`): the digest probe matched before and after. The route probe at `71ad53ef`: laid 12704, `BLOCKED` 2496, `ONE_SLOT` 1200.
