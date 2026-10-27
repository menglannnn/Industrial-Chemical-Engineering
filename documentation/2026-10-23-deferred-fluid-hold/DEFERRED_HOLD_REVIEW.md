# Deferred fluid hold record

2026-10-23, Asia/Singapore. Evidence only; owner explicitly deferred debugging. Runtime source `beb5c2fc` on `claude/floating-roof-tanks-81d68b`, plus the ten pre-existing local equipment/test files captured as patch/untracked source. Pending display follow-ups `a591404f` are not loaded into this client.

## Observed behaviour

- World: target worktree's `run/saves/New World`, client PID 32844, current fluid checkpoint 17 / unit 10. Session startup reset incompatible fluid format 16 at 22:46:58, as already approved; no old-save test or migration was performed.
- First observed hold: island 90 at 23:02:02.048, committed tick 18045, residual 3.0661905288966587. Later island 91 at 23:02:35.030, committed tick 18704, residual 0.13092152937243595. Subsequent topology identities and all repeated messages remain in the full log.
- Message: `HELD: Substep refinement exhausted: Newton line search stalled ... active-set pass=0`; the recorded reason counter is 20. These are symptoms, not an established physical mechanism or root cause.
- The user had separately discussed WTI feed at 200°C / 3 bar; this record does not assume that feed belongs to a held island. No unsupported attribution to a valve or the new accessory.

## Preserved evidence

- Canonical `tools/fluid-world-inspector/logs/deferred-hold-logs-2026-10-23-*.tar.gz`: complete latest/runClient log snapshots; atomic fluid core and immutable pack copies; level metadata, config, local source patch/untracked file, SHA-256 manifest and current revision.
- `tools/fluid-world-inspector/deferred-hold-capture.json` points to the newest archive and gives capture/save timestamps, saved epoch/island clocks and exact hold lines. Earlier captures remain available.
- Read-only capture validates every referenced unit's SHA-256 and format-17/unit-10 header. It reuses only the generic NBT reader from inspect_world.py; the old format-16 inspection/trace tool was not run on this save.
- The first on-disk checkpoint was saved 23:01:59 at epoch 17999, before the first logged hold, with islands 44/45/48/61/89. Do not claim that initial copy is an exact held-state reproduction. Later normal autosave evidence is recorded in the newest manifest when available.
- The normal 23:06:59 autosave was captured at 23:08:49 local / 15:08:49 UTC in `deferred-hold-logs-2026-10-23-150849.tar.gz`: epoch 23999, islands 44/45/48/61/94. Island 94's logged committed tick is 21577 and latest residual 0.12181887846794752 (20 line-search failures, active-set pass 0). All copied units validated by SHA-256; no reproduction was attempted.
- The user then closed the client cleanly at 23:09:40 (all dimensions saved, Gradle exit 0). Preferred final archive `deferred-hold-logs-2026-10-23-151051.tar.gz` captures that saved checkpoint at epoch 27212, with island 94 still committed at tick 21577; unit hashes pass and the complete shutdown log is included.
- The agent sent no game input, opened no world and forced no save/solve or process shutdown. Capturing files/logs did not change the simulation. No test suite was run against this world; separate display-merge fixture checks began only after the client exited.

## Later debugging scope

Use the archived revision/config/local patch and saved clocks to reconstruct only a frozen copied graph, first establishing expected flows/pressures and the exact action preceding the hold. Do not infer that successive island ids alone prove one unchanged circuit. Root-cause work and any proposed fix require a later owner request.

Code guide: no change; no product code was changed for evidence capture. Tool folder: `tools/fluid-world-inspector/`.

## Resumed tracing

The owner identified a 1.5 -> 2 m3/s WTI pumping-rate change, then resumed tracing on 2026-10-24. [WTI rate review](../2026-10-24-wti-pumping-rate-hold/WTI_RATE_REVIEW.md) reproduces the failure and isolates a mismatched cached phase/mode start; its proposed numerical correction remains offline.
