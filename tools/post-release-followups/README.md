# post-release-followups (batch `documentation/2026-10-19-post-release-followups/`)

Two threads of 2026-10-19 on `claude/post-0.9.0` (from `5b49f940`, 0.9.0). Nothing here is tracked.

- `d46/` (t52): what the unbooked junction buffers create (D46). See `d46/README.md` and `D46_MINT_MASS_REVIEW.md`.
- `t51/` (t51): the four code follow-ups and the D46 gate. See `FOLLOWUPS_PLAN.md` and `MOVED_REFERENCES.md`.

## t51
| Path | What |
|---|---|
| `t51/patches/rule-off-carry-sizing.patch` | Item 1 off on the final tree: the carry keeps the old buffer. `JunctionSeedingGateTest#aJunctionCarriedAcrossAReplacementHasItsBufferSizedOnWhatItHolds` then holds E9 at committed 3767. |
| `t51/patches/rule-off-still-pipe.patch` | Item 2 off: the still-pipe test is skipped. `HoldClassWorldsTest#aStillPipeBesideATankHoldingCrystalsDepositsNothing` then counts 1 146 solid transitions at rest and fails. |
| `t51/patches/rule-off-fingerprint-anchor.patch` | Item 4(c) off: the calibration point is dropped whatever the package. `MaterialFluidDataTest#aCalibrationPointMovesTheFingerprintOfAPackageWithoutASpineWhateverTheRecordTranslates` then fails. |
| `t51/patches/rule-off-roundoff-still.patch`, `rule-off-resolution-still.patch` | Round 3 (`235d69bb`). The first disables the interval-start still set: the 850 gate counts 1 146 transitions and fails. The second puts `50fd053f`'s D42 bound back inside the steps: the 829 gate (`aTankDrainingDownAWideLineBelowItsSuspensionVelocityClosesItAtTheIntervalStart`) holds. |
| `t51/patches/rule-off-standing-closure.patch`, `option3-formB.patch` | Option 3 (reverted in `c6b7d07b`). The rule-off patch applies on `e5042683`: `HoldClassWorldsTest#aStillPipeKeepsTheDepositionClosureItsLastIntervalLeftIt` then holds E5 at 10939. Form B applies on `e5042683`: it reads the drive across the closed pipe from a rate solve with it closed. |
| `t51/scripts/run-suites.sh` | The javac-lane suites (thermo, network, runtime, material), the extra classes, the gates and chain-100 on a compiled export, compared with t44d's `81bf147f` suite logs (`junction-seeding/logs/t44d-logs.tar.gz`, `d1/suites`). |
| `t51/scripts/run-clean.sh` | (`BX`, `NX`, `BL`, `NL` select the exports and labels; `NAMED_COLD`, `NAMED_BUNDLED` the named seeds.) After `fluid-solver-simplification/quick-lane.sh` (labels `qb` = `5b49f940` export `t51b`, `qn` = final export `t51q`): the clean-in-both counters per distribution and the named runs on both exports. |
| `t51/scripts/cmp-seeds.pl`, `cmp-counters.pl` | t48's comparisons (copies from `junction-seeding/logs/m16-logs.tar.gz`). |
| `t51/scripts/T44KeepPrintTest.java` | A copy of t44b's placement printer, used to transcribe the 145, 850 and 188 worlds. |
| `t51/logs/t51-logs.tar.gz` | `logs/`, which holds: `g1-*` and `g2-*` (gates and rule-off runs), `s1/` (suites and comparisons), `q/` (quick checks `qb`/`qn`), `c/` (clean in both, named runs), `split/` (the 8 seeds on the two rule-off exports), `alone/` (35, 72, 101 replayed alone), `trace15/` (cold 15 replayed on both trees), `d46-*` (the D46 gate on both trees), `b-850*` (850 on the baseline harness), and the second round: `pair15/` (the cold 15 pair minimization), `g3-*` and `o1-gates.txt` (option 3 and option 1 gates), `q/qo1-*` and `co1/` (option 1 quick check and counters), `q/qf-*` (the tree with option 3), `s2/` and `s3/` (suites with option 3, and on the final tree), and round 3: `r3/` (the Gradle full test with its `01-results` and `cmp-m17.txt`, the alone and lifted replays of 35 and 72, chain-100), `q/qr3-*` and `cr3/` (quick check, counters and named runs), `sp-*` (the 850 and 829 gates with their rule-offs). Extract with `tar --force-local -xzf`. |

