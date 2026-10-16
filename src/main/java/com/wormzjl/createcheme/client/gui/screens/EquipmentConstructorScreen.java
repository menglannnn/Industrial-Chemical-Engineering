package com.wormzjl.createcheme.client.gui.screens;

import com.wormzjl.createcheme.client.EquipmentGhost;
import com.wormzjl.createcheme.client.gui.common.ColumnDiagram;
import com.wormzjl.createcheme.client.gui.common.ProcessUi;
import com.wormzjl.createcheme.network.EquipmentNetwork;
import com.wormzjl.createcheme.science.equipment.ColumnTrays;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.science.equipment.VesselPlacement;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentConstructorBlockEntity;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentConstructorBlockEntity.Design;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

/**
 * The equipment constructor's screen: first what to build (a vertical drum, a horizontal drum or a distillation column;
 * owner request 2026-10-16), then its design, in a panel on the left beside the world and the vessel's ghost. It edits a
 * local draft; the server receives the design when the screen closes, or with a build request on Build. A column is
 * designed here by its trays and feed, or imported whole (draws, pumparounds and steam too) from the nearest column
 * calculator; its schematic is the calculator's ({@link ColumnDiagram}).
 */
public final class EquipmentConstructorScreen extends Screen {
    /** Panel layout in GUI pixels, top-down, so it fits a 240-pixel-high GUI (854×480 at scale 2). */
    /** The schematic strip's width: the steam lines (90 pixels left of the axis) to the draw marks (39 right of it), and margins. */
    private static final int STRIP = 140;
    private static final int PANEL = 196, TOP = 24, ROW = 17, ROWS = 8, LINE = 11, LINES = TOP + ROWS * ROW + 4, STATUS = LINES + 3 * LINE + 2, BUTTONS = STATUS + 14;
    /** The choice page: one button and up to three lines of description per kind of equipment. */
    private static final int CHOICE = 40, CHOICE_PITCH = 56;
    private static final String KEY = "gui.createcheme.equipment_constructor.";

    /** What the constructor builds; a horizontal drum lies along or across the constructor's facing. */
    private enum Equipment {
        VERTICAL_DRUM, HORIZONTAL_DRUM, COLUMN;

        static Equipment of(VesselPlacement.Form form) {
            return form.column() ? COLUMN : form.orientation() == EquipmentShape.Orientation.HORIZONTAL ? HORIZONTAL_DRUM : VERTICAL_DRUM;
        }

        String key() {
            return KEY + "equipment." + name().toLowerCase(Locale.ROOT);
        }
    }

    private final BlockPos position;
    private final Direction facing;
    private Design saved;
    /** On the choice page; the screen opens there. */
    private boolean choosing = true;
    private VesselPlacement.Form form;
    /** Diameter in half metres, so every step stays on the D5 grid. */
    private int halfMetres, tangentLength, support, forward, right, up;
    private ColumnTrays trays;
    private ShapeRasterizer.Layout layout;
    /** Set by Import until the constructor's synced design changes; the draft then takes it. */
    private @Nullable Design importing;
    private boolean sent;

    public EquipmentConstructorScreen(BlockPos position, Direction facing, Design design) {
        super(Component.translatable("block.createcheme.equipment_constructor"));
        this.position = position.immutable();
        this.facing = facing;
        load(design);
    }

    private void load(Design design) {
        saved = design;
        form = design.form();
        halfMetres = (int) Math.round(design.diameter() * 2);
        tangentLength = design.tangentLength();
        support = Math.max(design.support(), minimumSupport());
        forward = design.placement().forward();
        right = design.placement().right();
        up = design.placement().up();
        trays = design.trays();
        layout = ShapeRasterizer.layout(design().shape(facing));
    }

    /** The draft; always valid, because every stepper is clamped to the bounds of its parameter. */
    public Design design() {
        return new Design(halfMetres / 2.0, tangentLength, form, support, new VesselPlacement(forward, right, up), trays);
    }

    /** The vessel the draft would build, for the ghost; null on the choice page and once the constructor is gone. */
    public @Nullable EquipmentStructure.Vessel vessel() {
        var constructor = constructor();
        return constructor != null && !choosing ? constructor.vessel(design()) : null;
    }

