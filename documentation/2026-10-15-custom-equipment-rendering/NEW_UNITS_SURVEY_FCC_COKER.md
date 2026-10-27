# New units survey: FCC reactor-regenerator, process cyclone, delayed coker (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, step "new units". This survey was written on 2026-10-18 by an opus research
subagent. It defines three drawn-only units from plant practice: what they are, the player parameters, the visible parts,
nozzles, supports, moving parts and quad budget. Tags: [src] is a named source (listed at the end), typ. is general practice,
calc is a sizing estimate made here from physics. The rules are 1 block = 1 m, 1 px = 62.5 mm, a pipe outside diameter of
0.05 to 1.0 m, and one free block between the pipe blocks of two nozzles. Larger flows are internal to the unit.

## 1. FCC reactor and regenerator (main size D = regenerator shell OD)

**Definition**
- Forms:
  - Side-by-side (UOP, Shell, Lummus, ExxonMobil, Axens/S&W) [WP-FCC]. The reactor (disengager over a stripper) sits on a
    vertical riser beside the regenerator. Two inclined standpipes with slide valves join the two vessels.
  - Stacked (KBR Orthoflow) [WP-FCC]. The reactor stands on the regenerator on a skirt. Catalyst flow is controlled by plug
    valves with their stems below the regenerator [US4851196]. Modern Orthoflow units have an external riser.
  - Two-stage regeneration for resid (Axens/TechnipFMC R2R) [Axens]. A lower first regenerator burns 40-80 % of the coke in
    partial combustion. Air lifts the catalyst to a second regenerator, which is refractory-lined and has external cyclones.
- Recommendation: model side-by-side with a single-stage bubbling-bed regenerator (UOP/Shell look). It is the most common
  form. The silhouette reads clearly: a fat, short regenerator, a tall slim reactor on a riser, and two slanted standpipes
  with box valves. All of these parts already exist in the mesh kit. Stacked and R2R would be later variants (Q1).
- What one unit includes:
  - In the unit: the riser with its wye, the feed injectors, the stripper, the reactor, the regenerator, both standpipes with
    their slide valves, the air inlet with the start-up air heater, the first stretch of the vapour line and of the flue gas
    line, and the structure.
  - Separate units: the air blower, the flue gas train (orifice chamber, third-stage separator, expander, CO boiler,
    precipitator), the catalyst hoppers, the catalyst cooler and the main fractionator.
- Operating conditions [WP-FCC]: reactor about 535 °C and 1.7 bar, regenerator about 715 °C and 2.4 bar. Catalyst
  circulation is about 5 kg per kg of feed. Riser contact time is 2-4 s.
- Cold-wall construction (typ.): the vessels have 100-150 mm of internal refractory. The shells are bare outside, run at about
  150-250 °C and carry aluminium or grey heat-resistant paint. The vapour line is hot-wall, insulated and aluminium-jacketed.

**Player parameters**
- Main size D = regenerator shell OD, 4 to 17 m in steps of 0.5 m.
  - Physics: the air flow follows the coke burn, which follows the feed rate. The dense bed runs at a superficial velocity of
    0.6-1 m/s (2-3 ft/s) [vel patents]. So D grows with the square root of the capacity. At 50 kb/d, D is about 9.6 m (calc).
  - Anchors:

    | Unit | Regenerator diameter | Other | Source |
    |---|---|---|---|
    | 20 kb/d cross-flow unit | 4.9 m (16 ft) | | [US5202294] |
    | Williams revamp | 7.3 m | reactor 4.6 m | [OGJ-Williams] |
    | 50-60 kb/d unit | 9.1-11 m (30-36 ft) | | [US5202294] |
    | Stanlow | 14 m | | [Stanlow] |
    | Fawley | 17 m | 20 cyclone pairs, 700 t head | [Fawley] |

