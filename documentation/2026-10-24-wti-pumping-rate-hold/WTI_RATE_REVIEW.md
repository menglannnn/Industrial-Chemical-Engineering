# WTI pumping-rate hold review

2026-10-24. Source beb5c2fc; owned branch codex/wti-pumping-rate-hold. Trigger supplied by owner: 1.5 -> 2 m3/s. Frozen evidence: tools/fluid-world-inspector/logs/deferred-hold-logs-2026-10-23-151051.tar.gz.

Subsequent owner-approved implementation: [WTI_RATE_FIX_REVIEW.md](WTI_RATE_FIX_REVIEW.md), source 9fa53354. The report below records the earlier tracing/prototype snapshot.

## 1. Expected behaviour

- The held circuit's source is WTI Light Export at 298.15 K / 101325 Pa, not the separately configured hot generator. Its preset matches to 5.6e-17 mole fraction. Pumped line: 12.5 m, diameter 0.3 m; suction line: 4.5 m, diameter 0.3 m; source is 1 m below suction.
- At 1.5 m3/s (21.22 m/s nominal velocity), the model supports PUMP_TARGET: about 1206.680 kg/s, 143.793 kPa rise, suction 56.565 kPa, liquid only.
- At a 2 m3/s request (28.29 m/s nominal velocity), the existing equations have a partly flashed, head-limited solution: about 1479.839 kg/s, 377.591 kPa rise and 38.374 kPa suction. The preliminary rate root is about 1.9654 m3/s at suction conditions, with about 6.4% gas volume there. Requested flow need not be attained when a limit binds; the simulation should still advance.

## 2. What the code did instead

- Original 3.2 s interval: 1.5 succeeds (3 accepted substeps, 0 rejected); 2 fails with residual 0.12181887844301574, the captured 0.12181887846794752 signature, after 20 line-search failures. This is not a wall-budget hold or proof of physical infeasibility.
- A fresh single-step solver and a fresh port-rate solver both find the valid 2 m3/s head-limited branch. Calling portRate twice on the exact same graph with one workspace gives success then failure.
- The first preliminary rate solve changes suction from liquid-only to liquid+vapour and the pump to PUMP_HEAD_LIMIT. The next solve starts from the original liquid-only graph but carries that root's pump mode/flows/heads. Rate warm-start validity compares the old/new input states, but does not require the cached output phase basis to match the new input basis.
- initialPhaseSeeds uses the new graph's liquid-only junction state. That state is inconsistent with the carried head-limited point. The failed iterate drives suction toward 1 Pa with about 2060.76 kg/s, and stalls in the two hydraulic rows. No converged point is available for the outer phase correction.
- The interval's preliminary guard-rate and cold-rate calls expose the repeated-call defect. A failed cold seed then leaves the wrong mode available to the backward-Euler attempts. Halving time does not repair an algebraically inconsistent starting branch.

## 3. Proposal for owner validation

- At entry to a new rate solve, invalidate its previous point/modes when the stored output phase layout differs from the graph's new input layout. Leave the existing rate initializer, inside-pass continuation, equations, phase protections and conservation checks intact.
- The narrow offline [proposal.patch](../../tools/wti-pumping-rate-hold/proposal.patch) clears the previous-pipe/mode keys for that mismatch. It adds no WTI/id/rate special case, physical model, artificial pressure floor, deadband or relaxed tolerance.
- A first prototype checked phase validity every time a carried point was queried; the selected entry-only placement is narrower and preserves the measured benefit of within-pass continuation. Neither prototype entered production.

## Validation and limits

- Selected prototype: repeated port calls both converge. The 1.5 m3/s result and its 1319 checkpoint polls are unchanged. The 2 m3/s interval succeeds with 3 accepted / 0 rejected substeps.
- Eleven sequential 3.2 s intervals (35.2 s), with normal accepted-endpoint replay, all succeed: 33 accepted / 0 rejected substeps. The outlet valve changes from CLOSED to VALVE_REGULATING; outlet flow grows to about 434.95 kg/s while the pump remains on its head limit.
- The ordinary solver's equation/conservation gates remain enabled. This is one frozen-case diagnosis, not broad qualification, a 200/1000-seed campaign or an in-game check. The model's treatment of cavitation itself was not changed or independently validated against experiment.
- Product code remains unchanged; branch is clean at beb5c2fc. No merge, version bump, test suite or client launch was performed for tracing. Pending display changes remain separately committed at a591404f.
- Code guide: no change; this task changes no product contract. Tool folder: tools/wti-pumping-rate-hold/, with exact baseline copies, logging-only copies, two offline prototypes and compressed evidence. No tracked harness was detached.
