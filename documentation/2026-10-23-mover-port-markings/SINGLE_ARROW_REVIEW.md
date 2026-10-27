# Single arrow set — review

2026-10-23; code e612cff3, delivery 763939e8, branch codex/mover-port-markings.

1. Expected: one longitudinal set of directional arrows at a machine connection, on the adjoining pipe section; no words and no second set behind the flange.
2. Previous code drew independent arrow sets on the pipe and on the equipment nozzle neck. The owner's split-case pump screenshot showed both sides of the same flange carrying a set.
3. Owner requested removal of the duplicate layer. Removed the entire equipment-side decal path and its unused nozzle geometry builder. EquipmentShellModel now uses its original surface path. The adjoining pipe arrows and static role metadata retain their sizing, role direction and four radial viewing sides.

- Simplified the remaining decal frame to its pipe-plane axes, removing the now-unused nozzle angle parameter.
- Final jar --max-workers=4: PASS; git diff --check clean. No tests added/run, no automated GUI/in-game check; current integration client left running.
- Regenerated the 24-case software matrix (four machine variants, four headings, drawn OD extremes): every case now reports 0 equipment-nozzle marker polygons and 32 adjoining-pipe marker polygons. All 52 views and the preview sheet replaced with the single-set version. Owner's reported split-case discharge view was visually checked.
- Current contracts are CODE_GUIDE_ADDENDUM.md and owned equipment/client/runtime guide addenda, snapshot 763939e8. Initial PORT_MARKINGS_REVIEW.md describes the superseded two-sided draft.
- Tool cleanup: existing canonical tools/mover-port-markings renderer updated to omit equipment decals. No new product harness/task/asset. Build and rendering output archived in logs/single-arrow-logs-2026-10-23.tar.gz.
- Delivered on the existing unmerged feature branch. No integration/main merge, client restart or version bump.

Integration follow-up: 2026-10-23 — owner-requested fast-forward merged this refinement with the fluid GUI batch at `232b0466`; all 10 local files preserved, target jar passed, regular client launched (PID 49688). No tests or version bump.
