### In-game check (2026-09-27)

- Build `5b577d1c` (`claude/phase-ports-compressor`), owner's Windows machine, JDK 21.0.11, `runMcpClient` lane with the
  `createcheme_mcp_compat` mixins, bridge minecraft-mcp 0.3.0 over HTTP only (no desktop input). Fresh creative superflat
  world (checkpoint format 6, `islands=0` at load), GUI scale 1 in the 854x480 window (see F3). Record, exact bridge calls
  and positions: `tools/phase-ports-probes/in-game/NOTES.md`; screenshots `01`-`13`, `client-latest.log`,
  `gradle-runMcpClient.log` in the same folder. Presence checks only, no timing.
- Built: scenario 1, a 1000 L N2 tank filled from a 130 kPa water generator (201.6 kg of water, 130 kPa), then the
  generator removed and 0.01 m pipes on its top, east and bottom faces to three 101325 Pa voids; later the generator put
  back on the west face. Scenario 2, the same tank fill with a pump on the east face facing away (to a void), then the
  pump moved under the tank facing down (0.001 m3/s, then a second tank with the default 0.01 m3/s). Scenario 3, two N2
  tanks with a compressor between them facing the second, then the suction tank replaced by a water generator.

| # | check | result | evidence (game time) |
|---|---|---|---|
| 1.1 | top line carries gas | PASS | tank `Top: gas outlet / drawing gas · 1.4 kmol/h` (327 s, `01`); top pipe phase ratio Vapor 100.0 %, N2 97.3 mol % + water vapour 2.7 % (522 s, `04`) |
| 1.2 | bottom line carries liquid | PASS | `Bottom: liquid outlet (head 1.9 kPa) / drawing water · 81.1 kmol/h` (327 s); bottom pipe Water 100.0 %, Vapor 0.0 % (397 s, `03`) |
| 1.3 | side line carries the bulk mixture (D11) | PASS | `Side: mixed outlet / drawing all phases together · 25.6 kmol/h` (327 s); side pipe Vapor 50.2 % / Water 49.8 % (537 s, `05`) |
| 1.4 | "Connections by face" lists each face's port | PASS at GUI scale 1; see F3 for scale 2 | three rows (Bottom, Top, Side), four with the feed (542 s) |
| 1.5 | level head at the bottom port | PASS | `head 1.9 kPa` at 199.6 kg, 5.4 kPa at 547 kg, 9.8 kPa at 996 kg (= g m / A, H = 1 m) |
| 1.6 | kg/h vs kmol/h toggle | PASS | 81.1 kmol/h -> `drawing water · 1241.4 kg/h`, top 24.0 kg/h, side 302.4 kg/h (342 s, `02`); table headers follow |
| 1.7 | outlet hint tooltip | NOT OBSERVABLE | the bridge cannot hover; the text is in `en_us.json` (`gui.createcheme.fluid.outlets.hint`) |
| 1.8 | compressor block renders, item name | PASS | light-grey body, oxidised-copper motor, joins along its axis (`13`); `Gave 1 [Process Gas Compressor]`, hotbar name, screen title "Process Gas Compressor" |
| 2.1 | pump on the tank's side refused with the typed reason | PASS | `FULL / INLET_WRONG_PHASE / ERROR: pump inlet not liquid (vapour 79.9 % by volume, from reservoir at 0, -56, 10)`, warning colour, flow 0, no limit (602 s, `06`) |
| 2.2 | rest of the island keeps running | PASS (weak) | tank `STEADY: no flow since 606.4 s` (632 s), no HELD; the island had no other flowing line |
| 2.3 | pump on the bottom face runs | PASS, then FAIL | `SOLVING / PUMP_TARGET (limit 481688 Pa on this fluid)`, 181.0 kmol/h (723 s, `07`); the island HELD at 792 s with water left (F1) |
| 2.4 | refused again when the water runs out | NOT OBSERVABLE | the island held before the tank ran dry, twice (F1) |
| 3.1 | compressor on gas to its ratio limit | PASS | `PUMP_TARGET (limit 130887 Pa at this suction)`, ratio 2.2 (763 s); `STEADY: no flow since 774.3 s / CLOSED (limit 105318 Pa at this suction)`, suction 52.7 kPa, rise 105.3 kPa, `Discharge / suction pressure 3.0` (798 s, `08`), the GameTest's numbers |
| 3.2 | ratio 1.5 accepted | PASS | `Applied`; `CLOSED (limit 26330 Pa at this suction)` = 0.5 x 52.66 kPa |
| 3.3 | ratio 10.5 refused by the screen | PASS | `Check numeric inputs and composition; amounts must be non-negative with a positive total.`; nothing sent, limit unchanged (`09`) |
| 3.4 | compressor fed water refused | PASS | `INLET_WRONG_PHASE / ERROR: compressor inlet not gas (condensed 100.0 % by mass, from generator at 0, -59, 20)`, warning colour (868 s, 878 s, `10`) |

