# Tank indicator/port integration and client launch

2026-10-24. Owner requests merge and run into claude/floating-roof-tanks-81d68b.

- Source 81cabbe9 contains tank indicator/port change 08c7127e, neutral presentation 031380ad and side-ladder refinement b853fe9f requested during integration. Target remains 35bad9b8, the source's ancestor, with nine modified equipment files plus untracked TmpDiagTest.java. Incoming paths do not overlap them.
- No dev client or test process is running at preflight. Run required quick and slow suites plus jar in the owned source worktree; compare failures to preserved exact-35bad9b8 results from the preceding GUI integration. Replay a baseline only if a failure differs or retained evidence is insufficient.
- Before fast-forward, freeze target/source HEAD, branch, clean index, empty ignored-tracked list and local-file hashes. Require no incoming overlap and no intervening change; merge without stash/reset/staging, then verify all ten local files byte-for-byte.
- Compile/package target with its preserved local files and launch normal runClient there. Leave the client running; do not load a world or automate game/GUI input. No main merge, version bump or process-format change.
- Update canonical batch/index/code-guide snapshots and archive build/test/preservation/startup evidence in tools/tank-indicator-ports/logs. Existing software render/layout checks remain drawing evidence; no repeated campaign or GameTest.

- Initial full checks found a real rear-pipe/compact-ladder overlap and an incorrect port-cell-distance assertion. Move compact access to the right-hand side with adjacent rest-platform sections, retain both pipe lanes, and measure the port-spacing assertion at flange faces. Eight focused checks and expanded pipe-room/layout sweep pass; rerun full quick/slow suites on this final source.

- Final owner steering: main compact access stays on the right-hand side; gauge remains beside the front ports on every size. Source 63ca8f91 removes the small-tank side-gauge exception and extends the port/gauge gate to minimum and normal tanks in every heading. Eight focused checks and the 3108-plan/60-layout sweep pass; 138 local reframes retain world binding via native EquipmentRefresh.plan.
