# Drum skirt, silhouette outline, side connections, nozzles at the pipe's size: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-16 · committed on the branch as 91fcb448 (code) and d43509f5 (changelog), not merged.

Owner input: outlet "through that big hole", slots to see inside, the outline "cut it in half"; "specify the location & orientation & number of side pipe connection"; connection diameter "should scale with connected pipe diameter", resizing to "start with port to vessel connection, not on the junction" (chosen: nozzle always pipe size); "skirt port and drum-bottom-to-port size should scale as well".

## Physical expectation

- **Drainage.** A skirted drum drains from its bottom head's pole: on D3 × 4 with a 2 m skirt the pole is at 1.25 m; the line drops, turns in a 1 D elbow and leaves through a skirt port at 0.5 m. A real skirt is a closed plate; the slots are a game choice, so collision stays solid.
- **Outline.** A cylinder seen from distance d shows two generators at φ ± acos(R/d); a fixed line at 0° runs down the face the player sees.
- **Nozzles.** Placed by elevation, orientation and hillside offset, flanges not overlapping. The line sets the size and the flange matches it; where the vessel cannot carry that size, a reducer sits at the vessel, never at the flange. The bottom line fits what the head and base plate allow: 0.69 m here, with the line at the lowest cell centre.
- **Hydraulics.** The drum-to-pipe joint has the pipe's bore.

## What the code did

- The outlet left through a separate sleeve in a closed skirt; four fixed outline meridians; one fixed inlet.
- Nozzle, line and opening were capped (bottom line 0.5 m, opening r 0.3 m), so a wider pipe stepped at the flange.
- Hydraulics were already right: a link to a reservoir takes half the pipe's own geometry. My mid-task "50 mm joint" claim was wrong.

## Fixes (in their stage)

- **`EquipmentShape`**: `skirtOutletOd()` sizes the bottom line (elbow under the pole, port clear of base plate and seam, at most D/2) and `openingRadius()` cuts the port for it (`PORT_CLEARANCE`, `PORT_COLLAR`); `Connection` (side, station, offset; at most 6) with `clashes()` judged at a one-block flange; `Nozzle.exit` (where a reducer starts); `Role.SIDE`.
- **`ShapeRasterizer`**: hollow skirt with slots; `outline(shape, eye)` draws the silhouette; `nozzle()` gives the flange the pipe's size, the neck the pipe's size up to `maxOd`, then a reducer from `exit`; the bottom line (riser at the pole, 1 D elbow, run) and `skirtPort` at the line's size. The old sleeve branch is deleted: every drum outlet now has a port.
- **Server and helpers**: `EquipmentStructure.refreshNozzles` without a cap; `PipeSizes.MAX_OUTSIDE`; `DeterministicMath.atan2`.
- **Constructor and network**: a Nozzles page (Faces, Height or Along, Offset, Add, Remove, first clash; Build waits on a clash). Protocol `equipment-4`; NBT `Connections` (fresh worlds).

## Checks

- **Unit suite**: 1,229 tests in 215 classes, 0 failures, before the 1 D elbow; equipment and source gate after it, 37/37.
- **Rule breach**: one equipment run went while the bridge client was up. Both runs were repeated with no client.
- **New tests**: port and slots by sight-line count, connections and clashes, silhouette, reducer. **Changed expectations**: LIQUID faces the front; skirt rows hold no OPENING and show the HEAD; a small drum has a line-sized port.
- **Probes**: `VesselSweep`, and `VesselSweepSized` at 1, 0.693, 0.3 and 0.05 m: 0 back faces seen first. No GameTest builds a drum; solver code is unchanged.
- **In game** (bridge lane, fresh worlds, shots 52–61): port and slots; outline from the front and the diagonal; Nozzles page (add, turn, hillside, clash); bottom line and port at 0.05, 0.3 and 0.9 m bores, the 0.9 m bore with a reducer outside the skirt.

## For the owner to validate (reversible)

- The bottom line stays at the lowest cell centre, which caps it at 0.69 m; a 1 m line needs it raised one block.
- The collar is fixed at the largest port; three slots, one facet wide.
- Side connections are BULK at any height; at most 6; Add takes the highest free place.
- The reducer's loss is not modelled.

Code guide: still to be written with the batch. Tools: `tools/custom-equipment-rendering-poc/skirt-check/`; logs `logs/skirt-check-2026-10-16.tar.gz`.
