## Pipeline tool — branch addendum

**Snapshot:** b3bb813e (integrated into `claude/floating-roof-tanks-81d68b`), 2026-10-23. Refresh the main chapter snapshot when this batch reaches main.

`ModDataComponents` registers persistent/network-synchronized `icheme:pipeline_tool_settings`, a validated `PipelineToolSettings` internal diameter in metres (inclusive 0.001–1, default 0.05). `ModItems.PIPELINE_TOOL` is a reusable single item; `ModMenus.PIPELINE_TOOL` is a slot-free item settings session. `PipelineToolNetwork` owns the additive `pipeline-tool-1` settings protocol; fluid wire/checkpoint versions are unchanged.

Normal right-click uses `PipelineToolItem.onItemUseFirst` before a pipe's default GUI. The server checks build permission, spectator status and `Level.mayInteract`, reads `FluidWorldAuthority.at` (latest static registration without flushing events), verifies the loaded block's identity and PIPE kind, then queues `FluidWorldAuthority.edit`. The replacement changes only geometry.diameter; length, roughness, minor loss, identity, facing, ports, control and spec stay intact. The same diameter queues nothing. Existing `RegistryHost.geometry` and block-entity geometry/neighbor synchronization own the visual refresh after commit; no solver or publication is forced.

An empty main hand in `FluidDeviceBlock.useItemOn` yields its default interaction when an offhand pipeline tool is present, allowing the offhand's early hook. Shift always configures the tool, including against other blocks. The configuration menu/packet operates only on static item state and never subscribes to fluid contents or sends an edit acknowledgement.

`FluidWorldAuthority.place` also chooses a new PIPE's bore before queuing its registration: `placedPipeDiameter` reads the six face-adjacent latest static registrations and copies the largest PIPE bore (owner choice when they differ). All pipe-to-pipe faces connect regardless of facing. With no adjacent pipe it uses 0.05 m; a smaller neighbor remains smaller than that default. Other device kinds retain their defaults. Pending edits are read without applying them, and there is no follow-up resize event or chunk read.
