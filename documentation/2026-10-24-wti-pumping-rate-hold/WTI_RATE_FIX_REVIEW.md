# WTI pump cache and operating-error fix review

2026-10-24. Implementation 9fa53354 / c49f6654 / 98f3dce3, final presentation cb89f23a, integrated a591404f -> cb89f23a on claude/floating-roof-tanks-81d68b. Version 0.11.2.

## 1. Expected behaviour

- At 1.5 m3/s the cold WTI loop reaches its target. A 2 m3/s request can advance on the existing head-limited solution (about 1.9654 m3/s at suction, 1479.84 kg/s, 377.59 kPa rise), with a clear pump warning that requested flow is unavailable. Expected device limits/refusals do not freeze the network.

## 2. What the code did instead

- Repeated port-rate evaluation reused a flashed root's head-limit mode/flow with an all-liquid input basis. The next solve drove a trial suction toward 1 Pa and exhausted twenty refinements. The original branch was independently reproduced; see WTI_RATE_REVIEW.md.
- Pump views exposed solver mode names; cavitation/inlet refusals were not consistently ERROR-labelled. The network and device statuses were not clearly separated for the operator.

## 3. Implemented correction

- PassiveStepSolver seeds a repeated rate query from the cached output's phase layout when carrying a mover head/suction-limit mode and its numerical point on matching inputs/structure, with differing input phases. Flows, heads and phase coordinates remain consistent. Passive-loop seeding, committed starts, within-pass reuse, equations, tolerances and physical parameters remain unchanged.
- Pure PumpStatus formats accepted PUMP_HEAD_LIMIT, PUMP_SUCTION_LIMIT, INLET_WRONG_PHASE and enabled nonzero CLOSED as pump ERROR conditions; target operation and Off remain normal. FluidWorldAuthority composes these into the scheduled pump view while preserving the actual island status, supplier reason and effective limit.
- This fixes the numerical defect and reports a normal limited operating state. It does not catch arbitrary convergence failures or commit unsolved state; real solver failures remain visible. Both one-block and constructed pumps use the same accepted-state presentation. Compressor/pressure-valve treatment is retained.
- No solve on menu open/load, new per-tick work, worker/executor, custom payload, enum reorder, save/wire format or material-data change. Checkpoint 17/unit 10, fluid-13 and version 0.11.2 remain.

## Verification

- Nine focused tests passed: two new fresh WTI regressions, two pure pump-status tests, two retained-solver checks and three existing pressure-valve continuation tests. New interval regression performs eleven full intervals and independent component/energy ledgers through the outlet valve opening.
- Selected scoped complete-point negative control ran only repeatedRateCallsKeepTheFlashedPumpLimitConvergent and failed on the second null rate result. Source restored byte-for-byte; SHA-256 is in fix-validation/complete-point-negative/restoration.json. Final required merge gates compile that restored source. Earlier variants' negative evidence is historical.
- Dev client was confirmed at its empty menu and closed gracefully before any test; no world was opened/modified. No automated GUI check or new GameTest.
- The matched 200-seed comparison rejected two reset variants: each added cold seed 183 (E5). Its first-hold trace localized an eight-node passive LNG loop, exposing why clearing continuation is too broad. The third/final candidate preserves the complete point: all three 200-seed comparisons have no new held seeds, changed first-hold classes or increased hold publications.
- Final matched quick counters (baseline -> scoped Newton iterations): cold 631327 -> 631130; bundled random 150475 -> 150755; defaults 13103 -> 13103. Clean counts remain 195/200, 199/200 and 200/200. Judge cost by counters, not elapsed times.
- One final reference lane: quick 1368 tests / four failures; slow 185 / one failure. These are the five known unrelated baseline failures: FluidCheckpointCodecTest version expectation, FluidPacketCodecTest protocol expectation, ShapeRasterizerTest pump node, DeterministicMathSourceTest SightGlassDisplay Math.pow, ProcessUnitTest maximum nozzle clearance. No other unit failure.
- Exact chain-100 passes with zero deviation. Read the 33 junction references, remaining printed diagnostics, JUNCTION_SEEDING_GATE/D46_GATE and MIXED_GAS_COST from this same lane; no reference rerun.
- Fresh GameTests were bounded at 180 s, then their owned process tree stopped before campaign work: incomplete, not passed. Four checks failed/timed out (chunk-load dirty bucket, certified status, tank phase outlets, gas-tank pump refusal). Charging fixtures retain newly placed generators Off; no user world was used. This lane does not qualify fresh-world GUI/runtime behavior.
- One full c49f6654 campaign: cold 975/1000, bundled random 993/1000, defaults 1000/1000 clean. Its new cold-555 E5 traced to applying the new seeds in a passive 12-node/17-connection loop. Final 98f3dce3 limits the correction to the existing carried-mover-limit stage; passive loops retain their original path. No second full lane/campaign: final required quick/slow merge gates and original/matched 401-600 cohort checks are incremental qualification, recorded separately in scope-followup/.
- Final 401-600 matched cohort: candidate clean 197/200 cold, 197/200 random, 200/200 defaults; baseline 196/200, 197/200, 200/200. No new held seed or increased hold publication in either cohort (1200 matched cases total); cold 555 is clean. The sole class change is baseline budget F at cold 475 disappearing; no new C/F replay required and no timing improvement claimed.
- Final scoped merge gates: 1368 quick/four known failures, 185 slow/one known failure. After the inlet text correction, nine focused tests, existing GameTest-source compilation and jar pass again. InletPhase's existing ERROR reason is retained once; the existing refused-pump GameTest now awaits the user-facing inlet error rather than the removed raw mode token. No new GameTest.
- Tools: canonical tools/wti-pumping-rate-hold/; no tracked diagnostic harness or instrumentation; no tracked tooling detached or gate suite changed. Only two new worlds traced (183, 555), stopping at their first hold; no per-seed Newton cost replay.

## Delivery

Integrated and target jar passed with all ten local equipment/test files preserved byte-for-byte. Client PID 15508 (runClient session 85707) is running in that worktree; no agent world load or desktop input. See MERGE_REVIEW.md. Owner check: requested-rate error appears after a successful scheduled publication while the network advances; normal operation clears it on a later publication. No main merge/version bump.
