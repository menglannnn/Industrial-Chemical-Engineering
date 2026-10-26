package com.wormzjl.icheme.world.level.block;

import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.equipment.PipeRoute;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.entity.EquipmentShellBlockEntity;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Where a laid line may start or end (batch 2026-10-26-pipe-laying-system, plan section 4.2), read from the block the
 * player clicked and the face they clicked: the line's end cell, the face of that cell joined to what it meets there, and
 * the joints that block's pipe takes on. Client and server resolve it from their own world; the server never takes the
 * client's word for it.
 *
 * @param cell     the line's end cell
 * @param joint    the face of {@code cell} joined to {@code host}, or null for an open end
 * @param host     the block the line meets, or null
 * @param joints   a laid host pipe's joints once the line meets it, or -1 when the host needs no edit
 * @param kind     what the line meets
 * @param heading  the face the player clicked: a line starting here leaves along it
 */
public record PipeAnchor(Kind kind, BlockPos cell, @Nullable Direction joint, @Nullable BlockPos host, int joints, Direction heading) {
    public enum Kind {FREE, PIPE, DEVICE, NOZZLE}

    /** Why a click is no anchor; each has a player message. */
    public enum Refusal {FITTING, JUNCTION, DEVICE_FACE, OCCUPIED, OUTSIDE}

    /** An anchor, or why the click gives none. */
    public record Resolved(@Nullable PipeAnchor anchor, @Nullable Refusal refusal) {
    }

    public PipeRoute.End end() {
        return new PipeRoute.End(cell(cell), joint == null ? -1 : joint.ordinal());
    }

    public static PipeRoute.Cell cell(BlockPos position) {
        return new PipeRoute.Cell(position.getX(), position.getY(), position.getZ());
    }

    /**
     * The anchor a click on {@code face} of {@code clicked} gives:
     * <ul>
     * <li>a pipe: the next cell beyond that face. A hand-placed pipe (every face) needs no edit, unless an axial fitting
     * closes that face. A laid pipe takes a joint on that face (an open end extends, a pipe becomes a tee or a cross),
     * unless it has a fitting or four joints already;</li>
     * <li>another fluid device: the cell in front of a face it connects through;</li>
     * <li>an equipment cell with a nozzle on that face: the cell in front of the nozzle;</li>
     * <li>anything else: a free end, in the clicked block if it is replaceable (grass), else beside the clicked face.</li>
     * </ul>
     * The end cell must be loaded, inside the world and replaceable.
     */
    public static Resolved resolve(Level level, BlockPos clicked, Direction face) {
        var state = level.getBlockState(clicked);
        var next = clicked.relative(face);
        PipeAnchor anchor;
        if (state.getBlock() instanceof FluidDeviceBlock block) {
            if (!FluidDeviceBlock.face(state, face)) return new Resolved(null, block.kind() == TopologyCompiler.Kind.PIPE ? Refusal.FITTING : Refusal.DEVICE_FACE);
            if (block.kind() == TopologyCompiler.Kind.PIPE) {
                int joints = level.getBlockEntity(clicked) instanceof FluidDeviceBlockEntity entity ? entity.joints() : PhysicalFluidTopology.ALL_FACES;
                int edit = -1;
                if (joints != PhysicalFluidTopology.ALL_FACES && (joints >> face.ordinal() & 1) == 0) {
                    if (FluidDeviceBlock.hasFitting(state)) return new Resolved(null, Refusal.FITTING);
                    if (Integer.bitCount(joints) > 3) return new Resolved(null, Refusal.JUNCTION);
                    edit = joints | 1 << face.ordinal();
                }
                anchor = new PipeAnchor(Kind.PIPE, next, face.getOpposite(), clicked, edit, face);
            } else anchor = new PipeAnchor(Kind.DEVICE, next, face.getOpposite(), clicked, -1, face);
        } else if (state.getBlock() instanceof EquipmentShellBlock && level.getBlockEntity(clicked) instanceof EquipmentShellBlockEntity shell && shell.nozzleFacing(face) != null) {
            anchor = new PipeAnchor(Kind.NOZZLE, next, face.getOpposite(), clicked, -1, face);
        } else anchor = new PipeAnchor(Kind.FREE, state.canBeReplaced() ? clicked : next, null, null, -1, face);
        if (level.isOutsideBuildHeight(anchor.cell) || !level.getWorldBorder().isWithinBounds(anchor.cell)) return new Resolved(null, Refusal.OUTSIDE);
        if (!free(level, anchor.cell)) return new Resolved(null, Refusal.OCCUPIED);
        return new Resolved(anchor, null);
    }

    /** A cell a line may take: loaded, inside the world and replaceable (air, plants, fluids). */
    public static boolean free(Level level, BlockPos cell) {
        return level.hasChunkAt(cell) && !level.isOutsideBuildHeight(cell) && level.getWorldBorder().isWithinBounds(cell) && level.getBlockState(cell).canBeReplaced();
    }

    /** The route search's view of {@code level}: free cells, and nothing else. */
    public static PipeRoute.Space space(Level level) {
        var position = new BlockPos.MutableBlockPos();
        return cell -> free(level, position.set(cell.x(), cell.y(), cell.z())) ? PipeRoute.Admit.FREE : PipeRoute.Admit.BLOCKED;
    }
}
