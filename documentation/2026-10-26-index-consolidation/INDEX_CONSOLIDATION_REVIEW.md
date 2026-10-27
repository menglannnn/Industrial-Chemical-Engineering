# Index consolidation review (2026-10-26)

Batch `2026-10-26-index-consolidation`, branch `claude/index-consolidation-structure-6f70e7` from `2178a7c2` (0.12.0). Merged to `main` 2026-10-26 by fast-forward as 0.12.1 (`a8ce7558`) and, for the tools index, 0.12.2 (`bc7a4167`); not pushed. Code guide: no change.

## What changed

| Item | Before | After |
|---|---|---|
| `INDEX.md` | 195,717 bytes; 78 rows with five columns, longest row 25.8 KB; a 13 KB "Last updated" log; batch notes; old-name lookup (216 rows) | 15,505 bytes; one row per batch (folder, purpose, status), newest first; longest row about 200 characters |
| Batch detail | in the index rows and the "Batch notes" section | in each batch folder's `README.md`: Purpose, Status, Documents, Code areas, History, Notes (78 files; two existing READMEs got an appended "Index record" section) |
| Old-name lookup | end of `INDEX.md` | `repository/FILE_LOOKUP.md` |
| The old index | | `repository/INDEX_2026-10-26_ARCHIVE.md`, frozen (links prefixed with `../`) |

Rule (`AGENTS.md`, `54efd856`; `documentation/README.md` update rule): the index row holds only the folder, the purpose in a few words and the status with its date; everything else goes to the batch `README.md`.

## How the detail was kept

- `tools/index-consolidation/build.js` reads the old index and writes every file above. History bullets are the old status cell split at its `; ` separators (the shallowest split whose bullets stay under 600 characters); no words were rewritten. Purposes and statuses in the index are short texts written by hand (`short.js`).
- Check 1: 1,878 tokens of the old rows (commit ids, document names, numbers) looked up in the matching README; none lost (two hits are READMEs that do not list themselves).
- Check 2: every status word of every old row is in its README.
- Check 3 (`check.js`): 1,157 relative links in the new files, 0 broken.
- The "Last updated" log and the "Latest task update" paragraphs duplicated the rows; they stay only in the archive.

## Status corrections (checked against git and `CHANGELOG.md`)

- `2026-09-27-jdk-determinism`: "In progress" → Implemented 2026-10-14 (0.7.0). `DeterministicMath` is on `main` through the phase-ports squash `1dba69b3`; CHANGELOG 0.7.0 lists it. Open: the `winsweep` run after that merge.
- `2026-10-14-empty-start-progressive-volume`: "In progress" → Implemented 2026-10-14 (0.7.0). Merged into the phase-ports branch (D57); CHANGELOG 0.7.0 lists D55.
- Three rows (phase-ports, empty start, mixed-gas junction) stood below the lookup table; they are in date order now.

## Findings for the owner

1. `2026-09-24-minecraft-mcp-skill`: `.claude/skills/minecraft-mcp/` is not on `main`, and no branch holds its commits `20b1587` and `1287d6d` (the branch is gone; the objects remain). Status kept "In progress", with a note.
2. `FLANGE_JOINT_REVIEW.md` (2026-10-20, custom equipment batch) existed only in this session's worktree; its branch `claude/competent-franklin-43848a` (`bf38dee9` and later) is not on `main`. It is staged for the copy with a note in that batch's README.
3. `2026-10-24-wti-pumping-rate-hold`: the old row says the owner's check of the net-inflow/own-pressure monotonicity assumption was still required; the batch is in 0.12.0. The text is in its README History.

## Merge

- Release prep `a8ce7558`: `mod_version` 0.12.1, root `README.md` version line, CHANGELOG `[0.12.1] - 2026-10-26`.
- Gates at `a8ce7558`: `./gradlew test slowGates` green, `test` 1419 tests / 0 failures, `slowGates` 187 / 0 (4 min 59 s). No source, test or format change in the batch.
- `git ls-files -i -c --exclude-standard` empty. Fast-forward of `main` `2178a7c2` → `a8ce7558`.
- The documents were built and staged in the session worktree (a hook blocks writes to the main checkout from a worktree session) and copied to the main checkout after the merge, on the owner's instruction; the main `INDEX.md` was still the input (md5 `fb4100c9`).

## Tools index (owner request after the merge, 2026-10-26)

- `tools/INDEX.md` 73,256 → 26,007 bytes: one row per tool folder (folder, purpose, batch, status), sorted by batch date; 11 rows that stood below `examples/` back in order; the "Last updated" log and the Entry points column out. Each row's full text appended to the folder's `README.md` as an "Index record" (99 READMEs kept intact above it; 12 folders without one got a new README, for example `dwsim/`, `neural-budget/`, `lnn-gap/`); old index frozen as `tools/index-consolidation/INDEX_2026-10-26_ARCHIVE.md`. Checks: 0 tokens or words lost over 112 folders, 396 new links 0 broken; 120 files copied byte for byte.
- Status corrections: 13 rows read "In progress", a step name or nothing although their batch is on `main` (for example `junction-seeding` "t44 build measured", `floating-roof-tanks`, the six 2026-10-25 folders); now "Concluded <merge day> (batch in <version>)", with a note in each README.
- Rule `9676872f` (AGENTS.md tooling cleanup), CHANGELOG `77b3db72`, release prep `bc7a4167` (0.12.2); test run waived by the owner (no source, test or format change); fast-forward of `main` `a8ce7558` → `bc7a4167`.
- Incident: at 09:19 a whole-file copy of `documentation/INDEX.md` overwrote the row another session had added at 09:18 (`2026-10-26-air-cooler-chunk-rendering`); the mismatch check printed a warning but did not stop the copy. The row was rebuilt from that batch's README at 09:20 and the session was told. Shared index files are now patched in place, never copied whole.
- Tool folder: `tools/index-consolidation/` (`parse.js`, `short.js`, `build.js`, `tools-short.js`, `tools-build.js`, `check.js`, `README.md`, the gate log).
