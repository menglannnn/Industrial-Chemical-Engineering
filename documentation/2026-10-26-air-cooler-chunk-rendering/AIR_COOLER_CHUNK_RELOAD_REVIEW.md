# Air cooler "Moving parts" switch reloads every chunk: review

2026-10-26, branch `claude/air-cooler-chunk-rendering-5d0aba` from `a8ce7558` (0.12.1), rebased onto `bc7a4167` (0.12.2); merged to `main` as 0.12.3 (`2fbb80f2`).

## 1. Expected behaviour

The switch on a moving unit's datasheet (`VesselScreen`, "Moving parts: running / still") changes only how units' moving parts are drawn. Only the chunk sections holding a moving part's facets could need a new mesh; every other section stays as it is and stays on screen. In numbers: the largest air cooler bank (6 parallel × 4 series) is about 20 × 30 blocks, its fans in one band 2 to 3 m above grade, so its fan facets reach 1 to 9 sections. At render distance 12 the client holds 625 columns × 24 = 15,000 sections.

## 2. What the code did

- `MachineAnimation.toggle()` flipped a static flag and called `LevelRenderer.allChanged()`.
- `allChanged()` (1.21.1 sources, `LevelRenderer.java:716`) is the F3+A reload: it reloads the transparency post chain, frees every section's GPU buffers, waits on the render thread for the compile queue, and builds a new `ViewArea`. All 15,000 sections compiled from nothing and drew nothing until each one finished: the world vanished and popped back in.
- Why the call was there: `EquipmentShellModel.getQuads` read the flag while it meshed and picked a `still` layer (parts baked into the mesh) or a `moving` layer (parts left out). A flag change made those meshes stale, and the client had no list of the cells concerned.
- A pump or compressor's on/off (its published running state, `MoverSounds.running`) never touched the mesh: `MachineAnimation` keeps drawing a stopped mover's parts, at rest (motion time 0). That is why the movers did not redraw chunks.
- An air cooler is not a fluid device and has no process state; this switch is its only working status.

## 3. Proposals and owner decision

Proposed: A, re-mesh only the sections whose cells hold moving facets; B, remove the debug switch. Owner direction 2026-10-26: "The on/off button should behave like the pump / compressor, they didn't trigger chunk redraw." This is B's mesh change with the switch kept.

## 4. Fix (commit `c91af09a`; `1d57c010` before the rebase)

- `MachineAnimation`: `enabled` becomes `running()`. Off, every part is drawn at rest, as a stopped mover's is (`motionSeconds = running && MoverSounds.running(cell) ? seconds : 0`). `toggle()` touches no chunk.
- `EquipmentShellModel`: the chunk mesh always leaves moving parts out (`ShapeRasterizer.fixedSurface`). The `Baked(still, moving)` pair becomes one `Layers` per cell. A moving cell's cached quads are about halved.
- `ShapeRasterizer.moves(CellKey)` had no other user and is deleted.
- `VesselScreen` comment and the `motion.hint` text: no longer "in the chunk mesh, to compare frame rates".
- Behaviour change: with the switch off, parts more than `RANGE` (96 blocks) away are not drawn, as with it on. Before, the switch-off chunk mesh drew them at any distance. The switch remains global, client-only and not saved.

## 5. Verification

- Before the rebase: `test --tests ShapeRasterizerTest` 37/0, with all main and client code compiled; `slowGates --tests ProcessUnitTest` (full minus fixed surface areas) 22/0. The rebase brought in documentation only (`AGENTS.md`, `CHANGELOG.md`, `README.md`, `gradle.properties`).
- Merge: the owner waived the test run ("merge with no test"); `test` and `slowGates` were not run at `2fbb80f2`. `git ls-files -i -c --exclude-standard` empty.
- No gate covers the switch or the chunk mesh choice. Not run in game: the client is the only judge of the flicker, so one in-game check stays open (switch the button and watch the terrain).
- Release prep `2fbb80f2`: `mod_version` 0.12.3, CHANGELOG `[0.12.3]`, README version. Code guide: `client-gui.md` (`MachineAnimation`, `EquipmentShellModel` rows, rendering note), `equipment.md` (note) and `README.md` snapshot at `2fbb80f2`.
- Tooling cleanup: the batch created no probes, scripts or rigs; tool folders: none.
