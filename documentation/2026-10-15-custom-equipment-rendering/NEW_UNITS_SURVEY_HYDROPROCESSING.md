# New units: trickle-bed and ebullated-bed reactors, engineering survey (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, step "new units". This survey was written on 2026-10-18 by an opus research
subagent. It covers two drawn-only hydroprocessing reactors: the fixed-bed downflow trickle-bed reactor, and the
ebullated-bed reactor with its external recirculation (ebullating) pump. Tags: [src] is a vendor sheet, patent or press
item (listed at the end); typ. is general practice; est. is my physics or proportion estimate. Dimensions are in metres
at 1 block = 1 m. D is the inside diameter of the reactor.

## 1. Trickle-bed reactor (hydrotreater / hydrocracker, fixed bed, downflow)

**Physics.** Oil runs down as films over 1.5–3 mm catalyst extrudates while hydrogen is the continuous gas phase. The
reaction gives off heat, so each adiabatic bed rises 15–40 °C (typ.). Cold recycle H2 is injected between beds (quench)
to cool the flow back down. This sets the shape: several beds of 3–8 m, separated by 1–1.5 m quench/redistribution
zones, in a tall vessel. Design pressure is 50–200 bar at 425–455 °C. Shell thickness is t ≈ P·R/(S − 0.6P). At 180 bar,
R = 2.5 m and 2.25Cr-1Mo-V (S ≈ 165 MPa), t ≈ 0.29 m, so hemispherical heads are the norm.
- **Forms.** (a) Refinery hot-wall reactor: heavy forged rings, hemispherical heads, 1–5 beds, insulated with aluminium
  cladding. This is the iconic form. (b) Small chemical trickle beds (aldehyde, butynediol, anthraquinone H2O2 and
  nitrile hydrogenation): 0.6–2.5 m, 10–60 bar, one bed, 2:1 heads, often on lugs in a structure. (c) Refractory-lined
  cold-wall reactors: obsolete. (d) Multitubular trickle beds: rare.
- **Recommendation.** Model (a). Its lower size range also stands in for (b). One head form (hemispherical) keeps the
  unit recognisable as a "reactor" and distinct from the 2:1 drums (owner question Q1).
- **Main size.** D from 1.0 to 5.0 m in 0.5 m steps. The real range is 0.6–5.5 m: L&T's forged reactors reach 5.5 m
  diameter, 318 mm wall plus overlay and 1500 t [src L&T]; the GE Massa hydrocracker is 5.5 m OD × 42 m and 1450 t
  [src GE].
- **Second parameter.** Number of beds N, from 1 to 4. Hydrotreaters have 1–3, hydrocrackers 3–5 per reactor.
- **Scaling.**
  - Shell thickness t = 0.05 D (minimum 0.03 m).
  - Bed height Hb = min(1.2 D + 1.0, 7.0) m.
  - Quench zone Zq = 1.0 + 0.1 D. Top zone (inlet diffuser and distributor tray) Zt = 0.8 + 0.1 D.
  - T/T = Zt + N·Hb + (N−1)·Zq. The catalyst support grid sits at the bottom tangent; inert balls fill the bottom head.
  - Insulation: 0.1 + 0.02 D. Skirt: same OD as the shell, height = support parameter, minimum D/2 + t + 1.5 m.
- **Overall sizes.**
  - D = 1, N = 1: T/T 3.1 m, about 9 m tall with a 2 m skirt and the davit, footprint about 3.5 × 3.5 m with the top
    deck.
  - D = 5, N = 4: T/T 33.8 m, about 48 m tall with a 5 m skirt, footprint about 8.5 × 8.5 m. This is close to GE's
    42 m vessel.
