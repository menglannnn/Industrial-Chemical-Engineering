# Rack frame and grouping plan

Batch `2026-10-26-pipe-laying-system`, 2026-10-26. Owner report on the layered rack (`8d693dd1`): "2x3 model is broken, also find some real photo about how they looks like, this is too barren"; "The pipe rack should be able to stack"; "And crashed using the color selector". With two photos of their own: "These are much better".

**Status: built 2026-10-26** on `claude/pipe-laying-system-092ac8`, not merged. Owner: "build ... the sturdy frame (D-F all seven parts at the sturdy sizes) in the mod". Decision G is replaced by `RACK_JUNCTION_PLAN.md` (built with it). See section 6.

## 1. Findings

A real pipe rack is a row of bents (columns with a transverse beam per tier), tied by longitudinal struts, braced in its side planes and sometimes in plan, on concrete pedestals. Wide racks have middle columns. Tiers stack on the same columns. Racks built side by side, in line or on top of each other form one structure with one run direction.

| # | Expected | What the code did | Status |
|---|---|---|---|
| F1 | A palette click paints the edited line. | The custom field's responder heard the field's own `setValue` and painted again, without end. The stack overflowed (crash 20:28). | Fixed `131857a3`, as the constructor's Paint page guards it. |
| F2 | A world from an older build of this branch is refused cleanly. | The world was made at 17:08 with the five-slot racks. It was refused at load: "Invalid rack lane 4 of 2 positions" (crash 20:19). The branch formats were redefined in place (RD9), and a refusal shows as a server crash. | As designed. Use a new world. |
| F3 | Racks placed together form one rack: one run, bents in line across the width, joined sides. | Each rack faces the player's look. A 2x3 built from several viewpoints mixes facings. A rack facing across the group becomes a corner, and racks of crossing facings do not join, so the frame breaks apart (render `grid` mixed layouts). | Decision G. Workaround: place every rack looking the same way. |
| F4 | A turn several racks wide nests: each run turns at its own corner, on the diagonal. | The outer run's rack beside the inner corner took a corner too, and the outer run ended there. | Fixed `131857a3`: a corner whose turn comes in through an end continues the run there. |
| F5 | Every rack cell can be seen, aimed at and mined. | A straight cell joined on both sides with no bent draws no steel. In a group three wide, the middle cells between bents are invisible and cannot be targeted. | Fixed by F-light, or by D-F5. |
| F6 | Stacked racks form a multi-tier rack. | In the logic this works. The lower rack's posts run up to the upper tier, and a line rises from one tier into the next (new route test). In game the upper rack takes the look's facing (F3), and its thin steel is hard to aim at (F5). | Through G and F. |

## 2. Grouping (decision G)

- **G1 (recommended).** A placed rack takes the facing of the rack it is placed against: above, below, at an end or at a side. If it is not placed against a rack, it takes the facing of an adjacent rack: below, then above, then in line with the look, then beside. With no rack next to it, it takes the look. Placing while sneaking takes the look, which starts a crossing run (a corner). The corner rule stays.
- **G2.** Keep the look. Using the laying tool on an empty rack turns it 90°.
- **G3.** Keep as is (the workaround in F3).

## 3. Frame proposal (decision F), from the photos

Renders `drawings/frame-*.png`: `now` is today's steel; `t2` is two tiers, two racks wide, x 0 to 5; `t1` is one tier on the ground. Each part can be taken or left:

- **F-h.** H-section columns 3/32 m wide and I-beams 3/32 m deep at the bents. Today they are 1/16 m square posts and 1/16 m deep beams.
- **F-knee.** Knee braces under each bent beam at the outer columns.
- **F-pedestal.** A concrete pedestal 3/32 m tall and a base plate under each bent column that stands on the ground, not on a rack.
- **F-middle.** A middle column at the bents where racks stand side by side.
- **F-light.** A light frame at each block face without a bent: posts on the outer sides and an angle under the lines. The lines get a support every block, and every cell has steel (fixes F5).
- **F-side.** A Warren diagonal in each outer side panel, alternating by block. In a stacked rack it runs the full block height. It is left out on a side that a lane passes through: flat exits, exits over or under the tier, crossings.
- **F-plan.** A plan brace under the tier, alternating by block. It is left out in a cell with a riser, an exit over or under the tier, or a crossing.

Not proposed: bracing above the lines, ladders, pipe shoes and guides. The tower-and-truss span of the second photo could be a later piece of equipment.

### 3a. Sturdy sizes (owner 2026-10-26: "Structure needs to look more sturdy")

The parts above, made heavier. Renders: `drawings/sturdy-sheet-*.png` (before and after), `frame-sturdy-*.png`, `junction-sturdy-*.png`. Sizes in 1/32 m (before → after):

