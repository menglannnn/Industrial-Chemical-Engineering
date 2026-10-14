# In-game check of the GUI fixes F3-F5 (2026-09-27)

Branch `claude/phase-ports-compressor`, code commit `f089edd1` (on `b0923a5a`), worktree
`.claude/worktrees/remove-agents-attribution-line-3d0c97`, owner's Windows machine, JDK 21.0.11 (`JAVA_HOME` set
explicitly), `JAVA_OPTS=-Xshare:off`. Every step went through the minecraft-mod-mcp bridge's in-process HTTP endpoint
(`node %TEMP%/minecraft-mcp-skill/scripts/bridge.js ...`, after `enter_control_mode`); no desktop input of any kind.
Review section: `documentation/2026-09-26-phase-ports-and-compressor/PHASE_PORTS_REVIEW.md` "GUI fixes F3-F5 (2026-09-27)".

## Lane

- Before each launch only the idle Gradle daemon was running (no CPU over 3 s); no test suite ran while a client ran.
- `./gradlew.bat --no-configuration-cache runMcpClient --offline --console=plain`, twice: run 1 (`logs/05`, client log
  `logs/06`) found a readability problem on the new page (a row tooltip, below) and was quit; the screen was changed and
  run 2 (`logs/07`, client log `logs/08`) took every screenshot kept here except `03`.
- Fresh world: created in run 1 through the title screen with bridge clicks as **"GUI fixes F3-F5"** (Creative, Allow
  Commands ON, Superflat, Generate Structures OFF; the name box took `type_text` after a click and ctrl+a). Log:
  `fluid_world status=LOADED format=6 online_tick=0 islands=0`. Run 2 reopened the same world (`format=6 online_tick=6709
  islands=2`). The old save `run/mcp-client/saves/New World` was neither opened nor deleted.
- GUI scale: run 1 started with `guiScale:1` in `run/mcp-client/options.txt`; run 2 started with `guiScale:3`; every other
  change went through Options > Video Settings > GUI Scale with bridge clicks (scale 1: pause Options (373,203), Video
  Settings (347,124), GUI Scale (347,196); scale 2 frame: (320,288), (267,241), (267,393); framebuffer pixels).
- Shut down: `close_screen`, `pause_game`, `click_button_index 8` (Save and Quit; "All dimensions are saved" 20:12:38),
  `click_button_index 6` (Quit Game; "Stopping!" 20:12:47); Gradle BUILD SUCCESSFUL. Afterwards the only java.exe was the
  idle Gradle daemon (8832): no game JVM left.

## World (all `setblock`, `cmds` in the order below)

| block | position |
|---|---|
| fluid_reservoir T (placement default: N2, 101325 Pa, 25 C, 1000 L) | (0,-57,0) |
| fluid_pipe west / east / north / top / bottom (default 0.05 m) | (-1,-57,0) / (1,-57,0) / (0,-57,-1) / (0,-56,0) / (0,-58,0) |
| fluid_void east / north / top / bottom (101325 Pa) | (2,-57,0) / (0,-57,-2) / (0,-55,0) / (0,-59,0) |
| fluid_generator (default water, 101325 Pa; 130000 Pa entered through its page in run 1, reply "Queued for simulation event at tick 2208"; whether it applied was not read) | (-2,-57,0) |
| stone platforms | (0,-58,3), (-2,-58,2) |
| fluid_reservoir, pipe, fluid_compressor[facing=east], pipe, fluid_reservoir (defaults: 0.05 m3/s, ratio 3) | (0,-59,20) ... (4,-59,20) |

The tank has five connections: top, bottom and three side faces (north, west = the feed, east). Screens were opened with
`chat "/tp @s 0.5 -57 3.5 180 20.5"` + `right_click` (tank), `"/tp @s -1.5 -57 2.5 180 29"` (generator),
`"/tp @s 2.5 -60 22.5 180 3.4"` (compressor), with `MSYS_NO_PATHCONV=1`. The tank page's new tab is `click_button_index 4`.
Compressor ratio: `click {"x":173,"y":296}` (the ratio box at scale 2), ctrl+a, `type_text "10.5"`, `click_button_index 4`.
The list scrolls with `scroll_at {"x":600,"y":260,"clicks":-1}`.

