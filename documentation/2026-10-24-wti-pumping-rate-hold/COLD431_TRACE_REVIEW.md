# Cold 431 matched-state and solid-transition trace

2026-10-24. Diagnostic work on `cb89f23a`, branch `kimi/remaining-holds-physics-review`. Form 2 remains rejected. Copied-source instrumentation only; no product physics, test, deadline or save format changed.

## 1. Expected physical behaviour

A nonzero carrier flow below the suspension velocity deposits solids and may close that connection; the next segment must conserve the remaining inventory and continue. Exactly zero flow transports no solids and has no donor or direction, so it cannot create a new direction-dependent deposition event. An existing bed may nevertheless remain closed under the established D62 carry rule.

A 0.05 s online-time slice should advance conservatively or reject a numerical trial and backtrack. Repeatedly refining an exactly stationary connection does not locate a physical event. A wall deadline measures exhausted work, not an impossible thermodynamic state.

## 2. What the code did instead

The natural cold-431 prefix does not compare matched states. Baseline island 42 returned `FULL` on four attempts (about 0.550, 0.287, 0.235 and 0.227 s; slices 0.05, 0.1, 0.2 and 0.4 s). Form 2 returned `HELD: wall deadline` after about 2.001 s on all four retries of the first 0.05 s slice. Their tick-2158 graph digests differ (`9ac3b36f…` versus `81b54dfe…`): junction id 4 is liquid-filled in baseline but almost entirely vapour in form 2, with other inventory and pressure differences.

Injecting one baseline checkpoint into both runs produced the same restored graph digest (`91d2007f…`) and empty cached result. From that matched input, baseline and form 2 both returned `FULL`, in about 0.546 and 0.549 s. The restored digest differs from natural baseline (`9ac3b36f…`), so this proves only the controlled A/B: form 2 does not inherently stall this topology or baseline state. Its own earlier trajectory creates the difficult state.

On form 2's natural state, detailed tracing found no invocation of its new targeted regularized-row bisection. The deadline instead followed this work (`edge-candidate-run.log`):

| Segment | Accepted / other rejected steps | Transition refinements | Final declared closure |
|---|---:|---:|---|
| 1 | 6 / 5 | 18 | edge 8, pipe `1112950731065182303` |
| 2 | 0 / 3 | 10 | edge 7, pipe `629273826774253692` |
| 3 | 0 / 0 | 13, all at exactly `0 m/s` | edge 0, pipe `761801541324892508` |
| 4 | deadline during first attempt | 0 | none |

The first two cascades refine real, decreasing nonzero carrier velocities toward thresholds around 1.3–1.4 m/s. They are not repeated reopening of one closure. Segment 3 instead solves 13 converged trial steps, halving from 0.003125 s to 7.62939453125e-7 s, while edge 0's flow remains signed zero and its reported liquid velocity is exactly zero.

This is possible only with `still == null`: `SolidEventIntegrator.still(double[])` would classify zero as still, and `failed` would skip it. With no interval-start rate result or carried flow, `failed` keeps the sign-reading fallback; `flow >= 0` then manufactures a donor at signed zero, and `SolidMobility.check` reports `DEPOSITION` because `0 <= requiredVelocity`. The controller treats that as an in-step event instead of a stationary state.

Thus the balance form's regression is upstream trajectory divergence. The separate transport fallback amplifies its cost; neither the trace nor a larger deadline validates form 2.

## 3. Proposal for owner validation

**Recommended separate diagnostic:** leave `BalanceNet` and rejected form 2 untouched. In `SolidEventIntegrator.failed`, after the filter-capacity check but before selecting a direction/donor, treat `flow == 0` as no new transport transition. This applies the existing physical still-pipe contract without inventing a tolerance, and leaves nonzero fallback flows, filter clogging and D62 closure carry unchanged.

Prototype only after approval. Add a focused no-rate/no-carried-flow signed-zero regression beside nonzero-deposition and carried-closure controls; then replay cold 431 and the existing still-pipe/closure gates. It is not yet known whether removing these 13 trials clears the world, because segment 4 and island 37 remain unvalidated.

If the objective is instead to salvage a balance form, first trace its earliest accepted state divergence from baseline. Do not create a fourth form or increase the wall budget from this evidence alone.

Evidence: `tools/wti-pumping-rate-hold/cold431-trace/` and `tools/wti-pumping-rate-hold/logs/cold431-trace-logs-2026-10-24.tar.gz`. No campaign or repository gate was rerun. Code guide: no change.
