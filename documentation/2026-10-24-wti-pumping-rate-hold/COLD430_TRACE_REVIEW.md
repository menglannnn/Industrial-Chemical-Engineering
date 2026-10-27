# Cold 430 balance trace and fix proposal

2026-10-24; branch `kimi/remaining-holds-physics-review`, source `cb89f23a`. Scope: final-code tracing and an owner decision, not an implemented fix. Evidence: `tools/wti-pumping-rate-hold/cold430-trace/`; temporary logging was removed and the product tree restored.

## 1. Expected physical behaviour

At tick 404 the eight-block world connects an initially empty tank (101.325 kPa, 298.15 K) to junctions fed by CO2-rich gas (4.890312 MPa, 293.362 K) and LNG (0.884476 MPa, 128.264 K). All seven traced connections are passive; this reproduction has no pump.
The gas should expand into the tank. LNG can also supply while the connected junction stands below its supply pressure; otherwise the supply-only boundary prevents backflow. Choked flow, changing phase fractions and zero reverse flow are valid regimes, not reasons to hold.
The network should therefore continue through a conserved pressure/enthalpy filling and mixing transient. Nothing in this layout requires an unrepresentable third fluid phase or a thermodynamic capability hold.

## 2. What the code did

The focused `fluidRuntimeTest` method passed its intended no-capability-hold assertion, but printed E6 at tick 406, island 13, committed tick 404. That pass does not assert keep-up.
In the first balance round, residual began at 86.0284 kg/s. Newton found no descent at iteration 2; the existing sweep reduced merit from 107.3463529 to 0.1753555452, and two further accepted Newton steps brought residual to 6.56e-5 kg/s.
The next balance round began at 10.3252845 kg/s, reached 5.5010802 after its first step, and exhausted 30 iterations at 5.501074679 kg/s with zero sweeps. Its tolerance was 1e-6 times the 13.38666569 kg/s scale, or 1.338666569e-5 kg/s; the final residual was about 4.11e5 times tolerance.
From iteration 1 onward, Newton requested steps up to 3.3167e13 Pa and later 1.2533e16 Pa. Hull limits as small as 1.91715e-10 produced negligible residual improvement: unknown 1 (node 4) alternated between about 2.496 and 4.899 MPa while node 0 barely moved. Every tiny Armijo descent was accepted, so the no-descent sweep never ran.
The unconverged point seeded the later E6 failure. This localizes the first defect to the balance's Newton/sweep scheduling, before the transient solver; it does not establish a missing thermodynamic model.
The final re-seeded graph, explicitly not a balanced root, contained these junction states:

| Node | Unknown | Pressure (MPa) | Temperature (K) | Reported fluid phases |
|---|---:|---:|---:|---|
| 0 | 2 | 0.842629 | 193.120 | hydrocarbon liquid + vapour |
| 4 | 1 | 2.495818 | 118.900 | hydrocarbon liquid |
| 5 | 0 | 0.118644 | 172.041 | hydrocarbon liquid + vapour |

These are representable states inside the pressure hull, not evidence of impossible physics. Do not read `BNODE net` as the balance residual: the dump's graph was re-seeded while that field and some cap data use the preceding balance basis; `finalResidual` is the convergence measure.
The other three recorded balance calls each converged in three iterations at residual 1.68254e-9 kg/s. They do not repair the first mint or qualify another world.

## 3. Proposal for owner validation

Preferred option: in `BalanceNet.solveOnce`, retain each unmodified diagonal and mark a row when the existing diagonal floor replaces it. If that regularized unknown's own Newton component exceeds half the pressure hull (`limit < 1` for that component), try the existing junction-pressure bisection on those flat junctions before accepting the Newton step.
Extract the per-junction bisection from `sweep` rather than duplicate it. Keep the targeted update only on strict merit improvement; otherwise run the normal Newton line search. Retain the existing no-descent full sweep, 12-sweep cap, 30-iteration cap and Armijo test, counting targeted attempts within the existing sweep budget.
This changes only which existing local model is trusted on artificial-curvature rows. It does not change flow laws, phase admission, thermodynamics, conservation, tolerances or inventory transfer.
Unlike rejected form A (`d139fe93`), this does not sweep first for every hull-limited Newton step. A was dropped after cold 311, cold 567 and defaults 397 worsened; the proposed raw-diagonal condition specifically separates regularized flat rows from large but useful Newton steps.
Risk: a finite-difference diagonal can also be small near a valid root, and coordinate bisection can disturb other junctions. The component-level hull condition and whole-island merit guard are therefore essential, not optional.
Diagnostic limit: this final-code trace recorded steps, limits, residuals and states, but not the raw diagonal substitutions. The first prototype must log/assert its trigger so the inferred flat-row condition is verified rather than assumed.
Alternative for the owner: trigger only after an accepted hull-limited step makes negligible merit progress. This avoids inspecting the raw diagonal but is broader and less localized; it needs the same guards and comparison, and is not the preferred option.

If approved, strengthen the existing eight-block gate to require keep-up through tick 504 while retaining its no-capability-hold assertion. Run that single test with only the new fallback disabled as a negative control, then compare the selected form on the 200-seed quick check across all distributions before any campaign.
At this tracing checkpoint no solver fix had been prototyped and no keep-up result was claimed. The follow-up implemented three bounded forms and rejected all of them: cold 555 acquired E5 on form 1, cold 431 retained a 7 -> 11 publication increase in the complete isolated form-2 replay, and form 3 failed cold 430 at tick 406. Both edited Java files were restored; see `COLD430_FIX_REVIEW.md` and `tools/wti-pumping-rate-hold/cold430-validation/`.
Trace evidence is archived in `tools/wti-pumping-rate-hold/logs/cold430-trace-logs-2026-10-24.tar.gz`; validation evidence is in `cold430-validation-logs-2026-10-24.tar.gz`. Code guide: no change.
