# In-game check of the phase-ports and compressor batch (2026-09-27)

Branch `claude/phase-ports-compressor` at `5b577d1c`, worktree `.claude/worktrees/phase-ports-compressor`, no source edit.
Owner's Windows machine, JDK 21.0.11. Every step went through the minecraft-mod-mcp bridge's HTTP endpoint
(`node %TEMP%/minecraft-mcp-skill/scripts/bridge.js ...`); no desktop input of any kind was used.

## Lane

- Before launch: `tasklist | findstr java` showed the idle Gradle daemon and a timing campaign of the
  jdk-determinism-perf agent (`timing.sh`, fluid suite, then column probe). Waited until the machine had been free of
  test/perf JVMs for 60 s (18:17:34), then launched.
- `JAVA_HOME='C:/Program Files/Java/jdk-21.0.11' JAVA_OPTS=-Xshare:off ./gradlew.bat --no-configuration-cache runMcpClient --offline --console=plain`
  in the background, log `gradle-runMcpClient.log` (compileJava UP-TO-DATE, compileMcpCompatJava, prepareMcpControlMod,
  runMcpClient; BUILD SUCCESSFUL after Quit Game, 23 min 38 s).
- Bridge jar `run/mcp-client-mods/minecraft-mcp-1.21.1-neoforge-v0.3.0.jar` present (912248 bytes); the client log lists
  `CreateChemE MCP Test Compatibility 1.0.0 (createcheme_mcp_compat)`.
- `run/mcp-client/options.txt` written before launch with **`guiScale:1`** (plus `pauseOnLostFocus:false`,
  `onboardAccessibility:false`, ...). Reason: at GUI scale 2 in the 854x480 window the tank page cannot draw
  "Connections by face" at all (finding F3 in `REVIEW_SECTION.md`), so every screen here is at scale 1 (GUI units =
  framebuffer pixels).