- Second parameter: riser length L, measured from the wye to the termination inside the reactor. Range 24 to 44 m in steps of
  1 m, default 32 m.
  - Physics: the contact time of 2-4 s is fixed, so L does not scale with D. Modern risers are 30-40 m [riser review]; the
    classic length is 120 ft [riser patent].
- Support height = elevation of the regenerator bottom tangent, 6 to 14 m.
- The generator places the wye at about 3 m and keeps both standpipes within 45° of vertical, so that catalyst flows. If the
  layout cannot meet 45°, the generator raises the regenerator.
- Regenerator:
  - Straight shell (tangent to tangent) = 14 + 1.3 D: 19 m at D 4, 26.5 m at D 9.6, 36 m at D 17. A CFD case gives 26.2 m
    at 9.6 m [UofK].
  - Heads: 2:1 dome on top (0.25 D) and 2:1 bottom head.
  - Optional older look: a lower bed section of 0.85 D with a 30° swage. Leave it out by default.
- Reactor (disengager): OD 0.6 D (Williams: 0.63 D), straight shell 1.3 × its own OD, 2:1 heads.
- Stripper: OD 0.4 D, length 0.9 D + 3 m, with a 30° cone up to the reactor. Calc: at 9.6 m this gives a catalyst flux of
  about 45 kg/m²·s, which is in the normal range.
- Riser: OD 0.15 D + 0.1 m, which is 0.7 m at D 4, 1.5 m at D 9.6 and 2.7 m at D 17. Sources give ID 20-70 in
  [riser patent], 0.8-1.2 m [riser review], and a feed zone of 1.6-3 m in large units.
- Standpipes: OD 0.09 D + 0.15 m, which is 0.5 to 1.7 m.
- Riser axis offset from the regenerator axis: 0.7 D + 4 m.
- Overall envelope (vessels plus platforms; structures in real plants are 80-200 ft tall [support patent]):

  | D | Footprint | Height to the top of the vapour line |
  |---|---|---|
  | 4 m | about 14 × 8 m | about 34 m |
  | 9.6 m | about 23 × 14 m | about 50 m |
  | 17 m | about 34 × 22 m | about 66 m |

**Visible parts, outside in**
- Regenerator: the fattest vessel on site and squat, with a straight shell of 2.1 to 4.8 D. It has a plain dome, 2-4 manways
  at bed level and in the dilute phase, and a ring of 6-12 small stubs at bed level (thermowells, torch oil, catalyst addition).
- Air inlet: a horizontal air line runs under the vessel and rises into the centre of the bottom head. On the way it passes
  through a vertical direct-fired start-up air heater (typ.): shell 2.5 × the air line diameter, 3-4 × as tall, with a burner
  box on one side.
- Riser: a long plain tube with 2-3 bellows rings.
  - At the foot is the wye, where the regenerated-catalyst standpipe joins.
  - Signature part: 4-8 feed injectors, 2-4 m above the wye, set radially and pointing about 30° upward. Each has a feed line
    and a steam line from ring headers (typ.).
- Reactor and stripper: a slim, tall body on top of the riser.
  - The stripper has a steam ring header with 2-4 drops to it.
  - The vapour line leaves the top head: a large insulated pipe that turns 90° on long-radius elbows over the structure edge.
- Standpipes: two slanted pipes. They carry small aeration taps (skip) and bellows.
  - Each has a slide valve near its low end (typ.).
  - Valve body: a flanged box about 1.6 × 2.2 × 1.3 pipe OD.
  - On one side: a bonnet and a stem tube 1.5-2 OD long.
  - Actuator: an electro-hydraulic cylinder 0.4 × 2 OD, with a hydraulic power unit cabinet (1 × 0.8 × 1.2 m) on the
    platform.
