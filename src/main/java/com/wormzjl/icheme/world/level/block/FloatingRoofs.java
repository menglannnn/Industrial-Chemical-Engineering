package com.wormzjl.icheme.world.level.block;

import com.wormzjl.icheme.runtime.fluid.TankDisplay;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.science.math.DeterministicMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

/**
 * The floating roofs of a level's loaded floating-roof tanks as the world collides with them (owner request 2026-10-21:
 * "let the player on the lid actually move with a rising lid"; option A of the batch's review). An open tank's interior
 * cells ({@link EquipmentInteriorBlock#OPEN}) collide up to the roof's walking surface (its deck, its pontoon ring's top
 * beyond the deck's radius) and are empty above it; the surface stands where the tank's display puts it at the game tick
 * ({@link TankDisplay.Glide}), the same on the server and on the client, which receives the server's glide whole.
 * Vanilla lifts nothing that stands on a rising surface, so while a roof rises the server lifts the entities on it
 * ({@link #carry(Level)}, players excepted) and each client its own player ({@link #carry(Player)}). The rolling ladder
 * from the gauger's platform down to the deck is climbable ({@link #ladder}).
 *
 * <p>One index per level, filled by the tanks' node cells as they load and emptied as they unload; looked up by chunk
 * section, so a collision query costs a map lookup and a box test. Thread-safe: the server and the client read their own
 * level's. Before a tank's first display the roof stands at the level its node cell saved ({@link
 * EquipmentShellBlockEntity}), so a player on a high roof does not fall when the world loads.</p>
 */
public final class FloatingRoofs {
    /** How far under the surface an entity's feet may be for a rising roof to lift it, m: far more than a roof rises in a tick. */
    static final double LIFT = .6;
    /** How near the rolling ladder's line an entity's middle must be to climb it, m. */
    static final double LADDER_REACH = .8;
    /** How far past its head, up its line, the rolling ladder still holds a climber, m; how far over its feet a body's foot point stands. */
    static final double HEAD_REACH = .5, FEET = .1;
    /**
     * Climbing the rolling ladder ({@link #climb}): the most a tick rises (a running climb) and falls (a vanilla ladder's
     * slowed fall), m per tick, and the steepest rise per metre in it follows (a ladder near upright is climbed as one).
     */
    static final double MAX_CLIMB = .3, DESCENT = .15, MAX_SLOPE = 4;
    /** The surface's height steps, per metre: the collision shapes of a cell are cached by step. */
    static final int STEPS = 64;
    private static final VoxelShape[] SLABS = new VoxelShape[STEPS + 1];
    static {
        SLABS[0] = Shapes.empty();
        for (int i = 1; i < STEPS; i++) SLABS[i] = Shapes.box(0, 0, 0, 1, (double) i / STEPS, 1);
        SLABS[STEPS] = Shapes.block();
    }
    private static final Map<Level, FloatingRoofs> LEVELS = Collections.synchronizedMap(new WeakHashMap<>());

    /** One loaded tank: its node cell, box, roof and the glide its display gives. */
    static final class Tank {
        final BlockPos node, origin;
        final int sizeX, sizeY, sizeZ;
        final ShapeRasterizer.Roof roof;
        /** The roof's axis in world coordinates. */
        final double axisX, axisZ;
        /** The level the node cell saved, used until the first display. */
        final DoubleSupplier saved;
        volatile TankDisplay.@Nullable Glide glide;

        Tank(BlockPos node, BlockPos origin, EquipmentShape shape, ShapeRasterizer.Roof roof, DoubleSupplier saved) {
            this.node = node.immutable();
            this.origin = origin.immutable();
            sizeX = shape.sizeX();
            sizeY = shape.sizeY();
            sizeZ = shape.sizeZ();
            this.roof = roof;
            axisX = origin.getX() + roof.x();
            axisZ = origin.getZ() + roof.z();
            this.saved = saved;
        }

        boolean contains(BlockPos cell) {
            int x = cell.getX() - origin.getX(), y = cell.getY() - origin.getY(), z = cell.getZ() - origin.getZ();
            return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
        }

        /** The liquid's level at game time {@code ticks}, m. */
        double level(double ticks) {
            var g = glide;
            return g == null ? saved.getAsDouble() : g.level(ticks);
        }

        /**
         * The walking surface's world height over the column of cells at {@code x}, {@code z} (world block coordinates), on
         * whole {@link #STEPS}: the deck's where the column lies wholly inside the deck's radius, the pontoon ring's top
         * where any of it reaches beyond.
         */
        double surface(int x, int z, double level) {
            double x0 = x - axisX, x1 = x0 + 1, z0 = z - axisZ, z1 = z0 + 1;
            double far = Math.sqrt(Math.max(x0 * x0, x1 * x1) + Math.max(z0 * z0, z1 * z1));
            return Math.round((origin.getY() + roof.surface(far < roof.inner() ? 0 : roof.inner(), level)) * STEPS) / (double) STEPS;
        }
    }