- Fresh world: the game dir `run/mcp-client/` had no `saves/` before this run. Created at 18:19:27 as "New World"
  (the bridge's `type_text` into the Create World name box did not replace the name), Game Mode Creative, Allow Commands
  ON, World Type Superflat, Generate Structures OFF (checked on screenshots before Create). Log:
  `fluid_world status=LOADED format=6 online_tick=0 islands=0`. Gamerules doDaylightCycle / doMobSpawning false and
  difficulty peaceful confirmed in the log. Player spawned at (3.5, -60, 8.5); the superflat floor's top block is y = -61.
- Shut down: `pause_game`, `click_button_index 8` (Save and Quit; log "All dimensions are saved" 18:41:07),
  `click_button_index 6` (Quit Game). Afterwards the only java.exe were the idle Gradle daemon and a new timing JVM of
  the jdk-determinism-perf agent (started 18:41:37, after the quit); no game JVM left.
- Overlap note: the jdk-determinism-perf agent wrote its logs `30-kernel-sweep-1e6.txt` (18:23), `31-...` (18:24),
  `32-fluid-suites-candC.txt` (18:24) and `40-final-fluid-suite.txt` (18:29) while this client was running
  (18:17:50-18:41:20); timings from that window may be skewed by the game.

## Text entry: the exact bridge calls (all worked at GUI scale 1, compat lane)

Device screens were opened with `chat "/tp @s x y z yaw pitch"` then `use_item` (standing on the block, pitch 89) or
`right_click` (looking at it horizontally from a stone platform). Field centres from `enumerate_widgets`
(framebuffer pixels = GUI units at scale 1):

```
generator pressure: click {"x":147,"y":119,"button":0}; hotkey {"keys":"key.keyboard.left control,key.keyboard.a"}; type_text {"text":"130000"}; click_button_index {"index":6}   (Apply changes)
pipe diameter:      click {"x":147,"y":107,"button":0}; hotkey (ctrl+a as above); type_text {"text":"0.01"};  click_button_index {"index":4}
pump suction flow:  click {"x":147,"y":119,"button":0}; hotkey (ctrl+a); type_text {"text":"0.001"}; click_button_index {"index":4}
compressor ratio:   click {"x":147,"y":161,"button":0}; hotkey (ctrl+a); type_text {"text":"1.5"};   click_button_index {"index":4}
                    click {"x":147,"y":161,"button":0}; hotkey (ctrl+a); type_text {"text":"10.5"};  click_button_index {"index":4}
mass/mole toggle:   click_button_index {"index":2}   ("Mole % · kmol" <-> "Mass % · kg")
tank Vapor page:    click_button_index {"index":6};   compressor Water page: click_button_index {"index":8}
```

Each Apply read "Waiting for the next simulation update." at once and "Applied" after the next engine bucket (about 5 s).
Chat commands from Git Bash with `MSYS_NO_PATHCONV=1`; block placement through `bridge.js cmds cmds/<file>.txt` (the files
in `cmds/` are the exact command lists, in order).

Pitfalls met:
- A device screen opened before its island's first snapshot has no fields (4 widgets): the typed value went nowhere and
  `click_button_index 6` answered "index 6 out of range". Wait for the view (about 5 s), reopen, then type.
- `bridge.js shot $PWD/x.png` from Git Bash with `MSYS_NO_PATHCONV=1` wrote to `D:\d\...` (the MSYS path is not
  converted); use an absolute `D:/...` path. The stray file was removed.
- `tp ... facing x y z` aimed at the tank instead of the pipe under it twice; stone platforms at the right height and a
  horizontal look (pitch 0) were reliable.
- No bridge tool hovers, so tooltips (the tank's outlet hint) cannot be shown.
- Standing under a pipe to look up at it stops working once a void is placed below it (the player is inside the void).
- A heredoc containing this file's text failed in the agent's Bash wrapper (quote parsing); files were staged in `%TEMP%`
  and copied.

## World layout (all `setblock`)

| scenario | block | position | settings |
|---|---|---|---|
| 1 | fluid_generator G1 | (-2,-57,0) | water, 130000 Pa (GUI), 25 C; removed 18:24:47, re-placed and set again 18:27 |
| 1 | fluid_pipe (feed) | (-1,-57,0) | default 0.05 m; removed with G1, re-placed 18:27:55 |
| 1 | fluid_reservoir T1 | (0,-57,0) | placement default: N2, 101325 Pa, 25 C, 1000 L |
| 1 | fluid_pipe top / east / bottom | (0,-56,0) / (1,-57,0) / (0,-58,0) | diameter 0.01 m each (GUI, "Applied", field 0.01000) |
| 1 | fluid_void x3 | (0,-55,0) / (2,-57,0) / (0,-59,0) | default 101325 Pa, placed 18:24:48-51 |
| 2 | fluid_generator G2, pipe, T2 | (-2,-56,10), (-1,-56,10), (0,-56,10) | G2 130000 Pa; G2 and its pipe removed 18:30:35 |
| 2 | fluid_pump [facing=east], pipe, void | (1,-56,10), (2,-56,10), (3,-56,10) | default 0.01 m3/s, 500000 Pa; placed 18:29:17, removed 18:30:35 |
| 2 | fluid_pump [facing=down], pipe, void | (0,-57,10), (0,-58,10), (0,-59,10) | flow 0.001 m3/s (GUI) while dead-ended; void placed 18:31:20 |
| 2b | fluid_generator G3, pipe, T3 | (-2,-56,30), (-1,-56,30), (0,-56,30) | G3 130000 Pa; G3 and its pipe removed 18:38:36 |
| 2b | void, pipe, fluid_pump [facing=down] | (0,-59,30), (0,-58,30), (0,-57,30) | default 0.01 m3/s, placed 18:38:36 |
| 3 | fluid_reservoir TA, pipe, fluid_compressor [facing=east], pipe, fluid_reservoir TB | (0,-59,20) ... (4,-59,20) | compressor default 0.05 m3/s, ratio 3; placed 18:32:00 (compressor last); TA replaced by a default water generator (101325 Pa, 25 C) at 18:33:50 |

Stone platforms for aiming: (-1,-59,0), (-1,-58,0), (1,-59,2), (0,-60,2), (0,-59,-2), (0,-58,-2), (0,-59,3), (0,-59,8), (0,-59,28).

## Observations (game time = the screen's "Last delivered view")

### Scenario 1: tank outlets

| time | screen | read |
|---|---|---|
| 115.4 s | T1 after filling | `STEADY: no flow since 113.4 s`; 25.0 C, 130.0 kPa, 1000.0 L, 201.6 kg; Water 11.1 kmol; Connections by face: `Side: mixed outlet / no flow` (the feed). Dead-end pipes are not listed. |
| 326.6 s | T1, kmol/h (`01`) | `FULL`; 121.5 kPa, 199.6 kg; `Bottom: liquid outlet (head 1.9 kPa)` / `drawing water · 81.1 kmol/h`; `Top: gas outlet` / `drawing gas · 1.4 kmol/h`; `Side: mixed outlet` / `drawing all phases together · 25.6 kmol/h` |
| 341.6 s | T1, kg/h (`02`) | 109.5 kPa, 193.8 kg; bottom `drawing water · 1241.4 kg/h`; top `drawing gas · 24.0 kg/h`; side `drawing all phases together · 302.4 kg/h`; table header `Mass %`, `Amount (kg)` |
| 356.6 s | T1 Vapor page | 101.9 kPa, 187.9 kg; vapour N2 98.0 mass % 0.9 kg, water 2.0 % 0.0186 kg; head 1.8 kPa; bottom 1051.7, top 6.6, side 87.6 kg/h |
| 396.6 s | bottom pipe (`03`) | 0.01000 m; 55.5 kmol/h; 1.9 m/s; 1112.7 Pa/m; Phase ratio Liquid 0.0 %, Water 100.0 %, Vapor 0.0 %, Solids 0.0 %; `Ready.` |
| 416.6 / 421.6 s | top / side pipe | 0.0 kmol/h, 0.0 m/s, 1809.5 / 1923.3 Pa/m, phase ratio "—" (the tank had fallen below the voids' pressure; voids do not feed back) |
| 451.6 s | T1 | 98.4 kPa, 161.3 kg; bottom `(head 1.6 kPa)` `drawing water · 52.6 kmol/h`; top `no flow`; side `no flow`. The mass/mole choice was back to kmol on reopen. |
| 521.8 s | top pipe after the refeed (`04`) | 1.1 kmol/h, 37.4 m/s, 17687.8 Pa/m; Phase ratio Vapor 100.0 %, Liquid / Water / Solids 0.0 %; vapour N2 97.3 mol %, water 2.7 % |
| 536.8 s | side pipe (`05`) | 43.3 kmol/h, 1.9 m/s, 24210.8 Pa/m; Phase ratio Water 49.8 %, Vapor 50.2 %; vapour N2 97.5 %, water 2.5 % |
| 541.8 s | T1 with four connections | 125.8 kPa, 547.3 kg; bottom `(head 5.4 kPa)` `drawing water · 87.8`; top `drawing gas · 1.4`; `Side: mixed outlet / receiving · 1784.2 kmol/h` (the feed); `Side: mixed outlet / drawing all phases together · 45.3 kmol/h` |
| 1026.8 s | T1 (`11`) | `HELD: Substep refinement exhausted: Newton iteration limit at residual 1.1460616893371427E-9; active-set pass=0; ...`; last committed state 129.9 kPa, 996.0 kg (liquid-full); bottom `(head 9.8 kPa)` `drawing water · 95.5`; top `drawing liquid (overflow) · 66.4 kmol/h`; side `receiving · 228.3`; side `drawing all phases together · 66.4` |

Log: `fluid_island=36 committed_tick=14220 status=HELD: Substep refinement exhausted: Newton iteration limit at residual
1.1473122445506466E-9; active-set pass=0; reasons={Newton iteration limit at residual #; active-set pass=#=20}`, first at
18:31:21.7, 14 lines; the committed tick (711.0 s) never advanced until Save and Quit.

Head check: `g m_c H / V` with H = 1 m, V = 1 m3: 199.6 kg -> 1.96 kPa (shown 1.9), 996 kg -> 9.77 kPa (shown 9.8).

Compressor block and item: `/give @s createcheme:fluid_compressor` -> log `Gave 1 [Process Gas Compressor] to FluidMcpTester`
and the hotbar name "Process Gas Compressor" (seen on a screenshot, not kept); the placed block (`13`) renders a light-grey
body with the oxidised-copper motor on top, connected along its facing axis, the item in the hand; its screen title reads
"Process Gas Compressor" (`08`).

### Scenario 2: liquid-only pump

| time | screen | read |
|---|---|---|
| 570.7 s | T2 | `STEADY: no flow since 187.8 s`; 130.0 kPa, 201.6 kg |
| 602.1 s | side pump (`06`) | `FULL / INLET_WRONG_PHASE / ERROR: pump inlet not liquid (vapour 79.9 % by volume, from reservoir at 0, -56, 10)` in the warning colour; last flow 0.0 kmol/h; 130.0 kPa; pressure change -28.7 kPa; no limit shown |
| 632.1 s | T2 | `STEADY: no flow since 606.4 s`; two `Side: mixed outlet / no flow` rows; no HELD |
| about 705 s | bottom pump, dead-ended | `NO FLOW: dead-end pipe / FULL`; last flow `-6.49e-18 kmol/h`; 134.1 kPa; flow 0.001 applied |
| 722.7 s | bottom pump (`07`) | `SOLVING / PUMP_TARGET (limit 481688 Pa on this fluid)`; 141.0 kPa; last flow 181.0 kmol/h; pressure change -58.4 kPa |
| 877.7-967.7 s | bottom pump | `HELD: Interval substep limit; no partial interval may commit: advanced=0.03720703124999984 of 0.05 s, accepted=507, rejected=517, reasons=(Newton ite...` (cut at the screen edge); suction 129.3 kPa, water; last flow 199.0 kmol/h; pressure change -47.4 kPa |

Log: `fluid_island=53` HELD first at 18:32:43.9, committed tick 15847 (792.35 s): `advanced=2.8554687499999853 of 3.2 s,
accepted=506, rejected=518, reasons={Newton iteration limit at residual #; active-set pass=#=518}, last=Newton iteration
limit at residual 1.602586284614014E-9`; the committed tick crept 15847 -> 15879 -> 15895 -> 15899 (794.95 s) and stopped.
About 80 s of pumping at about 1 kg/s had passed, so roughly 120 kg of water was still in T2 (estimate; T2 was not read then).

Retry 2b (default 0.01 m3/s, fresh tank T3): 1123.7 s T3 `STEADY`, 130.0 kPa, 201.6 kg. 1154.1 s pump
`FULL / PUMP_TARGET (limit 500020 Pa on this fluid)`, 134.3 kPa, 1990.3 kmol/h, pressure change -44.6 kPa. Log
`fluid_island=81` HELD first at 18:38:53.7, committed tick 23207 (1160.35 s): `advanced=3.2541999572611306 of 5.0 s,
accepted=509, rejected=515, reasons={Backward-Euler state change #=2, Newton iteration limit at residual #; active-set
pass=#=513}, last=Newton iteration limit at residual 1.6347052587090356E-9`; ticks 23207 -> 23257 -> 23269 -> 23272 -> 23275
(1163.75 s), then stuck. 1209.1 s pump (`12`): `HELD: Interval substep limit; ... advanced=0.046888732910155766 of 0.15 s,
accepted=505, rejected=519`, suction 118.3 kPa, 1990.4 kmol/h. At about 10 kg/s the 201.6 kg last about 20 s; the hold
came 6 to 10 s after the start, with water left.

### Scenario 3: compressor

| time | screen | read |
|---|---|---|
| 763.2 s | compressor | fields `Suction flow (m³/s)` 0.05000, `Max pressure ratio (out/in)` 3.0; `FULL / PUMP_TARGET (limit 130887 Pa at this suction)`; suction -8.0 C, 65.4 kPa; last flow 5.7 kmol/h; pressure change 76.1 kPa; `Discharge / suction pressure` 2.2 |
| 798.2 s | compressor (`08`) | `STEADY: no flow since 774.3 s / CLOSED (limit 105318 Pa at this suction)`; -23.8 C, 52.7 kPa; 0.0 kmol/h; pressure change 105.3 kPa; ratio 3.0 (discharge about 158.0 kPa) |
| about 805 s | ratio 1.5 | "Waiting for the next simulation update.", then `Applied`; status `FULL / CLOSED (limit 26330 Pa at this suction)` (= 0.5 x 52.66 kPa) |
| about 832 s | ratio 10.5 (`09`) | message `Check numeric inputs and composition; amounts must be non-negative with a positive total.`; nothing sent (limit stays 26330 Pa); status `STEADY: replaying 8.529e-17 kg/s since 831.6 s, next check at 78591.6 s / CLOSED (limit 26330 Pa at this suction)` |
| 868.3 s | after TA -> water generator | `FULL / INLET_WRONG_PHASE / ERROR: compressor inlet not gas (condensed 100.0 % by mass, from generator at 0, -59, 20)` in the warning colour |
| 878.3 s | compressor (`10`) | `STEADY: no flow since 878.1 s / INLET_WRONG_PHASE / ERROR: compressor inlet not gas (condensed 100.0 % by mass, from generator at 0, -59, 20)`; suction rail 64.8 C, 101.3 kPa, 0.0 kmol/h, pressure change 56.7 kPa, ratio 1.6; its Vapor page shows N2 100 %, its Water page "No components in this phase." |

The compressor island never held. `grep -c status=HELD client-latest.log` = 46 lines, all on islands 36 (T1), 53 (T2 pump)
and 81 (T3 pump). No exception and no ERROR line from a createcheme class in the log.

## Files

`01`-`13` screenshots (854x480, flattened, named with the game time where one applies), `client-latest.log`
(= `run/mcp-client/logs/latest.log`), `gradle-runMcpClient.log`, `cmds/` (the command lists), `REVIEW_SECTION.md`
(draft of the review subsection).