    private @Nullable EquipmentConstructorBlockEntity constructor() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(position) instanceof EquipmentConstructorBlockEntity constructor ? constructor : null;
    }

    @Override
    protected void init() {
        if (choosing) {
            var current = Equipment.of(form);
            for (var equipment : Equipment.values()) {
                Component name = Component.translatable(equipment.key());
                if (equipment == current) name = Component.translatable(KEY + "current", name);
                addRenderableWidget(Button.builder(name, b -> choose(equipment)).bounds(8, CHOICE + equipment.ordinal() * CHOICE_PITCH, PANEL - 16, 20).build());
            }
            addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose()).bounds(100, BUTTONS, 88, 20).build());
            return;
        }
        var change = Button.builder(Component.translatable(KEY + "change"), b -> {
            choosing = true;
            rebuildWidgets();
        }).bounds(PANEL - 80, TOP - 1, 74, 16).build();
        change.setTooltip(Tooltip.create(Component.translatable(KEY + "change.hint")));
        addRenderableWidget(change);
        stepper(1, step -> diameter(halfMetres + step));
        int next = 3;
        if (form.column()) {
            stepper(2, step -> trays = trays.withTrays(clamp(trays.trays() + step, 1, ColumnTrays.MAX_TRAYS)));
            stepper(3, step -> trays = trays.withFeed(clamp(trays.feed() + step, 1, trays.trays() + 1)));
            next = 4;
        } else stepper(2, step -> tangentLength = clamp(tangentLength + step, 1, (int) EquipmentShape.MAX_TANGENT_LENGTH));
        if (Equipment.of(form) == Equipment.HORIZONTAL_DRUM) {
            var axis = Button.builder(Component.translatable(KEY + "form." + form.name().toLowerCase(Locale.ROOT)), b -> {
                form = form == VesselPlacement.Form.LENGTHWISE ? VesselPlacement.Form.CROSSWISE : VesselPlacement.Form.LENGTHWISE;
                refresh();
                rebuildWidgets();
            }).bounds(PANEL - 80, TOP + 3 * ROW, 74, 16).build();
            axis.setTooltip(Tooltip.create(Component.translatable(KEY + "axis.hint")));
            addRenderableWidget(axis);
            next = 4;
        }
        stepper(next, step -> support = clamp(support + step, minimumSupport(), EquipmentShape.MAX_SUPPORT));
        stepper(next + 1, step -> forward = clamp(forward + step, 1, VesselPlacement.MAX_FORWARD));
        stepper(next + 2, step -> right = clamp(right + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        stepper(next + 3, step -> up = clamp(up + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        // A column's Import joins Build and Done on the bottom row.
        int buttons = form.column() ? 3 : 2, gap = 4, span = (PANEL - 16 - (buttons - 1) * gap) / buttons, x = 8;
        if (form.column()) {
            var importButton = Button.builder(Component.translatable(KEY + "import"), b -> importColumn()).bounds(x, BUTTONS, span, 20).build();
            importButton.setTooltip(Tooltip.create(Component.translatable(KEY + "import.hint", EquipmentNetwork.IMPORT_REACH)));
            addRenderableWidget(importButton);
            x += span + gap;
        }
        addRenderableWidget(Button.builder(Component.translatable(KEY + "build"), b -> build()).bounds(x, BUTTONS, span, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose()).bounds(x + span + gap, BUTTONS, span, 20).build());
    }

    /** A row of [−] value [+]; shift steps by five. */
    private void stepper(int row, IntConsumer change) {
        int y = TOP + row * ROW;
        var hint = Tooltip.create(Component.translatable(KEY + "shift"));
        var minus = Button.builder(Component.literal("−"), b -> edit(change, -1)).bounds(PANEL - 80, y, 20, 16).build();
        var plus = Button.builder(Component.literal("+"), b -> edit(change, 1)).bounds(PANEL - 26, y, 20, 16).build();
        minus.setTooltip(hint);
        plus.setTooltip(hint);
        addRenderableWidget(minus);
        addRenderableWidget(plus);
    }

    private void edit(IntConsumer change, int direction) {
        change.accept(direction * (hasShiftDown() ? 5 : 1));
        refresh();
    }

    /** A skirt left at its default follows the diameter's default; any skirt stays tall enough to enclose the bottom head. */
    private void diameter(int value) {
        boolean standing = form.orientation() == EquipmentShape.Orientation.VERTICAL;
        boolean followed = standing && support == EquipmentShape.defaultSupport(halfMetres / 2.0, EquipmentShape.Orientation.VERTICAL);
        halfMetres = clamp(value, (int) (EquipmentShape.MIN_DIAMETER * 2), (int) (maximumDiameter() * 2));
        support = followed ? EquipmentShape.defaultSupport(halfMetres / 2.0, EquipmentShape.Orientation.VERTICAL) : Math.max(support, minimumSupport());
    }

    private double maximumDiameter() {
        return form.column() ? EquipmentShape.MAX_COLUMN_DIAMETER : EquipmentShape.MAX_DIAMETER;
    }

    /**
     * Goes to the design of {@code equipment}, keeping the draft's sizes; a horizontal drum keeps its axis. A change
     * between standing and lying proposes that support's default height.
     */
    private void choose(Equipment equipment) {
        var before = form.orientation();
        form = switch (equipment) {
            case VERTICAL_DRUM -> VesselPlacement.Form.VERTICAL;
            case HORIZONTAL_DRUM -> Equipment.of(form) == Equipment.HORIZONTAL_DRUM ? form : VesselPlacement.Form.LENGTHWISE;
            case COLUMN -> VesselPlacement.Form.COLUMN;
        };
        halfMetres = Math.min(halfMetres, (int) (maximumDiameter() * 2));
        if (before != form.orientation()) support = EquipmentShape.defaultSupport(halfMetres / 2.0, form.orientation());
        support = Math.max(support, minimumSupport());
        choosing = false;
        refresh();
        rebuildWidgets();
    }

    private void importColumn() {
        var constructor = constructor();
        if (constructor == null) return;
        importing = constructor.design();
        EquipmentNetwork.requestImport(position);
    }

    private int minimumSupport() {
        return form.orientation() == EquipmentShape.Orientation.VERTICAL ? EquipmentShape.minimumSkirt(halfMetres / 2.0) : 1;
    }

    private void refresh() {
        var shape = design().shape(facing);
        if (!shape.equals(layout.shape())) layout = ShapeRasterizer.layout(shape);
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    private void build() {
        EquipmentNetwork.send(position, design(), true);
        sent = true;
        onClose();
    }

    @Override
    public void tick() {
        var constructor = constructor();
        if (constructor == null) {
            onClose();
            return;
        }
        // An import arrives as the constructor's synced design: the draft takes it whole.
        if (importing != null && !constructor.design().equals(importing)) {
            importing = null;
            load(constructor.design());
            rebuildWidgets();
        }
    }

    /** Closing keeps the draft as the constructor's design (unless Build already sent it). */
    @Override
    public void removed() {
        if (!sent && !design().equals(saved)) EquipmentNetwork.send(position, design(), false);
        sent = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** No blur: the ghost in the world is half of this screen. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, PANEL, height, 0xE0202A31);
        graphics.fill(PANEL, 0, PANEL + 1, height, ProcessUi.LINE);
        if (!choosing && form.column()) graphics.fill(width - 4 - STRIP, 20, width - 4, height - 20, 0xC0202A31);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, 8, 8, ProcessUi.TEXT);
        if (choosing) {
            choices(graphics);
            return;
        }
        var equipment = Equipment.of(form);
        String[] names = switch (equipment) {
            case VERTICAL_DRUM -> new String[]{"", "diameter", "tangent_length", "skirt", "forward", "right", "up"};
            case HORIZONTAL_DRUM -> new String[]{"", "diameter", "tangent_length", "axis", "saddles", "forward", "right", "up"};
            case COLUMN -> new String[]{"", "diameter", "trays", "feed", "skirt", "forward", "right", "up"};
        };
        String place = Integer.toString(forward), shift = Integer.toString(right), lift = Integer.toString(up), across = number(halfMetres / 2.0) + " m";
        String[] values = switch (equipment) {
            case VERTICAL_DRUM -> new String[]{"", across, tangentLength + " m", support + " m", place, shift, lift};
            case HORIZONTAL_DRUM -> new String[]{"", across, tangentLength + " m", "", support + " m", place, shift, lift};
            case COLUMN -> new String[]{"", across, Integer.toString(trays.trays()), Integer.toString(trays.feed()), support + " m", place, shift, lift};
        };
        graphics.drawString(font, Component.translatable(equipment.key()), 8, TOP + 4, ProcessUi.ACCENT);
        for (int row = 1; row < names.length; row++) {
            int y = TOP + row * ROW + 4;
            graphics.drawString(font, Component.translatable(KEY + names[row]), 8, y, ProcessUi.TEXT);
            if (!values[row].isEmpty()) graphics.drawCenteredString(font, values[row], PANEL - 43, y, ProcessUi.ACCENT);
        }
        var shape = layout.shape();
        int y = LINES;
        for (var line : new Component[]{
                Component.translatable(KEY + "extent", number(shape.topElevation()), number(shape.overallLength())),
                Component.translatable(KEY + "volume", number(shape.volume()), number(shape.surfaceArea())),
                Component.translatable(KEY + "cells", layout.shell().size(), layout.interior().size())}) {
            graphics.drawString(font, line, 8, y, ProcessUi.MUTED);
            y += LINE;
        }
        int blocked = EquipmentGhost.blockedCount();
        var status = importing != null ? Component.translatable(KEY + "importing")
                : EquipmentGhost.standing() ? Component.translatable(KEY + "standing")
                : blocked == 0 ? Component.translatable(KEY + "clear") : Component.translatable(KEY + "blocked", blocked);
        graphics.drawString(font, status, 8, STATUS, blocked == 0 && !EquipmentGhost.standing() ? ProcessUi.ACCENT : ProcessUi.WARN);
        if (form.column()) schematic(graphics);
    }

    /** The choice page: what each kind of equipment is, under its button (the draft's kind is marked on it). */
    private void choices(GuiGraphics g) {
        g.drawString(font, Component.translatable(KEY + "choose"), 8, TOP + 4, ProcessUi.TEXT);
        for (var equipment : Equipment.values()) {
            int y = CHOICE + equipment.ordinal() * CHOICE_PITCH + 23;
            var lines = font.split(Component.translatable(equipment.key() + ".hint"), PANEL - 16);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), 8, y + i * 10, ProcessUi.MUTED);
        }
    }

    /**
     * The column's trays, feed, side draws, pumparounds and steam, drawn as the column calculator draws them, inside the
     * right strip: the draw marks end 4 pixels inside its right edge, the steam lines start at least 7 inside its left one.
     */
    private void schematic(GuiGraphics g) {
        int x = width - 8 - ColumnDiagram.MARK - 13, top = 32, bottom = height - 54, n = trays.trays();
        double pitch = (bottom - top - 34) / (double) Math.max(1, n);
        IntUnaryOperator at = k -> top + 14 + (int) Math.round((k - 1) * pitch);
        ColumnDiagram.shell(g, x, top, bottom, 0xFF2B3740, ProcessUi.LINE);
        for (int k = 1; k <= n; k++) if (pitch >= 3 || k % 5 == 0 || k == 1 || k == n) ColumnDiagram.tray(g, x, at.applyAsInt(k), ProcessUi.MUTED);
        int fy = at.applyAsInt(trays.feed());
        ColumnDiagram.feed(g, x, x - 70, fy, ProcessUi.ACCENT);
        g.drawString(font, Component.translatable(KEY + "schematic.feed", trays.feed()), x - 70, fy - 10, ProcessUi.ACCENT);
        for (int draw : trays.draws()) ColumnDiagram.draw(g, x, at.applyAsInt(draw), ProcessUi.ACCENT);
        for (int i = 0; i < trays.pumparounds().size(); i++) {
            var p = trays.pumparounds().get(i);
            ColumnDiagram.pumparound(g, x, i, at.applyAsInt(p.draw()), at.applyAsInt(p.ret()), ProcessUi.WARN);
        }
        for (int i = 0; i < trays.steam().size(); i++) ColumnDiagram.steam(g, x, i, at.applyAsInt(trays.steam().get(i)), ProcessUi.WARN);
        int left = width - STRIP;
        g.drawString(font, Component.translatable(KEY + "schematic.draws", trays.draws().size()), left, height - 46, ProcessUi.MUTED);
        g.drawString(font, Component.translatable(KEY + "schematic.extras", trays.pumparounds().size(), trays.steam().size()), left, height - 35, ProcessUi.MUTED);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
