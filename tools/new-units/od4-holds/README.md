# od4-holds (batch 2026-10-15-custom-equipment-rendering, 2026-10-20)

Purpose: trace and fix of the two holds rule OD4 exposed (bundled fuzz seeds 145 E7, 889 E9); review
`documentation/2026-10-15-custom-equipment-rendering/OD4_HOLDS_REVIEW.md`, `MOVED_REFERENCES.md`. Nothing here is tracked.

- `lane.sh`, `tree.sh`: the Gradle-free javac lane (copy of `unified-thermo-port-m5/d40/`), pointed at the
  custom-equipment-rendering worktree, exports under `/tmp/od4-<name>`, with the Create, Ponder (catnip) and Flywheel jars
  on the classpath (the branch's client code needs them).
- `quick-lane.sh`: t38's 200-seed quick check on that lane (`bash quick-lane.sh <export> <label> [seeds] [dists]`).
- `gradle-locked.sh`: one Gradle run under the build/gradle.lock protocol (copy of the m21 lane's).
- `od4-holds-fix.patch`: the fix against `8f9009b3` (PassiveStepSolver cycle key + PH arriving seed, gate Javadoc, CHANGELOG).
- `probe/pass-trace-on-8f9009b3.diff`: the temporary trace (`-Dod4.trace=true`: per pass column ends, modes, flows,
  pressures; nonfinite rows; arriving seeds; `-Dod4.ph=true` the PH prototype). Apply to a lane export only.
- `probe/Od4LayoutProbeTest.java`: compiled beside the bundled hold-fuzz harness, prints a seed's blocks with bores
  (`-Dseed=889`).
- Logs: `../logs/od4-holds-2026-10-20.tar.gz` (`build/od4-logs/`: traces t145*, t889*, replays w145-889, quick checks q1
  (crystal-competing PH, rejected) and q3 (final), the 8-seed form comparison c8-*, Gradle g1 and its results, the gate's
  rule-off proof g1off-gate.txt).

## Round 2 (2026-10-20, after final lane m22): `r2/`
- `r2/prep.sh <name> <tree-ish> [fix2off]`: `lane.sh export`, optionally fix 2 of `f983306b` off (`r2/fix2off.pl`, the
  m21 TP flash), then the trace (`r2/trace.pl` with the snippets `r2/arrive.java.txt`, `r2/fail.java.txt`;
  `-Dr2.trace=true`: per pass layout and seeds, each junction's arriving PH and TP split with the stock, buffer and
  property-state enthalpies, the failing rows and states of a refused pass, a suction row's infinite vapour pressure).
  Tree-ish of the worktree's uncommitted state: `"$(bash tree.sh)"`. Apply to lane exports only.
- `r2/rowB.pl`: the route B experiment (the suction limit's vapour pressure read where the step's start state holds
  liquid water too), applied after `prep.sh ... fix2off`: 889 E9 -> E2.
- The knife-edge proof edited the exported TP line in place (`weighted/mass*(1+eps)`, eps 1e-12, 1e-9, -1e-9); see
  `OD4_HOLDS_REVIEW.md`.
- Logs: `../logs/od4-holds-r2-2026-10-20.tar.gz` (`r2-logs/`: traces t583*, t322*, t889*, d34-* (the D34 gate), the
  six-seed replays w6buf, perturbation runs pert-*, `full-test.gradle.txt` and `full-results/` (1571 tests), the
  comparisons `cmp-vs-m20/m21/m22.txt`, the 200-seed quick check q200, and `scripts/` with the two comparison scripts).

## Round 3 (2026-10-20, the D34 gate's upper-tank trace condensate): `r3/`
- No tracked source changed; see `r3/README.md`. `r3/prep.sh` (export + r2 trace + r3 probes `probe.pl`, `passes.pl`,
  `segments.pl`), `r3/perturb.pl` and `r3/robust.sh` (the seven-variant robustness matrix: D34 gate + fuzz 322),
  `r3/prototypes/` (the forms tried, export-only; `volumeshare.pl` is owner option (a)).
- Logs: `../logs/od4-holds-r3-2026-10-20.tar.gz` (`r3-logs/`).

## Round 4 (2026-10-20, after release 0.10.0): cold 285 and the 889 gate proposal: `r4/`
- No tracked source changed; see `r4/README.md`. Review `OD4_COLD285_GATE889_REVIEW.md`. Prototypes P2-P2d (a junction's own split
  is its own rows), the 889 pair minimization and the draft gate `r4/gate/ArrivingSuctionWaterGateDraftTest.java`.
- Logs: `../logs/od4-holds-r4-2026-10-20.tar.gz` (`out/`); seed 170 traces (base, P2c, a6a305a1) in `../logs/od4-holds-r4-s170-2026-10-20.tar.gz`; balanced-proposal runs (quick checks, alone replays, seed-pressure factor probe, BALC call count) in `../logs/od4-holds-r4-balanced-2026-10-20.tar.gz`. The factor probe and BALC print were export-only edits (`t170.f` scaling of the junction branch's pressure in phaseCheck; a print after the balance in `balancedProposal`). Round 4 final lane (`48fb5678`, NOT GREEN): `../logs/final-lane-48fb5678-2026-10-20.tar.gz` (m23 lane scripts re-pointed, campaign reports, alone replays, 509 traces); `r4/prototypes/p3-arrival-balanced-vapour-against-48fb5678.patch` is the arrival form of the balanced seed, which does not clear 509 (balance 168 kPa).

## Round 5 (2026-10-20): the seed pressure of a flashing junction, `ca21bab8`
- Review `FLASH_SEED_REVIEW.md`. The fix is committed (`PassiveStepSolver.flashSeeds`, `settledFlash`, `restateFlashFlows`, pressure-ordered phase check); nothing tracked is detached.
- Logs: `../logs/flash-seed-ca21bab8-2026-10-20.tar.gz` (`stage-flash/`): `final-lane/` (m23 lane on ca21bab8: full test, chain-100, GameTests, three campaign reports with classes, `lane2-stdout.txt`), `alone/` (al-ls1..ls4: the four forms on the named worlds; al2-*: 475, 183, 729 on base0 and the ca21bab8 tree), `quick/` (200-seed checks of the last two forms, targeted 21 classes on ca21bab8 with its reference lines), `probe/` (729, 757, 592 traces, the seed-pressure factor runs g592-*), `scripts/` (the javac lane scripts of /d/tmp_285 and `flash-bisection-form.java`, the first form: root by bisection, which fails 592).

## Round 6 (2026-10-20): cold seed 729, `ff71d370`: `r5/`
- Review `COLD729_REVIEW.md`. The fix is committed (`PassiveStepSolver.arrivingPhases`: a junction nothing reaches is split from its stock alone).
- `r5/probe.pl`: on an r2-traced export (`/d/tmp_285/scripts/prep.sh` style), adds `R2 TOPO` (node kinds and edges whenever the island order changes), `R2 GATE` (the equation gate's largest rows and the booked states), `R2 NOVARS` and `R2 SOLIDIN` (solid streams arriving at a junction during the gate's residual: donor, crystal share, melting, the donor's population list). Export-only.
- `r5/named.sh <export> <label>`: the named worlds (13 cold, 7 bundled) on one worker.
- `r5/prototypes/a-junction-crystals-island-labels-against-ca21bab8.patch`: prototype A of the D61 label finding (committed in round 7 as `416f8601`, with `2def39dc`).
- Logs: `../logs/cold729-2026-10-20.tar.gz` (`stage-729/`: traces t729/p729 on `ca21bab8`, p729b on base, q729p with prototype A; alone runs of forms A, B, C; named worlds nC; quick check qC; targeted jC2; Gradle chain-100 r5-logs).

## Round 7 (2026-10-20): D61 junction-crystal labels and the final lane, `416f8601` + `2def39dc`: `r5/`
- Review `D61_LABELS_REVIEW.md`. Prototype A committed (`JunctionCrystals` splits an unlabelled stream over the island's populations); `2def39dc` holds that basis as a list (the `SolidInventory` of `416f8601` capped it at 64 and failed `SolidTransportAcceptanceTest`). Nothing tracked is detached.
- `r5/fallback-probe.pl`: on a probed export (`prep.sh` + `probe.pl`) with A, prints `R2 FALLBACK` where the split falls back to the island's populations with a melting share. Export-only.
- Logs: `../logs/d61-labels-final-lane-2026-10-20.tar.gz` (`stage-d61/`): `final-lane/` (m23 lane on `416f8601`: full test with results, chain-100, GameTests, three campaign reports with classes, `06-targeted-2def39dc` with results, `lane-stdout.txt`), `alone/` (994 on `ff71d370` and with A, one worker; one-worker bundled 200 of forms C and A, `q1/`), `quick/` (A's 200-seed quick check), `probe/` (traced 994 with A `pa994`/`pf994` and on `ff71d370` `pc994`; 729 with A and the certificate print `a729p`).

## Round 8 (2026-10-20): cold seed 994, `37797661`: `r6/`
- Review `COLD994_REVIEW.md`. The fix is committed (`PassiveStepSolver.laidOutFor` in the arrival check); nothing tracked is detached.
- `r6/seed-residual.pl`: on a probed export (`prep.sh` + `r5/probe.pl`), prints before each pass at dt < 1e-3 the residual at the pass's seed point (`R2 SEEDRES`) and per junction the seed against its stock (`R2 JSEED`: T, P, phase code, amounts, seed and stock specific enthalpy). Export-only.
- `r6/arrive-codes.pl`: in `arrivingPhases`, the seed, engine and own-split phase codes and the mixture's water per junction (`R2 ACODE`). Export-only.
- `r6/prototypes/d1-equal-phase-sets-against-2def39dc.patch`: form D1 (all phases equal), which drops D10's water trace (bundled 157 E4); not applied.
- Logs: `../logs/cold994-2026-10-20.tar.gz` (`stage-994/`): `alone/` (994 on D1, D2; 183 and 157 on D1 and `2def39dc`; `s994/` A and C), `quick/` (200-seed checks of D1 and D2), `probe/` (`ps994` seed residuals with A, `p157d1*` gate and arrival codes on D1), `targeted/` (21 classes on `37797661`), `named/` (13 cold + 7 bundled), `chain100-fl4/` (Gradle chain-100).
- Round 8 final lane (`37797661`, NOT GREEN by cold 654 B, `FINAL_LANE_37797661_REVIEW.md`): `../logs/final-lane-37797661-2026-10-20.tar.gz` (`stage-fl5/`: `final-lane/` m23 lane with full test results, chain-100, GameTests, three campaign reports with classes and `lane-stdout.txt`; `alone/` cold 6, 183, 651, 654, 673, 698, 994 and bundled 678 on `2def39dc` (alD, blD) and `37797661` (alE, blE), one worker each).
## Round 9 (2026-10-20): cold seed 654, `bde9013c`: `r6/`
- Review `COLD654_REVIEW.md`. The fix is committed (`PassiveStepSolver.balanceStart` closes a one-way line only off a converged balance); nothing tracked is detached.
- `r6/entry-gap.pl`: at each step solve's entry, every junction whose property state and buffer differ by more than 1 kJ/kg (`R2 ENTRYGAP`). Export-only.
- `r6/arrive-flows.pl`: in `arrivingPhases`, round 0, each junction's start inflows with their caps and law flows (`R2 AFLOW`). Export-only.
- `r6/start-closures.pl`: the connections closed for the solve after the start closures, and the balance's convergence (`R2 CLOSED`). Export-only.
- `r6/balance-probe.pl`: where the balance's pressure solve ends unconverged, its budget, residual and per-junction net inflow (`R2 BALANCE`). It restores the loss inversion's warm start (`lastQ`); without that the probe alone turned 654 clean. Export-only.
- `r6/unconverged-closures.pl`: each one-way closure the balance start took off an unconverged balance on `37797661` (`R2 UCLOSE`). Needs `R2T` declared (the r2 trace, or one line added by hand). Export-only.
- `r6/seed-residual.pl`: its missing closing brace (round 8) is fixed in the stored copy.
- `r6/prototypes/`: `d3-always-own-split-against-37797661.patch` (form D3), `e-junction-seed-is-mixture-against-37797661.patch` (form E), `f-closure-on-converged-balance-against-37797661.patch` (form F, committed as `bde9013c`).
- Logs: `../logs/cold654-2026-10-20.tar.gz` (`stage-654/`): `trace/` (pa654 base, pe654 and pg654 on `37797661`, pf654 on form E with every r6 probe, u654/u729/u743 unconverged closures and balances on `37797661`), `alone/` (d3, e1, f1 with 157 on `37797661`), `quick/` (qF), `named/` (nH), `targeted/` (kH), `chain100/` (Gradle chain-100).
## Round 10 (2026-10-21): balance-start convergence, `a6b14b22`: `r7/`
- Review `BALANCE_CONVERGENCE_REVIEW.md`. G1 and G2 are committed as `a6b14b22`; G3 is stored only (owner decision pending). Nothing tracked is detached.
- `r7/r7-balance-iter.pl`: per Newton iteration of `BalanceNet.solveOnce`, the merit, step and accepted fraction (`R2 BITER`) and why a sweep ends the solve (`R2 BSWEEP`). For an unconverged one-unknown balance it also scans its net inflow around the end point (`R2 BSCAN`, `lastQ` restored). Export-only; it needs `R2T` (from `prep.sh`).
- `r7/r7-rest-balance.pl`: at each balance start, its convergence, the largest junction pressure shift and the smallest and largest law flows (`R2 BSTART`). Export-only; `R2T` declared by hand on a plain export.
- `r7/prototypes/`:
  - `r7-g1-runflow-bracket-against-bde9013c.patch` (G1, the inversion's open-bracket test);
  - `r7-g2-runflow-bracket-and-slope-against-bde9013c.patch` (G1 + G2, committed as `a6b14b22`);
  - `r7-g3-suction-floor-against-a6b14b22.patch` and `r7-g3b-suction-floor-anchor-against-a6b14b22.patch` (G3's two shapes: one `suctionFloor` statement; both flip the 889 gate's pump label at the corner).
- Logs: `../logs/balance-convergence-2026-10-21.tar.gz` (`stage-bal/`):
  - `trace/`: b743 iterations and scans, g* on G1 with probes, d* with 1e-9 slopes, p994 on G2, pump-g1, rest-kG1 and rest-kI;
  - `alone/` (g2), `quick/` (qG2), `named/` (nI), `targeted/` (kI);
  - `gates/`: the two failing gates on G1, G2 and G3.

## Round 11 (2026-10-21): still pipe 850 on the converging balance, `aca82e8f` + `H`: `r8/`
- Review `STILL850_REVIEW.md`. G3 is committed as `aca82e8f` (`r7/prototypes/r7-g3-and-gate889-against-a6b14b22.patch` is G3 plus the 889 gate's new assertion). Nothing tracked is detached.
- All r8 probes are export-only, applied with `perl -i <probe> <export>/src/main/java/.../network/<file>`:
  - `r8/s850-probe.pl` (SolidEventIntegrator.java): the interval-start rate solve (`S850 RATE`: null or not, still pipes, largest flow) and every refused mobility check (`S850 REFUSE`: pipe, flow, still flag, reason, velocity and its minimum). The step-state dump at the first refusals (`S850 RFLOWS/RNODE/RPIPE`) was added by hand on export `s850b`.
  - `r8/s850-rate-probe.pl` (PassiveStepSolver.java): why `portRate` fails (`S850 RATEFAIL`).
  - `r8/s850-bstart-probe.pl`: every balance start, rate or step (`S850 BSTART`).
  - `r8/s850-bdump-probe.pl` (after the previous one): the first four unconverged rate-solve balances dumped whole (`S850 BDUMP/BNODE/BEDGE`).
  - `r8/s850-biter-probe.pl`: `r7-balance-iter.pl` retagged `S850` and keyed on `rateOnly`.
- `r8/prototypes/r8-h-still-carried-against-aca82e8f.patch`: `H` (still pipes from `PassiveIntervalSolver.carriedFlows` where the rate solve fails).
- Logs: `../logs/still850-2026-10-21.tar.gz` (`stage-850/`: `trace/` the s850 probe runs on G1 (`s850a`), G2 + G3 (`s850b`) and H (`s850f`); `quick/` qH; `targeted/` kL).

## Round 12 (2026-10-21): cold 430 and bundled 889 on the converging balance, `d139fe93` + `8d69949d`: `r9/`
- Review `COLD430_889_REVIEW.md`. Nothing tracked is detached; `HoldClassWorldsTest.aFailedIterateRaisesNoPhaseHold` now asserts keep-up.
- All r9 probes are export-only, applied with `perl -i <probe> <export>/src/main/java/.../network/PassiveStepSolver.java` on an export that carries the r2 trace (`-Dr2.trace=true`):
  - `r9/s430-bdump-probe.pl`: `../r8/s850-bdump-probe.pl` keyed on the first four balances of any kind (430's mint).
  - `r9/s889-bdump-probe.pl`: the same, keyed on islands holding a pump (889, the starved suction).
  - `r9/mixed-probe.pl`: every water-only call of `BalanceNet.mixed`, its inputs and answer (`R9 MIXED`).
  - `r9/jseed-probe.pl`: each pass's junction seeds, liquid and vapour water, T and P (`R9 JSEED`).
- `r9/prototypes/` (all against `e5357594`): `r9-a-sweep-first` (A, committed as `d139fe93`), `r9-ab` (A + B, the engine's PH for water, rejected), `r9-ac` (A + C, fails the starved suction), `r9-acd-final` (A + C + D, the committed state with the gate change). A + D without C was an in-export edit of `r9-acd-final` (the bisection's midpoint restored).
- Logs: `../logs/cold430-889-2026-10-21.tar.gz` (`stage-430/`: `trace/` the starved-suction traces on A (`stm-pS0`) and A + C (`stm-pS`) and the 430 traces; `gates/` the gate runs per form and the one-test proofs; `quick/` the two worlds per form, qACD and the alone replay rACD; `targeted/` kP).
- Final lane logs: `../logs/final-lane-8d69949d-2026-10-21.tar.gz` (`stage-fl7/`: `final-lane/` the fl7 logs with `06-alone`, the lane and alone outputs, `attribution/` cold 311 and 567 and defaults 397 one seed each on `d139fe93` (`tA*`) and `8d69949d` (`tACD*`)). Review `FINAL_LANE_8D69949D_REVIEW.md`.
- Owner decision 2026-10-21, option (b): A reverted as `c7b883cb`; A stays only as `r9/prototypes/r9-a-sweep-first-against-e5357594.patch`. Final lane logs: `../logs/final-lane-c7b883cb-2026-10-21.tar.gz` (`stage-fl8/`: `final-lane/` the fl8 logs with `06-alone`, the lane and alone outputs). Review `FINAL_LANE_C7B883CB_REVIEW.md`.

## Lane scripts as used through rounds 8-12 (2026-10-21, merge prep on `c7b883cb`): `scripts-2026-10-21/`
- `final-lane/`: the measurement lane run from the worktree root, alone: `LOGS=build/<run>-logs RUNID=<run> bash lane.sh <sha> test regression gametest campaign`. Gradle goes through `gradle-locked.sh`, which refuses while a dev client runs. Also `fuzz.sh` (the campaign harness), `alone.sh` (the one-worker batch), `cmp-seeds.pl <label> <base report> <new report>` (newly held, cleared, changed class, new C/F) and `cmp-perseed.pl` (per-seed counters of one-worker reports). The scripts were kept under `D:/tmp_fl/scripts`, and their paths name it.
- `javac-lane/`: the Gradle-free lane, kept under `D:/tmp_285/scripts`.
  - `lane.sh export|compile|run`: exports are git-archived to `D:/tmp_285/x/<name>`, with patches as absolute paths.
  - `quick.sh <export> <label> [seeds] [dists]`: the 200-seed check and one-seed runs.
  - `targeted.sh <export>` with `targeted.txt`: the 149 targeted classes.
  - `maxrel.pl`: printed-line moves between two runs.
  - `msdiff.pl`: per-seed ms rises between two campaign reports.
  - `prep.sh`: exports carrying the r2 trace.
- Merge prep 0.10.1 (`144321e7`): gate logs `../logs/merge-prep-0.10.1-2026-10-21.tar.gz` (`stage-mp/`); review `MERGE_PREP_0_10_1_REVIEW.md`.
