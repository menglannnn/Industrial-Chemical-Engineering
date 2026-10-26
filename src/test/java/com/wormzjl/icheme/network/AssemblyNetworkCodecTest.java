package com.wormzjl.icheme.network;

import com.wormzjl.icheme.science.assembly.AssemblyReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortState;
import com.wormzjl.icheme.science.assembly.AssemblyReport.SlotReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.State;
import com.wormzjl.icheme.science.assembly.AssemblyView;
import com.wormzjl.icheme.science.assembly.ColumnBlueprint;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The assembly protocol's wire (batch 2026-10-26-multi-machine-framework, plan section 11): a view crosses whole but for
 * its footprint, which no client reads; a read refuses counts out of range.
 */
class AssemblyNetworkCodecTest {
    private static final long COLUMN = new BlockPos(120, 64, -30).asLong(), DRUM = new BlockPos(131, 80, -30).asLong();

    private static AssemblyView roundTrip(AssemblyView view) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var payload = new AssemblyNetwork.StatePayload(7, view);
            AssemblyNetwork.StatePayload.STREAM_CODEC.encode(buffer, payload);
            var read = AssemblyNetwork.StatePayload.STREAM_CODEC.decode(buffer);
            assertEquals(7, read.container());
            assertEquals(0, buffer.readableBytes());
            return read.view();
        } finally {
            buffer.release();
        }
    }

    @Test
    void aViewCrossesWholeButForItsFootprint() {
        var report = new AssemblyReport(List.of(new SlotReport(ColumnBlueprint.COLUMN, State.OK, COLUMN, AssemblyReport.NONE, ""),
                new SlotReport(ColumnBlueprint.RECEIVER, State.WRONG_KIND, DRUM, DRUM, "DRUM_STANDING"),
                new SlotReport(ColumnBlueprint.drawValve(2), State.OPEN_END, AssemblyReport.NONE, new BlockPos(-5, -60, 7).asLong(), "")),
                List.of(new PortReport(COLUMN, Role.VAPOUR_OUTLET, 0, PortState.CONNECTED), new PortReport(DRUM, Role.SIDE, 3, PortState.UNUSED)),
                Set.of(1L, 2L, 3L), List.of(COLUMN, DRUM));
        var checked = new AssemblyView(AssemblyView.Status.CHECKED, report, AssemblyReport.NONE);
        var read = roundTrip(checked);
        assertEquals(new AssemblyView(AssemblyView.Status.CHECKED, new AssemblyReport(report.slots(), report.ports(), Set.of(), report.members()), AssemblyReport.NONE), read);
        assertEquals(1, read.report().connected());
        assertEquals(1, read.report().misplaced());
        assertEquals(1, read.report().missing());
        var reverted = new AssemblyView(AssemblyView.Status.NOT_VALIDATED, null, DRUM);
        assertEquals(reverted, roundTrip(reverted));
        assertEquals(AssemblyView.NO_DESIGN, roundTrip(AssemblyView.NO_DESIGN));
    }

    @Test
    void aReadRefusesCountsOutOfRange() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeVarInt(1);
            buffer.writeEnum(AssemblyView.Status.CHECKED);
            buffer.writeLong(AssemblyReport.NONE);
            buffer.writeVarInt(AssemblyNetwork.MAX_SLOTS + 1);
            assertThrows(DecoderException.class, () -> AssemblyNetwork.StatePayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
