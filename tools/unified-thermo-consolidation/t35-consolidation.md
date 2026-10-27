# t35: code consolidation of the unified thermo port (owner: "this is becoming too massive")

Owner request (2026-10-18), to the orchestrator of the unified thermo port (session d527c532): "Use a fable extra subagent with 1M context to perform a consolidation of the code, this is becoming too massive", then "use a background thread instead of subagent for this work". You are that thread: Fable 5.1, effort xhigh, a 1M-token window.

## Where
- Worktree: `D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0`. It is also the orchestrator's, but the orchestrator neither edits code nor runs Gradle while you work.
- Branch `claude/unified-thermo-consolidation`, created at `c9aea46e`, the head of `claude/unified-thermo-port`. That branch is the unified multiphase thermo port to main 0.8.0 (`ffeafe8a`), 88 commits, validated by the final lane at `3db5e5d3`; `c9aea46e` adds only its CHANGELOG line. First check `git branch --show-current` and `git log --oneline -1`.
- Never touch `claude/unified-thermo-port` or `main`. No push, merge, PR, history rewrite or `mod_version` bump.
- Batch folder: `documentation/2026-10-18-unified-thermo-consolidation/` in the worktree (git-ignored). The orchestrator copies it to the main checkout and writes the INDEX rows; you cannot write the main checkout.
- Scripts and probes you create: `tools/unified-thermo-consolidation/` in the worktree (git-ignored), with a `README.md` (AGENTS.md "Test classes and tools after a batch"). It already holds this brief and `compare-test-output.pl`.
- Progress: append one line per milestone (plan written, each phase committed, final lane started, done) to `D:/Minecraft/Modding/claude-threads/t35-progress.md`, with the time and the head sha. Final report: `D:/Minecraft/Modding/claude-threads/t35-report.md`, at most 50 lines.

## Size now (Java files, lines)
- src/main: 230 files, 60 103 lines. Main 0.8.0 had 192 and 45 580; the last consolidation (2026-10-15, batch `2026-10-15-code-consolidation`) took main from 245 files to 192.
- src/test: 312 files, 53 666 lines; main 0.8.0 had 236 and 35 912.
- Growth since `ffeafe8a`: science/thermo 30 files, 10 698 lines (+9 826); science/fluid 39 files, 13 716 (+2 416); science/material 19 files, 3 452 (+2 254). science/column (82 files, 18 979 lines) is unchanged by the port.
- Largest files: FluidTpEquilibrium 5 993; PassiveStepSolver 4 624; V3ColumnCalculator 3 319; FluidThermodynamics 1 499; V3ColumnInitializer 1 019; IslandCoordinator 978; V3AcceptanceAuditor 940; PengRobinsonKernel 939; V3SimultaneousColumnSolver 851; TangentPlaneStability 839; ColumnV3Network 827; BoundedCpuSolveService 820.

## Goal
Make the code markedly smaller and simpler with NO change of behaviour: every number the code computes stays bitwise identical. The scope is all of src/main and src/test. Start where the port grew the code: science/thermo, science/fluid, science/material, runtime/fluid and their tests.

Behaviour-free work:
- **Dead code:** classes, methods, fields, constants, enum values, record components and config entries nothing uses; branches that cannot be reached; parameters every caller passes the same value.
- **Duplication:** near-identical helpers and loops in different classes go into one shared utility. One-caller abstractions and wrapper types fold into their caller; single-use types are nested.
- **Modes and switches** with one live value, and defensive checks that cannot fire. Keep the numerical guards the owner rules require: the numerical box, T <= 0 and no equation-of-state root.
- **Comments:** condense long historical narratives (decision histories, traced numbers) to the rule they state; the batch documents keep the history. Keep, shortened if long, every comment that records a measured reason for a term (AGENTS.md "Small physics simplifications", last bullet).
- **Tests:**
  - Merge duplicate fixtures and helpers into shared ones, and merge test classes that test the same thing.
  - A test may go only together with code that it alone tested and that you deleted.
  - Never narrow a gate. Every assertion of the gate suites stays: the full `test` task, the 33 junction lines, MIXED_GAS_COST, STACK_PASS, the printed lines of the suites, chain-100 and the 31 fluid GameTests.
  - List every removed test by name.

Floating point: bitwise identity depends on evaluation order.
- Do not reorder sums or products, merge or split accumulating loops, turn `a*b+c` into `Math.fma` or back, or replace a formula with an algebraically equal one.
- Do not change numeric types, or the iteration order of maps and sets (HashMap against LinkedHashMap), unless the order provably reaches no number.
- Extracting a helper is safe when the same operations run in the same order.

Not behaviour-free. Do not implement these; propose each in the plan or the review with its effect in numbers, what it deletes and what it risks. The owner approves each one (AGENTS.md "Small physics simplifications").
- Anything that moves a printed number, a reference, a campaign seed's class or COUNTERS, or a GameTest read.
- Physics simplifications; model or tolerance changes; deleting a live capability, rule or mode.
- Save, checkpoint, wire or protocol format changes. Formats stay at checkpoint 10, unit 3, protocol `fluid-10`.
- Client, GUI and rendering behaviour, which would need an in-game check; the owner skipped the client test for now. In client code, delete only what is provably dead.

