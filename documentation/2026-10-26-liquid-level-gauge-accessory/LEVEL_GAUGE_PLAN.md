# Attachable floating-roof tank level gauge

Status: In progress since 2026-10-26. Owned worktree `.claude/worktrees/liquid-gauge-accessory`, branch `codex/liquid-gauge-accessory`, base main `600549d9` (0.13.2).

- Owner scope: floating-roof tanks only. One fixed, full-height level-gauge accessory beside the front ports; no dial range editor. New tanks have none.
- Item `icheme:liquid_level_gauge`, stack64, creative/commands, no recipe. Either hand installs; survival consumes one, creative free. Duplicate/blocked/unsupported placement consumes nothing.
- Reuse the existing non-dial fitting registry with a `LEVEL_GAUGE` type and `tank_level` logical slot. It coexists with reservoir pressure/temperature gauges. Keep instance/revision validation, immediate static transactions and exactly-once wrench/break/dismantle recovery.
- Static metadata identifies the fitting. Extract the approved custom-glass column/pipework into cached accessory geometry, leaving the constructed tank bare until installed. Ghost/selection/clearance use its actual geometry bounds; allow overhang and tall mounts. Shared host index must find any part of the long gauge, not just dial centres.
- Keep actual liquid colour, water/oil bands and external float. Use the tank's existing requested scheduled display and cached replay, without extra solves, inventories, storage nodes or new live payloads. Hover shows liquid height only.
- Preserve attachment identity through native equipment refresh; if the host/mount is invalid, recover once. No compatibility work, migration or old-world checks.
- Focused persistence/metadata/mount/recovery tests and native render checks; compile/package with no dev client running. No new GameTests. Keep working outputs in owned `tmp/level-gauge/`; publish final plan/review/contracts/index and required persistent compressed evidence.
- Deliver feature branch, no automatic merge/version bump.