- Flue gas line: a refractory-lined pipe of 0.2 D leaves the regenerator crown, turns at an elbow and runs off the structure.
- Structure: a steel frame of 4-6 columns with 6-9 m bays around the reactor and riser.
  - Platforms at the feed injectors, at each slide valve, at the stripper steam ring, at the reactor manway and on top.
  - Ring platforms at the regenerator manways.
  - A monorail or davit on top for cyclone maintenance.

**Nozzles** (fraction of D, capped at 1.0 m, minimum 0.05 m)

| Role | Place | Size |
|---|---|---|
| FEED | header end at the riser-base platform | 0.03 D (0.12-0.5 m). Calc: 0.27 m at 50 kb/d, 2 m/s |
| STEAM | lift, dispersion and stripping header at the riser base, other face | 0.02 D, at least 0.1 m |
| AIR | end of the air line at grade beside the air heater | drawn at 0.12 D (calc: 0.5 m at D 4, 2.0 m at D 17) |
| FUEL | start-up air heater gas | 0.08 m fixed |
| FLUE_GAS | end of the drawn flue gas line at the unit edge, high | drawn at 0.2 D |
| VAPOR | end of the vapour line, top, opposite heading | drawn at 0.17 D (calc: 1.6 m at D 9.6, 25-30 m/s) |
| CATALYST_IN | fresh catalyst into the regenerator bed | 0.08 m |
| CATALYST_OUT | equilibrium catalyst withdrawal, regenerator bottom side, away from the riser | 0.12 m |

- AIR, FLUE_GAS and VAPOR are drawn at their true size and end in a reducer to a nozzle capped at 1.0 m. The cap applies from
  D 8.3 for AIR, D 5 for FLUE_GAS and D 5.9 for VAPOR (Q2).
- Internal flows: both standpipes, the riser, the cyclones and diplegs, the plenum, the air grid and the aeration. The slurry
  recycle joins FEED.
- Pipe spacing: AIR is low under the regenerator, FEED and STEAM are on opposite faces of the riser base, and VAPOR and
  FLUE_GAS are at the top on opposite headings.

**Supports and access (typ.)**
- Regenerator: on lugs and a ring girder at the bottom tangent, or on a short skirt, standing on a raised RC table (4-8 columns
  of 1-1.5 m) so that the air line enters from below.
- Reactor: hung on lugs from the steel frame.
- Riser: guided. Standpipes: bellows.
- Access: a stair tower to the top (big-structure rule), rails 1.07 m high.

**Moving parts**
- None worth animating. The slide valve stems move a few mm, and the air blower is a separate unit.

**Simplifications and quad budget**
- Estimate 15-35k quads. The 17 m shells need 48-64 segments. As on the heaters, the structure (grating, rails, stairs) is
  the largest share.
- Leave out first:
  - aeration taps and thermowell stubs;
  - bolt rings on valve bonnets and on large manways (keep the flange discs);
  - secondary platforms.
- Never cut the feed-injector ring, the slide valves, the standpipes, the vapour line elbow or the air heater.
- Not drawn because they are internal: the cyclones, plenum, air grid and riser termination.

## 2. Process cyclone (main size D = body diameter)

**Definition**
- Forms:
  - (a) Single reverse-flow cyclone with a tangential rectangular inlet: spray dryer, mill, biomass and pneumatic-conveying
    service.
  - (b) N identical cyclones in parallel on common inlet and outlet manifolds. At a fixed inlet velocity, the cut size grows
    with the square root of D, so several small cyclones separate better than one large one (typ.).
  - (c) In series: a high-throughput primary followed by a high-efficiency secondary.
  - (d) Multicyclone, for example the FCC third-stage separator.
    - Construction: 50-100+ small swirl tubes in one casing, in a vessel up to 26 ft (8 m) [TSS patents].
    - Duty: removes 70-90 % of the particles [WP-FCC]; the underflow is 0.5-3 % of the gas [TSS patents].
    - Look: from outside it is a plain lined vessel with a cone.
  - (e) Cement preheater cyclones up to about 8 m. These are part of a tower and are not standalone.
