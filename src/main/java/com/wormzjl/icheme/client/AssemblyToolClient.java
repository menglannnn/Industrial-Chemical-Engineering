package com.wormzjl.icheme.client;

import com.wormzjl.icheme.client.gui.common.AssemblyDiagram;
import com.wormzjl.icheme.client.gui.screens.inventory.AssemblyScreen;
import com.wormzjl.icheme.network.AssemblyNetwork;
import com.wormzjl.icheme.science.assembly.AssemblyReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortReport;
import com.wormzjl.icheme.science.assembly.AssemblyReport.PortState;
import com.wormzjl.icheme.science.assembly.AssemblyReport.SlotReport;
import com.wormzjl.icheme.science.assembly.AssemblyView;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.world.level.block.entity.EquipmentShellBlockEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * The laying tool's view of the column assemblies around the player (batch 2026-10-26-multi-machine-framework, plan
 * section 9, W7). While the player holds the tool the client watches them ({@link AssemblyNetwork.WatchPayload}): the
 * server sends each column's stored view and its changes on the engine's buckets. A watched port gets a frame around the
 * tool's square: in its class colour while its assembly still needs it, yellow on a misplaced part's ports; aiming at it
 * names its line. Render thread only.
 */
public final class AssemblyToolClient {
    /** The watched columns' views, by master. */
    private static final Map<Long, AssemblyView> VIEWS = new TreeMap<>();
    /** The player who watches, or null: a new player (a respawn, another world) starts a new watch. */
    private static @Nullable LocalPlayer watching;

    private AssemblyToolClient() {
    }

    /** Each tick of the laying tool's client: the watch starts when the player takes the tool up and ends when they put it away. */
    static void watch(@Nullable LocalPlayer player, boolean holding) {
        if (player != null && holding) {
            if (watching == player) return;
            VIEWS.clear();
            watching = player;
            PacketDistributor.sendToServer(new AssemblyNetwork.WatchPayload(true));
        } else if (watching != null) {
            boolean connected = player != null && player == watching;
            watching = null;
            VIEWS.clear();
            if (connected) PacketDistributor.sendToServer(new AssemblyNetwork.WatchPayload(false));
        }
    }

    /** A watched column's view, or its end ({@code null}). */
    public static void received(long master, @Nullable AssemblyView view) {
        if (watching == null) return;
        if (view == null) VIEWS.remove(master);
        else VIEWS.put(master, view);
    }

    /** What a watched report says of a port: its master and report, the slot its unit fills, and the port's own state. */
    private record Seen(long master, AssemblyReport report, SlotReport slot, @Nullable PortReport port, EquipmentShape.Role role, int ordinal) {
    }

    private static @Nullable Seen seen(EquipmentShellBlockEntity shell, Direction face) {
        if (VIEWS.isEmpty()) return null;
        var origin = shell.origin();
        var port = shell.port(face);
        var shape = shell.shape();
        if (origin == null || port == null || shape == null) return null;
        var role = shape.nozzles().get(port.nozzle()).role();
        // A port is its unit's nozzle of that role, counted in nozzle order (AssemblyWalker).
        int ordinal = 0;
        for (int k = 0; k < port.nozzle(); k++) if (shape.nozzles().get(k).role() == role) ordinal++;
        long unit = origin.asLong();
        for (var watched : VIEWS.entrySet()) {
            var report = watched.getValue().report();
            if (report == null) continue;
            var slot = report.slots().stream().filter(s -> s.member() == unit).findFirst().orElse(null);
            if (slot == null) continue;
            int n = ordinal;
            var own = report.ports().stream().filter(p -> p.unit() == unit && p.role() == role && p.ordinal() == n).findFirst().orElse(null);
            return new Seen(watched.getKey(), report, slot, own, role, ordinal);
        }
        return null;
    }

    /**
     * The frame around the tool's square on the port of {@code shell} facing {@code face}: yellow on a misplaced part,
     * its class colour while its assembly still needs a pipe on it, or -1 for none.
     */
    static int frame(EquipmentShellBlockEntity shell, Direction face) {
        var seen = seen(shell, face);
        if (seen == null) return -1;
        if (seen.slot.state().colour == AssemblyReport.Colour.YELLOW) return PortColours.MISPLACED;
        var port = shell.port(face);
        return seen.port != null && (seen.port.state() == PortState.NEEDED || seen.port.state() == PortState.UTILITY) && port != null ? PortColours.of(port.port()) : -1;
    }

    /**
     * The assembly line of the port of {@code shell} facing {@code face}, for the tool's label: "Column x, y, z: role → the
     * part it pipes to (its state)"; null when no watched report holds the port, or its design pipes nothing to it.
     */
    static @Nullable Component line(EquipmentShellBlockEntity shell, Direction face) {
        var seen = seen(shell, face);
        var trays = seen == null ? null : trays(seen.master, shell);
        if (trays == null) return null;
        var diagram = AssemblyDiagram.of(trays);
        var mark = diagram.ports().stream().filter(m -> m.slot().equals(seen.slot.slot()) && m.role() == seen.role && (m.ordinal() < 0 || m.ordinal() == seen.ordinal))
                .findFirst().orElse(null);
        var partner = mark == null ? null : diagram.part(mark.partner());
        if (partner == null) return null;
        var partnerReport = seen.report.slots().stream().filter(s -> s.slot().equals(mark.partner())).findFirst().orElse(null);
        return Component.translatable(AssemblyDiagram.KEY + "tool.line", BlockPos.of(seen.master).toShortString(), AssemblyScreen.role(seen.role.name()),
                AssemblyScreen.text(partner.name()), AssemblyScreen.word(partnerReport));
    }

    /** The trays of the column at {@code master}, from a loaded shell cell of it: {@code shell} itself when it is the column's. */
    private static @Nullable ColumnTrays trays(long master, EquipmentShellBlockEntity shell) {
        if (shell.origin() != null && shell.origin().asLong() == master && shell.shape() != null) return shell.shape().trays();
        for (var cell : EquipmentShellBlockEntity.clientPortCells())
            if (!cell.isRemoved() && cell.origin() != null && cell.origin().asLong() == master && cell.shape() != null) return cell.shape().trays();
        return null;
    }
}
