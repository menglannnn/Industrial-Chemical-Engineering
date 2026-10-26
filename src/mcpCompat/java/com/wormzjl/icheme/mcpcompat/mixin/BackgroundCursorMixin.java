package com.wormzjl.icheme.mcpcompat.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Locale;

/** On Windows GLFW calls a window shown by a background process focused (GetActiveWindow is per thread), so vanilla's
 * mouse grab and release (world join, screen open and close, an in-world click) move the desktop cursor to the window
 * centre and confine it while an agent drives the client and the owner works elsewhere. The bridge's background input
 * mode covers only its own calls; this skips vanilla's cursor calls while the game window is not the foreground window.
 * Minecraft's own grabbed state still changes, and the bridge resets it every tick.
 */
@Mixin(InputConstants.class)
public abstract class BackgroundCursorMixin {
    @Inject(method="grabOrReleaseMouse",at=@At("HEAD"),cancellable=true)
    private static void keepDesktopCursor(long window,int cursorMode,double x,double y,CallbackInfo callback) {
        if(!ichemeMcpForeground(window))callback.cancel();
    }

    /** True when the window is the OS foreground window, on any platform but Windows, or when that cannot be read. */
    @Unique
    private static boolean ichemeMcpForeground(long window) {
        if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).startsWith("windows"))return true;
        try{
            WinDef.HWND foreground=User32.INSTANCE.GetForegroundWindow();
            return foreground!=null&&Pointer.nativeValue(foreground.getPointer())==GLFWNativeWin32.glfwGetWin32Window(window);
        }catch(Throwable unavailable){return true;}
    }
}
