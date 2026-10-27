# Functional pump and compressor equipment — review

Date: 2026-10-23. Code: `4e96e8ab`, branch `codex/functional-pumps-compressors`, base `7bf50b7c`.

## 1. Expected behavior

A machine passes fluid from suction to discharge while adding the existing ideal mover work. At the default pump target and reference-water pressure-rise cap, Q Δp = 0.01 × 500000 = 5000 W. A compressor at 101325 Pa inlet, 0.05 m³/s and ratio 3 has the existing ideal isothermal work of P Q ln(3) ≈ 5566 W; actual flow/rise still depends on its network. Mass, components, phase protections and existing energy accounting remain the engine's responsibility.

## 2. What the code did instead

The large models drew nozzles but only storage equipment registered with the fluid engine. The physical adapter accepted multi-cell ports only on reservoirs and inferred a mover's discharge from its facing and node coordinates. That inference cannot represent these models' perpendicular, spatially separated nozzles. The fluid GUI also restricted its datasheet tab to tanks.

## 3. Implemented owner-approved proposal

- All three machine designs now register one PUMP/COMPRESSOR with precisely one bulk SUCTION and DISCHARGE; oil-cooler ports are excluded.
- The suction flange cell binds the identity and establishes the hydraulic elevation. Compilation marks the actual discharge contact's link, removing coordinate-based actuator inference while retaining one-block facing behavior.
- Default controls reuse the same placement factory as one-block devices. Their phase trips, pressure limits, work/heat treatment, geometry and pipe-bore inheritance are reused without solver changes.
- Whole construction registers; dismantling unregisters; load rebinds saved records. Refresh preserves controls in place or queues an atomic old/new identity replacement when the suction binding moves.
- Normal machine clicks open existing controls and the datasheet; the Fluid Debugger opens diagnostics from shell or interior. Menu titles use the actual equipment kind. Pipe inspection and Pipeline Tool handling are unchanged.
- Port roles use stable codes (vessel 0, suction 1, discharge 2), validated on decode and by device invariants, in active and pending registrations. Checkpoint 14 / unit 7 requires a fresh world; no migration.
- Existing queued edits, server validation, replay-only menu opening and scheduled publication remain the integration path.

## Verification and limits

- `gradlew.bat jar --max-workers=4`: PASS, including Java compilation, resources and packaged jar. `git diff --check`: clean.
- No tests added or run; no in-game checks performed, following the owner's accepted plan.
- Static review covered explicit-role validation, both link endpoint orderings, reservoir phase ports, one-block facing equivalence, event encoding, identity reload and moved-node replacement.
- Owner manual checklist on a fresh world: all three machines and headings; suction → discharge through perpendicular/elevated ports; edit flow and rise/ratio limits; wrong-phase protection; disconnect/reconnect; save/reload without duplicate devices or lost controls; dismantle; normal/debugger GUI and datasheet; existing one-block devices and Pipeline Tool actions.
- Cosmetic animation remains the existing client behavior. Auxiliary cooler nozzles do not exchange fluid or heat.
- Code guide: branch contract addendum in `CODE_GUIDE_ADDENDUM.md`; fold it into canonical equipment/runtime/client chapters at a future main merge.
- Tool folder: `tools/functional-pumps-compressors/` contains compressed build evidence only; no probes, harnesses or temporary product code were introduced.
- Initial delivery was feature branch `29ff371b`; the owner subsequently requested integration into `claude/floating-roof-tanks-81d68b`, now fast-forwarded to that commit with all local work preserved (see `MERGE_REVIEW.md`). Main and mod_version remain unchanged; owner in-game checks and future main integration are pending.
