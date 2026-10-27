# Constructed mover controls/audio — integration plan

2026-10-23; owner explicitly requested merge to `claude/floating-roof-tanks-81d68b`, then runClient.

- Fast-forward target `b3b7a347` to owned feature tip `ab35ef47`; incoming changes must not overlap its nine modified tracked files or untracked diagnostic test.
- Back up the target diff, untracked file and SHA256 manifest; compare all ten local files and the status list after merging. Never stage them.
- Compile/package with `gradlew.bat jar --max-workers=4`; no tests, per owner instruction. Do not open old worlds or add migration work.
- Launch `gradlew.bat runClient --max-workers=4` in the target checkout; leave the responsive client running for the owner. No automated GUI or in-game check.
- Archive preservation/build/startup evidence, update batch/index/branch guide snapshots. Main and mod_version stay unchanged.
