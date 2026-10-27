# 2026-10-26-index-consolidation

Purpose: Owner request 2026-10-26: consolidate `INDEX.md`, put the big chapters into sub-documents, and keep it simple as a future rule. The one-file index (195 KB, rows up to 26 KB) becomes one short row per batch; each batch's detail moves into its folder's `README.md`. Then the same rule for `tools/INDEX.md` (73 KB): one short row per tool folder, detail in the folder's `README.md`.

Status: Implemented 2026-10-26 (0.12.1, 0.12.2). Index: [../INDEX.md](../INDEX.md).

## Documents

- [INDEX_CONSOLIDATION_REVIEW.md](INDEX_CONSOLIDATION_REVIEW.md)

## Code areas

`AGENTS.md` (documentation and tooling rules), `CHANGELOG.md`, `README.md`, `gradle.properties`; local `documentation/INDEX.md`, `documentation/README.md`, every batch `README.md`, `repository/FILE_LOOKUP.md`, `repository/INDEX_2026-10-26_ARCHIVE.md`, `tools/INDEX.md`, every tool folder `README.md`; tool `tools/index-consolidation/`

## History

- 2026-10-26: branch `claude/index-consolidation-structure-6f70e7` from `2178a7c2` (0.12.0): rule `54efd856`, CHANGELOG `d364ca25`; index and batch READMEs built by `tools/index-consolidation/build.js` from the old `INDEX.md` (md5 `fb4100c9`), staged in the session worktree.
- 2026-10-26: release prep `a8ce7558` (0.12.1); `test` 1419/0 and `slowGates` 187/0 at `a8ce7558`; merged to `main` by fast-forward as 0.12.1 (`a8ce7558`), not pushed; staged documents and the tool folder copied to the main checkout.
- 2026-10-26: tools index rebuilt by `tools-build.js` from the old `tools/INDEX.md` (md5 `eac70ad3`): 73 KB to 26 KB, 112 folder READMEs (12 new), 13 stale statuses corrected; copied to the main checkout. Rule `9676872f`, CHANGELOG `77b3db72`.
- 2026-10-26: release prep `bc7a4167` (0.12.2); test run waived by the owner (documentation and tooling only); merged to `main` by fast-forward as 0.12.2 (`bc7a4167`), not pushed.

## Notes

- Nothing open.
