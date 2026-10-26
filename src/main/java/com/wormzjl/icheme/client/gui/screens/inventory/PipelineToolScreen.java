package com.wormzjl.icheme.client.gui.screens.inventory;

import com.wormzjl.icheme.client.gui.common.PaintSwatch;
import com.wormzjl.icheme.client.gui.common.ProcessUi;
import com.wormzjl.icheme.network.FluidNetwork;
import com.wormzjl.icheme.science.equipment.Paint;
import com.wormzjl.icheme.world.inventory.PipelineToolMenu;
import com.wormzjl.icheme.world.item.PipeLayingToolItem;
import com.wormzjl.icheme.world.item.PipelineToolItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/**
 * The pipeline tools' settings; all displayed data comes from the opening menu, without process requests. Both tools set
 * the bore; the laying tool also sets the paint of the lines it lays, from the equipment constructor's palette, as
 * #RRGGBB, or bare steel (batch 2026-10-26-pipe-laying-system).
 */
public final class PipelineToolScreen extends AbstractContainerScreen<PipelineToolMenu> {
    /** The paint rows' top, the palette's swatches per row, and the panel height with and without them. */
    private static final int PAINT_TOP = 100, SWATCHES_PER_ROW = 8, PLAIN_HEIGHT = 136, PAINT_HEIGHT = 230;
    private EditBox diameter;
    private EditBox custom;
    private Button save;
    private Double parsed;
    /** The paint draft, saved with the bore: 0xRRGGBB or {@link Paint#BARE}. */
    private int paint;

    public PipelineToolScreen(PipelineToolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = menu.paints() ? PAINT_HEIGHT : PLAIN_HEIGHT;
        paint = menu.initialPaint();
    }

    @Override
    protected void init() {
        String draft = diameter == null ? Double.toString(menu.initialSettings().diameter()) : diameter.getValue();
        super.init();
        diameter = new EditBox(font, leftPos + 12, topPos + 46, imageWidth - 24, 20, label("diameter"));
        diameter.setMaxLength(32);
        diameter.setValue(draft);
        addRenderableWidget(diameter);
        custom = null;
        if (menu.paints()) paintRows();
        int buttons = topPos + imageHeight - 30;
        save = addRenderableWidget(Button.builder(label("save"), button -> save())
                .bounds(leftPos + 12, buttons, 94, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(leftPos + 114, buttons, 94, 20).build());
        diameter.setResponder(this::validate);
        validate(draft);
        setInitialFocus(diameter);
    }

    /** The palette, a custom colour as #RRGGBB, and bare steel: the same choices as the constructor's Paint page. */
    private void paintRows() {
        for (int i = 0; i < Paint.PALETTE.size(); i++) {
            int x = leftPos + 12 + i % SWATCHES_PER_ROW * (PaintSwatch.WIDTH + PaintSwatch.GAP);
            int y = topPos + PAINT_TOP + 16 + i / SWATCHES_PER_ROW * (PaintSwatch.HEIGHT + PaintSwatch.GAP);
            addRenderableWidget(new PaintSwatch(x, y, Paint.PALETTE.get(i), () -> paint, this::paint));
        }
        int row = topPos + PAINT_TOP + 16 + 2 * (PaintSwatch.HEIGHT + PaintSwatch.GAP) + 2;
        custom = new EditBox(font, leftPos + 12, row, 74, 16, label("paint.custom"));
        custom.setMaxLength(7);
        custom.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        custom.setValue(paint == Paint.BARE ? "" : Paint.hex(paint));
        custom.setResponder(text -> {
            if (Paint.isHex(text)) paint(Paint.parseHex(text));
        });
        custom.setTooltip(Tooltip.create(label("paint.custom.hint")));
        addRenderableWidget(custom);
        addRenderableWidget(Button.builder(label("paint.bare"), button -> paint(Paint.BARE))
                .bounds(leftPos + 94, row, 114, 16).tooltip(Tooltip.create(label("paint.bare.hint"))).build());
    }

    /** Sets the paint draft; the custom field shows it unless the player is typing in it. */
    private void paint(int rgb) {
        paint = rgb;
        if (custom != null && !custom.isFocused()) custom.setValue(rgb == Paint.BARE ? "" : Paint.hex(rgb));
    }

    private static Component label(String key) {
        return Component.translatable("gui.icheme.pipeline_tool." + key);
    }

    private void validate(String text) {
        parsed = null;
        try {
            double value = Double.parseDouble(text.trim());
            if (PipelineToolItem.Settings.valid(value)) parsed = value;
        } catch (NumberFormatException invalid) {
            // Incomplete text is a normal editing state; retain it until the player enters a valid value.
        }
        save.active = parsed != null;
    }

    private void save() {
        if (parsed == null) return;
        FluidNetwork.savePipelineTool(menu.containerId, parsed, menu.paints() ? paint : Paint.BARE);
        onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, ProcessUi.LINE);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, ProcessUi.BG);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, ProcessUi.TEXT, false);
        graphics.drawString(font, label("diameter"), 12, 32, ProcessUi.TEXT, false);
        graphics.drawString(font, label("range"), 12, 72, ProcessUi.MUTED, false);
        if (parsed == null) graphics.drawString(font, label("invalid"), 12, 87, ProcessUi.WARN, false);
        if (!menu.paints()) return;
        // The paint: its name and a sample of it (bare steel shows the steel's own grey).
        var name = Component.translatable("gui.icheme.pipeline_tool.paint.named", PipeLayingToolItem.paintName(paint));
        graphics.drawString(font, name, 12, PAINT_TOP + 2, ProcessUi.TEXT, false);
        int sample = imageWidth - 26;
        graphics.fill(sample, PAINT_TOP, sample + 14, PAINT_TOP + 12, ProcessUi.LINE);
        graphics.fill(sample + 1, PAINT_TOP + 1, sample + 13, PAINT_TOP + 11, 0xFF000000 | (paint == Paint.BARE ? BARE_STEEL : paint));
        int note = PAINT_TOP + 16 + 2 * (PaintSwatch.HEIGHT + PaintSwatch.GAP) + 24;
        graphics.drawString(font, label("paint.parts"), 12, note, ProcessUi.MUTED, false);
    }

    /** The bare pipe sprite's mean colour (textures/block/equipment_pipe.png), for the sample. */
    private static final int BARE_STEEL = 0x969BA0;

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        if (ProcessUi.textKeyPressed(this, key, scan, modifiers)) return true;
        return super.keyPressed(key, scan, modifiers);
    }
}
