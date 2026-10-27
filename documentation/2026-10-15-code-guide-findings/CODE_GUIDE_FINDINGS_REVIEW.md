# Code-guide findings: applied owner decisions (2026-10-15)

Branch `claude/progressive-disclosure-doc-a86bc0`, on top of `6275d4d6`. Owner decision 2026-10-15: "No need for any backward compatibility at current stage. Others fix as recommended." The findings come from the code-guide rewrite (`../reference/code-guide/README.md`) and a compatibility sweep (opus agent).

## Expected behaviour, and what the code did

1. **A player input is a queued engine event.** A filter recovery should wait for the engine like an edit. Instead, `FluidWorldAuthority.recoverFilter`, called from the packet handler, applied every queued event and delivered the item at once. It now validates and queues; the engine applies it with the next batch and refuses an empty filter then (`PhysicalRegistry`, reply on the menu's next bucket). The public `view(id)` read flushed queued events; it is now a cached read.
2. **Opening a screen replays the last published snapshot.** The fluid screen did; the column screen waited up to 5 s for the next bucket. Each bucket now records the block's published state (client nonce 0, in memory only: `ColumnCalculatorV3BlockEntity.published`), and a state request replays it at once. With none, the first bucket delivers.
3. **No compatibility.** Removed: `legacyUnbound` (a checkpoint with islands but no topology unit is a fixture image; `FluidSavedData.open` refuses it as a world); the column block's verbatim retention and re-save of unreadable saves and `V3Status.INCOMPATIBLE` (refused, a fresh input replaces it, all three revisions required); the server preset's default `columnDiameterMetres`; `FluidAppearance.DEFAULT` (component and assay records must carry `appearance`); migration and "explicitly reset" wording; the dead `examples/material-gametest.gradle`.
4. **Column GUI, pressure profile.** The stage drop sets the profile at diameter 0 and is the hydraulics' first guess at a positive diameter, so it must be authorable and survive a preset round trip. It had no field, and client presets had no key (a saved nonzero drop came back as 0). Now: canvas field `pressure_drop` beside top pressure; client column presets schema 2 with required `stage_pressure_drop_kpa` (schema 1 refused); folder `<gameDir>/CreateChemE/` (was `CreatChemE`).
5. `fluidServerBenchmark` measures 60 s by default (benchmark rule 60 + 60).

Kept on purpose: `UNBOUND: saved inventory retained` (a live consistency status, not compatibility); the authoring defaults listed as open below.

## Checks (lean rule; one lane, nothing else running)

| Check | Result |
|---|---|
| Compile, all four source sets | pass |
| Full `test` | 1260 tests; one failure (a refusal-message wording check, restored to "fresh development world"); `FluidCheckpointCodecTest` rerun 9/9 |
| G2 `fluidSolverRegression` exact (chain-100) | max deviation 0.000e+00 on moles, temperature, phase fraction, flow |
| G5 `runFluidGameTestServer` (run id `codeguide-20261015a`) | 33/33 |
| `runColumnGameTestServer` (same id) | 2/2 (`unreadableColumnDataIsRefusedAndReplacedByAFreshInput` replaces the two retention tests) |
| GUI in `runMcpClient` through the bridge (opus agent, no desktop input) | 3/3 pass, see below |

G1 and G3 of `gates.sh` are subsets of the full `test` run; the junction-line comparison was not run because no solver code changed (G2 exact confirms the trajectories).

## GUI check (854x480, guiScale 1, fresh creative superflat; `screenshots/`)

- Tray dP field: EditBox at (314,129) 70x18 beside Top P (246,129), no overlap among 20 widgets, hint tooltip shown; typing 0.7 gives "Draft changed", no rejection (`01`, `02`).
- Preset round trip: "Save column" wrote `<gameDir>/CreateChemE/columnpreset/guicheck.json` with `schema_version` 2 and `stage_pressure_drop_kpa` 0.7; after changing the field to 1.5, loading it back showed 0.7 (`03`-`07`).
- Replay on open: a never-opened block shows "Waiting for the engine" until its first bucket (+4.5 s); reopened after 8 s, both blocks show the full state at +0.2-0.25 s, identical to the post-bucket frame (`08`-`11`, `08-10-open-timings.txt`).
- Not checked: the narrow layout (guiScale 2). Bridge notes: `execute_command` uses the client dispatcher (commands went through chat), screenshots can lag one frame.

## Open for the owner

- Authoring defaults kept (not save compatibility): a package without `liquid_mixture` (zero coefficients; the `cdu17-reference` fixture relies on it and requiring it may move pinned fingerprints), the positional assay form (test fixtures only), package `aliases` (none bundled). Remove them too?
- Code guide: `../reference/code-guide/` updated for all of the above.