- Recommendation: one unit is form (a), with a count N that gives form (b). Series (c) is made by piping two units together.
  The multicyclone (d) is a later, separate unit (Q3).
- Standard proportions [Leith & Mehta; Koch & Licht 1977]. Columns are inlet height a, inlet width b, outlet diameter De,
  vortex finder depth S, cylinder height h, total height H and dust outlet B, all divided by D:

  | Design | a/D | b/D | De/D | S/D | h/D | H/D | B/D |
  |---|---|---|---|---|---|---|---|
  | Stairmand high efficiency (default) | 0.5 | 0.2 | 0.5 | 0.5 | 1.5 | 4.0 | 0.375 |
  | Lapple general purpose | 0.5 | 0.25 | 0.5 | 0.625 | 2.0 | 4.0 | 0.25 |
  | Stairmand high throughput | 0.75 | 0.375 | 0.75 | 0.875 | 1.5 | 4.0 | 0.375 |

- Inlet velocity 15-25 m/s; pressure drop about 8 inlet velocity heads (typ.).

**Player parameters**
- Main size D = body diameter, 0.3 to 2.0 m in steps of 0.1 m.
  - Standalone cyclones in real plants run from 0.1 to 4 m (typ.).
  - The cap of 2.0 m keeps the gas outlet (0.5 D) within the 1.0 m pipe limit. Larger flows use N.
- Second parameter: count N = 1-4, in a row at a pitch of D + 1.5 m. This pitch keeps the pipe blocks of neighbouring
  nozzles apart.
- Support height = elevation of the dust outlet, 1 to 8 m.
- Shell: cylinder 1.5 D, then a cone of 2.5 D down to the dust outlet of 0.375 D.
- Gas outlet: a pipe of 0.5 D. It reaches 0.5 D inside (hidden) and stands 0.4-0.6 D above the roof, ending in a 90° elbow.
- Inlet:
  - A rectangle 0.5 D high × 0.2 D wide. Its top is flush with the roof and its outer wall is tangent to the shell.
  - A duct of about 1 D, with a round-to-rectangle transition of 0.7 D.
- Below the cone: a dipleg about 1 D long, or a conical hopper when D is 1 m or more.
- Rotary airlock: flange B, at least 0.15 m. Housing 1.6 B square and 1.2 B tall, with a side gearmotor.
- Overall envelope:

  | D | N | Footprint | Height |
  |---|---|---|---|
  | 0.3 m | 1 | about 1 × 1 m | about 3 m |
  | 2.0 m | 1 | about 4 × 3 m | about 15 m on a 4 m stand |
  | 2.0 m | 4 | about 14 × 4 m | about 15 m |

**Visible parts, outside in**
- Signature: the cone (2.5 D) is longer than the cylinder.
- The side inlet duct with its rectangular mouth, and the outlet pipe with its elbow above the roof.
- A flanged joint between cylinder and cone, so the cone can be removed (typ.).
- 1-2 bolted inspection hatches.
- In dust duty, an explosion vent panel on the roof or the cylinder (typ.).
- 3-4 support lugs at the cylinder-cone junction.
- The rotary valve with its gearmotor.
- A set of N reads as a row of cyclones under common headers.

**Nozzles**

| Role | Place | Size |
|---|---|---|
| GAS_IN | round flange at the end of the transition | 0.36 D (equivalent area of the 0.5 × 0.2 D inlet); with N, the manifold end is 0.36 D √N, capped at 1.0 m |
| GAS_OUT | top, after the elbow | 0.5 D; with N, 0.5 D √N, capped at 1.0 m |
| SOLIDS_OUT | under the rotary valve, facing down | B = 0.375 D, at least 0.1 m |

- Internal: the vortex and the dipleg.
- Pipe spacing: when D is under 0.6 m, turn the outlet elbow to the heading opposite the inlet.

