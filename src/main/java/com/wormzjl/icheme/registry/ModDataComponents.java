package com.wormzjl.icheme.registry;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.world.item.PipelineToolSettings;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent item settings, synchronized through the ordinary inventory protocol. */
public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, IChemE.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PipelineToolSettings>> PIPELINE_TOOL_SETTINGS =
            COMPONENTS.register("pipeline_tool_settings", () -> DataComponentType.<PipelineToolSettings>builder()
                    .persistent(PipelineToolSettings.CODEC).networkSynchronized(PipelineToolSettings.STREAM_CODEC).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.wormzjl.icheme.world.item.GaugeSettings>> GAUGE_SETTINGS = COMPONENTS.register("gauge_settings", () -> DataComponentType.<com.wormzjl.icheme.world.item.GaugeSettings>builder().persistent(com.wormzjl.icheme.world.item.GaugeSettings.CODEC).networkSynchronized(com.wormzjl.icheme.world.item.GaugeSettings.STREAM_CODEC).build());

    private ModDataComponents() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
