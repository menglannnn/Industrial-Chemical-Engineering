# Column auxiliaries (condenser, reflux drum, ejector, PA coolers, reboiler): layout plan

Batch `2026-10-15-custom-equipment-rendering` · 2026-10-16 · decided by the owner the same day (below); built the same day, `COLUMN_AUXILIARIES_REVIEW.md`. Owner request: condenser (water or air cooled, required), steam ejector before it (optional), PA heat exchanger, reboiler (optional); "put on its side due to their size", connected pipelines, size scaling with the column "for now", purely visual. Owner 2026-10-16: "condenser needs to come with reflux drum". Read here: one PA cooler per pumparound of the tray layout (0 to 4).

## Physics first (D 3 m reference column)

- Vapour load scales with the column's cross-section: at F ≈ 2 Pa^0.5 and 3 kg/m³ about 1.15 m/s, 24 kg/s, so about 7 MW to condense. Every duty scales with D².
- Water-cooled condenser: at U 500 W/m²K and 30 K it needs about 490 m². A 1.25 m shell with 6 m tubes holds about 1,900 tubes, about 700 m², so it fits. Area goes as D_shell² · L. At a fixed L/D the shell goes as D^(2/3).
- Air-cooled: the overhead condenses near 120 °C against 35 °C air, so the air can warm about 35 K; at 3 m/s face air (3.5 kg/m²s) the face takes about 120 kW/m², so 60 m². That is 8.5 times the column's cross-section, which is why fin-fans stand on their own steel or on the pipe rack.
- Reflux drum: the condensate falls into it. The reflux pump lifts the reflux to tray 1, and a PA pump lifts the cooled liquid back above its draw.
- Ejector: its motive steam leaves with the overhead and condenses in the condenser, so the reflux drum needs a water boot. A real vacuum overhead has two or more stages and a 10 m barometric leg; one stage is the game simplification.
- Kettle: it is fed by gravity from the sump (sump level 2 to 3 m above grade, weir about 1.6 m) on any skirt. A vertical thermosiphon needs its top tubesheet at the sump level with 3 m tubes, so it needs a skirt of about 3 m or more.

## Proposed layout (figures)

- **Side structure** (`02-front-elevation`, `03-plan`): a steel structure on the column's right (−x), the side of the draws, 9 × 4 × 11 m for D 3, units lying along x.
  - Grade: PA coolers stacked two high, and the pumps.
  - 4 m floor: reflux drum.
  - 8 m floor: the water condenser. The air cooler has its own stand beyond the structure (decision 1).
  - Channels face away from the column, where a bundle pulls out.
- **Overhead line**: rises from the top head, crosses to the structure and drops in a down-leg to the condenser. The ejector stands upright in that down-leg on a frame extension, with motive steam from grade.
- **Kettle**: at grade on the left, along z. It is fed from the front bottoms nozzle and returns vapour to a new left nozzle between the sump level and the bottom tray.
- **Reflux line**: rises up the column's front, the piping side, to the reflux nozzle.
- **Utility ends**: cooling water, steam, distillate and bottoms end in blind flanges at the box edge (future ports).
- **Other figures**: `01-flow-scheme`; `04-condensers` (TEMA AES; forced-draft bay with plenum and fans); `05-ejector-reboilers` (ejector in suction diameters; kettle; thermosiphon on a 2 m skirt).

## Sizing rules (game form, scaling with D)

Overhead = the top nozzle (0.4 D, at most 1 m). Ejector 8.3 × suction. Condenser shell 0.65 D^(2/3) to 0.25 m, tubes 4.5 × shell. Reflux drum 0.6 D to 0.5 m (1 to 4 m), T/T 3 D. PA cooler 0.8 × condenser shell. Kettle bundle = condenser shell, kettle shell 1.6 × it. Structure as long as the longest unit plus 1 m, floors stacked from the units. Air cooler: see decision 5.

| D col | Overhead / ejector | Condenser shell × tubes | Reflux drum | PA cooler | Kettle shell / bundle | Structure L × W × H, floors | Air cooler bank (own stand) |
|---|---|---|---|---|---|---|---|
| 1 m | 0.40 m / 3.3 m long | 0.75 × 3 m | 1.0 × 3 m | 0.50 × 2 m | 1.25 / 0.75 m | 5 × 3 × 9 m, 3 and 6 m | 1 bay 2 × 5 m, 2 fans 1.6 m |
| 3 m | 1.00 m / 8.3 m long | 1.25 × 6 m | 2.0 × 6 m | 1.00 × 5 m | 2.00 / 1.25 m | 9 × 4 × 11 m, 4 and 8 m | 2 bays 3.5 × 9 m, 4 fans 2.8 m |
| 6 m | 1.00 m / 8.3 m long | 2.25 × 10 m | 3.5 × 11 m | 1.75 × 8 m | 3.50 / 2.25 m | 14 × 5 × 16 m, 6 and 12 m | 4 bays 5 × 12 m, 8 fans 4 m |
| 10 m | 1.00 m / 8.3 m long | 3.00 × 14 m | 4.0 × 12 m | 2.50 × 11 m | 4.75 / 3.00 m | 19 × 6 × 18 m, 7 and 13 m | 4 bays 5 × 12 m (capped) |

## Owner decisions 2026-10-16

1. **Arrangement**: the side structure, but "AC needs to be separated with the rest": the air cooler stands on its own frame, 1 m beyond the structure's outer end, in the same row.
2. **PA returns**: moved to the structure side, above their draws.
3. **Pumps**: drawn, a reflux pump and one per PA, at grade.
4. **Reboiler**: kettle.
5. **Air cooler size**: "find a reasonable size". Face = 8.5 × the column's cross-section (D 3: 60 m²), in bays of 2 to 5 m by 3 to 12 m tubes (tubes ≈ 2.4 × bay width), at most 4 bays of 60 m², two fans per bay (0.8 × bay width, at most 4.2 m). The bundle sits 2 m over the reflux drum's top (at least 8 m), so the condensate falls to the drum. From D 6 the bank stays at four 5 × 12 m bays (D 10 then has 36 % of the face; a plant would add banks).
6. **Ejector**: upright in the down-leg.
7. **Constructor**: a page of its own: Condenser (Water / Air), Ejector (Off / On), Reboiler (Off / Kettle). PA coolers follow the trays; the side is right.

After approval: a `ColumnAuxiliaries` layout beside `ColumnStructure`, drawn into the column's cells. The box widens by the structure and the kettle, and the floors and unit shells collide. The constructor protocol and NBT get bumped (fresh worlds). Tests: sight-line closure and box containment. The units are checked in game with the bridge.
