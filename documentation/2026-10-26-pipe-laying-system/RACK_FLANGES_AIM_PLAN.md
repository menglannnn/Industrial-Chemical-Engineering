# Rack flanges, placement facing and slot aim

Batch `2026-10-26-pipe-laying-system`. Owner requests of 2026-10-26, after the junction model and sturdy frame (`527ba706`):

- "Pipe on rack should have their flange rendered normally"
- "Placed rack direction should be strictly based on player facing"
- "Allow user to pre-visualize and choose which slot on the pipe rack to be placed on, both during initial and final segment placement, and allow the use to select the direction by aiming at different parts on the rack"

Owner answers (same day): flanges "Staggered, off steel"; slot choice "Aim on the tier".

Status: built, commits `84da49e5` (flanges, facing) and `3f6a39e6` (aim). Quick suite 1517/1517, fluid GameTests 25/25. Not checked in game; the owner's manual test covers it.

## 1. Flanges, as built

Physics: a laid line on a rack is the same pipe as a line on the ground: flanged at its ends and at spool breaks. Real spool breaks avoid supports, and flanged neighbours are staggered so that two flanges never meet side by side.

- **Open end:** a blind flange, as a laid pipe's dead end. It stands in from the face past every steel member its plate and the nuts behind it would reach (`RackGeometry.setback`): just in from a bent (2.5/32 m at a shared bent, 4.5/32 m at a whole one where the flange reaches the column), at the face where there is no bent.
- **Spool pair:** a lane joined straight through a straight cell carries a flange pair every 12 blocks (as a laid pipe), staggered 3 blocks a position, so lanes side by side flange 3 blocks apart. The pair stands at the nearest place to the face the side braces leave low where both plates and nuts clear the steel (`RackGeometry.spoolAt`): 2 to 6.7/32 m in.
- One search does both (`clearOf`): a disc of the flange's radius, stepped in past each member within that radius of the line; a brace counts only over the stretch of it that comes that near.
- Side channel flanges and junction side I-beam flanges narrowed from 3.25 to 2.5/32 m, so the outer slot's 6" flange (0.30 m) clears them.
- The lanes take the tool's secondary paint on their flanges, as laid pipes do. Rack lane copy format `rack-3` (fact field `Secondary`, saved only when it differs from `Paint`; absent = the paint, as before); corpus image `compat/racks/rack-3.snbt`.

Measured over all positions, both brace directions and six frames (shared, whole, stacked, none) with the probe in `tools/pipe-laying-renders/FlangeProbe.java`:

| Line | Spools with no room | Open ends capped |
| --- | --- | --- |
| up to 16" | none | none |
| 18" to 21" (3 positions) | none | 4 to 10 of 48, all in stacked racks |
| 24" | none | none |
| 26" to 28" (lowered beam) | 4 to 6 of 12, all in stacked racks | 28": 4 of 24, stacked |

