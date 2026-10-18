package com.wormzjl.createcheme.world.level.block;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.registry.ModBlocks;
import com.wormzjl.createcheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.equipment.ColumnTrays;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.Paint;
import com.wormzjl.createcheme.science.equipment.PipeSizes;
import com.wormzjl.createcheme.science.equipment.ProcessUnit;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentShellBlockEntity;
import com.wormzjl.createcheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whole parametric vessels in the world: placement, removal of every cell when one goes (owner decision D5, batch
 * 2026-10-15-custom-equipment-rendering), lookup of the vessel a cell belongs to, and the collision shape of each cell
 * key. The equipment constructor places vessels; {@code /createcheme equipment place} does too, for debugging.
 */
@EventBusSubscriber(modid = CreateChemE.MOD_ID)
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
        var shape = vessel.shape();
        String dimension = level.dimension().location().toString();
        var nodeCell = ShapeRasterizer.fluidCell(shape);
        var node = vessel.origin().offset(nodeCell.x(), nodeCell.y(), nodeCell.z());
        var ports = new java.util.ArrayList<PhysicalFluidTopology.Port>();
        for (var port : ShapeRasterizer.ports(shape)) {
            var cell = vessel.origin().offset(port.cell().x(), port.cell().y(), port.cell().z());
            var face = PhysicalFluidTopology.Direction.valueOf(Direction.fromDelta(port.dx(), port.dy(), port.dz()).name());
            var phase = switch (port.port()) {
                case VAPOUR -> PassiveNetwork.PhasePort.VAPOR;
                case LIQUID -> PassiveNetwork.PhasePort.LIQUID;
                case BULK -> PassiveNetwork.PhasePort.BULK;
            };
            ports.add(new PhysicalFluidTopology.Port(new PhysicalFluidTopology.Position(dimension, cell.getX(), cell.getY(), cell.getZ()), face, phase));
        }
        FluidWorldAuthority.find(level.getServer()).ifPresent(world -> {
            var record = world.placeVessel(new PhysicalFluidTopology.Position(dimension, node.getX(), node.getY(), node.getZ()), ports, shape.volume(), shape.levelHeight());
            if (level.getBlockEntity(node) instanceof EquipmentShellBlockEntity entity) entity.bindIdentity(record.device().id());
        });
    }

    /** The drum's registration leaves the fluid network with its node cell (its contents go with it, as a broken tank's do). */
    private static void unregister(ServerLevel level, Vessel vessel) {
        var nodeCell = ShapeRasterizer.fluidCell(vessel.shape());
        var node = vessel.origin().offset(nodeCell.x(), nodeCell.y(), nodeCell.z());
        var position = new PhysicalFluidTopology.Position(level.dimension().location().toString(), node.getX(), node.getY(), node.getZ());
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
        for (var direction : new Direction[]{Direction.EAST, Direction.SOUTH})
            for (int step = 0; step <= EquipmentShape.MAX_COLUMN_DIAMETER + 2; step++) {
                var at = position.relative(direction, step);
                if (level.getBlockEntity(at) instanceof EquipmentShellBlockEntity shell && shell.shape() != null) {
                    var vessel = new Vessel(shell.origin(), shell.shape());
                    return contains(vessel, position) ? vessel : null;
                }
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

    private static boolean contains(Vessel vessel, BlockPos position) {
        int x = position.getX() - vessel.origin().getX(), y = position.getY() - vessel.origin().getY(), z = position.getZ() - vessel.origin().getZ();
        return x >= 0 && x < vessel.shape().sizeX() && y >= 0 && y < vessel.shape().sizeY() && z >= 0 && z < vessel.shape().sizeZ();
    }

    /**
     * {@code /createcheme equipment place <bottom centre> <diameter m> <tangent length m> [rows | vertical <skirt m>
     * [front] | horizontal <saddle m> [front [boot]]]} (operators only). Without a support the drum is bare: vertical, on
     * its lower head. The front (north, east, south, west; north by default) is where a standing drum's liquid outlet and a
     * lying drum's front head face; {@code boot} hangs a water boot under a lying drum's far end. {@code /createcheme equipment column <bottom centre> <diameter m> <trays> <skirt m>
     * [front]} places a column fed at its middle tray, its piping side to the front. {@code /createcheme equipment unit
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
        event.getDispatcher().register(Commands.literal("createcheme").requires(source -> source.hasPermission(2))
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
            int placed = place(source.getLevel(), origin, shape, CreateChemE.defaultPaint(), rows);
            source.sendSuccess(() -> Component.literal("Placed " + placed + " cells of a " + name + " at " + origin.toShortString()), true);
            return placed;
        } catch (IllegalStateException blocked) {
            source.sendFailure(Component.literal(blocked.getMessage()));
            return 0;
        }
    }
}