- **Visible parts, outside in.**
  - Tall clad cylinder (sheen), with faint insulation support bands every 3–4 m.
  - Hemispherical top head topped by one big central inlet nozzle. The inlet doubles as the manway for catalyst loading.
    It has a heavy Class 2500 RTJ flange: OD 2.0–2.3 × pipe OD, thickness 0.4–0.55 × pipe OD, 12–24 large studs (typ.,
    B16.5 Class 2500 proportions). This flange is the strongest top detail. A 90° elbow on top brings the feed in from
    the side.
  - Square top deck at the top tangent, 1 m beyond the OD, with rails and a pipe davit (reach about 0.6 D, about 3 m
    tall) for the inlet elbow, blind and loading hopper.
  - One platform ring at each quench level, reached by caged ladders.
  - Thermowell row: 2–3 short radial stubs (0.05–0.08 m) at the top, middle and bottom of each bed, each with a junction
    head. Multipoint assemblies pass up to 48 points through one 1" nozzle [src JMS], so few large nozzles and many
    small heads.
  - Vertical quench risers run up the shell to each interbed nozzle, with a control-valve station at each platform.
  - Bottom head inside the skirt: outlet nozzle at the centre, and 1–2 catalyst dump nozzles angled 45° down through
    sleeves in the skirt. Optionally one 45° side dump nozzle at the foot of each bed (common on hydrocrackers).
  - Two lifting trunnions near the top tangent (0.15 D across, 0.12 D proud). Skirt with 2 access openings, a vent row
    and fireproofing.
- **Nozzles (ports).**
  - FEED: oil and H2 already mixed, top-head centre, 0.15 D (maximum 1.0 m, minimum 0.15 m). It ends after the elbow,
    horizontal at deck level.
  - HYDROGEN: real plants join the H2 to the feed upstream. Recommendation: draw a tee on the feed elbow with its own
    port, at least 2 blocks from FEED (Q2).
  - EFFLUENT: bottom-head centre, 0.15 D. It turns 90° inside the skirt and leaves horizontally through a skirt opening
    0.8–1.2 m above grade.
  - QUENCH: one port at skirt level, 0.05 D (minimum 0.1 m). The header and the N−1 risers are internal to the unit.
- **Drawn only, not ports.**
  - Catalyst dump nozzles, about 0.04 D, blind-flanged. Catalyst is changed at turnarounds, not on line.
  - Thermowells, vent and drain (under 0.05 m or no flow).
- **Supports.**
  - Cylindrical skirt welded to a forged ring at the bottom tangent, with anchor chairs on a concrete octagon or ring
    pedestal.
  - Real 30–45 m reactors are often reached from a shared stair tower (typ.). The game uses caged ladders and platforms.
- **Moving parts.** None.
- **Quad budget.** Shell and heads cost about 3k, platforms and rails 10–20k, ladders 5–10k. Leave out first: insulation
  bands, side dump nozzles, trunnions, then thermowell heads (keep one row per bed). Keep the top flange studs: they
  are the signature.

## 2. Ebullated-bed reactor (H-Oil / LC-Fining) with its ebullating pump

**Physics.** Residue and H2 flow upward through a bubble-cap distributor grid. The upflow expands about 0.8 mm extrudates
30–50 % above the settled height (typ.). The bed is back-mixed and nearly isothermal, so there are no quench beds; the
feed preheat controls temperature. Gas-free liquid overflows into a recycle cup near the top. It runs down a central
downcomer to the ebullating pump, which returns it to the plenum under the grid [src US 7,815,870]. Catalyst ages
uniformly and is replaced on line, about daily [src US 5,215,653]. The pump sets the bed height through its speed:
400–1800 rpm on a VFD [src patent, search abstract]. Head is 10–100 psi, typically 50 psi (3.5 bar); recycle is 1–10
times the feed [src US 5,389,238].
- **Pump physics check (est.).** Fluidising a 5 m bed takes about 4–5 cm/s × 19.6 m² ≈ 1 m³/s. At 3.5 bar that is
  0.35 MW of hydraulic power, so 0.45–0.6 MW at the shaft. This matches Flowserve's 300–800 hp range [src Flowserve].