Where no spool fits, the line runs on welded to the next spool. Where no blind flange fits in its own half of the cell, the end is capped at the face (the flange would meet the stacked rack's side brace where it runs high).

Gate: `PipeRackGeometryTest.everyFlangeStandsClearOfTheSteel` (every flange and bolt surface of every lane, open and joined, spool or not, in five frames, both axes). The render harness's `-Dclash` on the new `flanges` scene: 0 of 2.5 million samples inside steel. Renders `drawings/flanges-*.png`.

## 2. Placement facing, as built

`RackGeometry.placed`: a placed rack faces the look in every row of the J2 table. A corner faces the look; its shape bit says which end and side it joins. One exception: three feeds make a tee, and a tee faces its branch (the feed with none across from it), because the tee's frame is drawn from its branch as its front. With no feed the rack is straight along the look, whatever stands beside it.

## 3. Slot aim, as built

- **Ray** (`PipeLayingClient.aimed`): the client walks the view ray block by block up to the block it hits. The first rack cell whose tier (the beams' top) the ray crosses inside the cell is the target, so aiming through the open frame picks the rack. If the hit block is that rack and the hit is on a lane, the lane is picked as before (continue a lane). The crossing point goes to the server as the click's local point, just under the beams' top, where no resting lane is picked.
- **Aim** (`RackGeometry.aim`, client and server alike): the run face nearest the point in plan is the face the line passes through to its route (it leaves there at the first click, arrives there at the second). The lane runs from that face to another run face: on along a straight run, round a corner's bend, or a tee's run or turn. Of those lanes the free one whose centre line passes nearest the point wins; with none free, the nearest, refused (`anchor.slot_taken`). Exits over, under or out of the side are left to the route, in later cells.
- **Route** (`PipeRoute.End`): an aimed end carries its option, its `through` face and its `open` face. The search keeps the option, leaves the start only through `through`, enters the end only through it, and runs the lane open at `open`. The server's `check` refuses another option or face.
- **Ghost:** before the first click, and before the second while no route is found, the aimed lane is drawn as a ghost (the tool's paint, red if refused or if the route to it is refused) with a green square on the face it passes through (red when refused).

Gates: `RackAimTest` (face and slot, taken and full cells, corner, tee), `PipeRouteTest` (aimed start and end: slot kept, face kept, the route comes round, `check` refuses other slots and faces), the fluid GameTest (aim resolved on the real rack, the nearest free slot beside a laid lane, both paints on the lanes).

## 4. Residuals for the owner

1. **Flanges against the neighbouring pipe.** Positions are 3/16 m apart, sized for bare pipe plus 1/64 m. A flange of a 5" or larger line reaches into the pipe on the next position: about 1 cm for 5", 5 cm for 6", 7 cm for a 14" line beside a 6". Spool pairs never meet each other (3 blocks apart), but a spool pair cuts the next lane's bare pipe, and blind flanges at one run end overlap. Options: (a) accept as drawn; (b) space flanged lines wider (a 6" line would take two positions); (c) set the blind flanges of neighbouring lanes in by alternate depths. **Decided 2026-10-26, owner: "Accept residual 1 as drawn: flanges of 5"+ lines may reach the next position's pipe."** Option (a); no code change; the rack's position pitch and widths stay.
2. **Corner ends are capped.** A corner's bend meets both faces square; a flange set in would tilt into the junction beam, and a tighter bend closes on the next lane (6": 4.7/32 m apart, 6/32 m needed).
3. **A three-feed tee faces its branch**, not the look (section 2).
4. **Aim needs the tier crossed inside a rack cell.** Looking level along the tier, the ray never crosses it; the block hit decides (the frame: the aim uses that point; a lane: that lane).

## 5. The second click that dropped the start (fixed 2026-10-27, `43ad8063`)

Owner report: "right-clicking the rack 2nd time with the tool often cancels the ongoing placement and start a new placement".

- **Expected.** A line is one pipe: it passes through each block once, and a rack slot holds one lane. The first click fixes the face the line leaves through, and the second fixes the face it arrives through. If no route honours both, the tool says so and the start stays; a refused lay changes nothing.
- **What the code did.** The route search kept no record of the cells its own route held. A rack cell's room lists only the lanes already laid, not the line's own. A line aimed out of its start away from its end, or onto another slot, came round back through its start cell, often on the start's own slot. The client sent that route and dropped the start at once. The server's check refused it (a cell twice), so the next click began a new line. Over every aimed pair on a 10-cell run (2", 6" and 14"), 4170 of the 13702 routes found were of this kind. All of them passed back through the start cell. Most were first clicks on the cell's far half; the rest put the two ends on different slots two cells apart.
- **Fix.**
  - Search (`PipeRoute.holds`): a route never steps into a cell it holds.
  - An aimed end is entered only from the cell beyond its through face, so the route steps from that cell into the end and nowhere else. This adds no special case: a route that left that cell any other way could never come back to it. It makes an impossible pair fail at once: worst 3 ms against 76 ms with the first rule alone.
  - The server answers each lay with `PipeLayAnswer` (its message, and whether it laid the line). The client keeps the start until the server lays the line. It sends one lay at a time, and a refused route shows the server's reason until the aim moves.
  - On the same probe, 12484 routes are now found and laid, and 3916 pairs are refused on the client with the start kept. The server refuses none.
- **Gates:** `PipeRouteTest.anAimedStartLeavingAwayComesRoundOutsideItsCell` and `everyAimedRouteAlongARunIsOneTheServerLays` (every aimed pair on an 8-cell run, 2" and 14": every route found passes `check` and `blocked`, each cell once). Both fail with the rule off. Not checked in game.
- **Follow-up, 2026-10-27.** The owner first answered "still happening". On retest: "it's working again now". It did not reproduce; the client tested was built with the fix.

## 6. A one-block line on a rack (fixed 2026-10-27, `eeb0742f`)

Owner report: "you cannot place single-block pipe on the rack".

- **Expected.** A one-block pipe on a rack is one lane in one cell, open (blind-flanged) at both its faces. It has no route to pass on to, so which half of the cell each click aims at does not matter. The slot does: one lane holds one slot. A pipe or lane next to the cell can join the lane on either of its faces.
- **What the code did.** With both clicks on one cell, the search still read each click's half as a direction: the start leaves through its half, the end arrives through its half. Two clicks on the same slot and the same half were refused, "Both ends meet the same face". Two clicks on different slots got "The end cell is occupied". A pipe next to the start's cell joined only on the face the start had aimed through.
- **Fix.**
  - `PipeRoute.ends`: a one-cell line with an aimed end runs across that lane's two faces. The search's one-cell branch takes its faces from there.
  - Both aimed ends must name one slot (new refusal `ONE_SLOT`, "A one-block pipe takes one slot: aim the end at the start's slot").
  - A joint must lie on one of the lane's faces. The server's `blocked` checks this too.
- **Measured** (the section 5 probe, 10-cell run, 2", 6" and 14"): laid 12484 → 12704 (the 220 one-cell pairs on one slot and one half). The 1200 one-cell pairs on two slots now refuse with `ONE_SLOT`. The server refuses none.
- **Gate:** `PipeRouteTest.aOneBlockLineOnARackIsTheAimedLane` (both halves, a pipe on either face, two slots refused, a side pipe refused by the search and the server). It fails with the rule off. Quick suite 1520/1520, fluid GameTests 25/25. Not checked in game.

## 7. A laid one-block lane drawn only after a reload (fixed 2026-10-27, `9b5f1fcb`)

Owner report: "Seems that placed single block pipe does not update visually, I need to close the world and reopen it to see it".

- **Expected.** The server is the one holder of a rack's lanes, and a client draws only what the server sends it. So every change to a rack's lane copy must reach the clients that tick, whatever the lane's joints.
- **What the code did.** `addLane` changed the copy but did not send it; `removeLane` did. A new lane reached the clients only through `refresh`, which sent when a drawn join changed. A lane in a longer line always changes one: its joints come to meet the next lane or pipe. A one-block lane is open at both faces and has no joint, so nothing changed and nothing was sent. The server saved the lane, so it showed after a reload.
- **Fix.**
  - `PipeRackBlockEntity.setLanes`: on the server, a copy that changes is sent. `addLane`, `removeLane`, `refresh` and `bind` all go through it, so the separate sends in `removeLane` and `refresh` are gone.
  - `bind` at chunk load now also sends any lanes it adopts or drops after the chunk went out; before, that was a latent stale-client case.
  - A tick's block updates go out together at its end, so a lay still sends each rack once, with its final drawn state.
- **Gate:** the fluid GameTest `aOneBlockLaneOnARackIsSentToTheClients` lays a one-block lane on a rack placed two ticks earlier. It then checks that the rack sits in its chunk's pending block updates (`ChunkHolder.changedBlocksPerSection`, read through `ObfuscationReflectionHelper`). It fails with the rule off. Quick suite 1520/1520, fluid GameTests 26/26. Not checked in game.
