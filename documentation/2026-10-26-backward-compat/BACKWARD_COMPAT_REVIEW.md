# Backward compatibility: merge review (0.13.0)

Batch `2026-10-26-backward-compat`, branch `claude/backward-compat-system-design-11f7e0`, merged to `main` 2026-10-26 by fast-forward to `8d4c8df6` (mod 0.13.0), pushed to `origin/main` the same day. Plan and implementation notes: [BACKWARD_COMPAT_PLAN.md](BACKWARD_COMPAT_PLAN.md) (sections 10 and 13).

## What merges

- `97a8a714` AGENTS.md rule; `a470b3bd` W1-W2 (envelope 1, state format 20, facts schema 1, load EXACT / FACTS); `b432efc4` W3 (restatement, `/reload` hold text); `7f058499` W4 (`ShapeRepair`, retired kinds removed whole, D6); `c7e8f4e8` W5 (vessel and mover rebuild keeps identity and contents, D7; ports-first anchor; blocked retry; layout table); `754b328b` CHANGELOG; `17d2673d` main 0.12.4 merged in; `8d4c8df6` release prep (mod_version 0.13.0, CHANGELOG heading, README save sentence).

## Final lane at 8d4c8df6 (alone, `tools/floating-roof-tanks/final-lane/lane.sh` pointed at this worktree)

Baseline: the 0.12.0 lane at `e8ec6455` (`tools/floating-roof-tanks/logs/final-lane-e8ec6455-2026-10-26.tar.gz`); main changed no fluid code since.

| Step | Baseline e8ec6455 | 8d4c8df6 |
|---|---|---|
| `test slowGates` | 1606, 0 failed | 1624, 0 failed (18 new tests) |
| chain-100 exact | max deviation 0 | max deviation 0 |
| fluid GameTests | 19/19 | 21/21 (`EquipmentCompatGameTests` 2) |
| campaign cold random, clean | 975 | 975 |
| campaign bundled random, clean | 994 | 994 |
| campaign bundled defaults, clean | 1000 | 1000, counters identical |

- Every clean seed's line is identical to the baseline. Five held seeds differ (cold 431, 683, 833, 957; bundled 431), and the cold counters with them (`restStops` 999 → 887, `phaseRefusals` 37,386 → 61,796). The run was slower throughout (bundled defaults: identical counters, seed time +48 %).
- Replayed alone, one worker, batched (AGENTS.md: a wall-clock hold is replayed alone before it counts): cold 431, 683, 833 and bundled 431 equal the baseline line for line. Cold 957 follows the baseline's holds to tick 4224 and ends in its state (island 43 FULL, 45.2576 kg of crystals in 7 vessels), with one extra transient `HELD: wall deadline` at tick 4325.
- Seed 957 alone with the off-wall rest rule disabled (`NetworkReduction.WALL_RESOLUTION` check off, reverted): 15 holds, a chain of wall deadlines. 957 sits on its wall-clock budget; its hold count varies run to run. The new rule does not cause it.
- Verdict: green. No world newly held; chain-100 exact.

## Code guide

Updated at 8d4c8df6: `fluid-runtime.md` (checkpoint section rewritten: facts and state, load ladder, rebuild, restatement, refusals, the D4 reset; ledger and registry edit rules; `relocateMover` removed), `fluid-science.md` (off-wall rest rule, `WALL_RESOLUTION`, `Inventory.sameContents`), `equipment.md` (`ShapeRepair`, retired kinds, layout refresh contract, layout table), `client-gui.md` (datasheet blocked line), `verification.md` (new gates, the `compat/` corpus, merge checklist step), `README.md` (code map, versions, batch section). `instrument-accessories.md`, `column-v3.md`, `equilibrium-engine.md`, `thermo-materials.md`: no change, their code untouched.

## Tooling cleanup

- Nothing to detach: the batch kept no probe in a tracked path (the W5 ledger probe `ProbeMoveTest` was never committed). Every new test runs in `test` or the fluid GameTest task.
- Tool folder `tools/backward-compat/`: README (the lane run, the alone replays, the rule-off check) and `logs/final-lane-8d4c8df6-2026-10-26.tar.gz`. No CHANGELOG line for detached code: none was detached.
- `git ls-files -i -c --exclude-standard`: empty.

## Open items

- Owner: restate in process on `/reload` instead of the hold; keep the restatement notice on the status line after the island's first solve.
- The accessory store (`icheme_accessories`, format 1, another format refused) is not yet under the facts rules, although AGENTS.md counts accessories as facts. No format change has happened to it; the first one needs the same treatment.
- A vessel rebuilt smaller than its liquid (plan 13). The datasheet's blocked line is not checked in game.
