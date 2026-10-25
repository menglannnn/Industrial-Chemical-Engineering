package com.wormzjl.icheme.network;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.world.inventory.PipelineToolMenu;
import com.wormzjl.icheme.world.item.PipelineToolItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** Settings only: changing a tool's target does not read, edit or subscribe to a fluid process. */
public final class PipelineToolNetwork {
    private PipelineToolNetwork() {
    }

    public record SavePayload(int menuId, double diameter) implements CustomPacketPayload {
        public static final Type<SavePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "pipeline_tool_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SavePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SavePayload::menuId, ByteBufCodecs.DOUBLE, SavePayload::diameter, SavePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("pipeline-tool-1").executesOn(HandlerThread.MAIN)
                .playToServer(SavePayload.TYPE, SavePayload.STREAM_CODEC, PipelineToolNetwork::save);
    }

    public static void sendSave(int menuId, double diameter) {
        PacketDistributor.sendToServer(new SavePayload(menuId, diameter));
    }

    private static void save(SavePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof PipelineToolMenu menu)
                || menu.containerId != payload.menuId() || !menu.stillValid(player) || !PipelineToolItem.Settings.valid(payload.diameter())) return;
        menu.save(new PipelineToolItem.Settings(payload.diameter()));
        // The screen closes after sending Save. Also sync the inventory menu so its cached copy carries the new target.
        player.inventoryMenu.broadcastChanges();
    }
}
