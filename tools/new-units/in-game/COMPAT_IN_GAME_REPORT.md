# Layout refresh of old saves: in-game check (2026-10-18)

Setup: uncommitted refresh work on 1ca05c1c (already compiled). `runMcpClient` lane (bridge 0.4.7-osguard), `MSYS_NO_PATHCONV=1`, all input
through `bridge.js`, one Gradle run (exit 0). World: `run/saves/Equipment review 2026-10-18` copied unchanged (diff -rq identical) to
`run/mcp-client/saves/compat-old-2026-10-18`, loaded from the world list. The player loaded at (150, -28, 144), so all chunks of the scene were
in view distance 12. The row tour (`compat-tour.txt`, 10 s at each row centre) found nothing more. Afterwards the copy holds two fresh cyclones,
at 0 -60 225 and 110 -60 -30, and the check's gamerules.

## Refresh lines: 35, one per placement in review-scene.txt (4 + 8 + 8 + 6 + 9)
All 35 lines came 0.2-10 s after the load (21:25:40.18 to 21:25:50.24); blocked=0 on every line; no `drum node moved`; no refresh WARN.
- `equipment_refresh kind=cyclone origin=109, -60, 65 cleared=26 placed=24 blocked=0`
- `equipment_refresh kind=cyclone origin=-3, -60, 181 cleared=118 placed=115 blocked=0`
- The 33 other lines show cleared=0 placed=0. Row 1: drum -2,-60,-2 and 11,-60,-8; column 24,-60,-3 and 39,-60,-4. Row 2: exchanger, kettle,
  air_cooler, ejector, pump, compressor, vertical_heater, cabin_heater. Row 3: stirred_reactor, adsorber, trickle_bed, ebullated_bed,
  centrifugal_compressor, steam_turbine, condensing_turbine. Row 4: fcc 10.5, coker 7, fbb 15, fbg 3.75, grate_boiler, fixed_bed_gasifier.
  Row 5: stirred 4.0, adsorber 4.6, cabin_heater x2, fbg s16, fcc 17, fbb 26, coker 10. All lines are in `compat-log.txt`.

## Checks
1. Refresh lines: PASS. There are 35 of 35. Only the two cyclones changed cells. blocked=0 everywhere.
2. Cyclone match: PASS on blocks. Fresh `cyclone 2.0 1 8 north` at 0 -60 225 (303 cells at -3,-60,222) and `cyclone 1.1 1 3 north` at 110 -60 -30.
   `execute if blocks ... all` (`compat-compare.txt`) compares block states and block-entity NBT. Results: 2.0 m, region -6..6 / -60..-36 /
   179..193 against the fresh one at the same origin offset (dz 41): "Test passed, count: 4875". Masked: 303, which is the fresh cell count.
   1.1 m (dz -97): "Test passed, count: 1584". The geometry in the shots is the same: `compat-r20-*` against `compat-f20-*`,
   `compat-r11-*` against `compat-f11-*`, and the pairs `compat-pair-*`.
   Before the reload the 2.0 m cyclone showed black, unlit faces (Lighting below); after it, the views equal the fresh ones.
3. No strays: PASS. The "all" compare covers air; the old box and dog-leg lie in the region: no leftover shell or invisible interior cell.
4. Drum (row 1, vertical 3 x 6): PASS. `use_item` opens its FluidDeviceScreen. Overview: "EMPTY: holds nothing / STEADY". Datasheet:
   3.0 m, T/T 6.0 m, skirt 3 m, 49.5 m³. Connections: none. Shots: `compat-drum-gui`, `-datasheet`, `-connections`, and `compat-drums`
   (row 1 view).
5. Second load silent: PASS. After Save and Quit and a reload: 0 new `equipment_refresh` lines (revisited both cyclones, rows 5 and 3).
6. Log clean: PASS. No ERROR, no exception, all createcheme lines INFO. Only a harmless vanilla "version differences" WARN (iris, jei,
   fabric API parts MISSING: the world came from the runClient lane). The client quit through Quit Game; Gradle exited 0; no process left.

## Odd findings (for the owner)
- **The 2.0 m cyclone moved 1 block toward its front (north, -z).** Physics expectation: a vessel whose inlet duct is redrawn stays on its
  foundation; only the duct changes. What the code did: the refresh keeps the box origin (low corner). The new layout's box is one row
  shorter at the front: sizeZ was 8 and is now 7. So the body, frame, platform and ladder moved 1 block north; only the feed flange stayed
  on its cell. Evidence: `compat-pair-side-kit.png` (kit shot of the old layout and the refreshed one, same placement-relative camera).
  Also, the compare at the placement offset (dz 40) gives "Test failed", and the compare at the origin offset (dz 41) passes.
  No pipe was attached here; on a vessel with pipes on its other nozzles, those nozzles would move off their pipes. Proposition: anchor
  the refresh on the vessel axis or the command's centre point (origin + old size/2 - new size/2); this cyclone's body then stays put.
- **Lighting:** in the session that ran the refresh, the faces next to the cells it emptied rendered black (light 0) for at least 10 min.
  Shots: `compat-pair-inlet-se.png`, `compat-pair-side.png`, `compat-pair-iso.png`. After the reload they look normal:
  `compat-pair-inlet-se-reload.png`, `compat-pair-iso-reload.png`. The client probably did not get the recomputed light of those cells.
- **Load hitch:** the 35 refreshes used about 10 s of server time. One vanilla "Can't keep up! 2304ms or 46 ticks behind" came in that time.
  33 of the 35 refreshes changed nothing, but the large ones took 0.4-1.4 s each (fbb 26 and coker 10 about 1.36 s, fbb 15 0.85 s).
  This happens once per old save.

Files (`tools/new-units/in-game/`): shots `compat-*.png` (38), views `compat-view-*.txt`, `compat-shoot.sh`, commands `compat-setup.txt`,
`-quiet`, `-tour`, `-fresh`, `-compare`, `-aim-drum`, log excerpt `compat-log.txt`.
