package com.wormzjl.icheme.client.gui.screens;

import com.wormzjl.icheme.client.MachineAnimation;
import com.wormzjl.icheme.client.gui.common.ProcessUi;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.ProcessUnit;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.science.equipment.UnitDesign;
import com.wormzjl.icheme.world.level.block.EquipmentStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.wormzjl.icheme.science.equipment.UnitDesign.number;


/**
 * A column's or a process unit's screen: its datasheet, with its nozzles. A drum shows the same datasheet on a tab of
 * its tank screen (owner request 2026-10-16), from {@link #datasheet}. It shows geometry only, read from the cells the
 * client already has, so it requests nothing.
 */
public final class VesselScreen extends Screen {
    /**
     * The panel's least width (a longer line widens it: a unit's nozzle lines run past 256), the line pitch, and the
     * panel's height besides its lines (the title above them, the buttons below).
     */
    private static final int WIDTH = 256, LINE = 12, FRAME = 56;
    private static final String KEY = "gui.icheme.vessel.";
    private final List<Component> lines;
    private final boolean moving;
    private int left, top, panel, pitch, tall;

    public VesselScreen(EquipmentStructure.Vessel vessel) {
        super(Component.translatable(KEY + "title." + vessel.shape().kind().name().toLowerCase(Locale.ROOT)));
        lines = datasheet(vessel);
        moving = vessel.shape().unit() && ProcessUnit.moves(vessel.shape().kind());
    }

    /** The vessel's datasheet, a line each: its kind first, then its sizes and cells, then each nozzle. */
    public static List<Component> datasheet(EquipmentStructure.Vessel vessel) {
        var shape = vessel.shape();
        var layout = ShapeRasterizer.layout(shape);
        var cell = layout.controller();
        var controller = vessel.origin().offset(cell.x(), cell.y(), cell.z());
        var lines = new ArrayList<Component>();
        if (shape.designed()) {
            lines.addAll(unit(shape));
            // A storage vessel's volume and area, as a drum's.
            if (shape.storage()) lines.addAll(List.of(Component.translatable(KEY + "volume", number(shape.volume())), Component.translatable(KEY + "area", number(shape.surfaceArea()))));
        } else {
            var support = shape.saddles()
                    ? Component.translatable(KEY + "saddles", shape.support(), number(EquipmentShape.SADDLE_POSITION * shape.tangentLength()))
                    : shape.skirt() ? Component.translatable(KEY + "skirt", shape.support()) : Component.translatable(KEY + "unsupported");
            lines.addAll(List.of(
                    shape.column() ? Component.translatable(KEY + "kind.column", shape.trays().trays())
                            : Component.translatable(KEY + (shape.saddles() ? "kind.saddles" : shape.skirt() ? "kind.skirt" : "kind.bare")),
                    Component.translatable(KEY + "diameter", number(shape.diameter())),
                    Component.translatable(KEY + "tangent_length", number(shape.tangentLength())),
                    Component.translatable(KEY + "length", number(shape.overallLength())),
                    Component.translatable(KEY + "top", number(shape.topElevation())),
                    support,
                    Component.translatable(KEY + "volume", number(shape.volume())),
                    Component.translatable(KEY + "area", number(shape.surfaceArea()))));
        }
        lines.add(Component.translatable(KEY + "cells", layout.shell().size(), layout.interior().size()));
        lines.add(Component.translatable(KEY + "controller", controller.toShortString()));
        // Each nozzle: what it is for, the port its position makes it and where it faces (any pipe connects; its neck takes
        // the pipe's size, owner decision 2026-10-21: a 1 m pipe has a 1 m root).
        var transform = shape.transform();
        for (var nozzle : shape.nozzles()) {
            var out = transform.direction(nozzle.direction());
            var facing = Direction.getNearest(out[0], out[1], out[2]).getSerializedName();
            lines.add(Component.translatable(KEY + "nozzle", Component.translatable(KEY + "role." + nozzle.role().name().toLowerCase(Locale.ROOT)),
                    Component.translatable(KEY + "port." + nozzle.role().port.name().toLowerCase(Locale.ROOT)),
                    Component.translatable(KEY + "facing." + facing)));
        }
        return List.copyOf(lines);
    }

    /** A unit's kind and sizes, as its design says them ({@link UnitDesign#datasheet}), then its length, top and support's height. */
    private static List<Component> unit(EquipmentShape shape) {
        var design = UnitDesign.of(shape.kind());
        String kind = shape.kind().name().toLowerCase(Locale.ROOT);
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable(KEY + "kind." + kind + design.variant(shape)));
        for (var line : design.datasheet(shape)) lines.add(Component.translatable(KEY + line.key(), line.args()));
        lines.add(Component.translatable(KEY + "unit_length", number(shape.overallLength())));
        lines.add(Component.translatable(KEY + "top", number(shape.topElevation())));
        lines.add(Component.translatable(KEY + "unit_support." + kind, shape.support()));
        return lines;
    }

    @Override
    protected void init() {
        panel = WIDTH;
        for (var line : lines) panel = Math.max(panel, font.width(line) + 16);
        left = (width - panel) / 2;
        // Lines closer where they would not fit the window (an FCC's or a coker's 18 in a 240-pixel-high GUI), never
        // closer than the font's own line height.
        pitch = Math.max(font.lineHeight, Math.min(LINE, (height - FRAME - 4) / Math.max(1, lines.size())));
        tall = lines.size() * pitch + FRAME;
        top = Math.max(0, (height - tall) / 2);
        int y = top + tall - 26;
        // A moving unit's switch: every unit's fans, shafts and rods move, or stand still as a stopped mover's do.
        if (moving) addRenderableWidget(Button.builder(motionLabel(), b -> {
            MachineAnimation.toggle();
            b.setMessage(motionLabel());
        }).bounds(left + panel / 2 - 144, y, 140, 20).tooltip(Tooltip.create(Component.translatable(KEY + "motion.hint"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(moving ? left + panel / 2 + 4 : left + (panel - 80) / 2, y, 80, 20).build());
    }

    private static Component motionLabel() {
        return Component.translatable(KEY + (MachineAnimation.running() ? "motion.running" : "motion.still"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + panel, top + tall, ProcessUi.BG);
        graphics.renderOutline(left, top, panel, tall, ProcessUi.LINE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, left + 8, top + 8, ProcessUi.TEXT);
        for (int i = 0; i < lines.size(); i++)
            graphics.drawString(font, lines.get(i), left + 8, top + 24 + i * pitch, i == 0 ? ProcessUi.MUTED : ProcessUi.TEXT);
    }
}
