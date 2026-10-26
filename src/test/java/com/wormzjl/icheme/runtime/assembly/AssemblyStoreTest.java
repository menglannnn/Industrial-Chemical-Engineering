package com.wormzjl.icheme.runtime.assembly;

import com.wormzjl.icheme.runtime.assembly.AssemblyStore.Entry;
import com.wormzjl.icheme.runtime.assembly.AssemblyStore.Status;
import com.wormzjl.icheme.science.assembly.AssemblyReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortState;
import com.wormzjl.icheme.science.assembly.AssemblyReport.SlotReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.State;
import com.wormzjl.icheme.science.assembly.Blueprint.Slot;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The level's assembly reports (batch 2026-10-26-multi-machine-framework, plan sections 7 and 11): state that round-trips or drops whole. */
class AssemblyStoreTest {
    private static final long COLUMN = new BlockPos(10, 64, 10).asLong(), DRUM = new BlockPos(20, 64, 10).asLong(), PIPE = new BlockPos(15, 70, 10).asLong(),
            FAR = new BlockPos(200, 70, 10).asLong();

    private static AssemblyReport report(State condenser) {
        var slots = List.of(new SlotReport(new Slot("column", 0), State.OK, COLUMN, AssemblyReport.NONE, ""),
                new SlotReport(new Slot("receiver", 0), condenser, DRUM, PIPE, condenser == State.OK ? "" : "DRUM_STANDING"));
        var ports = List.of(new PortReport(COLUMN, Role.VAPOUR_OUTLET, 0, PortState.CONNECTED), new PortReport(DRUM, Role.SIDE, 1, PortState.CONNECTED),
                new PortReport(COLUMN, Role.VAPOUR_RETURN, 0, PortState.BLIND));
        return new AssemblyReport(slots, ports, Set.of(PIPE, FAR, COLUMN, DRUM), List.of(COLUMN, DRUM));
    }

    private static AssemblyStore roundTrip(AssemblyStore store) {
        return AssemblyStore.load(store.save(new CompoundTag()));
    }

    @Test
    void entriesRoundTripWithTheirIndexes() {
        var store = new AssemblyStore();
        var complete = Entry.of(report(State.OK), Map.of(PIPE, 0x123, FAR, -5));
        assertEquals(Status.COMPLETE, complete.status());
        store.put(COLUMN, complete);
        long other = new BlockPos(50, 64, 50).asLong();
        store.put(other, Entry.NOT_VALIDATED);
        var read = roundTrip(store);
        assertEquals(store.entries(), read.entries());
        assertFalse(read.rebuilding());
        assertEquals(Set.of(COLUMN), read.mastersAt(PIPE));
        assertEquals(Set.of(COLUMN), read.mastersAt(DRUM), "a member's origin is indexed");
        assertEquals(Set.of(COLUMN), read.mastersInChunk(ChunkPos.asLong(200 >> 4, 10 >> 4)));
        // Another master sees the drum as taken; the column itself does not.
        assertEquals(Set.of(DRUM), read.taken(other));
        assertEquals(Set.of(), read.taken(COLUMN));
    }

    @Test
    void aRevertKeepsTheMasterNotValidatedAndClearsItsIndex() {
        var store = new AssemblyStore();
        store.put(COLUMN, Entry.of(report(State.WRONG_KIND), Map.of(PIPE, 7)));
        assertEquals(Status.CHECKED, store.entry(COLUMN).status());
        store.revert(COLUMN, PIPE);
        assertEquals(Entry.reverted(PIPE), store.entry(COLUMN));
        assertEquals(Set.of(), store.mastersAt(PIPE));
        assertEquals(Set.of(), store.mastersInChunk(ChunkPos.asLong(15 >> 4, 10 >> 4)));
        assertEquals(PIPE, roundTrip(store).entry(COLUMN).changedAt(), "where the structure changed is kept");
        store.forget(COLUMN);
        assertNull(store.entry(COLUMN));
        assertTrue(roundTrip(store).entries().isEmpty());
    }

    @Test
    void aStoreThatDoesNotReadIsDroppedWholeAndMarkedForRebuilding() {
        var store = new AssemblyStore();
        store.put(COLUMN, Entry.of(report(State.OK), Map.of(PIPE, 1)));
        var tag = store.save(new CompoundTag());
        // Another state format.
        var format = tag.copy();
        format.putInt("format", AssemblyStore.FORMAT + 1);
        var dropped = AssemblyStore.load(format);
        assertTrue(dropped.entries().isEmpty());
        assertTrue(dropped.rebuilding());
        // An unknown state name inside a report.
        var damaged = tag.copy();
        damaged.getList("entries", 10).getCompound(0).getList("slots", 10).getCompound(1).putString("state", "SIDEWAYS");
        assertTrue(AssemblyStore.load(damaged).rebuilding());
        // A status that the report does not support: complete while a slot is missing.
        var status = AssemblyStore.load(tag.copy());
        assertFalse(status.rebuilding());
        var wrong = tag.copy();
        wrong.getList("entries", 10).getCompound(0).getList("slots", 10).getCompound(1).putString("state", "MISSING");
        assertTrue(AssemblyStore.load(wrong).rebuilding());
        // The rebuild mark is saved: it survives until each column is validated again.
        assertTrue(roundTrip(dropped).rebuilding());
    }

    @Test
    void aReportGoesWithACheckedStatusOnly() {
        assertThrows(IllegalArgumentException.class, () -> new Entry(Status.CHECKED, null, Map.of(), AssemblyReport.NONE));
        assertThrows(IllegalArgumentException.class, () -> new Entry(Status.NOT_VALIDATED, report(State.OK), Map.of(), AssemblyReport.NONE));
        assertThrows(IllegalArgumentException.class, () -> new Entry(Status.CHECKED, report(State.WRONG_KIND), Map.of(), PIPE));
    }
}
