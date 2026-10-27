# 2026-10-20-crystal-junction-holds

Purpose: The cold worlds 0.9.1's still-pipe rule exposes (owner 2026-10-19; thread t56, branch `claude/crystal-junction-holds` from `a08c7134`): D61 a crystal above its melting point melts on arrival in a junction (`30b11f2f`, `09e215c3`; cold 778 clean); 743 traced: the balance sweep moved a sealed junction to its supply's pressure, so (d) a sealed junction keeps its pressure (`be5c9101`; the 14-block world keeps up, the full world holds later in a blowdown: open option); a subnormal solid mass is roundoff zero (`2bacf29b`); D62 a closure nothing moves carries across an interval start, form C chosen on the quick check (`7b810779`; cold 183 clean); round 2: a junction nothing reaches is stated at its own enthalpy (`34c79758`; full cold 743 clean); 573 traced to `50fd053f` (report only). Cost gate missed narrowly on the quick check (cold +0.10 % iterations, +0.44 % builds). Final test GREEN, t57 m20 at `bb9de943`: cold 944 → 955, bundled 989 → 990, defaults 1000, none newly held; clean in both cold −0.07 % iterations / −1.38 % builds, bundled −0.20 %, defaults +3 iterations (accepted narrow miss). Open, report only: cold 573 (island 31 never certifies STEADY)

Status: Implemented 2026-10-20 (0.9.2). Index: [../INDEX.md](../INDEX.md).

## Documents

- [HOLDS_PLAN.md](HOLDS_PLAN.md)
- [MOVED_REFERENCES.md](MOVED_REFERENCES.md)
- [FINAL_TEST_REVIEW.md](FINAL_TEST_REVIEW.md)
- code guide refreshed to `449d398c` (fluid-science, verification, README)

## Code areas

`science/fluid/network` (`JunctionCrystals`, `ConservativeTransport`, `PassiveStepSolver.BalanceNet`, `SolidEventIntegrator`), `science/fluid/state/SolidInventory`; tools `crystal-junction-holds/`

## History

- Implemented 2026-10-20 (main 449d398c, 0.9.2; not pushed)
