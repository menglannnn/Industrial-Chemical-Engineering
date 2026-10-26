package com.wormzjl.icheme.runtime.assembly;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.runtime.assembly.AssemblyStore.Entry;
import com.wormzjl.icheme.runtime.assembly.AssemblyStore.Status;
import com.wormzjl.icheme.runtime.fluid.FluidWorldAuthority;
import com.wormzjl.icheme.runtime.fluid.PhysicalFluidTopology;
import com.wormzjl.icheme.science.assembly.AssemblyMatcher;
import com.wormzjl.icheme.science.assembly.ColumnBlueprint;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import com.wormzjl.icheme.world.level.block.EquipmentStructure.Vessel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Validates assemblies and reverts their reports without polling (batch 2026-10-26-multi-machine-framework, plan
 * sections 6 and 7). A validation walks the pipes once and stores its report; nothing runs periodically. Three paths
 * change what a report relies on, each answered with one index lookup: a fluid edit (a pipe placed or removed, a fitting
 * installed or removed; a setting edit changes no code and reverts nothing), a structure placed, removed or rebuilt, and a
 * chunk load, whose blocks are compared with the stored codes (reads only). A complete report reverts on any structural
 * change it relied on, a new branch included; a checked one on a change at a block or part it found, but not on an
 * addition beside it: the player is building. Requests and chunk checks queue and run at the next server tick.
 */
public final class AssemblyService {
    /** A master to validate, found from one of its cells; {@code rebuild}: only if the store was dropped and holds no entry. */
    private record Request(ResourceKey<Level> dimension, BlockPos cell, boolean rebuild) {
    }

    private record LoadedChunk(ResourceKey<Level> dimension, long chunk) {
    }

    private static final class Queue {
        final LinkedHashMap<BlockPos, Request> requests = new LinkedHashMap<>();
        final LinkedHashSet<LoadedChunk> chunks = new LinkedHashSet<>();
        long walks;
    }

    private static final Map<MinecraftServer, Queue> QUEUES = new IdentityHashMap<>();

    private AssemblyService() {
    }

    private static Queue queue(MinecraftServer server) {
        return QUEUES.computeIfAbsent(server, s -> new Queue());
    }

    /** Forgets a stopped server's queue. */
    public static void forget(MinecraftServer server) {
        QUEUES.remove(server);
    }

    /** How many validations walked on {@code server}: a counter the GameTests read. */
    public static long walks(MinecraftServer server) {
        var q = QUEUES.get(server);
        return q == null ? 0 : q.walks;
    }

    /** A column with a component design: one that has an assembly. */
    public static boolean designed(EquipmentShape shape) {
        return shape.kind() == EquipmentShape.Kind.COLUMN && shape.trays() != null && shape.trays().components() != null;
    }

    /** The entry of the column at {@code origin}, or null. */
    public static @Nullable Entry entry(ServerLevel level, BlockPos origin) {
        return AssemblyStore.of(level).entry(origin.asLong());
    }

    /** Queues a validation of the structure holding {@code cell}. */
    public static void request(ServerLevel level, BlockPos cell) {
        queue(level.getServer()).requests.putIfAbsent(cell.immutable(), new Request(level.dimension(), cell.immutable(), false));
    }

    /**
     * Validates the column holding {@code cell} now: walks its pipes, matches its blueprint and stores the report. Null for
     * a structure that is not a designed column.
     */
    public static @Nullable Entry validate(ServerLevel level, BlockPos cell) {
        var vessel = EquipmentStructure.find(level, cell);
        if (vessel == null || !designed(vessel.shape())) return null;
        var world = FluidWorldAuthority.find(level.getServer()).orElse(null);
        if (world == null) return null;
        var store = AssemblyStore.of(level);
        var walker = new AssemblyWalker(new LevelGround(level, world));
        long master = vessel.origin().asLong();
        var report = AssemblyMatcher.match(ColumnBlueprint.of(vessel.shape().trays()), walker, walker.unit(vessel.origin(), vessel.shape()), store.taken(master));
        var entry = Entry.of(report, walker.codes());
        store.put(master, entry);
        queue(level.getServer()).walks++;
        if (walker.exhausted())
            IChemE.LOGGER.warn("assembly status=WALK_LIMIT master={} visited={} detail=the walk stopped at {} blocks; lines beyond read as not checked",
                    vessel.origin().toShortString(), walker.visited(), AssemblyWalker.MAX_WALK);
        return entry;
    }

