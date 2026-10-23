package com.wormzjl.icheme.registry;

import com.wormzjl.icheme.IChemE;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original positional machine loops; ordinary Blocks sound settings apply. */
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,IChemE.MOD_ID);
    public static final DeferredHolder<SoundEvent,SoundEvent> PUMP_RUNNING=loop("pump_running");
    public static final DeferredHolder<SoundEvent,SoundEvent> COMPRESSOR_RUNNING=loop("compressor_running");
    private ModSounds(){}
    private static DeferredHolder<SoundEvent,SoundEvent> loop(String name){return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,name)));}
    public static void register(IEventBus bus){SOUNDS.register(bus);}
}
