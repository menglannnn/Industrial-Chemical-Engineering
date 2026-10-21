package com.wormzjl.icheme.registry;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.world.level.block.ColumnCalculatorV3Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IChemE.MOD_ID);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> FLUID_RESERVOIR=fluid("fluid_reservoir",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.RESERVOIR);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> FLUID_PIPE=fluid("fluid_pipe",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.PIPE);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> FLUID_PUMP=fluid("fluid_pump",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.PUMP);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> FLUID_COMPRESSOR=fluid("fluid_compressor",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.COMPRESSOR);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> PRESSURE_CONTROL_VALVE=fluid("pressure_control_valve",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.VALVE);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> FLUID_GENERATOR=fluid("fluid_generator",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.GENERATOR);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> FLUID_VOID=fluid("fluid_void",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.VOID);
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> INLINE_FILTER=fluid("inline_filter",com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind.FILTER);
    private static DeferredBlock<com.wormzjl.icheme.world.level.block.FluidDeviceBlock> fluid(String name,com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind kind) {
        var properties=BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.METAL).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK);
        // Every device model (the inline filter too, a Y-strainer since 2026-10-16) leaves part of its block space open:
        // none occludes the neighbouring block's faces in the chunk renderer, and light reaches inside each.
        properties.noOcclusion();
        return BLOCKS.registerBlock(name,p->new com.wormzjl.icheme.world.level.block.FluidDeviceBlock(kind,p),properties);
    }

    public static final DeferredBlock<ColumnCalculatorV3Block> COLUMN_CALCULATOR_V3 = BLOCKS.registerBlock(
            "column_calculator_v3",
            ColumnCalculatorV3Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
    );

    // Parametric vessels (batch 2026-10-15-custom-equipment-rendering): placed whole by EquipmentStructure, no items, no drops.
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.EquipmentShellBlock> EQUIPMENT_SHELL = BLOCKS.registerBlock(
            "equipment_shell",
            com.wormzjl.icheme.world.level.block.EquipmentShellBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.METAL)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).noOcclusion().dynamicShape().noLootTable()
    );
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.EquipmentInteriorBlock> EQUIPMENT_INTERIOR = BLOCKS.registerBlock(
            "equipment_interior",
            com.wormzjl.icheme.world.level.block.EquipmentInteriorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.METAL)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).noLootTable()
                    // An open tank's cells collide up to its floating roof, which moves (FloatingRoofs); nothing spawns on it.
                    // A dynamic shape leaves the state unsolid, which lets fluids wash it away: forced solid.
                    .dynamicShape().forceSolidOn().isValidSpawn((state, level, position, type) -> false)
    );
    public static final DeferredBlock<com.wormzjl.icheme.world.level.block.EquipmentConstructorBlock> EQUIPMENT_CONSTRUCTOR = BLOCKS.registerBlock(
            "equipment_constructor",
            com.wormzjl.icheme.world.level.block.EquipmentConstructorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
