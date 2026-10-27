# Pipeline check-valve integration plan

2026-10-24. Owner requested "merge and run". Source codex/pipeline-check-valve at b70e92aa, implementation 4e5c504d / arrow correction 9ffb294f. Target claude/floating-roof-tanks-81d68b at cb89f23a.

- Run required test/slowGates/jar with no active dev client. Inspect all failures against the five known baseline methods; fix any new feature regression before merging. No second solver campaign or automated GUI/GameTest lane is required for this integration.
- Snapshot SHA-256/status for all ten existing local equipment/test files. Reject overlapping merge paths, fast-forward from the owned feature branch, and verify every local file byte-for-byte afterwards. Do not stash/stage/commit local changes; require ignored tracked-file checks to be empty.
- Update actual code-guide integration snapshots, batch review/index and tooling records from the merging source. No merge to main or version bump (0.11.2).
- Build the target jar with its local equipment edits, then launch ordinary runClient there and leave it running. No tests overlap the client. Confirm resource/window startup without opening a world or sending desktop input.
- Checkpoint 18/unit 11 requires a fresh world. The owner was informed that opening incompatible format-17 simulation data invokes the existing reset and loses fluid state; do not load the owner's world to test it.
- Archive merge-gate XML/logs, preservation manifests, target package/startup logs and final source metadata under tools/pipeline-check-valve/logs/.
