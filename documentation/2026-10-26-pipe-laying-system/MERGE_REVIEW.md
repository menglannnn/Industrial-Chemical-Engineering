# Merge review (batch 2026-10-26-pipe-laying-system)

Date 2026-10-27. Branch `claude/pipe-laying-system-092ac8` (worktree `wonderful-banach-1f1152`), base `main` = `600549d9` (0.13.2), main unchanged since the branch began. Owner request: "consolidate and prepare for merge". The owner waived the final lane ("No need to do the final test, I'll do a manual test before merge preparations"). The manual test found four defects, all fixed on the branch: `131857a3` (2x3 groups, colour selector loop), `43ad8063` (second click dropped the start), `eeb0742f` (one-block rack lines refused), `9b5f1fcb` (a one-block lane drawn only after a reload).

## What merges

The pipe laying tool and the pipe racks: W1 explicit pipe joints; W2 the tool (route search, ghost, one ledger event per lay, one-block dismantle); W3/R1-R4 the layered rack; W4 transition pipes; line paint; the junction model and sturdy frame; lane flanges, facing by the look and slot aim on the tier; the fixes from the manual test; the consolidation (`CONSOLIDATION_REVIEW.md`). Commits `69c5ebf8` to `9b5f1fcb`, the consolidation `32de136e`..`71ad53ef`, the CHANGELOG `b393d8ea`, `fd570b0d`.

## Formats and protocols

- **Fluid facts:** schema 1 → 2 (explicit pipe joints), corpus `compat/fluid/schema-2/`; the schema-1 images still read.
- **Pipe block entity:** fact fields `Joints` (absent: every face, the behaviour before), `Paint` (absent: bare) and `Secondary` (absent: the paint); corpus `compat/pipes/pipe-1.snbt`, `pipe-2.snbt`.
- **Rack block entity (new):** lane copy format `rack-3`, fact field `Secondary` (absent: the paint); corpus `compat/racks/rack-1.snbt`, `rack-3.snbt`. `rack-1` was redefined in place by RD9, and `rack-2` was an earlier removed label; both were written only on this branch.
- **Wire:** registrar `pipeline-tool-1` → `pipeline-tool-2`, payloads `pipeline_tool_save` (now with the paints), `pipe_laying_open`, `pipe_lay`, `pipe_lay_answer` and `pipe_dismantle`. `fluid-14` and the other registrars are unchanged.
- **New ids:**
  - item `pipe_laying_tool` (survival too, one `fluid_pipe` item per cell, D1; in the creative tab, no recipe; the old `pipeline_tool` stays the creative-only sizing tool, D2);
  - block, item and block entity `pipe_rack`;
  - data components `pipe_laying_settings`, `pipe_laying_paint`, `pipe_laying_secondary`.

## Tool folders

`tools/pipe-laying-renders/` (software renders and the clash check, in use), `tools/pipe-laying-ingame/` (the bridge rig, concluded 2026-10-26), `tools/rack-slot-clearance/` (superseded by the layered rack), `tools/pipe-laying-system/` (new: the route probe, the consolidation digest probe and the gate logs). Nothing was detached from a tracked path, so no re-attach patch is needed and no CHANGELOG line for detached code.

## Code guide

New chapter `pipe-laying.md` (client, anchors, route search and server check, lay and dismantle, rack geometry, placement, steel, flanges, lane copy, fluid side, paints, formats, owner decisions, traps). Updated `README.md`, `fluid-runtime.md`, `equipment.md`, `client-gui.md`, `instrument-accessories.md` and `verification.md`; snapshot lines `71ad53ef`, to be set to the merge commit at the merge. Every cited name was checked against the code.

## Gates

At `71ad53ef` (the CHANGELOG commits after it change no code), logs in `tools/pipe-laying-system/logs/pipe-laying-system-logs-2026-10-27.tar.gz`:
- `./gradlew test slowGates`: BUILD SUCCESSFUL in 3 min 36 s. `test` 270 classes, 1520/1520 (up to date from the consolidation's run on the same inputs); `slowGates` 18 classes, 187/187.
- `runFluidGameTestServer` (run id `merge1`, folder deleted): 26/26.
- Route probe (`tools/pipe-laying-system/route-probe/`): laid 12704, client refuses `BLOCKED` 2496 and `ONE_SLOT` 1200, none refused by the server.
- Consolidation digest: 12 sections bit-identical before and after (`CONSOLIDATION_REVIEW.md`).
- `git ls-files -i -c --exclude-standard`: empty.
- Final lane: waived by the owner (manual test instead). No dev client ran during the merge gates.

## Version and merge

`0.14.0` (minor: a feature batch), the owner's choice. Release prep `4a7c807b`; `main` fast-forwarded `600549d9` → `4a7c807b` on 2026-10-27, not pushed.

## Open after the merge

- The owner's manual test of what was not checked in game: the last three fixes, and the parts listed in the batch README.
- D13 (the spacing rule) can be reviewed.
- The owner proposals in `CONSOLIDATION_REVIEW.md`.
- Facts schema clash: `codex/process-control` (`8523595a`, also from `600549d9`) sets `FluidFacts.SCHEMA=2` with other fields (geometry, modular control) and its own `compat/fluid/schema-2/` image. The branch that merges second takes schema 3 and a `schema-3/` image, and keeps the first branch's schema 2 readable.
