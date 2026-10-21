package com.wormzjl.icheme.world.level.block;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.registry.ModBlocks;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.icheme.runtime.fluid.FluidDeviceSpec;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.science.equipment.EquipmentRefresh;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.Paint;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.ProcessUnit;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.entity.EquipmentShellBlockEntity;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whole parametric vessels in the world: placement, removal of every cell when one goes (owner decision D5, batch
 * 2026-10-15-custom-equipment-rendering), lookup of the vessel a cell belongs to, and the collision shape of each cell
 * key. The equipment constructor places vessels; {@code /icheme equipment place} does too, for debugging. A vessel
 * whose cells an older layout of its shape placed is brought to the current layout after it loads ({@link #refreshLater},
 * {@link EquipmentRefresh}).
 */
@EventBusSubscriber(modid = IChemE.MOD_ID)
public final class EquipmentStructure {
    /** A vessel in the world: its shape and its origin cell (lower north-west corner of its box). */
    public record Vessel(BlockPos origin, EquipmentShape shape) {
        /** The controller cell: the role of one shell cell ({@link ShapeRasterizer.Layout#controller()}), not a block of its own. */
        public BlockPos controller() {
            var cell = ShapeRasterizer.layout(shape).controller();
            return origin.offset(cell.x(), cell.y(), cell.z());
        }
    }

    private static final int SHAPE_CACHE = 4096;
    private static final Map<ShapeRasterizer.CellKey, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    /** Set while a vessel is being removed, so the cells it removes do not start removals of their own (server thread only). */
    private static boolean dismantling;
    /**
     * Vessels an older layout placed, by level and origin, each with the region its old and new cells span (server
     * thread only; weak, so a level that goes drops its own).
     */
    private static final Map<ServerLevel, Map<BlockPos, Stale>> STALE = new WeakHashMap<>();

    private record Stale(EquipmentShape shape, BlockPos low, BlockPos high) {
        Stale with(Stale other) {
            return new Stale(shape, min(low, other.low), max(high, other.high));
        }
    }

    private EquipmentStructure() {
    }

    /** Collision and outline shape of a shell cell: its solid at 1/8 block. Both sides compute the same shape. */
    public static VoxelShape collision(ShapeRasterizer.CellKey key) {
        var shape = SHAPES.get(key);
        if (shape != null) return shape;
        if (SHAPES.size() >= SHAPE_CACHE) SHAPES.clear();
        return SHAPES.computeIfAbsent(key, k -> {
            double s = ShapeRasterizer.SOLID_RESOLUTION;
            VoxelShape union = Shapes.empty();
            for (var box : ShapeRasterizer.solid(k))
                union = Shapes.joinUnoptimized(union, Shapes.box(box.x0() / s, box.y0() / s, box.z0() / s, box.x1() / s, box.y1() / s, box.z1() / s), BooleanOp.OR);
            return union.optimize();
        });
    }

    /**
     * Places a vessel whose box starts at {@code origin}, painted {@code paint}. Only cells below {@code rows} are
     * placed, which builds a vessel part way. Every cell of the whole vessel must be loaded, inside the build height and
     * replaceable.
     *
     * @return the number of cells placed
     * @throws IllegalStateException naming the first cell that blocks the vessel
     */
    public static int place(ServerLevel level, BlockPos origin, EquipmentShape shape, Paint paint, int rows) {
        var layout = ShapeRasterizer.layout(shape);
        for (var list : java.util.List.of(layout.shell(), layout.interior()))
            for (var cell : list) {
                var position = origin.offset(cell.x(), cell.y(), cell.z());
                if (level.isOutsideBuildHeight(position) || !level.isLoaded(position))
                    throw new IllegalStateException("Cell " + position.toShortString() + " is outside the loaded world");
                if (!level.getBlockState(position).canBeReplaced())
                    throw new IllegalStateException("Cell " + position.toShortString() + " is occupied by " + level.getBlockState(position).getBlock().getName().getString());
            }
        int placed = 0, flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (var cell : layout.shell()) {
            if (cell.y() >= rows) continue;
            var position = origin.offset(cell.x(), cell.y(), cell.z());
            level.setBlock(position, ModBlocks.EQUIPMENT_SHELL.get().defaultBlockState(), flags);
            if (level.getBlockEntity(position) instanceof EquipmentShellBlockEntity entity) entity.bind(shape, cell, paint);
            placed++;
        }
        for (var cell : layout.interior()) {
            if (cell.y() >= rows) continue;
            level.setBlock(origin.offset(cell.x(), cell.y(), cell.z()), ModBlocks.EQUIPMENT_INTERIOR.get().defaultBlockState(), flags);
            placed++;
        }
        var vessel = new Vessel(origin, shape);
        if (shape.drum() && rows >= shape.sizeY()) register(level, vessel);
        reconnect(level, vessel);
        refreshNozzles(level, vessel);
        return placed;
    }

    /**
     * A whole drum joins the fluid network as a reservoir (owner request 2026-10-16): its node at its lowest inside cell,
     * each nozzle a port of the class its position fixes (top: vapour; lowest side: liquid; other sides: bulk), its
     * design volume, and its liquid height for the static head {@code g·m·H/V}. It is placed empty, as a tank is.
     */
    private static void register(ServerLevel level, Vessel vessel) {
        var node = node(vessel);
        FluidWorldAuthority.find(level.getServer()).ifPresent(world -> {
            var record = world.placeVessel(position(level, node), ports(level, vessel), vessel.shape().volume(), vessel.shape().levelHeight());
            if (level.getBlockEntity(node) instanceof EquipmentShellBlockEntity entity) entity.bindIdentity(record.device().id());
        });
    }

    /** The drum's node: the shell cell at its lowest inside point ({@link ShapeRasterizer#fluidCell}). */
    private static BlockPos node(Vessel vessel) {
        var cell = ShapeRasterizer.fluidCell(vessel.shape());
        return vessel.origin().offset(cell.x(), cell.y(), cell.z());
    }

    private static PhysicalFluidTopology.Position position(Level level, BlockPos at) {
        return new PhysicalFluidTopology.Position(level.dimension().location().toString(), at.getX(), at.getY(), at.getZ());
    }

    /** The drum's ports, one per nozzle, of the class its position fixes. */
    private static List<PhysicalFluidTopology.Port> ports(Level level, Vessel vessel) {
        var ports = new ArrayList<PhysicalFluidTopology.Port>();
        for (var port : ShapeRasterizer.ports(vessel.shape())) {
            var cell = vessel.origin().offset(port.cell().x(), port.cell().y(), port.cell().z());
            var face = PhysicalFluidTopology.Direction.valueOf(Direction.fromDelta(port.dx(), port.dy(), port.dz()).name());
            var phase = switch (port.port()) {
                case VAPOUR -> PassiveNetwork.PhasePort.VAPOR;
                case LIQUID -> PassiveNetwork.PhasePort.LIQUID;
                case BULK -> PassiveNetwork.PhasePort.BULK;
            };
            ports.add(new PhysicalFluidTopology.Port(position(level, cell), face, phase));
        }
        return ports;
    }

    /** The drum's registration leaves the fluid network with its node cell (its contents go with it, as a broken tank's do). */
    private static void unregister(ServerLevel level, Vessel vessel) {
        var position = position(level, node(vessel));
        FluidWorldAuthority.find(level.getServer()).ifPresent(world -> world.at(position)
                .filter(record -> record.device().position().equals(position) && !record.device().ports().isEmpty())
                .ifPresent(record -> world.remove(record.device().id())));
    }

    /** The pipes in front of the vessel's nozzles take up (or drop) their connection: the cells were set without shape updates. */
    private static void reconnect(Level level, Vessel vessel) {
        for (var port : ShapeRasterizer.ports(vessel.shape())) {
            var at = vessel.origin().offset(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz());
            if (!level.isLoaded(at)) continue;
            var state = level.getBlockState(at);
            if (state.getBlock() instanceof FluidDeviceBlock) Block.updateOrDestroy(state, Block.updateFromNeighbourShapes(state, level, at), level, at, Block.UPDATE_ALL);
        }
    }

    /**
     * Server: each nozzle drawn at the size of the pipe connected to it (owner decisions 2026-10-16: its flange always the
     * pipe's, a reducer where the vessel cannot carry the pipe's size); an unconnected nozzle shows its default size under
     * a blind flange. Only the cells the nozzles reach change, each keeping the sizes of the nozzles reaching it.
     */
    public static void refreshNozzles(Level level, Vessel vessel) {
        if (level.isClientSide()) return;
        var shape = vessel.shape();
        var nozzles = shape.nozzles();
        var outside = new double[nozzles.size()];
        for (var port : ShapeRasterizer.ports(shape)) {
            var at = vessel.origin().offset(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz());
            var towards = Direction.fromDelta(-port.dx(), -port.dy(), -port.dz());
            if (towards == null || !level.isLoaded(at)) continue;
            var state = level.getBlockState(at);
            if (state.getBlock() instanceof FluidDeviceBlock block && block.kind() == TopologyCompiler.Kind.PIPE && state.getValue(FluidDeviceBlock.connection(towards))
                    && level.getBlockEntity(at) instanceof FluidDeviceBlockEntity pipe)
                outside[port.nozzle()] = PipeSizes.outside(pipe.bore());
        }
        var cells = new java.util.HashSet<ShapeRasterizer.Cell>();
        for (int k = 0; k < nozzles.size(); k++) cells.addAll(ShapeRasterizer.nozzleCells(shape, k));
        for (var cell : cells) {
            var position = vessel.origin().offset(cell.x(), cell.y(), cell.z());
            if (level.isLoaded(position) && level.getBlockEntity(position) instanceof EquipmentShellBlockEntity entity && vessel.origin().equals(entity.origin()))
                entity.nozzles(ShapeRasterizer.cellCodes(shape, cell, outside));
        }
    }

    /** Server: when a pipe beside {@code position} changed, redraw the nozzles of the vessel whose port cell it is. */
    public static void refreshNozzlesAt(Level level, BlockPos position) {
        if (level.isClientSide() || !(level.getBlockEntity(position) instanceof EquipmentShellBlockEntity shell) || shell.shape() == null) return;
        var cell = shell.cell();
        for (var port : ShapeRasterizer.ports(shell.shape()))
            if (port.cell().equals(cell)) {
                refreshNozzles(level, new Vessel(shell.origin(), shell.shape()));
                return;
            }
    }

    /**
     * The vessel a shell or interior cell belongs to. An interior cell has no data of its own: walking east, or south
     * when east runs along a lying vessel's axis, crosses the convex cross-section and reaches a shell cell of the same
     * vessel within a diameter.
     */
    public static @Nullable Vessel find(BlockGetter level, BlockPos position) {
        var shell = shellReached(level, position);
        if (shell == null) return null;
        var vessel = new Vessel(shell.origin(), shell.shape());
        return contains(vessel, position) ? vessel : null;
    }

    /** The first bound shell cell a walk east, or else south, over interior cells reaches from {@code position}, or null. */
    private static @Nullable EquipmentShellBlockEntity shellReached(BlockGetter level, BlockPos position) {
        for (var direction : new Direction[]{Direction.EAST, Direction.SOUTH})
            for (int step = 0; step <= EquipmentShape.MAX_COLUMN_DIAMETER + 2; step++) {
                var at = position.relative(direction, step);
                if (level.getBlockEntity(at) instanceof EquipmentShellBlockEntity shell && shell.shape() != null) return shell;
                if (step > 0 && !level.getBlockState(at).is(ModBlocks.EQUIPMENT_INTERIOR.get())) break;
            }
        return null;
    }

    /** Removes every remaining cell of the vessel; nothing drops. */
    static void dismantle(Level level, Vessel vessel) {
        if (dismantling || level.isClientSide()) return;
        dismantling = true;
        try {
            if (level instanceof ServerLevel server && vessel.shape().drum()) unregister(server, vessel);
            var layout = ShapeRasterizer.layout(vessel.shape());
            for (var list : java.util.List.of(layout.shell(), layout.interior()))
                for (var cell : list) {
                    var position = vessel.origin().offset(cell.x(), cell.y(), cell.z());
                    var state = level.getBlockState(position);
                    boolean ours = state.is(ModBlocks.EQUIPMENT_INTERIOR.get())
                            || level.getBlockEntity(position) instanceof EquipmentShellBlockEntity shell && vessel.origin().equals(shell.origin());
                    if (ours) level.setBlock(position, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            reconnect(level, vessel);
        } finally {
            dismantling = false;
        }
    }

    /**
     * Server, from a shell cell's load: an older layout of its shape placed it (owner request 2026-10-18). Its vessel is
     * refreshed on a later tick, once every chunk its old and new cells span is loaded and block-ticking; blocks are not
     * changed while a chunk loads. {@code box} is the box the cell was placed by (zeros when saved before stamps),
     * {@code cell} its position, so a cell an old, larger box left outside the new one widens the region; the new box may
     * stand {@link EquipmentRefresh#REACH} from the old one along x and z.
     */
    public static void refreshLater(ServerLevel level, BlockPos origin, EquipmentShape shape, int[] box, BlockPos cell) {
        int reach = EquipmentRefresh.REACH;
        var low = origin.offset(-reach, 0, -reach);
        var high = origin.offset(Math.max(shape.sizeX(), box[0]) - 1 + reach, Math.max(shape.sizeY(), box[1]) - 1, Math.max(shape.sizeZ(), box[2]) - 1 + reach);
        var stale = new Stale(shape, min(low, cell), max(high, cell));
        STALE.computeIfAbsent(level, k -> new LinkedHashMap<>()).merge(origin.immutable(), stale, Stale::with);
    }

    /** At most one stale vessel a tick, the first whose region is ready; on most ticks there is none. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var pending = STALE.get(level);
        if (pending == null || pending.isEmpty()) return;
        for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            var stale = entry.getValue();
            if (!ready(level, stale.low(), stale.high())) continue;
            it.remove();
            try {
                refresh(level, entry.getKey(), stale.shape(), stale.low(), stale.high());
            } catch (RuntimeException failed) {
                // A vessel that cannot be refreshed keeps its cells as they stand; the world goes on.
                IChemE.LOGGER.error("equipment_refresh failed at {}", entry.getKey().toShortString(), failed);
            }
            return;
        }
    }

    /**
     * Every chunk of the region is loaded and block-ticking, where a block change reaches the players watching it as any
     * other does (outside that range the server sends a player no block changes).
     */
    private static boolean ready(ServerLevel level, BlockPos low, BlockPos high) {
        for (int x = low.getX() >> 4; x <= high.getX() >> 4; x++)
            for (int z = low.getZ() >> 4; z <= high.getZ() >> 4; z++)
                if (!level.hasChunk(x, z) || !level.shouldTickBlocksAt(ChunkPos.asLong(x, z))) return false;
        return true;
    }

    /**
     * Brings the vessel at {@code origin} to its shape's current layout ({@link EquipmentRefresh}). What of it stands
     * between {@code low} and {@code high} is read first: a shell cell is its own by origin and shape, an interior cell
     * where a walk over interior cells reaches one of its shell cells. The new layout stands where it covers the most of
     * that (its body stays on its foundation); the cells it dropped are cleared, those it added are placed where the world
     * is free, and every shell cell is rebound to it with its paint; the nozzles redraw at the sizes of their pipes, and a
     * drum keeps its registration ({@link #reregister}). Nothing else changes. A chunk the change reaches before it is
     * sent to a player waits for its light ({@link ChunkMap#waitForLightBeforeSending}): the server sends a player light
     * changes only on the edge of its view, a player relighting its own block changes, so a chunk sent with the light
     * of before would keep it.
     */
    static void refresh(ServerLevel level, BlockPos origin, EquipmentShape shape, BlockPos low, BlockPos high) {
        var world = new HashMap<ShapeRasterizer.Cell, EquipmentRefresh.Holding>();
        Paint paint = null;
        BlockPos oldNode = null;
        for (var at : BlockPos.betweenClosed(low, high)) {
            var cell = new ShapeRasterizer.Cell(at.getX() - origin.getX(), at.getY() - origin.getY(), at.getZ() - origin.getZ());
            var state = level.getBlockState(at);
            if (level.getBlockEntity(at) instanceof EquipmentShellBlockEntity shell && ours(shell, origin, shape)) {
                world.put(cell, EquipmentRefresh.Holding.SHELL);
                if (paint == null) paint = shell.paint();
                if (shell.fluidIdentity() != 0) oldNode = at.immutable();
            } else if (state.is(ModBlocks.EQUIPMENT_INTERIOR.get())) {
                var shell = shellReached(level, at);
                world.put(cell, shell != null && ours(shell, origin, shape) ? EquipmentRefresh.Holding.INTERIOR : EquipmentRefresh.Holding.OTHER);
            } else if (!state.canBeReplaced()) world.put(cell, EquipmentRefresh.Holding.OTHER);
        }
        if (paint == null) return;
        var layout = ShapeRasterizer.layout(shape);
        var plan = EquipmentRefresh.plan(layout, world);
        var placed = origin.offset(plan.shift().x(), 0, plan.shift().z());
        var placeShell = new HashSet<>(plan.shell());
        var blocked = new HashSet<>(plan.blocked());
        var touched = new ArrayList<BlockPos>();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        dismantling = true;
        try {
            for (var cell : plan.clear()) {
                var at = placed.offset(cell.x(), cell.y(), cell.z());
                level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), flags);
                touched.add(at);
            }
            for (var cell : layout.shell()) {
                if (blocked.contains(cell)) continue;
                var at = placed.offset(cell.x(), cell.y(), cell.z());
                var cellPaint = paint;
                if (placeShell.contains(cell)) {
                    level.setBlock(at, ModBlocks.EQUIPMENT_SHELL.get().defaultBlockState(), flags);
                    touched.add(at);
                }
                if (level.getBlockEntity(at) instanceof EquipmentShellBlockEntity entity) {
                    var before = entity.key();
                    if (!placeShell.contains(cell)) cellPaint = entity.paint();
                    entity.bind(shape, cell, cellPaint);
                    if (!entity.key().equals(before)) {
                        level.sendBlockUpdated(at, entity.getBlockState(), entity.getBlockState(), Block.UPDATE_CLIENTS);
                        touched.add(at);
                    }
                }
            }
            for (var cell : plan.interior()) level.setBlock(placed.offset(cell.x(), cell.y(), cell.z()), ModBlocks.EQUIPMENT_INTERIOR.get().defaultBlockState(), flags);
        } finally {
            dismantling = false;
        }
        for (int x = low.getX() >> 4; x <= high.getX() >> 4; x++)
            for (int z = low.getZ() >> 4; z <= high.getZ() >> 4; z++) level.getChunkSource().chunkMap.waitForLightBeforeSending(new ChunkPos(x, z), 0);
        var vessel = new Vessel(placed, shape);
        if (shape.drum()) reregister(level, vessel, oldNode);
        // Pipes beside a dropped, added or redrawn cell take up or drop their connection (old nozzles included).
        for (var at : touched)
            for (var direction : Direction.values()) {
                var next = at.relative(direction);
                var state = level.getBlockState(next);
                if (state.getBlock() instanceof FluidDeviceBlock) Block.updateOrDestroy(state, Block.updateFromNeighbourShapes(state, level, next), level, next, Block.UPDATE_ALL);
            }
        reconnect(level, vessel);
        refreshNozzles(level, vessel);
        IChemE.LOGGER.info("equipment_refresh kind={} origin={} shift={},{} cleared={} placed={} blocked={}", shape.kind().name().toLowerCase(Locale.ROOT),
                origin.toShortString(), plan.shift().x(), plan.shift().z(), plan.clear().size(), plan.shell().size() + plan.interior().size(), plan.blocked().size());
        if (!plan.blocked().isEmpty()) {
            var first = plan.blocked().getFirst();
            IChemE.LOGGER.warn("equipment_refresh left {} cells out: other blocks stand there, first at {}", plan.blocked().size(),
                    placed.offset(first.x(), first.y(), first.z()).toShortString());
        }
    }

    private static boolean ours(EquipmentShellBlockEntity shell, BlockPos origin, EquipmentShape shape) {
        return origin.equals(shell.origin()) && shape.equals(shell.shape());
    }

    /**
     * A refreshed drum keeps its registration, so its contents: its ports, volume and level height follow by a queued
     * edit. The ledger never moves a device, so a drum whose node cell the new layout moved registers anew, empty, as a
     * rebuilt drum would; one that had no registration registers now. {@code oldNode} is the cell that held its identity.
     */
    private static void reregister(ServerLevel level, Vessel vessel, @Nullable BlockPos oldNode) {
        var node = node(vessel);
        var world = FluidWorldAuthority.find(level.getServer()).orElse(null);
        if (world == null) return;
        var held = position(level, oldNode == null ? node : oldNode);
        var record = world.at(held).filter(r -> r.device().position().equals(held) && !r.device().ports().isEmpty()).orElse(null);
        if (record == null) {
            register(level, vessel);
            return;
        }
        var device = record.device();
        if (!device.position().equals(position(level, node))) {
            world.remove(device.id());
            if (level.getBlockEntity(oldNode) instanceof EquipmentShellBlockEntity old) old.releaseIdentity();
            register(level, vessel);
            IChemE.LOGGER.info("equipment_refresh drum node moved {} -> {}: registered anew, empty", oldNode.toShortString(), node.toShortString());
            return;
        }
        var spec = record.spec();
        world.edit(device.id(), record.revision(),
                new PhysicalFluidTopology.Device(device.id(), device.position(), device.kind(), device.facing(), device.geometry(), device.control(), ports(level, vessel)),
                new FluidDeviceSpec(vessel.shape().volume(), spec.temperature(), spec.pressure(), spec.composition(), spec.solids(), vessel.shape().levelHeight()));
        if (level.getBlockEntity(node) instanceof EquipmentShellBlockEntity entity && entity.fluidIdentity() != device.id()) entity.bindIdentity(device.id());
    }

    private static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private static boolean contains(Vessel vessel, BlockPos position) {
        int x = position.getX() - vessel.origin().getX(), y = position.getY() - vessel.origin().getY(), z = position.getZ() - vessel.origin().getZ();
        return x >= 0 && x < vessel.shape().sizeX() && y >= 0 && y < vessel.shape().sizeY() && z >= 0 && z < vessel.shape().sizeZ();
    }

    /**
     * {@code /icheme equipment place <bottom centre> <diameter m> <tangent length m> [rows | vertical <skirt m>
     * [front] | horizontal <saddle m> [front [boot]]]} (operators only). Without a support the drum is bare: vertical, on
     * its lower head. The front (north, east, south, west; north by default) is where a standing drum's liquid outlet and a
     * lying drum's front head face; {@code boot} hangs a water boot under a lying drum's far end. {@code /icheme equipment column <bottom centre> <diameter m> <trays> <skirt m>
     * [front]} places a column fed at its middle tray, its piping side to the front. {@code /icheme equipment unit
     * <bottom centre> <kind> <size m> <length> <support m> [front]} places a unit of any unit kind, by its lower-case name.
     * The kind's design gives the meaning and ranges of the size, the second size (a length, a height or a count: an air
     * cooler's size and length are a bay's, a compressor's its throws, 1 where the kind has none) and the support (0 where
     * the unit stands at grade); its channel, suction, feed or process nozzles face the front.
     */
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        var rows = Commands.argument("rows", IntegerArgumentType.integer(1))
                .executes(context -> place(context, IntegerArgumentType.getInteger(context, "rows"), EquipmentShape.Orientation.VERTICAL, 0, EquipmentShape.Heading.NORTH, false));
        var tangentLength = Commands.argument("tangentLength", IntegerArgumentType.integer(1, (int) EquipmentShape.MAX_TANGENT_LENGTH))
                .executes(context -> place(context, Integer.MAX_VALUE, EquipmentShape.Orientation.VERTICAL, 0, EquipmentShape.Heading.NORTH, false)).then(rows);
        for (var orientation : EquipmentShape.Orientation.values()) {
            var support = Commands.argument("support", IntegerArgumentType.integer(0, EquipmentShape.MAX_SUPPORT))
                    .executes(context -> place(context, Integer.MAX_VALUE, orientation, IntegerArgumentType.getInteger(context, "support"), EquipmentShape.Heading.NORTH, false));
            for (var front : EquipmentShape.Heading.values())
                support.then(Commands.literal(front.name().toLowerCase(Locale.ROOT))
                        .executes(context -> place(context, Integer.MAX_VALUE, orientation, IntegerArgumentType.getInteger(context, "support"), front, false))
                        .then(Commands.literal("boot").executes(context -> place(context, Integer.MAX_VALUE, orientation, IntegerArgumentType.getInteger(context, "support"), front, true))));
            tangentLength.then(Commands.literal(orientation.name().toLowerCase(Locale.ROOT)).then(support));
        }
        var diameter = Commands.argument("diameter", DoubleArgumentType.doubleArg(EquipmentShape.MIN_DIAMETER, EquipmentShape.MAX_DIAMETER)).then(tangentLength);
        var place = Commands.literal("place").then(Commands.argument("position", BlockPosArgument.blockPos()).then(diameter));
        // A column: diameter, tray count (feed at the middle tray) and skirt height; its feed side faces the front.
        var skirt = Commands.argument("skirt", IntegerArgumentType.integer(1, EquipmentShape.MAX_SUPPORT))
                .executes(context -> placeColumn(context, EquipmentShape.Heading.NORTH));
        for (var front : EquipmentShape.Heading.values())
            skirt.then(Commands.literal(front.name().toLowerCase(Locale.ROOT)).executes(context -> placeColumn(context, front)));
        var column = Commands.literal("column").then(Commands.argument("position", BlockPosArgument.blockPos())
                .then(Commands.argument("diameter", DoubleArgumentType.doubleArg(EquipmentShape.MIN_DIAMETER, EquipmentShape.MAX_DIAMETER))
                        .then(Commands.argument("trays", IntegerArgumentType.integer(1, ColumnTrays.MAX_TRAYS)).then(skirt))));
        // A unit: its kind, size (shell, bundle, suction, discharge, bore or radiant section; an air cooler's bay width),
        // length (tubes, throws, radiant height or length; an air cooler's bay length, an ejector's and a pump's 1) and
        // support (shell bottom, bundle, discharge, foundation or floor height; a pump's 0); its channel, suction or process
        // nozzles face the front; an air cooler's bays in parallel and in series after the front. The shape checks each
        // kind's ranges.
        var at = Commands.argument("position", BlockPosArgument.blockPos());
        for (var kind : EquipmentShape.Kind.values()) {
            if (!kind.unit()) continue;
            var support = Commands.argument("support", IntegerArgumentType.integer(0, EquipmentShape.MAX_SUPPORT))
                    .executes(context -> placeUnit(context, kind, EquipmentShape.Heading.NORTH, false));
            for (var front : EquipmentShape.Heading.values())
                support.then(Commands.literal(front.name().toLowerCase(Locale.ROOT)).executes(context -> placeUnit(context, kind, front, false))
                        .then(Commands.argument("parallel", IntegerArgumentType.integer(1, ProcessUnit.Bank.MAX_PARALLEL))
                                .then(Commands.argument("series", IntegerArgumentType.integer(1, ProcessUnit.Bank.MAX_SERIES)).executes(context -> placeUnit(context, kind, front, true)))));
            at.then(Commands.literal(kind.name().toLowerCase(Locale.ROOT)).then(Commands.argument("size", DoubleArgumentType.doubleArg(ProcessUnit.SUCTION_STEP, ProcessUnit.maxDiameter(kind)))
                    .then(Commands.argument("length", IntegerArgumentType.integer(1, (int) EquipmentShape.MAX_TANGENT_LENGTH)).then(support))));
        }
        var unit = Commands.literal("unit").then(at);
        event.getDispatcher().register(Commands.literal("icheme").requires(source -> source.hasPermission(2))
                .then(Commands.literal("equipment").then(place).then(column).then(unit)));
    }

    private static int place(CommandContext<CommandSourceStack> context, int rows, EquipmentShape.Orientation orientation, int support, EquipmentShape.Heading front, boolean boot) throws CommandSyntaxException {
        EquipmentShape shape;
        try {
            double diameter = DoubleArgumentType.getDouble(context, "diameter");
            int length = IntegerArgumentType.getInteger(context, "tangentLength");
            shape = EquipmentShape.drum(diameter, length, orientation, front, support, EquipmentShape.defaultConnections(diameter, length, orientation, support, boot), boot);
        } catch (IllegalArgumentException invalid) {
            context.getSource().sendFailure(Component.literal(invalid.getMessage()));
            return 0;
        }
        return place(context, shape, rows, shape.diameter() + " m x " + (int) shape.tangentLength() + " m drum");
    }

    private static int placeColumn(CommandContext<CommandSourceStack> context, EquipmentShape.Heading front) throws CommandSyntaxException {
        int trays = IntegerArgumentType.getInteger(context, "trays");
        EquipmentShape shape;
        try {
            shape = EquipmentShape.column(DoubleArgumentType.getDouble(context, "diameter"), ColumnTrays.simple(trays, (trays + 1) / 2), front,
                    IntegerArgumentType.getInteger(context, "skirt"));
        } catch (IllegalArgumentException invalid) {
            context.getSource().sendFailure(Component.literal(invalid.getMessage()));
            return 0;
        }
        return place(context, shape, Integer.MAX_VALUE, shape.diameter() + " m column of " + trays + " trays");
    }

    private static int placeUnit(CommandContext<CommandSourceStack> context, EquipmentShape.Kind kind, EquipmentShape.Heading front, boolean banked) throws CommandSyntaxException {
        EquipmentShape shape;
        try {
            shape = EquipmentShape.unit(kind, DoubleArgumentType.getDouble(context, "size"), IntegerArgumentType.getInteger(context, "length"), front,
                    IntegerArgumentType.getInteger(context, "support"), banked ? new ProcessUnit.Bank(IntegerArgumentType.getInteger(context, "parallel"),
                            IntegerArgumentType.getInteger(context, "series")) : ProcessUnit.Bank.ONE);
        } catch (IllegalArgumentException invalid) {
            context.getSource().sendFailure(Component.literal(invalid.getMessage()));
            return 0;
        }
        return place(context, shape, Integer.MAX_VALUE, kind.name().toLowerCase(Locale.ROOT) + " of " + shape.diameter() + " m");
    }

    private static int place(CommandContext<CommandSourceStack> context, EquipmentShape shape, int rows, String name) throws CommandSyntaxException {
        var source = context.getSource();
        var origin = BlockPosArgument.getLoadedBlockPos(context, "position").offset(-shape.sizeX() / 2, 0, -shape.sizeZ() / 2);
        try {
            int placed = place(source.getLevel(), origin, shape, IChemE.defaultPaint(), rows);
            source.sendSuccess(() -> Component.literal("Placed " + placed + " cells of a " + name + " at " + origin.toShortString()), true);
            return placed;
        } catch (IllegalStateException blocked) {
            source.sendFailure(Component.literal(blocked.getMessage()));
            return 0;
        }
    }
}
