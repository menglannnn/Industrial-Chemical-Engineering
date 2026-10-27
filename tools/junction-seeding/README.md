# junction-seeding (t44)

Purpose: thread t44's prototype and measurements for one junction seeding mechanism (owner request of 2026-10-19), batch
`documentation/2026-10-19-junction-seeding/` (`START_MACHINERY_MAP.md`, `JUNCTION_SEEDING_PLAN.md`). Nothing here is
tracked or committed; every patch was applied to `git archive` exports of `32bf3fa4` on the Gradle-free javac lane
(`tools/unified-thermo-port-m5/d40/lane.sh`), never to a worktree. Nothing lands without the owner.

## Contents

| Path | What |
|---|---|
| `patches/form-a-balance-start.patch` | Form A on `32bf3fa4` (applies cleanly to a fresh export): the island-wide balance (`PassiveStepSolver.BalanceNet`, `balanceStart`, `balanceStarts`, `needsBalance`, `initialModes`) as the start of every solve without a carried point and of the mint; the probes it carries (`-Dt44.trace`, `-Dt44.iter`, `-Dt44.count`) and the t40 pass, Newton and step probes (`-Dcreatecheme.fluid.trace`, `-Dt40.steps`). Switches: `-Dcreatecheme.start.balance=false` (the old machinery), `=probe` (the balance computed for its diagnostics, the old machinery starts the solve: the rule-off proof on the same export), `-Dcreatecheme.start.hull=true` (form B, the Newton's junction hull guard), `-Dcreatecheme.start.balance.every=true` (form C, the balance at every solve), `-Dcreatecheme.start.rounds=N` (composition rounds, 2) |
| `scripts/classify-run.sh` | Replays seeds through `M5SolveLogTest` (`tools/unified-thermo-port-m5/trace/src`) in probe mode with the start diagnostics printed: `classify-run.sh <export> "<harness switches>" <outdir> <parallel> seed...` |
| `scripts/classify-parse.pl` | Per replay log, the first held solve and the start diagnostics of its first attempt: `T44START` (the static estimate's junction imbalance, junctions outside their hull, lines on the wrong side of their cap, the largest pressure move the balance makes, its iterations and convergence) |
| `scripts/t44-balance-block-first-version.java.txt` | The first version of the balance block (before the density tables, the mode decisions and the budgets), for the record |
| `logs/t44-logs.tar.gz` | `logs/`: the baseline quick check `b0-*`; the form quick checks `a3-*`, `b3-*`, `c3-*` (frozen build) and `a4-*`, `b4-*`, `c4-*` (final build; the earlier attempts `a1`, `a2`, `b1`, `b2` were stopped on the balance's cost); the 40-seed cost probes `probe/p40-p43`; the javac-lane suites `suites/` (baseline `t44b-*`, forms `t44as-*`, `t44as2-*`, `t44a2-*`, `t44a3-*`, `t44a4-*`, with `cmp-*` comparisons); the probe-mode classification replays `classify/` (first build, wall-deadline artefacts) and `classify2/` (budgets lifted); the named runs of the candidate seeds `named/` (frozen build) and `named2/` (final build, the campaign's budgets, 2 workers) and `named3/` (the quick check's new holds and the cold rest replayed alone, budgets lifted); the counters on the seeds clean in both trees `clean/` (`both-*.txt` the seed lists, `cb-*` baseline, `ca-*` form A); the traced worlds `smoke/` |

## How it was run (worktree root, Git Bash; `D=tools/unified-thermo-port-m5/d40`, `P=tools/junction-seeding`)

```
bash $D/lane.sh export t44b 32bf3fa4 && OUT=<logs> THREADS=4 bash tools/fluid-solver-simplification/quick-lane.sh t44b b0 1-200
bash $D/lane.sh export t44a 32bf3fa4 $P/patches/form-a-balance-start.patch && bash $D/lane.sh compile t44a all
LOGS=<logs>/suites bash $D/run-touching.sh t44a science-fluid-thermo science-fluid-network runtime-fluid
OUT=<logs> THREADS=4 EXTRA_OPTS="-Dt44.count=true" bash tools/fluid-solver-simplification/quick-lane.sh t44a a3 1-200        # B: EXTRA_OPTS="-Dcreatecheme.start.hull=true"; C: "-Dcreatecheme.start.balance.every=true"
bash $D/lane.sh compile t44a <cold harness> <M5 trace sources>; bash $P/scripts/classify-run.sh t44a "-DholdFuzz.cold=true -DholdFuzz.defaults=false -Dreplay.budgets=600000000000:590000000000" <out> 2 19 29 ...
JAVA_OPTS="-DholdFuzz.cold=true -DholdFuzz.defaults=false -Dreplay.seed=15 -Dreplay.keep=<KEEP> -Dreplay.slices=6 -Dcreatecheme.start.balance=true -Dt44.trace=true" bash $D/lane.sh run t44a com.wormzjl.createcheme.runtime.fluid.M5SolveLogTest#replay
```
The pair-minimal KEEP lists of cold 6, 15, 199 are in `tools/fluid-solver-simplification/trace-d3/`, of bundled 988 in `../trace/README.md`.
The exported files are CRLF; `lane.sh export` converts the files a patch touches before applying it.

## t44b: the build on `claude/junction-seeding` (2026-10-19, from `32bf3fa4`)

Owner decisions on the plan: land form A with the cost target as its gate (cold seed time at most 1.3x the baseline's on the
200-seed quick check, no rise in the Newton's counters on the seeds clean in both trees), the gas-column band applied, the
balance deciding pump start modes. Commits: `c623b29b` the balance with its deletions; then the mitigations and the gates
(see `git log claude/junction-seeding`). Measured on the javac lane against `32bf3fa4` (`t44b` exports), 8 workers, alone and
back to back for the timing pairs; the owner's geosim JVMs ran throughout.

| Path | What |
|---|---|
| `scripts/t44b-edit1.pl`, `t44b-edit2.pl` | The rework of the prototype patch into product code (verified line ranges, content-keyed edits): the record of commit `c623b29b` |
| `scripts/t44b-revert-b.pl` | The revert of mitigation (b), tables only on choked outflows (measured, dropped) |
| `scripts/run-b1.sh`, `run-clean.sh`, `run-final.sh`, `run-final2.sh` | The measurement drivers: suites plus quick-check pair plus clean-in-both counters; the clean-in-both counters of one export; the gates, band test and alone timing pairs; the gates on the final tree and on `32bf3fa4` plus the quick-check pair |
| `src/T44KeepPrintTest.java` | Prints a fuzz seed's kept placements (`-Dprint.seed`, `-Dprint.keep`) as `EventPathWorld.place` lines with exact specs: the gate worlds' transcription |
| `patches/gates-test.patch` | `JunctionSeedingGateTest` alone, applied to a `32bf3fa4` export: the rule-off proof (the old start machinery holds 988 and 19, refuses seed 15's first interval) |
| `patches/rule-off-balance.patch` | The balance disabled on the final tree (`needsBalance` false, the mint keeps placeholders): the per-link law start without the balance; it does not reproduce the baseline's holds on 988 and 19 (their holds came from the old estimators the balance replaced), so the proof is the baseline tree |
| `scripts/t44b-fallback.pl`, `run-named.sh` | The fallback edits (`33455d4c`: the cold rate seed started from the balance) and the named-runs driver (not run: the gate was missed) |
| `scripts/t44b-mint.pl`, `run-mint.sh`, `run-mint2.sh` | The mint form (`c51df66a`: D55's supplier composition as the balance's round 0 at a mint, restored from `32bf3fa4` without `balancePressure`, flashed at the suppliers' flow-weighted pressure) and its measurement drivers (mint mass on three trees, suites, quick-check pair with clean-in-both counters on three distributions, gates, seeds 4 and 123, the bundled per-seed split) |
| `probe/mint-mass.pl`, `probe/mint-mass-parse.pl` | The unbooked mint mass (D46) probe for lane exports: one `MINT` line per minted junction in `PassiveNetwork.sizeJunctionHoldups`, summed per seed from a one-worker verbose run (the baseline sizes every junction twice: its fixed point re-sizes, so its totals are double) |
| `logs/t44b-logs.tar.gz` | `mint/` the first mint form (seeds flashed at the ambient: the CO2 let-down held), `mint2/` the form landed (suites, quick-check pair, clean-in-both counters, gates, seeds 4 and 123, the mint mass of the new tree; `cmp-mint-*` the suite comparisons behind MOVED_REFERENCES.md), `mint3/` the alternating cold pairs and the new budget holds replayed alone, `mint4/` chain-100, `named/` the named runs of the start-caused list; `fb/`, `fb2/`, `fb3/` the fallback's measurements (first form, the solids write-back fix, the scaled mixtures: suites, quick-check pair, clean-in-both counters, gates, the seed 197 and bundled 4 replays, the bundled per-seed split); `b1/` commit 1 (suites, quick-check pair, clean-in-both counters); `m1`, `m1a`, `m1b` fluid-only tables; `m2`, `m3` choked-only tables (dropped); `m4/` per-seed counters (one worker) and the seed 197 replays; `m5` the mixture memo; `m6/` gates; `gates/` pair minimizations of 988 and 19 and the placement prints; `f1/` gates on balanced and rule-off, the band, the alone timing pairs; `f2/` the final gates (final tree, `32bf3fa4`) and the quick-check pair |

## t45: `trace-branch/` (2026-10-19)

Thread t45's trace of the branch selections behind the fallback's bundled Newton rise (report
`documentation/2026-10-19-junction-seeding/BRANCH_SELECTION_TRACE.md`): the branch-class pair minimizer, the export-only probe of
the balance's rounds and the cold rate seed's root, the `BALANCE_ROUNDS` and junction-placeholder switches, the seed
signatures, the counters-only quick checks and their logs. Analysis only, nothing tracked; see `trace-branch/README.md`.

`logs/m15-logs.tar.gz` (t46, 2026-10-19): the Opus final lane at `8e470a3e` (`documentation/2026-10-19-junction-seeding/FINAL_TEST_REVIEW.md`): full test `01-*`, G2 `02-*`, GameTests `03-*`, the campaign `04-*`/`05-*`, the alone replays `06-alone-*`, the clean-in-both counters `07-clean/` (javac exports `t46b` = `32bf3fa4`, `t46nw` = `8e470a3e`), the pair minimizations `08-pair/` (bundled 483, cold 271), the classifier replays `09-classify/` (budgets lifted, 120 slices), the probe traces `10-trace/` (t45 probe), `cmp-seeds.txt`, `compare-junit-xml.txt`, `check-moves.txt`, `holds-raw-*.tsv`, and the two scripts `scripts/check-moves-t46.pl`, `scripts/classify-t46.sh`; extract with `tar --force-local -xzf`.

## t44c: the two follow-ups of t46's final test (2026-10-19, `373c8fee` and `bbbd8626`)

| Path | What |
|---|---|
| `scripts/t44c-edit.pl`, `t44c-fix1.pl`, `t44c-revert-signs.pl` | The edits of the two commits (the carry across a same-graph replacement, the valve modes decided by the balance) and the revert of the sign-kept composition round that was measured and not landed |
| `patches/rule-off-carry.patch`, `rule-off-valve.patch` | The rule-off proofs: the carry disabled (483's gate holds E5 with the sign rule absent), the balance's valve rule disabled (271's gate holds E2) |
| `patches/form-sign-kept-rounds.patch` | The other half of t46's 483 proposition, a composition round kept only if it keeps the previous round's edge signs: it also keeps 483 up alone (logs `c1/gates-a.txt`); not landed, the carry is exact where it applies and the rule is a heuristic the world does not need |
| `scripts/run-c2.sh` | The measurement driver: suites plus the two classes outside them, the quick checks of the baseline `32bf3fa4`, of `8e470a3e` and of the candidate, the clean-in-both counters against both on three distributions, the named runs of t46's new held worlds plus 483 and 271, chain-100 |
| `logs/t44b-logs.tar.gz` | `c0/` the placements of 483 and 271, `c1/` the gates and the rule-off variants (a carry off, b signs off, c valve off, d both halves off), `c2/` the landed tree's gates, `c3/` the measurements |

`logs/m16-logs.tar.gz` (t48, 2026-10-19): the second Opus final lane at `798bedd9` (`documentation/2026-10-19-junction-seeding/FINAL_TEST_REVIEW.md`, section "Final test 2"): full test `01-*`, G2 `02-*`, GameTests `03-*`, the campaign `04-*`/`05-*`, the alone replays `06-alone-*` (cold 768, bundled 157), the clean-in-both counters `07-clean/` (javac exports `t48b`/`t48m`/`t48n` = `32bf3fa4`/`8e470a3e`/`798bedd9`, `c` cold and `b` bundled harness; seed lists `clean-both-m15-*` S1, `clean-both-m14-*` S2, `m14x483` S2 without 483, `t46` t46's 980-seed list; `t48ncarry`/`t48nvalve` with `rule-off-carry.patch`/`rule-off-valve.patch`, `t48nresize` with `patches/probe-resize-carried-buffer.patch`), the per-seed counters with one worker `07-perseed/`, the pair minimizations `08-pair/` (bundled 145 to 16 blocks, cold 850 to 20), the classifier replays `09-classify/` (with `mechanism.tsv`), the traces `10-trace/` (145 with the t45 probe; 850 with the probe's in-solver hooks off, `-Dt45.trace=false`, and `-Dreplay.island=34` network prints, since the hooks perturb 850: `850-probe-on-perturbed/`), the resize probe's runs of both minimal worlds `11-resize/`, the alternated cold timing pair `12-timing/`, `cmp-seeds.txt`, `compare-junit-xml*.txt`, `counters-*.txt`, `holds-raw-*.tsv`, and the scripts `scripts/` (`cmp-seeds.pl`, `mech.pl` the mechanism sort, `cmp-counters.pl`, `perseed-diff.pl`, `run-clean-t48.sh`, t46's `classify-t46.sh`). `patches/probe-resize-carried-buffer.patch` (t48): the export probe that carries a junction's state and leaves its buffer to be sized on it at the solve entry; not landed.

## t44d: the pre-merge fixes (2026-10-19, `81bf147f`, `9ce2553a`, `47391899`)

| Path | What |
|---|---|
| `detached/balance-counters/` | The six `SolverDiagnostics.balance*` counters of the balanced start and their seven `count` calls (`PassiveStepSolver.balanceStart`, `BalanceNet.solve`, `runFlow`, `seeded`, `mixed`), **removed from the tracked tree by commit `47391899`** under the tooling rule. The stored `src/main/java/.../SolverDiagnostics.java` and `PassiveStepSolver.java` are byte for byte `git show 47391899^:<path>`; `reattach.patch` is `git diff 47391899 47391899^ -- <the two files>` and re-adds them on top of `47391899` (`git apply --check` passes there). Re-attach with `git apply tools/junction-seeding/detached/balance-counters/reattach.patch` at `47391899` or a descendant where the hunks still apply; the hold-fuzz harnesses (`tools/unified-thermo-port-forms/hold-fuzz-*/src/HoldFuzzTest.java`) print them by name again once present. |
| `scripts/t44d/` | The edit scripts of the three commits (`c1-comments.pl`, `c2-format.pl`, `c2-changelog.pl`, `c3-detach.pl`, `c3-changelog.pl`), `detach.sh` (stores the sources and the patch) and `run-d.sh` (the lane checks against the `798bedd9` logs `c3/suites`), and `materials/` (the MATERIALS.md rewrite's fact table, `MATERIALS_FACTS.md`). |
| `logs/t44d-logs.tar.gz` | `d1/`: the lane checks of the final tree (suites, the two outside classes, chain-100, the comparisons `cmp-*` with the `798bedd9` logs) and the server benchmark pilot start. |

`logs/m17-logs.tar.gz` (t49, 2026-10-19): the Opus full-test re-run at `81bf147f` (`documentation/2026-10-19-junction-seeding/FINAL_TEST_REVIEW.md`, section "Full-test re-run"): the lane console `00-lane.txt`, full test `01-*` with `01-results/`, G2 `02-*`, `compare-junit-xml.txt` against m16; extract with `tar --force-local -xzf`.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Threads t44 and t45 (batch `2026-10-19-junction-seeding`, branch `claude/junction-seeding`): t44's form A prototype patch, the t44b edit, revert and driver scripts, the classification scripts, the placement printer and logs; `trace-branch/` (t45): the branch-class pair minimizer, the export-only probe of the balance's rounds and the cold rate seed's root, the `BALANCE_ROUNDS` and junction-placeholder switches, the seed signatures, the counters-only quick checks of three forms and `trace-branch/logs/t45-logs.tar.gz`; `detached/balance-counters/` (t44d: the six `SolverDiagnostics.balance*` counters removed by `47391899`, the stored sources and `reattach.patch`) and `scripts/t44d/` (the edit scripts of the three pre-merge commits, the MATERIALS.md fact table)

Batch: 2026-10-19-junction-seeding

Status: Concluded 2026-10-19 (batch in 0.9.0). Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`, `trace-branch/README.md`

### History

- t44 build measured
- t45 analysis done 2026-10-19
- Opus final lanes m15 (t46) and m16 (t48, `logs/m16-logs.tar.gz`, the probe `patches/probe-resize-carried-buffer.patch`) 2026-10-19
- nothing tracked
- t44d pre-merge fixes 2026-10-19 (`logs/t44d-logs.tar.gz`)
- Opus full-test re-run m17 (t49) 2026-10-19 (`logs/m17-logs.tar.gz`)
- implemented 2026-10-19 (merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed))
- `detached/balance-counters/` holds the six counters t44d removed from the product

### Notes

- status corrected 2026-10-26: the batch reached `main`; the folder is kept as its evidence.
