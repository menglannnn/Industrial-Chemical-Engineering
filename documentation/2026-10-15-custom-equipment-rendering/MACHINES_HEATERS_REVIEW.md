# Centrifugal pump, reciprocating compressor, fired heaters: review

Batch `2026-10-15-custom-equipment-rendering` · branch `claude/custom-equipment-rendering-805e7d` · 2026-10-17 · plan `MACHINES_HEATERS_PLAN.md`, survey `MACHINES_HEATERS_SURVEY.md`. Committed on the branch as 974e64b2 (code) and a118d219 (changelog), not merged or pushed.

## Physical expectation (plan and survey)

- Duties as in the plan: pump 21–1,360 m³/h for discharge 0.05–0.4 m; compressor 4 × 0.4 m bore about 8,000 m³/h; VC 4 × 8 m about 6 MW, cabin 12 × 40 m about 95 MW.
- Owner on the first draft: "gross shape looks ok, but needs significant improvement". The survey's signature parts: an OH2 pump's eccentric volute, centreline feet (none under the bearing housing), Class 300 flanges, seal pot and yellow coupling hood; per API 618 throw a crosshead guide 3–4 strokes long, a windowed distance piece, valve caps above and below, bottles 0.2–0.5 m clear; an API 560 heater's floor 2.1 m or more up so an operator works under the burners, platforms at every peep-door row and the convection section, 1.07 m rails, caged ladders with offset flights.

## What was built

- **Pump:** volute from 0.55 to 0.88 impeller diameters, cover joint ring with studs, Class 300 flanges, cast suction bell to the block face, discharge throat to its flange; feet on pedestals at the casing only; bearing bracket with windows, oil sump, fins, oiler; seal pot on its post with its flush line; yellow hood (`GUARD`); finned TEFC motor with cowl and terminal box; baseplate on a chamfered plinth (`CONCRETE`).
- **Compressor:** per cylinder a crosshead guide (0.7 m at least), a distance piece with windows and covers, valve caps, head and outboard support; bottles 0.2 + 0.15 b clear; frame covers; a synchronous box motor (0.9 + 1.1 b + 0.1 per throw); concrete foundation.
- **VC heater:** legs max(6, 2·round(πD/4.6)) (6 at 2.5 m, 14 at 10 m) braced in alternate bays; `HEATER` casing with bands; muffler burners on a 0.4 D circle with their fuel header; a hearth ring platform, one every 8 m on taller sections and a roof railing round the convection base; caged ladders on alternating sides, the roof one walked through; peep doors; panelled header boxes, soot blowers, insulated crossovers (`CLADDING`); stack with damper actuator, lever, bands and lightning rod.
- **Cabin heater:** columns about every 3 m, braced in the end bays and every third; full-length hearth walkways on both sides, a middle walkway on tall walls, convection walkways at the soot blowers, walk-through caged ladders; peep and access doors, panelled end header boxes, muffler burners, a stack per 15 m with its damper.
- **`Access` (new):** deck, handrail and caged ladder taken out of `ColumnStructure`, run along an `Arc` or a `Line`; columns keep their 0.25 m chords.
- **Collision:** arc decks are rings cut to an angle range (`Solid.ring`), rails 1.4 m fences; rung planes are pressed against to climb, the cage's inside is climbable; a walk-through ladder's rung plane stops at its landing so the player steps off between its rails.
- **Quad trims** (`MachineCounts`): valve-cap studs dropped (58 % of the compressor's quads), straight runs in one piece, VC arc chords from a 5 mm sagitta, soot blowers every 4 m along. Compressor 19.4k → 10.9k, cabin 51k → 34.8k, VC 39.7k → 34.1k; layout 135 ms or less a unit.

| Unit | Quads |
|---|---|
| Pump 0.05 / 0.2 / 0.4 m | 1,702 / 2,384 / 3,110 |
| Compressor 0.15 × 2 / 0.45 × 4 / 0.8 × 6 | 2,595 / 6,089 / 10,947 |
| VC 2.5 × 4 / 4 × 8 / 10 × 27 | 4,803 / 7,257 / 34,104 |
| Cabin 4 × 6 / 6 × 12 / 12 × 40 | 6,655 / 10,748 / 34,825 |

## In-game check (bridge lane, fresh superflat world, `machines-check/`)

- Pumps (`n-pumps`, `n-pump-close`, `n-pump-side`): eccentric volutes, stud rings, seal pots, finned motors, yellow hoods. Compressors (`n-compressors`, `n-compressor-close`): guides, windowed distance pieces, caps, box motor; small bores look spidery, as high-pressure cylinders do.
- VC heaters (`n-vc`, `n-vc-under`, `n-vc-hearth`): ring platforms, caged ladders, mufflers, bracing. Cabins (`n-cabins`, `n-cabin-front`, `n-cabin-top`): three walkway levels, soot blowers, damper. GUI (`n-g-*`): heading "Compressor", "About 30,401 m³/h at 420 rpm", "Fuel gas".

## Gates

- New `ProcessUnitTest.heatersCarryWalkableDecksAndClimbableLadders` on six heaters (VC and cabin, smallest to largest): every deck walked on with head room, every ladder starting on a deck or the ground, climbable in front of its rungs and not 1.5 m out, a walk-through ladder passed over its landing. Proof: with the rung plane run to the ladder's top in `inside()` it fails on VC 2.5 × 4. Equipment package 53/53.
- Full unit suite: 1,247 tests, 1 failure: `DeterministicMathSourceTest` found `Math.asin`, `pow` and `hypot` in the heater layout. Fixed with `DeterministicMath.asin` (new, beside `acos`), a plain square and `Math.sqrt`; the gate and the equipment package re-run: 61/61.

## Tools

- `tools/custom-equipment-rendering-poc/machines-check/`: scene, views, aims; shots `m-*` (first draft), `n-*` (this pass), `g-*` (GUI); `MachineCounts.java`, `MaterialCounts.java`. Logs: `logs/machines-check-2026-10-17.tar.gz`.
- Code guide: refresh at the merge (equipment chapter: four kinds, `Access`, ring solids, heater ladders, protocol `equipment-9`).

## Propositions for the owner

1. A compressor operating deck at frame-base level (survey: large machines); the discharge bottles and the free pipe blocks under the cylinders conflict with it, so decks between throws only.
2. A damper platform on each stack with a ladder from the roof (API 560 asks for one).
3. A cabin end walkway joining both convection walkways past the end header boxes (API 560: both ends of the convection section).
4. The datasheet's generic "Overall length" line for a VC heater could read "Radiant height".
