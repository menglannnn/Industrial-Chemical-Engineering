# Fluid format mismatch reset — review

2026-10-23; implementation `49e57860`, delivery `7234bb46`, branch `codex/fluid-format-reset`.

## Expected behavior

- Owner selected destructive simulation reset rather than disabled simulation: incompatible fluid saves reopen from an empty authority, with placed pipes and completed equipment rebuilt as chunks load. Reservoirs start empty; default controls replace saved settings. This is an explicit reinitialization, not a conservative evolution of the old contents.

## Previous behavior and change

- The owner's existing client log shows checkpoint 14 rejected by build 15, followed by START_FAILED and a fatal server exception. Only existing crash/startup logs were inspected; no automated world interaction was performed.
- `FormatMismatchException` distinguishes checkpoint, unit and pack version refusals. `FluidSavedData.open` catches only that type and returns dirty empty saved data. Strict load/read/codec APIs still refuse incompatible bodies; malformed current data/checksums/property basis retain existing refusal behavior.
- `resetForFormatChange` starts a fresh store sharing only backend/IO ownership; new numbers exceed every retired pack. Existing packs are not overwritten before a replacement core commits. Existing serialized commit order and atomic writes retire them after success.
- Old clocks, inventories, filter solids, controls, certificates, pending events and recoveries are discarded together. No old body is decoded, migrated or normalized; format 15 / unit 8 is unchanged.
- Chunk events always enqueue TickTask on the logical server thread. Early notifications are retained until authority installation; pending block entities are promoted from their saved cell metadata. No chunks are forced and no solver runs on the load path.
- Missing one-block registrations use normal queued placement/defaults; matching positions rebind existing registrations. Multi-cell equipment waits for every bounding chunk and validates shell identity/layout plus interior cells, then queues the existing registration path. Partial/stale structures stay on existing construction/layout-refresh paths.
- Paint, shell geometry, nozzle sizing and sight-glass blockstates are untouched. Resets also restore equipment identity metadata for controls/audio; ordinary process publication stays on its existing engine schedule.

## Verification and delivery

- Final owned `gradlew.bat jar --max-workers=4`: PASS. `git diff --check` clean. No tests added/run, no automated GUI or in-game checks, no old-world checks.
- Owner checks: defaults/empty contents after reset; pipes and all constructed movers/vessels reconnect; structures across chunk boundaries; partial structures remain inactive; distant chunks/reload; same-format reload retains new state; normal controls/sight glass/audio still work.
- Guide contracts: CODE_GUIDE_ADDENDUM.md and owned fluid-runtime/equipment chapter addenda. Main guide consolidation remains due at a future main merge.
- Tool cleanup: no harness, probe or new Gradle task. Compressed owner crash/build/merge/startup evidence resides in canonical ignored `tools/fluid-format-reset/`.
- Target integrated at 7234bb46; target jar passed and responsive client 48904 left running. Passive startup log records format-14 RESET to 15 and player join; see MERGE_REVIEW.md. No main merge or mod_version change.
