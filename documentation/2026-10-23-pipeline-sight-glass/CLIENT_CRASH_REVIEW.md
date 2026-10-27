# Client launch and reported crash

2026-10-23; branch `codex/pipeline-sight-glass`, tip `57a55623`.

1. Expected: this development integration reads fluid checkpoint 14 / storage-unit 7. The accepted functional-machine plan and repository rules require a fresh world, with no compatibility reader or migrations.
2. Observed: the owner's earlier launch opened `run/saves/New World` with checkpoint 12. The existing save reader rejected it at `FluidCheckpointCodec.readCore:595`; `FluidSavedData.open:145` refused to replace its inventories. This caused the integrated-server crash at 13:54:03. It is not a sight-glass model/registration exception.
3. Action: archived the existing crash evidence and launched `gradlew.bat runClient --max-workers=4` from the owned sight-glass worktree. The new client loaded models/resources and remains running for the owner. A fresh world is required; no world was opened by the agent and no old save was changed.

No product-code change, tests or automated GUI/world checks. No other worktree was modified. Client exec session: `65025`; bootstrap client PID at startup: `21876`.

Evidence: canonical `tools/pipeline-sight-glass/logs/old-world-crash-logs-2026-10-23.tar.gz` and `client-startup-logs-2026-10-23.tar.gz`; live launch log `C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE/build/sight-glass-client-launch.log`.
