# chunk-loading-review

Purpose: verification logs of the chunk-loading review and fixes. The batch created no probes, scripts or rigs.

Batch: `2026-10-26-chunk-loading-review` ([../../documentation/2026-10-26-chunk-loading-review/README.md](../../documentation/2026-10-26-chunk-loading-review/README.md)).

## Contents

`logs/chunk-loading-review-logs-2026-10-26.tar.gz`, from the worktree `chunk-loading-behavior-1796a8` (`build/chunkload/`):

- `test.log`: `./gradlew test`, 1447 tests, 0 failures.
- `gametest.log`: `runFluidGameTestServer -PfluidGameTestRunId=chunkload1`, 22 of 23. `registeredPipesAndReservoirsKeepSimulatingAcrossUnloadedChunks` failed on a drum another test left registered.
- `gametest-base.log`: the same task on unmodified `main` `9eeeb1f4` (`chunkloadbase`), 21 of 21.
- `gametest-diag.log`: `chunkloaddiag`. A temporary assertion message (not kept) named the leftover `RESERVOIR`.
- `gametest2.log`: `chunkload2`, 23 of 23, after `EquipmentCompatGameTests` removes its drum.
- `gametest-proof.log`: `chunkloadproof`. With the `hasChunkAt` guard in `FluidDeviceBlock.connectedState` and the `cancelRequest` call in `ColumnCalculatorV3BlockEntity.setRemoved` removed (by `sed`, then restored byte-identical), exactly the two `ChunkLoadGameTests` failed.
- `test-slowgates.log`: `./gradlew test slowGates` at `7147d431`. `slowGates` 187 tests in 18 classes, 0 failures; `test` up to date (1447, 0 failures).

## How to reproduce

From the worktree root, one Gradle run at a time: `./gradlew test`, then `./gradlew runFluidGameTestServer -PfluidGameTestRunId=<fresh id>`.

## History

- 2026-10-26: created with the logs above.
- 2026-10-26: added `test-slowgates.log` (merge prep).
- 2026-10-26: concluded; the batch merged as 0.13.2 (`600549d9`).
