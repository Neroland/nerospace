package za.co.neroland.nerospace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * A sci-fi styled button for the Nerospace machine screens: a dark recessed body with a glowing
 * accent border (brighter on hover or when {@linkplain #setSelected selected}) and centred text,
 * drawn entirely in {@link #extractContents} so it doesn't look like a vanilla button.
 */
public class SpaceButton extends Button {

    private final int accent;
    private boolean selected;
    private boolean dotted;
    /** A tooltip set by the screen; the automatic full-label tip never replaces it. */
    private boolean explicitTooltip;
    /** The label the automatic full-label tooltip was made for (null = none set). */
    @Nullable
    private Component autoTooltipFor;

    public SpaceButton(int x, int y, int width, int height, Component message, int accent, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    /** A dotted border (every second border pixel in the body colour): an optional / bonus item. */
    public void setDotted(boolean dotted) {
        this.dotted = dotted;
    }

    @Override
    public void setTooltip(@Nullable Tooltip tooltip) {
        this.explicitTooltip = tooltip != null;
        this.autoTooltipFor = null;
        super.setTooltip(tooltip);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int textColor = drawFrame(extractor);
        Font font = Minecraft.getInstance().font;
        Component message = getMessage();
        Component shown = fit(font, message, getWidth() - 4);
        autoTooltip(message, shown != message);
        extractor.centeredText(font, shown, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
    }

    /**
     * A cut label gets its full text as the tooltip (only when the screen set none), updated when the label changes
     * and removed once it fits again.
     */
    private void autoTooltip(Component message, boolean cut) {
        if (this.explicitTooltip) {
            return;
        }
        if (cut && this.autoTooltipFor != message) {
            this.autoTooltipFor = message;
            super.setTooltip(Tooltip.create(message));
        } else if (!cut && this.autoTooltipFor != null) {
            this.autoTooltipFor = null;
            super.setTooltip(null);
        }
    }

    /**
     * {@code text} if it fits {@code maxWidth}, else cut with an ellipsis (screens put the full text in a tooltip
     * wherever a label can be cut).
     */
    public static Component fit(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String s = text.getString();
        return Component.literal(font.plainSubstrByWidth(s, Math.max(0, maxWidth - font.width("…"))) + "…");
    }

    /** Draws border, body and sheen; returns the text colour for the current state. */
    protected int drawFrame(GuiGraphicsExtractor extractor) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isHoveredOrFocused() && this.active;

        int border = !this.active ? 0xFF262B33 : (this.selected || hovered ? this.accent : 0xFF2E4A5A);
        int body = !this.active ? 0xFF12161F : (this.selected ? 0xFF123042 : (hovered ? 0xFF16344A : 0xFF0C1E2B));

        extractor.fill(x, y, x + w, y + h, border);
        if (this.dotted) {
            for (int i = 1; i < w; i += 2) {
                extractor.fill(x + i, y, x + i + 1, y + 1, body);
                extractor.fill(x + i, y + h - 1, x + i + 1, y + h, body);
            }
            for (int j = 1; j < h; j += 2) {
                extractor.fill(x, y + j, x + 1, y + j + 1, body);
                extractor.fill(x + w - 1, y + j, x + w, y + j + 1, body);
            }
        }
        extractor.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        extractor.fill(x + 1, y + 1, x + w - 1, y + 2, 0x22FFFFFF); // top sheen
        return !this.active ? 0xFF6A7280 : (hovered || this.selected ? 0xFFFFFFFF : 0xFFCFE7FF);
    }
}
