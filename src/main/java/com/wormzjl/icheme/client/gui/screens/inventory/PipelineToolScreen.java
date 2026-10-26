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
 * the bore; the laying tool also sets the paints of the lines it lays (batch 2026-10-26-pipe-laying-system): a primary
 * for the pipe and a secondary for the flanges, each picked as on the equipment constructor's Paint page, from its
 * palette, as #RRGGBB, or bare steel.
 */
public final class PipelineToolScreen extends AbstractContainerScreen<PipelineToolMenu> {
    /** The paint rows' top, the channel rows' pitch, the palette's top and swatches per row, and the panel height with and without them. */
    private static final int PAINT_TOP = 100, CHANNEL_ROW = 18, PALETTE_TOP = PAINT_TOP + 2 * CHANNEL_ROW + 4, SWATCHES_PER_ROW = 8, PLAIN_HEIGHT = 136, PAINT_HEIGHT = 270;
    /** The channel buttons' width, and the lines below the custom row. */
    private static final int CHANNEL_WIDTH = 74, NOTES = PALETTE_TOP + 2 * (PaintSwatch.HEIGHT + PaintSwatch.GAP) + 24, NOTE = 11;
    private EditBox diameter;
    private EditBox custom;
    private Button save;
    private Double parsed;
    /** The paint drafts, saved with the bore, each 0xRRGGBB or {@link Paint#BARE}: the primary ({@link Paint#BODY}) and the secondary ({@link Paint#STRUCTURE}). */
    private final int[] paints = new int[2];
    /** The channel the palette, the custom field and bare steel edit. */
    private int channel = Paint.BODY;
    /** The channel buttons, which name their colours (null without paint rows). */
    private final Button[] channelButtons = new Button[2];

    public PipelineToolScreen(PipelineToolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = menu.paints() ? PAINT_HEIGHT : PLAIN_HEIGHT;
        paints[Paint.BODY] = menu.initialPaint();
        paints[Paint.STRUCTURE] = menu.initialSecondary();
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
        channelButtons[0] = channelButtons[1] = null;
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

    /**
     * As the constructor's Paint page: the primary's and the secondary's colours, each a button that picks it for editing;
     * the palette; a custom colour as #RRGGBB; bare steel.
     */
    private void paintRows() {
        for (int c : new int[]{Paint.BODY, Paint.STRUCTURE}) {
            int k = c;
            channelButtons[c] = addRenderableWidget(Button.builder(colourName(paints[c]), button -> {
                channel = k;
                rebuildWidgets();
            }).bounds(leftPos + imageWidth - 12 - CHANNEL_WIDTH, topPos + PAINT_TOP + c * CHANNEL_ROW, CHANNEL_WIDTH, 16)
                    .tooltip(Tooltip.create(label(CHANNELS[c] + ".hint"))).build());
        }
        for (int i = 0; i < Paint.PALETTE.size(); i++) {
            int x = leftPos + 12 + i % SWATCHES_PER_ROW * (PaintSwatch.WIDTH + PaintSwatch.GAP);
            int y = topPos + PALETTE_TOP + i / SWATCHES_PER_ROW * (PaintSwatch.HEIGHT + PaintSwatch.GAP);
            addRenderableWidget(new PaintSwatch(x, y, Paint.PALETTE.get(i), () -> paints[channel], this::paint));
        }
        int row = topPos + PALETTE_TOP + 2 * (PaintSwatch.HEIGHT + PaintSwatch.GAP) + 2;
        custom = new EditBox(font, leftPos + 12, row, 74, 16, label("paint.custom"));
        custom.setMaxLength(7);
        custom.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        custom.setValue(paints[channel] == Paint.BARE ? "" : Paint.hex(paints[channel]));
        custom.setResponder(text -> {
            if (Paint.isHex(text)) paint(Paint.parseHex(text));
        });
        custom.setTooltip(Tooltip.create(label("paint.custom.hint")));
        addRenderableWidget(custom);
        addRenderableWidget(Button.builder(label("paint.bare"), button -> paint(Paint.BARE))
                .bounds(leftPos + 94, row, 114, 16).tooltip(Tooltip.create(label("paint.bare.hint"))).build());
    }

    /** The channels' label keys under {@code gui.icheme.pipeline_tool.}, by {@link Paint#BODY} and {@link Paint#STRUCTURE}. */
    private static final String[] CHANNELS = {"paint.primary", "paint.secondary"};

    /** Paints the edited channel {@code rgb}: its button names the colour, and the custom field shows it unless typed in. */
    private void paint(int rgb) {
        paints[channel] = rgb;
        if (channelButtons[channel] != null) channelButtons[channel].setMessage(colourName(rgb));
        if (custom != null && !custom.isFocused()) custom.setValue(rgb == Paint.BARE ? "" : Paint.hex(rgb));
    }

    /** A colour as a channel button names it: bare steel, its palette number, or #RRGGBB. */
    private static Component colourName(int rgb) {
        if (rgb == Paint.BARE) return label("paint.bare");
        var swatch = Paint.swatch(rgb);
        return Component.literal(swatch != null ? "RAL " + swatch.ral() : Paint.hex(rgb));
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
        FluidNetwork.savePipelineTool(menu.containerId, parsed, menu.paints() ? paints[Paint.BODY] : Paint.BARE, menu.paints() ? paints[Paint.STRUCTURE] : Paint.BARE);
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
        // Each channel's label (the edited one marked) and a sample of its colour (bare steel shows the steel's own grey);
        // then the edited colour's name, what it paints, and what keeps its own colour.
        for (int c : new int[]{Paint.BODY, Paint.STRUCTURE}) {
            int y = PAINT_TOP + c * CHANNEL_ROW, sample = imageWidth - 12 - CHANNEL_WIDTH - 18;
            graphics.drawString(font, (c == channel ? "▶ " : "  ") + label(CHANNELS[c]).getString(), 12, y + 4, c == channel ? ProcessUi.ACCENT : ProcessUi.TEXT, false);
            graphics.fill(sample, y + 1, sample + 14, y + 15, ProcessUi.LINE);
            graphics.fill(sample + 1, y + 2, sample + 13, y + 14, 0xFF000000 | (paints[c] == Paint.BARE ? BARE_STEEL : paints[c]));
        }
        graphics.drawString(font, Component.translatable("gui.icheme.pipeline_tool.paint.named", PipeLayingToolItem.paintName(paints[channel])), 12, NOTES, ProcessUi.MUTED, false);
        graphics.drawString(font, label(CHANNELS[channel] + ".parts"), 12, NOTES + NOTE, ProcessUi.MUTED, false);
        graphics.drawString(font, label("paint.unpainted"), 12, NOTES + 2 * NOTE, ProcessUi.MUTED, false);
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
