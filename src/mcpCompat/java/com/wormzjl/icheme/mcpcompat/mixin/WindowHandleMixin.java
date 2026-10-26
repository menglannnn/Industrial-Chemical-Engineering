package com.wormzjl.icheme.mcpcompat.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MCP 0.4.x guesses the GLFW window handle as the first no-argument long method of Minecraft's window that answers
 * non-zero; a mod mixing such a method into the window (Sodium does) hands GLFW a value that is not a window, and the
 * background input mode's per-tick glfwGetInputMode faults on it. Give the bridge the window's own handle.
 */
@Pseudo
@Mixin(targets="xyz.langyo.minecraft.mcp.common.WindowHelper",remap=false)
public abstract class WindowHandleMixin {
    @Inject(method="getWindowHandle",at=@At("HEAD"),cancellable=true,remap=false)
    private static void minecraftHandle(Object instance,CallbackInfoReturnable<Long> result) {
        if(instance instanceof Minecraft client&&client.getWindow()!=null)result.setReturnValue(client.getWindow().getWindow());
    }
}
