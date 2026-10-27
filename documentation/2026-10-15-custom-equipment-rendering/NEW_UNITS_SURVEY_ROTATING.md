# New rotating units: centrifugal compressor and steam turbine, engineering survey (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, step "new units". Written 2026-10-18 by an opus research subagent to
define two new drawn-only units from real plant practice before a plan is written. [src] = vendor sheet, standard,
handbook or equipment listing (list at the end); typ. = general practice or an estimate derived here. The scale is
1 block = 1 m; parts under 0.05 m are skipped unless they are a signature part. No code was changed.

## 1 Centrifugal compressor (D = impeller diameter, N = impellers)

**Definition**
- Forms: (a) horizontally split multistage between-bearings casing (API 617: BH MCL, Elliott M, Siemens STC-SH), the
  refinery/petrochemical workhorse to 40–60 bar [src BH, BHEL]; (b) barrel (vertically split: BCL, Elliott MB), same
  rotor in a forged cylinder with bolted or shear-ring end covers, for H2-rich gas and up to 350 bar [src BHEL,
  handbook]; (c) integrally geared (bull gear, 1–4 pinions, 1–2 overhung impellers each, intercooled) [src BHEL SRL],
  used for air separation, CO2 and plant air. It looks like a gearbox ringed with snail volutes.
- **Recommend (a)**: wet-gas, net-gas and refrigeration compressors. Physics: the nozzles point down so the upper
  half lifts off without opening the process piping, so the compressor and its driver sit on a mezzanine [src handbook].
  That is how a refinery compressor looks. (b) can come later as a style variant (same envelope, round heavy end
  covers, nozzles up at grade, common with gas-turbine drives [src handbook]). (c) is not modelled (other process family).
- One section (one suction, one discharge). Back-to-back and compound casings with intercooling exist (2MCL [src
  BHEL]) but need 4 nozzles 2 blocks apart; leave them for later.

**Parameters**
- Main size D: real 0.25–1.83 m (72 in impellers max [src BH]); game 0.3–1.5 m, step 0.1 m. Tip speed 230–300 m/s typ.
  gives 15,000–19,000 rpm at D 0.3 and 3,000–3,800 rpm at D 1.5 (BH ≤ 20,000 rpm [src]; Elliott 46M frame
  37,000–58,000 m³/h at 4,600–6,000 rpm [src handbook]).
- Second N impellers: 2–9, step 1 (rotor dynamics limits a casing to about 8–10 typ.).
- Casing OD 1.9 D (diffuser and return channel need a bore of about 1.7 D, plus the wall). Length between end walls
  (1.2 + 0.35 N) D (inlet plenum about 0.6 D, 0.35 D per stage, discharge volute and balance drum about 0.6 D).
  Bearing housings 0.7 D OD × 0.5 D long at each end. Shaft centre 1.0 D above the soleplates. Typ. throughout.
- Shaft power P ≈ 2.5 MW · (D/0.65)² · N/4. Anchor: Elliott 32M4, 33,000 m³/h, 2.5 MW, 7,176 rpm [src PHX]; typ.
- Driver (default, see Q3): 2-pole motor plus a parallel-shaft step-up gear. Motor shaft height Hm ≈ 0.5 m ·
  (P/1 MW)^0.25 (frame 450–500 at 1 MW, about 1,000–1,120 at 20 MW, typ.). Body 4.5 Hm long, 2.6 Hm wide and 2.2 Hm
  tall, plus a 0.8 Hm top cooler (TEWAC/TEAAC). Gear 1.2 Hm long × 2.0 Hm wide × 2.0 Hm tall; output shaft offset
  0.6 Hm.
- Envelopes (typ.): D 0.3, N 2 (≈0.3 MW): train ≈ 3.5 × 1.0 m, deck ≈ 5.5 × 3 m at 3.5 m, top ≈ 5 m. D 1.5, N 6
  (≈20 MW): casing 2.85 m OD × 6.5 m, motor 4.8 × 2.8 × 3.2 m, train ≈ 14–15 m, deck ≈ 17 × 7 m at 6–7 m, top ≈ 10 m.
  A packaged reference: Elliott 32M4 with an SRV4 turbine on one skid, 5.95 × 2.84 × 2.81 m, 32.6 t [src PHX].

