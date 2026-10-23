package com.wormzjl.icheme.network;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.client.SightGlassDisplays;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.TankWatchers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** Explicit, bounded visual subscriptions. Only packed colour is delivered, never a full process/menu snapshot. */
public final class SightGlassNetwork {
    private SightGlassNetwork(){}
    public record Target(BlockPos position,long identity) {
        public Target{position=position.immutable();if(identity<=0)throw new IllegalArgumentException("Invalid sight glass identity");}
    }
    public record WatchPayload(List<Target> watch,List<Target> unwatch) implements CustomPacketPayload {
        public static final Type<WatchPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,"sight_glass_watch"));
        public static final StreamCodec<RegistryFriendlyByteBuf,WatchPayload> STREAM_CODEC=new StreamCodec<>() {
            public WatchPayload decode(RegistryFriendlyByteBuf b){return new WatchPayload(targets(b),targets(b));}
            public void encode(RegistryFriendlyByteBuf b,WatchPayload p){targets(b,p.watch);targets(b,p.unwatch);}
        };
        public WatchPayload{watch=List.copyOf(watch);unwatch=List.copyOf(unwatch);validate(watch);validate(unwatch);}
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record DisplayPayload(Target target,int argb) implements CustomPacketPayload {
        public static final Type<DisplayPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,"sight_glass_display"));
        public static final StreamCodec<RegistryFriendlyByteBuf,DisplayPayload> STREAM_CODEC=new StreamCodec<>() {
            public DisplayPayload decode(RegistryFriendlyByteBuf b){return new DisplayPayload(SightGlassNetwork.target(b),b.readInt());}
            public void encode(RegistryFriendlyByteBuf b,DisplayPayload p){SightGlassNetwork.target(b,p.target);b.writeInt(p.argb);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static void validate(List<Target> list){if(list.size()>TankWatchers.MAXIMUM_PER_PLAYER||new HashSet<>(list).size()!=list.size())throw new IllegalArgumentException("Invalid sight glass watch list");}
    private static Target target(RegistryFriendlyByteBuf b){return new Target(b.readBlockPos(),b.readLong());}
    private static void target(RegistryFriendlyByteBuf b,Target t){b.writeBlockPos(t.position);b.writeLong(t.identity);}
    private static List<Target> targets(RegistryFriendlyByteBuf b){int count=b.readVarInt();if(count<0||count>TankWatchers.MAXIMUM_PER_PLAYER)throw new IllegalArgumentException("Too many sight glasses");var result=new ArrayList<Target>(count);for(int i=0;i<count;i++)result.add(target(b));return result;}
    private static void targets(RegistryFriendlyByteBuf b,List<Target> list){b.writeVarInt(list.size());for(var t:list)target(b,t);}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("sight-glass-1").executesOn(HandlerThread.MAIN);
        registrar.playToServer(WatchPayload.TYPE,WatchPayload.STREAM_CODEC,(payload,context)->{
            if(context.player() instanceof ServerPlayer player)FluidWorldAuthority.find(player.server).ifPresent(world->world.watchSightGlasses(player,payload.watch,payload.unwatch));
        });
        registrar.playToClient(DisplayPayload.TYPE,DisplayPayload.STREAM_CODEC,(payload,context)->SightGlassDisplays.accept(payload.target,payload.argb));
    }
    public static void request(List<Target> watch,List<Target> unwatch){if(!watch.isEmpty()||!unwatch.isEmpty())PacketDistributor.sendToServer(new WatchPayload(watch,unwatch));}
    public static void send(ServerPlayer player,Target target,int argb){PacketDistributor.sendToPlayer(player,new DisplayPayload(target,argb));}
}
