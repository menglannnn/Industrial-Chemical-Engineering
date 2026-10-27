# Layout refresh of old saves: recheck of anchor and light (2026-10-18)
Setup: uncommitted refresh work on 1ca05c1c (compileJava UP-TO-DATE; the class carries `shift={},{}` and `waitForLightBeforeSending`). `runMcpClient`
lane, all input through `bridge.js`, two client sessions (Gradle exit 0 both). `run/saves/Equipment review 2026-10-18` copied unchanged (diff -rq
identical) to `run/mcp-client/saves/compat-recheck-2026-10-18`, picked by the list's search filter (detail line shows the folder). Owner's world not opened.
Session 1: load, shots, row tour, fresh cyclones, compares, Save and Quit, reload, revisit + tour, Save and Quit, Quit Game. Session 2: one more load for
the kit-camera side shot. World: noon, daylight cycle off; player logs in at (150, -28, 144), simulation distance 8.

## Refresh lines: 35 of 35, blocked=0 on every line (`recheck-log.txt`)
- `equipment_refresh kind=cyclone origin=-3, -60, 181 shift=0,1 cleared=3 placed=0 blocked=0`
- `equipment_refresh kind=cyclone origin=109, -60, 65 shift=0,1 cleared=2 placed=0 blocked=0`
- The other 33: `shift=0,0 cleared=0 placed=0 blocked=0`.
- Timing (login 22:07:44.74): 4 row-1 lines at 44.21-44.38 (spawn chunks, before login); 26 more by 22:07:51.41, among them the 2.0 m cyclone at 49.05,
  0.5 s after the tp to it (it was outside simulation distance of the login spot), the 1.1 m at 51.14. Vertical heater 71,-60,29 and cabin heater
  90,-60,28 at 22:09:56 (tour reached row 1); coker 220,-60,176 at 22:11:06 (row 5 east stop). No "Can't keep up" (first check: one, 2.3 s).
## Checks
1. 35 lines, blocked=0: PASS.
2. Anchor, 2.0 m: PASS. Fresh `cyclone 2.0 1 8 north` at 0 -60 225 (303 cells at -3,-60,222). Placement offset dz 40 (`recheck-compare.txt`):
   "Test passed, count: 4875", masked "count: 303". The old-origin offset dz 41 now fails, as it should. At the kit check's placement-relative camera
   (`recheck-pair-side-kit.png`: kit `shots/k-i5-cyclone-side` vs `recheck-kitcam-side2`) body, frame, platform, ladder and rails are pixel-identical;
   only the feed end differs (dog-leg spool replaced by the eccentric transition's round end; 4954 px, all in that duct and the grass).
   Refreshed vs fresh: `recheck-pair-inlet-se/-iso/-side.png` differ only in background and grass.
3. 1.1 m compare: placement offset dz -97 (the first check's) "Test failed"; origin offset dz -98 "Test passed, count: 1584", masked 69. See finding 1.
4. Light without reload: PASS. Shots 7, 12, 17 s after login (2-13 s after the 2.0 m refresh, `recheck-t0-*`), at 43-65 s (`recheck-t30-*`), after the
   tour (`recheck-tour-after-*`): no black faces. Near-black px (`Dark.java`): 48 / 5 / 86 / 24 (inlet-se / iso / side / inlet-ne), equal to the fresh
   cyclone's (deck-edge bolt marks); first check before reload: 5528 (r20-inlet-se), 1563 (r20-side). Cyclone crops t0 = t30 = after tour = after
   reload (0 px; t0 inlet-se differs only in sky and horizon). `recheck-pair-light-inlet-se.png`, `-light-side.png`; 1.1 m clean (`recheck-pair11-*`).
5. Second load silent: PASS. 0 new lines after Save and Quit + reload (both cyclones revisited, all rows toured); session 2's load: 0 lines too.
6. Log clean: PASS. 0 ERROR, no createcheme WARN, no `drum node moved`, no exception besides the vanilla boot mixin ClassNotFound WARN.
## Findings
1. The 1.1 m cyclone also shifts 0,1. Physics: the vessel stays on its foundation; only its redrawn inlet changes. Code: at shift 0,1 every new cell
   lands on an old cell of the same kind (placed=0) and only 2 front cells (the dog-leg) are cleared; the first check's shift 0 cleared 26 and placed
   24, so it moved the 1.1 m body 1 block north unnoticed. The box went from 5 rows to 4 (placed=0 needs the old box to reach z 69) and the command
   centres the box (origin = position - size/2), so a fresh one at the same command stands 1 block north of the old body: its compare passes at the
   origin offset, not the placement offset. Proposition: accept (body kept); "refreshed = fresh at the same command" holds only when centring is unchanged.
2. Units outside the simulation distance refresh only when the player comes near (3 units, 2-3 min after load), as (a) intends.
3. Process: my first Play used `click_button_index 2`, which is Create New World (renderables: list 0, Play 1); left with Escape, no world created,
   copy still diff-identical before its load. The copy now holds `sendCommandFeedback false` and fresh cyclones at 0 -60 225, 110 -60 -30.
Files (`tools/new-units/in-game/`): `recheck-*.png` (shots + pairs), `recheck-view-*.txt`, `recheck-shoot.sh`, `recheck-first.sh`, `recheck-compare.txt`,
`recheck-log.txt`, `Pair.java`, `Dark.java`.