- **Forms.**
  - H-Oil (Axens): the pump is "located external to the reaction chamber", and the recycle returns through a bottom port
    with a bubble cap [src US 7,815,870].
  - LC-Fining / LC-MAX (CLG): the same patent draws the pump "at the bottom of the reactor".
  - Real ebullating pumps are always a separate pressure machine "provided directly at the reactor", with the "pump
    arranged above the drive" [src Sulzer].
  - Recommendation: one form, the pump coaxial under the bottom head, so the "special external pump" reads clearly.
    The alternative is the pump beside the skirt at grade (Q4).
- **The pump.**
  - Vertical sealless machine: process side above 180–200 bar and above 400 °C (460 °C example), drive near 60 °C
    [src Sulzer, Flowserve].
  - Drive: canned motor (Sulzer) or oil-filled wet motor (Flowserve Byron Jackson PR: 300/450/600/800 hp, more than 90
    units since 1967, unspared).
  - Barrier/flush oil is injected at high pressure, cooled in a closed circuit through an external heat exchanger, and
    leaks into the process at 20–30 L/h [src Sulzer]. The supply comes with redundant HP oil-injection and conditioning
    systems and redundant VFDs [src Flowserve].
  - So: no motor fan, no coupling, no visible shaft.
- **Main size.** D from 2.5 to 6.0 m in 0.5 m steps. Real commercial reactors are 3–6 m ("at least 5 m" in modern units
  [src Ebullated bed reactor modeling paper (ResearchGate)]). Older examples: 3.4 m × 21.7 m overall [src US 5,215,653,
  99 ft²/71.2 ft]; 4.0 m × 18 m T/T [src patent example]. Pilot reactors are 1.2 m.
- **Weights.** IOCL Panipat H-Oil: 2 × 1016 t, Cr-Mo-V [src L&T]. HPCL Visakh LC-MAX: 3 × 2260 t (reports range
  2200–2313 t), wall about 250 mm [src L&T; secondary article]. These are the heaviest reactors built.
- **Second parameter.** T/T from 4 D to 7 D, default 5 D, in 1 m steps.
- **Scaling.**
  - Shell thickness t = 0.055 D (0.14–0.33 m).
  - Grid at the bottom tangent + 0.1 D. Expanded bed top at about 0.6 T/T.
  - Recycle cup: rim at 0.85 T/T, funnel 0.55 D across. Downcomer 0.15 D on the axis (est.).
- **Overall sizes.**
  - D = 3: about 35 m tall, footprint about 8 × 6 m.
  - D = 6: about 55–60 m tall, footprint about 12 × 9.5 m, including the catalyst withdrawal vessel beside the skirt.
- **Pump bay (est.).**
  - Pump and motor stack Hp = 1.2 + 0.5 D.
  - Pump casing: forged drum of OD 0.25 D (minimum 0.9 m), 0.2 D tall.
  - Bolted joint ring 1.15 × casing OD, with 24–32 studs: the signature.
  - Motor pressure housing: OD 0.18 D (minimum 0.7 m), length 0.35 D + 0.8 m, below the casing.
  - Barrier-oil cooler: small horizontal shell-and-tube (0.3 × 1.5 m) on a bracket. Oil-injection tubing to a small skid.
  - Bay height Hs = D/2 + t + Hp + 2 m, which leaves room to drop the motor for removal: 6.5 m at D = 3, 9.5 m at D = 6.
- **Recycle loop (internal to the unit, any size).**
  - Suction: from the bottom-head centre nozzle (0.15 D), straight down into the top of the casing.
  - Discharge: from the side of the casing, horizontal, then 90° up into a plenum nozzle 0.3 D off the axis (0.12 D).
  - All joints get heavy flanges. This visible loop under the reactor is the recognisable feature.
- **Visible parts, outside in.**
  - Very fat clad shell, few shell nozzles.
  - Top head: EFFLUENT nozzle with an elbow, the catalyst addition line, and the top deck with a davit and the catalyst
    addition vessel.
  - On one side: a vertical row of 4–6 nuclear density-gauge detector boxes that track bed level (typ.).
  - One mid-height platform and the top deck. Caged ladders; real units this tall have a stair tower.
  - Tall skirt with 2 large openings (0.6 D wide), exposing the pump. The catalyst withdrawal vessel stands at grade
    beside it.