**Supports and access (typ.)**
- The lugs rest on a 4-post braced frame, or the cyclone hangs in a structure.
- When D is 1.2 m or more: a platform with rails at the roof level, reached by a ladder (caged above 3 m).

**Moving parts**
- Rotary airlock: the rotor is inside, so show a turning shaft end or coupling at 20-30 rpm. Dust-collector airlocks of 6-12 in
  run at 28-29 rpm, driven by a right-angle worm gearmotor [Rotolok].
- Alternative valve: a double flap valve whose counterweight levers rock ±15-20° every few seconds (typ.). The rotary valve is
  the default.

**Simplifications and quad budget**
- Estimate 1-3k quads per body.
- Leave out first: the bolts on the hatches, then the explosion vent when D is small.

## 3. Delayed coker drum set (main size D = coke drum OD)

**Definition**
- Delayed coker [WP-DC; Ellis & Paul]:
  - The heater feeds a drum pair through a switch valve. One drum fills in 12-24 h while the other is steamed, quenched,
    unheaded and cut. A typical cycle is 16 h.
  - Large units have up to 4 pairs (8 drums), with one heater per pair.
- Fluid coking and Flexicoking: licensed by ExxonMobil only, 18 units in 1998 [WP-DC]. They consist of a fluid-bed reactor
  and burner, plus a gasifier for Flexicoking, and look like an FCC. They could reuse FCC parts later. Not recommended.
- Recommendation: one unit = 1-2 drum pairs together with the RC table, the steel structure, the cutting deck, one derrick
  per drum, the switch valve, the overhead vapour header and the chute (Q4). The heater, fractionator, blowdown system, jet
  pump, coke pit and bridge crane stay separate.

**Player parameters**
- Main size D = drum shell OD, 4 to 10 m in steps of 0.5 m.
  - Sources: drums of 4-9 m [Ellis & Paul], up to 10 m [WP-DC]; the largest ID built is 9.8 m [Bechtel].
  - Petrobrazi drums are about 6 m × 30 m and weigh about 200 t each [Petrobrazi].
- Second parameter: number of drum pairs, 1-2.
- Support height = elevation of the bottom flange, 8 to 16 m (default 12).
- Drum shell:
  - Straight shell = min(29.3, 2.5 D + 8) m, capped from D 8.5. The typical straight side is about 25 m [Ellis & Paul]; the
    limit is 29.26 m [Bechtel].
  - Top head: 2:1, 0.25 D.
  - Bottom cone: about a 30° half-angle, 0.87 (D − 2) tall, down to a bottom flange of about 2.0 m. Sources give a bottom
    flange of 2 m [Ellis & Paul] or 6-7 ft [unheading patents].
  - Top flange 1.2-1.5 m [Ellis & Paul; unheading patents].
  - Overall drum height: 21 m at D 4, 39 m at D 9.8. The maximum is 43 m [WP-DC].
- Layout:
  - Drum pitch D + 4 m; drums of both pairs stand in one row.
  - Structure length = 2N (D + 4) + 2 m; width D + 5 m.
- Derrick: a square lattice with a base of 0.35 D (at least 2.5 m). Height = drum height + 5 m, so that the stem clears the
  drum. Sources give about 40 m [derrick patent].
- Overall envelope:

  | D | Pairs | Footprint | Height to the derrick top |
  |---|---|---|---|
  | 4 m | 1 | about 18 × 9 m | about 60 m |
  | 9.8 m | 1 | about 30 × 15 m | about 95 m |
  | any | 2 | double the length | same |

**Visible parts, outside in**
- Derricks: one per drum, and the tallest objects in a refinery (typ.).
  - Each derrick has a crown sheave block and a crosshead running on 2 guide rails.
  - The drill stem is 5-6 in extra-heavy pipe (0.14-0.17 m, 2-3 px) and hangs in the centre.
  - A large high-pressure hose loop (API 10,000 psi) runs from a gooseneck about halfway up to the rotary joint and air
    motor on the crosshead [Ellis & Paul].
  - A winch sits at deck level.
