package com.wormzjl.createcheme.client.gui.screens;

import com.wormzjl.createcheme.client.EquipmentGhost;
import com.wormzjl.createcheme.client.gui.common.ProcessUi;
import com.wormzjl.createcheme.network.EquipmentNetwork;
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

/**
 * The equipment constructor's design screen: a panel on the left, the world and the vessel's ghost on the right. It
 * edits a local draft; the server receives the design when the screen closes, or with a build request on Build.
 */
public final class EquipmentConstructorScreen extends Screen {
    /** Panel layout in GUI pixels, top-down, so it fits a 240-pixel-high GUI (854×480 at scale 2). */
    private static final int PANEL = 196, TOP = 24, ROW = 19, ROWS = 7, LINE = 11, LINES = TOP + ROWS * ROW + 4, STATUS = LINES + 3 * LINE + 2, BUTTONS = STATUS + 14;
    private static final String KEY = "gui.createcheme.equipment_constructor.";
    private final BlockPos position;
    private final Direction facing;
    private final Design saved;
    private VesselPlacement.Form form;
    /** Diameter in half metres, so every step stays on the D5 grid. */
    private int halfMetres, tangentLength, support, forward, right, up;
    private ShapeRasterizer.Layout layout;
    private @Nullable Button formButton;
    private boolean sent;

    public EquipmentConstructorScreen(BlockPos position, Direction facing, Design design) {
        super(Component.translatable("block.createcheme.equipment_constructor"));
        this.position = position.immutable();
        this.facing = facing;
        saved = design;
        form = design.form();
        halfMetres = (int) Math.round(design.diameter() * 2);
        tangentLength = design.tangentLength();
        support = Math.max(design.support(), minimumSupport());
        forward = design.placement().forward();
        right = design.placement().right();
        up = design.placement().up();
        layout = ShapeRasterizer.layout(design().shape(facing));
    }

    /** The draft; always valid, because every stepper is clamped to the bounds of its parameter. */
    public Design design() {
        return new Design(halfMetres / 2.0, tangentLength, form, support, new VesselPlacement(forward, right, up));
    }

    /** The vessel the draft would build, for the ghost; null once the constructor is gone. */
    public @Nullable EquipmentStructure.Vessel vessel() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(position) instanceof EquipmentConstructorBlockEntity constructor
                ? constructor.vessel(design()) : null;
    }

    @Override
    protected void init() {
        formButton = Button.builder(formName(), b -> cycleForm()).bounds(PANEL - 80, TOP, 74, 18).build();
        formButton.setTooltip(Tooltip.create(Component.translatable(KEY + "form.hint")));
        addRenderableWidget(formButton);
        stepper(1, step -> diameter(halfMetres + step));
        stepper(2, step -> tangentLength = clamp(tangentLength + step, 1, (int) EquipmentShape.MAX_TANGENT_LENGTH));
        stepper(3, step -> support = clamp(support + step, minimumSupport(), EquipmentShape.MAX_SUPPORT));
        stepper(4, step -> forward = clamp(forward + step, 1, VesselPlacement.MAX_FORWARD));
        stepper(5, step -> right = clamp(right + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        stepper(6, step -> up = clamp(up + step, -VesselPlacement.MAX_SHIFT, VesselPlacement.MAX_SHIFT));
        addRenderableWidget(Button.builder(Component.translatable(KEY + "build"), b -> build()).bounds(8, BUTTONS, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(KEY + "done"), b -> onClose()).bounds(100, BUTTONS, 88, 20).build());
    }

    /** A row of [−] value [+]; shift steps by five. */
    private void stepper(int row, IntConsumer change) {
        int y = TOP + row * ROW;
        var hint = Tooltip.create(Component.translatable(KEY + "shift"));
        var minus = Button.builder(Component.literal("−"), b -> edit(change, -1)).bounds(PANEL - 80, y, 20, 18).build();
        var plus = Button.builder(Component.literal("+"), b -> edit(change, 1)).bounds(PANEL - 26, y, 20, 18).build();
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
        boolean followed = form == VesselPlacement.Form.VERTICAL && support == EquipmentShape.defaultSupport(halfMetres / 2.0, EquipmentShape.Orientation.VERTICAL);
        halfMetres = clamp(value, (int) (EquipmentShape.MIN_DIAMETER * 2), (int) (EquipmentShape.MAX_DIAMETER * 2));
        support = followed ? EquipmentShape.defaultSupport(halfMetres / 2.0, EquipmentShape.Orientation.VERTICAL) : Math.max(support, minimumSupport());
    }

    /** Vertical, lengthwise, crosswise; a change between standing and lying proposes that support's default height. */
    private void cycleForm() {
        boolean lying = form != VesselPlacement.Form.VERTICAL;
        form = form.next();
        if (lying != (form != VesselPlacement.Form.VERTICAL)) support = EquipmentShape.defaultSupport(halfMetres / 2.0, form.orientation(facing.getStepX(), facing.getStepZ()));
        if (formButton != null) formButton.setMessage(formName());
        refresh();
    }

    private int minimumSupport() {
        return form == VesselPlacement.Form.VERTICAL ? EquipmentShape.minimumSkirt(halfMetres / 2.0) : 1;
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
        if (vessel() == null) onClose();
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
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, 8, 8, ProcessUi.TEXT);
        String[] names = {"form", "diameter", "tangent_length", form == VesselPlacement.Form.VERTICAL ? "skirt" : "saddles", "forward", "right", "up"};
        String[] values = {"", number(halfMetres / 2.0) + " m", tangentLength + " m", support + " m", Integer.toString(forward), Integer.toString(right), Integer.toString(up)};
        for (int row = 0; row < names.length; row++) {
            int y = TOP + row * ROW + 5;
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
        var status = EquipmentGhost.standing() ? Component.translatable(KEY + "standing")
                : blocked == 0 ? Component.translatable(KEY + "clear") : Component.translatable(KEY + "blocked", blocked);
        graphics.drawString(font, status, 8, STATUS, blocked == 0 && !EquipmentGhost.standing() ? ProcessUi.ACCENT : ProcessUi.WARN);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