    private final Map<BlockPos, Tank> byNode = new ConcurrentHashMap<>();
    private final Map<Long, List<Tank>> bySection = new ConcurrentHashMap<>();

    FloatingRoofs() {
    }

    public static FloatingRoofs of(Level level) {
        return LEVELS.computeIfAbsent(level, k -> new FloatingRoofs());
    }

    /** A tank's node cell loaded or took new data: its roof joins the index, keeping the glide it had. Nothing for a shape without a floating roof. */
    public void add(BlockPos node, BlockPos origin, EquipmentShape shape, DoubleSupplier saved) {
        var roof = ShapeRasterizer.roof(shape);
        if (roof == null) return;
        var old = byNode.get(node);
        remove(node);
        var tank = new Tank(node, origin, shape, roof, saved);
        if (old != null) tank.glide = old.glide;
        byNode.put(tank.node, tank);
        for (int sx = origin.getX() >> 4; sx <= (origin.getX() + tank.sizeX - 1) >> 4; sx++)
            for (int sy = origin.getY() >> 4; sy <= (origin.getY() + tank.sizeY - 1) >> 4; sy++)
                for (int sz = origin.getZ() >> 4; sz <= (origin.getZ() + tank.sizeZ - 1) >> 4; sz++)
                    bySection.compute(SectionPos.asLong(sx, sy, sz), (k, list) -> {
                        var out = list == null ? new ArrayList<Tank>() : new ArrayList<>(list);
                        out.add(tank);
                        return List.copyOf(out);
                    });
    }

    /** A tank's node cell unloaded or was removed. */
    public void remove(BlockPos node) {
        var tank = byNode.remove(node);
        if (tank == null) return;
        for (var key : List.copyOf(bySection.keySet()))
            bySection.computeIfPresent(key, (k, list) -> {
                var out = new ArrayList<>(list);
                out.remove(tank);
                return out.isEmpty() ? null : List.copyOf(out);
            });
    }

    /** The tank at {@code node} glides by {@code glide} (null: back to its saved level). */
    public void glide(BlockPos node, TankDisplay.@Nullable Glide glide) {
        var tank = byNode.get(node);
        if (tank != null) tank.glide = glide;
    }

    public TankDisplay.@Nullable Glide glide(BlockPos node) {
        var tank = byNode.get(node);
        return tank == null ? null : tank.glide;
    }

    /** The liquid's level in the tank at {@code node} at game time {@code ticks}, m; 0 for no such tank. */
    public double level(BlockPos node, double ticks) {
        var tank = byNode.get(node);
        return tank == null ? 0 : tank.level(ticks);
    }

    private @Nullable Tank at(BlockPos cell) {
        var list = bySection.get(SectionPos.asLong(cell.getX() >> 4, cell.getY() >> 4, cell.getZ() >> 4));
        if (list == null) return null;
        for (var tank : list) if (tank.contains(cell)) return tank;
        return null;
    }

    /**
     * The collision shape of the open interior cell {@code cell} at game tick {@code tick}: solid up to the roof's walking
     * surface over the cell and empty above it; null when no loaded tank holds the cell. A cell reaching into the pontoon
     * ring stands at the ring's top.
     */
    @Nullable VoxelShape collision(BlockPos cell, long tick) {
        var tank = at(cell);
        if (tank == null) return null;
        double height = tank.surface(cell.getX(), cell.getZ(), tank.level(tick)) - cell.getY();
        if (height <= 0) return SLABS[0];
        if (height >= 1) return SLABS[STEPS];
        return SLABS[(int) Math.round(height * STEPS)];
    }

    /** Whether {@code entity} in the open interior cell {@code cell} is on the rolling ladder at game tick {@code tick}. */
    boolean ladder(BlockPos cell, Entity entity, long tick) {
        return ladder(cell, entity.getX(), entity.getY() + FEET, entity.getZ(), tick);
    }

    /** Whether a body whose feet are at {@code x}, {@code y}, {@code z} in the cell {@code cell} are on the rolling ladder at game tick {@code tick}. */
    boolean ladder(BlockPos cell, double x, double y, double z, long tick) {
        var tank = at(cell);
        return tank != null && onLadder(tank, x, y, z, tick);
    }

    /** Whether a body whose feet are at {@code x}, {@code y}, {@code z} are on {@code tank}'s rolling ladder at game tick {@code tick}. */
    private static boolean onLadder(Tank tank, double x, double y, double z, long tick) {
        var roof = tank.roof;
        double pitch = roof.pitch(tank.level(tick)), c = DeterministicMath.cos(pitch), s = DeterministicMath.sin(pitch);
        double[] p = roof.pivot(), a = roof.along();
        double dx = a[0] * c, dy = -s, dz = a[2] * c;
        double ex = x - (tank.origin.getX() + p[0]), ey = y - (tank.origin.getY() + p[1]), ez = z - (tank.origin.getZ() + p[2]);
        // A little past its head too: a climber keeps hold while stepping onto the platform.
        double t = Math.max(-HEAD_REACH, Math.min(roof.length(), ex * dx + ey * dy + ez * dz));
        double rx = ex - t * dx, ry = ey - t * dy, rz = ez - t * dz;
        return rx * rx + ry * ry + rz * rz < LADDER_REACH * LADDER_REACH;
    }

