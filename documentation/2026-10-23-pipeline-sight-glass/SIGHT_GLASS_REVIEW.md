# Pipeline sight glass — delivery review

2026-10-23; branch `codex/pipeline-sight-glass`, base `29ff371b`; code `465f8af8`, `e1bffe54`, player-facing follow-up `065683c6`.

## Result

- Accessory installs on the same pipe block/entity with survival consumption and recovery; no hydraulic re-registration, inventory or geometry-setting edit.
- Final owner restrictions are enforced: straight sections only; corners and junctions reject installation. A later incompatible connection removes the flag and drops one accessory once, after rechecking the final state on a block tick.
- Revised the owner-rejected thin-ring model into a deeper cast chamber with chamfered four-bolt covers, recessed circular windows and bevelled retaining lips. All fitting dimensions follow the existing pipe size key.
- Installation now saves the nearest perpendicular window axis toward the installer, including top windows on horizontal runs. Static housing/glass and liquid share the same orientation key; see `ORIENTATION_REVIEW.md` and `renders/orientations.png`.
- One liquid colour uses the tank's mass-weighted appearance mixing and 200/255 opacity floor. Phase-volume haze affects colour/opacity only; gas-only or no recent flow gives no liquid overlay.
- Client explicitly requests colour. Server validates identity, installation, shape, loaded cell, dimension, reach and the 64-device limit. Cached replay never reads/advances simulation.
- Observed pipes reuse scheduled presentation buckets and the existing five-second accepted-volume speed window; ordinary block updates contain no process contents.
- Server-owned subscriptions and presentation state remain on the server thread. Client maps/weak bindings remain on the client thread; chunk model quads use the existing concurrent cache pattern.
- Fluid checkpoint 14, storage unit 7, existing fluid menu protocol and mod_version 0.11.2 are unchanged.

## Verification

- Final `gradlew.bat jar --max-workers=4`: PASS, Java/resources/package generation, no tests.
- Changed JSON parses; `git diff --check` clean; no ignored files staged. No tests or GameTests added or run. Owner later requested client launch; the feature client starts and loads resources. No automated GUI/world checks performed.
- Actual-mesh software renders inspected: `renders/preview.png`, `views.png`, `sizes.png`, `details.png`. These use flat software shading and example liquid colours, not Minecraft textures/shaders.
- Sizes cover 0.001, 0.05, 0.25, 0.5 and 1 m bores. The two smallest share the existing 1/16 m drawn-OD floor; the hydraulic bore is unchanged. Ordinary corners/junctions have zero glass/liquid polygons.
- Display-policy samples: single liquid `C892BED2`, 50/50 liquid/gas `FFADCEDD`, gas-only `00000000`.

## Owner checks and limits

- Fresh world: either hand; survival consume/drop/refund; creative free install; duplicate use; protected pipe rejection; corner/junction rejection and accessory drop when a new connection changes the shape.
- Confirm identity, diameter and pending edits survive installation/reload; resize with Pipeline Tool; check all three straight axes, end pipes, reducers, neighboring flange seams and minimum/maximum sizes.
- Run liquid, two liquids, liquid/gas and suspended solids; check homogeneous mixtures, gas-only, stopped flow, disconnect/reconnect, chunk reload, dimensions and watch-range changes. Stop clearing follows the accepted-flow window plus scheduled publication.
- Confirm debugger-only inspection and both Pipeline Tool actions remain unchanged. In-game drawing and multiplayer behavior remain owner checks.
- Code guide: branch addenda for equipment/rendering, client contracts and runtime/network are in `CODE_GUIDE_ADDENDUM.md` and the owned chapter copies; canonical main chapters await the future main merge.
- Tool cleanup: only product code/assets are tracked. `tools/pipeline-sight-glass/` holds the never-tracked software renderer, run instructions and compressed build/render logs.
- Initial delivery was feature-only. The owner subsequently requested target integration and a running client: `claude/floating-roof-tanks-81d68b` is now `b3b7a347`, with all ten local files preserved and target jar passing. Client left running; see `MERGE_REVIEW.md`. No main merge or version bump.
- Follow-up crash diagnosis: the owner's `New World` contains fluid checkpoint 12, while this build requires 14. Archived the existing crash, relaunched feature `runClient`, and advised a fresh world. No compatibility code or old-world run was added; see `CLIENT_CRASH_REVIEW.md`.