Fluid log: 46 `status=HELD` lines on three islands (below); no exception and no ERROR from a createcheme class. The
compressor island never held.

**Findings.**

- **F1 (solver behaviour, blocks 2.4).** A pump drawing water from a tank's bottom face holds its island within seconds,
  with water still in the tank; reproduced twice on fresh tanks. At 0.001 m3/s: running at 723 s, `fluid_island=53`
  HELD from committed tick 15847 (792 s), `Interval substep limit; no partial interval may commit: advanced=2.855 of
  3.2 s, accepted=506, rejected=518, reasons={Newton iteration limit at residual #; active-set pass=#=518}, last=Newton
  iteration limit at residual 1.60e-9`; the committed tick crept to 795 s and stopped (about 120 kg of water left,
  estimated). At the default 0.01 m3/s: running at 1154 s, `fluid_island=81` HELD from 1160 s (6 s after the start, about
  60 kg drawn of 201.6 kg), the same reason plus `Backward-Euler state change #=2`, residual 1.63e-9, crept to 1164 s. The
  same tank drained through a plain 0.01 m bottom pipe (scenario 1, 327-452 s) did not hold, so the pump on the bottom
  port is the trigger. The logged last rejections all stall with the Newton residual at 1.1-1.8e-9.
- **F2 (solver behaviour).** Scenario 1's tank, fed again from the 130 kPa water generator with its three outlets open,
  held once it ran liquid-full: `fluid_island=36 committed_tick=14220 (711 s) status=HELD: Substep refinement exhausted:
  Newton iteration limit at residual 1.147e-9; active-set pass=0`, never advanced again. The held view (`11`): 996.0 kg,
  129.9 kPa, `Top: gas outlet / drawing liquid (overflow) · 66.4 kmol/h`. This is the "VAPOR port on a tank filled
  liquid-full" case (plan fixture 2, second half; D11 open item "recovered rejections at phase boundaries"), here not
  recovered.
- **F3 (GUI).** The tank's "Connections by face" is not drawn at all unless the scaled screen is at least about 323 px
  high: `renderOutlets` returns when `y + 38 > imageHeight - 55`, the tank rail reaches y = 214 after mass, and
  `imageHeight = max(250, height - 16)`. At 854x480 with GUI scale 2 (240 px) and at 1920x1080 with the automatic scale 4
  (270 px) the list is absent; at 1080p scale 3 (360 px) about two rows fit before "+N more". Derived from the code at
  `5b577d1c`; this check ran at scale 1 and did not capture the scale 2 screen.
- **F4 (GUI).** Two connections on different side faces both read `Side: mixed outlet`, with no face or neighbour, so a
  player cannot tell which row is which pipe (542 s: one `receiving · 1784.2`, one `drawing all phases together · 45.3`).
- **F5 (GUI text).** The ratio 10.5 refusal is the screen's generic message about numeric inputs and composition; it does
  not name the ratio or its 1.01-10 bounds.
- **F6 (GUI, cosmetic).** Roundoff shown as values: a dead-ended pump `Last flow -6.49e-18 kmol/h`; a closed compressor's
  island `STEADY: replaying 8.529e-17 kg/s since 831.6 s, next check at 78591.6 s`. Pipes with no flow against a void show
  `Mean pressure drop / length 1809.5 Pa/m` (the static end difference, read as a friction loss). The mass/mole choice
  resets to kmol when a screen is reopened.
- **Observations, no defect.** Voids do not feed back: once the tank fell below 101.3 kPa the top and side lines read
  `no flow` (452 s). A pump at its target flow shows a negative pressure change (-58.4 kPa, -44.6 kPa): the prescribed-flow
  pump throttles a line that would otherwise flow faster. The compressor's refusal names the generator's stream
  ("condensed 100.0 %") while its own suction junction still holds only N2 (its Water page is empty): the reason comes
  from the supply walk (A48), the rail from the junction.
- **Bridge limitations.** No hover (1.7); a screen opened before its first snapshot has no fields; the Create World name
  box ignored `type_text` (the world is "New World" in a game dir that had no saves).

**For the owner.**

1. F1: the liquid pump cannot drain a tank from its bottom face in game without holding the island; scenario 2's last
   check is open until it is fixed. Diagnosis (which row fails at 1.6e-9 and why the substeps shrink) is the next step;
   per the fix-assumptions rule, options to be presented before any change.
2. F2: the liquid-full tank with a top outlet holds; same diagnosis track (D11 open item 2).
3. F3: where the outlet list should go at the common GUI scales (move above the rail values, a page of its own, or
   scroll).
4. F4, F5, F6: wording and cosmetic items; keep or fix in a GUI pass.
