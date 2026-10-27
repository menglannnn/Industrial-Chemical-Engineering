# WTI pump fix integration plan

2026-10-24. Owner: "merge and run when done". Target claude/floating-roof-tanks-81d68b, currently a591404f; source codex/wti-pumping-rate-hold at 19c8697f (implementation 9fa53354 / c49f6654 / 98f3dce3).

- Complete required quick/slow/package checks on the final source, then serial scoped quick/cohort checks; retain the known baseline failures and incomplete GameTest result explicitly. Do not repeat the full reference lane/campaign.
- Record SHA-256 for the target's ten existing modified/untracked equipment/test files. Fast-forward the integration branch from the owned source branch without staging, stashing or committing those files; verify every byte/hash afterwards and ensure no ignored tracked file.
- Update actual code-guide integration snapshots, batch review/index and tooling evidence. No main merge or version bump; checkpoint 17/unit 10/fluid-13 remain.
- Build/run the requested ordinary dev client in the integration worktree with its local equipment edits. Confirm startup and leave it running at the menu; no user world, keyboard/mouse input or overlapping test suite.
