package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.equipment.ShapeRasterizer.Cell;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer.Layout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A placed vessel brought back to its kind's current layout (owner request 2026-10-18: "when loading an old save,
 * updated models are replaced by new ones", an exception to the no-compatibility rule recorded in AGENTS.md). A cell
 * draws from the code at every load, so only the set of cells can be stale: the cells a new layout adds are missing,
 * those it drops still stand, and the box may have grown or shrunk. The world notices a stale vessel by its cells'
 * {@link ShapeRasterizer#stamp}. A refit stays on its foundation: the new layout stands where it covers the most of
 * what the vessel already holds, so its body stays and only the parts the layout redrew change, whichever side of the
 * box they grew or shrank on. The plan then clears the vessel's cells the layout no longer has, places the cells it
 * lacks where the world is free, and keeps the rest. A layout cell another block stands in is left out (the vessel
 * works without it), never overwritten.
 */
public final class EquipmentRefresh {
    /** How far along x and z the new layout's box may stand from the old one's, blocks: more than any layout change moves a body. */
    public static final int REACH = 3;

    /** What stands in a cell of the region the vessel spans: one of its shell or interior cells, or another block. Free cells are absent. */
    public enum Holding { SHELL, INTERIOR, OTHER }

    /**
     * Where the new box stands from the old origin ({@code shift}, along x and z only: the vessel stays at its grade), the
     * vessel's cells to clear, the shell and interior cells to place, and the layout's cells another block stands in;
     * every list from the new origin (old origin + shift), in x, y, z order.
     */
    public record Plan(Cell shift, List<Cell> clear, List<Cell> shell, List<Cell> interior, List<Cell> blocked) {
        public boolean changes() {
            return shift.x() != 0 || shift.z() != 0 || !clear.isEmpty() || !shell.isEmpty() || !interior.isEmpty();
        }
    }

    private static final Comparator<Cell> ORDER = Comparator.comparingInt(Cell::x).thenComparingInt(Cell::y).thenComparingInt(Cell::z);

    private EquipmentRefresh() {
    }

    /** The plan that brings {@code world} (what stands in each cell of the region, from the old origin, free cells absent) to {@code layout}. */
    public static Plan plan(Layout layout, Map<Cell, Holding> world) {
        var shift = anchor(layout, world);
        var shell = new HashSet<>(layout.shell());
        var interior = new HashSet<>(layout.interior());
        var clear = new ArrayList<Cell>();
        for (var entry : world.entrySet()) {
            var cell = minus(entry.getKey(), shift);
            if (entry.getValue() == Holding.SHELL && !shell.contains(cell) || entry.getValue() == Holding.INTERIOR && !interior.contains(cell)) clear.add(cell);
        }
        var placeShell = new ArrayList<Cell>();
        var placeInterior = new ArrayList<Cell>();
        var blocked = new ArrayList<Cell>();
        for (var cell : layout.shell()) {
            var held = world.get(plus(cell, shift));
            if (held == Holding.OTHER) blocked.add(cell);
            else if (held != Holding.SHELL) placeShell.add(cell);
        }
        for (var cell : layout.interior()) {
            var held = world.get(plus(cell, shift));
            if (held == Holding.OTHER) blocked.add(cell);
            else if (held != Holding.INTERIOR) placeInterior.add(cell);
        }
        for (var list : List.of(clear, placeShell, placeInterior, blocked)) list.sort(ORDER);
        return new Plan(shift, List.copyOf(clear), List.copyOf(placeShell), List.copyOf(placeInterior), List.copyOf(blocked));
    }

    /**
     * The shift, within {@link #REACH} along x and z, that puts the most of the layout's cells on cells the vessel holds
     * as the same kind; ties go to the smaller shift, then to the lower x and z. None when the layout fits where it stands.
     */
    static Cell anchor(Layout layout, Map<Cell, Holding> world) {
        var best = new Cell(0, 0, 0);
        int most = matches(layout, world, best);
        if (most == layout.shell().size() + layout.interior().size()) return best;
        for (int dx = -REACH; dx <= REACH; dx++)
            for (int dz = -REACH; dz <= REACH; dz++) {
                var shift = new Cell(dx, 0, dz);
                int found = matches(layout, world, shift);
                if (found > most || found == most && Math.abs(dx) + Math.abs(dz) < Math.abs(best.x()) + Math.abs(best.z())) {
                    best = shift;
                    most = found;
                }
            }
        return best;
    }

    private static int matches(Layout layout, Map<Cell, Holding> world, Cell shift) {
        int found = 0;
        for (var cell : layout.shell()) if (world.get(plus(cell, shift)) == Holding.SHELL) found++;
        for (var cell : layout.interior()) if (world.get(plus(cell, shift)) == Holding.INTERIOR) found++;
        return found;
    }

    private static Cell plus(Cell a, Cell b) {
        return new Cell(a.x() + b.x(), a.y() + b.y(), a.z() + b.z());
    }

    private static Cell minus(Cell a, Cell b) {
        return new Cell(a.x() - b.x(), a.y() - b.y(), a.z() - b.z());
    }
}
