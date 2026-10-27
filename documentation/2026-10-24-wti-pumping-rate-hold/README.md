# 2026-10-24-wti-pumping-rate-hold

Purpose: Correct carried mover-limit rate phase basis and report pump errors; numerical and physics-led remaining-hold inventories; cold-430 balance and cap-flat Jacobian candidates; cold-431 transition trace/signed-zero guard prototype

Status: Implemented 2026-10-26 (0.12.0). Index: [../INDEX.md](../INDEX.md).

## Documents

- [WTI_RATE_PLAN.md](WTI_RATE_PLAN.md)
- [WTI_RATE_REVIEW.md](WTI_RATE_REVIEW.md)
- [WTI_RATE_FIX_PLAN.md](WTI_RATE_FIX_PLAN.md)
- [WTI_RATE_FIX_REVIEW.md](WTI_RATE_FIX_REVIEW.md)
- [MERGE_PLAN.md](MERGE_PLAN.md)
- [MERGE_REVIEW.md](MERGE_REVIEW.md)
- [REMAINING_HOLDS_REVIEW.md](REMAINING_HOLDS_REVIEW.md)
- [REMAINING_HOLDS_PHYSICS_REVIEW.md](REMAINING_HOLDS_PHYSICS_REVIEW.md)
- [COLD430_TRACE_REVIEW.md](COLD430_TRACE_REVIEW.md)
- [COLD430_FIX_REVIEW.md](COLD430_FIX_REVIEW.md)
- [COLD430_JACOBIAN_TRACE_REVIEW.md](COLD430_JACOBIAN_TRACE_REVIEW.md)
- [COLD431_TRACE_REVIEW.md](COLD431_TRACE_REVIEW.md)
- [COLD431_ZERO_FLOW_FIX_REVIEW.md](COLD431_ZERO_FLOW_FIX_REVIEW.md)

## Code areas

Carried mover-limit basis, scheduled pump errors, numerical tags, physical scenarios, minimized-world evidence, `BalanceNet.solveOnce` validation, `PassiveStepSolver` diagonal regularization and `SolidEventIntegrator` guard/diagnostics; formats unchanged

## History

- Implemented 2026-10-26 (0.12.0, `2178a7c2`, fast-forward, not pushed
- in progress since 2026-10-24 (integrated `cb89f23a`
- production-fix 1200 matched cases without added holds
- audits concluded 2026-10-24: 12 cases published holds, four recovered/eight still held
- earlier cold-430 balance forms 1-3 rejected: form 1 added cold-555 E5, form 2 retained isolated cold-431 7 -> 11 publications, form 3 failed cold 430 at tick 406
- separate non-positive-diagonal candidate retained with a strengthened keep-up gate: focused gate passes and disabled-rule control fails at tick 406 as intended, matched seeds 1-200 in all distributions add no held seed/class/publication regression, and the selected 1000-seed/all-distribution campaign completes with cold/bundled-random/defaults clean counts 975/993/1000 and cold 430 at 9 -> 0 preserved-baseline holds
- quick suite 1365/1369 with four established unrelated failures
- cold-743 slow-gate failure isolated to the paused cold-431 guard, so the combined branch is not merge-ready
- owner approval of the global net-inflow/own-pressure monotonicity assumption remains required
- cold-431 exact-zero guard prototyped after tracing the separate deposition fallback, with its focused regressions and guarded target passing
- form 2 remains rejected, no fourth balance form or deadline change
- original remaining-hold order unchanged
- clean-branch final gates, code guide and main integration pending))
