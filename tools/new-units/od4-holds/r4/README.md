# od4-holds round 4 (2026-10-20): cold 285 and the 889 gate proposal

Batch 2026-10-15-custom-equipment-rendering, review `documentation/2026-10-15-custom-equipment-rendering/OD4_COLD285_GATE889_REVIEW.md`.
Nothing here is tracked; no tracked source was changed. Scratch exports went to `D:/tmp_285/x/<name>` (delete after use).
The lane's classpath is the custom-equipment-rendering worktree's `build/moddev` (read only); exports are `git archive` of a commit.

- `scripts/lane.sh`: copy of `../r3/lane.sh` exporting under `D:/tmp_285/x/`. `scripts/prep.sh <name> <tree-ish> [patch...]`: export,
  the r2 pass trace (`../r2/trace.pl`) and `trace285.pl` (phaseCheck print, `-Dt285.trace=true`); `trace285b.pl` adds the junction's
  PH consistency print (`-Dt285.ph=true`: crystal and fluid-only engine answers, TP at the own split's temperature).
- `scripts/run.sh <export> <label> [seeds]`: the cold harness, one worker, verbose. `scripts/quick.sh`: `../quick-lane.sh` on this lane.
- `scripts/pairmin.sh <seed> <class> <held> <clean> <label>`: `tools/fluid-solver-simplification/trace/lane-pair-minimize.sh` on this lane;
  exports compiled with the bundled harness, `tools/unified-thermo-port-m5/trace/src/{M7CandidatesTest,M5MinimizerTest,M5SolveLogTest,M5TraceWorld}.java`
  and (to print the specs) `tools/junction-seeding/src/T44KeepPrintTest.java`. Held export: `2e28a20a` + `../r2/fix2off.pl` (the m21 TP-flash arrival).
- `prototypes/p2.pl <export>`: the proposed rule (a junction's own split is its own rows closed at its P and h,
  `PhaseLayout.junctionClosure`, `PassiveStepSolver.junctionSplit`; a failed closure proposes nothing; the arrival seed uses it).
  `p2b.pl`: the same with the arrival closure only where the engine answer adds a phase (the measured form).
  `p2c.pl` (on top of p2b): the closure reads the D38 C vanishing rule on each iterate and stops refused (the recommended form, P2c).
  `p2d.pl` (on top of p2b+p2c): the closure owned by `FluidThermodynamics.junctionClosure`, used when phaseCheck gets no split (the
  structural placement; `NetworkPhaseEngineTest` then differs only by the 2-ulp junction pressure). `p2count.pl`: closure counters
  printed at JVM exit (`T285COUNT`). Apply order: `p2.pl` or `p2b.pl`, then `p2c.pl`, then `p2d.pl`; `p2count.pl` after p2/p2b.
- `scripts/targeted.sh <export>`: the 21 targeted classes (`targeted.txt`) on an export.
- `gate/ArrivingSuctionWaterGateDraftTest.java`: the draft gate (minimized bundled 889, 11 blocks), compile beside an export's tests.
- Logs: `../../logs/od4-holds-r4-2026-10-20.tar.gz` (`out/`: traces t1 (phaseCheck), t2 (PH consistency), p2-285, the named-world
  replays `q/w-*`, the 200-seed quick checks `q/q-*`, the minimization `min/`, the gate runs `gate-*`, the OD4-off control).
