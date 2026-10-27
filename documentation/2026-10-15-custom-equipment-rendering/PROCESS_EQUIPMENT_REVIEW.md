# Drums, column structure, pipes and accessories: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · commits `d1701780` (code) and `80fdce9b` (changelog) · plan `PROCESS_EQUIPMENT_PLAN.md`, photos `PROCESS_EQUIPMENT_PHOTO_SURVEY.md`.

## Physical expectation

- **Drum.** One well-mixed vessel: vapour leaves the top nozzle, liquid the lowest side nozzle, the feed enters a side nozzle (OD1). The liquid nozzle sees the headspace pressure plus `ρ g h_L` over the drum's own height (vertical: T/T + D/2; lying: D), not the one-block tank's 1 m.
- **Actuator hydraulics (OD4).** A pump's, compressor's or valve's half link carries the line's bore: a 0.5 m line no longer drops ~10 bar through a 0.05 m stub. Equal bores give the old doubles.
- **People on a column.** A Minecraft player is a 0.6 × 1.8 m box that stays aligned with the world. On the back diagonals, where the ladders stand, it spans 0.85 m radially, plus 1/8 m of collision sampling: 1.03 m. Plants give 0.76 m clear in front of rungs and 0.9–1.5 m platforms.

## What the code does (owner items 1–5)

1. Drums: two forms renamed; each registers as a RESERVOIR with nozzle ports (top VAPOR, lowest side LIQUID, others BULK), its design volume and level height; checkpoint format 9 (fresh worlds). Skirt access opening; top and side nozzles; no vent. Use opens the tank screen; sneak-use opens the datasheet.
2. Column (`ColumnStructure`): clad shell with flashing rings, skirt opening, part-ring platforms at the manways with handrails, mid-rails, toe boards and knee braces, caged ladders alternating on the back diagonals, manways with hinge davits, top ring with lifting davit, process nozzles at the front and sides (shape only). Constructor form Column, import from the nearest column calculator, the calculator's schematic (`ColumnDiagram`).
3. Pipes: OD = 1.1 × bore, capped at 1 m; long-radius elbows, tees, flanges at equipment and every 6 blocks, collision follows the OD.
4. Accessories: necks and flanges per face sized by the connected pipe; hydraulics follow the line (OD4).
5. Placement: only pipes and valves can face up or down.

## Defects found in the dev-client check, fixed in their stage

- **Skirted drums never bound.** The skirt hides the bottom head, so the node cell at the head's pole had no facet and was laid out as an interior block, which has no block entity. The drum had no identity and its tank screen never opened. Fix (layout stage): a drum's node is always a shell cell. Test `aDrumsFluidNodeIsAShellCell`.
- **Decks too narrow for a player.** The 0.9 m deck left 0.75 m between cladding and handrail, and 0.49 m in front of a ladder. A player dropped into the cage came to rest on the handrail band. Fix: `MIN_DECK` is derived from the player and the walls (1.45 m). Test `aPlayerWalksTheDecksAndStandsInFrontOfTheNextLadder` uses the real collision boxes; it failed at 1.38 m until the half-voxel diagonal term was added.
- **Tank ports on one-link runs (regression of this batch).** A one-link run from a junction into a tank gave the tank's port to both ends, and `PassiveNetwork` refused it. This failed 10 tests in 5 classes of the first gate run. Fix: `PhysicalFluidTopology.endPort` gives a port only to the tank end.
- **Constructor schematic overflowed** the 854 × 480 window. The strip was re-laid out (shot 37; before: `process-check/before/`).
- GameTest sources used the old menu constructor; a 5-argument form with the standard reach is back. `Math.pow` in `EquipmentShape` failed the determinism scan.

## Gate tests changed (validated by the owner 2026-10-16)

- **OD4 moves one pinned test.** `FluidPumpedFillLineTest` gas transfer: the compressor holds its ratio between its own ends, so the discharge tank misses it by the line friction at the closing flow. With OD4 the old rig RWCPR left the tank 7.2 Pa short, outside the stated 3 Pa. The rig is now RWCWR, wide blocks on both sides: 0.55 Pa. The alternative is to keep RWCPR and widen the tolerance to 8 Pa.
- **Update-tag whitelist.** `identityBindingOnAChunkLoadCarriesNoViewData` now allows exactly `FluidIdentity`, `Bore` and `Faces`: the pipe model's static geometry, refreshed on the engine schedule, no view data. Its first failure left a tank in the shared GameTest world, which failed two later tests; a probe confirmed that save and load keep all three devices.

## Checks

- In game (fresh worlds, bridge lane), shots 31–42:
  - The drum opens as "Drum"; a pipe on its west nozzle is listed as "Side (west): mixed outlet".
  - A 0.3 m pipe scales the pump's flange on that side.
  - Vertical pipe and valve are placed.
  - Climbing: the caged player slid 6.7 m with no damage and stood on the deck at its walking surface; open-air control: −7 HP over 10 m.
  - Import loads the calculator's design (40 trays, feed 37, 8 m, 3 draws, 3 pumparounds, 1 steam).
- Gates on the final code: full suite 215 classes, 1,225 tests, 0 failures; 33 junction lines, 0 diff (newtonSolves 339); chain-100 exact 0; fluid GameTests 31/31, column GameTests 2/2. The first run, before the fixes above, had 12 failures and the GameTests did not compile.
- Quad counts (`LayoutCounts`): columns 10 k (3 m × 15 m) to 87 k (15 m × 47 m), 3–7× a drum of similar size; the FPS pair was not rerun. Process note: one probe test ran under Gradle while the dev client was up (AGENTS.md breach, one class).

## Known simplifications and open items

- The node sits at the bottom pole, so a liquid nozzle above it misses `ρ g Δz` (0.75 m on a 3 m drum: about 7 kPa for water). Two BULK nozzles facing the same way read alike on the tank page.
- Cages do not collide. The davit mast narrows the top ring; both halves are reached from the ladder.
- Column import comes from the nearest calculator only; the plan's preset-library import is not done.
- In the test world, a line holding a pump and valves forms no island, with a one-block tank as with a drum (control in `DrumLineProbeTest`). This predates the batch and was not investigated.
- Before the merge: detach `FrameTimeProbe`, bump the version, and write the code guide (new equipment chapter; `fluid-runtime` for ports, level height and format 9; `client-gui` for `ColumnDiagram`). Code guide: not yet updated. Tool folder: `tools/custom-equipment-rendering-poc/` (`process-check/`: scene, views, drop scripts, probes `Ports`, `Ladders`, `NodeCell`, `DrumLineProbeTest`, `SavedLedgerProbeTest`; `LayoutCounts` updated; logs).
