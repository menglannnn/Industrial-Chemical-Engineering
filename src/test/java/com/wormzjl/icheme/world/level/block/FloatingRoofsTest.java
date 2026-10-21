package com.wormzjl.icheme.world.level.block;

import com.wormzjl.icheme.runtime.fluid.FluidPresentation;
import com.wormzjl.icheme.runtime.fluid.TankDisplay;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Heading;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Kind;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A floating roof as the world collides with it (batch 2026-10-21-floating-roof-tanks, option A, owner request
 * 2026-10-21: a player on the roof rides it): the server's roof stands where the client draws it, the open cells collide
 * up to the deck inside its radius and up to the pontoon ring beyond, the roof glides between displays on the game clock,
 * and the rolling ladder is climbable along its pitched line.
 */
class FloatingRoofsTest {
    private static final EquipmentShape TANK = EquipmentShape.storage(Kind.TANK, 20, 12, Heading.NORTH, 0);
    private static final BlockPos ORIGIN = new BlockPos(100, 64, -40);

    private static TankDisplay at(double level) {
        return new TankDisplay((float) level, 0, (float) TANK.fillHeight(), 0, 0);
    }

    @Test
    void theServersRoofStandsWhereTheClientDrawsIt() {
        var roof = ShapeRasterizer.roof(TANK);
        var drawn = ShapeRasterizer.floating(TANK);
        assertNotNull(roof);
        assertNotNull(drawn);
        for (double level : new double[]{0, 1, 1.8, 4, 6, 9.5, 11, 30}) {
            assertEquals(drawn.underside(level), roof.underside(level), 0, "underside at " + level);
            assertEquals(drawn.pitch(level), roof.pitch(level), 1e-12, "ladder pitch at " + level);
            assertEquals(roof.underside(level) + roof.deck(), roof.surface(0, level), 0);
            assertEquals(roof.underside(level) + roof.pontoon(), roof.surface(roof.inner(), level), 0);
        }
        assertTrue(roof.inner() < roof.radius() && roof.radius() < TANK.diameter() / 2, roof.inner() + " " + roof.radius());
        assertTrue(roof.pontoon() > roof.deck(), "the pontoon ring stands above the deck");
        assertNull(ShapeRasterizer.roof(EquipmentShape.storage(Kind.SPHERE, 14, 1, Heading.NORTH, 2)));
    }

    @Test
    void theRoofGlidesToEachDisplayOverABucketOnTheGameClock() {
        var first = TankDisplay.Glide.next(null, at(4), 1000);
        assertEquals(4, first.level(1000), 0, "the first display stands at once");
        var second = TankDisplay.Glide.next(first, at(6), 1000 + FluidPresentation.BUCKET_TICKS);
        assertEquals(4, second.level(1000 + FluidPresentation.BUCKET_TICKS), 1e-6);
        assertEquals(5, second.level(1000 + 1.5 * FluidPresentation.BUCKET_TICKS), 1e-6);
        assertEquals(6, second.level(1000 + 2 * FluidPresentation.BUCKET_TICKS), 0);
        assertEquals(6, second.level(1e9), 0);
        // A display arriving mid-glide starts from where the roof stands then, so the roof never jumps.
        var early = TankDisplay.Glide.next(second, at(2), 1000 + FluidPresentation.BUCKET_TICKS * 3 / 2);
        assertEquals(5, early.level(early.tick()), 1e-6);
    }

    @Test
    void openCellsCollideUpToTheDeckAndThePontoonRing() {
        var roofs = new FloatingRoofs();
        float[] saved = {6};
        BlockPos node = ORIGIN.offset(1, 0, 1);
        roofs.add(node, ORIGIN, TANK, () -> saved[0]);
        var roof = ShapeRasterizer.roof(TANK);
        int ax = ORIGIN.getX() + (int) Math.floor(roof.x()), az = ORIGIN.getZ() + (int) Math.floor(roof.z());
        // Before any display the roof stands at the level its node cell saved: deck top at 6.15 m over the floor.
        double deck = 6 + roof.deck();
        int floor = (int) Math.floor(deck);
        assertEquals(1, top(roofs, new BlockPos(ax, ORIGIN.getY() + floor - 1, az), 0), 0, "under the deck: solid");
        assertEquals(deck - floor, top(roofs, new BlockPos(ax, ORIGIN.getY() + floor, az), 0), 1. / FloatingRoofs.STEPS);
        assertEquals(0, top(roofs, new BlockPos(ax, ORIGIN.getY() + floor + 1, az), 0), 0, "over the deck: open");
        // Out on the pontoon ring the surface stands higher.
        int rim = ax + (int) Math.floor(roof.inner()) + 1;
        double ring = 6 + roof.pontoon();
        assertEquals(ring - Math.floor(ring), top(roofs, new BlockPos(rim, ORIGIN.getY() + (int) Math.floor(ring), az), 0), 1. / FloatingRoofs.STEPS);
        // The display takes over from the saved level and the cells follow its glide.
        roofs.glide(node, TankDisplay.Glide.next(TankDisplay.Glide.still(at(6)), at(8), 0));
        assertEquals(1, top(roofs, new BlockPos(ax, ORIGIN.getY() + floor, az), FluidPresentation.BUCKET_TICKS), 0, "the deck rose past the cell");
        assertNull(roofs.collision(ORIGIN.offset(-1, 0, 0), 0), "no tank there");
        roofs.remove(node);
        assertNull(roofs.collision(new BlockPos(ax, ORIGIN.getY() + floor, az), 0), "an unloaded tank collides no more");
    }

    @Test
    void theRollingLadderIsClimbableAlongItsPitchedLine() {
        var roofs = new FloatingRoofs();
        roofs.add(ORIGIN, ORIGIN, TANK, () -> 3);
        var roof = ShapeRasterizer.roof(TANK);
        double pitch = roof.pitch(3), along = roof.length() / 2;
        double[] p = roof.pivot(), a = roof.along();
        double x = ORIGIN.getX() + p[0] + a[0] * Math.cos(pitch) * along, y = ORIGIN.getY() + p[1] - Math.sin(pitch) * along,
                z = ORIGIN.getZ() + p[2] + a[2] * Math.cos(pitch) * along;
        var cell = BlockPos.containing(x, y, z);
        assertTrue(roofs.ladder(cell, x, y, z, 0), "halfway down the ladder");
        assertFalse(roofs.ladder(cell, x, y + 2, z, 0), "two metres over it");
        double cx = ORIGIN.getX() + roof.x(), cz = ORIGIN.getZ() + roof.z(), cy = ORIGIN.getY() + roof.surface(0, 3) + 1;
        assertFalse(roofs.ladder(BlockPos.containing(cx, cy, cz), cx, cy, cz, 0), "the middle of the deck");
    }

    /** The top of the collision shape of {@code cell} at {@code tick}, as a fraction of the cell. */
    private static double top(FloatingRoofs roofs, BlockPos cell, long tick) {
        var shape = roofs.collision(cell, tick);
        assertNotNull(shape, cell.toString());
        return shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y);
    }
}
