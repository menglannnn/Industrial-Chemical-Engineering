# Fluid scheduling follow-ups (started 2026-09-23)

Owner decisions on the batch review `../2026-09-23-fluid-scheduling-rest/FLUID_ISLAND_REST_REVIEW.md` section 5, given 2026-09-23 after the 0.3.0 merge:

| item | decision | package |
|---|---|---|
| Pumped fills held for minutes or forever (in-game finding) | find the root cause and fix | F1 (done 2026-09-24: fills fixed; gas transfers remain, see F4) |
| Placement quadratic in world device count (in-game finding) | find the root cause and fix | F2 |
| 1. Default stationarity tolerance 1e-9 | loosen it (1e-7) and keep it configurable | F1 |
| 2. Revalidation against the certified interval vs its extrapolation | keep for later | none |
| 3. Start-up wall-budget retry nondeterminism | fix | F1 |
| 4. 64 MiB checkpoint bound | replace with the recommended storage (not one JSON/NBT blob) | F3 (done 2026-09-24: format 4, per-island binary units in pack files, no bound; `f3/`) |
| 5. Refused save crashes the client | do nothing | none |
| Gas transfers hold at the 273.16 K property floor (F1 finding) | 2026-09-24: implement pump option P1 (pressure rise scaled by suction density); ALSO extend the thermo package domain below the floor because cryogenic air separation comes later; a state outside the package domain must raise a dedicated error made clear to the player and to agents | F4 (done 2026-09-24: P1, `fluid_domain` data, `ThermoDomainViolation`; `f4/`) |

Branch `claude/fluid-followups` from main 23beadd, worktree `agent-ae139e4fc1b184b36`. Reports land here as each package is verified.

Status 2026-09-24: F1 to F4 done and verified; release commit `d6bcc1e` (0.4.0) prepared on `claude/fluid-followups` over `dea8a7e`; then the AGENTS.md tooling rule (`88df883`) and the tooling cleanup (`c1b8464`..`f9d6be1`, review `FLUID_TOOLING_CLEANUP_REVIEW.md`, tools in the main `tools/`, see `TOOLS.md`); branch tip `f9d6be1`, MERGED TO MAIN 2026-09-24 by fast-forward (23beadd to f9d6be1, version 0.4.0, not pushed). Existing fluid worlds are refused at load (format 4, changed thermodynamic revision): create fresh worlds. Open owner decisions: whether islands loaded from a save should also start on a one-tick slice (F1); whether solver entry traces of out-of-range components should be exempt from the domain rule (F4 open item 1); the deferred items 2 and 5 above. Reports: `f1/`, `f2/`, `f3/`, `f4/` (the agent-facing error reference is `f4/THERMO_DOMAIN_ERROR.md`).

## Index record (moved from INDEX.md on 2026-10-26)

Purpose: Follow-ups to the fluid scheduling batch: pumped-fill solver failures and hold policy, start-up retry determinism, tolerance default 1e-7, placement cost, checkpoint storage

Status: Implemented 2026-09-24 (0.4.0). Index: [../INDEX.md](../INDEX.md).

### Code areas

`runtime/fluid`, `science/fluid/network`, `FluidCheckpointCodec`

### History

- Implemented 2026-09-24, MERGED TO MAIN 2026-09-24 (fast-forward 23beadd to f9d6be1, 33 commits, version 0.4.0
- not pushed).
- Worked on `claude/fluid-followups` from main 23beadd
- F1 (pumped fills, hold policy, start-up determinism, tolerance 1e-7) done and verified 2026-09-24 @111d805 (161/202 tests, 29 GameTests, exact regression zero, P12/P31 byte-identical
- fill100 12.2 to 0.35 cores with 0 holds
- pump simplification P1 proposed, owner decision pending)
- F2 placement done and verified 2026-09-24 @b86b147 (161/210 tests, 30 GameTests, exact regression zero, P12/P31 byte-identical
- events batched per tick and only touched components compiled: 5,000 devices 48.4 s to 0.64 s, one more block in a 5,000-device world 28 ms to about 1 ms)
- F3 checkpoint storage done and verified 2026-09-24 @e837ada (161/220 tests, 30 GameTests, exact regression zero, P12/P31 byte-identical
- format 4: per-island binary units in pack files with a small core record, dirty tracking seeded at load, atomic flushed writes on the IO worker, no world-wide bound
- certified rest100 island 144 KB to 34 KB, 100-island cold save 205+459 ms to 63+19 ms, load 806 to 49 ms, 1,000 islands save and load where format 3 refused)
- F4 pump P1, thermodynamic domain as data and dedicated thermo-domain error done and verified 2026-09-24 @dea8a7e (182/226 tests, 30 GameTests, exact regression zero, P12/P31 byte-identical, column/material/thermo 568/0
- nitrogen valid from its 63.151 K triple point against NIST (Psat +1.23 %, liquid density +0.34 % at 77.355 K)
- a gas transfer between closed tanks closes at the scaled 575 Pa limit within 0.25 s instead of holding at the 273.16 K floor
- water fill shutoff unchanged within 0.04 Pa
- `ThermoDomainViolation` with its own rejection key, `HELD (thermo domain)` status, one WARN per hold, waits for an input change after a reproduced retry
- oxygen and argon are data-only additions).
- Tooling cleanup done and verified 2026-09-24 @f9d6be1 under the new AGENTS.md rule (88df883): `FluidInGameDiagnostics`, `FluidInGameClientDiagnostics`, the `-PfluidRig*` switches, the F3 payload-section hook and the dead `fluidStressProfile`/`fluidModuleProfile` replays left the code (297 lines) for `tools/fluid-in-game-rig/`, `tools/fluid-probes/`, `tools/fluid-paced-analysis/`, `tools/mcp-gui-helpers/` and `tools/retired/fluid-profile-replays/` with re-attach patches that apply to HEAD
- gates identical case by case (182/226/30/exact 0/P12-P31).
- Batch COMPLETE and MERGED at f9d6be1 (release commit d6bcc1e = 0.4.0 included)
