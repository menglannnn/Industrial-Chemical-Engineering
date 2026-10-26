package com.wormzjl.icheme.world.level.block;

import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.equipment.PipeRoute;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.RackGeometry;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.entity.EquipmentShellBlockEntity;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import com.wormzjl.icheme.world.level.block.entity.PipeRackBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a laid line may start or end (batch 2026-10-26-pipe-laying-system, plan sections 4.2 and 5.2), read from the
 * block the player clicked, the face they clicked and where on the block: the line's end cell, the face of that cell
 * joined to what it meets there, and the joints that block's pipe or lane takes on. Client and server resolve it from
 * their own world; the server never takes the client's word for it, only for where the click landed.
 *
 * @param cell     the line's end cell: a plain cell, or a rack cell where the line is a lane
 * @param joint    the face of {@code cell} joined to {@code host}, or null for an open end
 * @param host     the block the line meets, or null
 * @param hostLane the host's lane number in its rack (option + 1), or 0 when the host is the block itself
 * @param joints   a laid host pipe's or lane's joints once the line meets it, or -1 when the host needs no edit
 * @param kind     what the line meets
 * @param heading  the face the player clicked: a line starting here leaves along it; for a rack, along its run
 * @param point    the point the line's lane must meet on {@code joint} in {@code cell} (a lane continued from the next
 *                 rack, {@link RackGeometry#point}), or -1
 * @param fit      the lines the anchor takes
 * @param runs     for a rack: the faces a lane may run along there (its end faces; a corner's end and side), a bit each
 */
public record PipeAnchor(Kind kind, BlockPos cell, @Nullable Direction joint, @Nullable BlockPos host, int hostLane, int joints, Direction heading, int point, Fit fit, int runs) {
    public enum Kind {FREE, PIPE, DEVICE, NOZZLE, RACK, LANE}

    /** Why a click is no anchor; each has a player message. */
    public enum Refusal {FITTING, JUNCTION, DEVICE_FACE, OCCUPIED, OUTSIDE, LANE_CLOSED, FACE_TAKEN, LANE_SIZE, TRANSITION}

    /** An anchor, or why the click gives none. */
    public record Resolved(@Nullable PipeAnchor anchor, @Nullable Refusal refusal) {
    }

    /**
     * The lines an anchor takes (plan 6), from {@code minWidth} to {@code maxWidth} rack positions wide
     * ({@link RackGeometry#width}), at centre height {@code height} where it is not -1: a lane continued into the next
     * rack keeps its width and height (I9), a line out of a lane into a plain cell is no wider than it (it meets the
     * lane's off-centre point), a lane a pipe leads into is at least as wide as the pipe (which meets it there), and a
     * rack takes no line over four positions (RD10).
     */
    public record Fit(int minWidth, int maxWidth, int height) {
        public static final Fit ANY = new Fit(1, Integer.MAX_VALUE, -1), RACK = new Fit(1, RackGeometry.MAX_WIDTH, -1);

        /** Why a line of outside diameter {@code outside} does not fit, or null. */
        public @Nullable Refusal refuses(double outside) {
            double standard = PipeSizes.outsideOf(PipeSizes.code(outside));
            int width = RackGeometry.width(standard);
            if (width < minWidth || width > maxWidth) return Refusal.LANE_SIZE;
            return height >= 0 && RackGeometry.height(standard) != height ? Refusal.LANE_SIZE : null;
        }
    }

    /** A click: the block, its face, and where on the block it landed (block-local, 0 to 1), which picks a rack's lane. */
    public record Click(BlockPos clicked, Direction face, Vec3 local) {
        private static final StreamCodec<ByteBuf, Vec3> LOCAL = StreamCodec.composite(ByteBufCodecs.FLOAT, v -> (float) v.x, ByteBufCodecs.FLOAT, v -> (float) v.y,
                ByteBufCodecs.FLOAT, v -> (float) v.z, (x, y, z) -> new Vec3(x, y, z));
        public static final StreamCodec<ByteBuf, Click> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Click::clicked, Direction.STREAM_CODEC, Click::face, LOCAL, Click::local, Click::new);

        public Click {
            local = new Vec3(clamp(local.x), clamp(local.y), clamp(local.z));
        }

        /** A click on the middle of {@code face}. */
        public Click(BlockPos clicked, Direction face) {
            this(clicked, face, new Vec3(.5 + .5 * face.getStepX(), .5 + .5 * face.getStepY(), .5 + .5 * face.getStepZ()));
        }

        private static double clamp(double value) {
            return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : .5;
        }
    }

    public PipeRoute.End end() {
        return new PipeRoute.End(cell(cell), joint == null ? -1 : joint.ordinal(), point);
    }

    public static PipeRoute.Cell cell(BlockPos position) {
        return new PipeRoute.Cell(position.getX(), position.getY(), position.getZ());
    }

    /** The host's ledger position. */
    public PhysicalFluidTopology.Position hostPosition(String dimension) {
        return new PhysicalFluidTopology.Position(dimension, host.getX(), host.getY(), host.getZ(), hostLane);
    }

    /** Why a line of outside diameter {@code outside} cannot start or end here, or null. */
    public @Nullable Refusal refuses(double outside) {
        return fit.refuses(outside);
    }

    /**
     * The face a line starting here leaves along, towards {@code towards}: the clicked face; or in a rack, along its run
     * (I8): the lane ends open on one of its run faces and runs to the other, the one pointing most towards the target
     * (the first in face order on a tie), so it enters along the face opposite its open end.
     */
    public int heading(PipeRoute.Cell towards) {
        if (kind != Kind.RACK) return heading.ordinal();
        int[] apart = {towards.x() - cell.getX(), towards.y() - cell.getY(), towards.z() - cell.getZ()};
        int open = -1, most = Integer.MIN_VALUE;
        for (int face = 0; face < 6; face++) {
            if ((runs >> face & 1) == 0) continue;
            var other = Direction.values()[Integer.numberOfTrailingZeros(runs & ~(1 << face))];
            int toward = apart[0] * other.getStepX() + apart[1] * other.getStepY() + apart[2] * other.getStepZ();
            if (toward > most) {
                most = toward;
                open = face;
            }
        }
        return open < 0 ? heading.ordinal() : open ^ 1;
    }

    public static Resolved resolve(Level level, BlockPos clicked, Direction face) {
        return resolve(level, new Click(clicked, face));
    }

    /**
     * The anchor a click gives:
     * <ul>
     * <li>a pipe: the next cell beyond that face. A hand-placed pipe (every face) needs no edit, unless an axial fitting
     * closes that face. A laid pipe takes a joint on that face (an open end extends, a pipe becomes a tee or a cross),
     * unless it has a fitting or four joints already. A rack next door takes the line as a lane there, at least as wide
     * as the pipe, which becomes a transition pipe (plan 6): a pipe that meets a lane, or will, keeps two joints at most
     * and no fitting or gauge;</li>
     * <li>another fluid device: the cell in front of a face it connects through;</li>
     * <li>an equipment cell with a nozzle on that face: the cell in front of the nozzle;</li>
     * <li>a lane on a rack: the cell beyond its open end, the clicked one where it has two (D11): an arm without a joint,
     * which takes one, or a joint nothing meets any more. A lane with no open end is refused, as a rack carries no
     * junctions. Into the next rack the line continues the lane at its point, width and height; into a plain cell a line
     * no wider than the lane meets it there;</li>
     * <li>a rack anywhere else, or a free spot inside one: a new lane in that rack, where its room allows;</li>
     * <li>anything else: a free end, in the clicked block if it is replaceable (grass), else beside the clicked face.</li>
     * </ul>
     * A plain end cell must be loaded, inside the world and replaceable. A lane meets a plain cell through a face no
     * other lane of its rack uses (plan 5.2); a nozzle and a device meet plain cells only.
     */
    public static Resolved resolve(Level level, Click click) {
        var clicked = click.clicked;
        var face = click.face;
        var state = level.getBlockState(clicked);
        var next = clicked.relative(face);
        PipeAnchor anchor;
        if (level.getBlockEntity(clicked) instanceof PipeRackBlockEntity rack) {
            var pick = rack.pick(click.local.x, click.local.y, click.local.z);
            if (pick.lane() < 0) anchor = rack(clicked, rack);
            else {
                var lane = rack.lanes().get(pick.lane());
                int open = rack.openFaces(pick.lane());
                if (open == 0) return new Resolved(null, Refusal.LANE_CLOSED);
                var out = Direction.values()[(open >> pick.face() & 1) != 0 ? pick.face() : Integer.numberOfTrailingZeros(open)];
                var beyond = clicked.relative(out);
                boolean continued = level.hasChunkAt(beyond) && level.getBlockEntity(beyond) instanceof PipeRackBlockEntity;
                for (var other : rack.lanes())
                    if (!continued && other != lane && (other.joints() >> out.ordinal() & 1) != 0) return new Resolved(null, Refusal.FACE_TAKEN);
                int edit = (lane.joints() >> out.ordinal() & 1) != 0 ? -1 : lane.joints() | 1 << out.ordinal();
                // On into the next rack the lane keeps its point, width and height; into a plain cell, a line no wider than
                // the lane fits its off-centre point.
                var fit = continued ? new Fit(lane.width(), lane.width(), RackGeometry.height(lane.outside())) : new Fit(1, lane.width(), -1);
                anchor = new PipeAnchor(Kind.LANE, beyond, out.getOpposite(), clicked, lane.option() + 1, edit, out, continued ? rack.point(lane, out.ordinal()) : -1, fit, 0);
            }
        } else if (state.getBlock() instanceof FluidDeviceBlock block) {
            if (!FluidDeviceBlock.face(state, face)) return new Resolved(null, block.kind() == TopologyCompiler.Kind.PIPE ? Refusal.FITTING : Refusal.DEVICE_FACE);
            if (block.kind() == TopologyCompiler.Kind.PIPE) {
                var entity = level.getBlockEntity(clicked) instanceof FluidDeviceBlockEntity e ? e : null;
                int joints = entity != null ? entity.joints() : PhysicalFluidTopology.ALL_FACES;
                int edit = -1;
                if (joints != PhysicalFluidTopology.ALL_FACES && (joints >> face.ordinal() & 1) == 0) {
                    if (FluidDeviceBlock.hasFitting(state)) return new Resolved(null, Refusal.FITTING);
                    if (Integer.bitCount(joints) > 3) return new Resolved(null, Refusal.JUNCTION);
                    edit = joints | 1 << face.ordinal();
                }
                var fit = Fit.ANY;
                boolean intoRack = level.hasChunkAt(next) && level.getBlockEntity(next) instanceof PipeRackBlockEntity;
                if (intoRack) {
                    if (((PipeRackBlockEntity) level.getBlockEntity(next)).laneTowards(face.getOpposite()) != null) return new Resolved(null, Refusal.FACE_TAKEN);
                    // The pipe becomes a transition pipe at the new lane's point: the lane is at least as wide as it.
                    fit = new Fit(RackGeometry.width(PipeSizes.drawn(entity != null ? entity.bore() : .05)), RackGeometry.MAX_WIDTH, -1);
                }
                // A pipe that meets a lane, or will, stays a transition pipe: laid, of two joints at most, and with no
                // fitting or gauge, as its run bends.
                if ((intoRack || meetsLane(level, clicked, joints)) && (!FluidDeviceBlock.transition(state, edit >= 0 ? edit : joints)
                        || entity != null && !entity.instruments().installed().isEmpty())) return new Resolved(null, Refusal.TRANSITION);
                anchor = new PipeAnchor(Kind.PIPE, next, face.getOpposite(), clicked, 0, edit, face, -1, fit, 0);
            } else anchor = new PipeAnchor(Kind.DEVICE, next, face.getOpposite(), clicked, 0, -1, face, -1, Fit.ANY, 0);
        } else if (state.getBlock() instanceof EquipmentShellBlock && level.getBlockEntity(clicked) instanceof EquipmentShellBlockEntity shell && shell.nozzleFacing(face) != null) {
            anchor = new PipeAnchor(Kind.NOZZLE, next, face.getOpposite(), clicked, 0, -1, face, -1, Fit.ANY, 0);
        } else {
            var target = state.canBeReplaced() ? clicked : next;
            // A rack's frame is open: a click through it on the block behind starts a lane in the rack.
            anchor = level.hasChunkAt(target) && level.getBlockEntity(target) instanceof PipeRackBlockEntity rack ? rack(target, rack)
                    : new PipeAnchor(Kind.FREE, target, null, null, 0, -1, face, -1, Fit.ANY, 0);
        }
        if (level.isOutsideBuildHeight(anchor.cell) || !level.getWorldBorder().isWithinBounds(anchor.cell) || !level.hasChunkAt(anchor.cell)) return new Resolved(null, Refusal.OUTSIDE);
        // A rack cell takes the line as a lane: whether it has room for it is the route's to say.
        boolean rackCell = level.getBlockEntity(anchor.cell) instanceof PipeRackBlockEntity;
        if (!rackCell && !free(level, anchor.cell)) return new Resolved(null, Refusal.OCCUPIED);
        if (rackCell && (anchor.kind == Kind.DEVICE || anchor.kind == Kind.NOZZLE)) return new Resolved(null, Refusal.OCCUPIED);
        // A pipe into a rack, or a lane out to the next rack, makes the end cell a lane: no wider than a rack takes.
        if (rackCell && anchor.fit.maxWidth > RackGeometry.MAX_WIDTH)
            anchor = new PipeAnchor(anchor.kind, anchor.cell, anchor.joint, anchor.host, anchor.hostLane, anchor.joints, anchor.heading, anchor.point,
                    new Fit(anchor.fit.minWidth, RackGeometry.MAX_WIDTH, anchor.fit.height), anchor.runs);
        return new Resolved(anchor, null);
    }

    /** A new lane in {@code rack}: it leaves along the faces its lanes run to. */
    private static PipeAnchor rack(BlockPos at, PipeRackBlockEntity rack) {
        var frame = rack.rack();
        int runs = frame.cornered() ? 1 << frame.end() | 1 << frame.side() : 1 << frame.front() | 1 << frame.back();
        return new PipeAnchor(Kind.RACK, at, null, null, 0, -1, Direction.values()[frame.facing()], -1, Fit.RACK, runs);
    }

    /** Whether the laid pipe at {@code pipe} with {@code joints} meets a rack lane through one of them. */
    private static boolean meetsLane(Level level, BlockPos pipe, int joints) {
        if (joints == PhysicalFluidTopology.ALL_FACES) return false;
        for (var direction : Direction.values()) {
            var at = pipe.relative(direction);
            if ((joints >> direction.ordinal() & 1) != 0 && level.hasChunkAt(at) && level.getBlockEntity(at) instanceof PipeRackBlockEntity rack
                    && rack.laneTowards(direction.getOpposite()) != null) return true;
        }
        return false;
    }

    /** A cell a line may take: loaded, inside the world and replaceable (air, plants, fluids). */
    public static boolean free(Level level, BlockPos cell) {
        return level.hasChunkAt(cell) && !level.isOutsideBuildHeight(cell) && level.getWorldBorder().isWithinBounds(cell) && level.getBlockState(cell).canBeReplaced();
    }

    /**
     * The route search's view of {@code level}: free cells, rack cells with their lanes, and nothing else. Its racks are
     * the rack block entities of the loaded chunks over the search box.
     */
    public static PipeRoute.Space space(Level level) {
        var position = new BlockPos.MutableBlockPos();
        return new PipeRoute.Space() {
            @Override
            public PipeRoute.Room room(PipeRoute.Cell cell) {
                position.set(cell.x(), cell.y(), cell.z());
                if (level.hasChunkAt(position) && level.getBlockEntity(position) instanceof PipeRackBlockEntity rack) return rack.room();
                return free(level, position) ? PipeRoute.Room.FREE : PipeRoute.Room.BLOCKED;
            }

            @Override
            public java.util.Collection<PipeRoute.Cell> racks(PipeRoute.Cell low, PipeRoute.Cell high) {
                var out = new java.util.ArrayList<PipeRoute.Cell>();
                for (int cx = low.x() >> 4; cx <= high.x() >> 4; cx++)
                    for (int cz = low.z() >> 4; cz <= high.z() >> 4; cz++) {
                        var chunk = level.getChunkSource().getChunk(cx, cz, false);
                        if (chunk == null) continue;
                        for (var entity : chunk.getBlockEntities().values()) {
                            var at = entity.getBlockPos();
                            if (entity instanceof PipeRackBlockEntity && at.getX() >= low.x() && at.getX() <= high.x() && at.getY() >= low.y() && at.getY() <= high.y()
                                    && at.getZ() >= low.z() && at.getZ() <= high.z()) out.add(cell(at));
                        }
                    }
                return out;
            }
        };
    }
}
