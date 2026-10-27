# FCC converter proportions: plan (2026-10-18)
Owner remark: "Regenerator is too big, reactor by itself does not have appropriate proportions". Research: `FCC_PROPORTIONS_RESEARCH.md` (16 data points, 13 sources). Prototype in `build/tmp/fcc/` (a copy of `FccConverterLayout`; nothing in `src/` changed). Figures: `figures/fcc-proportions-front.png` (D 4, 10.5, 17; one scale, grade aligned) and `figures/fcc-proportions-iso-10.5.png`.
## Real-life expectation first
- Diameters follow the flows; heights barely follow D. A regenerator is grid + a 3–6 m bed + freeboard + two-stage cyclones; a disengager (reactor) holds the riser end, cyclones and diplegs, so it is 2–3 times as tall as wide; a stripper is sized by 35–45 s of catalyst residence, 6–10 m long, not by D.
- Spent catalyst falls from the stripper bottom into the regenerator bed, so the reactor top stands above the regenerator top.
## What the code draws now
- Regenerator straight shell 14 + 1.3 D (tall at small D); reactor 1.3 times its own diameter (a squat drum); stripper 0.4 D across and 0.9 D + 3 m long (12.5 m at D 10.5): the long thin part under the reactor dominates its look.
## Forms (all keep riser OD 0.15 D + 0.1, riser 24–44 m, standpipes, valves, floors, 2:1 heads)
| Rule | Now | A (research) | B (A, wider reactor) |
|---|---|---|---|
| Regenerator straight shell | 14 + 1.3 D | 9 + 1.5 D | 9 + 1.5 D |
| Reactor OD | 0.6 D | 0.6 D | 0.75 D (data 0.41–0.97; the Chinese units 0.82–0.97) |
| Reactor straight shell | 1.3 OD | 6 + 1.2 OD | 6 + 1.2 OD |
| Stripper | 0.4 D x (0.9 D + 3) | 0.35 D x (5 + 0.3 D) | 0.35 D x (5 + 0.3 D) |
| Cone | 30°, (rr − rs)·√3 | same rule | same rule |
## Numbers at the default riser and support (probe `FccProbe`, every size 4–17 m)
| | Now | A | B |
|---|---|---|---|
| D 4: regen. top / reactor top, m; reactor H/OD | 28.2 / 38.6; 1.3 | 24.0 / 44.0; 3.7 | 24.0 / 44.9; 3.2 |
| D 10.5 | 39.3 / 46.4; 1.3 | 36.4 / 49.5; 2.15 | 36.4 / 51.6; 1.96 |
| D 17 | 51.4 / 62.9; 1.3 | 49.8 / 59.6; 1.8 | 49.8 / 65.2; 1.67 |
| Box at D 4 / 10.5 / 17 (height) | 41 / 51 / 70 | 46 / 54 / 66 | 47 / 57 / 72 |
| Quads at D 4 / 10.5 / 17 | 27.0k / 40.7k / 64.7k | 27.8k / 42.2k / 63.5k | 28.1k / 42.0k / 66.8k |
| Shortest riser at D 10.5 / 17 | 34 / 44 | 30 / 36 | 31 / 38 |
| Fitting (riser, support) pairs, all sizes | 2,217 | 2,717 | 2,537 |
- Both forms fit more cases than now (the shorter stripper gives the spent-catalyst standpipe more fall), and quads stay within 4 %.
- The stripper head depth in `fits` follows the stripper (rs / 2) instead of a fixed 0.1 D, which equalled it only at 0.4 D.
## Proposition
- A, the research rules: the reactor 0.6 D, closest to UOP's ratio and the smallest change. B, if the regenerator should look less dominant beside the reactor (within the measured range, 2–6 m taller overall).
- Optional, not in either form: a hemispherical regenerator top from D 14 (Fawley).
- After the choice: the rule in `FccConverterLayout` and its javadoc, the sweep (walk, climb, pipesApart, quads), the equipment gates, one in-game look, and a CHANGELOG line. Placed FCCs follow by the old-save refresh.
