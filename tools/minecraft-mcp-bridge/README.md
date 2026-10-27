# minecraft-mcp-bridge

Purpose: the Minecraft MCP control mod (langyo/minecraft-mod-mcp) that the `runMcpClient` lane pins. Batch: `documentation/2026-10-16-minecraft-mcp-mouse-input/`.

- `minecraft-mcp-1.21.1-neoforge-v0.4.18.jar`: the official release v0.4.18, unmodified, downloaded 2026-10-26 from the GitHub release. SHA-256 `e9359fce30b13e6337b8a40410ad57bdd31e1b9241ce03502bf668b380bd25f0`, identical to the release asset's published digest. This is the file `build.gradle` (`prepareMcpControlMod`) checks.
- The lane runs the bridge in its background input mode. `build.gradle` sets `mcp.background_input=true` on the `mcpClient` run (upstream issue #54, PR #57, since v0.4.9). In that mode:
  - `setCursorPos` edits only Minecraft's own cursor fields;
  - the `java.awt.Robot` fallbacks are skipped;
  - `exit_control_mode` does not lock the cursor;
  - each client tick sets a vanilla `GLFW_CURSOR_DISABLED` back to NORMAL.
- Vanilla's own grab still ran in that mode (world join, screen close: cursor to the window centre, confined for up to one tick; `cursor3.log`). The compat mixin `BackgroundCursorMixin` (`src/mcpCompat`, commit ce6923d9) skips it while the game window is not the foreground window; checked 2026-10-26 (`cursor4.log`), the cursor stays put.

Install in a worktree: copy the jar into `<worktree>/run/mcp-client-mods/`. `prepareMcpControlMod` copies it into `run/mcp-client/mods/` and deletes any other `minecraft-mcp-*.jar` there. The compat mod (`src/mcpCompat`) accepts `mcpmod` `[0.4.18,0.5.0)`.

Update to a newer release: `gh release download vX.Y.Z --repo langyo/minecraft-mod-mcp --pattern 'minecraft-mcp-1.21.1-neoforge-vX.Y.Z.jar'` into an empty folder. Compare the SHA-256 with the release asset's digest (`gh api repos/langyo/minecraft-mod-mcp/releases/tags/vX.Y.Z`). Then change the jar name and digest in `build.gradle` and the range in `src/mcpCompat/resources/META-INF/neoforge.mods.toml`, and check that every `src/mcpCompat` mixin target still exists (`ReflectedInputHandler.hotkey`, `ScreenInteractionHelper.guiClick`/`guiCharType`/`pasteText`, `PlayerWorldHelper.doUseItem`, `WindowHelper.getWindowHandle`).

`superseded-0.4.7-osguard/` holds the local build used 2026-10-16..26 (upstream 0.4.7 plus `OsInputGuard`, SHA-256 `d71b35aa…c4e7`, source commit 70bafcf on branch `fix/background-os-input` of the clone `D:/Minecraft/Modding/1.21/mcp`), with its patch. It prevented vanilla's grab while the window was in the background (focus withheld through the game's own GLFW focus callback), which the official mode does not do.
