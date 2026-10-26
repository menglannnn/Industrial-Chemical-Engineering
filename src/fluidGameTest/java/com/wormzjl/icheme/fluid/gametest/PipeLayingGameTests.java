package com.wormzjl.icheme.fluid.gametest;

import com.mojang.authlib.GameProfile;
import com.wormzjl.icheme.registry.ModBlocks;
import com.wormzjl.icheme.registry.ModItems;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.item.PipelineToolItem;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.PipeLaying;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The laying tool's server commands in a real world (batch 2026-10-26-pipe-laying-system, W2): a line laid between two
 * tanks joins them and nothing it runs beside, a refused lay changes nothing, and a dismantle removes one block whose
 * neighbours stay open ends that a new line joins again.
 */
@GameTestHolder("icheme_fluid_test")
@PrefixGameTestTemplate(false)
public final class PipeLayingGameTests {
    private static final int ALONG_X = 1 << Direction.WEST.ordinal() | 1 << Direction.EAST.ordinal();

    private PipeLayingGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "pipe-laying")
    public static void aLaidLineJoinsItsTanksOnlyAndComesOutOneBlockAtATime(GameTestHelper helper) {
        var level = helper.getLevel();
        var world = FluidWorldAuthority.find(level.getServer()).orElseThrow();
        var west = helper.absolutePos(new BlockPos(0, 1, 1));
        var east = west.east(5);
        var beside = west.east(2).south();
        var cells = List.of(west.east(), west.east(2), west.east(3), west.east(4));
        var placed = new ArrayList<BlockPos>(List.of(west, east, beside));
        placed.addAll(cells);
        // The line runs past the empty template's barrier ring: clear its path, as the other line tests overwrite it.
        for (var cell : cells) level.setBlock(cell, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(west, ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
        level.setBlock(east, ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
        // A hand-placed pipe beside the path: it joins every neighbour that has a joint towards it, and a laid line has none.
        level.setBlock(beside, ModBlocks.FLUID_PIPE.get().defaultBlockState(), 3);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PipeLayingTest"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(west.getX() + 2.5, west.getY(), west.getZ() - 1.5);
        var tool = new ItemStack(ModItems.PIPE_LAYING_TOOL.get());
        tool.set(ModItems.PIPE_LAYING_SETTINGS.get(), new PipelineToolItem.Settings(.1));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.getInventory().add(new ItemStack(ModItems.FLUID_PIPE.get(), 3));
        try {
            int registered = world.registrations().size();
            helper.assertTrue(cells.stream().allMatch(c -> level.getBlockState(c).canBeReplaced()), "The path is not clear: " + cells.stream().map(c -> c.toShortString() + "=" + level.getBlockState(c)).toList());
            var refused = PipeLaying.lay(player, InteractionHand.MAIN_HAND, west, Direction.EAST, east, Direction.WEST, cells);
            helper.assertTrue(refused != null && refused.getString().equals(PipeLaying.message("items", 4).getString()), "Laid four blocks with three pipe items: " + refused);
            helper.assertTrue(level.getBlockState(cells.getFirst()).isAir() && world.registrations().size() == registered,
                    "A refused lay changed the world: " + level.getBlockState(cells.getFirst()) + ", registrations " + registered + " -> " + world.registrations().size());
            player.getInventory().add(new ItemStack(ModItems.FLUID_PIPE.get(), 1));
            helper.assertTrue(PipeLaying.lay(player, InteractionHand.MAIN_HAND, west, Direction.EAST, east, Direction.WEST, List.of(cells.get(0), cells.get(2), cells.get(3))) != null,
                    "A route with a gap was laid");
            helper.assertTrue(PipeLaying.lay(player, InteractionHand.MAIN_HAND, west, Direction.EAST, east, Direction.WEST, cells) == null, "The line was refused");
            helper.assertTrue(world.registrations().size() == registered + 4, "Not one registration per laid block");
            helper.assertTrue(player.getInventory().countItem(ModItems.FLUID_PIPE.get()) == 0, "The pipe items were not taken");
            for (var cell : cells) {
                var record = world.at(position(level, cell)).orElseThrow();
                helper.assertTrue(record.device().kind() == TopologyCompiler.Kind.PIPE && record.device().joints() == ALONG_X && record.device().geometry().diameter() == .1,
                        "Laid block " + cell.toShortString() + " registered as " + record.device());
                helper.assertTrue(level.getBlockEntity(cell) instanceof FluidDeviceBlockEntity entity && entity.fluidIdentity() == record.device().id() && entity.joints() == ALONG_X,
                        "Laid block " + cell.toShortString() + " not bound to its registration");
                helper.assertTrue(connected(level, cell, Direction.WEST) && connected(level, cell, Direction.EAST), "Laid block " + cell.toShortString() + " not joined along the line");
            }
            helper.assertTrue(!connected(level, cells.get(1), Direction.SOUTH) && !connected(level, beside, Direction.NORTH), "The line joined the pipe it runs beside");
            helper.assertTrue(world.at(position(level, beside)).orElseThrow().device().joints() == PhysicalFluidTopology.ALL_FACES, "The hand-placed pipe lost its automatic joints");

            // One block out at once: its neighbours keep their joints towards it and show open ends.
            helper.assertTrue(PipeLaying.dismantle(player, InteractionHand.MAIN_HAND, cells.get(1)) == null, "The dismantle was refused");
            helper.assertTrue(level.getBlockState(cells.get(1)).isAir() && world.at(position(level, cells.get(1))).isEmpty(), "The dismantled block remains");
            helper.assertTrue(world.at(position(level, cells.get(0))).orElseThrow().device().joints() == ALONG_X && !connected(level, cells.get(0), Direction.EAST),
                    "The neighbour did not become an open end");
            helper.assertTrue(PipeLaying.dismantle(player, InteractionHand.MAIN_HAND, west) != null, "Dismantled a tank");

            // A one-block line between the two open ends joins them again; the ends need no edit.
            player.getInventory().add(new ItemStack(ModItems.FLUID_PIPE.get(), 1));
            helper.assertTrue(PipeLaying.lay(player, InteractionHand.MAIN_HAND, cells.get(0), Direction.EAST, cells.get(2), Direction.WEST, List.of(cells.get(1))) == null, "The gap was not refilled");
            for (var cell : cells)
                helper.assertTrue(connected(level, cell, Direction.WEST) && connected(level, cell, Direction.EAST), "Block " + cell.toShortString() + " not joined after the refill");
            helper.killAllEntities();
            helper.succeed();
        } finally {
            for (var at : placed) level.removeBlock(at, false);
        }
    }

    private static boolean connected(net.minecraft.world.level.Level level, BlockPos at, Direction face) {
        var state = level.getBlockState(at);
        return state.getBlock() instanceof FluidDeviceBlock && state.getValue(FluidDeviceBlock.connection(face));
    }

    private static PhysicalFluidTopology.Position position(net.minecraft.world.level.Level level, BlockPos at) {
        return new PhysicalFluidTopology.Position(level.dimension().location().toString(), at.getX(), at.getY(), at.getZ());
    }
}
