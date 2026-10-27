# Cold 430 Jacobian-sign trace and matched validation

Batch `2026-10-24-wti-pumping-rate-hold`, branch `kimi/remaining-holds-physics-review`, baseline `cb89f23ac31f67933caeaf0fb5e11aa92f8612d4`. Review: `documentation/2026-10-24-wti-pumping-rate-hold/COLD430_JACOBIAN_TRACE_REVIEW.md`.

This folder separates the cold-430 cap-flat diagonal defect from the three rejected bisection forms in `../cold430-validation/`. It contains read-only tracing, one non-positive-diagonal candidate, a strengthened keep-up gate, matched campaigns and controlled solid-class isolation. It does not overwrite the older validation evidence.

## Retained patch and outcome

- `final-sign-floor.patch` is the isolated product diff: `PassiveStepSolver.java` plus `HoldClassWorldsTest.java`, with no cold-431 file. SHA-256 `cf07265e8481063d419a1d6dbc9d624d89547932c81a9a5324c51b3366050d3c`.
- Candidate solver SHA-256 `39b0794a869bfd136f57687479b8e4f855c60c679d30d0124b5bde8e155e6f47`; `validate_sign_floor.py` refuses to compile a different candidate.
- `jacobian-trace.patch` and `cold430-jacobian-trace.xml` preserve the removed diagnostic and raw row/edge evidence. `candidate-sign-floor.patch` is the solver-only prototype.
- The focused candidate passes; the strengthened assertion fails at tick 406 with the solver restored. The quick matched comparison is clean. One full selected-candidate campaign is preserved, not to be rerun.
- The combined worktree remains blocked by the separate cold-431 solid guard. This is not hidden by the solver result; see the isolation evidence below.

No commit removed tracked diagnostic code. Re-attach diagnostics with `jacobian-trace.patch` against the baseline; use `final-sign-floor.patch` for the isolated candidate. Never put copied baseline or trace classes on a production or broad-test classpath.

## Entry points and evidence

- `validate_sign_floor.py`: compiles the existing cold/bundled harnesses from `../cold430-validation/harness/`, builds baseline solver overrides from `cb89f23a`, and runs serial matched arms. Both arms share current non-solver classes, including the paused cold-431 guard.
- `compare_full.py`: writes `campaign/full-comparison.json`, comparing every seed and publication with the preserved historical and rejected-form reports. Historical full reports predate final scope and are watchlist evidence, not an exact new baseline.
- `replay_sign_floor_budget.py`: one-worker, counter-free replay for only the new budget-class seeds. Seeds 244 and 855 were batched; both arms leave 244 clean and publish the same nine seed-855 deadlines.
- `trace_sign_floor_world.py`: counter-free diagnostic world runner, never a cost comparison. Baseline and candidate cold 743 produce the same five E6 records.
- `campaign/gate-candidate-solver-baseline-solid.log`: the candidate passes the cold-743 gate with `HEAD`'s `SolidEventIntegrator` substituted. `gate-baseline-solver-current-solid.log`: the baseline solver fails with the current solid guard, isolating the extra slow-gate failure from this candidate.
- `quick-summary.json`, `campaign-summary.json`, raw `*-report.txt` and classifier files are authoritative. Compiled classes, generated baseline sources and Python caches are regenerable.

## Reproduction

Use JDK 21.0.11, an owned worktree and no dev client. Run each arm serially. Quick runs use four workers; the one selected full campaign uses eight and runs alone. Do not run another full baseline or replay worse non-budget seeds for cost.

```bash
export PATH="/c/Program Files/Java/jdk-21.0.11/bin:$PATH"
# Apply final-sign-floor.patch only to a clean baseline worktree.
./gradlew --no-configuration-cache -I tools/wti-pumping-rate-hold/cold430-validation/classpath.gradle testClasses cold430ValidationClasspath --console=plain --max-workers=4
python tools/wti-pumping-rate-hold/cold430-jacobian-trace/validate_sign_floor.py compile
python tools/wti-pumping-rate-hold/cold430-jacobian-trace/validate_sign_floor.py run baseline cold-random 1-200 4
python tools/wti-pumping-rate-hold/cold430-jacobian-trace/validate_sign_floor.py run candidate cold-random 1-200 4
# Repeat quick baseline/candidate for bundled-random and bundled-defaults.
python tools/wti-pumping-rate-hold/cold430-jacobian-trace/validate_sign_floor.py quick-summarize
python tools/wti-pumping-rate-hold/cold430-jacobian-trace/compare_full.py
```

`validation-gradle.log` preserves the init script's configuration-cache failure; `validation-gradle-no-config-cache.log` is the successful build. `focused-sign-floor-final.log`, `quick-suite-sign-floor.log` and `slow-gates-sign-floor.log` record the final repository checks and their known/shared failures.

Compressed evidence: `../logs/cold430-jacobian-trace-logs-2026-10-24.tar.gz`. The archive excludes compiled classes and caches. Code guide: no change. No GameTest, client, commit, merge or version/changelog action was taken.
