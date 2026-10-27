# Accessory store compatibility: design as built

Batch 2026-10-26-accessory-store-compat. Rule: AGENTS.md, "Saves and data formats". Model: `runtime/fluid/FluidFacts`.

## 1. Expected behaviour

A world saved by 0.12.x or 0.13.0 with gauges and fittings opens with every gauge (mount, settings, instance) and every fitting record kept, and the wrench or a break still refunds each exactly once. A store a build cannot read refuses the world and its file stays as it is.

## 2. What 0.13.0 did instead

1. The store opened through `computeIfAbsent`. An unreadable `data/icheme_accessories.dat` (any format but 1, any damaged record) was logged by Minecraft and replaced by an empty store. The 100-tick retry deadline marks the store dirty, so the next autosave overwrote the file: gauges gone, fitting records gone (the wrench then refunds nothing).
2. So "format 1, any other refused" meant: the first format change discards every accessory.
3. A damaged pending operation (state) refused the whole store, which by 1 reset it.
4. The host's block-entity copy (`AccessoryRevision`, `Instruments`) threw on a record it could not read. Minecraft then drops the whole block entity (a pipe's fluid identity, a shell's shape and paint). Spawn chunks load before the store opens.
5. The reader was lenient: a missing field read 0 or "", an unknown field was ignored, settings went through the item codec.

## 3. Records (facts schema 1; the 0.13.0 keys, unchanged)

| Record | Facts fields (schema 1, required) | State fields |
|---|---|---|
| root | `format` (the facts schema), `installed`, `fittings`, `revisions` | `state_format` (new, default 1), `next_due`, `pending` |
| installed gauge (also the host copy) | `host`, `mount`, `instance`, `revision`, `settings` | |
| settings | `kind`, `minimum`, `maximum`, `unit`, `relative` | |
| pipe fitting | `host`, `instance`, `revision`, `type` | |
| slot revision | `host`, `revision` | |
| pending operation | | `request`, `player`, `host`, `mount`, `instance`, `revision`, `install`, `type`, `settings` (installs only), `dimension`, `position`, `slot`, `offhand` |

## 4. Rules as built (`AccessoryFacts`, `InstrumentService.Store`)

- Facts are read by name. A schema-1 field has no earlier behaviour: missing, the record is damaged. A later field reads the default written beside its read.
- A later `format`, or a key no read takes (root, gauge, settings, fitting, revision), refuses: "written by a newer build".
- Names (fitting `type`, settings `kind` and `unit`, `mount`) go through `ALIASES` (empty at schema 1); an unknown name refuses.
- Facts validate (record constructors, host conflicts, duplicate revisions); a failure refuses.
- State: `state_format` other than 1, or any state record that does not validate, drops the state (WARN `accessory_store status=STATE_DROPPED`), every fact kept. This loses nothing: only a commit shrinks the held stack or refunds an item.
- `Store.open` reads the file itself, before `FluidSavedData.open` and before any runtime starts. A file it cannot read throws `IllegalStateException` naming the file; the store never enters the data storage, so nothing writes it.
- The host's copy is state: unreadable, it is dropped (WARN `accessory_copy status=DROPPED`); `deviceLoaded` then `bind` restores it from the store.

## 5. Decisions logged (reversible before the merge)

- D1 Layout kept as 0.13.0 plus `state_format`. 0.13.0 ignores unknown keys, so it reads this build's saves: switching one world between `main` and this branch loses nothing. Alternative: a nested `state` compound (schema 2), a cleaner split, but a 0.13.0 build would reset the store on that world (defect 1).
- D2 Slot revisions are facts: the gauges, fittings and the hosts' copies are checked against them. Alternative: recompute them from the records (loses the tombstones of emptied hosts).
- D3 The store writes its own settings record (the same five keys as `GaugeSettings.CODEC`): an item-codec change cannot move the store's facts.
- D4 A later state format keeps its keys at the root (`next_due`, `pending`); a new root key is refused by this build (a safe refusal).

## 6. Item components (owner decision 2026-10-26: aliases only)

- `gauge_settings` reads its kind and unit names through `GaugeSettings.KIND_ALIASES` and `UNIT_ALIASES`; the store's `kind` and `unit` name sets use the same tables, so one alias serves both.
- Anything else a component cannot read keeps Minecraft's behaviour (checked with a probe of the same codec structure, DFU partial results): the stack keeps its item, count and other components; the failing component is dropped (logged "Tried to load invalid item"), so the gauge reverts to its item's default settings (0 to 10 bar(g), or 0 to 200 °C). An unknown field is ignored and is gone after the next save.
- `pipeline_tool_settings` is a bare diameter with no names: unchanged.
- Options not taken: repair in the codec (an unknown unit becomes the kind's default unit with the absolute SI endpoints kept; invalid endpoints the default range; a diameter clamped); strict fields (an unknown field fails the component).
