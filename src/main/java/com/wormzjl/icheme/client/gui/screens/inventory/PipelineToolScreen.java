package com.wormzjl.icheme.client.gui.screens.inventory;

import com.wormzjl.icheme.client.gui.common.ProcessUi;
import com.wormzjl.icheme.network.PipelineToolNetwork;
import com.wormzjl.icheme.world.inventory.PipelineToolMenu;
import com.wormzjl.icheme.world.item.PipelineToolSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/** A single item setting; all displayed data comes from the opening menu, without process requests. */
public final class PipelineToolScreen extends AbstractContainerScreen<PipelineToolMenu> {
    private EditBox diameter;
    private Button save;
    private Double parsed;

    public PipelineToolScreen(PipelineToolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 136;
    }

    @Override
    protected void init() {
        String draft = diameter == null ? Double.toString(menu.initialSettings().diameter()) : diameter.getValue();
        super.init();
        diameter = new EditBox(font, leftPos + 12, topPos + 46, imageWidth - 24, 20, label("diameter"));
        diameter.setMaxLength(32);
        diameter.setValue(draft);
        addRenderableWidget(diameter);
        save = addRenderableWidget(Button.builder(label("save"), button -> save())
                .bounds(leftPos + 12, topPos + 106, 94, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(leftPos + 114, topPos + 106, 94, 20).build());
        diameter.setResponder(this::validate);
        validate(draft);
        setInitialFocus(diameter);
    }

    private static Component label(String key) {
        return Component.translatable("gui.icheme.pipeline_tool." + key);
    }

    private void validate(String text) {
        parsed = null;
        try {
            double value = Double.parseDouble(text.trim());
            if (PipelineToolSettings.valid(value)) parsed = value;
        } catch (NumberFormatException invalid) {
            // Incomplete text is a normal editing state; retain it until the player enters a valid value.
        }
        save.active = parsed != null;
    }

    private void save() {
        if (parsed == null) return;
        PipelineToolNetwork.sendSave(menu.containerId, parsed);
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
    }

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
