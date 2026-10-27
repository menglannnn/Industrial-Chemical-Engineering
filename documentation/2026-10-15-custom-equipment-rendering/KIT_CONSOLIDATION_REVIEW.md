# Kit consolidation: review (2026-10-18)
Plan `KIT_CONSOLIDATION_PLAN.md`; branch `claude/custom-equipment-rendering-805e7d` on 4dc78964. Owner decisions: C1 (a rest every 9 m of caged climb) and E3 (small visible changes where a shared part replaces a hand-built one). Lane reports in `tools/new-units/CONSOLIDATION_LANES.md`; the integration in `INTEGRATION_REPORT.md`.
## Real-life expectation first
- **Ladders.** A caged ladder climbs at most 30 ft (9.14 m) to a landing, each section offset from the next. Before: the cyclone climbed 17.1 m; the cabin heater climbed up to 22 m (hearth walkway to eaves) and 10–16 m from grade to its floor (2,450 shapes).
- **Decks.** A platform hangs on the steel it bears on. Before: 443 trickle-bed and 976 ebullated-bed deck pieces had brackets 0.10–0.19 m off the skirt.
- **Cabin heater.** It carries a walkway at each row of peep doors (API 560 12.4.1), like the vertical heater's every 8 m.
- **Pipes.** A straight pipe meets its elbow flush. Before: the kit's mitred bend left a wedge gap.
## What the code does now
- **Migration.** Eight lanes moved the 14 kinds onto the kit (ProcessUnit, Access, Mesh, PipeSizes). One integration step then folded their copies into it: `towerPlan` + `stairTower(plan)` (7 copies); one flat-face rest `climb(...)` (cyclone, stirred reactor, adsorber, HTW feed floor, cabin heater); base-plated posts; valve, thermowell, reducer; a ladder hole in `squareDeck`; side counts for `pipe` and `stack`.
- **Kit fixes.** K1 bend ends square to the tangent; K2 pipe solids by leg direction; K3 tower posts flush with the deck; K4 `ringAccess` keeps a piece a ladder uses; K5 `fill` without its hidden face.
- **Files.** FixedBedGasifierHouse and TrickleBedReactorParts are deleted. The new HydroprocessingParts is owned by both reactors. The gate `designsShareOnlyTheKit` (comments not scanned) finds no code shared between two designs.
- **Lines.** Package 48 files and 18,959 lines → 47 and 18,933. Designs 10,357 → 9,147; kit 3,983 → 5,192 (rests, tower plan, fittings, the kit batch); ColumnStructure 496 → 471 (MAX_CLIMB moved, behaviour unchanged).
- **Access.** Every caged climb is at most 9.125 m (gated). Cabin heater walkways are at most 8 m apart; its grade climb rests on posts above a 9 m floor. Reactor deck brackets land on the skirt, none in the air.
## Gates and counts
- **Gradle, one run:** equipment.*, DeterministicMathSourceTest and UnitTextTest; 68 tests, 0 failures.
- **Sweep** (`tools/new-units/probes/Sweep.java`): every size, length and support of the 22 kinds, 19,638 shapes. Walk and climb, pipesApart, free pipe blocks and quads under 80k all pass. Worst: coker 10×2 at support 16, 79,240; CFB 26×3 at support 8, 78,949 (survey 6 m stack, 48 sides).
- **Quads against the base** (lowest support, min/mid/max rows summed): down for the fixed-bed gasifier −19.6 %, grate −11.9 %, adsorber −9.7 %, stirred −6.5 %, steam turbine −5.8 %, condensing −4.3 %, cyclone, ebullated and trickle −2 %; up for the FCC +4.5 %, centrifugal and HTW +3.0 %, cabin heater +1.8 %, coker +1.2 %; the rest within 0.5 %.
- **Moving parts and nozzles.** Counts are the base's in every row. Nozzle positions moved in two places: the coker's FEED, STEAM and VAPOUR_OUTLET, 1 m inward by the kit's `end` rule; the trickle-bed dump nozzle, from back right to front right (it ran through a ring platform in 29 of 464 shapes).
## In-game check
- **Setup.** The bridge lane (runMcpClient, bridge 0.4.7-osguard) on a fresh superflat world placed 24 units: scenes A and B, plus the tall rest shapes, cabin heaters at supports 3 and 12, FCC 17, CFB 26×3 and cyclone rows. Report: `tools/new-units/in-game/KIT_IN_GAME_REPORT.md`.
- **Results.** All items pass. Stair towers are complete on 7 kinds; the rests are complete on 6 kinds; elbows show no seam; the CFB stack has 48 sides; the reactor deck brackets reach the shell. The cyclone cage and inlet duct do not meet (2–3 m apart; they only seem to cross in oblique views). The client log shows no errors or mod warnings. The 40 reshoots differ only by changes the lanes listed.
## Visible changes (E3)
- **On the units.** Kit stair towers: 0.9 m flights, 1.2 m landings on beams, corner posts, openings in the deck rails; the coker's tower recentred. Rests with inner rails and offset ladders. No brackets where a deck sits on beams. Flush elbows, 8-sided small pipes, stack bands every 8 m. Cabin heater walkways.
- **Boxes against the base:** compressor one cell wider and 2–5 shorter; coker one cell shorter in z; FCC 10.5 and 17 one cell more in x; fixed-bed gasifier and 192 grate shapes one cell less in x; shapes with rests one or two cells more; cabin heater at supports 10–16 2–4 blocks more.
## Owner propositions (built as listed; reversible)
1. Tower landing beams stay (+400–660 quads). The CFB boiler's stay off, to keep it under 80k.
2. The compressor tower stands beside its widened right walkway. Alternatives: along the table front, or a wider table.
3. Small motors are PLAIN; FULL adds a turning fan to each. On 0.3 m cyclones the cowl is 1.8 times the body.
4. Rests: the lower ladder stands outside the rest deck, one or two cells more (alternative: an unrailed open end); the adsorber's rest stands on its own posts (alternative: on the vessel, which needs a platform opening); levels round up, so splits can be uneven (7.125 + 5.0 m); the FCC's rests below the shell and the HTW gasifier's stand on posts.
5. FCC: floors decked all round (alternative: the railed slot); ladders 1.2 m apart (alternative: 30°); frame face one cell out in x (alternative: longer decks).
6. Grate boiler: the drum deck's front overhang reaches 2.9 m (alternative: a corner landing); a furnace moved +0.2 m would drop the bridge. The fixed-bed gasifier's west braces end mid-face with no column.
7. Ebullated bays of 9 m and over: rests over the front skirt opening (828 of 907 shapes). Either (a) span between brackets at the opening's sides, or (b) put them on the solid sides.
8. Flashing bands are dropped at fittings (alternative: cut round them). The top-nozzle elbow has 6 segments.
9. HTW decks are 1.0 m wide at a ladder foot; MIN_DECK would need GAP and xm changed. Its stack has 48 sides where the pipe rule gives 60.
10. Kit: short forms kept as defaults (about 500 calls). Base plates have a 0.06 m margin everywhere (was 0.07–0.09). The coker jacket fill has 8 sides; one X brace per storey.
## Tools, logs, docs
- `tools/new-units/`: the lane report, the integration brief and report, KIT_API, RenderAll, check.sh, `probes/` Sweep, Dump and sweep.sh, the in-game `k-*` views and shots, `logs/kit-consolidation-2026-10-18.tar.gz`. The code guide is refreshed at the merge.
