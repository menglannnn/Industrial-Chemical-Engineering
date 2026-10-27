# Floating-roof tank indicator and ports

2026-10-24. Branch codex/floating-roof-indicator-ports in the owned worktree, based on integration 35bad9b8.

- Owner reports the liquid gauge is invisible from below/side. Expected: oil/water bands and the red level collar remain readable from exposed exterior sides and below, while the shell still occludes the far side.
- Trace: the hand-built four vertical coloured faces wind inward under entityTranslucentCull; their declared normals disagree with winding. Only the top winds outward, and the bottom is absent.
- Reuse the existing Mesh box primitive for a cached closed six-face unit column. After owner feedback on overall presentation, use steel corner rails, an unpainted pale .52 m scale with dark ticks, clear unfilled window, .24 m liquid strip and a slim .32 m x .07 m red collar. Keep published liquid colours, opacity floor and tank-watch timing; previews use the real tank colour reducer rather than fixed demonstration tints.
- Owner confirms bottom port 1 centered front and port 2 centered back, opposite each other. Retain identities/order, both LIQUID roles, lowest row, front OVERFLOW and the fluid node's world position. Decouple the stair approach setback from nozzle centers; stair planning clears both bottom ports and front overflow.
- Use existing EquipmentRefresh layout stamps and queued re-registration, with no custom migration, save/wire format or physics changes. Preserve origin, paint and nozzle sizes by existing layout-refresh contract.
- Compile/package and compile gate sources while the existing integration client stays running; no test suite or game interaction. Offline winding/port/access sweeps and actual mesh software renders decide drawing-only work. Update the existing port gate plus one focused winding regression; execute these/EquipmentRefreshTest after the dev client exits, before any merge.
- Deliver feature branch with Unreleased entry, short review, index/code-guide contract update, and compressed local tools evidence. No automatic merge/restart/version bump.
