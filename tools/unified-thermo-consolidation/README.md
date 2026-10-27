# unified-thermo-consolidation

Purpose: the measurement and editing tools of thread t35, the behaviour-free consolidation of the unified thermo port
(batch `2026-10-18-unified-thermo-consolidation`, branch `claude/unified-thermo-consolidation` from `c9aea46e`). Nothing
here was ever tracked, so nothing needs re-attaching. The brief is `t35-consolidation.md`.

- `lane-check.sh <name> [group...]`: the between-commit proof. Exports the worktree's current `src/` tree, compiles it
  on the Gradle-free javac lane of `tools/unified-thermo-port-m5/d40/` (`lane.sh`, `run-touching.sh`, the 16
  `classes-*.txt` groups), runs the named groups (default all) and compares every test's printed output with the
  baseline logs `c0-<group>.txt` in `/tmp/t35-logs` (taken at `c9aea46e`) through `compare-test-output.pl` and
  `notime2.pl` (strips timings, byte and allocation counts and stack-trace line numbers). One line per group: the
  M3RUN summary, same/moved counts, and the tests whose output differs beyond those fields (`PHYSICS`). The lane does
  not hold the `science/column` tests; those run only on the Gradle suite. `PackagedSparseLuTest` fails on the lane at
  the base too.
- `DeadCode.java`: javac tree API liveness over `src/main` and `src/test`. Roots: `@Test` family, `@SubscribeEvent`,
  `@Mod`, GameTest annotations, `main` methods, `@MethodSource` names, overrides of external methods, root-class
  constructors, static fields with invocation or allocation initialisers; record accessors map to their fields.
  Known blind spots: reflection (`@ExtendWith` extensions, so `Cdu17TestCatalog.withProduction` is live although
  reported dead) and `super(...)` arguments. Run: `javac DeadCode.java` then `java DeadCode <src dirs>` with the
  compile classpath; output is one TSV row per unreferenced declaration (`/tmp/t35-dc/report2.tsv` was the last).
- `comments.pl [--min N] <dir|files>`: comment metrics per file (total, code, comment, blank, largest block, lines in
  blocks of at least N). `blocks.pl [--min N] <file>`: prints every comment block with its line numbers and the code
  line after it. `dupes.pl [--window W] [--min M] <dirs|files>`: duplicate code runs over normalised lines.
  `outline.pl <file>`: the declarations of a file with line numbers.
- `condense.pl <spec>` and `condense-multi.pl <spec>`: replace line ranges of comment blocks from a spec that returns
  `[file, start, count, phrase, replacement]` rows (`specs/*.pl`: `pss` PassiveStepSolver, `fte` FluidTpEquilibrium,
  `ft` FluidThermodynamics, `b2` and `b3` the second and third main batches, `hcw` HoldClassWorldsTest). Edits are
  applied bottom-up; every row's phrase must occur in its range (whitespace, line breaks and comment stars ignored)
  or nothing is written. Each spec names the commit its line numbers belong to.
- `t35-consolidation.md`: the brief (copy of `D:/Minecraft/Modding/claude-threads/t35-consolidation.md`).

The batch's documents are `documentation/2026-10-18-unified-thermo-consolidation/CONSOLIDATION_PLAN.md` and
`CONSOLIDATION_REVIEW.md`; the thread's report is `D:/Minecraft/Modding/claude-threads/t35-report.md`.
- `compare-junit-xml.pl <baseline dir> <new dir>` (t36): two Gradle JUnit XML result folders; test names both ways (repeated
  parameterised display names numbered), failures, then each class's `<system-out>`/`<system-err>` line by line, every
  differing line tagged T (timing, byte counts or stack-trace line numbers only, as `notime2.pl`) or P (anything else).
  `compare-campaign.sh <baseline logs> <new logs>` (t36): per distribution, `.classes.txt` and the COUNTERS line exactly,
  report SEED lines with `ms=` blanked.
- `logs/m11-logs.tar.gz` (t36): the final-test lane on `688f4cd7` (`lane.sh`, all four steps) with `compare-junit-xml.txt`
  and `compare-campaign.txt` against `build/m10-logs`; result in `CONSOLIDATION_REVIEW.md`, "Final test (Opus)".

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Thread t35 (batch `2026-10-18-unified-thermo-consolidation`, branch `claude/unified-thermo-consolidation`): `lane-check.sh` (javac-lane export, compile, run and bitwise comparison of the 16 test groups against the `c0` baseline at `c9aea46e`), `DeadCode.java` (javac tree API liveness over main and test), `comments.pl`, `blocks.pl`, `dupes.pl`, `outline.pl`, the `condense*.pl` comment-range editors with their `specs/`, the brief and a README. Never tracked.

Batch: `2026-10-18-unified-thermo-consolidation`

Status: Done 2026-10-18. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `lane-check.sh`, `compare-junit-xml.pl`

### History

- Done 2026-10-18 (final test m11, t36; logs `logs/m11-logs.tar.gz`)
- merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed)
