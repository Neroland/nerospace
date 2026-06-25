package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;


import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Base class for Nerospace machine screens ("spacified"). Draws a sci-fi hull panel (256x256 PNG at
 * {@code assets/nerospace/textures/gui/<name>.png}, top-left {@code imageWidth x imageHeight} = the
 * panel) and themes the title/inventory labels for a dark background. Subclasses draw their
 * gauges/readouts in {@link #extractForeground} and may use the gauge/label helpers. 26.1 renders
 * container screens via {@code extract*(GuiGraphicsExtractor, ...)}: the panel is blitted in
 * {@link #extractContents}, custom drawing on top.
 */
public abstract class TexturedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    /** Shared sci-fi palette. */
    protected static final int INK = 0xFF05080D;       // near-black trough border
    protected static final int TROUGH = 0xFF0B1119;    // gauge backing
    protected static final int TITLE = 0xFFD6ECFF;     // bright label
    protected static final int SUBTLE = 0xFF8DA0B4;    // dim label

    private final Identifier background;
    /** Machine accent colour (ARGB). */
    protected final int accent;

    protected TexturedContainerScreen(T menu, Inventory playerInventory,
                                      Component title, Identifier background,
                                      int accent, int width, int height) {
        super(NerospaceCommon.requireNonNull(menu), NerospaceCommon.requireNonNull(playerInventory),
                NerospaceCommon.requireNonNull(title), width, height);
        this.background = NerospaceCommon.requireNonNull(background);
        this.accent = accent;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.blit(RenderPipelines.GUI_TEXTURED, this.background, this.leftPos, this.topPos,
                0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        extractForeground(extractor);
    }

    /** Subclass hook: draw gauges + readouts on top of the panel (absolute coords via leftPos/topPos). */
    protected void extractForeground(GuiGraphicsExtractor extractor) {
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, NerospaceCommon.requireNonNull(this.title), this.titleLabelX, this.titleLabelY,
                TITLE, false);
        extractor.text(this.font, NerospaceCommon.requireNonNull(this.playerInventoryTitle), this.inventoryLabelX,
                this.inventoryLabelY, SUBTLE, false);
    }

    // --- Drawing helpers (panel-relative dx/dy) -----------------------------

    /** A horizontal gauge: dark trough with an accent fill {@code frac} (0..1) of its width. */
    protected void hGauge(GuiGraphicsExtractor g, int dx, int dy, int w, int h, float frac, int fill) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, INK);
        g.fill(x, y, x + w, y + h, TROUGH);
        int fw = Math.max(0, Math.min(w, Math.round(w * frac)));
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, fill);
            g.fill(x, y, x + fw, y + 1, 0x55FFFFFF); // top sheen
        }
    }

    /**
     * A segmented gauge: the trough divided into ticked cells with an animated leading edge — energy
     * buffers read at a glance, exact values stay on the labels.
     */
    protected void segGauge(GuiGraphicsExtractor g, int dx, int dy, int w, int h, float frac, int fill) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, INK);
        g.fill(x, y, x + w, y + h, TROUGH);
        int segments = Math.max(4, w / 10);
        int segW = w / segments;
        int fw = Math.max(0, Math.min(w, Math.round(w * frac)));
        for (int s = 0; s < segments; s++) {
            int sx = x + s * segW;
            int sw = (s == segments - 1) ? (x + w - sx) : segW - 1; // 1px tick gap between cells
            int lit = Math.max(0, Math.min(sw, fw - s * segW));
            if (lit > 0) {
                g.fill(sx, y, sx + lit, y + h, fill);
                g.fill(sx, y, sx + lit, y + 1, 0x55FFFFFF);
            }
        }
        if (fw > 0 && fw < w) {
            long time = System.currentTimeMillis() / 250L;
            if ((time & 1L) == 0L) {
                g.fill(x + fw - 1, y, x + fw, y + h, 0xAAFFFFFF);
            }
        }
    }

    /**
     * A liquid gauge: two-tone wave fill so tank contents read as FLUID, not paint — pass the content
     * colour (fuel amber, O₂ cyan, water blue, meltwater frost).
     */
    protected void fluidGauge(GuiGraphicsExtractor g, int dx, int dy, int w, int h, float frac, int fill) {
        int x = this.leftPos + dx;
        int y = this.topPos + dy;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, INK);
        g.fill(x, y, x + w, y + h, TROUGH);
        int fw = Math.max(0, Math.min(w, Math.round(w * frac)));
        if (fw <= 0) {
            return;
        }
        int dark = darken(fill);
        long t = System.currentTimeMillis() / 200L;
        for (int px = 0; px < fw; px++) {
            boolean crest = ((px + t) / 3L) % 2L == 0L;
            g.fill(x + px, y, x + px + 1, y + h, crest ? fill : dark);
        }
        g.fill(x, y, x + fw, y + 1, 0x66FFFFFF);           // meniscus sheen
        g.fill(x + fw - 1, y, x + fw, y + h, 0x88FFFFFF);  // surface line
    }

    private static int darken(int argb) {
        int r = (int) (((argb >> 16) & 0xFF) * 0.72F);
        int gg = (int) (((argb >> 8) & 0xFF) * 0.72F);
        int b = (int) ((argb & 0xFF) * 0.72F);
        return (argb & 0xFF000000) | (r << 16) | (gg << 8) | b;
    }

    /** Left-aligned label text at a panel-relative position. */
    protected void label(GuiGraphicsExtractor g, Component text, int dx, int dy, int color) {
        g.text(this.font, NerospaceCommon.requireNonNull(text), this.leftPos + dx, this.topPos + dy, color, false);
    }

    /** Centred label text within {@code [dx, dx+width)}. */
    protected void labelCentered(GuiGraphicsExtractor g, Component text, int dx, int width, int dy,
            int color) {
        g.centeredText(this.font, NerospaceCommon.requireNonNull(text), this.leftPos + dx + width / 2,
                this.topPos + dy, color);
    }
}
