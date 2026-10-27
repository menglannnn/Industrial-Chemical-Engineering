# Sight-glass target integration

2026-10-23, owner request: merge into `claude/floating-roof-tanks-81d68b`, then leave a running client.

- Confirm clean owned feature branch and compare its history to the target.
- Record all target local paths/statuses/SHA256 hashes and preserve their diff/untracked backup in canonical ignored tooling; do not stage or commit them.
- Confirm incoming files do not overlap local changes and both branches have no tracked ignored files.
- Fast-forward the target from `29ff371b` to `b3b7a347`; verify all ten local files/statuses remain unchanged.
- Follow the owner's no-tests preference: target `jar` compile/package only. Keep main and mod_version unchanged.
- Update branch guide snapshots, batch review/index and compressed evidence; retain the future main merge's required checks and owner-chosen version.
- Launch target `runClient`, confirm the client remains alive from logs/process state, leave it running, and perform no automated GUI/world checks.
