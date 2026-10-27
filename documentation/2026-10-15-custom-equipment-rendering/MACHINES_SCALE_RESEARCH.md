# Pumps, reciprocating compressors, fired heaters at refinery scale (research, 2026-10-17)

Batch `2026-10-15-custom-equipment-rendering`, owner request "Allow larger pump, compressor, furnace. Check their real scale
in refinery." Opus research subagent, read-only. [n] = vendor sheet, standard or paper (list at the end); typ. = practice;
est. = my estimate from physics. Sizes in metres; d = pump discharge, b = compressor bore, n = throws or cylinders.

## Findings that change the mod's ranges

1. Larger discharge does not come from BB2: OH2 and BB2 both stop at 0.35–0.4 m (HPX 18", RPH DN 400, OHH 400 mm; HDX 16",
   BBS 14", RPHb 12"). Above 0.4 m refinery pumps are BB1 (axially split, to 1.2 m) or VS1. BB2 adds head, power, temperature.
2. Compressor bore 0.8 m and 6 throws are small: process frames reach 1.23–1.25 m bore and 10 throws; the largest refinery
   machines have 8 cylinders and 21 MW. The rule 900 − 600 b rpm gives 180 rpm at 1.2 m; real frames keep the mean piston
   speed at 3–4.5 m/s (rpm = 60 c / 2 s: about 280–340 rpm at a 0.4 m stroke).
3. VC radiant height: vertical radiant tubes are at most 18.3 m straight [22, 23]; the mod allows 2.75 D = 27.5 m at D 10.
   Cap the radiant height at about 19–20 m.
4. A 40 m single-cell cabin exceeds horizontal tube practice (12–24 m). Big box heaters are tall and narrow (H/W to 2.75
   [22]) and grow by cells (twin cell; 3–4 cell reformer heaters on one convection section).

## 1 Centrifugal pumps

