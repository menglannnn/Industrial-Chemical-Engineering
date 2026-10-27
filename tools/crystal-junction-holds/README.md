# crystal-junction-holds (batch `documentation/2026-10-20-crystal-junction-holds/`)

t56 (Opus, 2026-10-20) on `claude/crystal-junction-holds` from `a08c7134` (0.9.1). Nothing here is tracked. Lane: the javac lane of
`tools/unified-thermo-port-m5/d40/lane.sh` in the worktree; quick checks with `tools/fluid-solver-simplification/quick-lane.sh`.

Status: the batch merged to `main` as 0.9.2 (`449d398c`, 2026-10-20; not pushed). The batch put no probe, rig or instrumentation into the tracked tree, so nothing was detached and no re-attach commit applies; the gates it added (`HoldClassWorldsTest`, `SolidRuntimeTest.aSubnormalMassIsNoStock`) stay tracked. The four rule-off patches apply on `449d398c` (`git apply --check`, t58, 2026-10-20).

| Path | What |
|---|---|
| `patches/rule-off-junction-melt.patch` | D61 off (`JunctionCrystals.of` returns null). `HoldClassWorldsTest#aCrystalArrivingInAWarmJunctionMeltsThere` (cold 778, 7 blocks) then holds E4 at 1185, bitwise the base (1.9329645729104783e-7). Applies on `30b11f2f` and later. |
| `patches/rule-off-sealed-junction.patch` | (d) off (the balance sweep bisects a sealed junction too). `HoldClassWorldsTest#aSealedJunctionTheBalanceHasBalancedKeepsItsPressure` (cold 743, 14 blocks) then holds E5 at 11358. Applies on `be5c9101` and later. |
| `probes/probe-743-closure-balance-on-a08c7134.patch` | The 743 trace probes on `a08c7134`: interval-start closure loop (`T56CLOSE`, `T56TRANS`, `T56SEG`, `T56START`, `T56FAILSEG`), cold rate seed (`T56COLD`), rate-solve failures (`T56RATEFAIL`), pass seeds (`T56SEED`), balance closures (`T56BAL`) and sweeps (`T56SWEEP`), with t54's `T54*` probe; and `T56WorldTest` (743's world, `-Dt56.end`). Switches `-Dt56.close`, `-Dt56.seed`, `-Dt54.fail`, `-Dt54.pass`. |
| `patches/rule-off-closure-carry.patch` | D62 off (`carried` returns null). `HoldClassWorldsTest#aClosureNothingMovesCarriesAcrossTheIntervalStartInATankDrainingToAVoid` (cold 183, 10 blocks) then holds E5 at 2379. Applies on `7b810779` and later. |
| `patches/item3-forms-switch-on-2bacf29b.patch` | The comparison build of item 3 on `2bacf29b`: forms OFF, R (with a rate solve only), A, B (with `PassiveStepSolver.drivenFlow`) and C selected by `-Dt56.form` (default C), the switch the quick check ran. |
| `patches/rule-off-row-seed.patch` | Round 2 (t56b) off: a junction nothing reaches is restated at its old temperature, as before. `HoldClassWorldsTest#aJunctionPutAtANewPressureIsStatedAsWhatItsRowMixes` (the full cold 743 world, 22 blocks) then holds E5 at 13758. Applies on `34c79758` and later. |
| `logs/t56b-logs.tar.gz` | Round 2: `logs2/q/` quick check `final2` (with round 1's `base0`), `c2/` clean-in-both counters and named runs, `ps/` one-worker per-seed counters, `w/` the full-743 traces (`full-close.txt`, `full-seed.txt`), `s-r2/` suites and comparisons, `gradle-results/` and `gradle-test2.txt` the full Gradle `test`. |
| `logs/m20-logs.tar.gz` | t57 (2026-10-20), the Opus final lane at `bb9de943` (`FINAL_TEST_REVIEW.md`, GREEN): `00-lane.txt`, full test `01-*` (`01-results` = the `--rerun` run, `01-results-uptodate` the lane's replay), `compare-junit-xml.txt` vs m19, G2 `02-*`, GameTests `03-*`, campaign `04-*`/`05-*`, `cmp-seeds.txt`, alone `06-*` (cold 694, 833; bundled 157), per-seed one-worker `07-perseed/` (javac exports t57c/t57b = `bb9de943`, t57mb = `fea62919` defaults), clean-in-both 8-worker `07-clean/` (t57c/t57b against m16 exports t48nc/t48nb), `clean-both-*.txt`, `counters-*.txt`, `scripts/cost-t57.sh`; extract with `tar --force-local -xzf`. |
| `logs/t56-logs.tar.gz` | `q/` quick checks (`base0` = `a08c7134`; `qOFF`, `qA`, `qB`, `qC` the forms; `final` = `7b810779`; `qb-*`/`qB-*` collided on a case-insensitive disk and hold form B only), `c/` clean-in-both counters and named runs (`run-clean.sh` of t51), `ps/` one-worker per-seed counters (clean in both, defaults, named, the 573 bisection `z573-*`), `w/` traces (743 probes, 163 underflow, 183 gate with and without the carry, 573 solve logs, full 743 blowdown), `pair/` the 183 pair minimization, `s-base/` and `s-f/` suite logs with comparisons, `gradle-test.txt` the full Gradle `test`. Extract with `tar --force-local -xzf`. |

Re-run, from the worktree root (Git Bash):
```
D=tools/unified-thermo-port-m5/d40
bash $D/lane.sh export t56p a08c7134 D:/Minecraft/Modding/1.21/CreateChemE/tools/crystal-junction-holds/probes/probe-743-closure-balance-on-a08c7134.patch
bash $D/lane.sh compile t56p com/wormzjl/createcheme/runtime/fluid/T56WorldTest.java
JAVA_OPTS="-Dt56.close=1 -Dt56.seed=1" bash $D/lane.sh run t56p com.wormzjl.createcheme.runtime.fluid.T56WorldTest
bash $D/lane.sh export t56x <sha> D:/Minecraft/Modding/1.21/CreateChemE/tools/crystal-junction-holds/patches/rule-off-<x>.patch   # then compile and run the one gate
```

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Thread t56: rule-off patches `rule-off-junction-melt.patch` (D61), `rule-off-sealed-junction.patch` ((d)), `rule-off-closure-carry.patch` (D62), `rule-off-row-seed.patch` (round 2); the three-form comparison switch `item3-forms-switch-on-2bacf29b.patch` (OFF/R/A/B/C by `-Dt56.form`); the 743 trace probes `probes/probe-743-closure-balance-on-a08c7134.patch` with `T56WorldTest`; logs `logs/t56-logs.tar.gz`, `logs/t56b-logs.tar.gz`, `logs/m20-logs.tar.gz` (t57 final test, GREEN); nothing detached from the tracked tree (the batch added no probe or rig to it); the gates stay tracked; the four rule-off patches apply on `449d398c`

Batch: `2026-10-20-crystal-junction-holds`

Status: Implemented 2026-10-20. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `patches/rule-off-*.patch`, `logs/m20-logs.tar.gz`; gates `HoldClassWorldsTest` (five 0.9.2 worlds), `SolidRuntimeTest.aSubnormalMassIsNoStock`; re-run a gate with its rule off through `tools/unified-thermo-port-m5/d40/lane.sh export <id> 449d398c <patch>`

### History

- Implemented 2026-10-20 (merged in 0.9.2, main `449d398c`, not pushed; final test m20 green)
- nothing tracked
