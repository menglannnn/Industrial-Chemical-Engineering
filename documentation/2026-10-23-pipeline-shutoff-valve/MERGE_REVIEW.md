# Pipeline shutoff valve integration review

2026-10-23. Owner requested merge and runclient; target `claude/floating-roof-tanks-81d68b`, source `codex/pipeline-shutoff-valve`. Not main; no version bump or push.

- Fast-forward `19117af7 -> beb5c2fc`; source implementation `4dd96fde`. Target's nine modified equipment sources and untracked TmpDiagTest.java were preserved byte-for-byte, verified by SHA-256 before and after. No stash, staging or changes to those files.
- Both checkouts had no ignored tracked files; source remained clean. Fast-forward paths did not overlap target local work; the target branch/head/index were checked again before the merge.
- Required source invocation, client closed: `gradlew.bat test slowGates jar --continue --max-workers=8`. Quick: 1364 tests, 4 failures. Slow: 185 tests, 1 failure. Package generation passed; all four new PipelineShutoffValveTest methods passed in the full quick suite.
- The five failures match the already reproduced baseline in batch `2026-10-23-valve-regime-convergence`: checkpoint expected 13 (now 17); protocol expected fluid-11 (actual fluid-13); constructed mover shell-cell expectation; existing SightGlassDisplay Math.pow source rule; existing largest-nozzle clearance sweep. No new failing class/method. The full suite is not green; unrelated tests/source were left alone.
- Integrated target `gradlew.bat jar --max-workers=4` passed with its preserved local equipment edits. No tests ran after client launch. No GameTests or in-game checks were added/run for this integration.
- Ordinary `runClient --max-workers=4` launched in the target worktree after all test workers exited. Startup confirmation is recorded below; no world was opened by the agent.
- Code-guide integration snapshots updated in README, fluid-runtime, equipment, client-gui and verification, from the actual merging source. Existing constitutive science unchanged. No tracked tooling cleanup was needed; offline renderer and evidence remain under canonical `tools/pipeline-shutoff-valve/`.
- Plan/reviews/render sheets and guide files are canonical in main's ignored documentation tree, also copied to the owned and integration worktrees. Future main integration still requires the owner-selected version and repository checks.

## Startup

Client PID 32844 is running with window title `Minecraft NeoForge* 1.21.1`; render/resource atlases, Flywheel shaders and OpenAL sound initialized. The agent opened no world and sent no desktop input. Client Gradle session 78368 remains running.

## Evidence

`tools/pipeline-shutoff-valve/logs/pipeline-shutoff-valve-merge-logs-2026-10-23.tar.gz`: required-gate log, JUnit XML and failure summary, target package log, merge/preservation manifest and client startup log snapshot. The earlier feature-test/render archive remains unchanged.
