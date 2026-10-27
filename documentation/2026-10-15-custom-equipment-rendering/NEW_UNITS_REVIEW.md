# New units: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d`, working tree on 41ef764f · 2026-10-18 · owner request: eleven units ("Refine the definition before starting. Use sub-agents to work in parallel"), decisions D1–D4. Plan: `NEW_UNITS_PLAN.md`; surveys `NEW_UNITS_SURVEY_*.md`.

## Expectation
- Each unit is drawn as the real one stands in a plant, at its real size range, with the structure it stands on or in (D4): a turbine on its concrete table over its condenser, a coker's drums in a structure under two derricks, an FCC's riser and regenerator on columns, boilers on their steel. Halls, cranes, racks and separate auxiliaries stay out.
- Size beats detail (D3): the largest kinds (FCC 17 m regenerator, coker 10 m drums × 2 pairs, CFB 26 m furnace) must still fit the 80k-quad ceiling, so detail is trimmed first.
- Only what really moves in sight moves, slowly enough to read: agitator drive, fans, airlocks, cooler drums, feeders, rams, the coker's drill stem.

## What changed
- 14 kinds (D1: four solid-fuel kinds; D2: two turbine kinds), each a `UnitDesign` file plus helpers named after it (about 10.3k lines from eight parallel opus lanes on disjoint files, gated without Gradle by `tools/new-units/check.sh`).
- Shared: 26 nozzle roles; `UnitDesign.defaultLength` (a new kind starts at its typical second size: 2 adsorber beds, a 32 m riser); `ProcessUnit.stair` and the `Stair` record; `MOTION_PERIOD` 40 → 2400 ticks (slow speeds keep whole turns); the constructor's choice page in five categories.
- Fixed on the way: the moving-part cut chose cells with `floor(min)..floor(max)`, so a compressor rod's end disc lying on a block face (1.1 × 2, x = 10) fell out of the moving rod; it now uses the chunk mesh's own `cells(facet)`. The validate message says "count" for counts.
- Gates: decks, ladders, stairs and moving parts are checked on every size of every kind; the nozzle-face map is strict for all 22 unit kinds; defaults lie in range.

## Measured
- Equipment tests with the determinism source gate (Gradle): 58/58 on the final code (`ProcessUnitTest` 9, `ShapeRasterizerTest` 25, `ColumnStructureTest` 7, `LineGeometryTest` 7, `PaintTest` 4, `VesselPlacementTest` 3, `UnitTextTest` 1, `DeterministicMathSourceTest` 2); `UnitTextTest` failed on the texts before the fix, and `railedAtClosedEnds` (in the deck gate) on the condensing turbine with the end-rail fix taken out.
- Largest chunk quads (`UnitCounts`): stirred 9.6k, adsorber 38.9k, trickle 29.7k, ebullated 43.9k, centrifugal 11.1k, back-pressure turbine 7.3k, condensing 15.4k (with its table rails), cyclone 16.5k, FCC 65.6k at 17 m (end rails +0.2k), coker 74.6k (78.1k at 16 m support), CFB 76.8k (78.5k), HTW 59.2k (61.5k), grate 37.5k, FBDB 55.2k. Layout at most 79 ms.
- Lang: every key the new kinds' rows, held lines, datasheets and roles ask for exists with its `%s` count (`LangProbe`).

## In game
- Bridge lane, fresh world (`tools/new-units/in-game/`): all 14 kinds placed and drew with no missing textures, z-fighting or floating parts (the 1.1 m cyclone needs 4 m support: the scene's 3 was refused with the range message). Motion is on by default: the coker's crosshead slides on its stem with the hose joined, the CFB ash-cooler drum turns in its housing, the grate rams slide in their cylinders (three captures 5 s apart). Five category pages, design pages, and a stirred reactor built from the constructor equal to the placed one.
- Found and fixed:
  1. Condensing turbine: the table's front and back edges, a drop of 3.5 m or more, had no rail. Handrails as on the centrifugal compressor's table.
  2. Constructor middle lines ran out of the 196-px panel: FCC (to 263 px), coker, ebullated and trickle bed, FBDB, grate, centrifugal, stirred, and two older kinds (kettle, large air-cooler banks). Shortened (the hints keep the detail); `UnitTextTest` now holds every unit's words to their places with the vanilla font's advances.
  3. Coker and FCC datasheets (18 lines; 19 on the gasifiers) ran off a 240-px-high GUI: `VesselScreen` closes its line pitch to fit (12 px down to the font's 9).
  4. Category buttons 62 → 64 px ("▶ Solid fuel" touched the edge); the CFB's kind line no longer repeats its title.
- Recheck on a fresh world (`r-*` shots): table rails, buttons, the six shortened lines (longest ends at 171 px) and the four datasheets (title, lines and buttons inside 854 × 480) pass. It found one more opening, fixed in the kit:
  5. The condensing turbine's -x walkway end, beside its stair landing, had its fence band but no rail over a 6 m drop: `ProcessUnit.rails` drew a closed end's rail only with a side-rail piece starting there, and the landing's opening began 0.05 m in. `Access.endRail` now draws it alone (the FCC's landings gained theirs too); gate `railedAtClosedEnds`. Also the air cooler's bay value fits between its buttons ("3.0×4") and its line reads "fans 2 × 1.9 m". These last changes are gated, not seen in game.
- Not checked in game: hover texts (hovering needs desktop input); `LangProbe` checks their keys.

## Owner propositions
1. Coker: one stair tower per unit and one intermediate deck (survey: one tower per pair, 2–3 decks; +9–10k quads per tower, over the ceiling at 10 m × 2). Keep?
2. FCC at 17 m costs 66.5k quads (regenerator 40 sides instead of 48–64). Keep the range, or cap at 15.5 m (60.9k)?
3. CFB at 26 m furnace width: 76.8–78.5k quads, under the 80k ceiling (D3). Keep?
4. Adsorber about 2 m taller than the survey, for 2.1 m headroom under its top headers (alternative: headers at deck level with step-overs); skirt 2–4 m. Keep?
5. Grate boiler support 3–6 m (survey 2–4): the hoppers and ash trough need 3 m. Keep?
6. Condensing turbine: cooling water enters at the water-box bottom, which lifts the least table by about 1 m at some exhaust sizes (alternative: side inlet). Keep?
7. Ebullating pump drawn still under the reactor, the recycle loop to the front. Animate its shaft too?
8. Units with several chutes (coker, CFB) give one SOLIDS_OUTLET (roles are unique per unit); the other chutes end open. Keep?

## Code guide and tools
- Code guide: the equipment chapter is updated at the merge (unit designs, stair, motion period, choice categories); no merge here.
- Tools: `tools/new-units/` (KIT, lane gate, counts, renders, lang merge, probes, both in-game checks) and its `logs/`; batch copies are shots 126–137.
- Follow-up: kit consolidation (stair tower, bracket-less deck, motor axis, stack cap, segmented arc, shared fill and house helpers, a ladder climb gate for units), proposed as its own session.
