# Cold 431 signed-zero transport guard

2026-10-24. Prototype on `kimi/remaining-holds-physics-review`, based on `cb89f23a`. The product change is limited to `SolidEventIntegrator.failed`; rejected form 2 and `BalanceNet` are unchanged. Nothing is committed or merged.

## 1. Expected physical behaviour

Exactly zero flow transports no solids, has no donor or direction, and creates no new transport transition. This is exact, not a new roundoff tolerance. A nonzero carrier below its suspension velocity still deposits. An existing bed may remain closed under D62, and a full filter still closes before the zero-flow guard.

A 0.05 s slice may refine a real transport crossing, but repeatedly halving a stationary connection cannot locate a physical event. Other islands and other hold mechanisms must still be judged independently.

## 2. What the code did instead

When the interval-start rate solve fails and no carried flow exists, `still == null`. `failed` then used `flow >= 0` to choose a donor even at signed zero; `SolidMobility.check` returned `DEPOSITION` because zero was below the positive suspension threshold.

The unguarded form-2 cold-431 trace refined 13 such events on one edge, halving to `7.62939453125e-7` s before its next segment hit the wall deadline. The new focused control failed on the unmodified baseline at its first `-0.0` assertion, proving the fixture exercises that path.

## 3. Implemented proposal and validation

After the existing filter-capacity check, `failed` now skips `flow == 0`, covering both signed zeros. No solver deadline, mobility threshold, closure-carry rule, save format or wire protocol changed.

The new no-rate/no-carried-flow control requires no transition for `-0.0` or `0.0`, and `DEPOSITION` for nonzero `Double.MIN_VALUE`. It passes. `SolidClosureFeasibilityTest`, `SolidTransportAcceptanceTest`, `SolidChainTransportTest`, `SolidConservationTest`, the four named cold-850/829/183/15 closure regressions, and `PumpCavitationTest` all pass.

The isolated cold-431 experiment compiled the same guard into baseline and form-2 override classpaths. Form 2 retained solver SHA-256 `d28a3b6d…`; the manifest records identical guard class hashes and an unchanged working-tree `PassiveStepSolver.java`.

| Guarded natural run | Island 42 result | Segments | Zero-velocity transitions |
|---|---|---:|---:|
| Baseline `BalanceNet` | `FULL`, about 0.671 s | 2 | 0 |
| Rejected form 2 | `FULL`, about 0.836 s | 2 | 0 |

Guarded form 2 had 17 nonzero transitions in segment 1 and none in segment 2; its unguarded natural trace had four segments and hit the deadline. These runs are not state-matched: guard and solver changes alter earlier trajectories and graph digests. Therefore this is mechanism/target evidence, not a controlled speedup or campaign qualification. Form 2 remains rejected; only baseline plus the guard is proposed.

The quick suite completed 1369 tests: 1365 passed and four failed in unchanged files. Those assertions contradict the checked-in implementation: checkpoint 13 versus 17, protocol `fluid-11` versus `fluid-13`, null pump fluid cell versus the mover-nozzle contract, and no banned math versus two existing `SightGlassDisplay Math.pow` calls. They are recorded, not hidden or fixed out of scope.

**Owner proposal:** retain the signed-zero guard as the product candidate, without reviving form 2. `slowGates`, fluid GameTests and a hold campaign were not run; merge qualification, code-guide update, changelog and the owner's version decision remain pending.

Evidence: `tools/wti-pumping-rate-hold/cold431-trace/zero-fix-validation/`, `evidence/*zero-fix*`, and `tools/wti-pumping-rate-hold/logs/cold431-zero-flow-logs-2026-10-24.tar.gz`.
