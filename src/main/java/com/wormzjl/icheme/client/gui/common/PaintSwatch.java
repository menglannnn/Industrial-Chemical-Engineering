package com.wormzjl.icheme.client.gui.common;

import com.wormzjl.icheme.science.equipment.Paint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A palette colour (owner request 2026-10-16): a filled square, outlined in white while it is the edited colour. The
 * equipment constructor's Paint page and the pipe laying tool's settings show the same palette with it.
 */
public final class PaintSwatch extends AbstractButton {
    /** A swatch's size, and the gap between two. */
    public static final int WIDTH = 19, HEIGHT = 16, GAP = 4;
    private final Paint.Swatch swatch;
    private final IntSupplier edited;
    private final IntConsumer pick;

    /** {@code edited} reads the colour being edited; {@code pick} paints it this swatch's colour. */
    public PaintSwatch(int x, int y, Paint.Swatch swatch, IntSupplier edited, IntConsumer pick) {
        super(x, y, WIDTH, HEIGHT, name(swatch));
        this.swatch = swatch;
        this.edited = edited;
        this.pick = pick;
        setTooltip(Tooltip.create(Component.translatable("gui.icheme.equipment_constructor.paint.swatch", swatch.ral(), name(swatch))));
    }

    private static Component name(Paint.Swatch swatch) {
        return Component.translatable("gui.icheme.paint." + swatch.key());
    }

    @Override
    public void onPress() {
        pick.accept(swatch.rgb());
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean chosen = edited.getAsInt() == swatch.rgb();
        int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
        g.fill(x0, y0, x1, y1, chosen ? 0xFFFFFFFF : isHoveredOrFocused() ? ProcessUi.TEXT : ProcessUi.LINE);
        if (chosen) g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF000000);
        int inset = chosen ? 2 : 1;
        g.fill(x0 + inset, y0 + inset, x1 - inset, y1 - inset, 0xFF000000 | swatch.rgb());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