- Cutting deck (top): full grating, with stem guides, winches and the top unheading devices.
  - Modern units use an enclosed hydraulic slide-type top unheading device [Coking 101]. It is a flat box 2-3 flange widths
    long, with cylinders.
- Drums:
  - Tall cylinders with 100 mm of insulation under an aluminium or stainless jacket, showing jacket seams [Ellis & Paul].
  - Three small nuclear level detectors up the side [Ellis & Paul].
  - On top: a PSV [Ellis & Paul], 1-2 vapour outlets of 0.3-0.6 m, and bends into a common overhead header with a quench
    injection [Ellis & Paul].
- Bottom of each drum:
  - The cone ends at the 2 m flange.
  - Below it hangs the bottom unheading valve: a wide flat slide-gate body about 3.5 flange diameters long with its actuators,
    and a side-entry feed spool [Coking 101].
  - Below that, an enclosed telescoping chute.
- Structure:
  - A massive RC table with columns of 0.15 D (at least 1.2 m) up to the skirt base. The skirt is welded to the lower shell
    and rests on the table [US8221591].
  - The switch deck (unheading deck) lies just under the table [Coking 101]. The table carries the switch valve.
  - Above the table: a steel frame with 2-3 intermediate decks and lateral guides at the upper drum [US8221591].
- Chute and pad: the chute ends at a pit or pad edge. Coke leaves the pit by bridge crane or front-end loader [Coking 101]
  (a separate unit).

**Nozzles** (fraction of D, capped at 1.0 m)

| Role | Place | Size |
|---|---|---|
| FEED | switch valve inlet at table level, one per pair | 0.03 D (0.15-0.3 m; the inlet nozzle is 15-30 cm [Ellis & Paul]) |
| VAPOR | end of the overhead header at the edge of the top deck | min(0.08 D, 1.0 m) (0.3-0.6 m per drum; one refinery has two 0.76 m lines in parallel [Ellis & Paul]) |
| QUENCH | coker gas oil into the header, at a separate point | 0.06 m |
| STEAM | purge, stripping and warm-up | 0.02 D, at least 0.1 m |
| WATER | quench water, at table level | 0.03 D |
| BLOWDOWN | from the overhead lines to the blowdown system | 0.05 D |
| CUT_WATER | from the jet pump; rises up the structure to the hoses | 0.15 m fixed (2.8-4.7 m³/min at 86-275 bar [Ellis & Paul]) |
| RELIEF | PSV outlet header, top | 0.04 D |
| SOLIDS_OUT | chute toe; coke and cutting water together | 1.0 m port (Q5) |

- Internal: the transfer lines to each drum, the drum overhead lines and the switch valve.
- Pipe spacing: FEED, WATER and STEAM go on separate faces of the table. VAPOR, BLOWDOWN and RELIEF go at the top on separate
  headings.

**Supports and access (typ.)**
- Foundations: piled RC table, with steel framing above.
- Access: one stair tower per pair to the cutting deck; rails 1.07 m high. An elevator is common in modern units; it is
  optional here.

**Moving parts**
- Real cutting, from Ellis & Paul unless noted:
  - The stem rotates at 4-6 rpm. Another source gives 8-12 rpm [cut patent].
  - The stem descends 0.5 m per revolution, which is about 2-3 m/min. It works a 4 m band up and down.
  - The pilot hole is about 1 m across and takes 15-20 min. Cutting takes 3-4 h; Bechtel gives 3-4 h of stem time for a
    9.8 m drum [Bechtel].
- Game: animate one drum per pair (the drum being cut).
  - The stem turns at 5 rpm.
  - The crosshead, stem and hose end move on a slider-crank with a 4 m stroke and a period of about 2 min.
  - The unheading valves stay static.

