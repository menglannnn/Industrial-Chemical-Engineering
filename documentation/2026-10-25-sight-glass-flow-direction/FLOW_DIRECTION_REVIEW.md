# Sight-glass flow direction review

Status: In progress since 2026-10-25; feature delivered as `e5591c04` on `codex/sight-glass-flow-direction`, base `f658697c`. No integration/main merge or version bump.

1. Expected: entrained gas travels with the accepted straight-section flow, including downward and reversed flow. Looking through the opposite window must preserve the same world direction.
2. Previous code: rings always advanced along local window Y. The colour/strength-only sample could not describe actual transport. Rear-window rotation also mirrored horizontal travel.
3. Implemented: compiler link directions come from actual physical/nozzle contacts. Segment traversal and graph swaps produce a local graph-forward cardinal in ephemeral `View` metadata. Accepted `PipeConnection.reverse` selects the actual direction. Corners, synthetic and nonfinite split-half taps do not invent direction.

`Sample(argb,bubbles,flowDirection)` adds a validated explicit cardinal to `sight-glass-3`: target, ARGB, unsigned strength and direction bytes (0 unavailable; 1 down, 2 up, 3 north, 4 south, 5 west, 6 east). Invalid codes and visible motion without direction are rejected. Checkpoint19/unit12, fluid-14 and accessory storage1 are unchanged. No migration, world test, solve, new process state or ordinary block-update contents were added.

The renderer projects flow onto the selected window plane and mirrors back-window horizontal centres. Ring count, gas/liquid-ratio strength, haze, clipping and illustrative travel periods are unchanged. Paired windows agree physically. Requested subscriptions/cached replay and five-second publication/recent-flow clearing remain. An interval with reversals uses the existing dominant accepted-transport direction; it does not invent subinterval motion or physical slip/bubble velocities.

Verification: owner saved/closed client; focused `SightGlassDisplayTest` (5) and `PhysicalFluidTopologyTest` (20), 25 pass / 0 failures/errors/skips; package built. Covers explicit wire codes, unavailable/stopped samples, all world directions and window orientations, graph swaps, bent overall runs and remote/elevated constructed nozzles. Independent review checked quaternion signs and rear-face mirroring. No full-suite or in-game pass is claimed.

Software preview: 56 frames x12 native housing/window/run/direction configurations. Front/rear and top-window drawings inspected; production aperture/path/colour are used, Java2D shading is approximate. Tools/evidence: `tools/sight-glass-flow-direction/`, including compressed verification logs/XML. No tracked investigation probe/task/exclusion was introduced or detached.

Code guide: README, client-gui, fluid-runtime, equipment and verification updated from implemented source. Owner gameplay check remains: visible travel agrees with network flow and reverses after the next scheduled publication.

Integration update2026-10-25: merged as8b2112cf into claude/floating-roof-tanks-81d68b with latestfffbd514 equipment/ladder work preserved. Full quick1419/0, slow187/0 andpackage pass; target clean. Main pending; no version bump/client launch. See tank-gauge-enclosure/MERGE_REVIEW.md.
