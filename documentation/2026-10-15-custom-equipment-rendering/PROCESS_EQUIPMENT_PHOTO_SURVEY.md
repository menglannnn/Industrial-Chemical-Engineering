# Process equipment photo survey: drums, columns, pipes, pipeline accessories

Batch `2026-10-15-custom-equipment-rendering` · 2026-10-16 · game-level survey for procedural models at 1/16 m texels (1 block = 1 m, **1 tx = 62.5 mm**).
Method: real photos viewed (Wikimedia Commons, vendor photos found through Bing image search), dimension tables from web references, and common practice. AI-generated stock images in the results (pressure-tank.com KO-drum illustration, an Alamy "refinery towers" render) were excluded. **[unc]** = not confirmed by a fetched source (practice or memory).

## 0. Pipe and flange key (ASME B36.10 OD, B16.5/B16.47-A Class 150 weld-neck flange, B16.9 elbow)

| NPS (DN) | Pipe OD mm | OD tx | Flange OD mm | Flange OD tx | Flange/pipe | Flange t mm | Bolts | LR elbow R mm |
|---|---|---|---|---|---|---|---|---|
| 1 (25) | 33.4 | 0.5 | 108 | 1.7 | 3.2 | 13-14 | 4 | 38 |
| 2 (50) | 60.3 | 1.0 | 152 | 2.4 | 2.5 | 16-18 | 4 | 76 |
| 3 (80) | 88.9 | 1.4 | 191 | 3.0 | 2.1 | 19-22 | 4 | 114 |
| 4 (100) | 114.3 | 1.8 | 229 | 3.7 | 2.0 | 22-24 | 8 | 152 |
| 6 (150) | 168.3 | 2.7 | 279 | 4.5 | 1.7 | 24-25 | 8 | 229 |
| 8 (200) | 219.1 | 3.5 | 343 | 5.5 | 1.6 | 27-28 | 8 | 305 |
| 10 (250) | 273.0 | 4.4 | 406 | 6.5 | 1.5 | 28-30 | 12 | 381 |
| 12 (300) | 323.8 | 5.2 | 483 | 7.7 | 1.5 | 30-32 | 12 | 457 |
| 14 (350) | 355.6 | 5.7 | 533 | 8.5 | 1.5 | 33-35 | 12 | 533 |
| 16 (400) | 406.4 | 6.5 | 597 | 9.6 | 1.5 | 35-37 | 16 | 610 |
| 18 (450) | 457.2 | 7.3 | 635 | 10.2 | 1.4 | 38-40 | 16 | 686 |
| 20 (500) | 508.0 | 8.1 | 699 | 11.2 | 1.4 | 41-43 | 20 | 762 |
| 24 (600) | 609.6 | 9.8 | 813 | 13.0 | 1.3 | 41-48 | 20 | 914 |
| 30 (750) | 762 | 12.2 | 984 | 15.7 | 1.3 | 75 | 28 | 1143 |
| 36 (900) | 914 | 14.6 | 1168 | 18.7 | 1.3 | ~90 [unc] | 32 [unc] | 1372 |
| 40 (1000) | 1016 | 16.3 | 1289 | 20.6 | 1.3 | ~95 [unc] | 36 [unc] | 1524 |

