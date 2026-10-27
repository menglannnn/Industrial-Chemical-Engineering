# FCC converter proportions: review (2026-10-18)
Branch `claude/custom-equipment-rendering-805e7d` on 1c5fd421. Owner remark: "Regenerator is too big, reactor by itself does not have appropriate proportions"; research `FCC_PROPORTIONS_RESEARCH.md`; plan `FCC_PROPORTIONS_PLAN.md`; owner choice: form A (the research rules, reactor 0.6 D).
## Real-life expectation first
- **Diameters and heights.** Diameters follow the flows: air through the regenerator, vapour through the reactor, catalyst through the stripper. Heights barely follow D:
  - **Regenerator:** a grid, a 3–6 m bed, freeboard and two-stage cyclones.
  - **Reactor (disengager):** holds the riser end, cyclones and diplegs, so it is 2–3 times as tall as wide.
  - **Stripper:** 35–45 s of catalyst residence, 6–10 m long.
- **Elevations.** Spent catalyst falls from the stripper bottom into the regenerator bed, so the reactor top stands above the regenerator top.
## What the code did, and does now
- **Before.**
  - Regenerator straight shell 14 + 1.3 D, tall at small D.
  - Reactor 1.3 of its own diameter: a squat drum.
  - Stripper 0.4 D across and 0.9 D + 3 m long, 12.5 m at D 10.5: the long thin part dominated the look.
  - The datasheet repeated two of the rules (`14 + 1.3 * d`, `.4 * d`).
- **Now** (`FccConverterLayout`):
  - Regenerator 9 + 1.5 D.
  - Reactor 0.6 D across, straight shell 6 m + 1.2 OD.
  - Stripper 0.35 D across and 5 + 0.3 D long.
  - The cone is (rr − rs)·√3 (30° as before).
  - The stripper head in `fits` follows the stripper (rs / 2), where a fixed 0.1 D only equalled it at 0.4 D.
  - The datasheet reads the layout's numbers, so it cannot drift.
  - The constructor hints say 1.5 D + 9 m and the shortest riser of 25–36 m.
- **Unchanged:** riser OD, riser range, axis offset, standpipes, valves, floors and heads. The fit checks (`fits`, `minSupport`, `minLength`) apply as before.
## Numbers (default riser and bottom; `tools/new-units/fcc-proportions/probe-*.txt`)
- **Regenerator top / reactor top**, m:
  - D 4: 28.2 / 38.6 → 24.0 / 44.0.
  - D 10.5: 39.3 / 46.4 → 36.4 / 49.5.
  - D 17: 51.4 / 62.9 → 49.8 / 59.6.
- **Reactor height to diameter:** 1.3 → 3.7 / 2.15 / 1.8.
- **Box height:** 41 / 51 / 70 → 46 / 54 / 66.
- **Quads:** 27.0k / 40.7k / 64.7k → 27.8k / 42.2k / 63.5k.
- **Shortest riser** at D 10.5 / 17: 34 / 44 → 30 / 36 m. The default riser at D 10.5 is now 32 m (was 34).
- **Fitting (riser, bottom) pairs over all sizes:** 2,217 → 2,717. The shorter stripper gives the spent-catalyst standpipe more fall.
## Gates
- Gradle, one run: equipment.*, DeterministicMathSourceTest, UnitTextTest, ColumnStructureTest; 73 tests, 0 failures.
- Sweep (`tools/new-units/probes/Sweep.java`): 2,388 FCC shapes, 0 failed (walk, climb, pipesApart, quads). The most quads are 70,825, at 16.5 m with a 44 m riser and a 14 m bottom.
## In-game look
- **Setup.** A copy of the owner's review world on the bridge lane, stopped part way at the owner's word ("that's enough client testing"). No stairs walk, compare or reload was done.
- **Refresh.** The old-save refresh rebuilt both FCCs in place, and the regenerators stayed put:
  - `fcc_converter origin=123, -60, 171 shift=0,0 cleared=3017 placed=3243 blocked=0` (17 m).
  - `fcc_converter origin=-15, -60, 107 shift=0,0 cleared=1522 placed=1571 blocked=0` (10.5 m).
  - The other 33 structures were unchanged; the log is clean apart from vanilla warnings.
- **Look.** The shots (`tools/new-units/in-game/fcca-*.png`: fronts, isos, reactor tops, junctions, standpipe, valves) show tall reactors over a cone and a short stripper, with the regenerators below the reactor tops.
## Open (owner, optional)
- A hemispherical regenerator top from D 14 (Fawley), not built.
- Measured reactor-to-regenerator elevations were not found; the elevations above follow from the catalyst's fall.
## Tools
- `tools/new-units/fcc-proportions/`: form A and B layout copies, `FccProbe`, the fixed-scale renderer and `Montage`. Code guide: the FCC rules in the equipment chapter at the merge.
