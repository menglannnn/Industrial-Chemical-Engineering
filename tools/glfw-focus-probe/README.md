# glfw-focus-probe

Purpose: shows that GLFW (LWJGL 3.3.3, Minecraft 1.21.1's version) reports a window shown by a background process as focused, and so moves the global OS cursor on `glfwSetCursorPos` / `GLFW_CURSOR_DISABLED`. Batch: `documentation/2026-10-16-minecraft-mcp-mouse-input/` (review `MCP_MOUSE_INPUT_REVIEW.md`). Never in a tracked path.

- `Probe3.java`: no cursor calls; prints `GLFW_FOCUSED` every 500 ms for 4 s. Run `fg.ps1` at the same time to print the real foreground window.
- `Probe.java`: moves the cursor into the window, then disables the cursor (about 0.5 s), then restores it. Do not run while someone is using the mouse.

Run (Git Bash, LWJGL jars from the Gradle cache, classpath = lwjgl, lwjgl natives-windows, lwjgl-glfw, lwjgl-glfw natives-windows 3.3.3):
`java -Dsun.java2d.uiScale=1 -cp "<jars>" Probe3.java` and `powershell -File fg.ps1`.

`ingame-check/` (2026-10-16): `sampler.ps1 -ClientPid <pid> -Out <log> -Stop <file>` samples the OS cursor, the client's window centre, foreground and ClipCursor at 20 Hz (read only). Logs from the check of the bridge fix (`fix/background-os-input` 70bafcf in `D:/Minecraft/Modding/1.21/mcp`): `cursor.log` title → create world → join, `cursor2.log` with `actions.log` (bridge calls at ms timestamps). Lane: `runMcpClient -x prepareMcpControlMod` with the patched jar in `run/mcp-client/mods`, `JAVA_TOOL_OPTIONS=-Dmcp.port=9871`, the compat mod's `mcpmod` range temporarily widened to `[0.3.0,0.5.0)` (reverted).

## Index record (moved from tools/INDEX.md on 2026-10-26)

Purpose: Standalone LWJGL 3.3.3 probe (no Minecraft, no Gradle): a GLFW window shown by a background process reports itself focused and moves or confines the global cursor

Batch: [2026-10-16-minecraft-mcp-mouse-input](../../documentation/2026-10-16-minecraft-mcp-mouse-input/)

Status: Concluded 2026-10-16. Index: [../INDEX.md](../INDEX.md).

### Entry points

`Probe3.java` + `fg.ps1` (no cursor moves), `Probe.java` (moves the cursor), README.md

### History

- Concluded 2026-10-16

2026-10-26: `cursor3.log` + `actions3.log` are the check of the official v0.4.18 background input mode (`mcp.background_input=true`). The cursor was moved to the game window's centre at world join (held 22.7 s until the user moved it) and confined for one sample at a chat close. The bridge's own tools did not move it. `cursor4.log` + `actions4.log`: the same sequence with `BackgroundCursorMixin`, nothing moved.