**Visible parts (outside in)**
- Concrete table-top: beams 0.8–1.5 m deep on 4–8 columns 0.6–1.2 m square, deck 4–7 m above grade typ., with
  openings where the nozzle spools pass. The table carries a grating operating deck with rails and a stair (not a
  ladder) on large units.
- Casing: a long horizontal cylinder (1.9 D) with a heavy horizontal joint flange at the shaft centreline. The flange
  stands 0.12 D proud on each side, is 0.15 D thick and carries one row of large studs and nuts (pitch about 0.25 m;
  nuts 0.06–0.15 m). This joint is **the signature**. Also 2–4 lifting lugs on the upper half. Cast casings bulge at
  the inlet plenum and discharge volute [src handbook]; fabricated casings are smooth with ribbed nozzle bodies.
- Feet at centreline level on soleplates: two at the suction end, one or two at the discharge end [src handbook].
  Bearing housings at both ends with oil pipes and probe boxes; a domed thrust-bearing cover (0.5 D) outboard.
- Balance line: an external pipe 0.15–0.25 D from behind the balance drum (discharge end) back to the suction, typ.
- Dry gas seal panel: a cabinet about 1.0 × 0.6 × 2.0 m on the deck near the drive end; its tubing is under 0.05 m.
- Coupling guards: 0.6–0.9 D long, yellow or orange. Motor: a box with a top-hat cooler and a side terminal box.
  Gear: split housing with a breather.
- Lube/seal oil console (API 614) at grade beside or under the table. It has a reservoir, twin pumps with motors, twin
  coolers, twin filters and a transfer valve; from 2 × 1.2 × 1.2 m to 6 × 3 × 2.5 m typ. Physics: bearing drains
  return by gravity, so the reservoir sits below the machine. Overhead rundown tank: a horizontal drum 0.6–1.2 m ×
  1.5–3 m on a stand about 4–5 m above the shaft. It feeds the bearings by gravity during coast-down (≈4 m [src lube
  chapter]; 5.3 m in one BOG compressor [src PNL]).
- Below the table: suction and discharge drop with long-radius elbows. The suction run has a strainer spool and needs
  3–8 pipe diameters straight [src WIP]. The anti-surge valve (a large globe with a 0.5–1 m actuator) sits at the
  recycle loop's high point [src WIP]. The suction drum and aftercooler are separate units.

**Nozzles**
- SUCTION: lower half at the inlet plenum, pointing down. Size 0.9 D (real 0.6–1.2 D: 30 in on the 32M4 for a wheel of
  about 0.65 m [src PHX]; 86 in flanges with 72 in wheels [src BH]), capped at 1.0 m. The spool passes through the
  table and its flange sits on the table's underside face.
- DISCHARGE: lower half at the discharge volute, down; size 0.45 D (real 0.3–0.6 D; 12 in against 30 in on the 32M4
  [src PHX]). Real axial spacing is (0.1 + 0.35 N) D, which is under 2 m for most D < 1.0. Do not stretch the casing
  (+35 % to +130 % at D 0.3–0.6). Instead the discharge spool turns 90° outward under the table, perpendicular to
  the shaft, so the two pipe blocks are ≥ 2 blocks apart. Real piping under a table does the same.
- COOLING_WATER_IN / _OUT on the console coolers, 0.05–0.15 m, 2 blocks apart: the only utility nozzles proposed.
- Internal: lube and seal oil, seal gas and seal vents (0.025–0.05 m), casing drains, the balance line, and anti-surge
  recycle. In a real plant the recycle runs from after the aftercooler to upstream of the suction drum. Here the unit
  models it as an internal valve from discharge to suction (a game simplification: hot recycle).

