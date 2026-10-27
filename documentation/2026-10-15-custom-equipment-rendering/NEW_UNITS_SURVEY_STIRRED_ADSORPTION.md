# New units: stirred tank reactor and adsorption unit, engineering survey (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, step "new units". Written 2026-10-18 by an opus research subagent, for two
drawn-only units: a stirred tank reactor and an adsorption column. Each section covers what real plants build, what
the player sets, the visible parts, the nozzles, supports, moving parts and game simplifications. Notation: [src] = a
vendor sheet, standard or photo, listed at the end; typ. = general practice; calc = derived here from [src] numbers.
D = the main size. Proportions are for 1 block = 1 m.

## 1. Stirred tank reactor (D = shell inner diameter)

**Definition.** Plants use three common forms:
(a) A jacketed batch or CSTR vessel with a top drive, in glass-lined steel (Pfaudler / De Dietrich, DIN 28136 AE/BE/CE)
or stainless steel. Size 0.06-80 m3, D 0.5-4 m. Used in fine chemicals and pharma.
(b) A large carbon-steel or clad CSTR or polymerisation autoclave. PVC reactors are 70-200 m3 with straight-side
L/D 1.5-3. A 200 m3 reactor measures 5.5 m x 10 m. Many have a bottom drive and a reflux condenser on the head,
because the jacket area grows only as V^(2/3) [PVC].
(c) A loop or gas-sparged reactor: not a stirred tank.
- **Recommend (a)**, closed-welded with a manway (BE style), D 1.0-4.0 m. It is what people recognise as a "reactor",
  and its parts are visible from outside: drive, lantern, manway, sight glasses and jacket. Form (b) can come later as
  its own kind (owner question Q1).
- Real geometry [3V-CE]:
  - Jacket diameter d2 = D + 0.10 m (+0.15 m at D ≥ 3.2 m). The jacket covers about 90 % of the vessel height and
    the bottom head.
  - Head depth 0.20-0.26 D.
  - Vessel height, from the bottom of the bottom head to the top-nozzle flanges, is 1.29 D at D 1.4 m and 1.79 D at
    D 3.4 m: tall vessels grow taller relative to D.
  - Impeller 0.6 D (small) to 0.5 D (large). Wall baffles, if used, are 4 x D/12 (typ.).

**Player parameters.**
- Main: D 1.0-4.0 m, step 0.2 m. Smallest is about 1.3 m3; largest about 70 m3.
- Second, optional: straight-side height S, step 0.5 m, default 1.1 D, range 0.8-2.0 D.
- Support height: the clearance under the bottom valve, 0.9-3 m. It is not the deck elevation (see Supports).
- Derived dimensions:
  - Jacket = D + 0.1 m.
  - Each head = 0.25 D.
  - Lugs 0.2 D + 0.1 m below the top tangent.
  - Top flanges 0.6 + 0.2 D above the lug base (0.85 m at D 1.4, 1.30 m at D 3.4) [3V-CE].
  - Drive height (gear motor + lantern) 0.7 + 0.385 D (1.25 m at D 1.4, 2.0 m at D 3.4) [3V-CE, calc].
- Envelope:
  - D 1.0: deck at 2.1 m, top of drive about 4.1 m, footprint 3 x 3 to 4 x 4 blocks.
  - D 4.0: deck at about 6 m, top of drive about 9.8 m, footprint 7 x 7.
  - Check: a real 40 m3 reactor (D 3.4) is 9.2 m tall on legs [3V-CE].

**Visible parts, outside in.**
- Painted jacket shell. Stainless or glass-lined units are often insulated and clad: smooth with bands.
- Top head carries a dense crown of nozzles:
  - 6-9 process nozzles (DN 100-400) on a circle at 0.75-0.82 R.
  - An inclined manway (DN 350-600) with a ring of swing-bolt clamps: the strongest top detail.
  - A pair of sight glasses (DN 100-150): one with a lamp box, one viewing glass.
  - A baffle/thermowell nozzle with its transmitter head.
  - A rupture-disc/PSV stub.
- Drive in the centre, on the agitator nozzle (DN 150-250):
  - Lantern (stool) 0.4-0.6 m across with two large side windows. Shaft, coupling and mechanical seal are seen
    through them.
  - Gearbox on top, then the motor: either horizontal on a right-angle gearbox, or a vertical in-line gear motor
    [3V-CE, Pfaudler].
  - Seal pot (thermosiphon) about 0.18 x 0.5 m, with a gauge, on a bracket beside the lantern [3V-CE drawing].
