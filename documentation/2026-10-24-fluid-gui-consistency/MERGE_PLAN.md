# Fluid GUI integration and client launch

2026-10-24. Owner authorizes merge and run into the established `claude/floating-roof-tanks-81d68b` worktree.

- Source GUI implementation/delivery: f44930f5 / 7f0e65ed, including unmerged cutoff-tip fix 6f2f91e9.
- Integration tip is now b52732c9, with pipe hitbox/flange spacing and pump-casting work after 4ad6dc97. Merge that tip into the owned feature first; combined source is 35bad9b8.
- Target has nine modified equipment files plus untracked TmpDiagTest.java. Incoming paths do not overlap them. Before fast-forward, freeze hashes/status and require HEAD/branch/no-indexed-changes/empty ignored-tracked list; afterward verify all ten local files byte-for-byte.
- Run required quick test and slowGates, plus jar, on combined source with the dev client closed. Replay any failures in an isolated managed baseline checkout at b52732c9; no test edits or solver changes to make unrelated failures pass.
- Fast-forward integration to the checked source after qualification, preserving local files; compile/package the target and launch its normal runClient lane. Do not load/mutate a saved world or automate GUI input.
- Record counts, baseline qualification, preservation evidence and startup log. Update canonical batch/index/code-guide snapshots and compressed tools/fluid-gui-consistency/integration evidence. No main merge or version bump.