Physics: P = ρ g Q H / η. Vendor nozzles are small: at the top of a range the discharge carries 6–9 m/s (Goulds 3700:
1,475 m³/h through 10" = 8 m/s); lines run 2–4 m/s. The mod's 3 m/s is a line velocity (a real 0.25 m OH2 passes 1,000–1,500 m³/h).

| OH2 line | Sizes | Q max m³/h | H max m | p max bar |
|---|---|---|---|---|
| Flowserve HPX [1] | 1–18 in | 5,000 | 350 | 80 |
| KSB RPH [2] | DN 25–400 | 4,150 (50 Hz) / 5,000 (60 Hz) | 270 | 110 |
| Sulzer OHH [3] | 25–400 mm | 2,250 | 370 | 75 |
| Goulds 3700 [4] | to 10" | 1,475 | 360 | 60 |

Practice (typ.): refinery OH2 mostly d ≤ 0.2–0.25 m and ≤ 300–500 kW; a BB pump costs 1.5–2 × an OH2 [5], so OH2 is used
to its limit first. 0.4 m OH2 sizes are low-head, high-flow catalogue sizes.

Above OH2 (API 610 types):
- BB1 axially split, double suction (cooling water, transfer): Goulds 3610 11,355 m³/h, 215 m [4]; Flowserve LNN 125–1,200 mm,
  30,000 m³/h, 300 m [6]; Sulzer ZPP 30,000 m³/h [7]. Nozzles side–side, horizontal, opposite each other below the split flange.
- BB2 radially split, 1–2 stage, centreline mounted (crude charge, residue, coker charge at 370 °C, 42 bar [8]): Goulds 3620
  4,540 m³/h, 455 m, 455 °C; 3640 2-stage 1,700 m³/h, 760 m [4]; Flowserve HDX 6–16", 4,100 m³/h, top suction and discharge
  [9]; Sulzer BBS 150–350 mm, 5,000 m³/h, 450 m [10]; KSB RPHb 80–300 mm, RPHbd 80–250 mm, 1,900 m³/h, 550 m, "top-top" [2];
  Trillium DDH 6,000 m³/h, 500 m [11]. Flowserve 10HDX27B: 14" × 10", 0.69 m impeller, 1,376 m³/h at 232 m, 3.2 t;
  4HDX14B package 3.2 × 1.25 × 1.22 m with motor [9].
- BB3/BB5 multistage (hydrocracker charge, boiler feed, 3–10 MW typ.): Goulds 3600 1,020 m³/h, 1,825 m [4]; Ruhrpumpen SM
  3–14" discharge, 3,000 m³/h, 420 bar [12].
- VS1/VS3 vertical (cooling-water basin): Sulzer SJT-CWP impeller to 1.92 m, 90,000 m³/h; Ruhrpumpen VS3 8–120 in [13].

Largest refinery pumps (typ.): cooling water 5,000–20,000 m³/h each, 30–60 m, 0.6–3 MW, d 0.6–1.2 m (BB1 or VS1); crude
charge (400 kbpd = 2,650 m³/h) on 2–3 BB2 in parallel, 10–16" discharge, 1–3.5 MW. Motors above about 1 MW are TEWAC/CACA
boxes 1.2–1.8 m tall, 2.5–3.5 m long, with a cooler box on top, not finned drums.

BB2 look (est., D2 = impeller ≈ 2.2–2.7 d): casing a heavy drum ≈ 1.8 D2 across, ≈ 1.1 D2 long (1.8 D2 two-stage),
centreline feet at shaft height both sides; both nozzles vertical on top, lines parallel [2, 14], suction one size up, flange
faces ≈ 0.9 D2 over the shaft; seal chambers and ribbed, fan-cooled bearing housings on brackets at both ends [2, 10];
bearing span ≈ 3.3 D2 (2.3 m at 10"); coupling guard; baseplate ≈ span + 0.5 + motor = 4.5–8 m for d 0.25–0.4 m, on a plinth.

Recommendation: keep OH2 at 0.05–0.4 m. For "larger", add a between-bearings form by d: BB2 top–top 0.15–0.4 m (hot
process, 0.1–4 MW) and BB1 side–side 0.3–1.2 m (water); one form could switch nozzles at 0.4 m.

## 2 Reciprocating compressors (API 618 balanced-opposed)

- Siemens/Dresser-Rand HHE-VL [15]: 16.8 MW, stroke 305–406 mm, up to 10 throws (odd allowed), bore 178–1,232 mm. Frame length
  L = 2.18 + 0.787 n m (2 → 2.97, 6 → 6.91, 10 → 9.27 m); width over cylinders 8.76 m with Type B distance pieces (+0.86 m
  C/D); rod removal space 2.44 m at each head; frame 1.37 m across in plan.
- Baker Hughes / Nuovo Pignone [16]: 300–750 rpm, piston 3–4.5 m/s, bore to 1,250 mm. HD 2–8 throws 10.4 MW 322 kN 700 rpm;
  HE 2–10, 28.7 MW, 670 kN, 800 rpm; HF 2–10, 34.6 MW, 1,140 kN, 514 rpm.
- Burckhardt [17]: rod load to 1,500 kN, 31 MW; BA frame 2–8 cranks, 9.5 MW, 550 kN; Assiut make-up frames 1,700 kN.
- Ariel (high-speed separable packages: fuel gas, net gas) [18]: KBZ 6.75" stroke, 2/4/6 throws 1.9/3.9/5.8 MW; KBV/KBB
  6 throws 7.5 MW at 900/750 rpm; KBK/KBT ≤ 4.1 MW. Neuman & Esser [19]: refinery packages to 6.5 MW, rod load 1,700 kN.
- Largest in a refinery [20]: Thai Oil Sriracha, 4 units (make-up and recycle): 8 cylinders, 21 MW motor, 11 × 15 m footprint,
  330 t, 9,150 m³/h H₂. Howden Numaligarh CCR net gas: 6 cylinders about 1 m across. Chart/Howden India: 6 throws, 7.4 MW.
- Summary: H₂ make-up/recycle 2–6 throws typ. (8–10 largest), LP bore 0.4–0.9 m typ. (1.0–1.25 max), stroke 0.3–0.45 m,
  300–500 rpm, 2–21 MW. Same-side cylinders are two pitches (≈ 1.6 m) apart: the biggest bores take outer throws or fewer
  throws. Power per throw ≈ 0.05 + 1.7 b² MW (est. fit: HHE-VL 1.7, Thai Oil 2.6 MW per cylinder).

Synchronous motor (API 546; WEG/EM 375–18,600 kW at 180–900 rpm [21]): forged flanged shaft bolted to the crankshaft, one
outboard pedestal sleeve bearing, brushless exciter outboard of it, stator on soleplates (stator shift), enclosures WP II,
TEFV, TEWAC (CACW) or TEAAC (CACA). ABB slow-speed frames: shaft height 710–2,500 mm, 1–65 MW; GE 930 kW at 200 rpm:
3.56 m wide, 1.73 m long, 16.8 t [21]. Est. (output coefficient ≈ 4 kVA·min/m³, checked on these): frame across ≈
0.3 + 1.1 (P_kW / rpm)^⅓ = 1.9 m (2 MW, 600 rpm), 2.9 m (5 MW, 400), 3.8 m (10 MW, 300), 4.8 m (20 MW, 300); length ≈ 0.45 ×
across + 0.4 (wider than long); shaft height ≈ 0.55 × across; cooler hood on top 0.25–0.35 × across tall, frame-long (CACW
water pipes at one end, CACA a fan cowl); pedestal 0.6–1.0 m wide; exciter 0.5–0.9 m across; terminal box 0.8–1.2 m on one
side; fabricated box or octagonal frame, ribbed, on a base frame.

One inlet, one outlet for a single-stage service (typ.; recycle machines are one stage at ratio 1.3–2):
- Each cylinder's suction bottle (or one per two adjacent same-side cylinders) sends a branch up to a suction header along the
  crankshaft over the bottles, one per side; the side headers join across the outboard (non-drive) end, where the single
  inlet drops in from the suction knock-out drum standing at that end.
- Discharge bottles feed one header per side at grade or in a trench under the bottles; both join at the same end into one
  line (PSV, check valve, cooler).
- Sizes (est., suction 12 m/s, piston 4 m/s): branch ≈ 0.5 b; suction header ≈ 0.5 b √n (0.7 b at 2, 1.0 b at 4, 1.3 b at 6
  cylinders); discharge header ≈ 0.7 × suction header at ratio 2. Bottles 1.0–1.6 b: headers are thinner up to 4 cylinders.

Support:
- Skids carry high-speed packages to about 7.5 MW: Ariel KBZ/6 (4.8 MW) on a 15 × 9.8 m skid in 3 pieces [18]. Skid rules [24]:
  taller beams over heavier ones, full-depth gussets under loaded flanges, skid filled with concrete on studs, grouted to a
  concrete foundation or piles; a 1.8 MW engine sits on W18x106 rails (0.47 m deep).
- Slow-speed API 618 above about 1–2 MW (typ.): concrete block 3–5 × the machine's mass, steel deck at frame-base level.
- Steel arrangement for the mod (est.): two main rails under frame and motor, W24 (0.6 m) to 2 MW, W36 (0.9 m) to 20 MW,
  1.0–1.5 m apart; a cross beam of the same depth under each crosshead guide; outboard beams under each cylinder support;
  bottle stands of W8–W10 posts; deck 1.0–1.5 m over grade so discharge bottles clear grade. For the biggest frames steel is
  a stylisation (real: concrete block).

## 3 Fired heaters (API 560)

Duties: VC to 40 MMkcal/h (46 MW), box 20–125 MMkcal/h (23–145 MW), arbor 17.5–100 MMkcal/h [25]; VC "commonly up to
40–45 MW" [26]; one heater to 150 MMkcal/h (174 MW) [27]. Est.: 200 kbpd crude heater 120–150 MW fired; vacuum 30–80 MW;
CCR reformer 3–4 cells on one convection section [28], 100–250 MW; hydrotreater 5–30 MW (VC).

Real geometry: 24 MW crude VC [29]: 60 tubes 194 mm × 17 m, tube circle 6.7 m (casing ≈ 7.7 m), 6 burners × 4 MW, L/D 2.5.
10 MW VC [30]: 42 tubes 6" × 10.6 m, circle 4.08 m. Vertical tubes ≤ 18.3 m, H/D ≤ 2.75 [22, 23]. 23.6 MW crude box heater,
Homs [31]: 20.0 × 4.8 × 19.65 m outside, 2 passes, 219 mm tubes 20.0 m effective. Coker twin cell [32]: 62 tubes per cell,
18.5 m tubes. Cabin H/W ≤ 2.75 [22].

Structure (no published member tables; est. from loads, legs 2–2.6 m apart as in the survey):
- Legs continue the casing columns from base plate to radiant top. VC D 3 m (≈ 40 t): 6 × W8x31 (HE200B); D 6–8 m
  (150–300 t): 8–12 × W12x65–96 (HE300B); D 10–12 m (400–700 t): 12–16 × W14x90–132 (HE400B–HE500B). Cabin: W12–W14
  every 2–3 m along both walls, the casing stiffeners between them.
- Fireproofing (typ.): legs concrete-encased up to the floor, 0.45–0.6 m square: the chunkiest detail.
- Floor: VC plate on radial W12–W18 (0.3–0.45 m) to a ring girder 0.4–0.6 m deep on the leg tops; cabin beams across the
  width, depth ≈ W/20 (0.3–0.6 m) at 1.5–2.5 m pitch, edge girders along the column rows.
- Bracing: VC X (L100–L150 angles or 100–150 mm pipe) in alternate bays, burner bays open; cabin X in end bays and every
  3rd–4th bay, 45° knee braces (0.6–1.2 m) in open bays as portal frames, K where an aisle passes.
- Base: 25–50 mm gusseted base plates on grouted pedestals 0.3–0.6 m over grade, 0.6–1.2 m square, 4–8 anchor bolts.
- Rigid look: columns straight from pedestal to roof; a deep ring or edge girder at the hearth; legs ≥ 0.3 m (0.5 m
  fireproofed); braces ≥ 0.15 m; floor 2.1–2.7 m up [33], so legs are short and stocky against the casing.

Nozzles (typ.):
- Inlets: one per pass (2–8) into the top of the convection section through its end header box, from an inlet manifold with
  a flow valve per pass, symmetric from the split [33]; coil size: 3–6" convection, 6–8" crude.
- Crossover: external insulated pipes from the convection's lower header box to the radiant inlet (VC: a ring on the roof at
  the tube tops; cabin: the top of the wall coil at the end header box).
- Outlets: VC near the floor (through the floor or the shell 0.5–1.5 m above it); cabin from the hearth tubes through the end
  header box; coil size, stepped up in vaporising service (vacuum outlet 8–16" from 4–6" inlets); transfer line ≈ pass
  size × √passes.
- Fuel: gas from a valve station at grade to a ring (VC) or two row headers (cabin) under the floor, burner leads off the top
  of the header [33]; header ≈ 0.02 √MW m (est., 20 m/s at 3 bar g: 3" at 10 MW, 6" at 50, 8" at 100); leads 1–2"; pilot gas
  0.5–1" on its own ring; fuel oil 1–2" with atomising steam beside it; snuffing steam 2–3" to firebox and header boxes,
  manifold ≥ 15 m away [33].

## Recommended maximum sizes for the mod

| Unit | Today | Recommended max | Real equipment |
|---|---|---|---|
| OH2 pump d | 0.4 m | 0.4 m (keep) | KSB RPH DN 400, Sulzer OHH 400 mm, Flowserve HPX 18" |
| BB pump d (new) | – | BB2 0.4 m; BB1 1.2 m | Flowserve HDX 16", Sulzer BBS 14"; Flowserve LNN 1,200 mm |
| Compressor bore | 0.8 m | 1.2 m | HHE-VL 1,232 mm, BH 1,250 mm, Numaligarh ≈ 1 m |
| Compressor throws | 2/4/6 | 8 (10 if wanted) | Thai Oil 8 cylinders 21 MW; HHE-VL, BH HE/HF 10 |
| Motor | 0.9 + 1.1 b + 0.1 n | by power, 1.9–4.8 m across | ABB shaft height to 2.5 m; Nidec stator 3.35 m |
| VC D | 10 m | 12 m | 24 MW at 7.7 m; VC to 45 MW |
| VC radiant height | 2.75 D | min(2.75 D, 20 m) | 18.3 m vertical tube limit |
| Cabin W | 12 m | 12 m per cell, H/W to 2.75 | Homs 4.8 m wide, 19.65 m tall; coker cell ≈ 5.5 m |
| Cabin L | 40 m | 25 m per cell, more as twin cell | 18.5–20.8 m tubes; box heaters to 145–174 MW |

Sources: [1] flowserve.com HPX. [2] KSB RPH, RPHb, RPHbd leaflets (puffer.com), ksb.com. [3] Sulzer OHH/OHHL E00697. [4] Goulds
3620 bulletin with ITT API range table (motralec.com); surplusrecord 3700 listings. [5] epcland.com API 610. [6] flowserve.com
LNN. [7] sulzer.com cooling water pumps. [8] Sulzer A10525 coker pump. [9] flowserve.com HDX; equipt.com 10HDX27B, 4HDX14B.
[10] Sulzer BBS E00584. [11] Trillium DDH BB2 12-2023. [12] ruhrpumpen.com SM. [13] Sulzer SJT-CWP E10225; Ruhrpumpen VS slides.
[14] KSB lexicon "top/top or side/side". [15] Siemens Energy HHE-VL flyer 06-2021. [16] Baker Hughes recip overview 03-2021.
[17] Burckhardt 2011 range release, product pages, Assiut 2021. [18] Ariel KBZ/KBU catalogue, compressortech2 KBK:T, KBZ/6
listing. [19] neuman-esser.com India. [20] Baker Hughes Thai Oil case; Hydrogen Central (Howden); chemindigest (Chart).
[21] WEG/EM synchronous motors for recip compressors; ABB/Baldor slow-speed pages; Nidec slides; surplusrecord GE 6524L.
[22] Esteem Projects fired heater design guidelines. [23] Technip fired heater design slides. [24] Beta/Wood "Optimized Skid
Design for Compressor Packages"; Turbomachinery Mag. [25] Kochi Refinery heater deck. [26] Verga VC heaters. [27] Thermax.
[28] US10384186 family (reformer heaters). [29] AIDIC CET 39 (2014) 889–894. [30] LinkedIn heater560 "How to design a fired
heater". [31] Al-Haj Ibrahim, Period. Polytech. Chem. Eng. 54/1 (2010) 33–40. [32] Coker heater improvements, DCU Galveston
2018. [33] red-bag BN-DG-C01G plant layout, fired heaters.
