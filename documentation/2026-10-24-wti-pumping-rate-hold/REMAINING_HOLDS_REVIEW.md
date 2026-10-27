# Remaining fluid holds: sorted inventory

2026-10-24. Read-only audit of existing reports. Integration cb89f23a, numerical source 98f3dce3. No solver run, world load, product edit or client input.

## 1. Expected behaviour

Representable networks should advance while conserving components and energy, or settle to zero flow. Pump limits/refusals should produce accepted limited/zero-flow states and device errors. A numerical failure record alone does not establish a physically impossible network or justify relaxing conservation/tolerances.

## 2. Observed behaviour

Final-source coverage is seeds 1-200 and 401-600 in each of cold random, bundled random and bundled defaults: 1200 distinct distribution/seed cases. Twelve published a hold (124 repeated messages); four later recovered and eight ended with one held island each. Defaults had no holds. These are campaign observations, not a count of holds in the owner's world.

Latest published failure for the eight still-held cases, reclassified through the existing classify.sh on read-only extracts:

| Class | Cases | Distribution / seed | Description |
|---|---:|---|---|
| E5 step line-search stall | 3 | cold 6, 446; bundled 579 | No admissible Newton correction contracts the residual/merit; substep refinement also exhausts. |
| E6 step iteration limit | 2 | cold 101, 430 | Newton uses its iteration allowance without converging; smaller substeps do not complete the interval. |
| E1 initial-rate/startup stall | 1 | cold 85 | The initialization/rate solve stalls before an interval can be accepted. |
| E10 no finite Jacobian trial | 1 | bundled 430 | Finite-difference perturbations cannot produce a usable Jacobian column (latest variable 5). |
| F wall deadline | 1 | bundled 157 | The worker exhausts its wall-time allowance; this is a budget result, not proof that no physical solution exists. |

These labels identify failure stages, not proven root causes. Latest residuals: E5 cold 6 ~69.93, cold 446 ~1.46e-9, bundled 579 ~78.13; E6 cold 101 ~3.84e-6 and cold 430 ~0.7654; E1 cold 85 ~2.12e-11. Residuals are solver-scaled, not directly pressures or mass errors. Small printed values do not prove all acceptance conditions passed.

Recovered cases: cold 29 (iteration limit), cold 110 (line search), bundled 431 (line search/active-set cycles), cold 431 (line search with a non-finite-matrix rejection, later a deadline). The official first-hold inventory is E5=4, E6=4, E1=1, E10=1, B=1, F=1. Cold 446's latest tag changes E6 -> E5. Classifier B on cold 431 is triggered by "Matrix values must be finite" in the rejection map, although its primary first message is a line-search stall; underflow is not independently diagnosed.

Earlier c49f6654 full-campaign cases outside final-source coverage remain an unconfirmed watchlist (17): B cold 220/833; E1 cold 988; E5 cold 752/792/803; E6 cold 206/788/855/919/957/994; F cold 651/683/907; E2 bundled 241; E8 bundled 698. E2 means singular step Jacobians across refinement (dependent/degenerate equations need investigation); E8 means the interval's attempt allowance is exhausted before completion. This is not a final-code full sweep.

Historical cold 555, cold 475 and bundled 6 are clean in final-code rechecks. Cold 555 was the corrected passive-seeding regression; the two budget observations disappearing do not establish a performance improvement.

## 3. Proposed investigation order

1. Persistent E5/E6: five cases, the largest current group. Start a separately authorized tracing batch with cold 6 and cold 101; the latter's rejection history also names solid transport transitions. Inspect state/phase basis, active constraints, Jacobian and reconstruction before proposing a numerical/physics change.
2. E10 bundled 430: locate the invalid perturbation/property/state and keep numerical guards inside trial rejection/backtracking. E1 cold 85: inspect feasibility and stopping/merit consistency at its small reported residual; do not assume looser tolerance is valid.
3. F bundled 157: assess budget/counters in a permitted isolated lane with the dev client closed. Preserve the distinction between convergence failure and a deadline.

Evidence: canonical tools/wti-pumping-rate-hold/remaining-holds/classification-2026-10-24.json, latest-held extracts, scope-followup/result.json and referenced original reports. sort_remaining_holds.py reads these files and invokes only the existing classifier. A separate compressed classification archive preserves this audit. No fix, new root-cause trace, test, client restart or gate-scope change was performed.
