# In-game check of the device-page layout at small scaled screens (2026-09-27)

Branch `claude/phase-ports-compressor`, code commit `8b43cfdd` (on `17e43270`), worktree
`.claude/worktrees/remove-agents-attribution-line-3d0c97`, owner's Windows machine (display 3840x1600 at 150 %),
JDK 21.0.11 (`JAVA_HOME` set explicitly), `JAVA_OPTS=-Xshare:off`. Every step went through the minecraft-mod-mcp
bridge's in-process HTTP endpoint (`node %TEMP%/minecraft-mcp-skill/scripts/bridge.js ...`, after `enter_control_mode`);
no desktop input of any kind. Review section: `documentation/2026-09-26-phase-ports-and-compressor/PHASE_PORTS_REVIEW.md`
"GUI layout at small scaled screens (2026-09-27)".

## Lane

- Before each launch only the idle Gradle daemon (pid 8832) was running; no test suite ran while a client ran.
- Run 1: `./gradlew.bat --no-configuration-cache runMcpClient --offline --console=plain` (854x480, `logs/02`, client log
  `logs/03`), GUI scale 2 from `options.txt`, then scale 1 through Options > Video Settings (pause menu `click
  {320,288}`, Video Settings `click {267,241}`, GUI Scale `click {267,393}` twice: 2 -> Auto -> 1; framebuffer pixels).
- Run 2: the same with `-PmcpClientWidth=1920 -PmcpClientHeight=1080` (new in `build.gradle`, default 854x480; `logs/04`
  shows `--width, 1920, --height, 1080` in the launch arguments, client log `logs/05`) and `guiScale:0` (automatic = 4,
  scaled screen 480x270, confirmed by `enumerate_widgets`: world list 480 wide). `options.txt` was set back to
  `guiScale:2` afterwards (`options-before.txt` is the file before this check).
- World: the previous pass's **"GUI fixes F3-F5"** (format 6), reopened both times: Singleplayer (`click_button_index 0`),
  the first list entry with a bridge `click` (`{420,135}` at scale 2; `{960,264}` at 1080p after `type_text "GUI fixes"`
  into the list's search box), Play Selected World by `click` on its box. Logs: `fluid_world status=LOADED format=6
  online_tick=10589 islands=2` (run 1), `online_tick=24168 islands=3` (run 2); `New World` was neither opened nor deleted
  (its `level.dat` still dated 2026-09-26 17:46).
- Shut down both times: `close_screen`, `pause_game`, `click_button_index 8` (Save and Quit; "All dimensions are
  saved"), `click_button_index 6` (Quit Game; "Stopping!"); Gradle BUILD SUCCESSFUL. Afterwards the only java.exe was
  the idle Gradle daemon: no game JVM left.

## Devices added (`line.txt`, through `bridge.js cmds`, all "Changed the block")

A line at z = 40: water generator (0,-59,40) facing east; junction pipe (1,-59,40) with a branch pipe (1,-59,41) to a
void (1,-59,42); pump (2,-59,40) facing east; pipe (3); inline filter (4) facing east; pipe (5); pressure control
valve (6) facing east; pipe (7); void (8). The tank, its pipes and voids, the first generator and the compressor line
are the previous pass's.

`open.sh "<tp args>" <file>` closes any screen, teleports, `right_click`s and screenshots; `endshot.sh <file> [x y]`
sends ten `direct_scroll {"mouseX":x,"mouseY":y,"delta":-1}` over the rail (framebuffer pixels; the bridge divides by
the GUI scale; each call moves the rail three steps of 10 GUI px) and screenshots. Teleports: tank `0.5 -57 3.5 180
20.5`, generator `-1.5 -57 2.5 180 29`, compressor `2.5 -60 22.5 180 3.4`, line devices `X.5 -60 38.5 0 3.4`, branch
void `1.5 -60 43.5 180 3.4`.

Bridge notes for the next agent: `scroll_at` did not reach the screen here (three tries, nothing moved); `direct_scroll`
did. Screenshots are always an 854x480 crop from the bottom-left of the framebuffer (`ScreenshotHelper` reads a fixed
854x480), so at 1920x1080 they show only framebuffer x 0-854, y 600-1080, which at scale 4 is GUI x 0-213, y 150-270:
the rail's lower part, the status separator, the status line and Close/Apply: exactly the region of the defect. The
vanilla F2 screenshot key through `press_key` wrote nothing. A `click_button_index` sent right after a snapshot can be
lost (one tab click did nothing, the repeat worked).

## Screenshots (flattened)

| files | scaled screen | shows |
|---|---|---|
| `s2-01` .. `s2-02` (+ `s2-01b`) | 427x240 (854x480, scale 2) | compressor: fields, first value line, rail scrollbar; one wheel click (`01b`); at the end: Last flow, **Pressure change 105.3 kPa** and **Discharge / suction pressure 3.0**, which the old layout did not draw at this size; the bottom row has four separate buttons (Apply no longer covers the basis toggle) |
| `s2-03` .. `s2-05` | 427x240 | tank: Temperature, Pressure, Volume, Mass (scrolled to the end: `716.1 kg` clear of the separator); "Connections by face" tab, 3 rows and the table scrollbar |
| `s2-06` .. `s2-09` | 427x240 | generator: fields, values scrolled to Last flow `4719.8 kmol/h`; Composition and Solids pages with the rail kept at its scroll position (`s2-08` has the component search box's hover tooltip over the table: the cursor sits at the window centre, pre-existing) |
| `s2-10` .. `s2-11` | 427x240 | pump (HELD island, see below): fields, then Temperature, Pressure, Last flow |
| `s2-12`, `s2-12b` | 427x240 | filter: Recover solids button, Filter load, Captured solids, Pressure change |
| `s2-13`, `s2-14`/`15` | 427x240 | valve and end void of the new line (`WAITING: topology event`, no state): one field, nothing to scroll |
| `s2-15b`, `s2-15c` | 427x240 | branch void with state: field, Temperature, Pressure, Last flow |
| `s2-16` .. `s2-20` | 427x240 | pipe and junction pipe (`Junction · 3 connections`): metrics, phase ratio, the full status text (4 lines) at the end of the rail; the junction's Connections page |
| `s1-01` .. `s1-10` | 854x480 (scale 1) | tank (both pages), generator, compressor (all 5 value lines), pump, filter, valve, void, pipe, junction pipe: every rail fits, no scrollbar |
| `a4-01` .. `a4-18` (crops) | 480x270 (1920x1080, auto 4) | compressor, tank, generator, pump, filter, valve, void (branch), pipe, junction pipe, each opened and scrolled to the end: the rail stops at the separator, the status line and Close/Apply are clear |

## HELD lines (not investigated)

- Run 1: `fluid_island=46 committed_tick=11283 status=HELD: Singular Newton Jacobian: Sparse LU rejected a singular
  matrix; active-set pass=0`, 9 lines from 21:07:38.953, 60 ms after the inline filter was placed at (4,-59,40) (the
  island: generator, junction pipe with its branch void, pump, pipe, filter). The pump, filter, pipes and branch void
  pages show that status.
- Run 2 (the same world reloaded): no HELD line. No exception and no ERROR from a createcheme class in either log.

## Files

`logs/01` `build -x test` before the client, `02`/`04` the two `runMcpClient` runs, `03`/`05` their client logs, `06`
fluid suites (and `06-...-summary.txt`), `07` final `build -x test`; `line.txt`, `open.sh`, `endshot.sh`,
`options-before.txt`.