**Supports and access**
- Large trains: soleplates grouted to the table, no common baseplate. Up to about 5 MW: a packaged steel skid
  [src PHX], at grade with side or up nozzles, or on the table.
- Real plants: a shed with a monorail or EOT crane rated 150 % of the heaviest lift, 10 m between compressors, the lube
  cooler reachable from the road [src WIP]. Leave the shed and crane out. Deck walkway ≥ 0.8 m (game rule); real
  1.0–1.5 m all round for maintenance.

**Moving parts**: none visible in service. Couplings run under solid guards; the shaft turns at 3,000–20,000 rpm and
the motor at 2,980/3,580 rpm. Proposal: an NDE fan cowl on the motor (reuse the pump-motor fan) at a capped visual
rate; optionally a short shaft collar between the bearing housing and the guard. Nothing slides.

**Simplifications and quads**: about 5–12 k quads; studs are about 40 % at large D. Leave out first: seal and
instrument tubing, nuts below D 0.6 (studs only, or every second one), console pump detail, the anti-surge valve and
recycle piping, spring hangers, shed and crane.

## 2 Steam turbine (Dc = exhaust-end casing outer diameter, N = stages)

**Definition**
- Forms: (a) API 611 general purpose: single-stage (Curtis wheel) mechanical drive for spare pumps, fans and small
  compressors. Typically ≤ 6,000 rpm and ≤ 3,000 kW, ≤ 48 bar / 400 °C [src API 611 scope]. Elliott YR is rated up
  to 4 MW, with wheels of probably 18–28 in (460–710 mm) [src YR; columns unlabelled]. Mostly back-pressure to an
  LP header. (b) API 612 special purpose: multistage (5–15 stages), horizontally split; back-pressure, extraction or
  condensing; drives compressors directly at 3,000–13,500 rpm or generators through a gear [src Siemens]. (c)
  Turbo-generator set: (a) or (b) plus a gear and a 4-pole generator on one base frame or table. SST-100 8.5 MW
  package 8 × 3.7 × 3.4 m; SST-300 up to 50 MW, 12 × 4 × 5 m [src Siemens].
- Exhaust: back-pressure (1.5–25 bar into a header: the refinery let-down role) or condensing (0.08–0.3 bar abs into a
  surface condenser). Condensing exhaust areas are 0.22–8 m², about 0.5–3.2 m equivalent diameter [src Siemens].
  That is above the 1 m pipe cap, so the condenser must be part of the unit.
- **Recommend (c)**, with N ≥ 2 for the multistage turbine and N = 1 for the single-stage GP look. Two unit kinds:
  BACK_PRESSURE (default; at grade, exhaust up) and CONDENSING (table-top with an integral surface condenser
  underneath). Reference set: D-R RD9MQQ, 13 MW condensing, 12 stages, 4,500 rpm; Renk gear 2.514 to 1,800 rpm;
  Indar 4-pole 15.5 MVA; GEA condenser 970 m²; 52.7 t/h at 45 bar [src SR].

**Parameters**
- Main size Dc: real 0.5 m (single-stage) to about 4 m (100+ MW condensing); game 0.6–3.0 m, step 0.1 m. Power
  P ≈ 5 MW · Dc², within a factor 2 (typ.).
- Second N stages: 1–12 (industrial 5–15; 12 on the 13 MW set [src SR]).
- Casing (all typ.): exhaust end Dc; HP end 0.65 Dc (condensing) or 0.8 Dc (back-pressure), joined by a cone. Length
  between end walls (0.5 + 0.15 N) Dc: 0.65 Dc at N 1, 2.3 Dc at N 12. Split flange 0.08 Dc proud. Shaft centre
  0.6 Dc above the base (back-pressure) or 0.45 Dc above the deck (condensing; the lower exhaust casing sits in the
  deck opening).
- Front standard 0.6 Dc long. Gear 0.6 Dc long × 1.0 Dc wide × 0.9 Dc tall, turbine shaft high. Always draw a gear:
  sets up to 50–65 MW are geared [src Siemens SST-100…400].