    /** Runs the queued chunk checks and validations; nothing when none is queued. */
    public static void tick(MinecraftServer server) {
        var q = QUEUES.get(server);
        if (q == null || q.requests.isEmpty() && q.chunks.isEmpty()) return;
        var chunks = new ArrayList<>(q.chunks);
        q.chunks.clear();
        for (var c : chunks) {
            var level = server.getLevel(c.dimension());
            if (level != null) verify(level, c.chunk());
        }
        var requests = new ArrayList<>(q.requests.values());
        q.requests.clear();
        var done = new HashSet<Long>();
        for (var r : requests) {
            var level = server.getLevel(r.dimension());
            if (level == null || !level.isLoaded(r.cell())) continue;
            var vessel = EquipmentStructure.find(level, r.cell());
            // One validation per master per round.
            if (vessel == null || !done.add(vessel.origin().asLong())) continue;
            if (r.rebuild()) {
                var store = AssemblyStore.of(level);
                if (!store.rebuilding() || store.entry(vessel.origin().asLong()) != null) continue;
            }
            validate(level, r.cell());
        }
    }

    /**
     * A designed column's controller loaded: after a dropped store, a column with no entry is validated once, which
     * rebuilds its report and footprint.
     */
    public static void controllerLoaded(ServerLevel level, BlockPos cell, EquipmentShape shape) {
        if (!designed(shape) || !AssemblyStore.of(level).rebuilding()) return;
        queue(level.getServer()).requests.putIfAbsent(cell.immutable(), new Request(level.dimension(), cell.immutable(), true));
    }

    /** A chunk loaded: its blocks are compared with the codes of the reports that rely on them, at the next tick. */
    public static void chunkLoaded(ServerLevel level, ChunkPos chunk) {
        if (AssemblyStore.of(level).mastersInChunk(chunk.toLong()).isEmpty()) return;
        queue(level.getServer()).chunks.add(new LoadedChunk(level.dimension(), chunk.toLong()));
    }

