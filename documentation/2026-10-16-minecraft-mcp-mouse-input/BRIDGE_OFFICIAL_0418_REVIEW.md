# Minecraft MCP bridge: official v0.4.18 background mode replaces the local fix (review, 2026-10-26)

Owner request: upstream shipped a similar function; update the bridge from the official source, and update the docs and skills.

## Expectation
A client driven by an agent in the background should leave the OS cursor where the user put it, for every action. That covers the bridge's own tools, and also vanilla's grab and release, which Minecraft runs at world join, at screen close and on an in-world click.

## What upstream shipped
langyo/minecraft-mod-mcp#57 (v0.4.9; latest release v0.4.18). It adds an opt-in mode, `-Dmcp.background_input=true` or `MC_MCP_BACKGROUND_INPUT=1`. In that mode:
- `setCursorPos` writes only Minecraft's cursor fields;
- the `java.awt.Robot` fallbacks are skipped;
- `exit_control_mode` neither locks nor confines the cursor, and it also matches `grabMouse`;
- every client tick sets a vanilla `GLFW_CURSOR_DISABLED` back to NORMAL.

Vanilla's grab itself is not prevented. Our local fix did prevent it: it withheld focus from a background window, so `grabMouse` returned early.

## Change (this branch)
- `build.gradle`: the pin is now `minecraft-mcp-1.21.1-neoforge-v0.4.18.jar`, SHA-256 `e9359fce…25f0` (the release asset's published digest). The `mcpClient` run sets `systemProperty 'mcp.background_input', 'true'`, delivered through `build/moddev/mcpClientRunVmArgs.txt`.
- `src/mcpCompat`:
  - range `[0.4.18,0.5.0)`;
  - every mixin target is present in v0.4.18, all `@Pseudo`;
  - `WindowHandleMixin` stays: upstream still guesses the window handle, and background mode now calls `glfwGetInputMode` with it every tick.
- `tools/minecraft-mcp-bridge/`: the official jar is pinned. The local build, its patch and `OsInputGuard` moved to `superseded-0.4.7-osguard/`.

## In-game check (one run; client launched from this background session)
The lane loaded `mcpmod` 0.4.18 and the compat mod, and the old jar was removed. Cursor sampled at 20 Hz, 629 samples, logs in `tools/glfw-focus-probe/ingame-check/cursor3.log` and `actions3.log`:
- **World join (vanilla grab):** the cursor was moved to the window centre (1920,800) and stayed there 22.7 s, until the user moved it.
- **Chat close with control mode on:** one sample confined to the game window (1493,560)–(2347,1040), released by the next tick.
- **The bridge's own tools:** `scroll_at`, `mouse_drag` and three `exit_control_mode` calls ("background input mode, OS cursor untouched") moved nothing.

So the official mode fixes the bridge's own calls. Vanilla's grab still moves the cursor to the window centre once per world join or screen close while the game is in the background.

## Skills
No minecraft-mcp skill exists any more: the skill branch is gone, and none of the user or synced skills name the bridge. The docs updated are the code guide (`client-gui.md`, `verification.md`), `tools/minecraft-mcp-bridge/README.md`, `tools/INDEX.md`, `documentation/INDEX.md`, this batch's README and the memory note. `AGENTS.md` already points at `tools/minecraft-mcp-bridge/`, so it needs no change.

## Owner decision and result (option A)
- `ce6923d9` adds `BackgroundCursorMixin` in `src/mcpCompat` (test lane only). It cancels vanilla's `InputConstants.grabOrReleaseMouse` while the game window is not the OS foreground window, which it reads by comparing JNA's `GetForegroundWindow` with `glfwGetWin32Window`.
- In front, and on other platforms, vanilla is unchanged.
- Minecraft's own grabbed flag still changes, and the bridge's background mode resets it every tick.
- In-game check (one run, same sequence; `cursor4.log` and `actions4.log`): 718 samples over a fresh world join, two chat open/close cycles, `scroll_at`, `mouse_drag` and three `exit_control_mode` calls. None at the window centre or on a bridge target, and no confinement.
- Not checked: hand play with the window in front. That code path is unchanged.
- Merge (2026-10-26): main as 0.12.4 (c038ceaa, not pushed). Gates: `test` 1419/0, `slowGates` 187/0. Code guide: client-gui, verification and the README snapshot lines at c038ceaa.
