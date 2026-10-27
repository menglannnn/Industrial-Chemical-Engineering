# Fluid format mismatch reset — plan

2026-10-23; branch `codex/fluid-format-reset`, base integration `ab35ef47`.

1. Expected: opening a world with an incompatible fluid checkpoint starts a new simulation and reconstructs its placed devices instead of terminating Minecraft. Reservoirs start empty and controls take defaults; placed geometry, paint, nozzle sizes and accessories remain.
2. Previous code: a version refusal escaped SavedData loading, and server startup rethrew it as a fatal failure. The owner-generated 16:17:29 crash used checkpoint 14 with a format-15 build.
3. Owner explicitly selected: “Reset fluid simulation and rebuild it from placed blocks; existing fluid inventories are lost.” This overrides fresh-world-only delivery for the reset policy, without authorizing migrations or old-world testing.

- Keep strict current-format decoding and validation; distinguish checkpoint/pack/unit format mismatch from damaged data. Catch only that mismatch in world opening and discard its entire authority, including settings, clocks, certificates, cake, pending edits and recoveries.
- Start a fresh store whose new pack numbers skip retired files. Replace the core at the ordinary atomic save; retire obsolete packs only after the new core commits.
- Queue chunk notifications onto the server thread, retain pre-authority notifications and reconstruct missing registrations from placed cells. Read no old-format body, force no chunks, run no solve on load, and keep existing engine scheduling/publication rules.
- Bind existing identities at matching positions. Validate all shell/interior cells before creating a constructed registration; defer structures whose chunks have not arrived; partial/stale structures use existing construction/refresh paths.
- Compile/package only, no tests or automated GUI checks. Owner performs world checks. Carry the fix to the current requested integration branch, preserving its local changes, then leave its client running.
- Record changelog, review, guide contracts, index and compressed logs. No main merge or version bump; binary checkpoint remains 15 / unit 8.
