# Accessory store compatibility: merge review

Batch 2026-10-26-accessory-store-compat, merged to `main` as 0.13.1 (fast-forward to `9eeeb1f4`), 2026-10-26, on the owner's "merge". Not pushed.

## Commits

- `ae7951f7` accessory store under the saves rule: `AccessoryFacts`, `Store.open`/`read`/`state`, the host copy as state, gate `AccessoryStoreCompatTest`, corpus `compat/accessories/schema-1.snbt` (written by the unmodified 0.13.0 writer).
- `9ee3a66c` CHANGELOG `[Unreleased]`.
- `65754348` gauge item component: `GaugeSettings.KIND_ALIASES`, `UNIT_ALIASES`, `codec(kinds, units)`, shared with the store (owner decision "aliases only").
- `49b1bf63` CHANGELOG line extended.
- `9eeeb1f4` release prep: `mod_version` 0.13.1 (patch: a fix, as proposed), CHANGELOG `[0.13.1] - 2026-10-26`, README save sentence (gauges and pipe fittings).

## Expected behaviour, and what changed

A world keeps every installed gauge and fitting record across code and data changes; a store a build cannot read refuses the world with its file untouched. Before: Minecraft's `readSavedData` swallowed the load failure, `computeIfAbsent` started an empty store and the next autosave overwrote the file; a host's unreadable block-entity copy failed the whole block entity's load. Design and decisions D1 to D4: [ACCESSORY_STORE_COMPAT_PLAN.md](ACCESSORY_STORE_COMPAT_PLAN.md).

## Gates

Owner instruction at the merge: "Perform only minimal tests".

- `./gradlew test` at `ae7951f7`: 1445/1445; at `65754348` (the final code): 1446/1446, 0 skipped.
- At the merge commit `9eeeb1f4` (no code change since `65754348`): `test --tests 'com.wormzjl.icheme.runtime.instrument.*'`: `AccessoryStoreCompatTest` 8/8, `GaugeAccessoriesTest` 12/12.
- Not run: `slowGates`, the fluid GameTests (they would start a real server through the new `Store.open`), and the lane's solver steps (chain-100, campaign; no solver or physics code changed). `Store.open` is covered by the unit test of `Store.read` on a real file; the `storage.set` hand-over and the constructor order are not exercised by a server run.

## Code guide

Chapters at `9eeeb1f4`: instrument-accessories (Store, `AccessoryFacts`, `GaugeSettings` aliases, the saves contract, tests), fluid-runtime (the store opens first), verification (gate, corpus, checklist row, batch line), README (file count, versions). Other chapters: no change, their code untouched.

## Tooling cleanup

No tool folder. The corpus builder lives in the gate test; the DFU probe that checked the item component's fallback was a scratch test, deleted before any commit. `git ls-files -i -c --exclude-standard` empty.

## Open items

- An in-game or GameTest start of a world through `Store.open` (not run, owner's minimal tests).
- None of the 0.13.1 release is pushed.
