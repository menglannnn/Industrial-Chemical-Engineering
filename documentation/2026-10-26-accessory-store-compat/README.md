# 2026-10-26-accessory-store-compat

Purpose: bring the accessory store (`icheme_accessories`) under the AGENTS.md "Saves and data formats" rule; open item of [2026-10-26-backward-compat](../2026-10-26-backward-compat/README.md).

Status: Implemented 2026-10-26 (merged to `main` as 0.13.1, fast-forward to `9eeeb1f4`; not pushed). Index: [../INDEX.md](../INDEX.md).

## Documents

- [ACCESSORY_STORE_COMPAT_PLAN.md](ACCESSORY_STORE_COMPAT_PLAN.md): what 0.13.0 did, the records, the rules as built, decisions D1 to D4, the item components.
- [ACCESSORY_STORE_COMPAT_REVIEW.md](ACCESSORY_STORE_COMPAT_REVIEW.md): merge review: commits, gates (minimal, owner instruction), code guide, tooling cleanup.

## Code areas

`runtime/instrument/AccessoryFacts` (new: schema, aliases, the by-name record reader), `world/item/GaugeSettings` (`KIND_ALIASES`, `UNIT_ALIASES`, `codec`), `InstrumentService.Store` (`save`, `load`, `state`, `open`, `read`), `Instrument` (`MOUNTS`, `Metadata.read` drops an unreadable copy), `runtime/fluid/FluidWorldAuthority` (opens the store first, before `FluidSavedData.open`). Tests: `AccessoryStoreCompatTest` (new, 8 tests), `GaugeAccessoriesTest` (two assertions: unusable pending operations are dropped, not refused). Corpus: `src/test/resources/compat/accessories/schema-1.snbt`, written by the 0.13.0 writer at `8d4c8df6`.

## History

- 2026-10-26: task from the backward-compat session. Found: an unreadable store was replaced by an empty one and overwritten at the next autosave (Minecraft's `readSavedData` swallows the exception, confirmed in the NeoForge 21.1.219 sources); a host copy that could not be read failed the whole block entity's load. Rule settled every behavioural point; reversible layout choices logged as D1 to D4 (D1: 0.13.0 keys kept so 0.13.0 still reads this build's saves). Corpus image captured from the unmodified 0.13.0 code before any change. Commits `ae7951f7` (code, gate, corpus), `9ee3a66c` (CHANGELOG `[Unreleased]`). `./gradlew test` at `ae7951f7`: 1445/1445 green. `slowGates` not run (not a merge). No tool folder: the corpus builder lives in the gate test.
- 2026-10-26: item component aliases (owner request "Do this task here"). A probe of the `ItemStack.CODEC` structure showed that a component that fails to decode is dropped and the stack kept (the earlier "drops the stack" was wrong). Owner decision: "Aliases only" (not repair, not strict fields). `65754348`: `GaugeSettings.KIND_ALIASES`, `UNIT_ALIASES`, `GaugeSettings.codec(kinds, units)`, shared with the store's name sets; pipe tool unchanged (no names). Test `gaugeSettingsNamesReadThroughTheirAliasTables`. `49b1bf63` CHANGELOG. `./gradlew test` at `65754348`: 1446/1446 green.
- 2026-10-26: merge (owner request "merge", then "Perform only minimal tests"): release prep `9eeeb1f4` (0.13.1, the proposed patch; CHANGELOG heading; README save sentence). At `9eeeb1f4` the instrument tests only: 20/20; `slowGates`, fluid GameTests and the lane's solver steps not run. Code guide: instrument-accessories, fluid-runtime, verification, README at `9eeeb1f4`. No tool folder. Main fast-forwarded to `9eeeb1f4`, not pushed.

## Open items

- A server start through `Store.open` (fluid GameTests or in game) was not run, on the owner's minimal-test instruction.
- Not pushed.
