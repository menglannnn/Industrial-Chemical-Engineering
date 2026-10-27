# Sight-glass orientation — follow-up review

2026-10-23; code `065683c6`, branch `codex/pipeline-sight-glass`.

- Expected: on installation, a transparent window faces the installer as closely as the pipe's perpendicular cardinal axes permit, and then retains that orientation.
- Before: window normals were fixed from the run, with north/south windows on every vertical pipe, irrespective of player position.
- Implemented: server-side selection from eye position relative to block centre, excluding the run axis; new persisted `sight_glass_axis` enum property. Pipe `FACING`, identity, controls and registration remain unchanged.
- Both opposite windows share the selected axis. Top/bottom viewing is supported on horizontal pipes, including valid perpendicular frames for metal, fasteners, glass and liquid.
- Chunk and liquid caches now use `SightGlassGeometry.Key(pipe, window)` through one shared `FluidDeviceModel.sightGlassKey`. Parallel-axis inputs use a perpendicular geometry fallback; no migration or old-world check was added.
- Duplicate accessory use still does not rotate or consume. Straight-only eligibility and automatic corner/junction recovery are unchanged.
- `gradlew.bat jar --max-workers=4`: PASS; `git diff --check`: clean; no tests added or run, no automated GUI/world checks.
- Inspected `renders/orientations.png`: X/Y/Z runs, both permitted window axes and opposite installer sides. The production selector yields Z/Y for X runs, Z/X for Y runs, and X/Y for Z runs. Glass apertures remain open and aligned with the camera.
- Owner manual checks: install from each side and above/below, save/reload orientation, passing liquid alignment, size changes and unchanged tool interactions. Existing installed windows do not follow the player or rotate on duplicate installation.
- Code-guide contract addendum, accepted plan, Unreleased line and documentation/tool indices updated. Fluid checkpoint 14 / unit 7 and protocols unchanged; no merge/version bump.
- Offline tool: canonical ignored `tools/pipeline-sight-glass/SightGlassRender.java`; build/render logs in `logs/orientation-logs-2026-10-23.tar.gz`. Nothing detached from tracked product source.
