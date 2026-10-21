package com.wormzjl.icheme.network;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.client.TankDisplays;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.TankDisplay;
import com.wormzjl.icheme.runtime.fluid.TankWatchers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;

/**
 * The open tanks' display path (batch 2026-10-21-floating-roof-tanks, plan section 3.4, owner decision FR4 R-B): a client
 * asks to watch and to stop watching tanks by their node cells ({@link WatchPayload}); the server answers each new watch
 * with the tank's last published display, if it has one, and then sends one {@link DisplayPayload} at each of the tank's
 * presentation buckets ({@link TankWatchers}). About 32 bytes a tank every 5 s; nothing is sent unasked.
 */
public final class TankDisplayNetwork {
    private TankDisplayNetwork() {}
    /** Client to server: the node cells to start and to stop watching, each list at most {@link TankWatchers#MAXIMUM_PER_PLAYER}. */
    public record WatchPayload(List<BlockPos> watch,List<BlockPos> unwatch) implements CustomPacketPayload {
        public static final Type<WatchPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,"tank_watch"));
        public static final StreamCodec<RegistryFriendlyByteBuf,WatchPayload> STREAM_CODEC=new StreamCodec<>() {
            public WatchPayload decode(RegistryFriendlyByteBuf b){return new WatchPayload(positions(b),positions(b));}
            public void encode(RegistryFriendlyByteBuf b,WatchPayload p){positions(b,p.watch);positions(b,p.unwatch);}
        };
        public WatchPayload{watch=List.copyOf(watch);unwatch=List.copyOf(unwatch);
            if(watch.size()>TankWatchers.MAXIMUM_PER_PLAYER||unwatch.size()>TankWatchers.MAXIMUM_PER_PLAYER)throw new IllegalArgumentException("Too many tanks in one watch request");}
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** Server to client: one tank's display at its node cell. */
    public record DisplayPayload(BlockPos node,TankDisplay display) implements CustomPacketPayload {
        public static final Type<DisplayPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,"tank_display"));
        public static final StreamCodec<RegistryFriendlyByteBuf,DisplayPayload> STREAM_CODEC=new StreamCodec<>() {
            public DisplayPayload decode(RegistryFriendlyByteBuf b){return new DisplayPayload(b.readBlockPos(),new TankDisplay(b.readFloat(),b.readFloat(),b.readFloat(),b.readInt(),b.readInt()));}
            public void encode(RegistryFriendlyByteBuf b,DisplayPayload p){var d=p.display;b.writeBlockPos(p.node);b.writeFloat(d.level());b.writeFloat(d.waterLevel());b.writeFloat(d.height());b.writeInt(d.oilArgb());b.writeInt(d.waterArgb());}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static List<BlockPos> positions(RegistryFriendlyByteBuf b) {
        int count=b.readVarInt();if(count<0||count>TankWatchers.MAXIMUM_PER_PLAYER)throw new IllegalArgumentException("Too many tanks in one watch request");
        var list=new ArrayList<BlockPos>(count);for(int i=0;i<count;i++)list.add(b.readBlockPos());
        return list;
    }
    private static void positions(RegistryFriendlyByteBuf b,List<BlockPos> list){b.writeVarInt(list.size());for(var pos:list)b.writeBlockPos(pos);}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar(FluidNetwork.PROTOCOL).executesOn(HandlerThread.MAIN);
        registrar.playToServer(WatchPayload.TYPE,WatchPayload.STREAM_CODEC,TankDisplayNetwork::watch);
        registrar.playToClient(DisplayPayload.TYPE,DisplayPayload.STREAM_CODEC,(payload,context)->TankDisplays.accept(payload.node,payload.display));
    }
    private static void watch(WatchPayload payload,IPayloadContext context) {
        if(context.player() instanceof ServerPlayer player)FluidWorldAuthority.find(player.server).ifPresent(world->world.watchTanks(player,payload.watch,payload.unwatch));
    }
    /** Sends {@code display} of the tank at {@code node} to {@code player}. */
    public static void send(ServerPlayer player,BlockPos node,TankDisplay display){PacketDistributor.sendToPlayer(player,new DisplayPayload(node,display));}
    /** Client side: asks the server to watch and to stop watching tanks. */
    public static void request(List<BlockPos> watch,List<BlockPos> unwatch){if(!watch.isEmpty()||!unwatch.isEmpty())PacketDistributor.sendToServer(new WatchPayload(watch,unwatch));}
}
