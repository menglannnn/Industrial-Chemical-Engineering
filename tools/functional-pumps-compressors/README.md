# Functional pump and compressor build evidence

Serves batch `2026-10-23-functional-pumps-compressors`. Contains the compressed compilation/package log for code `4e96e8ab`; no standalone harness, detached source or investigation scripts.

From the owned feature worktree, run `./gradlew.bat jar --max-workers=4` to compile and package. The owner requested no tests and will perform fresh-world in-game checks.

`logs/functional-pumps-compressors-build-logs-2026-10-23.tar.gz` contains `functional-machines-build.log` (successful jar build). No client or test suite was launched for this batch.

`logs/functional-pumps-compressors-integration-logs-2026-10-23.tar.gz` contains the target's successful jar build and preservation evidence for its ten local changes: status/hash records and the overlapping ShapeRasterizer's original, base, feature and expected combined text. The target was fast-forwarded to `29ff371b`; the temporary named stash and backup tag were removed after restoration and verification. Pre-existing stashes were left untouched. These are local backups/evidence, not detached product code or a new harness.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Feature/target compilation and local-edit preservation evidence for functional constructed movers; no harness or detached code

Batch: 2026-10-23-functional-pumps-compressors

Status: Archived 2026-10-23. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `logs/*-logs-2026-10-23.tar.gz`

### History

- Archived 2026-10-23 (target integrated at `29ff371b`; all ten local changes preserved; tests waived by owner)