    /**
     * Server, once a tick: the roofs that rise this tick lift what stands on them, players excepted (their clients move
     * them). A roof that is still or sinks lifts nothing and costs one level read: what stands on a sinking roof follows
     * it down by gravity.
     */
    public static void carry(Level level) {
        var index = LEVELS.get(level);
        if (index == null || index.byNode.isEmpty()) return;
        long tick = level.getGameTime();
        for (var tank : index.byNode.values()) {
            double now = tank.level(tick), under = tank.roof.underside(now), before = tank.roof.underside(tank.level(tick - 1));
            if (!(under > before)) continue;
            double r = tank.roof.radius(), low = tank.origin.getY() + before + Math.min(tank.roof.deck(), tank.roof.pontoon()) - LIFT,
                    high = tank.origin.getY() + under + Math.max(tank.roof.deck(), tank.roof.pontoon());
            var box = new AABB(tank.axisX - r, low, tank.axisZ - r, tank.axisX + r, high, tank.axisZ + r);
            for (var entity : level.getEntities((Entity) null, box, e -> !(e instanceof Player) && !e.noPhysics && !e.isPassenger() && !e.isSpectator()))
                lift(tank, entity, now);
        }
    }

    /** Client, once a tick: the client's own player rides the roof under it, and walks up the rolling ladder it is on. */
    public static void carry(Player player) {
        if (player.isSpectator() || player.getAbilities().flying || player.isPassenger()) return;
        var index = LEVELS.get(player.level());
        if (index == null || index.byNode.isEmpty()) return;
        long tick = player.level().getGameTime();
        for (var tank : index.byNode.values()) {
            lift(tank, player, tank.level(tick));
            climb(tank, player, tick);
        }
    }

    /**
     * A player on the rolling ladder stands on its treads (owner reports 2026-10-21: the ladder did not climb, then a
     * climber stuck under the platform's edge): its feet follow the ladder's line, so walking along it climbs or descends
     * it as a ramp, and at its head, level with the platform's deck at the deck's edge, the climber walks off onto the deck
     * with nothing overhead. A vanilla ladder climbs only against a wall or with a jump; this one is pitched, with no wall.
     * The next tick's rise brings the feet to the line, at most a running climb up; down, at most a ladder's slowed fall,
     * the pace in along it held to what that fall follows.
     */
    private static void climb(Tank tank, Player player, long tick) {
        double feet = player.getY();
        if (!onLadder(tank, player.getX(), feet + FEET, player.getZ(), tick)) return;
        var roof = tank.roof;
        double pitch = roof.pitch(tank.level(tick)), cos = DeterministicMath.cos(pitch);
        double[] p = roof.pivot(), a = roof.along();
        // How far in from its head, along its level direction, the player stands; over the deck or past its foot the deck or the roof carries it.
        double in = (player.getX() - (tank.origin.getX() + p[0])) * a[0] + (player.getZ() - (tank.origin.getZ() + p[2])) * a[2];
        if (in < 0 || in > roof.length() * cos) return;
        double slope = Math.min(MAX_SLOPE, DeterministicMath.sin(pitch) / Math.max(1e-6, cos)), line = tank.origin.getY() + p[1] - in * slope;
        var motion = player.getDeltaMovement();
        double inward = motion.x * a[0] + motion.z * a[2], held = DESCENT / slope, x = motion.x, z = motion.z;
        if (inward > held) {
            x -= (inward - held) * a[0];
            z -= (inward - held) * a[2];
        }
        player.setDeltaMovement(x, Math.max(-DESCENT, Math.min(MAX_CLIMB, line - feet)), z);
        player.resetFallDistance();
    }

    /** Lifts {@code entity} onto the roof of {@code tank} at {@code level} when its feet stand just under the surface. */
    private static void lift(Tank tank, Entity entity, double level) {
        double dx = entity.getX() - tank.axisX, dz = entity.getZ() - tank.axisZ;
        if (dx * dx + dz * dz > tank.roof.radius() * tank.roof.radius()) return;
        double surface = tank.surface(entity.getBlockX(), entity.getBlockZ(), level), feet = entity.getY();
        if (feet >= surface || feet < surface - LIFT) return;
        entity.setPos(entity.getX(), surface, entity.getZ());
        var motion = entity.getDeltaMovement();
        if (motion.y < 0) entity.setDeltaMovement(motion.x, 0, motion.z);
        entity.setOnGround(true);
        entity.resetFallDistance();
    }
}