## Proof (AGENTS.md lean rule)
- **Baseline: `build/m10-logs/` in the worktree** (code `3db5e5d3`, which equals `c9aea46e`'s code; do not delete it):
  - `01-results/`: JUnit XML of the full test, 1476 tests, each with its printed output in `<system-out>`;
  - `02-G2-regression-exact.gradle.txt`; `03-G5-gametests.gradle.txt` (31/31);
  - `04-campaign-cold-random-report.txt` and `05-campaign-bundled-{random,defaults}-report.txt`, each with its `.gradle.txt` holding the COUNTERS line.
- **Between commits: the Gradle-free javac lane.** Tools: `tools/unified-thermo-port-m5/d40/lane.sh` and `run-touching.sh`, the class groups `classes-*.txt` (see that folder's README). Run it on the touched groups and compare printed numbers with `compare-test-output.pl` (and `d40/notime.pl`, `d40/movesize.pl`): only timing lines may differ.
  - First record your own javac-lane baseline at `c9aea46e`, all 16 groups. `/tmp/d40-logs/h13c-*.txt` (Git Bash /tmp) is an earlier run of the same code, for cross-checking.
  - `PackagedSparseLuTest` fails on the javac lane at base too, because it needs the built jar.
- **Gradle:** only through `tools/unified-thermo-port-m1/gradle-locked.sh`, which refuses while a dev client runs.
  - At most two Gradle runs at a time on the machine, each a correctness run (counts, not time).
  - Never a test suite while a Minecraft dev client runs.
- **Final test: NOT yours (owner, 2026-10-18: "The fable thread should not be doing final test, that's a task for opus").** When your commits are done and their javac-lane checks are bitwise, stop and hand off: the orchestrator gives the final lane (`LOGS=build/m11-logs RUNID=m11 bash tools/unified-thermo-port-m5/scripts/lane.sh <head-sha>`: full test, chain-100 exact, GameTests, the 1000-seed campaign) and its comparison against m10 to an Opus thread. Do not run lane.sh's `test`, `regression`, `gametest`, `campaign` or `quick` steps yourself, and no full Gradle `test`.
- **Behaviour-free means all of these hold against m10** (checked by the Opus final test):
  - every testcase's `<system-out>` matches 01-results, timing lines aside (write the small XML comparison script into the tools folder);
  - the test count is the base count minus the listed removals;
  - chain-100 deviation is 0, and GameTests are 31/31;
  - each campaign matches m10 seed for seed, with an identical COUNTERS line.
- Any difference is a behaviour change. If the final test finds one, the orchestrator sends it back to you: find the commit on the javac lane and revert or fix it; never explain it away.

## Working rules (AGENTS.md is binding and is loaded in your session)
- **Commits:** small and topical, one consolidation theme each, each revertible alone and passing its suites.
  - The message says what was deleted or merged, the line-count change, and "behaviour-free: suites bitwise".
  - Trailer: `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.
- **Git hygiene:**
  - Never bare `git stash` or `git stash pop`; use WIP commits.
  - Never `git add -f`; logs go into `tools/unified-thermo-consolidation/logs/*.tar.gz`.
  - `git ls-files -i -c --exclude-standard` must stay empty.
- **Boundaries:** work only in this worktree. Never touch other sessions' processes or worktrees, or the owner's JVMs (geosim, other sessions' Gradle daemons). No desktop input.
- Do the work yourself in this session; do not hand the code work to subagents.
- Reports at most 50 lines. The physics-first rule applies to every proposal.
- **Splitting or shrinking a big class** (for example FluidTpEquilibrium into several files, or smaller in one file): take the form that deletes most and adds no new abstraction layer, and record the choice in the plan.

## Deliverables
1. `CONSOLIDATION_PLAN.md` (at most 50 lines), written first, within about the first hour. It holds:
   - the inventory in numbers per package: dead, duplicated, foldable, comment bloat, tests;
   - the candidates in order of lines saved;
   - the proposals that need the owner, each with numbers, deletions and risks.

   Then implement the behaviour-free list without waiting.
2. Commits on `claude/unified-thermo-consolidation`, each checked on the javac lane. Then append "ready for final test <sha>" to t35-progress.md and stop; you are resumed if the final test finds a difference.
3. `CONSOLIDATION_REVIEW.md` (at most 50 lines):
   - files and lines before and after, per package, for src/main and src/test;
   - every removed class and test by name, or a pointer to a list file in the tools folder;
   - a slot "Final test (Opus)" left for the orchestrator to fill with the final lane's numbers against m10;
   - the owner proposals;
   - a code-guide delta for the merge: classes removed, merged or renamed per chapter of `documentation/reference/code-guide/` in the main checkout (read-only for you).
4. One `CHANGELOG.md` line under `[Unreleased]` / `### Changed` for this batch, beside the port's line (leave that line alone), in your last commit.
5. `t35-report.md` in claude-threads: commits, numbers, open items.
