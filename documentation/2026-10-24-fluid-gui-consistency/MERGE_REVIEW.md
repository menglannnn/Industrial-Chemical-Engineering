# Fluid GUI merge and client review

2026-10-24. Owner requested merge/run. Integrated `35bad9b8` into `claude/floating-roof-tanks-81d68b`; mod 0.11.2 retained, no main merge.

- Combined feature `7f0e65ed` with current integration `b52732c9` in the owned worktree first. This includes GUI implementation `f44930f5`, cutoff-tip fix `6f2f91e9`, and the integration's newer pipe/pump geometry.
- Required quick suite: 1382 tests, 1379 passed, three failures. Required slowGates: 185 tests, 184 passed, one failure. Jar/resource/material verification passed.
- Baseline replay used a separate managed checkout of `b52732c9`, only the four failed methods. Each reproduced the identical assertion message. No new failure; no test disabled, threshold changed or solver fix added.
- Existing quick failures: FluidCheckpointCodecTest expects format 13 rather than 18; ShapeRasterizerTest expects no pump shell cell; DeterministicMathSourceTest flags existing SightGlassDisplay Math.pow calls. Existing slow failure: ProcessUnitTest largest-nozzle clearance sweep.
- The 37 feature-focused checks are included in the quick run and pass. The updated protocol assertion now correctly pins fluid-14; checkpoint 18/unit 11 remains unchanged.
- Nonoverlap preflight froze HEAD, branch, clean index, ignored-tracked list and all nine modified equipment files plus untracked TmpDiagTest.java. Fast-forward performed without stash/reset/staging. All ten local files match their before hashes byte-for-byte; owner edits were not committed.
- Target compileJava/jar passed with the preserved local files. Normal runClient launched from the integration worktree; Java client PID 35128, session 24502. Renderer/assets, recipes and advancements loaded. Client left running; no automated world loading, GUI interaction or desktop input was performed.
- Fresh GUI/gameplay checks remain the owner's task. This is integration qualification with four proven baseline failures, not a green full-suite claim.
- Temporary baseline worktree archived after copying results; owned feature worktree retained. `tools/fluid-gui-consistency/integration/` holds prepare/apply preservation script/manifests, source/baseline XML and comparison, build/startup logs; compressed integration archive in its parent logs/.
- Code-guide README/client-gui/fluid-runtime/verification/equipment snapshots and documentation/tools indexes updated to this merge. No temporary product instrumentation or undocumented tracked harness was introduced.
