# FCC converter proportions: measured data and proposed rules (2026-10-18)

Batch `2026-10-15-custom-equipment-rendering`, follow-up to `NEW_UNITS_SURVEY_FCC_COKER.md` section 1 after the owner's remark
"Regenerator is too big, reactor by itself does not have appropriate proportions"; opus research subagent. [src] = source
(list at the end), calc = estimate made here: feed 0.92 t/m3, 8,400 h/a, catalyst/oil 6, regenerator bed at 0.9 m/s, so
D = 9.6 m x sqrt(kb/d / 50): D 4 = 9, 6.1 = 20, 9.6 = 50, 10.5 = 60, 13.6 = 100, 17 = 155 kb/d.

## Physics in short
- Diameters follow flows: regenerator (air, so feed), reactor (vapour and cyclone ring), stripper (catalyst flux round the riser).
- Heights barely follow D: regenerator = grid + 3-6 m bed + freeboard + two-stage cyclones of 1-2 m; disengager = riser
  termination + cyclones and diplegs (real ones are 2-3 x as tall as wide); stripper = 35-45 s residence, 6-10 m long.
- Spent catalyst falls from the stripper bottom to the regenerator bed, so the reactor top stands above the regenerator's
  (physics and layout; no measured elevation was found).

## Real data points
| Unit, form | Capacity | Regenerator | Reactor, stripper, riser | Source |
|---|---|---|---|---|
| Typical cross-flow regen. | 20 kb/d | ID 4.9 m (16 ft), dense bed 3.7-6.1 m | | [US5202294] |
| Same, larger unit | 50-60 kb/d | 9.1-11 m (30-36 ft), dense bed 3-4.6 m | | [US5202294] |
| Williams Memphis, UOP revamp | 69 kb/d | 7.3 m (24 ft), regenerator VSS; 7.3 m at 69 kb/d means ~2 m/s, so a combustor style (calc) | reactor 4.6 m (15 ft): 0.63 of regen., 0.41 of a bubbling-bed D (calc) | [OGJ-Williams] |
| Fawley (Esso), 1950s | large | 17 m, hemispherical top head, 20 cyclone pairs; new cyclones needed 1.6 m more straight shell | | [Fawley] |
| L&T shop vessels (Reliance) | world scale | up to 16.5 m x 45 m long, 1,320 t | | [L&T] |
| US unit, full-scale CFD | 20-24 kb/d (calc from 12.2 t/min circulation) | about 21.3 m (70 ft) high, 3 cyclone pairs | | [CPFD] |
| Khartoum refinery RFCC | not stated | 9.6 m, 26.2 m (T/T or overall not clear) | | [UofK] via survey |
| Two-stage regen. (R2R type) | not stated | 1st 10.4 m ID x 13.7 m T/T; 2nd 5.5 m ID x 10.5 m T/T | | [FX13] |
| Shengma 0.6 Mt/a RFCC | 12 kb/d (calc) | 6.6 m dense / 9.2 m dilute (widened top) | disengager 6.4 m, riser 0.8 m | [Shengma] |
| Chinese 1.6 Mt/a RFCC | 31 kb/d (calc) | | disengager 7.4 m x 20.6 m high, 2 primary + 4 single cyclones | [JCEU2019] |
| Chinese disengagers | not stated | | 8 m x 19 m overall (riser 1.56 m); 4.8 m x 13.4 m; 7 m x 13.4 m | [CJPE] |
| UOP/OMV reactor head | not stated | | top section 4.2 m x 13.5 m, 56 t | [Christof] |
| PCLA No. 1 (1942, upflow) | first unit | 5.9 m x 11.3 m | reactor 4.6 m x 8.5 m | [PCLA] |
| Review, typical unit | 40 kb/d | 10-15 m | riser 0.8-1.3 m across, 30-40 m long | [Fuel2011] |
| UOP strippers, commercial | | | flux 122-163 kg/m2.s (90-120 klb/h.ft2), residence 35-45 s | [AFPM-Q19] |
| Riser path | | | UOP: axial, up the stripper centre (annular stripper). Shell: external riser into one central primary cyclone | [US7250140], [axial] |

