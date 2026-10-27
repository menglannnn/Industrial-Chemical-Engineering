# Pipeline check-valve review

2026-10-24. Sources 4e5c504d / arrow correction 9ffb294f, integrated cb89f23a -> b70e92aa on claude/floating-roof-tanks-81d68b. Mod version remains 0.11.2. See MERGE_REVIEW.md.

## 1. Expected behaviour

Forward pressure drive passes fluid through ordinary pipe resistance; reverse drive closes transport. The arrow selects the permitted direction independently of pressure, gravity or fluid phase. Direction changes are queued; finite equipment contents and pipe identity/settings survive installation and reversal.

## 2. Existing code and implementation

- The shutoff accessory already installed onto straight pipes and retained identity/settings, but only provided open/closed cutoff. Reusing its temporary solids-block mask for a check would let NetworkReduction.reopened clear the restriction.
- PipelineValveItem shares installation/permission/consumption logic between PipelineShutoffValveItem and PipelineCheckValveItem. CHECK is physical cutoff code 3 and the existing blockstate's check value. Facing is the allowed arrow; aim selects its axial sign. Both-empty-hand use queues a reversal from latest state; other held items retain behavior. Accessories are mutually exclusive.
- PhysicalFluidTopology pins check pipes as existing-type pressure junctions through TopologyCompiler's retained-pipe set. Permanent masks follow each incident run and reverse with discharge-driven endpoint swaps. Ordinary geometric resistance is retained; no new K, cracking pressure or solver model. Existing junction-buffer rebuild rules remain.
- PassiveNetwork.Pipe.forbiddenDirections is independent of transient blockedDirections; both enter effectiveBlockedDirections/blocked(flow). Copy methods, identity, carried-flow key, certificate checks/signature and solids guards preserve the fixed mask. Ordinary pipes use zero, so their direction rule is unchanged. Permanent restrictions are not presented as solid blockages.
- Checkpoint 18/unit 11 encode permanent restrictions in scientific topology plus CHECK in active/pending registrations; readers bound both masks to 0-3 and reject unknown cutoff codes/non-pipe fittings. No wire payload/protocol change, compatibility reader or migration. Use a fresh world; existing incompatible-save reset behavior loses fluid state.
- CheckValveGeometry uses the existing pipe-size conventions, flanges/reducers, cast housing and painted arrows. No external lever or text. The chunk key includes check state and signed facing; the accessory icon uses a larger display casting. Collision remains ordinary pipe collision. Creative listing, English tooltip/name and survival loot are added; no recipe.
- Owner's render feedback identified misplaced arrows. 9ffb294f fits each badge to the actual flat casting facet's normal, width and axial bounds, moves the casting seam outside the badge band, and keeps physical badge locations/casting fixed while reversing glyph direction. Paint lifts are the existing 0.001-0.002 m stencil offsets, rather than tangent planes at the cylinder's outer vertex radius.

## Verification

- 13 focused tests pass: four PipelineCheckValveTest methods, four existing PipelineShutoffValveTest, two RetainedSolverTest and three PressureValveContinuationTest. Package generation passes. No active dev client was detected; no suite overlapped a client.
- Fresh tests verify real forward/reverse baseline flow on all six directions, forward passage/reverse zero, unchanged identity/equipment inventory, axis locking, duplicate install, rapid reversals with diameter edits, complete checkpoint/clock/topology round-trip including pending state, permanent-mask persistence/reopening, malformed registration codes, driven-end swapping and opposed checks blocking both ways. No forced engine commands during edits.
- Software render sheets inspected: X/Y/Z, both signs, 0.001/0.05/1 m, 0.25 m detail, item casting and reducer. These show production geometry, not a separate concept asset. No in-game/GUI check or new GameTest. Required merge suites completed: quick 1372/four known failures; slow 185/one known failure; no new failure. No new solver-form campaign.
- Final arrow source compiles/packages successfully; the drawing-only correction was verified with rebuilt render sheets. The offline renderer now uses per-pixel depth testing, since average-polygon sorting could incorrectly hide coplanar paint on the reverse-facing view. No process behavior changed after the 13 passing focused tests.
- Tooling: tools/pipeline-check-valve/CheckValveRender.java, README and compressed build/test/render logs. No tracked diagnostic source or ignored file was staged. No unrelated cold-430/431 investigation changes are included.

## Owner manual checklist

Install with either hand; aim/flip arrows on horizontal/vertical runs; check survival consumption/recovery and duplicate rejection; reject bends/junctions/competing accessories; verify side pipes cannot attach, sizes/reducers and tool/debugger interactions remain intact; forward/reverse liquid/gas flow, saved direction and repeated pending reversals behave correctly on a fresh world.
