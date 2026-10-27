# Kit integration report (2026-10-18, K1-K5, P1-P6, G1-G2)

Physics first (G2): a cabin heater wall carries a walkway at each row of peep doors, and a caged climb stops at a landing
within 9 m (rule C1). The lanes left one ladder from the hearth walkway to the eaves (up to 22 m). The sweep also found
the grade ladder to a floor 10 to 16 m up (2450 failing shapes). Nothing is committed. Gradle ran compileJava/compileTestJava only.

## Items (ProcessUnit line numbers unless named)
- K1 `Mesh.bend` (Mesh:765) is a tangent polygon whose end pieces are half pieces square to the tangent. `ends` false
  hands them to the caller's legs: `pipe` (1562) and `legs()` (1597) do this; `elbow` passes true.
- K2 Pipe solids end by direction. `legs` drops legs under 1 cm. FCC feed and steam use `u.pipe` again; SteamTurbineTrain's filter is removed.
- K3 Tower corner posts are flush with the deck side (0.025 m in). K4 `ringAccess` keeps a piece a ladder starts or
  lands on, and the FB gasifier's span workaround is gone. K5 `fill`'s hidden face is gone.
- P1 `TowerPlan`, `towerPlan`, `stairTower(plan, ...)` (3233-3310) and `Access.Line.gap` replace 7 copies: centrifugal
  compressor, condensing turbine, coker, grate, FCC, FB boiler, fixed bed.
- P2 One rest piece, `climb(run, base, top, lower, upper, post)` (2913), now used by the cyclone, stirred reactor,
  adsorber, FB gasifier feed floor and cabin heater.
- P3 `post(..., plate)` uses BASE_PLATE 0.03 thick with a 0.06 margin (3618).
- P5 `pipe(r, sides, ...)` and `stack(..., n, ...)`. The FB boiler stack is back to 6 m OD at 48 sides (STACK_SIDES).
- P4 `valve`, `thermowell`, `reducer` (3553-3600) and a `squareDeck` ladder `hole` (3060). The new HydroprocessingParts
  (194 lines, owned by both reactors) holds the shell, platforms, top deck, top nozzle, trunnions and bands. Trickle bed
  goes from 424 to 287 lines, ebullated from 594 to 455. The adsorber and FCC reducers now use the kit. No code is identical across designs.
- P6 Removed: transition without axis, motor(f, x, y, ...), verticalLadder(a, ...), band without paint, radialSide
  without sign, saddle without floor, Access.deck without parts, and the unused PipeSizes.largestUnder.
- G1 The gate strips comments before scanning and lists HydroprocessingParts. G2 (2329-2368): side walkways at most 8 m apart,
  with side-step ladders at alternate ends; the grade climb rests on 0.15 m posts beyond the walkway when the floor is over 9 m.

## Not done / owner choices
- Kept as defaults (removing them means about 500 calls repeat a rule): box(4) (~170), Solid.rod(6) (~190), Solid.cone(7)
  (27), Mesh.head(6) (~40), line(3) (~40), pipe(4), front (17), platform without parts (~20). Remove them?
- brace(4) stays: 8 free-standing braces need their ends drawn. STACK_SIDES 48 (the pipe rule says 60).
  The base plate margin is now 0.06 everywhere (it was 0.09 on the adsorber and 0.07/0.08 on the ebullated reactor).
- The cabin heater box is 2-4 blocks wider on supports 10-16 (grade rest). KIT_API.md has an integration section.
  CHANGELOG and INDEX are untouched.

## Gates, counts (build/tmp/units/int/counts.md), nozzles
- `check.sh int` (22 kinds): 53 passed, 0 failed. Sweep (probe/Sweep.java) covered every size, length and support of the
  22 kinds, 19,638 shapes, checking walk/climb, pipesApart, free pipe blocks and quads under 80k: 0 failed.
- Worst quads: coker 10x2 s16 79,240 (pre 79,533); FB boiler 26x3 s8 78,949 (pre 79,168, 5.5 m stack); FCC
  16.5x44 s14 69,755; cabin 12x48 s16 67,263; FB gasifier 63,782. Every other kind is under 47k.
- Lowest-support rows: 67 of 118 changed, no box changed. Coker -1408, adsorber -686, fixed bed -648, FB boiler -626,
  ebullated -372, stirred -367, compressor -308, trickle -190, condensing -148, steam turbine -141, FCC +249,
  cyclone +318, cabin +3264 (walkways). Eight kinds are unchanged. FB boiler 24/25 m x 3/4 is 1 block deeper (6 m stack).
- Nozzles are identical in every dumped shape (each size at least/most length, lowest/highest support; dump-pre.txt vs dump-int.txt).
- Moving parts keep the same pivot, axis, speed, phase, crank, reach and bounds. Only the coker swivel hose has 22 fewer polygons (K1).

## Visible changes (int/render against pre/render; render-diff/)
- Elbows have no light seam bands (fcc_converter-*, cyclone-2.0-iso, condensing_turbine-*).
- The coker tower is recentred (delayed_coker-4.0-iso3). Tower flights and rail gaps are exact (compressor, condensing, grate, FCC, FB boiler).
- Rests have inner rails, end beams and plates (cyclone-2.0-iso, stirred_reactor-*, adsorber-*). Cyclone and stirred rest decks are 0.125 m lower.
- Cabin heater intermediate walkway (cabin_heater-12.0-iso); FB boiler 6 m stack (fluidized_bed_boiler-26.0-iso3); hydroprocessing reactors under 0.3 %.