- Generator: shaft height Hg = 0.5 Dc, clamped to 0.4–1.4 m (ABB 6–85 MVA on frames 710–1400 mm [src ABB]; Indar
  LSA-900, 15.5 MVA [src SR]). Body 3.5 Hg long, 2.2 Hg wide and 2.0 Hg tall, plus a 0.6 Hg TEWAC cooler on top;
  exciter 0.6 Hg long at the NDE.
- Condenser (CONDENSING kind): cylindrical shell 1.6 Dc × 3.5 Dc, axis parallel to the shaft, with 0.35 Dc water boxes
  and a 0.3 Dc hotwell. Deck elevation ≥ max(3.5 m, 2.4 Dc + 0.5 m). Physics: the exhaust must fall straight into
  the condenser; the table height is the condenser height.
- Envelopes (typ.): Dc 0.6, N 1, back-pressure set ≈ 3.7 × 1.4 × 1.5 m on a 0.3 m plinth. Dc 3.0, N 12, condensing
  ≈ 18 m long, 6–7 m wide over the deck, deck 7.7 m, top ≈ 10.5 m. Compare SST-600, 19 × 6 × 5 m without the
  condenser, and SST-900, 20.5 × 11 × 10 m with it [src Siemens].

**Visible parts (outside in)**
- HP casing and steam chest under aluminium-sheet insulation cladding (big sets: a full hood). Exhaust casing bare
  painted steel, ribbed, typ.
- Steam chest across the top of the HP end, 0.25 Dc dia × 0.8 Dc. It carries 1 (N ≤ 3) to 5 governor-valve spindles
  under a bar-lift or cam-lift bridge 0.2 Dc high, with a hydraulic servo 0.12–0.15 Dc dia on a bracket. Single-stage
  units have one governor valve plus 1–3 hand valves (handwheels 0.15–0.25 m) on the nozzle ring. Typ.
- Trip-and-throttle (or stop) valve: a separate globe valve on the inlet pipe beside the HP end. Body 0.25 Dc, with a
  yoke, a hydraulic trip cylinder and a 0.4–0.6 m handwheel. **The signature** from 10–30 m, together with the
  steam chest.
- Front standard: a box over the front bearing. The governor actuator (Woodward) and the overspeed trip sit on its
  front face; it slides on keys as the casing grows.
- Exhaust. Back-pressure: a round flange 0.4 Dc rising from the exhaust casing, with an expansion joint; GP turbines
  add a small sentinel valve. Condensing: a rectangular neck 1.1 × 0.8 Dc straight down through the deck, then an
  expansion joint into the condenser.
- Generator box with its top cooler, exciter housing and terminal box; gear with a split housing; coupling guards.
- Condenser: horizontal shell under the table with bolted water-box covers at both ends (2-pass: in and out at the
  front end). Below it a hotwell sump and 2 vertical can-type condensate pumps with visible motors; on top the
  air-extraction line and an atmospheric relief disc, typ.
- Small items: a gland steam condenser with exhauster (about 0.4 × 1.5 m); lube console and rundown tank as for the
  compressor. The SST-100 has its oil system inside the base frame [src Siemens].

**Nozzles**
- STEAM_INLET into the T&T valve, from below (table-top) or from the side (grade). Size 0.15 Dc (real 0.1–0.2 Dc,
  4–16 in), minimum 0.05 m.
- EXHAUST (back-pressure only): up, 0.4 Dc, capped at 1.0 m. The SST-200 back-pressure exhaust area 0.17–0.34 m² is
  about 0.47–0.66 m [src Siemens].
- Condensing kind (exhaust internal):
  - CW_IN (lower) and CW_OUT (upper) on the front water box, 0.4 Dc, capped at 1.0 m, ≥ 2 blocks apart. The 13 MW set
    rejects about 34 MW; at a 10 K rise that is 0.8 m³/s, about a 0.7 m pipe at 2 m/s (typ.).
  - CONDENSATE from the hotwell, 0.08 Dc, minimum 0.05 m.
  - AIR_EXTRACTION from the shell top, 0.06 Dc, minimum 0.05 m. It feeds the existing steam ejector unit.
