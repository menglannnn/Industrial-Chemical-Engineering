# Merge plan for the four stacked batches (t47, 2026-10-19)

This is a plan only: read-only git, no Gradle, nothing merged. The drafts are in [MERGE_STACK_DRAFTS.md](MERGE_STACK_DRAFTS.md): the CHANGELOG blocks, the version lines, the code guide per chapter (five Opus subagents) and the merge-time fixes. The owner's decisions are in section 6.

## 1. Main's movement
- `main` = `ffeafe8a` (0.8.0, 2026-10-15; `origin/main` the same). Nothing has landed since; the custom equipment rendering branch has not merged. Every stack branch has its merge base at `ffeafe8a` and is 0 commits behind, so `main` is exactly what the port assumed.
- The stack is linear, with no merge commits. Port 88 commits (`ffeafe8a..c9aea46e`), consolidation 15, simplification 9, seeding 9 (to `798bedd9`, t44c's follow-ups included) = 121. Diff: 373 files, +77 366 / -61 237 lines.
- Hygiene checks on the tip: `git merge-tree` of `main` with the tip is clean (a fast-forward is possible); `git ls-files -i -c` and the tracked-but-ignored check are empty on all heads; the history packs to 2.7 MB with no log blobs.
- **The other line**, `claude/custom-equipment-rendering-805e7d` (36 commits on `ffeafe8a`):
  - **Text conflicts:** with the tip, 11 files and 14 hunks: CHANGELOG, `build.gradle`, the codec, `FluidDeviceSpec`, `ConservativeTransport`, `PassiveIntervalSolver`, `PassiveStepSolver`, `DeterministicMath`, `EventPathWorld` and two codec tests.
  - **Semantic conflicts**, larger: its checkpoint format 9 (vessel ports, level heights) against the stack's 9 to 11; the movers' efficiency and the m g z booking, both of which the stack deleted; `levelHeight` against inventories built without it.
  - Whichever line merges second resolves these (drafts 3.5).

## 2. Merge shapes
| | (a) fast-forward of the stack + one release commit | (b) one merge commit per batch, in order | (c) one squash per batch |
|---|---|---|---|
| Versions | 0.9.0 | 0.9.0, 0.9.1, 0.10.0, 0.11.0 | as (b) |
| Conflicts | none | CHANGELOG at merges 2 to 4 (`merge-file`: merge 2 files its line under 0.9.0 with no conflict reported; merge 3 conflicts) | none if each squash takes its batch head's tree; `git merge --squash` conflicts wherever a later batch rewrote port lines |
| Tree on main | the tested tip (+ CHANGELOG, version) | each tested head (+ text) | each tested head (+ text) |
| Reviewer sees | 121 commits on the first-parent line; batches by the CHANGELOG ranges | 4 first-parent entries, each diff one batch | 4 commits (the port's is 352 files, +76 k / -58 k) |
| History kept | every commit; cited SHAs resolve on main | the same | 4 squashes; ~100 cited SHAs (`15220a07`, `3db5e5d3`, rig `00ee8385`) live only on branches; no bisect inside a batch |
| Guide refreshes | 1 | 4 (each writes text the next deletes: `EquilibriumService`, format 10, `CAP_*`) | 4 |
- **Precedent:** only 0.7.0 was a squash (`1dba69b3`, 1521 files with logs, which was the reason for squashing). 0.7.2 (`26ec48de`) and 0.8.0 (`ffeafe8a`) were fast-forwards of a linear branch plus a two-file release commit. (a') A `--no-ff` merge gives one first-parent entry with the same tree; 0.5.0 (`9674bf16`) was one.
- **D32 as 0.8.1** (`15220a07`): its tree cherry-picks cleanly onto `ffeafe8a` (not compiled here). The stack then cannot fast-forward, and its merge meets 3 code hunks (`PhysicalRegistry` 1, `EventPathWorld` 2; take the stack's side). It is worth it only if `main` ships while the stack waits.
- **Recommendation:** (a), as 0.9.0: one version step, the exact tree the last Opus lane tests, no conflict and the full history.

## 3. Pre-merge tasks
- **Port:** sorted at `c9aea46e` (`MAIN_ADAPTATION_REVIEW.md` "Cleanup survey"): nothing tracked to detach; the bridge pin `026422fe` is kept for the GUI lane.
- **Consolidation:** nothing new tracked. The open note from t36 (bundled seed 47's extra wall-deadline hold, timing, not proven) is moot under (a).
- **Simplification:** no file added; its edited tests are gates under `test`.
- **Seeding:** `JunctionSeedingGateTest`, 5 tests, is a gate under `test`. Six `SolverDiagnostics.balance*` counters reach a reader only through the generic counter export of `fluidSolverRegression` and `fluidServerBenchmark`; only the untracked hold-fuzz harnesses read them by name.
- **All batches:** no new property, environment variable or Gradle switch. Every new test runs under `test`, which excludes only `**/fluid/benchmark/**`. Nothing fails all three tests (a gate task runs it, the product needs it, a documented Gradle task runs it).
- **Text to fix** (drafts 3.1): the CHANGELOG layout and "the 678 commit", done in the release commit; stale code comments in seven places (format 10 against 11, `PUMP_VELOCITY_LIMIT`, 24 against 30 iterations, MCP 0.3.0, ...); `MATERIALS.md`, which still documents `ThermoDomainViolation` and topology format 2 and omits the pilot package.
- **Gates:** t48, the Opus lane on `798bedd9`, is the final test (full test expected 1482; G2 chain-100 exact; fluid GameTests 31/31; campaign). The release edits (CHANGELOG, `mod_version`) need no re-run, as at 0.8.0. No column GameTest has run since 0.8.0 (drafts 3.4).
- **Code guide:** README, all 6 chapters and a new `equilibrium-engine.md`. The final test reviews' "Code guide: no change" covers the lanes, not the batches.

## 4. Sequence (shape pending)
1. t48 green on `798bedd9`, and the owner's choice of shape and version.
2. t44 (Fable, brief `t44d-pre-merge-fixes.md`) makes the fixes of section 6 (5).
3. An Opus thread re-runs the full test on t44's head.
4. The merge in the chosen shape: for (a), `main` fast-forwards to the head, then `Release 0.9.0` (drafts 1.2).
5. Code guide refresh (snapshot = the release SHA), with the drafts brought to t44's head for the counters, `UNIT_FORMAT` 4 and the revision string; `documentation/INDEX.md` rows Implemented with the date; docs copied.
6. Push only on the owner's word.
7. The equipment branch's merge task on the new `main`.

## 5. Blocking findings
None. The CHANGELOG defects are fixed in the release commit.

## 6. Owner decisions (2026-10-19, relayed by the orchestrator)
1. **Shape:** undecided until t48's final test of `798bedd9`.
2. **Version:** decided with the shape.
3. **D32:** inside 0.9.0, not 0.8.1.
4. **Order:** the stack first, then the custom equipment rendering branch on the new `main` as its own batch, with checkpoint format 12.
5. **Pre-merge:** (i) the comment and `MATERIALS.md` fixes, then a full-test re-run; (ii) the six balance counters are detached; (iii) the column GameTest run is skipped; (iv) `UNIT_FORMAT` goes to 4 and the revision string `velocity-clamp-v1` is renamed; (v) the in-game rows 3 to 12 and rig rows 5 and 10 come after the merge.
6. **Merged 2026-10-19 as shape (a):** `main` fast-forwarded to `81bf147f` (t44d's pre-merge commits `9ce2553a`, `47391899`, `81bf147f` on `798bedd9`), then `5b49f940` "Release 0.9.0" (CHANGELOG `[0.9.0] - 2026-10-19`, `mod_version=0.9.0`); not pushed (125 ahead of `origin/main`). Gates: t48 (m16 at `798bedd9`) and the full-test re-run t49 (m17 at `81bf147f`, 1482/0, G2 exact).
