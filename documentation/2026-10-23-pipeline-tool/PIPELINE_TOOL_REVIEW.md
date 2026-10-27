# Pipeline tool review

2026-10-23 · Implementation `d30694ed`, placement follow-up `459eda31`; merged into `claude/floating-roof-tanks-81d68b` at `b3bb813e`, not main. See `PIPELINE_TOOL_MERGE_REVIEW.md`.

## Expected behaviour

- The target is the pipe's hydraulic internal diameter in metres; 0.001–1 m, default 0.05 m.
- Shift + right-click opens settings; Save/Enter commits the target, Cancel/Escape leaves it unchanged.
- Right-click queues only the clicked IChemE pipe's diameter change; existing geometry synchronization draws it.

## Implementation and contracts

- `PipelineToolSettings` validates its scalar component in its constructor, persistent codec and wire decoder.
- `icheme:pipeline_tool_settings` persists/synchronizes through normal inventory data; each tool is independent.
- `PipelineToolMenu` captures the actual held stack, hand and main-hand slot. Moving/replacing it invalidates saves.
- `PipelineToolNetwork` protocol `pipeline-tool-1` accepts only a matching live menu and finite diameter in bounds.
- Settings are static item data: opening/saving creates no fluid subscription or process work.
- `PipelineToolItem.onItemUseFirst` precedes block interaction; Shift opens against blocks and `use` covers air.
- Empty-main-hand pipe interaction yields to an offhand pipeline tool; other hand priority stays vanilla.
- Resize reads the latest static registration and uses `FluidWorldAuthority.edit`, preserving all other geometry/device/spec fields.
- Same diameter returns without queuing an event. No new solver, checkpoint or process-presentation path.
- Creative Functional Blocks listing and `/give @s icheme:pipeline_tool`; no recipe or durability. Icon reuses vanilla shears.

## Verification

- `gradlew.bat test --tests com.wormzjl.icheme.world.item.PipelineToolSettingsTest --max-workers=4`: PASS, 1 test.
- Compiles all main/test Java and builds the jar. Bounds, full-precision NBT/wire round-trips, NaN/infinities, out-of-range and malformed storage are covered.
- Initial test compilation caught an incorrect NbtOps import; corrected, then the same targeted test passed.
- `git diff --check` clean; no tracked ignored files. Owner requested minimum tests and will do in-game verification.
- Implementation verification ran no client, GameTests, broad suites, simulations or campaigns.

## Documentation and tooling

- Branch addenda prepared in the worktree code guide: `client-gui.md` and `fluid-runtime.md`, snapshot `d30694ed` (2026-10-23).
- Main guide refresh/snapshot is deferred to the future merge; no code/version change was made to main.
- Evidence: main checkout `tools/pipeline-tool/logs/component-test-logs-2026-10-23.tar.gz`; README and tools index added.
- No one-off instrumentation, scripts, run configurations or Gradle switches were added to tracked code.
- Status: In progress since 2026-10-23; merged into the requested development branch at `b3bb813e`; owner in-game checks and main integration pending.

Owner-requested launch 2026-10-23, 10:28 SGT: started runClient from this worktree; IChemE loaded, textures and sound initialized. Client left running for owner testing; no automated in-game check performed.

## Placement follow-up (owner request 2026-10-23)

- Expected: extending 0.2 m or 0.02 m piping keeps that bore; joining 0.05/0.2 m piping chooses 0.2 m (owner choice). Standalone stays 0.05 m.
- Before: `FluidWorldAuthority.place` always registered a 0.05 m bore, regardless of connected neighbors.
- Implemented: read the six adjacent latest static registrations, choose the largest PIPE bore, and register that geometry in the initial placement event. Queued resizes/placements are included without applying events or loading chunks.
- Owner validation: place extensions and a bridge between unequal bores. Existing pipes, other equipment and the fluid algorithms are unchanged.
- Verification: `gradlew.bat jar --max-workers=4` PASS with no active dev client; no additional tests or in-game checks. Log: `tools/pipeline-tool/logs/placement-build-logs-2026-10-23.tar.gz`.
