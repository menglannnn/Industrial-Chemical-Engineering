# Cold 431 matched-state and transition trace

Batch `2026-10-24-wti-pumping-rate-hold`; diagnostic base `cb89f23ac31f67933caeaf0fb5e11aa92f8612d4`. The default lane explains rejected cold-430 form 2's wall deadline without modifying tracked Java. The explicit `--zero-fix` follow-up admits only the approved `SolidEventIntegrator.java` override. Neither is a fourth balance candidate or campaign qualification.

`trace.py` reads exact `PassiveStepSolver.java` and `PassiveIntervalSolver.java` bytes from the pinned Git object. It applies only `../cold430-validation/candidate-armijo.patch` to an ignored candidate copy and requires solver SHA-256 `d28a3b6d96565d61964213318d3d9d0ef12d7debd53b1a780c4cdf30c0f4350e`. `instrument.py` adds console diagnostics gated by `icheme.cold431.trace`; `harness/` enables that property only around the selected island command. The copied classes never enter a production or broad-test classpath.

Default compilation still rejects every tracked Java diff. `--zero-fix` instead requires the sole product diff to be `SolidEventIntegrator.java`, rejects untracked product Java, verifies `PassiveStepSolver.java` is unchanged, and compiles that guard into both arms. It writes separate compile logs and `manifest-zero-fix.json`, preserving the no-fix manifest and evidence; guarded runs verify those class/source hashes.

## Prerequisite and isolation

Use JDK 21.0.11 and this owned worktree at the pinned base. The harness uses `build/cold430-validation/test-runtime-classpath.txt`. If that existing file is absent, the prerequisite from `../cold430-validation/README.md` is:

```bash
./gradlew -I tools/wti-pumping-rate-hold/cold430-validation/classpath.gradle testClasses cold430ValidationClasspath --max-workers=4
```

Do not overlap a dev client, another measurement or a large campaign. Every trace below runs one seed, one worker and no counters; `trace.py` uses a 275 s subprocess timeout and stops on a nonzero exit or incomplete prefix. Timing lines are diagnostic observations, not a comparative benchmark.

## Reproduction

Run from the worktree root. Preserve the supplied evidence rather than overwriting it unnecessarily.

```bash
python tools/wti-pumping-rate-hold/cold431-trace/trace.py compile
python tools/wti-pumping-rate-hold/cold431-trace/trace.py run baseline
python tools/wti-pumping-rate-hold/cold431-trace/trace.py run candidate
python tools/wti-pumping-rate-hold/cold431-trace/trace.py capture
python tools/wti-pumping-rate-hold/cold431-trace/trace.py matched
python tools/wti-pumping-rate-hold/cold431-trace/trace.py stage baseline --label stage
python tools/wti-pumping-rate-hold/cold431-trace/trace.py stage candidate --label stage
python tools/wti-pumping-rate-hold/cold431-trace/trace.py stage candidate --label transition
python tools/wti-pumping-rate-hold/cold431-trace/trace.py stage candidate --label edge
python tools/wti-pumping-rate-hold/cold431-trace/summarize.py tools/wti-pumping-rate-hold/cold431-trace/evidence/edge-candidate-run.log

# Approved follow-up; compilation requires the sole product override described above.
python tools/wti-pumping-rate-hold/cold431-trace/trace.py compile --zero-fix
python tools/wti-pumping-rate-hold/cold431-trace/trace.py stage baseline --label zero-fix --zero-fix
python tools/wti-pumping-rate-hold/cold431-trace/trace.py stage candidate --label zero-fix --zero-fix
python tools/wti-pumping-rate-hold/cold431-trace/summarize.py tools/wti-pumping-rate-hold/cold431-trace/evidence/zero-fix-candidate-run.log
```

`run` traces four natural attempts. `capture` saves baseline's tick-2158 state through the public `FluidCheckpointCodec`; `matched` injects it into both forms for one attempt. Checkpoint restoration is a controlled A/B input, not a claim of bitwise identity with the natural baseline. `stage` enables detailed balance/step/interval diagnostics. The final `edge` label adds segment and edge-index transition evidence; earlier `stage` and `transition` logs were produced by smaller instrumentation revisions and are retained as history. The guarded natural runs are not state-matched with the unguarded runs or each other and support no controlled speedup claim.

## Evidence and provenance

- `evidence/prefix-*`: natural states, graph digests and four-attempt results; these states are not physically matched.
- `evidence/baseline-tick2158-checkpoint/` and `evidence/capture-*`: captured baseline state.
- `evidence/matched-*`: identical restored graph digest and the baseline/form-2 `FULL` results.
- `evidence/stage-*`: initial detailed natural-state traces.
- `evidence/transition-*` and `evidence/edge-*`: unguarded candidate segment/transition traces; `edge` is the final indexed trace summarized by `summarize.py`.
- `evidence/*zero-fix*` and `zero-fix-validation/`: guarded compile/run/summary evidence and focused/closure/quick-suite logs; the quick suite's four unchanged baseline failures are itemized in the follow-up review.
- `evidence/manifest.json` and `manifest-zero-fix.json`: respective compilation provenance. The latter adds the permitted override, fixed source/class hashes and unchanged-step-solver assertion.
- `work/`: regenerated copied sources and classes; `__pycache__/`: regenerated Python bytecode. Neither is evidence or archived.

The retained reviews are `COLD431_TRACE_REVIEW.md` and `COLD431_ZERO_FLOW_FIX_REVIEW.md` in `documentation/2026-10-24-wti-pumping-rate-hold/`. Compressed evidence is `../logs/cold431-trace-logs-2026-10-24.tar.gz` and `../logs/cold431-zero-flow-logs-2026-10-24.tar.gz`. No tracked source was removed, so there is no re-attachment patch or removing commit. One product guard and its test are retained on the branch; code-guide and merge qualification remain pending.
