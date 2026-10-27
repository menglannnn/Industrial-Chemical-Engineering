# Pipeline shutoff valve integration plan

2026-10-23. Owner requested merge and runclient. Target is the established integration branch `claude/floating-roof-tanks-81d68b`, not main; no version bump.

- Run required quick and slow Gradle gate tasks with the dev client closed, and package the source branch. Compare failures with the already recorded five unrelated baseline failures from batch `2026-10-23-valve-regime-convergence`.
- Verify no ignored tracked files and a clean source branch. Preserve SHA-256 hashes of all ten target-local equipment/test files; do not stage, stash or change those edits.
- Source `beb5c2fc` descends directly from target `19117af7`. Fast-forward only if the branch tip and local-file hashes still match, and changed paths do not overlap local work. Verify all hashes again afterwards.
- Code-guide contracts already describe source `4dd96fde`; update integration snapshot lines, batch review/index and tooling evidence. Keep local documents/tools ignored and canonical in the main checkout.
- Compile/package the integrated checkout with its preserved local edits, then launch ordinary `runClient` there after all tests exit. Wait for client startup; leave it running at the menu without opening a world or adding in-game checks.
- Archive gate logs, JUnit XML, preservation evidence, target package and client startup logs under `tools/pipeline-shutoff-valve/logs/`. Record any limits candidly.
