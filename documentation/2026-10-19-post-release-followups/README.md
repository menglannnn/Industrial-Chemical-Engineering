# 2026-10-19-post-release-followups

Purpose: Post-0.9.0 follow-ups (owner 2026-10-19, Opus threads, branch `claude/post-0.9.0`): one buffer sizing for a minted and a carried junction (`a313400b`, bundled 145 clean); round 3 forms: still is roundoff within the Newton's tolerance, decided once at the interval start (`235d69bb`, replacing `50fd053f`'s resolution-flow test; cold 850 and 829 gates), and the CDU17 fixture's water carries its ice (`987db979`); network water model without ice refused at loading (option 2, `340e43da`); material reader findings a, c, d; stale comments; D46 closed by the owner, gate bundled 188 (`73fbe48c`); options 1 and 3 built and reverted, 3 deferred. Final test m19 green (1489/0; campaign cold 944 / bundled 989 / defaults 1000; cold clean-in-both Newton −6.1 % vs m16). Cold 778 and 743 still held, moved to batch 2026-10-20-crystal-junction-holds

Status: Implemented 2026-10-19 (0.9.1). Index: [../INDEX.md](../INDEX.md).

## Documents

- [FOLLOWUPS_PLAN.md](FOLLOWUPS_PLAN.md)
- [MOVED_REFERENCES.md](MOVED_REFERENCES.md)
- [FINAL_TEST_REVIEW.md](FINAL_TEST_REVIEW.md)
- [D46_MINT_MASS_REVIEW.md](D46_MINT_MASS_REVIEW.md)
- tools `post-release-followups/`

## Code areas

`science/fluid/network` (`PassiveNetwork`, `SolidEventIntegrator`, `PassiveStepSolver`), `runtime/fluid/IslandCoordinator`, `science/material`; `test`

## History

- Implemented 2026-10-19 (main a08c7134, 0.9.1; not pushed)
