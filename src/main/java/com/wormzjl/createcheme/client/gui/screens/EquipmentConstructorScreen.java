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
 * The equipment constructor's design screen: a panel on the left, the world and the vessel's ghost on the right. It
 * edits a local draft; the server receives the design when the screen closes, or with a build request on Build. A
 * column is designed here by its trays and feed, or imported whole (draws, pumparounds and steam too) from the nearest
 * column calculator; its schematic is the calculator's ({@link ColumnDiagram}).
 */
public final class EquipmentConstructorScreen extends Screen {
    /** Panel layout in GUI pixels, top-down, so it fits a 240-pixel-high GUI (854×480 at scale 2). */
    /** The schematic strip's width: the steam lines (90 pixels left of the axis) to the draw marks (39 right of it), and margins. */
    private static final int STRIP = 140;
    private static final int PANEL = 196, TOP = 24, ROW = 17, ROWS = 8, LINE = 11, LINES = TOP + ROWS * ROW + 4, STATUS = LINES + 3 * LINE + 2, BUTTONS = STATUS + 14;
    private static final String KEY = "gui.createcheme.equipment_constructor.";
    private final BlockPos position;
    private final Direction facing;
    private Design saved;
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

    /** The vessel the draft would build, for the ghost; null once the constructor is gone. */
    public @Nullable EquipmentStructure.Vessel vessel() {
        var constructor = constructor();
        return constructor != null ? constructor.vessel(design()) : null;
    }

    private @Nullable EquipmentConstructorBlockEntity constructor() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(position) instanceof EquipmentConstructorBlockEntity constructor ? constructor : null;
    }

    @Override
    protected void init() {
        var formButton = Button.builder(formName(), b -> cycleForm()).bounds(PANEL - 80, TOP - 1, 74, 16).build();
        formButton.setTooltip(Tooltip.create(Component.translatable(KEY + "form.hint")));
        addRenderableWidget(formButton);
        var importButton = Button.builder(Component.translatable(KEY + "import"), b -> importColumn()).bounds(PANEL - 80, 4, 74, 16).build();
        importButton.setTooltip(Tooltip.create(Component.translatable(KEY + "import.hint", EquipmentNetwork.IMPORT_REACH)));
        addRenderableWidget(importButton);
        stepper(1, step -> diameter(halfMetres + step));
        if (form.column()) {
            stepper(2, step -> trays = trays.withTrays(clamp(trays.trays() + step, 1, ColumnTrays.MAX_TRAYS)));
            stepper(3, step -> trays = trays.withFeed(clamp(trays.feed() + step, 1, trays.trays() + 1)));
        } else stepper(2, step -> tangentLength = clamp(tangentLength + step, 1, (int) EquipmentShape.MAX_TANGENT_LENGTH));
        int next = form.column() ? 4 : 3;
        stepper(next, step -> support = clamp(support + step, minimumSupport(), EquipmentShape.MAX_SUPPORT));
        stepper(next + 1, step -> forward = clamp(forward + step, 1, VesselPlacement.MAX_FORWARD));
        stepper(next + 2, step -> right = clamp(right + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        stepper(next + 3, step -> up = clamp(up + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        addRenderableWidget(Button.builder(Component.translatable(KEY + "build"), b -> build()).bounds(8, BUTTONS, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose()).bounds(100, BUTTONS, 88, 20).build());
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

    /** Vertical, lengthwise, crosswise, column; a change between standing and lying proposes that support's default height. */
    private void cycleForm() {
        var before = form.orientation();
        form = form.next();
        halfMetres = Math.min(halfMetres, (int) (maximumDiameter() * 2));
        if (before != form.orientation()) support = EquipmentShape.defaultSupport(halfMetres / 2.0, form.orientation());
        support = Math.max(support, minimumSupport());
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

    private Component formName() {
        return Component.translatable(KEY + "form." + form.name().toLowerCase(Locale.ROOT));
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
        if (form.column()) graphics.fill(width - 4 - STRIP, 20, width - 4, height - 20, 0xC0202A31);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, 8, 8, ProcessUi.TEXT);
        String[] names = form.column()
                ? new String[]{"form", "diameter", "trays", "feed", "skirt", "forward", "right", "up"}
                : new String[]{"form", "diameter", "tangent_length", form == VesselPlacement.Form.VERTICAL ? "skirt" : "saddles", "forward", "right", "up"};
        String[] values = form.column()
                ? new String[]{"", number(halfMetres / 2.0) + " m", Integer.toString(trays.trays()), Integer.toString(trays.feed()), support + " m",
                Integer.toString(forward), Integer.toString(right), Integer.toString(up)}
                : new String[]{"", number(halfMetres / 2.0) + " m", tangentLength + " m", support + " m", Integer.toString(forward), Integer.toString(right), Integer.toString(up)};
        for (int row = 0; row < names.length; row++) {
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