- Below the vessel: a flush bottom outlet valve (DN 80-150) with a handwheel or actuator, under the bottom head.
- On the jacket: several small agitation nozzles (DN 50-80) low on one side.
- CE type only: a bolted top cover with 30-40 clamps.

**Nozzles** (size = fraction of D, minimum 0.05 m, cap 1 m):
- VAPOR: top head, 0.10 D (DN 300-400 at D 3.4), to an overhead condenser.
- FEED: top head, 0.06 D, ending in an internal dip pipe.
- Optional REFLUX/FEED_2: top head, 0.05 D.
- NITROGEN: top head, 0.05 m.
- VENT (PSV/rupture disc): top head, 0.04 D.
- PRODUCT/DRAIN: bottom outlet on the axis, 0.05 D, minimum 0.08 m.
- JACKET_IN: side, low on the jacket, 0.03 D (DN 50-100).
- JACKET_OUT: side, high on the jacket, 0.03 D. Steam reverses the direction: in at the top, condensate out at the
  bottom.
- SOLIDS/CATALYST: charged through the manway; a blind cover, not a pipe.
- Internal: agitator, baffles, dip pipes, thermowell, sampling.
- Pipe-spacing rule (calc). The drive takes the centre column (0, 0), and the top nozzles sit on a circle at 0.8 R.
  - D ≤ 1.2 m: no top pipe nozzle fits outside the drive block. Put VAPOR and FEED on the shell, just under the top
    tangent.
  - D 1.4-1.8 m: two top nozzles fit, on opposite edge blocks (±1, 0).
  - D ≥ 2.0 m: four top nozzles fit, on the corner blocks (±1, ±1), which keep one free block between each pair.
  - Never use edge-adjacent pairs such as (1, 0) and (0, 1): they touch at an edge.

**Supports and access.**
- Production reactors hang on 4 lugs (brackets to DIN 28145) in an opening of an operating floor. The top head,
  manway and drive are above the floor, at hand height (manway 0.85-1.3 m above the floor). The bottom valve is
  reached from the floor below [3V-CE; typ.].
- Small units (≤ 2.5 m3) also stand on 3-4 legs with 0.7-0.9 m clear under the bottom [3V-CE].
- Mass 3 t (2 m3) to 34 t (45 m3), empty [3V-CE].
- Game: build a square grating deck on 4 columns at lug level.
  - Clear walkway 0.8 m round the jacket; guard rails at 1.07 m.
  - Caged ladder, or a stair, when the deck is above 3 m.
  - Deck elevation = vessel length below the lugs + bottom-valve clearance, and never less than 2.1 m headroom.
  - Optional legs for D ≤ 1.6 m (owner question Q2).

**Moving parts.**
- Only the agitator shaft and its flanged coupling move, seen through the lantern windows.
- Axis vertical. Real speed 91-107 rpm on DIN glass-lined units (4-55 kW) [3V-CE]; anchors 20-60 rpm; large
  reactors 30-80 rpm (typ.).
- The motor fan turns under its cowl, as on the existing TEFC motors. Gearbox and impeller are not visible.

**Game simplifications and quads.**
- Estimate (calc): shell, jacket and heads 1-2k; deck, rails and ladder 2-5k; drive 1.5-2.5k (reuse the TEFC motor);
  nozzles with flanges about 1.2k; manway clamps about 0.7k. Total about 6-11k.
- Leave out first: nuts on small nozzles, jacket agitation nozzles, clamps beyond 8 on the manway, seal-pot
  instruments, then the lamp box.
- Leave out entirely: internal baffles and the impeller.
- Draw the half-pipe coil (stainless option) as stacked torus rings, not a helix: about 100 quads per ring.

## 2. Adsorption unit (D = bed vessel inner diameter)

