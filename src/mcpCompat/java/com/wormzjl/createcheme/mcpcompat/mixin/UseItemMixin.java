package com.wormzjl.createcheme.mcpcompat.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MCP 0.3.0 calls startUseItem on its HTTP thread, so a screen a block opens on use races the render thread
 * (it renders before its init). Run Minecraft's ordinary use path on the render thread and wait for it.
 */
@Pseudo
@Mixin(targets="xyz.langyo.minecraft.mcp.common.PlayerWorldHelper",remap=false)
public abstract class UseItemMixin {
    @Inject(method="doUseItem",at=@At("HEAD"),cancellable=true,remap=false)
    private static void useOnRenderThread(Object instance,CallbackInfoReturnable<String> result) {
        if(!(instance instanceof Minecraft client))return;
        client.executeBlocking(((MinecraftInputInvoker)client)::createchemeMcpUse);
        result.setReturnValue("{\"use_item\":true,\"method\":\"minecraft_use_path_render_thread\"}");
    }
}
