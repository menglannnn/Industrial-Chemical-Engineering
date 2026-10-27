# Cutoff handle-tip flicker

2026-10-24. Owner screenshot reports a flickering cutoff handle tip.

- Work on codex/cutoff-handle-flicker from integration 4ad6dc97 in the owned pipeline-tool worktree.
- Remove coincident shaft/grip/end-cap tip faces. End the shaft inside the grip and grip inside the cap; omit their buried forward faces with the existing Mesh end-mask convention.
- Retain the enlarged handle silhouette, materials, controls, collision and all persistence formats.
- Compile/package only while the integration client runs; no tests or game/GUI actions. Use actual-mesh software renders and a full size/orientation/state sweep of tip faces, areas and exterior bounds against the previous geometry.
- Record review, index and equipment rendering contract; keep offline tools/logs under tools/cutoff-handle-flicker/.
- Deliver the feature branch; no automatic merge, client restart or version bump.
