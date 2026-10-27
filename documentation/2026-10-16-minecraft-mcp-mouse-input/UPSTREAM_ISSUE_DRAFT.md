Title: Windows: the mod moves and confines the global OS cursor while the Minecraft window is in the background

**Environment:** Windows 11, Minecraft 1.21.1 NeoForge, `minecraft-mcp-1.21.1-neoforge-v0.3.0.jar`. I read the code with javap and checked `main`, where the same code is still present in `InputInjectionHelper` and `ControlModeHelper`. The client is launched by an AI agent from a background process, and the user keeps working in other windows.

**Symptom:** while an agent drives the client, the user's real mouse cursor jumps to the centre of the Minecraft window and is sometimes confined there. The Minecraft window never comes to the front.

**Root cause.** GLFW on Windows decides "focused" with `GetActiveWindow()`, which is per thread. A window created by a background process becomes its thread's active window without becoming the foreground window. I tested this with a standalone LWJGL 3.3.3 program, without Minecraft. The window stayed behind another app, and `glfwGetWindowAttrib(GLFW_FOCUSED)` returned 1 for the whole run. In that state:
- `glfwSetCursorPos` calls `SetCursorPos` and moves the global cursor;
- `GLFW_CURSOR_DISABLED` centres the cursor and confines it with `ClipCursor`.

**Where the mod reaches those calls:**
1. `InputInjectionHelper.setCursorPos` calls `glfwSetCursorPos(handle, x, y)`. It is used by `scroll_at` and `mouse_drag`.
2. `ControlModeHelper.exitMcpControlMode` calls `glfwSetInputMode(GLFW_CURSOR, GLFW_CURSOR_DISABLED)` directly.
   - This bypasses vanilla's `isWindowActive()` check in `MouseHandler.grabMouse`.
   - Its `lockCursor` lookup matches only Yarn/SRG names (`lockCursor`, `method_1611`, `func_224798_b`). On NeoForge 1.21.1, with Mojang names at runtime, that call never runs.
3. The `java.awt.Robot` fallbacks (`mouseMove`, `mousePress`, `mouseWheel`, `keyPress`) send real desktop input to whatever window is in front.
   - They run if the reflective lookup fails or the window handle is 0.
   - In `setCursorPos`, the Robot offset uses `glfwGetWindowPosX`/`glfwGetWindowPosY`, which do not exist in LWJGL (it has `glfwGetWindowPos(long, IntBuffer, IntBuffer)`). The move would therefore land relative to the screen origin.
4. Vanilla `MouseHandler.grabMouse`/`releaseMouse` also centre the cursor. They are triggered by tools that open or close screens, or by a `click` with no screen open. Vanilla believes the window is active for the same GLFW reason.

**Suggestion:** add an opt-in "background / no OS input" mode (config or system property). In that mode the mod would:
- update `MouseHandler`'s `xpos`/`ypos` fields instead of calling `glfwSetCursorPos`;
- leave GLFW's cursor mode alone in `exitMcpControlMode`;
- skip `InputConstants.grabOrReleaseMouse`;
- never use `java.awt.Robot`.

Agents and CI that drive a client in the background need this, so the developer's desktop is not affected.

**Workaround on our side:** mixins in a small companion mod that cancel these calls in our agent test lane.
Posted 2026-10-16 as https://github.com/langyo/minecraft-mod-mcp/issues/54
