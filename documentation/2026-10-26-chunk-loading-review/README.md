# 2026-10-26-chunk-loading-review

Purpose: Owner request 2026-10-26: review how the fluid network and the custom machines (equipment) behave when chunks load and unload, then fix what the review found.

Status: Implemented 2026-10-26 (0.13.2: `main` fast-forwarded to `600549d9`, from branch `claude/chunk-loading-behavior-1796a8`; not pushed). Index: [../INDEX.md](../INDEX.md).

## Documents

- [CHUNK_LOADING_REVIEW.md](CHUNK_LOADING_REVIEW.md)

## Code areas

`IChemE.onFluidChunkLoaded`, `runtime/fluid/FluidWorldAuthority.java` (`loadedChunk`, `deviceLoaded`, `deviceUnloaded`, `publishTank`, `PresentationHost.present`), `runtime/fluid/FluidPresentation.java`, `runtime/fluid/TankDisplay.java` (`Glide.next`, `standingAt`), `world/level/block/FluidDeviceBlock.java` (`connectedState`, `updateShape`), `world/level/block/entity/FluidDeviceBlockEntity.java` (`onLoad`, `refreshGeometry`), `world/level/block/entity/EquipmentShellBlockEntity.java` (`onLoad`, `released`), `world/level/block/EquipmentStructure.java` (`restoreFluidBinding`, `refreshLater`, `removeRetired`), `world/level/block/FloatingRoofs.java` (`standing`), `world/level/block/entity/ColumnCalculatorV3BlockEntity.java` (`setRemoved`), `network/ColumnV3Network.java` (completion routing); tests `FloatingRoofsTest`, `ChunkLoadGameTests`, `EquipmentCompatGameTests`

## History

- 2026-10-26: review at `2fbb80f2` (0.12.3), code read against the NeoForge 21.1.219 sources. The simulation does not depend on chunks, as intended. Defects: D1 a pipe's load loads neighbouring chunks in a chain (high), D2 a floating roof jumps after a reload, D3 a column solve continues after its calculator unloads, D4 tank watches stay after an unload.
- 2026-10-26: owner: "Code on main updated, review, assess, implement". Branch fast-forwarded to `9eeeb1f4` (0.13.1); D1 to D4 still stand; the new load paths add none. Implemented P1, P2 option A, P3 option A and P4 (the recommended options), with gates `FloatingRoofsTest` (+1) and `ChunkLoadGameTests` (2). Found and fixed a test leak on `main`: `aBlockedRefreshTriesAgainAtTheNextLoad` left its drum registered. `test` 1447/0; fluid GameTests 23/23 (`chunkload2`); rule-off proof failed exactly the two new tests. Tool folders: `tools/chunk-loading-review/` (logs).
- 2026-10-26: owner: "Commit, then do the merge prep". Commit `7147d431` (fixes and gates). `test slowGates`: `slowGates` 187/0 in 18 classes, `test` 1447/0 (up to date on the same inputs). Release prep `600549d9`: `mod_version` 0.13.2 (patch, proposed), CHANGELOG `[0.13.2] - 2026-10-26`, README version line. Code guide at `600549d9`: `fluid-runtime.md`, `equipment.md`, `column-v3.md`, `verification.md`, `README.md`. `git ls-files -i -c --exclude-standard` empty.
- 2026-10-26: owner: "0.13.2 is fine. Fast-forward main to 600549d9, set the INDEX row to Implemented. Do not push." `main` fast-forwarded `9eeeb1f4..600549d9` in the main checkout (clean before and after). INDEX row Implemented 2026-10-26 (0.13.2); `tools/INDEX.md` row of `chunk-loading-review/` Concluded. Not pushed: `origin/main` stays at `9eeeb1f4`.

## Open items

- Optional, one in-game check: a floating-roof tank that fills while its chunk is unloaded. Its roof should glide, not jump, when the chunk reloads.
- Watch item: a walk over interior cells in `refresh` and `removeRetired` (up to 42 blocks) may read past its ticking region. It is rare and happens once per structure.
