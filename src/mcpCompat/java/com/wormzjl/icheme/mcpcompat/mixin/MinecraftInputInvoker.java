package com.wormzjl.icheme.mcpcompat.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test-only forwarding to Minecraft's ordinary attack and use paths, with their normal game-mode checks. */
@Mixin(Minecraft.class)
public interface MinecraftInputInvoker {
    @Invoker("startAttack") boolean ichemeMcpAttack();
    @Invoker("startUseItem") void ichemeMcpUse();
}
