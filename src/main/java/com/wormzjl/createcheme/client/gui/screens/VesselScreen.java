package com.wormzjl.createcheme.client.gui.screens;

import com.wormzjl.createcheme.client.FanAnimation;
import com.wormzjl.createcheme.client.gui.common.ProcessUi;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ProcessUnit;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A column's or a process unit's screen: its datasheet, with its nozzles. A drum shows the same datasheet on a tab of
 * its tank screen (owner request 2026-10-16), from {@link #datasheet}. It shows geometry only, read from the cells the
 * client already has, so it requests nothing.
 */
public final class VesselScreen extends Screen {
    /** The panel's least width; a longer line widens it (a unit's nozzle lines run past 256). */
    private static final int WIDTH = 256, LINE = 12;
    private static final String KEY = "gui.createcheme.vessel.";
    private final List<Component> lines;
    private final boolean fans;
    private int left, top, panel;

    public VesselScreen(EquipmentStructure.Vessel vessel) {
        super(Component.translatable(KEY + "title." + vessel.shape().kind().name().toLowerCase(Locale.ROOT)));
        lines = datasheet(vessel);
        fans = vessel.shape().kind() == EquipmentShape.Kind.AIR_COOLER;
    }

    /** The vessel's datasheet, a line each: its kind first, then its sizes and cells, then each nozzle. */
    public static List<Component> datasheet(EquipmentStructure.Vessel vessel) {
        var shape = vessel.shape();
        var layout = ShapeRasterizer.layout(shape);
        var cell = layout.controller();
        var controller = vessel.origin().offset(cell.x(), cell.y(), cell.z());
        var lines = new ArrayList<Component>();
        if (shape.unit()) lines.addAll(unit(shape));
        else {
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
        // Each nozzle: what it is for, the port its position makes it, where it faces, and the widest neck the vessel carries there (any pipe connects).
        var transform = shape.transform();
        for (var nozzle : shape.nozzles()) {
            var out = transform.direction(nozzle.direction());
            var facing = Direction.getNearest(out[0], out[1], out[2]).getSerializedName();
            lines.add(Component.translatable(KEY + "nozzle", Component.translatable(KEY + "role." + nozzle.role().name().toLowerCase(Locale.ROOT)),
                    Component.translatable(KEY + "port." + nozzle.role().port.name().toLowerCase(Locale.ROOT)),
                    Component.translatable(KEY + "facing." + facing), size(nozzle.maxOd())));
        }
        return List.copyOf(lines);
    }

    /**
     * A unit's kind and sizes: an exchanger's shell, tubes and their surface; a kettle's bundle, shell, tubes and surface;
     * an air cooler bay's size, fans and face; an ejector's suction and length; a pump's nozzles, flow and motor; a
     * compressor's throws, bore, stroke and displacement; a heater's radiant section and duty; then its length, top and
     * support's height.
     */
    private static List<Component> unit(EquipmentShape shape) {
        double d = shape.diameter(), tubes = shape.tangentLength();
        var lines = new ArrayList<Component>();
        boolean split = shape.kind() == EquipmentShape.Kind.PUMP && shape.diameter() > ProcessUnit.OH2_MAX + 1e-9;
        lines.add(Component.translatable(KEY + "kind." + shape.kind().name().toLowerCase(Locale.ROOT) + (split ? ".split" : "")));
        switch (shape.kind()) {
            case EXCHANGER -> {
                lines.add(Component.translatable(KEY + "shell", size(d)));
                lines.add(Component.translatable(KEY + "tubes", number(tubes), number(ProcessUnit.tubeSurface(d, tubes))));
            }
            case KETTLE -> {
                lines.add(Component.translatable(KEY + "bundle", size(d), size(ProcessUnit.kettleShell(d))));
                lines.add(Component.translatable(KEY + "tubes", number(tubes), number(ProcessUnit.tubeSurface(d, tubes))));
            }
            case AIR_COOLER -> {
                var fans = ProcessUnit.fans(d, tubes);
                lines.add(Component.translatable(KEY + "bank", shape.bank().parallel(), shape.bank().series()));
                lines.add(Component.translatable(KEY + "bay", size(d), number(tubes), number(ProcessUnit.bundleFace(d, tubes))));
                lines.add(Component.translatable(KEY + "fans", fans.count(), size(fans.diameter()), Math.round(100 * ProcessUnit.coverage(d, tubes))));
            }
            case EJECTOR -> lines.add(Component.translatable(KEY + "suction", size(d), number(ProcessUnit.ejectorLength(d))));
            case PUMP -> {
                lines.add(Component.translatable(KEY + "pump", size(d), size(ProcessUnit.pumpSuction(d))));
                lines.add(Component.translatable(KEY + "pump_duty", whole(ProcessUnit.pumpFlow(d)), whole(ProcessUnit.pumpPower(d))));
            }
            case COMPRESSOR -> {
                lines.add(Component.translatable(KEY + "compressor", (int) tubes, size(d), size(ProcessUnit.stroke(d))));
                lines.add(Component.translatable(KEY + "displacement", whole(ProcessUnit.displacement(d, (int) tubes)), whole(ProcessUnit.compressorSpeed(d))));
            }
            case VERTICAL_HEATER, CABIN_HEATER -> {
                int cells = shape.kind() == EquipmentShape.Kind.CABIN_HEATER ? ProcessUnit.cabinCells(tubes) : 1;
                lines.add(Component.translatable(KEY + "radiant." + shape.kind().name().toLowerCase(Locale.ROOT) + (cells > 1 ? ".twin" : ""), size(d), number(tubes / cells)));
                lines.add(Component.translatable(KEY + "duty", number(ProcessUnit.heaterDuty(shape.kind(), d, tubes))));
            }
            default -> throw new IllegalStateException(shape.kind() + " is not a unit");
        }
        lines.add(Component.translatable(KEY + "unit_length", number(shape.overallLength())));
        lines.add(Component.translatable(KEY + "top", number(shape.topElevation())));
        lines.add(Component.translatable(KEY + "unit_support." + shape.kind().name().toLowerCase(Locale.ROOT), shape.support()));
        return lines;
    }

    @Override
    protected void init() {
        panel = WIDTH;
        for (var line : lines) panel = Math.max(panel, font.width(line) + 16);
        left = (width - panel) / 2;
        top = (height - (lines.size() * LINE + 56)) / 2;
        int y = top + lines.size() * LINE + 30;
        // An air cooler's debug switch: its fans turn, or stand still in the chunk mesh, to compare frame rates.
        if (fans) addRenderableWidget(Button.builder(fanLabel(), b -> {
            FanAnimation.toggle();
            b.setMessage(fanLabel());
        }).bounds(left + panel / 2 - 124, y, 120, 20).tooltip(Tooltip.create(Component.translatable(KEY + "fans.hint"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(fans ? left + panel / 2 + 4 : left + (panel - 80) / 2, y, 80, 20).build());
    }

    private static Component fanLabel() {
        return Component.translatable(KEY + (FanAnimation.enabled() ? "fans.turning" : "fans.still"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + panel, top + lines.size() * LINE + 56, ProcessUi.BG);
        graphics.renderOutline(left, top, panel, lines.size() * LINE + 56, ProcessUi.LINE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, left + 8, top + 8, ProcessUi.TEXT);
        for (int i = 0; i < lines.size(); i++)
            graphics.drawString(font, lines.get(i), left + 8, top + 24 + i * LINE, i == 0 ? ProcessUi.MUTED : ProcessUi.TEXT);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** A large quantity in whole units, thousands separated (30,401). */
    private static String whole(double value) {
        return String.format(Locale.ROOT, "%,d", Math.round(value));
    }

    /** A size on a 0.25 m or 0.1 m grid: one decimal where it has one, two where it has two (0.75). */
    private static String size(double value) {
        double tenths = value * 10;
        return Math.abs(tenths - Math.rint(tenths)) < 1e-6 ? number(value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
