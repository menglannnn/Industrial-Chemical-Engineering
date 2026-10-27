# Solid-fuel units: fluidized bed and fixed bed combustor/gasifier survey (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, step "new units". Written 2026-10-18 by an opus research subagent. It
defines two drawn-only units from real plant practice: a fluidized bed combustor/gasifier and a fixed bed
combustor/gasifier. For each unit it gives the form to model, the player parameters and scaling, the visible parts,
nozzles, supports, moving parts and quad budget. Tags: [src] = a source named in the list at the end; typ. = general
practice, not checked in a source; (calc) = my arithmetic from sourced numbers. Scale: 1 block = 1 m. Parts under
0.05 m are skipped unless they are signature parts.

## 0. Physics common to both units (sets the combustor/gasifier split)
- Combustor: excess air, λ ≈ 1.15–1.3 (typ.). The bed or grate must stay at about 850 °C, so 40–50 % of the heat goes
  into the water walls (typ.). The rest is recovered in a back pass and leaves at 120–150 °C. The visible result is
  membrane water walls, a steam drum, a big back pass, an air heater, fans and a stack.
- Gasifier: sub-stoichiometric, λ ≈ 0.25–0.45 (typ.), with O2 or air plus steam. No heat is taken from the bed
  (refractory or water jacket only), and 70–80 % of the fuel energy leaves as CO + H2 (typ.). The hot gas is cooled
  in a separate syngas cooler or quench. The visible result is a round pressure vessel, lock hoppers, a cooler, a
  syngas line and a vent to flare.
- Pipe rule (OD at most 1 m): flue gas does not fit. A 30 MWth coal boiler makes about 10 Nm³/s, which is 16 m³/s at
  150 °C and needs a 1.2 m duct at 15 m/s (calc). Flue gas therefore stays inside the unit and goes to a drawn stack,
  the same as on the existing fired heaters.
- Pressurised O2-blown syngas does fit. HTW Berrenrath: 34,000 Nm³/h at 10 bar [HTW], about 2 m³/s at 300 °C, so
  0.4–0.5 m. FBDB Mark V: 90,000 Nm³/h [TI] at about 30 bar, 200 °C, about 1.5 m³/s, so 0.35–0.45 m (calc).
  Pressurised gasifiers suit the game rule and give the N2-free syngas a chemical plant wants.
- Solids have no item logistics. FUEL, LIMESTONE and BED_MATERIAL enter through modelled hopper or bin tops. ASH and
  FLY_ASH leave through drawn chutes ending over grade.

## 1. Fluidized bed combustor/gasifier
### Definition
- BFB boiler: velocity 0.3–1.5 m/s on 0.3–1.0 mm sand [BFB-pat]. The bed expands to about 0.9 m [SNCR-pat]. The
  furnace has no hot cyclone. Valmet BFB covers 20–400 MWth [Valmet]. From outside it looks like a plain box boiler,
  the same as a grate boiler.
- CFB boiler: velocity 4.5–5 m/s (BFB 2–3) [slide]. Solids leave the furnace top, are caught in hot cyclones and
  return through a standpipe, a loop seal and a return leg. Range 30–1200 MWth [Valmet]. Grid heat release is about
  3.3 MW/m² (Lagisza: 966 MWth over 27.6 × 10.6 m, calc [Lagisza]; a design study uses 3.34 [UGM]).
- FB gasifiers:
  - HTW: pressurised bubbling bed with cyclone char recycle; O2/steam or air; 800–900 °C; post-gasification O2/steam
    in the freeboard [HTW][GIDARA].
  - U-Gas: a similar vessel with an agglomerating ash outlet.
  - Atmospheric air-blown CFB gasifiers: CFB boiler hardware in refractory; the low-heating-value gas goes to a kiln
    or boiler; the gas needs a duct over 1 m.
  - Dual fluidized bed (DFB): Güssing, 8 MW fuel, 2 MWe; a bubbling steam gasifier and a riser combustor joined by a
    chute and a loop seal [Guessing]. Small, two reactors, research-scale: do not model it now.
- Recommendation: model the CFB loop: a tall furnace or riser, hot cyclones beside it, a standpipe, a loop seal and a
  return leg. From 30 m away, the cyclone is the one feature that makes a fluidized bed readable. A BFB is not a
  separate kind.
