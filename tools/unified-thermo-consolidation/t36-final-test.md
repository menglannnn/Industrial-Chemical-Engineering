# t36: the Opus final test of the consolidation (owner: "that's a task for opus")

The owner (2026-10-18) gave the consolidation's code work to a Fable thread (t35) and its final test to Opus. t35 is done: branch `claude/unified-thermo-consolidation` at `688f4cd7`, 14 code commits and a changelog commit on `c9aea46e`. Read its report `D:/Minecraft/Modding/claude-threads/t35-report.md` and the review `documentation/2026-10-18-unified-thermo-consolidation/CONSOLIDATION_REVIEW.md` first.

## Where
- Worktree `D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0`. First check `git branch --show-current` (`claude/unified-thermo-consolidation`), `git log --oneline -1` (`688f4cd7`) and a clean `git status`.
- You test; you do not change code. No commits to src, no push, merge, PR, stash or `git add -f`.

## Run, alone on the machine
1. Check that nothing else runs.
   - No Minecraft dev client (`jps -lvm` with no `fml.modFolders`, `runMcpClient` or `runClient`).
   - No `build/gradle.lock`.
   - t35 is idle.
   - Never touch other sessions' processes or the owner's JVMs (geosim, other Gradle daemons).
2. `LOGS=build/m11-logs RUNID=m11 bash tools/unified-thermo-port-m5/scripts/lane.sh 688f4cd7`, all four default steps: full test, G2 chain-100 exact, fluid GameTests, then the 1000-seed campaign at 8 workers on cold random, bundled random and bundled defaults. The last lane took about 8 minutes. It runs Gradle only through `tools/unified-thermo-port-m1/gradle-locked.sh`.

## Compare against `build/m10-logs` (code `3db5e5d3` = `c9aea46e`)
- **Full test:**
  - The test count is 1476 minus exactly two: `LiquidFullBubblePointTest.theHeldOutUvIsTheFluidAnswerOfTheCrystalCompetitionsMaterial` (O4) and `CubicPhaseEvaluatorTest.derivativeBlocksAFamilyDidNotProvideAreRefused` (O5). Zero failures.
  - Compare test names in `01-results/*.xml` both ways.
  - For every testcase present in both, its `<system-out>` must be identical, except timing lines (wall ms, seconds, per-second rates) and the `hydrates (D8)` row of `G3F9DataRangeBoundaryTest`, which owner decision O1 deletes.
  - Write the XML comparison script into `tools/unified-thermo-consolidation/` (README line) and list every line that differs.
- **G2:** chain-100 deviation 0 (`02-G2-regression-exact.gradle.txt`).
- **GameTests:** 31/31 (`03-G5-gametests.gradle.txt`).
- **Campaign:**
  - For each of the three distributions, every seed's class must match m10 seed for seed (the `.classes.txt` files and the per-seed SEED lines).
  - The `COUNTERS` line in each `.gradle.txt` must be identical to m10's.
  - m10 had cold 911, bundled random 986 and defaults 1000.
- The column suite in the full test is the only check of commit `74785ddb` (the kernel's sparse-pair fold) that t35's javac lane did not run; say so explicitly in the result.
- If anything else differs, do not fix it. Give the test and line, or the seed and counter, that differ, and stop. The orchestrator sends it back to t35.

## Record
- Fill the slot "Final test (Opus)" in `CONSOLIDATION_REVIEW.md` in the worktree, keeping the review at most 50 lines. Then copy it to the main checkout's `documentation/2026-10-18-unified-thermo-consolidation/` if you are allowed; otherwise say so and the orchestrator copies it.
- Archive `build/m11-logs` to `tools/unified-thermo-consolidation/logs/m11-logs.tar.gz` (`tar --force-local -czf`), and add one line to that folder's README.
- Progress: one line per milestone (lane started, lane done, comparison done) in `D:/Minecraft/Modding/claude-threads/t36-progress.md`.
- Final report: `D:/Minecraft/Modding/claude-threads/t36-report.md`, at most 30 lines, giving each check, its numbers against m10, and the verdict: bitwise apart from the expected two tests and one row, or the differences.
