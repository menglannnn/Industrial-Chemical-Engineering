# Kit consolidation: lane results (2026-10-18)

## Lane B: delayed coker (done, lane gate 52/52)
- 1379 -> 873 lines in five files. Kit: stairTower (back), squareDeck (switch deck RAILED|WALL_EDGE, middle deck with
  fills), platform strips (cutting deck), brace X per storey, hoop, beam, box, concreteTable (hole per drum), pipe +
  elbow, end, motor PLAIN; PipeSizes.od replaces ColumnStructure.size for line sizes.
- Visible: X per storey; 0.9 m flights, 1.2 m landings on beams, tower ~1 m further -x; table columns on pads; mitred
  elbows; toe boards with tops, stopped at openings; chute openings show four walls; hoist motor cowl/box/pedestal; hose
  8 sides; FEED, STEAM, VAPOUR_OUTLET flanges on the structure's face (kit `end` rule), 1 m inward, same base/direction.
- Counts: box one cell shorter in z (old tower end rail 1.25 mm past zb+2); quads +1.4 to +2 %; worst 10 x 2 at 16 m
  support 79,533 (base 78,146), 467 under the ceiling (X braces 1,312, landing beams 470, jacket fill ~990).
- Kit gaps: pipe side count; fill draws a face the body always hides (~48 quads/drum at 24 sides); stair tower
  centring (towerLength(stops) or a centred option; placed from TOWER_RISE, off up to 0.4 m); squareDeck needs an
  opening; elbow 90 deg only (gooseneck stays torus).
- Owner choices: landing beams (470 quads at the ceiling case); jacket fill 8 sides; one X per storey (~55 deg);
  nozzle faces moved by the kit rule; 467-quad margin.

## Lane A: centrifugal compressor, steam turbine, condensing turbine (done, lane gate 52/52)
- Probe over 614 shapes (all sizes, both lengths, every support): walk, pipe-block and spacing gates all pass.
- Compressor -47/+31: concreteTable (suction hole), stairTower beside the right walkway (opening cut into its rail,
  walkway start closed), lube motors PLAIN, posts, tank stand brace tiers. Condensing -102/+47: concreteTable (two
  holes, edge railings and end rails kept), stairTower, tubesheet hoop, pump motors PLAIN, pipes via kit (8 sides,
  was 12). Steam turbine -7/+6, SteamTurbineTrain -20/+8 (local pipe helper gone; hoop for cladding ring and ribs).
- Visible: compressor tower beside the right walkway (small casings: tower starts behind the discharge pipe block);
  right walkway 1.1-2.1 m wide to meet the tower lane on a cell centre; condensing tower top landing always at the
  front (odd flight counts used to end at the back), -x walkway 1.2-2.2 m; steam turbine outlet mitred bends.
- Counts: moving parts and nozzles unchanged. Compressor one cell wider, 2-5 cells shorter, quads +2 to +4 %;
  condensing 0.6 one cell longer (tower 5.25 m past the 4.1 m table), quads -1 to -5 %; steam turbine -0 to -7 %.
- Kit gaps: pipe turns no elbow beside a zero-length leg (tripThrottle drops legs under 1 cm; 3-15 cm legs give tight
  mitres); stairTower has no length before drawing (design copies the flight-length formula + 0.175 m overhang);
  tower deck-side corner posts reach 0.025 m into the deck and touch its toe board; tower openings -> rail gaps by
  hand in each design.
- Owner choices: compressor tower beside the walkway (alt: along the table front, front moves out up to 1 m);
  widen the walkway to meet the cell-centre lane (alt: widen the table); small motors PLAIN (FULL adds a turning
  fan each, changes moving-part counts); tank stand in tiers of at most 3 m (was one tall X).

## Lane H: cyclone (done, lane gate 52/52)
- +90/-87 (566 -> 569 with rests). Kit: transition(2,...) replaces the local copy (identical sheets), brace tiers
  (struts stay local), motors PLAIN (airlock, screw drive; floor = shaft height, coupling still turns), end and
  radialSide nozzles (header apothem gone), elbow for the single outlet, head bands max(3, n/4), 11 hoops, buried
  faces dropped, posts (local post(u,...) adds the base plate).