- Owner question Q1: make one kind with a BOILER/GASIFIER mode. The solids loop, fuel chutes, bed-ash drain, start-up
  burners and steel are the same in both modes. Vendors built atmospheric CFB gasifiers on their CFB boiler platform.
  The mode only switches the wall (rectangular water-wall box or round refractory pressure vessel), the heat sink
  (drum + back pass + air heater, or syngas cooler), the feed (open chutes, or lock hoppers + screws) and the nozzle
  set. Alternative: two kinds. That is better only if the owner wants different main-size meanings.

### Player parameters
- Main size W: the inside width of the furnace or riser. This is the long side of the box (boiler mode) or the inside
  diameter (gasifier mode).
  - Boiler mode: 3–26 m, step 1 m. Real range: Chalmers 12 MW research CFB, 1.47 × 1.42 m bottom, 13.5 m tall
    [Chalmers]; Baima 300 MWe, 15.1 × 12.6 × 35.5 m; Lagisza 460 MWe, 27.6 × 10.6 × 48 m [Lagisza]; Baima 600 MWe,
    27.9 × 15.0 × 55 m [Baima].
  - Gasifier mode: 1.5–6 m, step 0.25 m. Berrenrath bed is 2.75 m, 140 MW [HTW].
- Boiler-mode scaling:
  - Depth D = clamp(0.65 W, 2.5, 11). Depth is capped because secondary air must reach the furnace centre, so large
    units grow in width.
  - Furnace height H = clamp(18 + 1.25 W, 20, 45). At W 15 this gives 37 m, close to Baima's 35.5 m. A 400 MWth design
    study has 29.4 m [UGM].
  - Thermal input ≈ 3.3 MW/m² × W × D: 25 MWth at W 3 and 215 MWth at W 10; capped at about 950 MWth.
- Second parameter n: number of cyclones in a row on the rear wall, 1–4. Default ceil(W/8).
  - Cyclone diameter Dc = clamp(0.85 W/n, 2, 8.7). Basu gives about 8 m as the practical maximum [UGM]. Baima 300 has
    four cyclones of 8.7 m [Baima]. The 400 MWth design study has two of 6.55 m [UGM]. Lagisza has eight, on the two
    long walls [Lagisza].
  - Gasifier mode: n = 1 (2 when ID is over 4.5 m).
- Cyclone proportions (Basu, as quoted in [UGM]):
  - Barrel 1.0–1.5 Dc tall; total height including the cone 2–3.5 Dc (use 1.25 and 2.75).
  - Roof gas outlet 0.45–0.55 Dc.
  - Inlet duct 0.75–1.5 Dc tall × 0.25–0.35 Dc wide, from the upper rear wall.
  - Standpipe 0.15–0.25 Dc.
  - Loop seal box 2.5 × 1.25 standpipe diameters, entering 2–4 m above the grid.
- Other boiler-mode dimensions:
  - Lower furnace: refractory-lined taper over the bottom 0.15 H; the grid is 0.6 D deep (typ.).
  - Grid elevation (support height) 5–8 m. Bed-ash coolers, fans and conveyors sit under it.
  - Back pass: behind the cyclones, width W, depth 0.45 D, the same top as the furnace.
  - Tubular air-heater box under the back pass, 0.25 H tall, with 2–4 pyramidal fly-ash hoppers.
  - Stack OD clamp(0.25 W, 1.5, 6), top at the furnace roof + 10 m, at most 60 m.
- Envelope:
  - W 3: about 12 × 9 m footprint, about 32 m tall.
  - W 26: about 40 × 36 m footprint, about 59 m tall (45 m furnace + 8 m grid elevation + 6 m roof steel and drum).
  - Real utility CFBs reach 48–55 m furnaces and about 65–70 m overall; the furnace cap of 45 m handles this.
