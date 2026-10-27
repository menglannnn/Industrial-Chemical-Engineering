# Integration items from lane reports (2026-10-18)

## Done during the lanes
- Lang: `vessel.role.steam` "Stripping steam" -> "Steam" (role generic now; turbines lane).
- Determinism FAILs from in-progress files told to their lanes: coker (DelayedCokerDrums Math.cos), fcc (FccConverterLayout Math.asin).

## Shared changes to make at integration
- UnitDesign `defaultLength(size)` (vessels: adsorber's default 2 beds; fcc default riser 32): constructor uses it when the
  kind changes (choose()) instead of carrying the length over.
- Hoist `StirredReactorDesign.fill` (grating between a square deck opening and a round body) and `od` into the kit
  (Access/ProcessUnit) if other lanes need it; AdsorberDesign depends on StirredReactorDesign now.
- Stair-tower helper (turbines built switchbacks by hand; see others).
- Nozzle-face map entries (strict at the end):
  - STEAM_TURBINE: STEAM 0+, EXHAUST 1+
  - CONDENSING_TURBINE: STEAM 0+, COOLANT_INLET 1-, COOLANT_OUTLET 2-, CONDENSATE 2+, VENT 0-
  - STIRRED_REACTOR: VAPOUR_OUTLET 1+, FEED 1+, NITROGEN 1+, VENT 1+, LIQUID_OUTLET 1-, COOLANT_INLET 2-, COOLANT_OUTLET 2+
  - ADSORBER: FEED 2-, VENT 2-, PRODUCT 2-, NITROGEN 2-, REGENERATION_OUTLET 2+, DRAIN 2+, REGENERATION_INLET 2+

- KIT.md corrected: box x/z start anywhere (only y = grade 0); prism = side only, cylinder = capped. Rotating lane dropped
  its draw-twice shift (one pass, counts identical, layout <= 52 ms).
- Kit gaps (rotating): `transition()` vertical only (own horizontal loft), `brace()` fixed 0.15 m bar (own scaled bars),
  no axial nozzle on a lying drum's head. (hydro): Solid head bulges up only (bottom head as stacked frustums);
  `access(..., via)` option in TrickleBedReactorParts. Gate idea: MAX_CLIMB per ladder for units (hydro: raised skirts
  can exceed 9 m first climb).
- Face map:
  - CENTRIFUGAL_COMPRESSOR: SUCTION 1-, DISCHARGE 0-, COOLANT_INLET 2-, COOLANT_OUTLET 2+
  - CYCLONE: FEED 2-, VAPOUR_OUTLET 2+, SOLIDS_OUTLET 1-
  - TRICKLE_BED_REACTOR: FEED 2-, HYDROGEN 0+, EFFLUENT 2-, QUENCH 0+
  - EBULLATED_BED_REACTOR: EFFLUENT 2-, FEED 2-, HYDROGEN 2+, CATALYST_INLET 1+, CATALYST_OUTLET 0+

## Owner choices (for the review)
- Turbines: condensing min deck +1 m at some Dc (CW inlet from the water-box bottom; alternative side inlet); vent from the
  shell's upper side; condensate pumps behind the rear water box; steam rises through the deck; terminal box + CW on +x;
  held line 5 Dc^2 MW, 7200/Dc rpm; plinth 0.31-0.55 m.
- Vessels: adsorber ~2 m taller than survey (2.1 m headroom under top headers; alt: headers at deck level with
  step-overs); adsorber skirt 2-4 m (survey 1.5-3), 1 bed allowed (survey 2-4); reactor < D 3.8 top-nozzle spools rise
  ~3 m over the walkway; manways and unloading nozzle are blind flanges, not ports.

## Counts (largest)
- STEAM_TURBINE 3.0x12 7,274; CONDENSING 3.0x12 15,155; STIRRED 4.0x8 9,590; ADSORBER 4.6x4 38,909.
- Rotating: compressor table max 7 m (two flights <= 3.5 m); switchback stair 3.7-5.7 m in front (alt: stair tower beside
  the left walkway); small casings' discharge dog-leg hidden in the slab (alt: own slab opening); single 0.3 m cyclone in
  a 1.3 m frame (posts clear the solids pipe block); cone ends on a 0.05-1.05 m spool so FEED lands on a cell centre;
  duty figures assume air + 2.5 g/cm3 dust and Elliott 32M4 scaling.
- Hydro: skirt openings 0.75-0.9 D (survey 0.6 D) so FEED/H2 pass; discharge riser offset larger than 0.3 D at small
  sizes; loop faces front (partly hidden from front-left iso); addition vessel on the top head, CATALYST_INLET up to
  0.36 m off its axis; oil skid left out, motor drop 1.2 m; single-bed quench riser feeds the top zone; top deck 1.2 m
  beyond cladding; davit > 3 m; first climb can exceed 9 m on raised skirts (MAX_CLIMB not a unit gate).
- Counts (largest): CENTRIFUGAL 1.5x9 11,095; CYCLONE 2.0x4 16,506; TRICKLE 5.0x4 29,664; EBULLATED 6.0x42 43,857.

## Coker (first report; asked to restore 2 pairs at every size per D3, trim detail to <= 80k at 10 x 2)
- Shared: `ProcessUnit.MOTION_PERIOD` 40 -> 2400 ticks (coker cycle 1/120 rev/s snaps every 2 s otherwise; 2400 is a
  multiple of 40 so existing speeds stay whole turns). Kit stair/handrail rails at width - 0.03 straddle cells when a
  flight or deck edge sits on a whole metre (~1.5x quads); `Access.deck` always draws brackets (coker drew its own
  grating floor registering decks). Derrick lattice collides at its legs only; hoses, ropes, moving parts none.
- Face map: DELAYED_COKER: FEED 2-, WATER 2+, STEAM 0-, VAPOUR_OUTLET 0-, QUENCH 1+, BLOWDOWN 0+, RELIEF 2+,
  CUTTING_WATER 0+, SOLIDS_OUTLET 1-
- Owner choices: one stair tower (brief: one per pair; +9-10k each); two intermediate decks at every size; drum facets
  about 1.25 m (24-28 sides); only drum 0's chute is the SOLIDS_OUTLET port (roles once per unit), the others end open.

## Fixed bed (done; own sweep 672 boiler + 39 gasifier shapes clean)
- Face map: FIXED_BED_GASIFIER: OXIDANT 2-, STEAM 2+, FEEDWATER 2-, NITROGEN 2-, VENT 1+, SOLIDS_INLET 1+,
  SOLIDS_OUTLET 1-, QUENCH 1+, LIQUID_OUTLET 1-, SYNGAS 0+. GRATE_BOILER: STEAM 1+, FEEDWATER 0+, BLOWDOWN 0+, DRAIN 1-,
  FUEL 2-, SOLIDS_INLET 1+, SOLIDS_OUTLET 1-.
- Kit gaps: `motor()` along +z only; `stack()` capped 30 m and HEATER material (own stack); `saddle()` on the ground only;
  no default length (grate length clamp(1.1 Wg, 3, 10)); everySize covers least length at middle sizes and least support
  only; FixedBedGasifierHouse (stair tower + beams) used by both kinds -> rename or hoist (stair towers hand-built in
  turbines, compressor, coker, fixed bed: candidate kit `stairTower`).
- Owner choices: grate support 3-6 m (survey 2-4; hoppers + trough need 3 m); bi-drum up to 6 m wide, back pass above;
  duty 85 % and 2.8 MJ/kg; ram feeders (grate inside, not drawn); ID fan only (FD fan left out, intake hood); gasifier no
  capacity line, lock piping left out.
- Counts: GRATE 12x10 37,499 (peak 38,664 at S6); FBDB 5.0 55,234 (peak 55,658 at S5).
- Coker rework done: 2 pairs at every drum size; 10x2 74.6k (78.1k at 16 m support), 7x2 61.8k, 6x2 55.7k, 4x1 27.7k.
  Trims: 24 drum sides, derrick stories 3 sides high (>= 4), one diagonal per face per story, flat bars, one
  intermediate deck (survey 2-3; owner choice), rails 0.05 m in from faces. Skirt access opening now shows on big drums.

## Fluid bed (done; own sweeps clean)
- Face map: FLUIDIZED_BED_BOILER: FEEDWATER 0+, STEAM 0+, BLOWDOWN 0+, DRAIN 0-, FUEL 2-, SOLIDS_INLET 1+, SOLIDS_OUTLET 1-.
  FLUIDIZED_BED_GASIFIER: OXIDANT 2-, SYNGAS 2+, FEEDWATER 0+, BLOWDOWN 0-, STEAM 1+, NITROGEN 1+, VENT 1+,
  SOLIDS_INLET 1+, SOLIDS_OUTLET 1-.
- Kit gaps: slowest motion 0.5 rev/s (MOTION_PERIOD 40 ticks) -> after 2400 ticks set cooler drums ~3 rpm and lock
  valves slow; own motor (u.motor fins too costly); verticalLadder fixed at (a, a) (own arc ladders); Mesh.ring always
  bored (own hoop()); Solid.rod head only at the high end; validate message says "whole metres" for counts.
- Owner choices: W26 76-79k quads (kept per D3, under 80k); support = grid height 5-8 m; cyclone counts limited so Dc
  never clamps; one square downcomer; fans behind the air heater; back-pass decks via corridor; decks at feeder floor,
  every 4th landing and top; one chute per 5 m width; gasifier support = bottom tangent (ash system min, max 16); two
  cyclones .7 ID/sqrt2 above 4.5 m; gasifier arc ladders, not a stair tower.
- Counts: BOILER 26x3 76,849 (78,523 at grid 8); GASIFIER 6.0 59,175 (61,471 at 16 m).

## FCC (done; own probe 58 shapes clean)
- Face map: FCC_CONVERTER: FEED 2-, STEAM 2+, AIR 2-, FUEL 2-, FLUE_GAS 0-, VAPOUR_OUTLET 0+, CATALYST_INLET 2-,
  CATALYST_OUTLET 2+.
- Ranges: riser min grows 25 m (D 4) to 44 m (D 17) so spent catalyst falls at <= 45 deg; bottom tangent min 8-11 m.
- Kit gaps: no tan in DeterministicMath (checked?); Mesh.head fixed band count (own coarse head); tori 2x quads (own
  segmented arc); no low-end head volume (per-row discs); no bracket-less deck (own deck(); coker did the same).
- Owner choices: 66.5k at 16.5x44 (alt cap 15.5 m at 60.9k, or a ladder platform for the regen valve -3k); 40-sided
  regenerator at 17 m (survey 48-64, +2k); partial ring platforms (~220 deg) not joined to the frame; table of four
  columns, no ring beam; HPU cabinet, top davit, girth stiffeners not drawn; 21 flights at 17 m, 26 m between stripper
  and reactor manway floors with no intermediate floor; regen valve floor 2.3 m over the injector floor from 15.5 m.