- C1 rests: rests(...) from Access.climbs(0, [], ceil(walkway)); one rest for every deck size (two need > 18 m).
  Rest deck 1.45 m (MIN_DECK) off the right end face, both ends railed, 0.6 m gap in the outer rail; 3 posts on
  plates, 2 end beams; lower ladder from grade just outside the deck, walked through (cabin-heater pattern); upper
  ladder where the old one stood; sections 1.45 m apart. Longest climb over 363 shapes 9.125 m (was 17.125);
  walkedAndClimbed passes on all 726 (two headings). About +133 quads per rest.
- Counts: moving parts, nozzles (role/position/length/size/exit, all 363 shapes) and interior cells unchanged;
  quads -10 % (small) to +1 %; box grows only from the rest's lower ladder at -x: 112 shapes +1 cell, 31 +2, 220 same.
- Kit gaps: no "small deck on posts" piece (rest frame from post + box; the rest pattern could be one kit piece);
  post() has no base plate; platform() rails only the outer edge (rest deck's inner side closed only by rungs,
  corner post and end bands, gaps <= 0.33 m: impassable, looks open); pipe() uses PipeSizes.sides (8 on small
  pipes) so it can't meet the transition's 12-sided circle (ducts and risers stay prism).
- Owner choices: rest layout grows the box up to 2 cells (alt: lower ladder arrives at the deck's open front end
  facing -z, at most 1 cell, but that end unrailed); plain motor cowl +0.03 m fixed radius, ~1.8 x body on 0.3 m.

## Lane F: stirred reactor, adsorber (done, lane gate 52/52)
- Current ProcessUnitTest compiled beside the lane: walkedAndClimbed (9.125 m), pipesApart, free pipe blocks on 373
  shapes, 0 failures, 134 with a rest. Adsorber no longer references StirredReactorDesign.
- Stirred 550 -> 520: PipeSizes.od, local fill gone, squareDeck RAILED (on beams, no brackets), motor FULL (shaft
  part 0, fan part 1), bottom head Solid.rod headLo, brace(.., .15, 3.5), rests. Adsorber 546 -> 551 (+27 rests):
  PipeSizes.od, u.pipe, squareDeck RAILED, Solid.rod headLo, cap/lagged/reducer on PipeSizes.sides; reducer local.
- C1 rests (cabin-heater pattern, one rests(...) each, never more than one rest under 18 m): stirred 1.45 x 2.2 m
  deck on the left frame face, brackets off a girt, lower section 1 m to the front walked through (4.0 x 8 s3:
  rest at 7 m, climbs 7.125 / 5.0 m; +389 quads); adsorber 2.2 x 1.45 m deck behind the platform on two 0.15 m
  posts, lower section 1 m left (4.6 s3: 7.125 / 6.075 m; +453 to +509 quads). Inner railing open at the upper
  section (platform rails only the outer edge).
- Visible: brackets under both main decks gone; stirred deck strips reach the opening (old 5 cm gap closed); brace
  ends hidden; motor fin start angle; adsorber pipes, caps, reducers 8 sides (were 12).
- Counts: moving parts 2/0, nozzles 7, interior cells unchanged; quads -3 to -12 %; boxes grow only with a rest
  (stirred 4.0 x 8: 9 -> 11 in x; adsorber 4.6: z +1 or +2); rests off = baseline boxes.
- Kit gaps: no reducer; no flat-face rest piece (inner rail with the ladder opening); kit bracket knee 0.6 m below
  the deck, deeper than its girt or beam.
- Seen in the current test source (other lanes / integration): CABIN_HEATER 12 x 15 s3 climbs 10 m (3.125 -> 13.125);
  designsShareOnlyTheKit flags CondensingTurbine -> SteamTurbine and EbullatedBed <-> TrickleBedReactorParts.
