package com.wormzjl.icheme.fluid.gametest;

import com.wormzjl.icheme.registry.ModBlocks;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.Paint;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import com.wormzjl.icheme.world.level.block.entity.EquipmentShellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/**
 * Backward compatibility of placed equipment (batch 2026-10-26-backward-compat, owner decision D6): a structure whose
 * kind this build no longer has is removed whole when its cells load, every cell it placed cleared, its fluid
 * registration removed, and no other block touched.
 */
@GameTestHolder("icheme_fluid_test")
@PrefixGameTestTemplate(false)
public final class EquipmentCompatGameTests {
    private EquipmentCompatGameTests() {}

    @GameTest(template="empty",timeoutTicks=400,batch="equipment-compat-retired")
    public static void aStructureOfARetiredKindIsRemovedWholeWithNoGhostBlock(GameTestHelper helper) {
        var level=helper.getLevel();var world=FluidWorldAuthority.find(level.getServer()).orElseThrow();
        var shape=EquipmentShape.drum(3,3,EquipmentShape.Orientation.VERTICAL,EquipmentShape.Heading.NORTH,0);
        var origin=helper.absolutePos(new BlockPos(1,1,1));var layout=ShapeRasterizer.layout(shape);
        var cells=new HashSet<BlockPos>();
        for(var list:List.of(layout.shell(),layout.interior()))for(var c:list)cells.add(origin.offset(c.x(),c.y(),c.z()));
        helper.assertTrue(!layout.interior().isEmpty(),"the drum has interior cells");
        // A block of another owner inside the vessel's box: the removal must leave it.
        BlockPos neighbour=null;
        for(var at:BlockPos.betweenClosed(origin,origin.offset(shape.sizeX()-1,shape.sizeY()-1,shape.sizeZ()-1)))if(!cells.contains(at)){neighbour=at.immutable();break;}
        EquipmentStructure.place(level,origin,shape,Paint.DEFAULT,Integer.MAX_VALUE);
        if(neighbour!=null)level.setBlock(neighbour,Blocks.STONE.defaultBlockState(),3);
        var node=ShapeRasterizer.fluidCell(shape);var nodeAt=origin.offset(node.x(),node.y(),node.z());
        var position=new PhysicalFluidTopology.Position(level.dimension().location().toString(),nodeAt.getX(),nodeAt.getY(),nodeAt.getZ());
        var keep=neighbour;
        helper.startSequence().thenWaitUntil(()->helper.assertTrue(world.at(position).isPresent(),"Waiting for the drum's registration"))
        .thenExecute(()->{
            // Every shell cell reloads with a kind this build does not have, as a world saved by an older build does.
            for(var c:layout.shell()) {
                var at=origin.offset(c.x(),c.y(),c.z());var shell=(EquipmentShellBlockEntity)level.getBlockEntity(at);
                var tag=shell.saveWithoutMetadata(level.registryAccess());tag.putString("Kind","pressure_cooker");
                shell.loadWithComponents(tag,level.registryAccess());
                helper.assertTrue("pressure_cooker".equals(shell.retired())&&shell.shape()==null,"the cell reads as retired");
                helper.assertTrue(shell.saveWithoutMetadata(level.registryAccess()).getString("Kind").equals("pressure_cooker"),"a retired cell saves its data unchanged");
                shell.onLoad();
            }
        }).thenWaitUntil(()->{
            for(var at:cells)helper.assertTrue(level.getBlockState(at).isAir(),"Waiting for the cell at "+at.toShortString()+" to clear: "+level.getBlockState(at));
            helper.assertTrue(world.at(position).isEmpty(),"Waiting for the drum's registration to leave");
        }).thenExecute(()->{
            if(keep!=null)helper.assertTrue(level.getBlockState(keep).is(Blocks.STONE),"another block in the box is left");
            for(var at:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(shape.sizeX(),shape.sizeY(),shape.sizeZ())))
                helper.assertFalse(level.getBlockState(at).is(ModBlocks.EQUIPMENT_INTERIOR.get())||level.getBlockState(at).is(ModBlocks.EQUIPMENT_SHELL.get()),"no ghost block at "+at.toShortString());
        }).thenSucceed();
    }

    /**
     * A refresh that finds another block in one of its layout's cells leaves the cell out and the block where it stands, and
     * the cells it wrote keep no stamp (batch 2026-10-26-backward-compat): the next load tries again and, the block cleared,
     * places the cell. The drum keeps its registration throughout.
     */
    @GameTest(template="empty",timeoutTicks=400,batch="equipment-compat-blocked")
    public static void aBlockedRefreshTriesAgainAtTheNextLoad(GameTestHelper helper) {
        var level=helper.getLevel();var world=FluidWorldAuthority.find(level.getServer()).orElseThrow();
        var shape=EquipmentShape.drum(3,3,EquipmentShape.Orientation.VERTICAL,EquipmentShape.Heading.NORTH,0);
        var origin=helper.absolutePos(new BlockPos(1,1,1));var layout=ShapeRasterizer.layout(shape);
        var node=ShapeRasterizer.fluidCell(shape);var nodeAt=origin.offset(node.x(),node.y(),node.z());
        var nozzles=new HashSet<ShapeRasterizer.Cell>();for(var port:ShapeRasterizer.ports(shape))nozzles.add(port.cell());
        // Two shell cells that are neither the node nor a nozzle's, as an older layout lacked them: a player's block stands in
        // one, the other is free.
        var free=layout.shell().stream().filter(c->!c.equals(node)&&!nozzles.contains(c)).limit(2).toList();
        var blockedAt=origin.offset(free.get(0).x(),free.get(0).y(),free.get(0).z());var missingAt=origin.offset(free.get(1).x(),free.get(1).y(),free.get(1).z());
        EquipmentStructure.place(level,origin,shape,Paint.DEFAULT,Integer.MAX_VALUE);
        var position=new PhysicalFluidTopology.Position(level.dimension().location().toString(),nodeAt.getX(),nodeAt.getY(),nodeAt.getZ());
        long[] identity={0};
        Runnable reload=()->{
            // Every shell cell reloads as an older layout placed it: no stamp of the current one.
            for(var c:layout.shell()) {
                var at=origin.offset(c.x(),c.y(),c.z());
                if(!(level.getBlockEntity(at) instanceof EquipmentShellBlockEntity shell))continue;
                var tag=shell.saveWithoutMetadata(level.registryAccess());tag.putLong("Layout",0);shell.loadWithComponents(tag,level.registryAccess());shell.onLoad();
            }
        };
        helper.startSequence().thenWaitUntil(()->helper.assertTrue(world.at(position).isPresent(),"Waiting for the drum's registration"))
        .thenExecute(()->{
            identity[0]=world.at(position).orElseThrow().device().id();
            // The cells leave without the dismantle a broken cell starts (their shape unread first).
            for(var at:List.of(blockedAt,missingAt)) {
                var shell=(EquipmentShellBlockEntity)level.getBlockEntity(at);
                var tag=shell.saveWithoutMetadata(level.registryAccess());tag.putString("Kind","pressure_cooker");shell.loadWithComponents(tag,level.registryAccess());
            }
            level.setBlock(blockedAt,Blocks.STONE.defaultBlockState(),3);level.setBlock(missingAt,Blocks.AIR.defaultBlockState(),3);
            helper.assertTrue(world.at(position).isPresent(),"the drum stands");
            reload.run();
        }).thenWaitUntil(()->helper.assertTrue(level.getBlockEntity(missingAt) instanceof EquipmentShellBlockEntity,"Waiting for the refresh to place the free cell"))
        .thenExecute(()->{
            helper.assertTrue(level.getBlockState(blockedAt).is(Blocks.STONE),"the player's block stands");
            for(var c:layout.shell()) {
                var at=origin.offset(c.x(),c.y(),c.z());if(at.equals(blockedAt))continue;
                helper.assertTrue(((EquipmentShellBlockEntity)level.getBlockEntity(at)).stale(),"a cell the blocked refresh wrote keeps no stamp: "+c);
            }
            helper.assertTrue(world.at(position).map(r->r.device().id()).orElse(0L)==identity[0],"the drum keeps its registration");
            level.setBlock(blockedAt,Blocks.AIR.defaultBlockState(),3);
            reload.run();
        }).thenWaitUntil(()->{
            helper.assertTrue(level.getBlockEntity(blockedAt) instanceof EquipmentShellBlockEntity,"Waiting for the cleared cell to be placed");
            for(var c:layout.shell())helper.assertFalse(((EquipmentShellBlockEntity)level.getBlockEntity(origin.offset(c.x(),c.y(),c.z()))).stale(),"Waiting for every cell's stamp at "+c);
        }).thenExecute(()->{
            helper.assertTrue(world.at(position).map(r->r.device().id()).orElse(0L)==identity[0],"the drum keeps its registration");
            // The drum leaves with its registration. GameTest areas are not cleared between batches, and a later test may
            // need a world with no registration (FluidHarnessGameTests' cleanup check): whether the next test's clearing
            // happened to reach the drum depended on its grid slot.
            level.removeBlock(nodeAt,false);
        }).thenWaitUntil(()->helper.assertTrue(world.at(position).isEmpty(),"Waiting for the drum's registration to leave"))
        .thenSucceed();
    }
}