**Simplifications and quad budget**
- Estimate 20-45k quads per pair at D 9.8.
  - Each derrick is about 120-150 bars (X-bracing in 3-4 m panels).
  - Decks and rails come next.
- Leave out first:
  - the level detectors and the jacket bands;
  - the partial intermediate decks and the elevator;
  - the cut-water riser detail.
- Never cut the derricks, the drill stems with their hoses, the unheading valves or the chute.

## Owner questions (recommendation first)
- **Q1, FCC form:** side-by-side with a single-stage regenerator only. Stacked (Orthoflow) and R2R would be later variants,
  if ever.
- **Q2, lines over 1 m (FCC VAPOR, FLUE_GAS and AIR):** draw them at their true size and end each in a concentric reducer to
  a connectable nozzle capped at 1.0 m. The alternative is a large blind stub with a separate 1.0 m nozzle.
- **Q3, cyclone:** D of 0.3-2.0 m with N of 1-4 in parallel on manifolds. Series is made by piping two units. The
  third-stage separator or multicyclone would be a separate later unit (a lined vessel with a cone and an underflow line).
- **Q4, coker unit:** 1-2 drum pairs with the table, structure, derricks and chute. The heater, jet pump, blowdown system,
  pit and bridge crane stay separate. A later "coke handling" unit could add the pit and crane; until then the chute ends at
  a pad edge.
- **Q5, coke outlet:** a SOLIDS_OUT port of 1.0 m at the chute toe, carrying coke and cut water together.
- **Q6, FCC second parameter:** riser length (24-44 m) rather than a regenerator stage count.

## Sources
- [WP-FCC] Wikipedia, "Fluid catalytic cracking": conditions, circulation, licensors, third-stage separator.
- [WP-DC] Wikipedia, "Delayed coker": drum sizes, pairs, fill time.
- [US5202294] US patent 5,202,294: regenerator diameter versus capacity.
- [OGJ-Williams] Oil & Gas Journal, "Fast-tracked FCC revamp meets project goals" (search excerpt).
- [Fawley] Digital Refining, "Regenerator head replacement project".
- [Stanlow] Stanlow regenerator head, trade press search excerpt.
- [UofK] University of Khartoum Engineering Journal, CFD study of a regenerator.
- [riser patent] FCC riser patents (classic 120 ft and 20-70 in ID; newer feed zone 1.6-3 m, US 9,376,633 family).
  [riser review] a 2015 review. Both are from search excerpts.
- [vel patents] FCC regenerator patents (US 5,032,251 family), search excerpts.
- [support patent] separate-vessel FCC patent: structures 80-200 ft (search excerpt).
- [US4851196] US patent 4,851,196: stacked FCC with plug valves.
- [Axens] Axens R2R pages and P+ portfolio.
- [TSS patents] third-stage separator patents (US 5,464,528 / 6,902,593 / 8,025,717 family), search excerpts.
- [Leith & Mehta] cyclone design table; [Koch & Licht 1977] via powderprocess.net.
- [Rotolok] dust-collector rotary airlock page.
- [Ellis & Paul] P. J. Ellis and C. A. Paul, "Tutorial: Delayed Coking Fundamentals", AIChE 1998 (Colorado School of Mines
  copy).
- [Bechtel] Bechtel BETS pages on drum height and diameter limits.
- [Coking 101] J. Adams, Adams Project Managers, "Coking 101" (deck names, DeltaGuard, crane).
- [US8221591] US patent 8,221,591: coke drum support system.
- [unheading patents] coke drum unheading patents (US 4,726,109 / 6,039,844 family): flange sizes (search excerpts).
- [derrick patent] decoking patent: derrick about 40 m on the top platform (search excerpt).
- [cut patent] Chinese flexible-pipe decoking patent: 8-12 rpm, winch 0.4-5 m/min (search excerpt).
- [Petrobrazi] Energy Industry Review: drum replacement.
