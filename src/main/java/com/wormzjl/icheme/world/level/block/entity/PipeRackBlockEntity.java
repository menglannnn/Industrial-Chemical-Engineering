package com.wormzjl.icheme.world.level.block.entity;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.registry.ModBlockEntities;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.equipment.PipeRoute;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.RackGeometry;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.PipeRackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pipe rack cell's lanes (batch 2026-10-26-pipe-laying-system, plan 5.4). Each lane is a laid pipe registered in the
 * fluid ledger at this block's position, lane number slot + 1. The block entity keeps a copy of each lane's facts, its
 * identity, slot range, joints, facing and bore: drawing, the shape and picking read it, and it registers a lane again
 * when the ledger has none (a facts reset), as a laid pipe's block entity keeps its joints. Which joint faces meet a line
 * next door ({@code joined}) is drawn state: computed on the server from loaded neighbours, sent in the update tag, never
 * saved.
 *
 * <p>Saved format {@value #FORMAT} (AGENTS.md, saves): a list {@code Lanes} of compounds, read by name and validated; a
 * damaged lane is dropped from the copy, and the ledger's registration, the fact, puts it back at the next load.</p>
 */
public final class PipeRackBlockEntity extends BlockEntity {
    public static final String FORMAT = "rack-1";

    /** A lane's saved copy: its identity in the ledger, its first slot and width, joints, facing (a face index) and bore. */
    public record Lane(long identity, int slot, int width, int joints, int facing, double bore) {
        public Lane {
            if (slot < 0 || width < 1 || slot + width > RackGeometry.SLOTS || (joints & ~PhysicalFluidTopology.ALL_FACES) != 0 || Integer.bitCount(joints) > 2
                    || facing < 0 || facing > 5 || !(bore > 0 && bore <= PipeSizes.MAX_BORE))
                throw new IllegalArgumentException("Invalid rack lane " + slot + "+" + width);
        }

        public int mask() {
            return ((1 << width) - 1) << slot;
        }

        int outside() {
            return PipeSizes.code(PipeSizes.outside(bore));
        }
    }

    /** What the model draws: each lane with the faces it meets a line through, and its facing. */
    public record Drawn(List<RackGeometry.Lane> lanes, List<Integer> facings) {
    }

    public static final ModelProperty<Drawn> DRAWN = new ModelProperty<>();
    private static final ConcurrentHashMap<Integer, VoxelShape> FRAMES = new ConcurrentHashMap<>();
    /** Client thread only; weak, so a level dropped without unloading its racks frees them. */
    private static final java.util.Set<PipeRackBlockEntity> CLIENT_RACKS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private List<Lane> lanes = List.of();
    private int[] joined = new int[0];
    private @Nullable VoxelShape shape;

    public PipeRackBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.PIPE_RACK.get(), position, state);
    }

    public List<Lane> lanes() {
        return lanes;
    }

    /** The rack's axis: 0 x, 1 y, 2 z. */
    public int axis() {
        return getBlockState().getValue(PipeRackBlock.AXIS).ordinal();
    }

    /** The slots the lanes use, a bit per slot. */
    public int used() {
        int used = 0;
        for (var lane : lanes) used |= lane.mask();
        return used;
    }

    /** The faces the lanes have joints on, a bit per face. */
    public int faces() {
        int faces = 0;
        for (var lane : lanes) faces |= lane.joints;
        return faces;
    }

    /** The route search's view of this cell: its lanes with the faces their arms run to. */
    public PipeRoute.Room room() {
        var out = new ArrayList<PipeRoute.RackLane>(lanes.size());
        for (var lane : lanes) out.add(new PipeRoute.RackLane(lane.slot, lane.width, lane.joints, RackGeometry.arms(lane.joints, lane.facing)));
        return PipeRoute.Room.rack(axis(), out);
    }

    public List<RackGeometry.Lane> drawnLanes() {
        var out = new ArrayList<RackGeometry.Lane>();
        for (int i = 0; i < lanes.size(); i++) {
            var lane = lanes.get(i);
            out.add(new RackGeometry.Lane(lane.slot, lane.width, lane.joints, joined[i] & lane.joints, lane.outside()));
        }
        return out;
    }

    public List<Integer> facings() {
        return lanes.stream().map(Lane::facing).toList();
    }

    /** The lane a click at cell-local point {@code (x, y, z)} hits, and its arm; {@link RackGeometry#NONE} for the frame. */
    public RackGeometry.Pick pick(double x, double y, double z) {
        return RackGeometry.pick(drawnLanes(), facings(), x, y, z);
    }

    /** The frame and the lanes' arms: collision, outline and picking all use it. */
    public VoxelShape shape() {
        var cached = shape;
        if (cached != null) return cached;
        var out = frame(axis());
        var drawn = drawnLanes();
        for (int i = 0; i < drawn.size(); i++)
            for (var box : RackGeometry.boxes(drawn.get(i), lanes.get(i).facing)) out = Shapes.or(out, Shapes.box(box[0], box[1], box[2], box[3], box[4], box[5]));
        return shape = out.optimize();
    }

    public static VoxelShape frame(int axis) {
        return FRAMES.computeIfAbsent(axis, a -> {
            var out = Shapes.empty();
            for (var box : RackGeometry.members(a)) out = Shapes.or(out, Shapes.box(box[0], box[1], box[2], box[3], box[4], box[5]));
            return out.optimize();
        });
    }

    /** The ledger position of the lane on {@code slot}. */
    public PhysicalFluidTopology.Position position(int slot) {
        return new PhysicalFluidTopology.Position(level.dimension().location().toString(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), slot + 1);
    }

    // ---- server ----

    /** Server: a lane just laid here, bound to its registration; drawn by {@link #refresh} once its line is placed. */
    public void addLane(Lane lane) {
        var next = new ArrayList<>(lanes);
        if ((used() & lane.mask()) != 0) throw new IllegalStateException("Rack slots already used");
        next.add(lane);
        setLanes(next, null);
    }

    /** Server: removes a lane from the copy (its registration is removed by the caller). */
    public void removeLane(long identity) {
        var next = new ArrayList<>(lanes);
        next.removeIf(lane -> lane.identity == identity);
        setLanes(next, null);
        sync();
    }

    private void setLanes(List<Lane> next, int @Nullable [] nextJoined) {
        var previous = lanes;
        var previousJoined = joined;
        next.sort(Comparator.comparingInt(Lane::slot));
        lanes = List.copyOf(next);
        joined = new int[lanes.size()];
        // A lane keeps its drawn joins while it stays; a new one starts open until refreshed.
        for (int i = 0; i < lanes.size(); i++)
            if (nextJoined != null) joined[i] = nextJoined[i];
            else for (int j = 0; j < previous.size(); j++) if (previous.get(j).identity == lanes.get(i).identity) joined[i] = previousJoined[j];
        shape = null;
        setChanged();
    }

    /**
     * Server: matches the copy to the ledger, which holds the facts. A lane with a registration takes its identity; a lane
     * without one registers again from the copy; a registration with no copy is adopted. Then every lane is drawn.
     */
    public void bind(FluidWorldAuthority world) {
        if (!(level instanceof ServerLevel)) return;
        var next = new ArrayList<Lane>();
        int used = 0;
        for (var lane : lanes) {
            var record = world.at(position(lane.slot)).orElse(null);
            if (record == null) record = world.placeLane(position(lane.slot), PhysicalFluidTopology.Direction.values()[lane.facing], lane.joints, lane.width, lane.bore);
            var copied = copy(record.device());
            if (copied != null && (used & copied.mask()) == 0) {
                next.add(copied);
                used |= copied.mask();
            }
        }
        for (int slot = 0; slot < RackGeometry.SLOTS; slot++) {
            if ((used >> slot & 1) != 0) continue;
            var record = world.at(position(slot)).orElse(null);
            var copied = record == null ? null : copy(record.device());
            if (copied != null && (used & copied.mask()) == 0) {
                next.add(copied);
                used |= copied.mask();
            }
        }
        setLanes(next, null);
        refresh(true);
    }

    /** A registered lane as its copy, or null when the registration is no lane this rack can draw. */
    private static @Nullable Lane copy(PhysicalFluidTopology.Device device) {
        if (device.kind() != TopologyCompiler.Kind.PIPE || device.slots() == 0) return null;
        try {
            return new Lane(device.id(), device.position().lane() - 1, device.slots(), device.joints(), device.facing().ordinal(), device.geometry().diameter());
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    /**
     * Server: each lane takes its registered joints, facing and bore (accepted input, as a laid pipe's joints do), and
     * the faces it meets a line through; with {@code neighbours}, the blocks next door redraw their side too.
     */
    public void refresh(boolean neighbours) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        var world = FluidWorldAuthority.find(serverLevel.getServer()).orElse(null);
        var next = new ArrayList<Lane>();
        for (var lane : lanes) {
            var record = world == null ? null : world.registrations().get(lane.identity);
            var copied = record == null ? null : copy(record.device());
            next.add(copied != null && copied.slot == lane.slot && copied.width == lane.width ? copied : lane);
        }
        boolean changed = !next.equals(lanes);
        if (changed) setLanes(next, null);
        if (refreshJoined() || changed) sync();
        if (neighbours) for (var direction : Direction.values()) {
            var at = worldPosition.relative(direction);
            if (!level.isLoaded(at)) continue;
            var neighbour = level.getBlockEntity(at);
            if (neighbour instanceof FluidDeviceBlockEntity pipe) pipe.refreshGeometry(pipe.bore(), false);
            else if (neighbour instanceof PipeRackBlockEntity rack && rack.refreshJoined()) rack.sync();
        }
    }

    /**
     * Server: which joint faces of each lane meet a line next door: the same lane range with the opposite joint in the
     * next rack, or a pipe connected towards this rack. A face whose neighbour is in an unloaded chunk keeps what it
     * showed (reading it would load the chunk). True when anything changed.
     */
    public boolean refreshJoined() {
        if (level == null || level.isClientSide()) return false;
        var next = new int[lanes.size()];
        for (int i = 0; i < lanes.size(); i++) {
            var lane = lanes.get(i);
            for (var direction : Direction.values()) {
                int face = direction.ordinal();
                if ((lane.joints >> face & 1) == 0) continue;
                var at = worldPosition.relative(direction);
                if (!level.isLoaded(at)) {
                    next[i] |= joined[i] & 1 << face;
                    continue;
                }
                var neighbour = level.getBlockEntity(at);
                boolean meets = false;
                if (neighbour instanceof PipeRackBlockEntity rack) {
                    for (var other : rack.lanes)
                        if (other.slot == lane.slot && other.width == lane.width && (other.joints >> (face ^ 1) & 1) != 0) meets = true;
                } else if (neighbour instanceof FluidDeviceBlockEntity pipe && pipe.kind() == TopologyCompiler.Kind.PIPE)
                    meets = pipe.getBlockState().getValue(FluidDeviceBlock.connection(direction.getOpposite()));
                if (meets) next[i] |= 1 << face;
            }
        }
        if (Arrays.equals(next, joined)) return false;
        joined = next;
        return true;
    }

    /**
     * The faces where lane {@code index} ends open: an arm without a joint, or a joint nothing meets any more (the next
     * lane or pipe was dismantled). A line laid from there extends the lane (D11).
     */
    public int openFaces(int index) {
        var lane = lanes.get(index);
        var drawn = drawnLanes().get(index);
        return RackGeometry.arms(drawn, lane.facing) & ~lane.joints | lane.joints & ~drawn.joined();
    }

    /** Server: the lane meeting {@code direction}'s neighbour through that face, if one has a joint there. */
    public @Nullable Lane laneTowards(Direction direction) {
        for (var lane : lanes) if ((lane.joints >> direction.ordinal() & 1) != 0) return lane;
        return null;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) CLIENT_RACKS.add(this);
        if (level instanceof ServerLevel serverLevel) FluidWorldAuthority.find(serverLevel.getServer()).ifPresent(this::bind);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_RACKS.remove(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_RACKS.remove(this);
    }

    /** Client: the racks in the client's level, whose open lane ends the pipe laying tool marks. */
    public static List<PipeRackBlockEntity> clientRacks() {
        return List.copyOf(CLIENT_RACKS);
    }

    // ---- saved format ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(lanes, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var read = read(tag);
        int[] readJoined = null;
        if (tag.contains("Joined", Tag.TAG_INT_ARRAY) && tag.getIntArray("Joined").length == read.size()) readJoined = tag.getIntArray("Joined");
        setLanes(new ArrayList<>(read), readJoined);
    }

    /** Writes {@code lanes} as format {@value #FORMAT}: the copy a rack saves. */
    public static CompoundTag write(List<Lane> lanes, CompoundTag tag) {
        var list = new ListTag();
        for (var lane : lanes) {
            var entry = new CompoundTag();
            entry.putLong("Identity", lane.identity);
            entry.putInt("Slot", lane.slot);
            entry.putInt("Width", lane.width);
            entry.putInt("Joints", lane.joints);
            entry.putString("Facing", Direction.values()[lane.facing].getSerializedName());
            entry.putDouble("Bore", lane.bore);
            list.add(entry);
        }
        tag.put("Lanes", list);
        return tag;
    }

    /**
     * Reads format {@value #FORMAT}: every lane whose fields are present and valid, in slot order; a lane overlapping one
     * read before it, or damaged, is dropped and logged. Absent: no lanes.
     */
    public static List<Lane> read(CompoundTag tag) {
        var out = new ArrayList<Lane>();
        int used = 0;
        for (var element : tag.getList("Lanes", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var facing = Direction.byName(entry.getString("Facing").toLowerCase(Locale.ROOT));
            Lane lane = null;
            if (entry.contains("Identity", Tag.TAG_LONG) && entry.contains("Slot", Tag.TAG_INT) && entry.contains("Width", Tag.TAG_INT) && entry.contains("Joints", Tag.TAG_INT)
                    && facing != null && entry.contains("Bore", Tag.TAG_DOUBLE))
                try {
                    lane = new Lane(entry.getLong("Identity"), entry.getInt("Slot"), entry.getInt("Width"), entry.getInt("Joints"), facing.ordinal(), entry.getDouble("Bore"));
                } catch (IllegalArgumentException ignored) {
                }
            if (lane == null || (used & lane.mask()) != 0) {
                IChemE.LOGGER.warn("pipe_rack status=DROPPED detail=Damaged or overlapping lane copy {}; the ledger's registration restores it", entry);
                continue;
            }
            used |= lane.mask();
            out.add(lane);
        }
        out.sort(Comparator.comparingInt(Lane::slot));
        return List.copyOf(out);
    }

    // ---- client ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = write(lanes, new CompoundTag());
        tag.putIntArray("Joined", joined.clone());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        requestModelDataUpdate();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.of(DRAWN, new Drawn(drawnLanes(), facings()));
    }
}
