# Cold 430 final-code balance trace

Batch `2026-10-24-wti-pumping-rate-hold`; source `cb89f23ac31f67933caeaf0fb5e11aa92f8612d4`. Purpose: reproduce the existing eight-block cold-430 world on final code and record balance iterations plus physical states without changing a numerical decision. Review: `documentation/2026-10-24-wti-pumping-rate-hold/COLD430_TRACE_REVIEW.md`.

- `balance-trace.patch`: temporary read-only logging against the source above; no product change is retained.
- `cold430-gradle.txt`: successful focused Gradle invocation with the trace flag enabled.
- `cold430-trace.xml`: one passing no-capability-hold test, including the printed E6 hold; this is not a keep-up result.
- `../logs/cold430-trace-logs-2026-10-24.tar.gz`: archived evidence in the canonical tool folder.

`BITER`, `BSTEP`, `BACCEPT` and `BSWEEP` describe the balance iterations. `BDUMP` follows re-seeding: its `BNODE net` and some cap data belong to the preceding balance basis, so use `finalResidual`, not `BNODE net`, for convergence. All seven traced connections are passive. The trace does not print raw Jacobian diagonals; a prototype must verify the proposed regularization trigger.

## Reproduction

Use an owned worktree at the source above, with no retained solver edits. Use JDK 21.0.11; this focused run measures no wall-time budget and uses at most four workers. Do not run it alongside another measurement lane.

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11"
export PATH="/c/Program Files/Java/jdk-21.0.11/bin:$PATH"
git apply --check tools/wti-pumping-rate-hold/cold430-trace/balance-trace.patch
git apply tools/wti-pumping-rate-hold/cold430-trace/balance-trace.patch
JAVA_TOOL_OPTIONS="-Dcold430.trace=true" ./gradlew fluidRuntimeTest --tests "com.wormzjl.icheme.runtime.fluid.HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold" --max-workers=4
```

Preserve the generated test XML and console output, then restore the source even if the invocation fails:

```bash
git restore --source=HEAD -- src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java
git diff --exit-code -- src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java
./gradlew compileJava --max-workers=4
```

Never attach the traced class to a production or broad-test classpath. The patch adds console output only; it does not add a residual evaluation or alter acceptance. The test's assertion intentionally permits E6 and rejects only a phase-capability hold. No fix, quick check, campaign or dev-client check is part of this tool run.