- **Catalyst handling (on line).**
  - Fresh catalyst goes from a hopper to an HP addition vessel (lock hopper, purged with oil, pressured to the reactor
    [src US 5,215,653 citing Engle]). It enters as a slurry through a tube that passes through the top head
    [src US 7,815,870].
  - Spent catalyst leaves from the base of the expanded bed through the grid and the bottom head [src US 7,815,870] to an
    HP withdrawal vessel. It is then depressured, washed and cooled into bins.
  - Lines are 3–4" with metal-seated severe-service ball valves (typ.).
  - Both vessels: OD 0.3 D (minimum 1.0 m), 0.6 D (minimum 2 m) T/T, 2:1 heads; the withdrawal vessel has a cone bottom
    (est.).
- **Nozzles (ports).**
  - EFFLUENT: top centre, 0.12 D. It is a mixed vapour/liquid stream that goes to the hot separator.
  - FEED: 0.08 D. HYDROGEN: 0.06 D. They join in a tee under the head; the combined line is 0.12 D into the plenum,
    opposite the recycle return. Each leaves horizontally through a different skirt opening, 1.5 m above grade, at least
    2 blocks apart.
  - CATALYST_IN: top of the addition vessel, 0.1 m.
  - CATALYST_OUT: low side outlet of the withdrawal vessel, 0.1 m.
- **Drawn only, not ports.**
  - There is no QUENCH: the bed is isothermal.
  - Thermowells, density gauges, and flush-oil and cooling-water lines.
- **Supports.**
  - Skirt anchored to a massive pile cap, carrying up to 2300 t plus water fill (typ.).
  - The pump stands on its own steel frame in the bay. Its suction joint takes the downward growth of the bottom head:
    about 15 mm, est. from 2.8 m × 12e-6/K × 430 K.
- **Moving parts.** None visible. The canned/wet motor hides rotation, so no animation is recommended (Q5).
- **Quad budget.** The shell is cheap. Platforms, rails and ladders dominate, then pump studs and flanges. Leave out
  first: density detectors, the oil skid and cooler, then the vessel davit. Keep the pump joint ring and the recycle loop.

## Owner questions (with recommendation)
- **Q1.** One trickle-bed form with hemispherical heads at every size, or switch to 2:1 heads for small chemical beds?
  Recommendation: one form, no switch.
- **Q2.** Separate HYDROGEN port on both reactors, joined to FEED by a drawn tee (real plants mix upstream), or one FEED
  port? Recommendation: the tee with two ports.
- **Q3.** Ebullated-bed CATALYST_IN / CATALYST_OUT as real solid-flow ports? Recommendation: yes, because they are
  continuous on-line flows. Trickle-bed dump nozzles stay decoration.
- **Q4.** Ebullating pump coaxial under the bottom head inside an open skirt bay, or beside the skirt at grade?
  Recommendation: coaxial under the head. This is inferred from Sulzer's "directly at the reactor, pump above drive",
  not from a plot plan.
- **Q5.** The real pump shows no rotating parts. Recommendation: keep it static.

## Sources
- Sulzer, US 2017/0122324 A1 / CA 2944273, "Pump drive unit for conveying a process fluid" (canned motor, barrier oil,
  ≥ 200 bar, > 400 °C).
- Flowserve, Byron Jackson PR "Ebullated Bed Recycle" product page (vertical oil-filled, 300–800 hp, VFD, oil injection).
- US 7,815,870 B2 (H-Oil vs LC-Fining description). US 5,389,238 (head, recycle ratio, seal oil and VSD).
  US 5,215,653 (example reactor, daily inventory check).
- L&T news: IOCL Panipat H-Oil reactors (2024); LC-MAX for HPCL (2021); L&T refinery products page (forged reactor
  limits).
- GE press release on the Massa hydrocracker reactor.
- Andritz and Topsoe reactor-internals pages; JMS multipoint thermometer sheet.
