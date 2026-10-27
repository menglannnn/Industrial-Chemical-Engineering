# Pressure accessory and equipment-arrow integration

2026-10-24. Owner request: merge and run. Target claude/floating-roof-tanks-81d68b in .claude/worktrees/rename-ichem-mod-30e50c.

- Fast-forward b70e92aa to 4ad6dc97, including equipment-arrow/pump-joint commits 7cb42268/2fe26e21 and pressure accessory/heavy-lever commits dc174f27/4ad6dc97.
- Run required test and slowGates in the clean owned source worktree, client closed; compare failures with the recorded b70e92aa merge baseline. Fix any new failure before integration.
- Capture all ten local target files. Nine have no source overlap and must remain byte-for-byte unchanged. ProcessUnit contains disjoint existing local instrumentation/heater/clearance changes and incoming pump-model changes; a read-only three-way preflight is clean.
- Use a named stash for ProcessUnit only, fast-forward, then apply the exact stash object. Verify the combined file against the preflight and all other file hashes/status. Retain the restored named stash as a recovery backup. Commit none of the target local edits.
- Verify no tracked ignored files. Build/package the integrated target with all local edits retained.
- Update both batches, index rows, tools and affected code-guide snapshots to the integration commit; retain compressed evidence under tools/pressure-valve-accessory/logs/.
- Launch runClient from the target worktree and leave it running. No world opening, automated game/GUI actions, main merge or version bump.
- Fluid formats stay 18/11; standalone pressure blocks were removed. Fresh-world gameplay checks remain with the owner.
