# fluid-tank-column-gui-fixes

Batch: `documentation/2026-10-15-fluid-tank-column-gui-fixes/` (branch `claude/fluid-tank-column-gui-fixes-3f0fc3`, merged to `main` as 0.8.0).

Nothing was detached from a tracked path: every test the batch added is a gate test that `test` runs (`V3ColumnFeedAndDrawLimitsTest`, the new cases in `V3BundledRegroupedModelTest`, `V3ClientPresetsTest`, `IslandCoordinatorTest`, `PhysicalRegistryTest`, `V3SideDrawContractTest`). This folder keeps the scratch material that lived only in the worktree's `build/`.

| Path | What | How to use |
|---|---|---|
| `scripts/gates-run.sh` | The batch's gate run: G1 fluid suites with the junction-line summary (`../phase-ports-probes/holds3/summarize.sh` against `../phase-ports-probes/holds/12-junction-lines.txt`), G2 chain-100 exact, G3 adjacent, G5 fluid GameTests | Copy to `<worktree>/build/gates/run.sh` and run `bash build/gates/run.sh` from the worktree root; one Gradle invocation at a time |
| `scripts/bridge.js` | The minecraft-mcp bridge driver used by the in-game checks (from `claude/minecraft-mcp-skill-c3f0f0:.claude/skills/minecraft-mcp/scripts/bridge.js`) | `node bridge.js <cmd>` against a `runMcpClient` lane |
| `scripts/Stitch.java` | Joins screenshot strips into one image (column product headers) | `java Stitch.java out.png in1.png in2.png ...` |
| `scripts/tjl_n2_test.json` | The custom mixture file used before the bundled nitrogen mixture existed | Copy to `<game>/CreateChemE/mixturepreset/` |
| `logs/ingame-check.tar.gz` | Both in-game checks (`runMcpClient`, bridge only): screenshots 00-38, the re-check under `recheck/`, widget dumps, client logs | `tar --force-local -xzf` |
| `logs/final-gates-ffeafe8a.tar.gz` | Final gates on the release commit: full `test` 210 classes / 1190 tests / 0 failures, 33 junction lines 0 diff (newtonSolves 339), chain-100 exact 0, fluid GameTests 31/31, column GameTests 2/2 | `tar --force-local -xzf` |
| `logs/gates-7ae9b66b.tar.gz` | Gate logs at `7ae9b66b`: G1 536/0, 33 junction lines 0 diff, G2 exact 0, G3 48/48, G5 31/31 | `tar --force-local -xzf` |

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Gate runner, bridge driver and the archived in-game screenshots and gate logs of the tank/column GUI fixes and held-island event batch

Batch: 2026-10-15-fluid-tank-column-gui-fixes

Status: Concluded 2026-10-15. Index: [../INDEX.md](../INDEX.md).

### Entry points

README.md, scripts/, logs/*.tar.gz

### History

- Concluded 2026-10-15 (merged as 0.8.0; nothing detached from a tracked path)
