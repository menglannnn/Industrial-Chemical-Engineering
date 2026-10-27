# Sight-glass flow direction

Status: In progress since 2026-10-25. Owned branch `codex/sight-glass-flow-direction`, base `f658697c`.

- Expected: visible gas bubbles travel along the straight pipe's accepted fluid direction, including downward/reverse flow. Both windows show the same physical travel.
- Defect: bubble paths always advance along local window Y; the published sample has colour/strength but no direction. Back-window rotation additionally mirrors horizontal travel.
- Change: retain local graph-forward cardinal direction in ephemeral physical inspection mappings, using actual nozzle contacts and compiler traversal. Invert by accepted transfer direction when producing the existing scheduled sight-glass sample.
- Add one validated direction byte in `sight-glass-3`; retain requested cache/replay, recent-flow clearing and nearest-64 watches. No process-payload, checkpoint, unit, solver or stored-inventory change.
- Project local bubble travel along the accepted cardinal direction for every permitted window/run orientation. Mirror horizontal centres on the back window.
- Verify focused display/codec/path and topology mapping checks, package generation, native software animation. No in-game check or old-world test. Client saved/closed by owner before Gradle tests.
- Deliver feature branch; no automatic merge or version bump.
