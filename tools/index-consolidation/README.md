# index-consolidation

Purpose: one-shot rebuilds of two indexes into a short index plus one `README.md` per folder: `documentation/INDEX.md` (old five-column layout) and `tools/INDEX.md` (old five-column layout with a "Last updated" log). Served batch `documentation/2026-10-26-index-consolidation/` (review `INDEX_CONSOLIDATION_REVIEW.md`). Nothing in the tracked tree uses it.

Files:

- `parse.js`: reads the old documentation index into rows (purpose, status, documents, code areas), batch notes and the lookup sections; its `cells` splits a table row outside backticks.
- `short.js`: the hand-written short purpose and status per batch folder. `build.js` refuses to run when a row has no entry here.
- `build.js`: writes `documentation/INDEX.md`, `<batch>/README.md` (an existing README gets an appended "Index record" section), `repository/FILE_LOOKUP.md` and `repository/INDEX_2026-10-26_ARCHIVE.md`.
- `tools-short.js`: short purposes and status corrections for tool rows that need them; other rows keep their purpose and the leading clause of their status.
- `tools-build.js`: writes `tools/INDEX.md` (rows sorted by batch date, newest first; the `examples/` section kept), appends an "Index record" to every tool folder's `README.md` (creates it for the 12 folders that had none) and freezes the old index as `index-consolidation/INDEX_2026-10-26_ARCHIVE.md`. It refuses to run while a short purpose is over 110 characters or a short status over 60.
- `check.js`: counts relative links in the output and lists broken ones (a target may sit in the output or in the source tree).

Run (Node.js; `python` in Git Bash is a stub):

```bash
node tools/index-consolidation/build.js <old documentation dir> <output documentation dir>
node tools/index-consolidation/tools-build.js <old tools dir> <output tools dir>
node tools/index-consolidation/check.js <output dir> <old dir>
```

The inputs must be the old layouts (md5 of the 2026-10-26 inputs: documentation `fb4100c9`, tools `eac70ad3`). Running them on the new indexes does not work: the new rows have fewer cells and the folder READMEs already carry their records.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: One-shot rebuild of the one-file `documentation/INDEX.md` into the short index and one `README.md` per batch

Batch: 2026-10-26-index-consolidation

Status: Concluded 2026-10-26 (one-shot). Index: [../INDEX.md](../INDEX.md).

### Entry points

`build.js`, `check.js`

### History

- Concluded 2026-10-26 (one-shot)
