package com.wormzjl.icheme.client.gui.common;

import com.wormzjl.icheme.client.gui.common.AssemblyDiagram.PortMark;
import com.wormzjl.icheme.science.assembly.Blueprint;
import com.wormzjl.icheme.science.assembly.ColumnBlueprint;
import com.wormzjl.icheme.science.equipment.ColumnComponents;
import com.wormzjl.icheme.science.equipment.ColumnComponents.Condenser;
import com.wormzjl.icheme.science.equipment.ColumnComponents.Pump;
import com.wormzjl.icheme.science.equipment.ColumnTrays;
import com.wormzjl.icheme.science.equipment.ColumnTrays.Pumparound;
import com.wormzjl.icheme.science.equipment.EquipmentShape.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The column GUI's drawing over the design table (batch 2026-10-26-multi-machine-framework, plan sections 5.5 and 8):
 * one part per slot of the blueprint, inside the drawing, none over another; a square for every port a line starts from
 * or ends at.
 */
class AssemblyDiagramTest {
    private static final int TRAYS = 20, SUMP = TRAYS + 1;

    private static List<ColumnTrays> designs() {
        var steams = List.<List<Integer>>of(List.of(), List.of(18), List.of(17, 18), List.of(SUMP), List.of(18, SUMP));
        var out = new ArrayList<ColumnTrays>();
        for (var condenser : Condenser.values())
            for (boolean ejector : new boolean[]{false, true})
                for (boolean kettle : new boolean[]{false, true})
                    for (var steam : steams) {
                        // Without a kettle the sump takes steam (the calculator's requirement rules).
                        if (!kettle && !steam.contains(SUMP)) continue;
                        for (var reflux : Pump.values())
                            for (int draws = 0; draws <= ColumnTrays.MAX_DRAWS; draws++)
                                for (int n = 0; n <= ColumnTrays.MAX_PUMPAROUNDS; n++)
                                    for (var pa : Pump.values()) {
                                        var pumps = new ArrayList<Pump>();
                                        var pas = new ArrayList<Pumparound>();
                                        for (int i = 0; i < n; i++) {
                                            // Each pump of the other kind than the one before it, starting with pa.
                                            pumps.add(i % 2 == 0 ? pa : pa == Pump.STANDARD ? Pump.IN_LINE : Pump.STANDARD);
                                            pas.add(new Pumparound(4 + 4 * i, 2 + 4 * i));
                                        }
                                        out.add(new ColumnTrays(TRAYS, 10, List.of(3, 6, 9, 12).subList(0, draws), steam, pas,
                                                new ColumnComponents(condenser, reflux, pumps, ejector, kettle)));
                                    }
                    }
        return out;
    }

    @Test
    void everySlotHasOnePartInsideTheDrawingAndNoPartCoversAnother() {
        int checked = 0;
        for (var trays : designs()) {
            var blueprint = ColumnBlueprint.of(trays);
            var diagram = AssemblyDiagram.of(trays);
            var parts = diagram.parts();
            assertEquals(blueprint.slots().size(), parts.size(), trays::toString);
            assertEquals(new HashSet<>(blueprint.slots()), new HashSet<>(parts.stream().map(AssemblyDiagram.Part::slot).toList()), trays::toString);
            for (var p : parts) {
                assertTrue(p.x() >= 0 && p.y() >= 0 && p.x() + p.w() <= AssemblyDiagram.WIDTH && p.y() + p.h() <= AssemblyDiagram.HEIGHT, p::toString);
                for (var q : parts)
                    if (p != q)
                        assertFalse(p.x() < q.x() + q.w() && q.x() < p.x() + p.w() && p.y() < q.y() + q.h() && q.y() < p.y() + p.h(), () -> p + " over " + q);
            }
            for (var w : diagram.wires())
                for (int i = 0; i + 3 < w.points().length; i += 2)
                    assertTrue(w.points()[i] == w.points()[i + 2] || w.points()[i + 1] == w.points()[i + 3], "lines run across or down");
            checked++;
        }
        assertEquals(2800, checked);
    }

    @Test
    void everyPortALineStartsFromOrEndsAtHasASquare() {
        for (var trays : designs()) {
            var blueprint = ColumnBlueprint.of(trays);
            var diagram = AssemblyDiagram.of(trays);
            var marks = new HashSet<Blueprint.PortSpec>();
            for (var m : diagram.ports()) {
                marks.add(new Blueprint.PortSpec(m.slot(), m.role(), Math.max(0, m.ordinal())));
                assertTrue(blueprint.slots().contains(m.slot()), m::toString);
                if (m.partner() != null) assertTrue(blueprint.slots().contains(m.partner()), m::toString);
                else assertEquals(new PortMark(ColumnBlueprint.COLUMN, Role.VAPOUR_RETURN, 0, m.x(), m.y(), null), m, "only a blind port has no partner");
            }
            for (var link : blueprint.links()) {
                var start = link.start();
                if (start.role() != null) assertTrue(marks.contains(new Blueprint.PortSpec(start.slot(), start.role(), start.ordinal())), () -> link.name() + " " + trays);
                if (link.finish() instanceof Blueprint.ToPort to) assertTrue(marks.contains(to.port()), () -> link.name() + " " + trays);
            }
            for (var blind : blueprint.blind()) assertTrue(marks.contains(blind), blind::toString);
        }
    }
}