**Definition.** Plants use three common forms:
(a) A TSA molecular-sieve dryer. 2 or 3 vertical beds (1+1, 2+1, 3+1 ...), used ahead of cryogenic, LNG and ethylene
units. Adsorption is downflow for 8-36 h. Regeneration is upflow with about 10 % of the product gas heated to
260-290 °C (320 °C maximum for 4A sieve). The regeneration loop has a heater, a cooler and a KO drum [CECA, GPN, JMC].
(b) A multi-bed hydrogen PSA. 4-16 vessels in two rows round a central valve skid at grade, plus a tail gas drum. Feed
at 6-40 barg, recovery 80-90 %. Examples [UOP]:
  - 10 beds: 65,000-73,000 Nm3/h.
  - 12 beds: 144,000 Nm3/h.
  - 16 beds: 192,000 Nm3/h.
(c) An activated-carbon adsorber. 2 vessels on legs with a valve "nest" between them. Calgon Model 10: D 3.05 m,
6.63 m tall, 7.95 x 3.38 m for the pair, 9.1 t of carbon each, 8.6 barg, 60 °C [Calgon].
- **Unit = the set of beds plus switching valves and headers.** A single bed cannot run continuously. The bed-to-bed
  flows (equalisation, purge, switching) are internal to the process. A set also keeps the external nozzles few,
  which helps the pipe-spacing rule. Alternative: one vessel per unit, with the player building the valves. Rejected
  (owner question Q3).
- **Recommend form (a)**: twin or triple TSA dryers. They read as "adsorption columns" and fit the gas-plant theme.
  - Form (c) is the same geometry, small, on legs, with no insulation and no platform. It can be a style of (a).
  - Form (b), PSA, is its own later kind: the tail gas drum and a skid of about 60 valves are heavy (Q4).

**Player parameters.**
- Main: D 1.0-4.6 m, step 0.2 m. 4-5 m is the shipping limit (typ.).
- Second: number of beds, 2-4. Default 2 (1+1); 3 = 2+1.
- Support height: skirt height, 1.5-3 m, default 1.5 + 0.25 D.
- Derived dimensions:
  - Bed height = clamp(2 D, 2.5, 9) m. Rule of thumb: bed L/D > 2, beds above 10 m are rare, superficial velocity
    ≤ 0.2 m/s, dP 0.2-0.6 bar [GPN, CECA].
  - Tangent-to-tangent = bed + 1.2 m (ceramic balls, silica-gel cap 0.3-0.5 m, inlet space) [CECA; typ.].
  - 2:1 heads.
  - Vessel pitch D + 2 m; unit width D + 3 m (headers, walkway).
- Check (calc): 100 MMSCFD at 62 bar on 2 towers at 0.125 m/s needs D ≈ 2.25 m and a bed about 3.4 m [JMC data].
- Envelope:
  - 2 beds, D 1.0: about 6 x 4 m, about 7 m tall.
  - 4 beds, D 4.0: about 24 x 7 m, about 16 m tall.

**Visible parts, outside in.**
- Tall vessels insulated in aluminium cladding (the beds cycle to 290 °C), with bands every 1-1.5 m, on skirts with
  an access opening.
- Top head: a manway/loading nozzle (DN 500-600), with a davit.
- Bottom head: an adsorbent unloading nozzle (DN 150-300), angled at 45°.
- Large-bore elbows from each top and bottom nozzle into a top header and a bottom header along the row.
  - On each header: per bed, 2 big actuated ball or butterfly valves (actuator boxes), plus small
    depressurisation/repressurisation valves.
  - The hot regeneration lines are insulated.
- Platform over the top heads, connected along the row, with a caged ladder. It serves the top valves and the
  loading manway.
- PSA look (later), from the UOP photos [UOP]:
  - Plain white vessels on short skirts. No platforms on the vessels.
  - Each top nozzle is a gooseneck pipe running down to the skid. Bottom nozzles go straight into the skid.
  - Skid at grade between the rows, about 2-3 m high.
  - Tail gas drum about 1.3 adsorber diameters across and taller than the adsorbers.

