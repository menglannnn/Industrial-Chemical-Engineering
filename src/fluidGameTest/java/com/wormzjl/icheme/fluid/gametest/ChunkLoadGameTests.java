package com.wormzjl.icheme.fluid.gametest;

import com.wormzjl.icheme.registry.ModBlocks;
import com.wormzjl.icheme.runtime.BoundedCpuSolveService;
import com.wormzjl.icheme.runtime.ProcessSolveServices;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.ColumnCalculatorV3BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

/**
 * What a chunk load or unload may do (batch 2026-10-26-chunk-loading-review): a loading pipe reads no neighbour in a chunk
 * that is not loaded (D1), and a calculator that unloads or breaks while it calculates cancels its solve (D3).
 */
@GameTestHolder("icheme_fluid_test")
@PrefixGameTestTemplate(false)
public final class ChunkLoadGameTests {
    private ChunkLoadGameTests() {}

    /**
     * D1: on a server, reading a block in a chunk that is not loaded loads that chunk on the main thread. A pipe on the
     * loaded area's edge read all six neighbours as it loaded, so each load loaded the next chunk along the run. The
     * joint towards such a neighbour keeps its saved state; loaded neighbours are read as before.
     */
    @GameTest(template="empty",batch="chunk-load-pipe-joint")
    public static void aLoadingPipeReadsNoNeighbourInAChunkThatIsNotLoaded(GameTestHelper helper) {
        var pipe=ModBlocks.FLUID_PIPE.get().defaultBlockState();
        // A pipe on the east edge of chunk 0, saved joined to a pipe beyond it in chunk 1, which is not loaded.
        var position=new BlockPos(15,64,8);var west=position.west();
        var unloadedReads=new ArrayList<BlockPos>();
        var level=stubLevel(at->(at.getX()>>4)!=1,at->{
            if((at.getX()>>4)==1){unloadedReads.add(at);return pipe;}
            return at.equals(west)?pipe:Blocks.AIR.defaultBlockState();
        });
        var saved=pipe.setValue(FluidDeviceBlock.connection(Direction.EAST),true);
        var result=FluidDeviceBlock.connectedState(saved,level,position);
        helper.assertTrue(unloadedReads.isEmpty(),"The pipe read "+unloadedReads+" in a chunk that is not loaded");
        helper.assertTrue(result.getValue(FluidDeviceBlock.connection(Direction.EAST)),"The saved joint towards the unloaded chunk was dropped");
        helper.assertTrue(result.getValue(FluidDeviceBlock.connection(Direction.WEST)),"The loaded pipe to the west was not joined");
        for(var direction:new Direction[]{Direction.UP,Direction.DOWN,Direction.NORTH,Direction.SOUTH})
            helper.assertTrue(!result.getValue(FluidDeviceBlock.connection(direction)),"Joined to air towards "+direction);
        // A joint saved towards a loaded neighbour that is gone is dropped, as before.
        var stale=FluidDeviceBlock.connectedState(pipe.setValue(FluidDeviceBlock.connection(Direction.UP),true),level,position);
        helper.assertTrue(!stale.getValue(FluidDeviceBlock.connection(Direction.UP)),"A joint to loaded air was kept");
        helper.succeed();
    }

    /**
     * D3: a calculator's operation is not saved, so a solve that outlives its block can only be dropped. Breaking (or
     * unloading) the block cancels the solve at once, which frees the worker and the block's solve slot.
     */
    @GameTest(template="empty",timeoutTicks=10000000,batch="chunk-load-column-cancel")
    public static void aCalculatorRemovedWhileItCalculatesCancelsItsSolve(GameTestHelper helper) {
        var level=helper.getLevel();var server=level.getServer();var pos=helper.absolutePos(new BlockPos(0,1,0));
        level.setBlock(pos,ModBlocks.COLUMN_CALCULATOR_V3.get().defaultBlockState(),3);
        var block=(ColumnCalculatorV3BlockEntity)level.getBlockEntity(pos);
        long id=ProcessSolveServices.nextRequestId();
        var operation=block.tryBegin(block.state(0).inputRevision(),id,block.currentInput()).orElseThrow();
        var target=new ProcessSolveServices.ColumnTarget(level.dimension(),pos);
        var admission=ProcessSolveServices.submitV3Column(server,new ProcessSolveServices.V3ColumnRequest(id,target,operation,System.nanoTime()));
        helper.assertTrue(admission.accepted(),"Column admission failed: "+admission.admission());
        level.removeBlock(pos,false);
        // The job was admitted this tick and is not dispatched before the tick hook: a cancel now finds it cancelled
        // (ALREADY_TERMINAL once its cancellation completion is published, ALREADY_REQUESTED while a worker still runs it)
        // only if the removal asked first. Without that, this call would be the first and answer REQUESTED.
        var again=ProcessSolveServices.cancelRequest(server,id);
        helper.assertTrue(again==BoundedCpuSolveService.CancellationResult.ALREADY_TERMINAL||again==BoundedCpuSolveService.CancellationResult.ALREADY_REQUESTED,
                "Removing the calculator did not cancel its solve: "+again);
        long deadline=System.nanoTime()+60_000_000_000L;boolean[] done={false};
        helper.onEachTick(()->{
            if(done[0])return;
            helper.assertTrue(System.nanoTime()<deadline,"The cancelled solve did not end within its wall deadline");
            if(ProcessSolveServices.cancelRequest(server,id)!=BoundedCpuSolveService.CancellationResult.NOT_FOUND){
                // The GameTest server ticks unpaced; yield while the worker reaches its cancellation point.
                java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
                return;
            }
            done[0]=true;helper.succeed();
        });
    }

    /**
     * A level that answers only what a pipe's joint rule asks: which chunks are loaded, and the block at a position. Its
     * default methods run as the interface defines them, so {@code hasChunkAt} asks {@code hasChunk}.
     */
    private static LevelReader stubLevel(java.util.function.Predicate<BlockPos> loaded,java.util.function.Function<BlockPos,BlockState> blocks) {
        InvocationHandler handler=(proxy,method,args)->switch(method.getName()) {
            case "hasChunk"->loaded.test(new BlockPos(((Integer)args[0])<<4,0,((Integer)args[1])<<4));
            case "getBlockState"->blocks.apply((BlockPos)args[0]);
            case "getBlockEntity"->null;
            case "toString"->"stub level";
            case "hashCode"->System.identityHashCode(proxy);
            case "equals"->proxy==args[0];
            default->{
                if(method.isDefault())yield InvocationHandler.invokeDefault(proxy,method,args);
                throw new UnsupportedOperationException("stub level: "+method.getName());
            }
        };
        return (LevelReader)Proxy.newProxyInstance(LevelReader.class.getClassLoader(),new Class<?>[]{LevelReader.class},handler);
    }
}
