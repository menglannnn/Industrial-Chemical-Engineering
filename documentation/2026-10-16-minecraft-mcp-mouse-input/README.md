# 2026-10-16-minecraft-mcp-mouse-input

Purpose: Owner report 2026-10-16: the minecraft MCP bridge moves the PC's mouse while the dev client is in the background; cause traced through the bridge jar and measured with a standalone GLFW probe

Status: Implemented 2026-10-26 (merged to main as 0.12.4, c038ceaa, not pushed). Index: [../INDEX.md](../INDEX.md).

## Documents

- [MCP_MOUSE_INPUT_REVIEW.md](MCP_MOUSE_INPUT_REVIEW.md)
- [BRIDGE_OFFICIAL_0418_REVIEW.md](BRIDGE_OFFICIAL_0418_REVIEW.md): official v0.4.18 background mode replaces the local fix (2026-10-26)
- [UPSTREAM_ISSUE_DRAFT.md](UPSTREAM_ISSUE_DRAFT.md): issue langyo/minecraft-mod-mcp#54 as posted

## Code areas

`build.gradle` (`prepareMcpControlMod`, `mcpClient` run), `src/mcpCompat` (range, `WindowHandleMixin`), `runMcpClient`; tools `tools/minecraft-mcp-bridge/`, `tools/glfw-focus-probe/`

## History

- In progress since 2026-10-16 (fix in the bridge on branch `fix/background-os-input` 70bafcf in D:/Minecraft/Modding/1.21/mcp, checked in game, kept local (owner); lane pinned to v0.4.7-osguard by 83254320 and cherry-picked to claude/unified-thermo-port 026422fe and claude/custom-equipment-rendering-805e7d 4ffae269; investigation: GLFW reports a background window as focused, so vanilla grab/release, bridge `scroll_at`/`mouse_drag`/`exit_control_mode` move and confine the global cursor; fix options A/B for the owner, no code changed; upstream issue langyo/minecraft-mod-mcp#54 filed 2026-10-16)
- 2026-10-26: upstream fixed #54 in #57 (v0.4.9, opt-in background input mode). The lane now pins the official v0.4.18 with `mcp.background_input=true`, and the local fix is superseded (kept in `tools/minecraft-mcp-bridge/superseded-0.4.7-osguard/`). Checked in game: the bridge tools are silent, but vanilla grab moves the cursor to the window centre at a world join or screen close. Options A-C for the owner in BRIDGE_OFFICIAL_0418_REVIEW.md.
- 2026-10-26: owner chose option A. `BackgroundCursorMixin` (ce6923d9) skips vanilla's cursor grab and release while the window is in the background; checked in game, the cursor stayed put over a join, chat cycles and the bridge tools (`cursor4.log`).
- 2026-10-26: merged to main as 0.12.4 (fast-forward to release prep c038ceaa, after main 2fbb80f2 0.12.3 was merged in as 85a46855). Gates: `test` 1419/0, `slowGates` 187/0. Code guide: client-gui, verification and the README snapshot at c038ceaa. Not pushed.
