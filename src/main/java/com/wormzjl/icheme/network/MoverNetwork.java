package com.wormzjl.icheme.network;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.client.MoverSounds;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.TankWatchers;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** Requested mover presentation, containing no process inventory or solver results. */
public final class MoverNetwork {
    public static final double RANGE=96;
    private MoverNetwork(){}
    public record Target(BlockPos position,long identity) {
        public Target{position=position.immutable();if(identity<=0)throw new IllegalArgumentException("Invalid mover identity");}
    }
    public record Watch(List<Target> watch,List<Target> unwatch) implements CustomPacketPayload {
        public static final Type<Watch> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,"mover_watch"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Watch> STREAM_CODEC=new StreamCodec<>() {
            public Watch decode(RegistryFriendlyByteBuf b){return new Watch(targets(b),targets(b));}
            public void encode(RegistryFriendlyByteBuf b,Watch p){targets(b,p.watch);targets(b,p.unwatch);}
        };
        public Watch{watch=List.copyOf(watch);unwatch=List.copyOf(unwatch);validate(watch);validate(unwatch);}
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record State(Target target,boolean compressor,boolean running) implements CustomPacketPayload {
        public State{Objects.requireNonNull(target);}
        public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,"mover_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> STREAM_CODEC=new StreamCodec<>() {
            public State decode(RegistryFriendlyByteBuf b){return new State(MoverNetwork.target(b),flag(b),flag(b));}
            public void encode(RegistryFriendlyByteBuf b,State p){MoverNetwork.target(b,p.target);b.writeBoolean(p.compressor);b.writeBoolean(p.running);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static boolean flag(RegistryFriendlyByteBuf b){int flag=b.readUnsignedByte();if(flag>1)throw new IllegalArgumentException("Invalid mover state flag");return flag==1;}
    private static void validate(List<Target> list){if(list.size()>TankWatchers.MAXIMUM_PER_PLAYER||new HashSet<>(list).size()!=list.size())throw new IllegalArgumentException("Invalid mover watch list");}
    private static Target target(RegistryFriendlyByteBuf b){return new Target(b.readBlockPos(),b.readLong());}
    private static void target(RegistryFriendlyByteBuf b,Target t){b.writeBlockPos(t.position);b.writeLong(t.identity);}
    private static List<Target> targets(RegistryFriendlyByteBuf b){int count=b.readVarInt();if(count<0||count>TankWatchers.MAXIMUM_PER_PLAYER)throw new IllegalArgumentException("Too many movers");var result=new ArrayList<Target>(count);for(int i=0;i<count;i++)result.add(target(b));return result;}
    private static void targets(RegistryFriendlyByteBuf b,List<Target> list){b.writeVarInt(list.size());for(var target:list)target(b,target);}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("mover-1").executesOn(HandlerThread.MAIN);
        registrar.playToServer(Watch.TYPE,Watch.STREAM_CODEC,(p,context)->{
            if(context.player() instanceof ServerPlayer player)FluidWorldAuthority.find(player.server).ifPresent(world->world.watchMovers(player,p.watch,p.unwatch));
        });
        registrar.playToClient(State.TYPE,State.STREAM_CODEC,(p,context)->MoverSounds.accept(p));
    }
    public static void request(List<Target> watch,List<Target> unwatch){if(!watch.isEmpty()||!unwatch.isEmpty())PacketDistributor.sendToServer(new Watch(watch,unwatch));}
    public static void send(ServerPlayer player,State state){PacketDistributor.sendToPlayer(player,state);}
}
