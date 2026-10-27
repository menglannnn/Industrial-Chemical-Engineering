package com.wormzjl.icheme.network;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.science.equipment.ColumnDesign;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.Paint;
import com.wormzjl.icheme.science.equipment.ProcessUnit;
import com.wormzjl.icheme.science.equipment.VesselPlacement;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import com.wormzjl.icheme.world.level.block.entity.EquipmentConstructorBlockEntity;
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
    public static final String PROTOCOL = "equipment-12";
    /** A player edits or builds only within this distance of the constructor, blocks. */
    private static final double REACH = 8;
    /** A column calculator is imported from within this many blocks of the constructor. */
    public static final int IMPORT_REACH = 16;

    private EquipmentNetwork() {
    }

    /**
     * Client to server: a constructor's design ({@code form} is the ordinal of its {@link VesselPlacement.Form},
     * {@code trays} its column's {@link ColumnTrays#encode()},
     * {@code connections} its drum's {@link EquipmentShape.Connection#encode}, {@code paint} its {@link Paint#encode},
     * {@code bank} an air cooler's {@link ProcessUnit.Bank#encode}, {@code boot} whether a lying drum has one,
     * {@code outlet} its bottom outlet's quarter turns from the front, {@code turns} its column's turned nozzles
     * ({@link ColumnTrays#encodeTurns})), and whether to build it now.
     */
    public record ConstructorPayload(BlockPos position, double diameter, int tangentLength, int form, int support, int forward, int right, int up,
                                     int[] trays, int[] connections, int[] paint, int[] bank, boolean boot, int outlet, int[] turns, boolean build) implements CustomPacketPayload {
        public static final Type<ConstructorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "equipment_constructor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConstructorPayload> STREAM_CODEC = new StreamCodec<>() {
            public ConstructorPayload decode(RegistryFriendlyByteBuf b) {
                return new ConstructorPayload(b.readBlockPos(), b.readDouble(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
                        b.readVarInt(), b.readVarIntArray(ColumnTrays.MAX_ENCODED), b.readVarIntArray(3 * EquipmentShape.MAX_CONNECTIONS), b.readVarIntArray(2), b.readVarIntArray(2), b.readBoolean(), b.readVarInt(),
                        b.readVarIntArray(3 * ColumnTrays.MAX_TURNS), b.readBoolean());
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
                b.writeVarIntArray(p.paint);
                b.writeVarIntArray(p.bank);
                b.writeBoolean(p.boot);
                b.writeVarInt(p.outlet);
                b.writeVarIntArray(p.turns);
                b.writeBoolean(p.build);
            }
        };

        public static ConstructorPayload of(BlockPos position, EquipmentConstructorBlockEntity.Design design, boolean build) {
            var p = design.placement();
            return new ConstructorPayload(position, design.diameter(), design.tangentLength(), design.form().ordinal(), design.support(), p.forward(), p.right(), p.up(),
                    design.trays().encode(), EquipmentShape.Connection.encode(design.connections()), design.paint().encode(), design.bank().encode(), design.boot(), design.outlet(),
                    ColumnTrays.encodeTurns(design.turns()), build);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: import the nearest column calculator's design into this constructor. */
    public record ImportPayload(BlockPos position) implements CustomPacketPayload {
        public static final Type<ImportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "equipment_import_column"));
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
            // The imported column stays the server's: only an import sets it (plan section 4.5).
            design = constructor.design().keepingColumn(new EquipmentConstructorBlockEntity.Design(payload.diameter(), payload.tangentLength(), forms[payload.form()],
                    payload.support(), new VesselPlacement(payload.forward(), payload.right(), payload.up()), ColumnTrays.decode(payload.trays()),
                    EquipmentShape.Connection.decode(payload.connections()), Paint.decode(payload.paint()), ProcessUnit.Bank.decode(payload.bank()), payload.boot(), payload.outlet(),
                    null, ColumnTrays.decodeTurns(payload.turns())));
        } catch (IllegalArgumentException invalid) {
            player.sendSystemMessage(Component.translatable("message.icheme.equipment.invalid", invalid.getMessage()).withStyle(ChatFormatting.RED));
            return;
        }
        constructor.apply(design);
        if (!payload.build()) return;
        if (!design.buildable()) {
            player.sendSystemMessage(Component.translatable("message.icheme.equipment.import_first", IMPORT_REACH).withStyle(ChatFormatting.RED));
            return;
        }
        var vessel = constructor.vessel(design);
        // Overlapping side connections are a design the screen shows but the server does not build.
        if (!vessel.shape().clashes().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.icheme.equipment.clash").withStyle(ChatFormatting.RED));
            return;
        }
        // The calculator checked the nozzles on the least skirt; this column stands on the constructor's.
        var misfit = design.form().column() ? ColumnDesign.misfit(vessel.shape()) : java.util.Optional.<String>empty();
        if (misfit.isPresent()) {
            player.sendSystemMessage(Component.translatable("message.icheme.equipment.misfit", misfit.get()).withStyle(ChatFormatting.RED));
            return;
        }
        try {
            int placed = EquipmentStructure.place(player.serverLevel(), vessel.origin(), vessel.shape(), design.paint(), Integer.MAX_VALUE);
            var at = vessel.origin().toShortString();
            player.sendSystemMessage(design.form().column()
                    ? Component.translatable("message.icheme.equipment.built_column", vessel.shape().diameter(), vessel.shape().trays().trays() + 2, placed, at)
                    : vessel.shape().designed() ? Component.translatable("message.icheme.equipment.built_unit",
                    Component.translatable("gui.icheme.vessel.title." + vessel.shape().kind().name().toLowerCase(java.util.Locale.ROOT)), placed, at)
                    : Component.translatable("message.icheme.equipment.built", design.diameter(), design.tangentLength(), placed, at));
        } catch (IllegalStateException blocked) {
            player.sendSystemMessage(Component.translatable("message.icheme.equipment.blocked", blocked.getMessage()).withStyle(ChatFormatting.RED));
        }
    }

    private static void importColumn(ImportPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        var constructor = editable(player, payload.position());
        if (constructor == null) return;
        var calculator = constructor.importColumn(IMPORT_REACH);
        if (calculator.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.icheme.equipment.no_calculator", IMPORT_REACH).withStyle(ChatFormatting.RED));
            return;
        }
        if (calculator.get().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.icheme.equipment.no_design").withStyle(ChatFormatting.RED));
            return;
        }
        var design = calculator.get().get();
        constructor.apply(design);
        var column = design.column();
        // The calculator's numbering: its trays and the condenser and reboiler.
        player.sendSystemMessage(Component.translatable("message.icheme.equipment.imported", column.trays().trays() + 2, column.trays().feed() + 1, column.diameter()));
    }
}
