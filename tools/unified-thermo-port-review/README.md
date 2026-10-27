# unified-thermo-port-review

Purpose: the probes of the design review of the unified thermo port (`documentation/2026-09-24-coolprop-low-temperature/
UNIFIED_THERMO_PORT_REVIEW.md`, 2026-10-16), batch `2026-09-24-coolprop-low-temperature`, on the M3 head `76e87740`.
Read-only on the repository: every tree is a `git archive` export into `/tmp/review-<name>`, the probe and the rule-off
edits are applied to the exported copy only, compiled with javac on the index lane (no Gradle), run one JVM at a time at
-Xmx6g while no dev client is live. Nothing here is a gate and nothing here was ever tracked.

## How to run

    bash scripts/export.sh 76e87740                       # /tmp/review-76e87740 (NAME= another folder, PATCH= staged patches)
    NAME=head PROBE=1 bash scripts/export.sh 76e87740     # with the probe class and probe/apply-probe.pl
    perl probe/apply-probe2.pl /tmp/review-head           # probe 2 on top (what the TP answer added; how the own split ended)
    perl probe/apply-probe3.pl /tmp/review-head           # rule-off on top: an unconverged own closure adds nothing
    bash scripts/index-compile.sh head                    # javac: every main source, then the cold harness
    bash scripts/run-seed.sh head 48 <label>              # one cold seed as the campaign runs it (120 slices), counters,
                                                          # the RPROBE tables at JVM exit; logs in /tmp/review-logs (LOGS=)

The lane scripts are the M3 arrival tool's (`tools/unified-thermo-port-m3-arrival/scripts/`) pointed at `/tmp/review-*`;
`harness/HoldFuzzTest.java` and `trace/M3ArrivalTrace.java` are copies of that tool's (the harness references the trace
switch; the switch stays off: no `m3arr.*` property is set).

## Contents

| Path | What it is |
|---|---|
| `scripts/export.sh`, `index-compile.sh`, `index-run.sh`, `M3Run.java` | The index lane on exported trees |
| `scripts/run-seed.sh` | One cold seed, one worker, counters, waits while a dev client is live |
| `harness/HoldFuzzTest.java`, `trace/M3ArrivalTrace.java` | The M3 cold harness and the (off) trace switch it references |
| `probe/ReviewProbe.java`, `probe/apply-probe.pl` | Probe 1: phaseCheck calls and own-split Newton iterations by call-site path |
| `probe/apply-probe2.pl` | Probe 2: the phase the TP answer asked for (stand-in or not, node holds solids) and how the own split ended |
| `probe/apply-probe3.pl` | Rule-off: `split` returns null on nonconvergence and `phaseCheck` adds nothing without an own split |
| `logs/review-logs.tar.gz` | The runs' stdout and reports (see "Runs") |

## Runs
All on `76e87740` exported with the probe, cold seed as the campaign runs it (120 slices), one worker, alone (no dev client,
other JVMs were M4's Gradle daemons; cost judged by counters, not wall time). Counters are bitwise reproducible across runs
(seed 48: 4 012 976 Newton iterations in runs 01, 02 and 04).

| Run | Tree | Seed | What it measured | Result |
|---|---|---|---|---|
| 01 | probe 1 | 48 | phaseCheck calls and own-split iterations by call site | 142 079 own splits, 3.41 M of 4.01 M Newton iterations inside them (step-conv 2.28 M, step-fail 1.11 M, polish 17 k); refresh closures 3.8 k; 114 holds |
| 02 | probe 1+2 | 48 | what the TP answer asked for; how the own split ended | every ask a vapour in a vessel holding solids (stand-in in 5); 141 989 stalls at the iteration cap, 79 line-search stalls, 11 converged |
| 03 | probe 1+2+rule-off | 48 | an unconverged own closure adds nothing | 1 991 iterations, 50 attempts, but held C (methane floor) after 2 accepted steps: 40 closures adding a real liquid to a gas tank stall too |
| 04 | probe 1+2 (buckets) | 48 | the stalled iterates: residual, vapour and liquid volume shares, solids share | 98 % at residual ~1e-6 with the vapour at <=1e-8 of the capacity or none; liquid 0.1..1, solids 0.01..0.1 of the capacity |
| 05 | probe 1+2 (buckets) | 183 | the same on the second budget seed | 15 490 own splits, 4 029 converged; stalls at residual ~1 drive a proposed liquid to 0 or 1e-11 in a gas tank (vapour 0.1..1 of capacity, frost 1e-6..1e-5); the stand-in asked 6 387 of 15 490 times |
| 06 | probe 1+2 (room buckets) | 48 | the room left in the bag and the vapour mole share of the stalled iterates | room <= 0 in every stall (liquid plus solids fill the capacity); vapour ~1e-9 of the moles (82 017) or none (57 673); 4 010 909 iterations: one wall-deadline hold replaced a substep limit (the JVM ran 23 % slower) |

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: The general design review of the unified thermo port (Fable, max effort): six single-seed index-lane probes of seeds 48 and 183 (own-split stalls, rule-off forms), probe class, patchers, archived logs

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`

### History

- Done 2026-10-16 (review `UNIFIED_THERMO_PORT_REVIEW.md`)
