# Fluid format reset — integration plan

2026-10-23; continue the owner's requested integration/client delivery with the crash fix.

- Fast-forward `claude/floating-roof-tanks-81d68b` from ab35ef47 to 7234bb46, preserving every existing tracked and untracked local change with hash/diff backups.
- Use only target `jar --max-workers=4` to compile/package. No tests or automated old-world/GUI checks.
- Restart `runClient --max-workers=4` for the owner; leave it at its normal startup screen. The owner chooses any world and checks the approved reset behavior.
- Archive crash/build/merge/startup logs, finish dated index/review and keep main/version unchanged.
