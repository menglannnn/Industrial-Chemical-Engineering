# Layered pipe rack: design for review

Batch `2026-10-26-pipe-laying-system`. Owner request 2026-10-26:

> Real rack is more like a supported layered layout, and it should have facing directions, Corner can be created when combining racks with different facing. Put racks horizontally they will combine, a single block can handle max 4 pipes. Pipe going out of the rack can pass from layers between, above, or below. Make some design drawings first.

Status: approved 2026-10-26 (option A, P2; section 7); R1-R4 implemented on the branch the same day (section 8), awaiting the owner's manual test. Revised the same day after the owner's comment "The support should be just beneath the pipes": the beam now carries the lines directly, the posts stop at it, and nothing stands above the tier. Drawings:

- [drawings/rack-1-section.svg](drawings/rack-1-section.svg): end views of one block, option A (recommended) and option B.
- [drawings/rack-2-exits.svg](drawings/rack-2-exits.svg): how a line leaves the rack; a side view of a run with its bents.
- [drawings/rack-3-joining.svg](drawings/rack-3-joining.svg): facing, a corner, side-by-side joining, a stacked rack.
- [drawings/rack-4-support.svg](drawings/rack-4-support.svg): the tier support with mixed line sizes, two forms (RD11).

## 1. Expected behaviour (real racks)

- A rack is a row of bents, each two posts with one beam per tier, about 6 m apart. Longitudinal struts tie the bents together. Lines lie side by side on the tier beams, resting on the steel or on short shoes, and span freely between the bents. The posts end at the top tier's beam; nothing stands above it.
- A line leaves the rack by changing elevation. It rises into the space above its tier or drops into the space below, then turns sideways. It never crosses another line on its own tier. Only a line at the outer edge of a tier may turn flat towards its own side.
- Lines are laid on the side of the rack they will leave from.
- At an L-corner, the lines turn in nested bends and keep their order: the outer line stays outer. (The other common corner changes the tier elevation between the legs.)

## 2. What the code does now

- **Slots:** five slots on the block's diagonal. Any slot can leave through any face, so lines cross at different heights inside one block.
- **Block:** a blockstate `axis` (x, y or z); no facing, no tiers, no corner shape. Corners are lanes that turn inside one block.
- **Frame:** thin members along the block edges in every block. There are no bents.

## 3. Proposal: option A, one tier per block (recommended)

Block units, 1 = 1 m. x runs across the run, y up, z along the run.

**Positions.** Four per block, numbered 0 to 3 from left to right as seen along the facing:

