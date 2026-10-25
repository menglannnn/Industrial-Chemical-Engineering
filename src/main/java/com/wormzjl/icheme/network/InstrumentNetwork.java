package com.wormzjl.icheme.network;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.instrument.*;
import com.wormzjl.icheme.world.inventory.GaugeMenu;
import com.wormzjl.icheme.world.item.GaugeSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import java.util.*;

/** Explicit bounded instrument watches. Ordinary block updates carry no live measurements. */
public final class InstrumentNetwork {
    private InstrumentNetwork(){}
    public record Target(BlockPos position,Instrument.Key key,UUID instance) {
        public Target{position=position.immutable();Objects.requireNonNull(key);Objects.requireNonNull(instance);}
    }
    public record Watch(List<Target> watch,List<Target> unwatch) implements CustomPacketPayload {
        public static final Type<Watch> TYPE=InstrumentNetwork.type("instrument_watch");
        public static final StreamCodec<RegistryFriendlyByteBuf,Watch> CODEC=new StreamCodec<>(){
            public Watch decode(RegistryFriendlyByteBuf b){return new Watch(targets(b),targets(b));}
            public void encode(RegistryFriendlyByteBuf b,Watch p){targets(b,p.watch);targets(b,p.unwatch);}
        };
        public Watch{watch=List.copyOf(watch);unwatch=List.copyOf(unwatch);for(var l:List.of(watch,unwatch))if(l.size()>64||new HashSet<>(l).size()!=l.size())throw new IllegalArgumentException("Invalid instrument watches");}
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Display(Target target,Instrument.Sample sample,long publishedTick) implements CustomPacketPayload {
        public static final Type<Display> TYPE=InstrumentNetwork.type("instrument_display");
        public static final StreamCodec<RegistryFriendlyByteBuf,Display> CODEC=new StreamCodec<>(){
            public Display decode(RegistryFriendlyByteBuf b){var t=InstrumentNetwork.target(b);double value=b.readDouble();long tick=b.readLong();int code=b.readUnsignedByte();if(code>=Instrument.Availability.values().length)throw new IllegalArgumentException("Invalid instrument availability");return new Display(t,new Instrument.Sample(value,tick,Instrument.Availability.fromCode(code)),b.readLong());}
            public void encode(RegistryFriendlyByteBuf b,Display p){InstrumentNetwork.target(b,p.target);b.writeDouble(p.sample.value());b.writeLong(p.sample.tick());b.writeByte(p.sample.availability().code());b.writeLong(p.publishedTick);}
        };
        public Display{if(publishedTick<sample.tick())throw new IllegalArgumentException("Invalid publication age");}
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Save(int menu,GaugeSettings settings) implements CustomPacketPayload {
        public static final Type<Save> TYPE=InstrumentNetwork.type("gauge_save");
        public static final StreamCodec<RegistryFriendlyByteBuf,Save> CODEC=new StreamCodec<>(){
            public Save decode(RegistryFriendlyByteBuf b){return new Save(b.readVarInt(),GaugeSettings.STREAM_CODEC.decode(b));}
            public void encode(RegistryFriendlyByteBuf b,Save p){b.writeVarInt(p.menu);GaugeSettings.STREAM_CODEC.encode(b,p.settings);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,path));}
    private static Target target(RegistryFriendlyByteBuf b){return new Target(b.readBlockPos(),new Instrument.Key(b.readLong(),b.readUtf(64)),b.readUUID());}
    private static void target(RegistryFriendlyByteBuf b,Target t){b.writeBlockPos(t.position);b.writeLong(t.key.host());b.writeUtf(t.key.mount(),64);b.writeUUID(t.instance);}
    private static List<Target> targets(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>64)throw new IllegalArgumentException("Too many instrument targets");var l=new ArrayList<Target>();for(int i=0;i<n;i++)l.add(target(b));return l;}
    private static void targets(RegistryFriendlyByteBuf b,List<Target> l){b.writeVarInt(l.size());l.forEach(t->target(b,t));}
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("instruments-1").executesOn(HandlerThread.MAIN);r.playToServer(Watch.TYPE,Watch.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer player)FluidWorldAuthority.find(player.server).ifPresent(w->w.instruments().watch(player,p.watch,p.unwatch));});r.playToClient(Display.TYPE,Display.CODEC,(p,c)->com.wormzjl.icheme.client.InstrumentDisplays.accept(p));r.playToServer(Save.TYPE,Save.CODEC,(p,c)->{if(c.player() instanceof ServerPlayer player&&player.containerMenu instanceof GaugeMenu menu&&menu.containerId==p.menu&&menu.stillValid(player)&&menu.initial().kind()==p.settings.kind()){menu.save(p.settings);player.inventoryMenu.broadcastChanges();}});}
    public static void save(int menu,GaugeSettings settings){PacketDistributor.sendToServer(new Save(menu,settings));}
    public static void request(List<Target> watch,List<Target> unwatch){if(!watch.isEmpty()||!unwatch.isEmpty())PacketDistributor.sendToServer(new Watch(watch,unwatch));}
    public static void send(ServerPlayer player,Target target,Instrument.Sample sample,long tick){PacketDistributor.sendToPlayer(player,new Display(target,sample,tick));}
}
