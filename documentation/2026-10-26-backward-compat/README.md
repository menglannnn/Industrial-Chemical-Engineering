# 2026-10-26-backward-compat

Purpose: Owner request 2026-10-26: design a backward compatibility system for the fluid system and the equipment models that changes a world as little as possible when code or data change.

Status: Implemented 2026-10-26 (merged to `main` as 0.13.0, fast-forward to `8d4c8df6`; pushed to `origin/main` 2026-10-26). Index: [../INDEX.md](../INDEX.md).

## Documents

- [BACKWARD_COMPAT_PLAN.md](BACKWARD_COMPAT_PLAN.md): design, owner decisions (section 10), implementation notes (section 13).
- [BACKWARD_COMPAT_REVIEW.md](BACKWARD_COMPAT_REVIEW.md): merge review, the final lane and its alone replays, code guide, tooling cleanup.

## Code areas

Fluid: `runtime/fluid/FluidCheckpointCodec` (envelope, sections), `FluidFacts` (facts schema, rebuild, restatement), `FluidCheckpointStore` (load ladder EXACT / FACTS), `FluidSavedData`, `FluidWorldAuthority`, `FluidPropertyReloadGuard` (the `/reload` hold text), `WorldTopologyLedger` (`Restated`, `Overlay.check`), `PhysicalRegistry` (vessel rebuild, chunk index), `IslandCoordinator.topology` (contents conserved), `FluidDeviceSpec.sameCharge`, `PassiveNetwork.Inventory.sameContents`, `NetworkReduction.atRest` (`WALL_RESOLUTION`). Equipment: `science/equipment/ShapeRepair`, `EquipmentRefresh` (ports-first anchor, `Holding.PIPE`), `world/level/block/EquipmentStructure` (`removeRetiredLater`, `refreshLater`, `reregister`), `EquipmentShellBlockEntity` (`readShape`, retired cells, `layoutIncomplete`), `EquipmentConstructorBlockEntity.readDesign`, `client/gui/screens/VesselScreen` (blocked cells line).

## History

- 2026-10-26: design written on worktree `backward-compat-system-design-11f7e0` (branch `claude/backward-compat-system-design-11f7e0`, base `2fbb80f2`, 0.12.3). Findings: eight checkpoint format bumps in five days, all added fields; any material data edit refused the world; an invalid shell shape left a vessel unbound.
- 2026-10-26: owner decisions: D6 "simply remove it (and don't leave ghost blocks behind)", "other as recommended" (D1 to D5, D7 to D9, the AGENTS.md rule). `97a8a714` AGENTS.md rule.
- 2026-10-26: W1-W2 `a470b3bd`: envelope 1, state format 20, facts schema 1; load EXACT or FACTS; unreadable data refuses; pre-envelope worlds get one reset. Gate `FluidFactsCompatTest`, corpus `compat/fluid/schema-1`.
- 2026-10-26: W3 `b432efc4`: a package change restates by component id; removed components booked and named; `/reload` hold says "save and reopen".
- 2026-10-26: W4 `7f058499`: `ShapeRepair` (names, defaults, repair ladder); retired kinds removed whole (D6). Gates `EquipmentShapeCompatTest` (corpus `shapes-1.snbt`), GameTest `aStructureOfARetiredKindIsRemovedWholeWithNoGhostBlock`.
- 2026-10-26: W5 `c7e8f4e8`: pre-existing defect fixed (a moved or resized refreshed drum threw in `reregister`): ledger, registry, coordinator and rest test rules (plan 13); movers keep identity (D7, `relocateMover` deleted); ports-first anchor; blocked retry and datasheet line; gate `EquipmentLayoutGoldenTest` (81 layouts). Decision logged: the two stamps of plan 5.2 dropped as unnecessary. `LevelHeadTest` fixture moved above the ambient (D54). Fluid GameTests 21/21.
- 2026-10-26: W6: full suite at `c7e8f4e8` green: `test` 1437/1437, `slowGates` 187/187; fluid GameTests 21/21 (both equipment compat tests; the blocked retry logged `blocked=1` then `blocked=0`). CHANGELOG `[Unreleased]` entry. Final lane (`lane.sh`) and code guide not run or updated: both belong to the merge.
- 2026-10-26: merge (owner request "Prepare the merge to main as 0.13.0 ... Stop before pushing"): local main 0.12.4 merged in (`17d2673d`, CHANGELOG conflict only), release prep `8d4c8df6` (mod_version 0.13.0, CHANGELOG heading, README save sentence). Final lane at `8d4c8df6` green: 1624 tests, chain-100 exact, GameTests 21/21, campaign clean 975 / 994 / 1000 as the `e8ec6455` baseline; held seeds that differed replayed alone (timing; 957 wall-clock marginal, rule-off check). Code guide: fluid-runtime, fluid-science, equipment, client-gui, verification, README at `8d4c8df6`. Tool folder `tools/backward-compat/`. Main fast-forwarded to `8d4c8df6`, not pushed.
- 2026-10-26: pushed `main` to `origin` (owner request "Push main to origin"): `2178a7c2..8d4c8df6`, 22 commits, 0.12.1 to 0.13.0.

## Open items

- The accessory store (`icheme_accessories`): taken up by batch [2026-10-26-accessory-store-compat](../2026-10-26-accessory-store-compat/README.md).
- Owner questions: restate in process on `/reload` instead of the hold; keep the restatement notice on the status line after the first solve (today it shows until then; the ledger keeps the totals).
- A vessel rebuilt smaller than its liquid (plan 13).
- The datasheet's blocked line is not checked in game.
