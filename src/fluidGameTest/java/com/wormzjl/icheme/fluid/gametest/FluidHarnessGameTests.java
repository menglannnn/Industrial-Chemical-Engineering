package com.wormzjl.icheme.fluid.gametest;

import com.wormzjl.icheme.science.fluid.linalg.SparseLuSolver;
import com.wormzjl.icheme.science.fluid.linalg.SparseMatrix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("icheme_fluid_test")
@PrefixGameTestTemplate(false)
public final class FluidHarnessGameTests {
    private FluidHarnessGameTests() {}
    @GameTest(template="empty",timeoutTicks=200000,batch="fluid-world-unloaded")
    public static void registeredPipesAndReservoirsKeepSimulatingAcrossUnloadedChunks(GameTestHelper helper) {
        var level=helper.getLevel();var world=com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority.find(level.getServer()).orElseThrow();
        var origin=helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        var source=new net.minecraft.core.BlockPos(((origin.getX()>>4)+64)*16+8,80,(origin.getZ()>>4)*16+8);var reservoir=source.east(128);var middle=source.east(64);
        var a=new net.minecraft.world.level.ChunkPos(source);var b=new net.minecraft.world.level.ChunkPos(reservoir);
        level.setChunkForced(a.x,a.z,true);level.setChunkForced(b.x,b.z,true);
        level.setBlock(source,com.wormzjl.icheme.registry.ModBlocks.FLUID_GENERATOR.get().defaultBlockState(),3);
        for(int x=1;x<128;x++)level.setBlock(source.east(x),com.wormzjl.icheme.registry.ModBlocks.FLUID_PIPE.get().defaultBlockState(),3);
        level.setBlock(reservoir,com.wormzjl.icheme.registry.ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(),3);
        long sourceId=((com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity)level.getBlockEntity(source)).fluidIdentity();
        long tankId=((com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity)level.getBlockEntity(reservoir)).fluidIdentity();
        var registration=world.registrations().get(sourceId);var spec=registration.spec();
        TankCharge.run(world,sourceId,new com.wormzjl.icheme.runtime.fluid.FluidDeviceSpec(spec.volume(),spec.temperature(),200000,spec.composition()));
        // A placed tank holds nothing (decision D54): no nitrogen at the start, and none may appear.
        var initialState=world.view(tankId).state();double initialNitrogen=initialState==null?0:initialState.phaseMoles()[2][com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog.nitrogenIndex()];int[] phase={0};double[] before={0};long[] changedAt={0};
        helper.succeedWhen(()->{
            if(phase[0]==0) {
                helper.assertTrue(!level.hasChunkAt(middle),"Waiting for intermediate pipe chunks to unload");
                var view=world.view(tankId);helper.assertTrue(view.state()!=null&&view.state().phaseMoles()[1][com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog.waterIndex()]>0,"No transfer across unloaded middle chunks");
                helper.assertTrue(level.hasChunkAt(source)&&level.hasChunkAt(reservoir),"Test endpoint tickets were not retained");
                level.setChunkForced(a.x,a.z,false);level.setChunkForced(b.x,b.z,false);phase[0]=1;
                helper.assertTrue(false,"Waiting for endpoint chunks to unload");
            }
            if(phase[0]==1) {
                helper.assertTrue(!level.hasChunkAt(source)&&!level.hasChunkAt(reservoir),"Waiting for all endpoint chunks to unload");
                before[0]=world.view(tankId).state().phaseMoles()[1][com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog.waterIndex()];changedAt[0]=world.onlineTick();var current=world.registrations().get(sourceId);var s=current.spec();
                world.edit(sourceId,current.revision(),current.device(),new com.wormzjl.icheme.runtime.fluid.FluidDeviceSpec(s.volume(),s.temperature(),250000,s.composition()));phase[0]=2;
                helper.assertTrue(false,"Waiting for the unloaded configuration event and new flow");
            }
            if(phase[0]==2) {
                var view=world.view(tankId);helper.assertTrue(view.committedTick()>changedAt[0]&&view.state().phaseMoles()[1][com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog.waterIndex()]>before[0]+1e-6,"Waiting for changed flow with every endpoint unloaded");
                helper.assertTrue(!level.hasChunkAt(source)&&!level.hasChunkAt(reservoir)&&!level.hasChunkAt(middle),"Simulation loaded a chunk");
                level.getChunk(reservoir);world.markViewDirty(tankId);
                var entity=(com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity)level.getBlockEntity(reservoir);
                helper.assertTrue(entity.fluidIdentity()==tankId,"Reload changed the tank identity");
                helper.assertTrue(Math.abs(world.view(tankId).state().phaseMoles()[2][com.wormzjl.icheme.science.fluid.thermo.FluidMaterialCatalog.nitrogenIndex()]-initialNitrogen)<1e-8,"Reload recreated nitrogen");
                for(int x=0;x<=128;x++)level.removeBlock(source.east(x),false);phase[0]=3;
                helper.assertTrue(false,"Waiting for test topology cleanup");
            }
            var saved=world.capture();helper.assertTrue(saved.world().active().isEmpty()&&saved.world().events().isEmpty()&&saved.checkpoint().islands().isEmpty(),"Waiting for all cleanup transactions");
            helper.assertTrue(!level.getForcedChunks().contains(a.toLong())&&!level.getForcedChunks().contains(b.toLong()),"Test endpoint tickets leaked");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sparseSolverLoadsInServer(GameTestHelper helper) {
        if (Boolean.getBoolean("icheme.fluid.testFailure")) {
            helper.fail("Intentional M0 GameTest failure: nonzero exit must propagate to Gradle");
            return;
        }
        var matrix = new SparseMatrix(2, new int[] {0, 1, 3}, new int[] {1, 0, 1}, new double[] {1, 2, 3});
        double[] result = SparseLuSolver.solve(matrix, new double[] {4, 7});
        if (Math.abs(result[0] - 1) > 1e-12 || Math.abs(result[1] - 2) > 1e-12) {
            helper.fail("Sparse solver returned the wrong pivoted solution");
            return;
        }
        helper.succeed();
    }
}
