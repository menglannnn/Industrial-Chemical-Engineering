package com.wormzjl.icheme.network;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.registry.ModItems;
import com.wormzjl.icheme.runtime.assembly.AssemblyPresentation;
import com.wormzjl.icheme.runtime.assembly.AssemblyService;
import com.wormzjl.icheme.runtime.assembly.AssemblyWatch;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.science.assembly.AssemblyReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortState;
import com.wormzjl.icheme.science.assembly.AssemblyReport.SlotReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.State;
import com.wormzjl.icheme.science.assembly.AssemblyView;
import com.wormzjl.icheme.science.assembly.Blueprint.Slot;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.world.inventory.AssemblyMenu;
import com.wormzjl.icheme.world.level.block.EquipmentShellBlock;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import com.wormzjl.icheme.world.level.block.EquipmentStructure.Vessel;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Assembly protocol {@value #PROTOCOL} (batch 2026-10-26-multi-machine-framework, plan sections 6, 8 and 10). The server
 * decides what a click on a structure opens: a column's GUI; for a unit that a column's report holds, that column's GUI
 * with the unit selected; otherwise the structure's own screen ({@link #datasheet} for a unit with no fluid screen).
 * An open GUI is a session of the engine's presentation: it gets the stored view at once, a press of "Validate
 * structure" queues one command that the next bucket runs, and the answer comes with that bucket. Holding the laying
 * tool is a watch of the columns around the player ({@link AssemblyWatch}, plan section 9): taking it up sends
 * {@link WatchPayload}, which replays their stored views, and their changes come with the buckets ({@link WatchedPayload}).
 */
public final class AssemblyNetwork {
    public static final String PROTOCOL = "assembly-2";
    /** Bounds a view's read checks: slots, ports, members, characters of a slot's name or detail. */
    static final int MAX_SLOTS = 256, MAX_PORTS = 1024, MAX_MEMBERS = 256, MAX_TEXT = 1024;

    private static final Map<MinecraftServer, AssemblyPresentation<AssemblyMenu>> SERVERS = new IdentityHashMap<>();
    private static final Map<MinecraftServer, AssemblyWatch<ServerPlayer>> WATCHES = new IdentityHashMap<>();

    private AssemblyNetwork() {
    }

    /** A view on the wire: its report without the footprint, which no client reads. */
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyView> VIEW = StreamCodec.of(AssemblyNetwork::write, AssemblyNetwork::read);

    private static void write(RegistryFriendlyByteBuf b, AssemblyView view) {
        b.writeEnum(view.status());
        b.writeLong(view.changedAt());
        var report = view.report();
        if (report == null) return;
        b.writeVarInt(report.slots().size());
        for (var s : report.slots()) {
            b.writeUtf(s.slot().part(), MAX_TEXT);
            b.writeVarInt(s.slot().index());
            b.writeEnum(s.state());
            b.writeLong(s.member());
            b.writeLong(s.at());
            b.writeUtf(s.detail(), MAX_TEXT);
        }
        b.writeVarInt(report.ports().size());
        for (var p : report.ports()) {
            b.writeLong(p.unit());
            b.writeEnum(p.role());
            b.writeVarInt(p.ordinal());
            b.writeEnum(p.state());
        }
        b.writeVarInt(report.members().size());
        for (long m : report.members()) b.writeLong(m);
    }

    private static AssemblyView read(RegistryFriendlyByteBuf b) {
        var status = b.readEnum(AssemblyView.Status.class);
        long changedAt = b.readLong();
        if (status != AssemblyView.Status.CHECKED && status != AssemblyView.Status.COMPLETE) return new AssemblyView(status, null, changedAt);
        var slots = new ArrayList<SlotReport>();
        for (int i = 0, n = count(b, MAX_SLOTS); i < n; i++) {
            String part = b.readUtf(MAX_TEXT);
            int index = b.readVarInt();
            slots.add(new SlotReport(new Slot(part, index), b.readEnum(State.class), b.readLong(), b.readLong(), b.readUtf(MAX_TEXT)));
        }
        var ports = new ArrayList<PortReport>();
        for (int i = 0, n = count(b, MAX_PORTS); i < n; i++) {
            long unit = b.readLong();
            var role = b.readEnum(Role.class);
            int ordinal = b.readVarInt();
            if (ordinal < 0) throw new DecoderException("negative port ordinal");
            ports.add(new PortReport(unit, role, ordinal, b.readEnum(PortState.class)));
        }
        var members = new ArrayList<Long>();
        for (int i = 0, n = count(b, MAX_MEMBERS); i < n; i++) members.add(b.readLong());
        try {
            return new AssemblyView(status, new AssemblyReport(slots, ports, Set.of(), members), changedAt);
        } catch (IllegalArgumentException invalid) {
            throw new DecoderException(invalid.getMessage());
        }
    }

    private static int count(RegistryFriendlyByteBuf b, int max) {
        int n = b.readVarInt();
        if (n < 0 || n > max) throw new DecoderException("count " + n + " out of range 0.." + max);
        return n;
    }

    /** Server to client: the view of the column whose GUI is open in container {@code container}. */
    public record StatePayload(int container, AssemblyView view) implements CustomPacketPayload {
        public static final Type<StatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "assembly_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, StatePayload::container, VIEW,
                StatePayload::view, StatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: "Validate structure" in the GUI of container {@code container}. */
    public record ValidatePayload(int container) implements CustomPacketPayload {
        public static final Type<ValidatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "assembly_validate"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ValidatePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ValidatePayload::container,
                ValidatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: open the own screen of the unit at {@code member} (the column, or a unit its report holds). */
    public record OpenUnitPayload(int container, long member) implements CustomPacketPayload {
        public static final Type<OpenUnitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "assembly_open_unit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenUnitPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenUnitPayload::container,
                ByteBufCodecs.VAR_LONG, OpenUnitPayload::member, OpenUnitPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the player took the laying tool up ({@code true}) or put it away. */
    public record WatchPayload(boolean on) implements CustomPacketPayload {
        public static final Type<WatchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "assembly_watch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WatchPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, WatchPayload::on, WatchPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: the view of the watched column at {@code master}, or its end (empty): out of range or gone. */
    public record WatchedPayload(long master, Optional<AssemblyView> view) implements CustomPacketPayload {
        public static final Type<WatchedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "assembly_watched"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WatchedPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, WatchedPayload::master,
                ByteBufCodecs.optional(VIEW), WatchedPayload::view, WatchedPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: open the datasheet of the structure holding {@code cell}. */
    public record DatasheetPayload(BlockPos cell) implements CustomPacketPayload {
        public static final Type<DatasheetPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "assembly_datasheet"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DatasheetPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, DatasheetPayload::cell,
                DatasheetPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL).executesOn(HandlerThread.MAIN);
        registrar.playToServer(ValidatePayload.TYPE, ValidatePayload.STREAM_CODEC, AssemblyNetwork::validate);
        registrar.playToServer(OpenUnitPayload.TYPE, OpenUnitPayload.STREAM_CODEC, AssemblyNetwork::openUnit);
        registrar.playToServer(WatchPayload.TYPE, WatchPayload.STREAM_CODEC, AssemblyNetwork::watch);
        registrar.playToClient(WatchedPayload.TYPE, WatchedPayload.STREAM_CODEC,
                (payload, context) -> com.wormzjl.icheme.client.AssemblyToolClient.received(payload.master(), payload.view().orElse(null)));
        registrar.playToClient(StatePayload.TYPE, StatePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof AssemblyMenu menu && menu.containerId == payload.container()) menu.accept(payload.view());
        });
        registrar.playToClient(DatasheetPayload.TYPE, DatasheetPayload.STREAM_CODEC,
                (payload, context) -> com.wormzjl.icheme.client.EquipmentClient.openVessel(payload.cell()));
    }

    /** The sessions of the open column GUIs, on the server thread. */
    private static final AssemblyPresentation.Host<AssemblyMenu> HOST = new AssemblyPresentation.Host<>() {
        @Override
        public boolean valid(AssemblyMenu menu) {
            var player = menu.serverPlayer();
            return player != null && !player.hasDisconnected() && player.containerMenu == menu && menu.stillValid(player);
        }

        @Override
        public long master(AssemblyMenu menu) {
            return menu.origin().asLong();
        }

        @Override
        public @Nullable AssemblyView view(AssemblyMenu menu) {
            return AssemblyService.view(Objects.requireNonNull(menu.serverPlayer()).serverLevel(), menu.cell());
        }

        @Override
        public void validate(AssemblyMenu menu) {
            AssemblyService.validate(Objects.requireNonNull(menu.serverPlayer()).serverLevel(), menu.cell());
        }

        @Override
        public void send(AssemblyMenu menu, AssemblyView view) {
            PacketDistributor.sendToPlayer(Objects.requireNonNull(menu.serverPlayer()), new StatePayload(menu.containerId, view));
        }
    };

    /** The laying tool's watches, on the server thread. */
    private static final AssemblyWatch.Host<ServerPlayer> WATCH = new AssemblyWatch.Host<>() {
        @Override
        public boolean holding(ServerPlayer player) {
            return !player.hasDisconnected() && !player.isRemoved()
                    && (player.getMainHandItem().is(ModItems.PIPE_LAYING_TOOL.get()) || player.getOffhandItem().is(ModItems.PIPE_LAYING_TOOL.get()));
        }

        @Override
        public Map<Long, AssemblyView> inRange(ServerPlayer player) {
            return AssemblyService.watched(player.serverLevel(), player.getX(), player.getY(), player.getZ(), AssemblyWatch.RANGE);
        }

        @Override
        public void send(ServerPlayer player, long master, @Nullable AssemblyView view) {
            PacketDistributor.sendToPlayer(player, new WatchedPayload(master, Optional.ofNullable(view)));
        }
    };

    private static AssemblyWatch<ServerPlayer> watches(MinecraftServer server) {
        return WATCHES.computeIfAbsent(server, s -> new AssemblyWatch<>(WATCH));
    }

    /** The laying tool taken up or put away. A deployer's fake player watches nothing. */
    private static void watch(WatchPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        if (!payload.on()) {
            var watches = WATCHES.get(player.server);
            if (watches != null) watches.stop(player);
            return;
        }
        if (!WATCH.holding(player)) return;
        FluidWorldAuthority.find(player.server).ifPresent(engine -> watches(player.server).start(player, engine.onlineTick()));
    }

    private static AssemblyPresentation<AssemblyMenu> presentation(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, s -> new AssemblyPresentation<>(HOST));
    }

    /**
     * A click on {@code cell} of {@code vessel}: opens a column's GUI, or the GUI of the column whose report holds the
     * unit, with the unit selected. False when the structure is neither.
     */
    public static boolean open(ServerPlayer player, Vessel vessel, BlockPos cell) {
        var target = target(player.serverLevel(), vessel);
        if (target == null) return false;
        // A deployer's fake player opens nothing.
        if (!(player instanceof FakePlayer)) open(player, target.origin(), target.column(), cell, vessel.shape(), target.selected());
        return true;
    }

    /** The column GUI a click opens: the column's origin, a shell cell of it, and the member selected ({@link AssemblyReport#NONE} for none). */
    public record Target(BlockPos origin, BlockPos column, long selected) {
    }

    /** The column GUI a click on {@code vessel} opens: its own if it is a column, else its holder's; null for neither. */
    public static @Nullable Target target(net.minecraft.server.level.ServerLevel level, Vessel vessel) {
        if (vessel.shape().kind() == EquipmentShape.Kind.COLUMN) {
            // The session finds its column through the controller, a shell cell; an interior cell would need a walk.
            var controller = ShapeRasterizer.layout(vessel.shape()).controller();
            return new Target(vessel.origin(), vessel.origin().offset(controller.x(), controller.y(), controller.z()), AssemblyReport.NONE);
        }
        var master = AssemblyService.holder(level, vessel.origin());
        var column = master == null ? null : AssemblyService.cellOf(level, master, master);
        return column == null ? null : new Target(BlockPos.of(master), column, vessel.origin().asLong());
    }

    private static void open(ServerPlayer player, BlockPos origin, BlockPos column, BlockPos anchor, EquipmentShape clicked, long selected) {
        double reach = AssemblyMenu.REACH + Math.max(clicked.sizeX(), Math.max(clicked.sizeY(), clicked.sizeZ()));
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AssemblyMenu(id, inventory, origin, column, anchor, reach, selected),
                Component.translatable("gui.icheme.assembly.title")), buffer -> {
            buffer.writeBlockPos(origin);
            buffer.writeBlockPos(column);
            buffer.writeBlockPos(anchor);
            buffer.writeDouble(reach);
            buffer.writeLong(selected);
        });
        // Subscribe after the opening packet, so the replay follows it.
        var engine = FluidWorldAuthority.find(player.server);
        if (player.containerMenu instanceof AssemblyMenu menu && menu.origin().equals(origin) && engine.isPresent())
            presentation(player.server).open(menu, engine.get().onlineTick());
    }

    /** A column GUI closed: its session ends. */
    public static void closed(AssemblyMenu menu) {
        var player = menu.serverPlayer();
        var presentation = player == null ? null : SERVERS.get(player.server);
        if (presentation != null) presentation.close(menu);
    }

    /**
     * Server: opens the datasheet of the structure holding {@code cell} on the player's client. An open menu (the column's
     * GUI) closes first, so its session ends with it.
     */
    public static void datasheet(ServerPlayer player, BlockPos cell) {
        if (player instanceof FakePlayer) return;
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
        PacketDistributor.sendToPlayer(player, new DatasheetPayload(cell));
    }

    private static @Nullable AssemblyMenu menu(IPayloadContext context, int container) {
        return context.player() instanceof ServerPlayer player && player.containerMenu instanceof AssemblyMenu menu && menu.containerId == container
                && menu.stillValid(player) ? menu : null;
    }

    private static void validate(ValidatePayload payload, IPayloadContext context) {
        var menu = menu(context, payload.container());
        var engine = menu == null ? Optional.<FluidWorldAuthority>empty() : FluidWorldAuthority.find(menu.serverPlayer().server);
        if (engine.isPresent()) presentation(menu.serverPlayer().server).validate(menu, engine.get().onlineTick());
    }

    /** Opens a member's own screen, which replaces the column's GUI. */
    private static void openUnit(OpenUnitPayload payload, IPayloadContext context) {
        var menu = menu(context, payload.container());
        if (menu == null) return;
        var player = Objects.requireNonNull(menu.serverPlayer());
        var level = player.serverLevel();
        long master = menu.origin().asLong();
        BlockPos cell;
        if (payload.member() == master) cell = menu.cell();
        else {
            var entry = AssemblyService.entry(level, menu.origin());
            cell = entry == null || !entry.holds(payload.member()) ? null : AssemblyService.cellOf(level, master, payload.member());
        }
        var vessel = cell == null ? null : EquipmentStructure.find(level, cell);
        if (vessel != null) EquipmentShellBlock.openOwn(player, vessel, cell, false);
    }

    /** The engine's tick: the column GUIs' and the laying tool's watches' buckets, when one is due. */
    public static void presentationTick(MinecraftServer server, long onlineTick) {
        var presentation = SERVERS.get(server);
        if (presentation != null) presentation.tick(onlineTick);
        var watches = WATCHES.get(server);
        if (watches != null) watches.tick(onlineTick);
    }

    public static void forget(MinecraftServer server) {
        SERVERS.remove(server);
        WATCHES.remove(server);
    }
}