    /**
     * A fluid edit: each device before and after it. A changed code at a position a report relies on reverts it; a block
     * placed beside a complete report's block is a new branch and reverts it too.
     */
    public static void edited(MinecraftServer server, List<FluidWorldAuthority.DeviceChange> changes) {
        for (var change : changes) {
            var p = change.position();
            var level = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.ResourceLocation.parse(p.dimension())));
            if (level == null) continue;
            Integer before = code(change.before()), after = code(change.after());
            if (Objects.equals(before, after)) continue;
            var store = AssemblyStore.of(level);
            var at = new BlockPos(p.x(), p.y(), p.z());
            for (long master : coded(store, at.asLong())) revert(level, store, master, at, "changed");
            if (before == null)
                for (var d : Direction.values())
                    for (long master : coded(store, at.relative(d).asLong()))
                        if (status(store, master) == Status.COMPLETE) revert(level, store, master, at, "branch");
        }
    }

    /**
     * A structure placed or rebuilt: a complete report whose blocks it stands beside reverts (its nozzles may join a walked
     * line). A placed designed column starts not validated.
     */
    public static void placed(ServerLevel level, Vessel vessel) {
        var store = AssemblyStore.of(level);
        for (var position : around(vessel))
            for (long master : coded(store, position))
                if (status(store, master) == Status.COMPLETE) revert(level, store, master, BlockPos.of(position), "structure");
        if (designed(vessel.shape()) && store.entry(vessel.origin().asLong()) == null) store.put(vessel.origin().asLong(), Entry.NOT_VALIDATED);
    }

    /**
     * A structure removed, or about to be rebuilt elsewhere: a column's own entry leaves; a report that held it, or reached
     * one of its nozzles, reverts.
     */
    public static void removed(ServerLevel level, Vessel vessel) {
        var store = AssemblyStore.of(level);
        long origin = vessel.origin().asLong();
        store.forget(origin);
        for (long master : store.mastersAt(origin))
            if (store.entry(master) != null && store.entry(master).holds(origin)) revert(level, store, master, vessel.origin(), "removed");
        for (var position : around(vessel)) for (long master : coded(store, position)) revert(level, store, master, BlockPos.of(position), "removed");
    }

    /** The masters whose reports coded the block at {@code position}. */
    private static List<Long> coded(AssemblyStore store, long position) {
        var out = new ArrayList<Long>();
        for (long master : store.mastersAt(position)) {
            var entry = store.entry(master);
            if (entry != null && entry.codes().containsKey(position)) out.add(master);
        }
        return out;
    }

    private static @Nullable Status status(AssemblyStore store, long master) {
        var entry = store.entry(master);
        return entry == null ? null : entry.status();
    }

    private static void revert(ServerLevel level, AssemblyStore store, long master, BlockPos at, String why) {
        var status = status(store, master);
        if (status == null || status == Status.NOT_VALIDATED) return;
        store.revert(master);
        IChemE.LOGGER.info("assembly status=REVERTED master={} was={} at={} reason={}", BlockPos.of(master).toShortString(), status, at.toShortString(), why);
    }

    /** The structural code of a registered device, or null for none or a vessel (which its shell's nozzles stand for). */
    private static @Nullable Integer code(@Nullable PhysicalFluidTopology.Device device) {
        if (device == null) return null;
        var piece = LevelGround.piece(device);
        return piece == null ? null : piece.code();
    }

    /** A structure's shell cells and the pipe block in front of each nozzle. */
    private static Set<Long> around(Vessel vessel) {
        var out = new HashSet<Long>();
        var o = vessel.origin();
        out.add(o.asLong());
        for (var cell : ShapeRasterizer.layout(vessel.shape()).shell()) out.add(o.offset(cell.x(), cell.y(), cell.z()).asLong());
        for (var port : ShapeRasterizer.ports(vessel.shape()))
            out.add(o.offset(port.cell().x() + port.dx(), port.cell().y() + port.dy(), port.cell().z() + port.dz()).asLong());
        return out;
    }

    /** Compares the coded blocks of a loaded chunk with what stands there; a difference reverts the report. */
    private static void verify(ServerLevel level, long chunk) {
        var world = FluidWorldAuthority.find(level.getServer()).orElse(null);
        if (world == null) return;
        var ground = new LevelGround(level, world);
        var store = AssemblyStore.of(level);
        var pos = new ChunkPos(chunk);
        for (long master : store.mastersInChunk(chunk)) {
            var entry = store.entry(master);
            if (entry == null || entry.status() == Status.NOT_VALIDATED) continue;
            for (var coded : entry.codes().entrySet()) {
                var at = BlockPos.of(coded.getKey());
                if (at.getX() >> 4 != pos.x || at.getZ() >> 4 != pos.z) continue;
                if (actual(ground, at, coded.getValue()) != coded.getValue()) {
                    revert(level, store, master, at, "chunk_load");
                    break;
                }
            }
        }
    }

    /** What stands at {@code at} as a code: the piece's, or the nozzle code {@code expected} if a nozzle of that code is there. */
    private static int actual(AssemblyWalker.Ground ground, BlockPos at, int expected) {
        if (expected >= 0) {
            var piece = ground.piece(at);
            return piece == null ? Integer.MIN_VALUE : piece.code();
        }
        var shell = ground.shell(at);
        if (shell == null) return Integer.MIN_VALUE;
        var o = shell.origin();
        for (var port : ShapeRasterizer.ports(shell.shape()))
            if (o.getX() + port.cell().x() == at.getX() && o.getY() + port.cell().y() == at.getY() && o.getZ() + port.cell().z() == at.getZ()
                    && AssemblyWalker.nozzleCode(shell.shape().kind(), port.nozzle()) == expected) return expected;
        return Integer.MIN_VALUE;
    }
}
