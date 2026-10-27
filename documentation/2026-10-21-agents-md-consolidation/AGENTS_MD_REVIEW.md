# AGENTS.md consolidation review

Batch: `2026-10-21-agents-md-consolidation` · Owner request 2026-10-21: "Consolidate agents.md so it contains clear indication about what to do, no need to state why. Review & remove obsolete indicators." · Merged to main as 0.11.2 (`ef64bd06`, fast-forward, not pushed).

## What changed

- Every rule restated as an instruction. Owner quotes, record dates, amendment notes and decision numbers (D36/D38, A141, D50) removed; they stay in the batch documents and agent memory.
- Rules regrouped by subject: saves and formats; engine schedule and player-facing updates (tick-loop rule merged in); physics and defects (thermo validity, small simplifications); Gradle, measurement and verification (Gradle lanes, worker threads and lean measurement merged); GUI and in-game checks; git and worktrees; documentation, research and tools; merging to main (changelog, version, code guide, tooling cleanup).
- Words: 2,148 → 1,829.

## Obsolete pointers corrected

| Old text | Current state (checked) |
|---|---|
| References read in one final `gates.sh` run | Final lane is `lane.sh` (`tools/unified-thermo-port-m5/scripts/`): `test slowGates`, chain-100 exact, fluid GameTests, campaign (code guide `verification.md`) |
| Bridge jar in `<worktree>/run/mods` | `build.gradle` `prepareMcpControlMod` reads `run/mcp-client-mods/`, jar from `tools/minecraft-mcp-bridge/`, lane `runMcpClient` |
| Version history (`0.1.0`, `0.2.0`) | Kept in `CHANGELOG.md` only |
| Quick vs full suite not stated | `test` quick, `test slowGates` / `check` full (0.11.1) |

## Owner rules added from agent memory

Standing owner feedback that only Claude's memory held, now visible to every agent:

- Version pace: patch for fixes, cleanups, consolidation; minor only for features and solver/model batches; owner picks (2026-10-15).
- Never `git add -f` ignored files; `git ls-files -i -c --exclude-standard` empty before a merge; logs as `tar.gz` in `tools/<folder>/logs/` (2026-10-14).
- At most one in-game check per task; drawing-only changes judged by gates, sweeps, software renders (2026-10-18).
- No real desktop input unless the owner watches (incident 2026-09-26).
- Fixes resting on unforced assumptions presented as options.
- Simplification proposals quantified at package extremes too (D38 lesson).

## Checks

- Docs-only change: no Gradle run; gates not affected.
- `git ls-files -i -c --exclude-standard` on main after the merge: empty.
- Code guide: no change (`verification.md` refers to AGENTS.md rules generically; still accurate).
- Tooling: none created.
