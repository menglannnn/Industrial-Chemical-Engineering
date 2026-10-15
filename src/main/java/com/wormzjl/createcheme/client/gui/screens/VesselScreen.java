package com.wormzjl.createcheme.client.gui.screens;

import com.wormzjl.createcheme.client.gui.common.ProcessUi;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * A vessel's screen, owned by its controller cell: the datasheet of the vessel. It shows geometry only (owner decision
 * D2: no process state in the world yet), read from the cells the client already has, so it requests nothing.
 */
public final class VesselScreen extends Screen {
    private static final int WIDTH = 256, LINE = 12;
    private static final String KEY = "gui.createcheme.vessel.";
    private final List<Component> lines;
    private int left, top;

    public VesselScreen(EquipmentStructure.Vessel vessel) {
        super(Component.translatable(KEY + "title"));
        var shape = vessel.shape();
        var layout = ShapeRasterizer.layout(shape);
        var cell = layout.controller();
        var controller = vessel.origin().offset(cell.x(), cell.y(), cell.z());
        var support = shape.saddles()
                ? Component.translatable(KEY + "saddles", shape.support(), number(EquipmentShape.SADDLE_POSITION * shape.tangentLength()))
                : shape.skirt() ? Component.translatable(KEY + "skirt", shape.support()) : Component.translatable(KEY + "unsupported");
        lines = List.of(
                Component.translatable(KEY + (shape.saddles() ? "kind.saddles" : shape.skirt() ? "kind.skirt" : "kind.bare")),
                Component.translatable(KEY + "diameter", number(shape.diameter())),
                Component.translatable(KEY + "tangent_length", number(shape.tangentLength())),
                Component.translatable(KEY + "length", number(shape.overallLength())),
                Component.translatable(KEY + "top", number(shape.topElevation())),
                support,
                Component.translatable(KEY + "volume", number(shape.volume())),
                Component.translatable(KEY + "area", number(shape.surfaceArea())),
                Component.translatable(KEY + "cells", layout.shell().size(), layout.interior().size()),
                Component.translatable(KEY + "controller", controller.toShortString()));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - (lines.size() * LINE + 56)) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + (WIDTH - 80) / 2, top + lines.size() * LINE + 30, 80, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + lines.size() * LINE + 56, ProcessUi.BG);
        graphics.renderOutline(left, top, WIDTH, lines.size() * LINE + 56, ProcessUi.LINE);
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
}