**Nozzles** (on the header ends only; size = fraction of D, minimum 0.05 m, cap 1 m):
- FEED, wet gas: top header, 0.10 D.
- PRODUCT, dry gas: bottom header, 0.10 D.
- REGEN_GAS in, hot: bottom header, opposite end, 0.06 D.
- REGEN_OUT, wet, to the cooler/KO drum: top header, opposite end, 0.06 D.
- NITROGEN, purge: 0.05 m.
- VENT, depressurisation to flare: 0.04 D.
- DRAIN: bottom header, 0.05 m.
- ADSORBENT loading and unloading: blind flanges, not pipes. They could become item ports.
- Internal: switching valves, bed-to-bed piping, distributors.
- Sizing (calc from [JMC]): at 62 bar and 15 m/s, a 0.5 m3/s feed needs about 0.09 D. A liquid carbon unit uses
  0.05-0.07 D (6-8" on 3.05 m) [Calgon].
- Spacing: put the FEED and REGEN_OUT ends on opposite ends of the top header, and the PRODUCT and REGEN_GAS ends on
  opposite ends of the bottom header. Top and bottom ends are then at least one free block apart.
- PSA (later): FEED 0.10 D, PRODUCT 0.08 D, TAIL_GAS from the drum 0.2 D (low pressure, 0.2-0.5 barg), NITROGEN,
  VENT.

**Supports and access.**
- TSA vessels stand on skirts on a ring foundation. Skirt 1.5-3 m, to give room for the bottom elbows, valves and the
  unloading nozzle.
- Carbon units stand on 4 legs; all their valves are at low elevation [Calgon].
- PSA vessels stand on short skirts and have no platforms. Reload is by crane [UOP photos].
- Game: one top platform per row, with rails and a caged ladder, at least 0.8 m clear and 2.1 m headroom under the
  top piping. A grade walkway along the bottom header.

**Moving parts.** None turn.
- TSA valves switch every 8-36 h; PSA valves every few minutes.
- At most, show valve position indicators as static state. Do not animate them.

**Game simplifications and quads.**
- Estimate (calc):
  - TSA, 2-4 beds: vessels about 1.5k each, valves about 0.3k each, headers, platform 3-5k. Total about 10-20k.
  - PSA, 12 beds, 60 valves, drum: about 35-45k.
- Leave out first: nuts on the elbow flanges, cladding bands beyond every 2 m, the small depressurisation valves,
  actuator detail (use a box + a cylinder), then the davit.
- Leave out entirely: internal distributors and support balls.
- Regeneration heater, cooler and KO drum: existing units, outside the adsorber (owner question Q5).

## Owner questions (each with a recommendation)
- Q1. Reactor form: the jacketed, top-driven DIN/stainless reactor, D 1-4 m (recommended), or the large
  bottom-driven polymer autoclave (D 4-5.5 m, 10 m tall, reflux condenser on the head)? Recommend the first now, the
  autoclave as a later kind if a process needs it.
- Q2. Reactor support: lugs on a built-in operating deck (recommended; plant-like), legs only for D ≤ 1.6 m.
- Q3. Adsorption unit = set of beds with internal switching valves (recommended), not one vessel.
- Q4. Adsorption form: TSA dryers, 2-4 beds (recommended). Carbon adsorbers as a style of TSA. PSA as a separate,
  later kind.
- Q5. TSA regeneration loop (heater, cooler, KO drum): external existing units connected by REGEN_GAS/REGEN_OUT
  (recommended; this matches real plants), not internal to the adsorber.

Sources:
- [3V-CE] 3V Tech "Glass lined reactor – CE" datasheet 240315 (DIN 28136; CE 1600-40000 dimensions, drives, impeller
  speeds, nozzles).
- [Pfaudler] GMM Pfaudler "DIN Reactors" web page (AE/BE/CE volumes, in-line drives, manways DN 500/600).
- [PVC] US 5,587,437 (≥ 100 m3, straight-side L/D ≥ 1.5, reflux condenser) and related PVC autoclave patents (50-200
  m3, bottom drive, H/D 2.4-3); Hüls 200 m3 reactor 5.5 x 10 m (conference paper, quoted through a search result).
- [CECA] Terrigeol and Trifilieff (CECA/Pall), "Practical considerations for the design of adsorbent beds".
- [GPN] Gas Processing News 2017-08, "Optimal design and operation of molecular sieve gas dehydration units, Part 1".
- [JMC] J.M. Campbell tip of the month 2015-11, "Two tower vs three tower system".
- [UOP] Honeywell UOP Polybed PSA revamp webinar 2023, slides 5, 9, 10, 12 and 18 (photos and unit data).
- [Calgon] Calgon Carbon MODEL 10 datasheet DS-MODEL1015-EIN-E1.
- typ.: Handbook of Industrial Mixing (standard tank geometry), ASME VIII-1 App. EE (half-pipe jackets).
