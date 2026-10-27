# Fluid format reset delivery evidence

Batch: `documentation/2026-10-23-fluid-format-reset/`; implementation `49e57860`, delivery `7234bb46`.

This folder contains compressed compilation, merge-preservation and client-startup evidence. The pre-fix crash is an owner-generated log; this task did not load or test an old world. No tests, automated GUI checks, investigation harness, detached code or new Gradle switch were added.

Reproduce packaging from the feature or integration checkout with `gradlew.bat jar --max-workers=4`; launch the owner client with `gradlew.bat runClient --max-workers=4`. Owner requested destructive reset on incompatible formats, discarding simulation contents/settings while rebuilding placed devices.

Archives are stored in logs/*-logs-2026-10-23.tar.gz. Integration preservation evidence includes hash/status/diff backups and the existing untracked diagnostic source; these are archival only, never staged.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Compressed owner crash, compile/package, integration preservation and startup evidence; no harness

Batch: 2026-10-23-fluid-format-reset

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `logs/*-logs-2026-10-23.tar.gz`

### History

- Archived 2026-10-23 (target `7234bb46`; ten local changes preserved; build passed, client restarted; no tests)
