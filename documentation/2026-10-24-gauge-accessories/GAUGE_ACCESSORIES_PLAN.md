# Pressure and temperature gauge accessories

Approved 2026-10-25; batch started 2026-10-24. Branch codex/gauge-accessories, base 1ba146c4.

Implement passive round-dial gauges and reusable accessory wrench. One accessory per pipe; separate stable instrument taps on vertical drums (P top/bottom, T bottom), floating-roof tanks (P/T bottom), spheres (P/T top). SI endpoints, exact held-stack editor, absolute/gauge pressure, Celsius/Kelvin.

Shared mount geometry governs installation, ghosts, hover and removal. Scheduled persistent attachment transactions revalidate reach, permissions, identity, slot revision and held items before consumption/refund. Preserve settings and identity through refresh/removal. Existing valve removal uses queued topology edits.

Reconstruct accepted pipe pressure at its hydraulic datum without new nodes/flashes; pruned/disconnected pressure unavailable, stale no-flow temperature retained ephemerally. Requested instruments-1 watches replay cached samples, publish on five-second buckets. Accessory storage 1; checkpoint18/unit11/fluid-14 unchanged.

Focused codec/mount/measurement/lifecycle/persistence tests, package generation and software render sweeps; at most one fresh-world pinned-MCP check. Broader gameplay left to owner. No automatic merge or version bump.

Owner refinement 2026-10-25: highlight/ghost previews apply to constructed equipment only. Pipe installation, rendering, hover values and wrench recovery remain supported.

Owner refinement 2026-10-25: replace powerful neon/cuboid highlights with faint native-material previews and thin muted circular accents.
