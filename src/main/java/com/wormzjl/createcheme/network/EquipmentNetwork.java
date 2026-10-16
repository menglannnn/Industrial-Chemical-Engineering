package com.wormzjl.createcheme.network;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.science.equipment.ColumnTrays;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.VesselPlacement;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentConstructorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;

/**
 * Equipment protocol {@value #PROTOCOL}: the constructor screen sends its design when it closes, and with a build
 * request when the player presses Build; it asks to import the design of the nearest column calculator. Placing a
 * vessel is a world edit, like placing blocks; it involves no process state, so the server answers at once in chat. An
 * import reads the calculator's stored input (nothing is solved) and comes back as the constructor's synced design.
 */
public final class EquipmentNetwork {
    public static final String PROTOCOL = "equipment-4";
    /** A player edits or builds only within this distance of the constructor, blocks. */
    private static final double REACH = 8;
    /** A column calculator is imported from within this many blocks of the constructor. */
    public static final int IMPORT_REACH = 16;

    private EquipmentNetwork() {
    }

    /**
     * Client to server: a constructor's design ({@code form} is the ordinal of its {@link VesselPlacement.Form},
     * {@code trays} its column's {@link ColumnTrays#encode()}, {@code connections} its drum's
     * {@link EquipmentShape.Connection#encode}), and whether to build it now.
     */
    public record ConstructorPayload(BlockPos position, double diameter, int tangentLength, int form, int support, int forward, int right, int up,
                                     int[] trays, int[] connections, boolean build) implements CustomPacketPayload {
        public static final Type<ConstructorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateChemE.MOD_ID, "equipment_constructor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConstructorPayload> STREAM_CODEC = new StreamCodec<>() {
            public ConstructorPayload decode(RegistryFriendlyByteBuf b) {
                return new ConstructorPayload(b.readBlockPos(), b.readDouble(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
                        b.readVarInt(), b.readVarIntArray(ColumnTrays.MAX_ENCODED), b.readVarIntArray(3 * EquipmentShape.MAX_CONNECTIONS), b.readBoolean());
            }

            public void encode(RegistryFriendlyByteBuf b, ConstructorPayload p) {
                b.writeBlockPos(p.position);
                b.writeDouble(p.diameter);
                b.writeVarInt(p.tangentLength);
                b.writeVarInt(p.form);
                b.writeVarInt(p.support);
                b.writeVarInt(p.forward);
                b.writeVarInt(p.right);
                b.writeVarInt(p.up);
                b.writeVarIntArray(p.trays);
                b.writeVarIntArray(p.connections);
                b.writeBoolean(p.build);
            }
        };

        public static ConstructorPayload of(BlockPos position, EquipmentConstructorBlockEntity.Design design, boolean build) {
            var p = design.placement();
            return new ConstructorPayload(position, design.diameter(), design.tangentLength(), design.form().ordinal(), design.support(), p.forward(), p.right(), p.up(),
                    design.trays().encode(), EquipmentShape.Connection.encode(design.connections()), build);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: import the nearest column calculator's design into this constructor. */
    public record ImportPayload(BlockPos position) implements CustomPacketPayload {
        public static final Type<ImportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateChemE.MOD_ID, "equipment_import_column"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ImportPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ImportPayload::position, ImportPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL).executesOn(HandlerThread.MAIN);
        registrar.playToServer(ConstructorPayload.TYPE, ConstructorPayload.STREAM_CODEC, EquipmentNetwork::constructor);
        registrar.playToServer(ImportPayload.TYPE, ImportPayload.STREAM_CODEC, EquipmentNetwork::importColumn);
    }

    /** Client: send a constructor's design, and build it if asked. */
    public static void send(BlockPos position, EquipmentConstructorBlockEntity.Design design, boolean build) {
        PacketDistributor.sendToServer(ConstructorPayload.of(position, design, build));
    }

    /** Client: ask for the nearest column calculator's design; it arrives as the constructor's synced design. */
    public static void requestImport(BlockPos position) {
        PacketDistributor.sendToServer(new ImportPayload(position));
    }

    /** The constructor a player may edit at {@code position}, or null. */
    private static EquipmentConstructorBlockEntity editable(ServerPlayer player, BlockPos position) {
        var level = player.serverLevel();
        if (!level.isLoaded(position) || !(level.getBlockEntity(position) instanceof EquipmentConstructorBlockEntity constructor)
                || player.distanceToSqr(position.getCenter()) > REACH * REACH || !player.mayBuild() || player.isSpectator() || !level.mayInteract(player, position))
            return null;
        return constructor;
    }

    private static void constructor(ConstructorPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        var constructor = editable(player, payload.position());
        if (constructor == null) return;
        EquipmentConstructorBlockEntity.Design design;
        try {
            var forms = VesselPlacement.Form.values();
            if (payload.form() < 0 || payload.form() >= forms.length) throw new IllegalArgumentException("Unknown form " + payload.form());
            design = new EquipmentConstructorBlockEntity.Design(payload.diameter(), payload.tangentLength(), forms[payload.form()], payload.support(),
                    new VesselPlacement(payload.forward(), payload.right(), payload.up()), ColumnTrays.decode(payload.trays()),
                    EquipmentShape.Connection.decode(payload.connections()));
        } catch (IllegalArgumentException invalid) {
            player.sendSystemMessage(Component.translatable("message.createcheme.equipment.invalid", invalid.getMessage()).withStyle(ChatFormatting.RED));
            return;
        }
        constructor.apply(design);
        if (!payload.build()) return;
        var vessel = constructor.vessel(design);
        // Overlapping side connections are a design the screen shows but the server does not build.
        if (!vessel.shape().clashes().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.createcheme.equipment.clash").withStyle(ChatFormatting.RED));
            return;
        }
        try {
            int placed = EquipmentStructure.place(player.serverLevel(), vessel.origin(), vessel.shape(), Integer.MAX_VALUE);
            player.sendSystemMessage(design.form().column()
                    ? Component.translatable("message.createcheme.equipment.built_column", design.diameter(), design.trays().trays(), placed, vessel.origin().toShortString())
                    : Component.translatable("message.createcheme.equipment.built", design.diameter(), design.tangentLength(), placed, vessel.origin().toShortString()));
        } catch (IllegalStateException blocked) {
            player.sendSystemMessage(Component.translatable("message.createcheme.equipment.blocked", blocked.getMessage()).withStyle(ChatFormatting.RED));
        }
    }

    private static void importColumn(ImportPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        var constructor = editable(player, payload.position());
        if (constructor == null) return;
        var imported = constructor.importColumn(IMPORT_REACH);
        if (imported.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.createcheme.equipment.no_calculator", IMPORT_REACH).withStyle(ChatFormatting.RED));
            return;
        }
        var design = imported.get();
        constructor.apply(design);
        player.sendSystemMessage(Component.translatable("message.createcheme.equipment.imported", design.trays().trays(), design.trays().feed(), design.diameter()));
    }
}
