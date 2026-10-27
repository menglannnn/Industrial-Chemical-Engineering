package xyz.langyo.minecraft.mcp.common;

import java.lang.reflect.Method;

/**
 * Keeps the mod from touching the OS cursor or sending desktop input while the game window is
 * in the background.
 *
 * <p>On Windows GLFW decides "focused" with {@code GetActiveWindow()}, which is per thread. A
 * window shown by a background process (a client launched by an agent or a script) becomes its
 * own thread's active window without ever becoming the foreground window. GLFW then reports
 * {@code GLFW_FOCUSED}, moves the global cursor on {@code glfwSetCursorPos} and confines it on
 * {@code GLFW_CURSOR_DISABLED}; Minecraft receives the same focus and grabs the mouse on world
 * join, screen close or an in-world click. This class asks the OS for the real foreground window
 * instead, gates the mod's own cursor calls and {@code java.awt.Robot} fallbacks on it, and
 * {@link #tick} tells Minecraft that a background window is not focused, so vanilla never grabs
 * or recentres the cursor there.
 *
 * <p>Other platforms report focus correctly through GLFW and are left alone. When the foreground
 * window cannot be read (no JNA, or LWJGL 2 without {@code Display.isActive}) the previous
 * behaviour is kept.
 */
public final class OsInputGuard {

    private static final boolean WINDOWS =
            System.getProperty("os.name", "").toLowerCase().startsWith("windows");

    private static boolean lookupDone = false;
    private static Object user32;
    private static Method getForegroundWindow, getPointer, pointerNativeValue, glfwGetWin32Window;
    private static Method glfwGetWindowAttrib, glfwSetWindowFocusCallback, focusCallbackInvoke;
    private static int glfwFocused;

    /** True while this class has told Minecraft its (background) window is not focused. */
    private static volatile boolean focusWithheld = false;

    private OsInputGuard() {}

    /**
     * True when the mod may move the OS cursor, change the cursor mode or send desktop input for
     * this window: the window is the OS foreground window, or that cannot be determined.
     */
    public static boolean mayUseOsInput(long handle) {
        Boolean fg = isForeground(handle);
        return fg == null || fg;
    }

    /**
     * Called every client tick. Withholds focus from Minecraft while GLFW reports a background
     * window as focused, and gives it back once the window is really in front.
     */
    public static void tick(Object mc) {
        if (!WINDOWS || !ReflectionCache.LWJGL3) return;
        try {
            long handle = WindowHelper.getWindowHandle(mc);
            if (handle == 0) return;
            Boolean fg = isForeground(handle);
            if (fg == null) return;
            if (!fg && !focusWithheld && glfwReportsFocused(handle)) {
                if (deliverFocus(handle, false)) {
                    focusWithheld = true;
                    ReflectionHelper.dbg("OsInputGuard: window is not the foreground window, focus withheld");
                }
            } else if (fg && focusWithheld) {
                deliverFocus(handle, true);
                focusWithheld = false;
                ReflectionHelper.dbg("OsInputGuard: window is in front, focus restored");
            }
        } catch (Exception e) {
            ReflectionHelper.dbg("OsInputGuard.tick: " + e.getMessage());
        }
    }

    /** Real foreground state of the game window, or null when it cannot be read. */
    static Boolean isForeground(long handle) {
        ReflectionCache.initClassCache();
        if (!ReflectionCache.LWJGL3) return lwjgl2DisplayActive();
        if (!WINDOWS) return Boolean.TRUE;
        if (handle == 0) return null;
        lookup();
        if (user32 == null || glfwGetWin32Window == null) return null;
        try {
            long hwnd = ((Number) glfwGetWin32Window.invoke(null, handle)).longValue();
            Object fgHwnd = getForegroundWindow.invoke(user32);
            long fg = fgHwnd == null ? 0 : ((Number) pointerNativeValue.invoke(null, getPointer.invoke(fgHwnd))).longValue();
            return hwnd != 0 && hwnd == fg;
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean lwjgl2DisplayActive() {
        try {
            Class<?> display = ReflectionCache.getDisplayClass();
            if (display == null) return null;
            return (Boolean) display.getMethod("isActive").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean glfwReportsFocused(long handle) throws Exception {
        return glfwGetWindowAttrib != null
                && ((Number) glfwGetWindowAttrib.invoke(null, handle, glfwFocused)).intValue() != 0;
    }

    /**
     * Hands a focus change to the game's own GLFW focus callback, read back with
     * glfwSetWindowFocusCallback (it returns the previous callback, which is put straight back).
     * Must run on the main thread.
     */
    private static boolean deliverFocus(long handle, boolean focused) throws Exception {
        if (glfwSetWindowFocusCallback == null || focusCallbackInvoke == null) return false;
        Object gameCallback = glfwSetWindowFocusCallback.invoke(null, handle, null);
        glfwSetWindowFocusCallback.invoke(null, handle, gameCallback);
        if (gameCallback == null) return false;
        focusCallbackInvoke.invoke(gameCallback, handle, focused);
        return true;
    }

    private static synchronized void lookup() {
        if (lookupDone) return;
        lookupDone = true;
        try {
            Class<?> glfw = ReflectionCache.getGlfwClass();
            Class<?> callbackI = Class.forName("org.lwjgl.glfw.GLFWWindowFocusCallbackI");
            glfwGetWindowAttrib = glfw.getMethod("glfwGetWindowAttrib", long.class, int.class);
            glfwFocused = glfw.getField("GLFW_FOCUSED").getInt(null);
            glfwSetWindowFocusCallback = glfw.getMethod("glfwSetWindowFocusCallback", long.class, callbackI);
            focusCallbackInvoke = callbackI.getMethod("invoke", long.class, boolean.class);
            glfwGetWin32Window = Class.forName("org.lwjgl.glfw.GLFWNativeWin32").getMethod("glfwGetWin32Window", long.class);
        } catch (Exception e) {
            ReflectionHelper.dbg("OsInputGuard: GLFW lookup failed: " + e.getMessage());
        }
        try {
            Class<?> user32Class = Class.forName("com.sun.jna.platform.win32.User32");
            Class<?> pointerClass = Class.forName("com.sun.jna.Pointer");
            getForegroundWindow = user32Class.getMethod("GetForegroundWindow");
            getPointer = Class.forName("com.sun.jna.PointerType").getMethod("getPointer");
            pointerNativeValue = pointerClass.getMethod("nativeValue", pointerClass);
            user32 = user32Class.getField("INSTANCE").get(null);
        } catch (Throwable e) {
            user32 = null;
            ReflectionHelper.dbg("OsInputGuard: JNA User32 unavailable, background detection off: " + e.getMessage());
        }
    }
}