## Screenshots (854x480, flattened)

| file | scale | shows |
|---|---|---|
| `01-tank-connections-by-face-scale1.png` | 1 | the new tab, all five rows, no scrollbar: `Bottom (down): liquid outlet (head 5.8 kPa)` / `drawing water · 4431.6 kmol/h`; `Top (up): gas outlet` / `no flow`; `Side (north): mixed outlet` / `drawing all phases together · 84.7 kmol/h`; `Side (west): mixed outlet` / `receiving · 4718.8 kmol/h`; `Side (east): mixed outlet` / `drawing all phases together · 84.7 kmol/h` (446.6 s) |
| `02-tank-connections-by-face-scale2.png` | 2 | three rows (bottom, top, north) and the scrollbar (471.6 s) |
| `03-tank-outlets-hint-tooltip-scale2.png` | 2 | run 1 only: the outlets hint as a tooltip. The cursor sits at the window centre when a screen opens, which at scale 2 is inside the rows, so a per-row tooltip covered the list; the final build shows the hint on the page title and on the tab button only (the hint of in-game check item 1.7, not observable before, renders) |
| `04-video-settings-guiScale3-at-854x480.png` | "3" | the option reads "GUI Scale: 3" but the game draws at 2: Minecraft caps the scale to the largest one leaving at least 320x240 scaled pixels, and 854/3 = 284 < 320 |
| `05-tank-connections-by-face-guiScale3-effective2.png` | 3 (= 2) | the same page as `02`, three rows and the scrollbar (371.6 s) |
| `06-tank-connections-by-face-guiScale3-effective2-scrolled.png` | 3 (= 2) | after one wheel click: north, west (`receiving · 4718.7 kmol/h`), east rows: nothing is lost |
| `07-compressor-ratio-10.5-refused-scale2.png` | 2 | ratio 10.5 entered, Apply: `Max pressure ratio (out/in) must lie between 1.01 and 10.`; nothing sent (status keeps `CLOSED (limit 105318 Pa at this suction)`) |

1920x1080 was not run: the `mcpClient` run configuration fixes the window at 854x480 (`build.gradle:138`) and resizing it
needs desktop input. Its scaled sizes are 1920x1080 (scale 1), 960x540, 640x360, 480x270 (4 = auto), all at least the
427x240 verified here in both directions, so the page shows at least three rows there as well (`visibleRows = (imageHeight
- 164) / 24`, `imageHeight = max(250, height - 16)`: 3 at 480x270, 7 at 640x360, 15 at 960x540, 37 at 1920x1080).

## Observations, not fixed here

- Pre-existing, same class as F3: at GUI scale 2 (and 1080p auto) a pump's, compressor's or generator's rail values
  start at y = 218 (`FluidDeviceScreen.renderLabels`) and are drawn over the bottom buttons (`07`: "Temperature -23.8 C"
  under Close); the tank rail's "Mass" line touches the separator (`02`). Out of this task's scope (F3 was the outlet list).
- Run 1 solver hold: `fluid_island=22` (the tank's island, 101.3 kPa water generator feeding the N2 tank with four open
  0.05 m outlets to 1 atm voids) logged `status=HELD: Interval substep limit; no partial interval may commit:
  advanced=3.96875 of 5.0 s, accepted=508, rejected=516, reasons={... Backward-Euler state change #=511 ...}` 14 times,
  committed tick 1135 -> 1785 in 50-tick steps (it crept), then no further HELD line (the queued generator edit, event
  tick 2208, replaces the island when it applies; not confirmed).
  Run 2: no HELD line. No exception and no ERROR from a createcheme class in either log. Not investigated (GUI task).

## Files

`logs/01` fluid suites (and `01-...-summary.txt`), `02` `build -x test`, `03` GameTest + mcpCompat compile, `04` fluid
GameTests on a fresh world (33/33), `05`/`07` the two `runMcpClient` runs, `06`/`08` their client logs, `09` final
`build -x test`, `options-before.txt` (the options file before this check).
