# Cutoff handle-tip flicker review

2026-10-24. Implementation 6f2f91e9 / delivery e518862d, codex/cutoff-handle-flicker, from integrated 4ad6dc97. Integration pending.

- Expected: a stable dark cap closes the enlarged handle tip.
- Cause: steel shaft, yellow grip and dark cap all ended at the same axial station and each drew a forward face. Their three coincident surfaces competed in the depth buffer, matching the screenshot.
- Fix: shaft ends at 54% of lever length inside the grip (starts at 48%); grip ends at 96% inside the cap (starts at 94%). The two buried forward faces are omitted using Mesh START/END masks. Only the cap closes the outer tip; no depth-offset patch.
- Heavy handle bounds/materials, orientation/open/closed behavior, ordinary pipe collision, controls and persistence are unchanged. No version/format change (mod 0.11.2, checkpoint 18/unit 11).
- compileJava jar and material verification pass with four workers. No tests or game/GUI input while the integration client runs; client/build in the target worktree remain unchanged.
- Actual-mesh sweep: 61 drawn OD codes (4..64), six facing signs, open/closed and installed/item geometry = 1464 combinations. Previous 4ad6dc97 geometry has three tip layers in all 1464; repaired geometry has only BOLT cap faces in all 1464.
- Tip face area equals the cap rectangle in every case, worst relative error 3.28e-15, and all 1464 exterior bounds match the original exactly.
- Before/after software closeups reproduce the competing-colour speckle and show a clean repaired cap. Reviewed the paired closeup and 18-case size/axis/open-closed sheet. Static software evidence establishes geometry; in-game temporal confirmation remains manual.
- Tools: tools/cutoff-handle-flicker/ with actual-mesh sweep, depth-tested renderer and baseline source (4ad6dc97, class-name-only rename). No probe added to product source or gate suite.
- Equipment/verification code-guide addenda and batch/tools indices updated. Logs compressed under tools/cutoff-handle-flicker/logs/. No merge or client restart.

Integration update 2026-10-24: included in `35bad9b8` on `claude/floating-roof-tanks-81d68b` with the fluid GUI batch. Combined quick/slow suites ran with four failures identical to baseline `b52732c9`; target jar passes and client is running. See the fluid-gui-consistency MERGE_REVIEW.md.