- DRAIN (casing and steam chest, 0.05 m) is optional. EXTRACTION is left out of the first cut (one switch fewer); if
  added later: mid casing, 0.25 Dc.
- Internal: gland steam and leak-offs, lube oil, generator cooler water. Optional: one 0.05–0.15 m CW pair on the
  console.

**Supports and access**: the back-pressure set stands at grade on a 0.3–1 m block plinth with a steel base frame that
holds the oil system. The condensing set stands on an RC table-top (deck 0.8–1.5 m thick on columns) with the
condenser beneath on springs or its own footing. Operating deck at table level with rails and a stair; headroom
under the table for the condensate pumps. A turbine hall with an EOT crane is left out.

**Moving parts**: none visible in service; shafts are enclosed and the turning gear runs only at shutdown. Turbines
run at 3,000–13,500 rpm (SST-600 to 18,000 [src Siemens]); generators at 1,500/1,800 rpm. Proposal: a static valve
bridge and an optional generator or exciter fan cowl at a capped visual rate. A coupling-stub variant (Q1) would
show a turning shaft end.

**Simplifications and quads**: about 4–12 k quads with the condenser. Leave out first: gland condenser, drains,
instrument tubing, condensate-pump detail, flange studs below Dc 1.0, hood and acoustic enclosure.

## Owner questions (each with a recommendation)
- Q1 What does the standalone turbine drive? Recommend a turbo-generator set (gear plus generator), because that is
  what stands alone in a plant. A coupling stub ending on a block face (a Create shaft output) is a cheap variant if
  kinetic output is wanted. Pump and compressor drive is better done as a driver option on those units.
- Q2 Should the condensing kind include its surface condenser? Recommend yes. The exhaust (0.5–3.2 m) exceeds the 1 m
  pipe cap, so the unit exposes cooling water, condensate and air extraction instead. Back-pressure is the default.
- Q3 Compressor driver: recommend a motor plus gear by default, and later a direct steam-turbine drive that reuses the
  turbine geometry without a gear (typical for FCC wet gas and ammonia; steam for variable speed [src BHEL]).
  Integrally geared and barrel forms are not modelled now.
- Q4 Should the compressor unit own its concrete table, deck, lube console and rundown tank, with the support height
  setting the deck elevation (≥ 3.5 m)? Recommend yes; the down-nozzle spools need the table.

## Sources
- [src BH] Baker Hughes, MCL horizontally split compressors page: ≤ 60 bar, ≤ 600,000 m³/h, ≤ 20,000 rpm, 72 in
  impellers, 86 in flanges, steam turbine or motor drive.
- [src BHEL] BHEL Centrifugal Compressors brochure (2025): MCL ≤ 40 bar, BCL ≤ 350 bar, 2MCL back-to-back,
  connections up or down, SRL integrally geared, steam drive for variable speed.
- [src handbook] Compressor handbook ch. 12, "Design considerations and manufacturing techniques" (Dresser-Rand and
  Elliott figures; tte-lisa library): mezzanine with nozzles down, barrel at grade with top nozzles, foot layout, 46M
  frame.
- [src PHX] Phoenix Equipment stock 20971: Elliott 32M4 plus SRV4 skid. [src SR] Surplus Record 1113991: D-R RD9MQQ
  13 MW condensing set.
- [src WIP] whatispiping.com, centrifugal compressor piping. [src lube chapter] tte-lisa "Lubrication, sealing"
  chapter (search snippet). [src PNL] e-jurnal PNL boil-off gas compressor rundown tank paper.
- [src Siemens] Siemens Industrial Steam Turbines brochure E50001-G410-A101 (2013). [src YR] Elliott YR literature
  (search snippets). [src API 611 scope] GlobalSpec summary of API 611. [src ABB] ABB generators for steam and gas
  turbines, technical data.