| Part | Before | After |
|---|---|---|
| Bent | one face plane, 2 deep | both cells at the block face, 2 + 2 = 4 deep; at a run end or against a junction, 4 deep in the cell |
| Column (H) | 3 × 2 | 3.75 × 4 (flanges 0.9) |
| Bent I-beam depth | 3 | 4 |
| Knee brace | 0.9 thick, about 8.5 long | 1.4 thick, about 10 long |
| Pedestal | 4 × 3, 3 tall | 5.5 × 5.5 at an end (5.5 × 2.75 per half of a shared bent), 4 tall; base plate 0.6 |
| Side member | flat bar 2 × 1 | channel 3 deep (C 3.25 × 3) |
| Side diagonal | 0.9 | 1.4 |
| Plan brace | 0.8 | 1.1 |
| Light frame | posts 1.5, angle 1.6 deep | posts 2.75 × 2, angle 2.5 deep |

- A shared bent is drawn as two halves, one in each cell. The cell before a bent face gets the second half; this is one more drawn bit, no fact.
- Steel above the beam top stays at u ≤ 3.75 or ≥ 28.25. Tier lines span about 4.26 to 27.74 (6" on the outer positions).
- A lowered beam (24" to 28" lines, beam top 4/32) has its bent on the ground, without pedestals.
- Clearance check: a new `-Dclash` switch on the render harness samples every pipe surface every 0.4/32 m and tests it against every steel box and bar. Scenes: `proposal` t1 and t2; a clash rig with exits under and over the tier at a bent and at a run end, crossings under the tier, and a 28" line on lowered beams; the four junction scenes. Result: no sample inside steel in any scene; closest approach 0.00, which is the lines resting on the beam tops (support P2).
- Cost: about 150 to 200 quads per straight cell (doubled bents add one more column half and beam), cached per frame state.

What changes:
- Only drawn steel (state) changes. No fact, format or corpus image changes.
- New drawn bits: "stands on the ground" and "a lane passes this side".
- Collision and outline keep boxes only. The braces are drawn but do not collide.
- Lanes stay clear by construction: u ≥ 4/32 stays free, and the parts under the tier are left out where lanes go. Gate: `everyLaneStaysInItsBlockAndClearOfTheSteel` extended to the new boxes and bars.

## 4. Decisions (owner, 2026-10-26)

- **D-G:** replaced by `RACK_JUNCTION_PLAN.md` D-J (one block decides its shape at placement).
- **D-F:** all seven parts, at the sturdy sizes of 3a. Built.
- **D-F5:** not needed: F-light gives every cell steel.

## 6. As built

- `RackGeometry.steelwork(rack, lanes)` draws every part of 3a for straight cells, and the junction steel of `RACK_JUNCTION_PLAN.md` J3 for corners and tees. It returns the drawn members (boxes and bars, with their materials) and the solid boxes. `polygons(rack, lanes)` is the chunk model's mesh. `members(rack)` gives the solid boxes for collision and outline; braces do not collide.
- Drawn bits (state, `RackGeometry.STEEL`, mask 511): bent back and front, stacked, left and right joined, grounded, whole bent at the back and front ends, rising (diagonal direction by block). The block entity saves them as before, read through the mask; the server recomputes them when the rack binds at load (`refresh`), so an older value is only drawn until then. No fact, format or corpus image changed.
- "A lane passes this side" is not a bit: the model passes the cell's lanes to `steelwork`, which leaves out a side diagonal on a side a lane arm uses, and the plan brace in a cell with a riser, an exit over or under the tier, or a crossing.
- Pedestals are concrete (`equipment_concrete`, a new texture key in `pipe_rack.json`). They stand where no rack is under the cell and the beam bottom is at least 4.6/32 m up.
- Gates: `everyLaneStaysInItsBlockAndClearOfTheSteel` tests every lane of every shape and frame variant against every drawn member (bars by distance to their axis); `theSteelStaysInItsBlockAndClearOverTheTierOfTheLinesAtTheSides`; `theBentsStandWhereTheFrameSays`.

## 5. Photo references

- The owner's two photos (2026-10-26): a three-tier rack braced in its side planes and in plan; truss spans on braced towers, standing on concrete pedestals.
- Wikimedia Commons, [Pipe rack constructing.jpg](https://commons.wikimedia.org/wiki/File:Pipe_rack_constructing.jpg) (Pbujair, 2009, CC BY-SA 4.0): multi-tier bents of H columns on concrete pedestals.
- [Piperacks are installed between the High-Level Waste and Low-Activity Waste facilities](https://commons.wikimedia.org/wiki/File:Piperacks_are_installed_between_the_High-Level_Waste_and_Low-Activity_Waste_facilities_(7825628184).jpg): braced racks with truss spans over a road.
- [Piperack to Avecia's Belasis Plant](https://commons.wikimedia.org/wiki/File:Piperack_to_Avecia%27s_Belasis_Plant_-_geograph.org.uk_-_90448.jpg): a low sleeper rack with painted lines.