- Gasifier mode (typ. unless tagged):
  - Refractory 0.3–0.45 m thick, so shell OD ≈ ID + 0.8 m.
  - Tangent height 12 + 4 ID (23 m at Berrenrath's 2.75 m).
  - Cyclone diameter ≈ 0.7 ID.
  - Lock-hopper stack on the feed side: three vessels, each about 1.1 ID across × 1.5 ID tall.
  - Syngas cooler 0.6 ID across × 3 ID long, with its own drum.
  - The Berrenrath structure is 80 m; follow-up plants were planned at 50 m [HTW]. Cap at 60 m.

### Visible parts (outside in)
- Steel frame: the boiler hangs from 2–4 m deep roof girders on 4–8 perimeter columns and grows downward when hot
  (typ.). A big braced open frame with a clad boiler inside is what open-air (Asian) plant photos show.
- Furnace: water walls behind grey/aluminium profiled cladding. Buckstays (horizontal girders) ring the furnace every
  3–4 m (signature).
- Roof: a steam drum across the width, W + 2 m long and 1.2–1.8 m across. Riser bundles run from the roof headers to
  the drum. Two to four downcomers, 0.4–0.6 m, run down outside the furnace corners. Safety valves with tall vent
  silencers stand on the drum and on the main steam header (signature skyline).
- Cyclones: round and clad, with a cone, a standpipe and a loop seal near the grid. Two or three small fluidising-air
  lines feed the loop seal. A rectangular crossover duct runs from each cyclone outlet into the top of the back pass.
- Lower furnace:
  - Secondary-air manifolds ring the furnace at 2 levels (1.5–5 m over the grid), 0.8–1.5 m ducts with stubs.
  - A windbox 2–3 m tall with the primary-air duct.
  - Two to four start-up burners at the duct or windbox and on the side walls.
- Front wall: one fuel chute per 3–5 m of width, 2–3 m above the grid. Each chute is fed from a day bin through a
  drag-chain or screw feeder on a feeder floor. Optional small bins for limestone and sand.
- Under the grid: 2–4 bed-ash drain pipes to bed-ash coolers (rotary drum 1–1.5 m × 4–6 m, or water screws) (typ.).
- Back pass: a clad box with access doors and soot-blower stubs. Below it, the air-heater box and ash hoppers, then the
  flue duct, the ID fan and the stack. Primary-air (PA) and secondary-air (SA) fans stand at grade with scrolls 3–6 m
  high.
- Gasifier mode replaces the wall and heat-sink parts above:
  - A painted carbon-steel pressure shell with 2:1 heads, no cladding, and Class 300 flanges.
  - O2/steam ring headers at the distributor and at the post-gasification level, about 0.4 H (signature).
  - The lock-hopper stack with big valves, and one or two horizontal screw feeders into the bed 1–2 m above the
    distributor.
  - A cyclone with a refractory dipleg back to the bed.
  - A water-cooled ash screw and an ash lock hopper under the vessel.
  - A vertical syngas cooler with a drum.
  - A tall open structure with a floor at each lock valve.

### Nozzles (sizes as OD, at most 1 m; different faces or levels so the pipe-spacing rule holds)
- Boiler mode:
  - FEEDWATER: economiser inlet on the back-pass side at about 0.35 H; clamp(0.015 W + 0.08, 0.1, 0.45).
  - STEAM: final superheater outlet at roof level, on the other side; clamp(0.025 W + 0.1, 0.15, 0.6).
  - BLOWDOWN: drum end, 0.05–0.08 m.
  - DRAIN: lower ring header at grade, 0.05 m.
  - START_UP_FUEL: oil or gas at windbox level, 0.05–0.1 m.
- Gasifier mode:
  - OXIDANT: air or O2, the material decides; bed and post-gasification rings; clamp(0.05 ID, 0.05, 0.25).
  - MODERATOR_STEAM: clamp(0.07 ID, 0.08, 0.3).
  - SYNGAS: syngas cooler exit; clamp(0.17 ID, 0.25, 1.0); 0.47 m at 2.75 m ID (calc).
  - FEEDWATER, STEAM (cooler export) and BLOWDOWN: as in boiler mode, sized 0.04 ID.
  - LOCK_GAS: N2 or CO2, 0.05–0.1 m.
  - VENT: lock depressurisation to flare, 0.1–0.15 m.
- Solids, both modes (modelled): FUEL bin tops, one per chute; optional LIMESTONE and BED_MATERIAL bins; ASH chutes
  under the coolers or ash lock; FLY_ASH under the hoppers or filter.
- Internal: AIR (fan inlets drawn as bellmouths or silencers), FLUE_GAS (to the drawn stack), PA and SA ducts,
  loop-seal air, the cyclone loop and superheater links.

### Supports and access
- Concrete pedestals 0.5–1 m high. Platforms ring the furnace at the feeder floor, the SA levels, the cyclone inlet and
  the drum or roof. Back-pass platforms every 4–6 m at the soot blowers.
- Boiler structures use a stair tower with a landing every 3–4 m, not caged ladders. Rails 1.07 m high.
- Gasifier mode: the vessel sits on a support ring at about 1/3 height on a structure floor. Concrete fireproofing up
  to the first floor (a syngas fire area, as on the heaters).

### Moving parts (visible)
- Bed-ash cooler drum: turns at 1–6 rpm on a horizontal axis (typ.). This is the clearest motion.
- Feeder drives: rotary valves at 5–20 rpm and screws at 10–60 rpm; the motor and coupling shafts turn (typ.).
- Fan couplings: 990–1490 rpm; turn the shaft or leave it static.
- Gasifier lock valves: actuators stroke every few minutes (slider).

### Simplifications and quads
- Water walls as textured panels with no tubes. Cladding as boxes. Drum, downcomers and riser bundles as single bars.
- Cyclones with 24 segments and standpipes with 16.
- Steel: perimeter columns and girders per platform level; X-bracing only in the end bays.
- Estimates: about 15–25k quads at W 10, about 40–50k at W 26; gasifier mode 10–20k.
- Leave out first: soot blowers, doors, SA branch stubs, loop-seal air lines, instrument and drain piping, buckstays
  below W 6, rail posts closer than 2 m.

## 2. Fixed bed combustor/gasifier
### Definition
- Grate-fired stoker boiler: a thin fuel layer, 0.1–0.3 m, on a travelling, vibrating or reciprocating grate in excess
  air.
  - Range 0.3–175 MWth at 1–2 MW/m² of grate, grate at most about 100 m² [Wiki].
  - B&W's water-cooled vibrating grate takes up to 2.5 MW/m² for 10–250 MW input [B&W].
  - Vibrating grates slope down and are built from sections about 1.8 m (6 ft) wide, with steep siftings hoppers
    underneath [VG-pat].
  - Fuels: biomass, MSW (reciprocating grates), coal (chain grate, spreader stoker).
- Updraft moving-bed gasifier, Lurgi/Sasol FBDB: lump coal sinks through a 3–4 m bed (typ.) against rising O2 + steam.
  - Gas leaves at 480–530 °C, ash at 290–330 °C, the coal lock runs at 140–180 °C [US8192514].
  - Pressure about 30 bar (a Lurgi design study gives about 450 psia [TriState]).
  - Tars and phenols leave with the gas and are washed out as gas liquor.
  - Fines below about 5 mm are rejected (typ.).
  - Fleet: 84 units at Secunda [Secunda] and 14 at Dakota [DGC].
  - Related: BGL (slagging); small atmospheric updraft biomass gasifiers.
- Downdraft (Imbert) gasifier: 50–500 kW [FAO]. The largest, Loma Plata, is 7.5 m tall and 2.1 m in OD [FAO]. The
  throat limits a single unit to about 1 MWth (typ.), and it makes air-blown producer gas for engines. Too small and
  the wrong product: do not model it.
- Owner question Q2: make two kinds, not one with a mode. The two share nothing visible. A grate boiler is a box
  boiler with front feeders and a drag-chain ash trough. An FBDB is a tall round water-jacketed vessel between a coal
  lock and an ash lock. A mode switch would be two builders under one name.
  - Build the FBDB gasifier first. It is iconic (Sasol rows), it is a chemical-plant unit, and its pressurised syngas
    fits the 1 m pipe rule.
  - Build the grate boiler later, or skip it: the fluidized bed boiler mode already raises steam from solids.

### FBDB gasifier: parameters and scaling
- Main size D: gasifier inside diameter, 2.0–5.0 m, step 0.25 m. Real units:
  - 1.13 m water-jacketed quarter-scale pilot [NWU].
  - 2.7 m Rheinbraun brown-coal units [NWU-s].
  - 3.66 m Mark III.
  - About 4 m Mark IV: GG41, 4 m, about 100 t of contents [NWU]; Dakota, 13 ft ID × 40 ft high [DGC].
  - Mark V: larger, 90,000 Nm³/h from 75 t/h of coal [TI]. Its diameter was not verified here.
- Second parameter: none needed. Optional later: gasifiers per row (1–3) sharing one structure, as at Sasol.
- Vessel and lock dimensions:
  - Vessel tangent height 3.1 D (12.2/3.96 at Dakota, calc [DGC]); 2:1 heads.
  - Water jacket: shell OD ≈ D + 0.4 m (typ.).
  - Coal lock above the top head: 0.6 D across × 1.0 D tall plus a cone. Coal bunker above it: 1.0 D × 0.8 D, a
    modelled hopper top.
  - Ash lock under the grate housing: 0.6 D across × 0.9 D tall plus a cone.
  - Locks and bunker together are more than the vessel height again [overview, via NWU-s].
- Envelope: overall about 7.5 D, which is 15 m at D 2, 30 m at D 4 and 38 m at D 5. Structure bay (D + 5) × (D + 5),
  plus about 2 m on the wash-cooler side.
- Support height: ash-lock bottom 3–5 m above grade, for the ash sluice or conveyor.

### FBDB gasifier: visible parts
- Shell: insulated and clad, because the jacket sits near saturation, about 235 °C at 30 bar (typ.). A horizontal
  jacket steam drum, 0.25 D × 0.8 D, sits on the top floor beside the coal lock, with riser pipes.
- Coal lock: a thick cylinder with top and bottom bell or disc valves. Their actuating rods and vertical hydraulic
  cylinders stand above the lock [US8192514] (signature), with pressurising and vent lines.
- Coal distributor (Mark IV): a 0.5 m to 3.4 m cone inside [US8192514], driven by a motor and gearbox on the top head.
  A hoist beam over the coal lock (typ.).
- Raw gas outlet near the top on one side. It leads to the wash cooler: a vertical pipe vessel 0.3 D × 1.5 D beside the
  vessel top with spray inlets. The gas then turns down to a waste-heat boiler, which is not part of this unit.
- Bottom: the grate housing cone with two drive sets (motor + gearbox + pinion on a ring gear) on opposite sides
  [US8192514]. The O2 + steam pipe enters the grate pillar from below.
- Under the vessel: the ash lock with valves and cylinders, then a chute to a quench or sluice trough at grade.
- Structure: an open steel or concrete gasifier house with 5–7 floors. Floors at the ash lock, the grate drives and
  agent inlet, the vessel support (about 0.3 height), the gas outlet and wash cooler, the coal-lock valves and the
  bunker top. Stair tower.

### FBDB gasifier: nozzles (D = ID)
- OXYGEN: agent floor; clamp(0.03 D, 0.05, 0.2).
- STEAM: agent floor, another face; clamp(0.05 D, 0.08, 0.3).
- SYNGAS: wash-cooler outlet; clamp(0.1 D, 0.2, 0.6); 0.4 m at Mark IV against 0.35 m needed (calc).
- QUENCH_LIQUOR: into the wash cooler; GAS_LIQUOR: out of the wash cooler. Each clamp(0.03 D, 0.05, 0.15).
- JACKET_FEEDWATER: 0.05–0.1 m, low on the jacket.
- VENT: coal and ash lock depressurisation, combined, 0.05–0.15 m.
- LOCK_GAS: 0.05–0.1 m.
- Solids: FUEL (bunker top) and ASH (chute).
- Internal: jacket steam into the agent steam, grate seal water.
- Flanges: Class 300/600.

### FBDB gasifier: moving parts and quads
- Moving parts:
  - Grate: 2–12 rev/h [US8192514], too slow to see. Turn the drive-motor couplings instead.
  - Cone-valve cylinders: slide with a 0.3–0.6 m stroke. The coal lock cycles several times an hour (typ.); the ash
    lock is emptied about once an hour and takes 5–10 min [ash-pat].
- Quads: about 8–15k, with the structure dominant. Leave out first: lock pressurising piping, the jacket risers beyond
  two, distributor internals and floor grating detail.

### Grate boiler: parameters and parts (second kind, later)
- Main size: grate width Wg, 2–12 m, step 0.5 m.
- Second parameter: grate length Lg, default clamp(1.1 Wg, 3, 10).
- Capacity: input ≈ 1.5 MW/m² × Wg × Lg, which gives 9–150 MWth.
- Furnace: depth Lg + 1; height clamp(10 + 2.2 Wg, 12, 36) (typ.). Grate slope: travelling 0°, vibrating 4–8°,
  reciprocating MSW 12–20° (typ.).
- Front to rear:
  - A firing aisle with day bins over the feeders: spreader spouts, screws or hydraulic rams, one per 1.5–2 m of
    width.
  - Two or three levels of over-fire air ports.
  - Three to five under-grate air plenums with siftings hoppers [VG-pat].
  - At the grate end, a submerged drag-chain ash trough, 0.8–1.5 m wide, rising at 30–35° to its discharge
    (signature).
- Boiler:
  - Up to about 100 t/h: a bi-drum boiler. The steam drum lies front-to-rear on top, with a convection bank down to a
    mud drum (typ.).
  - Larger: a single drum plus a back pass, economiser and tubular air heater; then the ID fan and a drawn stack, as
    in section 1.
- Nozzles: FEEDWATER, STEAM, BLOWDOWN, DRAIN and START_UP_FUEL, sized as in section 1 with Wg in place of W. Solids:
  FUEL bins, ASH from the trough, FLY_ASH hoppers.
- Supports:
  - Small units are bottom-supported on a plinth, grate at 2–4 m. Large units are top-supported in a frame.
  - A firing-aisle platform over the full width, a drum platform, stairs above 10 m.
- Moving parts:
  - Ram feeders: slide 0.5–1.5 m every 1–5 min.
  - Drag chain: 0.05–0.2 m/s.
  - Travelling-grate head shaft: chain speed 0.5–15 m/h.
  - Screws and fans (typ.).
- Quads: 10–25k.

## Owner questions (recommendation first)
- Q1. Fluidized bed: one kind, CFB loop, with a BOILER/GASIFIER mode (recommended). Alternative: two kinds.
- Q2. Fixed bed: two kinds, the FBDB gasifier first. The grate boiler comes later or is dropped as covered by Q1's
  boiler mode. No downdraft gasifier.
- Q3. Flue gas: stays inside and goes to a drawn stack, as on the heaters (recommended). The alternative, a capped
  1.0 m FLUE_GAS face, would understate the real duct by 1.5–6×.
- Q4. Gasifiers: draw the pressurised forms (HTW, FBDB), each with one OXIDANT or OXYGEN nozzle where the material
  decides air or O2 (recommended). Atmospheric air-blown forms need ducts over 1 m.
- Q5. Size cap: boiler furnace at most 45 m (W at most 26) to stay under 60 m overall. Real units reach 55 m furnaces
  [Baima].

## Sources
- [Lagisza] Mirek & Ziaja, Lagisza 966 MWth CFB tests, Arch. Thermodynamics (czasopisma.pan.pl, 13-paper-Mirek.pdf).
- [Baima] Frontiers in Energy reviews of large Chinese CFBs, 2017 (s11708-017-0512-4) and 2020 (s11708-020-0666-3).
- [UGM] Fadhlurrohman et al., 400 MWth biomass CFB design, JMDT 7(2) 2025, quoting Basu (2006).
- [Valmet] valmet.com BFB and CFB boiler pages. [Chalmers] Chalmers 12 MW research CFB.
- [slide] CFBC training slides (scribd). [BFB-pat] [SNCR-pat] US patents on BFB bed material and SNCR.
- [HTW] Rheinbraun Berrenrath HTW paper, NETL Gasification Technologies Conference. [GIDARA] gidara-energy.com/htw.
- [Guessing] IEA Bioenergy Task 33, Güssing FICFB (Rauch).
- [US8192514] Sasol-Lurgi fixed-bed coal gasifier patent. [ash-pat] Lurgi grate and ash-lock control patent.
- [DGC] Basin Electric DGC fact sheet. [Secunda] Secunda CTL profile.
- [NWU] Sasol GG41 Mark IV studies, NWU repository. [NWU-s] NWU studies as summarised in web search.
- [TI] Techniques de l'Ingénieur J5200. [TriState] Tri-State Synfuels Fluor study (fischer-tropsch.org).
- [Wiki] Wikipedia, Grate firing. [B&W] B&W water-cooled vibrating grate sheet PS201-113 (search snippet; the PDF now
  returns 404). [VG-pat] water-cooled oscillating grate patent. [FAO] FAO T0512E, wood gas producer review.
