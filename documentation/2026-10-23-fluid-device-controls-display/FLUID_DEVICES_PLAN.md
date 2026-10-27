# Fluid device controls and displays plan

Date: 2026-10-23. Owner request: show voided composition, default-Off generator switch, uniform /s and /h flow units.
Branch: `codex/fluid-device-controls-display`, owned worktree based on `763939e8`.

1. Show the Fluid Void's accepted incoming phase/component/solid transport, not its fictitious pressure-reference inventory. Sum independently timed boundary islands as rates.
2. Add a strict persistent generator-enabled flag to physical registrations. Fresh world placement is Off; queued edits open/close hydraulic contacts and retain source settings. Keep constructed mover controls unchanged.
3. Reuse the existing required Power control and server permission/revision checks. Preserve cached replay, requested subscriptions and scheduled publications; opening and unit toggles never solve.
4. Add a session-wide client flow time basis, default /s. Convert summaries, connection tables, component/solid transport and suction-flow drafts consistently; convert edits back to m3/s. Keep inventories in kg/kmol and velocity in m/s.
5. Preserve source power during rotation. Validate the new flag in active and pending registrations; checkpoint 16 / unit 9, menu fluid-13. No compatibility or migration; existing owner-approved reset handles incompatible formats.
6. Add translations, an Unreleased changelog line, this plan, a short review and code-guide branch addenda.
7. Owner preference: no tests or automated GUI checks. Compile/package only (`gradlew jar --max-workers=4`); leave fresh-world in-game checks to the owner.
8. Deliver the feature branch without automatic integration/main merge, version bump, or changing the running integration client.

Owner follow-up, same batch: add the existing four-phase ratio bar to the generator feed display; show reservoirs as current / maximum litres, using State.volume / State.capacity and a full-value hover hint. Compile/package only. Implemented in `89168361`.

Owner delivery follow-up: merge to `claude/floating-roof-tanks-81d68b` and run the client. Completed at `232b0466`; details in INTEGRATION_REVIEW.md.