- Pipe OD for NPS >= 14 is exactly NPS x 25.4 mm. Flange OD ~ 2 d at 4", ~1.5 d at 12", ~1.3 d from 24" up.
- Thickness values differ by edition/source by ~10 % (B16.5-2017+ vs older tables); all <= 0.8 tx up to 24".
- A flange pair (2 flanges + gasket) is 30-100 mm thick, i.e. **1 tx** up to 24", 2-3 tx for 30-40"; it protrudes (Df-d)/2 = 0.6 tx (1"), 0.9 tx (4"), 1.3 tx (12"), 1.6 tx (24"), 2 tx (36") beyond the pipe. Weld-neck hubs add a 1-2 tx taper (hub length ~ 56 mm at 1", 76 at 4", 114 at 12", 152 at 24" [unc]).
- Bolts (1/2"-1 3/8") are sub-texel; at >= 12" the bolt pitch (~110-130 mm = 2 tx) can be a dotted texture ring on the flange rim. Blind flange = same OD and roughly same thickness.

## 1. Process drums

**Vertical knock-out drum on a skirt.** Visible parts: shell, 2:1 elliptical heads (depth 0.25 D), skirt of shell OD on a base ring with anchor chairs, top vapour outlet, side inlet, bottom outlet leaving through the skirt, level bridle, PSV, manway with davit, top platform + ladder, nameplate.
- Proportions: T/T height 2.5-5 D (KO drums), D 1-4 m; skirt 1.5-3 m tall (bottom outlet + pump NPSH) [unc].
- **Skirt access opening:** round, 18" or 24" (457/610 mm = 7-10 tx), one on small skirts, two opposite each other on large ones [unc]; framed by a sleeve (ring of plate or pipe, ~8-12 mm thick) projecting ~50-150 mm out from the skirt (1-2 tx rim) [unc]; centre ~0.6-1.0 m above the base ring so you step over the ring and chairs [unc]. A pipe opening with its own sleeve where the bottom outlet line exits. Openings > 25 % of skirt D need special design (eng-tips).
- **Skirt vents:** 2-4 holes of 2-4" (1-2 tx) near the top of the skirt just below the head weld, equally spaced [unc]. In hydrocarbon units the skirt is fireproofed (concrete/cementitious, beige-grey, 50 mm each side), so it reads thicker and matt; photo: geopolymertech skirt on an octagonal concrete pedestal ~0.25 m tall.
- **Nozzles:** neck (pipe) + weld-neck flange; outside projection shell-to-flange-face typically 150-200 mm for <= 4", 200-250 mm for 6-12", 250-300 mm for 14-24" (= 2.4-4.8 tx), plus insulation thickness when insulated (flange face ~75-100 mm clear of cladding; AutoPIPE default 75 mm; COMPRESS min 152 mm) [unc as a table]. Head nozzles stand parallel to the vessel axis. Reinforcing pad: ring OD ~2 d, thickness ~shell thickness (sub-texel: texture ring or skip; invisible when insulated).
- **Typical nozzle set:** top vapour outlet at head centre (0.1-0.25 D); side inlet 0.3-0.6 D below the top tangent, often same or larger size, with an internal deflector/inlet device (not visible outside); bottom outlet 2-6" at head centre turned 90° inside the skirt; vent 1-2" top, drain 2" bottom; PSV on top head (2-6" inlet, angle body, discharge elbow); instrument nozzles 1-2" (pressure, temperature).
- **Level bridle:** vertical 2-4" standpipe (1-2 tx), 0.3-0.6 m off the shell, joined to two to four 1-2" taps by short horizontal pipes with block valves; magnetic gauge strip (orange/white flags) on the side; transmitter heads (yellow/blue boxes) on top; spans ~0.5-1.5 D of the liquid zone (InstrumentationTools / Bing thumbnails).
- **Manway:** 18", 20" or 24" (Wermac: >= 24" for breathing apparatus); neck projection 200-300 mm; cover = blind flange OD 635/699/813 mm (CL150) with 16/20/20 bolts. **Davit:** 2-3" pipe arm (1-1.5 tx) on a vertical hinge post held by two lugs beside the neck; the arm reaches the cover centre; on top heads the post is vertical and the arm swings over the cover. Manway centre ~0.9-1.0 m above its platform [unc].
- **Nameplate:** ~150 x 100 mm plate on a stand-off bracket at eye level (skirt or lower shell): a 2x3 tx patch at most, optional.