- across, x = 7/32, 13/32, 19/32 and 25/32, a pitch of 3/16 (today's slot pitch);
- height y = 1/2, the plain pipes' centre line, so a straight run into a plain pipe needs only a sideways offset.

A lane takes 1 to 4 adjacent positions:

| Positions | Largest OD (m) | Fits |
|---|---|---|
| 1 | 0.172 | 6" |
| 2 | 0.359 | 14" |
| 3 | 0.547 | 20" |
| 4 | 0.734 | 28" |

A line wider than 0.734 m does not go on a rack; today's five-slot lane is dropped.

**Crossing levels.** One above the tier and one below it, at y = 13/16 and 3/16 (±10/32 from the centre), always at mid-block (z = 1/2).

- Only one-position lines cross, OD at most 0.172. They pass over and under tier lines up to two positions wide (14"), with at least 1/64 m to spare (P1 below).
- Stacked racks give two crossing levels between each pair of layers: the lower block's upper level and the upper block's lower level.

**Leaving the rack** (sheet 2):

- (a) **Up or down:** straight out of the top or bottom face at the line's own position.
- (b) **Above:** rise to the upper level, pass over the tier, leave through a side face.
- (c) **Flat:** turn sideways at y = 1/2. Allowed only when no lane lies between the line and that side in this block.
- (d) **Below:** drop to the lower level, pass under the tier, leave through a side face.
- **Along the run:** out of an end face at the position's point.

Outside the rack, the existing transition pipe meets each exit point. The sideways offsets are ±3/32 and ±9/32 and the vertical offsets ±10/32, all on today's 1/32 m grid.

**Structure.**

- **Bents** stand in every 3rd block, counted in world coordinates along the run, plus at each run end and at corners. A bent is two 1/16 m posts at x = 1/32 to 3/32 and 29/32 to 31/32 and a beam 1/16 m deep just beneath the lines. The posts stop at the beam; nothing stands above the tier. A rack stacked on top carries the posts on up to its own beam. The bent stands in the block's face plane, so the mid-block turns clear it.
- **Stringers**, 1/32 m deep, tie the posts along both post lines at the beam's level in every block, so racks placed in line read as one structure.
- **Mixed sizes** (sheet 4, RD11). A straight beam touches every line only when the lines rest on it at their bottoms. Two forms:
  - **P1, centre lines fixed (recommended).** Every line's centre stays at y = 1/2. The beam sits just beneath the largest line on that bent; with equal lines it touches them all (6" lines: beam top at y = 0.414). Smaller lines there stand on shoes as tall as the difference of the radii: a 2" line beside a 14" line on 0.15 m, a 6" line on 0.09 m. All of today's sizes fit (up to 28"), every line meets plain pipes at y = 1/2, and crossing lines pass lines up to 14".
  - **P2, bottom of pipe on the beam (real practice).** The beam top is fixed at y = 13/32; every line rests on it, its centre at 13/32 + OD/2 rounded up to the 1/32 grid (2" at 14/32, 6" at 16/32, 14" at 19/32). Lines up to 20" fit. Joins and exits of every size but 6" need a vertical offset, and crossing lines pass over lines up to about 10" only (top at most 0.711).

**Facing and shape.**

- The blockstate holds `facing` (N, E, S, W), taken from the player's look as for stairs. The facing orders the positions.
- A rack becomes a corner where a rack with a perpendicular facing stands at the side of one of its ends (sheet 3). In the corner block the lines turn in nested bends about the inner corner, with radii 0.219 to 0.781 m. The innermost bend is 1.27 OD for a 6" line, a short elbow. Wide lanes turn in a ball, as today. The corner's posts stand at its three outer corners.
- Racks in line with the same or opposite facing join straight on.
- Lanes meet where their face points coincide, so the numbering is only a label.

**Joining.**

- **In line:** one run.
- **Side by side with the same facing:** one wider rack, eight positions across two blocks, posts on the outer lines only.
- **Stacked:** one more tier; the shared posts and the doubled beams are dropped.

**Placing a line** (route search):

- A rack portion becomes a run of joined rack blocks, corners included. A line keeps one position range along it.
- The choice of range changes. Today's choice is fewest crossings (`14bc86d9`). The new choice is the positions nearest the side the line leaves towards (real practice), then fewest level changes. Lines on one tier never cross, so the crossing count goes away.
- An exit block uses one crossing level. Each crossing level holds at most one crossing line per block.

**Kept as built:** explicit joints, transition pipes, the ledger events, the paint (lanes take the primary), dismantle, the fluid side.

## 4. Option B, two tiers of two per block (alternative)

Positions at x and y = 9/32 and 23/32, OD up to about 0.20 m, tiers 0.44 m apart. Each tier sits on a beam just beneath its lines.

- The crossing between the two tiers lies inside the block. Lines leave above and below through the top and bottom faces, by the middle gap.
- Against A:
  - A rack of two tiers is only one block high, but the tiers sit close.
  - A tier holds only two lines per block.
  - Every straight exit into a plain pipe also needs a vertical offset.

## 5. Saved data

The rack formats `rack-1` and `rack-2` and the fluid facts' rack lane fields (schema 2: a lane's first slot + 1 and its width) exist only on this branch. Their slot meaning does not carry over to positions.

Proposal: redefine them in place, since no world outside the branch has racks, and replace their corpus images with images of the new layout. The rack's lane copy also gains each side joint's way out (flat, above or below). Racks in the owner's test worlds would lose their lanes.

## 6. Work packages (after approval)

- **R1 block:** facing, shape from neighbours, joining, bents and stringers, model, collision.
- **R2 lanes:** positions, crossing levels and exits; saved data and corpus images (section 5).
- **R3 route search:** rack runs, corners, the position choice, exits; transition pipes at the new points.
- **R4 checks:** unit tests and GameTests rewritten for the new layout, software renders, one in-game check.

## 7. Decisions for the owner

| # | Question | Recommended |
|---|---|---|
| RD1 | Block layout | A: one tier of four per block |
| RD2 | "Combine horizontally" | In line and side by side; stacking adds tiers |
| RD3 | Crossing lines per level per block | One |
| RD4 | Bent spacing | Every 3rd block, plus run ends and corners |
| RD5 | Support down to the ground | None automatic: stack racks (empty tiers are spare tiers) or build supports |
| RD6 | Vertical racks (today's axis y) | Dropped: vertical runs are plain pipes |
| RD7 | Corner form | Nested flat bends in one block |
| RD8 | Tee of racks | No tee shape: a rack at the side of a run stays separate |
| RD9 | Branch-only saved formats (section 5) | Redefine in place; replace the corpus images |
| RD10 | Lines over 0.734 m OD | Not on racks |
| RD11 | Support with mixed sizes (sheet 4) | P1: centre lines fixed, beam just beneath the largest line, short shoes under smaller lines |

**Owner decision 2026-10-26:** "Use P2 (bottom of pipe on the beam) for RD11; otherwise the recommended answers. Go ahead with R1-R4." So RD1-RD10 stand as recommended and RD11 is P2. Owner addition the same day: "P2 can have lowered support as P1". A line too large to rest on the beam within the block (a four-position line, 24" to 28": 13/32 + 0.734 > 1) sits at mid-height, as in P1, on a beam lowered just beneath it; such a line fills its tier, so no other line needs a shoe. RD10 stays as recommended: lines over 0.734 m OD (more than four positions) are not on racks. The owner confirmed this reading the same day: "Your reading of the lowered support is right: only the 24-28 inch lines sit at mid-height on a lowered beam."

## 8. Implementation choices (R1-R4)

Reversible choices made while building, logged for the owner:

- **I1 Lane places.** A lane's option in its cell: a tier lane is (first position p, level of its side arm l: tier, above or below), option p + 4 l; a crossing lane is option 12 (above) or 13 (below). The ledger lane number is option + 1 (1 to 14).
- **I2 Face points.** A lane registration carries its face points (ten bits per face, as the transition pipes' offsets). The fluid topology joins two lanes where their points on the shared face coincide, so opposite facings and corners need no numbering rule (schema 2 redefined in place, RD9).
- **I3 Blockstate.** `facing` and `shape` (straight, or a corner from the back or front end to the left or right side). A rack that carries lanes keeps its shape. Bents, stacked posts and side-by-side joins are drawn state, computed from the neighbours.
- **I4 Corners.** Corner lanes turn in concentric bends about the inner corner (radius 1 - x), so no lane needs a ball. A corner block carries only corner lanes. Its posts stand at the three outer corners with beams along the two outer sides; the lines span free across it.
- **I5 Exits.** Up, down and flat exits lead only into plain cells. Above and below exits lead into a plain cell or a side rack's crossing lane. A crossing lane runs side to side at mid-block. Each level holds one line: an up or down riser, an above or below exit, or a crossing.
- **I6 Clearance.** The upper level is open only where every tier lane of the block clears it by 1/64 m (lines up to 10"); the lower level always clears.
- **I7 Placing.** A line takes the positions nearest the side the route leaves towards after the rack portion (else the side it came from), then the fewest level changes; above before below.
- **I8 Starts.** A line started on a rack leaves along its run (a corner: along one of its two lane faces).
- **I9 Continuing.** A line continued from a lane into the next rack keeps the lane's width and centre height.
- **I10 Width rule.** One rule says which options take a line of a given width (`RackGeometry.takes`): a tier lane stays within the tier, and leaves above or below only if one position wide; a crossing is one position wide. The checks, the route search and the face points all use it with the line's own width. This removed the special cases that mapped a crossing to width 1.
- **I11 Bend cap.** A lane's bends have a radius of 7/16 m at most, so each bend starts clear of the bents' planes (2/32 m at each run face). Without it, a 20" riser's bend swept the front bent's beam by about 1 mm. Lines up to 11" (1.5 OD ≤ 7/16 m) draw as before.
- **I12 Drawn size.** A lane's width and height come from the drawn outside diameter on the 1/64 m grid (`PipeSizes.drawn`), in the client, the route search and the server alike. A raw outside of 0.172 m is two positions wide, but its drawn 11/64 m size is one.
