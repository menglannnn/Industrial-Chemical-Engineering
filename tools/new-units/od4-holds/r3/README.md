# od4-holds round 3 (2026-10-20): the D34 gate's upper-tank trace condensate

Review: `documentation/2026-10-15-custom-equipment-rendering/OD4_HOLDS_REVIEW.md` (round 3). Nothing here is tracked; no
tracked source was changed. Scratch exports go to `D:/tmp_od4r3/<name>` (delete after use).

- `lane.sh`, `quick-lane.sh`: copies of `../lane.sh` and `../quick-lane.sh` exporting under `D:/tmp_od4r3/` (logs default
  `D:/tmp_od4r3/logs`).
- `prep.sh <name> <tree-ish> [notrace]`: export, then the r2 trace (`../r2/trace.pl`) and the r3 probes: `probe.pl`
  (SparseNewton per-iteration history and, at an iteration limit, the linear-model check `linear.java.txt`: the fresh
  direction and the residual at t = 1 .. 1e-4 against the linear model; PhaseLayout `r3describe` per component; the
  Jacobian action of a uniform split shift and of a uniform single-phase scaling), `passes.pl` (converged, folded,
  proposed, refuted passes and failed iterates with each vessel's liquid volume), `segments.pl` (a failed pass's
  segment codes and capacities). Run with `JAVA_OPTS="-Dr2.trace=true -Dr3.trace=true"`.
- `perturb.pl <export> tp|ph <eps>`: the arriving-seed perturbation (tp: the m21 TP flash at T_mix*(1+eps); ph: the PH
  split at H*(1+eps)). `robust.sh <label> <tree-ish> [variants]` with `FIX="<perl scripts>"`: the D34 gate and fuzz 322
  for each variant (default ph:0 tp:0 tp:1e-12 tp:1e-9 tp:-1e-9 ph:1e-9 ph:-1e-9).
- `prototypes/` (applied to exports only): `logamount.pl` (trace-phase single-phase amounts as logarithms),
  `branch.pl` (upwind on the Jacobian's base branch), `startcolumn.pl` (phase-port column on the start state),
  `startfill*.pl` (start fill, trial or start densities), `volumeshare.pl` (owner option (a): a hydrocarbon liquid
  nothing/vanishing below VANISHING_SHARE by volume alone; the only robust form, 7/7 + 7/7).
- Logs: `../../logs/od4-holds-r3-2026-10-20.tar.gz` (`r3-logs/`: traced gate runs d34-t0..t6, the robustness matrices
  base/startcolumn/startfill-liqstart/e1f1f2/f1f2/vol, the six-seed replay w6vol; `e5/`: the owner decision E5 on the real code: `logs/robust-real.txt` and `real-*` (the matrix), `w6real-*` (six replays), `targeted.gradle.txt`, `classes.txt`, `results/` (JUnit XML of the 19 classes), `cmp-vs-r2/m21/m20.txt` (printed-line comparisons with `../r2`'s `compare-junit-xml.pl` from the r2 archive)).
