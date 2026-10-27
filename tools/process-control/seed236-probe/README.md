# seed236-probe

Purpose: trace the seed-236 hold (E5, minimized nine-block world) to the failing Newton variable and residual rows.
Batch: 2026-10-26-process-control. Result: documentation/2026-10-26-process-control/SEED236_TRACE_REVIEW.md.

How to run (one Gradle lane, about 15 s, no dev client running):
1. In a worktree at `fa62fd81` (branch `claude/process-control-fixes`), apply the temporary hook: `git apply <this folder>/hook.patch`.
   It adds `PassiveStepSolver.FailureProbe` and four call sites (pass failure, pass convergence, interval rejection, closure failure).
2. `./gradlew --no-configuration-cache --offline --init-script <this folder>/trace.init.gradle test --tests '*Trace236Test'`
   The init script attaches `../bundled/src` (the bundled hold-fuzz harness) and `src/`.
3. Revert the hook: `git checkout -- src/main/java`. Never commit it.

Entry points: `src/com/wormzjl/icheme/runtime/fluid/Trace236Test.java` (world and probe switch-on at `trace236.from`, default 5580),
`src/com/wormzjl/icheme/science/fluid/network/Probe236.java` (labels unknowns and rows, prints the failing point, the Jacobian column's
trials, the closure rows and a TP flash of the closed inventory). `M5TraceWorld.java` is the `trace/` copy.

`Probe236.gateFailed` (the fix check) needs one more hook line, not in hook.patch: in `PassiveStepSolver`, before the "Conservative reconstruction fails equation gate" throw, `{var probe=failureProbe;if(probe!=null)probe.gateFailed(equations,reconstructed,projection.states());}`, and the method in `FailureProbe`.

History: 2026-10-27, created by Claude (Opus 5.5) for the seed-236 diagnosis. Logs: `../logs/seed236-trace-logs-2026-10-27.tar.gz`. Same day: gateFailed and per-node layout sizes added to check the fix (SEED236_FIX_REVIEW.md).