## Proposed rules (main size D = regenerator shell OD; current riser and support ranges kept)
| Part | Current rule: D 4 / 10.5 / 17 | Proposed rule: D 4 / 10.5 / 17 | Basis |
|---|---|---|---|
| Regen. straight shell | 14 + 1.3 D: 19.2 / 27.7 / 36.1 | **9 + 1.5 D: 15.0 / 24.8 / 34.5** | CPFD 21.3 m overall at about 6.2 m (fits); UofK -11 %; L&T 45 m at 16.5 m -7 % |
| Regen. heads, bed section | 2:1 heads, one diameter | keep; options: hemispherical top from D 14, a 0.72 D bed section | Fawley hemispherical; Shengma 6.6/9.2, no Western swage found |
| Reactor OD | 0.6 D: 2.4 / 6.3 / 10.2 | keep 0.6 D | data 0.41-0.97; 3.4-3.8 x riser, inside the 3-5 of [US5294331] |
| Reactor straight shell | 1.3 OD: 3.1 / 8.2 / 13.3 (H/OD 1.3) | **6 + 1.2 OD = 6 + 0.72 D: 8.9 / 13.6 / 18.2** (H/OD 3.7 / 2.2 / 1.8) | Chinese 13.4 m at 4.8 and 7 m, about 17 m straight at 7.4-8 m; PCLA 1.9 OD |
| Stripper OD | 0.4 D: 1.6 / 4.2 / 6.8 (0.67 reactor) | **0.35 D: 1.4 / 3.7 / 6.0** (0.58 reactor) | flux round the riser 117 / 88 / 82 kg/m2.s (calc), between old 40-60 and UOP 122-163 |
| Stripper length (cone) | 0.9 D + 3: 6.6 / 12.5 / 18.3 (cone 0.7 / 1.8 / 2.9) | **5 + 0.3 D: 6.2 / 8.2 / 10.1** (30 deg cone kept: 0.9 / 2.3 / 3.7) | 40 s at 0.15 m/s is 6 m (calc); not proportional to D |
| Riser OD | 0.15 D + 0.1: 0.70 / 1.68 / 2.65 | keep | calc 18 m/s outlet: OD 0.7 / 1.7 / 2.65; Shengma 0.8 m at 12 kb/d |
| Riser length, termination, axis offset | 24-44 m; end 0.05 into the shell; 0.7 D + 4 | keep (UOP VSS ends about 0.3 up; that would cost 4 m of reactor height) | review 30-40 m |

## Elevations, riser 32 m, regenerator bottom at its least allowed support (8 / 8 / 11 m)
| D | Current: regen. top / reactor top / stripper bottom | Proposed: regen. top / reactor top / stripper bottom |
|---|---|---|
| 4 | 28.2 / 38.6 (+10.4) / 27.6 | 24.0 / 44.0 (+20.0; a 24 m riser gives +12) / 27.5 |
| 10.5 | 38.3 / 44.4 (+6.1) / 20.3 | 35.4 / 49.5 (+14.1) / 23.9 |
| 17 | 51.4 / 50.9 (-0.5) / 13.9 | 49.8 / 55.6 (+5.9) / 21.1 |

## What visibly changes
- Reactor: a tall vessel (2-3.7 x as tall as wide) instead of a squat drum, 13.6 m instead of 8.2 m at D 10.5.
- Stripper: shorter and slimmer (8.2 x 3.7 m instead of 12.5 x 4.2 m at D 10.5); vessel + cone + stripper (24 m) now match
  the regenerator's straight shell. Regenerator 1.6-4.2 m shorter; regenerator/reactor volume 9.4 -> 5.1 at D 10.5.
- The reactor top stands 6-14 m over the regenerator top (today the large regenerator overtops it). The stripper bottom is
  2-7 m higher at D 10.5-17, so the spent-catalyst standpipe falls more; re-run `fits`, `minLength`, `minSupport`.
- Not found: ExxonMobil Flexicracking and Lummus sizes, any measured elevation (OGJ behind a bot check, not bypassed).

## Sources
- Patents: [US5202294] US 5,202,294 (Mobil 1993) text; search excerpts of [US5294331] vented riser, [US7250140] Shell
  reactor, [axial] axial-riser wording. OGJ search excerpts: [OGJ-Williams] "Fast-tracked FCC revamp meets project goals"
  (2000); [PCLA] "Fluid catalytic cracking hits 50 year mark on the run". [Fawley] Digital Refining "Regenerator head
  replacement project". [L&T] L&T FCC reactors/regenerators page. [Christof] Christof Group "FCC reactor head" page.
- [CPFD] Parker and Blaser, CPFD Software 2015, regenerator CO paper. [UofK] Ahmed et al., UofK Eng. J. 5(1) 2015. [FX13]
  Fluidization XIII two-stage regenerator paper and [Fuel2011] Fuel 2011 FCC review, search excerpts. [AFPM-Q19] AFPM Q&A
  "Question 19: FCC revamps commonly include technology". [Shengma] Sichuan Shengma 2023 acceptance report, table 2.3-1.
  [JCEU2019] J. Chem. Eng. Chinese Univ. 2019 (2) 008, 1.60 Mt/a disengager CFD. [CJPE] Chin. J. Process Eng., SVQS CFD.
