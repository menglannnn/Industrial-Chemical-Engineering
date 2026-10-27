# unified-thermo-port-m4

Purpose: the scratch tooling of milestone M4 (runtime, formats, presentation, GUI, consumers) of the unified thermo port,
batch `documentation/2026-09-24-coolprop-low-temperature/` (stage document `M4_RUNTIME_FORMATS_GUI.md`, GUI notes
`gui/M4_GUI_CHECK_NOTES.md`, plan `MAIN_ADAPTATION_PLAN.md` section M4), branch `claude/unified-thermo-port`, 2026-10-16.
Nothing here was committed or detached from a tracked path: every test M4 added is a gate test that `test` and
`fluidRuntimeTest` run (`CrystalPackageConsumersTest`, `SpineNetworkPathTest#theColumnRefusesASpinePackageTyped`, and
the new cases in `FluidCheckpointCodecTest`, `WorldTopologyLedgerTest`, `CrystalIslandRuntimeTest`,
`FluidPropertyReloadGuardTest`).

| Path | What | How to run |
|---|---|---|
| `scripts/lane.sh`, `scripts/M4Run.java` | The javac lane outside Gradle (from M3's index lane): exports a tree (a commit, the index or the working tree) to `/tmp/m4/<slot>`, compiles it whole against the dev client's and game-test server's libraries, runs JUnit selectors with the scheduler self-verification on | `bash scripts/lane.sh compile <commit\|index\|work> <slot>`, then `bash scripts/lane.sh run <slot> pkg.Class#method`; needs one Gradle compile first (`build/moddev/*LegacyClasspath.txt`) |
| `rule-off/run.sh`, `rules.txt`, `*.json` | The four rule-off proofs: each rule switched off in an exported copy only (`EDIT=` through `../unified-thermo-port-m2/scripts/edit.js`), its one test run alone, then the same test on the unedited copy | `bash rule-off/run.sh [rule ...]`; logs `rule-off-<rule>.txt` |
| `probes/M4WorldIslands.java` | Reads a dev world's fluid checkpoint (its `data/` folder) with the pilot model and prints every saved island (status, clock, certificate, nodes with p, T and solids, connections with their solid closure mask) and the ledger's devices | compile against a lane slot's `main` and `test` classes, run with `src/test/resources` and `src/main/resources` on the classpath and the data folder as argument (`logs/03-checkpoint-probe.txt` is its output) |
| `mcp/bridge.js`, `mcp/Flatten.java` | The minecraft-mcp HTTP driver (copy of `tools/fluid-tank-column-gui-fixes/scripts/bridge.js`) and the alpha flattener its `shot` uses | `node mcp/bridge.js <cmd> '<json>'` against a `runMcpClient` lane; `MSYS_NO_PATHCONV=1` for `chat`/`cmds` |
| `mcp/build-1-generators.txt`, `build-2-connect.txt`, `build-3-top-vent.txt` | The GUI check's world as chat commands: platform and two unconnected generators (a new generator holds water at 298 K, so each is set before anything joins it), then tank, void and pipes, then a top vent | `MSYS_NO_PATHCONV=1 node mcp/bridge.js cmds mcp/build-1-generators.txt` |
| `logs/m4-logs.tar.gz` | 01 M4 tests (Gradle log, JUnit results), 02 the dev client's Gradle logs and game log, 03 the checkpoint probe, 04 the crash report, 05 the full `test` (log, results), 06 printed lines against M3's log 26, the rule-off logs, and the dev world's two saved checkpoints (`worlds/world-1013`, `world-1018`) | `tar --force-local -xzf logs/m4-logs.tar.gz` |

GUI check: `tools/fluid-in-game-rig/template/bench-template` copied to `run/mcp-client/saves/<name>/` with
`tools/p6-pilot-acceptance/datapack/p6_pilot` in its `datapacks/`; `bash tools/unified-thermo-port-m1/gradle-locked.sh
<log> runMcpClient` (refuses while any dev client runs). Bridge notes from this run, at 854x480 and GUI scale 2:
`enumerate_widgets` reports GUI units (x2 for `click`); type the world's name into the world list's search box so only
the new world is listed, then click its entry and Play; a list scrolls by a `click` on its scrollbar track (the screen
seeks), never by `scroll_at`, which moves the real cursor through GLFW (it turned the camera once); `direct_scroll`
scrolls without the cursor (not used); a compat `click` with no screen open attacks the block under the crosshair.

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: M4 of the unified thermo port (formats 9 / 2 / `fluid-10`, crystals in the Solids tab, ledger totals, certificate replay): rule-off proofs of the four new tests, GUI-check notes and bridge logs, the save-crash probe (D32)

Batch: 2026-09-24-coolprop-low-temperature

Status: Done 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`README.md`

### History

- Done 2026-10-16 (`61a1eba0` on `claude/unified-thermo-port`, merged 2026-10-19 in 0.9.0 (main `5b49f940`, not pushed))
