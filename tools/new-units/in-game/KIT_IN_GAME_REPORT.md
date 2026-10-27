# Kit consolidation: in-game check (2026-10-18)

Uncommitted kit work on 4dc78964. Lane: `runMcpClient` (vanilla, bridge 0.4.7-osguard), `MSYS_NO_PATHCONV=1`, a fresh superflat
world ("New World (30)") from `new-world.sh`, all input through `bridge.js`. Scenes: `scene-a.txt` + `scene-a-cyclone.txt` (the
1.1 m cyclone at s3 is refused, "support must be 4 to 8 m", as in the first check), `scene-b.txt`, `scene-constructor.txt`,
`k-scene-c.txt` (cyclone 2.0 x 1 s8, stirred 4.0 x 8 s3, adsorber 4.6 x 1 s3, FBG 3.75 s16, cabin 12 x 15 s3 and s12, FCC 17 x 44
s11, FBB 26 x 3 s8) and `k-scene-c2.txt` (cyclone rows 2.0 x 2, 1.2 x 4, 2.0 x 4 at s8, for item 5). All 24 placements succeeded;
the world boxes are in `k-boxes.txt`.

## Items
1. Stair towers: PASS. Compressor (`k-i1-comp-tower`, `-landing`, `-top-arrival`, `-mid-landing`, near 662 -55 -3), condensing
   turbine (`k-i1-cond-*`, 695 -56 -2), coker, centred on the structure's back (`k-i1-coker-*`, 650 -35 78), grate (`k-i1-grate-*`,
   745 -48 63), FCC 10.5 and 17 (`k-i1-fcc-*`, `k-i1-fcc17-*`), FBB 15 and 26 (`k-i1-fbb-*`, `k-i1-fbb26-*`), fixed bed
   (`k-i1-fbdb-*`, 775 -50 69). On each: 0.9 m flights, landings on beams, corner posts flush with the deck, continuous rails, an
   opening where the landing meets the deck, end rails at landing ends, and no floating posts.
2. Rests: PASS. Cyclone 2.0 s8 (`k-i2-cyclone-rest-*`, 597 -51 139.5): a deck on posts to grade with inner and outer rails, the
   lower cage walked through, the upper cage above. Stirred 4.0 x 8 s3 (`k-i2-stirred-rest-*`, 626 -53 140.5). Adsorber 4.6 x 1 s3
   (`k-i2-adsorber-*`, 637.5 -53 144): the ladder foot is clear of the back header end. FBG 3.75 s16 (`k-i2-fbg-*`): a feed-floor
   rest on posts with offset ladders and the vessel rest arcs; the cage is clear of the bottom screw pipe (659 -46.5 141). FCC 17
   (`k-i2-fcc17-*`): a rest on posts under the shell and a rest on the shell. Cabin 12 x 15 s3 (`k-i2-cabin3-*`, x 681, z 133-147):
   two intermediate side walkways on brackets, side-step ladders at alternate ends. Cabin 12 x 15 s12 (`k-i2-cabin12-*`, 710 -54
   147): a rest on posts beside the grade climb, the upper cage outside the column bracing.
3. Elbows: PASS. No light seam band on the FCC 10.5 feed and steam bends under the injector floor (`k-i3-fcc-*`, 605.5 -57.5,
   z 66-79), the cyclone outlets (`k-i3-cyclone20-outlet*`, `k-i3-cyclone11-outlet`) or the condensing turbine's pipes (`k-i3-cond-*`).
4. PASS. FBB 26 x 3 s8: the 6 m stack is round (48 sides) with its bands, foundation and breeching fans (`k-i4-fbb26-*`, near 703 -56
   228), as in `fluidized_bed_boiler-26.0-iso3`. Trickle 3 and ebullated 4.5 (`k-i4-trickle-*`, `k-i4-ebullated-*`): decks go round
   the skirt, bracket ends meet the shell, rails are closed. In `k-trickle-close` and `k-ebullated-close`, only the rail posts and the
   neighbouring units changed.
5. PASS, no intersection. 2.0 m s8 single: the cage is on the -x frame face (x of about 599-600), the duct and transition on +x
   (x 601.9-602.9, z 137.5-141.5), 2-3 m away. They line up only in oblique views (`k-i5-cyclone-iso`, as in `cyclone-2.0-iso`);
   `k-i5-cyclone-plan`, `-front-close`, `-west` and `-below` show the gap. Rows 2.0 x 2, 2.0 x 4, 1.2 x 4: the inlet manifold lies more
   than 2 m in front of the cage and ends short of its line (`k-i5-cyc20x2-*`, `k-i5-cyc-multi-front`). Below 1.2 m, no ladder.

## Other findings
- Nothing differs from `build/tmp/units/int/render/` or the integration report; no missing or z-fighting faces, gaps, floating supports.
- Reshoot pixel changes (`Diff.java`; `k-diff.txt` in the log archive): 4.1k-74k px. The largest come from changes the lanes listed:
  `centrifugal-landing` 74k (tower beside the walkway; box 10 x 17 to 11 x 12, origin z -8 to -6), `fbdb-washcooler` 62k (box one
  cell narrower, origin x 773 to 774), `fbg-close` 60k, `grate-close` 58k (kit towers, bridges). Motion views also change by phase.
- Placed cells against the first check: compressor 457/494, condensing 610/655, ebullated 1619/1627, FCC 10.5 7235/7306, coker
  15287/15157, FBB 15 19261/19149, FBG 9 3102/3179, grate 4781/4727, fixed bed 3352/2973. The others are equal.

## Client log
No ERROR, no exception, no `createcheme` warning. The WARN lines are vanilla or dev-only (goat-horn sounds, assets URL schema, mixin
`ScheduledForRemoval`). I stopped the client by its process (`mcpClientRunProgramArgs`), so Gradle ended with exit -1, as expected.
No client or lane process is left. No Gradle test task ran, no source file changed. Logs: `logs/new-units-kit-check-2026-10-18.tar.gz`.

## Shots (`shots/k-*.png`, 186; views `k-view-*.txt`)
- 40 reshoots `k-<old name>`: row-a-1/2, row-b-1/2, the 14 unit views, 13 `-close` views, centrifugal-landing, condensing-deck,
  fcc-regen-bottom, fbdb-washcooler, trickle-back, fbb-feeders, m-cfb/m-coker/m-grate.
- `k-c-*` scene C overviews (25), `k-plan-*` (3), close views `k-i1-*` (25), `k-i2-*` (56), `k-i3-*` (11), `k-i4-*` (13), `k-i5-*` (13).