- Owner choices: tall adsorber's lower ladder ~0.7 m past the back nozzle faces (faces one cell inside the box,
  pipe blocks free; alt: move back header ends out); adsorber rest on own posts (alt: ladder on the vessel, needs a
  platform opening); rest level rounds the deck height up (uneven split, 7.125 + 5.0 m), lower section 1 m offset.

## Integration notes (orchestrator)
- designsShareOnlyTheKit: the CondensingTurbine -> SteamTurbine crossing is a javadoc {@link SteamTurbineDesign}, not
  code; strip comments before the scan. EbullatedBed <-> TrickleBedReactorParts: check lane G's final files.
- One kit rest piece for the four rests(...) copies (H, F x2, cabin heater; C, E may add more): small deck on
  posts or brackets, inner rail with the ladder opening, lower ladder walked through, upper ladder above.
- towerLength(stops) for stairTower (lanes A, B). post base plate (H). fill hidden face (B). bracket knee depth (F).

## Lane C: FCC converter (done, lane gate 52/52)
- Probe over 312 shapes (sizes, both riser lengths, every support): walkedAndClimbed passes; longest climb 9.000 m;
  rest decks and ladders keep >= 0.19 m from the table columns.
- FccConverterBuilder 833 -> 624: stairTower(beams) with openings cut into floor rails; own deck() gone (coplanar
  edge face and toe boards gone); squareDeck + fill (16 sides) on stripper/manway/top floors; standpipe floors
  platform(RAILED|WALL_EDGE) + rails; ringAccess (Access.clearance inside); rests() one method; head bands, bend,
  elbow, Solid.rod headLo, beam/post/line/pipe/end/radialSide(+1); casing(COLUMN) replaces FIREPROOF; table and
  reducer kept. Layout +7/-4: PipeSizes.od; xe moved to meet the tower side on cell centres (<= 1 m). Design 1 line.
- C1 rests: 4 m (s8) one rest at 5 m under the shell on 3 posts (+445); 10.5 m (s9) 6 m posts + 16 m on shell
  (+514); 17 m (s11) 7 m posts + 20 m (+369). Ladders alternate two angles 1.2 m apart at the rung plane; ground
  ladder and first rest between the -x columns (185-197 deg); manways moved to 225 deg.
- Counts: nozzles 8, moving parts 0 unchanged; quads +3 to +5 % (17 x 44: 65,628 -> 68,023); box +1 x at 10.5 and 17 m.
- Visible: kit flights/landings/posts/beams; ladders offset at rests; frame up to 1 m deeper in x; reactor floors
  decked all round (a 0.45-1.45 m strip on -x where the base had a railed slot, sizes >= ~10 m); standpipe floors
  lose the +x quadrant fill round the riser; column fireproofing 0.45 m (kit casing; was 0.5, z-fought the tower
  post); elbows replace balls on feed and steam lines.
- Kit gaps: fill per quadrant (fill(..., from, to)); squareDeck opening open to one face with side rails;
  pipe(solid) extends a leg's solid by r at its lower coordinate whatever the leg's direction (feed drawn solid=false
  + own rods); ringAccess has no rest below a vessel (posts under brackets in rests()); stairTower inner corner
  posts 0.025 m inside the deck edge clip its rails (also lane A).
- Owner choices: landing beams (400-660 quads); posts under rests below the shell (heater rests are brackets only);
  fully decked floors vs railed slot; 1.2 m ladder offset vs base 30 deg; frame face out (+1 x) vs longer decks.

## Lane D: fixed-bed gasifier, grate boiler, House deleted (done, lane gate 52/52)
- Net -151 lines: FixedBedGasifierHouse deleted (-149); gasifier +3 (tower + floor(): squareDeck RAILED + 8 beams,
  opened at tower.openings(); motor PLAIN on its diagonal; 4 hoops; end nozzles); boiler -5 (local box gone, 55
  u.box calls; posts; stack(CLADDING, Breeching) top min(60, yR + 10); conveyor PLAIN, trough and fan FULL; radial
  and end nozzles). All 711 shapes dumped old vs new: every nozzle's base, axis, length, ODs and exit match.
