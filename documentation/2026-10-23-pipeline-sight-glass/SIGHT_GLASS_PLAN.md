# Pipeline sight-glass accessory — accepted plan

Batch started 2026-10-23. Branch `codex/pipeline-sight-glass`, owned worktree `C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE`, base `29ff371b`. Feature delivery only; no automatic merge or version bump.

## Installation and recovery

- Add `icheme:pipeline_sight_glass`, stack 64, native model icon, translated tooltip and creative listing. No recipe.
- Either hand installs onto an existing individual pipe after server permission checks. Consume one in survival, free in creative, duplicate use consumes nothing.
- Keep the block, block entity, hydraulic identity, facing, bore, connections and pending edits. Persist only vanilla blockstate `sight_glass=false/true`.
- Survival mining returns the ordinary pipe and one accessory. Installation/removal refresh static models and adjacent flange geometry.
- Final owner decisions: **no junctions and no corners**. Accept only empty/dead-end pipe shapes or two opposite connections. A pipe becoming a corner or junction drops one accessory on a deferred block tick; the pipe and its ordinary connection remain.
- Retain normal pipe hydraulics/collision, diameter inheritance, both Pipeline Tool actions and debugger-only pipe GUI access.

## Model and appearance

- Approved reference: [Waterfitters flanged sight glass](https://waterfitters.com/en/flanged-sight-glass-1-aisi-316), paired round windows in a compact metal chamber.
- Owner rejected the initial thin circular housing. The revised model has a deeper cast chamber, chamfered square covers around round recessed windows, circular bevelled retaining lips, dark joints and four washer/nut/stud assemblies per side.
- Final orientation follow-up: installation chooses the cardinal window axis closest to the player's eye position, excluding the pipe's flow axis. Horizontal runs may have top/bottom windows when installed from above; vertical runs choose east/west or north/south windows. Save this visual orientation separately from hydraulic facing. Corners/junctions draw ordinary pipe geometry.
- Scale the body, aperture, depth, covers and fasteners from the existing pipe OD drawing key. Retain its 1/16 m minimum OD, 1/64 m drawing steps and 1 m maximum. Reuse reducers, square flange shoulders and joint seams.
- Combine accepted oil/water transport into one mass-weighted tank appearance colour; alpha floor 200/255. Empty, stopped, gas-only and unavailable displays are clear.
- For transported oil/water/gas/combined-solid volume fractions, use `h=clamp(2*(1-sum(phi^2)),0,1)`. Blend RGB toward white by `0.25*h` and alpha toward 255 by `h`. Components in one homogeneous phase do not cause haze.

## Synchronization and schedule

- Chunk-bake metal and glass separately; render only the liquid dynamically behind the windows. Installation is part of the model-cache key.
- Client requests nearest 64 equipped pipes within 120 blocks, once per second; server validates loaded position, pipe identity, installed/eligible state and 128-block range.
- Protocol `sight-glass-1`: watch/unwatch lists of position and identity, display replies with packed ARGB. Replay cached colour only on subscription, then use existing approximately five-second engine buckets.
- Use accepted transport and the existing 100-online-tick bulk-speed window. Refresh observed displays even without new simulation publication so stopped liquid clears.
- Release watches/caches on removal, unload, range/dimension changes and disconnect. Ordinary block updates carry geometry/identity only. No solver on installation, watch, replay or rendering.
- Retain checkpoint 14 / storage unit 7; no migration or process-format change. Colours and watches are ephemeral.

## Verification and delivery

- Owner instruction: no tests and no automated GUI/in-game checks. Compile/package with `gradlew.bat jar --max-workers=4` only.
- Generate actual-mesh software sheets for three straight orientations, clear/liquid/turbid states, reducers, unsupported corners/junctions and bore extremes. Store the offline renderer in canonical ignored `tools/pipeline-sight-glass/`.
- Record review, code-guide contract addendum, index entries and Unreleased changelog. Owner performs the fresh-world manual checks listed in the review.
