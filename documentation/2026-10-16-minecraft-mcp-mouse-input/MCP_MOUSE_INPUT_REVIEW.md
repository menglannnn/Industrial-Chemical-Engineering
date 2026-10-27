# Minecraft MCP bridge: why the PC mouse moves while the game is in the background (review, 2026-10-16)

Owner report: driving the dev client through the MCP bridge moves the PC's mouse. The game window is not brought to the front; the cursor still moves. Investigation only, no product code changed. Code guide: no change.

## Expectation
The OS cursor is a global resource. A background window should never move it. GLFW is meant to apply `glfwSetCursorPos` and `GLFW_CURSOR_DISABLED` only to the focused window, and an agent-driven client is never the window the user works in.

## What happens instead (measured)
1. GLFW on Windows decides "focused" with `GetActiveWindow()`, which is per thread. A window shown by a background process becomes its own thread's active window without being the foreground window. Probe (`tools/glfw-focus-probe/`, LWJGL 3.3.3 = Minecraft's GLFW, no Minecraft, no Gradle) checked this every 500 ms for 4 s. The real foreground window was another Minecraft window, and the probe window still reported `GLFW_FOCUSED=1` throughout.
2. With that flag set, `glfwSetCursorPos` calls `SetCursorPos` and moved the global cursor exactly to the requested window point. `GLFW_CURSOR_DISABLED` moved it to the window centre and confined it there (`ClipCursor`). The probe restored the cursor afterwards.
3. Minecraft receives the same focus flag, so `isWindowActive()` is true for a background dev client.

## Which game and bridge actions reach those calls
The bridge jar `minecraft-mcp-1.21.1-neoforge-v0.3.0.jar` was disassembled with javap.
- Vanilla `MouseHandler.grabMouse`/`releaseMouse` → `InputConstants.grabOrReleaseMouse` → `glfwSetCursorPos(centre)` + cursor mode. Triggers:
  - world join and `close_screen`/resume: grab (centre, hide, confine);
  - a GUI opening, e.g. `use_item` on a block: release (centre);
  - a bridge `click` with no screen open: `onPress` → grab.
- Bridge `scroll_at` and `mouse_drag` → `InputInjectionHelper.setCursorPos` → `glfwSetCursorPos(x, y)`.
- Bridge `exit_control_mode` → `mouseGrabbed=true` + `glfwSetInputMode(GLFW_CURSOR_DISABLED)` directly. Its `lockCursor` lookup matches Yarn/SRG names only and does nothing on NeoForge.
- Bridge `java.awt.Robot` fallbacks (`mouseMove`, `mousePress`, `mouseWheel`, `keyPress`) run only if reflection fails or the window handle is 0. No log showed one.

Not a cause:
- The camera tools (`look_delta`, `set_view_angle`) are in-process.
- The per-tick `tickMcpControlMode` is never registered in this build.
- The node bridge 0.4.7 is HTTP only.
- `robot.ps1`/`Poke` were not used after 2026-10-13. They are real desktop input and are already banned for unattended agents.

## Plain-words summary
GLFW believes the background Minecraft window has focus. Each time the game grabs or releases the mouse (world join, opening or closing a GUI, some bridge tools), it moves your real cursor to the centre of the Minecraft window, and when it grabs it also confines the cursor there.

## Proposition for the owner (superseded)
Options A (compat mixins in `src/mcpCompat`) and B (window created unfocused) were proposed. The owner then chose to fix the bridge itself (next section). A fix at the source covers every loader and lane, so no compat mixin is needed.

## Fix in the bridge (2026-10-16, owner request)
Upstream issue langyo/minecraft-mod-mcp#54. Fix on branch `fix/background-os-input` (commit 70bafcf) in `D:/Minecraft/Modding/1.21/mcp`, from upstream 0.4.7. It is not pushed. New `OsInputGuard` in `common` reads the real foreground window (JNA User32) and does three things:
- gates `setCursorPos`, the Robot fallbacks and the `exit_control_mode` grab on it;
- in the client tick, hands a background window focus=false through the game's own GLFW focus callback (true once it is in front);
- also matches `grabMouse` and uses `glfwGetWindowPos`.

In-game check, background client launched by the agent, cursor sampled at 20 Hz (`tools/glfw-focus-probe/ingame-check/`):
- No sample on any bridge target during title, world creation and join, chat open/close, `scroll_at`, `mouse_drag`, `use_item` and `exit_control_mode` ("window in the background, cursor not grabbed").
- No clip to the game window.
- When the user brought the window to the front (2.8 s), focus was handed back.
- No baseline run with the old jar (it would move the owner's cursor); the probe above shows that mechanism.

Lane pinned and synced (owner: keep the fix local, sync to active worktrees):
- Commit 83254320 pins `v0.4.7-osguard` (SHA `d71b35aa…`; `tools/minecraft-mcp-bridge/`). The copy task drops older bridge jars, and the compat range is `[0.4.7,0.5.0)`. Lane checked: loads, only the pinned jar present.
- The same commit was cherry-picked to `claude/unified-thermo-port` (026422fe) and `claude/custom-equipment-rendering-805e7d` (4ffae269), with the jar in each worktree's `run/mcp-client-mods/`.
- Open: the upstream PR waits for the owner; other branches pick the pin up when they merge `main`.