- Visible: kit tower (one flight per stop, 4 posts, landing beams, ~0.6 m narrower, floors open at alternating ends);
  no brackets under house floors, aisle, drum deck; gasifier floor west edge 5 cm out, west X braces 0.1 m inside
  the column line over the longer stretch beside the landing (3.3-9 m; was 3.9 m to the back), near landing 0.4 m
  behind the front column; boiler tower on the nearest cell centre clear of the furnace columns (up to 1 m off the
  wall, 0.8 m on non-bi) with a bridge per stop; tower rises to the front at supports 3-4, back at 5-6; drum deck
  front overhang 0-2.9 m (was 1.2), rare back overhang <= 1.1 m, edge beam, cross beams, knee braces; stack bands
  0.12 m cladding (were 0.10/0.15 shell).
- Counts: moving parts and nozzles unchanged; quads -10 to -20 % (FBG 5.0 55,234 -> 44,872); FBG boxes and 192 of
  672 boiler shapes (bi-drum, wall x >= 2.925) one cell narrower in x; boiler interior cells +0 to +9.
- Kit gaps: radialSide only for standing bodies (bi-drum keeps drumSide: needs a radial nozzle on any axis with a
  face plane); towerPlan(stops) (end + station per stop; boiler recomputes from TOWER_RISE/STAIR_RISE/STAIR_GOING);
  tower join to a set-back deck (bridge from platform + beam); gasifier house still on the 4-argument brace.
- Owner choices: (a) boiler drum deck front overhang up to 2.9 m at supports 3-4 (keep full width or a corner
  landing); (b) shift non-bi furnace +0.2 m so the tower meets the wall without a bridge (moves nozzle snapping);
  (c) gasifier west braces end mid-face with no column, as before (add a mid-column or drop that bracing).

## Lane G: trickle-bed, ebullated-bed reactors (done, lane gate 52/52)
- walkedAndClimbed on 464 trickle + 907 ebullated shapes: 0 failures. Net 1250 -> 1018 lines.
- TrickleBedReactorParts deleted (-524): outward/tangential/around, exact solids (rod, ring sectors, span), kit
  pedestal/pedestalCorner/skirt/baseRing/studs/heavyJoint/davit, head + Solid.rod headLo, elbow (6 segments),
  climbs + ringAccess + verticalLadder, squareDeck + platform + railing + beam, hoops; MIN_DECK/MAX_CLIMB from
  Access; PipeSizes.od(x, .1, max) same values. Trickle 274 -> 424 (took topNozzle, valve, thermowell, dumpNozzle,
  topDeck; quench header a Mesh.bend ring). Ebullated 452 -> 594, imports no design: pipe, elbow, posts, Solid.rod;
  catalyst withdrawal stays line + rods + ring sector (radial leg at 20-26 deg).
- Visible: top deck rail posts and brackets at other stations, girder tops hidden; mitred elbows show a wedge gap
  where a straight pipe meets them (KIT DEFECT: Mesh.bend end pieces square to their chord, not the pipe; affects
  every kit elbow incl. pipe()); a flashing band a fitting would cross is left out; decks under the bottom tangent
  laid round the skirt (wider by the insulation, outer edge in line), ladders stand off the skirt, no deck on the
  tangent (moves 1 m); ebullated first platform 1 m above the tangent in bays <= 8 m, a skirt rest in taller bays;
  trickle dump nozzle back right -> front right (it ran through a part ring in 29/464 and its walking room in 76).
- Counts: box, interior, moving parts, nozzles identical; trickle quads -44 to -469; ebullated -3 % to +4 % (4.5 x 18:
  one extra rest). Layout 11.3 -> 8.8 ms trickle, 22.3 -> 18.8 ms ebullated (indicative, lanes overlapping).