**Horizontal drum / accumulator on saddles** (Inma Steel photo, installed reflux drum).
- Overall L/D ~3.7 in the photo; T/T 3-5 D typical; saddles at ~0.2 L from tangent lines (some codes D/4), 120-150° contact.
- Nozzles on the top: inlet near one end, vapour outlet and PSV, vent; liquid outlet at the bottom near the other end; manway on the end head (horizontal davit) or on top. Full-length top platform on shell brackets with handrail, caged ladder from grade. Grey/silver paint, black stencilled tag number on the side.
- **Water boot:** a small vertical cylinder under the shell near the outlet end, D ~0.25-0.4 D (>= 12"), height ~0.3-0.5 D [unc], with its own bottom outlet, drain and small bridle; one boot example 500 mm wide (scribd datasheet).

## 2. Distillation columns

Photos: Godorf (Cologne) column, Wesseling LyondellBasell columns (Commons), geopolymertech skirt.
- **Proportions:** H/D 7-30 (crude towers 5-9 m D, 40-60 m tall; small towers 1-2 m D, 20-40 m) [unc]. Tray spacing 0.6 m typical (0.45-0.9 m); manways at the top, bottom, feed and every <= 15 trays (Wermac) -> 6-9 m pitch, each with a platform.
- **Insulation (Godorf, measured on photo):** aluminium cladding, silver weathering to dull grey with brown streaks; panel grid ~0.3 D wide x 0.17 D tall (~1 m x 0.6 m); protruding support/rain-flashing rings every ~0.7 D (~3 m), sticking out ~0.03 D (1-2 tx); top head darker/bronze. Insulation 50-150 mm adds 1-2.5 tx to the radius. Stainless banding every 0.3-0.45 m is sub-texel: faint texture lines or skip.
- **Platforms:** part-ring segments (90-180° arcs) on alternating sides, width 0.9-1.2 m (14-19 tx), galvanised grating (texture), handrail 1.07-1.1 m with mid-rail ~0.5 m and toe board 100-150 mm, on knee-braced triangular brackets; a full ring at the top head tangent (Godorf). Handrail colour: galvanised grey or safety yellow.
- **Ladders:** rungs every 300 mm (1-tx rung every ~5 tx), clear width 400-450 mm, side rails 1 tx; cage ~0.7 m wide, starts 2.1-2.4 m above the base, extends 1.07 m above the landing; hoops <= 1.2 m apart, vertical bars <= 0.24 m apart (old OSHA / UW checklist). Unbroken climb <= 9 m (old OSHA 1910.27: platform every 30 ft); EN ISO 14122-4 flights <= 6 m [unc]. Ladder switches side at each platform. European plants also use helical stairs wound round the column (Godorf).
- **Shell manways:** as in section 1, cover often painted (orange in Godorf photo), davit beside it; insulated nozzles look like boxy/cylindrical bumps.
- **Nozzles:** feed, reflux (near top), side draws and pump-around returns (on the shell at tray levels), reboiler return (just above the bottom liquid level), bottom outlet through the skirt; sizes 0.05-0.3 D.
- **Swaged sections:** conical transition, half-angle 15-30° -> cone length 0.9-1.9 x the diameter change [unc]; Wesseling shows a narrower top section over a cone. Vacuum towers: wide flash/wash zone ~1.5-2 x the top D and a narrow bottom boot ~0.5 D [unc].
- **Overhead vapour line (Wesseling):** 0.25-0.4 D, rises from the top head centre in a gooseneck 0.5-1 D above the head, then runs down the shell at ~0.1 D stand-off.
- **Risers along the shell:** 0.05-0.15 D pipes, stand-off 0.3-0.6 m (insulation + clips), guided by brackets every ~3-6 m [unc].
- **Top davit:** mast 4-6 m above the top platform with a jib reaching 1-2 m beyond the shell, painted yellow (Wesseling) [height unc].
- **Skirt:** as for drums, 2-6 m tall (pump NPSH) and fireproofed in hydrocarbon units; anchor chairs 150-300 mm tall (3-5 tx), readable.

## 3. Process pipes

- **Flanges** occur only at equipment nozzles, valves, instruments, spool breaks and removable spools; rack runs are butt-welded for long distances (spools ~6-12 m between field welds) [unc]. A flange pair every N blocks is a stylisation; prefer flanges at fittings, equipment and every ~6-12 blocks.
- **Elbows:** LR 90° radius 1.5 x NPS (table 0); SR 1.0 x NPS. **Tees** (B16.9 equal): centre-to-end ~64 mm (2"), 105 (4"), 254 (12"), 432 (24") [unc], i.e. 0.7-1.0 d. **Reducers:** length 76 (2"), 102 (4"), 140 (6"), 152 (8"), 203 (12"), 356 (16"), 559 (24") mm; concentric, or eccentric (flat on top) at pump suctions.
- **Supports on racks:** rack bents every ~6 m, tiers 1.5-2.5 m apart [unc]; bare pipes sit on the beam; insulated pipes on welded shoes (T or twin plates) 100-150 mm tall, 300-450 mm long (2-3 x 5-7 tx) [unc]; guides (angles beside the shoe) and line stops; expansion loops on hot lines (Inma photo).
- **Insulation:** aluminium jacket; on small lines the jacket OD ~ flange OD, so flanges vanish or get removable flange boxes. **Colour:** whole-pipe paint per plant standard (grey/silver common) or bands per ASME A13.1 (yellow = flammable, green = water, blue = air, red = fire, orange = toxic) with flow arrows [unc].

## 4. Pipeline accessories

- **Centrifugal pump, API 610 OH2** (Amarinth photo): grey volute with **axial end suction** and **vertical top discharge**, centreline-mounted feet, coupling guard, motor (blue/grey/green, finned), fabricated steel baseplate with drain rim, grouted on a concrete plinth 150-300 mm tall. Sizes named suction x discharge (e.g. 3x2, 4x3, 6x4): suction nozzle one size larger than discharge. Suction line 1-2 sizes larger with an eccentric reducer flat on top; discharge line equal or larger via a concentric reducer, then check valve + block valve. Baseplate 1.2-3.5 m long, 0.5-1.2 m wide; shaft centreline 0.3-0.6 m above the baseplate; casing height ~2.5-4 x discharge OD [unc, estimated from photos].
- **Gas compressor (simplified):** centrifugal = horizontal casing + gearbox + motor/turbine in line on an elevated concrete table foundation, nozzles = line size (8-24"+), often pointing down; reciprocating = frame with horizontal cylinders, pulsation bottles above/below each cylinder ~2-3 x pipe OD, skid 4-8 m long [unc]. Game form: a skid box with casing, driver and two vertical flanged nozzles.
- **Globe control valve:** globe body with flanges, bonnet, yoke (open frame), diaphragm casing ("mushroom", blue/red/green/grey) ~300-550 mm across, positioner box on the yoke. Face-to-face ISA 75.08.01 CL150 RF: 184 (1"), 254 (2"), 298 (3"), 352 (4"), 451 (6"), 543 (8") mm [unc, memory]. Height above centreline ~2-3 x F-F for 1-2", ~1.5-2.5 x for 4-8" (thumbnails). Body usually 1-2 sizes below the line with concentric reducers, in a manifold: two block valves + bypass globe valve + drains.
- **Y-strainer** (Sure Flow YF150): F-F 219 (2"), 362 (4"), 473 (6"), 619 (8") mm; screen leg at 45° below the line, centreline-to-cap 124/232/340/371 mm (~2 d); flanged blow-off cover with a drain valve. **Basket strainer:** cylindrical pot with a bolted/quick-opening top cover, side inlet/outlet, pot height ~2-3 x F-F [unc]. Flanges match the line (same NPS and class).
- **Orientation:** pumps and compressors only shaft-horizontal on a foundation (vertical in-line pumps OH3/OH4 stand in horizontal lines with the motor up). Valves in any line orientation, actuator/stem not below horizontal (actuator up preferred; in vertical lines the actuator is horizontal). Y-strainers in horizontal lines, or vertical lines with downward flow (leg pointing down). Basket strainers horizontal lines only, cover up [unc].

## What reads at 1/16 m

Reads: pipe >= 2", flange pairs (1-tx collar), nozzle necks (2-5 tx), manway covers and davits, skirt openings, sleeves and vents, anchor chairs, platform decks, handrails and toe boards (1-tx bars), ladder rails/rungs/cage hoops (1 tx), level bridles, overhead line, risers, boot, pump/motor/guard blocks, actuator casings, strainer legs.
Texture only: grating, cladding seams and flashing rings, flange bolts (>= 12"), nameplate, colour bands, motor fins. Skip: bolts < 12", repads, banding, welds, small tubing, cable trays.

## Sources

- Wikimedia Commons: Godorf_Cologne_Rhineland-Refinery-06.jpg, Wesseling_Germany_LyondellBasell-01.jpg (https://commons.wikimedia.org/wiki/Category:Distillation_columns)
- Inma Steel horizontal drum photo, via https://www.bing.com/images/search?q=reflux+accumulator+drum+saddles+refinery+photo (inmasteel.com)
- Geopolymertech fireproofed skirt photo: https://www.geopolymertech.com/wp-content/uploads/2025/01/fireproofing-case-study-vessel-skirt-and-tower-683x1024.jpg
- Amarinth OH2 pump photo: https://www.amarinth.com/media/Amarinth-API-610-OH2-Pump-for-FPSO-Atlanta.jpg
- Bing thumbnails (manway davits, level bridles, control valves): https://www.bing.com/images/search?q=vertical+separator+vessel+skirt+manway+davit+refinery , ...?q=vessel+level+bridle+standpipe+magnetic+level+gauge+installed+drum , ...?q=globe+control+valve+diaphragm+actuator+installed+pipe+refinery+bypass
- Flanges: https://wermac.org/flanges/dimensions_weldneck_in_asme-b16-5.html , https://blog.projectmaterials.com/charts/flange-dimensions-class-150/ , https://blog.projectmaterials.com/flanges/asme-b16-47-flange-dimensions , https://www.wermac.org/flanges/dimensions_welding-neck_blind-flanges_class-150_asme-b16-47-a.html
- Fittings: https://blog.projectmaterials.com/pipe-valves-fittings-charts-pdf/fittings-b16-9/ , https://texasflange.com/butt-weld-fittings/elbow-long-radius/ ; pipe OD: https://blog.projectmaterials.com/pipes/pipe-dimensions-asme-36-10-19/
- Manholes and KO drums: https://wermac.org/equipment/manhole.html , https://www.wermac.org/equipment/flare_ko_drum.html
- Nozzle projection: https://docs.hexagonppm.com/r/en-US/CodeCalc-Help/25/313370 , https://communities.bentley.com/products/pipe_stress_analysis/w/pipe_stress_analysis__wiki/38994/q-how-to-set-the-nozzle-projection-from-the-insulation-surface-in-an-autopipe-vessel-model
- Skirt openings: https://www.eng-tips.com/viewthread.cfm?qid=414073 , https://cr4.globalspec.com/thread/97272/Opening-in-Skirts-of-Pressure-Vessel
- Ladders: https://osha.gov/laws-regs/standardinterpretations/2004-10-05 , https://www.osha.gov/laws-regs/standardinterpretations/2021-09-28 , https://www.ehs.washington.edu/system/files/resources/fixed-ladder-annual-inspection-checklist.docx
- Saddles: https://eng-tips.com/threads/location-of-saddles.103725 ; boot example: https://www.scribd.com/document/603450540/Boot
- Y-strainer: https://www.sureflowequipment.com/solution/yf150-cast-steel-y-strainer/ ; control valve F-F scope: https://www.isa.org/products/ansi-isa-75-08-01-2016-r2025-face-to-face-dimensio
