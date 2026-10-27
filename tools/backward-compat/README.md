# backward-compat

Purpose: the final lane of batch `2026-10-26-backward-compat` (0.13.0) and its alone replays. No code was detached from a tracked path; the batch kept no probe.

Batch: `documentation/2026-10-26-backward-compat/` (review `BACKWARD_COMPAT_REVIEW.md`).

## How it was run

- The lane: `tools/floating-roof-tanks/final-lane/` (`lane.sh`, `gradle-locked.sh`, `fuzz.sh`) copied to `<worktree>/build/tmp/lane/` with `WT` set to worktree `backward-compat-system-design-11f7e0`, behind `tools/floating-roof-tanks/campaign/scripts/wait-idle.sh` (its `idle.ps1` path set to the copy):
  `LOGS=build/bc-lane RUNID=bc bash build/tmp/lane/wait-idle.sh build/tmp/lane/lane.sh 8d4c8df6`
- Alone replays of the held seeds that differed from the baseline, one worker, harnesses `tools/unified-thermo-port-forms/hold-fuzz-{cold,bundled}`:
  `THREADS=1 HARNESS=$F/hold-fuzz-cold DISTS=random EXTRA=-PholdFuzzCold=true OUT=build/bc-lane bash build/tmp/lane/fuzz.sh 06-alone-cold 431,683,833,957`
  `THREADS=1 HARNESS=$F/hold-fuzz-bundled DISTS=random OUT=build/bc-lane bash build/tmp/lane/fuzz.sh 07-alone-bundled 431`
- Rule-off check, seed 957 alone: the `WALL_RESOLUTION` line of `NetworkReduction.atRest` prefixed with `false&&`, run as `08-ruleoff-cold`, then reverted with `git checkout`.
- Baseline: `tools/floating-roof-tanks/logs/final-lane-e8ec6455-2026-10-26.tar.gz`; compare `SEED` lines with ` ms=` removed.

## Logs

- `logs/final-lane-8d4c8df6-2026-10-26.tar.gz`: `lane/` (steps 01 to 05, the replays 06 to 08, `lane-stdout.txt`), and the batch's earlier runs (`quick-w5.log`, `gametest-w5.log`, `gametest-w5b.log`, `full-w6.log`).

## History

- 2026-10-26: lane at `8d4c8df6` green (1624 tests, chain-100 exact, GameTests 21/21, campaign clean 975 / 994 / 1000 as baseline); held-seed differences were wall-clock timing (alone replays).