Re-run, from the worktree root (Git Bash, `D=tools/unified-thermo-port-m5/d40`):
```
bash $D/lane.sh export t51f <sha> && bash $D/lane.sh compile t51f all && bash tools/post-release-followups/t51/scripts/run-suites.sh t51f <logs> <t44d d1/suites>
OUT=<q> THREADS=4 bash tools/fluid-solver-simplification/quick-lane.sh t51b qb 1-200   # and t51q qn
bash tools/post-release-followups/t51/scripts/run-clean.sh <q> <out>
bash $D/lane.sh export t51r <sha> t51/patches/rule-off-<x>.patch   # then compile and run the one gate test
```

## t53
- `logs/m18-logs.tar.gz` (t53, 2026-10-19): the Opus final lane at `197e1aa7` (`FINAL_TEST_REVIEW.md`, NOT GREEN: 222 V3 failures from the CDU17 fixture water without ice): full test `01-*` (compare-junit-xml vs m17), G2 `02-*`, GameTests `03-*`, campaign `04-*`/`05-*`, alone `06-*` (cold 35, 101), clean-in-both `07-clean/` and per-seed `07-perseed/` (javac exports t53c/t53b = `197e1aa7`, t48n*/t48b* = m16/m14), pair minimizations `08-pair/` (778 to 7 blocks, 829 to 8), classifier `09-classify/`, rule-off split `10-split/` (still-pipe off t53x, carry-sizing off t53y), traces `11-trace/`, `cmp-seeds.txt`, `counters-*.txt`, `scripts/`; extract with `tar --force-local -xzf`.
- `logs/m19-logs.tar.gz` (t54, 2026-10-19): the Opus final lane at `fea62919` (`FINAL_TEST_REVIEW.md`, GREEN): `00-lane.txt`, full test `01-*` (`01-results` = the `--rerun` run; `01-results-uptodate` = the lane's UP-TO-DATE replay), `compare-junit-xml.txt` vs m17, G2 `02-*`, GameTests `03-*`, campaign `04-*`/`05-*`, `cmp-seeds*.txt`, alone `06-*` (cold 35, 72), per-seed `07-perseed/` and clean-in-both `07-clean/` (javac exports t54c/t54b = `fea62919`, t48n*/t48b* = m16/m14), `counters-*.txt`, pair minimizations `08-pair/` (778 to 7 blocks, 743 to 14), classifier `09-classify/`, traces `11-trace/` (with `patches/probe-gate-pass-fail-on-<sha>.patch`, the T54GATE/T54PASS/T54FAIL/T54ACCEPT probe; rule-off runs of 778 and 743), `scripts/` (t53's plus `split.pl`); extract with `tar --force-local -xzf`.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: `d46/` (thread t52) the D46 export probe (`D46Probe.java`, `apply-probe.pl`), the quick-check driver `run-d46.sh`, `summarize.pl`, the form patch `form-fitting-volume-tau.patch` and the reference `rule-off-supplier-seeds.patch`, logs `t52-logs.tar.gz`; analysis only, see `d46/README.md`; `t51/` (thread t51) the three rule-off patches, the suite, clean-in-both and named-run drivers, logs `t51-logs.tar.gz`; `logs/m18-logs.tar.gz` (thread t53) the final lane at `197e1aa7` with its scripts; `logs/m19-logs.tar.gz` (thread t54) the final lane at `fea62919` with its scripts and gate probe patches

Batch: `2026-10-19-post-release-followups`

Status: Implemented 2026-10-19. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `d46/README.md`, `t51/`, `logs/m19-logs.tar.gz`

### History

- Implemented 2026-10-19 (merged in 0.9.1, main `a08c7134`, not pushed; final test m19 green)
- nothing tracked
