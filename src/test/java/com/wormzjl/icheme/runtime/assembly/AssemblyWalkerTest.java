package com.wormzjl.icheme.runtime.assembly;

import com.wormzjl.icheme.runtime.assembly.AssemblyWalker.Piece;
import com.wormzjl.icheme.runtime.assembly.AssemblyWalker.Shell;
import com.wormzjl.icheme.science.assembly.PortGraph.*;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The walker on a hand-built ground (batch 2026-10-26-multi-machine-framework, plan section 6): drums of real geometry,
 * pipes, fittings and terminals placed by the test.
 */
class AssemblyWalkerTest {
    private static final int ALL = 0b111111;

    /** A ground of placed pieces and structures; chunks listed as unloaded read as such. */
    private static final class FakeGround implements AssemblyWalker.Ground {
        final Map<BlockPos, Piece> pieces = new HashMap<>();
        final Map<BlockPos, Shell> shells = new HashMap<>();
        final Set<Long> unloaded = new HashSet<>();

        Shell place(BlockPos origin, EquipmentShape shape) {
            var shell = new Shell(origin, shape);
            for (var c : ShapeRasterizer.layout(shape).shell()) shells.put(origin.offset(c.x(), c.y(), c.z()), shell);
            return shell;
        }

        void pipe(BlockPos at) {
            pieces.put(at, new Piece(true, false, null, Direction.NORTH, ALL));
        }

        /** A fitting on a pipe: it meets its neighbours along its outlet's axis only. */
        void fitting(BlockPos at, Fitting kind, Direction outlet) {
            pieces.put(at, new Piece(true, false, kind, outlet, 1 << outlet.ordinal() | 1 << outlet.getOpposite().ordinal()));
        }

        void tank(BlockPos at) {
            pieces.put(at, new Piece(false, true, null, Direction.NORTH, ALL));
        }

        @Override
        public boolean loaded(BlockPos position) {
            return !unloaded.contains(net.minecraft.world.level.ChunkPos.asLong(position.getX() >> 4, position.getZ() >> 4));
        }

        @Override
        public @Nullable Piece piece(BlockPos position) {
            return pieces.get(position);
        }

        @Override
        public @Nullable Shell shell(BlockPos position) {
            return shells.get(position);
        }
    }

    private static final BlockPos ORIGIN = new BlockPos(100, 64, 100);

    private static EquipmentShape drum() {
        return EquipmentShape.drum(2, 4, EquipmentShape.Orientation.HORIZONTAL, EquipmentShape.Heading.NORTH, 1);
    }

    /** The pipe block in front of {@code shape}'s first nozzle of {@code role}, and the way out of the nozzle. */
    private static Map.Entry<BlockPos, Direction> front(BlockPos origin, EquipmentShape shape, Role role) {
        for (var p : ShapeRasterizer.ports(shape))
            if (shape.nozzles().get(p.nozzle()).role() == role) {
                var d = Direction.fromDelta(p.dx(), p.dy(), p.dz());
                return Map.entry(origin.offset(p.cell().x(), p.cell().y(), p.cell().z()).relative(d), d);
            }
        throw new AssertionError("No nozzle " + role);
    }

    /** Two directions square to {@code d}, opposite each other. */
    private static Direction across(Direction d) {
        return d.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
    }

    @Test
    void aLineThroughAValveEndsAtABranchWhoseRunsReachATankAndAnOpenEnd() {
        var g = new FakeGround();
        var shape = drum();
        g.place(ORIGIN, shape);
        var front = front(ORIGIN, shape, Role.LIQUID_OUTLET);
        var d = front.getValue();
        var at = front.getKey();
        for (int i = 0; i < 5; i++) {
            if (i == 2) g.fitting(at, Fitting.CONTROL_VALVE, d);
            else g.pipe(at);
            at = at.relative(d);
        }
        // The tee: one branch to a tank beside it, the other two pipes to an open end.
        var tee = at;
        g.pipe(tee);
        var side = across(d);
        g.tank(tee.relative(side));
        g.pipe(tee.relative(side.getOpposite()));
        g.pipe(tee.relative(side.getOpposite(), 2));

        var walker = new AssemblyWalker(g);
        var unit = walker.unit(ORIGIN, shape);
        assertTrue(unit.lying());
        var run = walker.run(new Port(unit, Role.LIQUID_OUTLET, 0));
        assertEquals(new Junction(tee.asLong(), 2), run.end());
        assertEquals(6, run.pipes());
        assertEquals(List.of(new Element(Fitting.CONTROL_VALVE, 1, front.getKey().relative(d, 2).asLong(), 2)), run.elements());
        var branches = walker.branches((Junction) run.end(), run);
        assertEquals(2, branches.size());
        var ends = branches.stream().map(Run::end).toList();
        assertTrue(ends.contains(new Terminal(tee.relative(side).asLong())), ends::toString);
        assertTrue(ends.contains(new OpenEnd(tee.relative(side.getOpposite(), 2).asLong())), ends::toString);
        // Every block read is coded, the nozzle's flange cell with its kind and index.
        assertTrue(walker.codes().keySet().containsAll(run.positions()));
        assertTrue(walker.codes().values().stream().anyMatch(c -> c < 0));
        assertSame(run, walker.run(new Port(unit, Role.LIQUID_OUTLET, 0)), "a run is read once");
    }

