# Column nozzle rows review (finding F3)

Batch 2026-10-26-multi-machine-framework, 2026-10-27. Owner report from the review world: the column's steam nozzle does not stand where the column GUI shows it.

## 1. Expected (a real column)

Each side nozzle stands at its stage: the reflux above the top tray; a feed, a pumparound return or a tray steam by its tray; a side draw or a pumparound draw under its tray; sump steam and the reboiler's vapour return between the sump liquid and the bottom tray. Platforms and ladders are fitted round the nozzles (deck cut-outs), two nozzles at one height take different orientations, and every platform still carries a person from one ladder to the next. The GUI draws this order, so the GUI was right.

## 2. What the code did

`ColumnStructure.Rows.side` gave each nozzle a fixed side and the row nearest its stage that was clear of the other nozzles and of the access structure, searching rows below and above with no bound and reporting nothing. On the review column (2 m, 20 trays) the sump steam stood at 6.5 m instead of 3.5 m, the reflux under tray 5, the pumparound return under its own draw. Over 126 designs (scratch `tmp/steam-port/RowSweep.java`) 97 of 97 buildable designs had a nozzle two trays or more from its stage.

## 3. Options proposed

(A) nozzles first, the access round them (recommended); (B) bound the search only: every design refused (the top platform always covers the reflux); (C) the GUI follows the build: a column wrong as a process.

## 4. Decision and implementation

Owner, 2026-10-27: (A), moving nozzle pipe blocks on placed columns approved; then "Combine: stage bands, platform cut-outs and ladder moves as in (A); automatic headings by default; I can override any side nozzle's heading in the constructor."

- Bands: an inlet in the rows between its tray and the tray over it (the top tangent over tray 1), an outlet between its tray and the tray under it (the sump level under the bottom tray). Nearest the process height first.
- Headings: home sides as before (feed front; reflux and pumparounds right; draws, steam, vapour return left). A crowded nozzle turns (its side, the two at 90°, the opposite) before it changes row. The back stays clear at platform rows and manways. A first pass also keeps off the default ladder cages; no pass puts a cage into a pipe's block.
- Access: ladders move up to 40° (5° steps, the back's side first) to clear the nozzles' rooms; decks are cut round nozzle rooms, cut ends railed, pieces nobody reaches dropped; the manway stays on the piece nearest the back; the davit moves on the top ring.
- Found by the sweep: a cut could part a platform's walk between its two ladders (19 of 315 designs, slim columns: a left or right nozzle at a platform row cut the whole back quarter). A platform must carry a person from ladder to ladder, so a nozzle never parts a walk: it turns or takes another row, or it is unplaced and the design refused (F1). The room was also too wide near the shell: it is now the nozzle's neck (half its flange and 5 cm) and the block its pipe stands in, each cut at its true angle seen from the axis (a 0.5 m column's cut was a half plane), grown by a rail's bar so the railed cut end stands clear (found by `ShapeRasterizerTest`: an end rail reached 2 cm into a 0.5 m column's reflux pipe block).
- Override: `ColumnTrays.turns` (role by name, number, side), a shape fact field (shape fields 3, corpus `shapes-3.snbt`), the constructor's `Design.turns` and wire protocol `equipment-12`. The column's design page has a Nozzles button beside Change: the page lists every side nozzle (where it stands, its own side, its height), its Faces button cycles Auto, front, left, back, right (as seen from the constructor), All auto clears every turn. A turned nozzle keeps its side or is unplaced there; the status names it and Build waits.

Sweep (scratch `tmp/steam-port/NewSweep.java`; 315 designs: 0.5 to 15 m, 1 to 64 trays, four nozzle sets):

| Rule | Refused | Walks parted | Turned nozzles | Ladders moved | Deck or mast in a room | Cage in a pipe's block |
|---|---|---|---|---|---|---|
| (A), first build | 89 | 19 | 249 | 59 of 877 | 0 | 0 |
| Walks kept, full-width room | 124 | 0 | 140 | 54 of 745 | 0 | 0 |
| Walks kept, neck and block room (built) | 103 | 0 | 143 | 108 of 825 | 0 | 0 |

No built design has a structure cell in a nozzle's pipe block (rasterized). One nozzle stays outside its band: a 15 m column with one tray, whose 7.5 m sump puts the steam's band above the shell's first free row. One cage passes within a neck's 5 cm margin (1 m column).

Review column after the change (2 m, 20 trays): reflux 15.5 m right, feed 9.5 m front, pumparound draw 11.5 m and return 13.5 m right, vapour return 3.5 m left, side draw 10.5 m left, sump steam 3.5 m turned to the front, bottoms through the skirt; ladders at 135°, 50° and 130°, the top ring cut at the reflux, the davit at 30°.

## 5. 1 m columns (decided)

The 14 refusals that walk integrity adds over the first build are all 1 m columns: there a left or right nozzle at a platform's rows always reaches a walk's end, which runs a ladder's clearance (36°) past the next ladder's foot. Options: (a) keep refusing (the player builds at 1.5 m or turns the nozzle to the front); (b) a deck end runs only a player's width past a ladder's foot, which frees most of them but changes every part ring's ends; (c) a nozzle in the row under a deck passes under it and only the knee braces yield. Owner, 2026-10-27: (a), keep refusing; no code change.

Gates: quick suite 1524 green (the layout table and corpus re-recorded, compat 11/0 and golden 1/0 re-run); fluid GameTests 29/29; the review plant rebuilt COMPLETE (17 slots, 248 devices); one MCP client check of the Nozzles page: the steam Auto front at 3.5 m, turned left or back unplaced with Build inactive, turned right placed, the turn kept across a reopen. `slowGates` at the merge lane. Code guide: chapter updates at merge (equipment layout, constructor, shape fields 3, protocol equipment-12).
