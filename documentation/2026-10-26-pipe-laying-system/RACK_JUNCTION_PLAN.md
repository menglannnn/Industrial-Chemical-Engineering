# Rack junction plan: one block decides

Batch `2026-10-26-pipe-laying-system`, 2026-10-26. Owner: "The corner and maybe T/Cross junction needs to be determined based on single block placement direction. Show me your model before building it ingame". This plan is the model. It replaces decision G of `RACK_FRAME_PLAN.md`.

**Status: built 2026-10-26** on `claude/pipe-laying-system-092ac8`, not merged. Owner: "Approved: build the junction model (D-J as drawn, X1, keep the sneak opt-out)". See section 7.

## 1. Expected behaviour (real racks)

- A rack turns at a corner, where every line turns with it.
- A branch rack meets a main rack at a tee. The main lines pass; the branch lines turn left or right into the main rack.
- Two racks that cross stand at two heights. Lines on one tier cannot cross lines on the same tier, so a line that changes direction also changes height.
- Racks side by side are parallel runs; each run turns at its own corner.

## 2. What the code does today

A rack's shape comes from its neighbours' facings and changes whenever a neighbour is placed. A rack placed while looking another way reshapes the racks already standing (the 2x3 fault, F3). `131857a3` added a rule so wide turns nest; this model makes that rule unnecessary.

## 3. The model

**J1. A block's shape is decided once, when it is placed, and never changes.** Neighbours placed or broken later redraw steel and joins only. Broken and placed again, a block decides again.

**J2. At placement, the block looks at the racks whose run ends point into it ("feeds")** and at the player's look:

| Feeds into the new block | Look | Shape |
|---|---|---|
| None, no rack beside | any | Straight along the look |
| None, a run beside | any | Straight along that run (a wider rack) |
| One, in line with the look | – | Straight: the run goes on |
| One, across the look | – | Corner from that run toward the look |
| Two in line, look along them | – | Straight: fills the gap |
| Two in line, look across | – | Tee: the run passes, the branch opens toward the look |
| Two at a corner | any | Corner joining them |
| Three | any | Tee joining them |
| Four | any | Decision X |

Sneaking places a plain straight block along the look, with no junction. A rack beside a run, with its side face against the run, never feeds it, so groups side by side make no junctions.

**J3. Lines in a junction block** (`drawings/rack-6-junction-lines.svg`):
- Corner: every line turns, nested about the inner corner, keeping its order (as today).
- Tee: two corners back to back, plus the run. Lines that turn use the run positions nearest the branch: left-turners from the branch's left positions, right-turners from its right. One run position can carry a left-turner and a right-turner, in its two halves. Through lines use the positions on the far side. A line on a far position cannot turn in this tee.
- Steel of a junction (corner or tee): H columns at its outer corners only (a corner's three, a tee's two away from the branch), and an I beam 4/32 m deep under the lines on every face. The straight racks next to it carry a full bent against it, which holds its inner corners. No column stands inside a bend.

**J4. Wide racks** (`drawings/rack-7-crossing-wide-turn.svg`): placing one block at a time with these rules builds a two-wide turn as two nested corners on the diagonal.

## 4. Decision X: four runs meeting (crossing)

Physics: two full runs cannot cross on one tier. The block's crossing levels pass one line over the tier and one under it, about 6" at most each.

- **X1 (recommended).** No cross block. A crossing is built at two heights: a perpendicular rack stacked on the other. This works today. With four feeds, the block is placed straight along the look.
- **X2.** As X1, plus a riser rule, so a line can turn from the lower run up into the crossing run above. Today the riser points of two perpendicular stacked racks do not meet. They would meet where the two runs' position grids cross.
- **X3.** A cross block in which lines only turn, nested in its four quadrants, and none pass straight through. It is possible, but rarely useful.

## 5. What changes in the code (the plan; as built in section 7)

- `PipeRackBlock`: shape decided in `getStateForPlacement` by a pure `RackGeometry.placed(neighbours, look, sneaking)`. `updateShape` no longer changes the shape. `RackGeometry.corner` and its in-line rule are deleted.
- `Shape` gains `TEE`; blockstate values are saved by name, so this is append-only. A tee's lanes are corner lanes on its two halves plus through lanes; `valid`, `fits`, `canonicalPoint`, `path`, `along` and `PipeRoute`'s steps learn the tee. Formats are branch-only (RD9); corpus `rack-1` stays valid.
- Steel of a junction as in J3 (sturdy pass, 2026-10-26). Before it, the junction had four columns, and in a stacked junction the inner-corner columns ran up through the tier, about 6/32 m from a 14" line's bend against 6.2/32 m needed. Columns at the outer corners only remove that case: the neighbours' bents stand outside the junction block. The render harness's `-Dclash` check finds no pipe in steel in the corner, tee, crossing and two-wide turn. The build adds the same check to `everyLaneStaysInItsBlockAndClearOfTheSteel`.
- A straight rack next to a junction draws a full bent on that face (one drawn bit, as a run end).
- Tests: the J2 table, one test per row; the tee's lane pairs keep their clearance, as `everyPairOfLanesInACellKeepsItsClearance` does; the two-wide turn placed in any order; a route through a tee.

## 6. Decisions (owner, 2026-10-26)

- **D-J:** the J1-J3 model as drawn (`drawings/rack-5-placing.svg` to `rack-7`) and rendered (`drawings/junction-*.png`). Approved.
- **D-X:** X1. Four feeds place a straight block along the look.
- **D-S:** sneak opt-out kept.
- `RACK_FRAME_PLAN.md` decision F: all seven parts at the sturdy sizes, built with this model.

## 7. As built

- `RackGeometry.placed(Neighbours, look, sneaking)` is the J2 table; `PipeRackBlock.getStateForPlacement` calls it and `updateShape` only redraws. `RackGeometry.corner` and its in-line rule are deleted.
- A tee is `Shape.TEE` (appended), facing its branch; its run passes from its left face to its right. Its lane options are a position plus a leg: `RUN` (run positions, counted from the tee's back), `TURN_LEFT` and `TURN_RIGHT` (branch positions). A lane is drawn and routed on its leg's rack (`RackGeometry.leg`): the run as a straight cell along the run, a turning lane as the corner it turns through. A left-turner lands mirrored across the branch (run position 4 − p − width), a right-turner keeps its number. Room: no shared branch position; left-turners left of right-turners; run lanes behind every landing.
- The lane option codes are unchanged; the tee reuses the tier, above and below levels as its legs. Corpus `rack-1` reads as before.
- Tests: one per J2 row and the group cases (`RackGroupTest`); the tee's points, room and columns (`PipeRackGeometryTest`); a route through a tee (`PipeRouteTest`); every lane clear of every drawn member in every shape and frame.
- Observations from the build, as approved and not changed:
  - Feeds outrank "beside". A second row of a two-wide run, placed block by block while looking across the run, meets the first row's end feed and makes corners. Placing it looking along the run, or sneaking, gives straight blocks.
  - Sneaking places straight along the look, not along a run beside it.
  - In a tee, the free-slot marks of the laying tool show branch positions only.
