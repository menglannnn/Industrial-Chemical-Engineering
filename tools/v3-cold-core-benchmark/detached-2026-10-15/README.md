# Detached Java worker (2026-10-15)

Detached from the tracked test tree on 2026-10-15 by the code consolidation (items D1 and S20). Batch documents: `documentation/2026-10-15-code-consolidation/`. The worker served the 2026-09-05 cold core benchmark described in the parent `README.md`; no gate task and no documented Gradle task ran it, so it left the code under the tooling cleanup rule.

Removing commit: `8283ae9f` ("consolidate: D1 S20 detach the V3 cold-core benchmark worker"), on branch `worktree-agent-a5b779bfda02b988e`; its parent is `5f3e23f2`.

Contents:

- `src/V3ColdCoreBenchmarkWorker.java`: the JSONL worker the Python supervisor starts in each bounded JVM. It was `src/test/java/com/wormzjl/createcheme/science/column/v3/V3ColdCoreBenchmarkWorker.java`.
- `src/V3ColdCoreBenchmarkWorkerTest.java`: its one unit test (closure keeps the accepted truncation mass defect). It was `src/test/java/com/wormzjl/createcheme/science/column/v3/V3ColdCoreBenchmarkWorkerTest.java`.
- `src/v3-cold-core-v1.json`: the benchmark manifest. It was `src/test/resources/v3-benchmarks/v3-cold-core-v1.json`.
- `remove-cold-core-worker.patch`: `git format-patch` of `8283ae9f`. The stored copies equal `git show 5f3e23f2:<path>` for the three paths above.

Re-attach on a checkout at `5f3e23f2` (or any later commit where the patch still applies):

```powershell
git apply -R tools/v3-cold-core-benchmark/detached-2026-10-15/remove-cold-core-worker.patch
```

or copy the three files back to the paths above. Then run the supervisor as the parent `README.md` describes; the classpaths it needs are compiled from the test source set.

Later commits of the same consolidation changed APIs the worker calls. On a checkout after them the worker needs these edits before it compiles:

- `V3SolverFailureCode.DEADLINE_EXCEEDED` was removed (K5, `6c9d1f87`). A public-call deadline now surfaces as the thrown `CancellationException`, which the worker already maps to `DEADLINE_EXCEEDED`; drop the failure-code comparison.
- The 13- and 14-argument `V3ColumnInput` constructors were removed (C9). In `V3ColdCoreBenchmarkWorker.input(...)` append `List.of(), V3ColumnInput.PRESCRIBED_DROP_DIAMETER` to the 13-argument call (draws, steam). The 11-argument constructor the worker test uses and the 3-argument `V3ColumnCalculator.calculate(input, control, cutoff)` the worker calls are still there (C6 removed only the 2- and 6-argument `calculate`).
