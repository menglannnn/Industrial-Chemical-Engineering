# Liquid-level gauge accessory review

Status: In progress since 2026-10-26; delivered `b1477c09` on `codex/liquid-gauge-accessory`, based on released main `600549d9`. No merge/version bump.

Expected: the approved full-height liquid indicator is optional equipment fitted to a floating-roof tank. Installing consumes one in survival; wrench removal/breaking recovers one. It reads the same published liquid level and colours.

Previous behavior: construction always baked the complete gauge into the tank and always drew its liquid overlay. There was no item or installation record.

Implemented: a stack64 item, fixed full-height placement ghost/mount, shared revisioned non-dial fitting persistence/transactions, configured-instrument coexistence and exactly-once recovery. The tank is bare by default; metal, custom-glass sleeve, bands and float draw only while the fitting is installed. Selection reaches every part of the long gauge and uses surface-free server bounds. The drain's ground contact does not falsely block installation.

Validation: final24 focused tests pass, zero failures/errors/skips: LiquidLevelGaugeTest3, GaugeAccessoriesTest12, EquipmentRefreshTest7 and the two existing gauge-geometry methods. Package generation passes; GameTest sources compile as part of the repository's test setup, no GameTests run or added. No old-world, full-suite or in-game validation claimed.

Coverage: pending/fitting/metadata round-trips, type/mount rejection, revisioned duplicate/removal/reinstall recovery, coexistence with both dial types, bare versus accessory surfaces, world headings/size extremes, actual-model bounds/winding/pipe continuity and native refresh. Native software renders show uninstalled/ghost/filled/empty in two views each (8 views); colour reduction and the custom glass resource are reused.

Owner gameplay checks: install from either hand in survival/creative, duplicate/obstructed/unsupported placement, pressure/temperature coexistence, hover, wrench/full-inventory recovery, reload/resize/dismantle. Use fresh worlds for verification.

Tools: canonical `tools/liquid-level-gauge/` retains native preview and icon sources; no probe/task/exclusion added to product. Run output stays in owned `tmp/level-gauge/` with its index row; keep while this branch is unmerged, clean after merge/report per owner rule. Required persistent compressed log archive is under the canonical tool's `logs/`.

Code guide: feature addenda for instrument-accessories/equipment/client-gui/verification and a README pointer; implementation is the authority. Existing store fact/state codecs and process formats remain; no compatibility or migration work.

Rebase follow-up2026-10-26: owner requested main as the base. Current local main remains600549d9, already the direct parent of b1477c09. `git rebase main` reports up to date; branch and working tree remain clean. No source or commit changed, so prior24 focused passes/package and native views still apply; no duplicate test run. Code guide: no change for this follow-up.
