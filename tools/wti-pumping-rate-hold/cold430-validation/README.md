# Cold 430 targeted-balance validation

Batch `2026-10-24-wti-pumping-rate-hold`, branch `kimi/remaining-holds-physics-review`, baseline `cb89f23ac31f67933caeaf0fb5e11aa92f8612d4`. This folder contains the three rejected cold-430 candidate patches, copied diagnostic harnesses, matched comparisons and complete budget replays. No tracked source or gate switch remains; there is no removing commit or re-attachment step. Review: `documentation/2026-10-24-wti-pumping-rate-hold/COLD430_FIX_REVIEW.md`.

## Candidates and outcome

- `candidate.patch`: form 1, targeted regularized-row bisection accepted on any strict merit improvement. Solver SHA-256 `d7dbc2d72c78450c196090973a26727180e962530fc79df174416b988de33f56`; rejected after cold 555 acquired E5.
- `candidate-armijo.patch`: form 2, the same trigger with the existing full-step Armijo decrease. Solver SHA-256 `d28a3b6d96565d61964213318d3d9d0ef12d7debd53b1a780c4cdf30c0f4350e`; rejected after isolated cold 431 retained seven baseline versus eleven candidate held publications.
- `candidate-best.patch`: form 3, Newton and bisection evaluated from the same point with the lower accepted merit retained; rejected by the focused cold-430 gate. No campaign was run.
- All patches apply against the baseline above and include the proposed strengthened gate. The gate was not retained. `negative-control-strict/` and `negative-control-armijo/` record disabled-rule failures and byte-exact source restoration; the duplicate `negative-control/` output belongs to form 2.

`validate.py` compiles the copied cold/bundled harnesses and baseline solver overrides, then runs matched seeds with counters. `summarize_armijo.py` compares form 2 with the fresh 1-200 baseline and preserved exact-current 401-600 cohort baseline. `trace_world.py` is verbose diagnostics only: its removed-island `describe` call truncated cold 431 and its output must not decide a budget comparison. `trace_candidate555.py` uses a copied instrumented solver, not product instrumentation.

`replay_budget.py` runs selected seeds alone with one worker, no counters and no verbose diagnostics; its candidate hash check pins form 2. `audit_results.py` reads the retained XML, JSON and world reports, checks the decisive counts and restored Java diff, and writes `validation-summary.json` without running a solver. Raw reports and manifests are authoritative; summaries do not replace them.

## Reproduction

Use an owned worktree at the baseline, JDK 21.0.11 and no dev client. Run wall-budget replays alone; run no other Gradle measurement concurrently. Campaign runs use four workers here (the one completed form-1 full campaign used eight). Preserve completed reports instead of rerunning them.

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11"
export PATH="/c/Program Files/Java/jdk-21.0.11/bin:$PATH"
./gradlew -I tools/wti-pumping-rate-hold/cold430-validation/classpath.gradle testClasses cold430ValidationClasspath --max-workers=4
python tools/wti-pumping-rate-hold/cold430-validation/validate.py compile
# Select exactly one patch, then restore both Java files before selecting another.
git apply --check tools/wti-pumping-rate-hold/cold430-validation/candidate-armijo.patch
git apply tools/wti-pumping-rate-hold/cold430-validation/candidate-armijo.patch
./gradlew fluidRuntimeTest --tests "com.wormzjl.icheme.runtime.fluid.HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold" --max-workers=4
python tools/wti-pumping-rate-hold/cold430-validation/negative_control.py
python tools/wti-pumping-rate-hold/cold430-validation/replay_budget.py baseline cold-random 431,475 isolated-budget-baseline-armijo-nonverbose
python tools/wti-pumping-rate-hold/cold430-validation/replay_budget.py candidate cold-random 431,475 isolated-budget-candidate-armijo-nonverbose
```

The negative-control script restores the solver in `finally`; preserve/rename its output between candidate forms. After any candidate or diagnostic run:

```bash
git restore --source=HEAD -- src/main/java/com/wormzjl/icheme/science/fluid/network/PassiveStepSolver.java src/test/java/com/wormzjl/icheme/runtime/fluid/HoldClassWorldsTest.java
./gradlew compileJava --max-workers=4
python tools/wti-pumping-rate-hold/cold430-validation/audit_results.py
```

Never put copied trace classes on a production or broad-test classpath. `quick-suite.log` and `slow-gates.log` belong to form 1 and contain only the five established unrelated baseline failures. Form 2 has matched 1-200 and 401-600 evidence, not a full campaign or another repository-gate lane. `restored-baseline-final.log` records the successful final baseline compilation. Compiled classes and Python caches are regenerable and excluded from `../logs/cold430-validation-logs-2026-10-24.tar.gz`.