- Bracket check: before, 443 trickle + 976 ebullated deck pieces under/on the tangent had brackets 0.10-0.19 m off
  the skirt, in air; after 0. Still open: ebullated bays >= 9 m: rest platforms over the front skirt opening in
  828/907 (756 before), 4 of 8 default sizes (D >= 4.5); brackets over the opening bear on nothing, as before.
- Kit gaps: topNozzle, valve, thermowell, topDeck, the off-the-tangent loop and constants now duplicated (~90 lines
  each design; cause: squareDeck has no hole for a ladder coming up through it); Mesh.bend seam; pipe axis-aligned
  legs only; hoop not cut round a fitting; climbs cannot keep a level clear; Access.deck brackets at every post;
  ringAccess one wall radius for all rings (called twice).
- Owner choices: A ebullated bays >= 9 m: (a) deck spans the opening between jamb brackets, or (b) rests on the
  solid skirt sides, both ladders right; B helpers into the kit + ladder hole in squareDeck; C fix the bend seam
  once, top nozzle 6 elbow segments or the kit pipe's 2-3; D drop bands at fittings or a hoop cut round them.

## Lane E: fluidized-bed boiler and gasifier (done, lane gate 52/52)
- Offline sweep of 332 shapes: walkedAndClimbed (longest climb 9.125 m), sealing, ports, pipesApart all pass.
- FluidizedBedParts +39/-116: local box, trim, band, hoop, motor, stack gone; hoops; drive() kit motor PLAIN + shaft
  (sign parameter gone); fan() kit motor FINNED, each fan part's sides from its own diameter; closedBox and the
  cyclone, drum, drive, fan assemblies stay. Boiler layout +136/-152: stairTower (beams off; every stop opens next
  to the furnace deck) + a short bridge over the column line; box(faces, solid); posts; end nozzles (7 planes same);
  stack on a foundation box; hidden ground/duct faces dropped. Gasifier layout +99/-95: Solid.rod headLo; ringAccess
  with alternating ladder angles and Access.clearance; posts; 9 nozzles end/radialSide; top half rings now closed at
  the far end (were closed beside the ladder, open at the far end).
- C1 rests in one GasifierLayout.rests(base, levels) via Access.climbs: vessel rest arcs (taller sizes), stack rests
  from id 5.75, feed floor above 9 m (support >= 9): 2.4 x 1.45 m rest deck on 4 posts, two offset ladders. Boiler:
  stairs only, no rests. Gasifier 6.0 +2.2k quads at min support (rest arcs); s16 peak 63.7k.
- Counts: moving parts (boiler 6/9/9/12/12, gasifier 6/7/7) and nozzles (7, 9) unchanged; boiler quads -5 to +1 %,
  worst 26 x 3 s8 79,168 (base 78,530); boiler 3 x 1 and 26 x 3 one cell narrower in x; gasifier +0.3 to +3 %.
- Visible: kit tower (1 m lanes, no beams); stack bands every 8 m; gasifier rest arcs, top half ring front-right
  (ladder parity); feed-floor rest deck on posts at s16.
- Kit gaps: stack side count fixed (60 sides at 6 m OD = 7.3k quads vs old 3.2k) -> boiler stack capped at 5.5 m
  OD (survey 6 m); ringAccess drops pieces under 0.8 m even with a ladder on one (broke the stack climb at id 1.5;
  arcs lengthened to >= .85/(wall+GAP)); end() has no depth / no top-of-lying-drum nozzle (end + sagitta); no flat-face
  rest piece (built in access()); Access.climbs takes whole metres (rests() rounds up).
- Owner choices: stack OD cap 5.5 m vs a side-count cap in the kit; tower beams off (+470 quads, 26 x 3 s8 -> ~79.6k);
  gasifier decks stay 1.0 m wide (MIN_DECK 1.45 at a ladder start would need GAP and xm changed: at id 1.5 the stack
  top deck is 0.6 m over the support ring); feed-floor rest on posts vs hung off the floor frame.
