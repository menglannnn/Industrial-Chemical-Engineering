package com.wormzjl.icheme.fluid.gametest;

import com.wormzjl.icheme.registry.ModBlocks;
import com.wormzjl.icheme.runtime.fluid.FluidDeviceSpec;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * A placed tank holds nothing (owner decision D54: every vessel is a bag, placed empty), so a test about a charged tank
 * charges it as a player would: a nitrogen generator at the charge pressure (298.15 K) behind one pipe on a free face of
 * the tank, until the tank stands at that pressure; then the generator and its pipe go, and the tank keeps its charge.
 * Poll {@link #charged} once per tick.
 */
final class TankCharge {
    private final GameTestHelper helper;private final FluidWorldAuthority world;private final long tank;private final BlockPos pipe,generator;
    private final double pressure;private int stage;
    TankCharge(GameTestHelper helper,FluidWorldAuthority world,BlockPos tank,Direction face,double pressure) {
        this.helper=helper;this.world=world;this.pressure=pressure;
        this.tank=((FluidDeviceBlockEntity)helper.getLevel().getBlockEntity(tank)).fluidIdentity();pipe=tank.relative(face);generator=pipe.relative(face);
        helper.getLevel().setBlock(generator,ModBlocks.FLUID_GENERATOR.get().defaultBlockState(),3);
        long id=((FluidDeviceBlockEntity)helper.getLevel().getBlockEntity(generator)).fluidIdentity();var old=world.registrations().get(id);
        var nitrogen=FluidDeviceSpec.nitrogen(com.wormzjl.icheme.science.material.MaterialRuntime.active()).composition();
        world.edit(id,old.revision(),old.device(),new FluidDeviceSpec(1,298.15,pressure,nitrogen));
        helper.getLevel().setBlock(pipe,ModBlocks.FLUID_PIPE.get().defaultBlockState(),3);
    }
    /** True once the tank stands within 0.1 % of the charge pressure and the generator and its pipe are gone. */
    boolean charged() {
        if(stage==2)return true;
        if(stage==0) {
            var state=world.view(tank).state();
            if(state==null||state.pressure()<pressure*(1-1e-3))return false;
            helper.getLevel().removeBlock(generator,false);helper.getLevel().removeBlock(pipe,false);stage=1;return false;
        }
        if(!world.capture().world().events().isEmpty())return false;
        stage=2;return true;
    }
}