    @Test
    void aValveFacingBackAlongTheWalkPointsAgainstIt() {
        var g = new FakeGround();
        var shape = drum();
        g.place(ORIGIN, shape);
        var front = front(ORIGIN, shape, Role.VAPOUR_OUTLET);
        var d = front.getValue();
        g.pipe(front.getKey());
        g.fitting(front.getKey().relative(d), Fitting.CONTROL_VALVE, d.getOpposite());
        g.fitting(front.getKey().relative(d, 2), Fitting.OTHER, d);
        g.pipe(front.getKey().relative(d, 3));
        var walker = new AssemblyWalker(g);
        var run = walker.run(new Port(walker.unit(ORIGIN, shape), Role.VAPOUR_OUTLET, 0));
        assertEquals(-1, run.elements().get(0).direction());
        assertEquals(Fitting.OTHER, run.elements().get(1).kind());
        assertEquals(new OpenEnd(front.getKey().relative(d, 3).asLong()), run.end());
    }

    @Test
    void aPipeToAnotherDrumsSideConnectionEndsAtThatPort() {
        var g = new FakeGround();
        var shape = drum();
        g.place(ORIGIN, shape);
        var front = front(ORIGIN, shape, Role.VAPOUR_OUTLET);
        // A second drum whose first side connection's pipe block is reached by a straight pipe from the first's outlet.
        var other = EquipmentShape.drum(2, 4, EquipmentShape.Orientation.VERTICAL, EquipmentShape.Heading.NORTH, 2,
                EquipmentShape.defaultConnections(2, 4, EquipmentShape.Orientation.VERTICAL, 2, false));
        var side = front(BlockPos.ZERO, other, Role.SIDE);
        // The line rises eight blocks from the first drum's top outlet; the second drum stands beside its top, its side
        // connection's pipe block the line's last block.
        var d = front.getValue();
        assertEquals(Direction.UP, d);
        assertTrue(side.getValue().getAxis().isHorizontal());
        var target = front.getKey().relative(d, 8);
        var otherOrigin = target.subtract(side.getKey());
        g.place(otherOrigin, other);
        for (int i = 0; i <= 8; i++) g.pipe(front.getKey().relative(d, i));
        var walker = new AssemblyWalker(g);
        var run = walker.run(new Port(walker.unit(ORIGIN, shape), Role.VAPOUR_OUTLET, 0));
        var end = assertInstanceOf(AtPort.class, run.end());
        assertEquals(Role.SIDE, end.port().role());
        assertEquals(0, end.port().ordinal());
        assertEquals(otherOrigin.asLong(), end.port().unit().origin());
        assertFalse(end.port().unit().lying());
        assertEquals(9, run.pipes());
    }

    @Test
    void aNozzleWithoutAPipeIsUnconnectedAndAnUnloadedChunkIsNotChecked() {
        var g = new FakeGround();
        var shape = drum();
        g.place(ORIGIN, shape);
        var walker = new AssemblyWalker(g);
        var unit = walker.unit(ORIGIN, shape);
        assertEquals(new Unconnected(), walker.run(new Port(unit, Role.VAPOUR_OUTLET, 0)).end());
        // A tank right on a nozzle: only a pipe meets a nozzle.
        var liquid = front(ORIGIN, shape, Role.LIQUID_OUTLET).getKey();
        g.tank(liquid);
        assertEquals(new Unconnected(), new AssemblyWalker(g).run(new Port(new AssemblyWalker(g).unit(ORIGIN, shape), Role.LIQUID_OUTLET, 0)).end());
        // The pipe block's chunk not loaded.
        var vapour = front(ORIGIN, shape, Role.VAPOUR_OUTLET).getKey();
        g.unloaded.add(net.minecraft.world.level.ChunkPos.asLong(vapour.getX() >> 4, vapour.getZ() >> 4));
        var fresh = new AssemblyWalker(g);
        assertEquals(new NotLoaded(vapour.asLong()), fresh.run(new Port(fresh.unit(ORIGIN, shape), Role.VAPOUR_OUTLET, 0)).end());
    }

    @Test
    void aWalkStopsAtItsBudgetAndReadsAsNotChecked() {
        var g = new FakeGround();
        var shape = drum();
        g.place(ORIGIN, shape);
        var front = front(ORIGIN, shape, Role.VAPOUR_OUTLET);
        for (int i = 0; i < 30; i++) g.pipe(front.getKey().relative(front.getValue(), i));
        var walker = new AssemblyWalker(g, 10);
        var run = walker.run(new Port(walker.unit(ORIGIN, shape), Role.VAPOUR_OUTLET, 0));
        assertTrue(walker.exhausted());
        assertInstanceOf(NotLoaded.class, run.end());
        assertEquals(10, run.pipes());
    }
}
