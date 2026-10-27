# Square flange shoulders

2026-10-23 · Owner request from an in-game screenshot; owner confirmed removing the sloped neck to make a 90° shoulder.

- Use the shared pipe/flange sizing policy so pipe spool joints, pipe ends and equipment-nozzle halves agree.
- Keep the neck at pipe OD until the flange plate; preserve flange OD/thickness and the existing straight-neck length.
- Owner follow-up: draw a dark junction line between paired flange plates, including spool joints; keep blind caps as single plates.
- Owner follow-up: open pipe inspection only with the existing Fluid Debugger. Plain pipe clicks pass through; the Pipeline Tool retains right-click resizing and its own Shift + right-click settings.
- Keep hydraulic bore, topology, process calculations, item/menu/wire/save formats unchanged.
- Work on the owned `codex/pipeline-tool-integration` worktree, then fast-forward `claude/floating-roof-tanks-81d68b`, preserving its unrelated local equipment edits.
- Use actual-mesh software renders for before/after judgement and build the jar. Owner update: no tests needed; stop the already-started merge suites and do not run further tests.
- Keep in-game verification owner-run. The running client's Java geometry requires a restart after the source update.
- Record the plan/review and code-guide addenda in this batch; archive offline rendering sources and logs in `tools/square-flanges/`.
