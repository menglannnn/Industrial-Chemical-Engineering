# Pipeline check-valve integration review

2026-10-24. Owner requested merge/run. Fast-forward cb89f23a -> b70e92aa on claude/floating-roof-tanks-81d68b; source codex/pipeline-check-valve (4e5c504d / facet-arrow fix 9ffb294f). No main merge/version bump; mod 0.11.2.

- Required merge gates completed with no client active: quick 1372 tests/four known failures; slow 185/one known failure. Failure methods exactly match the baseline checkpoint/protocol expectation, pump node, SightGlassDisplay Math.pow source check and maximum nozzle-clearance failures. No new failure; the suite is not fully green. All check-valve tests pass, and source jar succeeds.
- All ten pre-existing local equipment/test files match SHA-256/status before and after. Feature paths do not overlap them; no stash, staging or commit of those edits; ignored tracked-file checks empty. Target jar builds with its local edits retained.
- Ordinary runClient is left running in the target worktree: PID 29920, Gradle session 42383, responsive Minecraft NeoForge window and resources/JEI runtime initialized. No test suite overlaps the client; no GUI automation or desktop input was sent.
- Checkpoint 18/unit 11 requires fresh-world verification. The owner was warned before merge. The session entered singleplayer at 16:01:25; at 16:01:26 the log confirms an incompatible format-17 reset: fluid inventories, controls and pending events discarded, followed by LOADED format=18. The agent only launched runClient and issued no world-load input. Record the actual reset rather than claiming the session stayed at its menu.
- Code-guide contracts/integration snapshots, batch/tool indexes and Unreleased entry are updated. Logs, gate XML, preservation manifests, target package and client-startup/reset evidence are compressed under tools/pipeline-check-valve/logs/. No tracked tooling detached, new GameTest or second campaign.

Owner subsequently requested moving constructed mover inlet/outlet arrows from adjoining pipes to equipment nozzle necks. That is a follow-up rendering change, separate from the completed check-valve integration.
